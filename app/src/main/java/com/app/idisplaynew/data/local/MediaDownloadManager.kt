package com.app.idisplaynew.data.local

import android.content.Context
import android.os.Environment
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpRedirect
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Downloads media (image/video) from URL and saves to app storage.
 * Uses external files dir so the folder is visible on device (Android/data/<pkg>/files/schedule_media).
 * Falls back to internal filesDir if external is not available.
 * Uses [fileName] so the same file is not re-downloaded (caller checks DB first).
 */
class MediaDownloadManager(private val context: Context) {

    private val httpClient = HttpClient(OkHttp) {
        install(HttpRedirect)
        install(HttpTimeout) {
            requestTimeoutMillis = 5 * 60_000L   // 5 min for large video
            connectTimeoutMillis = 30_000L
            socketTimeoutMillis = 5 * 60_000L
        }
    }

    private val mediaDir: File
        get() {
            val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
                ?: context.getExternalFilesDir(null)
                ?: context.filesDir
            return File(baseDir, MEDIA_DIR_NAME).also { if (!it.exists()) it.mkdirs() }
        }

    /** Returns existing file path if a file with this [fileName] exists in media dir; null otherwise. */
    fun getExistingFilePath(fileName: String): String? {
        if (fileName.isBlank()) return null
        val file = File(mediaDir, fileName)
        return file.absolutePath.takeIf { file.exists() }
    }

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

    /** Legacy internal path (used to detect and migrate old downloads). */
    fun getLegacyInternalMediaPath(): String = File(context.filesDir, MEDIA_DIR_NAME).absolutePath

    /**
     * Copy a file from legacy internal path to current (external) media dir.
     * @return new absolute path, or null if copy failed or file already at current location.
     */
    suspend fun copyToCurrentStorage(fromPath: String, fileName: String): String? = withContext(Dispatchers.IO) {
        if (fileName.isBlank()) return@withContext null
        val src = File(fromPath)
        if (!src.exists()) return@withContext null
        val currentDir = mediaDir
        if (fromPath.startsWith(currentDir.absolutePath)) return@withContext fromPath
        val dest = File(currentDir, fileName)
        try {
            src.copyTo(dest, overwrite = true)
            dest.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        const val MEDIA_DIR_NAME = "schedule_media"
    }
}
