# Stretch Daily — Sage redesign (v3) — design spec

**Date:** 2026-04-23
**Status:** Design approved; implementation plan pending.
**Handoff source:** [`docs/design_handoff_stretch_daily_v3/`](../../design_handoff_stretch_daily_v3/) (Sage theme).

---

## 1. Purpose and scope

Rebuild Stretch Daily's UI layer to the Sage design handoff. This is a complete
visual + information-architecture redesign of the app — new theme, new
typography, new 5-tab bottom navigation, and two net-new capabilities
(Analytics tab, Benchmark Carousel overlay flow) plus one net-new interaction
(swap exercise on the session overview).

**Not a tech rewrite.** The Kotlin / Jetpack Compose / Room / Hilt stack stays.
The domain layer (engine, repositories, benchmark resolution, all unit tests)
is preserved as-is. Only the `ui/` tree is rewritten.

The app remains offline, single-user, local-only. No account system, no
network, no sync.

### 1.1 Decisions ratified during brainstorming

| # | Decision | Choice |
|---|---|---|
| Q1 | Preservation strategy | **Preserve domain, rewrite UI.** Keep `core/`, `data/`, all tests. |
| Q2 | Rollout model | **Phased rip-and-replace** — 6 PRs mirroring the repo's Phase 1–8 discipline. |
| Q3 | Net-new features in scope | **All three in:** Analytics tab + Benchmark Carousel + Swap exercise. |
| Q4 | Theme strategy | **Sage only, palette abstracted** — tokens structured as swappable `AppColors` / `AppTypography` data classes so Grove / Moss / dark could slot in later without refactoring. |
| Q5 | Today's-session plan lifecycle | **In-memory `@Singleton`**, regenerated **only on calendar-day rollover**. Completion does NOT reset — user can redo the same plan that day. Force-close = fresh plan on re-launch (accepted tradeoff). |
| Q6 | Asset strategy | **Bundle Manrope + JetBrains Mono fonts**; **striped category-tinted placeholders only** for exercise imagery (matches handoff as the shipping production look). |
| Sub | oklch → sRGB conversion | **Runtime helper** preserves original design intent. Values memoized at first access. |
| Sub | Bottom sheet implementation | Default to Material 3 `ModalBottomSheet`, swap to hand-rolled only on styling friction. |
| Sub | Component gallery | **Yes** — debug-build-only hidden screen for visual QA during R2. |
| Sub | "Next benchmark" KPI semantics | **Days until the next-due benchmark** (forward-looking). |
| Sub | App icon | **Replace** — cream stretching figure on a sage `#5c7a4a` adaptive background. Splash theme updated to cream to remove the dark-to-cream flash. |

---

## 2. Feasibility summary (from assessment)

The existing stack supports this redesign with no new libraries. Jetpack
Compose can render every token, typography rule, hairline card, pill button,
striped tile, sparkline, and `BigChart` shown in the handoff. The Room + Hilt
+ ViewModel + StateFlow pattern already in use maps cleanly onto every new
screen's data needs.

**Biggest semantic gap — intentionally not adopted:** the designer's
`buildTodaySession` is a deterministic *"1 per category + 1 extra from the
weakest-band category"* picker. The existing `LongevityEngine` is
weighted-random driven by `FlexibilityTier.weight` with a 14-day Selection
Shield that forces stale exercises back into rotation. Both produce 7–8
exercises that fit the new UI; the engine is preserved because it is tested,
richer, and the handoff's simpler picker was a prototyping convenience.

**Visual gap that doesn't carry over:** the current dark `#0D0D0D` + orange
`#FF8C00` palette is replaced entirely by the Sage cream `#fbf8f0` + sage
green `#5c7a4a` palette. No dark-mode toggle ships with this redesign.

---

## 3. Architecture boundary

### 3.1 Preserved unchanged

- `core/model/` — all 8 Room entities + enums.
- `core/database/` — `StretchDailyDatabase`, `DatabaseSeeder`, `Converters`, all DAOs.
- `core/engine/` — `LongevityEngine`, `CategoryWeightCalculator`,
  `SelectionShield`, `SessionBuilder`, `SessionPlan`, `PlannedExercise`.
  Config stays at 5–8 exercises / 600–900s / 120s per-exercise cap.
- `core/benchmark/` — `TierResolver`, `BenchmarkProgressBuilder` (the latter
  is directly consumed by the new `BigChart`).
- `core/audio/SessionAudioPlayer`, `core/datastore/SettingsDataStore`,
  `core/util/Clock`.
