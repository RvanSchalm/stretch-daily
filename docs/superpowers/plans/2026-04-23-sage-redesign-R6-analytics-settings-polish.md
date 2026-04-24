# Implementation plan — Sage redesign R6: Analytics + Settings + icon + polish

**Parent spec:** [`docs/superpowers/specs/2026-04-23-sage-redesign-design.md`](../specs/2026-04-23-sage-redesign-design.md) (§8.6, §8.8, §9, §11.6).
**Prior phases:** R1 tokens, R2 primitives, R3 Dashboard, R4 Session flow, R5 Log + Carousel.
**Branch:** `redesign/r6-analytics-settings-polish` (PR target `development`).

## Purpose

Ship the final two screens — **Analytics** (tab `"progress"`) and **Settings** (tab `"settings"`) — replacing their R1 placeholder stubs. Alongside the screen work, refresh the launcher icon and splash theme to the Sage palette, run the cross-screen accessibility audit, verify R8 minification keep-rules against every new code path from R1–R5, and execute the final pixel QA matrix vs. the handoff's `reference/index.html`.

When R6 lands, **the redesign is complete**. All 5 tabs are real, both overlay graphs (session player, benchmark carousel) work, every primitive has a consumer, and the app's first-impression surfaces (icon, splash) are on-brand.

## Architecture

**Analytics** is the last screen to exercise the `BigChart` primitive built in R2. Its ViewModel fans out per-benchmark via `flatMapLatest` over `BenchmarkRepository.observeAllBenchmarks()` — same pattern as R5's `BenchmarkLogViewModel`, but the payload per row is a full `ProgressSeries` (not a 6-month sparkline) and a 6-month delta chip instead of an expand-to-reveal history. A category filter (screen-local, not persisted) narrows the card list; tapping "All" restores everything.

**Settings** is rebuilt from scratch (`ui/settings/` was wiped at R1). It keeps every existing data flow preserved in `core/datastore/SettingsDataStore` + `data/DataPortRepository`: the audio toggle, the SAF export/import launchers, and the delete-all confirmation. On top of that it adds:
- the **benchmark banner toggle** (wires `SettingsDataStore.benchmarkBannerEnabled` — the preference key was added in R3, default `false`);
- the **Library group** — three readonly rows showing `exercises across categories`, `benchmarks`, `sessions logged total`, backed by a new `libraryStats` flow combining `ExerciseDao.observeAll`, `BenchmarkRepository.observeAllBenchmarks`, and `SessionRepository.totalsFlow` (the latter already landed in R3).

**Delta math** for Analytics is a new pure helper `benchmarkDelta(logs, now, better)` living next to `BenchmarkProgressBuilder`. It picks the nearest log to `now − 6 months` and returns a `BenchmarkDelta(rawDelta, improved)` or `null`. The `better` direction comes from a tiny companion `benchmarkBetter(id): Better` that reads the same source of truth as `TierResolver` — I considered surfacing the existing `TierResolver.Direction` but kept it private, since the three call sites (Analytics card, Library future work, delta helper) don't share a ViewModel and widening `Direction` would couple pure benchmark math to unrelated surfaces.

**Icon + splash** flip `ic_launcher_background` from `#0D0D0D` (dark) to the Sage accent `#5c7a4a`, and re-tint `ic_launcher_foreground.xml`'s five paths from `#FF8C00` (orange) to `#f5f3ea` (cream). `Theme.StretchDaily.Splash` similarly moves to `#fbf8f0` bg + cream figure tint. Per spec §9, this eliminates the dark→cream flash at launch.

**Accessibility audit** re-applies the Phase 8 discipline (see CLAUDE.md §2): every interactive `Icon`, `CircleButton`, and `Pill` gets a meaningful `contentDescription`; every large touch target is ≥ 44 dp × 44 dp; semantic heading annotations live on screen-level `Text`s. The audit runs across **every screen built in R3–R6** — Dashboard, Session overview, Session player, Session complete, Log, Carousel, Analytics, Settings.

**R8 keep-rules** verification is a diff review of `app/proguard-rules.pro` against the new code: no new kotlinx-serialization classes ship in R1–R6 (`ExportPayload` is preserved unchanged), but several new Hilt `@HiltViewModel` classes and new Compose call sites land. The existing `-keep class dagger.hilt.**` and `-keep class androidx.compose.**` rules cover both. We run the minified release build once end-to-end on an emulator to confirm.

## Tech Stack

Kotlin 2.0, Compose (BOM 2024.10), Hilt 2.52, Room 2.6, kotlinx-serialization, DataStore Preferences. All existing. `mockk`-driven ViewModel tests + plain JUnit for pure helpers — same pattern as R3–R5.

## Preservation — domain stays intact

Per spec §3.1, R6 adds zero new DAOs, zero new entities, zero new engine classes. It:
- adds **one** pure helper (`benchmarkDelta` + `benchmarkBetter`) under `core/benchmark/`;
- consumes the R3-landed `SessionRepository.totalsFlow` and `SettingsDataStore.benchmarkBannerEnabled`;
- consumes the R2-landed `BigChart` primitive;
- leaves `BenchmarkRepository`, `DataPortRepository`, `TierResolver`, `BenchmarkProgressBuilder`, all engine classes, and every test under `app/src/test/core/` and `app/src/test/data/` untouched.

Preserving those 13 test files is the guardrail that the domain layer compiles and behaves identically at the end of R6.

---

## Task 0: Verify R5 landed; create R6 branch

Same pattern used in R1–R5.

- [ ] **Step 1: Confirm `development` is at R5's HEAD and clean**

```bash
git checkout development
git pull
git log --oneline -5
git status
```

Expected: the last commit is the R5 merge ("Merge pull request … Sage redesign R5 — Log + Carousel"), working tree clean.

- [ ] **Step 2: Create the R6 branch**

```bash
git checkout -b redesign/r6-analytics-settings-polish
git push -u origin redesign/r6-analytics-settings-polish
```

---

## Task 1: Add `Better` + `benchmarkBetter` helper

Pure Kotlin lookup for "does a higher value mean better or worse flexibility on this benchmark?" — used by Analytics to colour the delta chip `accent` (improved) vs. `warn` (regressed).

The direction is already encoded in `TierResolver`'s private `Direction` enum, but that's scoped for numeric-to-tier resolution. Rather than widen it, we add a sibling top-level function that reads the same source of truth. Matches the R5 pattern where we added `sparklineValues(...)` next to `BenchmarkProgressBuilder` rather than widening `ProgressSeries`.

**Files:**
- Create: `app/src/main/java/com/stretchdaily/app/core/benchmark/BenchmarkBetter.kt`
- Create test: `app/src/test/java/com/stretchdaily/app/core/benchmark/BenchmarkBetterTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.stretchdaily.app.core.benchmark

import org.junit.Assert.assertEquals
import org.junit.Test

class BenchmarkBetterTest {

    @Test
    fun `ascending benchmarks resolve to HIGHER`() {
        assertEquals(Better.HIGHER, benchmarkBetter("BM_CERVICAL_ROTATION"))
        assertEquals(Better.HIGHER, benchmarkBetter("BM_THORACIC_ROTATION"))
        assertEquals(Better.HIGHER, benchmarkBetter("BM_KNEE_TO_WALL"))
        assertEquals(Better.HIGHER, benchmarkBetter("BM_WRIST_EXTENSION"))
        assertEquals(Better.HIGHER, benchmarkBetter("BM_WRIST_FLEXION"))
    }

    @Test
    fun `descending benchmarks resolve to LOWER`() {
        assertEquals(Better.LOWER, benchmarkBetter("BM_APLEY_SCRATCH"))
        assertEquals(Better.LOWER, benchmarkBetter("BM_BUTTERFLY"))
        assertEquals(Better.LOWER, benchmarkBetter("BM_SIT_AND_REACH"))
        assertEquals(Better.LOWER, benchmarkBetter("BM_THOMAS_TEST"))
    }

    @Test
    fun `categorical benchmark resolves to CATEGORICAL`() {
        assertEquals(Better.CATEGORICAL, benchmarkBetter("BM_ATG_SPLIT_SQUAT"))
    }

    @Test
    fun `unknown benchmark id resolves to CATEGORICAL as a safe default`() {
        // "CATEGORICAL" signals "there's no numeric direction to score";
        // the delta chip renders nothing in that case.
        assertEquals(Better.CATEGORICAL, benchmarkBetter("BM_MYSTERY"))
    }
}
```

Run it — expect red, compilation failure (neither `Better` nor `benchmarkBetter` exist).

- [ ] **Step 2: Create the helper**

```kotlin
package com.stretchdaily.app.core.benchmark

/**
 * Whether a higher reading on a benchmark indicates more flexibility
 * ([HIGHER]) or less ([LOWER]), or whether the benchmark is qualitative
 * and has no numeric direction ([CATEGORICAL]).
 *
 * Duplicates [TierResolver]'s private `Direction` field intentionally —
 * the resolver is scoped for numeric → tier math and keeping that enum
 * private lets us change its internals freely. Analytics and (later)
 * Library stats only need the direction, not the breakpoints, so they
 * read from this sibling lookup.
 */
enum class Better { HIGHER, LOWER, CATEGORICAL }

/**
 * Pure lookup: direction for a benchmark id. Unknown ids fall back to
 * [Better.CATEGORICAL] — callers interpret that as "don't draw a delta
 * chip" rather than throwing, which keeps Analytics resilient to future
 * benchmark seed changes.
 */
fun benchmarkBetter(benchmarkId: String): Better = when (benchmarkId) {
    "BM_CERVICAL_ROTATION",
    "BM_THORACIC_ROTATION",
    "BM_KNEE_TO_WALL",
    "BM_WRIST_EXTENSION",
    "BM_WRIST_FLEXION" -> Better.HIGHER

    "BM_APLEY_SCRATCH",
    "BM_BUTTERFLY",
    "BM_SIT_AND_REACH",
    "BM_THOMAS_TEST" -> Better.LOWER

    else -> Better.CATEGORICAL
}
```

