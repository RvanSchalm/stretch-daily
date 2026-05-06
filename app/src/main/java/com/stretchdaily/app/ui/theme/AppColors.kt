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
 * Sage theme — the production palette. Mirrors the design handoff's
 * `--bg`, `--surface`, etc. 1:1.
 *
 * `accentSoft` is the handoff's `oklch(0.88 0.045 135)` resolved to sRGB
 * once here; we don't run oklch→sRGB for this single value at runtime.
 * `line` / `line2` are the handoff rgba() values; Compose accepts alpha
 * in the Color() constructor directly.
 */
fun sageColors(): AppColors = AppColors(
    bg = Color(0xFFF2EEE4),
    bg2 = Color(0xFFE8E4D9),
    surface = Color(0xFFF9F6EC),
    surface2 = Color(0xFFEDE9DE),
    ink = Color(0xFF252823),
    ink2 = Color(0xFF54584D),
    ink3 = Color(0xFF8A8D82),
    line = Color(red = 0x25, green = 0x28, blue = 0x23, alpha = 0x1F),
    line2 = Color(red = 0x25, green = 0x28, blue = 0x23, alpha = 0x0F),
    accent = Color(0xFF5C7A4A),
    accent2 = Color(0xFF4A6741),
    accentInk = Color(0xFFF5F3EA),
    accentSoft = Color(0xFFD4E2C4),
    warn = Color(0xFFA6632A),
)
