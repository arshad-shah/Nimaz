# Tafseer reader reskin & Quran audio player redesign — design

**Date:** 2026-06-22
**Branch:** `feat/tafseer-reskin-quran-player`
**Status:** Design approved (via companion prototype); pending spec review.

## Overview

Two focused, low-risk UI changes to the Quran reading experience, both honouring a
hard constraint: **keep every feature, behaviour, and internal contract intact.**
This is presentation-layer work only — no ViewModel, repository, DAO, navigation,
or data changes.

1. **Tafseer reader** — a visual reskin ("option B"): the 800-character inner
   pager and all interaction stay exactly as today; only the look changes.
2. **Quran audio player bar** — redesigned into a **floating mini-player**
   ("option B"): same state and controls, new presentation.

Both adopt the app's existing identity tokens (teal `#14B8A6`, gold `#EAB308`,
Amiri for Arabic, Outfit/Plus Jakarta Sans) and a shared ornamental divider.

## Out of scope (explicitly)

- No "Library" navigation restructure (idea was explored and dropped).
- No change to Quran reader list/mushaf modes, surah index, or settings screens.
- No behaviour change to Tafseer: the inner 800-char pagination, ayah pager,
  source switching, highlight create/colour/tap/delete + offset mapping, notes
  editor, notes-list sheet, and share/export all remain.
- No seek/scrub added to the audio player (progress remains a non-interactive
  reflection of reading/playback position, as today).
- No ViewModel / domain / data edits. No new routes.

## Shared visual language (reused, not new components where avoidable)

These tokens are applied in both changes and already exist in the theme:

- **Colours:** `NimazColors` teal family for primary/active, gold for accents,
  `MaterialTheme.colorScheme.*` for surfaces. No hardcoded `Color(0xFF…)` in
  screens (rule #7).
- **Chips/pills:** soft filled pills — selected = teal container/`onTeal`,
  unselected = outline. Replaces boxed `FilterChip` look in Tafseer.
- **Ornamental divider:** reuse the existing `TafseerOrnamentalDivider`
  (gold `۞ ❖ ۞`) between Arabic / translation / commentary sections.
- **Arabic:** `ArabicText` / `ArabicTextSize` atoms (Amiri).
- **Tap targets:** ≥48dp for interactive controls (consistent with the reader
  bottom-bar work already shipped in PR #198).

## 1. Tafseer reader reskin (option B)

### Affected files (presentation only)
- `presentation/screens/quran/TafseerScreen.kt` — `TopAppBar` title/subtitle.
- `presentation/components/organisms/TafseerPageContent.kt` — `TafseerNavBar`,
  the `TafseerBookFrame` content styling, and `TafseerHighlightControls`
  (floating pill) restyle. `splitTafseerIntoPages`, `highlightsForPage`, the
  `AnimatedContent` page transition, and all callbacks are **unchanged**.
- `presentation/components/molecules/TafseerBookFrame.kt` — frame surface
  (parchment tint, gold edge) restyle.

### Visual changes
- **App bar:** title becomes "Tafsir · {surahName}"; the ayah name/number shows
  as the subtitle (e.g. "Ayah 255 — Ayat al-Kursi"). Share action unchanged.
  *(Display-only; surah/ayah data already available to the screen.)*
- **Nav bar (`TafseerNavBar`):** ayah badge → teal pill; source `FilterChip`s →
  soft teal/outline pills; inner page nav restyled (teal chevrons + "Page X / Y").
  Structure and callbacks (`onSourceSwitch`, `onPreviousPage`, `onNextPage`,
  `currentContentPage`) are identical.
- **Book frame:** warm parchment surface tint with a subtle gold edge; gold
  ornamental dividers between Arabic ayah, translation, and commentary.
- **Highlight controls:** the floating pill is refined (rounded, refined colour
  swatches, notes badge) but keeps the same toggle / colour-select / notes-open
  behaviour and the same two bottom sheets.

### Behaviour preserved (acceptance)
- Switching source still reloads commentary; available-source fallback intact.
- Long commentary still paginates at `MAX_CHARS_PER_PAGE = 800` with the same
  paragraph/sentence break logic and slide animation.
- Highlight offsets still map through `highlightsForPage` correctly across pages.
- Notes add/edit/delete and share/export unchanged.

## 2. Quran audio player → floating mini-player (option B)

### Affected files (presentation only)
- `presentation/components/molecules/QuranAudioBottomBar.kt` — redesign the
  `AudioBottomBar` composable and its `@Preview`s. Same parameters:
  `isPlaying`, `isPreparing`, `downloadProgress`, `downloadedCount`,
  `totalToDownload`, `readingProgress`, `surahName`, `currentAyahInSurah`,
  `totalAyahsInSurah`, `onPlayClick`, `onStopClick`. **No signature change**, so
  `QuranReaderScreen` call sites are untouched.

### Visual design
- An **elevated, rounded floating card** (not a flat docked strip), inset from the
  screen edges, sitting above the bottom.
- **No art/avatar tile.** The **leading element is the primary play/pause** control
  itself — a teal circular button (≥44dp).
- **Meta column:** surah name (Outfit, bold) + position line
  "Ayah X / Y · Juz · Page" (muted); a slim progress track beneath the text.
- **Trailing control:** circular **stop** (`Close`). A subtle "playing"
  equalizer/indicator next to the title when `isPlaying`.
- **States:**
  - *Idle:* progress = `readingProgress`; play icon.
  - *Playing:* pause icon + equalizer; progress tracks position.
  - *Preparing/downloading:* progress = `downloadProgress`; text shows
    `R.string.audio_downloading_short_format` (downloadedCount / total).

### Decided: no reciter line
- A reciter-name line was considered and **omitted** (confirmed). The bar's
  parameter signature stays identical — no additive param, no call-site change.

### Behaviour preserved (acceptance)
- `onPlayClick` starts/pauses from the current ayah; `onStopClick` stops.
- Bar stays in sync with the playing ayah (driven by the screen's existing
  auto-scroll/position effect). Download/preparing state still surfaces.

## Accessibility & i18n

- **All user-facing strings come from `strings.xml`.** No hardcoded display text.
- **Every icon-only control has a `contentDescription`** sourced from strings
  (e.g. existing `cd_*` keys; add new ones as needed for play/pause/stop, source
  pills, page nav). This matters because the redesigned controls are icon-led.
- Tap targets ≥48dp; colour contrast meets the app's existing standard; RTL
  Arabic via the `ArabicText` atoms.

## Verification

- `./gradlew :app:compileDebugKotlin` (KSP / Hilt / Room wiring).
- `./gradlew :app:testDebugUnitTest`.
- Compose `@Preview`s updated for: Tafseer nav bar + book frame; the player bar
  in idle / playing / preparing states.
- Manual: open Tafseer (multi-page ayah), switch sources, create/edit/delete a
  highlight with a note, share — confirm unchanged behaviour. Play/stop audio,
  trigger a download — confirm player states.

## Resolved decisions

1. **Reciter line** — omitted. Player signature unchanged.
2. **Scope** — strictly Tafseer reskin + Quran player. The Hadith/Dua chip/divider
   refresh is deferred (not in this change).