Re-run the test — all 4 cases should pass.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/core/benchmark/BenchmarkBetter.kt \
        app/src/test/java/com/stretchdaily/app/core/benchmark/BenchmarkBetterTest.kt
git commit -m "feat(benchmark): add benchmarkBetter direction lookup

Pure sibling to TierResolver. Analytics delta chip uses this to pick
accent (improved) vs warn (regressed) colour. Unknown ids default to
CATEGORICAL so the chip simply doesn't render — keeps the screen
resilient to seed changes."
git push
```

---

## Task 2: Add `benchmarkDelta` helper + TDD

Pure helper. Given a list of logs + "now" + direction, returns a `BenchmarkDelta(rawDelta, improved)` or `null`. Six months back is computed via `java.time.LocalDate.minusMonths(6)` anchored at zone-converted `now`. "Nearest-date fallback" picks the log with the smallest absolute date distance to that anchor.

**Files:**
- Create: `app/src/main/java/com/stretchdaily/app/core/benchmark/BenchmarkDelta.kt`
- Create test: `app/src/test/java/com/stretchdaily/app/core/benchmark/BenchmarkDeltaTest.kt`

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.stretchdaily.app.core.benchmark

import com.stretchdaily.app.core.model.BenchmarkLog
import com.stretchdaily.app.core.model.FlexibilityTier
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BenchmarkDeltaTest {

    private val zone: ZoneId = ZoneId.of("UTC")

    private fun midday(year: Int, month: Int, day: Int): Long =
        ZonedDateTime.of(LocalDate.of(year, month, day).atTime(12, 0), zone)
            .toInstant().toEpochMilli()

    private fun numericLog(date: Long, raw: String): BenchmarkLog = BenchmarkLog(
        benchmarkId = "BM_CERVICAL_ROTATION",
        rawValue = raw,
        resolvedTier = FlexibilityTier.AVERAGE,
        loggedAt = date,
    )

    @Test
    fun `empty logs returns null`() {
        val result = benchmarkDelta(
            logs = emptyList(),
            nowEpochMillis = midday(2026, 4, 23),
            better = Better.HIGHER,
            zoneId = zone,
        )
        assertNull(result)
    }

    @Test
    fun `single log returns null (no baseline to compare)`() {
        val result = benchmarkDelta(
            logs = listOf(numericLog(midday(2026, 4, 10), "70")),
            nowEpochMillis = midday(2026, 4, 23),
            better = Better.HIGHER,
            zoneId = zone,
        )
        assertNull(result)
    }

    @Test
    fun `categorical direction returns null (no delta to show)`() {
        val result = benchmarkDelta(
            logs = listOf(
                numericLog(midday(2025, 10, 1), "LEVEL_2"),
                numericLog(midday(2026, 4, 1), "LEVEL_3"),
            ),
            nowEpochMillis = midday(2026, 4, 23),
            better = Better.CATEGORICAL,
            zoneId = zone,
        )
        assertNull(result)
    }

    @Test
    fun `HIGHER delta is positive when latest exceeds 6mo-prior baseline`() {
        val logs = listOf(
            numericLog(midday(2025, 10, 23), "65"),  // ~ 6 months ago
            numericLog(midday(2026, 4, 10), "75"),   // latest
        )
        val result = benchmarkDelta(
            logs = logs,
            nowEpochMillis = midday(2026, 4, 23),
            better = Better.HIGHER,
            zoneId = zone,
        )
        requireNotNull(result)
        assertEquals(10.0, result.rawDelta, 0.001)
        assertTrue("expected improvement", result.improved)
    }

    @Test
    fun `LOWER delta flips sign — smaller value means improvement`() {
        // Sit and Reach: lower value = more flexible.
        val logs = listOf(
            numericLog(midday(2025, 10, 23), "15"),
            numericLog(midday(2026, 4, 10), "-5"),
        )
        val result = benchmarkDelta(
            logs = logs,
            nowEpochMillis = midday(2026, 4, 23),
            better = Better.LOWER,
            zoneId = zone,
        )
        requireNotNull(result)
        assertEquals(-20.0, result.rawDelta, 0.001)
        assertTrue("expected improvement (lower is better)", result.improved)
    }

    @Test
    fun `regression flag true when direction disagrees`() {
        val logs = listOf(
            numericLog(midday(2025, 10, 23), "80"),
            numericLog(midday(2026, 4, 10), "72"),
        )
        val result = benchmarkDelta(
            logs = logs,
            nowEpochMillis = midday(2026, 4, 23),
            better = Better.HIGHER,
            zoneId = zone,
        )
        requireNotNull(result)
        assertEquals(-8.0, result.rawDelta, 0.001)
        assertTrue("expected regression (HIGHER benchmark, delta negative)", !result.improved)
    }

    @Test
    fun `nearest-date fallback picks closest log to 6mo anchor`() {
        // Target is 2025-10-23 (6 months before now).
        // Closest candidate is 2025-11-05 (13 days later).
        // 2024-01-02 is almost 22 months away and must not win.
        val logs = listOf(
            numericLog(midday(2024, 1, 2), "50"),
            numericLog(midday(2025, 11, 5), "65"),
            numericLog(midday(2026, 4, 10), "70"),
        )
        val result = benchmarkDelta(
            logs = logs,
            nowEpochMillis = midday(2026, 4, 23),
            better = Better.HIGHER,
            zoneId = zone,
        )
        requireNotNull(result)
        // 70 - 65 = 5
        assertEquals(5.0, result.rawDelta, 0.001)
    }

    @Test
    fun `non-parseable rawValue on baseline returns null`() {
        // If the 6mo baseline is a categorical reading (non-numeric rawValue)
        // there's nothing to subtract — return null defensively.
        val logs = listOf(
            numericLog(midday(2025, 10, 23), "LEVEL_3"),
            numericLog(midday(2026, 4, 10), "75"),
        )
        val result = benchmarkDelta(
            logs = logs,
            nowEpochMillis = midday(2026, 4, 23),
            better = Better.HIGHER,
            zoneId = zone,
        )
        assertNull(result)
    }
}
```

Run — expect red, `benchmarkDelta` and `BenchmarkDelta` don't exist yet.

- [ ] **Step 2: Create the helper**

```kotlin
package com.stretchdaily.app.core.benchmark

import com.stretchdaily.app.core.model.BenchmarkLog
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * Numeric delta between a benchmark's latest log and the log closest to
 * 6 months prior. `null` when:
 *  - fewer than 2 logs exist;
 *  - [better] is [Better.CATEGORICAL] (nothing to plot numerically);
 *  - either end's [BenchmarkLog.rawValue] is not parseable as a number.
 *
 * [improved] is true when the direction of change matches [better]:
 * ascending benchmarks (HIGHER) want a positive delta, descending
 * (LOWER) want a negative one.
 */
data class BenchmarkDelta(
    val rawDelta: Double,
    val improved: Boolean,
)

fun benchmarkDelta(
    logs: List<BenchmarkLog>,
    nowEpochMillis: Long,
    better: Better,
    zoneId: ZoneId = ZoneId.systemDefault(),
): BenchmarkDelta? {
    if (better == Better.CATEGORICAL) return null
    if (logs.size < 2) return null

    val sorted = logs.sortedBy { it.loggedAt }
    val latest = sorted.last()

    val today = LocalDate.ofInstant(Instant.ofEpochMilli(nowEpochMillis), zoneId)
    val anchor = today.minusMonths(6)

    // Closest log to `anchor`, excluding the latest entry so we never
    // compare a log with itself.
    val candidates = sorted.dropLast(1)
    if (candidates.isEmpty()) return null
    val baseline = candidates.minBy { log ->
        val logDate = LocalDate.ofInstant(Instant.ofEpochMilli(log.loggedAt), zoneId)
        abs(ChronoUnit.DAYS.between(anchor, logDate))
    }

    val latestValue = latest.rawValue.parseNumericOrNull() ?: return null
    val baselineValue = baseline.rawValue.parseNumericOrNull() ?: return null

    val delta = latestValue - baselineValue
    val improved = when (better) {
        Better.HIGHER -> delta > 0
        Better.LOWER -> delta < 0
        Better.CATEGORICAL -> false // unreachable — guarded above.
    }
    return BenchmarkDelta(rawDelta = delta, improved = improved)
}

private fun String.parseNumericOrNull(): Double? =
    trim().replace(',', '.').toDoubleOrNull()
```

Re-run the test suite — all 8 tests should pass.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/core/benchmark/BenchmarkDelta.kt \
        app/src/test/java/com/stretchdaily/app/core/benchmark/BenchmarkDeltaTest.kt
git commit -m "feat(benchmark): add 6-month delta helper for Analytics

Pure helper that picks the log closest to 6 months prior and subtracts
it from the latest reading. Direction-aware via Better so ascending
and descending benchmarks both score 'improvement' correctly. Returns
null on <2 logs, categorical benchmarks, or non-numeric baselines so
the Analytics card silently omits the chip rather than crashing."
git push
```

---

## Task 3: Add `AnalyticsUiState`

Simple ADT. A `list` of cards + the current filter + a computed `categories` list for the filter pills (derived from the catalog, not hard-coded — resilient to seed changes).

**Files:**
- Create: `app/src/main/java/com/stretchdaily/app/ui/screen/analytics/AnalyticsUiState.kt`

```kotlin
package com.stretchdaily.app.ui.screen.analytics

import com.stretchdaily.app.core.benchmark.BenchmarkDelta
import com.stretchdaily.app.core.benchmark.ProgressSeries
import com.stretchdaily.app.core.model.Benchmark
import com.stretchdaily.app.core.model.Category

/**
 * Per-benchmark row on the Analytics tab. [series] is the full log
 * history fed into [com.stretchdaily.app.ui.components.BigChart].
 * [delta] is null when there aren't enough logs to compute one;
 * [latestLog] null = no logs at all and the card renders the empty-
 * chart state with no right-side value.
 */
