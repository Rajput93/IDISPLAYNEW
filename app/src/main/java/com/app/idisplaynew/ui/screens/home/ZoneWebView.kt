package com.app.idisplaynew.ui.screens.home

import android.annotation.SuppressLint
import android.graphics.Color
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

private fun wrapHtmlForZone(htmlContent: String): String {
    val viewport = "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\"/>"
    val fullLayoutStyle = """
        <style>
            html, body { margin: 0; padding: 0; width: 100%; height: 100%; }
            #zone-root { position: fixed; top: 0; left: 0; right: 0; bottom: 0; width: 100%; height: 100%; margin: 0; padding: 0; }
            #zone-root > * { position: fixed !important; top: 0 !important; left: 0 !important; right: 0 !important; bottom: 0 !important; width: 100% !important; height: 100% !important; margin: 0 !important; box-sizing: border-box !important; display: block !important; overflow: auto !important; }
        </style>
    """.trimIndent()
    val bodyStyle = "margin:0;padding:0;width:100%;height:100%;"
    val wrappedContent = "<div id=\"zone-root\">${htmlContent.trim()}</div>"
    return "<!DOCTYPE html><html style=\"height:100%;margin:0\"><head>$viewport$fullLayoutStyle</head><body style=\"$bodyStyle\">$wrappedContent</body></html>"
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ZoneWebView(
    htmlContent: String?,
    sourceUrl: String?,
    durationSeconds: Int,
    modifier: Modifier = Modifier,
    onDurationReached: () -> Unit = {}
) {
    AndroidView(
        modifier = modifier.fillMaxSize().clip(RectangleShape),
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(Color.TRANSPARENT)
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    useWideViewPort = true
                    loadWithOverviewMode = true
                }
                webViewClient = WebViewClient()
            }
        },
        update = { webView ->
            val hasHtml = !htmlContent.isNullOrBlank()
            val hasUrl = !sourceUrl.isNullOrBlank()
            when {
                hasHtml -> {
                    val wrappedHtml = wrapHtmlForZone(htmlContent!!)
                    webView.loadDataWithBaseURL(
                        null,
                        wrappedHtml,
                        "text/html",
                        "UTF-8",
                        null
                    )
                }
                hasUrl -> webView.loadUrl(sourceUrl!!)
            }
        }
    )
    LaunchedEffect(htmlContent, sourceUrl, durationSeconds) {
        val durationMs = (durationSeconds.coerceAtLeast(1)) * 1000L
        delay(durationMs)
        onDurationReached()
    }
}
