package com.stretchdaily.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.stretchdaily.app.ui.theme.Theme

/** State for a single [SegmentProgress] cell. */
enum class SegmentState { Past, Current, Future }

/**
 * Maps an index to its visual state given the current index.
 *
 * - `index < currentIndex` to [SegmentState.Past]
 * - `index == currentIndex` to [SegmentState.Current]
 * - `index > currentIndex` to [SegmentState.Future]
 *
 * When `currentIndex >= total`, everything is Past (session complete).
 * When `currentIndex < 0`, everything is Future (nothing started).
 */
fun segmentStateFor(index: Int, currentIndex: Int, total: Int): SegmentState {
    if (currentIndex < 0) return SegmentState.Future
    if (currentIndex >= total) return SegmentState.Past
    return when {
        index < currentIndex -> SegmentState.Past
        index == currentIndex -> SegmentState.Current
        else -> SegmentState.Future
    }
}

/**
 * N-segment progress rail. Each cell is 4 dp tall, radius 1.5 dp,
 * separated by 3 dp gaps.
 *
 * Colors:
 *  - Past    → `accent x 0.55`
 *  - Current → `accent`
 *  - Future  → `ink x 0.18` (raised from `line` so the rail is legible
 *              at small heights against the cream bg)
 */
@Composable
fun SegmentProgress(
    total: Int,
    currentIndex: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = modifier.fillMaxWidth().height(4.dp),
    ) {
        for (i in 0 until total) {
            val color = when (segmentStateFor(i, currentIndex, total)) {
                SegmentState.Past -> Theme.colors.accent.copy(alpha = 0.55f)
                SegmentState.Current -> Theme.colors.accent
                SegmentState.Future -> Theme.colors.ink.copy(alpha = 0.18f)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(color),
            )
        }
    }
}
