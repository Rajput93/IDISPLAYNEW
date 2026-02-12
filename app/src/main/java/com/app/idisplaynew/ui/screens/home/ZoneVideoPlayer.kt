package com.app.idisplaynew.ui.screens.home

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import com.app.idisplaynew.R

@UnstableApi
@Composable
fun ZoneVideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    onPlaybackEnded: (() -> Unit)? = null,
    playerKey: Int = 0
) {
    val context = LocalContext.current

    val exoPlayer = remember(playerKey) {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
        val mediaSourceFactory = ProgressiveMediaSource.Factory(dataSourceFactory)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
                playWhenReady = true
            }
    }

    val currentOnPlaybackEnded = rememberUpdatedState(onPlaybackEnded)
    SideEffect {
        exoPlayer.repeatMode = if (currentOnPlaybackEnded.value != null) Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ALL
    }
    LaunchedEffect(videoUrl) {
        if (videoUrl.isBlank()) return@LaunchedEffect
        val uri = when {
            videoUrl.startsWith("file://") -> Uri.parse(videoUrl)
            videoUrl.startsWith("/") -> Uri.parse("file://$videoUrl")
            else -> Uri.parse(videoUrl)
        }
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
                callbackState.value?.invoke()
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
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
                (this as? PlayerView)?.player = exoPlayer
            }
        }
    )
}
