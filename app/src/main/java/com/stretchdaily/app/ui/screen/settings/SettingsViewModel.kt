package com.stretchdaily.app.ui.screen.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stretchdaily.app.core.database.dao.ExerciseDao
import com.stretchdaily.app.core.datastore.SettingsDataStore
import com.stretchdaily.app.data.BenchmarkRepository
import com.stretchdaily.app.data.DataPortRepository
import com.stretchdaily.app.data.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Backs the Settings tab. Composes:
 *  - [SettingsDataStore.audioCuesEnabled] (timer chimes)
 *  - [SettingsDataStore.benchmarkBannerEnabled] (Dashboard banner visibility)
 *  - Library stats from [ExerciseDao.observeAll],
 *    [BenchmarkRepository.observeAllBenchmarks], and
 *    [SessionRepository.totalsFlow]
 *  - A [SettingsStatus] one-shot state machine for export/import/delete
 *    progress
 *  - Delete-confirmation dialog visibility
 *
 * Every reactive source flows through a single `combine` into
 * [SettingsUiState] so the screen collects exactly one StateFlow.
 * Mutating actions ([showDeleteDialog], [deleteAllData], etc.) route
 * through [_ephemeral], which drives the `status` and `showDeleteDialog`
 * slices of the final state.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val settingsDataStore: SettingsDataStore,
    private val dataPortRepository: DataPortRepository,
    private val benchmarkRepository: BenchmarkRepository,
    private val sessionRepository: SessionRepository,
    private val exerciseDao: ExerciseDao,
) : ViewModel() {

    private data class Ephemeral(
        val status: SettingsStatus = SettingsStatus.Idle,
        val showDeleteDialog: Boolean = false,
    )

    private val _ephemeral = MutableStateFlow(Ephemeral())

    val state: StateFlow<SettingsUiState> = combine(
        settingsDataStore.audioCuesEnabled,
        settingsDataStore.benchmarkBannerEnabled,
        exerciseDao.observeAll(),
        benchmarkRepository.observeAllBenchmarks(),
        sessionRepository.totalsFlow,
        _ephemeral,
    ) { arr ->
        val audio = arr[0] as Boolean
        val banner = arr[1] as Boolean
        @Suppress("UNCHECKED_CAST")
        val exercises = arr[2] as List<com.stretchdaily.app.core.model.Exercise>
        @Suppress("UNCHECKED_CAST")
        val benchmarks = arr[3] as List<com.stretchdaily.app.core.model.Benchmark>
        val totals = arr[4] as SessionRepository.Totals
        val ephemeral = arr[5] as Ephemeral
        SettingsUiState(
            audioCuesEnabled = audio,
            benchmarkBannerEnabled = banner,
            libraryStats = LibraryStats(
                exerciseCount = exercises.size,
                categoryCount = exercises.map { it.category }.distinct().size,
                benchmarkCount = benchmarks.size,
                sessionsLogged = totals.sessions,
            ),
            status = ephemeral.status,
            showDeleteDialog = ephemeral.showDeleteDialog,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SettingsUiState(),
    )

    fun setAudioCuesEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setAudioCuesEnabled(enabled) }
    }

    fun setBenchmarkBannerEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setBenchmarkBannerEnabled(enabled) }
    }

    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            _ephemeral.update { it.copy(status = SettingsStatus.Working("Exporting…")) }
            try {
                val opened = appContext.contentResolver.openOutputStream(uri)
                    ?: error("Could not open file for writing")
                opened.use { stream -> dataPortRepository.exportTo(stream) }
                _ephemeral.update { it.copy(status = SettingsStatus.Success("Export complete")) }
            } catch (t: Throwable) {
                _ephemeral.update { it.copy(status = SettingsStatus.Error(t.message ?: "Export failed")) }
            }
        }
    }

    fun importFrom(uri: Uri) {
        viewModelScope.launch {
            _ephemeral.update { it.copy(status = SettingsStatus.Working("Importing…")) }
            try {
                val opened = appContext.contentResolver.openInputStream(uri)
                    ?: error("Could not open file for reading")
                opened.use { stream -> dataPortRepository.importFrom(stream) }
                _ephemeral.update { it.copy(status = SettingsStatus.Success("Import complete")) }
            } catch (t: Throwable) {
                _ephemeral.update { it.copy(status = SettingsStatus.Error(t.message ?: "Import failed")) }
            }
        }
    }

    fun showDeleteDialog() {
        _ephemeral.update { it.copy(showDeleteDialog = true) }
    }

    fun dismissDeleteDialog() {
        _ephemeral.update { it.copy(showDeleteDialog = false) }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            _ephemeral.update { it.copy(showDeleteDialog = false, status = SettingsStatus.Working("Deleting…")) }
            try {
                dataPortRepository.deleteAll()
                _ephemeral.update { it.copy(status = SettingsStatus.Success("All data deleted")) }
            } catch (t: Throwable) {
                _ephemeral.update { it.copy(status = SettingsStatus.Error(t.message ?: "Delete failed")) }
            }
        }
    }

    fun consumeStatus() {
        _ephemeral.update { it.copy(status = SettingsStatus.Idle) }
    }
}

private inline fun <T> MutableStateFlow<T>.update(transform: (T) -> T) {
    value = transform(value)
}
