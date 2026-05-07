package com.stretchdaily.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stretchdaily.app.core.model.Benchmark
import com.stretchdaily.app.core.model.FlexibilityTier
import com.stretchdaily.app.ui.theme.Theme

/**
 * Five-row band reference for a benchmark — VERY_FLEXIBLE → STIFF, each row
 * showing the tier name and its raw-value range (e.g., `"≥ 70°"`). Used in
 * the Log tab's expanded card and in the benchmark-day carousel.
 *
 * [highlightTier] = `null` renders all rows in the same neutral weight
 * (carousel's pre-log state). Pass the user's latest tier to bold and
 * accent that row (Log-tab usage).
 */
@Composable
fun BandsList(
    benchmark: Benchmark,
    highlightTier: FlexibilityTier?,
    modifier: Modifier = Modifier,
) {
    val tiers = listOf(
        FlexibilityTier.VERY_FLEXIBLE,
        FlexibilityTier.FLEXIBLE,
        FlexibilityTier.AVERAGE,
        FlexibilityTier.BELOW_AVERAGE,
        FlexibilityTier.STIFF,
    )
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        tiers.forEach { tier ->
            val highlighted = tier == highlightTier
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (highlighted) Theme.colors.accent else Theme.colors.line),
                )
                Text(
                    text = tier.name.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() },
                    style = Theme.typo.bodyMd.copy(
                        fontWeight = if (highlighted) FontWeight.W600 else FontWeight.W400,
                    ),
                    fontSize = 11.sp,
                    color = if (highlighted) Theme.colors.ink else Theme.colors.ink2,
                    modifier = Modifier.width(100.dp),
                )
                val hint = benchmark.tierRanges[tier.name].orEmpty()
                MonoCaps(text = hint, size = MonoCapsSize.Small, color = Theme.colors.ink3)
            }
        }
    }
}
