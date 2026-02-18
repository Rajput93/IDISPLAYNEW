package com.app.idisplaynew.data.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.idisplaynew.data.remote.RemoteViewSseManager
import com.app.idisplaynew.data.remote.ScreenshotUploader
import com.app.idisplaynew.ui.utils.DataStoreManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Request for the UI to capture the screen (sessionId from backend).
 */
data class ScreenshotRequest(val sessionId: String)

/**
 * Manages SSE remote-view connection and screenshot upload.
 * - Connects on init when baseUrl and token are available
 * - Disconnects on onCleared
 * - Emits ScreenshotRequest when backend sends take_screenshot; UI captures then calls uploadScreenshot
 */
class ScreenshotViewModel(
    private val dataStoreManager: DataStoreManager,
    private val uploader: ScreenshotUploader
) : ViewModel() {

    private val _screenshotRequest = MutableSharedFlow<ScreenshotRequest>(replay = 0, extraBufferCapacity = 1)
    val screenshotRequest: SharedFlow<ScreenshotRequest> = _screenshotRequest.asSharedFlow()

    private val _sseConnected = MutableStateFlow(false)
    val sseConnected: StateFlow<Boolean> = _sseConnected.asStateFlow()

    private val _uploadError = MutableStateFlow<String?>(null)
    val uploadError: StateFlow<String?> = _uploadError.asStateFlow()

    /** One-shot message for UI: "Screenshot sent to CMS" / "Capture failed" / "Upload failed: ..." */
    private val _screenshotFeedback = MutableStateFlow<String?>(null)
    val screenshotFeedback: StateFlow<String?> = _screenshotFeedback.asStateFlow()

    private var sseManager: RemoteViewSseManager? = null
    private var baseUrl: String = ""
    private var token: String = ""

    init {
        viewModelScope.launch {
            val base = dataStoreManager.baseUrl.first()
            val t = dataStoreManager.authToken.first()
            if (!base.isNullOrBlank() && !t.isNullOrBlank()) {
                baseUrl = base
                token = t
                sseManager = RemoteViewSseManager(
                    scope = viewModelScope,
                    baseUrl = baseUrl,
                    accessToken = token,
                    onTakeScreenshot = { sessionId ->
                        viewModelScope.launch {
                            _screenshotRequest.emit(ScreenshotRequest(sessionId))
                        }
                    },
                    onConnectionStateChanged = { connected ->
                        _sseConnected.value = connected
                    }
                )
                sseManager?.connect()
            }
        }
    }

    /**
     * Call this after the UI has captured the screen and converted to JPEG.
     * Runs upload on IO; sets screenshotFeedback for UI toast.
     */
    fun uploadScreenshot(jpegBytes: ByteArray, commandId: String? = null) {
        viewModelScope.launch {
            _uploadError.value = null
            _screenshotFeedback.value = null
            uploader.uploadFromBytes(baseUrl, token, jpegBytes, commandId)
                .onSuccess { _screenshotFeedback.value = "Screenshot sent to CMS" }
                .onFailure {
                    _uploadError.value = it.message
                    _screenshotFeedback.value = "Upload failed: ${it.message}"
                }
        }
    }

    /** Call when capture failed (bitmap null) so user sees feedback on device. */
    fun onScreenshotCaptureFailed() {
        _screenshotFeedback.value = "Screenshot capture failed"
    }

    fun clearUploadError() {
        _uploadError.value = null
    }

    fun clearScreenshotFeedback() {
        _screenshotFeedback.value = null
    }

    override fun onCleared() {
        sseManager?.disconnect()
        sseManager = null
        super.onCleared()
    }
}
