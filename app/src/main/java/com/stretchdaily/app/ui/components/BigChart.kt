package com.stretchdaily.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stretchdaily.app.core.benchmark.ProgressSeries
import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.core.model.FlexibilityTier
import com.stretchdaily.app.ui.theme.Theme
import com.stretchdaily.app.ui.theme.tint
import java.time.Month
import java.time.YearMonth

/**
 * 130 dp tall benchmark progress chart with a 36 dp month-axis row below.
 *
 * Layout (left to right):
 *  - Fixed 56 dp Y-axis gutter with three tick labels (max, mid, min).
 *  - Horizontally scrollable region: Canvas (chart) + month-axis row.
 *
 * Width of scrollable region = months between [ProgressSeries.firstMonth]
 * and [ProgressSeries.lastMonth] × 60 dp (with a minimum of one month
 * width). Default scroll position = end (most recent point in view).
 */
@Composable
internal fun BigChart(
    series: ProgressSeries,
    category: Category,
    modifier: Modifier = Modifier,
) {
    if (series.points.isEmpty()) {
        Box(modifier = modifier.height(166.dp).fillMaxWidth())
        return
    }

    val tint = category.tint().copy(alpha = 0.18f)
    val accentColor = Theme.colors.accent
    val accentFill = Theme.colors.accent.copy(alpha = 0.12f)
    val gridColor = Theme.colors.line2
    val markerOutline = Theme.colors.bg

    Row(modifier = modifier.height(166.dp).fillMaxWidth()) {
        // Y-axis gutter — fixed, doesn't scroll. Same height as the canvas.
        Column(
            modifier = Modifier.width(56.dp).height(130.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            series.yTickLabels.forEach { label ->
                MonoCaps(text = label, size = MonoCapsSize.Small, color = Theme.colors.ink3)
            }
        }

        // Scrollable chart region (Canvas + month axis), shared width.
        val totalMonths = monthsBetween(series.firstMonth, series.lastMonth) + 1
        val pxPerMonth = 60.dp
        val totalWidth: Dp = (pxPerMonth.value * totalMonths).dp.coerceAtLeast(pxPerMonth)
        val scrollState = rememberScrollState()
        LaunchedEffect(scrollState.maxValue) { scrollState.scrollTo(scrollState.maxValue) }

        Box(modifier = Modifier.fillMaxHeight().horizontalScroll(scrollState)) {
            Column(modifier = Modifier.width(totalWidth)) {
                Canvas(modifier = Modifier.width(totalWidth).height(130.dp)) {
                    // Tinted background fills the whole canvas.
                    drawRect(color = tint, size = Size(size.width, size.height))

                    // Two faint horizontal grid lines at 1/3 and 2/3.
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, size.height / 3f),
                        end = Offset(size.width, size.height / 3f),
                        strokeWidth = 1f,
                    )
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, size.height * 2f / 3f),
                        end = Offset(size.width, size.height * 2f / 3f),
                        strokeWidth = 1f,
                    )

                    if (series.points.isEmpty()) return@Canvas

                    // Y mapping: rawMax → top (y = 0), rawMin → bottom (y = size.height).
                    val rawSpan = (series.rawMax - series.rawMin).coerceAtLeast(0.0001f)
                    val offsets = series.points.map { p ->
                        val xWorld = p.xRatio * size.width
                        val raw = p.rawValue
                        val yWorld = if (series.isCategorical) {
                            // Tier-based Y for categorical: VERY_FLEXIBLE (top, idx 0)
                            // → STIFF (bottom, idx 4). Center of each band.
                            val tierIdx = when (p.tier) {
                                FlexibilityTier.VERY_FLEXIBLE -> 0
                                FlexibilityTier.FLEXIBLE -> 1
                                FlexibilityTier.AVERAGE -> 2
                                FlexibilityTier.BELOW_AVERAGE -> 3
                                FlexibilityTier.STIFF -> 4
                            }
                            ((tierIdx + 0.5f) / 5f) * size.height
                        } else if (raw != null) {
                            ((series.rawMax - raw) / rawSpan) * size.height
                        } else {
                            size.height / 2f
                        }
                        Offset(xWorld, yWorld)
                    }

                    // Filled area under polyline.
                    val fillPath = Path().apply {
                        moveTo(offsets.first().x, size.height)
                        offsets.forEach { lineTo(it.x, it.y) }
                        lineTo(offsets.last().x, size.height)
                        close()
                    }
                    drawPath(path = fillPath, color = accentFill)

                    // Polyline.
                    val linePath = Path().apply {
                        moveTo(offsets.first().x, offsets.first().y)
                        offsets.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(
                        path = linePath,
                        color = accentColor,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                    )

                    // Dot at every point — outline ring + filled dot.
                    offsets.forEach { o ->
                        drawCircle(color = markerOutline, radius = 6.dp.toPx(), center = o)
                        drawCircle(color = accentColor, radius = 4.dp.toPx(), center = o)
                    }
                }

                MonthAxisRow(
                    firstMonth = series.firstMonth,
                    totalMonths = totalMonths,
                    pxPerMonth = pxPerMonth,
                    modifier = Modifier.width(totalWidth).height(36.dp),
                )
            }
        }
    }
}

/** Months between [from] and [to] (e.g. April → June returns 2). */
private fun monthsBetween(from: YearMonth, to: YearMonth): Int =
    ((to.year - from.year) * 12 + (to.monthValue - from.monthValue)).coerceAtLeast(0)

@Composable
private fun MonthAxisRow(
    firstMonth: YearMonth,
    totalMonths: Int,
    pxPerMonth: Dp,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        var month = firstMonth
        var index = 0
        repeat(totalMonths) {
            MonthAxisCell(
                month = month,
                indexFromStart = index,
                modifier = Modifier.width(pxPerMonth).fillMaxHeight(),
            )
            month = month.plusMonths(1)
            index++
        }
    }
}

@Composable
private fun MonthAxisCell(
    month: YearMonth,
    indexFromStart: Int,
    modifier: Modifier = Modifier,
) {
    val isJanuary = month.month == Month.JANUARY
    val showMonth = (indexFromStart % 2 == 0) || isJanuary
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        if (showMonth) {
            MonoCaps(
                text = month.month.name.take(3),
                size = MonoCapsSize.Small,
                color = Theme.colors.ink3,
            )
        }
        if (isJanuary) {
            MonoCaps(
                text = "'${(month.year % 100).toString().padStart(2, '0')}",
                size = MonoCapsSize.Small,
                color = Theme.colors.ink3,
            )
        }
    }
}
