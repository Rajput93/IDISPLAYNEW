package com.app.idisplaynew.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Uploads a screenshot image to the backend as multipart/form-data.
 * POST player/device/screenshots
 * Headers: Authorization: Bearer {token}
 * Form: File, CommandId (null), CapturedAt (UTC timestamp)
 */
class ScreenshotUploader(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
) {
    suspend fun upload(
        baseUrl: String,
        bearerToken: String,
        imageFile: File,
        commandId: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val base = baseUrl.trimEnd('/')
        val url = "$base/${ApiEndpoints.SCREENSHOTS}"
        val capturedAtUtc = Instant.now().toString()

        val fileBody = imageFile.asRequestBody("image/jpeg".toMediaType())
        val filePart = MultipartBody.Part.createFormData("File", imageFile.name, fileBody)
        val commandIdPart = MultipartBody.Part.createFormData(
            "CommandId",
             "0"
        )
        val capturedAtPart = MultipartBody.Part.createFormData(
            "CapturedAt",
            capturedAtUtc
        )

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addPart(filePart)
            .addPart(commandIdPart)
            .addPart(capturedAtPart)
            .build()

        val request = Request.Builder()
            .url(url)
            .post(body)
            .header("Authorization", "Bearer $bearerToken")
            .build()

        runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("Screenshot upload failed: ${response.code} ${response.message}")
                }
            }
        }
    }

    suspend fun uploadFromBytes(
        baseUrl: String,
        bearerToken: String,
        jpegBytes: ByteArray,
        commandId: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val tempFile = File.createTempFile("screenshot", ".jpg")
        try {
            tempFile.writeBytes(jpegBytes)
            upload(baseUrl, bearerToken, tempFile, commandId)
        } finally {
            tempFile.delete()
        }
    }
}
