package com.app.idisplaynew.data.remote

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages the SSE connection to the remote-view stream.
 * - Connects with device access token
 * - Handles "connected" and "take_screenshot" events
 * - Auto-reconnects after 5 seconds on failure/close
 * - Thread-safe connect/disconnect
 */
class RemoteViewSseManager(
    private val scope: CoroutineScope,
    private val baseUrl: String,
    private val accessToken: String,
    private val onTakeScreenshot: (sessionId: String) -> Unit,
    private val onConnectionStateChanged: ((connected: Boolean) -> Unit)? = null
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS) // no timeout for SSE stream
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val factory = EventSources.createFactory(client)
    private var eventSource: EventSource? = null
    private var connectJob: Job? = null
    private val closed = AtomicBoolean(false)
    private val retryDelayMs = 5_000L

    fun connect() {
        if (closed.get()) return
        connectJob?.cancel()
        connectJob = scope.launch {
            if (!closed.get()) {
                runCatching { doConnect() }.onFailure {
                    if (!closed.get()) scheduleReconnect()
                }
            }
        }
    }

    private fun doConnect() {
        val base = baseUrl.trimEnd('/')
        val url = "$base/${ApiEndpoints.REMOTE_VIEW_STREAM}?access_token=${java.net.URLEncoder.encode(accessToken, "UTF-8")}"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        eventSource = factory.newEventSource(request, object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: okhttp3.Response) {
                onConnectionStateChanged?.invoke(true)
            }

            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                when (type) {
                    "connected" -> { /* connection acknowledged */ }
                    "take_screenshot" -> {
                        runCatching {
                            val json = JSONObject(data)
                            val sessionId = json.optString("sessionId", "").ifBlank { "unknown" }
                            onTakeScreenshot(sessionId)
                        }
                    }
                }
            }

            override fun onClosed(eventSource: EventSource) {
                this@RemoteViewSseManager.eventSource = null
                onConnectionStateChanged?.invoke(false)
                if (!closed.get()) scheduleReconnect()
            }

            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: okhttp3.Response?
            ) {
                this@RemoteViewSseManager.eventSource = null
                onConnectionStateChanged?.invoke(false)
                if (!closed.get()) scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        if (closed.get()) return
        connectJob?.cancel()
        connectJob = scope.launch {
            delay(retryDelayMs)
            if (!closed.get()) connect()
        }
    }

    fun disconnect() {
        closed.set(true)
        connectJob?.cancel()
        connectJob = null
        eventSource?.cancel()
        eventSource = null
        onConnectionStateChanged?.invoke(false)
    }
}
