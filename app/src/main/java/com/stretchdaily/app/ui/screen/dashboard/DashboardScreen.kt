package com.stretchdaily.app.ui.screen.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stretchdaily.app.core.engine.model.PlannedExercise
import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.ui.components.AppIcon
import com.stretchdaily.app.ui.components.CircleButton
import com.stretchdaily.app.ui.components.CircleButtonSize
import com.stretchdaily.app.ui.components.CircleButtonVariant
import com.stretchdaily.app.ui.components.IconName
import com.stretchdaily.app.ui.components.KpiCard
import com.stretchdaily.app.ui.components.MonoCaps
import com.stretchdaily.app.ui.components.MonoCapsSize
import com.stretchdaily.app.ui.components.WeekStrip
import com.stretchdaily.app.ui.theme.Theme
import com.stretchdaily.app.ui.theme.tint
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Dashboard (tab = "today"). See design spec §8.1.
 *
 * Layout, top to bottom:
 *  - TopStrip: "WEDNESDAY, MAY 6" + streak chip (flame + N).
 *  - HeroHeader: "Stretch Daily" + "N exercises · ~M minutes · ..." subtitle.
 *  - BenchmarkBanner (optional): visible only when the user turned on
 *    [com.stretchdaily.app.core.datastore.SettingsDataStore.benchmarkBannerEnabled]
 *    *and* there is at least one overdue benchmark.
 *  - TodaySessionCard: eyebrow + minutes + extra focus + play [CircleButton] +
 *    per-exercise category-tint bars.
 *  - "THIS WEEK" label + [WeekStrip].
 *  - "SNAPSHOT" label + 2×2 [KpiCard] grid.
 *
 * The outer Scaffold declares `contentWindowInsets = WindowInsets(0)`, so
 * each tab owns its system-bar insets. The LazyColumn applies
 * `statusBarsPadding()` on the outside to clear the system clock strip, then
 * [mergePadding] combines [Theme.dims.padScreen] (which bakes in the 100.dp
 * bottom-nav clearance) with the Scaffold's measured bottom-bar inset via
 * `maxOf` on each axis — so the two bottom contributions don't double-count.
 */
@Composable
fun DashboardScreen(
    onStartSession: () -> Unit,
    onBenchmarkBannerTap: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val vm: DashboardViewModel = hiltViewModel()
    val state by vm.state.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .background(Theme.colors.bg)
            // The outer Scaffold uses `contentWindowInsets = WindowInsets(0)`
            // (edge-to-edge convention per R1), so each screen consumes its
            // own status-bar inset. `statusBarsPadding()` shifts the LazyColumn
            // below the system clock/battery strip; `padScreen.top` (6.dp) then
            // adds the extra breathing room above the first item.
            .statusBarsPadding(),
        contentPadding = mergePadding(Theme.dims.padScreen, contentPadding),
        verticalArrangement = Arrangement.spacedBy(Theme.dims.gapSection),
    ) {
        item {
            TopStrip(todayLabel = formatDate(state.today), streak = state.streakDays)
        }
        item { HeroHeader(count = state.plannedExercises.size, minutes = state.plannedMinutes) }

        when (val banner = state.banner) {
            is BannerState.Visible -> item {
                BenchmarkBanner(
                    overdueCount = banner.overdueCount,
                    onClick = onBenchmarkBannerTap,
                )
            }
            BannerState.Hidden -> Unit
        }

        item {
            TodaySessionCard(
                minutes = state.plannedMinutes,
                extraFocus = state.extraFocus,
                items = state.plannedExercises,
                onStartSession = onStartSession,
            )
        }

        item { SectionLabel("THIS WEEK") }
        item {
            WeekStrip(
                today = state.today,
                completed = state.weekCompleted,
            )
        }

        item { SectionLabel("SNAPSHOT") }
        item { KpiGrid(state.kpis) }
    }
}

@Composable
private fun TopStrip(todayLabel: String, streak: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        MonoCaps(text = todayLabel, color = Theme.colors.ink3, size = MonoCapsSize.Regular)
        Spacer(modifier = Modifier.weight(1f))
        AppIcon(
            name = IconName.Flame,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = Theme.colors.accent,
        )
        Spacer(modifier = Modifier.size(4.dp))
        MonoCaps(
            text = "$streak",
            color = Theme.colors.accent,
            size = MonoCapsSize.Regular,
        )
    }
}

