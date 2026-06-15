package com.app.idisplaynew.ui.screens.home

import android.annotation.SuppressLint
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import coil.compose.AsyncImage
import com.app.idisplaynew.data.model.ScheduleCurrentResponse
import kotlinx.coroutines.delay

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun TickerStrip(
    ticker: ScheduleCurrentResponse.ScheduleResult.Ticker,
    heightDp: Dp,
    modifier: Modifier = Modifier
) {
    val textColor = parseHexColorTicker(ticker.textColor ?: "#000000")
    val backgroundColor = parseHexColorTicker(ticker.backgroundColor ?: "#ffffff")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp)
            .background(backgroundColor)
            .clip(RectangleShape),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Brand logo fixed on left – ticker height ke hisaab se, same background pe
        val brandUrl = ticker.brandImageUrl?.takeIf { it.isNotBlank() }
        if (brandUrl != null) {
            Box(
                modifier = Modifier
                    .height(heightDp)
                    .width(heightDp)
                    .background(backgroundColor)
                    .clip(RectangleShape),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = brandUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp)
                        .clip(RectangleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Scrolling text (remaining width) – clip taaki logo ke upar se na jaaye, sirf isi area mein dikhe
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(heightDp)
                .clip(RectangleShape)
        ) {
            val density = LocalDensity.current
            var textWidthPx by remember(ticker.text) { mutableIntStateOf(0) }
            val containerWidthPx = with(density) { maxWidth.toPx().toInt() }
            val gapPx = with(density) { 24.dp.toPx().toInt() }

            /**
             * Spec: smooth right-to-left ticker.
             * - Speed controlled by numeric value (e.g. 32)
             * - Duration derived from (textWidth + screenWidth) / speed
             * - Update every ~16ms (≈60fps)
             * - Reset when text fully exits left side
             */
            val speedValue = (ticker.speed ?: 32).coerceIn(1, 300)
            val travelPx by remember(containerWidthPx, textWidthPx, gapPx) {
                derivedStateOf { (containerWidthPx + textWidthPx + gapPx).coerceAtLeast(containerWidthPx + gapPx) }
            }
            val durationMs by remember(travelPx, speedValue) {
                derivedStateOf {
                    // Interpret "speed" as seconds per full travel / or just a scalar: higher = faster.
                    // Map to px/sec so 32 feels reasonably fast across screens.
                    val pxPerSec = (speedValue * 20f).coerceAtLeast(60f)
                    ((travelPx / pxPerSec) * 1000f).toLong().coerceIn(1000L, 300_000L)
                }
            }
            val pxPerMs by remember(travelPx, durationMs) {
                derivedStateOf { travelPx.toFloat() / durationMs.toFloat().coerceAtLeast(1f) }
            }

            var offsetX by remember(ticker.id, ticker.text, ticker.speed, containerWidthPx, textWidthPx) {
                mutableFloatStateOf(containerWidthPx.toFloat())
            }
            LaunchedEffect(ticker.id, ticker.text, ticker.speed, containerWidthPx, textWidthPx, durationMs) {
                // restart cleanly on any update
                offsetX = containerWidthPx.toFloat()
                while (true) {
                    // ~60 FPS
                    delay(16L)
                    offsetX -= pxPerMs * 16f
                    val exitLeft = -textWidthPx.toFloat() - gapPx.toFloat()
                    if (offsetX <= exitLeft) {
                        offsetX = containerWidthPx.toFloat()
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RectangleShape),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = ticker.text.orEmpty(),
                    color = textColor,
                    fontSize = (ticker.fontSize ?: 24).sp,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier
                        .onSizeChanged { sz ->
                            // Update only when changed to avoid recompose churn.
                            val w = sz.width
                            if (w > 0 && w != textWidthPx) textWidthPx = w
                        }
                        .graphicsLayer {
                            translationX = offsetX
                        }
                )
            }
        }
    }
}

private fun parseHexColorTicker(hex: String): Color {
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
