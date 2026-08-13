# Quran section redesign — decisions log

> **Status:** in progress — being filled in screen by screen against the prototype at
> `docs/superpowers/prototypes/2026-08-13-quran-redesign.html`.
> Not yet a spec. When every screen is walked, this becomes the design doc.

## How this was decided

Each screen was rendered twice: the prototype in a browser at phone width, and the same
screen from a debug build driven on a Pixel 10 Pro emulator. Decisions below were taken
against those two images side by side.

## Inventory — what exists today

Fifteen Quran-cluster routes already ship: `Quran`, `QuranReader`, `QuranPage`, `QuranJuz`,
`QuranSearch`, `QuranBookmarks`, `SurahInfo`, `SurahBackground`, `SurahPassages`,
`SurahSubjects`, `TafseerChapters`, `Tafseer`, `QuranTopics`, `QuranTopicDetail`, plus
`KhatamList` / `KhatamDetail` / `KhatamCreate` / `KhatamEdit`.

The prototype is therefore **not greenfield**. It is a visual redesign plus a small number
of structural moves.

---

## Screen 1 — Quran home (`Route.Quran`, `QuranHomeScreen`)

**Today:** pill-chip title with three app-bar actions (search, bookmark, settings); an
in-screen tab row *Home · Browse · Favorites*; a "Start Reading" hero; a Khatam card; a
"Recommended" horizontal strip; a "Subjects in the Qur'an" row; Verse of the day.

**Prototype:** plain title with two actions (search, settings); no tabs; a continue-reading
hero carrying juz / page / ayah and a progress bar; one card of four rows (Browse, Saved,
Themes, Khatam); Verse of the day; a "Recently saved" strip.

### Decided

- **Kill the tabs.** `Home · Browse · Favorites` collapse into the four-row card, and each
  row pushes a real destination with its own back arrow. Browse gets a full screen, which
  it needs for the juz/page search. The Favorites tab's content moves to a new Saved screen.
- **Keep the Recommended strip, and add Recently saved.** Recommended is the only surfaced
  entry into Al-Kahf / Al-Mulk / Yasin by occasion, and it is the one thing on the screen
  that works for a user with nothing saved yet. Home gets longer; that is accepted.

### Open

- Where the app-bar bookmark action goes once Saved is a row (drop it, or keep as a shortcut).
- Whether the hero shows progress when there is no reading position yet.

---

## Screen 2 — Browse (today: a tab inside `Quran`)

**Today:** two stacked tab rows — `Home · Browse · Favorites`, then `Surah · Juz · Page` —
so four rows of chrome precede the first surah. A "Search surahs" box that matches names
only. Rows are ~200 px: a scalloped gold medallion, the name, and a second line of five
pills (place, verses, page range, juz, ruku).

**Prototype:** one screen. One search box that also understands `juz 15` and `page 299`,
with a jump-to card. Juz section headers, carrying the juz's Arabic name, run inline
through the surah list. Rows are ~64 px: a flat number chip, the name, one meta line.

### Decided

- **Adopt the dense single-line row.** ~64 px, roughly nine surahs visible instead of three.
- **Fold Surah / Juz / Page into one searchable list — without losing any of the three.**
  Juz becomes section headers with the surahs beneath it, and **the page information stays
  on the row**, so a reader can still see where a surah sits in the mushaf. Smart search
  handles `juz 15` and `page 299`.
- **Keep the place chip as a chip; fix its palette.** Meccan and Medinan get a deliberate
  paired palette. Today's purple Makkah / teal Madinah reads as a status colour.

### Open

- Whether the row keeps the page *range* or only the start page.
- Whether the ruku count survives the density cut.

---

## Screen 3 — Saved (today: split three ways)

**Today:** saving is split across a **Favorites tab** (favourites only, cards leading with
the Arabic ayah), an **app-wide Bookmarks screen** behind the app-bar icon (with its own
search, sort, and `All · Quran · Hadith · Dua` tabs), and no surface found yet for notes.
Bookmarks name items "Surah 1, Ayah 3"; favourites name the same thing "Al-Fatiha · Verse 2".

**Prototype:** one Quran-scoped Saved screen merging all three kinds, with
`Everything / Bookmarks / Favourites / Notes` chips and dense rows.

### Decided

