package com.stretchdaily.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stretchdaily.app.core.model.FlexibilityTier
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Dashboard landing screen. Renders three KPI cards (streak / this week /
 * total volume), a category heatmap derived from the latest benchmark logs,
 * and the primary "Start session" CTA. Stale benchmarks surface as a banner
 * that jumps straight to the benchmarks tab.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onStartSession: () -> Unit,
    onOpenBenchmarks: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
            start = 20.dp,
            end = 20.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { HeaderBlock(state.lastSessionAt) }

        if (state.benchmarksDue) {
            item { BenchmarksDueBanner(onOpenBenchmarks = onOpenBenchmarks) }
        }

        item {
            KpiRow(
                streakDays = state.streakDays,
                totalSessions = state.totalSessions,
                sessionsThisWeek = state.sessionsThisWeek,
            )
        }

        item {
            Text(
                text = "Category focus",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
            )
        }

        if (state.heatmap.isEmpty()) {
            item { EmptyHeatmapHint() }
        } else {
            items(state.heatmap, key = { it.category.name }) { row ->
                CategoryHeatmapBar(row)
            }
        }

        item { Spacer(Modifier.height(8.dp)) }

        item {
            Button(
                onClick = onStartSession,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Start today's session", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun HeaderBlock(lastSessionAt: Long?) {
    Column {
        Text(
            text = "Stretch Daily",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = lastSessionLabel(lastSessionAt),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
        )
    }
}

@Composable
private fun KpiRow(
    streakDays: Int,
    totalSessions: Int,
    sessionsThisWeek: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        KpiCard(
            modifier = Modifier.weight(1f),
            value = streakDays.toString(),
            label = "day streak",
        )
        KpiCard(
            modifier = Modifier.weight(1f),
            value = sessionsThisWeek.toString(),
            label = "this week",
        )
        KpiCard(
            modifier = Modifier.weight(1f),
            value = totalSessions.toString(),
            label = "total",
        )
    }
}

@Composable
private fun KpiCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
) {
    Column(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(vertical = 16.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun CategoryHeatmapBar(row: CategoryHeatmapRow) {
    val containerModifier = Modifier
        .fillMaxWidth()
        .background(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
        )
        .let {
            if (row.isStiff) {
                it.border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp),
                )
            } else {
                it
            }
        }
        .padding(horizontal = 14.dp, vertical = 12.dp)

    Column(modifier = containerModifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = row.category.displayName,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = row.tier.displayName,
                style = MaterialTheme.typography.labelMedium,
                color = if (row.isStiff) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                },
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(8.dp))
        WeightBar(weight = row.weight, highlight = row.isStiff)
    }
}

/**
 * Horizontal bar whose fill width is normalized against the
 * [FlexibilityTier.STIFF] weight (3.0). Higher weight = more session time
 * coming = wider bar.
 */
@Composable
private fun WeightBar(weight: Double, highlight: Boolean) {
    val maxWeight = FlexibilityTier.STIFF.weight
    val fraction = (weight / maxWeight).coerceIn(0.0, 1.0).toFloat()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                shape = RoundedCornerShape(4.dp),
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(8.dp)
                .background(
                    color = if (highlight) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                    },
                    shape = RoundedCornerShape(4.dp),
                ),
        )
    }
}

@Composable
private fun EmptyHeatmapHint() {
    Text(
        text = "Log your benchmarks to see which areas need the most work.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
    )
}

@Composable
private fun BenchmarksDueBanner(onOpenBenchmarks: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = "Benchmarks due",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Benchmarks need updating",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Log fresh values so the engine can bias your sessions accurately.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
            )
        }
        OutlinedButton(onClick = onOpenBenchmarks) {
            Text("Log")
        }
    }
}

private fun lastSessionLabel(
    lastSessionAt: Long?,
    now: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): String {
    if (lastSessionAt == null) return "No sessions yet — let's fix that"
    val today = LocalDate.ofInstant(Instant.ofEpochMilli(now), zoneId)
    val last = LocalDate.ofInstant(Instant.ofEpochMilli(lastSessionAt), zoneId)
    return when (val days = ChronoUnit.DAYS.between(last, today)) {
        0L -> "Last session: today"
        1L -> "Last session: yesterday"
        in 2L..6L -> "Last session: $days days ago"
        else -> {
            val weeks = days / 7
            "Last session: $weeks week${if (weeks == 1L) "" else "s"} ago"
        }
    }
}
