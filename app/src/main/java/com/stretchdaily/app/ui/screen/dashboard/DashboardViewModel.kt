package com.stretchdaily.app.ui.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stretchdaily.app.core.datastore.SettingsDataStore
import com.stretchdaily.app.core.engine.CategoryWeightCalculator
import com.stretchdaily.app.core.model.Benchmark
import com.stretchdaily.app.core.session.TodaySession
import com.stretchdaily.app.core.session.TodaySessionHolder
import com.stretchdaily.app.core.util.Clock
import com.stretchdaily.app.data.BenchmarkRepository
import com.stretchdaily.app.data.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Observes today's plan + live repository flows + the banner preference
 * and emits a composed [DashboardUiState]. Fire-and-forget
 * [TodaySessionHolder.ensureFresh] runs on init so the state flips away
 * from `isLoading` the moment the engine returns.
 *
 * [categoryWeightCalculator] is held for a future enhancement where the
 * dashboard may display the raw weights (per spec §4, "Extra focus")
 * instead of deriving them from the selected plan — kept as a constructor
 * dep today so that refactor is trivial.
 */
@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val benchmarkRepository: BenchmarkRepository,
    private val settingsDataStore: SettingsDataStore,
    private val todaySessionHolder: TodaySessionHolder,
    @Suppress("unused") private val categoryWeightCalculator: CategoryWeightCalculator,
    private val clock: Clock,
) : ViewModel() {

    private val zoneId: ZoneId = ZoneId.systemDefault()

    /**
     * Seven inputs composed once; the screen only collects a single
     * `StateFlow<DashboardUiState>`. Kotlin's typed `combine` overloads
     * top out at 5 sources — using 7 drops to the `vararg` overload that
     * returns `Array<Any?>`, so we unpack by index with a UNCHECKED_CAST
     * suppression. Ugly but safe; the ViewModel test pins the contract.
     */
    @Suppress("UNCHECKED_CAST")
    private val composed: Flow<DashboardUiState> = combine(
        todaySessionHolder.state,
        sessionRepository.streakFlow(zoneId),
        sessionRepository.weeklyFlow(zoneId),
        sessionRepository.totalsFlow,
        benchmarkRepository.overdueFlow(zoneId),
        benchmarkRepository.nextDueFlow(zoneId),
        settingsDataStore.benchmarkBannerEnabled,
    ) { values ->
        val today: TodaySession? = values[0] as TodaySession?
        val streak = values[1] as Int
        val week = values[2] as Set<LocalDate>
        val totals = values[3] as SessionRepository.Totals
        val overdue = values[4] as List<Benchmark>
        val nextDue = values[5] as BenchmarkRepository.BenchmarkWithDueDate?
        val bannerEnabled = values[6] as Boolean

        val todayDate = LocalDate.ofInstant(Instant.ofEpochMilli(clock.now()), zoneId)

        DashboardUiState(
            isLoading = today == null,
            today = todayDate,
            streakDays = streak,
            weekCompleted = week,
            plannedExercises = today?.plan?.items.orEmpty(),
            plannedMinutes = (today?.plan?.totalSeconds ?: 0) / 60,
            extraFocus = today?.plan?.categoryWeights
                ?.maxByOrNull { it.value }?.key,
            banner = if (bannerEnabled && overdue.isNotEmpty()) {
                BannerState.Visible(
                    overdueCount = overdue.size,
                    nextBenchmark = nextDue?.benchmark,
                )
            } else BannerState.Hidden,
            kpis = Kpis(
                streakDays = streak,
                totalMinutes = totals.totalMinutes,
                totalSessions = totals.sessions,
                nextBenchmarkDays = nextDue?.daysUntilDue,
            ),
        )
    }

    val state: StateFlow<DashboardUiState> = composed.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = DashboardUiState(isLoading = true),
    )

    init {
        viewModelScope.launch { todaySessionHolder.ensureFresh(zoneId) }
    }
}
