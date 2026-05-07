package com.stretchdaily.app.ui.screen.log

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stretchdaily.app.core.model.Benchmark
import com.stretchdaily.app.core.model.BenchmarkInputType
import com.stretchdaily.app.core.model.FlexibilityTier
import com.stretchdaily.app.ui.components.IconName
import com.stretchdaily.app.ui.components.MonoCaps
import com.stretchdaily.app.ui.components.MonoCapsSize
import com.stretchdaily.app.ui.components.Pill
import com.stretchdaily.app.ui.components.PillVariant
import com.stretchdaily.app.ui.theme.Theme

@Composable
fun LogForm(
    benchmark: Benchmark,
    onSubmit: (rawValue: String, tier: FlexibilityTier?) -> Unit,
    onCancel: () -> Unit,
    errorMessage: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        when (benchmark.inputType) {
            BenchmarkInputType.NUMERIC -> NumericInput(
                unit = benchmark.unit,
                onSubmit = { raw -> onSubmit(raw, null) },
                onCancel = onCancel,
            )
            BenchmarkInputType.CATEGORICAL -> CategoricalInput(
                onSubmit = { tier -> onSubmit(tier.name, tier) },
                onCancel = onCancel,
            )
        }

        if (!errorMessage.isNullOrBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = errorMessage,
                style = Theme.typo.bodyMd,
                color = Theme.colors.warn,
            )
        }
    }
}

@Composable
private fun NumericInput(
    unit: String,
    onSubmit: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var raw by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        MonoCaps(
            text = "Your score",
            size = MonoCapsSize.Small,
            color = Theme.colors.ink3,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(Theme.dims.radiusMd))
                    .background(Theme.colors.bg)
                    .border(
                        width = 1.dp,
                        color = Theme.colors.line,
                        shape = RoundedCornerShape(Theme.dims.radiusMd),
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                BasicTextField(
                    value = raw,
                    onValueChange = { raw = it.filter { ch -> ch.isDigit() || ch == '.' || ch == '-' || ch == ',' } },
                    textStyle = LocalTextStyle.current.copy(
                        color = Theme.colors.ink,
                        fontSize = 30.sp,
                        fontFamily = Theme.typo.displayMd.fontFamily,
                        textAlign = TextAlign.Center,
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (unit.isNotBlank()) {
                Text(
                    text = unit,
                    style = Theme.typo.bodyMd,
                    color = Theme.colors.ink3,
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        FormFooter(
            saveEnabled = raw.isNotBlank(),
            onCancel = onCancel,
            onSave = { onSubmit(raw.trim()) },
        )
    }
}

@Composable
private fun CategoricalInput(
    onSubmit: (FlexibilityTier) -> Unit,
    onCancel: () -> Unit,
) {
    var selected by remember { mutableStateOf<FlexibilityTier?>(null) }
    val tiers = remember {
        // Display order: most flexible at the top.
        listOf(
            FlexibilityTier.VERY_FLEXIBLE,
            FlexibilityTier.FLEXIBLE,
            FlexibilityTier.AVERAGE,
            FlexibilityTier.BELOW_AVERAGE,
            FlexibilityTier.STIFF,
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        MonoCaps(
            text = "Pick your level",
            size = MonoCapsSize.Small,
            color = Theme.colors.ink3,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            tiers.forEach { tier ->
                val isSel = tier == selected
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Theme.dims.radiusMd))
                        .background(if (isSel) Theme.colors.accent else Theme.colors.bg)
                        .clickable { selected = tier }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = tier.name.replace('_', ' ').lowercase()
                            .replaceFirstChar { it.titlecase() },
                        style = Theme.typo.bodyMd,
                        color = if (isSel) Theme.colors.accentInk else Theme.colors.ink,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        FormFooter(
            saveEnabled = selected != null,
            onCancel = onCancel,
            onSave = { selected?.let(onSubmit) },
        )
    }
}

@Composable
private fun FormFooter(
    saveEnabled: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Pill(
            onClick = onCancel,
            label = "Cancel",
            variant = PillVariant.Neutral,
            modifier = Modifier.weight(1f),
        )
        Pill(
            onClick = { if (saveEnabled) onSave() },
            label = "Save entry",
            leadingIcon = IconName.Check,
            variant = PillVariant.Accent,
            modifier = Modifier.weight(2f),
        )
    }
}
