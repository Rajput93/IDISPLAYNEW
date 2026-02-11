package com.app.idisplaynew.data.repository

import com.app.idisplaynew.data.local.AppDatabase
import com.app.idisplaynew.data.local.MediaDownloadManager
import com.app.idisplaynew.data.local.entity.MediaFileEntity
import com.app.idisplaynew.data.local.entity.ScheduleEntity
import com.app.idisplaynew.data.model.ScheduleCurrentResponse
import com.app.idisplaynew.ui.utils.DataStoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant

private val json = Json { ignoreUnknownKeys = true; isLenient = true }

/** Result of a sync: how many new downloads and where they are stored. */
data class SyncResult(
    val downloadedImages: Int,
    val downloadedVideos: Int,
    val storagePath: String
)

class ScheduleRepository(
    private val api: Repository,
    private val db: AppDatabase,
    private val downloadManager: MediaDownloadManager,
    private val dataStore: DataStoreManager
) {

    private val scheduleDao get() = db.scheduleDao()
    private val mediaDao get() = db.mediaFileDao()

    /** Emitted after sync so display flow re-queries. */
    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    /** Incremented after every sync so layout flow always re-reads (zone add/delete updates UI). */
    private val layoutRefreshCount = MutableStateFlow(0)

    /** Tickers from API only – not saved in DB; updated on every sync, display directly. */
    private val _tickersFromApi = MutableStateFlow<List<ScheduleCurrentResponse.ScheduleResult.Ticker>>(emptyList())
    val tickersFromApi: StateFlow<List<ScheduleCurrentResponse.ScheduleResult.Ticker>> = _tickersFromApi.asStateFlow()

    /** Current time in ISO-8601 for start/end comparison. */
    private fun nowIso(): String = Instant.now().toString()

    /**
     * Layout from local DB (with local media paths). Tickers from API (tickersFromApi) – not from DB.
     * Re-emits when schedule/media_file changes or after sync.
     */
    fun getCurrentLayoutAndTickers(): Flow<Pair<ScheduleCurrentResponse.ScheduleResult.Layout?, List<ScheduleCurrentResponse.ScheduleResult.Ticker>>> =
        combine(
            getCurrentLayoutFlow(),
            _tickersFromApi
        ) { layout, tickers -> layout to tickers }
            .flowOn(Dispatchers.IO)

    /** Layout only from local DB. Tickers are not read from DB. */
    private fun getCurrentLayoutFlow(): Flow<ScheduleCurrentResponse.ScheduleResult.Layout?> =
        merge(
            flow {
                emit(nowIso())
                while (true) {
                    delay(1000)
                    emit(nowIso())
                }
            },
            refreshTrigger.map { nowIso() },
            layoutRefreshCount.map { nowIso() }
        )
            .flatMapLatest { now -> scheduleDao.getCurrentSchedule(now) }
            .flatMapLatest { entity ->
                flow {
                    if (entity == null) {
                        emit(null)
                        return@flow
                    }
                    val layout = entityToLayout(entity)
                    if (layout == null) {
                        emit(null)
                        return@flow
                    }
                    mediaDao.getAllByScheduleIdFlow(entity.scheduleId).collect { mediaList ->
                        emit(mergeLayoutWithMedia(layout, mediaList))
                    }
                }
            }
            .flowOn(Dispatchers.IO)

    private fun mergeLayoutWithMedia(
        layout: ScheduleCurrentResponse.ScheduleResult.Layout,
        mediaList: List<MediaFileEntity>
    ): ScheduleCurrentResponse.ScheduleResult.Layout {
        // Map (zoneId, mediaId) -> local path so we can show video/image even when API sends blank fileName
        val localPathByZoneAndMedia = mediaList
            .filter { it.localPath != null && downloadManager.isFilePresent(it.localPath!!) }
            .associate { (it.zoneId to it.mediaId) to it.localPath!! }
        val urlByFileName = mediaList
            .filter { it.localPath != null && downloadManager.isFilePresent(it.localPath!!) }
            .associate { it.fileName to it.localPath!! }
        val zonesWithLocalUrls = layout.zones.map { zone ->
            zone.copy(playlist = zone.playlist.map { item ->
                val localPath = localPathByZoneAndMedia[zone.zoneId to item.mediaId]
                    ?: item.fileName.takeIf { it.isNotBlank() }?.let { urlByFileName[it] }
                val isVideoOrImage = item.type.equals("video", ignoreCase = true) || item.type.equals("image", ignoreCase = true)
                val displayUrl = when {
                    localPath != null -> "file://$localPath"
                    isVideoOrImage -> "" // show only when downloaded
                    else -> item.url
                }
                item.copy(url = displayUrl)
            })
        }
        return layout.copy(zones = zonesWithLocalUrls)
    }

    private fun entityToLayout(entity: ScheduleEntity): ScheduleCurrentResponse.ScheduleResult.Layout? {
        return try {
            json.decodeFromString<ScheduleCurrentResponse.ScheduleResult.Layout>(entity.layoutJson)
        } catch (_: Exception) {
            null
        }
    }

    /** Derive a unique fileName when API sends blank (so we can still download and look up by zoneId+mediaId). */
    private fun deriveFileNameFromUrl(url: String, zoneId: Int, mediaId: Int, type: String): String {
        val fromUrl = url.substringAfterLast('/').substringBefore('?').trim()
        val ext = when {
            fromUrl.contains(".") -> fromUrl.substringAfterLast('.', "").take(4)
            type.equals("video", ignoreCase = true) -> "mp4"
            type.equals("image", ignoreCase = true) -> "jpg"
            else -> "bin"
        }
        return "media_${zoneId}_${mediaId}.${if (ext.isNotBlank()) ext else "mp4"}"
    }

    /** Resolve relative media URL (e.g. /uploads/video.mp4) to absolute using API baseUrl. */
    private fun resolveMediaUrl(baseUrl: String, url: String): String {
        if (url.isBlank()) return url
        if (url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)) return url
        return if (url.startsWith("/")) "$baseUrl$url" else "$baseUrl/$url"
    }

    /** Full path where downloaded images/videos are stored. */
    fun getMediaStoragePath(): String = downloadManager.getMediaStoragePath()

    /**
     * Moves files from legacy internal path to current (external) storage and updates DB.
     * Call once per app/sync so the displayed path contains all downloaded files.
     */
    suspend fun migrateMediaToExternalStorage(): Int = withContext(Dispatchers.IO) {
        val legacyPath = downloadManager.getLegacyInternalMediaPath()
        val all = mediaDao.getAll()
        var count = 0
        all.forEach { media ->
            val path = media.localPath ?: return@forEach
            if (!path.startsWith(legacyPath)) return@forEach
            val newPath = downloadManager.copyToCurrentStorage(path, media.fileName) ?: return@forEach
            mediaDao.insert(media.copy(localPath = newPath))
            count++
        }
        count
    }

    /**
     * Call this every 1 second. Fetches from API; if data changed, updates local DB,
     * downloads new media (skips if fileName already present), deletes expired and removed.
     * @return SyncResult with download counts and storage path (for toast); null if no sync or error.
     */
    suspend fun syncFromApi(): SyncResult? {
        migrateMediaToExternalStorage()
        val baseUrl = dataStore.baseUrl.first() ?: return null
        val token = dataStore.authToken.first() ?: return null

        val response = try {
            api.getScheduleCurrent(baseUrl, token)
        } catch (_: Exception) {
            return null
        }

        val now = nowIso()
        val storagePath = downloadManager.getMediaStoragePath()

        // 1) Delete expired schedules (endTime < now) and their media files
        val allSchedules = scheduleDao.getAll()
        val expired = allSchedules.filter { it.endTime != null && it.endTime.isNotBlank() && it.endTime < now }
        expired.forEach { schedule ->
            mediaDao.getAllByScheduleId(schedule.scheduleId).forEach { media ->
                downloadManager.deleteFileByPath(media.localPath)
                mediaDao.delete(media)
            }
            scheduleDao.delete(schedule)
        }

        // 2) If API returned no current schedule, clear all (schedule removed from API)
        if (!response.isSuccess || response.result == null) {
            _tickersFromApi.value = emptyList()
            allSchedules.filter { it !in expired }.forEach { schedule ->
                mediaDao.getAllByScheduleId(schedule.scheduleId).forEach { media ->
                    downloadManager.deleteFileByPath(media.localPath)
                    mediaDao.delete(media)
                }
                scheduleDao.delete(schedule)
            }
            refreshTrigger.tryEmit(Unit)
            layoutRefreshCount.value += 1
            return null
        }

        val result = response.result!!
        val scheduleId = result.scheduleId ?: 0
        val layout = result.layout
        val tickersFromResponse = result.tickers ?: emptyList()

        if (layout == null) {
            _tickersFromApi.value = tickersFromResponse
            scheduleDao.getByScheduleId(scheduleId)?.let { scheduleDao.delete(it) }
            mediaDao.deleteByScheduleId(scheduleId)
            refreshTrigger.tryEmit(Unit)
            layoutRefreshCount.value += 1
            return null
        }

        val apiZoneIds = layout.zones.map { it.zoneId }.toSet()
        val current = scheduleDao.getByScheduleId(scheduleId)
        val currentLayout = current?.let { entityToLayout(it) }
        val currentZoneIds = currentLayout?.zones?.map { it.zoneId }?.toSet() ?: emptySet()

        // Only update when zoneIds or layoutId or lastUpdated changed – no API apply, no flicker
        if (current != null && currentZoneIds == apiZoneIds &&
            current.layoutId == result.layoutId && current.lastUpdated == result.lastUpdated) {
            return null
        }

        _tickersFromApi.value = tickersFromResponse
        val layoutJson = json.encodeToString(layout)

        // Remove zones no longer in API: delete their media rows and files
        val existingMedia = mediaDao.getAllByScheduleId(scheduleId)
        existingMedia.filter { it.zoneId !in apiZoneIds }.forEach { media ->
            downloadManager.deleteFileByPath(media.localPath)
            mediaDao.delete(media)
        }

        val entity = ScheduleEntity(
            scheduleId = scheduleId,
            layoutId = result.layoutId,
            layoutName = result.layoutName,
            startTime = result.startTime,
            endTime = result.endTime,
            priority = result.priority,
            lastUpdated = result.lastUpdated,
            layoutJson = layoutJson,
            tickersJson = "[]"
        )
        scheduleDao.insert(entity)
        refreshTrigger.tryEmit(Unit)
        layoutRefreshCount.value += 1

        // 3) Sync media: for each playlist item, ensure file exists (download if not); count new downloads
        // API often returns relative URLs (e.g. /uploads/video.mp4) – resolve with baseUrl for download
        val normalizedBaseUrl = baseUrl.trimEnd('/')
        var downloadedImages = 0
        var downloadedVideos = 0
        val currentFileNames = mutableSetOf<String>()
        layout.zones.forEach { zone ->
            zone.playlist.forEach { item ->
                if (item.url.isNotBlank()) {
                    val downloadUrl = resolveMediaUrl(normalizedBaseUrl, item.url)
                    val effectiveFileName = item.fileName.takeIf { it.isNotBlank() }
                        ?: deriveFileNameFromUrl(item.url, zone.zoneId, item.mediaId, item.type)
                    currentFileNames.add(effectiveFileName)
                    val existing = mediaDao.getByFileName(effectiveFileName)
                    val hadLocal = existing?.localPath != null && downloadManager.isFilePresent(existing.localPath)
                    val localPath = when {
                        hadLocal -> existing!!.localPath
                        else -> downloadManager.downloadIfNeeded(downloadUrl, effectiveFileName)
                    }
                    if (localPath != null && !hadLocal) {
                        if (item.type.equals("video", ignoreCase = true)) downloadedVideos++
                        else downloadedImages++
                    }
                    val mediaEntity = MediaFileEntity(
                        fileName = effectiveFileName,
                        localPath = localPath,
                        url = item.url,
                        scheduleId = scheduleId,
                        zoneId = zone.zoneId,
                        mediaId = item.mediaId,
                        type = item.type,
                        duration = item.duration,
                        fileSizeBytes = item.fileSizeBytes,
                        checksum = item.checksum
                    )
                    mediaDao.insert(mediaEntity)
                }
            }
        }

        // Delete media rows (and files) that are no longer in playlist
        mediaDao.getAllByScheduleId(scheduleId).forEach { media ->
            if (media.fileName !in currentFileNames) {
                downloadManager.deleteFileByPath(media.localPath)
                mediaDao.delete(media)
            }
        }

        return SyncResult(
            downloadedImages = downloadedImages,
            downloadedVideos = downloadedVideos,
            storagePath = storagePath
        )
    }
}
