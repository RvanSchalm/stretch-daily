# Sage redesign — Phase R3: Dashboard + `TodaySessionHolder` — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fill in the Dashboard tab (`"today"`) with real data, introduce the single most load-bearing new class in the redesign (`TodaySessionHolder`), and add the new reactive flows on the two preserved repositories so the dashboard can observe live state. After R3 the user sees the real Sage dashboard — date + streak + optional benchmark banner + today's session card + week strip + KPI grid — backed by real Room data. The other 4 tabs stay as R1 placeholders.

**Architecture:**

- `TodaySessionHolder` is a `@Singleton` in `core/session/` — the cached-per-calendar-day plan holder, injected by Hilt and consumed by every ViewModel that needs to know "what is today's session". It regenerates only on calendar-day rollover; completion does not reset it (per spec §6, Q5 tradeoff).
- New flows on `SessionRepository` (`streakFlow`, `weeklyFlow`, `totalsFlow`) and `BenchmarkRepository` (`overdueFlow`, `nextDueFlow`) wrap existing DAO queries reactively so the Dashboard recomposes when sessions are recorded or benchmarks logged.
- `DashboardViewModel` composes all five flows plus the `SettingsDataStore.benchmarkBannerEnabled` preference plus `TodaySessionHolder.state` plus `CategoryWeightCalculator`'s per-category weights into a single `DashboardUiState` via `combine { ... }.stateIn(Eagerly)`.
- `DashboardScreen` renders that state using the R2 primitives (`WeekStrip`, `KpiCard`, `Pill`, `CircleButton`, `MonoCaps`, `AppIcon`) and the R1 tokens (`Theme.colors`, `Theme.typo`, `Theme.dims`, `Category.tint()`). The CTA navigates to `"session/overview"` — still a placeholder in R3; R4 fills it in.

**Tech Stack:** Kotlin 2.0, Compose, Hilt, Room, DataStore, kotlinx.coroutines Flow + StateFlow, JUnit 4 + mockk for tests.

**Spec:** [`docs/superpowers/specs/2026-04-23-sage-redesign-design.md`](../specs/2026-04-23-sage-redesign-design.md) sections 3.2–3.3, 6, 7, 8.1, 10.2, 11.3.
**Handoff reference:** [`docs/design_handoff_stretch_daily_v3/README.md`](../../design_handoff_stretch_daily_v3/README.md) §1 Dashboard, plus [`reference/screens.jsx`](../../design_handoff_stretch_daily_v3/reference/screens.jsx) `DashboardScreen` for pixel-level layout.

**Build verification:** Gradle CLI blocked on this machine — verify every commit via Android Studio → Build → Make Project. See [`CLAUDE.md §2 "Known issues"`](../../../CLAUDE.md).

**Prerequisite:** R2 is merged to `development`. All 12 primitives and tokens are available.

---

## File structure for R3

**New under `app/src/main/java/com/stretchdaily/app/core/session/`:**
- `TodaySessionHolder.kt` — `@Singleton`, wraps `LongevityEngine.generateSession()` with per-day caching + swap.

**New under `app/src/main/java/com/stretchdaily/app/core/di/`:**
- `SessionModule.kt` — Hilt module providing the `TodaySessionHolder`.

**New under `app/src/main/java/com/stretchdaily/app/ui/screen/dashboard/`:**
- `DashboardUiState.kt` — sealed state + row/KPI sub-types.
- `DashboardViewModel.kt` — `combine(...)` of 7 input flows → `DashboardUiState`.
- `DashboardScreen.kt` — full-screen Compose layout.

**New tests under `app/src/test/java/com/stretchdaily/app/`:**
- `core/session/TodaySessionHolderTest.kt` — 6 cases per spec §6.6.
- `data/BenchmarkRepositoryDueSelectionTest.kt` — `computeOverdueBenchmarks` + `computeNextDue` helpers.
- `data/SessionRepositoryFlowTest.kt` — reactive flow emissions.
- `ui/screen/dashboard/DashboardViewModelTest.kt` — mockk-driven state composition.

**Modified:**
- `app/src/main/java/com/stretchdaily/app/core/datastore/SettingsDataStore.kt` — add `benchmarkBannerEnabled` preference.
- `app/src/main/java/com/stretchdaily/app/data/SessionRepository.kt` — add `streakFlow`, `weeklyFlow`, `totalsFlow` + nested `Totals` type.
- `app/src/main/java/com/stretchdaily/app/data/BenchmarkRepository.kt` — add `overdueFlow`, `nextDueFlow` + nested `BenchmarkWithDueDate` type + `computeOverdueBenchmarks` + `computeNextDue` companion helpers.
- `app/src/main/java/com/stretchdaily/app/ui/navigation/StretchDailyNavHost.kt` — replace the `"today"` placeholder with the real `DashboardScreen` composable.

---

## Task 1: Create the `redesign/r3-dashboard` branch

- [ ] **Step 1: Verify clean state on `development`**

```bash
git checkout development && git pull
git status
```

Expected: clean tree on `development` with R2 merged.

- [ ] **Step 2: Create branch + push**

```bash
git checkout -b redesign/r3-dashboard
git push -u origin redesign/r3-dashboard
```

---

## Task 2: Add `benchmarkBannerEnabled` preference to `SettingsDataStore`

The Dashboard reads this to decide whether the benchmark-overdue banner shows. Per spec §8.1 and the handoff "user override noted" call-out, default is `false` (banner off).

- [ ] **Step 1: Edit `SettingsDataStore.kt`**

Add a second preference parallel to the existing audio cues one.

```kotlin
package com.stretchdaily.app.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.preferencesStore by preferencesDataStore(name = "stretch_daily_settings")

/**
 * Thin wrapper over the [Preferences] DataStore for app-wide settings. Each
 * setting gets a typed key + a `Flow` for observation and a `suspend fun` for
 * mutation.
 */
@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    val audioCuesEnabled: Flow<Boolean> = context.preferencesStore.data.map { prefs ->
        prefs[KEY_AUDIO_CUES] ?: DEFAULT_AUDIO_CUES
    }

    suspend fun setAudioCuesEnabled(enabled: Boolean) {
        context.preferencesStore.edit { prefs ->
            prefs[KEY_AUDIO_CUES] = enabled
        }
    }

    /**
     * Controls the "next benchmark day" banner on the Dashboard. Default is
     * `false` — the user opted out during brainstorming (see design spec
     * §8.1). Surfaced in Settings (R6) as a toggle.
     */
    val benchmarkBannerEnabled: Flow<Boolean> = context.preferencesStore.data.map { prefs ->
        prefs[KEY_BENCHMARK_BANNER] ?: DEFAULT_BENCHMARK_BANNER
    }

    suspend fun setBenchmarkBannerEnabled(enabled: Boolean) {
        context.preferencesStore.edit { prefs ->
            prefs[KEY_BENCHMARK_BANNER] = enabled
        }
    }

    companion object {
        private val KEY_AUDIO_CUES = booleanPreferencesKey("audio_cues_enabled")
        private const val DEFAULT_AUDIO_CUES = true

        private val KEY_BENCHMARK_BANNER = booleanPreferencesKey("benchmark_banner_enabled")
        private const val DEFAULT_BENCHMARK_BANNER = false
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/core/datastore/SettingsDataStore.kt
git commit -m "feat(datastore): add benchmarkBannerEnabled preference (default false)"
git push
```

---

## Task 3: Add reactive flows to `SessionRepository`

Per spec §3.3, the Dashboard needs `streakFlow`, `weeklyFlow`, and `totalsFlow`. These wrap the existing `SessionDao.observeAllRecords()` reactively — no new DAO queries required; all the math happens on the already-emitted list of records.

- [ ] **Step 1: Extend the DAO with a completion-timestamp observation**

The existing `getAllCompletionTimestamps()` is `suspend`. The reactive flows need a `Flow<List<Long>>`. Rather than add a new DAO query, derive from `observeAllRecords()` using `map { it.map { it.completedAt } }`.

No DAO change needed. Proceed directly to the repository.

- [ ] **Step 2: Edit `SessionRepository.kt`**

Add three `Flow` properties and a nested `Totals` type. Keep the existing suspend methods intact — everything that calls them (e.g. `SessionViewModel.recordSession`) still works.

Add these imports:

```kotlin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
```

Add these properties and type inside the class (alongside `observeAllSessions()`):

