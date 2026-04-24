package com.stretchdaily.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.stretchdaily.app.ui.theme.Theme

/**
 * 50x14 dp polyline sparkline.
 *
 * [values] are expected to be normalized 0..1 (consumer does the scaling,
 * typically divides raw values by benchmark's max band). Renders:
 *   - 1.5 dp `ink3` polyline.
 *   - 2.5 dp `accent` dot at the last point.
 *
 * Empty state (0 or 1 values): dashed flat line at midheight.
 */
@Composable
fun Sparkline(
    values: List<Double>,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(width = 50.dp, height = 14.dp)) {
        if (values.size < 2) {
            drawLine(
                color = Theme.colors.line,
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f), 0f),
            )
            return@Canvas
        }

        val stepX = size.width / (values.size - 1).toFloat()
        val points = values.mapIndexed { index, v ->
            Offset(
                x = index * stepX,
                y = size.height - (v.toFloat().coerceIn(0f, 1f) * size.height),
            )
        }

        for (i in 1 until points.size) {
            drawLine(
                color = Theme.colors.ink3,
                start = points[i - 1],
                end = points[i],
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }

        drawCircle(
            color = Theme.colors.accent,
            radius = 2.5.dp.toPx(),
            center = points.last(),
        )
    }
}
