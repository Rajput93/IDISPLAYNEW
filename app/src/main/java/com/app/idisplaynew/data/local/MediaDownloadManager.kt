package com.app.idisplaynew.data.local

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRedirect
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Downloads media (image/video) from URL and saves to app local storage.
 * Uses [fileName] so the same file is not re-downloaded (caller checks DB first).
 */
class MediaDownloadManager(private val context: Context) {

    private val httpClient = HttpClient(CIO) {
        install(HttpRedirect)
    }

    private val mediaDir: File
        get() = File(context.filesDir, MEDIA_DIR_NAME).also { if (!it.exists()) it.mkdirs() }

    suspend fun downloadIfNeeded(url: String, fileName: String): String? = withContext(Dispatchers.IO) {
        if (url.isBlank() || fileName.isBlank()) return@withContext null
        val safeFileName = fileName.takeIf { it.isNotBlank() } ?: url.substringAfterLast('/').ifBlank { "media_${System.currentTimeMillis()}" }
        val file = File(mediaDir, safeFileName)
        if (file.exists()) return@withContext file.absolutePath
        try {
            val bytes: ByteArray = httpClient.get(url).body()
            file.writeBytes(bytes)
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /** Delete a file by its local path (e.g. when schedule/item is removed). */
    fun deleteFileByPath(localPath: String?) {
        if (localPath.isNullOrBlank()) return
        try {
            File(localPath).takeIf { it.exists() }?.delete()
        } catch (_: Exception) { }
    }

    /** Delete file by fileName within app media dir. */
    fun deleteFileByFileName(fileName: String) {
        if (fileName.isBlank()) return
        try {
            File(mediaDir, fileName).takeIf { it.exists() }?.delete()
        } catch (_: Exception) { }
    }

    fun isFilePresent(localPath: String?): Boolean {
        if (localPath.isNullOrBlank()) return false
        return File(localPath).exists()
    }

    /** Full path where downloaded images/videos are stored (e.g. for display in settings/toast). */
    fun getMediaStoragePath(): String = mediaDir.absolutePath

    companion object {
        const val MEDIA_DIR_NAME = "schedule_media"
    }
}