```kotlin
/**
 * Current streak, reactively. Wraps [SessionDao.observeAllRecords] and
 * runs [computeStreak] on every emission. Dashboard uses this so the
 * streak updates the moment a session is recorded.
 */
fun streakFlow(zoneId: ZoneId = ZoneId.systemDefault()): Flow<Int> =
    sessionDao.observeAllRecords().map { records ->
        computeStreak(records.map { it.completedAt }, clock.now(), zoneId)
    }

/**
 * This week's completed session dates (Monday-start, inclusive of today).
 * Emits the set of `LocalDate`s the WeekStrip paints as "completed".
 */
fun weeklyFlow(zoneId: ZoneId = ZoneId.systemDefault()): Flow<Set<LocalDate>> =
    sessionDao.observeAllRecords().map { records ->
        val today = LocalDate.ofInstant(Instant.ofEpochMilli(clock.now()), zoneId)
        val mondayStart = today.minusDays((today.dayOfWeek.value - 1).toLong())
        records.asSequence()
            .map { LocalDate.ofInstant(Instant.ofEpochMilli(it.completedAt), zoneId) }
            .filter { !it.isBefore(mondayStart) && !it.isAfter(today) }
            .toSet()
    }

/**
 * Total session count + total minutes across all recorded sessions.
 * Used by two KPI cards on the Dashboard ("Total time", "Sessions")
 * and by Settings → Library (R6).
 */
val totalsFlow: Flow<Totals> = sessionDao.observeAllRecords().map { records ->
    Totals(
        sessions = records.size,
        totalMinutes = records.sumOf { it.totalDurationSeconds } / 60,
    )
}

data class Totals(val sessions: Int, val totalMinutes: Int)
```

- [ ] **Step 3: Add a small flow-emission test**

Create: `app/src/test/java/com/stretchdaily/app/data/SessionRepositoryFlowTest.kt`

```kotlin
package com.stretchdaily.app.data

import com.stretchdaily.app.core.database.dao.ExerciseDao
import com.stretchdaily.app.core.database.dao.SessionDao
import com.stretchdaily.app.core.model.SessionRecord
import com.stretchdaily.app.core.util.Clock
import io.mockk.coEvery
import io.mockk.mockk
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionRepositoryFlowTest {

    private val zone: ZoneId = ZoneId.of("UTC")

    private fun midday(year: Int, month: Int, day: Int): Long =
        ZonedDateTime.of(LocalDate.of(year, month, day).atTime(12, 0), zone)
            .toInstant().toEpochMilli()

    private fun record(completedAt: Long, durationSeconds: Int = 600): SessionRecord =
        SessionRecord(
            startedAt = completedAt - durationSeconds * 1000L,
            completedAt = completedAt,
            totalDurationSeconds = durationSeconds,
            exerciseCount = 7,
        )

    private fun repo(records: List<SessionRecord>, now: Long): SessionRepository {
        val sessionDao = mockk<SessionDao>(relaxed = true)
        coEvery { sessionDao.observeAllRecords() } returns flowOf(records)
        val exerciseDao = mockk<ExerciseDao>(relaxed = true)
        val clock = Clock { now }
        return SessionRepository(exerciseDao, sessionDao, clock)
    }

    @Test
    fun `streakFlow emits derived streak from observed records`() = runTest {
        val r = repo(
            listOf(record(midday(2026, 4, 22)), record(midday(2026, 4, 23))),
            now = midday(2026, 4, 23),
        )
        val streak = r.streakFlow(zoneId = zone).first()
        assertEquals(2, streak)
    }

    @Test
    fun `weeklyFlow scopes to Monday-through-today`() = runTest {
        // Thursday 2026-04-23. Week runs Mon 4/20 through Thu 4/23. A Sunday
        // session on 4/19 must be excluded.
        val r = repo(
            listOf(
                record(midday(2026, 4, 19)),
                record(midday(2026, 4, 21)),
                record(midday(2026, 4, 23)),
            ),
            now = midday(2026, 4, 23),
        )
        val week = r.weeklyFlow(zoneId = zone).first()
        assertEquals(
            setOf(LocalDate.of(2026, 4, 21), LocalDate.of(2026, 4, 23)),
            week,
        )
    }

    @Test
    fun `totalsFlow sums durations and counts sessions`() = runTest {
        val r = repo(
            listOf(record(midday(2026, 4, 22), 600), record(midday(2026, 4, 23), 900)),
            now = midday(2026, 4, 23),
        )
        val totals = r.totalsFlow.first()
        assertEquals(2, totals.sessions)
        // 600 + 900 = 1500s = 25 min.
        assertEquals(25, totals.totalMinutes)
    }
}
```

- [ ] **Step 4: Verify in Android Studio** — Build → Make Project, and run `SessionRepositoryFlowTest` (3 tests) — all green.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/data/SessionRepository.kt \
        app/src/test/java/com/stretchdaily/app/data/SessionRepositoryFlowTest.kt
git commit -m "feat(data): add streakFlow/weeklyFlow/totalsFlow on SessionRepository

Reactive wrappers over observeAllRecords for the Dashboard. Pure-math
helpers (computeStreak) stay unchanged; flows just re-run them on each
DAO emission. Totals is a nested data class carrying session count +
total minutes.

Tests cover streak derivation, Monday-start week scoping, and totals
aggregation via mockk-stubbed DAO."
git push
```

---

## Task 4: TDD `computeOverdueBenchmarks` + `computeNextDue` helpers on `BenchmarkRepository`

Per spec §3.3, the Dashboard needs two new observable views on the benchmark catalog:

- `overdueFlow: Flow<List<Benchmark>>` — benchmarks whose last log is before the current month-start.
- `nextDueFlow: Flow<BenchmarkWithDueDate?>` — the benchmark with the nearest upcoming due date, along with `daysUntilDue` (0 means overdue).

Both wrap the catalog + latest-logs DAO flows. Extract pure-Kotlin logic into companion functions (same pattern as `computeBenchmarksDue` and `computeStreak`) so the math is unit-testable without Room.

**Semantics (reconciled with existing `computeBenchmarksDue`):**

- A benchmark's "next due date" is `lastLoggedAt + 30 days` (ISO calendar days, same zone) if that's in the future; otherwise today (already overdue). Never-logged → overdue (due today).
- Overdue cutoff = "last log before the current month-start." If `lastLoggedAt` is before `today.withDayOfMonth(1)`, it's overdue. Never-logged is also overdue.
- Tie-break for `nextDue`: pick the benchmark with the smallest `daysUntilDue`; if multiple tie, pick by `benchmark.id` lex order (deterministic).

- [ ] **Step 1: Write the test first (red)**

Create: `app/src/test/java/com/stretchdaily/app/data/BenchmarkRepositoryDueSelectionTest.kt`

```kotlin
package com.stretchdaily.app.data

import com.stretchdaily.app.core.model.Benchmark
import com.stretchdaily.app.core.model.BenchmarkInputType
import com.stretchdaily.app.core.model.BenchmarkLog
import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.core.model.FlexibilityTier
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pure-JVM tests for the two companion helpers that feed
 * [BenchmarkRepository.overdueFlow] and [BenchmarkRepository.nextDueFlow].
 * The helpers are extracted from the repository so they can be exercised
 * without Room — same pattern as [SessionRepository.computeStreak] and
 * [BenchmarkRepository.computeBenchmarksDue].
 */
class BenchmarkRepositoryDueSelectionTest {

    private val zone: ZoneId = ZoneId.of("UTC")

    private fun midday(year: Int, month: Int, day: Int): Long =
        ZonedDateTime.of(LocalDate.of(year, month, day).atTime(12, 0), zone)
            .toInstant().toEpochMilli()

    private fun bm(id: String, category: Category = Category.HIPS): Benchmark =
        Benchmark(
            id = id,
            name = id,
            category = category,
            description = "",
            unit = "",
            inputType = BenchmarkInputType.NUMERIC,
            tierRanges = emptyMap(),
        )

    private fun log(benchmarkId: String, loggedAt: Long): BenchmarkLog =
        BenchmarkLog(
            id = 0,
            benchmarkId = benchmarkId,
            rawValue = "0",
            resolvedTier = FlexibilityTier.AVERAGE,
            loggedAt = loggedAt,
        )

    @Test
    fun `computeOverdueBenchmarks returns all when no logs exist`() {
        val benchmarks = listOf(bm("A"), bm("B"), bm("C"))
        val overdue = BenchmarkRepository.computeOverdueBenchmarks(
            benchmarks = benchmarks,
            latestLogs = emptyList(),
            now = midday(2026, 4, 23),
            zoneId = zone,
        )
        assertEquals(benchmarks, overdue)
    }

