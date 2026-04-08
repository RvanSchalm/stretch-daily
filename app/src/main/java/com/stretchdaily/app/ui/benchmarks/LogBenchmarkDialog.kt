package com.stretchdaily.app.ui.benchmarks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.stretchdaily.app.core.model.BenchmarkInputType
import com.stretchdaily.app.core.model.FlexibilityTier

/**
 * Modal dialog used for both logging and editing. Renders a numeric text
 * field for numeric benchmarks or five tier buttons for the single
 * categorical one. State is entirely owned by [BenchmarksViewModel] so the
 * dialog is pure display + callbacks.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogBenchmarkDialog(
    state: LogDialogState,
    onDismiss: () -> Unit,
    onRawInputChange: (String) -> Unit,
    onTierSelect: (FlexibilityTier) -> Unit,
    onSubmit: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            ) {
                Text(
                    text = if (state.editingLogId == null) "Log value" else "Edit value",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                Text(
                    text = state.benchmark.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = state.benchmark.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                )
                Spacer(Modifier.height(16.dp))

                when (state.benchmark.inputType) {
                    BenchmarkInputType.NUMERIC -> NumericBody(
                        state = state,
                        onRawInputChange = onRawInputChange,
                    )
                    BenchmarkInputType.CATEGORICAL -> CategoricalBody(
                        state = state,
                        onTierSelect = onTierSelect,
                    )
                }

                state.error?.let { err ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Button(
                        onClick = onSubmit,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text("Save", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NumericBody(
    state: LogDialogState,
    onRawInputChange: (String) -> Unit,
) {
    // Show the range copy from the benchmark for the user's reference.
    TierRangeHint(state.benchmark.tierRanges)
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = state.rawInput,
        onValueChange = onRawInputChange,
        label = { Text("Value (${state.benchmark.unit})") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = KeyboardType.Number,
        ),
    )
}

@Composable
private fun CategoricalBody(
    state: LogDialogState,
    onTierSelect: (FlexibilityTier) -> Unit,
) {
    val orderedTiers = listOf(
        FlexibilityTier.STIFF,
        FlexibilityTier.BELOW_AVERAGE,
        FlexibilityTier.AVERAGE,
        FlexibilityTier.FLEXIBLE,
        FlexibilityTier.VERY_FLEXIBLE,
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (tier in orderedTiers) {
            val isSelected = tier == state.selectedTier
            val description = state.benchmark.tierRanges[tier.name].orEmpty()
            TierOption(
                tier = tier,
                description = description,
                selected = isSelected,
                onClick = { onTierSelect(tier) },
            )
        }
    }
}

@Composable
private fun TierOption(
    tier: FlexibilityTier,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val container = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    } else {
        MaterialTheme.colorScheme.background
    }
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(container, RoundedCornerShape(8.dp)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = tier.displayName,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            if (description.isNotEmpty()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                )
            }
        }
    }
}

@Composable
private fun TierRangeHint(tierRanges: Map<String, String>) {
    val order = listOf(
        FlexibilityTier.STIFF,
        FlexibilityTier.BELOW_AVERAGE,
        FlexibilityTier.AVERAGE,
        FlexibilityTier.FLEXIBLE,
        FlexibilityTier.VERY_FLEXIBLE,
    )
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        for (tier in order) {
            val range = tierRanges[tier.name] ?: continue
            Text(
                text = "${tier.displayName}: $range",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            )
        }
    }
}