- `core/di/DatabaseModule`, `core/di/EngineModule`.
- `data/` — `BenchmarkRepository`, `SessionRepository`, `DataPortRepository`,
  `ExportPayload`.
- All `app/src/test/` unit tests — they exercise `core/` and `data/`, both
  preserved, so they continue to pass without modification.

### 3.2 New in `core/`

- **`core/session/TodaySessionHolder.kt`** — `@Singleton` owning the day's
  cached plan (§6).
- **`core/di/SessionModule.kt`** — thin, provides `TodaySessionHolder`.

Category tints live under `ui/theme/` (they depend on Compose's `Color`
type and belong to the theme surface) — see §4.1.

### 3.3 New flows on existing repositories

Added non-invasively to the existing repos:

- `SessionRepository.streakFlow: Flow<Int>` — reactive wrapper around
  `computeStreak`, derived from `observeAllSessions`.
- `SessionRepository.weeklyFlow: Flow<List<LocalDate>>` — this week's
  completed session dates, Monday-start.
- `SessionRepository.totalsFlow: Flow<Totals>` — total minutes + total
  session count.
- `BenchmarkRepository.overdueFlow: Flow<List<Benchmark>>` — benchmarks
  whose last log is before the current month-start.
- `BenchmarkRepository.nextDueFlow: Flow<BenchmarkWithDueDate?>` — the
  next benchmark to log and how many days until it's due.

### 3.4 Deleted wholesale (at R1)

Everything under `ui/` — `theme/`, `navigation/`, `home/`, `benchmarks/`,
`session/`, `settings/`. All gets rewritten from scratch. The dark-palette
`Color.kt` constants are removed with it.

### 3.5 New in `ui/`

- `ui/theme/` — abstracted token classes (§4).
- `ui/components/` — 12 shared primitives (§5).
- `ui/navigation/` — 5-tab bottom nav + nested graphs for session-player and
  benchmark-carousel overlays (§7).
- `ui/screen/{dashboard, session, log, analytics, settings, carousel}/` —
  one directory per screen, each containing `Screen.kt` + `ViewModel.kt` +
  `UiState.kt`.
- `ui/debug/ComponentGalleryScreen` — debug-build-only, hidden route for
  visual QA during R2.

---

## 4. Theme token system

Sage is the only active theme at runtime. Tokens are structured as immutable
data classes so Grove, Moss, or a dark theme can slot in later via a single
`CompositionLocalProvider` swap with zero changes to consumer code.

### 4.1 Files

- **`ui/theme/AppColors.kt`** — immutable `data class AppColors(bg, bg2,
  surface, surface2, ink, ink2, ink3, line, line2, accent, accent2,
  accentInk, accentSoft, warn)`. Function `sageColors()` returns the Sage
  values (`--bg = #fbf8f0` per user-ratified handoff tweak, `--accent =
  #5c7a4a`, etc.).
- **`ui/theme/AppTypography.kt`** — immutable `data class
  AppTypography(displayXl, displayLg, displayMd, bodyLg, bodyMd, bodySm,
  monoCaps, monoCapsSm)`. Each field is a `TextStyle`. Mono-caps
  uppercasing is done at the call site via `text.uppercase(Locale.getDefault())`
  since Compose has no CSS `text-transform` equivalent; the style itself
  just configures family, weight, size, letter-spacing.
- **`ui/theme/AppDimens.kt`** — `data class AppDimens(radiusXs = 8.dp,
  radiusSm = 12.dp, radiusMd = 16.dp, radiusLg = 22.dp, radiusPill =
  999.dp, gapList = 8.dp, gapSection = 14.dp, gapGroup = 18.dp,
  padScreen = PaddingValues(start = 20.dp, top = 6.dp, end = 20.dp,
  bottom = 100.dp))`. The 100dp bottom accounts for the fixed bottom nav.
- **`ui/theme/CategoryTint.kt`** — `fun Category.tint(): Color` lookup.
  Seven oklch triples at the top of the file, a `oklchToSrgb(l, c, h): Color`
  helper below them, and a lazy-initialized map of `Category` → `Color`.
- **`ui/theme/Theme.kt`** — three `CompositionLocal<T>` instances
  (`LocalAppColors`, `LocalAppTypography`, `LocalAppDimens`). The
  `StretchDailyTheme(content)` composable provides the Sage defaults and
  wraps a `MaterialTheme`.
