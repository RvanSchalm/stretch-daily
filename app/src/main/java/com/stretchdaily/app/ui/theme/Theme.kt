package com.stretchdaily.app.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * `CompositionLocal`s that carry the three token data classes down the tree.
 *
 * Read via [Theme.colors] / [Theme.typo] / [Theme.dims]. Consumers should
 * NOT touch these directly — the `Theme` accessor keeps call sites short
 * and survives a future refactor (e.g. multi-theme runtime swap).
 */
private val LocalAppColors = staticCompositionLocalOf<AppColors> {
    error("AppColors not provided — wrap your root in StretchDailyTheme().")
}
private val LocalAppTypography = staticCompositionLocalOf<AppTypography> {
    error("AppTypography not provided — wrap your root in StretchDailyTheme().")
}
private val LocalAppDimens = staticCompositionLocalOf<AppDimens> {
    error("AppDimens not provided — wrap your root in StretchDailyTheme().")
}

/**
 * Root theme composable. Provides the Sage tokens to the tree and wraps a
 * Material3 theme so default ripple color + unspecified `Text` / `Icon`
 * content-colors resolve to ink on surface.
 *
 * `MaterialTheme.colorScheme` is kept minimal — every concrete color in
 * app code flows through `Theme.colors.*`.
 */
@Composable
fun StretchDailyTheme(content: @Composable () -> Unit) {
    val colors = remember_sage_colors
    val typo = remember_sage_typo
    val dims = remember_sage_dims

    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalAppTypography provides typo,
        LocalAppDimens provides dims,
        LocalContentColor provides colors.ink,
    ) {
        MaterialTheme(
            colorScheme = lightColorScheme(
                primary = colors.accent,
                onPrimary = colors.accentInk,
                background = colors.bg,
                onBackground = colors.ink,
                surface = colors.surface,
                onSurface = colors.ink,
                surfaceVariant = colors.surface2,
                onSurfaceVariant = colors.ink2,
                error = colors.warn,
                onError = colors.accentInk,
            ),
            content = content,
        )
    }
}

/**
 * Short-form accessors used at call sites: `Theme.colors.ink`, etc.
 *
 * Keeps Compose code readable vs. the verbose `LocalAppColors.current.ink`
 * and means consumers never know which `CompositionLocal` a token lives in.
 */
object Theme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current

    val typo: AppTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalAppTypography.current

    val dims: AppDimens
        @Composable
        @ReadOnlyComposable
        get() = LocalAppDimens.current
}

// Intentionally top-level `val` rather than `remember { ... }`: Sage tokens
// are immutable data classes with no Composition-scoped state; one shared
// instance per process is correct.
private val remember_sage_colors: AppColors = sageColors()
private val remember_sage_typo: AppTypography = sageTypography()
private val remember_sage_dims: AppDimens = sageDimens()
