package com.stretchdaily.app.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Dimensional tokens: radii, gaps, and the global screen padding.
 *
 * Held as a data class so an alternate palette / density profile can slot in
 * later via [StretchDailyTheme] without call-site refactors. Access at call
 * sites through [Theme.dims].
 *
 * `padScreen`'s 100.dp bottom accounts for the fixed bottom-nav overlap —
 * screens apply it in their outer `Scaffold`'s `contentPadding` so tab tops
 * breathe against the status bar and list bottoms clear the nav.
 */
data class AppDimens(
    val radiusXs: Dp,
    val radiusSm: Dp,
    val radiusMd: Dp,
    val radiusLg: Dp,
    val radiusPill: Dp,
    val gapList: Dp,
    val gapSection: Dp,
    val gapGroup: Dp,
    val padScreen: PaddingValues,
)

/** Sage defaults — see spec §4.1. */
fun sageDimens(): AppDimens = AppDimens(
    radiusXs = 8.dp,
    radiusSm = 12.dp,
    radiusMd = 16.dp,
    radiusLg = 22.dp,
    radiusPill = 999.dp,
    gapList = 8.dp,
    gapSection = 14.dp,
    gapGroup = 18.dp,
    padScreen = PaddingValues(start = 20.dp, top = 6.dp, end = 20.dp, bottom = 100.dp),
)
