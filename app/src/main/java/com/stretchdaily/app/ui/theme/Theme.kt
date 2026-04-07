package com.stretchdaily.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val StretchDailyColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = Surface,
    onSurfaceVariant = OnSurface
)

@Composable
fun StretchDailyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StretchDailyColorScheme,
        typography = StretchDailyTypography,
        content = content
    )
}
