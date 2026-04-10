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

**Last updated**: 2026-04-10 (end of Phase 8 session)
**Active branch**: `chore/release-prep` (PR target is `development`)

**Just completed**: Phase 8 — Release prep
- **Splash screen**: Wired the AndroidX SplashScreen API
  (`core-splashscreen 1.0.1`). The activity theme starts as
  `Theme.StretchDaily.Splash` (dark background + launcher icon), then
  `installSplashScreen()` in `MainActivity.onCreate()` transitions to
  the regular theme. Works on Android 12+ natively and polyfills back
  to API 23 via the compat library.
- **App icon**: Replaced the placeholder plus sign with a stretching
  figure silhouette in the `#FF8C00` orange accent, drawn as a vector
  adaptive icon foreground over the `#0D0D0D` dark background.
- **Audio cues**: Two placeholder WAV files (`chime_start.wav`,
  `chime_end.wav`) generated as short sine-wave tones.
  `core/audio/SessionAudioPlayer.kt` is a `@Singleton` wrapping
  `SoundPool` — eagerly loads both sounds, checks
  `SettingsDataStore.audioCuesEnabled` (via `Flow.first()`) before
  every play call. `SessionViewModel` fires `playStart()` when the
  session begins and on every exercise transition, `playEnd()` when
  the session finishes. The existing audio toggle in Settings now
  controls real playback.
- **R8 minification**: Enabled `isMinifyEnabled = true` and
  `isShrinkResources = true` on the release build type. ProGuard keep
  rules added for kotlinx.serialization (`$$serializer`, `Companion`,
  `serializer()`), Room entities, Hilt/Dagger generated components,
  and Compose lambdas.
- **Release signing**: `app/build.gradle.kts` reads a
  `keystore.properties` file (gitignored) when present and configures
  `signingConfigs.release` from it. Missing file = no signing config =
  debug signing only, so debug builds and CI are unaffected. To sign a
  release, create `keystore.properties` at the project root with
  `storeFile`, `storePassword`, `keyAlias`, `keyPassword`.
- **Accessibility**: Replaced every `contentDescription = null` on
  interactive `Icon` composables across all screens with meaningful
  labels: Pause/Resume, Skip, Swap exercise, Benchmarks due,
  Session complete, Rotation shield, Open (settings row arrow).
- **Bug fix (Phase 7 follow-up)**: Made the benchmarks tab reactive
  to database wipes. Added `observeLatestPerBenchmark()` Flow query
  to `BenchmarkLogDao`, exposed reactive methods from
  `BenchmarkRepository`, refactored `BenchmarksViewModel` from
  one-shot `refresh()` to `combine(...).stateIn(Eagerly)`. New test
  `state recomputes when latest logs flow emits a new value` directly
  covers the delete-all scenario.

**Deferred items**: End-to-end instrumented tests and exercise
placeholder drawables are left for a future iteration — the app has
no exercise images in the data model yet, and instrumented tests
require a connected device which can't be verified in Claude Code.

**Next up**: All 8 planned phases are complete. The app is
feature-complete for v0.1.0. Remaining work for a production release:
1. Replace placeholder audio files with real chime samples.
2. Add exercise illustration assets (WebP/Lottie) and wire via Coil.
3. Create a release keystore and sign an AAB.
4. Instrumented tests on a real device/emulator.
5. Play Store listing and metadata.

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
- Phase 1 code has been compiled successfully via **Android Studio
  (Build → Make Project)** on 2026-04-07. Gradle CLI verification is still
  blocked by the issue above; Android Studio remains the verification path
  going forward.

---

## 3. Architecture summary

- **MVVM**: ViewModel + StateFlow (no LiveData). UI is 100% Compose.
- **DI**: Hilt. `SingletonComponent` modules so far: `DatabaseModule`
  (database, DAOs, ApplicationScope), `EngineModule` (Clock binding). All
  engine, repository, and ViewModel classes use `@Inject constructor` and
  don't need explicit `@Provides`. ViewModels are `@HiltViewModel`.
- **Database**: Room with KSP. Seeded once at file creation via
  `RoomDatabase.Callback.onCreate` running on `Dispatchers.IO` inside the
  injected `@ApplicationScope` coroutine scope.
- **Engine**: `core/engine/`. The three components
  (`CategoryWeightCalculator`, `SelectionShield`, `SessionBuilder`) are pure
  Kotlin — no Android, no Room, no coroutines — so they're tested via plain
  JVM JUnit. `LongevityEngine` is the thin Android-aware orchestrator that
  actually talks to Room and is the only public entry point. Tests inject a
  fake `Clock` and a seeded `Random` for determinism.
- **Repositories**: `data/`. Wrap DAOs so ViewModels never see Room.
  Pure-logic helpers (e.g. `SessionRepository.computeStreak`) live as
  `internal` companion functions so they're testable on the JVM without
  spinning up a database.
