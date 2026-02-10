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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import com.app.idisplaynew.data.model.ScheduleCurrentResponse
import kotlinx.coroutines.delay

private fun isDisplayable(item: ScheduleCurrentResponse.ScheduleResult.Layout.Zone.PlaylistItem): Boolean {
    return when (item.type.equals("video", ignoreCase = true)) {
        true -> item.url.isNotBlank()
        false -> when (item.type.lowercase()) {
            "image" -> item.url.isNotBlank()
            "notice", "event", "motivational", "weather" ->
                !item.htmlContent.isNullOrBlank() || !item.sourceUrl.isNullOrBlank()
            else -> item.url.isNotBlank()
        }
    }
}

@Composable
fun ZonePlaylistContent(
    zoneId: Int,
    playlist: List<ScheduleCurrentResponse.ScheduleResult.Layout.Zone.PlaylistItem>,
    modifier: Modifier = Modifier
) {
    if (playlist.isEmpty()) return

    var currentIndex by remember(zoneId) { mutableIntStateOf(0) }
    var lastPlayingMediaId by remember(zoneId) { mutableStateOf<Int?>(null) }
    var prevPlaylistSize by remember(zoneId) { mutableIntStateOf(0) }
    val displayIndex = currentIndex.coerceIn(0, playlist.size - 1)

    SideEffect {
        if (currentIndex != displayIndex) currentIndex = displayIndex
    }
    SideEffect {
        val size = playlist.size
        if (size > prevPlaylistSize && lastPlayingMediaId != null) {
            val found = playlist.indexOfFirst { it.mediaId == lastPlayingMediaId }
            if (found >= 0) currentIndex = found
        }
        prevPlaylistSize = size
        lastPlayingMediaId = playlist.getOrNull(displayIndex.coerceIn(0, size - 1))?.mediaId
    }

    val currentItem = playlist[displayIndex]

    // Skip non-displayable items (e.g. WebView types without content yet, or invalid)
    LaunchedEffect(displayIndex, currentItem.mediaId, currentItem.url, currentItem.htmlContent, currentItem.sourceUrl) {
        if (!isDisplayable(currentItem)) {
            delay(300)
            currentIndex = (displayIndex + 1) % playlist.size
        }
    }

    if (!isDisplayable(currentItem)) return

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
        if (index !in playlist.indices) return@AnimatedContent
        val item = playlist[index]
        if (!isDisplayable(item)) return@AnimatedContent
        key(item.mediaId) {
        when (item.type.lowercase()) {
            "video" -> ZoneVideoPlayer(
                videoUrl = item.url,
                modifier = Modifier.fillMaxSize(),
                onPlaybackEnded = if (playlist.size == 1) null else {
                    { currentIndex = (displayIndex + 1) % playlist.size }
                },
                playerKey = zoneId * 10000 + item.mediaId
            )
            "image" -> ZoneImageView(
                imageUrl = item.url,
                durationSeconds = item.duration.coerceAtLeast(1),
                modifier = Modifier.fillMaxSize(),
                onDurationReached = {
                    currentIndex = (displayIndex + 1) % playlist.size
                }
            )
            "notice", "event", "motivational", "weather" -> ZoneWebView(
                htmlContent = item.htmlContent,
                sourceUrl = item.sourceUrl,
                durationSeconds = item.duration.coerceAtLeast(1),
                modifier = Modifier.fillMaxSize(),
                onDurationReached = {
                    currentIndex = (displayIndex + 1) % playlist.size
                }
            )
            else -> ZoneImageView(
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
}
