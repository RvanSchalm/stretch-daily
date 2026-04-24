# Sage redesign — Phase R2: Shared primitives + gallery — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the 12 shared Compose primitives defined in spec §5 and wire them into a hidden debug-only `ComponentGalleryScreen` used for visual QA against the handoff prototype. After this phase the app still has 5 placeholder tabs, but `ui/components/` is fully populated and each primitive is visible (in the debug gallery) and demonstrably correct.

**Architecture:** One file per primitive under `ui/components/`, each self-contained and consuming only tokens from R1 (`Theme.colors`, `Theme.typo`, `Theme.dims`). Ordering matters — later primitives reuse earlier ones (`BigChart` uses `MonoCaps`; `WeekStrip` uses day state primitive). The gallery is registered in the NavHost under a debug-only route; the entry point is a long-press on the Settings placeholder screen.

**Tech Stack:** Kotlin 2.0, Compose, Compose Canvas drawing, Material 3 `ModalBottomSheet`, JUnit 4 for the handful of primitives with unit-testable logic.

**Spec:** [`docs/superpowers/specs/2026-04-23-sage-redesign-design.md`](../specs/2026-04-23-sage-redesign-design.md) sections 5, 10.3, 11.2.
**Handoff reference:** [`docs/design_handoff_stretch_daily_v3/reference/components.jsx`](../../design_handoff_stretch_daily_v3/reference/components.jsx) for the JS equivalents of every primitive.

**Build verification:** Gradle CLI blocked on this machine — verify every commit via Android Studio → Build → Make Project. See [`CLAUDE.md §2 "Known issues"`](../../../CLAUDE.md).

**Prerequisite:** R1 is merged to `development`. `Theme.colors`, `Theme.typo`, `Theme.dims`, and `Category.tint()` are available.

---

## File structure for R2

**New outline SVG icons under `app/src/main/res/drawable/`:**
- `ic_play.xml`, `ic_pause.xml`, `ic_skip_next.xml`, `ic_skip_prev.xml`
- `ic_swap.xml`, `ic_sparkle.xml`, `ic_flame.xml`
- `ic_chevron_right.xml`, `ic_chevron_down.xml`
- `ic_close.xml`, `ic_check.xml`
- `ic_benchmark.xml` (for nav — we keep the Material `EditNote` used in R1 as fallback; add `ic_benchmark.xml` only if the visual diff warrants)

**New under `app/src/main/java/com/stretchdaily/app/ui/components/`:**
- `MonoCaps.kt` — mono-caps text primitive (2 variants).
- `AppIcon.kt` — `IconName` enum + `AppIcon(name, ...)` composable.
- `Pill.kt` — `Pill(...)` + `CircleButton(...)` + shared variant enums.
- `Sheet.kt` — Material 3 `ModalBottomSheet` wrapper with token overrides.
- `ExerciseTile.kt` — striped category-tinted placeholder (two size variants).
- `CatChip.kt` — hairline category chip.
- `BandPill.kt` — tier-colored pill (five discrete shades).
- `Sparkline.kt` — 50×14 dp polyline canvas.
- `BigChart.kt` — 130 dp-tall benchmark chart canvas.
- `WeekStrip.kt` — 7 day-state squares, Monday-start.
- `SegmentProgress.kt` — N-segment progress rail.
- `KpiCard.kt` — surface card with eyebrow + icon + value + suffix.

**New under `app/src/main/java/com/stretchdaily/app/ui/debug/`:**
- `ComponentGalleryScreen.kt` — debug-only scrollable gallery of every primitive.

**New under `app/src/test/java/com/stretchdaily/app/ui/components/`:**
- `WeekStripHelpersTest.kt` — week-date computation logic (Monday-start).
- `SegmentProgressHelpersTest.kt` — segment state bucketing (past/current/future).

**Modified:**
- `app/src/main/java/com/stretchdaily/app/ui/navigation/StretchDailyNavHost.kt` — add hidden debug route + long-press entry on Settings placeholder.
- `app/src/main/java/com/stretchdaily/app/ui/screen/placeholder/PlaceholderScreen.kt` — accept optional `onLongPress` lambda.

---

## Task 1: Create the `redesign/r2-primitives` branch

- [ ] **Step 1: Verify clean state on `development`**

```bash
git checkout development && git pull
git status
```

Expected: clean tree on `development`, pulled R1.

- [ ] **Step 2: Create branch + push**

```bash
git checkout -b redesign/r2-primitives
git push -u origin redesign/r2-primitives
```

---

## Task 2: Add outline SVG icons under `res/drawable/`

The handoff's `Icon` component uses outline SVGs. We port the set we need now; more can be added when R3–R6 surfaces them.

**Files to create under `app/src/main/res/drawable/`:**
- `ic_play.xml`, `ic_pause.xml`, `ic_skip_next.xml`, `ic_skip_prev.xml`
- `ic_swap.xml`, `ic_sparkle.xml`, `ic_flame.xml`
- `ic_chevron_right.xml`, `ic_chevron_down.xml`
- `ic_close.xml`, `ic_check.xml`

- [ ] **Step 1: Source the vectors**

For each icon, copy the SVG `path` data from [Material Symbols Outlined](https://fonts.google.com/icons) or [Lucide](https://lucide.dev/) (both MIT/Apache — both acceptable). Target 24×24 px viewport. Stroke-width is irrelevant here because we'll render as filled vectors with `android:fillColor="#000000"` and tint at the call site via `tint = Theme.colors.ink3`.

Alternate: use Android Studio's *File → New → Vector Asset → Clip Art* picker — pick the Material outlined variant, set size to 24dp, color to black (we'll tint programmatically), save under `res/drawable/`.

- [ ] **Step 2: Example file — `ic_play.xml`**

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#000000"
        android:pathData="M8,5v14l11,-7z" />
</vector>
```

Repeat the pattern for every icon in the list. Each file's `<path android:pathData>` is replaced with the SVG path data for that icon.

- [ ] **Step 3: Verify compile**

Android Studio → Build → Make Project. Expected: clean. Each icon is reachable as `R.drawable.ic_*`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/drawable/ic_*.xml
git commit -m "feat(ui): add outline SVG icon set under res/drawable

Eleven icons (play, pause, skip next/prev, swap, sparkle, flame,
chevron right/down, close, check) used by R2 primitives and R3–R6
screens. Black fillColor — tinted at call sites via AppIcon."
git push
```

---

## Task 3: Add `MonoCaps` primitive

The design's voice mark. Wraps `Text` with `uppercase(Locale.getDefault())` and the mono-caps TextStyle from `Theme.typo`.

