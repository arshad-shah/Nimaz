# Qur'an redesign — progress and handoff

**Updated:** 2026-08-13
**Branch:** `feat/quran-redesign`
**Status:** Phase 1 of 5 complete. Phase 2 started (1 of 7 tasks). Paused for budget.

> **If you are an agent picking this up:** read [§1](#1-start-here) and do exactly what it
> says. Everything you need is in this repo — do not re-derive the design, and do not
> re-plan. Five phase plans already exist and are already argued from a spec.

## Contents

- [1. Start here](#1-start-here)
- [2. What is done](#2-what-is-done)
- [3. What is left](#3-what-is-left)
- [4. The documents, and what each is for](#4-the-documents-and-what-each-is-for)
- [5. Decisions already taken (do not re-litigate)](#5-decisions-already-taken-do-not-re-litigate)
- [6. Amendments to the plans](#6-amendments-to-the-plans)
- [7. Blocked on the user](#7-blocked-on-the-user)
- [8. Traps this work has already fallen into](#8-traps-this-work-has-already-fallen-into)
- [9. Verification contract](#9-verification-contract)
- [10. Defects found but not fixed](#10-defects-found-but-not-fixed)

---

## 1. Start here

Execute the next unfinished phase plan with the
**`superpowers:subagent-driven-development`** skill. The next one is
**Phase 2, from Task 2** (Task 1 is committed and reviewed).

```
docs/superpowers/plans/2026-08-13-quran-redesign-phase-2-navigation.md
```

Then Phase 3, Phase 4, Phase 5, in that order, each with the same skill.

Three things to do before dispatching anything:

1. **Read the spec**, not just the plan. The spec is the binding authority and the plans
   argue from it: `docs/superpowers/specs/2026-08-13-quran-redesign-design.md`.
2. **Read [§5](#5-decisions-already-taken-do-not-re-litigate) and
   [§6](#6-amendments-to-the-plans) of this file.** Several plan instructions are
   superseded. Handing an implementer an unamended brief will make it build things that
   already exist.
3. **Do the pre-flight scan against the codebase, not only against the plan.** Phase 1
   shipped a duplicate component because its scan only compared tasks with each other. For
   every file a task says "create", grep for it first. This has already caught four
   duplicates in Phase 2 alone.

`feat/quran-redesign` branched from `dev` before the bookmark-crash fix landed, so it does
not carry that fix. Rebase or merge `dev` when convenient.

---

## 2. What is done

### Shipped to `dev`

**The bookmark duplicate-key crash** — PR #523, merged as `798cd1bf`. The Qur'an home
screen crashed for any reader with two or more bookmarks (`Key "0" was already used`),
because `QuranBookmark.id` is always `0` — `BookmarkEntity` has no id column, its primary
key is the composite `(kind, target_id)` — and two lists keyed by it. Now keyed by `ayahId`
through one shared `quranBookmarkKey()`, pinned by a test that composes a lazy list over two
bookmarks. Found while walking the section for this redesign; raised separately so the fix
was not gated on the redesign.

### On `feat/quran-redesign` (11 commits, not pushed at time of writing)

**Design work — complete.** A spec, a screen-by-screen walk record of all 13 screens, two
HTML prototypes, five phase plans. See [§4](#4-the-documents-and-what-each-is-for).

**Phase 1 — Foundations — complete.** Reviewed task-by-task plus a whole-branch review;
all four gates green; **zero screen files touched**, which was the phase's exit criterion.

| Produced | Where |
|----------|-------|
| `QuranSurfaceColors.paper` / `.paperLine` / `.paperInk` — the mushaf's paper register | `presentation/theme/QuranSurfaceColors.kt` |
| `NimazSegmentedTabs` — the house segmented control, 8 tests | `presentation/components/organisms/` |
| `AyahReference` — one ayah-reference format, 6 tests | `domain/model/` |
| `ARCHITECTURE.md` §8 entries for both components | `docs/` |

**Phase 2 — Navigation & lists — Task 1 of 7 done.** `QuranSearchQuery` (`domain/model/`,
9 tests): one search field that understands a surah name, a surah number, `juz 15` and
`page 299`, and can tell `j 15` from `Jonah`.

---

## 3. What is left

~24 tasks. Phase 1's five took about two hours of wall clock, roughly a third of it
`lintDebug` alone. Phases 3–5 are heavier than 1 and 2 because they modify real screens
rather than adding leaf files, so expect more fix rounds.

| Phase | Plan | Tasks | State |
|-------|------|-------|-------|
| 2 | `…-phase-2-navigation.md` | 7 | **Task 1 done. Resume at Task 2.** |
| 3 | `…-phase-3-reader.md` | 5 | Not started. Task 2 is blocked — see [§7](#7-blocked-on-the-user) |
| 4 | `…-phase-4-player.md` | 6 | Not started |
| 5 | `…-phase-5-content-and-khatam.md` | 8 | Not started |

Nothing of the redesign is visible in the app yet. Phase 1 deliberately changed no screen;
Phase 2 is where the section starts to look different.

---

## 4. The documents, and what each is for

| Document | What it is |
|----------|-----------|
| `specs/2026-08-13-quran-redesign-design.md` | **The spec. Binding authority.** Goals, the navigation model, per-screen designs, shared components, audio scope, content dependencies, defects, the five-phase split. |
| `specs/2026-08-13-quran-redesign-walk.md` | The evidence. Every screen compared against a real screenshot of the shipping app, with the decision taken and why. Read this when a spec decision seems arbitrary — the reason is here. |
| `prototypes/2026-08-13-quran-redesign.html` | Prototype 1: the whole section. Open in a browser; it is a working click-through. |
| `prototypes/2026-08-13-quran-mushaf-and-player.html` | Prototype 2: mushaf reading modes and the recitation player. |
| `plans/…-phase-1-foundations.md` | ✅ Executed. Kept as the record. |
| `plans/…-phase-2-navigation.md` | Next. Resume at Task 2. |
| `plans/…-phase-3-reader.md` | Then this. |
| `plans/…-phase-4-player.md` | Then this. |
| `plans/…-phase-5-content-and-khatam.md` | Then this. |
| `plans/…-PROGRESS.md` | This file. |

The prototypes have **rendering bugs of their own** that were fixed in-place during the
walk (spans that needed `display:block`, a surah row whose number chips all piled into the
first card). If a prototype looks wrong, check the walk record before treating it as design
intent.

---

## 5. Decisions already taken (do not re-litigate)

Everything in the spec is decided. These are the ones an implementer is most likely to
second-guess.

**Structural**
- Qur'an home's in-screen tabs (`Home · Browse · Favorites`) become **four destinations**:
  Browse, Saved, Themes, Khatam.
- Browse folds surah/juz/page into **one searchable list** with juz section headers — but
  **keeps page information on the row**, so a reader can still see where a surah sits.
- Saved is **app-wide**, not Qur'an-scoped. The bookmark store already spans Qur'an, Hadith
  and Dua; scoping it down would strand a user's existing hadith and dua bookmarks.
- The mushaf becomes a **first-class reader mode**, not a browse destination. Prototype 1
  omitted it entirely, and folding Browse naively would have deleted both the page grid and
  the reading mode behind it.
- `SurahInfo` demotes from a screen to a **bottom sheet** — but keeps the summary paragraph
  and the **counted** onward rows ("Subjects · 14, most-cited first"), which the prototype
  dropped and which are the reason to tap.

**Visual**
- The section flattens; **the mushaf alone takes a warm paper register** (cream ground,
  hairline rules), replacing the current gold/teal double border and rosette.
- **Yellow is retired as a selection colour.** It currently marks selection in four
  unrelated places. Selection is teal; yellow and gold are Qur'anic ornament only —
  medallions, the cartouche, the pull-quote rule.
- Rows compress to ~64 px across Browse, Saved and Subjects.

**Six screens are better than the prototype and are kept, tightened not replaced:**
Tafseer (keeps its topic chips, notes and ayah pager), Subjects-in-this-surah (keeps its
context line "148 more verses elsewhere"), Search (keeps its term highlighting), Passages
(keeps its timeline and reading marker), Surah info's counted rows, and the Khatam journey
trail.

**Passages is an outline, not highlights.** Checked on Al-Kahf: 18 contiguous passages
covering all 110 verses. The prototype's title-plus-description card **cannot be built from
the content** — there is no description field and most entries are full sentences, not
titles. Entries clamp to two lines instead.

---

## 6. Amendments to the plans

Apply these when you extract the briefs. Each exists because the plan was written before
the relevant code had been read.

### Phase 2

| Task | Amendment |
|------|-----------|
| 2 | **Use the existing `Surah` domain model; do NOT create `QuranBrowseRow`.** `Surah` (`domain/model/QuranModels.kt:3`) already has number, nameArabic, nameEnglish, nameTransliteration, revelationType, ayahCount, juzStart, startPage. `QuranBrowseUiState.rows` is `List<Surah>`. Reach `GetSurahListUseCase` (`domain/usecase/QuranUseCases.kt:28`) through the use-cases aggregate `QuranViewModel` injects. |
| 4 | **Evolve `BookmarksScreen` into Saved; do NOT invent `SavedItem`/`SavedKind`/`SavedCorpus`/`QuranSavedUiState`.** `UnifiedBookmark` (`domain/model/UnifiedBookmark.kt`) already carries a unique `id: String`, `BookmarkType` (QURAN/HADITH/DUA), title, subtitle, arabicText, note, colour and navigation data; `BookmarksUiState` already has filtering, sorting, search and per-type counts. What is missing is the **kind** axis (bookmark / favourite / note) — derivable, since `BookmarkEntity` carries `bookmarked` and `favourite` booleans and a non-null `note` means a note. Add that axis and re-skin onto `NimazSegmentedTabs`. |
| 5 | **Build the surah-info sheet on the existing `NimazSheet` / `NimazSheetHeader`** (`components/molecules/NimazSheet.kt`), not a bespoke `ModalBottomSheet` — that would be a second sheet language. |
| 6 | **The compressed row keeps the start page, drops the page range, and drops the ruku count.** The plan left both open. A start page answers "where does this sit in the mushaf"; a range is two numbers for one question. Ruku is reference data no other redesigned surface shows, and it is what forces today's second line. |

### Phase 5

| Task | Amendment |
|------|-----------|
| 2 | **Use the existing `NimazTreeRow`** (`components/molecules/NimazTreeRow.kt`) — already applied to the plan file itself. Phase 1 built a `NimazTreeNode`, and the whole-branch review found it duplicated `NimazTreeRow`, which the three subject-browser screens already use and which is richer (depth indent ruling, RTL handling, `NimazBadge` count, 48 dp `NimazIconButton` chevron). It was deleted. Do not reintroduce a second tree component; if nested children are genuinely needed, add a lazy `content` slot to `NimazTreeRow`. |

---

## 7. Blocked on the user

**Phase 3, Task 2 — how many reading modes?**

The user chose three: Translation / Mushaf / 16-line. But **16-line is not a view mode in
this codebase — it is a *script***. `MushafScript` selects Madani vs IndoPak, is a persisted
`SettingsQuran` preference, and also changes `pagination.totalPages` (604 / 548 / 610 / 847).
`ARCHITECTURE.md` §9 records it as a deliberate accepted pattern. A three-way view-mode menu
would mean two places writing the same preference.

Phase 3's plan is written for **two** modes (Translation, Mushaf) with script staying in
reader settings, and Task 2 explicitly stops for confirmation. If the user wants three, that
task gains a third menu entry dispatching `SettingsEvent.SetMushafScript`.

Do not guess. Everything else in Phase 3 can proceed while this is outstanding.

---

## 8. Traps this work has already fallen into

Read these. Each cost real rework.

1. **The plans invent things that already exist.** Five occurrences so far
   (`NimazTreeNode`, `SavedItem` and friends, a bespoke info sheet, `QuranBrowseRow`, a
   second string pair for tree expand/collapse). **Grep before creating.** The pre-flight
   scan must have a "does it already exist?" section.
2. **Subagents background gradle and then stop.** Three implementers did this, returning
   "waiting for the notification" with work uncommitted. Put *"run gradle in the FOREGROUND —
   do not background it, do not poll"* in every dispatch, and check `git status` when a
   report looks like a non-answer.
3. **`origin/dev`-relative diffs on this branch are misleading.** `dev` has advanced past the
   fork point, so `git diff origin/dev -- …/screens/` shows unrelated files. Diff against the
   **merge-base** (`git merge-base origin/dev HEAD`) when checking what this branch changed.
4. **Compose forbids two `setContent` calls on one test rule.** A Phase 1 brief did it in a
   helper and three of four tests failed. Use `runComposeUiTest` if a test genuinely needs
   two.
5. **A new string in `values/` only fails CI.** `MissingTranslation` is not suppressed in
   `app/lint.xml`, and the app ships de/fr/id/ms/tr near-complete. Any new user-visible
   string needs all five.
6. **Verify a reviewer's severity rather than trusting it.** A "Minor" translation finding
   was actually a blocking gate; a "the tiles render as empty ovals" diagnosis of mine was
   wrong — the code passes a real `"0"`, it just renders illegibly.

---

## 9. Verification contract

Per `CLAUDE.md`, before finishing any task:

```bash
./gradlew :app:compileDebugKotlin     # runs KSP → validates Hilt + Room wiring
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug              # SLOW (~4-9 min) and CI-blocking — do not skip
python3 scripts/check_docs.py         # 23 checks, no Android toolchain needed
```

**Phase 2 must also run `./gradlew :app:assembleDebugAndroidTest`.** It retires two routes
and two `ScreenTags` entries; `FeatureNavigationTest` names those constants directly, so the
instrumented source set breaks while all four gates above stay green.

**`ScreenTags.QuranSurahList` must survive Phase 2.**
`app/src/androidTest/.../behavior/QuranOpenSurahTest.kt:33` scrolls by it.

Diagrams, when `NAVIGATION.md` §2 changes:
```bash
npm install --no-save mermaid jsdom && node scripts/check_mermaid.mjs
```

---

## 10. Defects found but not fixed

Found while walking the section. Each is recorded in the spec (§9, §10) with its cause.

**Assigned to a phase:**

| Defect | Fixed in |
|--------|----------|
| Khatam stat tiles read as empty — `KhatamDetailScreen.kt:164-177` does pass `"0"`; it renders illegibly. A `NimazStatsGrid` typography fix, not missing data | Phase 5, Task 5 |
| A khatam created today is already "Behind pace" — `KhatamModels.kt:186` grants `NOT_STARTED` only when `daysActive <= 0`, but day one has `daysActive == 1` and no pace, so it falls through to `BEHIND`. The `NOT_STARTED` KDoc already describes the intended behaviour | Phase 5, Task 6 |
| Tafseer commentary line height ~80 px for Latin prose — an Arabic line height applied to translation text | Phase 5, Task 4 |
| "1 passages across 7 verses" — unpluralised string | Phase 5, Task 4a |
| Recommended strip reports "Juz 1" for every surah — Al-Kahf (15) and Al-Mulk (29) both read Juz 1 | Phase 5 |

**Content, for `arshad-shah/nimaz-data` — not fixable in this repo:**

- The thematic tree's first root is titled **"Doctraine"**; its own description says "Doctrine".
- Near-duplicate subjects sharing one Arabic label: `Dua الدعاء` / `Supplication الدعاء`,
  `Judgement day يوم القيامة` / `Day of Resurrection يوم القيامة`. Two duplicate pairs in a
  14-row list. **Fix the index, do not dedupe in the app** — that hides the problem.
- Inconsistent subject casing: `only god worthy of worship` beside `Allah`.
- Passage prose: "waken up", "hundreds of year", mid-sentence capitals ("and They had to
  run away").
