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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.app.idisplaynew.data.viewmodel.HomeViewModel

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val context = LocalContext.current
    val layout by viewModel.layout.collectAsState()
    val tickers by viewModel.tickers.collectAsState()
    val apiMessage by viewModel.apiMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val mediaStoragePath by viewModel.mediaStoragePath.collectAsState()

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show()
            viewModel.clearToast()
        }
    }

    LaunchedEffect(layout) {
        (context as? Activity)?.requestedOrientation = when (layout?.orientation?.equals("portrait", ignoreCase = true)) {
            true -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            else -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (layout != null) {
            val currentLayout = layout!!
            val topTickers = tickers.filter { it.position.equals("top", ignoreCase = true) }.sortedBy { it.priority }
            val bottomTickers = tickers.filter { it.position.equals("bottom", ignoreCase = true) }.sortedBy { it.priority }

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val density = LocalDensity.current.density
                val screenHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
                val tickerScaleY = screenHeightPx / currentLayout.screenHeight

                Column(modifier = Modifier.fillMaxSize()) {
                    if (topTickers.isNotEmpty()) {
                        topTickers.forEach { ticker ->
                            val heightDp = (ticker.height * tickerScaleY / density).dp
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
                    val scaleX = zoneAreaWidthPx / currentLayout.screenWidth
                    val scaleY = zoneAreaHeightPx / currentLayout.screenHeight

                    val zonesSorted = currentLayout.zones.sortedBy { it.zIndex }
                    zonesSorted.forEach { zone ->
                        key(zone.zoneId) {
                            val leftDp = (zone.x * scaleX / density).dp
                            val topDp = (zone.y * scaleY / density).dp
                            val widthDp = (zone.width * scaleX / density).dp
                            val heightDp = (zone.height * scaleY / density).dp

                            Box(
                                modifier = Modifier
                                    .offset(leftDp, topDp)
                                    .size(widthDp, heightDp)
                                    .clip(RectangleShape)
                                    .background(parseHexColor(zone.backgroundColor))
                                    .border(2.dp, Color.White)
                            ) {
                                ZonePlaylistContent(
                                    zoneId = zone.zoneId,
                                    playlist = zone.playlist,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }

                    if (bottomTickers.isNotEmpty()) {
                        bottomTickers.forEach { ticker ->
                            val heightDp = (ticker.height * tickerScaleY / density).dp
                            TickerStrip(
                                ticker = ticker,
                                heightDp = heightDp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
           /* if (mediaStoragePath.isNotEmpty()) {
                Text(
                    text = "Media: $mediaStoragePath",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .fillMaxWidth(0.6f)
                )
            }*/
        } else {
            // No layout: show API message and any tickers
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val density = LocalDensity.current.density
                val scaleY = with(LocalDensity.current) { maxHeight.toPx() } / 1080f

                if (!apiMessage.isNullOrBlank()) {
                    Text(
                        text = apiMessage!!,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .padding(24.dp)
                    )
                }

                val topTickers = tickers.filter { it.position.equals("top", ignoreCase = true) }.sortedBy { it.priority }
                val bottomTickers = tickers.filter { it.position.equals("bottom", ignoreCase = true) }.sortedBy { it.priority }

                if (topTickers.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                    ) {
                        topTickers.forEach { ticker ->
                            val heightDp = (ticker.height * scaleY / density).dp
                            TickerStrip(
                                ticker = ticker,
                                heightDp = heightDp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                if (bottomTickers.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                    ) {
                        bottomTickers.forEach { ticker ->
                            val heightDp = (ticker.height * scaleY / density).dp
                            TickerStrip(
                                ticker = ticker,
                                heightDp = heightDp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            /*    if (mediaStoragePath.isNotEmpty()) {
                    Text(
                        text = "Media: $mediaStoragePath",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                            .fillMaxWidth(0.85f)
                    )
                }*/
            }
        }
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
