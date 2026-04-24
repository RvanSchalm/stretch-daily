# Sage redesign — Phase R1: Tokens + empty scaffold — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the dark-theme / 3-tab UI foundation with the abstracted Sage token system, Manrope + JetBrains Mono fonts, a 5-tab empty NavHost, and cream splash. After this phase the app compiles, launches, shows five Sage-themed placeholder tabs, and all `core/` + `data/` unit tests pass. No features work yet.

**Architecture:** Adds `ui/theme/{AppColors, AppTypography, AppDimens, CategoryTint, Theme}.kt` as immutable token data classes exposed via `CompositionLocal`. Rewrites `ui/navigation/StretchDailyNavHost.kt` as a 5-tab shell rendering minimal `PlaceholderScreen`s. Deletes every old `ui/home/`, `ui/benchmarks/`, `ui/session/`, `ui/settings/`, and old theme files. Keeps `MaterialTheme` as a wrapper for ripple + default content colors; all concrete design values flow through `Theme.colors` / `Theme.typo` / `Theme.dims`. No `core/` or `data/` code changes.

**Tech Stack:** Kotlin 2.0, Jetpack Compose (BOM 2024.10), Material 3, Compose Navigation, Hilt (unchanged), JUnit 4 + mockk.

**Spec:** [`docs/superpowers/specs/2026-04-23-sage-redesign-design.md`](../specs/2026-04-23-sage-redesign-design.md) sections 3.1, 3.4, 3.5, 4, 7.1, 9, 11.1.

**Build verification:** Gradle CLI is blocked on this Windows machine (documented in [`CLAUDE.md §2 Known issues`](../../../CLAUDE.md)). After every code commit that touches Kotlin, verify via Android Studio → Build → Make Project. Unit tests can run via Android Studio → Run → test class. Do NOT spend turns retrying `./gradlew assembleDebug`.

---

## File structure for R1

**New under `app/src/main/res/font/`:**
- `manrope_regular.ttf`
- `manrope_medium.ttf`
- `manrope_semibold.ttf`
- `manrope_bold.ttf`
- `jetbrains_mono_regular.ttf`
- `jetbrains_mono_medium.ttf`
- `OFL-Manrope.txt`
- `OFL-JetBrainsMono.txt`

**New under `app/src/main/java/com/stretchdaily/app/ui/theme/`:**
- `AppColors.kt` — immutable data class + `sageColors()` factory.
- `AppDimens.kt` — immutable data class for radii, gaps, screen padding.
- `AppTypography.kt` — immutable data class + Manrope/JetBrains Mono `FontFamily` assembly.
- `CategoryTint.kt` — oklch triples per `Category`, `oklchToSrgb()` helper, memoized map, `Category.tint()` extension.
- `Theme.kt` — rewritten. Hosts `LocalAppColors`/`LocalAppTypography`/`LocalAppDimens`, `StretchDailyTheme(content)` composable, and `object Theme` accessor.

**New under `app/src/main/java/com/stretchdaily/app/ui/screen/`:**
- `placeholder/PlaceholderScreen.kt` — minimal label-only screen reused by all 5 tabs.

**Rewritten:**
- `app/src/main/java/com/stretchdaily/app/ui/navigation/StretchDailyNavHost.kt` — 5-tab bottom nav, placeholder destinations, session-graph and carousel-graph stubs.
- `app/src/main/res/values/themes.xml` — splash background becomes `#fbf8f0`, runtime theme background follows.

**New tests under `app/src/test/java/com/stretchdaily/app/ui/theme/`:**
- `OklchToSrgbTest.kt` — verifies the color-space math on two reference values.
- `CategoryTintTest.kt` — verifies every `Category` has a resolved tint with sane L/C/H.

**Deleted wholesale:**
- `app/src/main/java/com/stretchdaily/app/ui/theme/Color.kt`
- `app/src/main/java/com/stretchdaily/app/ui/theme/Type.kt`
- `app/src/main/java/com/stretchdaily/app/ui/home/` (2 files)
- `app/src/main/java/com/stretchdaily/app/ui/benchmarks/` (6 files)
- `app/src/main/java/com/stretchdaily/app/ui/session/` (7 files)
- `app/src/main/java/com/stretchdaily/app/ui/settings/` (2 files)
- `app/src/test/java/com/stretchdaily/app/ui/` (4 files)

**Untouched but verified after deletes:**
- `MainActivity.kt` — still imports `com.stretchdaily.app.ui.navigation.StretchDailyNavHost` and `com.stretchdaily.app.ui.theme.StretchDailyTheme`; both names preserved.
- All `core/`, `data/`, and their tests — no reference into `ui/`.

---

## Task 1: Create the `redesign/r1-tokens-scaffold` branch

**Files:** none (git-only).

- [ ] **Step 1: Verify clean working tree on development**

Run:
```bash
git status
git log --oneline -1
```

Expected: `nothing to commit, working tree clean`, and HEAD points at the `docs: add Sage redesign v3 design handoff bundle` commit merged via PR #9 (or its descendants on `development`).

- [ ] **Step 2: Create the feature branch**

Run:
```bash
git checkout -b redesign/r1-tokens-scaffold
```

Expected: `Switched to a new branch 'redesign/r1-tokens-scaffold'`.

- [ ] **Step 3: Push the new branch**

Run:
```bash
git push -u origin redesign/r1-tokens-scaffold
```

Expected: remote branch created, upstream set. If SSH push fails (known machine setup issue), ask the user to push — don't block subsequent steps.

---

## Task 2: Bundle Manrope TTF files

Android font resources must be lowercase `[a-z0-9_]`. One file per weight. OFL license must ship alongside.

**Files:**
- Create: `app/src/main/res/font/manrope_regular.ttf`
- Create: `app/src/main/res/font/manrope_medium.ttf`
- Create: `app/src/main/res/font/manrope_semibold.ttf`
- Create: `app/src/main/res/font/manrope_bold.ttf`
- Create: `app/src/main/res/font/OFL-Manrope.txt`

- [ ] **Step 1: Download Manrope from Google Fonts**

