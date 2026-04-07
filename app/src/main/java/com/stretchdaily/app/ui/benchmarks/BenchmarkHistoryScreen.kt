package com.stretchdaily.app.ui.benchmarks

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stretchdaily.app.core.model.Benchmark
import com.stretchdaily.app.core.model.BenchmarkLog
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.Flow

/**
 * Per-benchmark history screen. Streams logs for [benchmarkId] via the
 * repository, lets the user edit or delete each row inline, and shares the
 * same log dialog the list screen uses.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BenchmarkHistoryScreen(
    viewModel: BenchmarksViewModel,
    benchmarkId: String,
    onBack: () -> Unit,
) {
    // Resolve the benchmark once; re-resolving would flicker on re-composition.
    var benchmark by remember { mutableStateOf<Benchmark?>(null) }
    LaunchedEffect(benchmarkId) {
        benchmark = viewModel.getBenchmark(benchmarkId)
    }

    val logsFlow: Flow<List<BenchmarkLog>> = remember(benchmarkId) {
        viewModel.observeLogsFor(benchmarkId)
    }
    val logs by logsFlow.collectAsState(initial = emptyList())
    val dialog by viewModel.dialog.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        benchmark?.name ?: "History",
                        fontWeight = FontWeight.Bold,
                    )
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
        val bm = benchmark
        if (bm == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Loading…",
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        } else {
            HistoryList(
                padding = padding,
                benchmark = bm,
                logs = logs,
                onEdit = { log -> viewModel.openEditDialog(bm, log) },
                onDelete = viewModel::deleteLog,
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
private fun HistoryList(
    padding: PaddingValues,
    benchmark: Benchmark,
    logs: List<BenchmarkLog>,
    onEdit: (BenchmarkLog) -> Unit,
    onDelete: (BenchmarkLog) -> Unit,
) {
    if (logs.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "No logs yet",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(logs, key = { it.id }) { log ->
            HistoryRow(
                benchmark = benchmark,
                log = log,
                onEdit = { onEdit(log) },
                onDelete = { onDelete(log) },
            )
        }
    }
}

@Composable
private fun HistoryRow(
    benchmark: Benchmark,
    log: BenchmarkLog,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = formatLogValue(benchmark, log),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${log.resolvedTier.displayName} • ${formatDate(log.loggedAt)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        IconButton(onClick = onEdit) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = "Edit",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
    }
}

private fun formatLogValue(benchmark: Benchmark, log: BenchmarkLog): String =
    when (benchmark.inputType) {
        com.stretchdaily.app.core.model.BenchmarkInputType.NUMERIC ->
            "${log.rawValue} ${benchmark.unit}"
        com.stretchdaily.app.core.model.BenchmarkInputType.CATEGORICAL ->
            log.resolvedTier.displayName
    }

private val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())

private fun formatDate(epochMillis: Long): String =
    dateFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