    @Test
    fun `computeOverdueBenchmarks excludes benchmarks logged within current month`() {
        val benchmarks = listOf(bm("A"), bm("B"))
        val latestLogs = listOf(log("A", midday(2026, 4, 10)))
        val overdue = BenchmarkRepository.computeOverdueBenchmarks(
            benchmarks = benchmarks,
            latestLogs = latestLogs,
            now = midday(2026, 4, 23),
            zoneId = zone,
        )
        assertEquals(listOf(bm("B")), overdue)
    }

    @Test
    fun `computeOverdueBenchmarks flags benchmarks whose last log is before the 1st of this month`() {
        val benchmarks = listOf(bm("A"))
        val latestLogs = listOf(log("A", midday(2026, 3, 28)))
        val overdue = BenchmarkRepository.computeOverdueBenchmarks(
            benchmarks = benchmarks,
            latestLogs = latestLogs,
            now = midday(2026, 4, 23),
            zoneId = zone,
        )
        assertEquals(listOf(bm("A")), overdue)
    }

    @Test
    fun `computeNextDue returns null when catalog is empty`() {
        val next = BenchmarkRepository.computeNextDue(
            benchmarks = emptyList(),
            latestLogs = emptyList(),
            now = midday(2026, 4, 23),
            zoneId = zone,
        )
        assertNull(next)
    }

    @Test
    fun `computeNextDue returns 0 days for a never-logged benchmark`() {
        val benchmarks = listOf(bm("A"), bm("B"))
        val next = BenchmarkRepository.computeNextDue(
            benchmarks = benchmarks,
            latestLogs = emptyList(),
            now = midday(2026, 4, 23),
            zoneId = zone,
        )
        // Both are never-logged — tie-break goes to the first id.
        assertEquals("A", next?.benchmark?.id)
        assertEquals(0, next?.daysUntilDue)
    }

    @Test
    fun `computeNextDue returns forward-looking days for recently-logged benchmarks`() {
        // Logged today (4/23); next due = +30 = 5/23; daysUntil = 30.
        val benchmarks = listOf(bm("A"))
        val latestLogs = listOf(log("A", midday(2026, 4, 23)))
        val next = BenchmarkRepository.computeNextDue(
            benchmarks = benchmarks,
            latestLogs = latestLogs,
            now = midday(2026, 4, 23),
            zoneId = zone,
        )
        assertEquals("A", next?.benchmark?.id)
        assertEquals(30, next?.daysUntilDue)
    }

    @Test
    fun `computeNextDue picks the benchmark with the nearest due date`() {
        // A logged 4/20 → due 5/20 → 27 days out
        // B logged 4/10 → due 5/10 → 17 days out → picked
        val benchmarks = listOf(bm("A"), bm("B"))
        val latestLogs = listOf(
            log("A", midday(2026, 4, 20)),
            log("B", midday(2026, 4, 10)),
        )
        val next = BenchmarkRepository.computeNextDue(
            benchmarks = benchmarks,
            latestLogs = latestLogs,
            now = midday(2026, 4, 23),
            zoneId = zone,
        )
        assertEquals("B", next?.benchmark?.id)
        assertEquals(17, next?.daysUntilDue)
    }

    @Test
    fun `computeNextDue clamps overdue benchmarks to 0 days`() {
        // A logged on 2026-01-01 → due 1/31 → negative; clamp to 0 overdue.
        val benchmarks = listOf(bm("A"), bm("B"))
        val latestLogs = listOf(
            log("A", midday(2026, 1, 1)),
            log("B", midday(2026, 4, 23)),
        )
        val next = BenchmarkRepository.computeNextDue(
            benchmarks = benchmarks,
            latestLogs = latestLogs,
            now = midday(2026, 4, 23),
            zoneId = zone,
        )
        assertEquals("A", next?.benchmark?.id)
        assertEquals(0, next?.daysUntilDue)
    }
}
```

Expected to fail compilation: `unresolved reference: computeOverdueBenchmarks`, `computeNextDue`, `BenchmarkWithDueDate`.

- [ ] **Step 2: Add the companion helpers and nested type (green)**

Edit `BenchmarkRepository.kt`. Add imports:

```kotlin
import java.time.temporal.ChronoUnit
```

Add inside `class BenchmarkRepository`, **below** the existing properties and just above `companion object`:

```kotlin
/**
 * Pair of [Benchmark] + how many days until its next benchmark-day reminder.
 * `daysUntilDue == 0` means "due today or overdue"; values > 0 are forward-
 * looking. Fed to the Dashboard "Next benchmark" KPI card.
 */
data class BenchmarkWithDueDate(
    val benchmark: Benchmark,
    val daysUntilDue: Int,
)
```

Add inside the existing `companion object`, below `computeBenchmarksDue`:

```kotlin
/** Per-benchmark cadence for the reminder clock. Keep in sync with [STALE_AFTER_DAYS]. */
internal const val REMIND_EVERY_DAYS: Long = 30

/**
 * Returns the subset of [benchmarks] whose latest log is before the start of
 * the current month (or which have never been logged). "Month-start" is
 * derived from [now] in [zoneId].
 */
internal fun computeOverdueBenchmarks(
    benchmarks: List<Benchmark>,
    latestLogs: List<BenchmarkLog>,
    now: Long,
    zoneId: ZoneId = ZoneId.systemDefault(),
): List<Benchmark> {
    val today = LocalDate.ofInstant(Instant.ofEpochMilli(now), zoneId)
    val monthStart = today.withDayOfMonth(1)
    val lastLoggedByBenchmark = latestLogs.associateBy { it.benchmarkId }
    return benchmarks.filter { benchmark ->
        val log = lastLoggedByBenchmark[benchmark.id]
        log == null || run {
            val loggedDate = LocalDate.ofInstant(
                Instant.ofEpochMilli(log.loggedAt),
                zoneId,
            )
            loggedDate.isBefore(monthStart)
        }
    }
}

/**
 * Returns the benchmark with the nearest upcoming reminder, or `null` if the
 * catalog is empty. A never-logged benchmark counts as `daysUntilDue = 0`
 * (overdue today). Ties break on `benchmark.id` lex order.
 */
internal fun computeNextDue(
    benchmarks: List<Benchmark>,
    latestLogs: List<BenchmarkLog>,
    now: Long,
    zoneId: ZoneId = ZoneId.systemDefault(),
): BenchmarkWithDueDate? {
    if (benchmarks.isEmpty()) return null
    val today = LocalDate.ofInstant(Instant.ofEpochMilli(now), zoneId)
    val lastLoggedByBenchmark = latestLogs.associateBy { it.benchmarkId }
    return benchmarks
        .map { benchmark ->
            val log = lastLoggedByBenchmark[benchmark.id]
            val due = if (log == null) {
                today
            } else {
                LocalDate.ofInstant(Instant.ofEpochMilli(log.loggedAt), zoneId)
                    .plusDays(REMIND_EVERY_DAYS)
            }
            val daysUntil = ChronoUnit.DAYS.between(today, due).toInt().coerceAtLeast(0)
            BenchmarkWithDueDate(benchmark, daysUntil)
        }
        .sortedWith(compareBy({ it.daysUntilDue }, { it.benchmark.id }))
        .first()
}
```

- [ ] **Step 3: Verify test passes in Android Studio**

Build → Make Project. Run `BenchmarkRepositoryDueSelectionTest` — 8 tests green.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/data/BenchmarkRepository.kt \
        app/src/test/java/com/stretchdaily/app/data/BenchmarkRepositoryDueSelectionTest.kt
git commit -m "feat(data): add computeOverdueBenchmarks + computeNextDue helpers

Pure-Kotlin companion functions following the existing computeStreak /
computeBenchmarksDue pattern. Overdue = logged-before-this-month-start
(or never logged). Next-due = benchmark with nearest (lastLoggedAt + 30d),
clamped at 0 for overdue, tie-broken lexicographically by benchmark id.

BenchmarkWithDueDate nested type carries the (benchmark, daysUntilDue)
tuple the Dashboard KPI card consumes."
git push
```

---

## Task 5: Wire the new flows onto `BenchmarkRepository`

Now the pure helpers exist, wrap them reactively. Both flows `combine()` the reactive catalog + latest-log flows that already exist on the DAO.

- [ ] **Step 1: Edit `BenchmarkRepository.kt`**

Add imports:

```kotlin
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
```

Add inside the class, next to `observeAllBenchmarks()` / `observeLatestLogs()`:

