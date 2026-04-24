package com.stretchdaily.app.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.stretchdaily.app.ui.theme.Theme
import java.util.Locale

/** Size variant for [MonoCaps]. `Regular` = 11 sp, `Small` = 9.5 sp. */
enum class MonoCapsSize { Regular, Small }

/**
 * The design's "voice mark" - JetBrains Mono, uppercase, letter-spaced.
 *
 * Used for eyebrows, KPI labels, chip labels, session-player state
 * captions, and nav tab labels. Consumers pass mixed-case strings; this
 * composable applies `uppercase(Locale.getDefault())`.
 */
@Composable
fun MonoCaps(
    text: String,
    size: MonoCapsSize = MonoCapsSize.Regular,
    color: Color = Theme.colors.ink3,
    modifier: Modifier = Modifier,
) {
    val style: TextStyle = when (size) {
        MonoCapsSize.Regular -> Theme.typo.monoCaps
        MonoCapsSize.Small -> Theme.typo.monoCapsSm
    }
    Text(
        text = text.uppercase(Locale.getDefault()),
        style = style,
        color = color,
        modifier = modifier,
    )
}
