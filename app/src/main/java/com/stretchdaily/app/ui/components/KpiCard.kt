package com.stretchdaily.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.stretchdaily.app.ui.theme.Theme

/**
 * KPI card for the dashboard 2x2 grid.
 *
 * Layout: mono-caps eyebrow + icon (top), big value + optional suffix (bottom).
 * [value] uses `displayMd` Manrope 26 sp; [suffix] uses `bodySm` 11.5 sp in
 * `ink3` next to it.
 */
@Composable
fun KpiCard(
    eyebrow: String,
    icon: IconName,
    value: String,
    modifier: Modifier = Modifier,
    suffix: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Theme.dims.radiusMd))
            .background(Theme.colors.surface)
            .padding(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            MonoCaps(text = eyebrow, color = Theme.colors.ink3)
            AppIcon(name = icon, contentDescription = null, tint = Theme.colors.ink3)
        }
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.padding(top = 12.dp),
        ) {
            Text(
                text = value,
                style = Theme.typo.displayMd,
                color = Theme.colors.ink,
            )
            if (suffix != null) {
                Text(
                    text = suffix,
                    style = Theme.typo.bodySm,
                    color = Theme.colors.ink3,
                    modifier = Modifier.padding(start = 6.dp, bottom = 4.dp),
                )
            }
        }
    }
}