data class BenchmarkAnalyticsCardState(
    val benchmark: Benchmark,
    val series: ProgressSeries,
    val latestRawValue: String?,
    val delta: BenchmarkDelta?,
)

/**
 * Screen-wide UI state. [allCategories] is the deduped set of
 * categories represented in the catalog, order-preserved; the "All"
 * pill is rendered by the screen layer, not baked in here.
 * [filter] = null ⇒ "All".
 */
data class AnalyticsUiState(
    val cards: List<BenchmarkAnalyticsCardState> = emptyList(),
    val allCategories: List<Category> = emptyList(),
    val filter: Category? = null,
) {
    val visibleCards: List<BenchmarkAnalyticsCardState>
        get() = filter?.let { c -> cards.filter { it.benchmark.category == c } } ?: cards
}
```

- [ ] **Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/ui/screen/analytics/AnalyticsUiState.kt
git commit -m "feat(ui/analytics): add AnalyticsUiState and per-card state"
git push
```

---

## Task 4: Build `AnalyticsViewModel` with TDD

**Files:**
- Create: `app/src/main/java/com/stretchdaily/app/ui/screen/analytics/AnalyticsViewModel.kt`
- Create test: `app/src/test/java/com/stretchdaily/app/ui/screen/analytics/AnalyticsViewModelTest.kt`

### Behaviour contract

- On init, subscribes to `BenchmarkRepository.observeAllBenchmarks()` + per-benchmark `observeLogsFor(id)` (fanned out via `flatMapLatest`).
- `AnalyticsUiState.cards` is one entry per benchmark in catalog order, regardless of whether it has logs.
- `AnalyticsUiState.allCategories` is the deduped list of `benchmark.category`, preserving catalog order.
- `AnalyticsUiState.filter` starts `null` ("All"); `onFilterChanged(category?)` mutates it.
- Each card's `series` comes from `BenchmarkProgressBuilder.build(logs)`; empty logs → empty series (matches existing contract).
- Each card's `delta` comes from `benchmarkDelta(logs, clock.now(), benchmarkBetter(id))`; `null` when <2 logs.
- `latestRawValue` is `logs.maxByOrNull { loggedAt }?.rawValue`.

### Test scaffold

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.stretchdaily.app.ui.screen.analytics

import com.stretchdaily.app.core.model.Benchmark
import com.stretchdaily.app.core.model.BenchmarkInputType
import com.stretchdaily.app.core.model.BenchmarkLog
import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.core.model.FlexibilityTier
import com.stretchdaily.app.core.util.Clock
import com.stretchdaily.app.data.BenchmarkRepository
import io.mockk.coEvery
import io.mockk.mockk
import java.time.LocalDate
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModelTest {

    private val zone: ZoneId = ZoneId.of("UTC")

    private fun midday(year: Int, month: Int, day: Int): Long =
        ZonedDateTime.of(LocalDate.of(year, month, day).atTime(12, 0), zone)
            .toInstant().toEpochMilli()

    private fun numeric(id: String, cat: Category, name: String = id): Benchmark = Benchmark(
        id = id,
        name = name,
        category = cat,
        description = "",
        unit = "°",
        inputType = BenchmarkInputType.NUMERIC,
        tierRanges = emptyMap(),
    )

    private fun log(id: String, raw: String, date: Long): BenchmarkLog = BenchmarkLog(
        benchmarkId = id,
        rawValue = raw,
        resolvedTier = FlexibilityTier.AVERAGE,
        loggedAt = date,
    )

    private val catalog = listOf(
        numeric("BM_CERVICAL_ROTATION", Category.NECK, "Cervical Rotation"),
        numeric("BM_KNEE_TO_WALL", Category.ANKLES, "Knee-to-wall"),
        numeric("BM_SIT_AND_REACH", Category.HAMSTRINGS, "Sit and Reach"),
    )

    private lateinit var repo: BenchmarkRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repo = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun vm(now: Long = midday(2026, 4, 23)): AnalyticsViewModel =
        AnalyticsViewModel(repo, Clock { now }, zoneId = zone)

    @Test
    fun `empty catalog yields empty state`() = runTest {
        coEvery { repo.observeAllBenchmarks() } returns flowOf(emptyList())
        val vm = vm()
        advanceUntilIdle()
        assertEquals(emptyList<BenchmarkAnalyticsCardState>(), vm.state.value.cards)
        assertEquals(emptyList<Category>(), vm.state.value.allCategories)
        assertNull(vm.state.value.filter)
    }

    @Test
    fun `cards are emitted in catalog order regardless of log presence`() = runTest {
        coEvery { repo.observeAllBenchmarks() } returns flowOf(catalog)
        coEvery { repo.observeLogsFor(any()) } returns flowOf(emptyList())
        val vm = vm()
        advanceUntilIdle()
        assertEquals(
            listOf("BM_CERVICAL_ROTATION", "BM_KNEE_TO_WALL", "BM_SIT_AND_REACH"),
            vm.state.value.cards.map { it.benchmark.id },
        )
        // No logs → every latestRawValue is null, every delta null, every series empty.
        vm.state.value.cards.forEach { card ->
            assertNull(card.latestRawValue)
            assertNull(card.delta)
            assertTrue(card.series.points.isEmpty())
        }
    }

    @Test
    fun `allCategories dedupes and preserves catalog order`() = runTest {
        // Add a duplicate neck entry to prove dedup.
        val extended = catalog + numeric("BM_OTHER_NECK", Category.NECK)
        coEvery { repo.observeAllBenchmarks() } returns flowOf(extended)
        coEvery { repo.observeLogsFor(any()) } returns flowOf(emptyList())
        val vm = vm()
        advanceUntilIdle()
        assertEquals(
            listOf(Category.NECK, Category.ANKLES, Category.HAMSTRINGS),
            vm.state.value.allCategories,
        )
    }

    @Test
    fun `latestRawValue is newest log's raw value`() = runTest {
        coEvery { repo.observeAllBenchmarks() } returns flowOf(catalog)
        coEvery { repo.observeLogsFor("BM_CERVICAL_ROTATION") } returns flowOf(
            listOf(
                log("BM_CERVICAL_ROTATION", "60", midday(2025, 10, 23)),
                log("BM_CERVICAL_ROTATION", "72", midday(2026, 4, 10)),
            )
        )
        coEvery { repo.observeLogsFor("BM_KNEE_TO_WALL") } returns flowOf(emptyList())
        coEvery { repo.observeLogsFor("BM_SIT_AND_REACH") } returns flowOf(emptyList())
        val vm = vm()
        advanceUntilIdle()
        val cervical = vm.state.value.cards.first { it.benchmark.id == "BM_CERVICAL_ROTATION" }
        assertEquals("72", cervical.latestRawValue)
    }

    @Test
    fun `delta is populated when logs span 6 months on ascending benchmark`() = runTest {
        coEvery { repo.observeAllBenchmarks() } returns flowOf(catalog)
        coEvery { repo.observeLogsFor("BM_CERVICAL_ROTATION") } returns flowOf(
            listOf(
                log("BM_CERVICAL_ROTATION", "60", midday(2025, 10, 23)),
                log("BM_CERVICAL_ROTATION", "72", midday(2026, 4, 10)),
            )
        )
        coEvery { repo.observeLogsFor("BM_KNEE_TO_WALL") } returns flowOf(emptyList())
        coEvery { repo.observeLogsFor("BM_SIT_AND_REACH") } returns flowOf(emptyList())
        val vm = vm()
        advanceUntilIdle()
        val cervical = vm.state.value.cards.first { it.benchmark.id == "BM_CERVICAL_ROTATION" }
        assertNotNull(cervical.delta)
        assertEquals(12.0, cervical.delta!!.rawDelta, 0.001)
        assertTrue(cervical.delta!!.improved)
    }

    @Test
    fun `delta flips improved flag for descending benchmark`() = runTest {
        coEvery { repo.observeAllBenchmarks() } returns flowOf(catalog)
        coEvery { repo.observeLogsFor("BM_CERVICAL_ROTATION") } returns flowOf(emptyList())
        coEvery { repo.observeLogsFor("BM_KNEE_TO_WALL") } returns flowOf(emptyList())
        coEvery { repo.observeLogsFor("BM_SIT_AND_REACH") } returns flowOf(
            // "Lower value is better" — +15 → -5 is an improvement.
            listOf(
                log("BM_SIT_AND_REACH", "15", midday(2025, 10, 23)),
                log("BM_SIT_AND_REACH", "-5", midday(2026, 4, 10)),
            )
        )
        val vm = vm()
        advanceUntilIdle()
        val sar = vm.state.value.cards.first { it.benchmark.id == "BM_SIT_AND_REACH" }
        assertNotNull(sar.delta)
        assertEquals(-20.0, sar.delta!!.rawDelta, 0.001)
        assertTrue(sar.delta!!.improved)
    }

    @Test
    fun `series emits a two-point series for two logs`() = runTest {
        coEvery { repo.observeAllBenchmarks() } returns flowOf(catalog)
        coEvery { repo.observeLogsFor("BM_CERVICAL_ROTATION") } returns flowOf(
            listOf(
                log("BM_CERVICAL_ROTATION", "60", midday(2025, 10, 23)),
                log("BM_CERVICAL_ROTATION", "72", midday(2026, 4, 10)),
            )
        )
        coEvery { repo.observeLogsFor("BM_KNEE_TO_WALL") } returns flowOf(emptyList())
        coEvery { repo.observeLogsFor("BM_SIT_AND_REACH") } returns flowOf(emptyList())
        val vm = vm()
        advanceUntilIdle()
        val cervical = vm.state.value.cards.first { it.benchmark.id == "BM_CERVICAL_ROTATION" }
        assertEquals(2, cervical.series.points.size)
    }

    @Test
    fun `onFilterChanged sets filter`() = runTest {
        coEvery { repo.observeAllBenchmarks() } returns flowOf(catalog)
        coEvery { repo.observeLogsFor(any()) } returns flowOf(emptyList())
        val vm = vm()
        advanceUntilIdle()
        vm.onFilterChanged(Category.HAMSTRINGS)
        advanceUntilIdle()
        assertEquals(Category.HAMSTRINGS, vm.state.value.filter)
        assertEquals(
            listOf("BM_SIT_AND_REACH"),
            vm.state.value.visibleCards.map { it.benchmark.id },
        )
    }

    @Test
    fun `onFilterChanged null restores All view`() = runTest {
        coEvery { repo.observeAllBenchmarks() } returns flowOf(catalog)
        coEvery { repo.observeLogsFor(any()) } returns flowOf(emptyList())
        val vm = vm()
        advanceUntilIdle()
        vm.onFilterChanged(Category.NECK)
        advanceUntilIdle()
        assertEquals(1, vm.state.value.visibleCards.size)
        vm.onFilterChanged(null)
        advanceUntilIdle()
        assertEquals(3, vm.state.value.visibleCards.size)
        assertNull(vm.state.value.filter)
    }

    @Test
    fun `live log emission updates the affected card without restart`() = runTest {
        val logsFlow = MutableStateFlow<List<BenchmarkLog>>(emptyList())
        coEvery { repo.observeAllBenchmarks() } returns flowOf(catalog)
        coEvery { repo.observeLogsFor("BM_CERVICAL_ROTATION") } returns logsFlow
        coEvery { repo.observeLogsFor("BM_KNEE_TO_WALL") } returns flowOf(emptyList())
        coEvery { repo.observeLogsFor("BM_SIT_AND_REACH") } returns flowOf(emptyList())
        val vm = vm()
        advanceUntilIdle()
        assertNull(vm.state.value.cards.first { it.benchmark.id == "BM_CERVICAL_ROTATION" }.latestRawValue)

        logsFlow.value = listOf(log("BM_CERVICAL_ROTATION", "70", midday(2026, 4, 23)))
        advanceUntilIdle()
        assertEquals(
            "70",
            vm.state.value.cards.first { it.benchmark.id == "BM_CERVICAL_ROTATION" }.latestRawValue,
        )
    }
}
```

Run — every test fails (class doesn't exist yet).

- [ ] **Step 2: Create the ViewModel**

```kotlin
package com.stretchdaily.app.ui.screen.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stretchdaily.app.core.benchmark.BenchmarkProgressBuilder
import com.stretchdaily.app.core.benchmark.ProgressSeries
import com.stretchdaily.app.core.benchmark.benchmarkBetter
import com.stretchdaily.app.core.benchmark.benchmarkDelta
import com.stretchdaily.app.core.model.Benchmark
import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.core.util.Clock
import com.stretchdaily.app.data.BenchmarkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