- **`object Theme`** — convenience accessor exposing `Theme.colors`,
  `Theme.typo`, `Theme.dims` via `@Composable` getters that resolve each
  `CompositionLocal`. Keeps call sites short (`Theme.colors.ink` vs.
  `LocalAppColors.current.ink`).

### 4.2 Fonts

Manrope (400 / 500 / 600 / 700) and JetBrains Mono (400 / 500) are bundled
under `app/src/main/res/font/` as TTF files, committed to the repo (both are
SIL Open Font Licensed). A single `FontFamily` per typeface is assembled in
`AppTypography.kt`. No runtime download; no Google Fonts dependency.

### 4.3 oklch → sRGB

Compose does not render oklch natively. Per-category tints are stored as
oklch triples and converted via a 30-line pure-Kotlin helper using the
standard oklab intermediate + linear-sRGB matrix + gamma correction. The
result for each `Category` is memoized in a `lazy { mapOf(...) }` so the
conversion happens once per process lifetime. If Compose ever ships native
oklch support, the helper swaps for a single Compose call and call sites
don't change.

### 4.4 What `MaterialTheme` still drives

Default ripple color (set to `accent`), `Surface` bg when unspecified,
`Icon` / `Text` content-color defaulting. Everything explicit uses
`Theme.colors.*` — `MaterialTheme.colorScheme.*` does not appear in app
code outside the theme wrapper itself.

### 4.5 Not part of the theme

- **Animation durations** (0.15–0.25s) — inlined at call sites via
  `AnimationSpec`.
- **Shadow rules** — Sage uses hairlines, not shadows. Only `Sheet` gets
  a shadow (`0 -8.dp 28.dp rgba(0,0,0,0.18)`), inlined in that component.

---

## 5. Shared primitive library

Twelve components built in Phase R2, before any screen. All under
`ui/components/`, one file per component. Order matters — each builds on
earlier ones.

1. **`MonoCaps(text, size, color, modifier)`** — wraps `Text` with
   `text.uppercase(Locale.getDefault())`, JetBrains Mono, letter-spacing
   1.0–1.4 sp, size 9.5 sp or 11 sp. Two size variants: `Small`, `Regular`.
   This is the design's voice-mark primitive.
2. **`Icon(name, size, tint, contentDescription)`** — outline SVG set.
   Each icon is a vector XML in `res/drawable/ic_*.xml` (play, pause,
   skip, swap, sparkle, flame, chevron, close, check). Wrapper routes by
   `IconName` enum. `contentDescription` is non-null only on interactive
   uses (matches existing Phase 8 accessibility discipline).
3. **`Pill(onClick, label, leadingIcon, variant)`** + **`CircleButton(size,
   onClick, icon, variant)`** — chrome primitives. `Pill` variants: `Accent`
   (accent bg), `Neutral` (bg2), `DashedOutline` (transparent + dashed
   accent border for "Perform at your own pace"). `CircleButton` sizes:
   36 / 44 / 52 / 58 dp (`Small` / `Medium` / `Large` / `Large58`).
   Variants: `Bg2` (default chrome), `Ink` (inverted — session-player next
   button), `Accent` (accent bg + `bg` icon — dashboard "Begin session" CTA).
4. **`Sheet(visible, onDismiss, content)`** — bottom sheet. Slide-up 250 ms
   ease, backdrop fade 200 ms. Top-only `radiusLg`. Shadow per §4.5.
   Defaults to Material 3 `ModalBottomSheet` with token overrides; swap
   to hand-rolled only if styling fights.
5. **`ExerciseTile(category, size)`** — category-tinted striped placeholder.
   `Canvas`: fill with `category.tint()`, draw diagonal 45° stripes at 4 dp
   spacing in the category tint's higher-opacity shade. Two size variants:
   `Small56` (56×56 dp) and `Large4x3` (full-width 4:3). The large variant
   renders a mono-caps footer `"{category} · animation / .webp placeholder"`.
6. **`CatChip(category)`** — 1 dp hairline outline, pill radius, 10 sp
   mono-caps label + 6×6 dp tint dot.
7. **`BandPill(tier)`** — fixed-size pill labeled by `FlexibilityTier`
   (Stiff → Very flexible). Five discrete colors shading from `warn` →
   `accent`.
8. **`Sparkline(values: List<Double>)`** — 50×14 dp `Canvas`. Normalized
   0..1 values; 1.5 dp polyline in `ink3` + 2.5 dp last-point marker in
   `accent`. Empty state: dashed flat line at 0.5.
