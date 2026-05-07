package com.stretchdaily.app.ui.screen.log

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.ui.components.AppIcon
import com.stretchdaily.app.ui.components.BandPill
import com.stretchdaily.app.ui.components.BandsList
import com.stretchdaily.app.ui.components.IconName
import com.stretchdaily.app.ui.components.MonoCaps
import com.stretchdaily.app.ui.components.MonoCapsSize
import com.stretchdaily.app.ui.components.Pill
import com.stretchdaily.app.ui.components.PillVariant
import com.stretchdaily.app.ui.components.Sheet
import com.stretchdaily.app.ui.components.Sparkline
import com.stretchdaily.app.ui.theme.Theme
import com.stretchdaily.app.ui.theme.tint

@Composable
fun BenchmarkLogScreen(
    onStartCarousel: () -> Unit,
    contentPadding: PaddingValues,
    viewModel: BenchmarkLogViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.bg)
            // Tab screen inside the bottom-nav Scaffold: we own status-bar inset,
            // Scaffold pipes the bottom-bar clearance via contentPadding.
            .statusBarsPadding(),
    ) {
        LazyColumn(
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 6.dp,
                bottom = 100.dp + contentPadding.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            item { HeroHeader() }

            item {
                Spacer(Modifier.height(14.dp))
                CarouselCtaCard(onClick = onStartCarousel)
                Spacer(Modifier.height(18.dp))
            }

            state.groups.forEach { group ->
                item {
                    CategoryGroupHeader(group.category)
                    Spacer(Modifier.height(8.dp))
                }
                items(group.rows, key = { it.benchmark.id }) { row ->
                    BenchmarkLogRow(
                        row = row,
                        expanded = row.benchmark.id == state.expandedRowId,
                        onToggle = { viewModel.onRowTapped(row.benchmark.id) },
                        onLog = { viewModel.onLogPressed(row.benchmark.id) },
                    )
                    Spacer(Modifier.height(8.dp))
                }
                item { Spacer(Modifier.height(10.dp)) }
            }
        }

        when (val sheet = state.sheet) {
            is LogSheetState.Hidden -> Unit
            is LogSheetState.Visible -> Sheet(onDismiss = { viewModel.onDismissSheet() }) {
                Column {
                    Text(
                        text = sheet.benchmark.name,
                        style = Theme.typo.displayMd,
                        fontSize = 20.sp,
                        color = Theme.colors.ink,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                    LogForm(
                        benchmark = sheet.benchmark,
                        onSubmit = { raw, tier -> viewModel.onSubmit(raw, tier) },
                        onCancel = { viewModel.onDismissSheet() },
                        errorMessage = state.errorMessage,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroHeader() {
    Column(modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)) {
        MonoCaps(
            text = "Monthly benchmarks",
            size = MonoCapsSize.Small,
            color = Theme.colors.ink3,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        Text(
            text = "Log & review",
            style = Theme.typo.displayMd,
            fontSize = 30.sp,
            fontWeight = FontWeight.W500,
            color = Theme.colors.ink,
            modifier = Modifier
                .padding(bottom = 6.dp)
                .semantics { heading() },
        )
        Text(
            text = "Ten benchmarks across seven categories. Logged on the 1st of each month.",
            style = Theme.typo.bodyMd,
            color = Theme.colors.ink2,
        )
    }
}

@Composable
private fun CarouselCtaCard(onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Theme.dims.radiusMd))
            .background(Theme.colors.accentSoft)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 13.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(Theme.dims.radiusSm))
                .background(Theme.colors.accent),
        ) {
            AppIcon(
                name = IconName.Sparkle,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Theme.colors.accentInk,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Start benchmark day",
                style = Theme.typo.bodyMd.copy(fontWeight = FontWeight.W600),
                fontSize = 13.5.sp,
                color = Theme.colors.ink,
            )
            Text(
                text = "Walk through all 10, one at a time",
                style = Theme.typo.bodyMd,
                fontSize = 11.sp,
                color = Theme.colors.ink2,
            )
        }
        AppIcon(
            name = IconName.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = Theme.colors.ink2,
        )
    }
}

@Composable
private fun CategoryGroupHeader(category: Category) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(category.tint()),
        )
        MonoCaps(
            text = category.name,
            size = MonoCapsSize.Small,
            color = Theme.colors.ink2,
        )
    }
}

