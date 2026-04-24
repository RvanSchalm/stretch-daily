package com.stretchdaily.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.stretchdaily.app.R
import com.stretchdaily.app.ui.theme.Theme

/**
 * Enum wrapper over the bundled outline icon set (res/drawable/ic_*.xml).
 *
 * Screens reference [IconName.Play] / [IconName.Swap] / etc. rather than
 * `R.drawable.ic_play` directly - keeps the icon surface small and
 * refactor-friendly.
 */
enum class IconName(@DrawableRes val res: Int) {
    Play(R.drawable.ic_play),
    Pause(R.drawable.ic_pause),
    SkipNext(R.drawable.ic_skip_next),
    SkipPrev(R.drawable.ic_skip_prev),
    Swap(R.drawable.ic_swap),
    Sparkle(R.drawable.ic_sparkle),
    Flame(R.drawable.ic_flame),
    ChevronRight(R.drawable.ic_chevron_right),
    ChevronDown(R.drawable.ic_chevron_down),
    Close(R.drawable.ic_close),
    Check(R.drawable.ic_check),
}

/**
 * Renders an outline icon from the bundled set.
 *
 * [contentDescription] is non-null only on interactive uses - purely
 * decorative icons (e.g. inside a labeled button) pass `null`. Matches
 * the accessibility discipline established in Phase 8 of the original
 * project.
 *
 * Default sizing is left to the Material3 `Icon` default (24 dp); call
 * sites override with `Modifier.size(...)`.
 */
@Composable
fun AppIcon(
    name: IconName,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Theme.colors.ink,
) {
    Icon(
        painter = painterResource(id = name.res),
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier,
    )
}
