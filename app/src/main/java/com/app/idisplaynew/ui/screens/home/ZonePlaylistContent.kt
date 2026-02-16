package com.app.idisplaynew.ui.screens.home

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.ui.unit.IntOffset
import androidx.media3.common.util.UnstableApi
import com.app.idisplaynew.data.model.ScheduleCurrentResponse
import kotlinx.coroutines.delay

private fun isDisplayable(item: ScheduleCurrentResponse.ScheduleResult.Layout.Zone.PlaylistItem): Boolean {
    return when (item.type.equals("video", ignoreCase = true)) {
        true -> item.url.isNotBlank()
        false -> when (item.type.lowercase()) {
            "image", "document" -> item.url.isNotBlank()
            "notice", "event", "motivational", "weather", "url" ->
                !item.htmlContent.isNullOrBlank() || !item.sourceUrl.isNullOrBlank()
            else -> item.url.isNotBlank()
        }
    }
}

/** Builds enter/exit transition from API transition effect (screenshot: None, Fade, Slide Left/Right/Up/Down, Zoom In/Out, Flip, Dissolve). */
private fun contentTransformForTransition(
    effectName: String?,
    effectCssClass: String?,
    durationMs: Int?
): ContentTransform {
    val key = listOf(
        effectName?.lowercase()?.trim(),
        effectCssClass?.lowercase()?.trim()
    ).firstOrNull { !it.isNullOrBlank() } ?: "fade"
    val d = when { key.contains("none") -> 80; else -> durationMs?.coerceIn(100, 3000) ?: 500 }
    val tweenFloat = tween<Float>(durationMillis = d, easing = FastOutSlowInEasing)
    val tweenOffset = tween<IntOffset>(durationMillis = d, easing = FastOutSlowInEasing)
    return when {
        key.contains("none") -> (fadeIn(tweenFloat) togetherWith fadeOut(tweenFloat))
        key.contains("slide") && (key.contains("left") || key.contains("right")) -> when {
            key.contains("left") -> (slideInHorizontally(tweenOffset) { fullWidth -> -fullWidth } + fadeIn(tweenFloat))
                .togetherWith(slideOutHorizontally(tweenOffset) { fullWidth -> fullWidth } + fadeOut(tweenFloat))
            else -> (slideInHorizontally(tweenOffset) { fullWidth -> fullWidth } + fadeIn(tweenFloat))
                .togetherWith(slideOutHorizontally(tweenOffset) { fullWidth -> -fullWidth } + fadeOut(tweenFloat))
        }
        key.contains("slide") && (key.contains("up") || key.contains("down")) -> when {
            key.contains("up") -> (slideInVertically(tweenOffset) { fullHeight -> fullHeight } + fadeIn(tweenFloat))
                .togetherWith(slideOutVertically(tweenOffset) { fullHeight -> -fullHeight } + fadeOut(tweenFloat))
            else -> (slideInVertically(tweenOffset) { fullHeight -> -fullHeight } + fadeIn(tweenFloat))
                .togetherWith(slideOutVertically(tweenOffset) { fullHeight -> fullHeight } + fadeOut(tweenFloat))
        }
        key.contains("zoom") -> when {
            key.contains("out") -> (scaleIn(initialScale = 1.3f, animationSpec = tweenFloat) + fadeIn(tweenFloat))
                .togetherWith(scaleOut(targetScale = 0.7f, animationSpec = tweenFloat) + fadeOut(tweenFloat))
            else -> (scaleIn(initialScale = 0.7f, animationSpec = tweenFloat) + fadeIn(tweenFloat))
                .togetherWith(scaleOut(targetScale = 0.7f, animationSpec = tweenFloat) + fadeOut(tweenFloat))
        }
        key.contains("flip") -> (scaleIn(initialScale = 0.85f, animationSpec = tweenFloat) + fadeIn(tweenFloat))
            .togetherWith(scaleOut(targetScale = 0.85f, animationSpec = tweenFloat) + fadeOut(tweenFloat))
        key.contains("dissolve") || key.contains("fade") -> (fadeIn(tweenFloat) togetherWith fadeOut(tweenFloat))
        else -> (fadeIn(tweenFloat) togetherWith fadeOut(tweenFloat))
    }
}

@OptIn(UnstableApi::class)
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

    // Skip non-displayable items after a short delay (advance to next so we don't get stuck)
    LaunchedEffect(displayIndex, currentItem.mediaId, currentItem.url, currentItem.htmlContent, currentItem.sourceUrl) {
        if (!isDisplayable(currentItem)) {
            delay(300)
            currentIndex = (displayIndex + 1) % playlist.size
        }
    }

    // Do not early-return here: AnimatedContent must stay mounted for transitions to run when
    // we switch to the next item. Non-displayable slots are skipped inside via return@AnimatedContent.
    AnimatedContent(
        targetState = displayIndex,
        modifier = modifier.fillMaxSize(),
        transitionSpec = {
            val targetItem = playlist.getOrNull(targetState)
            contentTransformForTransition(
                effectName = targetItem?.transitionEffectName,
                effectCssClass = targetItem?.transitionEffectCssClass,
                durationMs = targetItem?.transitionEffectDurationMs
            )
        },
        label = "zone_transition"
    ) { index ->
        if (index !in playlist.indices) return@AnimatedContent
        val item = playlist[index]
        if (!isDisplayable(item)) return@AnimatedContent
        key(zoneId, item.mediaId) {
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
            "notice", "event", "motivational", "weather", "url" -> ZoneWebView(
                htmlContent = item.htmlContent,
                sourceUrl = item.sourceUrl,
                durationSeconds = item.duration.coerceAtLeast(1),
                modifier = Modifier.fillMaxSize(),
                onDurationReached = {
                    currentIndex = (displayIndex + 1) % playlist.size
                }
            )
            "document" -> ZoneDocumentView(
                documentUrl = item.url,
                fileName = item.fileName,
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