9. **`BigChart(series: BenchmarkProgressBuilder.Series)`** — 130 dp tall,
   full-width `Canvas`. Draws 5 horizontal tier bands (subtle tints),
   dashed midline, gridlines at tier boundaries, filled area under the
   polyline at `accent × 0.12`, 2 dp polyline, 6 dp highlighted last-point
   marker with ink-bg outline. Left-gutter tier labels (Stiff / Average /
   Very flexible) rendered outside the `Canvas` in a `Row` so text baselines
   align with band centers.
10. **`WeekStrip(week: List<DayState>)`** — 7 aspect-1 squares in a `Row`
    with 6 dp gaps. Each: a tiny weekday mono-caps label above, then a
    16 dp-radius box — filled `accent` if completed, 2 dp dashed `accent`
    outline if it's today-not-completed, `line` 1 dp outline otherwise.
    Week starts Monday (ISO 8601).
11. **`SegmentProgress(total: Int, currentIndex: Int)`** — N equal-width
    segments with 3 dp gaps, 3 dp tall, radius 1.5 dp. State per segment:
    `Past` (accent × 0.55), `Current` (accent), `Future` (line). Used by
    session player (N = plan size) and benchmark carousel (N = 10).
12. **`KpiCard(eyebrow, icon, value, suffix)`** — `surface` bg, `radiusMd`,
    16 dp padding. Top row: mono-caps eyebrow left + icon right. Big row:
    26 sp Manrope 500 value + 12 sp suffix in `ink3`. Dashboard uses 4.

**Visual QA:** during R2, each component is rendered in the debug-only
`ComponentGalleryScreen` and compared against `reference/index.html`.
`Sparkline`, `BigChart`, `WeekStrip`, and `SegmentProgress` are the most
worth pixel-comparing.

**Tests:** Paparazzi / Roborazzi snapshot tests are out of scope for this
spec. Visual verification is manual during R2.

---

## 6. `TodaySessionHolder`

The single most load-bearing new class in the redesign. `@Singleton`,
injected by Hilt.

### 6.1 Public surface

```kotlin
@Singleton
class TodaySessionHolder @Inject constructor(
    private val engine: LongevityEngine,
    private val exerciseDao: ExerciseDao,
    private val clock: Clock,
    @ApplicationScope private val scope: CoroutineScope,
) {
    val state: StateFlow<TodaySession?>

    suspend fun ensureFresh()
    suspend fun swap(oldId: String, newId: String)
    fun onSessionCompleted()
}

data class TodaySession(
    val planDate: LocalDate,
    val plan: SessionPlan,
)
```

### 6.2 State machine

- **Initial:** `state.value == null`.
- **First access** — any consuming ViewModel calls `ensureFresh()` from
  `init`: invokes `engine.generateSession()`, wraps in
  `TodaySession(today, plan)`, emits.
- **Day rollover** — subsequent `ensureFresh()` calls check whether
  `state.value.planDate != LocalDate.now(clock)`. If so, regenerate.
  Otherwise no-op.
- **Swap** — locate `oldId` in `plan.items`, replace with
  `PlannedExercise(newExercise, newExercise.totalTime.coerceAtMost(120),
  isForced = false)`, emit the new state. Swap is same-category only —
  enforced by the ViewModel that picks alternatives from
  `exerciseDao.getByCategory(category)`.
- **Completion** — `onSessionCompleted()` is a **no-op on the cached plan**.
  The plan stays (per Q5 clarification: user can redo the same session
  that day). The `SessionRecord` is written via `SessionRepository` in the
  player ViewModel — separate concern. Next day's `ensureFresh()` sees
  `planDate != today` and regenerates.

### 6.3 Rollover detection

Happens only on access. No background midnight timer. Every consuming
ViewModel calls `ensureFresh()` in `init`, so by the time any UI renders,
the check has run.

### 6.4 Placement rationale

