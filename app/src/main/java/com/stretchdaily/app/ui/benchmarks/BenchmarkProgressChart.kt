package com.stretchdaily.app.ui.benchmarks

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stretchdaily.app.core.benchmark.BenchmarkProgressBuilder
import com.stretchdaily.app.core.benchmark.ProgressSeries
import com.stretchdaily.app.core.model.BenchmarkLog
import com.stretchdaily.app.core.model.FlexibilityTier
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Compact line chart showing how a benchmark's tier has progressed over
 * time. Renders five horizontal bands (one per [FlexibilityTier]) with
 * tier labels on the left, and a polyline + dots over the top mapping
 * each log's resolved tier against its timestamp.
 *
 * Sits at the top of [BenchmarkHistoryScreen] so the user always sees
 * the chart in the same place as the underlying data points.
 */
@Composable
internal fun BenchmarkProgressChart(
    logs: List<BenchmarkLog>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 180.dp,
) {
    val series = BenchmarkProgressBuilder.build(logs)
    val tiersTopDown = listOf(
        FlexibilityTier.VERY_FLEXIBLE,
        FlexibilityTier.FLEXIBLE,
        FlexibilityTier.AVERAGE,
        FlexibilityTier.BELOW_AVERAGE,
        FlexibilityTier.STIFF,
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(16.dp),
    ) {
        Text(
            text = "Tier progression",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight),
        ) {
            // Left gutter: one tier label per band. `weight(1f)` per box gives
            // each tier exactly 1/5 of the row height so the labels line up
            // with the chart's grid lines (also at band centers).
            Column(modifier = Modifier.fillMaxHeight()) {
                tiersTopDown.forEach { tier ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        Text(
                            text = tier.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            ChartCanvas(
                series = series,
                bandCount = tiersTopDown.size,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }

        Spacer(Modifier.height(8.dp))
        DateRangeFooter(series = series)
    }
}

@Composable
private fun ChartCanvas(
    series: ProgressSeries,
    bandCount: Int,
    modifier: Modifier = Modifier,
) {
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
    val lineColor = MaterialTheme.colorScheme.primary
    val pointFill = MaterialTheme.colorScheme.background

    Canvas(modifier = modifier) {
        val bandHeight = size.height / bandCount

        // Faint grid line at the center of each tier band.
        for (i in 0 until bandCount) {
            val y = (i + 0.5f) * bandHeight
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
            )
        }

        if (series.points.isEmpty()) return@Canvas

        // Polyline for ≥2 points.
        if (series.points.size > 1) {
            val path = Path().apply {
                series.points.forEachIndexed { index, point ->
                    val x = point.xRatio * size.width
                    val y = point.yRatio * size.height
                    if (index == 0) moveTo(x, y) else lineTo(x, y)
                }
            }
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 3f),
            )
        }

        // Point markers — orange ring with the background color filled inside
        // so they read clearly even when they overlap a grid line.
        val outerRadius = 5.dp.toPx()
        val innerRadius = 3.dp.toPx()
        series.points.forEach { point ->
            val center = Offset(
                x = point.xRatio * size.width,
                y = point.yRatio * size.height,
            )
            drawCircle(color = lineColor, radius = outerRadius, center = center)
            drawCircle(color = pointFill, radius = innerRadius, center = center)
        }
    }
}

@Composable
private fun DateRangeFooter(series: ProgressSeries) {
    if (series.points.isEmpty()) {
        Text(
            text = "Log this benchmark to start tracking progress.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        return
    }
    if (series.points.size == 1) {
        Text(
            text = "First log on ${formatDate(series.points.first().timestampMillis)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        return
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = formatDate(series.points.first().timestampMillis),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        Text(
            text = formatDate(series.points.last().timestampMillis),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
    }
}

private val footerDateFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())

private fun formatDate(epochMillis: Long): String =
    footerDateFormatter.format(
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
    )
