package com.app.idisplaynew.ui.screens.home

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.app.idisplaynew.R
import com.app.idisplaynew.data.viewmodel.HomeViewModel
import com.app.idisplaynew.data.viewmodel.ScreenshotViewModel
import com.app.idisplaynew.ui.theme.DisplayHubCardBackground
import com.app.idisplaynew.ui.utils.bitmapToJpegBytes
import com.app.idisplaynew.ui.utils.captureScreenBitmap
import com.app.idisplaynew.ui.utils.captureScreenBitmapAfterPost
import com.app.idisplaynew.ui.utils.waitForNextFrame
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun HomeScreen(viewModel: HomeViewModel, screenshotViewModel: ScreenshotViewModel) {
    val context = LocalContext.current
    val activity = context as? Activity
    val layout by viewModel.layout.collectAsState()
    val tickers by viewModel.tickers.collectAsState()
    val apiMessage by viewModel.apiMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val mediaStoragePath by viewModel.mediaStoragePath.collectAsState()
    val screenshotFeedback by screenshotViewModel.screenshotFeedback.collectAsState()

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show()
            viewModel.clearToast()
        }
    }

    LaunchedEffect(screenshotFeedback) {
        if (screenshotFeedback != null) {
            Toast.makeText(context, screenshotFeedback, Toast.LENGTH_SHORT).show()
            screenshotViewModel.clearScreenshotFeedback()
        }
    }

    LaunchedEffect(layout) {
        activity?.requestedOrientation = when (layout?.orientation?.equals("portrait", ignoreCase = true)) {
            true -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            else -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    // Remote View: on take_screenshot from SSE, capture screen and upload
    LaunchedEffect(screenshotViewModel, activity) {
        if (activity == null) return@LaunchedEffect
        screenshotViewModel.screenshotRequest.collectLatest { request ->
            waitForNextFrame()
            delay(150)
            var bitmap = captureScreenBitmap(activity)
            var attempt = 0
            while (bitmap == null && attempt < 4) {
                delay(150L * (attempt + 1))
                bitmap = if (attempt % 2 == 0) {
                    captureScreenBitmapAfterPost(activity)
                } else {
                    captureScreenBitmap(activity)
                }
                attempt++
            }
            val jpegBytes = bitmapToJpegBytes(bitmap)
            if (jpegBytes != null) {
                screenshotViewModel.uploadScreenshot(jpegBytes, request.sessionId)
            } else {
                screenshotViewModel.onScreenshotCaptureFailed()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (layout != null) {
            val currentLayout = layout!!
            val safeScreenHeight = (currentLayout.screenHeight ?: 1080).coerceAtLeast(1)
            val safeScreenWidth = (currentLayout.screenWidth ?: 1920).coerceAtLeast(1)
            val safeZones = currentLayout.zones ?: emptyList()
            val topTickers = tickers.filter { (it.position ?: "bottom").equals("top", ignoreCase = true) }.sortedBy { it.priority ?: 0 }
            val bottomTickers = tickers.filter { (it.position ?: "bottom").equals("bottom", ignoreCase = true) }.sortedBy { it.priority ?: 0 }

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val density = LocalDensity.current.density
                val screenHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
                val tickerScaleY = screenHeightPx / safeScreenHeight

                Column(modifier = Modifier.fillMaxSize()) {
                    if (topTickers.isNotEmpty()) {
                        topTickers.forEach { ticker ->
                            val tickerHeight = (ticker.height ?: 50).coerceAtLeast(1)
                            val heightDp = (tickerHeight * tickerScaleY / density).dp
                            TickerStrip(
                                ticker = ticker,
                                heightDp = heightDp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                    val zoneAreaWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
                    val zoneAreaHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
                    val scaleX = zoneAreaWidthPx / safeScreenWidth
                    val scaleY = zoneAreaHeightPx / safeScreenHeight

                    val zonesSorted = safeZones.sortedBy { it.zIndex ?: 0 }
                    zonesSorted.forEach { zone ->
                        val zoneId = zone.zoneId ?: 0
                        key(zoneId) {
                            val leftDp = ((zone.x ?: 0) * scaleX / density).dp
                            val topDp = ((zone.y ?: 0) * scaleY / density).dp
                            val widthDp = ((zone.width ?: 0) * scaleX / density).dp
                            val heightDp = ((zone.height ?: 0) * scaleY / density).dp

                            Box(
                                modifier = Modifier
                                    .offset(leftDp, topDp)
                                    .size(widthDp, heightDp)
                                    .clip(RectangleShape)
                                    .graphicsLayer { clip = true }
                                    .background(parseHexColor(zone.backgroundColor ?: "#000000"))
                                    .border(2.dp, Color.White)
                            ) {
                                ZonePlaylistContent(
                                    zoneId = zoneId,
                                    playlist = zone.playlist ?: emptyList(),
                                    modifier = Modifier.fillMaxSize(),
                                    onDisplayedMediaChanged = { z, m -> viewModel.setCurrentDisplayedMediaId(z, m) }
                                )
                            }
                        }
                    }
                }

                    if (bottomTickers.isNotEmpty()) {
                        bottomTickers.forEach { ticker ->
                            val tickerHeight = (ticker.height ?: 50).coerceAtLeast(1)
                            val heightDp = (tickerHeight * tickerScaleY / density).dp
                            TickerStrip(
                                ticker = ticker,
                                heightDp = heightDp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

        } else {
            // No layout / API not run: default 4-zone screen with app logo in each zone
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val density = LocalDensity.current.density
                val scaleY = with(LocalDensity.current) { maxHeight.toPx() } / 1080f
                val topTickers = tickers.filter { (it.position ?: "").equals("top", ignoreCase = true) }.sortedBy { it.priority ?: 0 }
                val bottomTickers = tickers.filter { (it.position ?: "").equals("bottom", ignoreCase = true) }.sortedBy { it.priority ?: 0 }

                Column(modifier = Modifier.fillMaxSize()) {
                    if (topTickers.isNotEmpty()) {
                        topTickers.forEach { ticker ->
                            val heightDp = ((ticker.height ?: 50) * scaleY / density).dp
                            TickerStrip(
                                ticker = ticker,
                                heightDp = heightDp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Default 4 zones (2x2) with app logo
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            DefaultZoneWithLogo(modifier = Modifier.weight(1f).fillMaxHeight())
                            DefaultZoneWithLogo(modifier = Modifier.weight(1f).fillMaxHeight())
                        }
                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            DefaultZoneWithLogo(modifier = Modifier.weight(1f).fillMaxHeight())
                            DefaultZoneWithLogo(modifier = Modifier.weight(1f).fillMaxHeight())
                        }
                    }

                    if (bottomTickers.isNotEmpty()) {
                        bottomTickers.forEach { ticker ->
                            val heightDp = ((ticker.height ?: 50) * scaleY / density).dp
                            TickerStrip(
                                ticker = ticker,
                                heightDp = heightDp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

       /* // Debug: test button to trigger a crash and verify Firebase Crashlytics
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Button(
                onClick = { throw RuntimeException("Test crash for Firebase Crashlytics") }
            ) {
                Text("Test Crash")
            }
        }*/
    }
}

@Composable
private fun DefaultZoneWithLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(4.dp)
            .clip(RectangleShape)
            .background(DisplayHubCardBackground)
            .border(2.dp, Color.White),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher),
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            contentScale = ContentScale.Fit
        )
    }
}

private fun parseHexColor(hex: String): Color {
    val clean = hex.removePrefix("#")
    return when (clean.length) {
        6 -> {
            val rgb = clean.toLong(16)
            Color(
                red = ((rgb shr 16) and 0xFF) / 255f,
                green = ((rgb shr 8) and 0xFF) / 255f,
                blue = (rgb and 0xFF) / 255f,
                alpha = 1f
            )
        }
        8 -> {
            val argb = clean.toLong(16)
            Color(
                red = ((argb shr 16) and 0xFF) / 255f,
                green = ((argb shr 8) and 0xFF) / 255f,
                blue = (argb and 0xFF) / 255f,
                alpha = ((argb shr 24) and 0xFF) / 255f
            )
        }
        else -> Color.Black
    }
}