Lives at `core/session/`, not `data/`. It's orchestration over the engine,
not a repository. Owns an ephemeral in-memory concept ("today's cached
plan"), not durable data. Sits next to the engine it wraps.

### 6.5 DI wiring

New `core/di/SessionModule.kt` provides the `@Singleton`. Reuses the
`@ApplicationScope` binding already defined in `DatabaseModule`.

### 6.6 Tests — `src/test/core/session/TodaySessionHolderTest.kt`

- First access generates via engine (fake `LongevityEngine` records invocation).
- Second access same day returns cached plan; engine is NOT re-invoked.
- Access on the next day regenerates (drive `Clock` forward).
- `swap(oldId, newId)` replaces the correct item, preserves others, keeps
  order.
- `onSessionCompleted()` followed by same-day access returns the same plan
  unchanged.
- Swap after completion still works (user can swap, redo, swap again).

### 6.7 Accepted tradeoff

Process death between dashboard load and session start → fresh plan on
re-launch (possibly different exercises). This is the explicit Q5-(A)
tradeoff. Matches current architecture's behavior.

---

## 7. Navigation and ViewModels

### 7.1 Route tree

```
StretchDailyNavHost  (outer Scaffold w/ BottomNav, contentWindowInsets = WindowInsets(0))
├── "today"                              → DashboardScreen
├── "session"  (nested graph)
│   ├── "session/overview"               → SessionOverviewScreen        (bottom bar)
│   ├── "session/player"                 → SessionPlayerScreen          (overlay)
│   └── "session/complete"               → SessionCompleteScreen        (overlay)
├── "log"                                → BenchmarkLogScreen
├── "progress"                           → AnalyticsScreen
├── "settings"                           → SettingsScreen
└── "carousel"  (nested graph, overlay)
    └── "carousel/{step}"                → BenchmarkCarouselScreen
```

`BOTTOM_NAV_ROUTES = setOf("today", "session/overview", "log", "progress", "settings")`.
All other routes hide the bar via the outer Scaffold's `bottomBar = { if
(currentRoute in BOTTOM_NAV_ROUTES) BottomNavigation(...) }` — same pattern
the current NavHost uses.

### 7.2 Entry points

- **Session overview** — Dashboard hero CTA, or tap the Session tab.
- **Session player** — "Begin session" pill on overview.
- **Session complete** — automatic on session end; CTA returns to Dashboard.
- **Benchmark carousel** — Dashboard benchmark banner (when enabled) +
  "Start benchmark day" card on Log. Both call `navigate("carousel/0")`.
  Close (×) pops back to wherever the user came from. "Save entry" on
  step N advances to `carousel/{N+1}`; on step 10 it pops the whole
  carousel graph.

### 7.3 ViewModels

| ViewModel | Scope | Key dependencies |
|---|---|---|
| `DashboardViewModel` | screen | `TodaySessionHolder`, `SessionRepository` (streak + weekly + totals flows), `BenchmarkRepository` (overdue + nextDue flows), `SettingsDataStore` (for `benchmarkBannerEnabled`) |
| `SessionOverviewViewModel` | screen | `TodaySessionHolder`, `ExerciseDao` (same-category alternatives for swap) |
| `SessionPlayerViewModel` | nested nav graph `"session"` via `hiltViewModel(parentEntry)` | `TodaySessionHolder.state`, internal 1 Hz timer, `SessionAudioPlayer`, `SessionRepository.recordSession` on complete |
| `BenchmarkLogViewModel` | screen | `BenchmarkRepository`, `BenchmarkProgressBuilder` for per-benchmark sparkline data |
| `BenchmarkCarouselViewModel` | nested graph `"carousel"` via `hiltViewModel(parentEntry)` | `BenchmarkRepository.all`, `saveEntry(benchmarkId, value)` |
| `AnalyticsViewModel` | screen | `BenchmarkRepository.all` + history → `BenchmarkProgressBuilder.Series` per benchmark; category filter state |
| `SettingsViewModel` | screen | existing class, repurposed — `SettingsDataStore` + `DataPortRepository` untouched |

The `hiltViewModel(parentEntry)` pattern is already established in the
current NavHost; it keeps timer state alive across overview → player →
complete transitions without re-instantiation.

---

## 8. Screen specifications

The design handoff's [`README.md`](../../design_handoff_stretch_daily_v3/README.md)
is the authoritative specification for every pixel, spacing value, and
interaction detail. This section calls out only the deltas or decisions
that differ from the handoff or warrant repetition here.

### 8.1 Dashboard (`"today"`)

Matches handoff section "1. Dashboard" with the following production
defaults:

- **Benchmark banner default = OFF.** Per the user tweak noted in handoff
  README, banner visibility is stored under a new `SettingsDataStore`
  preference key (`benchmarkBannerEnabled`, default `false`). A future
  Settings toggle can flip it.
- **"Extra focus" category** — derived as `argmin(FlexibilityTier.ordinal)`
  across `CategoryWeightCalculator`'s output. If multiple categories tie
  at Stiff, pick the first in `Category` enum order.
- **Session order bars** — rendered from `TodaySessionHolder.plan.items`
  in order, one colored rect per `PlannedExercise`.
- **"Next benchmark" KPI** — shows `{n} d` where `n` is
  `daysUntil(benchmark.nextDueDate)` from `BenchmarkRepository.nextDueFlow`.
  If a benchmark is already overdue, displays `0 d` (not negative).

### 8.2 Session overview (`"session/overview"`)

Matches handoff section "2. Session overview".

- **Swap sheet** — opens via `ModalBottomSheet`. Populated from
  `exerciseDao.getByCategory(ex.category)` minus the current exercise and
  any other exercises already in the plan. Tapping an alternative calls
  `TodaySessionHolder.swap(oldId, newId)` and closes the sheet.
- **Swap persistence** — per Q5, swaps mutate the in-memory plan. Backing
  out of overview and returning shows the swapped version. Day rollover
  or process death resets.

### 8.3 Session player (`"session/player"`)

Matches handoff section "3. Session player" exactly.

- **Timer loop** — 1 Hz `delay`-based coroutine inside `viewModelScope`,
  driven from an `internal fun tick()` for testability (preserves current
  pattern).
- **L/R cycling** — unilateral + timed moves cycle L → R → next exercise.
  Timer resets when the side changes. Prev mirrors this.
- **Audio cues** — `SessionAudioPlayer.playStart()` on session begin and
  on every exercise transition; `playEnd()` when the session finishes.
  Existing preference gating in `SessionAudioPlayer` is unchanged.
- **Reaching last exercise** — past-the-end fires `onComplete`, which
  calls `SessionRepository.recordSession(plan, actualExercises)` and
  navigates to `"session/complete"`.

### 8.4 Session complete (`"session/complete"`)

Matches handoff section "4. Session complete". Full-bleed accent, cream ink.

- **Body copy** — "You stretched {areaCount} areas in {min} minutes.
  Streak's at **{streak + 1}** now." — `areaCount` is the distinct
  category count from `plan.items`; `streak` is read from
  `SessionRepository.streakFlow` post-record.
- **Italic display font** — Manrope italic for R6. If the user wants a
  true serif later (Fraunces or Playfair italic), that's a single
  `AppTypography.displayXl` swap.

### 8.5 Benchmark log (`"log"`)

Matches handoff section "5. Benchmark log".

- **"OVERDUE" pill** — rendered next to the name when the latest log is
  before the current month-start.
- **Sparkline** — last 6 months of logs, rendered by the shared
  `Sparkline` primitive from data provided by `BenchmarkProgressBuilder`.
- **Log sheet** — two variants: `NumberInputSheet` (big centered numeric
  input with unit suffix) and `CategoryPickSheet` (vertical list of 5
  bands with the selected one turning accent). Shared composable for
  Cancel / Save entry footer.
- **Same log sheet is reused** by the benchmark carousel (§8.7).

### 8.6 Analytics / Progress (`"progress"`)

Matches handoff section "6. Analytics (Progress)".

- **Category filter** — horizontally scrolling mono-caps pills including
  "All". Filter state is screen-local (not persisted).
- **Per-benchmark card** — 3 dp full-width category tint strip on top.
  Delta calculation: compare latest value to the log ~6 months prior
  (nearest-date fallback if no exact match). Color: `accent` if improving
  (per `benchmark.better`), `warn` if regressing.
- **Chart** — `BigChart` primitive fed from
  `BenchmarkProgressBuilder.Series`.

### 8.7 Benchmark carousel (`"carousel/{step}"`)

Matches handoff "Benchmark carousel" section.

- **Header** — mirrors session player's top bar: close (×) +
  `SegmentProgress(total = 10, currentIndex = step)` + mono-caps
  `"BENCHMARK {step + 1} OF 10 · {category}"`.
- **Body** — benchmark name + description + same log form from the Log
  sheet (§8.5).
- **Save entry** — persists via `BenchmarkRepository` and advances to
  `carousel/{step + 1}`. On step 10 (the last), pops the entire carousel
  graph.
- **Close (×)** — pops the entire carousel graph (discards any unsaved
  in-progress entry for the current step).

### 8.8 Settings (`"settings"`)

Matches handoff section "7. Settings".

- **Preferences group** — Timer sound-effects toggle (existing behavior),
  plus a new "Benchmark day reminder banner" toggle bound to the new
  `benchmarkBannerEnabled` preference.
- **Your data group** — Export / Import / Delete-all — existing
  `DataPortRepository` wiring preserved; only visual redesign.
- **Library group** — read-only rows showing "Exercises · 45 across 7
  categories", "Benchmarks · 10 tests", "Sessions logged · {n} total".
  The session count is derived from
  `SessionRepository.totalsFlow.sessions`.
- **Footer** — "Stretch Daily · v3 · local only" in mono caps, centered.

---

## 9. App icon and splash

Per user decision in brainstorming, the launcher icon is redesigned for
Sage:

- **Adaptive icon foreground** — the existing stretching-figure vector
  silhouette, re-tinted to cream (`#f5f3ea`).
