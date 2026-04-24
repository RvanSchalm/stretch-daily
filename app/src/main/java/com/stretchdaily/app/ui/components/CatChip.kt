package com.stretchdaily.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.ui.theme.Theme
import com.stretchdaily.app.ui.theme.tint

/**
 * Hairline-outlined category pill.
 *
 * Layout: 6 dp tint dot + mono-caps category label. Used as a
 * sub-identifier on session-overview rows and analytics cards.
 */
@Composable
fun CatChip(
    category: Category,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .clip(RoundedCornerShape(Theme.dims.radiusPill))
            .border(
                width = 1.dp,
                color = Theme.colors.line,
                shape = RoundedCornerShape(Theme.dims.radiusPill),
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(category.tint()),
        )
        MonoCaps(
            text = category.displayName,
            size = MonoCapsSize.Small,
            color = Theme.colors.ink2,
        )
    }
}