- **Session ViewModel**: One `SessionViewModel` for the entire session
  flow, scoped to the nested session NavGraph entry via
  `hiltViewModel(parentEntry)`. The 1 Hz timer is a `delay`-based
  coroutine inside `viewModelScope`; the `tick()` function is `internal`
  so unit tests can drive it without a real dispatcher.
  `SessionAudioPlayer` (injected) plays start/end chimes gated by the
  audio-cues preference in DataStore.
- **Navigation**: Compose Navigation with string routes. The
  `NavHost` lives inside an outer `Scaffold` that owns a Material3
  `NavigationBar` (Home / Benchmarks / Settings). The outer Scaffold
  sets `contentWindowInsets = WindowInsets(0)` so the inner screen
  Scaffolds keep handling status-bar insets themselves — only the
  bottom-bar height bubbles down via a `contentPadding` parameter
  that the per-tab screens opt into. Two nested `navigation(...)`
  graphs: `SESSION_GRAPH` (preview → follow → complete) and
  `BENCHMARKS_GRAPH` (list → history). Both scope their ViewModel to
  the graph entry via `hiltViewModel(parentEntry)` so multi-screen
  state (the running session timer, the log dialog) stays coherent
  across destinations. The session flow and the session history
  detail are immersive (no bottom bar) — controlled by the
  `BOTTOM_NAV_ROUTES` set. Top-level routes are constants in
  `ui/navigation/StretchDailyNavHost.kt`.
- **Dashboard**: `HomeViewModel` joins streak math, total/weekly
  volume, last-session timestamp, benchmarks-due banner, and the
  engine's `CategoryWeightCalculator` into a single `HomeUiState`
  pass. `HomeScreen` snaps each fractional category weight to its
  closest `FlexibilityTier` for display and renders a horizontal
  weight bar normalized against `STIFF` (3.0). Stiff /
  below-average rows get the orange border treatment.
- **Benchmarks pipeline**: `TierResolver` (pure Kotlin, hard-coded
  per-benchmark breakpoint profiles) → `BenchmarkRepository` (DAO
  wrapper + CRUD on logs + `isBenchmarksDue` nagging helper) →
  `BenchmarksViewModel` (list + dialog + history) → three Compose
  screens sharing one ViewModel via the nested graph. Five benchmarks
  are ASCENDING (higher = more flexible), four are DESCENDING (lower =
  more flexible — Apley, Butterfly, Sit and Reach, Thomas), and the one
  categorical benchmark (ATG Split Squat) is tier-picked directly.
- **Progress chart**: `BenchmarkProgressBuilder` (pure Kotlin, sibling
  to `TierResolver`) maps `BenchmarkLog`s to a `ProgressSeries` of
  normalized `(xRatio, yRatio)` points keyed off the resolved tier.
  `BenchmarkProgressChart` (Compose Canvas) consumes that series and
  draws the five tier bands + polyline + dot markers, sized via
  `Column` weights so the left-gutter labels line up with the band
  centers. The chart is embedded as the first item of
  `BenchmarkHistoryScreen`'s LazyColumn — no separate Progress tab.
- **Settings + data port**: `SettingsDataStore` (Preferences DataStore
  named `stretch_daily_settings`) holds the audio cues toggle.
  `DataPortRepository` is the schema-aware orchestrator for
  export/import/delete-all: it knows the FK order, snapshots all five
  tables into a versioned `ExportPayload`, and re-seeds the catalog
  from `DatabaseSeeder` after a wipe so the app stays usable. JSON IO
  is `kotlinx.serialization` `encodeToStream`/`decodeFromStream` (the
  Room entities themselves are `@Serializable` — no DTO layer).
  `SettingsViewModel` exposes a `SettingsStatus` sealed-interface
  state machine (Idle/Working/Success/Error) that the screen surfaces
  via a `SnackbarHost` and consumes after each one-shot result.
  Document picking happens in the screen via SAF
  `ActivityResultContracts.CreateDocument`/`OpenDocument`; the
  resulting `Uri`s are passed into the ViewModel, which uses an
  injected `@ApplicationContext` to open the streams.

---

## 4. Key conventions

- **Branch model**: `main` (protected) ← PRs ← `development` ← feature
  branches. One PR per phase. Conventional Commits (`feat:`, `fix:`, `chore:`,
  `docs:`). Push every commit immediately. See
  `.claude/skills/git-flow/SKILL.md`.
- **Theme**: Fixed dark theme. Background `#0D0D0D`, surface `#1A1A1A`,
  primary/accent `#FF8C00`. No dynamic colors.
- **No backwards compatibility cruft**: Don't keep dead enum values, unused
  re-exports, or `// removed` comments. Just delete.
- **No speculative abstractions**: Inline a few similar lines instead of
  building an early helper. Add structure when the third use case shows up.
- **Comments**: Only where the logic isn't self-evident. Don't narrate code.

---

## 5. File map (critical files)