@Composable
private fun BenchmarkLogRow(
    row: BenchmarkRowUiState,
    expanded: Boolean,
    onToggle: () -> Unit,
    onLog: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Theme.dims.radiusMd))
            .border(
                width = 1.dp,
                color = Theme.colors.line,
                shape = RoundedCornerShape(Theme.dims.radiusMd),
            )
            .background(Theme.colors.surface),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                NameLine(name = row.benchmark.name, isOverdue = row.isOverdue)
                Spacer(Modifier.height(4.dp))
                ValueLine(row = row)
            }
            Pill(
                onClick = onLog,
                label = "Log",
                variant = PillVariant.Accent,
            )
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center,
            ) {
                AppIcon(
                    name = IconName.ChevronDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(if (expanded) 180f else 0f),
                    tint = Theme.colors.ink3,
                )
            }
        }

        AnimatedVisibility(visible = expanded) {
            ExpandedDetails(row = row)
        }
    }
}

@Composable
private fun NameLine(name: String, isOverdue: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = name,
            style = Theme.typo.bodyMd.copy(fontWeight = FontWeight.W600),
            fontSize = 13.5.sp,
            color = Theme.colors.ink,
        )
        if (isOverdue) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Theme.dims.radiusPill))
                    .background(Theme.colors.warn.copy(alpha = 0.12f))
                    .padding(horizontal = 6.dp, vertical = 1.dp),
            ) {
                MonoCaps(
                    text = "Overdue",
                    size = MonoCapsSize.Small,
                    color = Theme.colors.warn,
                )
            }
        }
    }
}

@Composable
private fun ValueLine(row: BenchmarkRowUiState) {
    if (row.latestLog == null) {
        Text(
            text = "Not logged yet",
            style = Theme.typo.bodyMd,
            fontSize = 11.sp,
            color = Theme.colors.ink3,
        )
        return
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = row.latestLog.rawValue,
                style = Theme.typo.displayMd,
                fontSize = 17.sp,
                fontWeight = FontWeight.W500,
                color = Theme.colors.ink,
            )
            if (row.benchmark.unit.isNotBlank()) {
                Spacer(Modifier.width(2.dp))
                Text(
                    text = row.benchmark.unit,
                    style = Theme.typo.bodyMd,
                    fontSize = 11.sp,
                    color = Theme.colors.ink3,
                )
            }
        }
        row.latestTier?.let { BandPill(tier = it) }
        Sparkline(values = row.sparkline)
    }
}

@Composable
private fun ExpandedDetails(row: BenchmarkRowUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Theme.colors.bg)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        if (row.benchmark.description.isNotBlank()) {
            Text(
                text = row.benchmark.description,
                style = Theme.typo.bodyMd,
                fontSize = 11.5.sp,
                color = Theme.colors.ink2,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }
        MonoCaps(
            text = "Bands",
            size = MonoCapsSize.Small,
            color = Theme.colors.ink3,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        BandsList(
            benchmark = row.benchmark,
            highlightTier = row.latestTier,
        )

        if (row.history.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            MonoCaps(
                text = "History",
                size = MonoCapsSize.Small,
                color = Theme.colors.ink3,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            HistoryList(row = row)
        }
    }
}

@Composable
private fun HistoryList(row: BenchmarkRowUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        row.history.forEachIndexed { index, log ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
            ) {
                Text(
                    text = formatMonth(log.loggedAt),
                    style = Theme.typo.bodyMd,
                    fontSize = 11.5.sp,
                    color = Theme.colors.ink2,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val valueText = buildString {
                        append(log.rawValue)
                        if (row.benchmark.unit.isNotBlank()) {
                            append(' ')
                            append(row.benchmark.unit)
                        }
                    }
                    Text(
                        text = valueText,
                        style = Theme.typo.bodyMd.copy(fontWeight = FontWeight.W700),
                        fontSize = 11.5.sp,
                        color = Theme.colors.ink,
                    )
                    BandPill(tier = log.resolvedTier)
                }
            }
            if (index != row.history.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Theme.colors.line2),
                )
            }
        }
    }
}

private fun formatMonth(epochMilli: Long): String {
    val date = java.time.LocalDate.ofInstant(
        java.time.Instant.ofEpochMilli(epochMilli),
        java.time.ZoneId.systemDefault(),
    )
    return date.format(
        java.time.format.DateTimeFormatter.ofPattern("MMM yyyy", java.util.Locale.ENGLISH)
    )
}