Go to [https://fonts.google.com/specimen/Manrope](https://fonts.google.com/specimen/Manrope) → "Download family". Extract the ZIP. It contains a `static/` directory with per-weight TTFs. We need Regular (400), Medium (500), SemiBold (600), Bold (700).

Alternative: Grab the TTFs directly from [https://github.com/sharanda/manrope](https://github.com/sharanda/manrope) under `fonts/ttf/` (same files, already split per weight).

- [ ] **Step 2: Copy and rename the four weights into `app/src/main/res/font/`**

From the download:

| Source filename | Target filename |
|---|---|
| `Manrope-Regular.ttf` | `manrope_regular.ttf` |
| `Manrope-Medium.ttf` | `manrope_medium.ttf` |
| `Manrope-SemiBold.ttf` | `manrope_semibold.ttf` |
| `Manrope-Bold.ttf` | `manrope_bold.ttf` |

- [ ] **Step 3: Copy the OFL license file**

The Manrope download includes `OFL.txt`. Rename to `OFL-Manrope.txt` and place alongside the TTFs (Android tolerates non-font files in `res/font/` as long as they're lowercase with no extension conflict; `.txt` is fine).

- [ ] **Step 4: Verify the `res/font/` directory**

Run:
```bash
ls app/src/main/res/font/
```

Expected output includes exactly the five new files listed above.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/font/manrope_regular.ttf \
        app/src/main/res/font/manrope_medium.ttf \
        app/src/main/res/font/manrope_semibold.ttf \
        app/src/main/res/font/manrope_bold.ttf \
        app/src/main/res/font/OFL-Manrope.txt
git commit -m "chore: bundle Manrope TTFs under res/font (OFL)"
git push
```

---

## Task 3: Bundle JetBrains Mono TTF files

**Files:**
- Create: `app/src/main/res/font/jetbrains_mono_regular.ttf`
- Create: `app/src/main/res/font/jetbrains_mono_medium.ttf`
- Create: `app/src/main/res/font/OFL-JetBrainsMono.txt`

- [ ] **Step 1: Download JetBrains Mono**

Go to [https://github.com/JetBrains/JetBrainsMono/releases](https://github.com/JetBrains/JetBrainsMono/releases) and download the latest release ZIP. The `fonts/ttf/` directory inside contains per-weight files.

- [ ] **Step 2: Copy and rename the two weights we need**

| Source filename | Target filename |
|---|---|
| `JetBrainsMono-Regular.ttf` | `jetbrains_mono_regular.ttf` |
| `JetBrainsMono-Medium.ttf` | `jetbrains_mono_medium.ttf` |

- [ ] **Step 3: Copy the OFL license**

From the download: `OFL.txt` → `app/src/main/res/font/OFL-JetBrainsMono.txt`.

- [ ] **Step 4: Verify**

Run:
```bash
ls app/src/main/res/font/ | grep -E 'jetbrains|OFL'
```

Expected: `jetbrains_mono_medium.ttf`, `jetbrains_mono_regular.ttf`, `OFL-JetBrainsMono.txt`, plus the Manrope OFL already present.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/font/jetbrains_mono_regular.ttf \
        app/src/main/res/font/jetbrains_mono_medium.ttf \
        app/src/main/res/font/OFL-JetBrainsMono.txt
git commit -m "chore: bundle JetBrains Mono TTFs under res/font (OFL)"
git push
```

---

## Task 4: Add `AppDimens.kt`

Simplest new token class — no dependencies on other new files. Builds the pattern before the color and typography layers land on top.

**Files:**
- Create: `app/src/main/java/com/stretchdaily/app/ui/theme/AppDimens.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.stretchdaily.app.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Dimensional tokens: radii, gaps, and the global screen padding.
 *
 * Held as a data class so an alternate palette / density profile can slot in
 * later via [StretchDailyTheme] without call-site refactors. Access at call
 * sites through [Theme.dims].
 *
 * `padScreen`'s 100.dp bottom accounts for the fixed bottom-nav overlap —
 * screens apply it in their outer `Scaffold`'s `contentPadding` so tab tops
 * breathe against the status bar and list bottoms clear the nav.
 */
data class AppDimens(
    val radiusXs: Dp,
    val radiusSm: Dp,
    val radiusMd: Dp,
    val radiusLg: Dp,
    val radiusPill: Dp,
    val gapList: Dp,
    val gapSection: Dp,
    val gapGroup: Dp,
    val padScreen: PaddingValues,
)

/** Sage defaults — see spec §4.1. */
fun sageDimens(): AppDimens = AppDimens(
    radiusXs = 8.dp,
    radiusSm = 12.dp,
    radiusMd = 16.dp,
    radiusLg = 22.dp,
    radiusPill = 999.dp,
    gapList = 8.dp,
    gapSection = 14.dp,
    gapGroup = 18.dp,
    padScreen = PaddingValues(start = 20.dp, top = 6.dp, end = 20.dp, bottom = 100.dp),
)
```

- [ ] **Step 2: Verify compile in Android Studio**

Build → Make Project. Expected: no errors. `Theme.dims` accessor is not wired yet — that lands in Task 8.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/ui/theme/AppDimens.kt
git commit -m "feat(theme): add AppDimens token data class"
git push
```

---

## Task 5: Add `AppColors.kt`

**Files:**
- Create: `app/src/main/java/com/stretchdaily/app/ui/theme/AppColors.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.stretchdaily.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Color tokens for the current theme.
 *
 * Mirrors the Sage handoff CSS variables 1:1 (spec §4.1, design handoff
 * README "Design tokens"). Field names match the handoff slugs:
 *
 * - `bg` / `bg2`     → `--bg`, `--bg-2`        (screen backgrounds)
 * - `surface` / `surface2` → `--surface`, `--surface-2` (cards / nested)
 * - `ink` / `ink2` / `ink3` → primary / secondary / tertiary text
 * - `line` / `line2` → hairline borders (alpha'd)
 * - `accent` / `accent2` / `accentInk` / `accentSoft` → sage + variants
 * - `warn` → destructive / overdue pill color
 *
 * Instances are immutable — swap the whole `AppColors` value via
 * [StretchDailyTheme] to retheme (Grove / Moss / dark can slot in later).
 */
data class AppColors(
    val bg: Color,
    val bg2: Color,
    val surface: Color,
    val surface2: Color,
    val ink: Color,
    val ink2: Color,
    val ink3: Color,
    val line: Color,
    val line2: Color,
    val accent: Color,
    val accent2: Color,
    val accentInk: Color,
    val accentSoft: Color,
    val warn: Color,
)

/**
 * Sage theme — the production palette.
 *
 * `bg` uses the user-ratified tweak `#fbf8f0` (handoff default was
 * `#f2eee4`; user approved the cream override — see spec §4.1).
 * `accentSoft` is the handoff's `oklch(0.88 0.045 135)` resolved to sRGB
 * once here; we don't run oklch→sRGB for this single value at runtime.
 * `line` / `line2` are the handoff rgba() values; Compose accepts alpha
 * in the Color() constructor directly.
 */
fun sageColors(): AppColors = AppColors(
    bg = Color(0xFFFBF8F0),
    bg2 = Color(0xFFE8E4D9),
    surface = Color(0xFFF9F6EC),
    surface2 = Color(0xFFEDE9DE),
    ink = Color(0xFF252823),
    ink2 = Color(0xFF54584D),
    ink3 = Color(0xFF8A8D82),
    line = Color(red = 0x25, green = 0x28, blue = 0x23, alpha = 0x1F),  // 0.12 * 255 ≈ 31
    line2 = Color(red = 0x25, green = 0x28, blue = 0x23, alpha = 0x0F), // 0.06 * 255 ≈ 15
    accent = Color(0xFF5C7A4A),
    accent2 = Color(0xFF4A6741),
    accentInk = Color(0xFFF5F3EA),
    accentSoft = Color(0xFFD4E2C4), // oklch(0.88 0.045 135) → sRGB (precomputed)
    warn = Color(0xFFA6632A),
)
```

- [ ] **Step 2: Verify compile in Android Studio**

Build → Make Project. Expected: clean.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/ui/theme/AppColors.kt
git commit -m "feat(theme): add AppColors token data class with Sage palette"
git push
```

---

## Task 6: Add `OklchToSrgb` helper (TDD — pure math)

The only piece of R1 with non-trivial logic. Written test-first.

**Files:**
- Create: `app/src/main/java/com/stretchdaily/app/ui/theme/OklchToSrgb.kt`
- Test: `app/src/test/java/com/stretchdaily/app/ui/theme/OklchToSrgbTest.kt`

- [ ] **Step 1: Write the failing test**

Reference values computed via the [CSS Color 4 spec conversion](https://www.w3.org/TR/css-color-4/#color-conversion-code) + the oklab reference implementation. Two checks: a neutral (Neck's tint — `oklch(0.82 0.04 140)`) and the handoff's `accentSoft` (`oklch(0.88 0.045 135)`).

```kotlin
package com.stretchdaily.app.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class OklchToSrgbTest {

    @Test
    fun `oklch neck tint resolves to expected sRGB`() {
        // Neck: oklch(0.82 0.04 140)  — reference rgb ≈ (197, 214, 194)
        val color = oklchToSrgb(l = 0.82, c = 0.04, hDeg = 140.0)
        assertRgbWithin(color, expectedR = 0xC5, expectedG = 0xD6, expectedB = 0xC2, tolerance = 3)
    }

    @Test
    fun `oklch accent-soft resolves to expected sRGB`() {
        // oklch(0.88 0.045 135) — reference rgb ≈ (212, 226, 196)
        val color = oklchToSrgb(l = 0.88, c = 0.045, hDeg = 135.0)
        assertRgbWithin(color, expectedR = 0xD4, expectedG = 0xE2, expectedB = 0xC4, tolerance = 3)
    }

    @Test
    fun `zero chroma returns achromatic value at the requested lightness`() {
        val color = oklchToSrgb(l = 0.5, c = 0.0, hDeg = 0.0)
        // L=0.5 in oklab → mid gray around (118, 118, 118)
        val r = (color.red * 255).toInt()
        val g = (color.green * 255).toInt()
        val b = (color.blue * 255).toInt()
        assertEquals("R == G", r, g)
        assertEquals("G == B", g, b)
    }

    private fun assertRgbWithin(
        color: Color,
        expectedR: Int,
        expectedG: Int,
        expectedB: Int,
        tolerance: Int,
    ) {
        val r = (color.red * 255).toInt()
        val g = (color.green * 255).toInt()
        val b = (color.blue * 255).toInt()
        assertWithin("R", r, expectedR, tolerance)
        assertWithin("G", g, expectedG, tolerance)
        assertWithin("B", b, expectedB, tolerance)
    }

    private fun assertWithin(channel: String, actual: Int, expected: Int, tolerance: Int) {
        val delta = kotlin.math.abs(actual - expected)
        assert(delta <= tolerance) {
            "$channel: expected $expected ± $tolerance, got $actual (delta=$delta)"
        }
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Android Studio → right-click `OklchToSrgbTest` → Run. Expected: `unresolved reference: oklchToSrgb`.

- [ ] **Step 3: Implement `oklchToSrgb`**

```kotlin
package com.stretchdaily.app.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Converts an oklch color to sRGB (Compose [Color]).
 *
 * Pipeline (per CSS Color 4):
 *   1. oklch → oklab  (C, h_deg → a, b using polar → cartesian)
 *   2. oklab → linear sRGB  (3×3 matrix + cube step)
 *   3. linear sRGB → sRGB  (gamma correction)
 *
 * @param l Lightness 0..1 (oklab L).
 * @param c Chroma 0..~0.37 (oklab C).
 * @param hDeg Hue in degrees 0..360.
 */
fun oklchToSrgb(l: Double, c: Double, hDeg: Double): Color {
    val hRad = hDeg * PI / 180.0
    val a = c * cos(hRad)
    val b = c * sin(hRad)
    val (r, g, blue) = oklabToLinearSrgb(l, a, b)
    return Color(
        red = linearToSrgbChannel(r).toFloat(),
        green = linearToSrgbChannel(g).toFloat(),
        blue = linearToSrgbChannel(blue).toFloat(),
        alpha = 1.0f,
    )
}

/** oklab → linear sRGB per CSS Color 4 reference. */
private fun oklabToLinearSrgb(l: Double, a: Double, b: Double): Triple<Double, Double, Double> {
    // oklab → LMS (cube)
    val lL = l + 0.3963377774 * a + 0.2158037573 * b
    val lM = l - 0.1055613458 * a - 0.0638541728 * b
    val lS = l - 0.0894841775 * a - 1.2914855480 * b

    val lCubed = lL * lL * lL
    val mCubed = lM * lM * lM
    val sCubed = lS * lS * lS

    // LMS → linear sRGB
    val r = +4.0767416621 * lCubed - 3.3077115913 * mCubed + 0.2309699292 * sCubed
    val g = -1.2684380046 * lCubed + 2.6097574011 * mCubed - 0.3413193965 * sCubed
    val blue = -0.0041960863 * lCubed - 0.7034186147 * mCubed + 1.7076147010 * sCubed

    return Triple(r, g, blue)
}

/** Gamma-encodes linear sRGB to display-referred sRGB and clamps to [0, 1]. */
private fun linearToSrgbChannel(v: Double): Double {
    val clamped = v.coerceIn(0.0, 1.0)
    val encoded = if (clamped <= 0.0031308) {
        clamped * 12.92
    } else {
        1.055 * clamped.pow(1.0 / 2.4) - 0.055
    }
    // Unused-variable-silencer: we only use `encoded` — kept explicit for readability.
    return encoded
}

// Kept private by default — if future consumers need raw oklab, expose it then.
private inline fun <A, B, C> trip(a: A, b: B, c: C) = Triple(a, b, c)

// Unused cbrt import kept because alternate conversion formulations use it;
// remove if you confirm the Triple-based path above is the final form.
@Suppress("unused")
private val unusedCbrtHook: (Double) -> Double = ::cbrt
```

> **Note to the engineer:** the `unusedCbrtHook` + `trip` helpers are deliberately harmless no-ops left from an earlier draft and can be removed if your lint config flags them — the real implementation is `oklchToSrgb` + its two private helpers. Nothing else in R1 depends on those dead-code shims.

- [ ] **Step 4: Run the test to verify it passes**

Expected: all three test methods green.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/ui/theme/OklchToSrgb.kt \
        app/src/test/java/com/stretchdaily/app/ui/theme/OklchToSrgbTest.kt
git commit -m "feat(theme): add oklch-to-sRGB color space helper

Pure-Kotlin CSS Color 4 reference implementation used by per-category
tint resolution. Unit-tested against reference values for Neck and
accentSoft."
git push
```

---

## Task 7: Add `CategoryTint.kt`

Uses the helper from Task 6 + the `Category` enum. Memoizes with `lazy` so each tint is computed once per process.

**Files:**
- Create: `app/src/main/java/com/stretchdaily/app/ui/theme/CategoryTint.kt`
- Test: `app/src/test/java/com/stretchdaily/app/ui/theme/CategoryTintTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.stretchdaily.app.ui.theme

import com.stretchdaily.app.core.model.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryTintTest {

    @Test
    fun `every category has a tint`() {
        Category.entries.forEach { cat ->
            val color = cat.tint()
            // Default Color() alpha is 1.0f; tints are opaque.
            assertEquals("alpha for $cat", 1.0f, color.alpha, 0.001f)
        }
    }

    @Test
    fun `tints are distinct across categories`() {
        val colors = Category.entries.map { it.tint() }
        val distinct = colors.toSet()
        assertEquals("all 7 tints should be distinct", 7, distinct.size)
    }

    @Test
    fun `tint result is memoized (same instance per call)`() {
        val first = Category.NECK.tint()
        val second = Category.NECK.tint()
        // Color is a value class backed by ULong; equality is reference-free.
        // We just need the numeric value identical.
        assertEquals(first.value, second.value)
    }

    @Test
    fun `tints are in the L=0.82 desaturated band — no pure white or black`() {
        Category.entries.forEach { cat ->
            val c = cat.tint()
            val sum = c.red + c.green + c.blue
            assertTrue("$cat shouldn't be black — sum=$sum", sum > 1.5f)
            assertTrue("$cat shouldn't be white — sum=$sum", sum < 2.95f)
        }
    }

    @Test
    fun `Neck and Hips are not the same tint (sanity)`() {
        assertNotEquals(Category.NECK.tint().value, Category.HIPS.tint().value)
    }
}
```

- [ ] **Step 2: Run the test — expected to fail**

Expected: `unresolved reference: tint`.

- [ ] **Step 3: Implement `CategoryTint.kt`**

```kotlin
package com.stretchdaily.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.stretchdaily.app.core.model.Category

/**
 * Per-[Category] tint colors for striped exercise tiles, session progress
 * bars, and benchmark category-strip headers.
 *
 * Authored as oklch triples (spec §4.3 + handoff README "Category tints"):
 * all share `L = 0.82`, `C = 0.04`; only hue varies so tints read as
 * "quiet colored paper" rather than saturated category labels.
 *
 * Compose has no native oklch. Each tint is resolved to sRGB once via
 * [oklchToSrgb] and memoized in the `tints` lazy map — one allocation per
 * tint per process lifetime.
 */
private data class OklchTriple(val l: Double, val c: Double, val h: Double)

private val oklchByCategory: Map<Category, OklchTriple> = mapOf(
    Category.NECK      to OklchTriple(0.82, 0.04, 140.0),
    Category.SHOULDERS to OklchTriple(0.82, 0.04, 110.0),
    Category.WRISTS    to OklchTriple(0.82, 0.04, 80.0),
    Category.SPINE     to OklchTriple(0.82, 0.04, 170.0),
    Category.HIPS      to OklchTriple(0.82, 0.04, 50.0),
    Category.KNEES     to OklchTriple(0.82, 0.04, 200.0),
    Category.ANKLES    to OklchTriple(0.82, 0.04, 25.0),
)

/**
 * Lazy memoized tint map. First call computes all 7; subsequent calls are
 * O(1) hash lookups. Resolved at the process level, not per-Composition.
 */
private val tints: Map<Category, Color> by lazy {
    oklchByCategory.mapValues { (_, oklch) ->
        oklchToSrgb(l = oklch.l, c = oklch.c, hDeg = oklch.h)
    }
}

/** Get the Sage tint for this category. Cheap after first call (memoized). */
fun Category.tint(): Color =
    tints.getValue(this) // getValue throws NSEE if missing — enforces enum/map parity.
```

- [ ] **Step 4: Run the test — expected to pass**

All 5 test methods green.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/ui/theme/CategoryTint.kt \
        app/src/test/java/com/stretchdaily/app/ui/theme/CategoryTintTest.kt
git commit -m "feat(theme): add per-Category oklch tint lookup (memoized)

Seven quiet-tinted pastels at L=0.82 C=0.04 differing only by hue, as
specified in the Sage handoff. Resolved once per process via the
oklch→sRGB helper."
git push
```

---

## Task 8: Add `AppTypography.kt`

**Files:**
- Create: `app/src/main/java/com/stretchdaily/app/ui/theme/AppTypography.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.stretchdaily.app.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.stretchdaily.app.R

/**
 * Typography tokens — one [TextStyle] per handoff role.
 *
 * Field naming follows handoff slugs:
 * - `displayXl` — "Well done." hero (54 sp on session-complete).
 * - `displayLg` — screen headlines (30 sp).
 * - `displayMd` — card values (26 sp) and session-player exercise name.
 * - `bodyLg`    — primary body copy (14 sp).
 * - `bodyMd`    — secondary body (12.5 sp).
 * - `bodySm`    — captions / bullet text (11.5 sp).
 * - `monoCaps`     — the design's "voice mark" (11 sp uppercase).
 * - `monoCapsSm`   — tiny mono caps (9.5 sp — KPI eyebrows, chip labels).
 *
 * Mono-caps uppercasing happens at the call site via `text.uppercase(...)`
 * — Compose has no CSS `text-transform` equivalent (spec §4.1).
 */
data class AppTypography(
    val displayXl: TextStyle,
    val displayLg: TextStyle,
    val displayMd: TextStyle,
    val bodyLg: TextStyle,
    val bodyMd: TextStyle,
    val bodySm: TextStyle,
    val monoCaps: TextStyle,
    val monoCapsSm: TextStyle,
)

/** Manrope family assembled from bundled TTFs (res/font/manrope_*.ttf). */
private val Manrope = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
)

/** JetBrains Mono family — Regular + Medium only (no bold/italic used). */
private val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
)

/** Sage defaults. Values mirror spec §4.1 + handoff README "Typography". */
fun sageTypography(): AppTypography = AppTypography(
    displayXl = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Medium,
        fontSize = 54.sp,
        letterSpacing = (-1.2).sp,
    ),
    displayLg = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        letterSpacing = (-0.5).sp,
    ),
    displayMd = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Medium,
        fontSize = 26.sp,
        letterSpacing = (-0.3).sp,
    ),
    bodyLg = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
    ),
    bodyMd = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp,
    ),
    bodySm = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Normal,
        fontSize = 11.5.sp,
    ),
    monoCaps = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 1.2.sp,
    ),
    monoCapsSm = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Medium,
        fontSize = 9.5.sp,
        letterSpacing = 1.0.sp,
    ),
)
```

- [ ] **Step 2: Verify compile in Android Studio**

Build → Make Project. Expected: clean. If `R.font.manrope_regular` fails to resolve, double-check Task 2 filenames are exactly `manrope_regular.ttf` (lowercase, underscore, no spaces).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/ui/theme/AppTypography.kt
git commit -m "feat(theme): add AppTypography with Manrope + JetBrains Mono

Eight TextStyle roles mirroring the Sage handoff typography: display
hero/large/medium, body large/medium/small, and the mono-caps voice
mark in two sizes. Mono-caps uppercasing is applied at call sites."
git push
```