**Files:**
- Create: `app/src/main/java/com/stretchdaily/app/ui/components/MonoCaps.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.stretchdaily.app.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.stretchdaily.app.ui.theme.Theme
import java.util.Locale

/** Size variant for [MonoCaps]. `Regular` = 11 sp, `Small` = 9.5 sp. */
enum class MonoCapsSize { Regular, Small }

/**
 * The design's "voice mark" — JetBrains Mono, uppercase, letter-spaced.
 *
 * Used for eyebrows, KPI labels, chip labels, session-player state
 * captions, and nav tab labels. Consumers pass mixed-case strings; this
 * composable applies `uppercase(Locale.getDefault())`.
 */
@Composable
fun MonoCaps(
    text: String,
    size: MonoCapsSize = MonoCapsSize.Regular,
    color: Color = Theme.colors.ink3,
    modifier: Modifier = Modifier,
) {
    val style: TextStyle = when (size) {
        MonoCapsSize.Regular -> Theme.typo.monoCaps
        MonoCapsSize.Small -> Theme.typo.monoCapsSm
    }
    Text(
        text = text.uppercase(Locale.getDefault()),
        style = style,
        color = color,
        modifier = modifier,
    )
}
```

- [ ] **Step 2: Verify compile**

Build → Make Project. Expected: clean.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/ui/components/MonoCaps.kt
git commit -m "feat(ui): add MonoCaps primitive"
git push
```

---

## Task 4: Add `AppIcon` wrapper

Wraps `Icon` so screens don't reference `R.drawable.*` directly, and adds a consistent enum-based API.

**Files:**
- Create: `app/src/main/java/com/stretchdaily/app/ui/components/AppIcon.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.stretchdaily.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stretchdaily.app.R
import com.stretchdaily.app.ui.theme.Theme

/**
 * Enum wrapper over the bundled outline icon set (res/drawable/ic_*.xml).
 *
 * Screens reference [IconName.Play] / [IconName.Swap] / etc. rather than
 * `R.drawable.ic_play` directly — keeps the icon surface small and
 * refactor-friendly.
 */
enum class IconName(@DrawableRes val res: Int) {
    Play(R.drawable.ic_play),
    Pause(R.drawable.ic_pause),
    SkipNext(R.drawable.ic_skip_next),
    SkipPrev(R.drawable.ic_skip_prev),
    Swap(R.drawable.ic_swap),
    Sparkle(R.drawable.ic_sparkle),
    Flame(R.drawable.ic_flame),
    ChevronRight(R.drawable.ic_chevron_right),
    ChevronDown(R.drawable.ic_chevron_down),
    Close(R.drawable.ic_close),
    Check(R.drawable.ic_check),
}

/**
 * Renders an outline icon from the bundled set.
 *
 * [contentDescription] is non-null only on interactive uses — purely
 * decorative icons (e.g. inside a labeled button) pass `null`. Matches
 * the accessibility discipline established in Phase 8 of the original
 * project.
 */
@Composable
fun AppIcon(
    name: IconName,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    tint: Color = Theme.colors.ink,
) {
    Icon(
        painter = painterResource(id = name.res),
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier.then(Modifier),
        // size is applied through modifier at call sites when needed;
        // default sizing is left to the consumer to keep the API minimal.
    )
    // NOTE: if call sites consistently want the icon sized via this API
    // rather than modifier, change Modifier.size(size) here. For R2 we
    // use the Material3 `Icon` defaults (24dp square) unless the call
    // site overrides with Modifier.size(...).
}
```

> **Simplification:** the `size` parameter above is defensive but unused. If your lint config flags it, remove the parameter entirely and let call sites apply `Modifier.size(...)`. Either choice is fine; the simpler one is to drop `size` now and re-add if R3 asks for it.

- [ ] **Step 2: Verify compile**

Build → Make Project. Expected: clean.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/ui/components/AppIcon.kt
git commit -m "feat(ui): add AppIcon wrapper and IconName enum"
git push
```

---

## Task 5: Add `Pill` + `CircleButton` chrome primitives

Two closely related primitives — kept in one file because they share variant enums and look/feel.

**Files:**
- Create: `app/src/main/java/com/stretchdaily/app/ui/components/Pill.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.stretchdaily.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stretchdaily.app.ui.theme.Theme

/** Visual variants for [Pill]. */
enum class PillVariant { Accent, Neutral, DashedOutline }

/**
 * Pill button. Rounded to `radiusPill`, 18 dp vertical padding by default.
 *
 * Variants:
 *  - [PillVariant.Accent]  — accent bg + accentInk text ("Begin session").
 *  - [PillVariant.Neutral] — bg2 bg + ink text (secondary actions).
 *  - [PillVariant.DashedOutline] — transparent + dashed accent border.
 *    Used for "Perform at your own pace" on rep-based session moves.
 *
 * Dashed borders are rare in Compose Material; we use `Modifier.border`
 * with a `BorderStroke`. If the dashed style needs tweaking for R4, swap
 * to a `Modifier.drawBehind` with `PathEffect.dashPathEffect(...)`.
 */
@Composable
fun Pill(
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: IconName? = null,
    variant: PillVariant = PillVariant.Accent,
) {
    val bg = when (variant) {
        PillVariant.Accent -> Theme.colors.accent
        PillVariant.Neutral -> Theme.colors.bg2
        PillVariant.DashedOutline -> Color.Transparent
    }
    val fg = when (variant) {
        PillVariant.Accent -> Theme.colors.accentInk
        PillVariant.Neutral -> Theme.colors.ink
        PillVariant.DashedOutline -> Theme.colors.accent
    }
    val borderMod: Modifier = when (variant) {
        PillVariant.DashedOutline -> Modifier.border(
            border = BorderStroke(2.dp, Theme.colors.accent),
            shape = RoundedCornerShape(Theme.dims.radiusPill),
        )
        else -> Modifier
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(Theme.dims.radiusPill))
            .background(bg)
            .then(borderMod)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
    ) {
        if (leadingIcon != null) {
            AppIcon(name = leadingIcon, contentDescription = null, tint = fg)
        }
        Text(text = label, style = Theme.typo.bodyLg, color = fg)
    }
}

/** Size of a [CircleButton] — 36 / 44 / 52 / 58 dp. */
enum class CircleButtonSize(val dp: Dp) {
    Small(36.dp),
    Medium(44.dp),
    Large(52.dp),
    Large58(58.dp),
}

enum class CircleButtonVariant { Bg2, Ink, Accent }

/**
 * Round icon-only button. Three variants:
 *  - [CircleButtonVariant.Bg2] — `bg2` background + `ink` icon. Used for
 *    close (×), swap, nav chevrons.
 *  - [CircleButtonVariant.Ink] — `ink` background + `bg` icon. Used for
 *    session-player "next".
 *  - [CircleButtonVariant.Accent] — `accent` background + `bg` icon. Used
 *    for the big "Begin session" CTA on the dashboard today card.
 */
@Composable
fun CircleButton(
    onClick: () -> Unit,
    icon: IconName,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: CircleButtonSize = CircleButtonSize.Medium,
    variant: CircleButtonVariant = CircleButtonVariant.Bg2,
) {
    val (bg, fg) = when (variant) {
        CircleButtonVariant.Bg2 -> Theme.colors.bg2 to Theme.colors.ink
        CircleButtonVariant.Ink -> Theme.colors.ink to Theme.colors.bg
        CircleButtonVariant.Accent -> Theme.colors.accent to Theme.colors.bg
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick),
    ) {
        AppIcon(name = icon, contentDescription = contentDescription, tint = fg)
    }
}
```

