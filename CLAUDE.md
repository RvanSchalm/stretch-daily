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

**Last updated**: 2026-04-07 (end of Phase 4 session)
**Active branch**: `feature/benchmarks` (PR target is `development`)

**Just completed**: Phase 4 — Benchmarks Tab
- `core/benchmark/TierResolver.kt` — resolves a raw numeric reading to a
  `FlexibilityTier` for each of the 9 numeric benchmarks. Uses hard-coded
  per-benchmark breakpoint profiles rather than parsing the free-text
  `Benchmark.tierRanges` strings — those have awkward copy like
  `"> 15° up"` and `"< 0 cm (Finger Overlap)"` that's much cleaner to
  keep for display only. Five benchmarks are ASCENDING (higher = more
  flexible: cervical rotation, thoracic rotation, knee-to-wall, wrist
  extension, wrist flexion); four are DESCENDING (lower = more flexible:
  Apley Scratch gap, Butterfly knee-to-floor, Sit and Reach, Thomas Test
  — which goes signed with positive = thigh up = stiff). Returns `null`
  for the categorical ATG Split Squat, which is picked directly.
- `data/BenchmarkRepository.kt` — wraps `BenchmarkDao` + `BenchmarkLogDao`
  + `TierResolver`. `logNumeric()` parses the raw string (accepts `,` as
  decimal separator) and re-resolves the tier; `logCategorical()` trusts
  the tier the user picked; `updateLog()` handles both paths for the
  edit flow. `isBenchmarksDue()` delegates to an `internal` companion
  function `computeBenchmarksDue` so the banner logic stays unit-testable
  without Room — returns `true` if never logged, > 30 days stale, or on
  the 1st of the month when the last log wasn't today.
- `BenchmarkLogDao` gained `getById` and `@Update` so the history screen
  can edit individual rows without delete + insert.
- `ui/benchmarks/BenchmarksUiState.kt` — `BenchmarksUiState`
  (`Loading | Loaded | Error`) plus `BenchmarkRow` (catalog entry +
  latest log) and `LogDialogState` (benchmark, editing id, raw input,
  selected tier, error) owned by the ViewModel so rotations don't lose
  pending input.
- `ui/benchmarks/BenchmarksViewModel.kt` — `@HiltViewModel`. Backs both
  the list and the history screen. `refresh()` rebuilds the row list
  from the repo; `openLogDialog` / `openEditDialog` seed the dialog
  state; `submitDialog()` branches on `BenchmarkInputType` and calls
  `logNumeric` / `logCategorical` / `updateLog` with error surfacing;
  `deleteLog` forwards + refreshes.
- `ui/benchmarks/BenchmarksScreen.kt` — Material3 Scaffold + TopAppBar.
  `LazyColumn` of `BenchmarkCard`s: name + category chip, latest value
  + `TierChip`, Log button, history icon. Tapping anywhere on the card
  or the "Log …" button opens the dialog.
- `ui/benchmarks/BenchmarkHistoryScreen.kt` — per-benchmark log history.
  Streams `repository.observeLogsFor(id)` via `Flow.collectAsState`.
  Each row shows value + resolved tier + formatted date plus Edit /
  Delete icon buttons. Shares the same dialog as the list screen.
- `ui/benchmarks/LogBenchmarkDialog.kt` — modal dialog driven entirely
  by `LogDialogState`. Numeric body shows the tier range hints for the
  current benchmark and a `KeyboardType.Number` OutlinedTextField.
  Categorical body shows 5 tier buttons (Stiff → Very Flexible) each
  with the qualitative description. Error text rendered inline.
- `ui/home/HomeViewModel.kt` — minimal ViewModel for Home, only job is
  to compute `benchmarksDue` from the repo. Will grow in Phase 5.
- `ui/home/HomeScreen.kt` — now takes `HomeUiState`. Adds a
  `BenchmarksDueBanner` (warning icon + copy + "Log" button) that
  renders only when `state.benchmarksDue`, plus an outlined
  "Benchmarks" CTA under the existing Start Session button.
- `ui/navigation/StretchDailyNavHost.kt` — new `Routes.BENCHMARKS_GRAPH`
  nested graph (list + history). `BenchmarksViewModel` is scoped to the
  graph entry via the same `hiltViewModel(parentEntry)` pattern used
  for the session flow, so the dialog state and list refresh stay
  consistent between the list and the history screen. `HomeScreen`
  gains a `LaunchedEffect` that calls `HomeViewModel.refresh()` on
  re-entry so the banner clears after the user logs.