```
app/src/main/java/com/stretchdaily/app/
├── StretchDailyApp.kt              # @HiltAndroidApp Application
├── MainActivity.kt                 # @AndroidEntryPoint, hosts StretchDailyNavHost
├── core/
│   ├── model/
│   │   ├── Category.kt             # 7 body areas
│   │   ├── FlexibilityTier.kt      # 5 tiers + weights for the engine
│   │   ├── BenchmarkInputType.kt   # NUMERIC | CATEGORICAL
│   │   ├── Exercise.kt             # @Entity
│   │   ├── Benchmark.kt            # @Entity
│   │   ├── BenchmarkLog.kt         # @Entity (FK -> benchmarks)
│   │   ├── SessionRecord.kt        # @Entity (session header)
│   │   └── SessionExercise.kt      # @Entity (FKs -> session_records, exercises)
│   ├── database/
│   │   ├── StretchDailyDatabase.kt # RoomDatabase, seeds on create
│   │   ├── DatabaseSeeder.kt       # All 46 exercises + 10 benchmarks
│   │   ├── Converters.kt           # JSON for List/Map, name() for enums
│   │   └── dao/                    # ExerciseDao, BenchmarkDao, SessionDao, ...
│   ├── engine/
│   │   ├── LongevityEngine.kt      # Public generateSession() — only DAO-aware class
│   │   ├── CategoryWeightCalculator.kt   # latest logs -> Map<Category, Double>
│   │   ├── SelectionShield.kt      # exercises stale > 14d
│   │   ├── SessionBuilder.kt       # weighted random + 120s cap
│   │   └── model/
│   │       └── SessionPlan.kt      # SessionPlan + PlannedExercise
│   ├── benchmark/
│   │   ├── TierResolver.kt         # numeric reading -> FlexibilityTier
│   │   └── BenchmarkProgressBuilder.kt   # logs -> normalized chart series
│   ├── audio/
│   │   └── SessionAudioPlayer.kt   # SoundPool wrapper — start/end chimes
│   ├── datastore/
│   │   └── SettingsDataStore.kt    # Preferences DataStore — audio cues toggle
│   ├── util/
│   │   └── Clock.kt                # fun interface { now(): Long }
│   └── di/
│       ├── DatabaseModule.kt       # Hilt — database, DAOs, ApplicationScope
│       └── EngineModule.kt         # Hilt — Clock binding
├── data/
│   ├── BenchmarkRepository.kt      # benchmark CRUD + isBenchmarksDue
│   ├── SessionRepository.kt        # streak / weekly / total / lastAt + observeAllSessions
│   ├── DataPortRepository.kt       # export / import / delete-all + re-seed
│   └── export/
│       └── ExportPayload.kt        # @Serializable versioned snapshot of all 5 tables
└── ui/
    ├── theme/                      # Color, Type, Theme
    ├── navigation/
    │   └── StretchDailyNavHost.kt  # Routes + bottom nav + nested graphs
    ├── home/
    │   ├── HomeViewModel.kt        # Joins streak / volume / heatmap / due
    │   └── HomeScreen.kt           # KPI cards + heatmap + Start CTA
    ├── benchmarks/
    │   ├── BenchmarksUiState.kt
    │   ├── BenchmarksViewModel.kt
    │   ├── BenchmarksScreen.kt
    │   ├── BenchmarkHistoryScreen.kt   # hosts the progress chart
    │   ├── BenchmarkProgressChart.kt   # Canvas line chart over tier bands
    │   └── LogBenchmarkDialog.kt
    ├── session/
    │   ├── SessionUiState.kt
    │   ├── SessionViewModel.kt
    │   ├── SessionPreviewScreen.kt
    │   ├── SessionFollowAlongScreen.kt
    │   ├── SessionCompleteScreen.kt
    │   ├── SessionHistoryViewModel.kt   # Loading|Empty|Loaded
    │   └── SessionHistoryScreen.kt      # read-only newest-first list
    └── settings/
        ├── SettingsScreen.kt       # Audio toggle + export/import/delete + history link
        └── SettingsViewModel.kt    # SettingsStatus state machine + data port wiring

app/src/test/java/com/stretchdaily/app/
├── core/engine/
│   ├── CategoryWeightCalculatorTest.kt
│   ├── SelectionShieldTest.kt
│   └── SessionBuilderTest.kt       # 5-8 in 600-900s, statistical bias check
├── core/benchmark/
│   ├── TierResolverTest.kt
│   └── BenchmarkProgressBuilderTest.kt   # chart series math
├── data/
│   ├── BenchmarkRepositoryDueTest.kt
│   ├── SessionRepositoryStreakTest.kt   # streak math: empty/today/gap/grace
│   ├── SessionRepositoryWindowTest.kt   # trailing N-day count helper
│   └── DataPortRepositoryTest.kt        # snapshot/round-trip/version/delete
└── ui/
    ├── benchmarks/
    │   └── BenchmarksViewModelTest.kt
    ├── home/
    │   └── HomeViewModelTest.kt    # mockk-driven dashboard transitions
    └── session/
        ├── SessionViewModelTest.kt
        └── SessionHistoryViewModelTest.kt
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

- Plan: `C:\Users\RamonvanSchalm\.claude\plans\logical-napping-flask.md`
- Git workflow skill: `.claude/skills/git-flow/SKILL.md`
- GitHub remote: https://github.com/RvanSchalm/stretch-daily
