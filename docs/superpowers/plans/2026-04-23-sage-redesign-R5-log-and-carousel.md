# Sage redesign — Phase R5: Benchmark log + Benchmark carousel — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the two benchmark-logging surfaces defined in spec §8.5 and §8.7:

1. **`BenchmarkLogScreen`** — the Log tab. Grouped by category, every row shows the latest value + band pill + 50×14 sparkline + "Log" pill + chevron. Tapping the chevron expands the row to reveal description + bands + date/value history. Tapping "Log" opens a bottom sheet with the shared `LogForm` (numeric or categorical variant).
2. **`BenchmarkCarouselScreen`** — a full-screen 10-step overlay that walks the user through every benchmark using the same `LogForm`. Entered from the Dashboard banner (when enabled) and the Log tab's "Start benchmark day" CTA.

After R5, both routes are real screens; the Dashboard banner wired in R3 has somewhere to go; and `BenchmarkRepository.logNumeric` / `logCategorical` are exercised end-to-end.

**Architecture:**

- **Log tab** (`"log"`, bottom bar visible) — screen-scoped `BenchmarkLogViewModel` composes `BenchmarkRepository.observeAllBenchmarks()`, `observeLatestLogs()`, and one `observeLogsFor(id)` flow per benchmark (for sparkline + history) into a single `BenchmarkLogUiState` keyed by category. Sheet state (`openBenchmarkId`) is screen-local.
- **Carousel** (`"carousel/{step}"`, overlay inside nested graph `"carousel"`) — nested-graph-scoped `BenchmarkCarouselViewModel` (via `hiltViewModel(parentEntry)`) holds the ordered benchmark list. `state` exposes `currentIndex`, `currentBenchmark`, and `totalSteps = 10`. `onSaveEntry(rawValue, tier?)` delegates to the repository then emits `AdvanceEvent` — the screen observes it and either navigates to `carousel/{step+1}` or pops the whole carousel graph on step 10.
- **Shared form** — `LogForm` Compose function takes `(benchmark, onSubmit: (rawValue: String, tier: FlexibilityTier?) -> Unit, onCancel)`. Numeric variant shows the big centered number input + unit suffix; categorical variant shows 5 vertical band buttons. Both end with the shared Cancel / Save entry footer from the handoff.

**Tech Stack:** Kotlin 2.0, Compose, Hilt, Compose Navigation nested graphs (carousel), Material 3 `ModalBottomSheet` via R2's `Sheet` primitive, JUnit 4 + mockk + `kotlinx.coroutines.test`.

**Spec:** [`docs/superpowers/specs/2026-04-23-sage-redesign-design.md`](../specs/2026-04-23-sage-redesign-design.md) §7.1, §7.2, §7.3, §8.5, §8.7, §10.2, §11.5.
**Handoff reference:**
- [`reference/screens.jsx`](../../design_handoff_stretch_daily_v3/reference/screens.jsx) `BenchmarkLogScreen` (lines 397–480), `BenchmarkRow` (482–608), `BandPill` (610–629), `LogForm` (631–700).
- [`reference/session.jsx`](../../design_handoff_stretch_daily_v3/reference/session.jsx) `BenchmarkCarousel` (264–310).

**Build verification:** Gradle CLI blocked on this machine — verify every commit via Android Studio → Build → Make Project. See [`CLAUDE.md §2 "Known issues"`](../../../CLAUDE.md).

**Prerequisite:** R4 is merged to `development`. Session flow works end-to-end; `TodaySessionHolder` + `DashboardViewModel` with the `benchmarkBannerEnabled` preference are live.

---

## File structure for R5

**New under `app/src/main/java/com/stretchdaily/app/ui/screen/log/`:**
- `BenchmarkLogUiState.kt` — grouped Log UI state.
- `BenchmarkLogViewModel.kt` — composes repo flows; handles save + delete.
- `BenchmarkLogScreen.kt` — grouped list with expand-to-reveal rows + sheet wiring.
- `LogForm.kt` — **shared** composable reused by the Log sheet and the Carousel.

**New under `app/src/main/java/com/stretchdaily/app/ui/screen/carousel/`:**
- `BenchmarkCarouselUiState.kt` — carousel step state.
- `BenchmarkCarouselViewModel.kt` — step orchestration + save-and-advance.
- `BenchmarkCarouselScreen.kt` — full-screen overlay with `SegmentProgress` header.

**New pure-Kotlin helper under `app/src/main/java/com/stretchdaily/app/ui/screen/log/`:**
- `LogSparkline.kt` (top-level `internal fun sparklineValues(logs, now): List<Double>`) — maps the last-6-months log window to normalized 0..1 tier ordinals.

**New tests under `app/src/test/java/com/stretchdaily/app/ui/screen/log/`:**
- `LogSparklineTest.kt` — empty / single point / clamping / 6-month cutoff.
- `BenchmarkLogViewModelTest.kt` — grouping, sparkline data, overdue flag, save/delete roundtrip.

**New tests under `app/src/test/java/com/stretchdaily/app/ui/screen/carousel/`:**
- `BenchmarkCarouselViewModelTest.kt` — step initialization, save-and-advance, last-step event, close event, numeric-vs-categorical save dispatch.

**Modified:**
- `app/src/main/java/com/stretchdaily/app/ui/navigation/StretchDailyNavHost.kt` — register `"carousel"` nested graph with `"carousel/{step}"` composable, hidden from bottom nav; wire Dashboard banner + Log CTA to `navigate("carousel/0")`.
- `app/src/main/java/com/stretchdaily/app/ui/screen/dashboard/DashboardScreen.kt` — pass through `onBenchmarkBannerClick` lambda from the NavHost (if the R3 placeholder left it as a stub, replace with a real `navController.navigate(...)`). No-op if R3 already wired it.

**No changes to:**
- `data/BenchmarkRepository.kt` — existing `logNumeric`, `logCategorical`, `deleteLog`, `observeAllBenchmarks`, `observeLatestLogs`, `observeLogsFor` cover every write + read path R5 needs.
- `core/benchmark/TierResolver.kt` — unchanged; repository already consults it inside `logNumeric`/`updateLog`.
- R2 primitives.

---

## Task 1: Create the `redesign/r5-log-carousel` branch

- [ ] **Step 1: Verify clean state on `development`**

```bash
git checkout development && git pull
git status
```

Expected: clean tree with R4 merged.

- [ ] **Step 2: Create branch + push**

```bash
git checkout -b redesign/r5-log-carousel
git push -u origin redesign/r5-log-carousel
```

---

## Task 2: TDD the `sparklineValues` pure helper

Spec §8.5 says "Sparkline — last 6 months of logs, rendered by the shared `Sparkline` primitive from data provided by `BenchmarkProgressBuilder`." The primitive signature from R2 is `Sparkline(values: List<Double>, ...)` — it expects normalized 0..1 values and renders a dashed flat line when empty.

We do not widen `BenchmarkProgressBuilder` (its `internal` API is preserved). Instead we write a small sibling helper — isolated, pure Kotlin, deterministic — that:

1. Filters the input logs to the **last 6 months** from `now`.
2. Sorts them oldest-first.
3. Maps each `resolvedTier` to a 0..1 value where `VERY_FLEXIBLE = 1.0` (top of the sparkline, since the primitive inverts y under the hood: higher value = taller point) and `STIFF = 0.0`.

Keeping this helper separate from `BenchmarkProgressBuilder` keeps `BigChart`'s band-math (which uses inverted y) decoupled from the sparkline's simpler "higher is better" semantic.

- [ ] **Step 1: Write the test (red)**

Create: `app/src/test/java/com/stretchdaily/app/ui/screen/log/LogSparklineTest.kt`

```kotlin
package com.stretchdaily.app.ui.screen.log

import com.stretchdaily.app.core.model.BenchmarkLog
import com.stretchdaily.app.core.model.FlexibilityTier
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogSparklineTest {

    private val zone = ZoneId.of("UTC")
    private val now = ZonedDateTime.of(2026, 6, 15, 12, 0, 0, 0, zone)
        .toInstant().toEpochMilli()

    private fun log(tier: FlexibilityTier, daysAgo: Long) = BenchmarkLog(
        id = 0L,
        benchmarkId = "BM",
        rawValue = tier.name,
        resolvedTier = tier,
        loggedAt = Instant.ofEpochMilli(now).minusSeconds(daysAgo * 86_400).toEpochMilli(),
    )

    @Test
    fun `empty input returns empty list`() {
        assertTrue(sparklineValues(emptyList(), now).isEmpty())
    }

    @Test
    fun `single log returns list with one normalized value`() {
        val values = sparklineValues(listOf(log(FlexibilityTier.AVERAGE, 10)), now)
        assertEquals(1, values.size)
        // AVERAGE is the middle of 5 tiers: ordinal math maps it to 0.5.
        assertEquals(0.5, values.single(), 0.001)
    }

    @Test
    fun `tier extremes map to 0 and 1`() {
        val values = sparklineValues(
            listOf(
                log(FlexibilityTier.STIFF, 30),
                log(FlexibilityTier.VERY_FLEXIBLE, 5),
            ),
            now,
        )
        assertEquals(listOf(0.0, 1.0), values)
    }

    @Test
    fun `logs older than 6 months are dropped`() {
        val values = sparklineValues(
            listOf(
                // 220 days back — outside window
                log(FlexibilityTier.STIFF, 220),
                // 30 days back — inside window
                log(FlexibilityTier.VERY_FLEXIBLE, 30),
            ),
            now,
        )
        assertEquals(listOf(1.0), values)
    }

    @Test
    fun `values are sorted oldest first`() {
        val values = sparklineValues(
            listOf(
                // emitted newest-first, but helper should re-sort
                log(FlexibilityTier.VERY_FLEXIBLE, 5),
                log(FlexibilityTier.STIFF, 90),
                log(FlexibilityTier.AVERAGE, 30),
            ),
            now,
        )
        // oldest (90 d) first, then 30 d, then 5 d
        assertEquals(listOf(0.0, 0.5, 1.0), values)
    }
}
```

