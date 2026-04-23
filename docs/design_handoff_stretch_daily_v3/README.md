# Handoff: Stretch Daily v3 — full app redesign

## Overview

Stretch Daily is a local-only Android app for daily mobility/stretching routines
plus monthly self-measured flexibility benchmarks. This handoff is a complete
visual + interaction redesign (v3) covering all five top-level screens plus
two full-screen modal flows.

The app is offline, single-user, data lives on-device. There is no account
system, no network, no sync.

## About the design files

The files in `reference/` are **design references created in HTML** — prototypes
showing intended look and behavior, not production code to copy directly. They
are a React + inline-Babel sandbox wrapped in a design-canvas presentation
shell; they are not meant to run in the target app.

**The task:** recreate these designs in the existing Stretch Daily codebase
using its established patterns, component boundaries, and whatever UI library
it already uses. If no framework is in place yet, choose the most appropriate
one for an offline-first Android app (React Native, native Kotlin/Compose, or
Flutter are all reasonable — confirm with the user before committing).

## Fidelity

**High-fidelity.** Colors, typography, spacing, iconography, and interaction
details are all final. Recreate pixel-close using the target codebase's
existing libraries and patterns.

The design ships three named themes (Grove, Sage, Moss) as complete CSS-var
token sets — **Sage is the default** and what every artboard in this handoff
is rendered in. Grove and Moss are preserved in the tokens file for later
theming work but are not part of this implementation scope unless the user
asks for them explicitly.

---

## Scope — seven screens/states

| # | Screen | File | Purpose |
|---|---|---|---|
| 1 | Dashboard | `screens.jsx` → `DashboardScreen` | Today's landing — hero CTA + week strip + KPIs + (optional) benchmark-overdue banner |
| 2 | Session overview | `screens.jsx` → `SessionOverviewScreen` | Pre-start list of today's 8 moves; each is tappable (cues) and swappable |
| 3 | Session player | `session.jsx` → `SessionPlayer` | Full-screen, one exercise at a time; timer for timed moves, rep counter for reps, L/R cycling for unilateral |
| 4 | Session complete | `session.jsx` → `SessionDoneScreen` | Full-bleed celebration screen in accent color |
| 5 | Benchmark log | `screens.jsx` → `BenchmarkLogScreen` | Grouped list of all 10 benchmarks; expand for bands + history; tap "Log" to open sheet |
| 6 | Analytics | `screens.jsx` → `AnalyticsScreen` | One line chart per benchmark, filterable by category |
| 7 | Settings | `screens.jsx` → `SettingsScreen` | Sound toggle, data export/import/delete, library stats |

Plus one auxiliary flow: **Benchmark Carousel** (`session.jsx` → `BenchmarkCarousel`) — walks the user through all 10 benchmarks one at a time on "benchmark day".

## Navigation

Bottom tab bar (persistent, 5 tabs): Today · Session · Log · Progress · Settings.

The session *player*, *complete* screen, and *benchmark carousel* are
full-bleed overlays that cover the tab bar (not new tabs).

---

## Design tokens (Sage theme — the canonical one)

These are authored as CSS custom properties in `reference/themes.js` on
`window.THEMES.sage.vars`. Port them to the target codebase's token system
FIRST, before touching any component.

### Colors

| Token | Value | Use |
|---|---|---|
| `--bg` | `#f2eee4` (user-tweaked to `#fbf8f0`) | App background |
| `--bg-2` | `#e8e4d9` | Secondary bg / chip bg / button bg |
| `--surface` | `#f9f6ec` | Cards |
| `--surface-2` | `#ede9de` | Nested surfaces |
| `--ink` | `#252823` | Primary text |
| `--ink-2` | `#54584d` | Secondary text |
| `--ink-3` | `#8a8d82` | Tertiary text / icons |
| `--line` | `rgba(37,40,35,0.12)` | Borders |
| `--line-2` | `rgba(37,40,35,0.06)` | Subtle dividers |
| `--accent` | `#5c7a4a` | Primary action, active state |
| `--accent-2` | `#4a6741` | Accent hover/pressed |
| `--accent-ink` | `#f5f3ea` | Text on accent |
| `--accent-soft` | `oklch(0.88 0.045 135)` | Tinted banner / chip backgrounds |
| `--warn` | `#a6632a` | Destructive / overdue |

