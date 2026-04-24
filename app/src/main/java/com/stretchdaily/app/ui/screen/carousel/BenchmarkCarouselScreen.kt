package com.stretchdaily.app.ui.screen.carousel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import com.stretchdaily.app.ui.components.CircleButton
import com.stretchdaily.app.ui.components.CircleButtonSize
import com.stretchdaily.app.ui.components.CircleButtonVariant
import com.stretchdaily.app.ui.components.IconName
import com.stretchdaily.app.ui.components.MonoCaps
import com.stretchdaily.app.ui.components.MonoCapsSize
import com.stretchdaily.app.ui.components.SegmentProgress
import com.stretchdaily.app.ui.screen.log.LogForm
import com.stretchdaily.app.ui.theme.Theme

@Composable
fun BenchmarkCarouselScreen(
    step: Int,
    parentEntry: NavBackStackEntry,
    onAdvance: (toStep: Int) -> Unit,
    onFinish: () -> Unit,
    viewModel: BenchmarkCarouselViewModel = hiltViewModel(parentEntry),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(step) { viewModel.setStep(step) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is CarouselEvent.Advance -> onAdvance(event.toIndex)
                is CarouselEvent.Finished -> onFinish()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.bg)
            .systemBarsPadding(),
    ) {
        CarouselHeader(
            total = state.totalSteps.coerceAtLeast(1),
            currentIndex = state.currentIndex,
            categoryLabel = state.currentBenchmark?.category?.name.orEmpty(),
            onClose = viewModel::onClose,
        )

        LazyColumn(
            contentPadding = PaddingValues(
                start = 20.dp, end = 20.dp, top = 8.dp, bottom = 100.dp,
            ),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                val bm = state.currentBenchmark
                if (bm != null) {
                    Text(
                        text = bm.name,
                        style = Theme.typo.displayMd,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.W500,
                        color = Theme.colors.ink,
                        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp),
                    )
                    if (bm.description.isNotBlank()) {
                        Text(
                            text = bm.description,
                            style = Theme.typo.bodyMd,
                            fontSize = 13.sp,
                            color = Theme.colors.ink2,
                            modifier = Modifier.padding(bottom = 14.dp),
                        )
                    }
                    LogForm(
                        benchmark = bm,
                        onSubmit = { raw, tier -> viewModel.onSaveEntry(raw, tier) },
                        onCancel = viewModel::onClose,
                        errorMessage = state.errorMessage,
                    )
                }
            }
        }
    }
}

@Composable
private fun CarouselHeader(
    total: Int,
    currentIndex: Int,
    categoryLabel: String,
    onClose: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 10.dp),
    ) {
        CircleButton(
            onClick = onClose,
            icon = IconName.Close,
            contentDescription = "Close benchmark carousel",
            variant = CircleButtonVariant.Bg2,
            size = CircleButtonSize.Small,
        )
        Column(modifier = Modifier.weight(1f)) {
            SegmentProgress(total = total, currentIndex = currentIndex)
            Spacer(Modifier.height(6.dp))
            MonoCaps(
                text = "Benchmark ${currentIndex + 1} of $total · $categoryLabel",
                size = MonoCapsSize.Small,
                color = Theme.colors.ink3,
            )
        }
    }
}
