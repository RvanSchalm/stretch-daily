package com.stretchdaily.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.stretchdaily.app.core.model.FlexibilityTier
import com.stretchdaily.app.ui.theme.Theme

/**
 * Tier-colored pill labeled by [FlexibilityTier.displayName].
 *
 * Five discrete colors shading from warn to accent along the tier axis
 * (stiff/below-average/average/flexible/very flexible). Warn tier uses
 * `warn` at 0.9 alpha; accent tier uses `accent` at full.
 */
@Composable
fun BandPill(
    tier: FlexibilityTier,
    modifier: Modifier = Modifier,
) {
    val bg: Color = when (tier) {
        FlexibilityTier.STIFF -> Theme.colors.warn.copy(alpha = 0.90f)
        FlexibilityTier.BELOW_AVERAGE -> Theme.colors.warn.copy(alpha = 0.55f)
        FlexibilityTier.AVERAGE -> Theme.colors.ink3.copy(alpha = 0.35f)
        FlexibilityTier.FLEXIBLE -> Theme.colors.accent.copy(alpha = 0.60f)
        FlexibilityTier.VERY_FLEXIBLE -> Theme.colors.accent
    }
    val fg: Color = when (tier) {
        FlexibilityTier.AVERAGE -> Theme.colors.ink
        else -> Theme.colors.accentInk
    }
    MonoCaps(
        text = tier.displayName,
        size = MonoCapsSize.Small,
        color = fg,
        modifier = modifier
            .clip(RoundedCornerShape(Theme.dims.radiusPill))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