**User override noted:** the user tweaked `--bg` to `#fbf8f0` (cream) and
turned the benchmark banner OFF by default. Ship those as the production
defaults.

### Category tints (`data.js`)

One desaturated oklch color per body-part category. Used on exercise
placeholder tiles, session progress bars, and benchmark category-strip
headers.

```
Neck       oklch(0.82 0.04 140)
Shoulders  oklch(0.82 0.04 110)
Wrists     oklch(0.82 0.04 80)
Spine      oklch(0.82 0.04 170)
Hips       oklch(0.82 0.04 50)
Knees      oklch(0.82 0.04 200)
Ankles     oklch(0.82 0.04 25)
```

All share lightness 0.82 and chroma 0.04; only hue varies. **Do not
substitute saturated category colors** — the restraint is intentional.

### Typography

| Token | Stack |
|---|---|
| `--font-display` | `Manrope` (Sage is sans-only by design) |
| `--font-body` | `Manrope` |
| `--font-mono` | `JetBrains Mono` |

All text labels ("TODAY'S SESSION", "MOVE 3 OF 8", category chips, KPI
labels, unit suffixes, band hints) use the mono at **9.5–11px**,
**letter-spacing 1–1.4px**, **uppercase**, color `--ink-3`. This is the
strongest voice mark of the design — do not lose it.

Body copy is 12.5–14px Manrope 400; headlines are 22–40px Manrope 500 with
letter-spacing −0.3 to −1px. `text-wrap: pretty` is set on long paragraphs.

### Radii

```
--radius-xs     8px    (chips, tiny tiles)
--radius-sm    12px    (buttons, small cards)
--radius-md    16px    (primary cards)
--radius-lg    22px    (hero cards, sheets)
--radius-pill  999px   (CTAs, toggles, chips)
```

### Elevation

Sage uses a **hairline** vocabulary: `1px solid var(--line)` rather than
shadows. The one exception is bottom sheets, which get a hard downward
shadow: `0 -8px 28px rgba(0,0,0,0.18)`.

### Spacing

Screens: `padding: 6px 20px 100px` (the 100px bottom accounts for the
fixed bottom nav). Card interior: 12–14px padding. Stack gaps: 8px for
lists, 10–14px between sections, 18px before a new logical group.

---

## Data model

See `reference/data.js`. The app is driven by static reference data plus
user-generated logs:

- **`EXERCISES`** — ~45 entries across 7 categories, each with:
  `{id, name, difficulty, cues (semicolon-separated), focus, category,
   unilateral (bool), timed (bool), target, secPerRep, total}`
- **`BENCHMARKS`** — 10 self-measured tests, each with:
  `{id, name, category, unit, kind ("number"|"category"), description,
   bands (5-level array of {label, hint, max, value?, inverse?}), better
   ("higher"|"lower")}`
- **Sessions logged** — `{date (ISO), minutes, exerciseCount}`
- **Benchmark history** — `{[benchmarkId]: [{date, value}...]}`

`bandForValue(bm, value)` in `data.js` contains the full band-matching
logic including the "lower is better" inverse cases. Port this verbatim —
it handles edge cases that are easy to get wrong.

"Today's session" builder (`buildTodaySession` in `data.js`) picks 1
exercise from each of the 7 categories + 1 extra from the weakest-band
category, where weakness is derived from the most recent benchmark entry
in that category.

---

## Screen-by-screen details

### 1. Dashboard

- **Top strip:** left — today's date in mono-caps (e.g. `THU, APR 23`);
  right — flame icon + streak count in accent green, mono, 11px 600.
- **Hero heading:** "Stretch Daily" — 30px Manrope 600, letter-spacing
  −0.5. Subtitle: `{n} exercises · ~{m} minutes · mixed full body` in
  14px `--ink-2`.
- **Benchmark banner** (conditional, default OFF): `--accent-soft` bg,
  16px radius, 14/16 padding. Left: 36px square tile with sparkle icon in
  accent. Middle: bolded title + small subtitle. Right: chevron. Tap →
  opens the Benchmark Carousel.
- **Today's session card:** `--surface` bg, 22px radius, 20/20 padding.
  Left: "TODAY'S SESSION" eyebrow, "{m} minutes" 22px 600, "Extra focus:
  **{category}**" 12px. Right: 58px circle in accent with play icon.
  Bottom: tiny rounded bars (height 5px) colored by category in
  session-order, one per move.