- **Adaptive icon background** — sage accent (`#5c7a4a`).
- **Splash theme (`Theme.StretchDaily.Splash`)** — background updated to
  `#fbf8f0` (cream) + the sage figure overlay. Eliminates the dark →
  cream flash at app launch.

The swap is a one-value change to the adaptive icon background XML if a
cream-background icon is preferred later.

---

## 10. Testing

### 10.1 Preserved test tree

All existing unit tests under `app/src/test/` continue to pass without
modification. They exercise `core/` and `data/`, both of which are
preserved.

### 10.2 New unit tests per phase

- **R3** — `TodaySessionHolderTest` (6 cases per §6.6),
  `DashboardViewModelTest` (mockk-driven state composition).
- **R4** — `SessionOverviewViewModelTest` (swap behavior, same-category
  filter), `SessionPlayerViewModelTest` (drive `tick()`, L/R cycling,
  timer reset on side change, completion call path).
- **R5** — `BenchmarkLogViewModelTest` (grouping, sparkline data,
  overdue flag), `BenchmarkCarouselViewModelTest` (advance, save, close
  behavior).
- **R6** — `AnalyticsViewModelTest` (category filter, delta calculation,
  series shape), `SettingsViewModelTest` updates for new state fields.

### 10.3 Snapshot tests

