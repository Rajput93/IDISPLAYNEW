package com.app.idisplaynew.ui.screens.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import com.app.idisplaynew.data.model.ScheduleCurrentResponse
import kotlinx.coroutines.delay

@Composable
fun ZonePlaylistContent(
    zoneId: Int,
    playlist: List<ScheduleCurrentResponse.ScheduleResult.Layout.Zone.PlaylistItem>,
    modifier: Modifier = Modifier
) {
    if (playlist.isEmpty()) return

    var currentIndex by remember(zoneId) { mutableIntStateOf(0) }
    val displayIndex = currentIndex.coerceIn(0, playlist.size - 1)

    SideEffect {
        if (currentIndex != displayIndex) currentIndex = displayIndex
    }

    val currentItem = playlist[displayIndex]

    // If current item has no URL, skip to next after a short delay
    LaunchedEffect(displayIndex, currentItem.url) {
        if (currentItem.url.isBlank()) {
            delay(300)
            currentIndex = (displayIndex + 1) % playlist.size
        }
    }

    if (currentItem.url.isBlank()) return

    val durationMillis = 400
    AnimatedContent(
        targetState = displayIndex,
        modifier = modifier.fillMaxSize(),
        transitionSpec = {
            (slideInHorizontally(animationSpec = tween(durationMillis)) { fullWidth -> fullWidth } + fadeIn(animationSpec = tween(durationMillis)))
                .togetherWith(
                    slideOutHorizontally(animationSpec = tween(durationMillis)) { fullWidth -> -fullWidth / 4 } + fadeOut(animationSpec = tween(durationMillis))
                )
        },
        label = "zone_slide"
    ) { index ->
        val item = playlist[index]
        if (item.url.isBlank()) return@AnimatedContent
        when (item.type.equals("video", ignoreCase = true)) {
            true -> ZoneVideoPlayer(
                videoUrl = item.url,
                modifier = Modifier.fillMaxSize(),
                onPlaybackEnded = if (playlist.size == 1) null else {
                    { currentIndex = (displayIndex + 1) % playlist.size }
                },
                playerKey = zoneId * 1000 + index
            )
            false -> ZoneImageView(
                imageUrl = item.url,
                durationSeconds = item.duration.coerceAtLeast(1),
                modifier = Modifier.fillMaxSize(),
                onDurationReached = {
                    currentIndex = (displayIndex + 1) % playlist.size
                }
            )
        }
    }
}