- **This week strip:** 7 squares, each with weekday narrow label above.
  Filled accent if session was completed; dashed accent outline if it's
  today (not yet completed). Aspect-ratio 1.
- **KPI grid:** 2×2 — Streak / Total time / Sessions / Next benchmark.
  Each card: `--surface` bg, 16px radius, mono eyebrow + icon top-right,
  big 26px Manrope 500 value with tiny suffix next to it.

### 2. Session overview

- Header eyebrow "TODAY'S SESSION", title `{m} min · {n} moves` with the
  move count in `--ink-3`.
- Category spread bar (one thin rect per move, colored by category).
- List of 8 `ExerciseRow`s. Each row: 56px category-tinted striped
  placeholder tile on left, title row (mono index `01` + bold name), chip
  row (category chip + target like "30s" or "15 reps · L/R"), right-side
  swap button (34×34 rounded square). Tap the row to expand form cues
  inline.
- **CTA:** full-width pill button, 18px vertical padding, accent bg,
  `Begin session` with play icon.
- **Swap sheet:** bottom sheet, title `Swap "{name}"`. Lists other
  exercises in the same category with tile, name, difficulty, target.

### 3. Session player (full-screen)

Top bar:
- 32px close (×) button on left, `--bg-2` circle.
- 8-segment progress track (one segment per move). Past = filled accent
  at 55% opacity. Current = filled accent at 100%. Future = `--line`.
- Below progress: mono caps — left "MOVE {i} OF {n}", right the category.

Body:
- 4:3 category-tinted striped placeholder where the animation goes.
  Footer of placeholder, mono 10.5px: "{category} · animation" / ".webp
  placeholder".
- 26px Manrope 500 exercise name.
- Bullet list of form cues (split on `;`, show up to 4). Each bullet uses
  an `—` dash in accent instead of a disc.

Timer area (center of the remaining vertical space):
- If unilateral: pill chip above showing "LEFT — RIGHT" with the active
  side at full opacity and the other at 0.3.
- If timed: 76px Manrope 500 `MM:SS` counter, letter-spacing −3.
- If reps: 56px count with "reps" suffix in ink-3.
- Caption below: "HOLD THE POSITION" / "READY WHEN YOU ARE" / "TAP NEXT
  WHEN COMPLETE".

Controls (bottom):
- Left: 52px circle prev (`--bg-2`, ink).
- Middle: pill CTA 18px padding — if timed, accent "Start / Pause /
  Resume"; if reps, a dashed-outline neutral "Perform at your own pace".
- Right: 52px circle next (ink bg, bg fg).

Next-button logic: unilateral moves cycle L → R → next exercise. Prev
mirrors. Reaching past the last exercise fires `onComplete`.

### 4. Session complete

Full-bleed `--accent` background with `--accent-ink` text. Centered:
- "SESSION COMPLETE" mono caps 11px, 0.7 opacity.
- Big italic serif/display "Well done." — 54px, letter-spacing −1.2.
  *(Sage uses Manrope as display, so the italic lean comes from Manrope
  italic. If the codebase ships a serif, the designer is open to swapping
  in Fraunces/Playfair italic here — ask.)*
- Body copy: "You stretched {count} areas in {min} minutes. Streak's at
  **{streak+1}** now."
- Divider row: short line — flame icon — short line.
- Pill "Back to today" button: `--accent-ink` bg, accent text.

### 5. Benchmark log

- Header: eyebrow "MONTHLY BENCHMARKS", 30px "Log & review", body
  description.
- CTA card ("Start benchmark day") with sparkle icon tile — opens
  BenchmarkCarousel.
- Grouped by category. Group header: 10×10 colored square in the
  category tint + mono caps category label.
