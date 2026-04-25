package com.stretchdaily.app.ui.screen.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.ui.components.MonoCaps
import com.stretchdaily.app.ui.components.MonoCapsSize
import com.stretchdaily.app.ui.theme.Theme

@Composable
fun AnalyticsScreen(
    contentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: AnalyticsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.bg)
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(
            top = 6.dp,
            bottom = contentPadding.calculateBottomPadding() + 100.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            AnalyticsHeader()
        }
        item {
            CategoryFilterRow(
                all = state.allCategories,
                selected = state.filter,
                onSelect = viewModel::onFilterChanged,
            )
        }
        items(
            items = state.visibleCards,
            key = { it.benchmark.id },
        ) { card ->
            BenchmarkAnalyticsCard(state = card)
        }
    }
}

@Composable
private fun AnalyticsHeader() {
    Column(modifier = Modifier.padding(top = 14.dp, bottom = 0.dp)) {
        MonoCaps(
            text = "Progress",
            size = MonoCapsSize.Small,
            color = Theme.colors.ink3,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Six months in.",
            modifier = Modifier.semantics { heading() },
            color = Theme.colors.ink,
            fontSize = 30.sp,
            fontWeight = FontWeight.W500,
            fontFamily = Theme.typo.displayLg.fontFamily,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Each chart is one benchmark. The colored strip marks its category.",
            color = Theme.colors.ink2,
            fontSize = 13.sp,
            fontFamily = Theme.typo.bodyLg.fontFamily,
        )
    }
}

@Composable
private fun CategoryFilterRow(
    all: List<Category>,
    selected: Category?,
    onSelect: (Category?) -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item(key = "all") {
            FilterPill(
                label = "All",
                selected = selected == null,
                onClick = { onSelect(null) },
            )
        }
        items(items = all, key = { it.name }) { category ->
            FilterPill(
                label = category.displayName,
                selected = selected == category,
                onClick = { onSelect(category) },
            )
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (selected) Theme.colors.accent else Theme.colors.bg2
    val fg = if (selected) Theme.colors.accentInk else Theme.colors.ink2
    Row(
        modifier = Modifier
            .background(bg, RoundedCornerShape(Theme.dims.radiusPill))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonoCaps(
            text = label,
            size = MonoCapsSize.Small,
            color = fg,
        )
    }
}
