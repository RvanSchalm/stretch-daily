# CLAUDE.md — Stretch Daily

This file is the handoff document between Claude Code sessions. Update the
**Current state** section at the end of every session.

---

## 1. Project overview

Stretch Daily is a native Android app (Kotlin / Jetpack Compose) that gives
the user a personalized 10–15 minute mobility + tendon-resilience routine
each day. The "Longevity Engine" weights exercise selection toward the body
areas where the user's monthly benchmarks indicate the most stiffness.

- **Package**: `com.stretchdaily.app`
- **Tech**: Kotlin 2.0, Jetpack Compose (BOM 2024.10), Room 2.6, Hilt 2.52,
  KSP, Compose Navigation, DataStore, kotlinx.serialization, Coil
- **Min SDK**: 30 — **Target/Compile SDK**: 35
- **Java target**: 17 (using JDK 21 bundled with Android Studio)

---

## 2. Current state

**Last updated**: 2026-04-25 (end of Sage redesign R6 session)
**Active branch**: `redesign/r6-analytics-settings-polish`
(PR [#15](https://github.com/RvanSchalm/stretch-daily/pull/15) → `development`)

**Just completed**: Sage redesign R6 — Analytics + Settings + icon/splash
polish. Final phase of the 6-part redesign that began with R1
(tokens + scaffold) on 2026-04-23.

- **Analytics tab** (`ui/screen/analytics/`): per-benchmark `BigChart`
  cards with category-tint top strip, latest raw value, 6-month delta
  chip, and a "All / category" filter row. Two new pure-Kotlin helpers
  power it: `core/benchmark/BenchmarkBetter.kt` (direction lookup —
  ASCENDING / DESCENDING / categorical) and
  `core/benchmark/BenchmarkDelta.kt` (6-month nearest-date delta math).
- **Settings tab** (`ui/screen/settings/`): grouped Preferences /
  Your data / Library layout. Audio cue toggle + new
  `benchmarkBannerEnabled` toggle, both backed by `SettingsDataStore`.
  Library group reactively shows live exercise / category / benchmark /
  session counts via `combine(...).stateIn(Eagerly)`. Export / import /
  delete-all unchanged from the Phase 7 `DataPortRepository`.
- **Launcher icon + splash**: Repainted the figure from
  `#FF8C00`/`#0D0D0D` (legacy orange + dark) to `#5c7a4a`/`#f5f3ea`
  (sage + cream). Splash uses a dedicated `ic_splash_foreground`
  drawable so the AndroidX SplashScreen API renders the figure on the
  cream background.
- **Accessibility audit (cross-screen)**: every redesigned screen's
  hero title now applies `Modifier.semantics { heading() }` —
  Dashboard, Session overview, Session complete, Benchmark log,
  Benchmark carousel, Analytics, Settings.
- **R8 keep-rules**: reviewed; the legacy Phase 8 keep-rules cover
  every R6 type already (no new `@Serializable` classes outside
  `ExportPayload`, no new Hilt-generated components, no new Room
  entities). No edits required.

**In-session compile fixes**: `Theme.typo.display` / `Theme.typo.body`
references in the plan were wrong — `AppTypography` exposes
`displayLg/Md/Xl` and `bodyLg/Md/Sm` `TextStyle`s; corrected via
`.fontFamily` accessor on the existing styles.
`AnalyticsViewModel`'s `ZoneId` constructor parameter needed an
explicit `@Provides` binding in `EngineModule` (Hilt ignores Kotlin
default values). Test sources used a non-existent `Category.HAMSTRINGS`
(swapped to `HIPS`) and `UnconfinedTestDispatcher` as a *type*
(it's a factory function — type annotation must be `TestDispatcher`).
Status-bar overlap on Analytics + Settings tabs fixed by adding
`.statusBarsPadding()` to the root LazyColumn / Column, matching the
Dashboard pattern (the outer Scaffold uses `contentWindowInsets =
WindowInsets(0)` so each tab owns its top inset).

**Pending before merge**:
- Task 14 — pixel QA matrix vs.
  `docs/design_handoff_stretch_daily_v3/reference/index.html`.
  Ramon-led, requires emulator + browser side-by-side.
- Task 15 — 15-step end-to-end smoke test (install → session → log →
  carousel → export → delete → import → orientation). Ramon-led.

**Next up**: After PR #15 lands on `development`, the Sage redesign
is complete. Post-redesign follow-ups (none of which are scoped now):
1. Real chime samples replacing the sine-wave placeholders.
2. Exercise illustration assets (WebP/Lottie) wired via Coil.
3. Release keystore + signed AAB.
4. Instrumented tests on a real device/emulator.
5. Play Store listing + metadata.

**Known issues**:
- **Gradle CLI build blocked on this Windows machine — no JDK-side fix
  exists.** Any `./gradlew` invocation that needs a Selector (i.e. anything
  that starts a daemon, which is every real task) fails with
  `java.io.IOException: Unable to establish loopback connection` →
  `SocketException: Invalid argument: connect` inside
  `UnixDomainSockets.connect0`.
  - **Root cause**: `sun.nio.ch.PipeImpl` (used by *every* Windows Selector
    implementation — both `WEPollSelectorImpl` **and** the legacy
    `WindowsSelectorImpl`) constructs its wakeup pipe by calling
    `createListener(preferUnixDomain = true)` and then opening a client
    `SocketChannel` against it. `createListener()` will fall back from UDS
    to TCP on listener bind failure, but there is **no fallback** if the
    client `connect()` fails — so `UnixDomainSockets.connect0` throws
    `SocketException: Invalid argument: connect` and pipe init dies. On
    Ramon's Windows 11 24H2 Enterprise machine the UDS *listener bind*
    succeeds but the *client connect* fails at the kernel level — almost
    certainly corporate security software (NinjaOne / Citrix) intercepting
    AF_UNIX sockets. Reproducible with a 3-line `Selector.open()` program.
  - **Verified in JDK 17, JDK 21, and JDK 25 source** (extracted from each
    JDK's `lib/src.zip`). The UDS code path is present in all three — the
    earlier theory that "JDK 17 uses TCP loopback only" was wrong: both
    `WEPollSelectorImpl` (JDK 17+ default) and `WindowsSelectorImpl` (the
    legacy provider still shipped in JDK 17+) call `new PipeImpl(sp, /*
    AF_UNIX */ true, ...)`. There is no JDK 17/21/25 that avoids UDS in
    `PipeImpl`.
  - **Tested and failed (2026-04-07)**: JBR 21 (Android Studio bundled),
    Microsoft OpenJDK 17.0.18+8 LTS, Microsoft OpenJDK 25.0.2+10 LTS, with
    `--no-daemon`, removing `org.gradle.jvmargs`, `GRADLE_OPTS`,
    `-Djava.nio.channels.spi.SelectorProvider=sun.nio.ch.WindowsSelectorProvider`
    (forces the legacy provider — stack moves to `WindowsSelectorImpl` but
    still hits UDS), `-Dsun.nio.ch.defaultProvider=...`,
    `JAVA_TOOL_OPTIONS=-Djava.net.preferIPv4Stack=true`, custom
    `java.io.tmpdir`, run from bash and `cmd.exe`, Bash tool sandbox
    disabled. All fail identically at `UnixDomainSockets.connect0`. Gradle
    `--version` is the only task that "works" because the launcher JVM
    never opens a Selector.
  - **Workaround that works**: build from Android Studio's UI
    (Build → Make Project). Its internal Gradle Tooling API integration
    uses a different IPC path that bypasses the failing socket.
  - **True fixes** (none attempted yet, not required for development):
    1. Ask IT to whitelist / reconfigure the UDS-intercepting security
       software.
    2. Run Gradle inside WSL2 where UDS goes through Linux, not the
       Windows security filter driver.
    3. Patch / shim `sun.nio.ch.PipeImpl` to force TCP — invasive,
       not recommended.
  - **Operational rule for Claude sessions**: do NOT spend turn budget
    retrying gradle CLI workarounds. Either ask Ramon to build from Android
    Studio and report back, or restrict verification to manual code review
    and tell Ramon that compilation is unverified.
- All R1–R6 code compiled successfully via **Android Studio
  (Build → Make Project)** during the redesign sessions. Gradle CLI
  verification is still blocked by the issue above; Android Studio
  remains the verification path going forward.

---

## 3. Architecture summary

- **MVVM**: ViewModel + StateFlow (no LiveData). UI is 100% Compose.
- **DI**: Hilt. `SingletonComponent` modules: `DatabaseModule`
  (database, DAOs, ApplicationScope), `EngineModule` (Clock + ZoneId
  bindings), `SessionModule`. All engine, repository, holder, and
  ViewModel classes use `@Inject constructor`. ViewModels are
  `@HiltViewModel`. **Hilt does not honour Kotlin default values** —
  any constructor parameter that isn't another `@Inject`-able type
  needs an explicit `@Provides`. `EngineModule.provideZoneId()` is
  the example for `java.time.ZoneId`.
- **Database**: Room with KSP. Seeded once at file creation via
  `RoomDatabase.Callback.onCreate` running on `Dispatchers.IO` inside
  the injected `@ApplicationScope` coroutine scope.
- **Engine**: `core/engine/`. The three components
  (`CategoryWeightCalculator`, `SelectionShield`, `SessionBuilder`)
  are pure Kotlin — no Android, no Room, no coroutines — so they're
  tested via plain JVM JUnit. `LongevityEngine` is the thin
  Android-aware orchestrator that actually talks to Room and is the
  only public entry point. Tests inject a fake `Clock` and a seeded
  `Random` for determinism.
- **Today's session cache**: `core/session/TodaySessionHolder` is the
  `@Singleton` in-memory cache for today's plan. Regenerates only on
  calendar-day rollover; user completion does NOT reset it (the user
  can redo today's session). `Dashboard` and `SessionOverview`
  ViewModels both observe its `StateFlow<SessionPlan?>` so the swap
  on the overview screen reflects on the dashboard immediately. Swaps
  are not persisted — accepted tradeoff.
- **Repositories**: `data/`. Wrap DAOs so ViewModels never see Room.
  `BenchmarkRepository` exposes Flow-based observers
  (`observeAllBenchmarks`, `observeLogsFor`,
  `observeLatestPerBenchmark`) so log mutations propagate live.
  `SessionRepository` exposes `observeAllSessions` plus pure-logic
  helpers (`computeStreak`, etc.) as `internal` companion functions
  testable on the JVM without a database.
- **Session player**: `SessionPlayerViewModel` is scoped to the
  nested `SESSION_GRAPH` NavBackStackEntry via
  `hiltViewModel(parentEntry)`. The 1 Hz timer is a `delay`-based
  coroutine inside `viewModelScope`; `tick()` is `internal` so unit
  tests can drive it without a real dispatcher. `SessionAudioPlayer`
  (injected) plays start/end chimes gated by the audio-cues
  preference in DataStore.
- **Navigation**: Compose Navigation with string routes. The
  `NavHost` lives inside an outer `Scaffold` that owns a Material3
  `NavigationBar` with five tabs (Today / Session / Log / Progress /
  Settings). The outer Scaffold sets `contentWindowInsets =
  WindowInsets(0)` so each tab consumes its own status-bar inset
  via `.statusBarsPadding()` — only the bottom-nav height bubbles
  down via a `contentPadding` parameter. Two nested `navigation(...)`
  graphs:
  - `SESSION_GRAPH` (`session/overview` → `session/player` →
    `session/complete`) shares one `SessionPlayerViewModel`.
  - `CAROUSEL_GRAPH` (`carousel/{step}`) shares one
    `BenchmarkCarouselViewModel` across all 10 steps.

  `session/player`, `session/complete`, and `carousel/{step}` are
  immersive overlays — bottom nav hidden, controlled by route lists in
  `StretchDailyNavHost.kt`.
- **Theme**: Sage palette tokens live in `ui/theme/`:
  `AppColors` (sageColors() — bg `#fbf8f0`, accent `#5c7a4a`,
  cream `#f5f3ea`, warn `#a6632a`), `AppTypography`
  (`displayXl/Lg/Md` + `bodyLg/Md/Sm` + `monoCaps[/Sm]`), `AppDimens`
  (radii, gaps, `padScreen` with bottom 100.dp baked in for nav
  clearance), `CategoryTint` (per-category accent stripe colour).
  Access at call sites via `Theme.colors`, `Theme.typo`, `Theme.dims`.
- **Component primitives**: `ui/components/` holds the 12 reusable
  Compose primitives that R2 introduced — `BigChart`, `Sparkline`,
  `KpiCard`, `MonoCaps`, `Pill`, `BandPill`, `CatChip`,
  `ExerciseTile`, `Sheet`, `WeekStrip`, `SegmentProgress`,
  `AppIcon`. Every screen composes from this set; no screen ships its
  own card / chip / pill drawables.
- **Dashboard**: `DashboardViewModel` joins date, streak, today's
  plan (from `TodaySessionHolder`), week-strip data, KPI snapshot,
  and the benchmark banner state into one `DashboardUiState`. The
  banner shows only when `SettingsDataStore.benchmarkBannerEnabled`
  is true *and* `BenchmarkRepository.isBenchmarksDue()` returns
  overdue rows.
- **Benchmark log + carousel**: `BenchmarkLogViewModel` powers the
  Log tab — grouped sections per category, expandable rows showing
  description / bands / sparkline / history, sheet-based numeric or
  categorical input. `BenchmarkCarouselViewModel` powers the 10-step
  carousel reached from the dashboard banner; both write through
  `BenchmarkRepository`. `core/benchmark/BenchmarkProgressBuilder`
  produces the per-benchmark sparkline / history series. Five
  benchmarks are ASCENDING (higher = more flexible), four are
  DESCENDING (lower = more flexible — Apley, Butterfly, Sit and
  Reach, Thomas), and the one categorical benchmark (ATG Split Squat)
  is tier-picked directly. Direction lookup is centralized in
  `core/benchmark/BenchmarkBetter`.
- **Analytics (Progress tab)**: `AnalyticsViewModel` fans out one
  card per benchmark using `combine(repo.observeAllBenchmarks(),
  flatMap-of-observeLogsFor)`, plus an "All / category" filter.
  Each card resolves a 6-month delta via
  `core/benchmark/BenchmarkDelta` (nearest-date raw subtraction +
  direction-aware `improved` flag), then renders via
  `BenchmarkAnalyticsCard` (category-tint top strip + eyebrow +
  name + latest raw value + delta chip + 130dp `BigChart`).
- **Settings + data port**: `SettingsDataStore` (Preferences
  DataStore named `stretch_daily_settings`) holds the audio cues
  and benchmark-banner toggles. `DataPortRepository` is the
  schema-aware orchestrator for export / import / delete-all: it
  snapshots all 5 tables in FK order into a versioned
  `ExportPayload`, then re-seeds the catalog from `DatabaseSeeder`
  after a wipe. JSON IO is `kotlinx.serialization`
  `encodeToStream` / `decodeFromStream` — the Room entities
  themselves are `@Serializable`, no DTO layer. `SettingsViewModel`
  composes the live state with `combine(...).stateIn(Eagerly)` so
  toggle flips and library counts both surface immediately. A
  `SettingsStatus` sealed interface (Idle / Working / Success /
  Error) drives one-shot snackbar messages. Document picking
  happens in the screen via SAF
  `ActivityResultContracts.CreateDocument` / `OpenDocument`; the
  resulting `Uri`s are passed to the ViewModel, which uses
  `@ApplicationContext` to open streams.

---

## 4. Key conventions

- **Branch model**: `main` (protected) ← PRs ← `development` ← feature
  branches. One PR per phase. Conventional Commits (`feat:`, `fix:`,
  `chore:`, `docs:`). Push every commit immediately. See
  `.claude/skills/git-flow/SKILL.md`.
- **Theme**: Fixed Sage palette (light, warm cream). Background
  `#fbf8f0`, surface `#f9f6ec`, accent `#5c7a4a`, accent-ink `#f5f3ea`,
  warn `#a6632a`. No dynamic colors. Token data classes
  (`AppColors`/`AppTypography`/`AppDimens`/`CategoryTint`) are
  data-class shaped so an alternate palette (Grove / Moss / dark)
  can slot in via `StretchDailyTheme(colors = ...)` later.
- **Status-bar inset rule**: the outer Scaffold sets
  `contentWindowInsets = WindowInsets(0)`. Each tab is responsible
  for its own top inset — apply `.statusBarsPadding()` on the root
  scrollable. Bottom-nav clearance comes via the `contentPadding`
  parameter + `padScreen`'s baked-in 100.dp.
- **No backwards compatibility cruft**: Don't keep dead enum values,
  unused re-exports, or `// removed` comments. Just delete.
- **No speculative abstractions**: Inline a few similar lines instead
  of building an early helper. Add structure when the third use case
  shows up.
- **Comments**: Only where the logic isn't self-evident. Don't
  narrate code.

---

## 5. File map (critical files)

```
app/src/main/java/com/stretchdaily/app/
├── StretchDailyApp.kt              # @HiltAndroidApp Application
├── MainActivity.kt                 # @AndroidEntryPoint; hosts StretchDailyNavHost
├── core/
│   ├── model/                      # Category, FlexibilityTier, Benchmark, ...
│   │   ├── Category.kt             # NECK/SHOULDERS/WRISTS/SPINE/HIPS/KNEES/ANKLES
│   │   ├── FlexibilityTier.kt      # 5 tiers + weights
│   │   ├── BenchmarkInputType.kt   # NUMERIC | CATEGORICAL
│   │   ├── Exercise.kt             # @Entity
│   │   ├── Benchmark.kt            # @Entity (@Serializable)
│   │   ├── BenchmarkLog.kt         # @Entity FK→benchmarks (@Serializable)
│   │   ├── SessionRecord.kt        # @Entity (session header)
│   │   └── SessionExercise.kt      # @Entity FKs→session_records, exercises
│   ├── database/
│   │   ├── StretchDailyDatabase.kt # RoomDatabase + onCreate seeding
│   │   ├── DatabaseSeeder.kt       # 46 exercises + 10 benchmarks
│   │   ├── Converters.kt           # JSON for List/Map, enum name()
│   │   └── dao/                    # ExerciseDao, BenchmarkDao, BenchmarkLogDao,
│   │                               #   SessionDao
│   ├── engine/
│   │   ├── LongevityEngine.kt      # Public generateSession() — DAO-aware
│   │   ├── CategoryWeightCalculator.kt   # latest logs → Map<Category, Double>
│   │   ├── SelectionShield.kt      # exercises stale > 14d
│   │   ├── SessionBuilder.kt       # weighted random + 120s cap
│   │   └── model/SessionPlan.kt    # SessionPlan + PlannedExercise
│   ├── benchmark/
│   │   ├── TierResolver.kt         # numeric reading → FlexibilityTier
│   │   ├── BenchmarkProgressBuilder.kt   # logs → sparkline series
│   │   ├── BenchmarkBetter.kt      # ASCENDING / DESCENDING / categorical lookup
│   │   └── BenchmarkDelta.kt       # 6-month nearest-date raw delta
│   ├── session/
│   │   └── TodaySessionHolder.kt   # @Singleton — today's plan cache
│   ├── audio/
│   │   └── SessionAudioPlayer.kt   # SoundPool wrapper — start/end chimes
│   ├── datastore/
│   │   └── SettingsDataStore.kt    # audio cues + benchmark banner toggles
│   ├── util/Clock.kt               # fun interface { now(): Long }
│   └── di/
│       ├── DatabaseModule.kt       # database, DAOs, @ApplicationScope
│       ├── EngineModule.kt         # Clock + ZoneId
│       └── SessionModule.kt
├── data/
│   ├── BenchmarkRepository.kt      # observeAllBenchmarks/observeLogsFor +
│   │                               #   isBenchmarksDue
│   ├── SessionRepository.kt        # streak/weekly/total/lastAt +
│   │                               #   observeAllSessions
│   ├── DataPortRepository.kt       # export/import/delete-all + re-seed
│   └── export/ExportPayload.kt     # @Serializable snapshot of all 5 tables
└── ui/
    ├── theme/                      # AppColors (sage), AppTypography,
    │                               #   AppDimens, CategoryTint, Theme,
    │                               #   OklchToSrgb
    ├── navigation/
    │   └── StretchDailyNavHost.kt  # Routes + bottom nav + nested graphs
    │                               #   (SESSION_GRAPH, CAROUSEL_GRAPH)
    ├── components/                 # 12 reusable primitives:
    │                               #   AppIcon, BandPill, BigChart, CatChip,
    │                               #   ExerciseTile, KpiCard, MonoCaps, Pill,
    │                               #   SegmentProgress, Sheet, Sparkline,
    │                               #   WeekStrip
    ├── debug/ComponentGalleryScreen.kt   # R2 visual catalog
    └── screen/
        ├── dashboard/              # Today tab
        │   ├── DashboardScreen.kt
        │   ├── DashboardUiState.kt
        │   └── DashboardViewModel.kt
        ├── session/                # SESSION_GRAPH (overview→player→complete)
        │   ├── SessionOverviewScreen.kt + UiState + ViewModel
        │   ├── SessionPlayerScreen.kt + UiState + ViewModel
        │   └── SessionCompleteScreen.kt
        ├── log/                    # Log tab
        │   ├── BenchmarkLogScreen.kt + UiState + ViewModel
        │   ├── LogForm.kt          # numeric + categorical input sheet
        │   └── LogSparkline.kt     # log-row sparkline helper
        ├── carousel/               # CAROUSEL_GRAPH (carousel/{step})
        │   └── BenchmarkCarouselScreen.kt + UiState + ViewModel
        ├── analytics/              # Progress tab
        │   ├── AnalyticsScreen.kt + UiState + ViewModel
        │   └── BenchmarkAnalyticsCard.kt   # per-benchmark BigChart card
        ├── settings/               # Settings tab
        │   ├── SettingsScreen.kt + UiState + ViewModel
        │   └── (SettingsStatus inside ViewModel)
        └── placeholder/PlaceholderScreen.kt   # used during R1–R5

app/src/test/java/com/stretchdaily/app/
├── core/engine/                    # CategoryWeightCalculator, SelectionShield,
│                                   #   SessionBuilder
├── core/benchmark/                 # TierResolver, BenchmarkProgressBuilder,
│                                   #   BenchmarkBetter, BenchmarkDelta
├── core/session/                   # TodaySessionHolder
├── data/                           # BenchmarkRepositoryDue/DueSelection,
│                                   #   SessionRepositoryStreak/Window/Flow,
│                                   #   DataPortRepository
└── ui/
    ├── components/                 # SegmentProgressHelpers, WeekStripHelpers
    ├── theme/                      # CategoryTint, OklchToSrgb
    └── screen/
        ├── dashboard/              # DashboardViewModelTest
        ├── session/                # SessionOverview/Player ViewModelTests
        ├── log/                    # BenchmarkLog ViewModelTest + LogSparkline
        ├── carousel/               # BenchmarkCarousel ViewModelTest
        ├── analytics/              # AnalyticsViewModelTest
        └── settings/               # SettingsViewModelTest
```

Reference materials (gitignored — kept locally only):
- `Project Blueprint.docx` — original product spec
- `Exercises.xlsx` — source of truth for exercise catalog
- `Benchmarks V2.xlsx` — source of truth for benchmark catalog
- `Screenshots/` — UI inspiration

---

## 6. Data notes

- **46 exercises** across 7 categories. IDs are zero-padded per category
  (`N01..N06`, `S01..S07`, `W01..W06`, `SP01..SP07`, `H01..H07`, `K01..K07`,
  `A01..A06`).
- **10 benchmarks**: 9 numeric (`Cervical Rotation`, `Apley Scratch Test`,
  `Seated Thoracic Rotation`, `Sit and Reach`, `Thomas Test`,
  `Butterfly Stretch`, `Knee-to-Wall`, `Wrist Extension`, `Wrist Flexion`)
  and 1 categorical (`ATG Split Squat`).
- **`Apley Scratch Test`**: original spreadsheet labels unit "Score" but the
  ranges are cm gaps. We treat it as numeric in cm.
- **`Sit and Reach`**: lower (negative) values mean *more* flexible. The Phase
  4 TierResolver must handle direction-reversed ranges.
- **`totalTime`** in the spreadsheet already accounts for both sides of
  unilateral exercises — don't double it.
- **Cue count**: variable (1–4 per exercise), not always 3 as the blueprint
  suggested. Stored faithfully as a `List<String>`.
- **Exercise time cap**: the engine (Phase 2) caps any single exercise at
  120s of session budget — defensive even though no current exercise blows
  past that.

---

## 7. Build commands

The user has not modified Windows env vars. Always invoke gradle with the
`JAVA_HOME` and `ANDROID_HOME` prefixes inline (Bash/MINGW path syntax):

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" \
ANDROID_HOME="/c/Users/RamonvanSchalm/AppData/Local/Android/Sdk" \
./gradlew <task>
```

Common tasks:

```bash
# Compile + assemble debug APK (use this to verify Room/Hilt code-gen)
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ANDROID_HOME="/c/Users/RamonvanSchalm/AppData/Local/Android/Sdk" ./gradlew assembleDebug

# Run JVM unit tests (engine, utils)
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ANDROID_HOME="/c/Users/RamonvanSchalm/AppData/Local/Android/Sdk" ./gradlew test

# Clean
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ANDROID_HOME="/c/Users/RamonvanSchalm/AppData/Local/Android/Sdk" ./gradlew clean

# Instrumented tests (require connected device/emulator — user runs these)
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ANDROID_HOME="/c/Users/RamonvanSchalm/AppData/Local/Android/Sdk" ./gradlew connectedAndroidTest
```

We can't run an emulator from inside Claude Code, so UI verification is
delegated to the user. Compilation + unit tests are the verification gate
during a session.

---

## 8. Pointers

- Sage redesign plans (R1–R6): `docs/superpowers/plans/2026-04-23-sage-redesign-R*.md`
- Sage design handoff: `docs/design_handoff_stretch_daily_v3/` —
  open `reference/index.html` in a browser for the per-screen
  pixel reference.
- Git workflow skill: `.claude/skills/git-flow/SKILL.md`
- GitHub remote: https://github.com/RvanSchalm/stretch-daily