- Each benchmark row: name + optional "OVERDUE" pill, on the next line:
  latest value (display, 17px 500) + band pill + 50×14 sparkline + "Log"
  pill button + chevron to expand. Expanded reveals description, bands
  list (with the user's current band highlighted), and a date-value
  history table.
- Log form (opened as a bottom sheet): for number benchmarks, a large
  centered numeric input with unit suffix; for "category" benchmarks, a
  vertical list of 5 levels (button per band) where the selected one
  turns accent. Footer: Cancel / Save entry buttons.

### 6. Analytics (Progress)

- Header + body description.
- Horizontally scrolling category filter pills (mono caps) including
  "All".
- One card per benchmark: 3px full-width category tint strip on top.
  Body: left eyebrow (category) + name; right: latest value (22px 500) +
  delta ("+5 ° · 6mo" in accent if improving, warn if regressing). Below
  that, a 130px-tall line chart (see `BigChart` in `components.jsx`) with
  gridlines, dashed midline, y-axis labels, filled area under the line,
  and a highlighted last-point marker.

### 7. Settings

- Header "Your app, offline."
- Grouped rows (group label in mono caps above, rounded `--surface` card
  below):
  - **Preferences:** Timer sound effects toggle.
  - **Your data:** Export data / Import data / Delete all data (last one
    is `--warn` colored).
  - **Library:** read-only rows — "Exercises · 45 across 7 categories",
    "Benchmarks · 10 tests", "Sessions logged · {n} total".
- Footer: "Stretch Daily · v3 · local only" in mono caps, centered.

### Benchmark carousel (full-screen overlay)

Header mirrors the session player's top bar: close button + segment
progress across all 10 benchmarks + "BENCHMARK {i} OF 10 · {category}"
caption. Body: benchmark name 30px, description, the same log-form UI
from the log-screen sheet. "Save entry" advances to the next benchmark;
the last one closes.

---

## Interaction & behavior notes

- **Bottom nav** `--bg-2` circles are the default chrome color for
  small secondary buttons (close, swap, pagination arrows).
- **Transitions:** bottom sheets slide up 0.25s ease, backdrop fades in
  0.2s. Everything else is CSS transitions on color/bg/transform around
  0.15–0.2s. No spring physics, no layout animation.
- **Sound toggle / data-management buttons** are visual only in the
  prototype — wire to real handlers in the target app.
- **Benchmark "overdue" pill** appears on any benchmark not yet logged
  for the current month (check first-of-month cutoff).
- **Streak** counts consecutive non-today days with a session logged,
  walking backward from yesterday. Today's incomplete status doesn't
  break the streak.
- **Timer behavior:** tick every 1000ms while `running`. Pause clears
  the interval. Reaching 0 sets `running = false` but does NOT auto-
  advance; the user taps the ink-colored next arrow. On timed +
  unilateral: timer resets when side changes L → R.
- **"Today's session" picker is deterministic** from `data.js` — same
  8 moves every render. Swaps are local to the current session only
  (not persisted). This may or may not be the desired production
  behavior — confirm with user.

## Migration plan — suggested order

1. **Tokens first.** Port `window.THEMES.sage.vars` + `CATEGORY_TINTS`
   into the app's token system (Tailwind config / theme object /
   Compose theme / whatever). Do not touch any component yet. Get user
   review on the token diff.
2. **Shared primitives.** Port in this order:
   `Icon` (outline SVG set) · `StatusBar` · `BottomNav` · `Sheet` ·
   `ExerciseTile` (the striped category-tinted placeholder) ·
   `Sparkline` · `BigChart` · `WeekStrip` · `CatChip` · `BandPill` ·
   `KpiCard`.
3. **Screens.** Dashboard → Session overview → Session player →
   Session complete → Log → Analytics → Settings → Benchmark carousel.
4. **Data layer.** Port `bandForValue`, `buildTodaySession`, and the
   streak calculator verbatim — they're small, tested, and fiddly.
5. **Mock data off.** In production, replace `MOCK_*` globals with
   reads from the real local store (SQLite / Room / Core Data /
   whatever the app uses).

## Files in this bundle

```
design_handoff_stretch_daily_v3/
├── README.md                    — this document
└── reference/
    ├── index.html               — the prototype shell
    ├── app.jsx                  — <StretchApp> root component + routing
    ├── screens.jsx              — Dashboard, Session overview, Log, Analytics, Settings
    ├── session.jsx              — SessionPlayer, SessionDoneScreen, BenchmarkCarousel
    ├── components.jsx           — shared primitives (Icon, BottomNav, Sheet, Sparkline, BigChart, etc.)
    ├── themes.js                — Grove/Sage/Moss theme token sets + applyTweaks()
    └── data.js                  — EXERCISES, BENCHMARKS, mock history generator, bandForValue
```

Open `reference/index.html` in a browser to interact with the prototype.
Toggle the in-page Tweaks panel to flip session state, history
populated/empty, and the benchmark banner — useful for verifying empty-
state and all three session states.
