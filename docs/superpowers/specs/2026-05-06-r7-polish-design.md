# R7 — Sage redesign polish pass

**Date:** 2026-05-06
**Branch:** `redesign/r7-polish` (off `development`)
**Continuation of:** R1–R6 (2026-04-23 → 2026-04-25, [#15](https://github.com/RvanSchalm/stretch-daily/pull/15))

A focused refinement of the R1–R6 redesign, driven by a side-by-side comparison
between the current build and the design handoff in
`docs/design_handoff_stretch_daily_v3/`. Most changes correct the implementation
to match the design more faithfully; a handful (sparkline removal, scrollable
charts, Progress page title) deliberately diverge from the handoff per user
direction.

---

## 1. Decisions log

| # | Question | Decision |
|---|---|---|
| 1 | Build/test environment for Claude | **Path C** — stay on Windows, status quo. Claude cannot run Gradle; Ramon verifies in Android Studio. |
| 2 | Sparkline removal scope | **Both Carousel and Log tab.** Once Progress page has full charts, the mini-line is redundant. |
| 3a | Charts scrollable or fixed-window? | **Scrollable.** Each chart grows with log history; default scroll = end. |
| 3b | X-axis label style | **Horizontal MonoCaps every other month, year tucked under each January.** |
| 4 | Progress page title | **"Your trajectory."** |
| 5 | Manual-start interaction | Per-exercise Start required; per-side Start for unilateral; single chime only at timer-phase-end (not Start, not Next, not Prev). |

---

## 2. Theme tokens (cross-cutting)

Revert the previously-approved bg override and restore the design handoff's
Sage palette. The visual effect — "darker bg, lighter cards" — cascades
across every screen with one constant change.

**File:** `app/src/main/java/com/stretchdaily/app/ui/theme/AppColors.kt`

```diff
- bg = Color(0xFFFBF8F0),
+ bg = Color(0xFFF2EEE4),
```

Also delete the now-stale "user-ratified tweak" comment block (lines 38–47);
we're back on the design defaults. `bg2`, `surface`, `surface2`,
`ink/line/accent/warn` already match the handoff and don't change.

**Side-effect notes for visual QA:**
- Cards (`surface = #F9F6EC`) now read as visibly *lighter* than the page bg.
- Bottom nav (`containerColor = surface`) reads as a "lighter shelf" under
  the darker page — consistent with cards, on-design.
- Empty `WeekStrip` boxes (which use `bg2`) may visually pop a little more on
  the new bg — likely an improvement.

---

## 3. Dashboard

### 3.1 Date format

Change pattern from abbreviated locale-aware to fully-spelled English.

**File:** `app/src/main/java/com/stretchdaily/app/ui/screen/dashboard/DashboardScreen.kt:349-350`

```diff
- DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
+ DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH)
```

Renders `"WEDNESDAY, MAY 6"`. The `MonoCaps` wrapper handles uppercasing.

**Why pin English:** every other UI string in the app is hardcoded English
(`"TODAY'S SESSION"`, `"THIS WEEK"`, `"NEXT BENCHMARK"`, etc.). Mixing a
localized date with English everything-else feels off, and Dutch grammar
would render an awkward `"WOENSDAG, MEI 6"`. If a real localization pass
happens later, this is one line to change.

### 3.2 Card icons

The design's `screens.jsx:147-150` specifies:

| KPI | Current | Design | Action |
|---|---|---|---|
| Streak | `Flame` | `flame` | ✓ already matches |
| Total time | `Sparkle` | `clock` | **add `Clock`** |
| Sessions | `Check` | `check` | ✓ already matches |
| Next benchmark | `Sparkle` | `calendar` | **add `Calendar`** |

**New assets:** `app/src/main/res/drawable/ic_clock.xml`,
`app/src/main/res/drawable/ic_calendar.xml`. Both follow the same outline
aesthetic as the existing 14 icons (`ic_play`, `ic_flame`, `ic_check`, etc.) —
matching stroke-width and 24×24 viewport. Source path data from a
permissively-licensed icon set already aligned with the existing iconography
(Lucide or Phosphor).

**Enum:** add `Clock(R.drawable.ic_clock)` and `Calendar(R.drawable.ic_calendar)`
to `IconName` in
`app/src/main/java/com/stretchdaily/app/ui/components/AppIcon.kt:19`.

**Call sites:** swap the two `IconName.Sparkle` references in
`DashboardScreen.kt`'s `KpiGrid` (around lines 308 and 315) to
`IconName.Clock` and `IconName.Calendar` respectively.
`Sparkle` itself stays in the enum — still used by the benchmark banner.

### 3.3 Other dashboard items unchanged

Week strip, benchmark banner, KPI grid layout, "Stretch Daily" headline,
and `"$count exercises · ~$minutes minutes · mixed full body"` sub-line all
stay as-is.

---

## 4. Session overview — swap explainer

The design's `screens.jsx:250-253` specifies an inline-icon subhead between
the headline and the exercise list:

> *Tap any exercise to see cues, or [swap-icon] to swap for another in the same category.*

Currently missing in `SessionOverviewScreen.kt`. Insert as a new `item { }`
in the `LazyColumn` between the headline (line 72-95) and the
`CategorySpreadBar` (line 96).

**Implementation — inline icon via `AnnotatedString`:**

```kotlin
val swapId = "swap"
val annotated = buildAnnotatedString {
    append("Tap any exercise to see cues, or ")
    appendInlineContent(swapId, "[swap]")
    append(" to swap for another in the same category.")
}
val inline = mapOf(
    swapId to InlineTextContent(
        Placeholder(
            width = 14.sp,
            height = 14.sp,
            placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
        ),
    ) { AppIcon(IconName.Swap, contentDescription = null, tint = Theme.colors.ink2) }
)
Text(
    text = annotated,
    inlineContent = inline,
    style = Theme.typo.bodySm,
    color = Theme.colors.ink2,
)
```

**Why `inlineContent` over `Row { Text + Icon + Text }`:** the row variant
breaks line-wrapping — if available width forces a break, the icon ends up
orphaned on its own line. `inlineContent` keeps the icon in-flow with the
surrounding text and breaks land on word boundaries.

**Spacing:** the LazyColumn's existing `Arrangement.spacedBy(Theme.dims.gapSection)`
provides the gap above and below. No extra spacers.

---

## 5. Session player

Five interconnected changes — the largest single screen overhaul.

### 5.1 Progress bar visibility (root cause of "it's missing")

The progress bar is wired at
`SessionPlayerScreen.kt:96-100`, but at 3 dp tall with Future segments using
`Theme.colors.line` (12% alpha), it's nearly invisible against the page bg.
That's why the user reads it as "below the X and not present" — the bar is
there but the eye doesn't catch it.

**Fix in `app/src/main/java/com/stretchdaily/app/ui/components/SegmentProgress.kt`:**

```diff
- Row(... modifier.fillMaxWidth().height(3.dp)) {
+ Row(... modifier.fillMaxWidth().height(4.dp)) {
```

```diff
SegmentState.Future -> Theme.colors.line
+                    Theme.colors.ink.copy(alpha = 0.18f)
```

Global change — also benefits the carousel where the same component is used.

### 5.2 Manual Start — state-machine refactor

Replace the `isPaused: Boolean` field on `SessionPlayerUiState.Running` with
an enum `phase: TimerPhase` taking three values: `READY`, `RUNNING`, `PAUSED`.

**Behavior per phase (timed exercises):**

| Phase | Timer | Caption | Pill |
|---|---|---|---|
| `READY` | static at full duration | `"READY WHEN YOU ARE"` | `"▶ Start"` |
| `RUNNING` | counting down | `"HOLD THE POSITION"` | `"Pause"` |
| `PAUSED` | frozen mid-count | `"PAUSED"` | `"Resume"` |

Non-timed (rep-based) exercises stay equivalent to "always READY" — rep count
+ `"TAP NEXT WHEN COMPLETE"` + dashed `"Perform at your own pace"` pill.
The existing `isTimed` branch in `TimerArea` and `ControlRow` already handles
the divergence cleanly; the new phase enum just gates the pill label.

**ViewModel transitions:**

- `init` → first phase's `READY`. **Delete the `audioPlayer.playStart()` call
  from `SessionPlayerViewModel.kt:61`.** Session entry is silent.
- New `start()` action — `READY → RUNNING`, kicks off the 1 Hz timer coroutine.
- `pause()` — `RUNNING → PAUSED`. `resume()` — `PAUSED → RUNNING`.
- `tick()` at zero — play chime, advance to next phase's `READY`.
  End-of-session → `Complete`.
- `next()` / `prev()` — advance silently to target phase's `READY`.
  **No chime, no auto-Run.**

**Migrating callsites:** the existing single `togglePause()` callsite splits
into `start()`, `pause()`, `resume()` exposed by the VM; the screen branches
on `state.phase` to wire the pill's `onClick` to the right handler.

**Per-side Start for unilateral exercises (per Q5 confirmation):** when a
unilateral exercise's LEFT phase finishes (or is advanced via Next), the
RIGHT phase loads in `READY` requiring its own Start tap. Same pattern as
exercise-to-exercise transitions, just within a single exercise. Symmetric
on `prev()`: from RIGHT, prev goes to LEFT in `READY`.

### 5.3 Side pill — category-tinted

Replace the existing plain-text `SideChip` (line 215-222 of
`SessionPlayerScreen.kt`) with a real pill that uses `category.tint()` as
its background fill when active:

```kotlin
@Composable
private fun SideChip(label: String, active: Boolean, tint: Color) {
    val bg = if (active) tint.copy(alpha = 0.32f) else Color.Transparent
    val ink = if (active) Theme.colors.ink else Theme.colors.ink3
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Theme.dims.radiusPill))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        MonoCaps(text = label, color = ink, size = MonoCapsSize.Regular)
    }
}
```

`tint` comes from the current exercise's `category.tint()`. The "—"
separator stays as-is. Call site updates: pass
`tint = state.currentItem.exercise.category.tint()` to both sides.

### 5.4 Beep timing — single source

Currently `audioPlayer.playStart()` fires inside `advance()` at
`SessionPlayerViewModel.kt:148`, which is why both timer-end *and* Next-button
tap produce a beep. Fix:

- **Delete** `audioPlayer.playStart()` from inside `advance()` (line 148).
- **Delete** `audioPlayer.playStart()` from the init block (line 61).
- **Add** `audioPlayer.playEnd()` inside `tick()`, fired exactly once when
  `remainingSeconds` hits 0, *before* the phase transition.
- **Delete** `audioPlayer.playEnd()` from inside `finish()` (line 163).
  Don't replace it — the last timed phase's `tick()` already fires the chime
  before transitioning to `Complete`.

Net effect: one chime per timer-phase-end (including the final phase).
Manual `start()` / `pause()` / `resume()` / `next()` / `prev()` are silent.
Edge cases:
- **Last phase tick-ends naturally** → chime once via `tick()`, then `Complete`.
  No duplicate from `finish()`.
- **Last exercise is rep-based and user taps Next to finish** → silent
  completion (no `tick=0` event ever fires). Acceptable: consistent with
  silent Next behavior elsewhere; the user's "session complete" cue is the
  visual transition to the Complete screen.

**`SessionAudioPlayer` itself doesn't change** — only the call sites move.

### 5.5 Tests to update / add

`SessionPlayerViewModelTest` currently asserts the old `isPaused` boolean
and the old advance behavior. Update:

- Phase transitions: starting in `READY`, `start()` → `RUNNING`,
  `tick(N)` to zero → `playEnd` called once → next phase in `READY`.
- `next()` from `READY`: advances silently, no `playEnd` call.
- `pause()` / `resume()` round-trip in `RUNNING`.
- Unilateral: LEFT phase ends → RIGHT phase loads `READY` (one Start required
  for the right side per Q5).
- `prev()` from `RIGHT, READY` → `LEFT, READY` of same exercise; from
  `LEFT, READY` → previous exercise's terminal side, `READY`.

---

## 6. Carousel + Log tab cleanup

Combined because they share concerns (sparkline removal, shared `BandsList`
component).

### 6.1 Carousel progress bar

Already wired at `BenchmarkCarouselScreen.kt:135` — the same `SegmentProgress`
that gets the global visibility bump from §5.1. **No new code in the carousel
for this** — the height + alpha tweak inside `SegmentProgress.kt` lights it
up automatically.

### 6.2 Carousel category context — promote `BandsList` to a shared component

Currently `BandsList` is a `private @Composable` inside `BenchmarkLogScreen.kt`
(lines 386-421). Move it to
`app/src/main/java/com/stretchdaily/app/ui/components/BandsList.kt` with a
public signature:

```kotlin
@Composable
fun BandsList(
    benchmark: Benchmark,
    highlightTier: FlexibilityTier?,   // null = no highlight (carousel pre-log state)
    modifier: Modifier = Modifier,
)
```

The carousel inserts it inside the LazyColumn item, between description and
`LogForm`. Layout becomes:

```
Title
Description
[NEW] MonoCaps "BANDS"
[NEW] BandsList(benchmark = bm, highlightTier = null)
LogForm(...)
```

`highlightTier = null` because in the carousel the user is *about to* log a
value — there's no "current band" yet. All five tier rows render normally;
nothing bolded. The Log-tab expanded card continues to pass
`highlightTier = row.latestTier`.

### 6.3 Log tab — remove the sparkline

- **Delete** the `Sparkline(values = row.sparkline)` call at
  `BenchmarkLogScreen.kt:343` (the collapsed-row line).
- **Delete** `app/src/main/java/com/stretchdaily/app/ui/screen/log/LogSparkline.kt`
  and its associated unit test (`LogSparklineTest.kt` if present).
- **Remove** the `sparkline: List<Float>` field from `BenchmarkRowUiState`
  and stop populating it in `BenchmarkLogViewModel`.

The underlying `BenchmarkProgressBuilder` stays — Analytics (§7) still uses it.
The shared `Sparkline.kt` primitive also stays (still referenced by the debug
`ComponentGalleryScreen`); removing it from the gallery is out of scope.

### 6.4 LogForm description deduplication

`BenchmarkCarouselScreen.kt:92-100` already renders the description, and
`LogForm.kt:49-56` renders it again — the description currently shows twice
in the carousel. Drop the description block from `LogForm`. The carousel
and the Log-tab expanded card both render it themselves.

---

## 7. Progress page redesign

The largest single screen rewrite.

### 7.1 Title + subhead

`AnalyticsScreen.kt:84` → change `"Six months in."` to `"Your trajectory."`
Sub stays unchanged: `"Each chart is one benchmark. The colored strip marks
its category."`

### 7.2 Filter chip ordering — body-top-to-bottom

`AnalyticsViewModel.kt:71` currently:

```kotlin
allCategories = benchmarks.map { it.category }.distinct(),
```

This preserves whatever order the seeder produced. Replace with enum-order
filter:

```kotlin
allCategories = Category.values().filter { c -> benchmarks.any { it.category == c } },
```

`Category.values()` is already in body-top-to-bottom order
(NECK / SHOULDERS / WRISTS / SPINE / HIPS / KNEES / ANKLES — see `Category.kt`),
matching the Log-tab grouping order.

### 7.3 ProgressSeries refactor — raw values

`BenchmarkProgressBuilder.kt` currently maps tier → 0..1 Y. New shape:

```kotlin
data class ProgressPoint(
    val xRatio: Float,         // 0..1 across whole-history time span (unchanged semantics)
    val rawValue: Float?,      // numeric raw value; null for categorical (ATG)
    val tier: FlexibilityTier, // still derived, used for chart legend / categorical fallback
    val timestampMillis: Long, // unchanged
)

data class ProgressSeries(
    val points: List<ProgressPoint>,
    val isCategorical: Boolean,
    val rawMin: Float,         // y-axis bottom (5% padding below data min)
    val rawMax: Float,         // y-axis top (5% padding above data max)
    val yTickLabels: List<String>, // 3 entries — max, mid, min — formatted to 1 decimal
    val firstMonth: YearMonth, // x-axis start — first log's month
    val lastMonth: YearMonth,  // x-axis end — most recent log's month (or today)
)
```

**Build signature changes from `build(logs)` → `build(benchmark, logs)`** so
the builder knows the input type and unit.

For categorical benchmarks (`ATG Split Squat` only — `inputType = CATEGORICAL`):
set `isCategorical = true`, `rawValue = null`, fall back to existing tier-based
Y mapping. Rendering for that one benchmark stays identical to current
behavior.

For DESCENDING-direction numeric benchmarks (Apley, Sit and Reach, Thomas,
Butterfly — see `BenchmarkBetter.kt`): keep raw value on the Y axis as-is.
The "improved/regressed" semantics live in `BenchmarkDelta` and surface via
the delta chip — the chart plots reality.

**Padding formula:** `rawMin = dataMin - (dataMax - dataMin) * 0.05`,
`rawMax = dataMax + (dataMax - dataMin) * 0.05`. For single-point edge cases:
fall back to ±5% around the value, or ±0.5 if value is 0.

### 7.4 BigChart rewrite — scrollable, tinted, dotted, month-labeled

Replace `app/src/main/java/com/stretchdaily/app/ui/components/BigChart.kt`
end-to-end. New structure:

```
Row [card content]
├── Column (left gutter, 56dp wide, fixed — does NOT scroll)
│     Three MonoCaps Y-tick labels: max / mid / min
│     (from series.yTickLabels — e.g. "75.3" / "67.6" / "59.8")
│
└── Box (scrollable horizontally; scrollState.scrollTo(maxValue) on first layout)
      Column inside scrollable area:
      ├── Canvas
      │     Width = monthsBetween(firstMonth, lastMonth) * 60.dp
      │     Height = 130.dp
      │     • Background: category.tint() at alpha 0.18 (tinted wash)
      │     • Two faint horizontal grid lines at 1/3 and 2/3 (line2)
      │     • Polyline through all points: 2.dp accent stroke, round cap
      │     • Filled area below polyline: accent @ alpha 0.12
      │     • Dot at EVERY logged point: 4.dp accent + 2.dp bg outline ring
      │       (replaces current "only last dot" rendering)
      │
      └── MonthAxisRow (just below the Canvas, same width)
            One Column per month — width = 60.dp each.
            MonoCapsSize.Small inside each column:
              • Every other month from chart start (indexes 0, 2, 4, ...) —
                show month abbreviation ("JAN", "MAR", ...).
              • EVERY January — always show its month label AND a "'25" /
                "'26" year line underneath, regardless of index parity.
                (The year is the anchor; January overrides the every-other
                rule when they disagree.)
              • All other months — empty column (preserves alignment).
            Pixel positions of chart values align with month label centers
            because both use the same `monthsTotal * 60.dp` total width.
```

**Default scroll position = end (most recent in view).** Use
`rememberScrollState()` + `LaunchedEffect(scrollState.maxValue) { scrollState.scrollTo(scrollState.maxValue) }`
so the scroll lands at the right edge once the chart's content width is known.

**Y-axis labels stay outside the scroll region** — they're computed from the
data range as a whole and remain visually fixed as the user scrolls
horizontally.

**Categorical benchmark rendering (fallback for ATG Split Squat):** Y-tick
labels become tier names (`"VERY FLEXIBLE"`, `"AVERAGE"`, `"STIFF"` —
matching current behavior); chart still scrolls horizontally and renders
dots at each log; the tier-based Y mapping preserves backwards-compatible
visualization for that one benchmark.

### 7.5 Card layout — chart background bleeds tint inside the chart only

`BenchmarkAnalyticsCard.kt` currently wraps the chart in a card with
`surface` background and a category-color strip at the top. Both stay.
The new chart's tinted Canvas wash sits *inside* the chart's drawing area —
the surrounding card stays surface-colored. Net effect: each card shows its
category color in two places (top strip + chart wash) without the whole
card going colored.

### 7.6 Tests

New / updated unit tests for `BenchmarkProgressBuilder`:

- Empty logs → empty series, `rawMin = rawMax = 0`.
- Single log → centered `xRatio = 0.5`, `rawMin/rawMax` framed around the
  single value with 5% padding.
- Multi-log numeric → ordered chronologically, raw values preserved,
  `yTickLabels` computed at max/mid/min with 1-decimal formatting.
- Categorical input type → `isCategorical = true`, `rawValue = null`, tier-based
  Y still works.
- Direction-aware sanity: build for Apley (DESCENDING) and Cervical (ASCENDING),
  verify raw values are stored unmodified in both.

`AnalyticsViewModelTest` updates: filter ordering matches `Category.values()`.

---

## 8. Dashboard play button → bottom nav fix

**Symptom (confirmed Scenario A):** play button on dashboard navigates to
`session/overview`, bottom nav remains visible, but tapping Today on the
nav bar does not return to the dashboard.

**Diagnosis:** `popUpTo(graph.findStartDestination().id) { saveState = true }`
in `navigateToTab` (`StretchDailyNavHost.kt:199`) can resolve to a nested
graph's start destination rather than the outer NavHost's root in some
Compose Navigation configurations. When that happens, popUpTo becomes a
no-op and the subsequent `navigate("today")` adds a new entry on top of the
session graph rather than popping cleanly back.

**Fix — single line:**

```diff
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
-       popUpTo(graph.findStartDestination().id) { saveState = true }
+       popUpTo(Routes.TODAY) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
```

Pinning the popUpTo target explicitly removes the ambiguity. Same fix is
correct for every tab tap because the outer NavHost's start destination
*is* `Routes.TODAY` — there's only one true root.

`saveState = true` and `restoreState = true` stay — they power the
"switching tabs preserves scroll position" behavior. With the explicit
popUpTo target, save/restore should now work as documented.

`BOTTOM_NAV_ROUTES` stays unchanged — Scenario B (bar disappearing) was
ruled out, so the route set is correct as-is.

**Verification (Ramon, on device):** Today → tap play → land on overview,
bar visible → tap Today → back on dashboard with state restored.
Then repeat for Log/Progress/Settings tabs from session/overview to confirm
the fix is uniform across all five tabs.

---

## 9. Implementation strategy & verification

### 9.1 Branch + PR shape

Branch off `development` as `redesign/r7-polish`. One PR matching the R1–R6
pattern, single squashed merge. Rebase onto current `development` after R6
([#15](https://github.com/RvanSchalm/stretch-daily/pull/15)) lands.

### 9.2 Internal sequencing

| Order hint | Why |
|---|---|
| Theme bg (§2) **first** | Touches no other code; verifies palette before everything else changes visually. |
| `SegmentProgress` change (§5.1) **early** | Lights up player + carousel progress bar simultaneously. |
| `BandsList` extraction (§6.2) **before** carousel work | Carousel imports the moved-out shared component. |
| Player state-machine refactor (§5.2) is the **biggest single block** | VM + screen + tests; one focused commit so the diff reads cleanly. |
| `BigChart` rewrite (§7.4) is the **second-biggest block** | Replaces the chart end-to-end; ProgressSeries shape changes too. Own commit. |

Everything else (icons, dates, swap subhead, chip ordering, sparkline removal,
nav fix) is small and order-independent.

### 9.3 Verification path — path C confirmed

**Claude cannot run Gradle on this Windows machine** due to the documented
AF_UNIX issue (CLAUDE.md §2). The implementer commits code believed correct
based on review and marks the PR description with
*"compilation unverified — Ramon to verify in Studio."* If Studio reports a
compile error, the implementer fixes and re-pushes.

JVM unit tests (`SessionPlayerViewModelTest`, `BenchmarkProgressBuilderTest`,
`AnalyticsViewModelTest`) get written/updated as part of the implementation.
They live in `app/src/test/` and use plain JUnit + Coroutines test. The
implementer writes them; Ramon runs `./gradlew test` from Android Studio
(which works — only the daemon-spawning CLI is broken) to confirm they pass.

### 9.4 Smoke-test checklist (Ramon, post-build)

- [ ] Dashboard: bg is darker than cards; date reads `WEDNESDAY, MAY 6` (or
      current); KPI cards show flame / clock / check / calendar icons.
- [ ] Tap play → overview shows swap subhead with inline icon.
- [ ] Tap Today on bar from overview → dashboard restored.
- [ ] Begin session → timer exercise loads with full `00:05` static + Start
      button; tap Start, timer runs; let one phase finish — single chime
      fires; next phase loads in Ready.
- [ ] Tap Next mid-Ready or mid-Running — silent advance, no chime.
- [ ] Unilateral exercise — LEFT side pill is category-tinted, RIGHT is dim;
      complete LEFT, RIGHT side pill goes ready, tap Start again.
- [ ] Player progress bar visible at top alongside X close; segments fill
      as you advance.
- [ ] Open Log tab → no sparklines on collapsed rows.
- [ ] Tap Start benchmark day → carousel shows visible progress bar +
      Bands section above the input.
- [ ] Progress tab: title reads `"Your trajectory."`; filter chips order is
      NECK / SHOULDERS / WRISTS / SPINE / HIPS / KNEES / ANKLES; charts have
      category-tinted bg, raw-value Y axis, dot at every log point,
      scrollable horizontally with default scroll-to-end, month labels with
      year-under-January.

### 9.5 Risk areas to flag during review

- **Player state machine** — the `READY/RUNNING/PAUSED` enum touches the
  most cross-cutting code (VM + screen + tests). Easy to introduce
  regressions in unilateral handling. Tests must cover LEFT→RIGHT handoff
  explicitly.
- **`BigChart` raw-value Y axis** — needs careful handling of
  DESCENDING-direction benchmarks (Apley etc.) where lower-is-better. Data is
  plotted as-is; the implementer must verify the delta chip and chart agree
  directionally on what counts as improvement.
- **Scroll-to-end default** — if a user has many months of log history, the
  chart is wide. Performance on a low-end device should be sanity-checked
  but Compose's lazy painting handles it; no special optimization needed
  up front.

---

## 10. Out of scope

Explicitly NOT in this PR:

- Real chime samples (still sine-wave placeholders).
- Exercise illustration assets (still striped placeholders).
- Localized Dutch strings.
- Removing `Sparkline.kt` primitive entirely (still used by `ComponentGalleryScreen`).
- Anything in CLAUDE.md §2 "Next up" post-redesign list.
- Any change to `SessionAudioPlayer` itself (only call sites move in §5.4).
- Any change to `BenchmarkBetter.kt`, `BenchmarkDelta.kt`, the delta chip
  rendering, or the analytics card's category-strip top header.

---

## Appendix A — File-touch summary

Files modified or added by R7:

```
app/src/main/java/com/stretchdaily/app/
├── ui/theme/AppColors.kt                                # §2 — bg revert
├── ui/components/AppIcon.kt                             # §3.2 — IconName.Clock + Calendar
├── ui/components/SegmentProgress.kt                     # §5.1 — height + alpha
├── ui/components/BandsList.kt                           # §6.2 — NEW (moved from BenchmarkLogScreen)
├── ui/components/BigChart.kt                            # §7.4 — full rewrite
├── ui/navigation/StretchDailyNavHost.kt                 # §8 — popUpTo target
├── ui/screen/dashboard/DashboardScreen.kt               # §3.1 + §3.2 — date + icons
├── ui/screen/session/SessionOverviewScreen.kt           # §4 — swap subhead
├── ui/screen/session/SessionPlayerViewModel.kt          # §5.2 + §5.4 — phase enum + chime
├── ui/screen/session/SessionPlayerScreen.kt             # §5.2 + §5.3 — phase wiring + side pill
├── ui/screen/session/SessionPlayerUiState.kt            # §5.2 — phase enum
├── ui/screen/log/BenchmarkLogScreen.kt                  # §6.3 — remove Sparkline; §6.2 — BandsList moved out
├── ui/screen/log/LogForm.kt                             # §6.4 — drop description block
├── ui/screen/log/BenchmarkLogUiState.kt                 # §6.3 — drop sparkline field
├── ui/screen/log/BenchmarkLogViewModel.kt               # §6.3 — stop populating sparkline
├── ui/screen/log/LogSparkline.kt                        # §6.3 — DELETE
├── ui/screen/carousel/BenchmarkCarouselScreen.kt        # §6.2 — render BandsList
├── ui/screen/analytics/AnalyticsScreen.kt               # §7.1 — title
├── ui/screen/analytics/AnalyticsViewModel.kt            # §7.2 — filter ordering
├── ui/screen/analytics/BenchmarkAnalyticsCard.kt        # §7.4 — chart layout (Y gutter + scrollable)
└── core/benchmark/BenchmarkProgressBuilder.kt           # §7.3 — raw values + new ProgressSeries shape

app/src/main/res/drawable/
├── ic_clock.xml                                         # §3.2 — NEW
└── ic_calendar.xml                                      # §3.2 — NEW

app/src/test/java/com/stretchdaily/app/
├── ui/screen/session/SessionPlayerViewModelTest.kt      # §5.5 — phase enum
├── core/benchmark/BenchmarkProgressBuilderTest.kt       # §7.6 — raw values
├── ui/screen/analytics/AnalyticsViewModelTest.kt        # §7.6 — filter order
└── ui/screen/log/LogSparklineTest.kt                    # §6.3 — DELETE if present
```

That's roughly 22 source files modified, 3 added, 1–2 deleted.