```kotlin
/**
 * Reactive view of benchmarks that need re-logging — anything whose latest
 * log is before the current month-start, or that's never been logged.
 * Dashboard banner + Log screen both subscribe.
 */
fun overdueFlow(zoneId: ZoneId = ZoneId.systemDefault()): Flow<List<Benchmark>> =
    combine(observeAllBenchmarks(), observeLatestLogs()) { benchmarks, logs ->
        computeOverdueBenchmarks(benchmarks, logs, clock.now(), zoneId)
    }

/**
 * Reactive view of the single next-up benchmark + how many days until it's
 * due. Emits `null` only if the catalog is empty (shouldn't happen in
 * production because the seeder inserts 10).
 */
fun nextDueFlow(zoneId: ZoneId = ZoneId.systemDefault()): Flow<BenchmarkWithDueDate?> =
    combine(observeAllBenchmarks(), observeLatestLogs()) { benchmarks, logs ->
        computeNextDue(benchmarks, logs, clock.now(), zoneId)
    }
```

- [ ] **Step 2: Verify** — Build → Make Project. No new test required here; the flow wiring is trivial composition of already-tested helpers.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/data/BenchmarkRepository.kt
git commit -m "feat(data): add overdueFlow + nextDueFlow to BenchmarkRepository

Reactive combine() of observeAllBenchmarks + observeLatestLogs through
the new pure helpers. No new tests — composition of already-TDD'd parts."
git push
```

---

## Task 6: TDD `TodaySessionHolder`

The hinge of the redesign. Six tests per spec §6.6. Write the test first, then the implementation.

- [ ] **Step 1: Write the test (red)**

Create: `app/src/test/java/com/stretchdaily/app/core/session/TodaySessionHolderTest.kt`

```kotlin
package com.stretchdaily.app.core.session

import com.stretchdaily.app.core.database.dao.ExerciseDao
import com.stretchdaily.app.core.engine.LongevityEngine
import com.stretchdaily.app.core.engine.model.PlannedExercise
import com.stretchdaily.app.core.engine.model.SessionPlan
import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.core.model.Exercise
import com.stretchdaily.app.core.util.Clock
import io.mockk.coEvery
import io.mockk.mockk
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TodaySessionHolderTest {

    private val zone: ZoneId = ZoneId.of("UTC")

    private fun epoch(year: Int, month: Int, day: Int): Long =
        ZonedDateTime.of(LocalDate.of(year, month, day).atTime(12, 0), zone)
            .toInstant().toEpochMilli()

    private fun exercise(id: String, category: Category = Category.HIPS): Exercise =
        Exercise(
            id = id,
            name = "Exercise $id",
            category = category,
            cues = emptyList(),
            isUnilateral = false,
            isTimed = true,
            targetReps = null,
            secondsPerRep = null,
            totalTime = 60,
            lastPerformed = null,
        )

    private fun plan(vararg ids: String): SessionPlan =
        SessionPlan(
            items = ids.map { id ->
                PlannedExercise(exercise(id), effectiveSeconds = 60, isForced = false)
            },
            totalSeconds = ids.size * 60,
            categoryWeights = emptyMap(),
        )

    /**
     * Helper that builds a holder with deterministic collaborators. Each test
     * owns a `MutableLong` for the clock so it can advance days.
     */
    private fun holder(
        engine: LongevityEngine,
        exerciseDao: ExerciseDao = mockk(relaxed = true),
        nowProvider: () -> Long,
    ): TodaySessionHolder {
        val scope: CoroutineScope = TestScope(UnconfinedTestDispatcher())
        val clock = Clock { nowProvider() }
        return TodaySessionHolder(
            engine = engine,
            exerciseDao = exerciseDao,
            clock = clock,
            scope = scope,
        )
    }

    @Test
    fun `first ensureFresh generates via engine and emits a TodaySession`() = runTest {
        val engine = mockk<LongevityEngine>()
        val expected = plan("E1", "E2")
        coEvery { engine.generateSession() } returns expected

        val h = holder(engine) { epoch(2026, 4, 23) }
        assertNull(h.state.value)
        h.ensureFresh()

        val today = h.state.value
        assertNotNull(today)
        assertEquals(LocalDate.of(2026, 4, 23), today!!.planDate)
        assertSame(expected, today.plan)
    }

    @Test
    fun `second ensureFresh same day returns cached plan without re-invoking engine`() = runTest {
        val engine = mockk<LongevityEngine>()
        var callCount = 0
        coEvery { engine.generateSession() } answers {
            callCount++
            plan("E$callCount")
        }

        val h = holder(engine) { epoch(2026, 4, 23) }
        h.ensureFresh()
        h.ensureFresh()

        assertEquals(1, callCount)
        assertEquals("E1", h.state.value?.plan?.items?.first()?.exercise?.id)
    }

    @Test
    fun `ensureFresh regenerates on calendar-day rollover`() = runTest {
        val engine = mockk<LongevityEngine>()
        var callCount = 0
        coEvery { engine.generateSession() } answers {
            callCount++
            plan("DAY$callCount")
        }

        var nowMillis = epoch(2026, 4, 23)
        val h = holder(engine) { nowMillis }
        h.ensureFresh()
        assertEquals(LocalDate.of(2026, 4, 23), h.state.value?.planDate)

        // Advance the clock to the next calendar day.
        nowMillis = epoch(2026, 4, 24)
        h.ensureFresh()
        assertEquals(LocalDate.of(2026, 4, 24), h.state.value?.planDate)
        assertEquals("DAY2", h.state.value?.plan?.items?.first()?.exercise?.id)
        assertEquals(2, callCount)
    }

    @Test
    fun `swap replaces the targeted exercise and preserves order and others`() = runTest {
        val engine = mockk<LongevityEngine>()
        coEvery { engine.generateSession() } returns plan("A", "B", "C")

        val exerciseDao = mockk<ExerciseDao>()
        coEvery { exerciseDao.getById("Z") } returns exercise("Z")

        val h = holder(engine, exerciseDao) { epoch(2026, 4, 23) }
        h.ensureFresh()
        h.swap(oldId = "B", newId = "Z")

        val ids = h.state.value!!.plan.items.map { it.exercise.id }
        assertEquals(listOf("A", "Z", "C"), ids)
    }

    @Test
    fun `onSessionCompleted is a no-op; same-day access returns the same plan`() = runTest {
        val engine = mockk<LongevityEngine>()
        var callCount = 0
        coEvery { engine.generateSession() } answers {
            callCount++
            plan("E$callCount")
        }

        val h = holder(engine) { epoch(2026, 4, 23) }
        h.ensureFresh()
        val before = h.state.value

        h.onSessionCompleted()
        h.ensureFresh()
        val after = h.state.value

        assertSame(before, after)
        assertEquals(1, callCount)
    }

    @Test
    fun `swap works after completion`() = runTest {
        val engine = mockk<LongevityEngine>()
        coEvery { engine.generateSession() } returns plan("A", "B")
        val exerciseDao = mockk<ExerciseDao>()
        coEvery { exerciseDao.getById("Z") } returns exercise("Z")

        val h = holder(engine, exerciseDao) { epoch(2026, 4, 23) }
        h.ensureFresh()
        h.onSessionCompleted()
        h.swap(oldId = "A", newId = "Z")

        val ids = h.state.value!!.plan.items.map { it.exercise.id }
        assertEquals(listOf("Z", "B"), ids)
        assertTrue(h.state.value!!.plan.items.first().exercise.id == "Z")
    }
}
```

Expected: compile error `unresolved reference: TodaySessionHolder`.

- [ ] **Step 2: Implement `TodaySessionHolder.kt` (green)**

Create: `app/src/main/java/com/stretchdaily/app/core/session/TodaySessionHolder.kt`

```kotlin
package com.stretchdaily.app.core.session

import com.stretchdaily.app.core.database.dao.ExerciseDao
import com.stretchdaily.app.core.di.ApplicationScope
import com.stretchdaily.app.core.engine.LongevityEngine
import com.stretchdaily.app.core.engine.model.PlannedExercise
import com.stretchdaily.app.core.engine.model.SessionPlan
import com.stretchdaily.app.core.util.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The in-memory cache for "today's session plan." Regenerates only on
 * calendar-day rollover; completion does not reset (per design spec §6,
 * Q5 tradeoff — user can redo the same session that day).
 *
 * - First access from any ViewModel calls [ensureFresh]; if the plan is
 *   absent or its planDate is stale, the engine regenerates.
 * - [swap] mutates one item in-place. No persistence — swaps are lost on
 *   process death, another accepted tradeoff.
 * - [onSessionCompleted] is a no-op on the cached plan. The
 *   [com.stretchdaily.app.data.SessionRepository.completeSession] call
 *   happens separately in [com.stretchdaily.app.ui.screen.session.SessionPlayerViewModel].
 */
