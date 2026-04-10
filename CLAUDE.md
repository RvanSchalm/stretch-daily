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

**Last updated**: 2026-04-09 (end of Phase 7 session)
**Active branch**: `feature/settings-polish` (PR target is `development`)

**Just completed**: Phase 7 — Settings: data port, audio toggle, delete
- `core/datastore/SettingsDataStore.kt` — Singleton wrapper around a
  Preferences `DataStore` named `stretch_daily_settings`. Currently
  exposes a single `audioCuesEnabled: Flow<Boolean>` (defaulting
  `true`) and a `setAudioCuesEnabled` writer. Constructor takes
  `@ApplicationContext` because Preferences DataStore is a
  context-extension property; that context is the Application, so it's
  safe to hold in a `@Singleton`.
- All Room entities (`Exercise`, `Benchmark`, `BenchmarkLog`,
  `SessionRecord`, `SessionExercise`) and their value enums
  (`Category`, `FlexibilityTier`, `BenchmarkInputType`) gained
  `@Serializable` annotations. Room's `@Entity` and
  `kotlinx.serialization`'s `@Serializable` coexist cleanly — no DTOs
  needed. The flat-dump JSON schema is the table layout itself, which
  keeps the export trivial.
- `data/export/ExportPayload.kt` — versioned wrapper holding all five
  table lists plus an `exportedAt` timestamp. `version = 1` is the
  current schema; `importFrom` rejects anything else with
  `IllegalArgumentException` so future schema bumps fail loudly.
- `data/DataPortRepository.kt` — `@Singleton` that owns the three
  Settings actions:
  - `exportTo(OutputStream)` writes the snapshot via
    `Json.encodeToStream` (`@OptIn(ExperimentalSerializationApi)`).
    The Json instance has `prettyPrint = true`, `encodeDefaults = true`,
    `ignoreUnknownKeys = true`.
  - `importFrom(InputStream)` decodes, validates the version, then
    `replaceAll(payload)` clears children before parents
    (session_exercises → session_records → benchmark_logs), REPLACE-
    inserts the catalog (benchmarks + exercises), then re-inserts user
    rows in dependency order. Schema-aware order is encapsulated here
    so the ViewModel never has to know about FK ordering.
  - `deleteAll()` wipes user-mutable tables in the same FK order,
    resets every exercise's `lastPerformed`, then re-seeds the catalog
    from `DatabaseSeeder` so the app stays fully usable immediately
    after a wipe (no empty exercise list, no missing benchmarks).
- DAO additions: `ExerciseDao.resetAllLastPerformed()`,
  `BenchmarkLogDao.insertAll/deleteAll/getAll()`, and `SessionDao`
  read/write helpers (`getAllRecords`, `getAllSessionExercises`,
  `insertAllRecords`, `deleteAllRecords`,
  `deleteAllSessionExercises`). These exist purely to back the export/
  import/delete pipeline and are otherwise unused.
- `ui/settings/SettingsViewModel.kt` — combines DataStore, the data
  port, and a small `SettingsStatus` sealed-interface state machine
  (`Idle | Working(msg) | Success(msg) | Error(msg)`). Audio cues
  preference is exposed as a `StateFlow<Boolean>` via
  `stateIn(WhileSubscribed(5_000), initialValue = true)`. The three
  data-port actions are `viewModelScope.launch` blocks that flip
  status to `Working`, run the repository call, and set
  `Success/Error` based on the outcome. `consumeStatus()` is the
  one-shot reset the screen calls after surfacing a snackbar so the
  status doesn't replay across recomposition. The screen passes URIs
  in (no streams across the boundary); the ViewModel uses the injected
  `@ApplicationContext` to open input/output streams from those URIs.
- `ui/settings/SettingsScreen.kt` — full rewrite. Uses
  `rememberLauncherForActivityResult` with
  `ActivityResultContracts.CreateDocument("application/json")` for
  export and `ActivityResultContracts.OpenDocument()` for import,
  routing the resulting URIs to the ViewModel. A `SnackbarHost`
  surfaces every non-Idle status via a `LaunchedEffect(status)`. The
  five rows are: audio cues toggle (Material3 Switch + tap-the-row
  affordance), Session history (existing nav callback), Export, Import,
  and Delete (which opens an `AlertDialog` with explicit copy about
  the catalog reset). The `SettingsRow` composable now has an optional
  `destructive: Boolean` flag that paints the title in primary orange.
- JVM unit tests:
  `app/src/test/java/.../data/DataPortRepositoryTest.kt` — 5 cases
  using mockk to fake all four DAOs and a fixed `Clock`:
  1. `snapshot()` pulls from every DAO and stamps the export time.
  2. `exportTo()` writes JSON the repository can re-import (round-trip
     through `ByteArrayOutputStream`/`ByteArrayInputStream` with the
     captured DAO inserts asserted equal to the original fixtures).
  3. `importFrom()` rejects an unsupported version with
     `IllegalArgumentException` and performs no DAO writes.
  4. `importFrom()` clears children before parents and reinserts in FK
     order (`coVerifyOrder`).
  5. `deleteAll()` deletes user tables in FK order, resets exercise
     `lastPerformed`, and re-seeds the full
     `DatabaseSeeder.exercises()`/`benchmarks()` lists.

**Departures from the original plan**: the plan listed
"`SoundPool` placeholder chimes" as part of Phase 7. I shipped only the
DataStore-backed audio toggle and skipped the SoundPool wiring. Why:
there are no audio assets in the project yet, and a SoundPool that
plays nothing is dead code. The toggle's persistence is the part that
needs to land first; the actual chime playback can be added in Phase 8
alongside the placeholder audio files. The "general polish" laundry
list (loading states, error handling, accessibility, placeholder
drawables) is also deferred to Phase 8 — Phase 7 is intentionally
scoped to the three concrete Settings rows the user can act on today.

**Next up**: Phase 8 — Release prep (`chore/release-prep`)
1. App icon + splash screen.
2. Placeholder audio assets + `SoundPool` wiring gated by the
   `audioCuesEnabled` flag from `SettingsDataStore`.
3. Placeholder drawables for missing exercise WebPs.
4. ProGuard/R8 rules tuned for kotlinx.serialization, Hilt, Room.
5. Signed release AAB build.
6. End-to-end instrumented tests for critical flows.
7. Accessibility pass (content descriptions, min touch targets).

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