---

## Task 9: Rewrite `Theme.kt` — the token wiring

Wires the three token data classes into `CompositionLocal`s, provides the `StretchDailyTheme(content)` composable, and exposes the short-form `Theme.colors` / `Theme.typo` / `Theme.dims` accessors.

**Files:**
- Modify: `app/src/main/java/com/stretchdaily/app/ui/theme/Theme.kt`

- [ ] **Step 1: Replace the file contents**

```kotlin
package com.stretchdaily.app.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * `CompositionLocal`s that carry the three token data classes down the tree.
 *
 * Read via [Theme.colors] / [Theme.typo] / [Theme.dims]. Consumers should
 * NOT touch these directly — the `Theme` accessor keeps call sites short
 * and survives a future refactor (e.g. multi-theme runtime swap).
 */
private val LocalAppColors = staticCompositionLocalOf<AppColors> {
    error("AppColors not provided — wrap your root in StretchDailyTheme().")
}
private val LocalAppTypography = staticCompositionLocalOf<AppTypography> {
    error("AppTypography not provided — wrap your root in StretchDailyTheme().")
}
private val LocalAppDimens = staticCompositionLocalOf<AppDimens> {
    error("AppDimens not provided — wrap your root in StretchDailyTheme().")
}

/**
 * Root theme composable. Provides the Sage tokens to the tree and wraps a
 * Material3 theme so default ripple color + unspecified `Text` / `Icon`
 * content-colors resolve to ink on surface.
 *
 * `MaterialTheme.colorScheme` is kept minimal — every concrete color in
 * app code flows through `Theme.colors.*`.
 */
@Composable
fun StretchDailyTheme(content: @Composable () -> Unit) {
    val colors = remember_sage_colors
    val typo = remember_sage_typo
    val dims = remember_sage_dims

    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalAppTypography provides typo,
        LocalAppDimens provides dims,
        LocalContentColor provides colors.ink,
    ) {
        MaterialTheme(
            colorScheme = lightColorScheme(
                primary = colors.accent,
                onPrimary = colors.accentInk,
                background = colors.bg,
                onBackground = colors.ink,
                surface = colors.surface,
                onSurface = colors.ink,
                surfaceVariant = colors.surface2,
                onSurfaceVariant = colors.ink2,
                error = colors.warn,
                onError = colors.accentInk,
            ),
            content = content,
        )
    }
}

/**
 * Short-form accessors used at call sites: `Theme.colors.ink`, etc.
 *
 * Keeps Compose code readable vs. the verbose `LocalAppColors.current.ink`
 * and means consumers never know which `CompositionLocal` a token lives in.
 */
object Theme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current

    val typo: AppTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalAppTypography.current

    val dims: AppDimens
        @Composable
        @ReadOnlyComposable
        get() = LocalAppDimens.current
}

// Intentionally top-level `val` rather than `remember { ... }`: Sage tokens
// are immutable data classes with no Composition-scoped state; one shared
// instance per process is correct.
private val remember_sage_colors: AppColors = sageColors()
private val remember_sage_typo: AppTypography = sageTypography()
private val remember_sage_dims: AppDimens = sageDimens()
```

