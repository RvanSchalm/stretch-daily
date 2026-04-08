package com.stretchdaily.app.ui.benchmarks

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stretchdaily.app.core.model.Benchmark
import com.stretchdaily.app.core.model.BenchmarkLog
import com.stretchdaily.app.core.model.FlexibilityTier

/**
 * Top-level benchmarks screen: a scrollable list of all 10 benchmarks showing
 * the latest logged value (or "Not logged") and a Log CTA. Tapping the
 * history icon navigates to the per-benchmark history screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BenchmarksScreen(
    viewModel: BenchmarksViewModel,
    onBack: () -> Unit,
    onOpenHistory: (Benchmark) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val dialog by viewModel.dialog.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Benchmarks", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        when (val snapshot = state) {
            BenchmarksUiState.Loading -> CenteredBox(padding) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            is BenchmarksUiState.Error -> CenteredBox(padding) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = snapshot.message,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = viewModel::refresh) {
                        Text("Retry")
                    }
                }
            }
            is BenchmarksUiState.Loaded -> BenchmarkList(
                padding = padding,
                rows = snapshot.rows,
                onLog = viewModel::openLogDialog,
                onHistory = onOpenHistory,
            )
        }
    }

    dialog?.let { dialogState ->
        LogBenchmarkDialog(
            state = dialogState,
            onDismiss = viewModel::dismissDialog,
            onRawInputChange = viewModel::onRawInputChange,
            onTierSelect = viewModel::onTierSelect,
            onSubmit = { viewModel.submitDialog() },
        )
    }
}

@Composable
private fun CenteredBox(
    padding: PaddingValues,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}

@Composable
private fun BenchmarkList(
    padding: PaddingValues,
    rows: List<BenchmarkRow>,
    onLog: (Benchmark) -> Unit,
    onHistory: (Benchmark) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(rows, key = { it.benchmark.id }) { row ->
            BenchmarkCard(
                row = row,
                onLog = { onLog(row.benchmark) },
                onHistory = { onHistory(row.benchmark) },
            )
        }
    }
}

@Composable
private fun BenchmarkCard(
    row: BenchmarkRow,
    onLog: () -> Unit,
    onHistory: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onLog)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.benchmark.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = row.benchmark.category.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = onHistory) {
                Icon(
                    imageVector = Icons.Filled.History,
                    contentDescription = "History",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        LatestLine(row.benchmark, row.latestLog)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onLog,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (row.latestLog == null) "Log baseline" else "Log new value")
        }
    }
}

@Composable
private fun LatestLine(benchmark: Benchmark, latest: BenchmarkLog?) {
    if (latest == null) {
        Text(
            text = "Not logged yet",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        return
    }
    val formatted = formatLatest(benchmark, latest)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = formatted,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.padding(horizontal = 6.dp))
        TierChip(latest.resolvedTier)
    }
}

@Composable
private fun TierChip(tier: FlexibilityTier) {
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = tier.displayName,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun formatLatest(benchmark: Benchmark, log: BenchmarkLog): String =
    when (benchmark.inputType) {
        com.stretchdaily.app.core.model.BenchmarkInputType.NUMERIC ->
            "${log.rawValue} ${benchmark.unit}"
        com.stretchdaily.app.core.model.BenchmarkInputType.CATEGORICAL ->
            log.resolvedTier.displayName
    }
