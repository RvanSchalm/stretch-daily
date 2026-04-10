package com.stretchdaily.app.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stretchdaily.app.core.datastore.SettingsDataStore
import com.stretchdaily.app.data.DataPortRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * One-shot status the [SettingsScreen] can surface to the user via a snackbar.
 * `Idle` means there's nothing to show; everything else gets displayed once
 * and then [SettingsViewModel.consumeStatus] resets it.
 */
sealed interface SettingsStatus {
    data object Idle : SettingsStatus
    data class Working(val message: String) : SettingsStatus
    data class Success(val message: String) : SettingsStatus
    data class Error(val message: String) : SettingsStatus
}

/**
 * Backs the Settings tab. Combines:
 *
 * - The audio cues preference (read/write through [SettingsDataStore]).
 * - The three data-port actions (export / import / delete all) routed through
 *   [DataPortRepository], with stream IO done against URIs the user picks via
 *   the SAF document launchers in the screen layer.
 * - A small [SettingsStatus] state machine so the screen can show progress and
 *   one-shot success/error messages without managing its own coroutines.
 *
 * The Application context comes from Hilt; it's never the Activity context, so
 * holding it in the ViewModel is safe.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val settingsDataStore: SettingsDataStore,
    private val dataPortRepository: DataPortRepository,
) : ViewModel() {

    val audioCuesEnabled: StateFlow<Boolean> = settingsDataStore.audioCuesEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = true,
        )

    private val _status = MutableStateFlow<SettingsStatus>(SettingsStatus.Idle)
    val status: StateFlow<SettingsStatus> = _status.asStateFlow()

    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog: StateFlow<Boolean> = _showDeleteDialog.asStateFlow()

    fun setAudioCuesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setAudioCuesEnabled(enabled)
        }
    }

    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            _status.value = SettingsStatus.Working("Exporting…")
            try {
                val opened = appContext.contentResolver.openOutputStream(uri)
                    ?: error("Could not open file for writing")
                opened.use { stream -> dataPortRepository.exportTo(stream) }
                _status.value = SettingsStatus.Success("Export complete")
            } catch (t: Throwable) {
                _status.value = SettingsStatus.Error(t.message ?: "Export failed")
            }
        }
    }

    fun importFrom(uri: Uri) {
        viewModelScope.launch {
            _status.value = SettingsStatus.Working("Importing…")
            try {
                val opened = appContext.contentResolver.openInputStream(uri)
                    ?: error("Could not open file for reading")
                opened.use { stream -> dataPortRepository.importFrom(stream) }
                _status.value = SettingsStatus.Success("Import complete")
            } catch (t: Throwable) {
                _status.value = SettingsStatus.Error(t.message ?: "Import failed")
            }
        }
    }

    fun showDeleteDialog() {
        _showDeleteDialog.value = true
    }

    fun dismissDeleteDialog() {
        _showDeleteDialog.value = false
    }

    fun deleteAllData() {
        viewModelScope.launch {
            _showDeleteDialog.value = false
            _status.value = SettingsStatus.Working("Deleting…")
            try {
                dataPortRepository.deleteAll()
                _status.value = SettingsStatus.Success("All data deleted")
            } catch (t: Throwable) {
                _status.value = SettingsStatus.Error(t.message ?: "Delete failed")
            }
        }
    }

    fun consumeStatus() {
        _status.value = SettingsStatus.Idle
    }
}