- [ ] **Step 2: Verify compile**

Build → Make Project. Expected: clean.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/ui/components/Pill.kt
git commit -m "feat(ui): add Pill + CircleButton chrome primitives

Pill in three variants (Accent / Neutral / DashedOutline) and
CircleButton in four sizes (36/44/52/58) × three variants
(Bg2 / Ink / Accent). Covers every button style in the Sage handoff."
git push
```

---

## Task 6: Add `Sheet` primitive

Thin wrapper over Material 3 `ModalBottomSheet` with token overrides. Per spec §5/Sub-Q, default to M3; hand-roll only if styling fights.

**Files:**
- Create: `app/src/main/java/com/stretchdaily/app/ui/components/Sheet.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.stretchdaily.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stretchdaily.app.ui.theme.Theme

/**
 * Bottom sheet for swap pickers, log forms, and confirmations.
 *
 * Wraps Material 3 [ModalBottomSheet] with Sage token overrides:
 *  - `containerColor` = `Theme.colors.bg`
 *  - `scrimColor` = black × 0.45 (close to handoff's backdrop)
 *  - `shape` = top-only `radiusLg`
 *
 * Content is wrapped in 20 dp horizontal + 20 dp vertical padding so
 * call sites only provide the inner column.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Sheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = Theme.colors.bg,
        contentColor = Theme.colors.ink,
        shape = RoundedCornerShape(
            topStart = Theme.dims.radiusLg,
            topEnd = Theme.dims.radiusLg,
        ),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            content()
        }
    }
}
```

- [ ] **Step 2: Verify compile**

Build → Make Project. Expected: clean. If `ExperimentalMaterial3Api` opt-in triggers additional warnings, confirm the `@OptIn` annotation covers the whole file.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/ui/components/Sheet.kt
git commit -m "feat(ui): add Sheet wrapper over M3 ModalBottomSheet"
git push
```

---

## Task 7: Add `ExerciseTile` striped placeholder

The category-tinted diagonal-striped tile used as exercise placeholder art in session overview and player.

**Files:**
- Create: `app/src/main/java/com/stretchdaily/app/ui/components/ExerciseTile.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.stretchdaily.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.ui.theme.Theme
import com.stretchdaily.app.ui.theme.tint

/** Size variant for [ExerciseTile]. */
enum class ExerciseTileSize { Small56, Large4x3 }

/**
 * Category-tinted diagonal-striped placeholder tile.
 *
 * Draws:
 *  1. Base fill with `category.tint()`.
 *  2. 45° diagonal stripes at 4 dp spacing, slightly darker tint.
 *  3. (Large only) mono-caps footer "{CATEGORY} · ANIMATION / .WEBP
 *     PLACEHOLDER" so the shipping production look matches the handoff.
 *
 * Small56: 56×56 dp, used in session-overview exercise rows.
 * Large4x3: full-width, 4:3 aspect, used in session-player hero slot.
 */
@Composable
fun ExerciseTile(
    category: Category,
    size: ExerciseTileSize,
    modifier: Modifier = Modifier,
) {
    val tileColor = category.tint()
    val stripeColor = tileColor.darken(0.12f)

    when (size) {
        ExerciseTileSize.Small56 -> Box(
            modifier = modifier
                .size(56.dp)
                .clip(RoundedCornerShape(Theme.dims.radiusSm))
                .background(tileColor),
        ) {
            StripedCanvas(stripeColor = stripeColor, stripeSpacingDp = 4)
        }

        ExerciseTileSize.Large4x3 -> BoxWithConstraints(
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(RoundedCornerShape(Theme.dims.radiusLg))
                .background(tileColor),
        ) {
            StripedCanvas(stripeColor = stripeColor, stripeSpacingDp = 6)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter,
            ) {
                MonoCaps(
                    text = "${category.displayName} · animation · .webp placeholder",
                    size = MonoCapsSize.Small,
                    color = Theme.colors.ink3,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
        }
    }
}

/** Diagonal stripe layer drawn on top of the base tint. */
@Composable
private fun StripedCanvas(stripeColor: Color, stripeSpacingDp: Int) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val spacing = stripeSpacingDp.dp.toPx()
        val diag = size.width + size.height
        var x = -size.height
        while (x < diag) {
            drawLine(
                color = stripeColor,
                start = Offset(x = x, y = 0f),
                end = Offset(x = x + size.height, y = size.height),
                strokeWidth = 1.5f,
            )
            x += spacing
        }
    }
}

/** Subtracts a small amount of lightness to darken a tint for stripe contrast. */
private fun Color.darken(amount: Float): Color =
    Color(
        red = (red - amount).coerceAtLeast(0f),
        green = (green - amount).coerceAtLeast(0f),
        blue = (blue - amount).coerceAtLeast(0f),
        alpha = alpha,
    )

// Pulls in Modifier.padding via an inline re-import used only by the
// Large4x3 branch. Kept at the bottom so the primary imports read clean.
private val _forcePaddingImport = androidx.compose.foundation.layout.padding
```

> **Clean-up note:** the `_forcePaddingImport` line is a reminder to add `import androidx.compose.foundation.layout.padding` to the imports block (used by the `Modifier.padding(bottom = 12.dp)` inside the Large branch). Remove the reminder line before committing.

- [ ] **Step 2: Clean up imports**

Replace the imports block to include `padding`:

```kotlin
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.ui.theme.Theme
import com.stretchdaily.app.ui.theme.tint
```

Delete the trailing `_forcePaddingImport` val and its comment.

- [ ] **Step 3: Verify compile**

Build → Make Project. Expected: clean.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/ui/components/ExerciseTile.kt
git commit -m "feat(ui): add ExerciseTile striped placeholder (Small56 + Large4x3)"
git push
```

