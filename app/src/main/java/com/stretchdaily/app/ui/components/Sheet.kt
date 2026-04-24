package com.stretchdaily.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stretchdaily.app.ui.theme.Theme

/**
 * Bottom sheet for swap pickers, log forms, and confirmations.
 *
 * Wraps Material 3 [ModalBottomSheet] with Sage token overrides:
 *  - `containerColor` = `Theme.colors.bg`
 *  - `contentColor` = `Theme.colors.ink`
 *  - `shape` = top-only `radiusLg`
 *
 * Content is wrapped in 20 dp horizontal + 20 dp vertical padding so
 * call sites only provide the inner column.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Sheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = Theme.colors.bg,
        contentColor = Theme.colors.ink,
        shape = RoundedCornerShape(
            topStart = Theme.dims.radiusLg,
            topEnd = Theme.dims.radiusLg,
        ),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            content()
        }
    }
}