- [ ] **Step 2: Write the helper (green)**

Create: `app/src/main/java/com/stretchdaily/app/ui/screen/log/LogSparkline.kt`

```kotlin
package com.stretchdaily.app.ui.screen.log

import com.stretchdaily.app.core.model.BenchmarkLog
import com.stretchdaily.app.core.model.FlexibilityTier

/**
 * Builds a list of 0..1 sparkline values for the last six months of [logs],
 * relative to [nowMillis]. Each value encodes the log's resolved tier:
 *
 *  - STIFF → 0.0   (bottom of the sparkline, worst)
 *  - BELOW_AVERAGE → 0.25
 *  - AVERAGE → 0.5
 *  - FLEXIBLE → 0.75
 *  - VERY_FLEXIBLE → 1.0 (top of the sparkline, best)
 *
 * Empty when there are no in-window logs — the `Sparkline` primitive renders
 * a dashed flat line in that case (see R2 `Sparkline.kt`).
 *
 * Kept as a top-level function (not a companion of `BenchmarkProgressBuilder`)
 * so its "higher value is better" semantic stays decoupled from the inverted-y
 * coordinate system `BigChart` requires.
 */
private const val SIX_MONTHS_MILLIS = 6L * 30L * 24L * 60L * 60L * 1000L

internal fun sparklineValues(
    logs: List<BenchmarkLog>,
    nowMillis: Long,
): List<Double> {
    val cutoff = nowMillis - SIX_MONTHS_MILLIS
    return logs
        .asSequence()
        .filter { it.loggedAt >= cutoff }
        .sortedBy { it.loggedAt }
        .map { tierToValue(it.resolvedTier) }
        .toList()
}

private fun tierToValue(tier: FlexibilityTier): Double = when (tier) {
    FlexibilityTier.STIFF -> 0.0
    FlexibilityTier.BELOW_AVERAGE -> 0.25
    FlexibilityTier.AVERAGE -> 0.5
    FlexibilityTier.FLEXIBLE -> 0.75
    FlexibilityTier.VERY_FLEXIBLE -> 1.0
}
```

- [ ] **Step 3: Run the test**

In Android Studio, right-click `LogSparklineTest.kt` → Run. Expected: 5 passing.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/ui/screen/log/LogSparkline.kt \
        app/src/test/java/com/stretchdaily/app/ui/screen/log/LogSparklineTest.kt
git commit -m "feat(log): add sparklineValues pure helper for last-6-months tier window"
git push
```

---

## Task 3: `BenchmarkLogUiState`

The Log tab's render shape. Rows are grouped by category, preserving enum order, matching the reference screen's sectioned list. Each row carries the data it needs to render — including the sparkline values pre-computed by the VM so Compose never touches timestamps.

- [ ] **Step 1: Create `BenchmarkLogUiState.kt`**

Create: `app/src/main/java/com/stretchdaily/app/ui/screen/log/BenchmarkLogUiState.kt`

```kotlin
package com.stretchdaily.app.ui.screen.log

import com.stretchdaily.app.core.model.Benchmark
import com.stretchdaily.app.core.model.BenchmarkLog
import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.core.model.FlexibilityTier

data class BenchmarkLogUiState(
    val isLoading: Boolean = true,
    val groups: List<CategoryGroup> = emptyList(),
    val expandedRowId: String? = null,
    val sheet: LogSheetState = LogSheetState.Hidden,
    val errorMessage: String? = null,
)

data class CategoryGroup(
    val category: Category,
    val rows: List<BenchmarkRowUiState>,
)

data class BenchmarkRowUiState(
    val benchmark: Benchmark,
    val latestLog: BenchmarkLog?,
    val sparkline: List<Double>,
    val history: List<BenchmarkLog>, // newest-first, for the expanded table
    val isOverdue: Boolean,          // no log this calendar month
) {
    val latestTier: FlexibilityTier? get() = latestLog?.resolvedTier
}

sealed interface LogSheetState {
    data object Hidden : LogSheetState
    data class Visible(val benchmark: Benchmark) : LogSheetState
}
```

- [ ] **Step 2: Verify compile**

Build → Make Project. Expected: clean.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/ui/screen/log/BenchmarkLogUiState.kt
git commit -m "feat(log): add BenchmarkLogUiState shape — grouped rows + sheet state"
git push
```

---

## Task 4: TDD `BenchmarkLogViewModel`

Drives Task 3's state. Composes three repository flows:

- `observeAllBenchmarks()` for the catalog,
- `observeLatestLogs()` for each row's latest value,
- For every benchmark in the catalog, `observeLogsFor(benchmarkId)` for sparkline + history.

The last one fans out — we use `combine` over a dynamically-built list of flows when the catalog emits. That's a heavier combine, so we keep the math in the companion and the flow orchestration in the VM.

Two user actions: `onRowTapped(benchmarkId)` (expand/collapse), `onLogPressed(benchmarkId)` (open sheet), `onDismissSheet()`, `onSubmit(rawValue, tierOverride?)`, `onDeleteLog(logId)`.

- [ ] **Step 1: Write the test (red)**

Create: `app/src/test/java/com/stretchdaily/app/ui/screen/log/BenchmarkLogViewModelTest.kt`

