package com.stretchdaily.app.ui.screen.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stretchdaily.app.ui.components.AppIcon
import com.stretchdaily.app.ui.components.IconName
import com.stretchdaily.app.ui.components.MonoCaps
import com.stretchdaily.app.ui.components.MonoCapsSize
import com.stretchdaily.app.ui.theme.Theme

@Composable
fun SettingsScreen(
    contentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> if (uri != null) viewModel.exportTo(uri) }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) viewModel.importFrom(uri) }

    LaunchedEffect(state.status) {
        when (val s = state.status) {
            is SettingsStatus.Working -> { snackbarHostState.showSnackbar(s.message) }
            is SettingsStatus.Success -> { snackbarHostState.showSnackbar(s.message); viewModel.consumeStatus() }
            is SettingsStatus.Error -> { snackbarHostState.showSnackbar(s.message); viewModel.consumeStatus() }
            SettingsStatus.Idle -> { }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Theme.colors.bg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = contentPadding.calculateBottomPadding() + 100.dp),
        ) {
            Spacer(modifier = Modifier.height(14.dp))
            MonoCaps(text = "Settings", size = MonoCapsSize.Small, color = Theme.colors.ink3)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Your app, offline.",
                modifier = Modifier.semantics { heading() },
                color = Theme.colors.ink,
                fontSize = 30.sp,
                fontWeight = FontWeight.W500,
                fontFamily = Theme.typo.display,
            )
            Spacer(modifier = Modifier.height(18.dp))

            SettingGroup(label = "Preferences") {
                SettingToggle(
                    label = "Timer sound effects",
                    description = "Chimes at exercise transitions",
                    iconName = IconName.Sound,
                    value = state.audioCuesEnabled,
                    onChange = viewModel::setAudioCuesEnabled,
                )
                RowDivider()
                SettingToggle(
                    label = "Benchmark day reminder",
                    description = "Show a dashboard banner when benchmarks are due",
                    iconName = IconName.Sparkle,
                    value = state.benchmarkBannerEnabled,
                    onChange = viewModel::setBenchmarkBannerEnabled,
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            SettingGroup(label = "Your data") {
                SettingRow(
                    iconName = IconName.Download,
                    label = "Export data",
                    description = "Save a .json backup of sessions and benchmarks",
                    onClick = {
                        val filename = "stretch-daily-${System.currentTimeMillis()}.json"
                        exportLauncher.launch(filename)
                    },
                )
                RowDivider()
                SettingRow(
                    iconName = IconName.Upload,
                    label = "Import data",
                    description = "Restore from a previous export",
                    onClick = { importLauncher.launch(arrayOf("application/json")) },
                )
                RowDivider()
                SettingRow(
                    iconName = IconName.Trash,
                    label = "Delete all data",
                    description = "Clear everything on this device",
                    destructive = true,
                    onClick = viewModel::showDeleteDialog,
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            SettingGroup(label = "Library") {
                ReadonlyRow(
                    label = "Exercises",
                    value = "${state.libraryStats.exerciseCount} across ${state.libraryStats.categoryCount} categories",
                )
                RowDivider()
                ReadonlyRow(
                    label = "Benchmarks",
                    value = "${state.libraryStats.benchmarkCount} tests",
                )
                RowDivider()
                ReadonlyRow(
                    label = "Sessions logged",
                    value = "${state.libraryStats.sessionsLogged} total",
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            MonoCaps(
                text = "Stretch Daily · v3 · local only",
                size = MonoCapsSize.Small,
                color = Theme.colors.ink3,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(10.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
                .padding(bottom = contentPadding.calculateBottomPadding() + 24.dp),
        )

        if (state.showDeleteDialog) {
            AlertDialog(
                onDismissRequest = viewModel::dismissDeleteDialog,
                title = { Text("Delete all data?") },
                text = { Text("All sessions and benchmark logs will be erased. The catalog will be re-seeded. This cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = viewModel::deleteAllData) {
                        Text("Delete", color = Theme.colors.warn)
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissDeleteDialog) {
                        Text("Cancel")
                    }
                },
                containerColor = Theme.colors.surface,
                titleContentColor = Theme.colors.ink,
                textContentColor = Theme.colors.ink2,
            )
        }
    }
}

@Composable
private fun SettingGroup(label: String, content: @Composable () -> Unit) {
    Column {
        MonoCaps(
            text = label,
            size = MonoCapsSize.Small,
            color = Theme.colors.ink3,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Theme.dims.radiusMd))
                .background(Theme.colors.surface),
        ) {
            content()
        }
    }
}

@Composable
private fun SettingToggle(
    label: String,
    description: String,
    iconName: IconName,
    value: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!value) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppIcon(
            name = iconName,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = Theme.colors.ink2,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, color = Theme.colors.ink, fontSize = 13.sp, fontWeight = FontWeight.W600, fontFamily = Theme.typo.body)
            Spacer(modifier = Modifier.height(1.dp))
            Text(text = description, color = Theme.colors.ink3, fontSize = 11.sp, fontFamily = Theme.typo.body)
        }
        ToggleSwitch(value = value, onChange = onChange)
    }
}

@Composable
private fun ToggleSwitch(value: Boolean, onChange: (Boolean) -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 38.dp, height = 22.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(if (value) Theme.colors.accent else Theme.colors.line)
            .clickable { onChange(!value) },
        contentAlignment = Alignment.CenterStart,
    ) {
        val thumbOffset = if (value) 18.dp else 2.dp
        Box(
            modifier = Modifier
                .padding(start = thumbOffset)
                .size(18.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(androidx.compose.ui.graphics.Color.White),
        )
    }
}

@Composable
private fun SettingRow(
    iconName: IconName,
    label: String,
    description: String,
    onClick: () -> Unit,
    destructive: Boolean = false,
) {
    val textColor = if (destructive) Theme.colors.warn else Theme.colors.ink
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppIcon(
            name = iconName,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = if (destructive) Theme.colors.warn else Theme.colors.ink2,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.W600, fontFamily = Theme.typo.body)
            Spacer(modifier = Modifier.height(1.dp))
            Text(text = description, color = Theme.colors.ink3, fontSize = 11.sp, fontFamily = Theme.typo.body)
        }
        AppIcon(
            name = IconName.ChevronRight,
            contentDescription = "Open",
            modifier = Modifier.size(14.dp),
            tint = Theme.colors.ink3,
        )
    }
}

@Composable
private fun ReadonlyRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, color = Theme.colors.ink, fontSize = 13.sp, fontWeight = FontWeight.W500, fontFamily = Theme.typo.body)
        Spacer(modifier = Modifier.weight(1f))
        Text(text = value, color = Theme.colors.ink3, fontSize = 12.sp, fontFamily = Theme.typo.body)
    }
}

@Composable
private fun RowDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Theme.colors.line2),
    )
}
