package com.stretchdaily.app.ui.screen.session

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stretchdaily.app.core.engine.model.PlannedExercise
import com.stretchdaily.app.core.model.Exercise
import com.stretchdaily.app.ui.components.AppIcon
import com.stretchdaily.app.ui.components.CatChip
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
import com.stretchdaily.app.ui.components.Sheet
import com.stretchdaily.app.ui.theme.Theme
import com.stretchdaily.app.ui.theme.tint

@Composable
fun SessionOverviewScreen(
    onBeginSession: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val vm: SessionOverviewViewModel = hiltViewModel()
    val state by vm.state.collectAsState()

    Box(modifier = modifier.fillMaxSize().background(Theme.colors.bg)) {
        LazyColumn(
            // Tab screen inside the bottom-nav Scaffold, which sets
            // contentWindowInsets = WindowInsets(0) — so each tab handles
            // its own status bar. Bottom is covered by `contentPadding`
            // coming in from the Scaffold.
            modifier = Modifier.statusBarsPadding(),
            contentPadding = mergePadding(Theme.dims.padScreen, contentPadding),
            verticalArrangement = Arrangement.spacedBy(Theme.dims.gapSection),
        ) {
            item {
                Column {
                    MonoCaps(
                        text = "TODAY'S SESSION",
                        color = Theme.colors.ink3,
                        size = MonoCapsSize.Regular,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${state.planMinutes} min",
                            style = Theme.typo.displayMd,
                            color = Theme.colors.ink,
                        )
                        Spacer(modifier = Modifier.padding(horizontal = 6.dp))
                        Text(
                            text = "· ${state.items.size} moves",
                            style = Theme.typo.bodyLg,
                            color = Theme.colors.ink3,
                        )
                    }
                }
            }
            item { CategorySpreadBar(state.items) }
            items(state.items, key = { it.exercise.id }) { item ->
                ExerciseRow(
                    index = state.items.indexOf(item) + 1,
                    item = item,
                    onSwap = { vm.onExerciseTap(item.exercise.id) },
                )
            }
            item {
                Pill(
                    onClick = onBeginSession,
                    label = "Begin session",
                    variant = PillVariant.Accent,
                    leadingIcon = IconName.Play,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Swap sheet — Sheet has no `visible` param in this codebase, so we
        // render conditionally. Smart-cast on `swap` keeps the inner access clean.
        val swap = state.swap
        if (swap is SwapSheetState.Visible) {
            Sheet(onDismiss = vm::onDismissSwap) {
                SwapSheetContent(
                    oldItem = swap.oldItem,
                    candidates = swap.candidates,
                    onSelect = { vm.onSwapSelect(it) },
                )
            }
        }
    }
}

@Composable
private fun CategorySpreadBar(items: List<PlannedExercise>) {
    if (items.isEmpty()) return
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.fillMaxWidth().height(5.dp),
    ) {
        items.forEach { item ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(item.exercise.category.tint()),
            )
        }
    }
}

@Composable
private fun ExerciseRow(index: Int, item: PlannedExercise, onSwap: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ExerciseTile(
            category = item.exercise.category,
            size = ExerciseTileSize.Small56,
        )
        Spacer(modifier = Modifier.padding(horizontal = 6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                MonoCaps(
                    text = "%02d".format(index),
                    color = Theme.colors.ink3,
                    size = MonoCapsSize.Small,
                )
                Spacer(modifier = Modifier.padding(horizontal = 6.dp))
                Text(
                    text = item.exercise.name,
                    style = Theme.typo.bodyLg.copy(fontWeight = FontWeight.SemiBold),
                    color = Theme.colors.ink,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CatChip(category = item.exercise.category)
                Spacer(modifier = Modifier.padding(horizontal = 6.dp))
                MonoCaps(
                    text = targetLabel(item),
                    color = Theme.colors.ink3,
                    size = MonoCapsSize.Small,
                )
            }
        }
        CircleButton(
            onClick = onSwap,
            icon = IconName.Swap,
            size = CircleButtonSize.Medium,
            variant = CircleButtonVariant.Bg2,
            contentDescription = "Swap ${item.exercise.name}",
        )
    }
}

private fun targetLabel(item: PlannedExercise): String {
    val ex = item.exercise
    return when {
        ex.isTimed -> "${item.effectiveSeconds}s" + if (ex.isUnilateral) " · L/R" else ""
        ex.targetReps != null -> "${ex.targetReps} reps" + if (ex.isUnilateral) " · L/R" else ""
        else -> "${item.effectiveSeconds}s"
    }
}

@Composable
private fun SwapSheetContent(
    oldItem: PlannedExercise,
    candidates: List<Exercise>,
    onSelect: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MonoCaps(
            text = "SWAP \"${oldItem.exercise.name}\"",
            color = Theme.colors.ink3,
            size = MonoCapsSize.Regular,
        )
        candidates.forEach { candidate ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(candidate.id) }
                    .padding(vertical = 6.dp),
            ) {
                ExerciseTile(
                    category = candidate.category,
                    size = ExerciseTileSize.Small56,
                )
                Spacer(modifier = Modifier.padding(horizontal = 10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = candidate.name,
                        style = Theme.typo.bodyLg.copy(fontWeight = FontWeight.SemiBold),
                        color = Theme.colors.ink,
                    )
                    MonoCaps(
                        text = candidateSubtitle(candidate),
                        color = Theme.colors.ink3,
                        size = MonoCapsSize.Small,
                    )
                }
                AppIcon(
                    name = IconName.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Theme.colors.ink3,
                )
            }
        }
    }
}

private fun candidateSubtitle(ex: Exercise): String = when {
    ex.isTimed -> "${ex.totalTime}s" + if (ex.isUnilateral) " · L/R" else ""
    ex.targetReps != null -> "${ex.targetReps} reps" + if (ex.isUnilateral) " · L/R" else ""
    else -> "${ex.totalTime}s"
}

private fun mergePadding(inner: PaddingValues, outer: PaddingValues): PaddingValues =
    PaddingValues(
        start = inner.calculateStartPadding(LayoutDirection.Ltr),
        end = inner.calculateEndPadding(LayoutDirection.Ltr),
        top = maxOf(inner.calculateTopPadding(), outer.calculateTopPadding()),
        bottom = maxOf(inner.calculateBottomPadding(), outer.calculateBottomPadding()),
    )
