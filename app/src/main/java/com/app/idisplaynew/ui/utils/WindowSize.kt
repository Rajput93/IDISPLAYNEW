package com.app.idisplaynew.ui.utils

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Breakpoints for responsive design across LED TV, Tablet, and Mobile.
 * - Mobile: < 600dp
 * - Tablet: 600dp - 840dp
 * - LED TV / Large: > 840dp
 */
enum class WindowSizeClass {
    Compact,   // Mobile
    Medium,    // Tablet
    Expanded   // LED TV / Large screen
}

@Composable
fun rememberWindowSizeClass(): WindowSizeClass {
    val configuration = LocalConfiguration.current
    val widthDp = configuration.screenWidthDp
    return when {
        widthDp < 600 -> WindowSizeClass.Compact
        widthDp < 840 -> WindowSizeClass.Medium
        else -> WindowSizeClass.Expanded
    }
}

/**
 * Responsive values. effectiveCardWidth = min(92% screen, cardMaxWidth) so text/labels don't truncate.
 */
data class ResponsiveValues(
    val cardMaxWidth: Dp,
    val effectiveCardWidth: Dp,
    val cardHorizontalPadding: Dp,
    val logoSize: Dp,
    val titleFontScale: Float,
    val bodyFontScale: Float,
    val buttonHeight: Dp,
    val inputMinHeight: Dp,
    val isLandscape: Boolean
)

@Composable
fun rememberResponsiveValues(): ResponsiveValues {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val windowSize = when {
        screenWidthDp < 600 -> WindowSizeClass.Compact
        screenWidthDp < 840 -> WindowSizeClass.Medium
        else -> WindowSizeClass.Expanded
    }

    val cardMaxWidth = when (windowSize) {
        WindowSizeClass.Compact -> 400.dp
        WindowSizeClass.Medium -> 480.dp
        WindowSizeClass.Expanded -> 560.dp
    }
    val effectiveCardWidth = minOf((screenWidthDp * 0.92f).dp, cardMaxWidth)
    val cardHorizontalPadding = when (windowSize) {
        WindowSizeClass.Compact -> 16.dp
        WindowSizeClass.Medium -> 24.dp
        WindowSizeClass.Expanded -> 32.dp
    }
    val logoSize = when (windowSize) {
        WindowSizeClass.Compact -> 80.dp
        WindowSizeClass.Medium -> 100.dp
        WindowSizeClass.Expanded -> 120.dp
    }
    val titleFontScale = when (windowSize) {
        WindowSizeClass.Compact -> 1f
        WindowSizeClass.Medium -> 1.15f
        WindowSizeClass.Expanded -> 1.3f
    }
    val bodyFontScale = when (windowSize) {
        WindowSizeClass.Compact -> 1f
        WindowSizeClass.Medium -> 1.05f
        WindowSizeClass.Expanded -> 1.15f
    }
    val buttonHeight = when (windowSize) {
        WindowSizeClass.Compact -> 48.dp
        WindowSizeClass.Medium -> 52.dp
        WindowSizeClass.Expanded -> 56.dp
    }
    val inputMinHeight = when (windowSize) {
        WindowSizeClass.Compact -> 48.dp
        WindowSizeClass.Medium -> 52.dp
        WindowSizeClass.Expanded -> 56.dp
    }

    return ResponsiveValues(
        cardMaxWidth = cardMaxWidth,
        effectiveCardWidth = effectiveCardWidth,
        cardHorizontalPadding = cardHorizontalPadding,
        logoSize = logoSize,
        titleFontScale = titleFontScale,
        bodyFontScale = bodyFontScale,
        buttonHeight = buttonHeight,
        inputMinHeight = inputMinHeight,
        isLandscape = isLandscape
    )
}