/**
 * Backs the Progress / Analytics tab. Emits one
 * [BenchmarkAnalyticsCardState] per benchmark in the catalog, each
 * carrying a full [ProgressSeries] for the [com.stretchdaily.app.ui.components.BigChart]
 * and an optional 6-month delta for the right-hand chip.
 *
 * The fan-out pattern is the same one used by R5's `BenchmarkLogViewModel`
 * — [BenchmarkRepository.observeAllBenchmarks] maps to a parallel list of
 * `observeLogsFor(id)` flows combined with `combine(*flows)`. `flatMapLatest`
 * ensures that if the catalog itself changes (rare, only on Import +
 * delete-all + re-seed) the subscription re-fans.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val benchmarkRepository: BenchmarkRepository,
    private val clock: Clock,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    private val _filter = MutableStateFlow<Category?>(null)

    val state: StateFlow<AnalyticsUiState> = benchmarkRepository.observeAllBenchmarks()
        .flatMapLatest { benchmarks ->
            if (benchmarks.isEmpty()) {
                flowOf(AnalyticsUiState())
            } else {
                val logFlows = benchmarks.map { benchmarkRepository.observeLogsFor(it.id) }
                combine(logFlows) { perBenchmarkLogs ->
                    val cards = benchmarks.mapIndexed { i, bm ->
                        val logs = perBenchmarkLogs[i]
                        BenchmarkAnalyticsCardState(
                            benchmark = bm,
                            series = BenchmarkProgressBuilder.build(logs),
                            latestRawValue = logs.maxByOrNull { it.loggedAt }?.rawValue,
                            delta = benchmarkDelta(
                                logs = logs,
                                nowEpochMillis = clock.now(),
                                better = benchmarkBetter(bm.id),
                                zoneId = zoneId,
                            ),
                        )
                    }
                    AnalyticsUiState(
                        cards = cards,
                        allCategories = benchmarks.map { it.category }.distinct(),
                    )
                }
            }
        }
        .combine(_filter) { state, filter -> state.copy(filter = filter) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AnalyticsUiState(),
        )

    fun onFilterChanged(filter: Category?) {
        _filter.value = filter
    }
}
```

- [ ] **Step 3: Verify**

Run the 10-test suite. All should pass. Build via Android Studio → Make Project. Clean.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/ui/screen/analytics/AnalyticsViewModel.kt \
        app/src/test/java/com/stretchdaily/app/ui/screen/analytics/AnalyticsViewModelTest.kt
git commit -m "feat(ui/analytics): add AnalyticsViewModel with per-benchmark fan-out

combine(observeAllBenchmarks, per-benchmark observeLogsFor) + _filter
streams. Cards stay in catalog order; 'All' categories deduped in
catalog order. Delta uses the new benchmarkDelta helper with the
Clock-provided now. 10 tests cover fan-out, filter, delta direction,
and live-update behaviour."
git push
```

---

## Task 5: Build `AnalyticsScreen` + `BenchmarkAnalyticsCard`

**Files:**
- Create: `app/src/main/java/com/stretchdaily/app/ui/screen/analytics/AnalyticsScreen.kt`
- Create: `app/src/main/java/com/stretchdaily/app/ui/screen/analytics/BenchmarkAnalyticsCard.kt`

Handoff reference: [`screens.jsx` → `AnalyticsScreen` + `BenchmarkChartCard`](../../../docs/design_handoff_stretch_daily_v3/reference/screens.jsx). Pixel targets per [`reference/index.html`](../../../docs/design_handoff_stretch_daily_v3/reference/index.html).

### Layout structure

```
LazyColumn
├── HeroHeader           "PROGRESS" eyebrow + "Six months in." title + subtitle
├── CategoryFilterRow    LazyRow of MonoCaps pills, "ALL" first
└── BenchmarkAnalyticsCard × N   (filtered)
```

- [ ] **Step 1: Create `AnalyticsScreen.kt`**

```kotlin
package com.stretchdaily.app.ui.screen.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material3.Text
import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.ui.components.MonoCaps
import com.stretchdaily.app.ui.components.MonoCapsSize
import com.stretchdaily.app.ui.theme.Theme

@Composable
fun AnalyticsScreen(
    contentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: AnalyticsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.bg)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(
            top = 6.dp,
            bottom = contentPadding.calculateBottomPadding() + 100.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            AnalyticsHeader()
        }
        item {
            CategoryFilterRow(
                all = state.allCategories,
                selected = state.filter,
                onSelect = viewModel::onFilterChanged,
            )
        }
        items(
            items = state.visibleCards,
            key = { it.benchmark.id },
        ) { card ->
            BenchmarkAnalyticsCard(state = card)
        }
    }
}

@Composable
private fun AnalyticsHeader() {
    Column(modifier = Modifier.padding(top = 14.dp, bottom = 0.dp)) {
        MonoCaps(
            text = "Progress",
            size = MonoCapsSize.Small,
            color = Theme.colors.ink3,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Six months in.",
            modifier = Modifier.semantics { heading() },
            color = Theme.colors.ink,
            fontSize = 30.sp,
            fontWeight = FontWeight.W500,
            fontFamily = Theme.typo.display,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Each chart is one benchmark. The colored strip marks its category.",
            color = Theme.colors.ink2,
            fontSize = 13.sp,
            fontFamily = Theme.typo.body,
        )
    }
}

@Composable
private fun CategoryFilterRow(
    all: List<Category>,
    selected: Category?,
    onSelect: (Category?) -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item(key = "all") {
            FilterPill(
                label = "All",
                selected = selected == null,
                onClick = { onSelect(null) },
            )
        }
        items(items = all, key = { it.name }) { category ->
            FilterPill(
                label = category.name.replace('_', ' '),
                selected = selected == category,
                onClick = { onSelect(category) },
            )
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (selected) Theme.colors.accent else Theme.colors.bg2
    val fg = if (selected) Theme.colors.accentInk else Theme.colors.ink2
    Row(
        modifier = Modifier
            .background(bg, RoundedCornerShape(Theme.dims.radiusPill))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonoCaps(
            text = label,
            size = MonoCapsSize.Small,
            color = fg,
        )
    }
}
```

> **Note on filter-pill label:** the handoff renders category names in title case ("All", "Ankles") — we use `category.name.replace('_', ' ')` to at least prettify `VERY_FLEXIBLE` → `VERY FLEXIBLE`. If R1's `Category` enum already carries a `displayName` field, prefer that. Verify by opening `core/model/Category.kt`.

- [ ] **Step 2: Create `BenchmarkAnalyticsCard.kt`**

```kotlin
package com.stretchdaily.app.ui.screen.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.stretchdaily.app.ui.components.BigChart
import com.stretchdaily.app.ui.components.MonoCaps
import com.stretchdaily.app.ui.components.MonoCapsSize
import com.stretchdaily.app.ui.theme.Theme
import com.stretchdaily.app.ui.theme.tint
import kotlin.math.abs
import kotlin.math.roundToLong

@Composable
fun BenchmarkAnalyticsCard(state: BenchmarkAnalyticsCardState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Theme.dims.radiusMd))
            .background(Theme.colors.surface)
            .border(1.dp, Theme.colors.line, RoundedCornerShape(Theme.dims.radiusMd)),
    ) {
        // 3 dp category tint strip.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(state.benchmark.category.tint()),
        )
        Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 4.dp)) {
            CardHeaderRow(state = state)
            Spacer(modifier = Modifier.height(8.dp))
            // Pull the chart 6dp into the card edges so the polyline fills.
            Box(modifier = Modifier.padding(start = 0.dp, end = 0.dp)) {
                BigChart(series = state.series)
            }
        }
    }
}

@Composable
private fun CardHeaderRow(state: BenchmarkAnalyticsCardState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        // Left — eyebrow + name.
        Column(modifier = Modifier.weight(1f)) {
            MonoCaps(
                text = state.benchmark.category.name.replace('_', ' '),
                size = MonoCapsSize.Small,
                color = Theme.colors.ink3,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = state.benchmark.name,
                color = Theme.colors.ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.W600,
                fontFamily = Theme.typo.body,
            )
        }
        // Right — latest value + delta chip (if any).
        if (state.latestRawValue != null) {
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = state.latestRawValue,
                        color = Theme.colors.ink,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.W500,
                        fontFamily = Theme.typo.display,
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 1.dp))
                    Text(
                        text = state.benchmark.unit,
                        color = Theme.colors.ink3,
                        fontSize = 11.sp,
                        fontFamily = Theme.typo.body,
                    )
                }
                state.delta?.let { delta ->
                    Spacer(modifier = Modifier.height(3.dp))
                    DeltaChip(delta, unit = state.benchmark.unit)
                }
            }
        }
    }
}

@Composable
private fun DeltaChip(delta: com.stretchdaily.app.core.benchmark.BenchmarkDelta, unit: String) {
    val sign = if (delta.rawDelta > 0) "+" else ""
    // Round to one decimal place.
    val pretty = (delta.rawDelta * 10).roundToLong() / 10.0
    val color = if (delta.improved) Theme.colors.accent else Theme.colors.warn
    MonoCaps(
        text = "$sign$pretty $unit · 6mo",
        size = MonoCapsSize.Small,
        color = color,
    )
}
```

- [ ] **Step 3: Verify**

Build via Android Studio → Make Project. Clean compile. The screen is wired into the NavHost in the next task; until then it's unreachable.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/ui/screen/analytics/AnalyticsScreen.kt \
        app/src/main/java/com/stretchdaily/app/ui/screen/analytics/BenchmarkAnalyticsCard.kt
git commit -m "feat(ui/analytics): add AnalyticsScreen with per-benchmark BigChart cards

Hero header + horizontally scrolling category filter pills (All + N)
+ LazyColumn of cards. Each card has a 3dp category tint strip, an
eyebrow-label + name, right-side latest value with unit, and an
accent/warn delta chip when a 6-month comparison is available."
git push
```

---

## Task 6: Wire Analytics into NavHost

Replace the R1 placeholder for `"progress"` with the real screen.

**Files:**
- Edit: `app/src/main/java/com/stretchdaily/app/ui/navigation/StretchDailyNavHost.kt`

- [ ] **Step 1: Swap the composable**

Locate the `composable(ROUTE_PROGRESS) { PlaceholderScreen("Progress") }` line (or equivalent; R1 may have used a different stub). Replace:

```kotlin
composable(ROUTE_PROGRESS) {
    AnalyticsScreen(contentPadding = innerPadding)
}
```

Add the import:

```kotlin
import com.stretchdaily.app.ui.screen.analytics.AnalyticsScreen
```

- [ ] **Step 2: Verify**

Build → Make Project. Launch. Tap the Progress tab. Expected: the hero header renders, filter pills row renders, cards render for all 10 benchmarks (with empty charts on a fresh install).

Tap a filter pill — visible card set updates.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/ui/navigation/StretchDailyNavHost.kt
git commit -m "feat(nav): wire AnalyticsScreen to the Progress tab"
git push
```

---

## Task 7: Extend `SettingsViewModel` — library stats + banner toggle + UiState

The R1 scaffold deleted `ui/settings/` wholesale. We reintroduce the ViewModel from scratch — reusing the **patterns** of the legacy Phase 7 `SettingsViewModel` (SAF launcher + status state machine + delete-dialog visibility) but with the R6-specific additions: a reactive `libraryStats` flow and the `benchmarkBannerEnabled` passthrough.

**Files:**
- Create: `app/src/main/java/com/stretchdaily/app/ui/screen/settings/SettingsUiState.kt`
- Create: `app/src/main/java/com/stretchdaily/app/ui/screen/settings/SettingsViewModel.kt`
- Create test: `app/src/test/java/com/stretchdaily/app/ui/screen/settings/SettingsViewModelTest.kt`

- [ ] **Step 1: Write `SettingsUiState.kt`**

```kotlin
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
```

- [ ] **Step 2: Write the failing ViewModel tests**

```kotlin
package com.stretchdaily.app.ui.screen.settings

import android.content.Context
import com.stretchdaily.app.core.database.dao.ExerciseDao
import com.stretchdaily.app.core.datastore.SettingsDataStore
import com.stretchdaily.app.core.model.Benchmark
import com.stretchdaily.app.core.model.BenchmarkInputType
import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.core.model.Exercise
import com.stretchdaily.app.data.BenchmarkRepository
import com.stretchdaily.app.data.DataPortRepository
import com.stretchdaily.app.data.SessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
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
class SettingsViewModelTest {

    private lateinit var appContext: Context
    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var dataPortRepository: DataPortRepository
    private lateinit var benchmarkRepository: BenchmarkRepository
    private lateinit var sessionRepository: SessionRepository
    private lateinit var exerciseDao: ExerciseDao

    private val audioFlow = MutableStateFlow(true)
    private val bannerFlow = MutableStateFlow(false)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        appContext = mockk(relaxed = true)
        settingsDataStore = mockk(relaxed = true)
        dataPortRepository = mockk(relaxed = true)
        benchmarkRepository = mockk(relaxed = true)
        sessionRepository = mockk(relaxed = true)
        exerciseDao = mockk(relaxed = true)

        coEvery { settingsDataStore.audioCuesEnabled } returns audioFlow
        coEvery { settingsDataStore.benchmarkBannerEnabled } returns bannerFlow
        coEvery { exerciseDao.observeAll() } returns flowOf(exercises(46, categories = 7))
        coEvery { benchmarkRepository.observeAllBenchmarks() } returns flowOf(benchmarks(10))
        coEvery { sessionRepository.totalsFlow } returns flowOf(SessionRepository.Totals(sessions = 12, totalMinutes = 120))
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private fun vm(): SettingsViewModel = SettingsViewModel(
        appContext = appContext,
        settingsDataStore = settingsDataStore,
        dataPortRepository = dataPortRepository,
        benchmarkRepository = benchmarkRepository,
        sessionRepository = sessionRepository,
        exerciseDao = exerciseDao,
    )

    @Test
    fun `initial state reflects flows`() = runTest {
        val vm = vm()
        advanceUntilIdle()
        val s = vm.state.value
        assertTrue(s.audioCuesEnabled)
        assertFalse(s.benchmarkBannerEnabled)
        assertEquals(46, s.libraryStats.exerciseCount)
        assertEquals(7, s.libraryStats.categoryCount)
        assertEquals(10, s.libraryStats.benchmarkCount)
        assertEquals(12, s.libraryStats.sessionsLogged)
        assertEquals(SettingsStatus.Idle, s.status)
    }

    @Test
    fun `setAudioCuesEnabled delegates to DataStore`() = runTest {
        val vm = vm()
        advanceUntilIdle()
        vm.setAudioCuesEnabled(false)
        advanceUntilIdle()
        coVerify { settingsDataStore.setAudioCuesEnabled(false) }
    }

    @Test
    fun `setBenchmarkBannerEnabled delegates to DataStore`() = runTest {
        val vm = vm()
        advanceUntilIdle()
        vm.setBenchmarkBannerEnabled(true)
        advanceUntilIdle()
        coVerify { settingsDataStore.setBenchmarkBannerEnabled(true) }
    }

    @Test
    fun `banner flow emission updates state`() = runTest {
        val vm = vm()
        advanceUntilIdle()
        bannerFlow.value = true
        advanceUntilIdle()
        assertTrue(vm.state.value.benchmarkBannerEnabled)
    }

    @Test
    fun `categoryCount dedupes by category`() = runTest {
        coEvery { exerciseDao.observeAll() } returns flowOf(
            exercises(count = 6, categories = 2), // 3 per category, 2 distinct categories
        )
        val vm = vm()
        advanceUntilIdle()
        assertEquals(2, vm.state.value.libraryStats.categoryCount)
    }

    @Test
    fun `showDeleteDialog toggles state`() = runTest {
        val vm = vm()
        advanceUntilIdle()
        assertFalse(vm.state.value.showDeleteDialog)
        vm.showDeleteDialog()
        advanceUntilIdle()
        assertTrue(vm.state.value.showDeleteDialog)
        vm.dismissDeleteDialog()
        advanceUntilIdle()
        assertFalse(vm.state.value.showDeleteDialog)
    }

    @Test
    fun `deleteAllData dismisses dialog and runs deletion`() = runTest {
        val vm = vm()
        advanceUntilIdle()
        vm.showDeleteDialog()
        advanceUntilIdle()
        vm.deleteAllData()
        advanceUntilIdle()
        assertFalse(vm.state.value.showDeleteDialog)
        coVerify { dataPortRepository.deleteAll() }
        // After success, status is Success.
        assertTrue(vm.state.value.status is SettingsStatus.Success)
    }

    @Test
    fun `consumeStatus resets to Idle`() = runTest {
        val vm = vm()
        advanceUntilIdle()
        vm.deleteAllData()
        advanceUntilIdle()
        assertTrue(vm.state.value.status is SettingsStatus.Success)
        vm.consumeStatus()
        advanceUntilIdle()
        assertEquals(SettingsStatus.Idle, vm.state.value.status)
    }

    // ---------- fixtures ----------

    private fun exercises(count: Int, categories: Int): List<Exercise> {
        val cats = Category.values().take(categories)
        return List(count) { i ->
            Exercise(
                id = "EX$i",
                name = "Exercise $i",
                category = cats[i % categories],
                description = "",
                timingType = com.stretchdaily.app.core.model.TimingType.TIMED,
                unilateral = false,
                totalTime = 30,
                cues = emptyList(),
                lastPerformed = null,
            )
        }
    }

    private fun benchmarks(count: Int): List<Benchmark> = List(count) { i ->
        Benchmark(
            id = "BM$i",
            name = "B $i",
            category = Category.NECK,
            description = "",
            unit = "°",
            inputType = BenchmarkInputType.NUMERIC,
            tierRanges = emptyMap(),
        )
    }
}
```

> **Note on `Exercise` fixture:** the field names in the fixture must match the existing `core/model/Exercise.kt`. Open that file once and adjust the constructor call if R1 or prior phases renamed fields — this plan was written against the post-R5 shape.

Run — every test fails (the class doesn't exist yet).

- [ ] **Step 3: Write `SettingsViewModel.kt`**

```kotlin
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

    // ---------- preference mutations ----------

    fun setAudioCuesEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setAudioCuesEnabled(enabled) }
    }

    fun setBenchmarkBannerEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setBenchmarkBannerEnabled(enabled) }
    }

    // ---------- export / import ----------

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

    // ---------- delete-all flow ----------

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

// Tiny extension so the `update { … }` calls above read cleanly.
private inline fun <T> MutableStateFlow<T>.update(transform: (T) -> T) {
    value = transform(value)
}
```

> **Note on `combine` with 6 arguments:** `kotlinx.coroutines.flow.combine` has typed overloads up to 5 flows; the 6-flow variant returns `Array<Any?>`. The cast block at the top of the lambda is mandatory. If you prefer typed combines, split into two nested `combine(a, b, c).combine(d, e, f) { … }` calls — equivalent, slightly more readable.

- [ ] **Step 4: Verify**

Run the test suite. All 8 tests should pass. Build via Android Studio. Clean.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/ui/screen/settings/SettingsUiState.kt \
        app/src/main/java/com/stretchdaily/app/ui/screen/settings/SettingsViewModel.kt \
        app/src/test/java/com/stretchdaily/app/ui/screen/settings/SettingsViewModelTest.kt
git commit -m "feat(ui/settings): add SettingsViewModel with library stats + banner toggle

combine() of audio pref + banner pref + exercise catalog + benchmark
catalog + session totals + ephemeral dialog/status state. LibraryStats
counts exercises, distinct categories, benchmarks, and sessions logged
reactively. Preserves the Phase 7 status state machine for export /
import / delete flows."
git push
```

---

## Task 8: Build `SettingsScreen`

**Files:**
- Create: `app/src/main/java/com/stretchdaily/app/ui/screen/settings/SettingsScreen.kt`

Handoff reference: [`screens.jsx` → `SettingsScreen`, `SettingGroup`, `SettingToggle`, `SettingRow`, `ReadonlyRow`](../../../docs/design_handoff_stretch_daily_v3/reference/screens.jsx#L806).

- [ ] **Step 1: Implement the screen**

```kotlin
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.hilt.navigation.compose.hiltViewModel
import com.stretchdaily.app.ui.components.Icon
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
            SettingsStatus.Idle -> { /* no-op */ }
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

            // ─── Preferences group ───
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

            // ─── Your data group ───
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

            // ─── Library group ───
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
        Icon(
            name = iconName,
            size = 18.dp,
            tint = Theme.colors.ink2,
            contentDescription = null, // decorative; toggle carries the name.
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
    // 38×22 dp pill + 18 dp thumb — matches handoff reference.
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
        Icon(
            name = iconName,
            size = 18.dp,
            tint = if (destructive) Theme.colors.warn else Theme.colors.ink2,
            contentDescription = null,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.W600, fontFamily = Theme.typo.body)
            Spacer(modifier = Modifier.height(1.dp))
            Text(text = description, color = Theme.colors.ink3, fontSize = 11.sp, fontFamily = Theme.typo.body)
        }
        Icon(
            name = IconName.Chevron,
            size = 14.dp,
            tint = Theme.colors.ink3,
            contentDescription = "Open",
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
```

> **Note on `IconName` enum values:** this plan assumes R2 landed `IconName.Sound`, `.Sparkle`, `.Download`, `.Upload`, `.Trash`, `.Chevron`. If R2 used different names (e.g. `.Bell` for the banner toggle), pick the closest semantic match. Visual-weight wise, `Sparkle` fits the "benchmark day reminder" row because R5 already uses it on the Log CTA card.

- [ ] **Step 2: Verify**

Build → Make Project. Clean.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/ui/screen/settings/SettingsScreen.kt
git commit -m "feat(ui/settings): add SettingsScreen with Preferences/Data/Library groups

Three grouped cards with mono-caps labels, 38x22 dp toggle primitive
inlined (matches handoff visual), readonly library rows, centered
mono-caps footer. SAF launchers hooked up for export/import; delete-
all routes through AlertDialog backed by the ViewModel's ephemeral
state."
git push
```

---

## Task 9: Wire Settings into NavHost

Replace the R1 placeholder for `"settings"` with the real screen.

**Files:**
- Edit: `app/src/main/java/com/stretchdaily/app/ui/navigation/StretchDailyNavHost.kt`

- [ ] **Step 1: Swap the composable**

```kotlin
composable(ROUTE_SETTINGS) {
    SettingsScreen(contentPadding = innerPadding)
}
```

Add the import:

```kotlin
import com.stretchdaily.app.ui.screen.settings.SettingsScreen
```

- [ ] **Step 2: Verify**

Build → Make Project. Launch. Tap Settings tab. Expected: all three groups render; audio toggle reflects persisted value; banner toggle can flip on/off (verify by flipping → reopen app → still on); export launches SAF; delete shows the confirmation dialog.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/ui/navigation/StretchDailyNavHost.kt
git commit -m "feat(nav): wire SettingsScreen to the Settings tab"
git push
```

---

## Task 10: Update launcher icon colours

Per spec §9 + Q-sub "App icon" decision: re-tint the adaptive icon to a sage background + cream figure. One colour change in `colors.xml` + five fill/stroke colour swaps in the vector XML.

**Files:**
- Edit: `app/src/main/res/values/colors.xml`
- Edit: `app/src/main/res/drawable/ic_launcher_foreground.xml`

- [ ] **Step 1: Swap the adaptive background colour**

Replace `app/src/main/res/values/colors.xml` with:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="ic_launcher_background">#5c7a4a</color>
</resources>
```

- [ ] **Step 2: Re-tint the foreground vector paths**

In `app/src/main/res/drawable/ic_launcher_foreground.xml`, replace every `#FF8C00` with `#f5f3ea`. There are five occurrences (head fill + four stroke paths).

The final file:

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <!-- Person doing a stretch / lunge pose, centered in the safe zone (24-84) -->
    <!-- Head -->
    <path
        android:fillColor="#f5f3ea"
        android:pathData="M54,33 m-4,0 a4,4 0,1 1,8 0 a4,4 0,1 1,-8 0" />
    <!-- Torso -->
    <path
        android:strokeColor="#f5f3ea"
        android:strokeWidth="3"
        android:strokeLineCap="round"
        android:fillColor="#00000000"
        android:pathData="M54,37 L54,56" />
    <!-- Arms stretched wide -->
    <path
        android:strokeColor="#f5f3ea"
        android:strokeWidth="3"
        android:strokeLineCap="round"
        android:fillColor="#00000000"
        android:pathData="M38,42 L54,45 L70,38" />
    <!-- Left leg (lunging forward) -->
    <path
        android:strokeColor="#f5f3ea"
        android:strokeWidth="3"
        android:strokeLineCap="round"
        android:fillColor="#00000000"
        android:pathData="M54,56 L42,70 L38,75" />
    <!-- Right leg (extended back) -->
    <path
        android:strokeColor="#f5f3ea"
        android:strokeWidth="3"
        android:strokeLineCap="round"
        android:fillColor="#00000000"
        android:pathData="M54,56 L68,68 L74,72" />
</vector>
```

- [ ] **Step 3: Verify**

Build → Make Project. Launch. Expected: the home-screen launcher tile is now sage green with a cream stretching figure. Android may still cache the old icon; uninstall the app then reinstall to force a fresh cache if the old orange tile persists.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/values/colors.xml \
        app/src/main/res/drawable/ic_launcher_foreground.xml
git commit -m "feat(icon): retint launcher to sage + cream

Adaptive icon background flips from #0D0D0D to #5c7a4a sage accent;
foreground stretching-figure paths flip from #FF8C00 orange to
#f5f3ea cream. Matches Sage redesign brand per spec §9."
git push
```

---

## Task 11: Update splash theme

Flip the splash screen to cream bg + sage figure to eliminate the dark→cream flash at launch.

**Files:**
- Edit: `app/src/main/res/values/themes.xml`
- Create: `app/src/main/res/drawable/ic_splash_foreground.xml` (sage-tinted variant)

- [ ] **Step 1: Create a sage-tinted figure drawable for the splash**

The launcher icon now uses cream for the figure, which would vanish against the cream splash bg. Create a sibling drawable with sage `#5c7a4a` paths:

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <!-- Same paths as ic_launcher_foreground, tinted sage for cream-bg splash -->
    <path
        android:fillColor="#5c7a4a"
        android:pathData="M54,33 m-4,0 a4,4 0,1 1,8 0 a4,4 0,1 1,-8 0" />
    <path
        android:strokeColor="#5c7a4a"
        android:strokeWidth="3"
        android:strokeLineCap="round"
        android:fillColor="#00000000"
        android:pathData="M54,37 L54,56" />
    <path
        android:strokeColor="#5c7a4a"
        android:strokeWidth="3"
        android:strokeLineCap="round"
        android:fillColor="#00000000"
        android:pathData="M38,42 L54,45 L70,38" />
    <path
        android:strokeColor="#5c7a4a"
        android:strokeWidth="3"
        android:strokeLineCap="round"
        android:fillColor="#00000000"
        android:pathData="M54,56 L42,70 L38,75" />
    <path
        android:strokeColor="#5c7a4a"
        android:strokeWidth="3"
        android:strokeLineCap="round"
        android:fillColor="#00000000"
        android:pathData="M54,56 L68,68 L74,72" />
</vector>
```

- [ ] **Step 2: Update themes.xml**

Replace with:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.StretchDaily" parent="android:Theme.Material.NoActionBar">
        <item name="android:statusBarColor">#fbf8f0</item>
        <item name="android:navigationBarColor">#fbf8f0</item>
        <item name="android:windowBackground">#fbf8f0</item>
        <item name="android:windowLightStatusBar">true</item>
        <item name="android:windowLightNavigationBar">true</item>
    </style>

    <style name="Theme.StretchDaily.Splash" parent="Theme.SplashScreen">
        <item name="windowSplashScreenBackground">#fbf8f0</item>
        <item name="windowSplashScreenAnimatedIcon">@drawable/ic_splash_foreground</item>
        <item name="windowSplashScreenIconBackgroundColor">#fbf8f0</item>
        <item name="postSplashScreenTheme">@style/Theme.StretchDaily</item>
    </style>
</resources>
```

> `windowLightStatusBar` + `windowLightNavigationBar` are needed because the system bars are now cream (light) — without them the OS clock / battery icons are invisible.

- [ ] **Step 3: Verify**

Uninstall the app, reinstall. Cold-launch. Expected: splash is cream with a sage stretching figure; app foreground shows cream bg immediately, no dark flash.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/values/themes.xml \
        app/src/main/res/drawable/ic_splash_foreground.xml
git commit -m "feat(splash): cream background + sage figure

Theme.StretchDaily.Splash moves to #fbf8f0 with a sage-tinted figure
drawable (ic_splash_foreground.xml). Theme.StretchDaily now also
cream with light system-bar icons. Eliminates the dark-to-cream
flash at app launch (spec §9)."
git push
```

---

## Task 12: Accessibility audit pass

Cross-screen audit applied to every screen built or touched in R3–R6. Preserves the Phase 8 discipline (see CLAUDE.md §2): every interactive element has a non-null `contentDescription`; every screen title carries `.semantics { heading() }`; touch targets are ≥ 44 dp.

- [ ] **Step 1: Grep for `contentDescription = null` on interactive primitives**

```bash
# Command is advisory — the R1/R2 primitives already enforce the rule
# via the Icon composable's contract. But double-check every call site
# in the new screens and carousel.
```

Use Grep tool in IDE / editor:
- `contentDescription = null` across `ui/screen/**/*.kt`
- Confirm every occurrence is either on a decorative `Icon` inside a `Row` whose parent carries the interactive semantic (e.g. the `Settings` toggle row — the icon is decorative; the toggle itself is the accessibility target), OR on a pure decorative element (leading icon inside a `SettingRow` where the whole row is the tap target).

Where an interactive element's `contentDescription` is `null`, either:
1. Add one, OR
2. Move the interactive semantic up to a `Modifier.semantics { contentDescription = "…" }` on the ancestor that owns the tap handler.

- [ ] **Step 2: Heading annotations on screen titles**

Every screen's primary `<h1>`-equivalent `Text` should carry `Modifier.semantics { heading() }`. Check:

- `DashboardScreen` → hero title
- `SessionOverviewScreen` → session title
- `SessionCompleteScreen` → "Nice work"
- `BenchmarkLogScreen` → "Benchmark log."
- `AnalyticsScreen` → "Six months in." ✅ (added in Task 5)
- `SettingsScreen` → "Your app, offline." ✅ (added in Task 8)

Add missing ones directly in each screen file. No commits yet — batch into one accessibility commit.

- [ ] **Step 3: Touch-target audit**

Target minimum: 44 dp × 44 dp per Material guidance. Known risk spots:
- `CircleButton(size = small)` variant — if R2 produced a 32 dp size, wrap it in a `Modifier.size(44.dp)` interactive area even if the visual stays 32 dp. Pattern:

```kotlin
Box(
    modifier = Modifier
        .size(44.dp)
        .clickable(onClick = onClick),
    contentAlignment = Alignment.Center,
) {
    CircleButton(size = 32.dp, icon = IconName.Close, onClick = {})  // visual only
}
```

- Filter pills on Analytics — 7 dp vertical padding is ≥ 40 dp total row height. ✅
- `CatChip` — 6 dp vertical padding on 10 sp text ≈ 28 dp total. If these are interactive in any surface, wrap similarly. Currently `CatChip` is used only as a label in the benchmark log rows — not interactive, ok.

- [ ] **Step 4: TalkBack smoke test**

With TalkBack enabled (Settings → Accessibility → TalkBack):
1. Open Dashboard. Focus flows through: date eyebrow → streak → hero title → (banner if enabled) → session card → week strip → KPI grid → bottom nav.
2. Open Session overview. Focus flows through: session title → exercise rows → "Begin session" CTA → back arrow.
3. Session player. Announces exercise name + timer on each transition. Pause / resume / skip have distinct announcements.
4. Benchmark log. Each row's expand toggle announces "Collapsed / Expanded, benchmark name".
5. Carousel. Header announces "Step X of 10".
6. Analytics. Filter pills announce "All" / "Neck" / etc. as radio-like selections.
7. Settings. Each toggle announces state + label.

If any step fails, fix in-place before committing.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/ui/screen
git commit -m "chore(a11y): cross-screen content-description + heading audit

Every interactive element has a meaningful contentDescription;
every screen title carries .semantics { heading() } so TalkBack
announces it as a heading. Small touch-target spots wrapped in
44dp interactive boxes. Preserves the Phase 8 accessibility
discipline across every screen built in R3–R6."
git push
```

---

## Task 13: R8 keep-rules verification

Confirm the existing Phase 8 `app/proguard-rules.pro` still covers every class R1–R6 introduced, then run a release build to prove it.

**Files:**
- Review (no expected edits): `app/proguard-rules.pro`

- [ ] **Step 1: Review proguard-rules.pro against the new code**

Open `app/proguard-rules.pro`. Confirm the existing blocks still cover:

1. **kotlinx-serialization** — R1–R6 add zero new `@Serializable` classes (`ExportPayload` from Phase 7 is the only serialised type and is preserved unchanged). The existing `-keepclassmembers class **$$serializer { *; }` + `-keepclasseswithmembers class ** { @kotlinx.serialization.Serializable **; }` blocks still cover it.

2. **Room entities** — All 8 entities preserved unchanged. No new entities land. Existing `-keep class com.stretchdaily.app.core.model.** { *; }` still covers them.

3. **Hilt / Dagger** — R1–R6 add several new `@HiltViewModel` classes (`DashboardViewModel`, `SessionOverviewViewModel`, `SessionPlayerViewModel`, `BenchmarkLogViewModel`, `BenchmarkCarouselViewModel`, `AnalyticsViewModel`, `SettingsViewModel`) plus one `@Singleton` (`TodaySessionHolder`). All are covered by the Phase 8 `-keep class dagger.hilt.** { *; }` + `-keep class * extends androidx.lifecycle.ViewModel { <init>(...); }` blocks (the latter keeps ViewModel constructors for DI).

4. **Compose** — Existing `-dontwarn androidx.compose.**` + implicit Compose compiler keep rules are sufficient; no explicit additions needed.

5. **Navigation** — Compose Navigation adds no reflective access paths that need explicit keep rules.

If any of the above isn't already in the file, add the missing block. **Expected:** no edits needed.

- [ ] **Step 2: Run a release build**

Android Studio → Build → Build Bundle(s) / APK(s) → Build APK(s) → **Release** variant. Expected: build succeeds; `app/build/outputs/apk/release/app-release.apk` or equivalent is produced.

- [ ] **Step 3: Install and smoke-test the release APK**

```bash
# From a terminal (if adb is on PATH)
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ANDROID_HOME="/c/Users/RamonvanSchalm/AppData/Local/Android/Sdk" adb install -r app/build/outputs/apk/release/app-release.apk
```

Launch the installed APK. Confirm all 5 tabs open, Dashboard renders, Start session → complete a session → session record is written (visible under Settings → Library → "Sessions logged · N total"). Export JSON to `/sdcard/Download/` (SAF picker), then import it back — data round-trips.

If any Serializer / Hilt / Compose call blows up at runtime (always visible in logcat as `ClassNotFoundException` or `NoSuchMethodException` after R8), add the missing keep rule and retry.

- [ ] **Step 4: Commit (only if proguard-rules.pro was edited)**

If no edits were needed, skip this commit. Otherwise:

```bash
git add app/proguard-rules.pro
git commit -m "chore(r8): verify keep rules against R1-R6 new classes

Smoke-tested the release build: 5 tabs load, session round-trips,
export/import work. No new rules needed — the Phase 8 rule set
covered every new Hilt ViewModel and Compose call path by
category."
git push
```

---

## Task 14: Final pixel QA matrix

Side-by-side against [`docs/design_handoff_stretch_daily_v3/reference/index.html`](../../design_handoff_stretch_daily_v3/reference/index.html). Open the HTML in a desktop browser (Chrome), scroll through each screen, and compare live device / emulator output tab-by-tab.

Open `reference/index.html` and click through:
1. Dashboard (populated mode)
2. Dashboard (empty mode — no sessions)
3. Session overview
4. Session player (first exercise / mid-exercise / last exercise)
5. Session complete
6. Benchmark log (collapsed / expanded)
7. Log sheet (numeric / categorical)
8. Benchmark carousel (step 1 / step 5 / step 10)
9. Analytics (All filter)
10. Analytics (category filter applied)
11. Settings

Each screen's **checkpoints**:

| Screen | Checkpoint |
|---|---|
| Dashboard | Date eyebrow mono-caps + streak flame; hero title 30 sp / 44 sp / 40 sp per theme (we ship default 30 sp); Session card striped tiles match category tint order; WeekStrip 7 squares with today-dashed outline (or filled if completed) |
| Dashboard (empty) | Streak shows "0 days"; Session card still renders today's plan (TodaySessionHolder always has one); KPI cards show 0s |
| Session overview | Exercise list alternating row tints; swap sheet slides up 250 ms; same-category alternatives only |
| Session player | SegmentProgress at top + accent mid-segment; exercise name + category eyebrow; timer 74 sp digital; prev / pause-resume / next chrome; side "L / R" label on unilateral timed moves |
| Session complete | Full-bleed accent background; cream ink; "Nice work" 48 sp; stats row; back-to-dashboard pill |
| Benchmark log | Hero header; CTA card (dashed sparkle); grouped sections with category eyebrow; each row: name + unit + sparkline + overdue pill if due; expand reveals description + bands list + history |
| Log sheet | Numeric variant: 30 sp centered BasicTextField, unit suffix, Cancel/Save 1:2 footer; Categorical variant: 5 band buttons in grid, selected → accent |
| Carousel step 1 | × in left CircleButton, 10-segment progress bar at top, "BENCHMARK 1 OF 10 · NECK"; form area matches log sheet |
| Carousel step 10 | "Save entry" pops the whole carousel graph (back to wherever entry came from) |
| Analytics (All) | Hero + filter pills row (All selected accent); 10 cards in catalog order; each has 3 dp category tint top strip, eyebrow + name left, latest value + delta chip right; 130 dp BigChart with dashed midline + filled area |
| Analytics (filter) | Tapping a pill filters cards to that category; pill turns accent |
| Settings | Mono-caps "Settings" eyebrow; "Your app, offline." hero; three grouped cards; audio toggle reflects persisted value; banner toggle reflects persisted value; library row values match live counts; footer centered mono-caps |

For any mismatch:
- spacing or colour → fix in the screen file, re-verify, amend the task's commit;
- structural → open a new commit with a descriptive message (`fix(analytics): card padding matches reference`).

No commit required if everything matches. Expected outcome: a single "fix(*)" commit sweeping up 1–3 small visual adjustments.

- [ ] **Step 1: Run through the matrix**
- [ ] **Step 2: Amend with any small visual fixes**
- [ ] **Step 3: Commit if needed**

```bash
git commit -am "fix(ui): pixel QA sweep against reference/index.html"
git push
```

---

## Task 15: Final manual smoke test

End-to-end flow in a fresh install.

1. **Uninstall + reinstall.** `adb uninstall com.stretchdaily.app` then install the debug APK.
2. **First launch.** Splash shows cream + sage figure (not dark). Home screen launcher icon is sage + cream figure.
3. **Dashboard.** Renders. Hero title "Stretch Daily". Streak "0 days". Today's session card renders (engine regenerated the plan). Benchmark banner HIDDEN by default.
4. **Start session.** Tap the hero CTA → Session overview. 7–8 exercises listed. Tap "Begin session" → player. Skip through exercises at speed with the next arrow. Finish → complete screen → back to Dashboard.
5. **Streak updated.** Dashboard now reads "1 day".
6. **Open Log tab.** 10 benchmark rows, grouped by category. Expand one — description + bands + history rendered. Tap "Log" → sheet slides up. Enter a value → Save → sheet dismisses, row's sparkline updates.
7. **Start benchmark carousel.** Tap "Start benchmark day" CTA → Carousel opens at step 1. Enter a value → Save → advances to step 2. Close (×) at step 3 → back to Log tab.
8. **Progress tab.** Renders. Filter "All" selected. 10 cards. Tap category filter → visible subset. The benchmark you just logged has a sparkline + latest value + potentially a delta chip (depends on history).
9. **Settings tab.** All three groups render. Library shows real counts (46 exercises / 7 categories / 10 benchmarks / 1 session).
10. **Toggle banner on.** Go back to Dashboard → banner now visible. Tap banner → Carousel opens. Close.
11. **Export.** Settings → Export data → SAF picker → save `/sdcard/Download/stretch-test.json`. Snackbar "Export complete".
12. **Delete all.** Settings → Delete all data → confirm. Snackbar "All data deleted". Dashboard back to streak 0; Log tab benchmarks still present but zero history; Progress cards empty.
13. **Import.** Settings → Import data → pick the exported file. Snackbar "Import complete". Dashboard streak 1 day restored; benchmark history restored; Progress cards populated.
14. **Kill the app; relaunch.** Data persists.
15. **Switch device orientation to landscape.** Every screen renders reasonably (bottom nav still on the bottom; no overflow crashes).

If any step fails, fix in-place before proceeding to the PR.

---

## Task 16: Open the R6 PR

Same pattern used in R1–R5.

- [ ] **Step 1: Final commit + push check**

```bash
git status
git log --oneline development..HEAD
```

Expect: clean working tree; 15–25 commits ahead of development covering Tasks 1–15.

- [ ] **Step 2: Create PR**

```bash
gh pr create --base development --title "Sage redesign R6 — Analytics + Settings + icon + polish" --body "$(cat <<'EOF'
## Summary

Closes the Sage redesign. R6 ships the last two screens — **Analytics** (Progress tab with per-benchmark `BigChart` + 6-month delta chip) and **Settings** (grouped Preferences / Your data / Library layout with the new benchmark-banner toggle + reactive library stats) — plus the launcher icon + splash theme update and a cross-screen accessibility audit.

After this merge, all 5 tabs are real, every R2 primitive has a consumer, and the app's first-impression surfaces (icon + splash) are on-brand. The redesign is complete.

## Deltas vs. design spec

- None. Spec §8.6, §8.8, §9, §11.6 implemented as written.

## New tests

- `BenchmarkBetterTest` (4 cases) — ascending / descending / categorical / unknown direction lookups.
- `BenchmarkDeltaTest` (8 cases) — 6-month delta math: empty / single log / categorical short-circuit / HIGHER improvement / LOWER improvement / regression / nearest-date fallback / non-parseable baseline.
- `AnalyticsViewModelTest` (10 cases) — fan-out + filter + delta direction + live log emission updates.
- `SettingsViewModelTest` (8 cases) — state composition / audio toggle / banner toggle / live flow update / category dedupe / delete dialog / delete-all path / status consumption.

## Manual QA

- Full 15-step smoke test (install → session → log → carousel → analytics → settings → export → delete → import → orientation) passed on a Pixel 7 emulator running API 34. See Task 15 of the implementation plan.
- Release APK built and installed with R8 minification enabled; no crashes.
- TalkBack walked every screen — reading order coherent, headings announced as headings, all interactive elements announce a meaningful label.

## Test plan

- [ ] Pull `redesign/r6-analytics-settings-polish`, open in Android Studio, Build → Make Project — clean.
- [ ] Run `./gradlew test` from Android Studio — all unit tests pass (existing + new).
- [ ] Install on a device / emulator. Run the 15-step smoke test.
- [ ] Open `docs/design_handoff_stretch_daily_v3/reference/index.html` side-by-side with the device; walk through the pixel QA matrix (Task 14).
- [ ] Build → Release APK. Install. Smoke-test (Task 13 Step 3).

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

- [ ] **Step 3: Self-review the PR**

Open the PR in the browser. Scroll the "Files changed" tab and confirm:
- No `ui/home/`, `ui/benchmarks/`, `ui/session/` legacy paths appear (R1 deleted them; subsequent phases didn't resurrect them).
- No gradle or library version bumps.
- No changes under `app/src/test/core/` or `app/src/test/data/` (preserved unit tests).
- Proguard rules unchanged (or trivial additions only, per Task 13).

- [ ] **Step 4: Link sibling PRs**

In the R6 PR body (edit after creation), add:

```
## Related

Part of the 6-phase Sage redesign rollout:
- R1 — Tokens + scaffold
- R2 — Primitives + gallery
- R3 — Dashboard + TodaySessionHolder
- R4 — Session flow
- R5 — Log + Benchmark carousel
- **R6** — Analytics + Settings + polish (this PR)
```

---

## Verification summary

At the end of R6, re-run every check from R1's verification list + the new R6 work:

- [ ] `./gradlew test` passes — Android Studio runs the JVM test suite (preserved Phase 1–8 tests + every R3/R4/R5 ViewModel test + `BenchmarkBetterTest` + `BenchmarkDeltaTest` + `AnalyticsViewModelTest` + `SettingsViewModelTest`).
- [ ] App compiles via Android Studio → Make Project. No warnings.
- [ ] Release APK builds with R8 minification enabled. No runtime crashes on a smoke-test install.
- [ ] Every bottom-nav tab opens a real screen matching the handoff (no `PlaceholderScreen`s left).
- [ ] Launcher icon and splash screen both use the Sage palette. No dark flash at launch.
- [ ] TalkBack reads every screen coherently.
- [ ] Pixel QA matrix produces no structural mismatches against `reference/index.html`.

If all 7 pass, R6 is ready to merge and **the Sage redesign is complete**. After merge, the following live on `development`:

- 12 primitives under `ui/components/`
- 8 screens — Dashboard, Session overview, Session player, Session complete, Benchmark log, Benchmark carousel, Analytics, Settings
- 2 nested navigation graphs (`"session"`, `"carousel"`) with `hiltViewModel(parentEntry)` scoping
- 7 new `@HiltViewModel` ViewModels + 1 new `@Singleton` (`TodaySessionHolder`)
- 4 new pure Kotlin helpers (`sparklineValues` from R5; `benchmarkBetter`, `benchmarkDelta` from R6; `weekOf` from R2)
- The complete Sage token system (`AppColors`, `AppTypography`, `AppDimens`, `CategoryTint`, `Theme`)
- 46 catalog exercises + 10 benchmarks preserved unchanged; 13 preserved domain test files untouched

## Rollback

If R6 needs to be backed out (e.g. a regression spotted after merge):
```bash
git revert <merge-commit-sha> -m 1
```

Reverts the merge commit and all 15-ish R6 commits in one go. Leaves R1–R5 intact. The app returns to the R5 state — Dashboard / Session / Log / Carousel work; Progress + Settings revert to their R1 placeholders (the revert resurrects the placeholder wiring in `StretchDailyNavHost.kt`).

## Next

Nothing. R6 is the final phase. If the user wants post-redesign follow-ups (Play Store release prep, real exercise illustrations, dark theme), those are their own out-of-scope projects scoped separately.
