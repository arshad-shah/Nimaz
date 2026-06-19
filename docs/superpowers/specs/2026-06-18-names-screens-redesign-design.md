# Names Screens Redesign — Design Spec

**Issue:** #162 — "the names in the app need redesign"
**Date:** 2026-06-18
**Status:** Design complete — all decisions locked, ready for implementation plan.
**Platform:** Native Android, Jetpack Compose (Kotlin), clean architecture.

## Problem

Three "names" screens are visually generic and inconsistent — each list card is just a
number badge + name column + favorite icon, differing only by accent color:

1. **99 Names of Allah** (Asma ul Husna)
2. **Names of the Prophet ﷺ** (Asma un Nabi)
3. **The Prophets** (Adam, Nuh, …)

Goal: one reusable, consistent, beautiful card design used across all three, then a cohesive
detail screen and list chrome ("full experience" scope, confirmed with user).

## Scope (confirmed)

Full experience: list cards + a shared reusable component, detail screens, and list-screen
chrome (hero/header, search, filter chips, empty states).

## Locked decisions

### Card design — Direction B "Refined Row"
A scannable list row (chosen over a calligraphic hero card and a gradient tile grid):
- Leading **gradient medallion** (52dp, rounded 16) with the number, accent-colored.
- Left **accent rail** (4dp border-start) in the screen's accent color.
- Body: **Arabic name** (Amiri, ~25–26sp, accent-tinted) + **transliteration** (bold) +
  **English meaning** (muted).
- Trailing **favorite** toggle.
- Built as ONE reusable composable; accent color is a parameter.

### Per-screen accent colors
- 99 Names of Allah → **teal** (primary)
- Names of the Prophet ﷺ → **purple** (tertiary)
- The Prophets → **gold** (secondary)  ← made distinct so all three screens are recognizable.

### Prophets "story variant"
Same card DNA, expands downward to carry the richer prophet data:
- title (e.g. "Safiyyullah · The Chosen of Allah")
- era / Quran-mentions **chips**
- **NO story-summary preview text** on the list card — the story lives on the detail screen only.

### List chrome (locked)
- **Header: slim accent top bar** (chosen over a gradient hero banner). Keep the existing
  `NimazBackTopAppBar`, but tint the title (and back icon) in the screen's accent color.
  Requires adding a `titleColor` / `colors` passthrough param to `NimazBackTopAppBar`
  (today it hardcodes defaults; `NimazTopAppBar` already accepts `colors`).
- **Search bar**: keep `NimazSearchBar` as-is, placed directly below the top bar.
- **Filter chips**: keep the existing All / ♥ Favorites `FilterChip` row, but recolor the
  selected state to the screen's accent (accent container + on-accent label) instead of the
  hardcoded `primaryContainer`. Accent passed in per screen.
- **Empty states**: upgrade the plain centered `Text` to an icon + message (a muted accent
  glyph above the existing localized string) — applies to both the "no results" and
  "no favorites yet" cases.

### Detail screens (locked)
- **Header: calligraphic on-surface** (chosen over the full-bleed gradient hero). Drop the
  gradient card. Center on the plain background: an **accent-ringed number medallion**
  (outlined circle, accent border + accent number), the **large Amiri Arabic name**
  (accent-tinted), the **transliteration** (bold, on-surface), the **English meaning/title**
  (muted), and a short **accent divider** (~60dp × 3dp) closing the header.
  - Allah-name and Prophet-name (AsmaUnNabi) details use the number medallion (`number`/`id`
    from the model). The Prophet detail header centers Arabic name + English name + title
    (no number medallion needed) above the same accent divider.
- **Section cards below stay structurally as today** (meaning / explanation / benefits /
  Quran refs / dua usage; prophet: story / lessons / Quran mentions / timeline / miracles)
  but adopt accent cohesion: section title labels and bullet/`Icon` tints use the screen
  accent; Quran-reference / mention `AssistChip`s use an accent-tinted container.
- Keep the favorite **FAB**, recolored to the accent (accent container + on-accent icon).

### Prophets list card (locked)
- **Layout B — "title only, era chip inline"** (chosen over a two-chip row): medallion +
  Arabic + English name + **title line** (`titleEnglish`, accent-tinted) + a **single era
  chip** (accent-tinted `era`). No `storySummary` preview, no Quran-mentions chip on the
  list card. Keeps prophet cards close in height to the name cards.

## Implementation outline
1. Build the **shared `NameCard` composable** (accent + content slots / params) and replace
   the three inline card functions (`AsmaUlHusnaNameCard`, `AsmaUnNabiNameCard`,
   `ProphetCard`). Card spec = Direction B above; medallion 52dp/rounded-16, 4dp accent rail.
   Prophets pass the extra title + era-chip slot.
2. Add accent plumbing: a small per-screen accent definition (teal / purple / gold from
   `Color.kt`) passed into card, top bar, chips, empty state, detail header, FAB.
3. Add `titleColor`/`colors` passthrough to `NimazBackTopAppBar`; tint per screen.
4. Recolor filter chips + empty states (shared helper for the empty state).
5. Rebuild the three **detail headers** as the calligraphic on-surface header; apply accent
   cohesion to existing section cards + FAB.
6. Verify the three list + three detail screens (and the adaptive list-detail panes that
   reuse these) build and render. Then: writing-plans → implement → verify.

## Relevant code

Shared design system:
- `presentation/components/atoms/NimazCard.kt` (FILLED/ELEVATED/OUTLINED/GRADIENT)
- `presentation/components/atoms/ArabicText.kt` (Amiri, RTL, sizes)
- `presentation/components/organisms/NimazSearchBar.kt`
- `presentation/components/organisms/TopAppBar.kt` (NimazBackTopAppBar)
- Theme: `presentation/theme/Theme.kt`, `Color.kt` (teal #14B8A6, gold #EAB308, purple #7C4DFF), `Type.kt` (Outfit / Plus Jakarta Sans / Amiri), `Shape.kt` (NimazSpacing)

Per-screen (list + detail + adaptive, viewmodel, model, dao/repo/usecases):
- `presentation/screens/asma/` — AsmaUlHusna (card `AsmaUlHusnaNameCard`)
- `presentation/screens/asmaunnabi/` — AsmaUnNabi (card `AsmaUnNabiNameCard`)
- `presentation/screens/prophets/` — Prophets (card `ProphetCard`)
- Adaptive (tablet list-detail panes): `presentation/screens/adaptive/Adaptive*Screen.kt`
- Routes: `core/navigation/Routes.kt`, wired in `core/navigation/NavGraph.kt`

Implementation should extract a single shared card composable (accent + content slots) and
replace the three inline card functions with it.

## Note on the visual companion
All visual decisions (card, list chrome, detail header, Prophets card) were made via the local
brainstorm visual companion (`.superpowers/brainstorm/…`). Mockups persist there for reference;
the server is local-only and does not survive a remote handoff. Restart it locally
(`scripts/start-server.sh --project-dir <repo> --open`) only if revisiting a visual decision.
