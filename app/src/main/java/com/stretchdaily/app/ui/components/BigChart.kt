package com.stretchdaily.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.stretchdaily.app.core.benchmark.ProgressSeries
import com.stretchdaily.app.core.model.FlexibilityTier
import com.stretchdaily.app.ui.theme.Theme

/**
 * 130 dp-tall full-width benchmark progress chart.
 *
 * Layers (bottom to top):
 *  1. Five horizontal tier bands (subtle ink3 x 0.05 alternating).
 *  2. Gridlines at band boundaries (line2).
 *  3. Dashed midline (line, dashed).
 *  4. Filled area under the polyline (accent x 0.12).
 *  5. 2 dp polyline (accent).
 *  6. 6 dp highlighted last-point marker with ink-bg outline.
 *
 * Left gutter: mono-caps labels "STIFF", "AVERAGE", "VERY FLEXIBLE"
 * rendered outside the Canvas in a Column with weighted children so
 * text baselines align with band centers.
 */
@Composable
internal fun BigChart(
    series: ProgressSeries,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.height(130.dp).fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxHeight().width(56.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            MonoCaps(
                text = FlexibilityTier.VERY_FLEXIBLE.displayName,
                size = MonoCapsSize.Small,
                color = Theme.colors.ink3,
            )
            MonoCaps(
                text = FlexibilityTier.AVERAGE.displayName,
                size = MonoCapsSize.Small,
                color = Theme.colors.ink3,
            )
            MonoCaps(
                text = FlexibilityTier.STIFF.displayName,
                size = MonoCapsSize.Small,
                color = Theme.colors.ink3,
            )
        }

        // Capture theme colors at the composable scope — `Theme.colors` is a
        // @Composable property and can't be read inside the Canvas draw lambda.
        val bandShade = Theme.colors.ink3.copy(alpha = 0.05f)
        val gridColor = Theme.colors.line2
        val midlineColor = Theme.colors.line
        val accentColor = Theme.colors.accent
        val accentFill = Theme.colors.accent.copy(alpha = 0.12f)
        val markerOutline = Theme.colors.bg

        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val bandCount = 5
                val bandHeight = size.height / bandCount

                for (i in 0 until bandCount) {
                    drawRect(
                        color = if (i % 2 == 0) Color.Transparent else bandShade,
                        topLeft = Offset(0f, i * bandHeight),
                        size = Size(size.width, bandHeight),
                    )
                }

                for (i in 1 until bandCount) {
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, i * bandHeight),
                        end = Offset(size.width, i * bandHeight),
                        strokeWidth = 1f,
                    )
                }

                drawLine(
                    color = midlineColor,
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f),
                )

                if (series.points.isEmpty()) return@Canvas

                val offsets = series.points.map { p ->
                    Offset(
                        x = p.xRatio * size.width,
                        y = size.height - (p.yRatio * size.height),
                    )
                }
                val fillPath = Path().apply {
                    moveTo(offsets.first().x, size.height)
                    offsets.forEach { lineTo(it.x, it.y) }
                    lineTo(offsets.last().x, size.height)
                    close()
                }
                drawPath(path = fillPath, color = accentFill)

                val linePath = Path().apply {
                    moveTo(offsets.first().x, offsets.first().y)
                    offsets.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(
                    path = linePath,
                    color = accentColor,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                )

                val last = offsets.last()
                drawCircle(color = markerOutline, radius = 6.dp.toPx(), center = last)
                drawCircle(color = accentColor, radius = 4.dp.toPx(), center = last)
            }
        }
    }
}
