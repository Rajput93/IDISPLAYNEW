package com.app.idisplaynew.ui.screens.home

import android.annotation.SuppressLint
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.app.idisplaynew.data.model.ScheduleCurrentResponse

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
            var textWidthPx by remember { mutableStateOf(0f) }
            val containerWidthPx = with(LocalDensity.current) { maxWidth.toPx() }

            key(textWidthPx, ticker.speed ?: 1) {
                val speed = (ticker.speed ?: 1).coerceAtLeast(1)
                val durationMs = (60000 / speed).toInt().coerceIn(1000, 120000)
                val targetOffsetPx = if (textWidthPx > 0) -(textWidthPx + 16f) else -containerWidthPx
                val infiniteTransition = rememberInfiniteTransition(label = "ticker")
                val offsetPx by infiniteTransition.animateFloat(
                    initialValue = containerWidthPx,
                    targetValue = targetOffsetPx,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = if (textWidthPx > 0) durationMs else 1,
                            easing = LinearEasing
                        ),
                        repeatMode = RepeatMode.Restart,
                        initialStartOffset = androidx.compose.animation.core.StartOffset(0)
                    ),
                    label = "tickerOffset"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(heightDp)
                        .clip(RectangleShape)
                        .layout { measurable, constraints ->
                            val unboundedConstraints = constraints.copy(maxWidth = Int.MAX_VALUE)
                            val placeable = measurable.measure(unboundedConstraints)
                            if (placeable.width > 0) textWidthPx = placeable.width.toFloat()
                            layout(constraints.maxWidth, constraints.maxHeight) {
                                placeable.place(offsetPx.toInt(), 0)
                            }
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = ticker.text.orEmpty(),
                        color = textColor,
                        fontSize = (ticker.fontSize ?: 24).sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
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