Paparazzi / Roborazzi are out of scope. Visual verification is manual
via the debug `ComponentGalleryScreen` during R2 and side-by-side vs.
`reference/index.html` at each phase.

### 10.4 Instrumented tests

Out of scope (same as current repo state).

---

## 11. Phasing — 6 PRs

Each phase is one feature branch (`redesign/r{N}-{slug}`) → one PR to
`development`. Each phase leaves the app compiling and runnable;
unfinished screens get minimal placeholders rather than broken state.

### 11.1 Phase R1 — Tokens, fonts, empty scaffold

*Branch:* `redesign/r1-tokens-scaffold`

- Bundle Manrope + JetBrains Mono under `res/font/`.
- Add `ui/theme/{AppColors, AppTypography, AppDimens, Theme,
  CategoryTint}`. Implement `oklchToSrgb` helper + memoized per-category
  tints.
- Rewrite `StretchDailyTheme` composable.
- Replace `StretchDailyNavHost` with the 5-tab version; each tab renders
  a minimal `PlaceholderScreen(label)`.
- Delete `ui/home/`, `ui/benchmarks/`, `ui/session/`, `ui/settings/`,
  `ui/navigation/` old files and `ui/theme/Color.kt` constants.
- Update `MainActivity` reference.
- App runs and shows 5 empty Sage-themed tabs. All `core/` + `data/`
  tests still pass.

### 11.2 Phase R2 — Primitives + gallery

*Branch:* `redesign/r2-primitives`

- Build the 12 primitives from §5 in order.
- Add outline SVG icons to `res/drawable/`.
- Add `ui/debug/ComponentGalleryScreen` (debug build variant only) —
  hidden route, long-press on Settings to open it (default) or a
  hidden 6th tab, developer's call at implementation.
- No screen integration; all 5 tabs still placeholders.
- Visual QA against `reference/index.html`.

### 11.3 Phase R3 — Dashboard + `TodaySessionHolder`

*Branch:* `redesign/r3-dashboard`

- Add `core/session/TodaySessionHolder` + its unit tests.
- Add `core/di/SessionModule`.
- Add the new flows on `SessionRepository` (`streakFlow`, `weeklyFlow`,
  `totalsFlow`) and `BenchmarkRepository` (`overdueFlow`, `nextDueFlow`).
- Add `benchmarkBannerEnabled` preference key to `SettingsDataStore`
  (default `false`).
- Build `ui/screen/dashboard/` — `DashboardScreen`, `DashboardViewModel`,
  `DashboardUiState`. Hero + date + streak + conditional banner + Today's
  session card + WeekStrip + KPI grid.
