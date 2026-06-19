# Tasbih Redesign — Design Spec

**Date:** 2026-06-19
**Branch:** feat/names-screens-redesign
**Status:** Approved (visual prototype validated in brainstorming companion)

## Goal

Redesign the Tasbih feature with a clean, modern, atomic-component system mirroring
the recently-redesigned Qibla feature, now that Tasbih supports **two counting modes**
(Beads and Classic). Surface the pluggable bead-design system to users.

## Design language (inherited from Qibla)

- **Dark app theme**, driven by `MaterialTheme` (adapts to user light/dark). Mockups
  shown dark; implementation must use theme tokens, not hard-coded screen colors,
  except the bead material gradients and the brand accents below.
- **Accents:** gold `#EAB308` (in-progress / selection), green `#22C55E` (goal reached
  / completion), jade `#2C6E49` for the imame lap bead.
- **Atomic structure:** atoms → molecules → organisms → screen. Reuse `NimazPillTabs`
  for segmented toggles. Components must be independently previewable.
- Rounded pills (`percent = 50`), soft cards (`12–16.dp`), `tween(200–400)` animations,
  crossfade between modes.

## Screens

### 1. Tasbih main — Beads mode
- **Top bar:** `Beads | Classic` pill toggle (left); `🎨` bead-design, `🕑` history,
  `⋮` overflow (right). No title.
- **Count capsule (pill):** centered, e.g. `12 / 33 · lap 2`. Gold while counting,
  flips **green** with `· goal ✓` when count reaches target.
- **Bead strand (hero):** curved, realistic beads on a cord that **arches upward**
  (control point above the chord) and runs **edge to edge**, fading off both ends. A
  **wide gap in the middle** holds the single gold **active bead** that travels across
  it. Counted bunch + **jade imame** (lap marker) on the left; upcoming bunch on the
  right. Tap or flick to increment.
- **Controls row:** `↺` reset (error color), `🔊` sound toggle, `📳` vibration toggle.
- **Current-tasbih bottom sheet (peek):** grab handle + Arabic + name + translation +
  `target N` with an up-chevron. Drag/tap to expand.

### 2. Tasbih main — Classic mode
Same chrome (toggle, capsule, controls, peek sheet). Center shows the existing
**circular ring counter**: large count, progress ring that fills and turns **green**
at goal. Crossfade with Beads mode.

### 3. Current-tasbih sheet — expanded
Large Arabic, transliteration (italic), translation, a 3-up stat row
(**Target / Today / Laps**), a reference/source card, and a primary
**⇄ Change Dhikr** button that opens screen 5.

### 4. Bead-design picker — sheet (opened via 🎨)
Title "Bead Design" + a 3-column grid of labelled material swatches:
**Wood, Marble, Amethyst, Onyx, Pearl, Jade**. Selected swatch has a gold ring.
Tapping re-skins the strand live. Beads-mode only. Persisted preference.

### 5. Choose Dhikr — full screen
- Top bar: `←`, "Choose Dhikr", `＋` (new).
- Search field.
- **Scrollable category pill tabs:** `All · ★ Favorites · After Prayer · Morning ·
  Evening · Mine`. Selected = gold. Filters the list (search filters within).
- **Robust rows:** name (line 1) + Arabic (line 2), both single-line with **ellipsis**;
  fixed **target badge** pinned right (never shrinks). Optional ★ favorite. Selected row
  gold-tinted. Long names/Arabic must truncate cleanly, never break layout.
- Pinned **＋ New Tasbih** button at the bottom.

### 6. New Tasbih — form
Fields: Name, Arabic (RTL), Transliteration, Target (stepper), Category chips. Primary
**✓ Create Tasbih** action (green). This is the restyled existing AddPresetScreen.

### 7. History — screen
Stat summary card (Today / Sessions / This Week) + period pill tabs
(`Today · This Week · All Time`) + session cards (icon, name [ellipsis], `count/target ·
laps · duration`, DONE badge). Restyle of existing TasbihHistoryScreen.

## Data / state changes

- `TasbihCounterStyle` enum already exists (CLASSIC / BEADS) — keep.
- Add **selected bead design** to counter state + persist (DataStore), default `Wood`.
  Extend `BeadDesign` from one (Wooden) to the six named designs.
- Choose Dhikr needs **category filtering + search + favorites**. Favorites = new
  persisted set of preset ids (or a `isFavorite` flag). Frequent/least-effort: start
  with category tabs + search + a `★` favorites tab backed by a favorites id set.
- Capsule "goal reached" is derived from `count >= targetCount`.

## Component inventory (new)

Atoms (`components/atoms/tasbih/`): `TasbihCountCapsule`, `DhikrRow`, `BeadSwatch`,
`TasbihStatTile`.
Molecules (`components/molecules/tasbih/`): `CurrentTasbihSheet` (peek + expanded
content), `BeadDesignPickerSheet`, `TasbihControlsRow`, `DhikrCategoryTabs`.
Organisms (`components/organisms/tasbih/`): `BeadsCounterView`, `ClassicCounterView`,
`TasbihTopBar`.
Reuse: `NimazPillTabs` for the Beads/Classic toggle.

Bead rendering (`screens/tasbih/`): rewrite `TasbihBeads.kt` for the arched-cord
geometry + wide central gap + travelling active bead; extend `BeadDesign.kt` with the
six designs.

## Out of scope
Streaks/achievements, charts/heatmaps, social/share, haptic-pattern customization,
sound themes. (Captured as future ideas, not this redesign.)
