# 16-Line IndoPak Mushaf — Fidelity Pass/Fail Sheet

> Auto-derived from the shipped assets `assets/quran/mushaf/indopak_16_layout.json`
> and `assets/quran/mushaf/indopak_text.json` and pinned by
> `MushafLayoutFidelityTest`. Regenerate after any asset change.

## Book-level invariants

| Invariant | Expected | Actual | Result |
|---|---|---|---|
| Total pages (contiguous 1..N) | 548 | 548 (1..548) | ✅ |
| Total ayahs with IndoPak text | 6236 | 6236 | ✅ |
| Ayahs laid out (each once, all words) | 6236 | 6236 | ✅ |
| Surah headers (one per surah) | 114 | 114 | ✅ |
| Basmalah lines (all except 1 & 9) | 112 | 112 | ✅ |
| Word completeness (no gap/dup/reorder) | pass | pass | ✅ |
| Pages exceeding 16 lines | 0 | 0 | ✅ |
| Header+basmalah shared-line surahs (mapper must split) | 81 | 81 | ✅ |

## Sampled pages (line-for-line spot check)

Representative sample across the mushaf: the opening spread, a juz-boundary page,
a shared header/basmalah page, and the short closing surahs. `Lines` is the count of
distinct printed line numbers; `H`/`B` mark a surah header / basmalah on the page.

| Page | Lines | Surah(s) | H | B | Notes | Result |
|---|---|---|---|---|---|---|
| 1 | 7 | 1 | H | · | Al-Fātiḥah — opening spread (top half) | ✅ |
| 2 | 7 | 2 | H | B | Al-Baqarah opening — spread bottom half (starts at line 10) | ✅ |
| 3 | 16 | 2 | · | · |  | ✅ |
| 22 | 16 | 2 | · | · |  | ✅ |
| 42 | 16 | 2 | · | · |  | ✅ |
| 102 | 16 | 5 | · | · |  | ✅ |
| 151 | 16 | 7 | · | · |  | ✅ |
| 201 | 16 | 11 | · | · |  | ✅ |
| 251 | 16 | 16 | · | · |  | ✅ |
| 290 | 15 | 21 | H | B | Al-Anbiyāʾ header+basmalah share line 1; spacer line 2 | ✅ |
| 301 | 16 | 22 | · | · |  | ✅ |
| 342 | 16 | 27 | · | · |  | ✅ |
| 402 | 16 | 37 | · | · |  | ✅ |
| 452 | 16 | 46 | H | B |  | ✅ |
| 502 | 16 | 63–64 | H | B |  | ✅ |
| 528 | 16 | 78 | H | B |  | ✅ |
| 546 | 16 | 105–108 | H | B | Sūrah 108 Al-Kawthar (shortest surah) | ✅ |
| 547 | 16 | 109–112 | H | B | Sūrah 112 Al-Ikhlāṣ | ✅ |
| 548 | 10 | 113–114 | H | B | Sūrah 114 An-Nās (last page) | ✅ |

## Verified edge cases

| Case | Finding | Result |
|---|---|---|
| Al-Fātiḥah (p.1) | Header + 7 ayahs, no separate basmalah line (basmalah is ayah 1) | ✅ |
| At-Tawbah (sūrah 9) | Header present, no basmalah line | ✅ |
| Opening two-page spread | p.1 top-half, p.2 offset to line 10 (mirror) | ✅ faithful |
| Short muqaṭṭaʿāt / closing surahs | 108/112/114 each open with header on their page | ✅ |
| Line spanning two ayahs | Words carry per-word ayahId; tap/highlight per word | ✅ |
| Header + basmalah on one line (81 surahs) | Mapper now emits both as distinct lines | ✅ fixed |
| RTL | Words rendered with `TextDirection.Rtl` / `LayoutDirection.Rtl` | ✅ |
| Font scaling | Ayah lines auto-fit font **down** to page width; never overflow | ✅ |
| Dual-page spread parity | Page count script-aware (548 vs 604); even/odd pairing intact | ✅ |

## Known limitations (not defects in this layer)

- **Sajda (۩) and rukūʿ (۞) markers are not rendered.** The shipped IndoPak text
  (`mushaf_ayah_texts`) and the layout table carry no sajda/rukūʿ glyphs or line types, so
  the 16-line renderer shows the printed word glyphs faithfully but overlays no
  sajda/rukūʿ medallions. Sajda metadata still lives on the ayah entity
  (`sajda_type`) and is surfaced by the ayah-keyed reader. Adding markers to the
  16-line view requires regenerating the asset with those spans — tracked as an open
  item in `docs/ARCHITECTURE.md` §9.
