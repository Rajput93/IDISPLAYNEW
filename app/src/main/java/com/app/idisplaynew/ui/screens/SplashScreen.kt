package com.app.idisplaynew.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.app.idisplaynew.R
import com.app.idisplaynew.ui.theme.DisplayHubBackground
import com.app.idisplaynew.ui.utils.DataStoreManager

@Composable
fun SplashScreen(
    dataStoreManager: DataStoreManager,
    onSplashFinished: (isLoggedIn: Boolean) -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }
    var isLoggedIn by remember { mutableStateOf<Boolean?>(null) }
    val alpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "splash_alpha"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        isLoggedIn = withContext(Dispatchers.IO) {
            val token = dataStoreManager.authToken.first()
            val baseUrl = dataStoreManager.baseUrl.first()
            !token.isNullOrBlank() && !baseUrl.isNullOrBlank()
        }
    }

    LaunchedEffect(alpha, isLoggedIn) {
        if (alpha >= 0.99f && isLoggedIn != null) {
            kotlinx.coroutines.delay(1200)
            onSplashFinished(isLoggedIn!!)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DisplayHubBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.ic_launcher),
                contentDescription = null,
                modifier = Modifier
                    .size(120.dp)
                    .alpha(alpha),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.displayhub),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
