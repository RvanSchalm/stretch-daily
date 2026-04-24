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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
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
 *  - Dashed 2 dp `accent` outline if [DayState.Today] (and not completed).
 *  - Solid 1 dp `line` outline if [DayState.Idle].
 *
 * Tiny weekday label above each square in mono-caps small. `Modifier.border`
 * only supports solid strokes, so the "today" state draws its dashed accent
 * outline via `Modifier.drawBehind` + `Stroke(pathEffect = dashPathEffect(...))`.
 * Same pattern as `Pill.DashedOutline`.
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
                                DayState.Today -> {
                                    val strokeColor = Theme.colors.accent
                                    Modifier.drawBehind {
                                        val stroke = 2.dp.toPx()
                                        val inset = stroke / 2f
                                        val corner = (16.dp.toPx() - inset).coerceAtLeast(0f)
                                        drawRoundRect(
                                            color = strokeColor,
                                            topLeft = Offset(inset, inset),
                                            size = Size(size.width - stroke, size.height - stroke),
                                            cornerRadius = CornerRadius(corner, corner),
                                            style = Stroke(
                                                width = stroke,
                                                pathEffect = PathEffect.dashPathEffect(
                                                    floatArrayOf(6f, 4f),
                                                    0f,
                                                ),
                                            ),
                                        )
                                    }
                                }
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
