package com.app.idisplaynew.ui.screens.home

import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.app.idisplaynew.R
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

/** Resolves file paths with proper encoding (spaces, etc.); avoids blank playback from bad file:// URIs. */
private fun videoUriFromString(videoUrl: String): Uri = when {
    videoUrl.startsWith("content://", ignoreCase = true) -> Uri.parse(videoUrl)
    videoUrl.startsWith("file://", ignoreCase = true) -> run {
        val path = Uri.parse(videoUrl).path
        if (!path.isNullOrBlank()) File(path).toUri() else Uri.parse(videoUrl)
    }
    videoUrl.startsWith("/") -> File(videoUrl).toUri()
    else -> Uri.parse(videoUrl)
}

@UnstableApi
@Composable
fun ZoneVideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    onPlaybackEnded: (() -> Unit)? = null,
    playerKey: Int = 0
) {
    val context = LocalContext.current

    // Tight buffers avoid OOM on 4K. FFmpeg extension decodes in software (like VLC) when MediaCodec can't.
    val exoPlayer = remember(playerKey) {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                10_000,
                35_000,
                2000,
                5000
            )
            .setTargetBufferBytes(12 * 1024 * 1024)
            .setPrioritizeTimeOverSizeThresholds(false)
            .build()
        val renderersFactory = DefaultRenderersFactory(context).apply {
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            setEnableDecoderFallback(true)
        }
        ExoPlayer.Builder(context, renderersFactory)
            .setLoadControl(loadControl)
            .build()
            .apply {
                playWhenReady = true
            }
    }
    val playbackErrorAttempts = remember(playerKey) { AtomicInteger(0) }
    var playerView by remember(playerKey) { mutableStateOf<PlayerView?>(null) }

    val currentOnPlaybackEnded = rememberUpdatedState(onPlaybackEnded)
    val currentVideoUrl = rememberUpdatedState(videoUrl)
    SideEffect {
        exoPlayer.repeatMode = if (currentOnPlaybackEnded.value != null) Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ALL
    }
    LaunchedEffect(videoUrl) {
        if (videoUrl.isBlank()) return@LaunchedEffect
        playbackErrorAttempts.set(0)
        val uri = videoUriFromString(videoUrl)
        exoPlayer.setMediaItem(MediaItem.fromUri(uri))
        exoPlayer.prepare()
        exoPlayer.play()
    }
    DisposableEffect(playerKey) {
        val callbackState = currentOnPlaybackEnded
        exoPlayer.repeatMode = if (callbackState.value != null) Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ALL
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> exoPlayer.play()
                    Player.STATE_ENDED -> callbackState.value?.invoke()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                // 4K UHD / HEVC often fails on devices that cannot decode — check Logcat; backend may need a 1080p variant.
                Log.e("ZoneVideoPlayer", "Playback error url=${currentVideoUrl.value}", error)
                // One retry (buffer/decoder) before advancing playlist — avoids skipping large files on transient failures.
                if (playbackErrorAttempts.incrementAndGet() <= 1) {
                    exoPlayer.seekToDefaultPosition()
                    exoPlayer.prepare()
                    exoPlayer.play()
                } else {
                    callbackState.value?.invoke()
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            playerView?.player = null
            playerView = null
            exoPlayer.removeListener(listener)
            // Free decoder/buffers before native release (reduces peak heap around teardown)
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.release()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            LayoutInflater.from(ctx).inflate(R.layout.zone_video_player, null).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = { view ->
            val pv = view as PlayerView
            playerView = pv
            pv.player = exoPlayer
        }
    )
}
