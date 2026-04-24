# Sage redesign — Phase R4: Session flow (overview + player + complete) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fill the three routes inside the `"session"` nested nav graph with real screens backed by the preserved `SessionAudioPlayer`, `SessionRepository`, and new `TodaySessionHolder`. Wire the Dashboard CTA from R3 into a working end-to-end session flow: overview → player → complete → back to Dashboard.

**Architecture:**

- **Overview** (`"session/overview"`, bottom bar visible) — screen-scoped `SessionOverviewViewModel` reads `TodaySessionHolder.state` and exposes the swap sheet. Tapping an alternative calls `TodaySessionHolder.swap()`; the emitted new plan drives recomposition of both the overview list and the Dashboard preview bars.
- **Player** (`"session/player"`, overlay, bottom bar hidden) — nested-graph-scoped `SessionPlayerViewModel` owns the 1 Hz timer, L/R cycling for unilateral moves, audio cues via `SessionAudioPlayer`, and writes the `SessionRecord` on completion. Uses `hiltViewModel(parentEntry)` so state persists across `player → complete` navigation without a re-instantiation.
- **Complete** (`"session/complete"`, overlay) — reads the completion summary from the same player VM (via `parentEntry` scoping). "Back to today" CTA pops the session graph and returns to the Dashboard.