@Singleton
class TodaySessionHolder @Inject constructor(
    private val engine: LongevityEngine,
    private val exerciseDao: ExerciseDao,
    private val clock: Clock,
    @ApplicationScope private val scope: CoroutineScope,
) {

    private val _state = MutableStateFlow<TodaySession?>(null)
    val state: StateFlow<TodaySession?> = _state.asStateFlow()

    /** Single-writer guard: prevents two ViewModels racing on the first access. */
    private val mutex = Mutex()

    /**
     * Regenerates today's plan if the cache is empty or stale. Called from
     * every consuming ViewModel's `init` block. Cheap no-op on subsequent
     * same-day calls.
     */
    suspend fun ensureFresh(zoneId: ZoneId = ZoneId.systemDefault()) {
        mutex.withLock {
            val today = LocalDate.ofInstant(Instant.ofEpochMilli(clock.now()), zoneId)
            val current = _state.value
            if (current == null || current.planDate != today) {
                val plan = engine.generateSession()
                _state.value = TodaySession(today, plan)
            }
        }
    }

    /**
     * Replace the item with id [oldId] with a freshly-loaded exercise keyed
     * by [newId]. Requires an active cached plan — callers must
     * [ensureFresh] first (the overview ViewModel does).
     *
     * Same-category enforcement happens in the calling ViewModel, which
     * picks candidates from [ExerciseDao.getByCategory].
     */
    suspend fun swap(oldId: String, newId: String) {
        val newExercise = exerciseDao.getById(newId)
            ?: error("swap: exercise $newId not found")
        mutex.withLock {
            val current = _state.value
                ?: error("swap called before ensureFresh emitted a plan")
            val items = current.plan.items.map { item ->
                if (item.exercise.id == oldId) {
                    PlannedExercise(
                        exercise = newExercise,
                        effectiveSeconds = newExercise.totalTime.coerceAtMost(
                            PER_EXERCISE_CAP_SECONDS,
                        ),
                        isForced = false,
                    )
                } else {
                    item
                }
            }
            val newPlan = current.plan.copy(
                items = items,
                totalSeconds = items.sumOf { it.effectiveSeconds },
            )
            _state.value = current.copy(plan = newPlan)
        }
    }

    /**
     * Called by [com.stretchdaily.app.ui.screen.session.SessionPlayerViewModel]
     * after [com.stretchdaily.app.data.SessionRepository.completeSession]
     * returns. By design this is a no-op on the cached plan — the user can
     * redo today's session. Day rollover picks up a fresh plan.
     */
    fun onSessionCompleted() {
        // Intentionally empty. See class-level KDoc.
    }

    companion object {
        /** Matches [SessionBuilder.DEFAULT_PER_EXERCISE_CAP_SECONDS]. Kept as a local constant so this class doesn't import engine internals. */
        private const val PER_EXERCISE_CAP_SECONDS = 120
    }
}

/**
 * The value held by [TodaySessionHolder.state]. Null before first
 * [TodaySessionHolder.ensureFresh] call; non-null afterward.
 */
data class TodaySession(
    val planDate: LocalDate,
    val plan: SessionPlan,
)
```

- [ ] **Step 3: Verify — Build → Make Project, run `TodaySessionHolderTest`** — all 6 tests green.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/core/session/TodaySessionHolder.kt \
        app/src/test/java/com/stretchdaily/app/core/session/TodaySessionHolderTest.kt
git commit -m "feat(core): add TodaySessionHolder with per-day cached plan

@Singleton wrapping LongevityEngine.generateSession() with day-rollover
regeneration, in-memory swap, and onSessionCompleted no-op. Mutex guards
the first-access race. Six tests cover the state machine per spec §6.6."
git push
```

---

## Task 7: Provide `TodaySessionHolder` via a new Hilt module

Per spec §6.5: new `core/di/SessionModule.kt` reusing `@ApplicationScope` from `DatabaseModule`. The class itself has `@Inject constructor` so no `@Provides` is strictly required — but the dedicated module gives a future-proof landing spot for any other session-adjacent singletons (e.g. a future `SessionProgressCache`).

- [ ] **Step 1: Create `SessionModule.kt`**

Create: `app/src/main/java/com/stretchdaily/app/core/di/SessionModule.kt`

```kotlin
package com.stretchdaily.app.core.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module for session-adjacent singletons. `TodaySessionHolder` itself
 * uses `@Inject constructor` so no explicit `@Provides` is required — the
 * module is a landing spot for future session-layer providers (e.g. player
 * state caches).
 */
@Module
@InstallIn(SingletonComponent::class)
object SessionModule
```

Note: `TodaySessionHolder` doesn't need explicit binding — its `@Singleton @Inject constructor` wires it automatically. This empty object exists purely to anchor the `core/di/` convention and give future R4+ code an obvious home.

- [ ] **Step 2: Verify** — Build → Make Project. If Dagger/Hilt complains about ambiguous bindings, remove the module and rely on constructor injection alone. Most likely: compiles silently.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/core/di/SessionModule.kt
git commit -m "chore(di): scaffold SessionModule for session-layer singletons"
git push
```

---

## Task 8: Create `DashboardUiState`

A sealed-style discriminator (Loading vs. Loaded) keeps the view code simple. Sub-types express the composed KPI grid shape without coupling the screen to five raw flows.

- [ ] **Step 1: Create `DashboardUiState.kt`**

Create: `app/src/main/java/com/stretchdaily/app/ui/screen/dashboard/DashboardUiState.kt`

```kotlin
package com.stretchdaily.app.ui.screen.dashboard

import com.stretchdaily.app.core.engine.model.PlannedExercise
import com.stretchdaily.app.core.model.Benchmark
import com.stretchdaily.app.core.model.Category
import java.time.LocalDate

/**
 * Rendered state for the Dashboard ("today") tab.
 */
data class DashboardUiState(
    val isLoading: Boolean = true,
    val today: LocalDate = LocalDate.MIN,
    val streakDays: Int = 0,
    val weekCompleted: Set<LocalDate> = emptySet(),
    val plannedExercises: List<PlannedExercise> = emptyList(),
    val plannedMinutes: Int = 0,
    val extraFocus: Category? = null,
    val banner: BannerState = BannerState.Hidden,
    val kpis: Kpis = Kpis(),
)

/** Whether and what the benchmark banner advertises. Hidden when the preference is off. */
sealed interface BannerState {
    data object Hidden : BannerState
    data class Visible(val overdueCount: Int, val nextBenchmark: Benchmark?) : BannerState
}

/** 2×2 KPI grid: streak / total minutes / sessions / next benchmark. */
data class Kpis(
    val streakDays: Int = 0,
    val totalMinutes: Int = 0,
    val totalSessions: Int = 0,
    val nextBenchmarkDays: Int? = null,
)
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/ui/screen/dashboard/DashboardUiState.kt
git commit -m "feat(ui): add DashboardUiState + BannerState + Kpis types"
git push
```

---

## Task 9: TDD `DashboardViewModel`

Compose the flows and verify the resulting state shape. The ViewModel itself is mostly `combine(...)` plumbing; the test gives us confidence we wired the right flows together.

- [ ] **Step 1: Write the test (red)**

Create: `app/src/test/java/com/stretchdaily/app/ui/screen/dashboard/DashboardViewModelTest.kt`

```kotlin
package com.stretchdaily.app.ui.screen.dashboard