---

## Task 8: Add `CatChip`

Tiny pill with a 6×6 tint dot + mono-caps category label. Used in session-overview rows and analytics cards.

**Files:**
- Create: `app/src/main/java/com/stretchdaily/app/ui/components/CatChip.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.stretchdaily.app.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.ui.theme.Theme
import com.stretchdaily.app.ui.theme.tint

/**
 * Hairline-outlined category pill.
 *
 * Layout: 6 dp tint dot + mono-caps category label. Used as a
 * sub-identifier on session-overview rows and analytics cards.
 */
@Composable
fun CatChip(
    category: Category,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .clip(RoundedCornerShape(Theme.dims.radiusPill))
            .border(
                width = 1.dp,
                color = Theme.colors.line,
                shape = RoundedCornerShape(Theme.dims.radiusPill),
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(category.tint()),
        )
        MonoCaps(
            text = category.displayName,
            size = MonoCapsSize.Small,
            color = Theme.colors.ink2,
        )
    }
}
```

- [ ] **Step 2: Verify compile**

Build → Make Project. Expected: clean.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/ui/components/CatChip.kt
git commit -m "feat(ui): add CatChip category identifier pill"
git push
```

---

## Task 9: Add `BandPill`

Tier-labeled pill with five discrete colors. Used on benchmark log + analytics cards to show the user's current band.

**Files:**
- Create: `app/src/main/java/com/stretchdaily/app/ui/components/BandPill.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.stretchdaily.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.stretchdaily.app.core.model.FlexibilityTier
import com.stretchdaily.app.ui.theme.Theme

/**
 * Tier-colored pill labeled by [FlexibilityTier.displayName].
 *
 * Five discrete colors shading from warn → accent along the tier axis
 * (stiff/below-average/average/flexible/very flexible). Warn tier uses
 * `warn` at 0.85 alpha; accent tier uses `accent` at full.
 */
@Composable
fun BandPill(
    tier: FlexibilityTier,
    modifier: Modifier = Modifier,
) {
    val bg: Color = when (tier) {
        FlexibilityTier.STIFF -> Theme.colors.warn.copy(alpha = 0.90f)
        FlexibilityTier.BELOW_AVERAGE -> Theme.colors.warn.copy(alpha = 0.55f)
        FlexibilityTier.AVERAGE -> Theme.colors.ink3.copy(alpha = 0.35f)
        FlexibilityTier.FLEXIBLE -> Theme.colors.accent.copy(alpha = 0.60f)
        FlexibilityTier.VERY_FLEXIBLE -> Theme.colors.accent
    }
    val fg: Color = when (tier) {
        FlexibilityTier.AVERAGE -> Theme.colors.ink
        else -> Theme.colors.accentInk
    }
    MonoCaps(
        text = tier.displayName,
        size = MonoCapsSize.Small,
        color = fg,
        modifier = modifier
            .clip(RoundedCornerShape(Theme.dims.radiusPill))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
```

- [ ] **Step 2: Verify compile**

Build → Make Project. Expected: clean.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/ui/components/BandPill.kt
git commit -m "feat(ui): add BandPill tier-colored pill (five discrete shades)"
git push
```

---

## Task 10: Add `Sparkline`

Tiny 50×14 dp Canvas polyline. Used in the benchmark log rows — last 6 months of values.

**Files:**
- Create: `app/src/main/java/com/stretchdaily/app/ui/components/Sparkline.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.stretchdaily.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.stretchdaily.app.ui.theme.Theme

/**
 * 50×14 dp polyline sparkline.
 *
 * [values] are expected to be normalized 0..1 (consumer does the scaling
 * — typically divides raw values by benchmark's max band). Renders:
 *   - 1.5 dp `ink3` polyline.
 *   - 2.5 dp `accent` dot at the last point.
 *
 * Empty state (0 or 1 values): dashed flat line at midheight.
 */
@Composable
fun Sparkline(
    values: List<Double>,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(width = 50.dp, height = 14.dp)) {
        if (values.size < 2) {
            // Empty state: dashed flat line.
            drawLine(
                color = Theme.colors.line,
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f), 0f),
            )
            return@Canvas
        }

        val stepX = size.width / (values.size - 1).toFloat()
        val points = values.mapIndexed { index, v ->
            Offset(
                x = index * stepX,
                y = size.height - (v.toFloat().coerceIn(0f, 1f) * size.height),
            )
        }

        // Polyline: draw each segment.
        for (i in 1 until points.size) {
            drawLine(
                color = Theme.colors.ink3,
                start = points[i - 1],
                end = points[i],
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }

        // Last-point marker.
        drawCircle(
            color = Theme.colors.accent,
            radius = 2.5.dp.toPx(),
            center = points.last(),
        )
    }
}
```

- [ ] **Step 2: Remove the unused `Color` + `Stroke` imports**

Kotlin linting on the final file: trim `import androidx.compose.ui.graphics.Color` and `import androidx.compose.ui.graphics.drawscope.Stroke` — they aren't referenced above. Final imports block:

```kotlin
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.stretchdaily.app.ui.theme.Theme
```

- [ ] **Step 3: Verify compile**

Build → Make Project. Expected: clean.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/ui/components/Sparkline.kt
git commit -m "feat(ui): add Sparkline 50x14 dp polyline primitive"
git push
```

---

## Task 11: Add `BigChart`

The benchmark progress chart used on analytics cards. Consumes a
`ProgressSeries` (top-level `internal data class` in
`core/benchmark/BenchmarkProgressBuilder.kt`, preserved from the current
codebase — `data class ProgressSeries(val points: List<ProgressPoint>)`
where `ProgressPoint(xRatio: Float, yRatio: Float, timestampMillis: Long,
tier: FlexibilityTier)`). `internal` visibility is fine because both
`BigChart` and every consumer live in the `app` module.

**Files:**
- Create: `app/src/main/java/com/stretchdaily/app/ui/components/BigChart.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.stretchdaily.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.stretchdaily.app.core.benchmark.ProgressSeries
import com.stretchdaily.app.core.model.FlexibilityTier
import com.stretchdaily.app.ui.theme.Theme

/**
 * 130 dp-tall full-width benchmark progress chart.
 *
 * Layers (bottom → top):
 *  1. Five horizontal tier bands (subtle ink3 × 0.06 alternating).
 *  2. Gridlines at band boundaries (line2).
 *  3. Dashed midline (line, dashed).
 *  4. Filled area under the polyline (`accent × 0.12`).
 *  5. 2 dp polyline (accent).
 *  6. 6 dp highlighted last-point marker with ink-bg outline.
 *
 * Left gutter: mono-caps labels "STIFF" / "AVERAGE" / "VERY FLEXIBLE"
 * rendered outside the Canvas in a Column with weighted children so
 * text baselines align with band centers.
 */
@Composable
fun BigChart(
    series: ProgressSeries,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.height(130.dp).fillMaxWidth()) {
        // Left gutter — tier labels at Stiff / Average / Very flexible.
        Column(
            modifier = Modifier.fillMaxHeight().width(56.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            MonoCaps(
                text = FlexibilityTier.VERY_FLEXIBLE.displayName,
                size = MonoCapsSize.Small,
                color = Theme.colors.ink3,
            )
            MonoCaps(
                text = FlexibilityTier.AVERAGE.displayName,
                size = MonoCapsSize.Small,
                color = Theme.colors.ink3,
            )
            MonoCaps(
                text = FlexibilityTier.STIFF.displayName,
                size = MonoCapsSize.Small,
                color = Theme.colors.ink3,
            )
        }

        // Canvas — bands + grid + polyline.
        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val bandCount = 5
                val bandHeight = size.height / bandCount

                // 1. Alternating band fills (subtle).
                for (i in 0 until bandCount) {
                    drawRect(
                        color = if (i % 2 == 0) {
                            Color.Transparent
                        } else {
                            Theme.colors.ink3.copy(alpha = 0.05f)
                        },
                        topLeft = Offset(0f, i * bandHeight),
                        size = androidx.compose.ui.geometry.Size(size.width, bandHeight),
                    )
                }

                // 2. Gridlines at band boundaries.
                for (i in 1 until bandCount) {
                    drawLine(
                        color = Theme.colors.line2,
                        start = Offset(0f, i * bandHeight),
                        end = Offset(size.width, i * bandHeight),
                        strokeWidth = 1f,
                    )
                }

                // 3. Dashed midline (tier boundary between avg/below).
                drawLine(
                    color = Theme.colors.line,
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f),
                )

                if (series.points.isEmpty()) return@Canvas

                // 4–5. Polyline + fill.
                val offsets = series.points.map { p ->
                    Offset(
                        x = p.xRatio * size.width,
                        y = size.height - (p.yRatio * size.height),
                    )
                }
                val fillPath = Path().apply {
                    moveTo(offsets.first().x, size.height)
                    offsets.forEach { lineTo(it.x, it.y) }
                    lineTo(offsets.last().x, size.height)
                    close()
                }
                drawPath(
                    path = fillPath,
                    color = Theme.colors.accent.copy(alpha = 0.12f),
                )
                val linePath = Path().apply {
                    moveTo(offsets.first().x, offsets.first().y)
                    offsets.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(
                    path = linePath,
                    color = Theme.colors.accent,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                )

                // 6. Last-point highlight.
                val last = offsets.last()
                drawCircle(
                    color = Theme.colors.bg,
                    radius = 6.dp.toPx(),
                    center = last,
                )
                drawCircle(
                    color = Theme.colors.accent,
                    radius = 4.dp.toPx(),
                    center = last,
                )
            }
        }
    }
}
```

- [ ] **Step 2: Verify compile**

Build → Make Project. Expected: clean.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/ui/components/BigChart.kt
git commit -m "feat(ui): add BigChart benchmark progress chart primitive

130 dp canvas with five tier bands, dashed midline, filled area under
polyline, highlighted last-point marker, and mono-caps left-gutter
labels aligned to band centers."
git push
```