**Tech Stack:** Kotlin 2.0, Compose, Hilt, Compose Navigation nested graphs, kotlinx.coroutines (delay-based timer), Material 3 `ModalBottomSheet` (via R2's `Sheet` primitive), JUnit 4 + mockk.

**Spec:** [`docs/superpowers/specs/2026-04-23-sage-redesign-design.md`](../specs/2026-04-23-sage-redesign-design.md) §3.2, §6, §7.1, §7.3, §8.2–8.4, §10.2, §11.4.
**Handoff reference:** [`docs/design_handoff_stretch_daily_v3/reference/screens.jsx`](../../design_handoff_stretch_daily_v3/reference/screens.jsx) `SessionOverviewScreen` + [`reference/session.jsx`](../../design_handoff_stretch_daily_v3/reference/session.jsx) `SessionPlayer` / `SessionDoneScreen`.

**Build verification:** Gradle CLI blocked on this machine — verify every commit via Android Studio → Build → Make Project. See [`CLAUDE.md §2 "Known issues"`](../../../CLAUDE.md).

**Prerequisite:** R3 is merged to `development`. `TodaySessionHolder` + Dashboard are available.

---

## Size guard — optional R4a / R4b split

Per spec §11.4, if this phase grows past ~1 week of real work, split at **Task 8**:

- **R4a (overview + complete only):** Tasks 1–5 + 11 + 12 (skipping player wiring). The CTA on overview navigates to a temporary `"session/player"` placeholder; the placeholder's own CTA navigates to `"session/complete"` with stub data, so the complete screen can be built and QA'd. Commit as `redesign/r4a-overview-and-complete`.
- **R4b (player + completion path):** Tasks 6–10 + real repository write + the NavHost switch. Branch `redesign/r4b-session-player`.

This document covers the full R4 as one unit. If you split, cherry-pick tasks onto two branches using the same commit messages.

---

## File structure for R4

**New under `app/src/main/java/com/stretchdaily/app/ui/screen/session/`:**
- `SessionOverviewUiState.kt` — overview's rendered state.
- `SessionOverviewViewModel.kt` — reads holder, handles swap.
- `SessionOverviewScreen.kt` — list + swap sheet + Begin CTA.
- `SessionPlayerUiState.kt` — sealed player states (Idle/Running/Paused/Complete).
- `SessionPlayerViewModel.kt` — nested-graph scope; timer + L/R cycling + completion.
- `SessionPlayerScreen.kt` — full-screen player UI.
- `SessionCompleteScreen.kt` — full-bleed accent; shares VM with player via parentEntry.

**New tests under `app/src/test/java/com/stretchdaily/app/ui/screen/session/`:**
- `SessionOverviewViewModelTest.kt` — swap filter + overview state composition.
- `SessionPlayerViewModelTest.kt` — ~10 cases covering tick, pause, skip, L/R, completion.

**Modified:**
- `app/src/main/java/com/stretchdaily/app/ui/navigation/StretchDailyNavHost.kt` — replace the placeholders for all three session routes; set up nested graph's `hiltViewModel(parentEntry)` scope.

**No changes to:**
- `core/session/TodaySessionHolder.kt` (API already final from R3).
- `core/audio/SessionAudioPlayer.kt` (behavior preserved).
- `data/SessionRepository.kt` (behavior preserved — R3 added flows; R4 calls `completeSession` which was already there).

---

## Task 1: Create the `redesign/r4-session` branch

- [ ] **Step 1: Verify clean state on `development`**

```bash
git checkout development && git pull
git status
```

Expected: clean tree with R3 merged.

- [ ] **Step 2: Create branch + push**

```bash
git checkout -b redesign/r4-session
git push -u origin redesign/r4-session
```

---

## Task 2: `SessionOverviewUiState` + `SessionOverviewViewModel` (with tests)

Overview VM reads two flows: `TodaySessionHolder.state` (for the plan) and an internal `swapTargetId` for the sheet. It exposes:
- `state: StateFlow<SessionOverviewUiState>`
- `onExerciseTap(exerciseId)` — opens the swap sheet for that exercise
- `onSwapSelect(newExerciseId)` — calls `holder.swap(oldId, newId)`, closes the sheet
- `onDismissSwap()` — closes the sheet

The "same-category candidates" list is loaded eagerly on sheet open: filter `exerciseDao.getByCategory(current.category)` to exclude exercises already in the plan.

- [ ] **Step 1: Create `SessionOverviewUiState.kt`**

```kotlin
package com.stretchdaily.app.ui.screen.session

import com.stretchdaily.app.core.engine.model.PlannedExercise
import com.stretchdaily.app.core.model.Exercise

data class SessionOverviewUiState(
    val isLoading: Boolean = true,
    val planMinutes: Int = 0,
    val items: List<PlannedExercise> = emptyList(),
    val swap: SwapSheetState = SwapSheetState.Hidden,
)

sealed interface SwapSheetState {
    data object Hidden : SwapSheetState
    data class Visible(
        val oldItem: PlannedExercise,
        val candidates: List<Exercise>,
    ) : SwapSheetState
}
```

- [ ] **Step 2: Write the VM test (red)**

Create: `app/src/test/java/com/stretchdaily/app/ui/screen/session/SessionOverviewViewModelTest.kt`

```kotlin
package com.stretchdaily.app.ui.screen.session

import com.stretchdaily.app.core.database.dao.ExerciseDao
import com.stretchdaily.app.core.engine.model.PlannedExercise
import com.stretchdaily.app.core.engine.model.SessionPlan
import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.core.model.Exercise
import com.stretchdaily.app.core.session.TodaySession
import com.stretchdaily.app.core.session.TodaySessionHolder
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionOverviewViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var holder: TodaySessionHolder
    private lateinit var exerciseDao: ExerciseDao

    private fun ex(id: String, cat: Category = Category.HIPS) = Exercise(
        id = id,
        name = id,
        category = cat,
        cues = emptyList(),
        isUnilateral = false,
        isTimed = true,
        targetReps = null,
        secondsPerRep = null,
        totalTime = 60,
    )

    private fun planItem(id: String, cat: Category = Category.HIPS) =
        PlannedExercise(ex(id, cat), effectiveSeconds = 60, isForced = false)

    private val plan = SessionPlan(
        items = listOf(planItem("H01"), planItem("H02"), planItem("N01", Category.NECK)),
        totalSeconds = 180,
        categoryWeights = emptyMap(),
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        holder = mockk(relaxed = true)
        exerciseDao = mockk(relaxed = true)
        coEvery { holder.state } returns MutableStateFlow(
            TodaySession(LocalDate.of(2026, 4, 23), plan)
        )
        coJustRun { holder.ensureFresh(any()) }
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun vm(): SessionOverviewViewModel =
        SessionOverviewViewModel(holder, exerciseDao)

    @Test
    fun `state maps holder plan to Loaded with minute total`() = runTest {
        val viewModel = vm()
        advanceUntilIdle()

        val s = viewModel.state.value
        assertEquals(3, s.items.size)
        assertEquals(3, s.planMinutes) // 180s / 60 = 3 min
        assertTrue(s.swap is SwapSheetState.Hidden)
    }

    @Test
    fun `onExerciseTap loads same-category candidates excluding plan exercises`() = runTest {
        coEvery { exerciseDao.getByCategory(Category.HIPS) } returns listOf(
            ex("H01"), ex("H02"), ex("H03"), ex("H04"),
        )

        val viewModel = vm()
        advanceUntilIdle()
        viewModel.onExerciseTap("H01")
        advanceUntilIdle()

        val s = viewModel.state.value
        assertTrue(s.swap is SwapSheetState.Visible)
        val sheet = s.swap as SwapSheetState.Visible
        assertEquals("H01", sheet.oldItem.exercise.id)
        // H01 is the current item, H02 is already in the plan → both excluded.
        assertEquals(listOf("H03", "H04"), sheet.candidates.map { it.id })
    }

    @Test
    fun `onSwapSelect delegates to holder and closes the sheet`() = runTest {
        coEvery { exerciseDao.getByCategory(Category.HIPS) } returns listOf(
            ex("H01"), ex("H03"),
        )
        val viewModel = vm()
        advanceUntilIdle()
        viewModel.onExerciseTap("H01")
        advanceUntilIdle()
        viewModel.onSwapSelect("H03")
        advanceUntilIdle()

        coVerify { holder.swap("H01", "H03") }
        assertTrue(viewModel.state.value.swap is SwapSheetState.Hidden)
    }

    @Test
    fun `onDismissSwap closes the sheet without calling holder`() = runTest {
        coEvery { exerciseDao.getByCategory(Category.HIPS) } returns listOf(ex("H01"))
        val viewModel = vm()
        advanceUntilIdle()
        viewModel.onExerciseTap("H01")
        advanceUntilIdle()
        viewModel.onDismissSwap()

        assertTrue(viewModel.state.value.swap is SwapSheetState.Hidden)
        coVerify(exactly = 0) { holder.swap(any(), any()) }
    }
}
```

- [ ] **Step 3: Implement `SessionOverviewViewModel.kt` (green)**

```kotlin
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
```

- [ ] **Step 4: Verify tests pass** — Build → Make Project, run `SessionOverviewViewModelTest` (4 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/ui/screen/session/SessionOverviewUiState.kt \
        app/src/main/java/com/stretchdaily/app/ui/screen/session/SessionOverviewViewModel.kt \
        app/src/test/java/com/stretchdaily/app/ui/screen/session/SessionOverviewViewModelTest.kt
git commit -m "feat(ui): add SessionOverviewViewModel + swap state machine

Screen-scoped VM reading TodaySessionHolder.state. Sheet opens with a
same-category candidate list (current exercise + already-used plan
exercises filtered out); selection delegates to holder.swap()."
git push
```

---

## Task 3: `SessionOverviewScreen`

Layout (top-to-bottom per handoff §2):

```
Column (padScreen, scrollable)
├── Header — MonoCaps "TODAY'S SESSION" + Text "{m} min · {n} moves" (n in ink3)
├── Category spread bar — Row of 1-wide rects, category-tinted, 5 dp height
├── LazyColumn of ExerciseRow — for each PlannedExercise
│     ├── ExerciseTile.Small56 (striped category tint)
│     ├── Column: MonoCaps "{01}" index + Text name (bodyLg SemiBold)
│     │             CatChip(category) + MonoCaps "{target}"
│     └── CircleButton.Bg2(IconName.Swap, onClick = onExerciseTap(item.id))
└── Pill.Accent "Begin session" — full-width, onClick = onBeginSession
```

Plus a `Sheet(visible = state.swap is Visible)` with candidates.

- [ ] **Step 1: Create `SessionOverviewScreen.kt`**

```kotlin
package com.stretchdaily.app.ui.screen.session

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stretchdaily.app.core.engine.model.PlannedExercise
import com.stretchdaily.app.core.model.Exercise
import com.stretchdaily.app.ui.components.AppIcon
import com.stretchdaily.app.ui.components.CatChip
import com.stretchdaily.app.ui.components.CircleButton
import com.stretchdaily.app.ui.components.CircleButtonSize
import com.stretchdaily.app.ui.components.CircleButtonVariant
import com.stretchdaily.app.ui.components.ExerciseTile
import com.stretchdaily.app.ui.components.ExerciseTileSize
import com.stretchdaily.app.ui.components.IconName
import com.stretchdaily.app.ui.components.MonoCaps
import com.stretchdaily.app.ui.components.MonoCapsSize
import com.stretchdaily.app.ui.components.Pill
import com.stretchdaily.app.ui.components.PillVariant
import com.stretchdaily.app.ui.components.Sheet
import com.stretchdaily.app.ui.theme.Theme
import com.stretchdaily.app.ui.theme.tint

@Composable
fun SessionOverviewScreen(
    onBeginSession: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val vm: SessionOverviewViewModel = hiltViewModel()
    val state by vm.state.collectAsState()

    Box(modifier = modifier.fillMaxWidth().background(Theme.colors.bg)) {
        LazyColumn(
            contentPadding = Theme.dims.padScreen,
            verticalArrangement = Arrangement.spacedBy(Theme.dims.gapSection),
        ) {
            item {
                Column {
                    MonoCaps(
                        text = "TODAY'S SESSION",
                        color = Theme.colors.ink3,
                        size = MonoCapsSize.Regular,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${state.planMinutes} min",
                            style = Theme.typo.displayMd,
                            color = Theme.colors.ink,
                        )
                        Spacer(modifier = Modifier.padding(horizontal = 6.dp))
                        Text(
                            text = "· ${state.items.size} moves",
                            style = Theme.typo.bodyLg,
                            color = Theme.colors.ink3,
                        )
                    }
                }
            }
            item { CategorySpreadBar(state.items) }
            items(state.items, key = { it.exercise.id }) { item ->
                ExerciseRow(
                    index = state.items.indexOf(item) + 1,
                    item = item,
                    onSwap = { vm.onExerciseTap(item.exercise.id) },
                )
            }
            item {
                Pill(
                    label = "Begin session",
                    onClick = onBeginSession,
                    variant = PillVariant.Accent,
                    leadingIcon = IconName.Play,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            // Spacer the height of the bottom-nav inset so the CTA isn't pinned under it.
            item { Spacer(modifier = Modifier.height(contentPadding.calculateBottomPadding())) }
        }

        // Swap sheet.
        val swap = state.swap
        Sheet(
            visible = swap is SwapSheetState.Visible,
            onDismiss = vm::onDismissSwap,
        ) {
            if (swap is SwapSheetState.Visible) {
                SwapSheetContent(
                    oldItem = swap.oldItem,
                    candidates = swap.candidates,
                    onSelect = { vm.onSwapSelect(it) },
                )
            }
        }
    }
}

@Composable
private fun CategorySpreadBar(items: List<PlannedExercise>) {
    if (items.isEmpty()) return
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.fillMaxWidth().height(5.dp),
    ) {
        items.forEach { item ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(item.exercise.category.tint()),
            )
        }
    }
}

@Composable
private fun ExerciseRow(index: Int, item: PlannedExercise, onSwap: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ExerciseTile(
            category = item.exercise.category,
            size = ExerciseTileSize.Small56,
        )
        Spacer(modifier = Modifier.padding(horizontal = 6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                MonoCaps(
                    text = "%02d".format(index),
                    color = Theme.colors.ink3,
                    size = MonoCapsSize.Small,
                )
                Spacer(modifier = Modifier.padding(horizontal = 6.dp))
                Text(
                    text = item.exercise.name,
                    style = Theme.typo.bodyLg.copy(fontWeight = FontWeight.SemiBold),
                    color = Theme.colors.ink,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CatChip(category = item.exercise.category)
                Spacer(modifier = Modifier.padding(horizontal = 6.dp))
                MonoCaps(
                    text = targetLabel(item),
                    color = Theme.colors.ink3,
                    size = MonoCapsSize.Small,
                )
            }
        }
        CircleButton(
            onClick = onSwap,
            icon = IconName.Swap,
            size = CircleButtonSize.Medium,
            variant = CircleButtonVariant.Bg2,
            contentDescription = "Swap ${item.exercise.name}",
        )
    }
}

private fun targetLabel(item: PlannedExercise): String {
    val ex = item.exercise
    return when {
        ex.isTimed -> "${item.effectiveSeconds}s" + if (ex.isUnilateral) " · L/R" else ""
        ex.targetReps != null -> "${ex.targetReps} reps" + if (ex.isUnilateral) " · L/R" else ""
        else -> "${item.effectiveSeconds}s"
    }
}

@Composable
private fun SwapSheetContent(
    oldItem: PlannedExercise,
    candidates: List<Exercise>,
    onSelect: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MonoCaps(
            text = "SWAP \"${oldItem.exercise.name}\"",
            color = Theme.colors.ink3,
            size = MonoCapsSize.Regular,
        )
        candidates.forEach { candidate ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(candidate.id) }
                    .padding(vertical = 6.dp),
            ) {
                ExerciseTile(
                    category = candidate.category,
                    size = ExerciseTileSize.Small56,
                )
                Spacer(modifier = Modifier.padding(horizontal = 10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = candidate.name,
                        style = Theme.typo.bodyLg.copy(fontWeight = FontWeight.SemiBold),
                        color = Theme.colors.ink,
                    )
                    MonoCaps(
                        text = candidateSubtitle(candidate),
                        color = Theme.colors.ink3,
                        size = MonoCapsSize.Small,
                    )
                }
                AppIcon(
                    name = IconName.ChevronRight,
                    size = 16.dp,
                    tint = Theme.colors.ink3,
                    contentDescription = null,
                )
            }
        }
    }
}

