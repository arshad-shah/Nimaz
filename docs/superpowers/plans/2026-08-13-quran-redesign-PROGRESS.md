# Qur'an redesign — progress and handoff

**Updated:** 2026-08-13
**Branch:** `feat/quran-redesign` (PR #524), mirrored on `claude/pr-524-completion-g5iqfb`
**Status:** All five phases executed. Ready for device validation.

> **If you are an agent picking this up:** the phases are done. What is left is
> [§3](#3-what-is-left) — a device walk, and the content faults that belong to another
> repository. Do not re-run the phase plans; they are records now, not instructions.

## Contents

- [1. Start here](#1-start-here)
- [2. What is done](#2-what-is-done)
- [3. What is left](#3-what-is-left)
- [4. The documents, and what each is for](#4-the-documents-and-what-each-is-for)
- [5. Decisions already taken (do not re-litigate)](#5-decisions-already-taken-do-not-re-litigate)
- [6. Amendments to the plans](#6-amendments-to-the-plans)
- [7. Decisions the user made](#7-decisions-the-user-made)
- [8. Traps this work has already fallen into](#8-traps-this-work-has-already-fallen-into)
- [9. Verification contract](#9-verification-contract)
- [10. Defects found, and what happened to them](#10-defects-found-and-what-happened-to-them)

---

## 1. Start here

**Validate on a device**, then merge. The section has not been walked on an emulator since the
redesign landed — every gate is green and no screen has been *looked at* in its new form.

The one environmental caveat: this work was completed in a session that could not fetch the
content artifact (`fetchNimazData` 403 — no `NIMAZ_DATA_TOKEN`), so the gates were run with
`-x fetchNimazData`. Everything passed except `DeviceStateCorpusTest`, which opens the
prepackaged database and therefore cannot run without it. It fails identically on a clean
checkout of the base branch in that environment; CI, which has the token, is the check that
matters for it.

---

## 2. What is done

### Shipped to `dev` before this branch

**The bookmark duplicate-key crash** — PR #523, merged as `798cd1bf`. The Qur'an home screen
crashed for any reader with two or more bookmarks (`Key "0" was already used`), because
`QuranBookmark.id` is always `0` — `BookmarkEntity` has no id column, its primary key is the
composite `(kind, target_id)` — and two lists keyed by it. Found while walking the section for
this redesign; raised separately so the fix was not gated on the redesign.

### On this branch

**Design work — complete.** A spec, a screen-by-screen walk record of all 13 screens, two HTML
prototypes, five phase plans. See [§4](#4-the-documents-and-what-each-is-for).

**Component consolidation** — not in any phase plan; raised on the PR by the design owner. Three
components were drawing the same inset pill row: `NimazPillTabs` (organism, eight call sites),
`NimazSegmentedControl` (atom, two call sites) and phase 1's own `NimazSegmentedTabs`. All three
are now `NimazSegmentedControl`, with what each contributed expressed as a parameter — `purpose`
(`VALUE`/`VIEW`) for the semantics `NimazPillTabs` got wrong by announcing every choice as a tab,
`width` (`FILL`/`WRAP`) for the call sites sharing a row, and the lift from `NimazSegmentedTabs`.
Closes `ARCHITECTURE.md` §9 open deviation 15.

| Phase | What landed |
|-------|-------------|
| **1 — Foundations** | `QuranSurfaceColors.paper`/`.paperLine`/`.paperInk`; the segmented control; `AyahReference` |
| **2 — Navigation & lists** | `QuranBrowse` and `QuranSaved` as destinations; home's four rows; surah info as a sheet; `SurahInfo` and `QuranBookmarks` retired with both announcement keys repointed; the ~64 px surah row |
| **3 — Reader** | `AyahActionSheet`; `ReaderAnchorBar`; the two-mode reading control; plain translation; the mushaf's paper register and its page-position hairline |
| **4 — Player** | Seek, prev/next verse, the download strip, `RecitationRepeat`, speed, follow-along, `RecitationSheet`; `SUBSYSTEMS.md` §1 corrected |
| **5 — Content & khatam** | `RollUpTopicCounts`; topic detail lists its whole subtree; today's khatam portion; search chip counts; passages clamping and its teal marker; the subjects chevron; the background index's check marks dropped |

**All five §10 defects fixed** — see [§10](#10-defects-found-and-what-happened-to-them).

---

## 3. What is left

**Device validation.** Re-walk all thirteen screens in light and dark against the prototypes.
Nothing in this redesign has been seen on a screen; it has only been compiled, tested and linted.
Two things to look at first, because they are judgement calls a test cannot make:

- the **mushaf's paper register** in both themes — it must read as paper in light and as a calm
  dark page at night, never as a glaring white card;
- the **ayah row's new density** — record ayahs-visible before and after, which is the number the
  phase-3 exit criterion asks for.

**The content faults** (spec §9) belong to **arshad-shah/nimaz-data** and are deliberately not
fixed here: the "Doctraine" root, the near-duplicate subjects sharing one Arabic label, the
inconsistent subject casing, and the passage prose faults. Do not dedupe in the app — that hides
the problem rather than fixing it.

---

## 4. The documents, and what each is for

| Document | What it is |
|----------|-----------|
| `specs/2026-08-13-quran-redesign-design.md` | **The spec. Binding authority.** Goals, the navigation model, per-screen designs, shared components, audio scope, content dependencies, defects, the five-phase split. |
| `specs/2026-08-13-quran-redesign-walk.md` | The evidence. Every screen compared against a real screenshot of the shipping app, with the decision taken and why. Read this when a spec decision seems arbitrary — the reason is here. |
| `prototypes/2026-08-13-quran-redesign.html` | Prototype 1: the whole section. Open in a browser; it is a working click-through. |
| `prototypes/2026-08-13-quran-mushaf-and-player.html` | Prototype 2: mushaf reading modes and the recitation player. |
| `plans/…-phase-1-foundations.md` | ✅ Executed. Kept as the record. |
| `plans/…-phase-2-navigation.md` | ✅ Executed. |
| `plans/…-phase-3-reader.md` | ✅ Executed. |
| `plans/…-phase-4-player.md` | ✅ Executed. |
| `plans/…-phase-5-content-and-khatam.md` | ✅ Executed. |
| `plans/…-PROGRESS.md` | This file. |

The prototypes have **rendering bugs of their own** that were fixed in-place during the walk
(spans that needed `display:block`, a surah row whose number chips all piled into the first
card). If a prototype looks wrong, check the walk record before treating it as design intent.

---

## 5. Decisions already taken (do not re-litigate)

Everything in the spec is decided. These are the ones a reader of the diff is most likely to
second-guess.

**Structural**
- Qur'an home's in-screen tabs (`Home · Browse · Favorites`) became **four destinations**:
  Browse, Saved, Themes, Khatam.
- Browse folds surah/juz/page into **one searchable list** with juz section headers — and
  **keeps page information on the row**, so a reader can still see where a surah sits.
- Saved is **app-wide**, not Qur'an-scoped. The bookmark store already spans Qur'an, Hadith and
  Dua; scoping it down would strand a user's existing hadith and dua bookmarks.
- The mushaf is a **reader mode**, not a browse destination. Prototype 1 omitted it entirely, and
  folding Browse naively would have deleted both the page grid and the reading mode behind it.
- `SurahInfo` demoted from a screen to a **bottom sheet** — keeping the summary paragraph and the
  **counted** onward rows, and gaining the "Revealed in" tile the screen never had.

**Visual**
- The section flattens; **the mushaf alone takes the paper register**, replacing the gold/teal
  double border. Tafseer keeps the illuminated frame by decision (spec §5.9).
- **Yellow is retired as a selection colour.** Selection is teal; yellow and gold are Qur'anic
  ornament only.
- Rows compress to ~64 px across Browse, Saved and Subjects.

**Six screens were better than the prototype and were kept, tightened not replaced:** Tafseer,
Subjects-in-this-surah, Search, Passages, Surah info's counted rows, and the Khatam journey trail.

**Passages is an outline, not highlights.** Checked on Al-Kahf: 18 contiguous passages covering
all 110 verses. The prototype's title-plus-description card **cannot be built from the content** —
there is no description field and most entries are full sentences, not titles. Entries clamp to
two lines instead, except the one being read.

---

## 6. Amendments to the plans

Applied during execution. Kept because each says why the plan as written was wrong.

### Phase 2

| Task | Amendment |
|------|-----------|
| 2 | Used the existing `Surah` domain model; **no** `QuranBrowseRow` was created. |
| 4 | Evolved `BookmarksScreen` into `SavedScreen` rather than inventing `SavedItem`/`SavedKind`/`SavedCorpus`. `UnifiedBookmark` gained a `kinds: Set<SavedKind>` axis — a set, not a value, because a verse can be bookmarked *and* annotated. |
| 5 | The surah-info sheet is built on the existing `NimazBottomSheet`, not a bespoke `ModalBottomSheet`. It is a **stateless** molecule with a `SurahInfoSheetHost` supplying state, so it can be tested without Hilt. |
| 6 | The compressed row keeps the start page, drops the page range and the rukūʿ count — and drops the juz badge too, which is the section header the row now sits under. |

### Phase 3

| Task | Amendment |
|------|-----------|
| 2 | **Two** reading modes, not three — confirmed by the design owner, see [§7](#7-decisions-the-user-made). |

### Phase 5

| Task | Amendment |
|------|-----------|
| 2 | Used the existing `NimazTreeRow`. Phase 1's `NimazTreeNode` was deleted for duplicating it. |
| 4d | Search's per-corpus counts are computed in the screen from `state.allResults` with `SearchFilter.accepts` — **the same predicate the list filters by**. Four count fields were deleted from `SearchStatsUiState` before this work for being both dead and wrong (they counted unfiltered lists beside a filtered total). Sharing the predicate is what makes them safe to reintroduce. |
| 5 | The stat tiles do pass `"0"`. The fix is that a zero is now drawn **muted rather than accented** — three bold accent-coloured zeroes at headline size read as three empty outlined ovals. |

---

## 7. Decisions the user made

**Reading modes: two, not three.** The spec asked for Translation / Mushaf / 16-line. The code
has **four** mushaf scripts — MADANI (604 pages), INDOPAK_16 (548), INDOPAK_15 (610), INDOPAK_13
(847) — as a persisted `SettingsQuran` preference, so "16-line" is a *script*, not a view of the
same page, and a third menu entry would mean two places writing one preference. The design owner
chose **Translation / Mushaf, with the script staying in reader settings**. Phase 3 was written
that way and is implemented that way.

---

## 8. Traps this work has already fallen into

Read these. Each cost real rework.

1. **The plans invent things that already exist.** Six occurrences (`NimazTreeNode`, `SavedItem`
   and friends, a bespoke info sheet, `QuranBrowseRow`, a second string pair for tree
   expand/collapse, and `NimazSegmentedTabs` itself). **Grep before creating.**
2. **Subagents background gradle and then stop.** Put *"run gradle in the FOREGROUND"* in every
   dispatch, and check `git status` when a report looks like a non-answer.
3. **`origin/dev`-relative diffs on this branch are misleading.** Diff against the **merge-base**.
4. **Compose forbids two `setContent` calls on one test rule.** Use `runComposeUiTest` if a test
   genuinely needs two.
5. **A new string in `values/` only fails CI.** Any new user-visible string needs all six locales
   — and an **unescaped apostrophe** fails resource compilation rather than lint, which is a
   confusing "Can not extract resource" with no file named in the default output.
6. **Verify a reviewer's severity rather than trusting it.**
7. **A field can be a lie.** `Surah.juzStart` was hardcoded to `1` for all 114 rows because the
   `surahs` table has no juz column — which is why every Recommended card read "Juz 1". It is
   deleted rather than corrected: the juz moves with the edition, so it cannot be cached on a
   model that does not know which edition is active.

---

## 9. Verification contract

Per `CLAUDE.md`, before finishing any task:

```bash
./gradlew :app:compileDebugKotlin     # runs KSP → validates Hilt + Room wiring
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug              # SLOW (~4-9 min) and CI-blocking — do not skip
python3 scripts/check_docs.py         # 23 checks, no Android toolchain needed
./gradlew :app:assembleDebugAndroidTest   # routes and ScreenTags changed
```

Diagrams, when `NAVIGATION.md` §2 changes:
```bash
npm install --no-save mermaid jsdom && node scripts/check_mermaid.mjs
```

All of the above pass on this branch. `DeviceStateCorpusTest` is the one exception and it is
environmental — see [§1](#1-start-here).

---

## 10. Defects found, and what happened to them

**Fixed on this branch:**

| Defect | Fix |
|--------|-----|
| Khatam stat tiles read as empty | A zero is drawn muted rather than accented, in `NimazStatCard`, so it reads as a number instead of an empty oval |
| A khatam created today is already "Behind pace" | `paceStatus` grants `NOT_STARTED` for `daysActive <= 1`; the enum's KDoc already described this |
| Tafseer commentary line height ~2.4× | The renderer multiplied `bodyLarge.lineHeight` — already 1.5× its font size — by 1.6 again |
| "1 passages across 7 verses" | A `plurals` resource, in all six locales |
| Recommended strip reports "Juz 1" for every surah | `Surah.juzStart` deleted; every caller resolves through `MushafPagination.juzForPage` |

**Found on device by the user (internal build 3.0.114), fixed on this branch:**

| Defect | Fix |
|--------|-----|
| The player looked nothing like the prototype | `AudioBottomBar` rebuilt against `prototypes/2026-08-13-quran-mushaf-and-player.html`: violet full-bleed download strip, now-playing left / transport right, seek rail between two clocks, reciter+speed left with repeat in the accent right |
| The player was always on screen | It draws nothing unless `isAudioActive \|\| isPreparing`; playback starts from the ayah sheet or surah info |
| Stop sat beside next/previous | Removed from the bar — it is the recitation sheet's secondary action |
| Juz headers did not stick | `stickyHeader` in `QuranBrowseScreen`, with an opaque background so rows do not scroll through the text |
| Surahs crossing a juz were filed under one | `juzBySurah: Map<Int, Int>` → `juzSpans: Map<Int, IntRange>`, derived from a new `pageSpans`; the row names its range, and a `juz 2` query now finds Al-Baqarah instead of returning nothing |
| The `next` surah was found by `number + 1` | By list position instead, so a filtered or partial list spans correctly |
| Bookmark and favourite in the ayah sheet were grey | Gold and red — the colours they carried on the pill the sheet replaced |
| "Go to…" opened the passage outline | New `ReaderGoToSheet` — Verse / Juz / Page, bounded, scrolls or re-targets |
| The surah's name was rendered twice | `ReaderAnchorBar` lost its `title`; the app bar one line above already says it |
| The ayah sheet's Note action opened Tafseer | New shared `NoteEditorSheet` + `QuranEvent.SetAyahNote`; `QuranReaderUiState.ayahNotes` carries what is already written |
| The segmented control's ripple was a square | `.clip(cellShape)` **before** `.selectable` — `Surface(shape)` clips its own drawing, not the indication of a modifier applied outside it |
| The mushaf did not look like the prototype | `QuranFrame.READER` is two nested rounded rectangles with a page-number pill on the inner keyline; new `RuledSurahHeading` replaces the teal cartouche on both paper renderers |
| Follow-along and Continuous Reading read as duplicates | "Continuous playback", reworded and moved under **Audio** beside the reciter |
| The repeat range steppers were crushed side by side | Stacked in a `Column`, each full width |
| Surah info printed the transliteration as if it were a translation | The subtitle is drawn only when it differs from the title; the Arabic name moved out of the badge chip into `ArabicText` at the head of the sheet |
| Hizb was wrong, and doubled | Derived from the 1..240 quarter counter (`Ayah.hizbOfQuarter`) and omitted entirely when that counter is absent — `Ayah.hizbNumber` holds the quarter index in the shipped data, so page 82 reported "Hizb 33" for what is hizb 9 |

**Found on device by the user (internal build 3.0.115), fixed on this branch:**

| Defect | Fix |
|--------|-----|
| The ayah sheet reprinted the verse | Actions only. The reader tapped that verse to open the sheet and it is still behind it; the header's reference says which one |
| The ayah sheet's actions were squished | New `NimazSheetActionGrid` — two columns of wide pills, icon beside label. Five to a row left ~64dp per label, which is where "Unbookmark" ellipsised |
| Saved had two tab strips | One. Kind stays on screen; corpus moves into the app-bar menu beside sort, each row carrying its count — new `NimazDropdownSectionLabel` heads the two groups |
| Saved did not look like the prototype | A 3dp kind-coloured spine, the kind named small and letter-spaced in that colour, the corpus demoted to muted meta, and the gold ornamental divider dropped |
| "Search bookmarks" on a screen called Saved | "Search saved"; "Clear all bookmarks" → "Clear everything saved"; "No Bookmarks Yet" → "Nothing saved yet". All six locales |
| A verse gave no sign it was saved | `AyahItem` draws 14dp bookmark / heart / note glyphs in the same three colours, from `ayah.isBookmarked`, `favoriteAyahIds` and `QuranReaderUiState.ayahNotes` |

**Content, for `arshad-shah/nimaz-data` — not fixable in this repo:**

- The thematic tree's first root is titled **"Doctraine"**; its own description says "Doctrine".
- Near-duplicate subjects sharing one Arabic label: `Dua الدعاء` / `Supplication الدعاء`,
  `Judgement day يوم القيامة` / `Day of Resurrection يوم القيامة`. **Fix the index, do not dedupe
  in the app** — that hides the problem.
- Inconsistent subject casing: `only god worthy of worship` beside `Allah`.
- Passage prose: "waken up", "hundreds of year", mid-sentence capitals ("and They had to run
  away").
