package com.stretchdaily.app.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stretchdaily.app.core.model.SessionRecord
import com.stretchdaily.app.data.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** State rendered by [SessionHistoryScreen]. */
sealed interface SessionHistoryUiState {
    data object Loading : SessionHistoryUiState
    data object Empty : SessionHistoryUiState
    data class Loaded(val records: List<SessionRecord>) : SessionHistoryUiState
}

/**
 * Streams the user's completed sessions newest-first. Sits on top of
 * [SessionRepository.observeAllSessions] so the list updates automatically
 * when a fresh session lands.
 */
@HiltViewModel
class SessionHistoryViewModel @Inject constructor(
    repository: SessionRepository,
) : ViewModel() {

    val state: StateFlow<SessionHistoryUiState> = repository
        .observeAllSessions()
        .map { records ->
            if (records.isEmpty()) {
                SessionHistoryUiState.Empty
            } else {
                SessionHistoryUiState.Loaded(records)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SessionHistoryUiState.Loading,
        )
}
