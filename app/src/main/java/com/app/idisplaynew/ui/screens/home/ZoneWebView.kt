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
import java.util.regex.Pattern

/** Extracts YouTube video ID from watch, short, or embed URLs; returns null if not YouTube. */
private fun extractYouTubeVideoId(url: String?): String? {
    if (url.isNullOrBlank()) return null
    val u = url.trim()
    // youtu.be/VIDEO_ID
    val shortPattern = Pattern.compile("youtu\\.be/([a-zA-Z0-9_-]{11})(?:\\?|$|/)")
    val shortMatcher = shortPattern.matcher(u)
    if (shortMatcher.find()) return shortMatcher.group(1)
    // youtube.com/embed/VIDEO_ID or youtube.com/watch?v=VIDEO_ID
    val longPattern = Pattern.compile("(?:youtube\\.com/(?:embed/|watch\\?.*v=)|youtube-nocookie\\.com/embed/)([a-zA-Z0-9_-]{11})")
    val longMatcher = longPattern.matcher(u)
    return if (longMatcher.find()) longMatcher.group(1) else null
}

/** Builds a minimal-UI embed URL: no controls, no related videos, no branding, autoplay. */
private fun buildYouTubeEmbedUrl(videoId: String): String {
    val params = listOf(
        "autoplay=1",
        "controls=0",
        "disablekb=1",
        "fs=0",
        "iv_load_policy=3",
        "modestbranding=1",
        "rel=0",
        "showinfo=0",
        "playsinline=1",
        "loop=0",
        "mute=0"
    ).joinToString("&")
    return "https://www.youtube-nocookie.com/embed/$videoId?$params"
}

/** Fullscreen HTML that shows only the YouTube video (no title, channel, controls, etc.). */
private fun wrapYouTubeFullscreen(embedUrl: String): String {
    val viewport = "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\"/>"
    val style = """
        <style>
            * { margin: 0; padding: 0; box-sizing: border-box; }
            html, body { width: 100%; height: 100%; overflow: hidden; background: #000; }
            .yt-container {
                position: fixed;
                top: 0; left: 0; right: 0; bottom: 0;
                width: 100%; height: 100%;
                overflow: hidden;
            }
            .yt-container iframe {
                position: absolute;
                top: -10%;
                left: -5%;
                width: 110%;
                height: 120%;
                border: none;
                pointer-events: auto;
            }
        </style>
    """.trimIndent()
    val iframe = "<iframe src=\"$embedUrl\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture\" allowfullscreen></iframe>"
    return "<!DOCTYPE html><html><head>$viewport$style</head><body><div class=\"yt-container\">$iframe</div></body></html>"
}

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
                hasUrl -> {
                    val url = sourceUrl!!
                    val videoId = extractYouTubeVideoId(url)
                    if (videoId != null) {
                        val embedUrl = buildYouTubeEmbedUrl(videoId)
                        val fullscreenHtml = wrapYouTubeFullscreen(embedUrl)
                        webView.loadDataWithBaseURL(
                            "https://www.youtube-nocookie.com/",
                            fullscreenHtml,
                            "text/html",
                            "UTF-8",
                            null
                        )
                    } else {
                        webView.loadUrl(url)
                    }
                }
            }
        }
    )
    LaunchedEffect(htmlContent, sourceUrl, durationSeconds) {
        val durationMs = (durationSeconds.coerceAtLeast(1)) * 1000L
        delay(durationMs)
        onDurationReached()
    }
}
