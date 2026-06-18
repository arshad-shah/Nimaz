# Names Screens Redesign — Design Spec

**Issue:** #162 — "the names in the app need redesign"
**Date:** 2026-06-18
**Status:** Design in progress (card spec locked; detail screens + list chrome still open)
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

## Open items (still to design)
1. **Detail screens** — cohesive layout for all three (Allah-name detail: meaning/explanation/
   benefits/Quran refs/dua usage; Prophet-name detail: explanation/source; Prophet detail:
   story/lessons/Quran mentions/era/lineage/years/place/miracles).
2. **List chrome** — hero/header per screen, search bar, All/Favorites filter chips, empty states.
3. Then: write implementation plan (writing-plans skill) → implement → verify.

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
Card decisions above were made via the local brainstorm visual companion
(`.superpowers/brainstorm/…`). That server is local-only and does not survive a remote handoff;
restart it locally (`scripts/start-server.sh --project-dir <repo> --open`) when continuing the
remaining visual decisions (detail screens, list chrome).