private fun candidateSubtitle(ex: Exercise): String = when {
    ex.isTimed -> "${ex.totalTime}s" + if (ex.isUnilateral) " · L/R" else ""
    ex.targetReps != null -> "${ex.targetReps} reps" + if (ex.isUnilateral) " · L/R" else ""
    else -> "${ex.totalTime}s"
}
```

**Expected adjustments against R2 primitives:** none — R2 landed
`CircleButtonSize { Small / Medium / Large / Large58 }`,
`CircleButtonVariant { Bg2 / Ink / Accent }`,
`ExerciseTileSize { Small56 / Large4x3 }`, `PillVariant { Accent / Neutral / DashedOutline }`,
and `Sheet(visible, onDismiss, content)` — the call sites above use those exact names.

- [ ] **Step 2: Verify** — Build → Make Project. Visual wiring is verified in Task 5.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/ui/screen/session/SessionOverviewScreen.kt
git commit -m "feat(ui): add SessionOverviewScreen with swap sheet

Header (minutes + moves) + category spread bar + per-exercise rows
(tile + index + name + chip + target + swap button) + full-width
Begin pill. Swap sheet renders filtered same-category candidates."
git push
```

---

## Task 4: Wire Overview into the session nested graph

Edit `StretchDailyNavHost.kt`. The existing structure (from R1/R3) defines a nested `navigation(startDestination = ROUTE_SESSION_OVERVIEW, route = ROUTE_SESSION_GRAPH)` block with three placeholders. Replace the overview placeholder.

- [ ] **Step 1: Find the overview placeholder block**

```kotlin
navigation(
    route = ROUTE_SESSION_GRAPH,
    startDestination = ROUTE_SESSION_OVERVIEW,
) {
    composable(ROUTE_SESSION_OVERVIEW) {
        PlaceholderScreen(tabLabel = "SESSION OVERVIEW", unlocksInPhase = "R4", ...)
    }
    composable(ROUTE_SESSION_PLAYER) { ... }
    composable(ROUTE_SESSION_COMPLETE) { ... }
}
```

- [ ] **Step 2: Replace the overview `composable` block**

```kotlin
composable(ROUTE_SESSION_OVERVIEW) {
    SessionOverviewScreen(
        onBeginSession = { navController.navigate(ROUTE_SESSION_PLAYER) },
        contentPadding = innerPadding,
    )
}
```

- [ ] **Step 3: Verify smoke test**

