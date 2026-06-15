package com.app.idisplaynew.ui.screens.home

import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
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
import com.app.idisplaynew.data.local.MediaDownloadManager
import com.app.idisplaynew.data.local.VideoOptimizationManager
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.launch

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
    val scope = rememberCoroutineScope()
    val downloadManager = remember { MediaDownloadManager(context) }

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
    var lastSourceLocalPath by remember(playerKey) { mutableStateOf<String?>(null) }
    var triedOptimized by remember(playerKey) { mutableStateOf(false) }

    val currentOnPlaybackEnded = rememberUpdatedState(onPlaybackEnded)
    val currentVideoUrl = rememberUpdatedState(videoUrl)
    SideEffect {
        exoPlayer.repeatMode = if (currentOnPlaybackEnded.value != null) Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ALL
    }
    LaunchedEffect(videoUrl) {
        if (videoUrl.isBlank()) return@LaunchedEffect
        playbackErrorAttempts.set(0)
        val originalUri = videoUriFromString(videoUrl)
        triedOptimized = false
        lastSourceLocalPath = originalUri.path
        val playableUri = if (originalUri.scheme.equals("file", ignoreCase = true) && !originalUri.path.isNullOrBlank()) {
            val optimizedPath = VideoOptimizationManager.ensureOptimizedPlayablePath(
                context = context,
                downloadManager = downloadManager,
                sourcePath = originalUri.path!!
            )
            File(optimizedPath).toUri()
        } else originalUri
        exoPlayer.setMediaItem(MediaItem.fromUri(playableUri))
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
                Log.e("ZoneVideoPlayer", "Playback error url=${currentVideoUrl.value}", error)
                val localPath = lastSourceLocalPath
                if (!triedOptimized && !localPath.isNullOrBlank()) {
                    triedOptimized = true
                    scope.launch {
                        try {
                            val optimizedPath = VideoOptimizationManager.ensureOptimizedPlayablePath(
                                context = context,
                                downloadManager = downloadManager,
                                sourcePath = localPath
                            )
                            if (optimizedPath != localPath) {
                                Log.w("ZoneVideoPlayer", "Retrying with optimized=$optimizedPath")
                                exoPlayer.setMediaItem(MediaItem.fromUri(File(optimizedPath).toUri()))
                                exoPlayer.prepare()
                                exoPlayer.play()
                                return@launch
                            }
                        } catch (e: Exception) {
                            Log.e("ZoneVideoPlayer", "Optimization retry failed", e)
                        }
                        // fallback to normal retry/advance
                        if (playbackErrorAttempts.incrementAndGet() <= 1) {
                            exoPlayer.seekToDefaultPosition()
                            exoPlayer.prepare()
                            exoPlayer.play()
                        } else {
                            callbackState.value?.invoke()
                        }
                    }
                    return
                }

                // One retry (buffer/decoder) before advancing playlist — avoids skipping transient failures.
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
