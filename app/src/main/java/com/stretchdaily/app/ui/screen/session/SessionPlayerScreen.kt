package com.stretchdaily.app.ui.screen.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import com.stretchdaily.app.ui.components.CircleButton
import com.stretchdaily.app.ui.components.CircleButtonSize
import com.stretchdaily.app.ui.components.CircleButtonVariant
import com.stretchdaily.app.ui.components.ExerciseTile
import com.stretchdaily.app.ui.components.ExerciseTileSize
import com.stretchdaily.app.ui.components.IconName
import com.stretchdaily.app.ui.components.MonoCaps
import com.stretchdaily.app.ui.components.MonoCapsSize
import com.stretchdaily.app.ui.components.Pill
import com.stretchdaily.app.ui.components.PillVariant
import com.stretchdaily.app.ui.components.SegmentProgress
import com.stretchdaily.app.ui.theme.Theme
import com.stretchdaily.app.ui.theme.tint

@Composable
fun SessionPlayerScreen(
    parentEntry: NavBackStackEntry,
    onClose: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm: SessionPlayerViewModel = hiltViewModel(parentEntry)
    val state by vm.state.collectAsState()

    androidx.compose.runtime.LaunchedEffect(state) {
        if (state is SessionPlayerUiState.Complete) onComplete()
    }

    Box(modifier = modifier.fillMaxSize().background(Theme.colors.bg)) {
        when (val s = state) {
            SessionPlayerUiState.Loading -> Unit
            is SessionPlayerUiState.Complete -> Unit
            is SessionPlayerUiState.Running -> PlayerBody(
                state = s,
                onClose = onClose,
                onPrev = vm::prev,
                onNext = vm::next,
                onStart = vm::start,
                onPause = vm::pause,
                onResume = vm::resume,
            )
        }
    }
}

@Composable
private fun PlayerBody(
    state: SessionPlayerUiState.Running,
    onClose: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().systemBarsPadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircleButton(
                onClick = onClose,
                icon = IconName.Close,
                size = CircleButtonSize.Medium,
                variant = CircleButtonVariant.Bg2,
                contentDescription = "Close session",
            )
            Spacer(modifier = Modifier.padding(horizontal = 12.dp))
            SegmentProgress(
                total = state.plan.items.size,
                currentIndex = state.currentIndex,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.size(44.dp))
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            MonoCaps(
                text = "MOVE ${state.currentIndex + 1} OF ${state.plan.items.size}",
                color = Theme.colors.ink3,
                size = MonoCapsSize.Regular,
            )
            Spacer(modifier = Modifier.weight(1f))
            MonoCaps(
                text = state.currentItem.exercise.category.displayName.uppercase(),
                color = Theme.colors.ink3,
                size = MonoCapsSize.Regular,
            )
        }

        ExerciseTile(
            category = state.currentItem.exercise.category,
            size = ExerciseTileSize.Large4x3,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = state.currentItem.exercise.name,
            style = Theme.typo.displayMd,
            color = Theme.colors.ink,
        )
        state.currentItem.exercise.cues.take(4).forEach { cue ->
            Row {
                Text(text = "— ", style = Theme.typo.bodyMd, color = Theme.colors.accent)
                Text(text = cue, style = Theme.typo.bodyMd, color = Theme.colors.ink2)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        TimerArea(state = state)

        Spacer(modifier = Modifier.weight(1f))

        ControlRow(
            state = state,
            onPrev = onPrev,
            onNext = onNext,
            onStart = onStart,
            onPause = onPause,
            onResume = onResume,
        )
    }
}

@Composable
private fun TimerArea(state: SessionPlayerUiState.Running) {
    val ex = state.currentItem.exercise
    val tint = ex.category.tint()
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (ex.isUnilateral) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SideChip(label = "LEFT", active = state.side == Side.LEFT, tint = tint)
                Spacer(modifier = Modifier.padding(horizontal = 6.dp))
                Text(text = "—", style = Theme.typo.bodyLg, color = Theme.colors.ink3)
                Spacer(modifier = Modifier.padding(horizontal = 6.dp))
                SideChip(label = "RIGHT", active = state.side == Side.RIGHT, tint = tint)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        if (ex.isTimed) {
            Text(
                text = formatMmSs(state.remainingSeconds),
                style = Theme.typo.displayXl,
                color = Theme.colors.ink,
            )
        } else {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = (ex.targetReps ?: 0).toString(),
                    style = Theme.typo.displayLg,
                    color = Theme.colors.ink,
                )
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text(text = "reps", style = Theme.typo.bodyLg, color = Theme.colors.ink3)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        MonoCaps(
            text = captionFor(state),
            color = Theme.colors.ink3,
            size = MonoCapsSize.Regular,
        )
    }
}

@Composable
private fun SideChip(label: String, active: Boolean, tint: Color) {
    val bg = if (active) tint.copy(alpha = 0.32f) else Color.Transparent
    val ink = if (active) Theme.colors.ink else Theme.colors.ink3
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Theme.dims.radiusPill))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        MonoCaps(text = label, color = ink, size = MonoCapsSize.Regular)
    }
}

@Composable
private fun ControlRow(
    state: SessionPlayerUiState.Running,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
) {
    val ex = state.currentItem.exercise
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        CircleButton(
            onClick = onPrev,
            icon = IconName.SkipPrev,
            size = CircleButtonSize.Large,
            variant = CircleButtonVariant.Bg2,
            contentDescription = "Previous exercise",
        )
        if (ex.isTimed) {
            val (label, action) = when (state.phase) {
                TimerPhase.READY -> "Start" to onStart
                TimerPhase.RUNNING -> "Pause" to onPause
                TimerPhase.PAUSED -> "Resume" to onResume
            }
            Pill(
                label = label,
                onClick = action,
                variant = PillVariant.Accent,
                leadingIcon = if (state.phase == TimerPhase.READY) IconName.Play else null,
                modifier = Modifier.weight(1f),
            )
        } else {
            Pill(
                label = "Perform at your own pace",
                onClick = {},
                variant = PillVariant.DashedOutline,
                modifier = Modifier.weight(1f),
            )
        }
        CircleButton(
            onClick = onNext,
            icon = IconName.SkipNext,
            size = CircleButtonSize.Large,
            variant = CircleButtonVariant.Ink,
            contentDescription = "Next exercise",
        )
    }
}

private fun formatMmSs(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%d:%02d".format(m, s)
}

private fun captionFor(state: SessionPlayerUiState.Running): String = when {
    !state.currentItem.exercise.isTimed -> "TAP NEXT WHEN COMPLETE"
    state.phase == TimerPhase.PAUSED -> "PAUSED"
    state.phase == TimerPhase.READY -> "READY WHEN YOU ARE"
    else -> "HOLD THE POSITION"
}