@Composable
private fun HeroHeader(count: Int, minutes: Int) {
    Column {
        Text(
            text = "Stretch Daily",
            modifier = Modifier.semantics { heading() },
            style = Theme.typo.displayLg,
            color = Theme.colors.ink,
        )
        Spacer(modifier = Modifier.size(2.dp))
        Text(
            text = "$count exercises · ~$minutes minutes · mixed full body",
            style = Theme.typo.bodyMd,
            color = Theme.colors.ink2,
        )
    }
}

@Composable
private fun BenchmarkBanner(overdueCount: Int, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Theme.dims.radiusMd))
            .background(Theme.colors.accentSoft)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Theme.colors.bg2),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(
                name = IconName.Sparkle,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Theme.colors.accent,
            )
        }
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .weight(1f),
        ) {
            Text(
                text = "Benchmark day is here",
                style = Theme.typo.bodyLg.copy(fontWeight = FontWeight.SemiBold),
                color = Theme.colors.ink,
            )
            Text(
                text = "$overdueCount tests waiting on a fresh reading.",
                style = Theme.typo.bodySm,
                color = Theme.colors.ink2,
            )
        }
        AppIcon(
            name = IconName.ChevronRight,
            contentDescription = "Open benchmark carousel",
            modifier = Modifier.size(18.dp),
            tint = Theme.colors.ink2,
        )
    }
}

@Composable
private fun TodaySessionCard(
    minutes: Int,
    extraFocus: Category?,
    items: List<PlannedExercise>,
    onStartSession: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Theme.dims.radiusLg))
            .background(Theme.colors.surface)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                MonoCaps(
                    text = "TODAY'S SESSION",
                    color = Theme.colors.ink3,
                    size = MonoCapsSize.Regular,
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = "$minutes minutes",
                    style = Theme.typo.displayMd,
                    color = Theme.colors.ink,
                )
                extraFocus?.let {
                    Spacer(modifier = Modifier.size(2.dp))
                    Text(
                        text = "Extra focus: ${it.displayName}",
                        style = Theme.typo.bodySm,
                        color = Theme.colors.ink2,
                    )
                }
            }
            CircleButton(
                onClick = onStartSession,
                icon = IconName.Play,
                contentDescription = "Begin session",
                size = CircleButtonSize.Large58,
                variant = CircleButtonVariant.Accent,
            )
        }
        if (items.isNotEmpty()) SessionOrderBars(items)
    }
}

@Composable
private fun SessionOrderBars(items: List<PlannedExercise>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(5.dp),
    ) {
        items.forEach { item ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(item.exercise.category.tint()),
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    MonoCaps(text = text, color = Theme.colors.ink3, size = MonoCapsSize.Regular)
}

@Composable
private fun KpiGrid(kpis: Kpis) {
    Column(verticalArrangement = Arrangement.spacedBy(Theme.dims.gapList)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Theme.dims.gapList)) {
            KpiCard(
                eyebrow = "Streak",
                icon = IconName.Flame,
                value = kpis.streakDays.toString(),
                suffix = if (kpis.streakDays == 1) "day" else "days",
                modifier = Modifier.weight(1f),
            )
            KpiCard(
                eyebrow = "Total time",
                icon = IconName.Clock,
                value = kpis.totalMinutes.toString(),
                suffix = "min",
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Theme.dims.gapList)) {
            KpiCard(
                eyebrow = "Sessions",
                icon = IconName.Check,
                value = kpis.totalSessions.toString(),
                suffix = "done",
                modifier = Modifier.weight(1f),
            )
            KpiCard(
                eyebrow = "Next benchmark",
                icon = IconName.Calendar,
                value = kpis.nextBenchmarkDays?.toString() ?: "—",
                suffix = if (kpis.nextBenchmarkDays != null) "d" else "",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Merge the outer bottom-nav inset with the screen-local [Theme.dims.padScreen].
 * `padScreen` already carries a 100.dp bottom for the nav; the outer Scaffold
 * contributes the exact measured bottom-bar height. Taking `maxOf` on each axis
 * prevents double-counting (both insets cover the same nav). Start/end come
 * from padScreen directly — the outer Scaffold has `contentWindowInsets =
 * WindowInsets(0)` so it contributes nothing horizontally.
 */
private fun mergePadding(inner: PaddingValues, outer: PaddingValues): PaddingValues =
    PaddingValues(
        start = inner.calculateStartPadding(LayoutDirection.Ltr),
        end = inner.calculateEndPadding(LayoutDirection.Ltr),
        top = maxOf(inner.calculateTopPadding(), outer.calculateTopPadding()),
        bottom = maxOf(inner.calculateBottomPadding(), outer.calculateBottomPadding()),
    )

private val DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH)

private fun formatDate(date: java.time.LocalDate): String = date.format(DATE_FORMATTER)