---

## Task 12: Add `WeekStrip` + helper tests

Seven aspect-1 squares, Monday-start. Filled accent if completed, 2 dp dashed outline if today-not-completed, 1 dp outline otherwise. Logic-free portion (date → day-of-week indexing) is a small pure-Kotlin helper that we TDD.

**Files:**
- Create: `app/src/main/java/com/stretchdaily/app/ui/components/WeekStrip.kt`
- Test: `app/src/test/java/com/stretchdaily/app/ui/components/WeekStripHelpersTest.kt`

- [ ] **Step 1: Write the failing helper test**

```kotlin
package com.stretchdaily.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class WeekStripHelpersTest {

    @Test
    fun `weekOf returns Monday-start dates for a Wednesday`() {
        // Wednesday 2026-04-22
        val days = weekOf(LocalDate.of(2026, 4, 22))
        val expected = listOf(
            LocalDate.of(2026, 4, 20), // Monday
            LocalDate.of(2026, 4, 21),
            LocalDate.of(2026, 4, 22),
            LocalDate.of(2026, 4, 23),
            LocalDate.of(2026, 4, 24),
            LocalDate.of(2026, 4, 25),
            LocalDate.of(2026, 4, 26), // Sunday
        )
        assertEquals(expected, days)
    }

    @Test
    fun `weekOf on a Monday returns that Monday as the first day`() {
        val days = weekOf(LocalDate.of(2026, 4, 20))
        assertEquals(LocalDate.of(2026, 4, 20), days.first())
        assertEquals(7, days.size)
    }

    @Test
    fun `weekOf on a Sunday returns the preceding Monday as first`() {
        val days = weekOf(LocalDate.of(2026, 4, 26))
        assertEquals(LocalDate.of(2026, 4, 20), days.first())
        assertEquals(LocalDate.of(2026, 4, 26), days.last())
    }

    @Test
    fun `dayStateFor marks today as Today when not in completed set`() {
        val today = LocalDate.of(2026, 4, 22)
        val state = dayStateFor(day = today, today = today, completed = emptySet())
        assertEquals(DayState.Today, state)
    }

    @Test
    fun `dayStateFor marks today as Completed when in completed set`() {
        val today = LocalDate.of(2026, 4, 22)
        val state = dayStateFor(day = today, today = today, completed = setOf(today))
        assertEquals(DayState.Completed, state)
    }

    @Test
    fun `dayStateFor marks past day as Completed when in set and Idle otherwise`() {
        val today = LocalDate.of(2026, 4, 22)
        val past = LocalDate.of(2026, 4, 20)
        assertEquals(DayState.Completed, dayStateFor(past, today, setOf(past)))
        assertEquals(DayState.Idle, dayStateFor(past, today, emptySet()))
    }

    @Test
    fun `dayStateFor marks future day as Idle regardless of completed set`() {
        val today = LocalDate.of(2026, 4, 22)
        val future = LocalDate.of(2026, 4, 26)
        assertEquals(DayState.Idle, dayStateFor(future, today, setOf(future)))
    }
}
```