import com.stretchdaily.app.core.datastore.SettingsDataStore
import com.stretchdaily.app.core.engine.CategoryWeightCalculator
import com.stretchdaily.app.core.engine.model.PlannedExercise
import com.stretchdaily.app.core.engine.model.SessionPlan
import com.stretchdaily.app.core.model.Benchmark
import com.stretchdaily.app.core.model.BenchmarkInputType
import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.core.model.Exercise
import com.stretchdaily.app.core.model.FlexibilityTier
import com.stretchdaily.app.core.session.TodaySession
import com.stretchdaily.app.core.session.TodaySessionHolder
import com.stretchdaily.app.core.util.Clock
import com.stretchdaily.app.data.BenchmarkRepository
import com.stretchdaily.app.data.SessionRepository
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.mockk
import java.time.LocalDate
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var sessionRepository: SessionRepository
    private lateinit var benchmarkRepository: BenchmarkRepository
    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var todaySessionHolder: TodaySessionHolder
    private val calculator = CategoryWeightCalculator()
    private val clock = Clock { 1_700_000_000_000L }

    private val hipsEx = Exercise(
        id = "H01",
        name = "Pigeon",
        category = Category.HIPS,
        cues = emptyList(),
        isUnilateral = false,
        isTimed = true,
        targetReps = null,
        secondsPerRep = null,
        totalTime = 60,
        lastPerformed = null,
    )

    private val planItem = PlannedExercise(hipsEx, effectiveSeconds = 60, isForced = false)

    private val plan = SessionPlan(
        items = listOf(planItem),
        totalSeconds = 60,
        categoryWeights = mapOf(Category.HIPS to FlexibilityTier.STIFF.weight),
    )

    private val benchmark = Benchmark(
        id = "BM_SIT_AND_REACH",
        name = "Sit and Reach",
        category = Category.SPINE,
        description = "",
        unit = "cm",
        inputType = BenchmarkInputType.NUMERIC,
        tierRanges = emptyMap(),
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        sessionRepository = mockk(relaxed = true)
        benchmarkRepository = mockk(relaxed = true)
        settingsDataStore = mockk(relaxed = true)
        todaySessionHolder = mockk(relaxed = true)

        coEvery { sessionRepository.streakFlow(any()) } returns flowOf(4)
        coEvery { sessionRepository.weeklyFlow(any()) } returns flowOf(
            setOf(LocalDate.of(2023, 11, 13), LocalDate.of(2023, 11, 14))
        )
        coEvery { sessionRepository.totalsFlow } returns flowOf(
            SessionRepository.Totals(sessions = 14, totalMinutes = 120)
        )
        coEvery { benchmarkRepository.getAllBenchmarks() } returns listOf(benchmark)
        coEvery { benchmarkRepository.getLatestLogs() } returns emptyList()
        coEvery { benchmarkRepository.overdueFlow(any()) } returns flowOf(listOf(benchmark))
        coEvery { benchmarkRepository.nextDueFlow(any()) } returns flowOf(
            BenchmarkRepository.BenchmarkWithDueDate(benchmark, daysUntilDue = 4)
        )
        coEvery { settingsDataStore.benchmarkBannerEnabled } returns flowOf(false)
        coEvery { todaySessionHolder.state } returns MutableStateFlow(
            TodaySession(LocalDate.of(2023, 11, 15), plan)
        )
        coJustRun { todaySessionHolder.ensureFresh(any()) }
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(): DashboardViewModel = DashboardViewModel(
        sessionRepository = sessionRepository,
        benchmarkRepository = benchmarkRepository,
        settingsDataStore = settingsDataStore,
        todaySessionHolder = todaySessionHolder,
        categoryWeightCalculator = calculator,
        clock = clock,
    )

    @Test
    fun `Loaded state composes flows into a single UiState`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.isLoading)
        assertEquals(4, state.streakDays)
        assertEquals(4, state.kpis.streakDays)
        assertEquals(14, state.kpis.totalSessions)
        assertEquals(120, state.kpis.totalMinutes)
        assertEquals(4, state.kpis.nextBenchmarkDays)
        assertEquals(1, state.plannedExercises.size)
        assertEquals(1, state.plannedMinutes) // 60 s / 60 = 1 minute
        assertEquals(Category.HIPS, state.extraFocus)
        // Banner disabled by preference.
        assertTrue(state.banner is BannerState.Hidden)
    }

    @Test
    fun `banner surfaces when preference is true and at least one benchmark is overdue`() = runTest {
        coEvery { settingsDataStore.benchmarkBannerEnabled } returns flowOf(true)
        val vm = viewModel()
        advanceUntilIdle()

        val banner = vm.state.value.banner
        assertTrue(banner is BannerState.Visible)
        val visible = banner as BannerState.Visible
        assertEquals(1, visible.overdueCount)
        assertEquals("BM_SIT_AND_REACH", visible.nextBenchmark?.id)
    }

    @Test
    fun `banner stays hidden when preference is true but nothing is overdue`() = runTest {
        coEvery { settingsDataStore.benchmarkBannerEnabled } returns flowOf(true)
        coEvery { benchmarkRepository.overdueFlow(any()) } returns flowOf(emptyList())
        val vm = viewModel()
        advanceUntilIdle()

        assertTrue(vm.state.value.banner is BannerState.Hidden)
    }

    @Test
    fun `ensureFresh is invoked on init`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        io.mockk.coVerify(exactly = 1) { todaySessionHolder.ensureFresh(any()) }
        // Avoid unused-var warnings.
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun `extraFocus resolves to the stiffest category`() = runTest {
        // Two categories in the weight map — HIPS at STIFF (3.0) and SPINE at
        // AVERAGE (2.0). Stiffest has the highest weight. Plan has both.
        val spineEx = hipsEx.copy(id = "SP01", category = Category.SPINE)
        val planWithBoth = SessionPlan(
            items = listOf(planItem, PlannedExercise(spineEx, 60, false)),
            totalSeconds = 120,
            categoryWeights = mapOf(
                Category.HIPS to FlexibilityTier.STIFF.weight,
                Category.SPINE to FlexibilityTier.AVERAGE.weight,
            ),
        )
        coEvery { todaySessionHolder.state } returns MutableStateFlow(
            TodaySession(LocalDate.of(2023, 11, 15), planWithBoth)
        )

        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(Category.HIPS, vm.state.value.extraFocus)
    }
}
```

Expected: `unresolved reference: DashboardViewModel`.

- [ ] **Step 2: Implement `DashboardViewModel.kt` (green)**

Create: `app/src/main/java/com/stretchdaily/app/ui/screen/dashboard/DashboardViewModel.kt`

```kotlin
package com.stretchdaily.app.ui.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stretchdaily.app.core.datastore.SettingsDataStore
import com.stretchdaily.app.core.engine.CategoryWeightCalculator
import com.stretchdaily.app.core.model.Category
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Observes today's plan + live repository flows + the banner preference and
 * emits a composed [DashboardUiState]. Fire-and-forget [TodaySessionHolder.ensureFresh]
 * on init so the state flips to `Loaded` the moment the engine returns.
 */
@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val benchmarkRepository: BenchmarkRepository,
    private val settingsDataStore: SettingsDataStore,
    private val todaySessionHolder: TodaySessionHolder,
    private val categoryWeightCalculator: CategoryWeightCalculator,
    private val clock: Clock,
) : ViewModel() {

    private val zoneId: ZoneId = ZoneId.systemDefault()

    /**
     * Combined upstream. Seven inputs composed once; the screen only
     * collects a single `StateFlow<DashboardUiState>`.
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
        val overdue = values[4] as List<com.stretchdaily.app.core.model.Benchmark>
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
```

**Note on the 7-arg `combine`:** Kotlin's typed `combine` overloads top out at 5 sources. Passing 7 uses the `vararg` overload which returns `Array<Any?>`, hence the index-based unpacking with `@Suppress("UNCHECKED_CAST")`. Ugly but safe — covered by the ViewModel test.

- [ ] **Step 3: Verify — Build → Make Project, run `DashboardViewModelTest`** — all 5 tests green.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/ui/screen/dashboard/DashboardViewModel.kt \
        app/src/test/java/com/stretchdaily/app/ui/screen/dashboard/DashboardViewModelTest.kt
git commit -m "feat(ui): add DashboardViewModel composing 7 flows into UiState

combine() over TodaySessionHolder.state + streak + weekly + totals +
overdue + nextDue + benchmarkBannerEnabled. ensureFresh() runs on init
so the engine regenerates today's plan if the cache is stale or empty."
git push
```

---

## Task 10: Build `DashboardScreen`

The visual piece. Everything here renders from R1 tokens and R2 primitives — no new components should be needed.

**Layout (top-to-bottom, matching handoff §1 Dashboard):**

```
LazyColumn (padding = Theme.dims.padScreen)
├── Top strip           — Row: date (MonoCaps, ink3) · Spacer · flame icon + streak (accent)
├── Hero heading        — "Stretch Daily" (displayLg) + subtitle ("{n} exercises · ~{m} min · mixed full body", ink2)
├── Banner (optional)   — accent-soft surface card with sparkle tile + title + chevron, if BannerState.Visible
├── Today's session card — surface card:
│                         ├── Row: (eyebrow "TODAY'S SESSION" + title "{m} minutes" + "Extra focus: {category}") | CircleButton.Ink(IconName.Play)
│                         └── Row of per-exercise color bars (Category.tint(), height 5 dp, radius 3 dp)
├── MonoCaps "THIS WEEK" header
├── WeekStrip(completed = weekCompleted, today = today)
├── MonoCaps "SNAPSHOT" header
└── Kpi grid (2×2 Row of 2)   — 4 KpiCards
```

- [ ] **Step 1: Create `DashboardScreen.kt`**

Create: `app/src/main/java/com/stretchdaily/app/ui/screen/dashboard/DashboardScreen.kt`

```kotlin
package com.stretchdaily.app.ui.screen.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stretchdaily.app.core.engine.model.PlannedExercise
import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.ui.components.AppIcon
import com.stretchdaily.app.ui.components.CircleButton
import com.stretchdaily.app.ui.components.CircleButtonSize
import com.stretchdaily.app.ui.components.CircleButtonVariant
import com.stretchdaily.app.ui.components.IconName
import com.stretchdaily.app.ui.components.KpiCard
import com.stretchdaily.app.ui.components.MonoCaps
import com.stretchdaily.app.ui.components.MonoCapsSize
import com.stretchdaily.app.ui.components.WeekStrip
import com.stretchdaily.app.ui.theme.Theme
import com.stretchdaily.app.ui.theme.tint
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Dashboard (tab = "today"). See design spec §8.1. Scrolls; the bottom 100 dp
 * of [Theme.dims.padScreen] keeps content above the fixed bottom nav.
 */
@Composable
fun DashboardScreen(
    onStartSession: () -> Unit,
    onBenchmarkBannerTap: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val vm: DashboardViewModel = hiltViewModel()
    val state by vm.state.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .background(Theme.colors.bg),
        contentPadding = combinePadding(Theme.dims.padScreen, contentPadding),
        verticalArrangement = Arrangement.spacedBy(Theme.dims.gapSection),
    ) {
        item {
            TopStrip(todayLabel = formatDate(state.today), streak = state.streakDays)
        }
        item { HeroHeader(count = state.plannedExercises.size, minutes = state.plannedMinutes) }

        // Optional banner.
        when (val banner = state.banner) {
            is BannerState.Visible -> item {
                BenchmarkBanner(
                    overdueCount = banner.overdueCount,
                    onClick = onBenchmarkBannerTap,
                )
            }
            BannerState.Hidden -> Unit
        }

        item {
            TodaySessionCard(
                minutes = state.plannedMinutes,
                extraFocus = state.extraFocus,
                items = state.plannedExercises,
                onStartSession = onStartSession,
            )
        }

        item { SectionLabel("THIS WEEK") }
        item {
            WeekStrip(
                completed = state.weekCompleted,
                today = state.today,
            )
        }

        item { SectionLabel("SNAPSHOT") }
        item { KpiGrid(state.kpis) }
    }
}

