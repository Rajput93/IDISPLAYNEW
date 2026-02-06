package com.app.idisplaynew.ui.screens.home

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
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
import com.app.idisplaynew.data.model.ScheduleCurrentResponse

@Composable
fun TickerStrip(
    ticker: ScheduleCurrentResponse.ScheduleResult.Ticker,
    heightDp: Dp,
    modifier: Modifier = Modifier
) {
    val textColor = parseHexColorTicker(ticker.textColor)
    val backgroundColor = parseHexColorTicker(ticker.backgroundColor)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp)
            .background(backgroundColor)
            .clip(RectangleShape)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(heightDp)
        ) {
            var textWidthPx by remember { mutableStateOf(0f) }
            val containerWidthPx = with(LocalDensity.current) { maxWidth.toPx() }

            key(textWidthPx) {
                val durationMs = (15000 / maxOf(1, ticker.speed)).toInt()
                val targetOffsetPx = if (textWidthPx > 0) -(textWidthPx + 100f) else containerWidthPx - 100f
                val infiniteTransition = rememberInfiniteTransition(label = "ticker")
                val offsetPx by infiniteTransition.animateFloat(
                    initialValue = containerWidthPx,
                    targetValue = targetOffsetPx,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = if (textWidthPx > 0) durationMs else 1,
                            easing = LinearEasing
                        ),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "tickerOffset"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(heightDp)
                        .layout { measurable, constraints ->
                            val unboundedConstraints = constraints.copy(maxWidth = Int.MAX_VALUE)
                            val placeable = measurable.measure(unboundedConstraints)
                            if (placeable.width > 0) {
                                textWidthPx = placeable.width.toFloat()
                            }
                            layout(constraints.maxWidth, constraints.maxHeight) {
                                placeable.place(offsetPx.toInt(), 0)
                            }
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = ticker.text,
                            color = textColor,
                            fontSize = ticker.fontSize.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
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