- JVM unit tests:
  - `app/src/test/java/.../core/benchmark/TierResolverTest.kt` — 12
    cases: each ascending and descending benchmark's five tiers,
    on-breakpoint edge cases for both directions, Sit-and-Reach's
    negative range, Thomas Test's signed "up/down", unknown ID + the
    categorical ATG Split Squat both returning null, and a loop
    asserting `handles()` for every numeric benchmark ID.
  - `app/src/test/java/.../data/BenchmarkRepositoryDueTest.kt` — 6
    cases: never-logged → due, fresh → not due, exactly-30-days → not
    due, 31-days → due, 1st-of-month with yesterday's log → due,
    1st-of-month already-logged-today → not due.
  - `app/src/test/java/.../ui/benchmarks/BenchmarksViewModelTest.kt` —
    12 cases with mockk + `UnconfinedTestDispatcher`. Covers refresh
    (empty + with logs + error), openLog / openEdit dialog seeding,
    numeric submit (success, failure-message surfacing, empty guard),
    categorical submit (success, no-selection guard), edit numeric
    updateLog forwarding, and delete + refresh.

Phase 4 has not yet been compiled — Ramon will run Build → Make Project
in Android Studio to verify. Gradle CLI is still blocked on this machine
(see Known issues — unchanged from Phase 2).

**Next up**: Phase 5 — Dashboard & Bottom Navigation
(`feature/dashboard`)
1. Replace the Home placeholder with the real dashboard: current
   streak, this-week volume, category heatmap fed by the latest
   benchmark tiers.
2. `MaterialTheme` bottom nav bar with Home / Benchmarks / Settings
   tabs. Benchmarks leg moves from a push-route to a tab switch.
3. Session history screen (list of completed `SessionRecord`s).

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
- **Navigation**: Compose Navigation with string routes. Two nested
  `navigation(...)` graphs so far: `SESSION_GRAPH` (preview → follow →
  complete) and `BENCHMARKS_GRAPH` (list → history). Both scope their
  ViewModel to the graph entry via `hiltViewModel(parentEntry)` so
  multi-screen state (the running session timer, the log dialog) stays
  coherent across destinations. Top-level routes are constants in
  `ui/navigation/StretchDailyNavHost.kt`.
- **Benchmarks pipeline**: `TierResolver` (pure Kotlin, hard-coded
  per-benchmark breakpoint profiles) → `BenchmarkRepository` (DAO
  wrapper + CRUD on logs + `isBenchmarksDue` nagging helper) →
  `BenchmarksViewModel` (list + dialog + history) → three Compose
  screens sharing one ViewModel via the nested graph. Five benchmarks
  are ASCENDING (higher = more flexible), four are DESCENDING (lower =
  more flexible — Apley, Butterfly, Sit and Reach, Thomas), and the one
  categorical benchmark (ATG Split Squat) is tier-picked directly.

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
│   ├── util/
│   │   └── Clock.kt                # fun interface { now(): Long }
│   └── di/
│       ├── DatabaseModule.kt       # Hilt — database, DAOs, ApplicationScope
│       └── EngineModule.kt         # Hilt — Clock binding
├── data/
│   └── SessionRepository.kt        # Persists finished sessions, computes streak
└── ui/
    ├── theme/                      # Color, Type, Theme
    ├── navigation/
    │   └── StretchDailyNavHost.kt  # Routes + nested session NavGraph
    ├── home/
    │   └── HomeScreen.kt           # Phase 3 placeholder landing screen
    └── session/
        ├── SessionUiState.kt       # sealed Loading|Preview|FollowAlong|Complete|Error + Side
        ├── SessionViewModel.kt     # @HiltViewModel — generate/start/tick/pause/skip/finish
        ├── SessionPreviewScreen.kt # exercise list + swap + start
        ├── SessionFollowAlongScreen.kt # countdown + cues + pause/skip
        └── SessionCompleteScreen.kt # checkmark + stat cards + done

app/src/test/java/com/stretchdaily/app/
├── core/engine/
│   ├── CategoryWeightCalculatorTest.kt
│   ├── SelectionShieldTest.kt
│   └── SessionBuilderTest.kt       # 5-8 in 600-900s, statistical bias check
├── data/
│   └── SessionRepositoryStreakTest.kt  # streak math: empty/today/gap/grace
└── ui/session/
    └── SessionViewModelTest.kt     # state transitions via mockk + UnconfinedTestDispatcher
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
