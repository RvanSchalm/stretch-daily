package com.stretchdaily.app.ui.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.core.model.FlexibilityTier
import com.stretchdaily.app.ui.components.AppIcon
import com.stretchdaily.app.ui.components.BandPill
import com.stretchdaily.app.ui.components.CatChip
import com.stretchdaily.app.ui.components.CircleButton
import com.stretchdaily.app.ui.components.CircleButtonSize
import com.stretchdaily.app.ui.components.CircleButtonVariant
import com.stretchdaily.app.ui.components.ExerciseTile
import com.stretchdaily.app.ui.components.ExerciseTileSize
import com.stretchdaily.app.ui.components.IconName
import com.stretchdaily.app.ui.components.KpiCard
import com.stretchdaily.app.ui.components.MonoCaps
import com.stretchdaily.app.ui.components.MonoCapsSize
import com.stretchdaily.app.ui.components.Pill
import com.stretchdaily.app.ui.components.PillVariant
import com.stretchdaily.app.ui.components.SegmentProgress
import com.stretchdaily.app.ui.components.Sparkline
import com.stretchdaily.app.ui.components.WeekStrip
import com.stretchdaily.app.ui.theme.Theme
import java.time.LocalDate

/**
 * Debug-only gallery. Renders every R2 primitive in a scrollable Column
 * so they can be visually compared against `reference/index.html`.
 *
 * Not reachable from the main UI in release builds - wired to a hidden
 * route from the NavHost, entered via a long-press on the Settings tab
 * placeholder (R2 only; removed once Settings ships in R6).
 */
@Composable
fun ComponentGalleryScreen(
    contentPadding: PaddingValues,
    onClose: () -> Unit,
) {
    val scroll = rememberScrollState()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.bg)
            .padding(contentPadding),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(scroll)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            MonoCaps(text = "Component gallery (debug)")

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                MonoCaps(text = "MonoCaps regular", size = MonoCapsSize.Regular)
                MonoCaps(text = "MonoCaps small", size = MonoCapsSize.Small)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(
                    onClick = {},
                    label = "Begin session",
                    leadingIcon = IconName.Play,
                    variant = PillVariant.Accent,
                )
                Pill(
                    onClick = {},
                    label = "Swap exercise",
                    leadingIcon = IconName.Swap,
                    variant = PillVariant.Neutral,
                )
                Pill(
                    onClick = {},
                    label = "Perform at your own pace",
                    variant = PillVariant.DashedOutline,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleButton(
                    onClick = {},
                    icon = IconName.Close,
                    contentDescription = "Close",
                    variant = CircleButtonVariant.Bg2,
                )
                CircleButton(
                    onClick = {},
                    icon = IconName.SkipNext,
                    contentDescription = "Next",
                    variant = CircleButtonVariant.Ink,
                )
                CircleButton(
                    onClick = {},
                    icon = IconName.Play,
                    contentDescription = "Begin",
                    size = CircleButtonSize.Large58,
                    variant = CircleButtonVariant.Accent,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ExerciseTile(category = Category.SPINE, size = ExerciseTileSize.Small56)
                ExerciseTile(category = Category.ANKLES, size = ExerciseTileSize.Small56)
            }
            ExerciseTile(category = Category.HIPS, size = ExerciseTileSize.Large4x3)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CatChip(category = Category.NECK)
                CatChip(category = Category.HIPS)
                CatChip(category = Category.WRISTS)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FlexibilityTier.entries.forEach { BandPill(tier = it) }
            }

            Sparkline(values = listOf(0.2, 0.35, 0.3, 0.5, 0.7, 0.9))
            Sparkline(values = emptyList())

            SegmentProgress(total = 8, currentIndex = 3)
            SegmentProgress(total = 10, currentIndex = 0)

            WeekStrip(
                today = LocalDate.now(),
                completed = setOf(LocalDate.now().minusDays(1), LocalDate.now().minusDays(3)),
                modifier = Modifier.fillMaxWidth(),
            )

            KpiCard(eyebrow = "Streak", icon = IconName.Flame, value = "14", suffix = "days")
            KpiCard(eyebrow = "Next benchmark", icon = IconName.Sparkle, value = "4", suffix = "d")

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconName.entries.forEach {
                    AppIcon(name = it, contentDescription = null, tint = Theme.colors.ink2)
                }
            }

            Pill(onClick = onClose, label = "Close gallery", variant = PillVariant.Neutral)
        }
    }
}
