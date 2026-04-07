package com.stretchdaily.app.ui.session

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * The active workout screen. Shows a big countdown, the current exercise name
 * + cues, and pause / skip controls. For unilateral exercises the ViewModel
 * flips [SessionUiState.FollowAlong.side] between LEFT and RIGHT between
 * countdown phases; this screen just reflects whatever state it's handed.
 */
@Composable
fun SessionFollowAlongScreen(
    state: SessionUiState.FollowAlong,
    onTogglePause: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 24.dp),
    ) {
        ProgressHeader(state)
        Spacer(Modifier.height(24.dp))
        AnimationPlaceholder()
        Spacer(Modifier.height(24.dp))
        ExerciseHeadline(state)
        Spacer(Modifier.height(16.dp))
        CueList(state.currentItem.exercise.cues)
        Spacer(Modifier.weight(1f))
        Countdown(state)
        Spacer(Modifier.height(24.dp))
        Controls(isPaused = state.isPaused, onTogglePause = onTogglePause, onSkip = onSkip)
    }
}

@Composable
private fun ProgressHeader(state: SessionUiState.FollowAlong) {
    val total = state.plan.items.size
    val current = state.currentIndex + 1
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Exercise $current of $total",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { (current - 1).toFloat() / total.toFloat() },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surface,
        )
    }
}

@Composable
private fun AnimationPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Animation placeholder",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun ExerciseHeadline(state: SessionUiState.FollowAlong) {
    val exercise = state.currentItem.exercise
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            if (state.side != Side.NONE) {
                SideBadge(state.side)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = exercise.category.displayName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun SideBadge(side: Side) {
    val label = if (side == Side.LEFT) "LEFT" else "RIGHT"
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun CueList(cues: List<String>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (cue in cues) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = "•",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(
                    text = cue,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                )
            }
        }
    }
}

@Composable
private fun Countdown(state: SessionUiState.FollowAlong) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = formatTime(state.remainingSeconds),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { state.progressFraction },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surface,
        )
    }
}

@Composable
private fun Controls(
    isPaused: Boolean,
    onTogglePause: () -> Unit,
    onSkip: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(
            onClick = onTogglePause,
            modifier = Modifier.weight(1f).height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(
                imageVector = if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                contentDescription = null,
            )
            Spacer(Modifier.padding(end = 8.dp))
            Text(if (isPaused) "Resume" else "Pause", fontWeight = FontWeight.SemiBold)
        }
        Button(
            onClick = onSkip,
            modifier = Modifier.weight(1f).height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(imageVector = Icons.Filled.SkipNext, contentDescription = null)
            Spacer(Modifier.padding(end = 8.dp))
            Text("Skip", fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun formatTime(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%d:%02d".format(m, s)
}
