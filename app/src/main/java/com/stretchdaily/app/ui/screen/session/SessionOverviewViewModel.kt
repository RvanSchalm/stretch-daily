package com.stretchdaily.app.ui.screen.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stretchdaily.app.core.database.dao.ExerciseDao
import com.stretchdaily.app.core.session.TodaySessionHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SessionOverviewViewModel @Inject constructor(
    private val holder: TodaySessionHolder,
    private val exerciseDao: ExerciseDao,
) : ViewModel() {

    private val swapState = MutableStateFlow<SwapSheetState>(SwapSheetState.Hidden)

    val state: StateFlow<SessionOverviewUiState> = combine(
        holder.state,
        swapState,
    ) { today, swap ->
        if (today == null) {
            SessionOverviewUiState(isLoading = true)
        } else {
            SessionOverviewUiState(
                isLoading = false,
                planMinutes = today.plan.totalSeconds / 60,
                items = today.plan.items,
                swap = swap,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SessionOverviewUiState(isLoading = true),
    )

    init {
        viewModelScope.launch { holder.ensureFresh() }
    }

    fun onExerciseTap(exerciseId: String) {
        val today = holder.state.value ?: return
        val item = today.plan.items.firstOrNull { it.exercise.id == exerciseId } ?: return
        viewModelScope.launch {
            val usedIds = today.plan.items.map { it.exercise.id }.toSet()
            val candidates = exerciseDao.getByCategory(item.exercise.category)
                .filter { it.id !in usedIds }
            swapState.value = SwapSheetState.Visible(
                oldItem = item,
                candidates = candidates,
            )
        }
    }

    fun onSwapSelect(newExerciseId: String) {
        val visible = swapState.value as? SwapSheetState.Visible ?: return
        val oldId = visible.oldItem.exercise.id
        viewModelScope.launch {
            holder.swap(oldId, newExerciseId)
            swapState.value = SwapSheetState.Hidden
        }
    }

    fun onDismissSwap() {
        swapState.value = SwapSheetState.Hidden
    }
}
