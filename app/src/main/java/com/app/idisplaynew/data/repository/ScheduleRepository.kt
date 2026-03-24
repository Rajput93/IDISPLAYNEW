package com.app.idisplaynew.data.repository

import com.app.idisplaynew.data.local.AppDatabase
import com.app.idisplaynew.data.local.MediaDownloadManager
import com.app.idisplaynew.data.local.entity.MediaFileEntity
import com.app.idisplaynew.data.local.entity.ScheduleEntity
import com.app.idisplaynew.data.model.AckCommandPayload
import com.app.idisplaynew.data.model.DeviceCommandsResponse
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
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

    /** Call after syncFromApi() returns so layout flow re-reads DB and UI shows newly downloaded media. */
    fun notifyLayoutRefresh() {
        refreshTrigger.tryEmit(Unit)
        layoutRefreshCount.value += 1
    }

    /**
     * One-shot read: current layout + tickers from DB. Use after sync so UI updates even when
     * flow doesn't re-emit (e.g. single image → multiple).
     */
    suspend fun getCurrentLayoutAndTickersSnapshot(): Pair<ScheduleCurrentResponse.ScheduleResult.Layout?, List<ScheduleCurrentResponse.ScheduleResult.Ticker>> =
        withContext(Dispatchers.IO) {
            val now = nowIso()
            val entity = scheduleDao.getCurrentScheduleOnce(now)
            val layout: ScheduleCurrentResponse.ScheduleResult.Layout? = when {
                entity == null -> null
                else -> {
                    val l = entityToLayout(entity) ?: null
                    if (l == null) null else {
                        val mediaList = mediaDao.getAllByScheduleId(entity.scheduleId)
                        mergeLayoutWithMedia(l, mediaList)
                    }
                }
            }
            layout to _tickersFromApi.value
        }

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
                if (entity == null) return@flatMapLatest flowOf(null)
                val layout = entityToLayout(entity)
                if (layout == null) return@flatMapLatest flowOf(null)
                // combine so Room's media_file invalidation (on insert) triggers re-emit and UI updates
                combine(
                    flowOf(layout),
                    mediaDao.getAllByScheduleIdFlow(entity.scheduleId)
                ) { layoutOnly, mediaList -> mergeLayoutWithMedia(layoutOnly, mediaList) }
            }
            .flowOn(Dispatchers.IO)

    /**
     * Same naming as [syncFromApi] so DB rows, disk files, and layout JSON always align.
     * Prefer [originalFileName] when [fileName] is blank (common for UUID files on server).
     */
    private fun effectiveMediaFileName(
        item: ScheduleCurrentResponse.ScheduleResult.Layout.Zone.PlaylistItem,
        zoneId: Int,
        mediaId: Int
    ): String {
        val fromApi = (item.fileName ?: "").trim()
        if (fromApi.isNotBlank()) return fromApi
        val fromOriginal = (item.originalFileName ?: "").trim()
        if (fromOriginal.isNotBlank()) return fromOriginal
        return deriveFileNameFromUrl(item.url ?: "", zoneId, mediaId, item.type ?: "video")
    }

    /**
     * Resolves local file path for a playlist item without using [associate], which drops rows when
     * multiple [MediaFileEntity] share the same (zoneId, mediaId) — a case that breaks the last item.
     */
    private fun resolveLocalPathForPlaylistItem(
        zoneId: Int,
        item: ScheduleCurrentResponse.ScheduleResult.Layout.Zone.PlaylistItem,
        mediaList: List<MediaFileEntity>
    ): String? {
        val mediaId = item.mediaId ?: 0
        val effectiveName = effectiveMediaFileName(item, zoneId, mediaId)
        val origAlt = (item.originalFileName ?: "").trim().takeIf { it.isNotBlank() && !it.equals(effectiveName, ignoreCase = true) }

        fun File.lengthIfPresent(): Long? = takeIf { exists() && isFile }?.length()

        val presentRows = mediaList.filter {
            it.localPath != null && downloadManager.isFilePresent(it.localPath!!)
        }

        // 1) zone + mediaId + exact fileName (disambiguates duplicate zone+media rows)
        presentRows.firstOrNull {
            it.zoneId == zoneId && it.mediaId == mediaId &&
                it.fileName.equals(effectiveName, ignoreCase = true)
        }?.localPath?.let { return it }

        // 2) zone + mediaId (any file — legacy / single row per slot)
        presentRows.firstOrNull { it.zoneId == zoneId && it.mediaId == mediaId }?.localPath?.let { return it }

        // 3) Match alternate originalFileName stored in DB
        if (origAlt != null) {
            presentRows.firstOrNull {
                it.zoneId == zoneId && it.mediaId == mediaId &&
                    it.fileName.equals(origAlt, ignoreCase = true)
            }?.localPath?.let { return it }
        }

        // 4) fileName alone (unique index in DB)
        presentRows.firstOrNull { it.fileName.equals(effectiveName, ignoreCase = true) }?.localPath?.let { return it }

        // 5) On-disk lookup (exact + case-insensitive)
        downloadManager.getExistingFilePath(effectiveName)?.let { return it }
        if (origAlt != null) downloadManager.getExistingFilePath(origAlt)?.let { return it }

        // 5b) URL basename (e.g. 3e9c7a8b-....mp4) — API often sets a long display `fileName` while
        // the same asset was saved earlier under the URL’s file name only.
        item.url?.let { u ->
            fileNameFromMediaUrl(u)?.let { urlBase ->
                presentRows.firstOrNull { it.fileName.equals(urlBase, ignoreCase = true) }?.localPath?.let { return it }
                downloadManager.getExistingFilePath(urlBase)?.let { return it }
            }
        }

        // 6) Match by declared file size (helps when names differ between API and disk)
        val expectedSize = item.fileSizeBytes ?: 0L
        if (expectedSize > 0L) {
            presentRows.firstOrNull { row ->
                row.zoneId == zoneId && row.mediaId == mediaId &&
                    row.localPath?.let { File(it).lengthIfPresent() } == expectedSize
            }?.localPath?.let { return it }
            presentRows.firstOrNull { row ->
                row.zoneId == zoneId &&
                    row.localPath?.let { File(it).lengthIfPresent() } == expectedSize
            }?.localPath?.let { return it }
        }

        return null
    }

    private fun mergeLayoutWithMedia(
        layout: ScheduleCurrentResponse.ScheduleResult.Layout,
        mediaList: List<MediaFileEntity>
    ): ScheduleCurrentResponse.ScheduleResult.Layout {
        val zones = layout.zones ?: emptyList()
        val zonesWithLocalUrls = zones.map { zone ->
            val zoneId = zone.zoneId ?: 0
            val playlist = zone.playlist ?: emptyList()
            zone.copy(playlist = playlist.map { item ->
                val type = item.type ?: ""
                val fileName = item.fileName ?: ""
                val url = item.url ?: ""
                val pathOnDisk = resolveLocalPathForPlaylistItem(zoneId, item, mediaList)
                val isVideoOrImage = type.equals("video", ignoreCase = true) || type.equals("image", ignoreCase = true)
                val isDocument = type.equals("document", ignoreCase = true)
                val isPdf = fileName.endsWith(".pdf", true) || url.contains(".pdf", true)
                val displayUrl = when {
                    isDocument -> when {
                        pathOnDisk != null && isPdf -> "file://$pathOnDisk"
                        else -> url
                    }
                    isVideoOrImage -> when {
                        pathOnDisk != null -> "file://$pathOnDisk"
                        else -> ""
                    }
                    else -> when {
                        pathOnDisk != null -> "file://$pathOnDisk"
                        else -> url
                    }
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

    /** Last path segment of a media URL (e.g. `3e9c7a8b-....mp4`), used when API `fileName` ≠ name on disk. */
    private fun fileNameFromMediaUrl(url: String): String? {
        val part = url.trim().substringAfterLast('/', "").substringBefore('?').trim()
        return part.takeIf { it.isNotBlank() && it.contains('.') }
    }

    /** Derive a unique fileName when API sends blank (so we can still download and look up by zoneId+mediaId). */
    private fun deriveFileNameFromUrl(url: String, zoneId: Int, mediaId: Int, type: String): String {
        if (url.isBlank()) return "media_${zoneId}_${mediaId}.bin"
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
     * Fetches device commands. Only when commandType == "refresh" we hit ack API;
     * after ack response is received, we hit player/schedule/current.
     */
    suspend fun fetchAndProcessCommands(): Unit = withContext(Dispatchers.IO) {
        val baseUrl = dataStore.baseUrl.first() ?: return@withContext
        val token = dataStore.authToken.first() ?: return@withContext
        val response = try {
            api.getDeviceCommands(baseUrl, token)
        } catch (_: Exception) {
            return@withContext
        }
        val refreshCommands = response.result?.commands
            ?.filter { it.commandType.equals("refresh", ignoreCase = true) }
            ?: emptyList()
        if (refreshCommands.isEmpty()) return@withContext
        for (command in refreshCommands) {
            try {
                api.ackCommand(baseUrl, token, AckCommandPayload(commandId = command.commandId, status = "success", result = ""))
            }
            catch (_: Exception) { }
        }
        syncFromApi()
    }

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
            if (_tickersFromApi.value.isNotEmpty()) _tickersFromApi.value = emptyList()
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
            if (_tickersFromApi.value != tickersFromResponse) _tickersFromApi.value = tickersFromResponse
            // API returned no layout: clear all schedule data so only API data is shown (default 4 zones)
            scheduleDao.getAll().forEach { schedule ->
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

        // 2b) API says "current" is this scheduleId (e.g. default layout). Remove any other schedule
        // so that when user moved a schedule to past date, we don't keep showing the old one.
        scheduleDao.getAll().filter { it.scheduleId != scheduleId }.forEach { schedule ->
            mediaDao.getAllByScheduleId(schedule.scheduleId).forEach { media ->
                downloadManager.deleteFileByPath(media.localPath)
                mediaDao.delete(media)
            }
            scheduleDao.delete(schedule)
        }

        val layoutZones = layout.zones ?: emptyList()
        val apiZoneIds = layoutZones.map { it.zoneId ?: 0 }.toSet()
        val current = scheduleDao.getByScheduleId(scheduleId)
        val currentLayout = current?.let { entityToLayout(it) }
        val currentZoneIds = currentLayout?.zones?.map { it.zoneId ?: 0 }?.toSet() ?: emptySet()

        // File names from API layout (used for skip check and later for delete logic)
        val apiLayoutFileNames = layoutZones.flatMap { zone ->
            val zoneId = zone.zoneId ?: 0
            (zone.playlist ?: emptyList()).filter { (it.url ?: "").isNotBlank() }.map { item ->
                (item.fileName ?: "").takeIf { it.isNotBlank() }
                    ?: deriveFileNameFromUrl(item.url ?: "", zoneId, item.mediaId ?: 0, item.type ?: "video")
            }
        }.toSet()
        val currentDbFileNames = mediaDao.getAllByScheduleId(scheduleId).map { it.fileName }.toSet()

        // Skip only when zones, layoutId, lastUpdated AND playlist (file names) are unchanged.
        // Otherwise adding only new images (same layoutId/lastUpdated) would never sync.
        if (current != null && currentZoneIds == apiZoneIds &&
            current.layoutId == result.layoutId && current.lastUpdated == result.lastUpdated &&
            apiLayoutFileNames == currentDbFileNames) {
            return null
        }

        if (_tickersFromApi.value != tickersFromResponse) _tickersFromApi.value = tickersFromResponse
        val layoutJson = json.encodeToString(layout)
        val currentLayoutFileNames = apiLayoutFileNames

        // Remove zones no longer in API: delete media rows; delete file only if fileName not used elsewhere
        val existingMedia = mediaDao.getAllByScheduleId(scheduleId)
        existingMedia.filter { it.zoneId !in apiZoneIds }.forEach { media ->
            if (media.fileName !in currentLayoutFileNames) {
                downloadManager.deleteFileByPath(media.localPath)
            }
            mediaDao.delete(media)
        }

        val entity = ScheduleEntity(
            scheduleId = scheduleId,
            layoutId = result.layoutId,
            layoutName = result.layoutName,
            startTime = result.startTime,
            endTime = result.endTime,
            priority = result.priority ?: 0,
            lastUpdated = result.lastUpdated,
            layoutJson = layoutJson,
            tickersJson = "[]"
        )
        scheduleDao.insert(entity)
        // Do not refresh here – wait until after all media is downloaded and inserted (see notifyLayoutRefresh after sync)

        // 3) Sync media: for each playlist item, ensure file exists (download if not); count new downloads
        // API often returns relative URLs (e.g. /uploads/video.mp4) – resolve with baseUrl for download
        val normalizedBaseUrl = baseUrl.trimEnd('/')
        var downloadedImages = 0
        var downloadedVideos = 0
        val currentFileNames = mutableSetOf<String>()
        layoutZones.forEach { zone ->
            val zoneId = zone.zoneId ?: 0
            (zone.playlist ?: emptyList()).forEach { item ->
                val itemType = (item.type ?: "").lowercase()
                val isDownloadableType = itemType == "video" || itemType == "image" || itemType == "document"
                val itemUrl = item.url ?: ""
                if (itemUrl.isNotBlank() && isDownloadableType) {
                    val downloadUrl = resolveMediaUrl(normalizedBaseUrl, itemUrl)
                    val effectiveFileName = effectiveMediaFileName(item, zoneId, item.mediaId ?: 0)
                    currentFileNames.add(effectiveFileName)
                    val existing = mediaDao.getByFileName(effectiveFileName)
                    val hadLocalInDb = existing?.localPath != null && downloadManager.isFilePresent(existing.localPath)
                    // Prefer API fileName on disk; else same bytes saved under URL basename (e.g. 3e9c7a8b-....mp4)
                    val existingPathByFileName = downloadManager.getExistingFilePath(effectiveFileName)
                        ?: run {
                            val urlBase = fileNameFromMediaUrl(itemUrl) ?: return@run null
                            val byUrl = downloadManager.getExistingFilePath(urlBase) ?: return@run null
                            val expected = item.fileSizeBytes ?: 0L
                            val len = File(byUrl).length()
                            if (expected <= 0L || len == expected) byUrl else null
                        }
                    val localPath = when {
                        hadLocalInDb -> existing!!.localPath
                        existingPathByFileName != null -> existingPathByFileName
                        else -> downloadManager.downloadIfNeeded(downloadUrl, effectiveFileName)
                    }
                    if (localPath != null && !hadLocalInDb && existingPathByFileName == null) {
                        if ((item.type ?: "").equals("video", ignoreCase = true)) downloadedVideos++
                        else downloadedImages++
                    }
                    val mediaEntity = MediaFileEntity(
                        fileName = effectiveFileName,
                        localPath = localPath,
                        url = itemUrl,
                        scheduleId = scheduleId,
                        zoneId = zoneId,
                        mediaId = item.mediaId ?: 0,
                        type = item.type ?: "video",
                        duration = item.duration ?: 0,
                        fileSizeBytes = item.fileSizeBytes ?: 0L,
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

        // Caller (ViewModel) must call notifyLayoutRefresh() after syncFromApi() returns so UI updates

        return SyncResult(
            downloadedImages = downloadedImages,
            downloadedVideos = downloadedVideos,
            storagePath = storagePath
        )
    }
}
