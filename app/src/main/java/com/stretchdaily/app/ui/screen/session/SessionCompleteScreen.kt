package com.stretchdaily.app.ui.screen.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import com.stretchdaily.app.ui.components.AppIcon
import com.stretchdaily.app.ui.components.IconName
import com.stretchdaily.app.ui.components.MonoCaps
import com.stretchdaily.app.ui.components.MonoCapsSize
import com.stretchdaily.app.ui.components.Pill
import com.stretchdaily.app.ui.components.PillVariant
import com.stretchdaily.app.ui.theme.Theme

@Composable
fun SessionCompleteScreen(
    parentEntry: NavBackStackEntry,
    onBackToToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm: SessionPlayerViewModel = hiltViewModel(parentEntry)
    val state by vm.state.collectAsState()
    val c = (state as? SessionPlayerUiState.Complete)
        ?: return // shouldn't be reached; player navigates here only after complete

    Box(
        // Accent bg fills edge-to-edge (drawn before the inset padding), then
        // systemBarsPadding shrinks the content area so centered content
        // stays clear of the system clock (top) and gesture nav (bottom).
        modifier = modifier
            .fillMaxSize()
            .background(Theme.colors.accent)
            .systemBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp),
        ) {
            MonoCaps(
                text = "SESSION COMPLETE",
                color = Theme.colors.accentInk.copy(alpha = 0.7f),
                size = MonoCapsSize.Regular,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Well done.",
                style = Theme.typo.displayXl.copy(fontStyle = FontStyle.Italic),
                color = Theme.colors.accentInk,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "You stretched ${c.areasStretched} areas in ${c.totalMinutes} minutes. Streak's at ${c.streakAfter} now.",
                style = Theme.typo.bodyLg,
                color = Theme.colors.accentInk,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(28.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(1.dp)
                        .background(Theme.colors.accentInk.copy(alpha = 0.4f)),
                )
                Spacer(modifier = Modifier.width(10.dp))
                AppIcon(
                    name = IconName.Flame,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Theme.colors.accentInk,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(1.dp)
                        .background(Theme.colors.accentInk.copy(alpha = 0.4f)),
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Pill(
                label = "Back to today",
                onClick = onBackToToday,
                variant = PillVariant.Inverse,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
