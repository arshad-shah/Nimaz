# Qur'an section redesign — design

**Date:** 2026-08-13
**Branch:** `feat/quran-redesign`
**Status:** Design agreed screen by screen against the running app; pending spec review.
**Companion record:** [`2026-08-13-quran-redesign-walk.md`](2026-08-13-quran-redesign-walk.md)
— the screen-by-screen comparison this design was decided from.
**Prototypes:** [`../prototypes/2026-08-13-quran-redesign.html`](../prototypes/2026-08-13-quran-redesign.html)
(the section) and
[`../prototypes/2026-08-13-quran-mushaf-and-player.html`](../prototypes/2026-08-13-quran-mushaf-and-player.html)
(mushaf modes and the recitation player).

## Contents

- [1. Overview](#1-overview)
- [2. Goals and non-goals](#2-goals-and-non-goals)
- [3. What supersedes what](#3-what-supersedes-what)
- [4. Navigation](#4-navigation)
- [5. Screen designs](#5-screen-designs)
- [6. Shared components and tokens](#6-shared-components-and-tokens)
- [7. Audio subsystem](#7-audio-subsystem)
- [8. Khatam](#8-khatam)
- [9. Data and content dependencies](#9-data-and-content-dependencies)
- [10. Defects](#10-defects)
- [11. Phasing](#11-phasing)
- [12. Testing](#12-testing)
- [13. Documentation obligations](#13-documentation-obligations)
- [14. Risks and open questions](#14-risks-and-open-questions)

---

## 1. Overview

The Qur'an section already ships fifteen routes and a great deal of well-made content
surface. This is **not** a rebuild. Every screen in the section was rendered twice — the
prototype in a browser and the same screen driven on a Pixel 10 Pro emulator — and the
design below is what survived that comparison.

The comparison changed the brief. Six screens turned out to be **better than the prototype**
and are kept, tightened rather than replaced: Tafseer, Subjects-in-this-surah, Search,
Passages, Surah info's counted onward rows, and the Khatam journey trail. Two prototype
assumptions were simply wrong about the app: the Bookmarks screen is **app-wide** (Qur'an,
Hadith and Dua), and the **mushaf page view does not exist in the prototype at all** — its
Browse redesign would have silently deleted both the page grid and the reading mode behind
it.

What remains is three structural moves, one visual system, and one genuinely new capability:

1. **Structural** — the Qur'an home's in-screen tabs become real destinations; Browse folds
   surah/juz/page into one dense searchable list; saving consolidates into one app-wide
   Saved screen.
2. **Visual** — the section flattens to a denser, quieter language, with one exception: the
   mushaf adopts a warm **paper** register. Yellow stops meaning "selected".
3. **New** — the recitation player grows from a collapsed bar into a real player with seek,
   repeat, speed, follow-along and visible downloads.

## 2. Goals and non-goals

### Goals

- Raise information density across the section's lists without losing information. Browse
  goes from ~3 visible surahs to ~9; the reader from ~3.5 ayahs to ~6.
- Reduce navigation chrome. Qur'an home currently stacks four rows of it before content.
- Make one accent mean one thing. Yellow currently marks selection in four unrelated places.
- Give the mushaf a coherent identity of its own rather than borrowing the app's chrome.
- Make recitation usable for memorisation (repeat, speed, follow-along).
- Keep every existing feature. Nothing in the current section is dropped except decoration.

### Non-goals

- No change to Hadith, Dua, Names, Qaida or Tasbih, beyond Saved and Search becoming
  app-wide surfaces that already had app-wide data.
- No change to prayer times, widgets, notifications or background work.
- No AI/Ask-with-Proof changes. The consent card on Search stays exactly as it is.
- No content authoring. Content faults are filed against `arshad-shah/nimaz-data`
  (§9), not fixed here.
- No new translations or reciters.

## 3. What supersedes what

[`2026-06-22-tafseer-reskin-and-quran-player-design.md`](2026-06-22-tafseer-reskin-and-quran-player-design.md)
declared the player a presentation-only reskin and explicitly ruled out seek/scrub, route
changes and ViewModel edits. **This design supersedes it** for the player and the reader:
seek is added, the player gains repeat/speed/follow-along, and navigation changes. Its
Tafseer decisions still stand and are carried forward unchanged (§5.9).

## 4. Navigation

### 4.1 Principle

Two things were conflated today and are separated here:

- **Finding** — surah, juz, page and search are all ways of *locating* a place. They belong
  to one Browse surface.
- **Reading** — translation, mushaf and 16-line are *modes* of reading the same place. They
  belong to the reader.

The current app makes page a browse tab that happens to open a different reading mode. That
is why folding Browse naively would have deleted the mushaf.

### 4.2 Route changes

| Route | Change |
|-------|--------|
| `Quran` | Keeps its route; loses its three in-screen tabs |
| `QuranBrowse` | **New.** The merged surah/juz/page list, previously a tab |
| `QuranSaved` | **New.** App-wide saved items, absorbing `QuranBookmarks` |
| `QuranBookmarks` | **Retired**, replaced by `QuranSaved` |
| `QuranPage` | **Kept as a route**, now opening the reader in mushaf mode |
| `QuranJuz` | **Kept as a route**, now opening the reader anchored to that juz |
| `SurahInfo` | **Retired as a screen**; becomes a bottom sheet |
| everything else | Unchanged |

`QuranPage` and `QuranJuz` are deliberately **kept even though they stop being browse
destinations**, because `docs/NAVIGATION.md` §4 documents `quran/page/{n}` and
`quran/juz/{n}` as announcement route keys. They become thin entry points that open
`QuranReader` in the right mode at the right anchor.

Two retirements break documented announcement routes in §4 and must be resolved before the
routes are deleted:

- `quran/surah/{n}/info` → `SurahInfo`. Must instead open the reader (or Browse) with the
  sheet raised.
- `quran/bookmarks` → `QuranBookmarks`. Repoints to `QuranSaved`.

Every new destination is wired with `taggedComposable<Route.X>(ScreenTags.X)` and a
`ScreenTags` entry, per non-negotiable rule 6.

### 4.3 Qur'an home's four rows

Browse · Saved · Themes · Khatam, each pushing a real destination. `Themes` maps to the
existing `QuranTopics`; `Khatam` to the existing `KhatamList`.

## 5. Screen designs

### 5.1 Qur'an home (`Quran`)

- App bar: title, search, settings. The bookmark action is **removed** — Saved is a row.
- Continue-reading hero: one tap target for the whole card (today a card *and* a Resume
  button do the same thing). Shows surah, ayah, juz, page and progress.
- One card of four rows → four destinations, each with a count or status badge.
- **Recommended strip kept** — it is the only surfaced entry into Al-Kahf / Al-Mulk / Yasin
  by occasion, and the only thing on the screen that works for a user with nothing saved.
- **Recently saved strip added** beneath it.
- Verse of the day kept.

Home gets longer as a result. That is accepted deliberately.

### 5.2 Browse (`QuranBrowse`)

- One screen, no tabs.
- One search field understanding names, numbers, `juz 15` and `page 299`, with a jump-to
  card for exact matches.
- **Juz section headers** carrying the juz's Arabic name run inline through the surah list.
- Rows compress from ~200 px to ~64 px: number chip, name, one meta line
  (place · ayahs · page). About nine surahs visible instead of three.
- **Page information stays on the row.** Folding the Page tab must not cost the reader the
  ability to see where a surah sits in the mushaf.
- The place chip stays a chip, with a deliberate Meccan/Medinan palette pair replacing the
  current purple/teal accident.

Open: whether the row keeps the page *range* or only the start page, and whether the ruku
count survives the density cut.

### 5.3 Saved (`QuranSaved`)

One **app-wide** screen. The existing bookmark store is already app-wide; scoping it to the
Qur'an would strand a user's Hadith and Dua bookmarks.

- Two chip groups: kind (bookmark / favourite / note) and content (Qur'an / Hadith / Dua).
- Rows: a coloured spine, the reference, the note or excerpt, and a **small coloured kind
  chip** — not the prototype's uppercase word.
- One reference format throughout (§6.4).

Open: what a bookmark row shows in the excerpt slot, having no note attached.

### 5.4 Reader (`QuranReader`)

- **Ayah actions move to a tap-to-open bottom sheet.** The permanent five-icon pill goes.
  The sheet carries: play from here, repeat this ayah, bookmark, favourite, tafseer,
  subjects, copy, share, and — only with an active khatam — mark read.
- **Translation renders as plain text**, not inside an outlined box.
- **The juz/page marker appears once**, in the anchor bar, not on every ayah.
- Anchor bar shows the current surah/juz/page with a "Go to…" action.
- ~200 px per ayah instead of ~440.

#### Reading modes

Three: **Translation**, **Mushaf**, **16-line (IndoPak)**. Switched from a **top-bar icon**
that shows the current mode and opens a short menu — *not* the prototype's segmented row.
The app already has "Switch to page view" in the reader overflow, so this extends an
existing pattern rather than inventing one. Place is kept across a mode switch.

#### Mushaf and 16-line modes

- A warm **paper** register: cream ground, hairline rules, a simple ruled cartouche, a small
  page medallion at the foot. The current double gold/teal border, scalloped cartouche and
  gold rosette are dropped.
- A page bar: prev/next, `Page N · juz · hizb`, and a hairline position indicator across the
  full 604 (or the active script's count).
- Page-fit: the page is sized to fit without scrolling, with a font stepper.
- A dual-page toggle in the app bar.

#### Read tracking

A **page-level read mark in the page bar**, plus **"Mark read for khatam"** in the ayah
sheet — **both shown only while a khatam is active**. The always-present per-ayah circle is
retired. Most reading is not part of a plan and should carry no tracking chrome; marking a
whole page matches how a juz-a-day plan is actually kept; the sheet keeps per-verse precision
available.

### 5.5 Surah info — now a bottom sheet

Raised from Browse and from the reader, keeping you in place rather than pushing a screen
you immediately back out of. It keeps what the current **screen** does better than the
prototype's sheet:

- The summary paragraph.
- **Counted** onward rows — "Background · 3 sections", "Passages · 1 across 7 verses",
  "Subjects · 14, most-cited first". A bare "Subjects" does not tell you whether to tap.
- Four fact tiles including "Revealed in", which the current screen lacks.
- **One primary action** — "Read surah" in the section accent. Listen becomes secondary.
  Today's yellow-beside-teal pair puts two accents in one row.

Taller than the prototype's sheet, and scrollable.

### 5.6 Surah background (`SurahBackground`)

- The sticky section chips **stay as a jump index** — the longest background is 47 KB of
  prose — but lose the check mark and the selected-yellow. They mark position; they do not
  filter.
- **One heading per section.** Today every section prints its name twice (a teal icon
  eyebrow, then a large heading), costing ~90 px per section.
- **Pull-quotes**: cited ayahs render in a gold-ruled block with their reference, tapping
  through to the reader.

### 5.7 Passages (`SurahPassages`) — an outline, not highlights

Checked on Al-Kahf: **18 passages across 110 verses**, contiguous.

- **Keep the timeline** — verse range and count in a left column, a rail with dots, the
  entry to the right. It handles 18 rows well.
- **Keep the reading marker** — a filled dot and a chip on the passage containing your
  position, driven by the existing `currentAyah` argument. **Recoloured to teal.**
- **Clamp entries to two lines**, full text on the passage currently being read.
- **Keep contiguous coverage** and retitle the screen as an outline. The prototype's
  "notable passages" framing is dropped.

The prototype's title-plus-description card **cannot be built from this content**: there is
no description field, and most entries are full sentences rather than titles (§9).

### 5.8 Subjects in this surah (`SurahSubjects`)

The shipping screen beats the prototype and is kept: the filter box, the
`14 subjects / 25 citations` summary, Arabic names inline, count badges, and the well-judged
context line ("148 more verses elsewhere in the Qur'an", with a special case for "Every
verse on this subject is in this surah").

Changes: compress to the §6 row density, and **add a chevron** — the rows carry no
affordance today.

### 5.9 Tafseer (`Tafseer`)

Richer than the prototype, which drops three things it does well. All three are kept: the
**topic chips** linking commentary into the subject index, the **note editor**, and the
**ayah pager** with position.

- The framed treatment **stays as it is** on this screen.
- Topic chips restyled to the section's chips, with casing normalised.
- The ayah card shows **Arabic and translation**, and **collapses** to the reference once you
  are deep in a long commentary.

**Noted tension:** with the mushaf moving to paper, the gold/teal frame will live *only* on
Tafseer. That is a deliberate choice, worth revisiting once both are visible in the new
language.

### 5.10 Themes (`QuranTopics`) and Topic detail (`QuranTopicDetail`)

The three-way segment the prototype proposes is **already shipped**
(`Themes · Kinds · Index`). What is missing is scale and substance:

- **Rolled-up counts on every tree node** — the total ayahs beneath it, children included.
  Requires a recursive count, computed once and cached.
- **Every topic shows an ayah list, rolled up from its children.** A branch topic lists the
  ayahs of everything beneath it, so a root is never empty and "0 verses" becomes
  unreachable.
- Nodes become cards with a rotating chevron and an indent rail (§6.2).

Open: whether the chevron-expands / label-navigates split survives, or the whole row becomes
one target.

### 5.11 Search (`QuranSearch`)

Already app-wide and already opening with the Ask-with-Proof consent card, which is
unchanged.

- **Keep the chips, add per-type counts** (`Quran 42`, `Hadith 18`, …) so you can see where
  matches are before filtering.
- **Keep the term highlighting** — the prototype has no equivalent.
- **Add Subjects as a result type.** 2,512 hand-indexed subjects are currently reachable only
  by walking the tree.

### 5.12 Khatam list and detail

- **Lead with today's portion** (`Juz 18 · Al-Mu'minun to An-Nur`) with "Read today's
  portion"; resume-where-you-stopped becomes secondary. A khatam exists to assign a daily
  portion; position is the fallback, not the headline.
- **Keep the journey trail** — the snaking 30-juz path is the most distinctive thing in the
  section — and **add a recent-days list beneath it**, which the trail cannot express.
- Fix the two defects in §10.

## 6. Shared components and tokens

### 6.1 Segmented tabs — rebuilt

The prototype's pill-in-tray control: a recessed track with the selected segment lifted as a
raised pill. **Rebuilt, not restyled**, and it replaces:

- the underlined `TabRow` on Qur'an home (which is being removed anyway),
- the `Surah/Juz/Page` sub-tabs (removed),
- `NimazPillTabs` on Khatam,
- the tab row on Saved/Bookmarks,
- the `Themes · Kinds · Index` segment,
- the repeat and speed selectors in the player sheet.

One control for all of them.

### 6.2 Tree node / accordion — rebuilt

The prototype's card-per-node with a rotating chevron and an indent rail down the children,
replacing the bare text rows on Themes.

### 6.3 Paper palette

New tokens held **separately** from the app's surfaces, with their own dark-mode values:
`paper`, `paperLine`, `paperInk`. Used only by the mushaf and 16-line modes. Per rule 7
these live in the theme, not in screens.

### 6.4 Reference formatting

The same ayah is currently named three ways — `Al-Fatiha · Verse 2` (favourites),
`Surah 1, Ayah 3` (bookmarks), `Surah 2:45 / Al-Baqara` (search). **One format, decided once,
used everywhere**, including the bookmark rows that today drop the surah name entirely.

### 6.5 Colour

**Yellow is retired as a selection colour.** It currently marks selection in four unrelated
places: the search `All` chip, the background `✓ Name` chip, the `Listen` button, and the
passages "Reading" chip. Selection is teal throughout; yellow and gold are reserved for
Qur'anic ornament — ayah medallions, the mushaf cartouche, the pull-quote rule. One accent
for interaction, one for scripture.

Meccan and Medinan get a deliberate palette pair, replacing today's purple/teal.

### 6.6 Row density

A shared list-row density of roughly 64 px — number/leading element, name, one meta line —
applied to Browse, Saved and Subjects.

## 7. Audio subsystem

The largest single piece of scope. `docs/SUBSYSTEMS.md` §1 already records that ayahs are
downloaded before playback and cached under `filesDir/quran_audio/`, so the download UI
surfaces existing behaviour rather than adding machinery. Repeat and speed are new.

### 7.1 Player bar

- Now-playing (surah, ayah N of M, page).
- Prev ayah · play/pause · next ayah · expand.
- **Seek** with elapsed and remaining. This is the item the June design explicitly excluded;
  it is now in scope.
- Reciter name and style inline, tapping to the reciter sheet.
- A **download strip** — "Downloading N of M" with progress — shown while downloading.

### 7.2 Recitation sheet

- Reciter row → reciter sheet (per-reciter downloaded audio).
- **Repeat**: off / ayah / range / surah, with a count stepper for ayah and a from–to picker
  for range.
- **Speed**: 0.75× / 1× / 1.25× / 1.5×.
- **Follow along and turn the page** toggle.
- Stop / Done.

### 7.3 Follow-along

While playing, the current ayah is highlighted **in all three reading modes**, and with
follow-along enabled the page turns to keep it visible.

### 7.4 Notes

`QuranViewModel` currently injects `QuranAudioManager` directly and exposes it as a public
field — a known clean-architecture deviation recorded in `docs/ARCHITECTURE.md` §9. This work
touches that surface substantially. Whether to fix the deviation here or carry it is an open
question (§14).

## 8. Khatam

- **Today's portion** must be derivable from the plan's pace. The pace setting already
  implies it; the computation needs to exist in the domain layer, not the screen.
- Page-level read marks (§5.4) feed khatam progress. The existing per-ayah marking path must
  keep working, since the ayah sheet still offers it.
- Stat tiles get real zero states (§10).
- Pace gets a starting grace so a new plan is not immediately "Behind pace" (§10).

## 9. Data and content dependencies

Filed against `arshad-shah/nimaz-data`; **not fixed in this branch**, but three of them
constrain screens here:

| Fault | Blocks |
|-------|--------|
| Passage entries are full sentences, no separate title/description field | §5.7 — clamping is the workaround chosen |
| Near-duplicate subjects sharing one Arabic label (`Dua`/`Supplication`, `Judgement day`/`Day of Resurrection`) | §5.8 — two duplicate pairs in a 14-row list |
| Inconsistent subject casing (`only god worthy of worship`) | §5.8, §5.9 chips |
| Thematic root titled "Doctraine"; its own description says "Doctrine" | §5.10 |
| Passage prose faults — "waken up", "hundreds of year", mid-sentence capitals | §5.7 |

## 10. Defects

### Fixed as part of this work

| Defect | Where |
|--------|-------|
| Khatam stat tiles render as empty ovals — `Day streak`, `Avg Pace`, `Juz done` draw a bare outlined ellipse where the value belongs | `KhatamDetailScreen` |
| A khatam created seconds ago is already "Behind pace" in red — no starting grace | Khatam pace calculation |
| Tafseer commentary line height ~80 px for Latin prose — an Arabic line height applied to translation text | `TafseerPageContent` |
| "1 passages across 7 verses" — unpluralised string | `SurahPassagesScreen` |
| The Recommended strip reports "Juz 1" for every surah — Al-Kahf (15) and Al-Mulk (29) both read Juz 1 | `QuranRecommendedSurahs` |

### Filed separately — own branch, not this one

**The Qur'an home screen crashes for any user with two or more Qur'an bookmarks.**
`IllegalArgumentException: Key "0" was already used`, thrown when the home bookmarks
`LazyRow` composes.

`QuranRepositoryImpl.toQuranBookmark()` hardcodes `id = 0`
(`QuranRepositoryImpl.kt:879`), so every Qur'an bookmark carries the same id, and
`QuranHomeScreen.kt:458` keys that row by `it.id`. Deterministic with two bookmarks. One
line in either place — give the domain model the entity's real id, or key by `ayahId`.

This is a **shipping crash on `dev`** and should not wait for the redesign.

## 11. Phasing

The work is too large for one plan. Five phases, each independently shippable:

| # | Phase | Contains |
|---|-------|----------|
| 1 | **Foundations** | Paper palette, colour rules (§6.5), reference formatting (§6.4), row density (§6.6), the rebuilt segmented tabs (§6.1) and tree node (§6.2). No screen changes beyond adopting them. |
| 2 | **Navigation & lists** | `QuranBrowse`, `QuranSaved`, home's four rows, surah info as a sheet, route changes and doc updates. The biggest navigation risk, done once. |
| 3 | **Reader** | Ayah sheet, plain translation, anchor bar, mode switching, mushaf and 16-line in the paper register, page-level read marks. |
| 4 | **Player** | Seek, repeat, speed, follow-along, download strip, reciter sheet. |
| 5 | **Content screens & khatam** | Themes counts and rolled-up ayahs, Topic detail, Passages, Subjects, Search, Background, Tafseer, Khatam portion and defects. |

Phase 1 must land first; 2–5 are independent of each other except that 3 depends on 2 for
the reader's entry points.

## 12. Testing

Per `CLAUDE.md`:

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
python3 scripts/check_docs.py
./gradlew :app:assembleDebugAndroidTest    # phase 2 changes routes and ScreenTags
```

Phase 2 **must** build `androidTest` — `FeatureNavigationTest` names `ScreenTags` constants
directly, and retiring `SurahInfo`/`QuranBookmarks` will break the instrumented source set
while all four other gates stay green.

Additional coverage:

- Robolectric tests for the two rebuilt components (§6.1, §6.2).
- Unit tests for the rolled-up topic counts (§5.10) and today's-portion computation (§8).
- Unit test for the pace grace period (§10).
- A regression test for the bookmark-key crash, wherever that fix lands.

## 13. Documentation obligations

Enforced by `scripts/check_docs.py` and the PR workflow:

- **`docs/NAVIGATION.md`** — §3.2 route table for `QuranBrowse`, `QuranSaved`, the retirement
  of `QuranBookmarks` and `SurahInfo`, and the changed meaning of `QuranPage`/`QuranJuz`; the
  §2 mermaid map (validate with `scripts/check_mermaid.mjs`); **§4 announcement route keys**
  `quran/surah/{n}/info` and `quran/bookmarks`, both of which lose their destination; §5 help
  deep links if any Qur'an key changes; the destination count.
- **`docs/SUBSYSTEMS.md`** — §1 audio, for repeat/speed/follow-along and the download UI.
- **`docs/ARCHITECTURE.md`** — §8 for the new/rebuilt components; §9 if the
  `QuranViewModel`/`QuranAudioManager` deviation is resolved or deliberately carried.
- **`docs/CLEAN_ARCHITECTURE_CHECKLIST.md`** — tick anything resolved in passing.

## 14. Risks and open questions

1. **`SurahInfo`'s deep link.** Retiring the route leaves `quran/surah/{n}/info` needing a
   destination. Must be resolved before the route is deleted — likely the reader with the
   sheet raised.
2. **The `QuranViewModel` / `QuranAudioManager` deviation.** Phase 4 rewrites much of this
   surface. Fixing the deviation there is natural but enlarges the phase; carrying it means
   touching it again later. Needs an `ARCHITECTURE.md` decision.
3. **Today's-portion computation** does not exist yet and is assumed derivable from pace.
   Confirm against `KhatamUiState` before phase 5.
4. **Rolled-up topic counts** over a 2,512-subject tree need a cost check; the recursive
   count should be computed once and cached, not per composition.
5. **Two ornamental registers.** Paper on the mushaf, gold/teal on Tafseer only. Revisit once
   both are visible.
6. **Browse row content** — page range vs start page, and whether ruku survives.
7. **Saved rows for bookmarks**, which have no note to show in the excerpt slot.