```kotlin
package com.stretchdaily.app.ui.screen.log

import com.stretchdaily.app.core.model.Benchmark
import com.stretchdaily.app.core.model.BenchmarkInputType
import com.stretchdaily.app.core.model.BenchmarkLog
import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.core.model.FlexibilityTier
import com.stretchdaily.app.core.util.Clock
import com.stretchdaily.app.data.BenchmarkRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BenchmarkLogViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repo: BenchmarkRepository
    private val zone = ZoneId.of("UTC")
    private val now = ZonedDateTime.of(2026, 6, 15, 12, 0, 0, 0, zone)
        .toInstant().toEpochMilli()
    private val clock = Clock { now }

    private val sitReach = Benchmark(
        id = "BM_SIT_AND_REACH",
        name = "Sit and Reach",
        category = Category.SPINE,
        description = "Reach forward, legs straight.",
        unit = "cm",
        inputType = BenchmarkInputType.NUMERIC,
        tierRanges = emptyMap(),
    )

    private val atgSquat = Benchmark(
        id = "BM_ATG_SPLIT_SQUAT",
        name = "ATG Split Squat",
        category = Category.KNEES,
        description = "Depth control under load.",
        unit = "",
        inputType = BenchmarkInputType.CATEGORICAL,
        tierRanges = emptyMap(),
    )

    private fun log(
        id: Long,
        benchmarkId: String,
        tier: FlexibilityTier,
        daysAgo: Long,
        raw: String = tier.name,
    ) = BenchmarkLog(
        id = id,
        benchmarkId = benchmarkId,
        rawValue = raw,
        resolvedTier = tier,
        loggedAt = Instant.ofEpochMilli(now)
            .minusSeconds(daysAgo * 86_400).toEpochMilli(),
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repo = mockk(relaxed = true)
    }

    @After fun teardown() = Dispatchers.resetMain()

    private fun viewModel(): BenchmarkLogViewModel =
        BenchmarkLogViewModel(repo, clock)

    @Test
    fun `initial state is loading`() = runTest {
        coEvery { repo.observeAllBenchmarks() } returns MutableStateFlow(emptyList())
        coEvery { repo.observeLatestLogs() } returns MutableStateFlow(emptyList())
        coEvery { repo.observeLogsFor(any()) } returns MutableStateFlow(emptyList())
        val vm = viewModel()
        // Pre-advance: initial StateFlow value is still loading.
        assertTrue(vm.state.value.isLoading)
    }

    @Test
    fun `rows group by category in enum order`() = runTest {
        coEvery { repo.observeAllBenchmarks() } returns flowOf(listOf(atgSquat, sitReach))
        coEvery { repo.observeLatestLogs() } returns flowOf(emptyList())
        coEvery { repo.observeLogsFor(any()) } returns flowOf(emptyList())

        val vm = viewModel()
        advanceUntilIdle()

        val groups = vm.state.value.groups
        // SPINE comes before KNEES in Category enum.
        assertEquals(listOf(Category.SPINE, Category.KNEES), groups.map { it.category })
    }

    @Test
    fun `row exposes latest log and sparkline values in window`() = runTest {
        val older = log(1, sitReach.id, FlexibilityTier.STIFF, daysAgo = 90)
        val recent = log(2, sitReach.id, FlexibilityTier.AVERAGE, daysAgo = 20)
        coEvery { repo.observeAllBenchmarks() } returns flowOf(listOf(sitReach))
        coEvery { repo.observeLatestLogs() } returns flowOf(listOf(recent))
        coEvery { repo.observeLogsFor(sitReach.id) } returns flowOf(listOf(older, recent))

        val vm = viewModel()
        advanceUntilIdle()

        val row = vm.state.value.groups.single().rows.single()
        assertEquals(recent.id, row.latestLog?.id)
        assertEquals(listOf(0.0, 0.5), row.sparkline) // oldest→newest
        // History is newest-first for the expanded table.
        assertEquals(listOf(recent.id, older.id), row.history.map { it.id })
    }

    @Test
    fun `overdue flag is true when latest log predates this calendar month`() = runTest {
        // today is June 15, 2026. A log from May 20 → predates June → overdue.
        val lastMonthLog = log(5, sitReach.id, FlexibilityTier.AVERAGE, daysAgo = 26)
        coEvery { repo.observeAllBenchmarks() } returns flowOf(listOf(sitReach))
        coEvery { repo.observeLatestLogs() } returns flowOf(listOf(lastMonthLog))
        coEvery { repo.observeLogsFor(sitReach.id) } returns flowOf(listOf(lastMonthLog))

        val vm = viewModel()
        advanceUntilIdle()

        assertTrue(vm.state.value.groups.single().rows.single().isOverdue)
    }

    @Test
    fun `overdue flag is false when a log exists this calendar month`() = runTest {
        val thisMonthLog = log(6, sitReach.id, FlexibilityTier.AVERAGE, daysAgo = 3)
        coEvery { repo.observeAllBenchmarks() } returns flowOf(listOf(sitReach))
        coEvery { repo.observeLatestLogs() } returns flowOf(listOf(thisMonthLog))
        coEvery { repo.observeLogsFor(sitReach.id) } returns flowOf(listOf(thisMonthLog))

        val vm = viewModel()
        advanceUntilIdle()

        assertFalse(vm.state.value.groups.single().rows.single().isOverdue)
    }

    @Test
    fun `overdue flag is true when user has never logged`() = runTest {
        coEvery { repo.observeAllBenchmarks() } returns flowOf(listOf(sitReach))
        coEvery { repo.observeLatestLogs() } returns flowOf(emptyList())
        coEvery { repo.observeLogsFor(sitReach.id) } returns flowOf(emptyList())

        val vm = viewModel()
        advanceUntilIdle()

        val row = vm.state.value.groups.single().rows.single()
        assertNull(row.latestLog)
        assertTrue(row.isOverdue)
    }

    @Test
    fun `onRowTapped toggles expansion`() = runTest {
        coEvery { repo.observeAllBenchmarks() } returns flowOf(listOf(sitReach))
        coEvery { repo.observeLatestLogs() } returns flowOf(emptyList())
        coEvery { repo.observeLogsFor(any()) } returns flowOf(emptyList())
        val vm = viewModel()
        advanceUntilIdle()

        assertNull(vm.state.value.expandedRowId)
        vm.onRowTapped(sitReach.id)
        assertEquals(sitReach.id, vm.state.value.expandedRowId)
        vm.onRowTapped(sitReach.id)
        assertNull(vm.state.value.expandedRowId)
    }

    @Test
    fun `onLogPressed then onDismissSheet flips the sheet visibility`() = runTest {
        coEvery { repo.observeAllBenchmarks() } returns flowOf(listOf(sitReach))
        coEvery { repo.observeLatestLogs() } returns flowOf(emptyList())
        coEvery { repo.observeLogsFor(any()) } returns flowOf(emptyList())
        val vm = viewModel()
        advanceUntilIdle()

        vm.onLogPressed(sitReach.id)
        val visible = vm.state.value.sheet
        assertTrue(visible is LogSheetState.Visible)
        assertEquals(sitReach.id, (visible as LogSheetState.Visible).benchmark.id)

        vm.onDismissSheet()
        assertTrue(vm.state.value.sheet is LogSheetState.Hidden)
    }

    @Test
    fun `onSubmit numeric calls repository logNumeric then closes sheet`() = runTest {
        coEvery { repo.observeAllBenchmarks() } returns flowOf(listOf(sitReach))
        coEvery { repo.observeLatestLogs() } returns flowOf(emptyList())
        coEvery { repo.observeLogsFor(any()) } returns flowOf(emptyList())
        coEvery { repo.logNumeric(sitReach.id, "12.5") } returns Result.success(1L)
        val vm = viewModel()
        advanceUntilIdle()

        vm.onLogPressed(sitReach.id)
        vm.onSubmit(rawValue = "12.5", tierOverride = null)
        advanceUntilIdle()

        coVerify(exactly = 1) { repo.logNumeric(sitReach.id, "12.5") }
        assertTrue(vm.state.value.sheet is LogSheetState.Hidden)
        assertNull(vm.state.value.errorMessage)
    }

    @Test
    fun `onSubmit categorical calls repository logCategorical`() = runTest {
        coEvery { repo.observeAllBenchmarks() } returns flowOf(listOf(atgSquat))
        coEvery { repo.observeLatestLogs() } returns flowOf(emptyList())
        coEvery { repo.observeLogsFor(any()) } returns flowOf(emptyList())
        coEvery { repo.logCategorical(atgSquat.id, FlexibilityTier.FLEXIBLE) } returns 9L
        val vm = viewModel()
        advanceUntilIdle()

        vm.onLogPressed(atgSquat.id)
        vm.onSubmit(rawValue = FlexibilityTier.FLEXIBLE.name, tierOverride = FlexibilityTier.FLEXIBLE)
        advanceUntilIdle()

        coVerify(exactly = 1) { repo.logCategorical(atgSquat.id, FlexibilityTier.FLEXIBLE) }
        assertTrue(vm.state.value.sheet is LogSheetState.Hidden)
    }

    @Test
    fun `numeric submit surfacing parse failure sets error message and keeps sheet open`() = runTest {
        coEvery { repo.observeAllBenchmarks() } returns flowOf(listOf(sitReach))
        coEvery { repo.observeLatestLogs() } returns flowOf(emptyList())
        coEvery { repo.observeLogsFor(any()) } returns flowOf(emptyList())
        coEvery { repo.logNumeric(sitReach.id, "oops") } returns
            Result.failure(IllegalArgumentException("Not a number: oops"))
        val vm = viewModel()
        advanceUntilIdle()

        vm.onLogPressed(sitReach.id)
        vm.onSubmit(rawValue = "oops", tierOverride = null)
        advanceUntilIdle()

        assertTrue(vm.state.value.sheet is LogSheetState.Visible)
        assertEquals("Not a number: oops", vm.state.value.errorMessage)
    }

    @Test
    fun `onDeleteLog delegates to repository`() = runTest {
        coEvery { repo.observeAllBenchmarks() } returns flowOf(listOf(sitReach))
        coEvery { repo.observeLatestLogs() } returns flowOf(emptyList())
        coEvery { repo.observeLogsFor(any()) } returns flowOf(emptyList())
        val vm = viewModel()
        advanceUntilIdle()

        vm.onDeleteLog(42L)
        advanceUntilIdle()

        coVerify(exactly = 1) { repo.deleteLog(42L) }
    }
}
```

- [ ] **Step 2: Write the ViewModel (green)**

Create: `app/src/main/java/com/stretchdaily/app/ui/screen/log/BenchmarkLogViewModel.kt`

```kotlin
package com.stretchdaily.app.ui.screen.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stretchdaily.app.core.model.Benchmark
import com.stretchdaily.app.core.model.BenchmarkLog
import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.core.model.FlexibilityTier
import com.stretchdaily.app.core.util.Clock
import com.stretchdaily.app.data.BenchmarkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class BenchmarkLogViewModel @Inject constructor(
    private val repository: BenchmarkRepository,
    private val clock: Clock,
) : ViewModel() {

    private val zone: ZoneId = ZoneId.systemDefault()

    private val _ephemeral = MutableStateFlow(EphemeralState())

    private val _state = MutableStateFlow(BenchmarkLogUiState())
    val state: StateFlow<BenchmarkLogUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.Default) {
            val benchmarksFlow = repository.observeAllBenchmarks()
            val latestFlow = repository.observeLatestLogs()

            // For each catalog update, fan out into per-benchmark log flows.
            val derivedFlow: Flow<DerivedCatalog> =
                @Suppress("UNCHECKED_CAST")
                benchmarksFlow.flatMapLatest { benchmarks ->
                    if (benchmarks.isEmpty()) {
                        flowOf(DerivedCatalog(benchmarks = emptyList(), perBenchmarkLogs = emptyMap()))
                    } else {
                        val flows: List<Flow<Pair<String, List<BenchmarkLog>>>> =
                            benchmarks.map { bm ->
                                repository.observeLogsFor(bm.id).let { f ->
                                    f.let { inner ->
                                        kotlinx.coroutines.flow.flow {
                                            inner.collect { logs -> emit(bm.id to logs) }
                                        }
                                    }
                                }
                            }
                        combine(flows) { pairs ->
                            DerivedCatalog(
                                benchmarks = benchmarks,
                                perBenchmarkLogs = pairs.toMap(),
                            )
                        }
                    }
                }

            combine(derivedFlow, latestFlow, _ephemeral) { derived, latest, ephemeral ->
                buildState(derived, latest, ephemeral, clock.now(), zone)
            }.collect { next -> _state.value = next }
        }
    }

    fun onRowTapped(benchmarkId: String) {
        _ephemeral.update {
            it.copy(expandedRowId = if (it.expandedRowId == benchmarkId) null else benchmarkId)
        }
    }

    fun onLogPressed(benchmarkId: String) {
        val bm = _state.value.groups.asSequence()
            .flatMap { it.rows.asSequence() }
            .firstOrNull { it.benchmark.id == benchmarkId }?.benchmark
            ?: return
        _ephemeral.update { it.copy(sheet = LogSheetState.Visible(bm), errorMessage = null) }
    }

    fun onDismissSheet() {
        _ephemeral.update { it.copy(sheet = LogSheetState.Hidden, errorMessage = null) }
    }

    /**
     * Save entry. For numeric benchmarks pass [tierOverride] = null; for categorical pass the chosen tier.
     */
    fun onSubmit(rawValue: String, tierOverride: FlexibilityTier?) {
        val current = (_ephemeral.value.sheet as? LogSheetState.Visible)?.benchmark ?: return
        viewModelScope.launch {
            val outcome: Result<Unit> = if (tierOverride == null) {
                repository.logNumeric(current.id, rawValue).map { }
            } else {
                runCatching { repository.logCategorical(current.id, tierOverride) }.map { }
            }
            outcome.fold(
                onSuccess = {
                    _ephemeral.update { it.copy(sheet = LogSheetState.Hidden, errorMessage = null) }
                },
                onFailure = { t ->
                    _ephemeral.update { it.copy(errorMessage = t.message ?: "Could not save entry") }
                },
            )
        }
    }

    fun onDeleteLog(logId: Long) {
        viewModelScope.launch { repository.deleteLog(logId) }
    }

    fun dismissError() {
        _ephemeral.update { it.copy(errorMessage = null) }
    }

    // ────────────────────────────────────────────
    // Internal helpers

    private data class EphemeralState(
        val expandedRowId: String? = null,
        val sheet: LogSheetState = LogSheetState.Hidden,
        val errorMessage: String? = null,
    )

    private data class DerivedCatalog(
        val benchmarks: List<Benchmark>,
        val perBenchmarkLogs: Map<String, List<BenchmarkLog>>,
    )

    private fun buildState(
        derived: DerivedCatalog,
        latest: List<BenchmarkLog>,
        ephemeral: EphemeralState,
        nowMillis: Long,
        zone: ZoneId,
    ): BenchmarkLogUiState {
        val latestById = latest.associateBy { it.benchmarkId }
        val todayFirstOfMonth = LocalDate.ofInstant(Instant.ofEpochMilli(nowMillis), zone)
            .withDayOfMonth(1)

        val rows = derived.benchmarks.map { bm ->
            val allLogs = derived.perBenchmarkLogs[bm.id].orEmpty()
            val latestLog = latestById[bm.id]
            val isOverdue = isOverdue(latestLog, todayFirstOfMonth, zone)
            BenchmarkRowUiState(
                benchmark = bm,
                latestLog = latestLog,
                sparkline = sparklineValues(allLogs, nowMillis),
                history = allLogs.sortedByDescending { it.loggedAt },
                isOverdue = isOverdue,
            )
        }

        // Group by category in enum order.
        val grouped = rows
            .groupBy { it.benchmark.category }
            .toSortedMap(compareBy { it.ordinal })
            .map { (category, rs) -> CategoryGroup(category, rs) }

        return BenchmarkLogUiState(
            isLoading = false,
            groups = grouped,
            expandedRowId = ephemeral.expandedRowId,
            sheet = ephemeral.sheet,
            errorMessage = ephemeral.errorMessage,
        )
    }

    private fun isOverdue(
        latestLog: BenchmarkLog?,
        monthStart: LocalDate,
        zone: ZoneId,
    ): Boolean {
        if (latestLog == null) return true
        val loggedDate = LocalDate.ofInstant(Instant.ofEpochMilli(latestLog.loggedAt), zone)
        return loggedDate.isBefore(monthStart)
    }

    companion object {
        /** Visible for tests — shared tier category-order comparator could live here if needed. */
        @Suppress("unused")
        private val CATEGORY_ORDER: Comparator<Category> = compareBy { it.ordinal }
    }
}
```

