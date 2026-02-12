package com.app.idisplaynew.ui.screens.home

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

private fun isPdf(fileName: String, url: String): Boolean =
    fileName.endsWith(".pdf", ignoreCase = true) || url.contains(".pdf", ignoreCase = true)

@Composable
fun ZoneDocumentView(
    documentUrl: String,
    fileName: String,
    durationSeconds: Int,
    modifier: Modifier = Modifier,
    onDurationReached: () -> Unit = {}
) {
    val isPdfDoc = isPdf(fileName, documentUrl)

    if (isPdfDoc && documentUrl.startsWith("file://")) {
        val path = documentUrl.removePrefix("file://")
        ZonePdfView(
            filePath = path,
            durationSeconds = durationSeconds,
            modifier = modifier,
            onDurationReached = onDurationReached
        )
    } else {
        ZoneDocumentWebView(
            documentUrl = documentUrl,
            durationSeconds = durationSeconds,
            modifier = modifier,
            onDurationReached = onDurationReached
        )
    }
}

@Composable
private fun ZonePdfView(
    filePath: String,
    durationSeconds: Int,
    modifier: Modifier,
    onDurationReached: () -> Unit
) {
    var pageIndex by remember { mutableIntStateOf(0) }
    var pageCount by remember { mutableStateOf(0) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(filePath) {
        pageCount = withContext(Dispatchers.IO) {
            runCatching {
                val file = File(filePath)
                if (!file.exists()) return@runCatching 0
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { it.pageCount }
                }
            }.getOrElse { 0 }
        }
    }

    LaunchedEffect(pageIndex, filePath, pageCount) {
        if (pageCount <= 0) return@LaunchedEffect
        val bitmap = withContext(Dispatchers.IO) {
            runCatching {
                val file = File(filePath)
                if (!file.exists()) return@runCatching null
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        if (pageIndex >= renderer.pageCount) return@runCatching null
                        val page = renderer.openPage(pageIndex)
                        val width = page.width * 2
                        val height = page.height * 2
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()
                        bitmap
                    }
                }
            }.getOrNull()
        }
        currentBitmap = bitmap
    }

    LaunchedEffect(pageCount, pageIndex) {
        if (pageCount <= 0) return@LaunchedEffect
        val perPageSeconds = maxOf(3, durationSeconds / pageCount)
        delay(perPageSeconds * 1000L)
        if (pageIndex + 1 >= pageCount) {
            onDurationReached()
        } else {
            pageIndex += 1
        }
    }

    currentBitmap?.let { bitmap ->
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun ZoneDocumentWebView(
    documentUrl: String,
    durationSeconds: Int,
    modifier: Modifier,
    onDurationReached: () -> Unit
) {
    val loadUrl = when {
        documentUrl.startsWith("http") ->
            "https://docs.google.com/gview?embedded=true&url=${Uri.encode(documentUrl)}"
        else -> documentUrl
    }

    LaunchedEffect(Unit) {
        delay(durationSeconds * 1000L)
        onDurationReached()
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                }
                webViewClient = WebViewClient()
                loadUrl(loadUrl)
            }
        }
    )
}
