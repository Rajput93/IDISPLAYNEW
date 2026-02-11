package com.app.idisplaynew.ui.screens.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

@Composable
fun ZoneImageView(
    imageUrl: String,
    durationSeconds: Int,
    modifier: Modifier = Modifier,
    onDurationReached: () -> Unit = {}
) {
    Box(modifier = modifier.fillMaxSize()) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillWidth
        )
        LaunchedEffect(imageUrl, durationSeconds) {
            val durationMs = (durationSeconds.coerceAtLeast(1)) * 1000L
            delay(durationMs)
            onDurationReached()
        }
    }
}