- [ ] **Step 3: Run the tests**

Android Studio → right-click `BenchmarkLogViewModelTest` → Run. Expected: 11 passing.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/ui/screen/log/BenchmarkLogViewModel.kt \
        app/src/test/java/com/stretchdaily/app/ui/screen/log/BenchmarkLogViewModelTest.kt
git commit -m "feat(log): BenchmarkLogViewModel composes repo flows into grouped UiState

State is built from observeAllBenchmarks(), observeLatestLogs(), and a
flatMapLatest over observeLogsFor(...) per benchmark. Each row carries
pre-computed sparkline values + this-month overdue flag. Actions for
expand/collapse, open/dismiss sheet, submit (numeric or categorical),
delete log. Error messages surface without closing the sheet."
git push
```

---

## Task 5: Build the shared `LogForm` composable

Reused by:

- **Log screen** (inside an R2 `Sheet`, via `BenchmarkLogScreen`)
- **Carousel** (inline, inside `BenchmarkCarouselScreen`)

Signature:

```kotlin
LogForm(
    benchmark: Benchmark,
    onSubmit: (rawValue: String, tier: FlexibilityTier?) -> Unit,
    onCancel: () -> Unit,
    errorMessage: String? = null,
)
```

For `BenchmarkInputType.NUMERIC`: one centered text field, unit suffix at baseline right.
For `BenchmarkInputType.CATEGORICAL`: 5 vertical band buttons (`FlexibilityTier.values()`). Selected turns accent. On Save, emits `(tier.name, tier)`.

- [ ] **Step 1: Create `LogForm.kt`**

Create: `app/src/main/java/com/stretchdaily/app/ui/screen/log/LogForm.kt`

```kotlin
package com.stretchdaily.app.ui.screen.log

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stretchdaily.app.core.model.Benchmark
import com.stretchdaily.app.core.model.BenchmarkInputType
import com.stretchdaily.app.core.model.FlexibilityTier
import com.stretchdaily.app.ui.components.AppIcon
import com.stretchdaily.app.ui.components.IconName
import com.stretchdaily.app.ui.components.MonoCaps
import com.stretchdaily.app.ui.components.Pill
import com.stretchdaily.app.ui.components.PillVariant
import com.stretchdaily.app.ui.theme.Theme

@Composable
fun LogForm(
    benchmark: Benchmark,
    onSubmit: (rawValue: String, tier: FlexibilityTier?) -> Unit,
    onCancel: () -> Unit,
    errorMessage: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (benchmark.description.isNotBlank()) {
            Text(
                text = benchmark.description,
                style = Theme.typo.body,
                color = Theme.colors.ink2,
                modifier = Modifier.padding(bottom = 14.dp),
            )
        }

        when (benchmark.inputType) {
            BenchmarkInputType.NUMERIC -> NumericInput(
                unit = benchmark.unit,
                onSubmit = { raw -> onSubmit(raw, null) },
                onCancel = onCancel,
            )
            BenchmarkInputType.CATEGORICAL -> CategoricalInput(
                onSubmit = { tier -> onSubmit(tier.name, tier) },
                onCancel = onCancel,
            )
        }

        if (!errorMessage.isNullOrBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = errorMessage,
                style = Theme.typo.body,
                color = Theme.colors.warn,
            )
        }
    }
}

