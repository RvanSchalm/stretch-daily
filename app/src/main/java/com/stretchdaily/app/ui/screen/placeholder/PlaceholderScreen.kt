package com.stretchdaily.app.ui.screen.placeholder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stretchdaily.app.ui.theme.Theme
import java.util.Locale

/**
 * Minimal per-tab landing until the real screen ships.
 *
 * Shows:
 *  - a small mono-caps eyebrow ("PLACEHOLDER") proving the Sage
 *    typography is wired end-to-end.
 *  - the tab label in displayLg.
 *  - the phase tag that unlocks this tab.
 *
 * Applies the outer scaffold's `contentPadding` so content breathes
 * against the status bar and clears the bottom nav.
 */
@Composable
fun PlaceholderScreen(
    tabLabel: String,
    unlocksInPhase: String,
    contentPadding: PaddingValues,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.bg)
            .padding(contentPadding)
            .padding(Theme.dims.padScreen),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "PLACEHOLDER".uppercase(Locale.getDefault()),
                style = Theme.typo.monoCaps,
                color = Theme.colors.ink3,
            )
            Text(
                text = tabLabel,
                style = Theme.typo.displayLg,
                color = Theme.colors.ink,
            )
            Text(
                text = "Unlocks in $unlocksInPhase",
                style = Theme.typo.bodyMd,
                color = Theme.colors.ink2,
            )
        }
    }
}
