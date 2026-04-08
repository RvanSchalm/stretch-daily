package com.stretchdaily.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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

/**
 * Placeholder landing screen. Phase 5 will replace this with the real
 * dashboard (streak, volume, category heatmap). For now it's the entry
 * point into the session and benchmarks flows, plus a banner that nags
 * the user to log their benchmarks when the data is stale.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onStartSession: () -> Unit,
    onOpenBenchmarks: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(64.dp))
        Text(
            text = "Stretch Daily",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Your daily mobility routine",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        )

        Spacer(Modifier.weight(1f))

        if (state.benchmarksDue) {
            BenchmarksDueBanner(onOpenBenchmarks = onOpenBenchmarks)
            Spacer(Modifier.height(16.dp))
        }

        Button(
            onClick = onStartSession,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("Start today's session", fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onOpenBenchmarks,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("Benchmarks", fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(16.dp))
    }
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
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
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
