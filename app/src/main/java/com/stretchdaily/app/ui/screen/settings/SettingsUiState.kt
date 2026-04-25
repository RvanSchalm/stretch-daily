package com.stretchdaily.app.ui.screen.settings

/**
 * Settings tab UI state. Streams from preferences + repository flows.
 * [status] is the one-shot banner surface for export/import/delete
 * progress and results — see [SettingsStatus].
 */
data class SettingsUiState(
    val audioCuesEnabled: Boolean = true,
    val benchmarkBannerEnabled: Boolean = false,
    val libraryStats: LibraryStats = LibraryStats(),
    val status: SettingsStatus = SettingsStatus.Idle,
    val showDeleteDialog: Boolean = false,
)

data class LibraryStats(
    val exerciseCount: Int = 0,
    val categoryCount: Int = 0,
    val benchmarkCount: Int = 0,
    val sessionsLogged: Int = 0,
)

/**
 * One-shot status the screen surfaces via a snackbar.
 * `Idle` = nothing to show; other states display once and
 * [SettingsViewModel.consumeStatus] resets it.
 */
sealed interface SettingsStatus {
    data object Idle : SettingsStatus
    data class Working(val message: String) : SettingsStatus
    data class Success(val message: String) : SettingsStatus
    data class Error(val message: String) : SettingsStatus
}
