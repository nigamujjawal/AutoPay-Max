package com.autopaymax.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = NavySecondary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF16233D),
    onPrimaryContainer = Color(0xFFC7D9FF),

    secondary = AppDarkSecondary,
    onSecondary = Color.White,

    tertiary = AppDarkTertiary,
    onTertiary = Color(0xFF241A00),

    error = AppDarkError,
    onError = Color.White,
    errorContainer = Color(0xFF3A1215),
    onErrorContainer = Color(0xFFFFB4AB),

    background = AppDarkBackground,
    onBackground = AppDarkTextWhite,

    surface = AppDarkCardBackground,
    onSurface = AppDarkTextWhite,
    surfaceVariant = AppDarkCardBackground,
    onSurfaceVariant = AppDarkTextGray,

    surfaceDim = AppDarkSurfaceDim,
    surfaceBright = AppDarkSurfaceBright,
    surfaceContainerLowest = AppDarkSurfaceContainerLowest,
    surfaceContainerLow = AppDarkSurfaceContainerLow,
    surfaceContainer = AppDarkSurfaceContainer,
    surfaceContainerHigh = AppDarkSurfaceContainerHigh,
    surfaceContainerHighest = AppDarkSurfaceContainerHighest,

    outline = AppDarkBorderColor,
    outlineVariant = Color(0xFF1E2733)
)

private val LightColorScheme = lightColorScheme(
    primary = NavySecondary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE7FF),
    onPrimaryContainer = Color(0xFF0B2A63),

    secondary = AppLightSecondary,
    onSecondary = Color.White,

    tertiary = AppLightTertiary,
    onTertiary = Color.White,

    error = AppLightError,
    onError = Color.White,
    errorContainer = Color(0xFFFBEAEA),
    onErrorContainer = Color(0xFF7F1D1D),

    background = AppLightBackground,
    onBackground = AppLightTextDark,

    surface = AppLightCardBackground,
    onSurface = AppLightTextDark,
    surfaceVariant = AppLightCardBackground,
    onSurfaceVariant = AppLightTextGray,

    surfaceDim = AppLightSurfaceDim,
    surfaceBright = AppLightSurfaceBright,
    surfaceContainerLowest = AppLightSurfaceContainerLowest,
    surfaceContainerLow = AppLightSurfaceContainerLow,
    surfaceContainer = AppLightSurfaceContainer,
    surfaceContainerHigh = AppLightSurfaceContainerHigh,
    surfaceContainerHighest = AppLightSurfaceContainerHighest,

    outline = AppLightBorderColor,
    outlineVariant = Color(0xFFE2E7EE)
)

@Composable
fun AppStorysAutoPayManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as Activity).window
        window.statusBarColor = colorScheme.background.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
