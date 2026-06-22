# Reader design language & deferred refresh (Hadith / Dua / settings)

**Date:** 2026-06-22
**Status:** Reference + backlog. The language below is *implemented* for the Tafseer
reader and Quran player (PR #199). Applying it to the **Hadith** and **Dua**
readers and the **settings** screens was deliberately **deferred** to keep that PR
scoped — this doc is the recipe for doing it later.

## Why this exists

The Tafseer/player work established a consistent "reader" visual language built
entirely from existing reusable components. Rather than re-deriving it per screen,
this captures the conventions and the exact components to compose from, plus a
per-screen checklist for the readers we haven't refreshed yet.

## The design language

### Identity tokens
- **Colour:** teal `#14B8A6` (primary), gold `#EAB308` (accent/tertiary in the
  theme). Use `MaterialTheme.colorScheme.*` / `NimazColors.*` — never hardcode in
  screens (rule #7).
- **Type:** Outfit (display/headings), Plus Jakarta Sans (body/labels), **Amiri**
  for all Arabic via the `ArabicText` family.
- **Shape:** 12–16dp card radii; 100% (pill) for chips, switchers, action buttons.

### Conventions
| Concern | Use | Not |
|---|---|---|
| Source / mode / view selector | **`NimazPillTabs`** switcher | boxed `FilterChip`s |
| Position / metadata (ayah, juz, page, grade, reference, count) | **`NimazBadge`** chips — filled for the primary, `outlined = true` for secondary | plain text lines |
| Reader navigation + actions | **`NimazReaderBottomBar`** (prev/next + page indicator) hosting a **`NimazActionPill`** of `NimazPillActionButton`s | bespoke top nav bars / floating pills |
| Bottom sheets | **`NimazBottomSheet`** (`molecules/NimazSheet.kt`) | ad-hoc `ModalBottomSheet` |
| List rows / cards | **`NimazCard`**, feature list items (`SurahListItem`, hadith/dua items) | one-off `Card`s |
| Section separation (Arabic / translation / commentary) | gold **ornamental divider** (`TafseerOrnamentalDivider` style) | plain `HorizontalDivider` |
| Overlay-on-sky surfaces | **glass system** (`GlassPill`, `GlassIconButton`, `Modifier.glassSurface`, `rememberGlassBackdrop`) | flat scrims |

### Rules baked in
- **Tap targets ≥ 48dp** for all interactive reader controls.
- **No duplicate indicators**: the ayah/position lives in the **app-bar subtitle**,
  not repeated in the body.
- **Highlighted text** over a light pastel background must use a **dark foreground**
  (`#1C1C1C`) so it stays legible in dark mode — see `TafseerHighlightableText`.
- **Strings from `strings.xml`**; every icon-only control needs a
  `contentDescription`; translate new strings to **all 5 locales** (de, fr, id, ms, tr).
- **"My notes / saved" surface**: a tab on the feature's list page (see the Tafseer
  chapters "My notes" tab) backed by a `getAll…()` query resolved to a navigable
  location. Reuse this pattern instead of a separate hidden screen.

## Applied since

### Bookmarks page (`BookmarksScreen`) — done
The unified bookmarks page (Quran/Hadith/Dua, routes `QuranBookmarks` /
`HadithBookmarks` / `AllBookmarks`) was reskinned into this language:
- Boxed filter chips → **`NimazPillTabs`** switcher, each pill carrying its
  count; the separate `NimazStatsGrid` was removed (counts live in the tabs +
  the app-bar subtitle "%d saved").
- Bespoke `BookmarkCard` → a minimal **`NimazCard`** row: a filled
  **`NimazBadge`** type chip, a locator title, optional source line, a gold
  **`TafseerOrnamentalDivider`** + **`ArabicText`** (Amiri) preview when ayah
  text is present, and an italic note preview.
- Interactions: **swipe-to-delete** (`SwipeToDismissBox`) with an **Undo**
  snackbar (lossless re-insert), and inline **note editing** via a
  **`NimazBottomSheet`** reached from a `⋯` overflow (`NimazSheetActionRow`:
  Edit note · Share · Delete).
- Data plumbing added for the above: `insertBookmark(domain)` on the Quran
  (via existing `addBookmark`), Hadith and Dua repositories/use-cases (lossless
  Undo), plus a Dua `updateBookmark` use-case so Dua notes have parity with
  Quran/Hadith. `DuaBookmark.toUnified()` now carries its note.
- `UnifiedBookmark.arabicText` is still `null` for all types (an existing
  TODO), so the divider/Arabic block stays hidden until that data is wired —
  the card degrades gracefully to badge + title + note.

## Deferred work (backlog)

### Hadith reader & collection
- Grade chip + "Narrated by" → keep, but render via `NimazBadge` (grade keeps its
  semantic colour; consider a `BadgeType` for Sahih/Hasan/Da'if/Mawdu).
- Reference / book·hadith number → `NimazBadge` (outlined).
- Chain-of-narration toggle: align styling with the action-pill / switcher idiom.
- Ornamental gold divider between Arabic and translation.
- Confirm the reader uses `NimazReaderBottomBar` at 48dp (already does) and the
  action pill matches Tafseer/Dua.
- Consider a **"My bookmarks" tab** on `HadithCollectionScreen` (it currently uses a
  top-bar bookmark icon) for parity with the Tafseer notes tab.

### Dua reader & category
- Occasion / reference / repeat-count chips → `NimazBadge`.
- "Benefits" card → consistent teal-tinted surface treatment.
- Ornamental gold divider between Arabic / transliteration / translation.
- Category gradient headers: keep, but reconcile radii/spacing with the language.
- Consider a **"Favourites" tab** on `DuasCollectionScreen` for parity.

### Settings (Quran / Hadith / Dua)
- Already share a pattern (preview card + sliders + dropdown + toggle groups).
  Refresh chips/labels to the pill style and ensure the live-preview card uses the
  parchment/gold treatment where Arabic is shown.

## Components reference (all exist today)
`NimazPillTabs` (organisms) · `NimazBadge` / `NimazBadgeSize` / `BadgeType` (atoms) ·
`NimazReaderBottomBar` (molecules) · `NimazActionPill` / `NimazPillActionButton` (atoms) ·
`NimazBottomSheet` (molecules/NimazSheet.kt) · `NimazCard` (atoms) ·
`SurahListItem` (molecules) · `ArabicText` family (atoms) ·
`GlassPill` / `GlassIconButton` / `glassSurface` / `rememberGlassBackdrop` (atoms) ·
`TafseerOrnamentalDivider` (molecules).

## Reuse rule
Before building any reader UI piece, check this list first — only build new when no
reusable component fits (the highlight colour rail was the sole exception in PR #199).