- [ ] **Step 2: Run — expected fail**

Expected: `unresolved reference: weekOf`, `DayState`, `dayStateFor`.

- [ ] **Step 3: Implement `WeekStrip.kt`**

```kotlin
package com.stretchdaily.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.stretchdaily.app.ui.theme.Theme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Visual state for a single day in [WeekStrip]. */
enum class DayState { Completed, Today, Idle }

/** Returns the 7 dates Monday → Sunday for the ISO week containing [day]. */
fun weekOf(day: LocalDate): List<LocalDate> {
    val monday = day.minusDays((day.dayOfWeek.value - 1).toLong())
    return (0..6L).map { monday.plusDays(it) }
}

/** Resolves the visual state for [day] given [today] and the completed-set. */
fun dayStateFor(
    day: LocalDate,
    today: LocalDate,
    completed: Set<LocalDate>,
): DayState = when {
    day in completed -> DayState.Completed
    day == today -> DayState.Today
    else -> DayState.Idle
}

/**
 * 7-square week strip, Monday-start. Each square:
 *  - Filled `accent` if [DayState.Completed].
 *  - 2 dp dashed `accent` outline if [DayState.Today] (and not completed).
 *  - 1 dp `line` outline if [DayState.Idle].
 *
 * Tiny weekday label above each square in mono-caps small.
 */
@Composable
fun WeekStrip(
    today: LocalDate,
    completed: Set<LocalDate>,
    modifier: Modifier = Modifier,
) {
    val days = weekOf(today)

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        days.forEach { day ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
            ) {
                MonoCaps(
                    text = day.dayOfWeek.narrowLabel(),
                    size = MonoCapsSize.Small,
                    color = Theme.colors.ink3,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .then(
                            when (dayStateFor(day, today, completed)) {
                                DayState.Completed -> Modifier.background(Theme.colors.accent)
                                DayState.Today -> Modifier.border(
                                    width = 2.dp,
                                    color = Theme.colors.accent,
                                    shape = RoundedCornerShape(16.dp),
                                ) // Dashed effect rendered via drawBehind below if needed
                                DayState.Idle -> Modifier.border(
                                    width = 1.dp,
                                    color = Theme.colors.line,
                                    shape = RoundedCornerShape(16.dp),
                                )
                            }
                        ),
                )
            }
        }
    }
}

/** Single-char narrow weekday (M T W T F S S). */
private fun DayOfWeek.narrowLabel(): String =
    this.getDisplayName(TextStyle.NARROW, Locale.getDefault())
```

> **Note on dashed border:** Compose `Modifier.border(...)` does not support dashed strokes. If the Today-state dashed look matters visually at R2, swap the `Modifier.border(...)` call for a `Modifier.drawBehind { drawRoundRect(stroke = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f), 0f)), ...) }` — defer the swap until you see the gallery and decide.

- [ ] **Step 4: Run the helper test — expected pass**

Expected: all 7 cases green.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/ui/components/WeekStrip.kt \
        app/src/test/java/com/stretchdaily/app/ui/components/WeekStripHelpersTest.kt
git commit -m "feat(ui): add WeekStrip primitive + Monday-start helpers

weekOf() returns 7 dates for the ISO week, dayStateFor() buckets to
Completed/Today/Idle, and WeekStrip composable renders the 7 squares
with narrow weekday labels. Unit tests cover week-boundary and state
bucketing logic."
git push
```

---

## Task 13: Add `SegmentProgress` + helper tests

N equal-width segments in three states — `Past`, `Current`, `Future`. Used by session player (N = plan size) and carousel (N = 10).

**Files:**
- Create: `app/src/main/java/com/stretchdaily/app/ui/components/SegmentProgress.kt`
- Test: `app/src/test/java/com/stretchdaily/app/ui/components/SegmentProgressHelpersTest.kt`

- [ ] **Step 1: Write the failing helper test**

```kotlin
package com.stretchdaily.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class SegmentProgressHelpersTest {

    @Test
    fun `index before current is Past`() {
        assertEquals(SegmentState.Past, segmentStateFor(index = 0, currentIndex = 3, total = 5))
        assertEquals(SegmentState.Past, segmentStateFor(index = 2, currentIndex = 3, total = 5))
    }

    @Test
    fun `index equal to current is Current`() {
        assertEquals(SegmentState.Current, segmentStateFor(index = 3, currentIndex = 3, total = 5))
    }

    @Test
    fun `index after current is Future`() {
        assertEquals(SegmentState.Future, segmentStateFor(index = 4, currentIndex = 3, total = 5))
    }

    @Test
    fun `currentIndex equal to total means everything is Past`() {
        // Edge case — session is "past the last move", i.e. complete.
        (0 until 5).forEach { i ->
            assertEquals(SegmentState.Past, segmentStateFor(index = i, currentIndex = 5, total = 5))
        }
    }

    @Test
    fun `negative currentIndex treats everything as Future`() {
        // Edge case — nothing selected yet.
        (0 until 5).forEach { i ->
            assertEquals(SegmentState.Future, segmentStateFor(index = i, currentIndex = -1, total = 5))
        }
    }
}
```

- [ ] **Step 2: Run — expected fail**

- [ ] **Step 3: Implement `SegmentProgress.kt`**

```kotlin
package com.stretchdaily.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.stretchdaily.app.ui.theme.Theme

/** State for a single [SegmentProgress] cell. */
enum class SegmentState { Past, Current, Future }

/**
 * Maps an index to its visual state given the current index.
 *
 * - `index < currentIndex` → [SegmentState.Past]
 * - `index == currentIndex` → [SegmentState.Current]
 * - `index > currentIndex` → [SegmentState.Future]
 *
 * When `currentIndex >= total`, everything is Past (session complete).
 * When `currentIndex < 0`, everything is Future (nothing started).
 */
fun segmentStateFor(index: Int, currentIndex: Int, total: Int): SegmentState {
    if (currentIndex < 0) return SegmentState.Future
    if (currentIndex >= total) return SegmentState.Past
    return when {
        index < currentIndex -> SegmentState.Past
        index == currentIndex -> SegmentState.Current
        else -> SegmentState.Future
    }
}

/**
 * N-segment progress rail. Each cell is 3 dp tall, radius 1.5 dp,
 * separated by 3 dp gaps.
 *
 * Colors:
 *  - Past    → `accent × 0.55`
 *  - Current → `accent`
 *  - Future  → `line`
 */