Open the app → tap Dashboard Play CTA → overview shows with real moves. Tap any swap button → bottom sheet slides up with alternatives. Select one → sheet dismisses; the overview row updates (holder emits a new plan). "Begin session" still goes to the R4-stub placeholder player (that's Task 6 territory).

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/ui/navigation/StretchDailyNavHost.kt
git commit -m "feat(navigation): wire SessionOverviewScreen into session graph"
git push
```

> **R4a stop line.** Task 5 finishes the overview + complete pair; R4a would stop after Task 5 + a temporary complete-screen wiring. Tasks 6–10 cover the player.

---

## Task 5: `SessionCompleteScreen` (uses shared VM in Task 7)

The complete screen is simpler than the player — a full-bleed accent celebration. Build it now so the overview → (stub player) → complete flow can be smoke-tested end-to-end in R4a.

Because the complete screen reads summary from the shared `SessionPlayerViewModel` that Task 7 introduces, we temporarily accept parameters (via NavArgs or a temporary VM) to keep R4a shippable. Simplest form: accept `areasStretched: Int`, `totalMinutes: Int`, `streakAfter: Int` as NavArgs. Task 10 replaces those with the real shared VM read.

Not strictly required in R4 if R4 is a single PR — this task is mostly a convenience for split R4a. Skip this task if you are doing R4 as a single PR; jump to Task 6 and let the VM feed the complete screen directly.

- [ ] **Step 1 (single-PR R4): skip this task, proceed to Task 6.**

- [ ] **Step 1 (split R4a): Create `SessionCompleteScreen.kt` with NavArg inputs**

```kotlin
package com.stretchdaily.app.ui.screen.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stretchdaily.app.ui.components.AppIcon
import com.stretchdaily.app.ui.components.IconName
import com.stretchdaily.app.ui.components.MonoCaps
import com.stretchdaily.app.ui.components.MonoCapsSize
import com.stretchdaily.app.ui.components.Pill
import com.stretchdaily.app.ui.components.PillVariant
import com.stretchdaily.app.ui.theme.Theme

@Composable
fun SessionCompleteScreen(
    areasStretched: Int,
    totalMinutes: Int,
    streakAfter: Int,
    onBackToToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.colors.accent),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp),
        ) {
            MonoCaps(
                text = "SESSION COMPLETE",
                color = Theme.colors.accentInk.copy(alpha = 0.7f),
                size = MonoCapsSize.Regular,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Well done.",
                style = Theme.typo.displayXl.copy(fontStyle = FontStyle.Italic),
                color = Theme.colors.accentInk,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "You stretched $areasStretched areas in $totalMinutes minutes. Streak's at $streakAfter now.",
                style = Theme.typo.bodyLg,
                color = Theme.colors.accentInk,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(28.dp))
            // Divider row: short line · flame · short line.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(1.dp)
                        .background(Theme.colors.accentInk.copy(alpha = 0.4f)),
                )
                Spacer(modifier = Modifier.width(10.dp))
                AppIcon(
                    name = IconName.Flame,
                    size = 14.dp,
                    tint = Theme.colors.accentInk,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(1.dp)
                        .background(Theme.colors.accentInk.copy(alpha = 0.4f)),
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Pill(
                label = "Back to today",
                onClick = onBackToToday,
                variant = PillVariant.Inverse,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
```

**Verify:** R2's `PillVariant` includes an `Inverse` (or `InverseAccent`) variant with cream bg + accent text — needed for the on-accent CTA. If not, add it as a minor R4 extension: `Inverse` = `background = accentInk`, `contentColor = accent`.

- [ ] **Step 2 (split R4a only): Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/ui/screen/session/SessionCompleteScreen.kt
git commit -m "feat(ui): add SessionCompleteScreen (full-bleed accent)"
git push
```

---

## Task 6: `SessionPlayerUiState`

A sealed interface carrying the full player state. Mirrors the old `SessionUiState` (from pre-R1 code, which was deleted) but renamed to avoid any cross-talk, and scoped specifically to the player + complete pair.

- [ ] **Step 1: Create `SessionPlayerUiState.kt`**

```kotlin
package com.stretchdaily.app.ui.screen.session

import com.stretchdaily.app.core.engine.model.PlannedExercise
import com.stretchdaily.app.core.engine.model.SessionPlan

sealed interface SessionPlayerUiState {
    data object Loading : SessionPlayerUiState

    /** Running / paused player state. */
    data class Running(
        val plan: SessionPlan,
        val currentIndex: Int,
        val side: Side,
        val remainingSeconds: Int,
        val totalSecondsForPhase: Int,
        val isPaused: Boolean,
    ) : SessionPlayerUiState {
        val currentItem: PlannedExercise get() = plan.items[currentIndex]
        val progressFraction: Float
            get() = if (totalSecondsForPhase == 0) 0f
                    else 1f - remainingSeconds.toFloat() / totalSecondsForPhase.toFloat()
    }

    /** Terminal — completion summary drives `SessionCompleteScreen`. */
    data class Complete(
        val areasStretched: Int,
        val totalMinutes: Int,
        val streakAfter: Int,
    ) : SessionPlayerUiState
}

/** Unilateral-side enum, preserved from the previous session VM. */
enum class Side { NONE, LEFT, RIGHT }
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/ui/screen/session/SessionPlayerUiState.kt
git commit -m "feat(ui): add SessionPlayerUiState sealed hierarchy"
git push
```

---

## Task 7: `SessionPlayerViewModel` (with tests)

Nested-graph-scoped VM. Reads `TodaySessionHolder.state` for the plan, owns the 1 Hz timer, writes `SessionRepository.completeSession`, plays audio cues, calls `holder.onSessionCompleted()` at the end (no-op per spec §6.2).

Key mechanic: **`tick()` is `internal`** so unit tests can drive it deterministically (preserving the pattern the deleted `SessionViewModel` used).

- [ ] **Step 1: Write the VM test (red)**

Create: `app/src/test/java/com/stretchdaily/app/ui/screen/session/SessionPlayerViewModelTest.kt`

```kotlin
package com.stretchdaily.app.ui.screen.session

import com.stretchdaily.app.core.audio.SessionAudioPlayer
import com.stretchdaily.app.core.engine.model.PlannedExercise
import com.stretchdaily.app.core.engine.model.SessionPlan
import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.core.model.Exercise
import com.stretchdaily.app.core.session.TodaySession
import com.stretchdaily.app.core.session.TodaySessionHolder
import com.stretchdaily.app.core.util.Clock
import com.stretchdaily.app.data.SessionRepository
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionPlayerViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var holder: TodaySessionHolder
    private lateinit var repository: SessionRepository
    private lateinit var audioPlayer: SessionAudioPlayer
    private val clock = Clock { 1_700_000_000_000L }

    private fun ex(id: String, isUnilateral: Boolean = false, totalTime: Int = 60) =
        Exercise(
            id = id,
            name = id,
            category = Category.HIPS,
            cues = emptyList(),
            isUnilateral = isUnilateral,
            isTimed = true,
            targetReps = null,
            secondsPerRep = null,
            totalTime = totalTime,
        )

    private fun item(id: String, seconds: Int = 60, isUnilateral: Boolean = false) =
        PlannedExercise(ex(id, isUnilateral, seconds), seconds, isForced = false)

    private fun plan(vararg items: PlannedExercise) =
        SessionPlan(items.toList(), items.sumOf { it.effectiveSeconds }, emptyMap())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        holder = mockk(relaxed = true)
        repository = mockk(relaxed = true)
        audioPlayer = mockk(relaxed = true)
        coJustRun { holder.onSessionCompleted() }
        coJustRun { holder.ensureFresh(any()) }
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun stubHolder(plan: SessionPlan) {
        coEvery { holder.state } returns MutableStateFlow(
            TodaySession(LocalDate.of(2026, 4, 23), plan)
        )
    }

    private fun viewModel(): SessionPlayerViewModel = SessionPlayerViewModel(
        holder = holder,
        repository = repository,
        audioPlayer = audioPlayer,
        clock = clock,
    )

    @Test
    fun `init emits Running at index 0 and fires start chime`() = runTest {
        stubHolder(plan(item("a", 30), item("b", 30)))
        val vm = viewModel()
        advanceUntilIdle()

        val running = vm.state.value as SessionPlayerUiState.Running
        assertEquals(0, running.currentIndex)
        assertEquals(Side.NONE, running.side)
        assertEquals(30, running.remainingSeconds)
        assertEquals(30, running.totalSecondsForPhase)
        assertTrue(!running.isPaused)
        coVerify { audioPlayer.playStart() }
    }

    @Test
    fun `tick decrements remainingSeconds`() = runTest {
        stubHolder(plan(item("a", 5)))
        val vm = viewModel()
        advanceUntilIdle()

        vm.tick()
        assertEquals(4, (vm.state.value as SessionPlayerUiState.Running).remainingSeconds)
        vm.tick()
        assertEquals(3, (vm.state.value as SessionPlayerUiState.Running).remainingSeconds)
    }

    @Test
    fun `paused tick is a no-op`() = runTest {
        stubHolder(plan(item("a", 5)))
        val vm = viewModel()
        advanceUntilIdle()

        vm.togglePause()
        vm.tick()
        vm.tick()

        val r = vm.state.value as SessionPlayerUiState.Running
        assertEquals(5, r.remainingSeconds)
        assertTrue(r.isPaused)
    }

    @Test
    fun `tick rolling over advances to next exercise and fires start chime`() = runTest {
        stubHolder(plan(item("a", 1), item("b", 30)))
        coEvery { audioPlayer.playStart() } returns Unit
        val vm = viewModel()
        advanceUntilIdle()

        vm.tick() // 1 → 0 → advance

        val r = vm.state.value as SessionPlayerUiState.Running
        assertEquals(1, r.currentIndex)
        assertEquals(30, r.remainingSeconds)
        coVerify(atLeast = 2) { audioPlayer.playStart() }
    }

    @Test
    fun `unilateral LEFT advances to RIGHT before next exercise`() = runTest {
        stubHolder(plan(item("a", 2, isUnilateral = true), item("b", 10)))
        val vm = viewModel()
        advanceUntilIdle()

        // 2s split: LEFT gets ceiling → 1s, RIGHT 1s.
        vm.tick() // LEFT 1 → 0 → switch to RIGHT
        var r = vm.state.value as SessionPlayerUiState.Running
        assertEquals(0, r.currentIndex)
        assertEquals(Side.RIGHT, r.side)
        assertEquals(1, r.remainingSeconds)

        vm.tick() // RIGHT 1 → 0 → advance to b
        r = vm.state.value as SessionPlayerUiState.Running
        assertEquals(1, r.currentIndex)
        assertEquals(Side.NONE, r.side)
    }

    @Test
    fun `skip jumps straight to next exercise`() = runTest {
        stubHolder(plan(item("a", 60), item("b", 60)))
        val vm = viewModel()
        advanceUntilIdle()

        vm.skip()
        val r = vm.state.value as SessionPlayerUiState.Running
        assertEquals(1, r.currentIndex)
    }

    @Test
    fun `finishing the last exercise persists a session and transitions to Complete`() = runTest {
        val only = item("only", 1)
        stubHolder(plan(only))
        coEvery { repository.completeSession(any(), any()) } returns 1L
        coEvery { repository.currentStreakDays() } returns 7
        val vm = viewModel()
        advanceUntilIdle()

        vm.tick() // 1 → 0 → finish
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue("expected Complete but was $state", state is SessionPlayerUiState.Complete)
        val c = state as SessionPlayerUiState.Complete
        assertEquals(1, c.areasStretched)
        assertEquals(0, c.totalMinutes) // 1s → 0 min (integer truncation)
        assertEquals(7, c.streakAfter)

        coVerify { repository.completeSession(any(), any()) }
        coVerify { audioPlayer.playEnd() }
        coVerify { holder.onSessionCompleted() }
    }

    @Test
    fun `areasStretched counts distinct categories`() = runTest {
        val hipsOne = item("H01", 1)
        val hipsTwo = PlannedExercise(
            exercise = ex("H02").copy(category = Category.HIPS),
            effectiveSeconds = 1,
            isForced = false,
        )
        val neckOne = PlannedExercise(
            exercise = ex("N01").copy(category = Category.NECK),
            effectiveSeconds = 1,
            isForced = false,
        )
        stubHolder(plan(hipsOne, hipsTwo, neckOne))
        coEvery { repository.completeSession(any(), any()) } returns 2L
        coEvery { repository.currentStreakDays() } returns 3

        val vm = viewModel()
        advanceUntilIdle()
        // Skip through all three items.
        vm.skip(); vm.skip(); vm.skip()
        advanceUntilIdle()

        val c = vm.state.value as SessionPlayerUiState.Complete
        assertEquals(2, c.areasStretched) // HIPS + NECK
    }

    @Test
    fun `phaseSeconds bilateral returns full budget`() {
        val full = item("a", 60, isUnilateral = false)
        assertEquals(60, SessionPlayerViewModel.phaseSeconds(full, Side.NONE))
    }

    @Test
    fun `phaseSeconds unilateral splits with ceiling on LEFT`() {
        val uni = item("a", 61, isUnilateral = true)
        assertEquals(31, SessionPlayerViewModel.phaseSeconds(uni, Side.LEFT))
        assertEquals(30, SessionPlayerViewModel.phaseSeconds(uni, Side.RIGHT))
    }
}
```

- [ ] **Step 2: Implement `SessionPlayerViewModel.kt` (green)**

```kotlin
package com.stretchdaily.app.ui.screen.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stretchdaily.app.core.audio.SessionAudioPlayer
import com.stretchdaily.app.core.engine.model.PlannedExercise
import com.stretchdaily.app.core.engine.model.SessionPlan
import com.stretchdaily.app.core.session.TodaySessionHolder
import com.stretchdaily.app.core.util.Clock
import com.stretchdaily.app.data.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Nested-graph-scoped ViewModel for `"session/player"` + `"session/complete"`.
 * Reads the plan from [TodaySessionHolder] on init, runs the 1 Hz timer
 * loop, writes the [SessionRepository.completeSession] record at the end,
 * and stays alive for the complete screen to read [SessionPlayerUiState.Complete].
 *
 * Timer: [startTimer] launches a coroutine that calls [tick] every second.
 * [tick] is `internal` so unit tests can drive it deterministically.
 */
@HiltViewModel
class SessionPlayerViewModel @Inject constructor(
    private val holder: TodaySessionHolder,
    private val repository: SessionRepository,
    private val audioPlayer: SessionAudioPlayer,
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow<SessionPlayerUiState>(SessionPlayerUiState.Loading)
    val state: StateFlow<SessionPlayerUiState> = _state.asStateFlow()

    private var startedAt: Long = 0L
    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            // Holder is already populated by the overview screen, but guard anyway.
            holder.ensureFresh()
            val today = holder.state.value ?: return@launch
            startedAt = clock.now()
            val first = today.plan.items.firstOrNull() ?: return@launch
            val side = if (first.exercise.isUnilateral) Side.LEFT else Side.NONE
            val secs = phaseSeconds(first, side)
            _state.value = SessionPlayerUiState.Running(
                plan = today.plan,
                currentIndex = 0,
                side = side,
                remainingSeconds = secs,
                totalSecondsForPhase = secs,
                isPaused = false,
            )
            audioPlayer.playStart()
            startTimer()
        }
    }

    fun togglePause() {
        val r = _state.value as? SessionPlayerUiState.Running ?: return
        _state.value = r.copy(isPaused = !r.isPaused)
    }

    fun skip() {
        val r = _state.value as? SessionPlayerUiState.Running ?: return
        advance(r)
    }

    fun prev() {
        val r = _state.value as? SessionPlayerUiState.Running ?: return
        // Unilateral RIGHT → LEFT on the same exercise; otherwise step back one exercise.
        val item = r.plan.items[r.currentIndex]
        if (item.exercise.isUnilateral && r.side == Side.RIGHT) {
            val secs = phaseSeconds(item, Side.LEFT)
            _state.value = r.copy(side = Side.LEFT, remainingSeconds = secs, totalSecondsForPhase = secs)
            return
        }
        val prevIndex = (r.currentIndex - 1).coerceAtLeast(0)
        val prevItem = r.plan.items[prevIndex]
        val prevSide =
            if (prevItem.exercise.isUnilateral) Side.LEFT else Side.NONE
        val secs = phaseSeconds(prevItem, prevSide)
        _state.value = r.copy(
            currentIndex = prevIndex,
            side = prevSide,
            remainingSeconds = secs,
            totalSecondsForPhase = secs,
        )
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1_000)
                tick()
            }
        }
    }

    /** Exposed to tests. Decrements or advances when the phase reaches zero. */
    internal fun tick() {
        val r = _state.value as? SessionPlayerUiState.Running ?: return
        if (r.isPaused) return
        val next = r.remainingSeconds - 1
        if (next > 0) {
            _state.value = r.copy(remainingSeconds = next)
        } else {
            advance(r)
        }
    }

    private fun advance(r: SessionPlayerUiState.Running) {
        val item = r.plan.items[r.currentIndex]

        // Unilateral LEFT → RIGHT same exercise.
        if (item.exercise.isUnilateral && r.side == Side.LEFT) {
            val secs = phaseSeconds(item, Side.RIGHT)
            _state.value = r.copy(
                side = Side.RIGHT,
                remainingSeconds = secs,
                totalSecondsForPhase = secs,
            )
            return
        }

        val nextIndex = r.currentIndex + 1
        if (nextIndex >= r.plan.items.size) {
            finish(r.plan)
            return
        }
        val nextItem = r.plan.items[nextIndex]
        val nextSide = if (nextItem.exercise.isUnilateral) Side.LEFT else Side.NONE
        val secs = phaseSeconds(nextItem, nextSide)
        _state.value = r.copy(
            currentIndex = nextIndex,
            side = nextSide,
            remainingSeconds = secs,
            totalSecondsForPhase = secs,
        )
        viewModelScope.launch { audioPlayer.playStart() }
    }

    private fun finish(plan: SessionPlan) {
        timerJob?.cancel()
        viewModelScope.launch {
            audioPlayer.playEnd()
            repository.completeSession(plan, startedAt)
            holder.onSessionCompleted()
            val streak = repository.currentStreakDays()
            _state.value = SessionPlayerUiState.Complete(
                areasStretched = plan.items.map { it.exercise.category }.toSet().size,
                totalMinutes = plan.totalSeconds / 60,
                streakAfter = streak,
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    companion object {
        /**
         * Split an exercise's budget by side. Bilateral or `Side.NONE`: full
         * budget. Unilateral: half each, LEFT gets the ceiling so odd totals
         * don't lose a second. Preserved from the pre-R1 session VM.
         */
        internal fun phaseSeconds(item: PlannedExercise, side: Side): Int =
            if (!item.exercise.isUnilateral || side == Side.NONE) {
                item.effectiveSeconds
            } else {
                (item.effectiveSeconds + if (side == Side.LEFT) 1 else 0) / 2
            }
    }
}
```

- [ ] **Step 3: Verify** — Build → Make Project, run `SessionPlayerViewModelTest` (10 tests green).

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/ui/screen/session/SessionPlayerViewModel.kt \
        app/src/test/java/com/stretchdaily/app/ui/screen/session/SessionPlayerViewModelTest.kt
git commit -m "feat(ui): add SessionPlayerViewModel with timer + L/R cycling

1 Hz delay-based timer; tick() is internal for deterministic unit tests.
Unilateral moves cycle L→R→next; bilateral advance directly. Audio cues
fire on exercise transitions via SessionAudioPlayer. Finish persists via
SessionRepository.completeSession and transitions to Complete state with
areasStretched (distinct category count) + totalMinutes + streakAfter."
git push
```

---

## Task 8: `SessionPlayerScreen`

Full-screen overlay. Layout per handoff §3:

```
Column (fill, padScreen-ish)
├── Top bar
│     ├── CircleButton.Bg2(IconName.Close, onClick = onClose)        ← pops the session graph
│     ├── SegmentProgress(total = items.size, currentIndex = currentIndex, modifier.weight(1f))
│     └── Spacer matching the close button (layout symmetry)
├── MonoCaps row — "MOVE {i} OF {n}" | "{CATEGORY}"
├── ExerciseTile.Large4x3(current.category)
├── Exercise name + cue bullets (em-dash bullet in accent)
├── Spacer (center of screen)
├── Timer area
│     ├── if isUnilateral: L / R chip (active side full, other 0.3 alpha)
│     ├── if isTimed: MM:SS counter (displayXl)
│     └── else reps: "{reps}" + "reps" suffix
├── Caption (context-sensitive)
└── Controls row
      ├── CircleButton.Bg2(IconName.SkipPrev, onClick = prev)
      ├── Pill (timed: Accent, label Start/Pause/Resume; reps: DashedOutline "Perform at your own pace")
      └── CircleButton.Ink(IconName.SkipNext, onClick = skip)
```

- [ ] **Step 1: Create `SessionPlayerScreen.kt`**

```kotlin
package com.stretchdaily.app.ui.screen.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import com.stretchdaily.app.ui.components.CircleButton
import com.stretchdaily.app.ui.components.CircleButtonSize
import com.stretchdaily.app.ui.components.CircleButtonVariant
import com.stretchdaily.app.ui.components.ExerciseTile
import com.stretchdaily.app.ui.components.ExerciseTileSize
import com.stretchdaily.app.ui.components.IconName
import com.stretchdaily.app.ui.components.MonoCaps
import com.stretchdaily.app.ui.components.MonoCapsSize
import com.stretchdaily.app.ui.components.Pill
import com.stretchdaily.app.ui.components.PillVariant
import com.stretchdaily.app.ui.components.SegmentProgress
import com.stretchdaily.app.ui.theme.Theme

@Composable
fun SessionPlayerScreen(
    parentEntry: NavBackStackEntry,
    onClose: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm: SessionPlayerViewModel = hiltViewModel(parentEntry)
    val state by vm.state.collectAsState()

    // Navigate to Complete screen when the VM transitions.
    androidx.compose.runtime.LaunchedEffect(state) {
        if (state is SessionPlayerUiState.Complete) onComplete()
    }

    Box(
        modifier = modifier.fillMaxSize().background(Theme.colors.bg),
    ) {
        when (val s = state) {
            SessionPlayerUiState.Loading -> Unit
            is SessionPlayerUiState.Complete -> Unit // handled by LaunchedEffect above
            is SessionPlayerUiState.Running -> PlayerBody(
                state = s,
                onClose = onClose,
                onPrev = vm::prev,
                onSkip = vm::skip,
                onTogglePause = vm::togglePause,
            )
        }
    }
}

@Composable
private fun PlayerBody(
    state: SessionPlayerUiState.Running,
    onClose: () -> Unit,
    onPrev: () -> Unit,
    onSkip: () -> Unit,
    onTogglePause: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Top bar.
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircleButton(
                onClick = onClose,
                icon = IconName.Close,
                size = CircleButtonSize.Medium,
                variant = CircleButtonVariant.Bg2,
                contentDescription = "Close session",
            )
            Spacer(modifier = Modifier.padding(horizontal = 12.dp))
            SegmentProgress(
                total = state.plan.items.size,
                currentIndex = state.currentIndex,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.size(44.dp)) // symmetry
        }

        // Caption row.
        Row(modifier = Modifier.fillMaxWidth()) {
            MonoCaps(
                text = "MOVE ${state.currentIndex + 1} OF ${state.plan.items.size}",
                color = Theme.colors.ink3,
                size = MonoCapsSize.Regular,
            )
            Spacer(modifier = Modifier.weight(1f))
            MonoCaps(
                text = state.currentItem.exercise.category.displayName.uppercase(),
                color = Theme.colors.ink3,
                size = MonoCapsSize.Regular,
            )
        }

        // Exercise tile.
        ExerciseTile(
            category = state.currentItem.exercise.category,
            size = ExerciseTileSize.Large4x3,
            modifier = Modifier.fillMaxWidth(),
        )

        // Title + cues.
        Text(
            text = state.currentItem.exercise.name,
            style = Theme.typo.displayMd,
            color = Theme.colors.ink,
        )
        state.currentItem.exercise.cues.take(4).forEach { cue ->
            Row {
                Text(
                    text = "— ",
                    style = Theme.typo.bodyMd,
                    color = Theme.colors.accent,
                )
                Text(
                    text = cue,
                    style = Theme.typo.bodyMd,
                    color = Theme.colors.ink2,
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Timer area.
        TimerArea(state = state)

        Spacer(modifier = Modifier.weight(1f))

        // Controls.
        ControlRow(
            state = state,
            onPrev = onPrev,
            onSkip = onSkip,
            onTogglePause = onTogglePause,
        )
    }
}

@Composable
private fun TimerArea(state: SessionPlayerUiState.Running) {
    val ex = state.currentItem.exercise
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (ex.isUnilateral) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SideChip(label = "LEFT", active = state.side == Side.LEFT)
                Spacer(modifier = Modifier.padding(horizontal = 6.dp))
                Text(
                    text = "—",
                    style = Theme.typo.bodyLg,
                    color = Theme.colors.ink3,
                )
                Spacer(modifier = Modifier.padding(horizontal = 6.dp))
                SideChip(label = "RIGHT", active = state.side == Side.RIGHT)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        if (ex.isTimed) {
            Text(
                text = formatMmSs(state.remainingSeconds),
                style = Theme.typo.displayXl,
                color = Theme.colors.ink,
            )
        } else {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = (ex.targetReps ?: 0).toString(),
                    style = Theme.typo.displayLg,
                    color = Theme.colors.ink,
                )
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text(
                    text = "reps",
                    style = Theme.typo.bodyLg,
                    color = Theme.colors.ink3,
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        MonoCaps(
            text = captionFor(state),
            color = Theme.colors.ink3,
            size = MonoCapsSize.Regular,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SideChip(label: String, active: Boolean) {
    MonoCaps(
        text = label,
        color = if (active) Theme.colors.ink else Theme.colors.ink.copy(alpha = 0.3f),
        size = MonoCapsSize.Regular,
    )
}

@Composable
private fun ControlRow(
    state: SessionPlayerUiState.Running,
    onPrev: () -> Unit,
    onSkip: () -> Unit,
    onTogglePause: () -> Unit,
) {
    val ex = state.currentItem.exercise
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        CircleButton(
            onClick = onPrev,
            icon = IconName.SkipPrev,
            size = CircleButtonSize.Large,
            variant = CircleButtonVariant.Bg2,
            contentDescription = "Previous exercise",
        )
        if (ex.isTimed) {
            Pill(
                label = if (state.isPaused) "Resume" else if (state.remainingSeconds == state.totalSecondsForPhase) "Start" else "Pause",
                onClick = onTogglePause,
                variant = PillVariant.Accent,
                modifier = Modifier.weight(1f),
            )
        } else {
            Pill(
                label = "Perform at your own pace",
                onClick = {}, // non-interactive for reps
                variant = PillVariant.DashedOutline,
                modifier = Modifier.weight(1f),
            )
        }
        CircleButton(
            onClick = onSkip,
            icon = IconName.SkipNext,
            size = CircleButtonSize.Large,
            variant = CircleButtonVariant.Ink,
            contentDescription = "Next exercise",
        )
    }
}

private fun formatMmSs(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%d:%02d".format(m, s)
}

private fun captionFor(state: SessionPlayerUiState.Running): String = when {
    state.isPaused -> "PAUSED"
    !state.currentItem.exercise.isTimed -> "TAP NEXT WHEN COMPLETE"
    state.remainingSeconds == state.totalSecondsForPhase -> "READY WHEN YOU ARE"
    else -> "HOLD THE POSITION"
}
```

**Adjustments against R2 primitives:** top bar uses `CircleButtonSize.Medium`
(44 dp), controls use `CircleButtonSize.Large` (52 dp). If `SegmentProgress`
or `ExerciseTile` do not accept a `modifier` after R2, add the overload at
the primitive — these are trivial param additions.

- [ ] **Step 2: Verify compile** — Build → Make Project. Visual test in Task 10.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/ui/screen/session/SessionPlayerScreen.kt
git commit -m "feat(ui): add SessionPlayerScreen full-screen player layout

Top bar (close + SegmentProgress), MonoCaps caption row, large striped
tile, title + cue bullets (em-dash in accent), timer area (L/R chip +
MM:SS or reps), controls row (prev + start/pause pill or dashed reps
pill + next). Caption bucketizes ready/hold/paused/reps."
git push
```

---

## Task 9: `SessionCompleteScreen` (shared-VM version)

If you skipped Task 5 (single-PR R4 path), create the complete screen now and source its data from the shared player VM.

- [ ] **Step 1: Create `SessionCompleteScreen.kt` using `hiltViewModel(parentEntry)`**

```kotlin
package com.stretchdaily.app.ui.screen.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import com.stretchdaily.app.ui.components.AppIcon
import com.stretchdaily.app.ui.components.IconName
import com.stretchdaily.app.ui.components.MonoCaps
import com.stretchdaily.app.ui.components.MonoCapsSize
import com.stretchdaily.app.ui.components.Pill
import com.stretchdaily.app.ui.components.PillVariant
import com.stretchdaily.app.ui.theme.Theme

@Composable
fun SessionCompleteScreen(
    parentEntry: NavBackStackEntry,
    onBackToToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm: SessionPlayerViewModel = hiltViewModel(parentEntry)
    val state by vm.state.collectAsState()
    val c = (state as? SessionPlayerUiState.Complete)
        ?: return // shouldn't be reached; player navigates here only after complete

    Box(
        modifier = modifier.fillMaxSize().background(Theme.colors.accent),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp),
        ) {
            MonoCaps(
                text = "SESSION COMPLETE",
                color = Theme.colors.accentInk.copy(alpha = 0.7f),
                size = MonoCapsSize.Regular,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Well done.",
                style = Theme.typo.displayXl.copy(fontStyle = FontStyle.Italic),
                color = Theme.colors.accentInk,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "You stretched ${c.areasStretched} areas in ${c.totalMinutes} minutes. Streak's at ${c.streakAfter} now.",
                style = Theme.typo.bodyLg,
                color = Theme.colors.accentInk,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(28.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(1.dp)
                        .background(Theme.colors.accentInk.copy(alpha = 0.4f)),
                )
                Spacer(modifier = Modifier.width(10.dp))
                AppIcon(
                    name = IconName.Flame,
                    size = 14.dp,
                    tint = Theme.colors.accentInk,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(1.dp)
                        .background(Theme.colors.accentInk.copy(alpha = 0.4f)),
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Pill(
                label = "Back to today",
                onClick = onBackToToday,
                variant = PillVariant.Inverse,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/ui/screen/session/SessionCompleteScreen.kt
git commit -m "feat(ui): add SessionCompleteScreen sharing player VM via parentEntry"
git push
```

---

## Task 10: Wire player + complete into the nested graph

Now the session graph uses `hiltViewModel(parentEntry)` for both player and complete so they share a single `SessionPlayerViewModel` instance.

- [ ] **Step 1: Edit `StretchDailyNavHost.kt` nested session graph**

Replace the placeholder entries for `ROUTE_SESSION_PLAYER` and `ROUTE_SESSION_COMPLETE` with:

```kotlin
navigation(
    route = ROUTE_SESSION_GRAPH,
    startDestination = ROUTE_SESSION_OVERVIEW,
) {
    composable(ROUTE_SESSION_OVERVIEW) {
        SessionOverviewScreen(
            onBeginSession = { navController.navigate(ROUTE_SESSION_PLAYER) },
            contentPadding = innerPadding,
        )
    }
    composable(ROUTE_SESSION_PLAYER) { backStackEntry ->
        val parentEntry = remember(backStackEntry) {
            navController.getBackStackEntry(ROUTE_SESSION_GRAPH)
        }
        SessionPlayerScreen(
            parentEntry = parentEntry,
            onClose = {
                navController.popBackStack(
                    route = ROUTE_TODAY,
                    inclusive = false,
                )
            },
            onComplete = {
                navController.navigate(ROUTE_SESSION_COMPLETE) {
                    // Clear the player from backstack so "back to today" from
                    // Complete jumps straight to today.
                    popUpTo(ROUTE_SESSION_PLAYER) { inclusive = true }
                }
            },
        )
    }
    composable(ROUTE_SESSION_COMPLETE) { backStackEntry ->
        val parentEntry = remember(backStackEntry) {
            navController.getBackStackEntry(ROUTE_SESSION_GRAPH)
        }
        SessionCompleteScreen(
            parentEntry = parentEntry,
            onBackToToday = {
                navController.popBackStack(
                    route = ROUTE_TODAY,
                    inclusive = false,
                )
            },
        )
    }
}
```

Imports needed:

```kotlin
import androidx.compose.runtime.remember
import com.stretchdaily.app.ui.screen.session.SessionCompleteScreen
import com.stretchdaily.app.ui.screen.session.SessionPlayerScreen
import com.stretchdaily.app.ui.screen.session.SessionOverviewScreen
```

- [ ] **Step 2: Add player + complete to `BOTTOM_NAV_ROUTES` exclusion**

Per spec §7.1, only `session/overview` shows the bottom bar. Player and complete are overlays. The existing R1 scaffold should already handle this via:

```kotlin
private val BOTTOM_NAV_ROUTES = setOf(
    ROUTE_TODAY,
    ROUTE_SESSION_OVERVIEW,
    ROUTE_LOG,
    ROUTE_PROGRESS,
    ROUTE_SETTINGS,
)
```

Confirm this set still matches. If R1 accidentally included `ROUTE_SESSION_PLAYER` / `ROUTE_SESSION_COMPLETE`, remove them now.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/ui/navigation/StretchDailyNavHost.kt
git commit -m "feat(navigation): wire session player + complete with shared VM

Both routes read the same SessionPlayerViewModel via hiltViewModel(parentEntry)
on ROUTE_SESSION_GRAPH. Close pops to today; Complete-screen Back-to-today
pops to today. Player→Complete transition clears the player from backstack."
git push
```

---

## Task 11: Manual smoke test + pixel QA

- [ ] **Step 1: End-to-end smoke test on device**

1. Launch app → Dashboard.
2. Tap Play CircleButton → Overview shows the real plan.
3. Tap swap button on any exercise → bottom sheet appears with same-category alternatives.
4. Tap an alternative → sheet dismisses; row updates with the new exercise; Dashboard preview bars (if you back out) also update.
5. Tap "Begin session" → Player opens, bottom nav hidden.
6. Observe the timer count down (or tap Pause/Resume to validate).
7. Tap Skip → advances an exercise.
8. On a unilateral exercise, confirm LEFT/RIGHT chip active-side switches after the LEFT phase runs out.
9. Continue through the plan to the end → audio end-chime → Complete screen full-bleed accent.
10. Tap "Back to today" → pops to Dashboard, streak has incremented.
11. Open the session history (if R6 exposes it) to confirm the `SessionRecord` was written.

- [ ] **Step 2: Pixel QA**

Open `docs/design_handoff_stretch_daily_v3/reference/index.html` and compare:

| Screen | Key check |
|---|---|
| Overview | Category spread bar colors, swap button size, chip alignment. |
| Overview swap sheet | Top-only radiusLg, shadow, candidate row layout. |
| Player | SegmentProgress widths, timer font-size + letter-spacing, em-dash bullets in accent. |
| Complete | Accent bg full-bleed, italic heading, divider row ornament, cream-bg pill. |

- [ ] **Step 3: Run all tests in Android Studio**

Right-click `app/src/test/java` → Run 'Tests in 'java''. Expected pass count:

- All preserved (engine, repo, resolver).
- R2 helper tests.
- R3 tests (`TodaySessionHolderTest`, flow/due tests, `DashboardViewModelTest`).
- R4 new: `SessionOverviewViewModelTest` (4), `SessionPlayerViewModelTest` (~10).

- [ ] **Step 4: Push any smoke-test fixes**

```bash
git status
# ...review...
git add <file>
git commit -m "fix(ui): <short>"
git push
```

---

## Task 12: Open the PR

- [ ] **Step 1: Create PR against `development`**

```bash
gh pr create --base development \
  --title "R4 — Session flow (overview + player + complete)" \
  --body "$(cat <<'EOF'
## Summary

- Session Overview screen + VM with swap sheet (same-category candidates filtered against the current plan).
- Session Player screen + VM with 1 Hz timer, L/R cycling, pause/skip/prev, audio cues via `SessionAudioPlayer`, and completion path that writes a `SessionRecord` and calls `TodaySessionHolder.onSessionCompleted()` (no-op).
- Session Complete screen reads summary from the shared player VM (scoped to nested session graph via `hiltViewModel(parentEntry)`).
- NavHost wires all three routes with correct popBackStack behavior (close/complete both return to Dashboard, player→complete clears player from back-stack).

## Test plan

- [x] `SessionOverviewViewModelTest` (4) — state composition + swap filter + dismiss.
- [x] `SessionPlayerViewModelTest` (10) — tick/pause/skip/unilateral L↔R/completion/areasStretched.
- [x] All preserved + R2 + R3 tests green.
- [ ] **Ramon — manual:**
  - Full flow end-to-end: Dashboard Play → Overview → swap works → Begin → Player timer counts down → audio start+transition+end chimes → Complete accent screen → Back to today (streak +1).
  - Unilateral exercise shows correct L/R cycling; timer resets on side change.
  - Close (×) on player pops correctly back to Dashboard.
  - Pixel QA pass against reference/index.html for overview + player + complete.

## Design spec

[2026-04-23-sage-redesign-design.md](../docs/superpowers/specs/2026-04-23-sage-redesign-design.md) §7.3, §8.2–8.4, §11.4.

## Implementation plan

[2026-04-23-sage-redesign-R4-session-flow.md](../docs/superpowers/plans/2026-04-23-sage-redesign-R4-session-flow.md).

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

- [ ] **Step 2: Paste PR URL to Ramon.**

---

## After R4 lands

The core user journey (daily session) is fully redesigned. The LOG, PROGRESS, and SETTINGS tabs are still placeholders; R5 + R6 fill them.

Next: R5 plan ([`2026-04-23-sage-redesign-R5-log-and-carousel.md`](./2026-04-23-sage-redesign-R5-log-and-carousel.md)) — Benchmark Log screen (grouped, sparklines, expand-to-reveal, log bottom sheet variants) and Benchmark Carousel (10-step full-screen flow reusing the log form).
