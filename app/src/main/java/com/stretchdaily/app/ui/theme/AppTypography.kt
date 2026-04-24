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