@Composable
fun SegmentProgress(
    total: Int,
    currentIndex: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = modifier.fillMaxWidth().height(3.dp),
    ) {
        for (i in 0 until total) {
            val color = when (segmentStateFor(i, currentIndex, total)) {
                SegmentState.Past -> Theme.colors.accent.copy(alpha = 0.55f)
                SegmentState.Current -> Theme.colors.accent
                SegmentState.Future -> Theme.colors.line
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(color),
            )
        }
    }
}
```

- [ ] **Step 4: Run the helper test — expected pass**

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/ui/components/SegmentProgress.kt \
        app/src/test/java/com/stretchdaily/app/ui/components/SegmentProgressHelpersTest.kt
git commit -m "feat(ui): add SegmentProgress rail + state-bucketing helpers"
git push
```

---

## Task 14: Add `KpiCard`

Surface-backed card with mono-caps eyebrow, icon, big value, and small suffix. Used 4× on the dashboard KPI grid.

**Files:**
- Create: `app/src/main/java/com/stretchdaily/app/ui/components/KpiCard.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.stretchdaily.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.stretchdaily.app.ui.theme.Theme

/**
 * KPI card for the dashboard 2×2 grid.
 *
 * Layout:
 *   ┌─────────────────────────────┐
 *   │ [EYEBROW]       [ICON]      │
 *   │                             │
 *   │ 14 d                        │
 *   └─────────────────────────────┘
 *
 * Where [value] is `displayMd` Manrope 26 sp and [suffix] is `bodySm`
 * 12 sp in `ink3` next to it.
 */
@Composable
fun KpiCard(
    eyebrow: String,
    icon: IconName,
    value: String,
    suffix: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Theme.dims.radiusMd))
            .background(Theme.colors.surface)
            .padding(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            MonoCaps(text = eyebrow, color = Theme.colors.ink3)
            AppIcon(name = icon, contentDescription = null, tint = Theme.colors.ink3)
        }
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.padding(top = 12.dp),
        ) {
            Text(
                text = value,
                style = Theme.typo.displayMd,
                color = Theme.colors.ink,
            )
            if (suffix != null) {
                Text(
                    text = suffix,
                    style = Theme.typo.bodySm,
                    color = Theme.colors.ink3,
                    modifier = Modifier.padding(start = 6.dp, bottom = 4.dp),
                )
            }
        }
    }
}
```

- [ ] **Step 2: Verify compile**

Build → Make Project. Expected: clean.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/ui/components/KpiCard.kt
git commit -m "feat(ui): add KpiCard for dashboard 2x2 grid"
git push
```

---

## Task 15: Build `ComponentGalleryScreen`

Debug-only gallery that renders every primitive in a scrollable column, grouped by section. Wired into a hidden route.

**Files:**
- Create: `app/src/main/java/com/stretchdaily/app/ui/debug/ComponentGalleryScreen.kt`
- Modify: `app/src/main/java/com/stretchdaily/app/ui/navigation/StretchDailyNavHost.kt`
- Modify: `app/src/main/java/com/stretchdaily/app/ui/screen/placeholder/PlaceholderScreen.kt`

- [ ] **Step 1: Add the long-press hook to `PlaceholderScreen`**

Add an optional `onLongPress` lambda + wire it to `Modifier.pointerInput`.

Replace file contents:

```kotlin
package com.stretchdaily.app.ui.screen.placeholder

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.stretchdaily.app.ui.theme.Theme
import java.util.Locale

@Composable
fun PlaceholderScreen(
    tabLabel: String,
    unlocksInPhase: String,
    contentPadding: PaddingValues,
    onLongPress: (() -> Unit)? = null,
) {
    val pressMod = if (onLongPress != null) {
        Modifier.pointerInput(Unit) {
            detectTapGestures(onLongPress = { onLongPress() })
        }
    } else Modifier

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.bg)
            .padding(contentPadding)
            .padding(Theme.dims.padScreen)
            .then(pressMod),
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

- [ ] **Step 2: Create `ComponentGalleryScreen.kt`**

```kotlin
package com.stretchdaily.app.ui.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stretchdaily.app.core.model.Category
import com.stretchdaily.app.core.model.FlexibilityTier
import com.stretchdaily.app.ui.components.AppIcon
import com.stretchdaily.app.ui.components.BandPill
import com.stretchdaily.app.ui.components.CatChip
import com.stretchdaily.app.ui.components.CircleButton
import com.stretchdaily.app.ui.components.CircleButtonVariant
import com.stretchdaily.app.ui.components.ExerciseTile
import com.stretchdaily.app.ui.components.ExerciseTileSize
import com.stretchdaily.app.ui.components.IconName
import com.stretchdaily.app.ui.components.KpiCard
import com.stretchdaily.app.ui.components.MonoCaps
import com.stretchdaily.app.ui.components.MonoCapsSize
import com.stretchdaily.app.ui.components.Pill
import com.stretchdaily.app.ui.components.PillVariant
import com.stretchdaily.app.ui.components.SegmentProgress
import com.stretchdaily.app.ui.components.Sparkline
import com.stretchdaily.app.ui.components.WeekStrip
import com.stretchdaily.app.ui.theme.Theme
import java.time.LocalDate

/**
 * Debug-only gallery. Renders every R2 primitive in a scrollable Column
 * so they can be visually compared against `reference/index.html`.
 *
 * Not reachable from the main UI in release builds — wired to a hidden
 * route from the NavHost, entered via a long-press on the Settings tab
 * placeholder (R2 only; removed once Settings ships in R6).
 */
@Composable
fun ComponentGalleryScreen(
    contentPadding: PaddingValues,
    onClose: () -> Unit,
) {
    val scroll = rememberScrollState()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.bg)
            .padding(contentPadding),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(scroll)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            MonoCaps(text = "Component gallery (debug)")

            // MonoCaps samples
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                MonoCaps(text = "MonoCaps regular", size = MonoCapsSize.Regular)
                MonoCaps(text = "MonoCaps small", size = MonoCapsSize.Small)
            }

            // Pill variants
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(onClick = {}, label = "Begin session", leadingIcon = IconName.Play, variant = PillVariant.Accent)
                Pill(onClick = {}, label = "Swap exercise", leadingIcon = IconName.Swap, variant = PillVariant.Neutral)
                Pill(onClick = {}, label = "Perform at your own pace", variant = PillVariant.DashedOutline)
            }

            // CircleButton variants
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                CircleButton(onClick = {}, icon = IconName.Close, contentDescription = "Close", variant = CircleButtonVariant.Bg2)
                CircleButton(onClick = {}, icon = IconName.SkipNext, contentDescription = "Next", variant = CircleButtonVariant.Ink)
                CircleButton(onClick = {}, icon = IconName.Play, contentDescription = "Begin", size = CircleButtonSize.Large58, variant = CircleButtonVariant.Accent)
            }

            // ExerciseTile
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                ExerciseTile(category = Category.SPINE, size = ExerciseTileSize.Small56)
                ExerciseTile(category = Category.ANKLES, size = ExerciseTileSize.Small56)
            }
            ExerciseTile(category = Category.HIPS, size = ExerciseTileSize.Large4x3)

            // CatChips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CatChip(category = Category.NECK)
                CatChip(category = Category.HIPS)
                CatChip(category = Category.WRISTS)
            }

            // BandPills
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FlexibilityTier.entries.forEach { BandPill(tier = it) }
            }

            // Sparkline
            Sparkline(values = listOf(0.2, 0.35, 0.3, 0.5, 0.7, 0.9))
            Sparkline(values = emptyList()) // empty state

            // SegmentProgress
            SegmentProgress(total = 8, currentIndex = 3)
            SegmentProgress(total = 10, currentIndex = 0)

            // WeekStrip
            WeekStrip(
                today = LocalDate.now(),
                completed = setOf(LocalDate.now().minusDays(1), LocalDate.now().minusDays(3)),
                modifier = Modifier.fillMaxWidth(),
            )

            // KpiCard
            KpiCard(eyebrow = "Streak", icon = IconName.Flame, value = "14", suffix = "days")
            KpiCard(eyebrow = "Next benchmark", icon = IconName.Sparkle, value = "4", suffix = "d")

            // Icons
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconName.entries.forEach {
                    AppIcon(name = it, contentDescription = null, tint = Theme.colors.ink2)
                }
            }

            // Close link back to Settings
            Pill(onClick = onClose, label = "Close gallery", variant = PillVariant.Neutral)
        }
    }
}
```

