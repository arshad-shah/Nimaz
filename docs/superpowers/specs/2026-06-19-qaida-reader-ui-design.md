# Qaida Reader — Child-Facing UI Design

**Issue:** #178 (part of #171) · **Branch:** `feat/qaida-reader-ui` · **Date:** 2026-06-19

## Goal

Build the child-facing Jetpack Compose UI for the Qaida Reader (Arabic foundational
reader for kids), on top of the already-complete data, audio, and progress layers
(`QaidaReaderViewModel`, `QaidaAudioManager`, `Qaida*UseCases`, Room entities). The UI
binds to existing state — it adds no business logic.

**Audience:** children ages ~4–9, parent-assisted. Audio + visuals carry the experience;
text is available for parents and older readers.

## Visual Language

A blend of "illuminated calm" mood with "storybook warmth," on the existing teal + gold brand:

- **Surface:** warm parchment gradient (`#FBF5E6 → #F4E9CE`), text `#3A3327`.
- **Illuminated top border:** a thin repeating teal/gold band at the top of each Qaida screen.
- **Brand:** teal `#14B8A6` / `#0D9488` (primary, "current/active"), gold/amber `#EAB308` (stars, "done", rewards).
- **Arabic:** existing `ArabicText` atom + bundled **Amiri** font; RTL throughout.
- **Tone:** calm and reverent everywhere **except** the lesson-complete celebration, which is festive.

This mood is consistent with the app's existing Onboarding "Illuminated" redesign.

## Surfaces

Four surfaces, specced together as one visual language, **built in order**:
course map → lesson reader → celebration → letter explorer.

### 1. Course Map — `QaidaHomeScreen`

A custom **Canvas-drawn winding trail** of the 17 lessons.

- **Pinned header** (does not scroll): Arabic title (`رِحْلَتِي مَعَ القاعدة` / "My Qaida Journey"),
  a progress bar (`overallFraction`), "Lesson X of 17", and total star count (`totalStars`).
- **Continue button:** resumes at `courseProgress.nextLessonId` → navigates to `QaidaReader(lessonId)`.
  Hidden if no resumable lesson.
- **The trail:** a serpentine path through lesson **medallions**.
  - Walked stretch (up to current lesson): solid gold→teal gradient stroke with small footprint dots.
  - Locked stretch (after current): faded dashed stroke.
  - Medallions: **done** = gold with star rating beneath; **current** = teal, gently pulsing, with a
    "YOU" marker; **locked** = faded with a lock. Lesson number rendered in Arabic-Indic digits.
  - Tapping an unlocked medallion → `QaidaReader(lessonId)`. Locked medallions are non-actionable
    (subtle shake / no-op).

### 2. Lesson Reader — `QaidaReaderScreen`

RTL **tile grid**. Reached via `QaidaReader(lessonId)`; on entry the screen calls
`viewModel.selectLesson(lessonId)`.

- **Top bar:** back button, lesson title (Arabic + transliteration), line-progress dots.
- **Harakat legend:** small key (fatha = teal, kasra = gold, etc.).
- **Content:** lessons render as lines (`QaidaLineContent`); each line is a heading/instruction
  (from `instructionEnglish/Arabic`) plus a row of **cell tiles**:
  - Each tile = one `QaidaCell`: big Arabic glyph + (optional) transliteration.
  - **Harakat color-coding:** the harakat mark itself is tinted per `cell.highlightGroup`.
  - **Tap a tile** → `viewModel.onCellTapped(cell)` (plays audio + marks heard). The tile that matches
    `playingCell` glows teal with a sound indicator.
  - Per-line **"Play line"** pill → `viewModel.playLine(lineId)` (plays cells back-to-back).
- **Transliteration toggle:** in-screen show/hide control, **on by default** (helpful for parents/older
  kids; hideable for a pure look-and-listen mode). Local UI state.
- **Lesson navigation:** next/previous controls wired to `nextLesson()` / `previousLesson()`
  (respect lock status).

### 3. Celebration — lesson complete

When a lesson transitions to `COMPLETED` (observed from `lessonProgress.status` / `courseProgress`),
an overlay appears over the reader:

- Stars reveal **one-by-one** (soft chime + optional haptic), `ما شاء الله` header, a line on what was
  learned, the newly-unlocked next lesson, and **Map** / **Next lesson** actions.
- **Festive** styling here only: cream glow, gold sparkles, bouncy stars. The rest of the app stays calm.

### 4. Letter Explorer — `QaidaLettersScreen`

- **Alphabet board:** RTL grid of all letters (`letters` flow). A ★ marks letters already heard.
  Tapping a letter opens its detail.