- Dashboard CTA routes to `session/overview`, still a placeholder ("R4
  stub").
- Tests: `DashboardViewModelTest`, `TodaySessionHolderTest`.

### 11.4 Phase R4 — Session flow

*Branch:* `redesign/r4-session`

- `ui/screen/session/SessionOverviewScreen` + `SessionOverviewViewModel`
  — exercise list, swap sheet, "Begin session" CTA.
- `ui/screen/session/SessionPlayerScreen` + `SessionPlayerViewModel`
  (scoped to `"session"` nested graph) — timer, reps counter, L/R
  unilateral cycling, SegmentProgress header, start / pause / resume /
  prev / next, audio cues via existing `SessionAudioPlayer`.
- `ui/screen/session/SessionCompleteScreen` — full-bleed accent
  celebration; pill CTA returns to Dashboard.
- Completion writes `SessionRecord` via existing
  `SessionRepository.recordSession`.
- Tests: `SessionOverviewViewModelTest`, `SessionPlayerViewModelTest`.
- Size guard: if this phase exceeds ~1 week, split into R4a
  (overview + complete) and R4b (player). The nested-graph wiring
  supports the split.

### 11.5 Phase R5 — Log + Benchmark carousel

*Branch:* `redesign/r5-log-carousel`

- `ui/screen/log/BenchmarkLogScreen` + `BenchmarkLogViewModel` — grouped
  by category, expand-to-reveal bands + history, log bottom sheet
  (numeric + categorical variants), sparkline per row.
- `ui/screen/carousel/BenchmarkCarouselScreen` +
  `BenchmarkCarouselViewModel` (nested-graph scope) — reuses the log form
  composable. SegmentProgress header. "Save entry" advances; last step
  pops the carousel graph.
- Carousel entry points wired: Dashboard banner + Log "Start benchmark
  day" card.
- Tests: `BenchmarkLogViewModelTest`, `BenchmarkCarouselViewModelTest`.

### 11.6 Phase R6 — Analytics + Settings + icon + polish

*Branch:* `redesign/r6-analytics-settings-polish`

- `ui/screen/analytics/AnalyticsScreen` + `AnalyticsViewModel` — category
  filter pills, per-benchmark card with `BigChart`, delta indicator
  (+ / − vs. 6 mo prior, accent / warn colored).
- `ui/screen/settings/SettingsScreen` — new grouped layout (Preferences
  / Your data / Library), reusing existing `SettingsViewModel` with
  minor UiState adjustments for library stats + banner toggle.
- Replace launcher icon: cream stretching figure on sage adaptive
  background.
- Update `Theme.StretchDaily.Splash` to cream bg + sage figure.
- Content-description audit across all new screens (re-apply Phase 8
  accessibility discipline).
- R8 keep-rules verified against new code paths.
- Final pixel QA against `reference/index.html` for every screen.

---

## 12. Out of scope

Explicit carve-outs for this spec:

- Grove + Moss themes (token structure supports; no runtime toggle).
- Dark theme (removed; can be re-added later via the abstracted
  `AppColors` — not built here).
- Real exercise illustrations or Lottie animations (striped placeholders
  are the shipping production look).
- Instrumented tests.
- Paparazzi / Roborazzi snapshot tests.
- Play Store metadata / release keystore / AAB signing.

---

## 13. Known risks and mitigations

| Risk | Mitigation |
|---|---|
| Gradle CLI builds still blocked on this Windows machine (per CLAUDE.md). | Build + verify via Android Studio → Make Project after each phase. Spec acknowledges this; no phase requires CLI builds. |
| Visual drift between Compose render and `reference/index.html`. | `ComponentGalleryScreen` (R2) + side-by-side QA at each phase. |
| Phase R4 growing too large. | Split into R4a/R4b as noted; nested-graph scope supports it. |
| oklch → sRGB helper getting the color math wrong. | Memoized — one computation per color per process. Visually verify all 7 tints against the reference during R1. Helper is ~30 lines of pure math; trivially unit-testable if needed. |
| Process-death during session results in a swapped plan being lost. | Accepted tradeoff from Q5 option (A). Documented in §6.7. |

---

## 14. References

- Design handoff — [`docs/design_handoff_stretch_daily_v3/README.md`](../../design_handoff_stretch_daily_v3/README.md)
- Handoff reference prototypes — [`docs/design_handoff_stretch_daily_v3/reference/`](../../design_handoff_stretch_daily_v3/reference/)
- Current-state project overview — [`CLAUDE.md`](../../../CLAUDE.md)
