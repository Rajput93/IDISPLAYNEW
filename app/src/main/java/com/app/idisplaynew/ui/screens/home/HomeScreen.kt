package com.app.idisplaynew.ui.screens.home

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.app.idisplaynew.data.viewmodel.HomeViewModel

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val layout by viewModel.layout.collectAsState()
    val tickers by viewModel.tickers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadSchedule()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        layout?.let { currentLayout ->
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val density = LocalDensity.current.density
                val deviceWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
                val deviceHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
                val scaleX = deviceWidthPx / currentLayout.screenWidth
                val scaleY = deviceHeightPx / currentLayout.screenHeight

                val zonesSorted = currentLayout.zones.sortedBy { it.zIndex }
                zonesSorted.forEach { zone ->
                    val leftDp = (zone.x * scaleX / density).dp
                    val topDp = (zone.y * scaleY / density).dp
                    val widthDp = (zone.width * scaleX / density).dp
                    val heightDp = (zone.height * scaleY / density).dp

                    Box(
                        modifier = Modifier
                            .offset(leftDp, topDp)
                            .size(widthDp, heightDp)
                            .background(parseHexColor(zone.backgroundColor))
                            .border(2.dp, Color.White)
                    ) {
                        val firstVideo = zone.playlist.firstOrNull { it.type == "video" }
                        if (firstVideo != null && firstVideo.url.isNotBlank()) {
                            ZoneVideoPlayer(
                                videoUrl = firstVideo.url,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
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