@Composable
private fun TopStrip(todayLabel: String, streak: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        MonoCaps(text = todayLabel, color = Theme.colors.ink3, size = MonoCapsSize.Regular)
        Spacer(modifier = Modifier.padding(4.dp).fillMaxWidth().weight(1f))
        AppIcon(
            name = IconName.Flame,
            size = 16.dp,
            tint = Theme.colors.accent,
            contentDescription = null,
        )
        Spacer(modifier = Modifier.padding(start = 4.dp))
        MonoCaps(
            text = "$streak",
            color = Theme.colors.accent,
            size = MonoCapsSize.Regular,
        )
    }
}

@Composable
private fun HeroHeader(count: Int, minutes: Int) {
    Column {
        Text(
            text = "Stretch Daily",
            style = Theme.typo.displayLg,
            color = Theme.colors.ink,
        )
        Spacer(modifier = Modifier.padding(top = 2.dp))
        Text(
            text = "$count exercises · ~$minutes minutes · mixed full body",
            style = Theme.typo.bodyMd,
            color = Theme.colors.ink2,
        )
    }
}

@Composable
private fun BenchmarkBanner(overdueCount: Int, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Theme.dims.radiusMd))
            .background(Theme.colors.accentSoft)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Theme.colors.bg2),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(
                name = IconName.Sparkle,
                size = 18.dp,
                tint = Theme.colors.accent,
                contentDescription = null,
            )
        }
        Column(modifier = Modifier.padding(horizontal = 12.dp).weight(1f)) {
            Text(
                text = "Benchmark day is here",
                style = Theme.typo.bodyLg.copy(fontWeight = FontWeight.SemiBold),
                color = Theme.colors.ink,
            )
            Text(
                text = "$overdueCount tests waiting on a fresh reading.",
                style = Theme.typo.bodySm,
                color = Theme.colors.ink2,
            )
        }
        AppIcon(
            name = IconName.ChevronRight,
            size = 18.dp,
            tint = Theme.colors.ink2,
            contentDescription = "Open benchmark carousel",
        )
    }
}

@Composable
private fun TodaySessionCard(
    minutes: Int,
    extraFocus: Category?,
    items: List<PlannedExercise>,
    onStartSession: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Theme.dims.radiusLg))
            .background(Theme.colors.surface)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                MonoCaps(
                    text = "TODAY'S SESSION",
                    color = Theme.colors.ink3,
                    size = MonoCapsSize.Regular,
                )
                Spacer(modifier = Modifier.padding(top = 4.dp))
                Text(
                    text = "$minutes minutes",
                    style = Theme.typo.displayMd,
                    color = Theme.colors.ink,
                )
                extraFocus?.let {
                    Spacer(modifier = Modifier.padding(top = 2.dp))
                    Text(
                        text = "Extra focus: ${it.displayName}",
                        style = Theme.typo.bodySm,
                        color = Theme.colors.ink2,
                    )
                }
            }
            CircleButton(
                onClick = onStartSession,
                icon = IconName.Play,
                size = CircleButtonSize.Large58,
                variant = CircleButtonVariant.Accent,
                contentDescription = "Begin session",
            )
        }
        if (items.isNotEmpty()) SessionOrderBars(items)
    }
}

