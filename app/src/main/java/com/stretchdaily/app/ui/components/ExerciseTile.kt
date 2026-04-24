package com.stretchdaily.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.ui.theme.Theme
import com.stretchdaily.app.ui.theme.tint

/** Size variant for [ExerciseTile]. */
enum class ExerciseTileSize { Small56, Large4x3 }

/**
 * Category-tinted diagonal-striped placeholder tile.
 *
 * Draws:
 *  1. Base fill with `category.tint()`.
 *  2. 45-degree diagonal stripes at 4 or 6 dp spacing, slightly darker tint.
 *  3. (Large only) mono-caps footer labeling the category for the shipping
 *     production look matches the handoff.
 *
 * Small56: 56x56 dp, used in session-overview exercise rows.
 * Large4x3: full-width, 4:3 aspect, used in session-player hero slot.
 */
@Composable
fun ExerciseTile(
    category: Category,
    size: ExerciseTileSize,
    modifier: Modifier = Modifier,
) {
    val tileColor = category.tint()
    val stripeColor = tileColor.darken(0.12f)

    when (size) {
        ExerciseTileSize.Small56 -> Box(
            modifier = modifier
                .size(56.dp)
                .clip(RoundedCornerShape(Theme.dims.radiusSm))
                .background(tileColor),
        ) {
            StripedCanvas(stripeColor = stripeColor, stripeSpacingDp = 4)
        }

        ExerciseTileSize.Large4x3 -> BoxWithConstraints(
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(RoundedCornerShape(Theme.dims.radiusLg))
                .background(tileColor),
        ) {
            StripedCanvas(stripeColor = stripeColor, stripeSpacingDp = 6)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter,
            ) {
                MonoCaps(
                    text = "${category.displayName} animation .webp placeholder",
                    size = MonoCapsSize.Small,
                    color = Theme.colors.ink3,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
        }
    }
}

/** Diagonal stripe layer drawn on top of the base tint. */
@Composable
private fun StripedCanvas(stripeColor: Color, stripeSpacingDp: Int) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val spacing = stripeSpacingDp.dp.toPx()
        val diag = size.width + size.height
        var x = -size.height
        while (x < diag) {
            drawLine(
                color = stripeColor,
                start = Offset(x = x, y = 0f),
                end = Offset(x = x + size.height, y = size.height),
                strokeWidth = 1.5f,
            )
            x += spacing
        }
    }
}

/** Subtracts a small amount of lightness to darken a tint for stripe contrast. */
private fun Color.darken(amount: Float): Color =
    Color(
        red = (red - amount).coerceAtLeast(0f),
        green = (green - amount).coerceAtLeast(0f),
        blue = (blue - amount).coerceAtLeast(0f),
        alpha = alpha,
    )