> **BigChart is intentionally omitted** from the gallery — it needs a
> real `ProgressSeries`, which isn't trivially constructible without Room.
> Verify `BigChart` visually during R6 when the analytics screen
> integrates it.

- [ ] **Step 3: Wire the hidden route in `StretchDailyNavHost.kt`**

Add a route constant and a `composable` entry; wire the Settings tab's long-press handler to navigate there.

Insert into `Routes`:

```kotlin
const val DEBUG_GALLERY = "debug/gallery"
```

Replace the Settings placeholder `composable` with:

```kotlin
composable(Routes.SETTINGS) {
    PlaceholderScreen(
        tabLabel = "Settings",
        unlocksInPhase = "R6",
        contentPadding = padding,
        onLongPress = { navController.navigate(Routes.DEBUG_GALLERY) },
    )
}
```

Add a new top-level `composable` outside the bottom-nav set:

```kotlin
composable(Routes.DEBUG_GALLERY) {
    com.stretchdaily.app.ui.debug.ComponentGalleryScreen(
        contentPadding = padding,
        onClose = { navController.popBackStack() },
    )
}
```

- [ ] **Step 4: Verify compile + smoke test**

Android Studio → Make. Then Run → device/emulator. Long-press the Settings tab placeholder. Expected: gallery opens, every primitive is visible, scrollable, close pill returns to Settings placeholder.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/stretchdaily/app/ui/debug/ComponentGalleryScreen.kt \
        app/src/main/java/com/stretchdaily/app/ui/navigation/StretchDailyNavHost.kt \
        app/src/main/java/com/stretchdaily/app/ui/screen/placeholder/PlaceholderScreen.kt
git commit -m "feat(ui): add debug ComponentGalleryScreen

Hidden route accessible via long-press on the Settings placeholder
tab. Renders every R2 primitive for side-by-side visual comparison
against the Sage reference prototype. Entry point removed when
Settings ships in R6."
git push
```

---

## Task 16: Visual QA pass against the handoff

- [ ] **Step 1: Open the reference prototype**

In a browser: [`docs/design_handoff_stretch_daily_v3/reference/index.html`](../../design_handoff_stretch_daily_v3/reference/index.html). Verify the Sage theme is selected.

- [ ] **Step 2: Side-by-side compare each primitive**

For each entry in the gallery, compare against its handoff equivalent:

| Primitive | Reference location |
|---|---|
| MonoCaps | Any uppercase label (bottom nav, eyebrow) |
| Pill / CircleButton | Dashboard "Begin session" CTA + swap button |
| ExerciseTile | Session overview exercise rows + session player hero |
| CatChip | Session overview row chips |
| BandPill | Benchmark log rows |
| Sparkline | Benchmark log row right side |
| BigChart | Analytics cards (skipped this phase) |
| WeekStrip | Dashboard weekly strip |
| SegmentProgress | Session player top bar + carousel header |
| KpiCard | Dashboard 2×2 grid |

Log any drift in a short note to use during R3–R6 integration.

- [ ] **Step 3: Open PR**

```bash
gh pr create --base development --head redesign/r2-primitives \
  --title "Sage redesign R2: shared primitives + debug gallery" \
  --body "$(cat <<'EOF'
## Summary

- Adds 12 shared Compose primitives under `ui/components/`: MonoCaps,
  AppIcon, Pill, CircleButton, Sheet, ExerciseTile, CatChip, BandPill,
  Sparkline, BigChart, WeekStrip, SegmentProgress, KpiCard.
- Adds 11 outline icon vectors under `res/drawable/ic_*.xml`.
- Adds debug-only `ComponentGalleryScreen` reachable via long-press on
  the Settings tab placeholder.
- Unit tests for week-date and segment-state helpers.

## Out of scope

Integration into real screens — that's R3–R6. The 5 tabs still show
placeholders.

## Test plan

- [ ] All unit tests pass (R1 tests + new WeekStrip + SegmentProgress
      helpers).
- [ ] Long-press Settings opens the gallery; every primitive renders
      without crashing.
- [ ] Visual diff against `reference/index.html` is within acceptable
      drift (note any large gaps for R3–R6 integration).

Spec: docs/superpowers/specs/2026-04-23-sage-redesign-design.md
Plan: docs/superpowers/plans/2026-04-23-sage-redesign-R2-primitives-and-gallery.md
EOF
)"
```

---

## After R2 lands

Update `CLAUDE.md §2`. Next: R3 plan
([`2026-04-23-sage-redesign-R3-dashboard-and-today-session.md`](./2026-04-23-sage-redesign-R3-dashboard-and-today-session.md))
— adds `TodaySessionHolder`, new reactive flows on existing repos, and
wires the Dashboard screen using the primitives shipped this phase.