@Composable
private fun SessionOrderBars(items: List<PlannedExercise>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
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
private fun SectionLabel(text: String) {
    MonoCaps(text = text, color = Theme.colors.ink3, size = MonoCapsSize.Regular)
}

@Composable
private fun KpiGrid(kpis: Kpis) {
    Column(verticalArrangement = Arrangement.spacedBy(Theme.dims.gapList)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Theme.dims.gapList)) {
            KpiCard(
                eyebrow = "Streak",
                icon = IconName.Flame,
                value = kpis.streakDays.toString(),
                suffix = if (kpis.streakDays == 1) "day" else "days",
                modifier = Modifier.weight(1f),
            )
            KpiCard(
                eyebrow = "Total time",
                icon = IconName.Sparkle,
                value = kpis.totalMinutes.toString(),
                suffix = "min",
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Theme.dims.gapList)) {
            KpiCard(
                eyebrow = "Sessions",
                icon = IconName.Check,
                value = kpis.totalSessions.toString(),
                suffix = "done",
                modifier = Modifier.weight(1f),
            )
            KpiCard(
                eyebrow = "Next benchmark",
                icon = IconName.Sparkle,
                value = kpis.nextBenchmarkDays?.toString() ?: "—",
                suffix = if (kpis.nextBenchmarkDays != null) "d" else "",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Merge the outer bottom-nav inset padding with the screen-local
 * [Theme.dims.padScreen]. The outer [contentPadding] has only a nonzero
 * bottom (the 100 dp nav offset is already baked into [Theme.dims.padScreen]
 * but we still add the Scaffold's insets for edge-to-edge safety).
 */
@Composable
private fun combinePadding(inner: PaddingValues, outer: PaddingValues): PaddingValues {
    // Take the max on each axis — simple, avoids double-counting.
    return PaddingValues(
        start = maxOf(
            inner.calculateStartPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
            outer.calculateStartPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
        ),
        top = maxOf(inner.calculateTopPadding(), outer.calculateTopPadding()),
        end = maxOf(
            inner.calculateEndPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
            outer.calculateEndPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
        ),
        bottom = maxOf(inner.calculateBottomPadding(), outer.calculateBottomPadding()),
    )
}

private val DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())

private fun formatDate(date: java.time.LocalDate): String =
    date.format(DATE_FORMATTER)
```

**Implementation notes / likely adjustments:**

- `CircleButton` enums (`CircleButtonSize.Large58`, `CircleButtonVariant.Accent`) are landed by R2 — the call site above matches.
- `KpiCard(... modifier: Modifier)` needs to accept a `Modifier` parameter — verify that R2's `KpiCard.kt` has it. If not, add it (single-arg overload addition, no behavior change).
- `combinePadding` could use `PaddingValues.calculateStartPadding` helpers from `androidx.compose.foundation.layout` — adjust imports if the compiler complains. A simpler alternative: just use `Theme.dims.padScreen` directly as `contentPadding` on the LazyColumn, and rely on the outer Scaffold's `contentWindowInsets = WindowInsets(0)` (R1 convention) to mean the inner screen owns its padding entirely.

- [ ] **Step 2: Verify — Build → Make Project** — no new tests here; visual check once wired in Task 11.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/ui/screen/dashboard/DashboardScreen.kt
git commit -m "feat(ui): add DashboardScreen composed from R2 primitives

Top strip · Hero heading · optional BenchmarkBanner · Today's session
card (eyebrow + minutes + extra focus + play CircleButton + per-move
category tint bars) · WeekStrip · 2x2 KpiGrid. All styling via
Theme.colors / Theme.typo / Theme.dims — no literal colors."
git push
```

---

## Task 11: Wire the Dashboard into `StretchDailyNavHost`

Replace the R1 placeholder for the `"today"` tab with the real `DashboardScreen`. The session graph's `"session/overview"` is still a placeholder (that's R4's territory); the CTA and banner both navigate there.

- [ ] **Step 1: Edit `StretchDailyNavHost.kt`**

Find the composable registered for `route = ROUTE_TODAY` (a placeholder from R1) and replace it. Keep everything else untouched — placeholders for `session`, `log`, `progress`, `settings` stay.

Replace the `composable(ROUTE_TODAY) { ... }` block with:

```kotlin
composable(ROUTE_TODAY) {
    DashboardScreen(
        onStartSession = { navController.navigate(ROUTE_SESSION_OVERVIEW) },
        onBenchmarkBannerTap = { navController.navigate("$ROUTE_CAROUSEL/0") },
        contentPadding = innerPadding,
    )
}
```

Adjust import block:

```kotlin
import com.stretchdaily.app.ui.screen.dashboard.DashboardScreen
```

Remove the now-unused `PlaceholderScreen` import if no tab still uses it. Quick check: `session/overview`, `log`, `progress`, `settings` all still use `PlaceholderScreen` in R3, so the import stays.

- [ ] **Step 2: Manual smoke test (post-compile)**

Build → Make Project, then Run → App on a device/emulator. Verify:

- App opens on the Dashboard tab with cream background and Manrope hero heading.
- Today's date renders in mono caps, upper-left.
- Streak is `0` initially (no sessions recorded).
- "TODAY'S SESSION" card shows `~0 minutes` initially because the engine returned 0 items (catalog is seeded, so it should be 7–8 actually — if the engine fired, expect `≈12 minutes`).
- Tap Play CircleButton → navigates to `session/overview` placeholder (R4 stub).
- If `benchmarkBannerEnabled = true` (via ADB `datastore` tweak, optional), banner shows and tap navigates to `carousel/0` placeholder.
- WeekStrip renders 7 empty squares, today's cell has the dashed outline.
- KPI grid shows 4 cards, all with 0s and `—` for next benchmark.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/ui/navigation/StretchDailyNavHost.kt
git commit -m "feat(navigation): replace TODAY placeholder with DashboardScreen

CTA routes to session/overview (still placeholder); banner routes to
carousel/0 (ditto). Other 4 tabs unchanged."
git push
```

---

## Task 12: Final build, test sweep, and pixel QA

- [ ] **Step 1: Full build + test in Android Studio**

- Build → Clean Project
- Build → Rebuild Project
- Run All Tests (right-click `app/src/test/java` → Run 'Tests in 'java'')

Expected: ~all tests pass, including:

- Preserved R1 tests (engine, benchmark, data layer — unchanged).
- R2 helper tests (`WeekStripHelpersTest`, `SegmentProgressHelpersTest`).
- R3-new: `SessionRepositoryFlowTest` (3), `BenchmarkRepositoryDueSelectionTest` (8), `TodaySessionHolderTest` (6), `DashboardViewModelTest` (5).

If any R1/R2 test fails: do **not** mask by editing the test. Investigate the regression first — most likely a type mismatch introduced by a renamed DAO or mistaken import.

- [ ] **Step 2: Visual QA against the handoff prototype**

Open `docs/design_handoff_stretch_daily_v3/reference/index.html` in a browser (Sage theme, populated history toggle on, benchmark banner off). Compare side-by-side to the Android app Dashboard:

| Element | Reference | Our rendering | Notes |
|---|---|---|---|
| Background color | Cream `#fbf8f0` | `Theme.colors.bg` | Should match exactly. |
| Top strip date | "THU, APR 23" mono caps ink3 | MonoCaps Regular | Font size 11, letter-spacing ~1.2 sp. |
| Hero heading | 30 px Manrope 500 | `Theme.typo.displayLg` | Verify letter-spacing matches. |
| Hero subtitle | 14 px ink-2 | `Theme.typo.bodyMd` | Font weight 400. |
| Banner (if enabled) | accent-soft bg + sparkle tile | `Theme.colors.accentSoft` | Radius 16 dp, padding 14/16. |
| Session card | 22 dp radius, 20/20 padding | `Theme.dims.radiusLg` | Play circle 58 dp accent. |
| Bars | One per move, category-tinted | `Category.tint()` | Height 5 dp, gap 4 dp, radius 3 dp. |
| WeekStrip | 7 squares 6 dp gaps | R2 component | Today dashed outline visible. |
| KpiCard grid | 2×2, 16 dp radius | R2 component | Value in Manrope 500 26 sp. |

Log any pixel-level diffs in the PR description; non-blocking unless they're framework-level (e.g. wrong font family).

- [ ] **Step 3: Verify the bottom nav and other tabs**

Tap each of the other 4 tabs (SESSION, LOG, PROGRESS, SETTINGS) and verify:

- Each still shows the R1 `PlaceholderScreen`.
- Icons render in mono-caps label style per R1 decision.
- Back-stack: tapping TODAY from any other tab returns to the Dashboard with scroll position preserved (Compose Navigation default behavior).

- [ ] **Step 4: Push final commits**

If anything was fixed during smoke testing, commit it:

```bash
git status
# ...review any fix-ups...
git add <file>
git commit -m "fix(ui): <short>"
git push
```

---

## Task 13: Open the PR

- [ ] **Step 1: Create PR against `development`**

```bash
gh pr create --base development \
  --title "R3 — Dashboard + TodaySessionHolder" \
  --body "$(cat <<'EOF'
## Summary

- Adds `core/session/TodaySessionHolder` (`@Singleton`, per-day cached plan) with 6 unit tests per spec §6.6.
- New reactive flows: `SessionRepository.{streakFlow, weeklyFlow, totalsFlow}` and `BenchmarkRepository.{overdueFlow, nextDueFlow}` — wraps around existing DAO flows, math is TDD'd in companion helpers.
- `benchmarkBannerEnabled` preference added to `SettingsDataStore` (default `false`, matching the user tweak in the design brief).
- Dashboard tab (`"today"`) replaces R1 placeholder: hero heading + top strip + (optional) benchmark banner + Today's session card + WeekStrip + 2×2 KpiGrid, all composed via R2 primitives.

## Test plan

- [x] All preserved tests pass (engine, benchmark resolver, data layer, R2 helpers)
- [x] New tests pass: `TodaySessionHolderTest` (6), `BenchmarkRepositoryDueSelectionTest` (8), `SessionRepositoryFlowTest` (3), `DashboardViewModelTest` (5).
- [ ] **Ramon — manual run on device:**
  - Fresh install: Dashboard opens, engine generates a plan, KPIs show 0s, week strip has "today" dashed outline.
  - Complete a session (via the old placeholder? — R4 stub, may not record yet). Streak bumps to 1 next session open.
  - Flip `benchmarkBannerEnabled` via ADB (`adb shell ...`) → banner appears with overdue count.
  - Tap Play CircleButton → navigates to session/overview placeholder (R4).
  - Tap banner → navigates to carousel/0 placeholder.
  - Re-open app same day: same session plan (TodaySessionHolder cached).
  - Advance device date to tomorrow, re-open: fresh plan (TodaySessionHolder regenerates).

## Design spec

[2026-04-23-sage-redesign-design.md](../docs/superpowers/specs/2026-04-23-sage-redesign-design.md) §6, §8.1, §11.3.

## Implementation plan

[2026-04-23-sage-redesign-R3-dashboard-and-today-session.md](../docs/superpowers/plans/2026-04-23-sage-redesign-R3-dashboard-and-today-session.md).

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

- [ ] **Step 2: Paste the PR URL back to Ramon.**

---

## After R3 lands

The app shows a live Sage Dashboard backed by real data. The CTA still goes to a placeholder; R4 fills it with the session flow.

Next: R4 plan ([`2026-04-23-sage-redesign-R4-session-flow.md`](./2026-04-23-sage-redesign-R4-session-flow.md)) — Session overview (with swap sheet), Session player (timer, L/R cycling, SegmentProgress, audio cues), Session complete (full-bleed accent). Size-guard in effect: may split into R4a / R4b if the phase grows past ~1 week of work.
