package com.stretchdaily.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.stretchdaily.app.ui.theme.Theme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Visual state for a single day in [WeekStrip]. */
enum class DayState { Completed, Today, Idle }

/** Returns the 7 dates Monday to Sunday for the ISO week containing [day]. */
fun weekOf(day: LocalDate): List<LocalDate> {
    val monday = day.minusDays((day.dayOfWeek.value - 1).toLong())
    return (0..6L).map { monday.plusDays(it) }
}

/** Resolves the visual state for [day] given [today] and the completed-set. */
fun dayStateFor(
    day: LocalDate,
    today: LocalDate,
    completed: Set<LocalDate>,
): DayState = when {
    day in completed -> DayState.Completed
    day == today -> DayState.Today
    else -> DayState.Idle
}

/**
 * 7-square week strip, Monday-start. Each square:
 *  - Filled `accent` if [DayState.Completed].
 *  - 2 dp `accent` outline if [DayState.Today] (and not completed).
 *  - 1 dp `line` outline if [DayState.Idle].
 *
 * Tiny weekday label above each square in mono-caps small. Compose
 * `Modifier.border` renders solid strokes only; the "today" state uses
 * a solid 2 dp accent outline. Swap to `Modifier.drawBehind` with
 * `PathEffect.dashPathEffect(...)` if a dashed look is required later.
 */
@Composable
fun WeekStrip(
    today: LocalDate,
    completed: Set<LocalDate>,
    modifier: Modifier = Modifier,
) {
    val days = weekOf(today)

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        days.forEach { day ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f),
            ) {
                MonoCaps(
                    text = day.dayOfWeek.narrowLabel(),
                    size = MonoCapsSize.Small,
                    color = Theme.colors.ink3,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .then(
                            when (dayStateFor(day, today, completed)) {
                                DayState.Completed -> Modifier.background(Theme.colors.accent)
                                DayState.Today -> Modifier.border(
                                    width = 2.dp,
                                    color = Theme.colors.accent,
                                    shape = RoundedCornerShape(16.dp),
                                )
                                DayState.Idle -> Modifier.border(
                                    width = 1.dp,
                                    color = Theme.colors.line,
                                    shape = RoundedCornerShape(16.dp),
                                )
                            }
                        ),
                )
            }
        }
    }
}

/** Single-char narrow weekday (M T W T F S S). */
private fun DayOfWeek.narrowLabel(): String =
    this.getDisplayName(TextStyle.NARROW, Locale.getDefault())