@Composable
private fun NumericInput(
    unit: String,
    onSubmit: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var raw by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        MonoCaps(
            text = "Your score",
            size = 10.sp,
            color = Theme.colors.ink3,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(Theme.dims.radiusMd))
                    .background(Theme.colors.bg)
                    .border(
                        width = 1.dp,
                        color = Theme.colors.line,
                        shape = RoundedCornerShape(Theme.dims.radiusMd),
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                BasicTextField(
                    value = raw,
                    onValueChange = { raw = it.filter { ch -> ch.isDigit() || ch == '.' || ch == '-' || ch == ',' } },
                    textStyle = LocalTextStyle.current.copy(
                        color = Theme.colors.ink,
                        fontSize = 30.sp,
                        fontFamily = Theme.typo.display.fontFamily,
                        textAlign = TextAlign.Center,
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (unit.isNotBlank()) {
                Text(
                    text = unit,
                    style = Theme.typo.body,
                    color = Theme.colors.ink3,
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        FormFooter(
            saveEnabled = raw.isNotBlank(),
            onCancel = onCancel,
            onSave = { onSubmit(raw.trim()) },
        )
    }
}

@Composable
private fun CategoricalInput(
    onSubmit: (FlexibilityTier) -> Unit,
    onCancel: () -> Unit,
) {
    var selected by remember { mutableStateOf<FlexibilityTier?>(null) }
    val tiers = remember {
        // Display order: most flexible at the top.
        listOf(
            FlexibilityTier.VERY_FLEXIBLE,
            FlexibilityTier.FLEXIBLE,
            FlexibilityTier.AVERAGE,
            FlexibilityTier.BELOW_AVERAGE,
            FlexibilityTier.STIFF,
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        MonoCaps(
            text = "Pick your level",
            size = 10.sp,
            color = Theme.colors.ink3,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            tiers.forEach { tier ->
                val isSel = tier == selected
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Theme.dims.radiusMd))
                        .background(if (isSel) Theme.colors.accent else Theme.colors.bg)
                        .clickable { selected = tier }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = tier.name.replace('_', ' ').lowercase()
                            .replaceFirstChar { it.titlecase() },
                        style = Theme.typo.body,
                        color = if (isSel) Theme.colors.accentInk else Theme.colors.ink,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        FormFooter(
            saveEnabled = selected != null,
            onCancel = onCancel,
            onSave = { selected?.let(onSubmit) },
        )
    }
}

@Composable
private fun FormFooter(
    saveEnabled: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Pill(
            onClick = onCancel,
            label = "Cancel",
            variant = PillVariant.Neutral,
            modifier = Modifier.weight(1f),
        )
        Pill(
            onClick = { if (saveEnabled) onSave() },
            label = "Save entry",
            leadingIcon = IconName.Check,
            variant = PillVariant.Accent,
            modifier = Modifier.weight(2f),
        )
    }
}
```

**Note on `Pill` shape:** R2 provided `Pill(onClick, label, leadingIcon, variant, modifier)`. If the actual signature from R2 differs (optional leadingIcon handled differently), adjust these calls accordingly. The footer layout (1:2 weight ratio, Cancel left / Save entry right) matches the handoff.

**Note on `isDigit()`:** The filter permits negative sign `-` for Sit-and-Reach, which can go below 0, plus `.` and `,` (the repository's `parseNumeric` already normalises `,` → `.`).

- [ ] **Step 2: Verify compile**

Build → Make Project. If `Pill` doesn't accept a leading icon, pass the icon via a trailing slot or switch the Save button to a `Row`-wrapped accent container.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/ui/screen/log/LogForm.kt
git commit -m "feat(log): add shared LogForm (numeric + categorical variants)"
git push
```

---

## Task 6: Build `BenchmarkLogScreen`

Top-level layout matches handoff §5:

```
Scaffold (inner; tab-hosted, contentPadding from outer)
│
├─ Column (padding 20 dp horizontal, 100 dp bottom inset for bottom bar)
│   ├─ Hero header
│   │   ├─ MonoCaps("Monthly benchmarks")
│   │   ├─ Display "Log & review"
│   │   └─ Body description
│   ├─ "Start benchmark day" CTA card (accent-soft bg, sparkle tile, chevron R)
│   └─ LazyColumn
│       ├─ For each category group:
│       │   ├─ Header row: 10×10 dp category-tinted square + MonoCaps category name
│       │   └─ For each benchmark in the group → BenchmarkLogRow
│       └─ Spacer(20 dp)
│
└─ Sheet (visible when state.sheet is Visible): LogForm
```

`BenchmarkLogRow` includes the expand-to-reveal band list + history.

- [ ] **Step 1: Create `BenchmarkLogScreen.kt`**

Create: `app/src/main/java/com/stretchdaily/app/ui/screen/log/BenchmarkLogScreen.kt`

```kotlin
package com.stretchdaily.app.ui.screen.log

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stretchdaily.app.core.model.Benchmark
import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.core.model.FlexibilityTier
import com.stretchdaily.app.ui.components.AppIcon
import com.stretchdaily.app.ui.components.BandPill
import com.stretchdaily.app.ui.components.IconName
import com.stretchdaily.app.ui.components.MonoCaps
import com.stretchdaily.app.ui.components.Pill
import com.stretchdaily.app.ui.components.PillVariant
import com.stretchdaily.app.ui.components.Sheet
import com.stretchdaily.app.ui.components.Sparkline
import com.stretchdaily.app.ui.theme.Theme
import com.stretchdaily.app.ui.theme.tint

@Composable
fun BenchmarkLogScreen(
    onStartCarousel: () -> Unit,
    contentPadding: PaddingValues,
    viewModel: BenchmarkLogViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LazyColumn(
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 6.dp,
            bottom = 100.dp + contentPadding.calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        item { HeroHeader() }

        item {
            Spacer(Modifier.height(14.dp))
            CarouselCtaCard(onClick = onStartCarousel)
            Spacer(Modifier.height(18.dp))
        }

        state.groups.forEach { group ->
            item {
                CategoryGroupHeader(group.category)
                Spacer(Modifier.height(8.dp))
            }
            items(group.rows, key = { it.benchmark.id }) { row ->
                BenchmarkLogRow(
                    row = row,
                    expanded = row.benchmark.id == state.expandedRowId,
                    onToggle = { viewModel.onRowTapped(row.benchmark.id) },
                    onLog = { viewModel.onLogPressed(row.benchmark.id) },
                )
                Spacer(Modifier.height(8.dp))
            }
            item { Spacer(Modifier.height(10.dp)) }
        }
    }

    when (val sheet = state.sheet) {
        is LogSheetState.Hidden -> Unit
        is LogSheetState.Visible -> Sheet(onDismiss = { viewModel.onDismissSheet() }) {
            Column {
                Text(
                    text = sheet.benchmark.name,
                    style = Theme.typo.display,
                    fontSize = 20.sp,
                    color = Theme.colors.ink,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
                LogForm(
                    benchmark = sheet.benchmark,
                    onSubmit = { raw, tier -> viewModel.onSubmit(raw, tier) },
                    onCancel = { viewModel.onDismissSheet() },
                    errorMessage = state.errorMessage,
                )
            }
        }
    }
}

@Composable
private fun HeroHeader() {
    Column(modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)) {
        MonoCaps(
            text = "Monthly benchmarks",
            size = 10.sp,
            color = Theme.colors.ink3,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        Text(
            text = "Log & review",
            style = Theme.typo.display,
            fontSize = 30.sp,
            fontWeight = FontWeight.W500,
            color = Theme.colors.ink,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        Text(
            text = "Ten benchmarks across seven categories. Logged on the 1st of each month.",
            style = Theme.typo.body,
            color = Theme.colors.ink2,
        )
    }
}

@Composable
private fun CarouselCtaCard(onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Theme.dims.radiusMd))
            .background(Theme.colors.accentSoft)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 13.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(Theme.dims.radiusSm))
                .background(Theme.colors.accent),
        ) {
            AppIcon(name = IconName.Sparkle, size = 16.dp, tint = Theme.colors.accentInk)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Start benchmark day",
                style = Theme.typo.body.copy(fontWeight = FontWeight.W600),
                fontSize = 13.5.sp,
                color = Theme.colors.ink,
            )
            Text(
                text = "Walk through all 10, one at a time",
                style = Theme.typo.body,
                fontSize = 11.sp,
                color = Theme.colors.ink2,
            )
        }
        AppIcon(name = IconName.ChevronRight, size = 16.dp, tint = Theme.colors.ink2)
    }
}

@Composable
private fun CategoryGroupHeader(category: Category) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(category.tint()),
        )
        MonoCaps(
            text = category.name,
            size = 10.sp,
            color = Theme.colors.ink2,
        )
    }
}

@Composable
private fun BenchmarkLogRow(
    row: BenchmarkRowUiState,
    expanded: Boolean,
    onToggle: () -> Unit,
    onLog: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Theme.dims.radiusMd))
            .border(
                width = 1.dp,
                color = Theme.colors.line,
                shape = RoundedCornerShape(Theme.dims.radiusMd),
            )
            .background(Theme.colors.surface),
    ) {
        // Header row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                NameLine(name = row.benchmark.name, isOverdue = row.isOverdue)
                Spacer(Modifier.height(4.dp))
                ValueLine(row = row)
            }
            Pill(
                onClick = onLog,
                label = "Log",
                leadingIcon = IconName.Plus,
                variant = PillVariant.Accent,
            )
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center,
            ) {
                AppIcon(
                    name = IconName.ChevronDown,
                    size = 18.dp,
                    tint = Theme.colors.ink3,
                    modifier = Modifier.rotate(if (expanded) 180f else 0f),
                )
            }
        }

        AnimatedVisibility(visible = expanded) {
            ExpandedDetails(row = row)
        }
    }
}

@Composable
private fun NameLine(name: String, isOverdue: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = name,
            style = Theme.typo.body.copy(fontWeight = FontWeight.W600),
            fontSize = 13.5.sp,
            color = Theme.colors.ink,
        )
        if (isOverdue) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Theme.dims.radiusPill))
                    .background(Theme.colors.warn.copy(alpha = 0.12f))
                    .padding(horizontal = 6.dp, vertical = 1.dp),
            ) {
                MonoCaps(
                    text = "Overdue",
                    size = 9.5.sp,
                    color = Theme.colors.warn,
                )
            }
        }
    }
}

@Composable
private fun ValueLine(row: BenchmarkRowUiState) {
    if (row.latestLog == null) {
        Text(
            text = "Not logged yet",
            style = Theme.typo.body,
            fontSize = 11.sp,
            color = Theme.colors.ink3,
        )
        return
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = row.latestLog.rawValue,
                style = Theme.typo.display,
                fontSize = 17.sp,
                fontWeight = FontWeight.W500,
                color = Theme.colors.ink,
            )
            if (row.benchmark.unit.isNotBlank()) {
                Spacer(Modifier.width(2.dp))
                Text(
                    text = row.benchmark.unit,
                    style = Theme.typo.body,
                    fontSize = 11.sp,
                    color = Theme.colors.ink3,
                )
            }
        }
        row.latestTier?.let { BandPill(tier = it) }
        Sparkline(values = row.sparkline)
    }
}

@Composable
private fun ExpandedDetails(row: BenchmarkRowUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Theme.colors.bg)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        if (row.benchmark.description.isNotBlank()) {
            Text(
                text = row.benchmark.description,
                style = Theme.typo.body,
                fontSize = 11.5.sp,
                color = Theme.colors.ink2,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }
        MonoCaps(
            text = "Bands",
            size = 9.5.sp,
            color = Theme.colors.ink3,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        BandsList(row = row)

        if (row.history.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            MonoCaps(
                text = "History",
                size = 9.5.sp,
                color = Theme.colors.ink3,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            HistoryList(row = row)
        }
    }
}

@Composable
private fun BandsList(row: BenchmarkRowUiState) {
    val tiers = listOf(
        FlexibilityTier.VERY_FLEXIBLE,
        FlexibilityTier.FLEXIBLE,
        FlexibilityTier.AVERAGE,
        FlexibilityTier.BELOW_AVERAGE,
        FlexibilityTier.STIFF,
    )
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        tiers.forEach { tier ->
            val highlighted = tier == row.latestTier
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (highlighted) Theme.colors.accent else Theme.colors.line),
                )
                Text(
                    text = tier.name.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() },
                    style = Theme.typo.body.copy(
                        fontWeight = if (highlighted) FontWeight.W600 else FontWeight.W400,
                    ),
                    fontSize = 11.sp,
                    color = if (highlighted) Theme.colors.ink else Theme.colors.ink2,
                    modifier = Modifier.width(100.dp),
                )
                val hint = row.benchmark.tierRanges[tier.name].orEmpty()
                MonoCaps(text = hint, size = 10.5.sp, color = Theme.colors.ink3)
            }
        }
    }
}

@Composable
private fun HistoryList(row: BenchmarkRowUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        row.history.forEachIndexed { index, log ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
            ) {
                Text(
                    text = formatMonth(log.loggedAt),
                    style = Theme.typo.body,
                    fontSize = 11.5.sp,
                    color = Theme.colors.ink2,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val valueText = buildString {
                        append(log.rawValue)
                        if (row.benchmark.unit.isNotBlank()) {
                            append(' ')
                            append(row.benchmark.unit)
                        }
                    }
                    Text(
                        text = valueText,
                        style = Theme.typo.body.copy(fontWeight = FontWeight.W700),
                        fontSize = 11.5.sp,
                        color = Theme.colors.ink,
                    )
                    BandPill(tier = log.resolvedTier)
                }
            }
            if (index != row.history.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Theme.colors.line2),
                )
            }
        }
    }
}

private fun formatMonth(epochMilli: Long): String {
    val date = java.time.LocalDate.ofInstant(
        java.time.Instant.ofEpochMilli(epochMilli),
        java.time.ZoneId.systemDefault(),
    )
    return date.format(
        java.time.format.DateTimeFormatter.ofPattern("MMM yyyy", java.util.Locale.ENGLISH)
    )
}
```

**Adjustment notes:**

- `BandPill` was spec'd in R2 as `BandPill(tier: FlexibilityTier)`. Confirm the signature there; if it includes a `small` flag, pass `small = true` in the history list.
- `Sparkline` takes a `List<Double>`. We pass `row.sparkline` directly.
- `Theme.colors.accentSoft` should exist from R1 — confirm; if it's named `accentSoft` or `accent2` or missing, substitute or add a token.
- `IconName.Plus` is referenced in R3 for another button — confirm it exists; if not, `AppIcon` can take a different name.

- [ ] **Step 2: Verify compile**

Build → Make Project.

- [ ] **Step 3: Wire the route into `StretchDailyNavHost`**

Locate the `"log"` placeholder (still a `PlaceholderScreen` from R1). Replace with:

```kotlin
composable(ROUTE_LOG) {
    BenchmarkLogScreen(
        onStartCarousel = { navController.navigate("$ROUTE_CAROUSEL_GRAPH/0") },
        contentPadding = innerPadding,
    )
}
```

`ROUTE_CAROUSEL_GRAPH` is defined in Task 10 — you can temporarily make this a stub lambda (`{}`) for this commit and flesh it out at Task 10.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/ui/screen/log/BenchmarkLogScreen.kt \
        app/src/main/java/com/stretchdaily/app/ui/navigation/StretchDailyNavHost.kt
git commit -m "feat(log): BenchmarkLogScreen with grouped rows + expand-to-reveal + sheet

Hero header + 'Start benchmark day' CTA card + category groups with the
shared BenchmarkLogRow (value + BandPill + Sparkline + Log pill + chevron).
Expanded rows show description, bands list (user's current tier highlighted),
and newest-first history. Sheet hosts the shared LogForm."
git push
```

---

## Task 7: `BenchmarkCarouselUiState`

The carousel's shape is deliberately thin — a pointer into an ordered benchmark list + any pending error.

- [ ] **Step 1: Create `BenchmarkCarouselUiState.kt`**

Create: `app/src/main/java/com/stretchdaily/app/ui/screen/carousel/BenchmarkCarouselUiState.kt`

```kotlin
package com.stretchdaily.app.ui.screen.carousel

import com.stretchdaily.app.core.model.Benchmark

data class BenchmarkCarouselUiState(
    val isLoading: Boolean = true,
    val allBenchmarks: List<Benchmark> = emptyList(),
    val currentIndex: Int = 0,
    val errorMessage: String? = null,
) {
    val totalSteps: Int get() = allBenchmarks.size
    val currentBenchmark: Benchmark? get() = allBenchmarks.getOrNull(currentIndex)
    val isLastStep: Boolean get() = currentIndex >= totalSteps - 1
}

/** One-shot events the screen listens for (save-and-advance or carousel-complete). */
sealed interface CarouselEvent {
    data class Advance(val toIndex: Int) : CarouselEvent
    data object Finished : CarouselEvent
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/ui/screen/carousel/BenchmarkCarouselUiState.kt
git commit -m "feat(carousel): add BenchmarkCarouselUiState shape"
git push
```

---

## Task 8: TDD `BenchmarkCarouselViewModel`

Nested-graph-scoped. Constructor loads all 10 benchmarks once on init (via `observeAllBenchmarks`) and holds the ordered list. The current step comes in via the route arg `{step}` — the VM exposes `setStep(i)` which the screen calls from `LaunchedEffect(key = step)` to stay in sync with the back-stack.

Save semantics:

- Numeric: `repository.logNumeric(benchmark.id, rawValue)`.
- Categorical: `repository.logCategorical(benchmark.id, tier)`.

After a successful save, the VM fires a one-shot `CarouselEvent.Advance(next)` or `CarouselEvent.Finished`. Failures surface via `errorMessage` without advancing.

Events come via a `SharedFlow<CarouselEvent>` so back-pressure is never an issue.

- [ ] **Step 1: Write the test (red)**

Create: `app/src/test/java/com/stretchdaily/app/ui/screen/carousel/BenchmarkCarouselViewModelTest.kt`

```kotlin
package com.stretchdaily.app.ui.screen.carousel

import com.stretchdaily.app.core.model.Benchmark
import com.stretchdaily.app.core.model.BenchmarkInputType
import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.core.model.FlexibilityTier
import com.stretchdaily.app.data.BenchmarkRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BenchmarkCarouselViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repo: BenchmarkRepository

    private fun bm(id: String, type: BenchmarkInputType = BenchmarkInputType.NUMERIC) =
        Benchmark(
            id = id,
            name = id,
            category = Category.SPINE,
            description = "",
            unit = "cm",
            inputType = type,
            tierRanges = emptyMap(),
        )

    private val ten = (1..10).map { bm("BM_$it") }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repo = mockk(relaxed = true)
    }

    @After fun teardown() = Dispatchers.resetMain()

    private fun viewModel(): BenchmarkCarouselViewModel =
        BenchmarkCarouselViewModel(repo)

    @Test
    fun `init loads all benchmarks`() = runTest {
        coEvery { repo.observeAllBenchmarks() } returns flowOf(ten)
        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.isLoading)
        assertEquals(10, state.totalSteps)
        assertEquals(0, state.currentIndex)
        assertEquals("BM_1", state.currentBenchmark?.id)
    }

    @Test
    fun `setStep moves currentIndex`() = runTest {
        coEvery { repo.observeAllBenchmarks() } returns flowOf(ten)
        val vm = viewModel()
        advanceUntilIdle()

        vm.setStep(4)
        assertEquals(4, vm.state.value.currentIndex)
        assertEquals("BM_5", vm.state.value.currentBenchmark?.id)
    }

    @Test
    fun `setStep clamps negative or past-end values`() = runTest {
        coEvery { repo.observeAllBenchmarks() } returns flowOf(ten)
        val vm = viewModel()
        advanceUntilIdle()

        vm.setStep(-3)
        assertEquals(0, vm.state.value.currentIndex)
        vm.setStep(99)
        assertEquals(9, vm.state.value.currentIndex)
    }

    @Test
    fun `onSaveEntry numeric advances step`() = runTest {
        coEvery { repo.observeAllBenchmarks() } returns flowOf(ten)
        coEvery { repo.logNumeric(any(), any()) } returns Result.success(1L)
        val vm = viewModel()
        advanceUntilIdle()

        val captured = mutableListOf<CarouselEvent>()
        val job = launch { vm.events.toList(captured) }

        vm.onSaveEntry("5.5", tier = null)
        advanceUntilIdle()

        coVerify(exactly = 1) { repo.logNumeric("BM_1", "5.5") }
        assertEquals(listOf(CarouselEvent.Advance(1)), captured)
        job.cancel()
    }

    @Test
    fun `onSaveEntry categorical advances step and calls logCategorical`() = runTest {
        val carousel = listOf(bm("CAT_1", BenchmarkInputType.CATEGORICAL)) + ten
        coEvery { repo.observeAllBenchmarks() } returns flowOf(carousel)
        coEvery { repo.logCategorical(any(), any()) } returns 7L
        val vm = viewModel()
        advanceUntilIdle()

        val captured = mutableListOf<CarouselEvent>()
        val job = launch { vm.events.toList(captured) }

        vm.onSaveEntry("FLEXIBLE", tier = FlexibilityTier.FLEXIBLE)
        advanceUntilIdle()

        coVerify(exactly = 1) { repo.logCategorical("CAT_1", FlexibilityTier.FLEXIBLE) }
        assertEquals(listOf(CarouselEvent.Advance(1)), captured)
        job.cancel()
    }

    @Test
    fun `onSaveEntry on last step fires Finished`() = runTest {
        coEvery { repo.observeAllBenchmarks() } returns flowOf(ten)
        coEvery { repo.logNumeric(any(), any()) } returns Result.success(1L)
        val vm = viewModel()
        advanceUntilIdle()
        vm.setStep(9) // last step

        val captured = mutableListOf<CarouselEvent>()
        val job = launch { vm.events.toList(captured) }

        vm.onSaveEntry("1.0", tier = null)
        advanceUntilIdle()

        coVerify(exactly = 1) { repo.logNumeric("BM_10", "1.0") }
        assertEquals(listOf(CarouselEvent.Finished), captured)
        job.cancel()
    }

    @Test
    fun `onSaveEntry parse failure sets errorMessage and does NOT advance`() = runTest {
        coEvery { repo.observeAllBenchmarks() } returns flowOf(ten)
        coEvery { repo.logNumeric(any(), any()) } returns
            Result.failure(IllegalArgumentException("Not a number: oops"))
        val vm = viewModel()
        advanceUntilIdle()

        val captured = mutableListOf<CarouselEvent>()
        val job = launch { vm.events.toList(captured) }

        vm.onSaveEntry("oops", tier = null)
        advanceUntilIdle()

        assertEquals("Not a number: oops", vm.state.value.errorMessage)
        assertEquals(0, vm.state.value.currentIndex)
        assertTrue(captured.isEmpty())
        job.cancel()
    }

    @Test
    fun `onClose emits Finished`() = runTest {
        coEvery { repo.observeAllBenchmarks() } returns flowOf(ten)
        val vm = viewModel()
        advanceUntilIdle()

        val captured = mutableListOf<CarouselEvent>()
        val job = launch { vm.events.toList(captured) }

        vm.onClose()
        advanceUntilIdle()

        assertEquals(listOf(CarouselEvent.Finished), captured)
        job.cancel()
    }

    @Test
    fun `dismissError clears errorMessage without changing index`() = runTest {
        coEvery { repo.observeAllBenchmarks() } returns flowOf(ten)
        coEvery { repo.logNumeric(any(), any()) } returns
            Result.failure(IllegalArgumentException("Not a number: oops"))
        val vm = viewModel()
        advanceUntilIdle()

        vm.onSaveEntry("oops", tier = null)
        advanceUntilIdle()
        assertEquals("Not a number: oops", vm.state.value.errorMessage)

        vm.dismissError()
        assertEquals(null, vm.state.value.errorMessage)
        assertEquals(0, vm.state.value.currentIndex)
    }
}
```

- [ ] **Step 2: Write the ViewModel (green)**

Create: `app/src/main/java/com/stretchdaily/app/ui/screen/carousel/BenchmarkCarouselViewModel.kt`

```kotlin
package com.stretchdaily.app.ui.screen.carousel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stretchdaily.app.core.model.FlexibilityTier
import com.stretchdaily.app.data.BenchmarkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class BenchmarkCarouselViewModel @Inject constructor(
    private val repository: BenchmarkRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BenchmarkCarouselUiState())
    val state: StateFlow<BenchmarkCarouselUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<CarouselEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<CarouselEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            val benchmarks = repository.observeAllBenchmarks().first()
            _state.update { it.copy(isLoading = false, allBenchmarks = benchmarks) }
        }
    }

    fun setStep(i: Int) {
        _state.update { current ->
            val clamped = i.coerceIn(0, (current.totalSteps - 1).coerceAtLeast(0))
            current.copy(currentIndex = clamped, errorMessage = null)
        }
    }

    fun onSaveEntry(rawValue: String, tier: FlexibilityTier?) {
        val current = _state.value
        val benchmark = current.currentBenchmark ?: return
        viewModelScope.launch {
            val outcome: Result<Unit> = if (tier == null) {
                repository.logNumeric(benchmark.id, rawValue).map { }
            } else {
                runCatching { repository.logCategorical(benchmark.id, tier) }.map { }
            }
            outcome.fold(
                onSuccess = {
                    if (current.isLastStep) {
                        _events.emit(CarouselEvent.Finished)
                    } else {
                        _events.emit(CarouselEvent.Advance(current.currentIndex + 1))
                    }
                },
                onFailure = { t ->
                    _state.update { it.copy(errorMessage = t.message ?: "Could not save entry") }
                },
            )
        }
    }

    fun onClose() {
        viewModelScope.launch { _events.emit(CarouselEvent.Finished) }
    }

    fun dismissError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
```

- [ ] **Step 3: Run the tests**

Android Studio → right-click `BenchmarkCarouselViewModelTest` → Run. Expected: 9 passing.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/ui/screen/carousel/BenchmarkCarouselViewModel.kt \
        app/src/test/java/com/stretchdaily/app/ui/screen/carousel/BenchmarkCarouselViewModelTest.kt
git commit -m "feat(carousel): BenchmarkCarouselViewModel — step orchestration + save-and-advance

State flows from observeAllBenchmarks().first() — the catalog is static once
loaded, so no stream subscription. onSaveEntry dispatches to logNumeric or
logCategorical based on tier argument, then emits CarouselEvent.Advance(next)
or Finished on the last step. Parse failures set errorMessage; the screen
keeps the user on the same step so they can correct input."
git push
```

---

## Task 9: Build `BenchmarkCarouselScreen`

Full-screen overlay (bottom bar hidden via the NavHost's `BOTTOM_NAV_ROUTES` exclusion). Layout matches handoff §305+:

```
Column (fillMaxSize, Theme.colors.bg)
│
├─ Header Row (padding 20/16/20/10)
│   ├─ CircleButton(Close)  → onClose
│   └─ Column(weight 1)
│       ├─ SegmentProgress(total = 10, currentIndex)
│       └─ MonoCaps("BENCHMARK {step+1} OF 10 · {category}")
│
└─ LazyColumn (weight 1, padding bottom 100 dp for IME)
    ├─ Display heading (30 sp)
    ├─ Body description
    └─ LogForm (emits onSubmit/onCancel)
```

`onClose` and a `close`-tap on the Save-Entry path both drive `CarouselEvent.Finished`, which the screen's `LaunchedEffect` observer turns into `navController.popBackStack(ROUTE_CAROUSEL_GRAPH, inclusive = true)`.

- [ ] **Step 1: Create `BenchmarkCarouselScreen.kt`**

Create: `app/src/main/java/com/stretchdaily/app/ui/screen/carousel/BenchmarkCarouselScreen.kt`

```kotlin
package com.stretchdaily.app.ui.screen.carousel

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavBackStackEntry
import com.stretchdaily.app.ui.components.AppIcon
import com.stretchdaily.app.ui.components.CircleButton
import com.stretchdaily.app.ui.components.CircleButtonVariant
import com.stretchdaily.app.ui.components.IconName
import com.stretchdaily.app.ui.components.MonoCaps
import com.stretchdaily.app.ui.components.SegmentProgress
import com.stretchdaily.app.ui.screen.log.LogForm
import com.stretchdaily.app.ui.theme.Theme

@Composable
fun BenchmarkCarouselScreen(
    step: Int,
    parentEntry: NavBackStackEntry,
    onAdvance: (toStep: Int) -> Unit,
    onFinish: () -> Unit,
    viewModel: BenchmarkCarouselViewModel =
        hiltViewModel(viewModelStoreOwner = parentEntry),
) {
    val state by viewModel.state.collectAsState()

    // Sync route → VM whenever the step arg changes
    LaunchedEffect(step) { viewModel.setStep(step) }

    // Route events to navigation
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
            .background(Theme.colors.bg),
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
                        style = Theme.typo.display,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.W500,
                        color = Theme.colors.ink,
                        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp),
                    )
                    if (bm.description.isNotBlank()) {
                        Text(
                            text = bm.description,
                            style = Theme.typo.body,
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
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 10.dp),
    ) {
        CircleButton(
            onClick = onClose,
            icon = IconName.Close,
            variant = CircleButtonVariant.Bg2,
            size = 32.dp,
            contentDescription = "Close benchmark carousel",
        )
        Column(modifier = Modifier.weight(1f)) {
            SegmentProgress(total = total, currentIndex = currentIndex)
            Spacer(Modifier.height(6.dp))
            MonoCaps(
                text = "Benchmark ${currentIndex + 1} of $total · $categoryLabel",
                size = 9.5.sp,
                color = Theme.colors.ink3,
            )
        }
    }
}
```

**Notes:**

- `CircleButton`, `CircleButtonVariant`, `SegmentProgress`, `MonoCaps` are all R2 primitives — adjust imports if their package differs.
- `hiltViewModel(viewModelStoreOwner = parentEntry)` ties the VM lifecycle to the carousel nested graph entry, so navigating from `carousel/0` → `carousel/1` reuses the same VM instance.
- The screen reads `state.totalSteps.coerceAtLeast(1)` when passing to `SegmentProgress` to avoid a divide-by-zero while the catalog is loading.

- [ ] **Step 2: Verify compile**

Build → Make Project.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/ui/screen/carousel/BenchmarkCarouselScreen.kt
git commit -m "feat(carousel): BenchmarkCarouselScreen — full-screen 10-step flow with SegmentProgress header

Shares BenchmarkCarouselViewModel across steps via hiltViewModel(parentEntry).
LaunchedEffect(step) keeps the VM in sync with the route arg when navigating
carousel/N → carousel/N+1. Events collected in a LaunchedEffect turn
Advance / Finished into navigation callbacks."
git push
```

---

## Task 10: Wire the carousel nested graph + entry points into `StretchDailyNavHost`

The carousel is a nested graph so all 10 step routes share one `BenchmarkCarouselViewModel` via `hiltViewModel(parentEntry = carouselGraphEntry)`. Entry points are the Dashboard banner (wired in R3 via an `onBenchmarkBannerClick` lambda) and the Log tab's CTA (wired in Task 6).

- [ ] **Step 1: Define route constants**

Edit `StretchDailyNavHost.kt`. Add near the other route constants:

```kotlin
private const val ROUTE_CAROUSEL_GRAPH = "carousel"
private const val ROUTE_CAROUSEL_STEP = "$ROUTE_CAROUSEL_GRAPH/{step}"
private const val ARG_STEP = "step"
```

- [ ] **Step 2: Register the nested graph**

Add after the `"session"` nested graph block:

```kotlin
navigation(
    route = ROUTE_CAROUSEL_GRAPH,
    startDestination = ROUTE_CAROUSEL_STEP,
) {
    composable(
        route = ROUTE_CAROUSEL_STEP,
        arguments = listOf(
            navArgument(ARG_STEP) { type = NavType.IntType; defaultValue = 0 },
        ),
    ) { backStackEntry ->
        val step = backStackEntry.arguments?.getInt(ARG_STEP) ?: 0
        val parentEntry = remember(backStackEntry) {
            navController.getBackStackEntry(ROUTE_CAROUSEL_GRAPH)
        }
        BenchmarkCarouselScreen(
            step = step,
            parentEntry = parentEntry,
            onAdvance = { next ->
                navController.navigate("$ROUTE_CAROUSEL_GRAPH/$next") {
                    popUpTo(ROUTE_CAROUSEL_STEP) { inclusive = true }
                }
            },
            onFinish = {
                navController.popBackStack(
                    route = ROUTE_CAROUSEL_GRAPH,
                    inclusive = true,
                )
            },
        )
    }
}
```

Imports needed:

```kotlin
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.navArgument
import androidx.compose.runtime.remember
import com.stretchdaily.app.ui.screen.carousel.BenchmarkCarouselScreen
```

**Advance behavior rationale:** Each `navigate(...)` for the next step uses `popUpTo(ROUTE_CAROUSEL_STEP) { inclusive = true }`, which keeps the back-stack from growing 10 deep during the walk-through. When the user hits Finish (step 10 or Close), we pop the whole carousel graph.

- [ ] **Step 3: Ensure the carousel route is excluded from the bottom bar**

Confirm `BOTTOM_NAV_ROUTES` is unchanged (it should only hold the 5 tab routes). The carousel's route starts with `"carousel/"`, which is not in the set, so the bottom bar hides automatically thanks to the NavHost's existing `currentRoute in BOTTOM_NAV_ROUTES` predicate.

If the predicate uses exact-match (not `startsWith`), you may need to add an explicit clause. Inspect `currentRoute` computation; the current logic reads:

```kotlin
val currentRoute = navBackStackEntry?.destination?.route
val showBottomBar = currentRoute in BOTTOM_NAV_ROUTES
```

— this correctly excludes `carousel/{step}` because the route constant includes the `{step}` placeholder, not a literal value. So no change needed.

- [ ] **Step 4: Wire the Dashboard banner**

Locate the `"today"` composable in `StretchDailyNavHost.kt`. R3 should have added an `onBenchmarkBannerClick` lambda parameter to `DashboardScreen`. Point it at the carousel:

```kotlin
composable(ROUTE_TODAY) {
    DashboardScreen(
        onStartSessionClick = { navController.navigate(ROUTE_SESSION_GRAPH) },
        onBenchmarkBannerClick = { navController.navigate("$ROUTE_CAROUSEL_GRAPH/0") },
        contentPadding = innerPadding,
    )
}
```

If the lambda name differs, adjust; if R3 accidentally omitted it, add it now to `DashboardScreen` (signature + one `onClick` on the banner container).

- [ ] **Step 5: Finalize the Log tab CTA**

Confirm the `ROUTE_LOG` composable you wired in Task 6 references `ROUTE_CAROUSEL_GRAPH` correctly:

```kotlin
composable(ROUTE_LOG) {
    BenchmarkLogScreen(
        onStartCarousel = { navController.navigate("$ROUTE_CAROUSEL_GRAPH/0") },
        contentPadding = innerPadding,
    )
}
```

- [ ] **Step 6: Verify compile**

Build → Make Project. Expected: clean.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/ui/navigation/StretchDailyNavHost.kt
git commit -m "feat(navigation): register carousel nested graph + wire Dashboard banner and Log CTA

Carousel is a nested graph rooted at ROUTE_CAROUSEL_GRAPH with a single
composable 'carousel/{step}'. Advance navigates to 'carousel/{step+1}' with
popUpTo=current (inclusive) so the back-stack never grows during the 10-step
walk-through. Finish pops the whole graph back to the caller (Dashboard or Log)."
git push
```

---

## Task 11: Manual smoke test + pixel QA

- [ ] **Step 1: End-to-end smoke test on device**

Start with a blank DB (Settings → Delete all data, then re-launch) so the overdue + empty-state variants surface.

Log tab:

1. Open the app → tap **Log** tab. Verify: hero header renders, "Start benchmark day" CTA visible, every benchmark is grouped under its category with the tint square + mono-caps label, every row shows "Not logged yet", "Overdue" pill visible.
2. Tap a chevron on any row → band list + (empty) history area expand; tap again to collapse.
3. Tap **Log** on a numeric benchmark (e.g. "Sit and Reach") → sheet opens with the description + centered numeric field + unit suffix. Type a value, tap **Save entry** → sheet dismisses, row updates: value + band pill + sparkline (1-point).
4. Log a second entry a day later (for sparkline validation): temporarily tweak `Clock.now()` in a debug dropdown or just verify the UI re-renders with the new value.
5. Tap **Log** on the categorical benchmark (ATG Split Squat) → sheet opens with 5 band buttons. Tap one → selection turns accent. Tap **Save entry** → row updates with the chosen tier name.
6. Re-open any just-logged row's chevron → history table shows one row with month + value + band pill.
7. Tap **Start benchmark day** → carousel opens.

Carousel:

8. Header shows `CLOSE · [####──────] · "BENCHMARK 1 OF 10 · {category}"`.
9. Save a numeric entry → screen advances to step 2; header progresses; back-stack still pops to Log tab.
10. Tap **Close (×)** → pops entire carousel back to wherever you entered from.
11. Re-enter via Dashboard banner if the `benchmarkBannerEnabled` preference is on → same flow.
12. On step 10, save → finishes and pops back to caller (no lingering player state).

- [ ] **Step 2: Pixel QA**

Open `docs/design_handoff_stretch_daily_v3/reference/index.html` in a browser next to the app. Compare:

| Screen | Key check |
|---|---|
| Log hero | 30 sp "Log & review", mono-caps eyebrow, body prose tone. |
| "Start benchmark day" CTA | Accent-soft bg, 34×34 accent sparkle tile, chevron-right end cap. |
| Category group headers | 10×10 dp category-tinted square + mono-caps label, spacing. |
| Row collapsed | name + OVERDUE pill + value/band/sparkline line + Log pill + chevron. |
| Row expanded | bands list (current tier highlighted), history table with dividers. |
| Log sheet | Title + description + big centered numeric or 5 band buttons + Cancel/Save footer (1:2 weight). |
| Carousel header | 32 dp bg2 close CircleButton, 10-segment SegmentProgress, mono-caps step counter. |
| Carousel body | 30 sp heading, description, same LogForm. |

- [ ] **Step 3: Run all tests in Android Studio**

Right-click `app/src/test/java` → Run 'Tests in 'java''. Expected pass count (cumulative through R5):

- All preserved (engine, repo, resolver, data-port).
- R2 helper tests (WeekStrip, SegmentProgress helpers).
- R3 tests (TodaySessionHolder, DashboardViewModel, benchmark helpers, session flows).
- R4 tests (SessionOverviewViewModel × 4, SessionPlayerViewModel × 10).
- R5 new: LogSparklineTest (5), BenchmarkLogViewModelTest (11), BenchmarkCarouselViewModelTest (9).

- [ ] **Step 4: Push any smoke-test fixes**

```bash
git status
# ... review ...
git add <file>
git commit -m "fix(<scope>): <short>"
git push
```

---

## Task 12: Open the PR

- [ ] **Step 1: Create PR against `development`**

```bash
gh pr create --base development \
  --title "R5 — Benchmark log + Benchmark carousel" \
  --body "$(cat <<'EOF'
## Summary

- **`BenchmarkLogScreen`** — grouped by category with expand-to-reveal rows showing bands list + history. Each row pulls `observeLogsFor(id)` for its sparkline; the bottom sheet hosts the shared `LogForm`.
- **`BenchmarkCarouselScreen`** — full-screen nested-graph overlay walking the user through all 10 benchmarks. `SegmentProgress` header, shared `LogForm` body, one-shot `CarouselEvent` flow driving navigation.
- **`LogForm`** — shared composable between the Log sheet and the Carousel. Numeric variant (big centered input + unit suffix) and categorical variant (5 band buttons). Cancel / Save entry footer.
- **Carousel entry points** — Dashboard banner (R3) + Log tab "Start benchmark day" CTA both call `navigate("carousel/0")`.
- **NavHost** — carousel nested graph registered with `popUpTo(currentStep) inclusive` advance behavior so the back-stack doesn't grow during the 10-step walk-through.

## Test plan

- [x] `LogSparklineTest` (5) — window filter + extremes + sort.
- [x] `BenchmarkLogViewModelTest` (11) — grouping, sparkline, overdue flag, row/sheet state, numeric+categorical submit, parse failure, delete.
- [x] `BenchmarkCarouselViewModelTest` (9) — init load, setStep clamp, save-and-advance, last-step Finished, failure path, close, dismissError.
- [x] All preserved + R2 + R3 + R4 tests green.
- [ ] **Ramon — manual:**
  - Log tab: hero + CTA card + grouped rows; expand/collapse chevron; numeric sheet; categorical sheet; overdue pill surfaces for unlogged benchmarks.
  - Carousel: 10 steps advance end-to-end; close (×) pops; Dashboard banner + Log CTA both open the carousel.
  - Pixel QA against reference/index.html.

## Design spec

[2026-04-23-sage-redesign-design.md](../docs/superpowers/specs/2026-04-23-sage-redesign-design.md) §7.1, §7.3, §8.5, §8.7, §11.5.

## Implementation plan

[2026-04-23-sage-redesign-R5-log-and-carousel.md](../docs/superpowers/plans/2026-04-23-sage-redesign-R5-log-and-carousel.md)

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

- [ ] **Step 2: Share URL** — the PR URL printed by `gh` is the handoff back to Ramon for review + merge.

---

## Post-R5 state

After merge:

- **Log tab** is the first place in the app where the user's benchmark data is genuinely useful — latest values, bands, history, and a month's-worth of sparkline.
- **Carousel** is the one-tap "it's the 1st of the month, re-log everything" flow users will run monthly. Entry from the banner means zero friction; from the Log tab it's a deliberate choice.
- `BenchmarkLogViewModel` composes 3+ reactive flows (catalog + latest + per-benchmark history) in a stable, testable shape — no Room in the VM layer.
- `BenchmarkRepository` remains unchanged — all writes flow through existing `logNumeric` / `logCategorical` / `deleteLog`.

Next: R6 plan ([`2026-04-23-sage-redesign-R6-analytics-settings-polish.md`](./2026-04-23-sage-redesign-R6-analytics-settings-polish.md)) — Analytics tab (category filter + per-benchmark card + BigChart with 6-month delta) + Settings redesign (Preferences / Your data / Library groups + banner toggle) + app icon / splash update + cross-screen accessibility audit + final pixel QA.
