package com.app.idisplaynew.ui.screens.home

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

@Composable
fun ZoneVideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    onPlaybackEnded: (() -> Unit)? = null,
    playerKey: Int = 0
) {
    val context = LocalContext.current

    val exoPlayer = remember(playerKey, videoUrl) {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
        val mediaSourceFactory = ProgressiveMediaSource.Factory(dataSourceFactory)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(videoUrl))
                playWhenReady = true
                repeatMode = if (onPlaybackEnded != null) Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ALL
            }
    }

    DisposableEffect(videoUrl, onPlaybackEnded, playerKey) {
        exoPlayer.repeatMode = if (onPlaybackEnded != null) Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ALL
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> exoPlayer.play()
                    Player.STATE_ENDED -> onPlaybackEnded?.invoke()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                onPlaybackEnded?.invoke()
            }
        }
        exoPlayer.addListener(listener)
        exoPlayer.prepare()

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        }
    )
}
