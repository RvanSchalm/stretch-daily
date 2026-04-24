package com.stretchdaily.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stretchdaily.app.ui.theme.Theme

/** Visual variants for [Pill]. */
enum class PillVariant { Accent, Neutral, DashedOutline }

/**
 * Pill button. Rounded to `radiusPill`, 14 dp vertical padding by default.
 *
 * Variants:
 *  - [PillVariant.Accent] - accent bg + accentInk text ("Begin session").
 *  - [PillVariant.Neutral] - bg2 bg + ink text (secondary actions).
 *  - [PillVariant.DashedOutline] - transparent + accent border. Used for
 *    "Perform at your own pace" on rep-based session moves.
 *
 * Compose `Modifier.border` does not support dashed strokes directly; for
 * DashedOutline we currently render a solid accent border. If a dashed
 * look is required in R4, swap to a `Modifier.drawBehind` with
 * `PathEffect.dashPathEffect(...)`.
 */
@Composable
fun Pill(
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: IconName? = null,
    variant: PillVariant = PillVariant.Accent,
) {
    val bg = when (variant) {
        PillVariant.Accent -> Theme.colors.accent
        PillVariant.Neutral -> Theme.colors.bg2
        PillVariant.DashedOutline -> Color.Transparent
    }
    val fg = when (variant) {
        PillVariant.Accent -> Theme.colors.accentInk
        PillVariant.Neutral -> Theme.colors.ink
        PillVariant.DashedOutline -> Theme.colors.accent
    }
    val borderMod: Modifier = when (variant) {
        PillVariant.DashedOutline -> Modifier.border(
            border = BorderStroke(2.dp, Theme.colors.accent),
            shape = RoundedCornerShape(Theme.dims.radiusPill),
        )
        else -> Modifier
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(Theme.dims.radiusPill))
            .background(bg)
            .then(borderMod)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
    ) {
        if (leadingIcon != null) {
            AppIcon(name = leadingIcon, contentDescription = null, tint = fg)
        }
        Text(text = label, style = Theme.typo.bodyLg, color = fg)
    }
}

/** Size of a [CircleButton] - 36 / 44 / 52 / 58 dp. */
enum class CircleButtonSize(val dp: Dp) {
    Small(36.dp),
    Medium(44.dp),
    Large(52.dp),
    Large58(58.dp),
}

/** Color scheme for a [CircleButton]. */
enum class CircleButtonVariant { Bg2, Ink, Accent }

/**
 * Round icon-only button. Three variants:
 *  - [CircleButtonVariant.Bg2] - `bg2` background + `ink` icon. Used for
 *    close, swap, nav chevrons.
 *  - [CircleButtonVariant.Ink] - `ink` background + `bg` icon. Used for
 *    session-player "next".
 *  - [CircleButtonVariant.Accent] - `accent` background + `bg` icon. Used
 *    for the big "Begin session" CTA on the dashboard today card.
 */
@Composable
fun CircleButton(
    onClick: () -> Unit,
    icon: IconName,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: CircleButtonSize = CircleButtonSize.Medium,
    variant: CircleButtonVariant = CircleButtonVariant.Bg2,
) {
    val (bg, fg) = when (variant) {
        CircleButtonVariant.Bg2 -> Theme.colors.bg2 to Theme.colors.ink
        CircleButtonVariant.Ink -> Theme.colors.ink to Theme.colors.bg
        CircleButtonVariant.Accent -> Theme.colors.accent to Theme.colors.bg
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick),
    ) {
        AppIcon(name = icon, contentDescription = contentDescription, tint = fg)
    }
}
