package com.stretchdaily.app.ui.screen.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stretchdaily.app.ui.components.BigChart
import com.stretchdaily.app.ui.components.MonoCaps
import com.stretchdaily.app.ui.components.MonoCapsSize
import com.stretchdaily.app.ui.theme.Theme
import com.stretchdaily.app.ui.theme.tint
import kotlin.math.roundToLong

@Composable
fun BenchmarkAnalyticsCard(state: BenchmarkAnalyticsCardState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Theme.dims.radiusMd))
            .background(Theme.colors.surface)
            .border(1.dp, Theme.colors.line, RoundedCornerShape(Theme.dims.radiusMd)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(state.benchmark.category.tint()),
        )
        Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 4.dp)) {
            CardHeaderRow(state = state)
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.padding(start = 0.dp, end = 0.dp)) {
                BigChart(series = state.series)
            }
        }
    }
}

@Composable
private fun CardHeaderRow(state: BenchmarkAnalyticsCardState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            MonoCaps(
                text = state.benchmark.category.displayName,
                size = MonoCapsSize.Small,
                color = Theme.colors.ink3,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = state.benchmark.name,
                color = Theme.colors.ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.W600,
                fontFamily = Theme.typo.body,
            )
        }
        if (state.latestRawValue != null) {
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = state.latestRawValue,
                        color = Theme.colors.ink,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.W500,
                        fontFamily = Theme.typo.display,
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 1.dp))
                    Text(
                        text = state.benchmark.unit,
                        color = Theme.colors.ink3,
                        fontSize = 11.sp,
                        fontFamily = Theme.typo.body,
                    )
                }
                state.delta?.let { delta ->
                    Spacer(modifier = Modifier.height(3.dp))
                    DeltaChip(delta, unit = state.benchmark.unit)
                }
            }
        }
    }
}

@Composable
private fun DeltaChip(delta: com.stretchdaily.app.core.benchmark.BenchmarkDelta, unit: String) {
    val sign = if (delta.rawDelta > 0) "+" else ""
    val pretty = (delta.rawDelta * 10).roundToLong() / 10.0
    val color = if (delta.improved) Theme.colors.accent else Theme.colors.warn
    MonoCaps(
        text = "$sign$pretty $unit · 6mo",
        size = MonoCapsSize.Small,
        color = color,
    )
}