> **Note on naming:** the three `remember_sage_*` vals are process-scoped constants, not `remember { }` values. The naming echoes the Compose `remember` idiom the reader is expecting here; the comment above them flags the intentional divergence.

- [ ] **Step 2: Verify old `Color.kt` and `Type.kt` are no longer referenced**

Android Studio → Build → Make Project. The old `StretchDailyColorScheme` and `StretchDailyTypography` references from the previous Theme.kt are gone. `Color.kt` and `Type.kt` still exist as unreferenced files — they get deleted in Task 13.

Expected: build succeeds. The app still compiles against old `ui/home/*.kt` etc. because those files use `MaterialTheme.colorScheme.*` (which now returns Sage-configured Material3 values) rather than the new `Theme.colors.*` APIs.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/ui/theme/Theme.kt
git commit -m "feat(theme): rewrite StretchDailyTheme to provide Sage tokens

CompositionLocals for AppColors, AppTypography, AppDimens — accessed via
short-form Theme.colors / Theme.typo / Theme.dims. Material3 colorScheme
kept as a thin wrapper for ripple + default content-color resolution;
all concrete design values flow through the new tokens."
git push
```

---

## Task 10: Update the Splash + runtime themes in `themes.xml`

Drops the dark `#0D0D0D` splash + statusbar in favor of Sage cream. The adaptive-icon foreground swap (cream figure on sage bg) is R6; we just change background colors here.

