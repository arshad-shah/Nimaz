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

- **The Quran home screen crashes for any user with two or more Quran bookmarks.**
  `IllegalArgumentException: Key "0" was already used`, thrown when the home screen's
  bookmarks `LazyRow` composes.

  Root cause: `QuranRepositoryImpl.toQuranBookmark()` hardcodes `id = 0`
  (`QuranRepositoryImpl.kt:879`), so every Quran bookmark carries the same id, and
  `QuranHomeScreen.kt:458` keys that row by `it.id`.

  It first appeared when tapping "Start New Khatam" and again on "Subjects in the Qur'an" —
  both taps merely triggered the lazy prefetch that composed the row. The destination is
  irrelevant; only the bookmark count matters. Deterministic with two bookmarks.

  Fix is one line in either place — give the domain model the entity's real id, or key the
  row by `ayahId` — but it belongs on its own branch, not in the redesign.

- **Content: the thematic tree's first root is titled "Doctraine".** Its own description
  says "Doctrine". A content fix, against `arshad-shah/nimaz-data`.

- **Content/behaviour: a branch topic reports "0 verses".** Technically true — the root
  cites no ayahs itself — but it reads as broken. Resolved by the rolled-up counts and
  rolled-up ayah list decided for screens 5 and 6.

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

## Screen 5 — Themes (`QuranTopics`)

**Today:** the three-way segment the prototype proposes is **already shipped** —
`Themes · Kinds · Index` against the prototype's `Outline · By kind · Index`. Three roots
render (Doctraine, Stories, The Unseen) as bare text plus a chevron on the page background,
with **no counts anywhere**, even though the Qur'an home advertises "2,512 subjects,
indexed by hand". The row carries two tap targets that read as one: the chevron expands,
the label navigates.

**Prototype:** each node is a card with a count, children indented under a rail.

### Decided

- **Rolled-up counts on every node** — the total ayahs beneath it, children included. It
  gives the tree a sense of scale, and it makes a branch showing "0 verses" impossible.
  Needs a recursive count, computed once and cached.

### Open

- Whether the chevron/label split survives, or the whole row becomes one target.

---

## Screen 6 — Topic detail (`QuranTopicDetail`)

**Today:** a title, a `0 verses` chip, a prose description, and a "Subtopics" list rendered
as plain bullets with no counts and no chevrons — they do not read as tappable. No Arabic
name, no breadcrumb, and **no ayah list at all**, which is the point of a subject.

**Prototype:** name plus "N ayahs across the Quran", provenance ("Arrived from Al-Kahf", or
a breadcrumb), a card carrying the Arabic name and description, "Ayahs — first 3 of N"
tapping into the reader, and related subjects.

### Decided

- **Every topic shows an ayah list, rolled up from its children.** A branch topic lists the
  ayahs of everything beneath it, so "Doctrine" is never empty and the 0-verses state
  becomes unreachable.

---

## Screen 7 — Surah info (`SurahInfo`)

**Today:** a full destination. Scalloped gold cartouche, a Verses · Juz · Page strip, a
short summary of how the surah got its name, then "Go deeper" — Background, Passages and
Subjects, **each carrying a count** ("14 subjects, most-cited first") — and a bottom bar of
`Listen` (yellow) and `Start Reading` (teal).

**Prototype:** a bottom sheet. Four labelled fact tiles (it adds "Revealed in", which the
screen lacks), then four flat buttons with **no counts** and **no summary**.

### Decided

- **Adopt the sheet, and carry over what the screen does better.** The sheet keeps you in
  Browse instead of pushing a screen you immediately back out of — but it keeps the summary
  paragraph and the **counted** onward rows, because "14 subjects, most-cited first" tells
  you whether tapping is worth it and a bare "Subjects" does not. Taller than the
  prototype's sheet, and scrollable.
- **One primary action.** "Read surah" is primary in the section accent; Listen becomes
  secondary. Today's yellow-beside-teal pair puts two accents in one row and reads as a
  warning.

---

## Component rebuilds (applies across screens)

Two shared components are **rebuilt to the prototype's design**, not restyled in place:

- **The segmented tabs** — the prototype's pill-in-tray control: a recessed track with the
  selected segment lifted as a white pill. This replaces the underlined `TabRow` on Quran
  home and the pill tabs on Themes, Khatam and Bookmarks, so one control serves them all.
- **The accordion / tree node** — the prototype's card-per-node with a rotating chevron and
  an indent rail down the children, replacing the bare text rows on Themes.

---

## Screen 8 — Surah background (`SurahBackground`)

**Today:** a sticky, horizontally-scrolling chip row across the top styled as a **filter**
(`✓ Name` selected in yellow) when it is really a jump index. Every section then prints its
name **twice** — a small teal icon eyebrow, then a large heading saying the same word.

**Prototype:** no chip row, one small uppercase eyebrow per section, a gold-ruled ayah
pull-quote, and Passages / Subjects buttons at the foot.

### Decided

- **Keep the chip row as a jump index, restyle away the filter look.** The longest
  background is 47 KB of prose, so an index earns its place — but the check mark and the
  selected-yellow have to go. It marks position; it does not filter.
- **One heading per section**, in the eyebrow style. Deleting the duplicate recovers about
  90 px per section.
- **Adopt the pull-quote.** Cited ayahs render in the gold-ruled block with their reference,
  tapping through to the reader.

---

## Screen 4c — Recitation player

**Today:** the whole player is one collapsed bar — play, "Al-Fatiha", pills for
`Ayah 1/7 · Juz 1 · p.1`, and a hairline progress line. No seek, no reciter shown, no
repeat, no speed; tapping it does not expand. Reciter choice lives away in
Settings → Quran → Select Reciter.

**Prototype:** seek with elapsed/remaining, prev/next ayah, reciter name and style inline
and tappable, repeat (off / ayah / range / surah) with a count stepper and range picker,
speed 0.75–1.5×, a "Follow along and turn the page" toggle, a violet
"Downloading N of M" strip, follow-along highlighting in the text, and
"Play from here" / "Repeat this ayah" in the ayah sheet.

`docs/SUBSYSTEMS.md` §1 already says ayahs are downloaded before playback and cached under
`filesDir/quran_audio/` — so the download strip gives a UI to work the app already does
invisibly. Repeat and speed are genuinely new.

### Decided

- **Build all of it** — repeat, speed, follow-along and the download strip. Repeat-by-range
  and repeat-count are memorisation features; the download strip only surfaces existing
  behaviour. This is the largest single piece of scope in the redesign.

---

## Reader mode switching (revises screen 4b)

The second prototype puts a three-way `Translation · Mushaf · 16-line` segmented row under
the app bar. **That row is rejected.**

### Decided

- **A top-bar icon showing the current mode**, opening a short menu to switch. Always one
  tap, no permanent row, and the icon states which mode you are in.
- The mode set grows to three: Translation, Mushaf, and **16-line (IndoPak)**.

---

## Mushaf visual register (revises screen 4b)

The two prototypes disagree with the shipping app about how ornate the mushaf should be.
The shipping page has a double gold/teal border, a scalloped cartouche and a gold rosette;
the second prototype uses a warm **paper** register — cream ground, hairline rules, a simple
ruled cartouche, a small page medallion.

### Decided

- **Adopt the prototype's paper register**, including a paper palette
  (`--paper` / `--paper-line` / `--paper-ink`) held separately from the app's surface
  colours, with its own dark-mode values. The heavy gold border, scalloped cartouche and
  rosette are dropped.

This supersedes the earlier, looser "the mushaf keeps its ornament" note: the mushaf still
reads as a printed page, but through paper and hairlines rather than gold.

---

## Read tracking (revises screen 4)

### Decided

- **A page-level read mark in the page bar, plus "Mark read for khatam" in the ayah sheet —
  both shown only while a khatam is active.** The control disappears entirely for the
  majority of reading, which is not part of a plan; marking a whole page matches how a
  juz-a-day plan is actually kept, and the ayah sheet keeps per-verse precision available.

This resolves the open question left on screen 4 about where the read-toggle lives, and
retires the always-present per-ayah circle.

---

## Screens 9+ — pending

Passages · Subjects · Tafseer · Khatam list · Khatam detail · Search.