- **Letter detail = `ModalBottomSheet`** (keeps the board behind for quick browsing). Uses the full
  `QaidaLetter`:
  - Hero: big letter, name (`nameArabic` + `nameTransliteration`), `phoneticHint`, a play button
    (plays the letter's `audioKey`).
  - **4 positional forms** (isolated/initial/medial/final) — shown only when `isConnecting`.
  - **Makhraj helper:** a friendly line built from `makhrajArea` + `makhrajDetail` (e.g. "Made with the
    two lips — press your lips together gently").

## Architecture

### Navigation

Add three flat routes to `Routes.kt` (matching the existing `@Serializable` sealed-interface pattern):

```kotlin
@Serializable data object QaidaHome : Route
@Serializable data class  QaidaReader(val lessonId: Int) : Route
@Serializable data object QaidaLetters : Route
```

Register three `composable<Route.X>` blocks in `NavGraph.kt` following the `QuranReader` pattern
(`toRoute()` for args, `popBackStack()` for back). No nested graph is introduced.

### Entry point

Add a **"Qaida"** `NimazMenuItem` to `MoreMenuScreen` (title + subtitle + icon), with an
`onNavigateToQaida` callback threaded from the nav host → `Route.QaidaHome`. The Letter Explorer is
reachable from the course map (e.g. a header/overflow action) and may also get its own menu item.

### ViewModel

Reuse the existing `QaidaReaderViewModel` via `hiltViewModel()` in **each** screen. Because all its
state derives from Room-backed flows (single source of truth), separate per-screen instances observe
the same underlying data — consistent with the app's per-screen ViewModel convention. Each screen reads
only the slices it needs:

- Course map → `courseProgress`
- Reader → `lessonContent`, `lessonProgress`, `playingCell`, `audioState` (+ `selectLesson`,
  `onCellTapped`, `playLine`, `nextLesson`, `previousLesson`)
- Letters → `letters`

No ViewModel changes are required for this UI work. If a gap surfaces during build (e.g. a needed
derived flag), it is added to the ViewModel/use-case layer, not the UI.

### Component breakdown (atoms / molecules / organisms)

Components live **flat** in their existing packages with a `Qaida` prefix (so the 90% JaCoCo gate on
`...components.atoms/molecules/organisms` applies). Screens live in `presentation/screens/qaida/`.

**Atoms** (presentational, no domain logic):
- `QaidaMedallion` — circular lesson node; params: label, state (`Done`/`Current`/`Locked`), modifier.
  Static visual; pulse animation applied by the caller.
- `QaidaStarRow` — N filled stars out of max (default 3).
- `HarakatArabicText` — renders Arabic with the harakat mark tinted per `highlightGroup`; supports a
  `playing` emphasized state. Built on `ArabicText` / `AnnotatedString`.

**Molecules:**
- `QaidaCellTile` — tappable tile (HarakatArabicText + optional transliteration + playing glow); `onClick`.
- `QaidaLetterTile` — board tile (letter + heard ★); `onClick`.
- `QaidaPlayLineButton` — "Play line" pill.
- `QaidaLineProgressDots` — line-progress indicator.
- `QaidaCourseHeader` — pinned map header (Arabic title + progress bar + star count + Continue button).
- `QaidaLetterForms` — row of the 4 positional forms.
- `QaidaMakhrajHelper` — makhraj info row.

**Organisms:**
- `QaidaCoursePath` — the Canvas winding trail + medallion nodes (see below). Takes
  `List<QaidaLessonState>` + an `onLessonClick`.
- `QaidaLessonLines` — renders `QaidaLessonContent` as lines of `QaidaCellTile`s; binds tap/play-line.
- `QaidaLetterBoard` — the alphabet grid.
- `QaidaLetterDetailSheet` — bottom-sheet body (hero + forms + makhraj + play).
- `QaidaCelebrationOverlay` — festive completion overlay with one-by-one star reveal.

### Canvas winding path

`QaidaCoursePath` uses a **custom `Layout`** (vertical, scrollable):

- Each medallion node composable is measured and placed at a computed serpentine anchor: `x` alternates
  between a left and right column by index parity; `y = topPadding + index * verticalSpacing`. Total
  height = `nodeCount * verticalSpacing`.
- The connecting trail is drawn in `Modifier.drawBehind` by building a `Path` of cubic beziers through
  the node-center anchors, drawn as two strokes:
  - **Completed** (anchors up to the current lesson): solid stroke with a gold→teal gradient `Brush`,
    plus a phase-offset dashed overlay for footprint dots.
  - **Locked** (remaining anchors): faded stroke with a dashed `PathEffect`.
- Because nodes are real composables, taps, star rows, content descriptions, and the current-node pulse
  are ordinary Compose — only the connecting line is custom-drawn.

### Child-friendly & accessibility requirements

- **Touch targets:** medallions and tiles ≥ 56–64dp.
- **TalkBack:** meaningful `contentDescription`s (e.g. "Lesson 4, Fatha and Kasra, current, 2 of 3
  stars"; "Letter baa, heard"; "Tile ba, tap to hear"). Decorative art marked as such.
- **Large text:** use `sp`; tiles/medallions size to content rather than hard-clipping.
- **RTL:** reader grid and letter board lay out right-to-left; Arabic via the RTL-correct `ArabicText`.
- **Haptics:** optional, gated by the existing `hapticEnabled` CompositionLocal (tap-to-hear, star reveal).
- **Animations:** respect the existing `animationsEnabled` setting (pulse, star reveal, sparkles).

## Testing

- Every new **atom / molecule / organism** gets Robolectric Compose tests to the enforced **90%**
  coverage, following the existing convention: `@RunWith(RobolectricTestRunner::class)`,
  `createComponentComposeRule()`, `setThemedContent { … }`, in `app/src/testDebug/...`. Cover each visual
  state (done/current/locked medallion; playing/idle tile; connecting vs non-connecting letter; etc.) and
  click callbacks.
- `QaidaReaderViewModel` is already unit-tested; no new VM tests required unless the VM changes.
- Screens are not under the coverage gate; optional light binding tests may be added.

## Out of scope

- No changes to data/audio/progress layers (issues C–F).
- No new audio assets or content seeding.
- Authoring/admin tooling, multi-profile, or non-Qaida screens.

## Open items to confirm during build

- Exact Home/More wording and icon for the Qaida entry, and whether Letter Explorer also gets its own
  menu item vs. only being reachable from the course map.
- Final makhraj phrasing per `MakhrajArea` (child-friendly copy), reviewed with the maintainer.
- Per issue #178, copy/colors/animations remain iteration fodder — expect design passes during build.