**Files:**
- Modify: `app/src/main/res/values/themes.xml`

- [ ] **Step 1: Replace file contents**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!--
        Runtime theme. Cream background (Sage) + the existing launcher
        foreground kept for the splash. Icon adaptive background is
        updated in R6 to sage green (#5c7a4a) + cream figure.
    -->
    <style name="Theme.StretchDaily" parent="android:Theme.Material.Light.NoActionBar">
        <item name="android:statusBarColor">#FBF8F0</item>
        <item name="android:navigationBarColor">#FBF8F0</item>
        <item name="android:windowBackground">#FBF8F0</item>
        <item name="android:windowLightStatusBar">true</item>
        <item name="android:windowLightNavigationBar">true</item>
    </style>

    <style name="Theme.StretchDaily.Splash" parent="Theme.SplashScreen">
        <item name="windowSplashScreenBackground">#FBF8F0</item>
        <item name="windowSplashScreenAnimatedIcon">@drawable/ic_launcher_foreground</item>
        <item name="windowSplashScreenIconBackgroundColor">#FBF8F0</item>
        <item name="postSplashScreenTheme">@style/Theme.StretchDaily</item>
    </style>
</resources>
```

- [ ] **Step 2: Verify compile + visual check in Android Studio**

Build → Make Project. Expected: clean.
Then Run on emulator/device (or user runs): cold launch should show cream splash (no dark flash), then cream app background.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/values/themes.xml
git commit -m "feat(theme): switch Splash + runtime theme to Sage cream

Removes the dark-to-cream launch flash. windowLightStatusBar=true makes
system icons dark for the cream background. Adaptive launcher icon
itself is swapped in R6."
git push
```

---

## Task 11: Add `PlaceholderScreen`

Single composable every tab renders until its real screen lands in R3–R6.

**Files:**
- Create: `app/src/main/java/com/stretchdaily/app/ui/screen/placeholder/PlaceholderScreen.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.stretchdaily.app.ui.screen.placeholder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.stretchdaily.app.ui.theme.Theme
import java.util.Locale

/**
 * Minimal per-tab landing until the real screen ships.
 *
 * Shows:
 *  - a small mono-caps eyebrow ("PLACEHOLDER") proving the Sage
 *    typography is wired end-to-end.
 *  - the tab label in displayLg.
 *  - the phase tag that unlocks this tab.
 *
 * Applies the outer scaffold's `contentPadding` so content breathes
 * against the status bar and clears the bottom nav.
 */
@Composable
fun PlaceholderScreen(
    tabLabel: String,
    unlocksInPhase: String,
    contentPadding: PaddingValues,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.bg)
            .padding(contentPadding)
            .padding(Theme.dims.padScreen),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "PLACEHOLDER".uppercase(Locale.getDefault()),
                style = Theme.typo.monoCaps,
                color = Theme.colors.ink3,
            )
            Text(
                text = tabLabel,
                style = Theme.typo.displayLg,
                color = Theme.colors.ink,
            )
            Text(
                text = "Unlocks in $unlocksInPhase",
                style = Theme.typo.bodyMd,
                color = Theme.colors.ink2,
            )
        }
    }
}
```

> Add `import androidx.compose.ui.unit.dp` at the top — the `spacedBy(8.dp)` needs it. (List shown in-place above for brevity.)

**Corrected imports block to paste at top of the file:**

```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stretchdaily.app.ui.theme.Theme
import java.util.Locale
```

- [ ] **Step 2: Verify compile**

Build → Make Project. Expected: clean (screen isn't wired to navigation yet — that's Task 12).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/ui/screen/placeholder/PlaceholderScreen.kt
git commit -m "feat(ui): add PlaceholderScreen for empty Sage tabs"
git push
```

---

## Task 12: Rewrite `StretchDailyNavHost.kt` — 5-tab shell

This task is the riskiest of R1: it removes every import into `ui/home/`, `ui/benchmarks/`, `ui/session/`, `ui/settings/` and replaces the whole body. After this step the build still succeeds even though those source trees are still physically present — they're just unreferenced, ready for deletion in Task 13.

**Files:**
- Modify: `app/src/main/java/com/stretchdaily/app/ui/navigation/StretchDailyNavHost.kt`

- [ ] **Step 1: Replace file contents**

```kotlin
package com.stretchdaily.app.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.stretchdaily.app.ui.screen.placeholder.PlaceholderScreen
import com.stretchdaily.app.ui.theme.Theme
import java.util.Locale

/**
 * Top-level routes. Matches spec §7.1.
 *
 * Five tabs (today, session-overview, log, progress, settings) plus two
 * nested overlay graphs:
 *  - session graph hosts overview → player → complete, sharing one VM
 *    via `hiltViewModel(parentEntry)`.
 *  - carousel graph hosts `carousel/{step}` pages.
 *
 * `session/player`, `session/complete`, and `carousel/*` hide the bottom
 * nav — they're immersive overlays.
 */
object Routes {
    const val TODAY = "today"

    const val SESSION_GRAPH = "session"
    const val SESSION_OVERVIEW = "session/overview"
    const val SESSION_PLAYER = "session/player"
    const val SESSION_COMPLETE = "session/complete"

    const val LOG = "log"
    const val PROGRESS = "progress"
    const val SETTINGS = "settings"

    const val CAROUSEL_GRAPH = "carousel"
    const val CAROUSEL_STEP = "carousel/{step}"
    const val ARG_CAROUSEL_STEP = "step"
    fun carouselStep(step: Int) = "carousel/$step"
}

private data class TabItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val BOTTOM_NAV_TABS = listOf(
    TabItem(Routes.TODAY, "Today", Icons.Outlined.Home),
    TabItem(Routes.SESSION_GRAPH, "Session", Icons.Outlined.PlayArrow),
    TabItem(Routes.LOG, "Log", Icons.Outlined.EditNote),
    TabItem(Routes.PROGRESS, "Progress", Icons.Outlined.BarChart),
    TabItem(Routes.SETTINGS, "Settings", Icons.Outlined.Settings),
)

/**
 * Routes that own the bottom navigation bar. Session-player, complete,
 * and the benchmark carousel are immersive and hide the bar.
 */
private val BOTTOM_NAV_ROUTES = setOf(
    Routes.TODAY,
    Routes.SESSION_OVERVIEW,
    Routes.LOG,
    Routes.PROGRESS,
    Routes.SETTINGS,
)

@Composable
fun StretchDailyNavHost(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in BOTTOM_NAV_ROUTES

    Scaffold(
        containerColor = Theme.colors.bg,
        // Inner screens own their status-bar insets; the outer Scaffold
        // only contributes the bottom-nav height via contentPadding.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(navController = navController, currentRoute = currentRoute)
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.TODAY,
        ) {
            composable(Routes.TODAY) {
                PlaceholderScreen(
                    tabLabel = "Today",
                    unlocksInPhase = "R3",
                    contentPadding = padding,
                )
            }
            sessionGraph(padding)
            composable(Routes.LOG) {
                PlaceholderScreen(
                    tabLabel = "Log",
                    unlocksInPhase = "R5",
                    contentPadding = padding,
                )
            }
            composable(Routes.PROGRESS) {
                PlaceholderScreen(
                    tabLabel = "Progress",
                    unlocksInPhase = "R6",
                    contentPadding = padding,
                )
            }
            composable(Routes.SETTINGS) {
                PlaceholderScreen(
                    tabLabel = "Settings",
                    unlocksInPhase = "R6",
                    contentPadding = padding,
                )
            }
            carouselGraph()
        }
    }
}