- **One app-wide Saved**, reached from the Quran home row *and* from wherever the current
  Bookmarks screen is reached. It carries kind chips (bookmark / favourite / note) **and**
  content chips (Quran / Hadith / Dua). It stops being a Quran-only screen — that is the
  point: the existing bookmark store is already app-wide, and scoping it down would strand
  a user's hadith and dua bookmarks.
- **Adopt the prototype's dense row**, with one change: the uppercase kind word
  (`BOOKMARK`, `FAVOURITE`, `NOTE`) becomes a **small chip**, keeping the per-kind colour.
  Row = coloured spine · reference · note or excerpt · kind chip.
- Fix the naming inconsistency: one reference format everywhere.

### Open

- What a bookmark row shows in the excerpt slot, having no note attached.

---

## Bugs found along the way (not part of this redesign)

- **Quran home → "Start New Khatam" crashes** with
  `IllegalArgumentException: Key "0" was already used` from a `LazyColumn`, every time,
  but only while no khatam exists. The same destination reached from More → Khatam Quran
  is fine. Needs its own branch.

---

## Screen 4 — Reader (`QuranReader`)

**Today:** ~440 px per ayah — about three and a half ayahs on a 6" screen. Each ayah carries
a permanent five-icon action pill (favourite, bookmark, share, play, tafseer), a number
circle, a hollow read-toggle, the Arabic with an ornamental inline ayah medallion, the
translation inside its own outlined box, and a `Juz 1 · Page 1` pill **repeated on every
ayah**. A persistent audio bar sits at the bottom.

**Prototype:** ~200 px per ayah. An anchor card at the top (`Al-Kahf · Juz 15 · page 293 ·
110 ayahs`, with "Go to…"), a gold bismillah, then per ayah only a small `18:1` chip, the
Arabic, and the translation as plain text. No action pill, no read-toggle, no audio bar.

### Decided

- **Ayah actions move to a tap-to-open bottom sheet.** The permanent five-icon pill goes.
- **Per-ayah read tracking stays.** It is what drives khatam progress; dropping it would
  break the feature. It needs a home in the new ayah row — the prototype has no answer for
  this and one must be designed.
- **Translation becomes plain text**, and the juz/page marker appears **once in the anchor
  bar** rather than on every ayah.
- **The audio bar stays.** The prototype omits it rather than arguing against it;
  recitation is a shipped feature with its own reciter picker. It is carried forward,
  restyled to the new language.

### Open

- Where the read-toggle lives in the new, lighter ayah row.
- Whether the ornamental inline ayah medallion survives, or is replaced by the leading chip.

---

## Screen 4b — Mushaf page view (`QuranPage`)

**The prototype omits this screen entirely.** Its only nod to pages is typing `page 299`
into Browse, and that jump lands in the *translation* reader. So the prototype does not
simplify the mushaf view — it deletes it, along with the Page grid that leads to it.

**Today** it is well developed and quite unlike anything else in the app: a framed page
with a gold/teal double border, ornamental head and foot dividers, the scalloped surah
cartouche, and a page-number rosette at the foot; **continuous mushaf line layout**, where
the Arabic flows as lines of the page rather than one block per ayah, with inline ayah
medallions and the ruku `ع` marker in place; a `Page · Juz · Hizb` pill bar with prev/next
arrows and the read-toggle; and the audio bar.

The Page **grid** that reaches it is also substantial: a jump-to-page field, juz group
headers with their page ranges, a fast-scroll juz rail, and page tiles labelled where a
surah begins.

### Decided

- **The mushaf becomes a first-class reader mode.** One reader, two modes — Translation and
  Mushaf — switched from the reader itself and keeping your place across the switch. It
  stops being something reachable only through a browse tab.
- **The mushaf keeps its ornament while the rest of the section flattens.** The gold frame,
  cartouche and rosette are imitating a printed Quran, which is real work; the lists and
  the translation reader take the prototype's flatter language. Two registers, used
  deliberately.

### Consequence for screen 2

Folding Surah / Juz / Page into one list is right for *finding*, but Page was also the door
to a different reading **mode**. With the mushaf promoted to a reader mode, that door moves
into the reader — and Browse keeps the page **information** on its rows, as already decided,
rather than a Page tab.

---

## Screens 5+ — pending

Background · Passages · Subjects · Themes · Topic detail · Tafseer · Khatam list ·
Khatam detail · Search.
