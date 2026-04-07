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

**Last updated**: 2026-04-07 (end of Phase 1 session)
**Active branch**: `chore/repo-init` (per plan, this single branch covers the
whole Phase 1 scaffold; PR target is `development`)

**Just completed**: Phase 1 — Project Scaffold + Database
- Gradle wrapper + version catalog (`gradle/libs.versions.toml`)
- App shell: `AndroidManifest.xml`, `StretchDailyApp` (`@HiltAndroidApp`),
  `MainActivity` (`@AndroidEntryPoint`) with placeholder Compose screen
- Dark theme with orange accents (`ui/theme/`)
- All Room entities: `Exercise`, `Benchmark`, `BenchmarkLog`, `SessionRecord`,
  `SessionExercise` + enums `Category`, `FlexibilityTier`, `BenchmarkInputType`
- DAOs: `ExerciseDao`, `BenchmarkDao`, `BenchmarkLogDao`, `SessionDao`
- `Converters` (kotlinx.serialization for List/Map, name-based for enums)
- `StretchDailyDatabase` with `RoomDatabase.Callback.onCreate` seeding
- `DatabaseSeeder` with all 46 exercises and 10 benchmarks
- Hilt `DatabaseModule` providing the database, all DAOs, and an
  `@ApplicationScope CoroutineScope`

**Next up**: Phase 2 — Longevity Engine (`feature/longevity-engine`)
1. `CategoryWeightCalculator` — latest BenchmarkLog → `Map<Category, Double>`
2. `SelectionShield` — exercises stale for >14 days
3. `SessionBuilder` — weighted random selection within 600–900s budget
4. `LongevityEngine` — public `generateSession()` API
5. Unit tests (this is the core algorithm — test thoroughly)

**Known issues**:
- **Gradle CLI build blocked on this Windows machine.** Running `./gradlew`
  (any task) fails with `java.io.IOException: Unable to establish loopback
  connection` → `SocketException: Invalid argument: connect` inside
  `UnixDomainSockets.connect0`. Root cause: Android Studio's bundled JBR 21
  selector tries to create a loopback pipe via Windows Unix Domain Sockets,
  which fails on this machine. Tried (none worked):
  `--no-daemon`, removing `org.gradle.jvmargs`, `JAVA_TOOL_OPTIONS=-Djava.net.preferIPv4Stack=true`,
  custom `java.io.tmpdir`, running through `cmd.exe` instead of bash.
  - **Workaround for now**: build from Android Studio's UI (Build → Make
    Project) — Android Studio's internal Gradle integration bypasses the
    failing socket path.
  - **Permanent fix**: install a non-JBR JDK 17 (e.g. Microsoft OpenJDK 17 or
    Adoptium Temurin 17) and point `JAVA_HOME` at it. Update the build
    commands in section 7 once that's done.
- Phase 1 code has been written and code-reviewed but **not yet verified by
  a successful gradle build**. Compilation will be re-verified at the start
  of Phase 2 (either via Android Studio or a fixed JDK).

---

## 3. Architecture summary

- **MVVM**: ViewModel + StateFlow (no LiveData). UI is 100% Compose.
- **DI**: Hilt. Single `SingletonComponent` module (`DatabaseModule`) for now;
  more modules will be added per feature phase.
- **Database**: Room with KSP. Seeded once at file creation via
  `RoomDatabase.Callback.onCreate` running on `Dispatchers.IO` inside the
  injected `@ApplicationScope` coroutine scope.
- **Engine**: Will live in `core/engine/` as pure Kotlin (no Android
  dependencies) so it can be unit-tested with plain JVM tests.
- **Navigation**: Will use Compose Navigation with type-safe routes (Phase 3+).

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
├── MainActivity.kt                 # @AndroidEntryPoint, Compose entry point
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
│   │   └── dao/                    # ExerciseDao, BenchmarkDao, ...
│   └── di/
│       └── DatabaseModule.kt       # Hilt — database, DAOs, ApplicationScope
└── ui/theme/                       # Color, Type, Theme
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