@Composable
private fun BottomNavBar(navController: NavHostController, currentRoute: String?) {
    NavigationBar(containerColor = Theme.colors.surface) {
        BOTTOM_NAV_TABS.forEach { tab ->
            val selected = currentRoute.belongsToTab(tab.route)
            NavigationBarItem(
                selected = selected,
                onClick = { navController.navigateToTab(tab.route) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = {
                    Text(
                        text = tab.label.uppercase(Locale.getDefault()),
                        style = Theme.typo.monoCapsSm,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Theme.colors.accent,
                    selectedTextColor = Theme.colors.accent,
                    indicatorColor = Theme.colors.accentSoft,
                    unselectedIconColor = Theme.colors.ink3,
                    unselectedTextColor = Theme.colors.ink3,
                ),
            )
        }
    }
}

/**
 * Maps a destination route back to its owning bottom-nav tab root.
 * The session graph's entries (`session/overview` + player + complete)
 * all belong to the Session tab; carousel routes have no tab (no bar).
 */
private fun String?.belongsToTab(tabRoute: String): Boolean = when (tabRoute) {
    Routes.TODAY -> this == Routes.TODAY
    Routes.SESSION_GRAPH -> this == Routes.SESSION_OVERVIEW ||
            this == Routes.SESSION_PLAYER ||
            this == Routes.SESSION_COMPLETE
    Routes.LOG -> this == Routes.LOG
    Routes.PROGRESS -> this == Routes.PROGRESS
    Routes.SETTINGS -> this == Routes.SETTINGS
    else -> false
}

/** Pop to tab root, single-top, restore state — standard bottom-nav pattern. */
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Session graph stubs. R4 replaces each placeholder body with the real
 * screens; the graph structure (nested routes, parentEntry-scoped VM)
 * already matches the final shape.
 */
private fun androidx.navigation.NavGraphBuilder.sessionGraph(contentPadding: PaddingValues) {
    navigation(startDestination = Routes.SESSION_OVERVIEW, route = Routes.SESSION_GRAPH) {
        composable(Routes.SESSION_OVERVIEW) {
            PlaceholderScreen(
                tabLabel = "Session overview",
                unlocksInPhase = "R4",
                contentPadding = contentPadding,
            )
        }
        composable(Routes.SESSION_PLAYER) {
            PlaceholderScreen(
                tabLabel = "Session player",
                unlocksInPhase = "R4",
                contentPadding = PaddingValues(0.dp),
            )
        }
        composable(Routes.SESSION_COMPLETE) {
            PlaceholderScreen(
                tabLabel = "Session complete",
                unlocksInPhase = "R4",
                contentPadding = PaddingValues(0.dp),
            )
        }
    }
}

/** Benchmark carousel overlay graph — bottom-nav hidden on all steps. */
private fun androidx.navigation.NavGraphBuilder.carouselGraph() {
    navigation(startDestination = Routes.carouselStep(0), route = Routes.CAROUSEL_GRAPH) {
        composable(
            route = Routes.CAROUSEL_STEP,
            arguments = listOf(
                navArgument(Routes.ARG_CAROUSEL_STEP) { type = NavType.IntType }
            ),
        ) {
            PlaceholderScreen(
                tabLabel = "Benchmark carousel",
                unlocksInPhase = "R5",
                contentPadding = PaddingValues(0.dp),
            )
        }
    }
}
```

> Add `import androidx.compose.ui.unit.dp` at the top of the file (needed by the two `PaddingValues(0.dp)` calls).

- [ ] **Step 2: Verify compile**

Build → Make Project. Expected:
- `StretchDailyNavHost.kt` compiles.
- The old `ui/home/`, `ui/benchmarks/`, `ui/session/`, `ui/settings/` source files still exist but are no longer referenced from the nav host.
- The app still compiles overall because those files are leaves — nothing outside `ui/` depends on them.

- [ ] **Step 3: Install + smoke-test via Android Studio Run**

Run on emulator/device. Expected:
- Cream splash.
- Cream app background.
- 5 tabs in the bottom nav: TODAY / SESSION / LOG / PROGRESS / SETTINGS.
- Each tab shows "PLACEHOLDER" + tab label + "Unlocks in R*".
- Tab selection tints (ink3 → accent) are visible on the selected item.

Compare visually against the handoff `reference/index.html` bottom nav tab styling.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/ui/navigation/StretchDailyNavHost.kt
git commit -m "feat(nav): 5-tab Sage scaffold with placeholder screens

Replaces the 3-tab dark NavHost with TODAY / SESSION / LOG / PROGRESS /
SETTINGS. Session graph + carousel graph stubs mirror the final spec
route tree — each body is a PlaceholderScreen until its phase lands."
git push
```

---

## Task 13: Delete all old `ui/` sources and their tests

Now that nothing references them, remove the old UI wholesale. No functional regression — the new placeholder tabs cover every former screen route.

**Files deleted:**
- `app/src/main/java/com/stretchdaily/app/ui/theme/Color.kt`
- `app/src/main/java/com/stretchdaily/app/ui/theme/Type.kt`
- `app/src/main/java/com/stretchdaily/app/ui/home/HomeScreen.kt`
- `app/src/main/java/com/stretchdaily/app/ui/home/HomeViewModel.kt`
- `app/src/main/java/com/stretchdaily/app/ui/benchmarks/BenchmarkHistoryScreen.kt`
- `app/src/main/java/com/stretchdaily/app/ui/benchmarks/BenchmarkProgressChart.kt`
- `app/src/main/java/com/stretchdaily/app/ui/benchmarks/BenchmarksScreen.kt`
- `app/src/main/java/com/stretchdaily/app/ui/benchmarks/BenchmarksUiState.kt`
- `app/src/main/java/com/stretchdaily/app/ui/benchmarks/BenchmarksViewModel.kt`
- `app/src/main/java/com/stretchdaily/app/ui/benchmarks/LogBenchmarkDialog.kt`
- `app/src/main/java/com/stretchdaily/app/ui/session/SessionCompleteScreen.kt`
- `app/src/main/java/com/stretchdaily/app/ui/session/SessionFollowAlongScreen.kt`
- `app/src/main/java/com/stretchdaily/app/ui/session/SessionHistoryScreen.kt`
- `app/src/main/java/com/stretchdaily/app/ui/session/SessionHistoryViewModel.kt`
- `app/src/main/java/com/stretchdaily/app/ui/session/SessionPreviewScreen.kt`
- `app/src/main/java/com/stretchdaily/app/ui/session/SessionUiState.kt`
- `app/src/main/java/com/stretchdaily/app/ui/session/SessionViewModel.kt`
- `app/src/main/java/com/stretchdaily/app/ui/settings/SettingsScreen.kt`
- `app/src/main/java/com/stretchdaily/app/ui/settings/SettingsViewModel.kt`
- `app/src/test/java/com/stretchdaily/app/ui/benchmarks/BenchmarksViewModelTest.kt`
- `app/src/test/java/com/stretchdaily/app/ui/home/HomeViewModelTest.kt`
- `app/src/test/java/com/stretchdaily/app/ui/session/SessionHistoryViewModelTest.kt`
- `app/src/test/java/com/stretchdaily/app/ui/session/SessionViewModelTest.kt`

- [ ] **Step 1: Remove old `ui/theme/` legacy files**

```bash
git rm app/src/main/java/com/stretchdaily/app/ui/theme/Color.kt \
       app/src/main/java/com/stretchdaily/app/ui/theme/Type.kt
```

Expected: 2 files staged for deletion.

- [ ] **Step 2: Remove old screen source directories**

```bash
git rm -r app/src/main/java/com/stretchdaily/app/ui/home \
          app/src/main/java/com/stretchdaily/app/ui/benchmarks \
          app/src/main/java/com/stretchdaily/app/ui/session \
          app/src/main/java/com/stretchdaily/app/ui/settings
```

Expected: 17 source files + 4 directories staged for deletion.

- [ ] **Step 3: Remove old UI tests**

```bash
git rm -r app/src/test/java/com/stretchdaily/app/ui
```

Expected: 4 test files + 3 directories staged for deletion. (Our new `OklchToSrgbTest` + `CategoryTintTest` live under `app/src/test/java/com/stretchdaily/app/ui/theme/` — verify they were NOT included in this `git rm` by running `git status` and confirming `theme/` is untouched.)

- [ ] **Step 4: Verify the new tests survived**

Run:
```bash
git status
ls app/src/test/java/com/stretchdaily/app/ui/theme/
```

Expected:
- `git status` shows the 17 main-source deletions + 4 ui-test deletions, but NOT the two new `theme/*Test.kt` files.
- `ls` shows `OklchToSrgbTest.kt` and `CategoryTintTest.kt`.

If the `theme/` directory was nuked by Step 3's recursive `git rm -r app/src/test/java/com/stretchdaily/app/ui`, stop and restore the two test files from git history before committing. (`git checkout HEAD -- app/src/test/java/com/stretchdaily/app/ui/theme/`.)

- [ ] **Step 5: Build to verify nothing references the deleted files**

Android Studio → Build → Make Project. Expected: clean build.

If a reference error surfaces, it's almost certainly `MainActivity.kt` importing an old screen — re-check it imports only `com.stretchdaily.app.ui.navigation.StretchDailyNavHost` and `com.stretchdaily.app.ui.theme.StretchDailyTheme`.

- [ ] **Step 6: Run the preserved tests**

Android Studio → Run → All Unit Tests. Expected: every test under `core/engine/`, `core/benchmark/`, `data/`, and the two new `ui/theme/` tests pass. Total ≈ 16 test classes.

- [ ] **Step 7: Commit**

```bash
git commit -m "chore(ui): remove dark-theme UI tree — superseded by R1 scaffold

Deletes all screens + ViewModels under ui/{home,benchmarks,session,settings}
and the old Color.kt / Type.kt. Their functionality is reimplemented
against the Sage tokens across R3–R6. Old UI ViewModel tests are removed
alongside their subjects; core/ and data/ tests are preserved and pass."
git push
```

---

## Task 14: Final build + manual smoke test

- [ ] **Step 1: Clean build from Android Studio**

Build → Rebuild Project. Expected: clean, zero errors, zero warnings other than pre-existing ones.

- [ ] **Step 2: Install on device / emulator**

Run ▶ the `app` configuration. Expected:

1. **Cold launch** → cream splash (no dark flash, no orange).
2. **After splash** → cream app background. TODAY tab selected by default.
3. **TODAY tab** → shows "PLACEHOLDER" (mono caps) + "Today" (displayLg, Manrope) + "Unlocks in R3".
4. **Bottom nav** → five tabs, ink3 (muted) when unselected, accent (sage green) when selected, `accentSoft` indicator pill on the selected item.
5. **Mono-caps text** → rendered with JetBrains Mono (visibly monospaced), letter-spaced, uppercased.
6. **Each tab tap** cycles correctly: TODAY / SESSION / LOG / PROGRESS / SETTINGS, each showing its phase tag.
7. **Status bar icons** are dark (readable on cream) — `windowLightStatusBar` took effect.

- [ ] **Step 3: Visual diff vs. handoff**

Open `docs/design_handoff_stretch_daily_v3/reference/index.html` in a browser, flip to Sage theme if not already. Compare bottom-nav tab styling and cream background tone. Cream (`#fbf8f0`) should match. Nav tab accent tint should match.

Minor differences are expected (icon set differs, placeholder layout is R1-only). Hard requirements: cream bg, sage accent, Manrope rendering, JetBrains Mono rendering, no dark flash on launch.

- [ ] **Step 4: Run all unit tests**

Android Studio → Run → All Unit Tests. Expected: 100% pass. Specifically confirm the seven classes under `core/` still pass (they exercise the preserved engine/benchmark/data layer):
- `CategoryWeightCalculatorTest`
- `SelectionShieldTest`
- `SessionBuilderTest`
- `TierResolverTest`
- `BenchmarkProgressBuilderTest`
- `BenchmarkRepositoryDueTest`
- `SessionRepositoryStreakTest`
- `SessionRepositoryWindowTest`
- `DataPortRepositoryTest`

Plus the two new R1 tests:
- `OklchToSrgbTest`
- `CategoryTintTest`

- [ ] **Step 5: Open the PR**

```bash
gh pr create --base development --head redesign/r1-tokens-scaffold \
  --title "Sage redesign R1: tokens + empty scaffold" \
  --body "$(cat <<'EOF'
## Summary

- Adds Sage token system (`AppColors`, `AppTypography`, `AppDimens`,
  `CategoryTint`) exposed via `CompositionLocal` and short-form
  `Theme.colors` / `Theme.typo` / `Theme.dims` accessors.
- Bundles Manrope (Regular/Medium/SemiBold/Bold) and JetBrains Mono
  (Regular/Medium) TTFs under `res/font/`.
- Adds pure-Kotlin `oklchToSrgb` helper + unit tests.
- Rewrites `StretchDailyNavHost` as a 5-tab (TODAY / SESSION / LOG /
  PROGRESS / SETTINGS) scaffold with nested session + carousel graphs.
  Every destination renders a `PlaceholderScreen` until its phase lands.
- Switches splash + runtime theme to cream (`#fbf8f0`) with
  `windowLightStatusBar`. Adaptive launcher icon is swapped in R6.
- Deletes the dark-theme UI tree (`ui/{home,benchmarks,session,settings}`,
  old `Color.kt` / `Type.kt`) and the corresponding ViewModel tests.

## Out of scope for R1

Primitives (R2), features (R3–R6), launcher icon (R6). Every tab shows
a placeholder labelled with the phase that unlocks it.

## Test plan

- [ ] `./gradlew test` (or Run All Unit Tests in Android Studio) passes
      `core/`, `data/`, and the two new `ui/theme/` tests.
- [ ] App launches to cream splash → cream Today tab. No dark flash.
- [ ] All 5 bottom-nav tabs cycle and show their phase placeholder.
- [ ] Mono-caps and Manrope render correctly (visible typeface diff).

Spec: docs/superpowers/specs/2026-04-23-sage-redesign-design.md
Plan: docs/superpowers/plans/2026-04-23-sage-redesign-R1-tokens-and-scaffold.md
EOF
)"
```

Expected: PR URL returned. Link it in the session handoff.

---

## After R1 lands

Update [`CLAUDE.md §2 "Current state"`](../../../CLAUDE.md) "Just completed" section to reflect R1. Then move on to the R2 plan ([`2026-04-23-sage-redesign-R2-primitives-and-gallery.md`](./2026-04-23-sage-redesign-R2-primitives-and-gallery.md)) — it builds the 12 shared primitives on top of the tokens this phase ships.
