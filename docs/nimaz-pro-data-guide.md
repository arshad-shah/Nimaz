# Nimaz - Data Sourcing & Integration Guide

> **Status:** the data *formats* below are still a useful reference, but the app now ships a
> prebuilt database (`app/src/main/assets/database/nimaz_prepopulated.db`) and backfills newer
> content at runtime via idempotent seeders. See the "Database & migrations" and "Content seeding
> & versioning" sections of [`SUBSYSTEMS.md`](SUBSYSTEMS.md), and `nimaz-pro-data/` for the
> generation scripts. App name: **Nimaz**; package: **`com.arshadshah.nimaz`**.


## For Parallel Claude Code Instance

**Purpose**: Generate all Islamic content data in the exact JSON format required by the app.

---

# Quick Start

## Output Structure

```
nimaz-pro-data/
├── json/
│   ├── surahs.json              # 114 surahs
│   ├── ayahs.json               # 6,236 verses
│   ├── translations.json        # Sahih International
│   ├── hadith_books.json        # 6 collections metadata
│   ├── hadith_bukhari.json      # Bukhari hadiths
│   ├── hadith_muslim.json       # Muslim hadiths
│   ├── hadith_abudawud.json     # Abu Dawud hadiths
│   ├── hadith_tirmidhi.json     # Tirmidhi hadiths
│   ├── hadith_nasai.json        # Nasai hadiths
│   ├── hadith_ibnmajah.json     # Ibn Majah hadiths
│   ├── dua_categories.json      # 15 categories
│   ├── duas.json                # 200+ duas
│   ├── islamic_events.json      # Calendar events
│   └── tasbih_presets.json      # Dhikr presets
└── scripts/
    └── generate_database.py     # SQLite generator
```

---

# 1. Quran Data

## Sources
- **Tanzil.net**: https://tanzil.net/download/ (Arabic text)
- **Quran.com API**: https://api.quran.com/api/v4/ (Translations)
- **quran-json GitHub**: https://github.com/semarketir/quranjson

## surahs.json Format

```json
[
  {
    "id": 1,
    "number": 1,
    "name_arabic": "الفاتحة",
    "name_english": "The Opening",
    "name_transliteration": "Al-Fatihah",
    "revelation_type": "MECCAN",
    "verses_count": 7,
    "order_revealed": 5,
    "start_page": 1
  }
]
```

**Required**: All 114 surahs with accurate metadata.

## ayahs.json Format

```json
[
  {
    "id": 1,
    "surah_id": 1,
    "number_in_surah": 1,
    "number_global": 1,
    "text_arabic": "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
    "text_uthmani": "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ",
    "juz": 1,
    "hizb": 1,
    "page": 1,
    "sajda": false,
    "sajda_type": null
  }
]
```

**Required**: All 6,236 ayahs with juz/hizb/page info.

**Sajda Verses** (mark `sajda: true`):
- 7:206, 13:15, 16:50, 17:109, 19:58, 22:18, 22:77, 25:60, 27:26, 32:15, 38:24, 41:38, 53:62, 84:21, 96:19

## Tajweed colouring pipeline (`text_tajweed`, issues #288 + #290 + #289 of epic #287)

The `ayahs.text_tajweed` column holds a compact JSON array of segments
(`[{"t": "…", "r": "code"}]`, `r = null` for plain text) that the Android
renderer colours per rule. It is produced at DB-build time by
`scripts/generate_database.py`, which calls
`preparse_tajweed.preparse_single(raw, key="surah:ayah")` on the quran.com
markup in `json/tajweed.json`.

> **Source datasets & licences:** `json/tajweed.json` (quran.com `uthmani_tajweed`
> API — rule span boundaries) and `json/tajweed_cpfair.json` (cpfair
> `quran-tajweed` — the independent second source used for the madd
> classification). Provenance and licensing are recorded in
> **[`json/LICENSES_TAJWEED.md`](../nimaz-pro-data/json/LICENSES_TAJWEED.md)**.

**Pre-parser (`scripts/preparse_tajweed.py`).** The markup contains
arbitrarily **nested** `<tajweed>` tags (33 in the current source, e.g. `2:190`
has a `slnt` span inside a `madda_obligatory` span). The parser is a
**stack-based tokenizer**, not a regex:

- The **innermost** rule wins for the characters it covers; the enclosing rule
  is kept for the rest. `2:190` → `ُوٓ` = `mt`, `اْ` = `sl`, `‌ۚ` = `mt`.
- **Invariant:** concatenating every segment's `t` reconstructs the source text
  exactly (tags + verse markers removed) — no character is ever dropped.
  Because of nesting, per-*segment* code counts do not equal raw *tag* counts.
- Unknown class, stray/unbalanced tag, or unclosed tag → **`ValueError`**
  (fail loud), so corrupt source can never silently ship the wrong colours.
- **`SOURCE_FIXUPS`** (keyed by `surah:ayah`) patches ayahs malformed at
  source *before* tokenizing — currently only `32:3`, whose opening
  `madda_normal` tag was corrupted to a stray `>`. Keying by fragment means a
  corrected upstream re-fetch simply no-ops.

**Orthography normalisation (issue #290).** The quran.com tajweed markup and
the app's canonical `ayahs.text_arabic` are two *different* Uthmani
transcriptions of the same verse — stripping the tags leaves them disagreeing on
**87% of ayahs** (tatweel carriers for the dagger alef, ZWNJ, pause-mark
encodings U+06DF/06E2/06ED, tanween forms, alef variants). If they disagree,
toggling "Show Tajweed Colors" changes the **glyphs** on screen — word widths,
line breaks — not just their colour, and no single string can back search,
bookmarks and audio highlighting.

`text_arabic` is treated as the **single canonical text** (it is what the app
already renders everywhere else). The coloured segments are **re-derived over
it** rather than shipped in the quran.com encoding:

1. `normalise_uthmani(text)` strips the BOM / zero-width marks and trims — light
   by design, so it never alters the glyphs the app already renders.
2. `align_segments_to_canonical(segments, canonical)` runs a character-level
   diff (`difflib`) of the stripped tajweed text against the canonical text and
   transfers each rule label across: *equal* runs copy per-char; *replace* runs
   take the region's last non-null rule (the rule usually sits on a re-encoded
   mark, e.g. a small-yeh madd); *insert* runs (canonical-only combining marks)
   inherit the preceding rule; *delete* runs (tatweel/ZWNJ, or a small-waw madd
   with no canonical glyph) drop out.

Because the segments are rebuilt from `text_arabic`'s **own** characters, the
invariant `strip(text_tajweed) == normalise_uthmani(text_arabic)` holds
**byte-for-byte for all 6 236 ayahs** by construction. `generate_database.py`
passes `canonical_text=text_arabic` into `preparse_single`, and also stores the
**normalised** `text_arabic` (BOM removed). Verify with:

```bash
python3 nimaz-pro-data/scripts/verify_tajweed_orthography.py   # exits non-zero on drift
```

**Validation harness (issue #292).** `scripts/verify_tajweed.py` is the full gate.
In **pipeline mode** (default) it runs the pre-parser + taxonomy split + alignment
over the JSON sources in memory (no Git-LFS DB needed — CI-friendly) and asserts:
coverage (every ayah has spans, except the pinned #298 allow-list), well-formedness
(no leaked `<`/`>`/`tajweed`), the #290 round-trip, the v3 code whitelist (no legacy
`mo`/`mp`/`q`), character-coverage conservation, cross-source **drift** vs cpfair
(signed per-category deltas checked against `tests/fixtures/cpfair_drift_allowlist.json`
— this is drift detection, *not* independent validation: the two datasets share the
identical 63-ayah gap), and a pinned golden-ayah fixture. In **db mode**
(`--db out.db`) it verifies a generated `nimaz_prepopulated.db`. It is invoked as a
fail-the-build post-step by both `generate_database.py` and `verify_database.py`, and
by the `tajweed_data_checks.yml` CI workflow on PRs touching `nimaz-pro-data/**`.

> **DB regeneration note.** These pipeline changes only affect the shipped DB
> when `nimaz_prepopulated.db` is regenerated; the `text_arabic` column changes
> (BOM removed on 1:1) so a regeneration is required for the fix to reach the
> app, and — like all prepackaged-DB edits — it does **not** reach existing
> installs on update (see `docs/SUBSYSTEMS.md` §5/§7; a runtime seeding path, if
> needed, would be decided with the renderer work in #293).

**Rule taxonomy — v3 codes (issue #289).** The quran.com classes mis-name the
madd rules: `madda_obligatory` merges two *different* rules, and
`madda_permissible` is actually Madd 'Aarid. Cross-validated against the
independent `json/tajweed_cpfair.json` dataset, the pipeline emits a corrected
**v3 code set**. The munfasil/muttasil split is applied at the **source-tag**
level (`reclassify_madd_obligatory`) using cpfair's per-ayah reading order — the
i-th `madda_obligatory` tag is rewritten to `madd_munfasil`/`madd_muttasil` per
cpfair; the counts agree for 6 227/6 236 ayahs, and the 9 that disagree keep the
conservative obligatory (`mt`) default. Qalqalah is split positionally
(`split_qalqalah`): word-final → Kubra (`qk`), medial → Sughra (`qs`).

| code | rule | counts | was |
|---|---|---|---|
| `mn` | Madd Tabee'i (natural) | 2 | `mn` |
| `mf` | Madd Jaiz **Munfasil** | 2/4/5 | part of `mo` |
| `mt` | Madd Wajib **Muttasil** | 4/5 | part of `mo` |
| `ma` | Madd **'Aarid** lis-Sukun | 2/4/6 | `mp` (mis-named) |
| `ml` | Madd **Lin** | 2/4/6 | — (code defined; populated in #291) |
| `my` | Madd Lazim (necessary) | 6 | `my` |
| `qs` / `qk` | Qalqalah Sughra / Kubra | — | `q` |

Beat counts follow the **Hafs 'an 'Asim** reading (ref: Kareema Czerepinski,
*Tajweed Rules of the Qur'an*). The canonical rule names, one-line explanations
and colours are the single source of truth in `TajweedParser.rules`
(`core/util/TajweedParser.kt`); legacy `mo`/`mp`/`q` and the v1 single-letter
codes still parse (mapped to `mt`/`ma`/`qs`) so an older prepackaged DB never
crashes. Colours live in `NimazColors.TajweedColors` — the six madd sub-rules
share one warm hue family (rose→red→pink) with distinct lightness.

`scripts/preparse_tajweed.py` can also be run standalone to regenerate the
`json/tajweed_parsed.json` reference artifact (not consumed by the build —
`generate_database.py` parses inline); run standalone it loads `ayahs.json` +
`tajweed_cpfair.json` and applies the same normalisation and taxonomy split so
the artifact matches the DB. Tests live in
`scripts/tests/test_preparse_tajweed.py` (`python3 -m unittest`), covering the
nested/malformed/unknown/whitespace cases, the normalisation/alignment helpers,
the munfasil/muttasil + qalqalah split, and the whole-corpus invariants.

**Rules not yet implemented (issue #291).** A printed colour-coded mushaf shows
rules that *neither* shipped dataset marks. They are **not** in the pipeline yet
because the derived ones encode fiqh-of-recitation decisions that **require
scholarly review against a printed tajweed mushaf before shipping** (getting them
wrong teaches incorrect recitation). Status:

| Rule | Kind | Status |
|---|---|---|
| Tafkhim/Tarqiq of Raa (ر) | derived (position + vowel) | **not implemented** — needs review |
| Tafkhim/Tarqiq of Lam in لفظ الجلالة | derived | **not implemented** — needs review |
| Isti'la letters (خ ص ض غ ط ق ظ) always heavy | deterministic | recorded in `tajweed_special_rules.json` (not wired) |
| Madd al-Lin (`ml`) | derived (و/ي sakinah after fatha before a stop) | code/colour defined (#289); **population needs review** |
| Sakt (السكت) | enumerated | recorded + reconciled (see below); not wired |
| Idgham Mutamathilayn | derived | **not implemented** |
| Imalah 11:41 · Ishmam 12:11 · Tasheel 41:44 · Naql 49:11 | enumerated | recorded in `tajweed_special_rules.json`; not wired |
| Qalqalah Sughra/Kubra | derived (positional) | **implemented** — `split_qalqalah`, word-final → kubra (a simplified heuristic; a waqf-mark-aware version is future) |
| Waqf / stop signs (7 signs) | present-but-unstyled | classified in `tajweed_special_rules.json`; styling not wired |

The enumerated, citable facts (fixed in Hafs) are recorded in
**`json/tajweed_special_rules.json`** as a reference for a future *reviewed*
implementation — that file is deliberately **not** consumed by
`generate_database.py`. Notable reconciliation recorded there: the **7 occurrences
of U+06DC** in `text_arabic` are 4 canonical Hafs sakt (18:1, 36:52, 75:27, 83:14)
+ 1 additional sakt (69:28, مَالِيَهْ→هَلَكَ) + 2 non-sakt uses of the same sign as
the *small-seen-over-ṣād* alternate-reading marker (2:245, 7:69) — answering the
"where do the extra 3 come from" question.

> Still open in sibling sub-issues of epic #287: the extended rules above
> (#291, with mandatory scholarly review), the in-app legend + accessibility UI
> half (#294), the 63 fully-unannotated ayahs (#298), and the grapheme-boundary
> decision (#299). The in-app legend will consume `TajweedParser.rules` directly.

## translations.json Format

```json
[
  {
    "id": 1,
    "ayah_id": 1,
    "translator_id": "sahih_international",
    "text": "In the name of Allah, the Entirely Merciful, the Especially Merciful."
  }
]
```

**Recommended Translations**:
- Sahih International (primary)
- Pickthall (optional)
- Yusuf Ali (optional)

## 16-line IndoPak Mushaf data (issue #265 — sub-task 1/7 of #263)

> **Status: data acquired, validated, and in-repo.** `ayahs_indopak.json` (6,236 rows) and
> `mushaf_layout_indopak16.json` (548 pages) are generated by
> `nimaz-pro-data/scripts/download_indopak_mushaf_data.py` from the Quranic Universal Library
> (QUL) resource *"Indopak 16 lines layout (Taj company)"* and pass every acceptance check below.
> The one item that remains a **human decision** is the licensing sign-off for *shipping* the bulk
> data in a release build — see the ⚠️ note in `nimaz-pro-data/json/LICENSES_INDOPAK.md` (the
> parent issue's own estimate — "3–5 days, dominated by data validation + license verification" —
> always assumed a human in that loop). **DB/schema wiring is done in sub-task 2/7 — see below.**

**Why existing `ayahs.json` isn't enough.** `ayahs.json` / `AyahEntity` (`text_arabic` /
`text_uthmani`, `page`) is keyed to the **604-page Madani Mushaf** in Uthmani script. A
line-accurate "16-line" IndoPak display needs, per the printed 16-line Mushaf: IndoPak glyph text
for every ayah, and a **548-page, ≤16-lines-per-page** layout map (page, line, line type,
word/segment position). Neither exists anywhere in this repo today.

**Source used.** [Quranic Universal Library (QUL) / Tarteel.ai](https://qul.tarteel.ai) resource
[**"Indopak 16 lines layout (Taj company)"**](https://qul.tarteel.ai/resources/mushaf-layout/11)
(resource id 11): a 548-page, ≤16-lines-per-page IndoPak layout that renders every word with its
`surah:ayah:word` location and IndoPak glyph text. The acquisition script reads QUL's **public
per-page preview** for pages 1–548 (no account/auth), then normalises and validates. Candidates
considered and why QUL won:

| Source | What it provides | Outcome |
|---|---|---|
| **QUL resource 11 (used)** | 548-page 16-line IndoPak layout with per-word page/line positions **and** IndoPak glyph text in one place | Public, parseable per page, MIT-licensed platform, and the reference dataset behind Quran for Android/iOS + Tarteel. Chosen. |
| [Quran Foundation Content API](https://api-docs.quran.foundation/docs/tutorials/fonts/page-layout/) | `page-layout` `INDOPAK_16_LINES` (548 pages) | Requires OAuth2 client registration; ToS focuses on live per-request use. Viable fallback, heavier to set up. |
| Tanzil.net | Uthmani Arabic text (source for `ayahs.json`) | No IndoPak glyphs and no line-layout — insufficient alone. |

**On the "Taj company" naming and copyright.** The issue rightly warns against copying a commercial
edition's *typesetting*. What ships here is **QUL's own structured line-break data** (word →
page/line positions), not a scan, image, or transcription of Taj Company's typeset artwork. "Taj
company" names the 548-page 16-line *layout standard* the data follows — the edition ḥuffāẓ
memorise from — which is exactly the "structured data, not scanned page images" path the issue
points to. The final sign-off for *shipping* this in a release build is a documented human decision
(see `LICENSES_INDOPAK.md`).

**Target files & schema**, reconciled against the existing 6,236-ayah id space (`AyahEntity.id`):

`nimaz-pro-data/json/ayahs_indopak.json` — one row per existing ayah id. `words[]` is indexed 1:1
with the layout's word positions, and `text_indopak == " ".join(words)`:
```json
[
  {
    "ayah_id": 1,
    "text_indopak": "بِسْمِ اللّٰهِ الرَّحْمٰنِ الرَّحِیْمِ ۟",
    "words": ["بِسْمِ", "اللّٰهِ", "الرَّحْمٰنِ", "الرَّحِیْمِ", "۟"]
  }
]
```
The final `words[]` entry of most ayahs is QUL's end-of-ayah marker glyph (a Private Use Area
codepoint, U+F500…U+F6FF); it — like the IndoPak diacritics — renders fully only with the matching
IndoPak font. That font is now bundled (sub-task 3/7, issue #267):
`app/src/main/res/font/indopak_nastaleeq.ttf` = *AlQuran IndoPak by QuranWBW* v2.100, the QUL
companion face, verified to cover all 346 PUA marker codepoints used here plus full IndoPak
letterforms; wired into the font picker as `QuranArabicFont.INDOPAK` (`presentation/theme/Type.kt`),
licence/attribution in `docs/FONT_LICENSES.md`. Any detached waqf mark that the source renders as a
spaced fragment is re-attached to its word so positions stay 1:1 with the layout.

`nimaz-pro-data/json/mushaf_layout_indopak16.json` — one row per line-segment (a line may hold one
or more ayahs/partial ayahs, a surah header, or a basmalah):
```json
[
  {
    "page_number": 1, "line_number": 1, "line_type": "surah_header",
    "surah_id": 1, "ayah_id": null, "first_word_position": null, "last_word_position": null
  },
  {
    "page_number": 1, "line_number": 2, "line_type": "ayah",
    "surah_id": 1, "ayah_id": 1, "first_word_position": 1, "last_word_position": 5
  }
]
```
`line_type` is one of `ayah` / `surah_header` / `basmalah`. `ayah_id`/word positions are null for
`surah_header` and `basmalah` lines. Word positions are 1-based into the ayah's `words[]`, so a
line covering part of a long ayah records just its word range. Rows are ordered by
`(page_number, line_number)`; multiple rows can share a line (e.g. two ayahs, or a header and its
basmalah band). QUL marks bismillah bands inconsistently, so basmalah rows are reconciled to
exactly one per surah (owner = the surah whose first ayah follows the band). Where QUL folds a
surah's bismillah into the name banner, the reconciled basmalah row is emitted on the **same
`line_number` as that surah's `surah_header`** — this happens for **81 of the 112** basmalah-bearing
surahs (the other 31 get a dedicated basmalah line). The consumer must therefore treat a
header-and-basmalah pair sharing a `line_number` as two logical lines, not collapse them: see the
7/7 fidelity note below and `MushafLayoutMapper`.

**Acceptance criteria (from #265 — all met):**
- [x] `ayahs_indopak.json` covers all 6,236 ayah ids (ids match `AyahEntity.id`; no missing/extra/empty).
- [x] `mushaf_layout_indopak16.json` covers all 548 pages, each with ≤16 lines; 114 surah headers,
  112 basmalah lines, and every ayah's word positions exactly covered by its line-segments.
- [x] `nimaz-pro-data/json/LICENSES_INDOPAK.md` records the exact source, version/date, and license
  situation actually used, with the shipping sign-off flagged as the remaining human decision.

Re-generate/validate any time with `python3 nimaz-pro-data/scripts/download_indopak_mushaf_data.py`
(pages cache under `$INDOPAK_CACHE_DIR`, default `/tmp/indopak_pages`).

**Fidelity verification (sub-task 7/7 of #263, #271).** The shipped assets are now pinned by
`MushafLayoutFidelityTest` (`app/src/test/.../data/local/quran/`), which reads
`assets/quran/mushaf_layout_indopak16.json` + `ayahs_indopak.json` directly and re-asserts every
acceptance criterion above on the bytes that actually ship — plus the strong invariant that **each
ayah's words are covered exactly once, in reading order, with no gaps, duplicates, or reordering**
across its line-segments (a single wrong line break would fail the test). The human-readable
per-page pass/fail sheet is generated at
[`docs/quran/16-line-fidelity-sheet.md`](quran/16-line-fidelity-sheet.md). Two known-good
structural quirks are documented there rather than "fixed": the decorative **opening two-page
spread** (page 2's first printed line is line 10, mirroring page 1) and a spacer line on page 290.
One limitation: the IndoPak text/layout carry **no sajda (۩) or rukūʿ (۞) markers**, so the 16-line
view does not overlay them (sajda metadata still lives on `ayahs.sajda_type`; see the sajda ayah
list under "ayahs.json Format" above and `ARCHITECTURE.md` §9 Open). If the assets are ever
regenerated, re-run `:app:testDebugUnitTest` and regenerate the sheet.

### DB schema, migration & distribution (sub-task 2/7 of #263)

**Schema (`NIMAZ_DATABASE_VERSION = 18`).** `MIGRATION_17_18` (in `NimazDatabase.kt`):
- adds a nullable **`ayahs.text_indopak`** column (full IndoPak text per ayah, keyed by the global
  `AyahEntity.id` 1–6236); and
- creates **`mushaf_layout_indopak16`** — one row per line-segment, mirroring
  `mushaf_layout_indopak16.json` 1:1 (`id` autoincrement PK, `page`, `line`, `line_type`,
  `surah_id`, `ayah_id` = global id or null, `first_word_position`, `last_word_position`), indexed
  on `(page, line)`.

The glyph text is **not** duplicated into the layout table — the rendering/data layer (4/7)
reconstructs each line's words by slicing `text_indopak.split(' ')` with the stored inclusive
positions. This is lossless: the 1/7 data guarantees `text_indopak == " ".join(words)` with no
intra-word spaces, and every ayah's line-segments exactly cover `1..len(words)`.

**Distribution — seeded assets, NOT a regenerated prepackaged DB.** The prepackaged
`assets/database/nimaz_prepopulated.db` (~147 MB, Git LFS) is copied by `createFromAsset` **only on
a fresh install**, so baking the IndoPak data into it would never reach existing installs *and*
would grow the LFS blob by tens of MB. Instead the two JSON files are shipped verbatim as bundled
Android assets (`app/src/main/assets/quran/ayahs_indopak.json` +
`mushaf_layout_indopak16.json`, **~0.75 MB compressed in the APK**) and populated at runtime by
`QuranIndopakSeeder` (idempotent, version-gated on `PreferencesKeys.INDOPAK_CONTENT_VERSION`),
exactly as Dua/Help/Qaida content is seeded. Fresh installs and upgraders both converge on the same
data. See `docs/SUBSYSTEMS.md` §5/§7 and `docs/ARCHITECTURE.md` §9.

**`generate_database.py` is kept schema-complete** (`populate_indopak_16line()` ingests the two JSON
files into `text_indopak` + `mushaf_layout_indopak16`) so a from-scratch DB regeneration stays
consistent with the Room schema — but the shipped LFS asset is intentionally left untouched by 2/7.

---

# 2. Hadith Data

## Sources
- **Sunnah.com API**: https://sunnah.com/developers (requires free API key)
- **HadithAPI**: https://hadithapi.com/
- **GitHub**: https://github.com/semarketir/hadith-json

## hadith_books.json Format

```json
[
  {
    "id": 1,
    "name_english": "Sahih al-Bukhari",
    "name_arabic": "صحيح البخاري",
    "author": "Imam Muhammad al-Bukhari",
    "hadith_count": 7563,
    "description": "The most authentic collection of Hadith.",
    "icon": "📗"
  }
]
```

**Books Required**:
| ID | Name | Arabic | Hadiths |
|----|------|--------|---------|
| 1 | Sahih al-Bukhari | صحيح البخاري | ~7,563 |
| 2 | Sahih Muslim | صحيح مسلم | ~7,500 |
| 3 | Sunan Abu Dawud | سنن أبي داود | ~5,274 |
| 4 | Jami at-Tirmidhi | جامع الترمذي | ~3,956 |
| 5 | Sunan an-Nasai | سنن النسائي | ~5,758 |
| 6 | Sunan Ibn Majah | سنن ابن ماجه | ~4,341 |

## hadith_[book].json Format

```json
[
  {
    "id": 1,
    "book_id": 1,
    "chapter_id": 1,
    "number_in_book": 1,
    "number_in_chapter": 1,
    "text_arabic": "إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ...",
    "text_english": "Actions are judged by intentions...",
    "narrator": "Narrated 'Umar bin Al-Khattab",
    "grade": "Sahih",
    "reference": "bukhari:1"
  }
]
```

**Grade Values**: `Sahih`, `Hasan`, `Da'if`, `Mawdu`

---

# 3. Duas & Adhkar

## Source
- **Hisnul Muslim** (Fortress of the Muslim) - Public domain

## dua_categories.json Format

```json
[
  {
    "id": 1,
    "name_english": "Morning Adhkar",
    "name_arabic": "أذكار الصباح",
    "icon": "🌅",
    "display_order": 1,
    "dua_count": 15
  }
]
```

**Categories Required**:
1. Morning Adhkar (أذكار الصباح)
2. Evening Adhkar (أذكار المساء)
3. After Prayer (أذكار بعد الصلاة)
4. Waking Up (دعاء الاستيقاظ)
5. Before Sleep (دعاء النوم)
6. Entering Home (دعاء دخول المنزل)
7. Leaving Home (دعاء الخروج من المنزل)
8. Entering Mosque (دعاء دخول المسجد)
9. Leaving Mosque (دعاء الخروج من المسجد)
10. Before Eating (دعاء قبل الطعام)
11. After Eating (دعاء بعد الطعام)
12. Traveling (دعاء السفر)
13. Rain (دعاء المطر)
14. Distress & Anxiety (دعاء الكرب)
15. Forgiveness (الاستغفار)

## duas.json Format

```json
[
  {
    "id": 1,
    "category_id": 1,
    "title_english": "Ayatul Kursi",
    "title_arabic": "آية الكرسي",
    "text_arabic": "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ...",
    "transliteration": "Allahu la ilaha illa Huwa, Al-Hayyul-Qayyum...",
    "translation": "Allah! There is no god but He, the Living, the Self-subsisting...",
    "source": "Quran 2:255",
    "virtue": "Whoever recites this when he rises in the morning will be protected...",
    "repeat_count": 1,
    "audio_file": null,
    "display_order": 1
  }
]
```

---

# 4. Islamic Events

## islamic_events.json Format

```json
[
  {
    "id": 1,
    "name_english": "Islamic New Year",
    "name_arabic": "رأس السنة الهجرية",
    "hijri_month": 1,
    "hijri_day": 1,
    "event_type": "HOLIDAY",
    "description": "The first day of the Islamic calendar.",
    "is_holiday": true
  }
]
```

**Event Types**: `HOLIDAY`, `FASTING`, `NIGHT`, `CELEBRATION`

**Required Events**:
| Event | Month | Day | Type |
|-------|-------|-----|------|
| Islamic New Year | 1 | 1 | HOLIDAY |
| Day of Ashura | 1 | 10 | FASTING |
| Mawlid an-Nabi | 3 | 12 | CELEBRATION |
| Isra and Mi'raj | 7 | 27 | CELEBRATION |
| Start of Ramadan | 9 | 1 | FASTING |
| Laylat al-Qadr | 9 | 27 | NIGHT |
| Eid al-Fitr | 10 | 1-3 | HOLIDAY |
| Day of Arafah | 12 | 9 | FASTING |
| Eid al-Adha | 12 | 10-13 | HOLIDAY |

---

# 5. Tasbih Presets

## tasbih_presets.json Format

```json
[
  {
    "id": 1,
    "name": "SubhanAllah",
    "arabic": "سُبْحَانَ اللَّهِ",
    "transliteration": "SubhanAllah",
    "translation": "Glory be to Allah",
    "target_count": 33,
    "is_custom": false,
    "display_order": 1
  }
]
```

**Required Presets**:
1. SubhanAllah (سُبْحَانَ اللَّهِ) - 33x
2. Alhamdulillah (الْحَمْدُ لِلَّهِ) - 33x
3. Allahu Akbar (اللَّهُ أَكْبَرُ) - 34x
4. La ilaha illallah (لَا إِلَٰهَ إِلَّا اللَّهُ) - 100x
5. Astaghfirullah (أَسْتَغْفِرُ اللَّهَ) - 100x
6. La hawla wa la quwwata illa billah - 100x
7. Salawat on Prophet ﷺ - 100x
8. SubhanAllahi wa bihamdihi - 100x

---

# 6. Database Generation Script

Create `scripts/generate_database.py`:

```python
#!/usr/bin/env python3
"""
Nimaz - SQLite Database Generator
Converts JSON files to pre-populated Room database
"""

import sqlite3
import json
from pathlib import Path

JSON_DIR = Path("json")
OUTPUT_DB = Path("output/nimaz_prepopulated.db")
OUTPUT_DB.parent.mkdir(exist_ok=True)

def create_tables(conn):
    """Create all tables matching Room entity definitions"""
    cursor = conn.cursor()
    
    # Surahs
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS surahs (
            id INTEGER PRIMARY KEY,
            number INTEGER NOT NULL,
            name_arabic TEXT NOT NULL,
            name_english TEXT NOT NULL,
            name_transliteration TEXT NOT NULL,
            revelation_type TEXT NOT NULL,
            verses_count INTEGER NOT NULL,
            order_revealed INTEGER NOT NULL,
            start_page INTEGER NOT NULL
        )
    ''')
    
    # Ayahs
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS ayahs (
            id INTEGER PRIMARY KEY,
            surah_id INTEGER NOT NULL,
            number_in_surah INTEGER NOT NULL,
            number_global INTEGER NOT NULL,
            text_arabic TEXT NOT NULL,
            text_uthmani TEXT NOT NULL,
            juz INTEGER NOT NULL,
            hizb INTEGER NOT NULL,
            page INTEGER NOT NULL,
            sajda INTEGER NOT NULL DEFAULT 0,
            sajda_type TEXT,
            FOREIGN KEY (surah_id) REFERENCES surahs(id)
        )
    ''')
    
    # Translations
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS translations (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            ayah_id INTEGER NOT NULL,
            translator_id TEXT NOT NULL,
            text TEXT NOT NULL,
            FOREIGN KEY (ayah_id) REFERENCES ayahs(id)
        )
    ''')
    
    # Hadith Books
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS hadith_books (
            id INTEGER PRIMARY KEY,
            name_english TEXT NOT NULL,
            name_arabic TEXT NOT NULL,
            author TEXT NOT NULL,
            hadith_count INTEGER NOT NULL,
            description TEXT NOT NULL,
            icon TEXT NOT NULL
        )
    ''')
    
    # Hadiths
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS hadiths (
            id INTEGER PRIMARY KEY,
            book_id INTEGER NOT NULL,
            chapter_id INTEGER NOT NULL,
            number_in_book INTEGER NOT NULL,
            number_in_chapter INTEGER NOT NULL,
            text_arabic TEXT NOT NULL,
            text_english TEXT NOT NULL,
            narrator TEXT NOT NULL,
            grade TEXT NOT NULL,
            reference TEXT NOT NULL,
            FOREIGN KEY (book_id) REFERENCES hadith_books(id)
        )
    ''')
    
    # Dua Categories
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS dua_categories (
            id INTEGER PRIMARY KEY,
            name_english TEXT NOT NULL,
            name_arabic TEXT NOT NULL,
            icon TEXT NOT NULL,
            display_order INTEGER NOT NULL,
            dua_count INTEGER NOT NULL
        )
    ''')
    
    # Duas
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS duas (
            id INTEGER PRIMARY KEY,
            category_id INTEGER NOT NULL,
            title_english TEXT NOT NULL,
            title_arabic TEXT NOT NULL,
            text_arabic TEXT NOT NULL,
            transliteration TEXT NOT NULL,
            translation TEXT NOT NULL,
            source TEXT NOT NULL,
            virtue TEXT,
            repeat_count INTEGER NOT NULL DEFAULT 1,
            audio_file TEXT,
            display_order INTEGER NOT NULL,
            FOREIGN KEY (category_id) REFERENCES dua_categories(id)
        )
    ''')
    
    # Islamic Events
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS islamic_events (
            id INTEGER PRIMARY KEY,
            name_english TEXT NOT NULL,
            name_arabic TEXT NOT NULL,
            hijri_month INTEGER NOT NULL,
            hijri_day INTEGER NOT NULL,
            event_type TEXT NOT NULL,
            description TEXT NOT NULL,
            is_holiday INTEGER NOT NULL DEFAULT 0
        )
    ''')
    
    # Tasbih Presets
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS tasbih_presets (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            arabic TEXT NOT NULL,
            transliteration TEXT NOT NULL,
            translation TEXT NOT NULL,
            target_count INTEGER NOT NULL DEFAULT 33,
            is_custom INTEGER NOT NULL DEFAULT 0,
            display_order INTEGER NOT NULL DEFAULT 0
        )
    ''')
    
    # Create indices
    cursor.execute('CREATE INDEX IF NOT EXISTS idx_ayahs_surah ON ayahs(surah_id)')
    cursor.execute('CREATE INDEX IF NOT EXISTS idx_ayahs_juz ON ayahs(juz)')
    cursor.execute('CREATE INDEX IF NOT EXISTS idx_translations_ayah ON translations(ayah_id)')
    cursor.execute('CREATE INDEX IF NOT EXISTS idx_hadiths_book ON hadiths(book_id)')
    cursor.execute('CREATE INDEX IF NOT EXISTS idx_duas_category ON duas(category_id)')
    
    conn.commit()

def load_json(filename):
    """Load JSON file"""
    filepath = JSON_DIR / filename
    if filepath.exists():
        with open(filepath, 'r', encoding='utf-8') as f:
            return json.load(f)
    return []

def populate_database(conn):
    """Populate database from JSON files"""
    cursor = conn.cursor()
    
    # Surahs
    surahs = load_json('surahs.json')
    for s in surahs:
        cursor.execute('''
            INSERT OR REPLACE INTO surahs VALUES (?,?,?,?,?,?,?,?,?)
        ''', (s['id'], s['number'], s['name_arabic'], s['name_english'],
              s['name_transliteration'], s['revelation_type'], s['verses_count'],
              s['order_revealed'], s['start_page']))
    print(f"Inserted {len(surahs)} surahs")
    
    # Ayahs
    ayahs = load_json('ayahs.json')
    for a in ayahs:
        cursor.execute('''
            INSERT OR REPLACE INTO ayahs VALUES (?,?,?,?,?,?,?,?,?,?,?)
        ''', (a['id'], a['surah_id'], a['number_in_surah'], a['number_global'],
              a['text_arabic'], a['text_uthmani'], a['juz'], a['hizb'],
              a['page'], 1 if a.get('sajda') else 0, a.get('sajda_type')))
    print(f"Inserted {len(ayahs)} ayahs")
    
    # Translations
    translations = load_json('translations.json')
    for t in translations:
        cursor.execute('''
            INSERT INTO translations (ayah_id, translator_id, text) VALUES (?,?,?)
        ''', (t['ayah_id'], t['translator_id'], t['text']))
    print(f"Inserted {len(translations)} translations")
    
    # Hadith Books
    books = load_json('hadith_books.json')
    for b in books:
        cursor.execute('''
            INSERT OR REPLACE INTO hadith_books VALUES (?,?,?,?,?,?,?)
        ''', (b['id'], b['name_english'], b['name_arabic'], b['author'],
              b['hadith_count'], b['description'], b['icon']))
    print(f"Inserted {len(books)} hadith books")
    
    # Hadiths (all books)
    total_hadiths = 0
    for book_file in ['hadith_bukhari.json', 'hadith_muslim.json', 'hadith_abudawud.json',
                      'hadith_tirmidhi.json', 'hadith_nasai.json', 'hadith_ibnmajah.json']:
        hadiths = load_json(book_file)
        for h in hadiths:
            cursor.execute('''
                INSERT OR REPLACE INTO hadiths VALUES (?,?,?,?,?,?,?,?,?,?)
            ''', (h['id'], h['book_id'], h['chapter_id'], h['number_in_book'],
                  h['number_in_chapter'], h['text_arabic'], h['text_english'],
                  h['narrator'], h['grade'], h['reference']))
        total_hadiths += len(hadiths)
        print(f"  Inserted {len(hadiths)} hadiths from {book_file}")
    print(f"Total hadiths: {total_hadiths}")
    
    # Dua Categories
    categories = load_json('dua_categories.json')
    for c in categories:
        cursor.execute('''
            INSERT OR REPLACE INTO dua_categories VALUES (?,?,?,?,?,?)
        ''', (c['id'], c['name_english'], c['name_arabic'], c['icon'],
              c['display_order'], c['dua_count']))
    print(f"Inserted {len(categories)} dua categories")
    
    # Duas
    duas = load_json('duas.json')
    for d in duas:
        cursor.execute('''
            INSERT OR REPLACE INTO duas VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
        ''', (d['id'], d['category_id'], d['title_english'], d['title_arabic'],
              d['text_arabic'], d['transliteration'], d['translation'],
              d['source'], d.get('virtue'), d['repeat_count'],
              d.get('audio_file'), d['display_order']))
    print(f"Inserted {len(duas)} duas")
    
    # Islamic Events
    events = load_json('islamic_events.json')
    for e in events:
        cursor.execute('''
            INSERT OR REPLACE INTO islamic_events VALUES (?,?,?,?,?,?,?,?)
        ''', (e['id'], e['name_english'], e['name_arabic'], e['hijri_month'],
              e['hijri_day'], e['event_type'], e['description'],
              1 if e.get('is_holiday') else 0))
    print(f"Inserted {len(events)} events")
    
    # Tasbih Presets
    presets = load_json('tasbih_presets.json')
    for p in presets:
        cursor.execute('''
            INSERT OR REPLACE INTO tasbih_presets VALUES (?,?,?,?,?,?,?,?)
        ''', (p.get('id'), p['name'], p['arabic'], p['transliteration'],
              p['translation'], p['target_count'], 1 if p.get('is_custom') else 0,
              p['display_order']))
    print(f"Inserted {len(presets)} tasbih presets")
    
    conn.commit()

def main():
    print("=" * 60)
    print("Nimaz - Database Generator")
    print("=" * 60)
    
    # Remove existing database
    if OUTPUT_DB.exists():
        OUTPUT_DB.unlink()
    
    conn = sqlite3.connect(OUTPUT_DB)
    
    print("\nCreating tables...")
    create_tables(conn)
    
    print("\nPopulating database...")
    populate_database(conn)
    
    conn.close()
    
    print(f"\nDatabase created: {OUTPUT_DB}")
    print(f"Size: {OUTPUT_DB.stat().st_size / 1024 / 1024:.2f} MB")

if __name__ == "__main__":
    main()
```

---

# 7. Integration with Android App

## Step 1: Place Database in Assets

```
app/src/main/assets/database/nimaz_prepopulated.db
```

## Step 2: Update DatabaseModule.kt

The app's `DatabaseModule.kt` already configured to load from assets:

```kotlin
@Provides
@Singleton
fun provideDatabase(@ApplicationContext context: Context): NimazDatabase {
    return Room.databaseBuilder(
        context,
        NimazDatabase::class.java,
        "nimaz_database"
    )
    .createFromAsset("database/nimaz_prepopulated.db")
    .fallbackToDestructiveMigration()
    .build()
}
```

## Step 3: Verify Data

After app launch, query counts should match:
- Surahs: 114
- Ayahs: 6,236
- Hadith Books: 6
- Hadiths: ~34,000
- Dua Categories: 15
- Duas: 200+
- Events: 18+
- Tasbih Presets: 8+

---

# 8. Claude Code Prompt

Use this prompt to run data generation in parallel:

```
I need you to generate all Islamic content data for the Nimaz Android app.

## Task
Create JSON files matching the exact formats specified in this document, then generate a pre-populated SQLite database.

## Data Sources to Use
1. Quran: Use quran-json GitHub or Tanzil.net
2. Hadith: Use Sunnah.com API (get free key) or hadith-json GitHub
3. Duas: Compile from Hisnul Muslim
4. Events: Use the provided list
5. Tasbih: Use the provided presets

## Output Requirements
1. Generate all JSON files in the exact formats shown
2. Run generate_database.py to create nimaz_prepopulated.db
3. Verify all data counts are correct
4. Output the database file ready for Android assets folder

## Quality Checks
- All Arabic text must be properly encoded UTF-8
- All IDs must be sequential and unique
- Foreign key relationships must be valid
- No null values in required fields

Start by creating the folder structure and downloading Quran data first.
```

---

# Summary

| Data Type | Records | Source | Format |
|-----------|---------|--------|--------|
| Surahs | 114 | Tanzil/quran-json | surahs.json |
| Ayahs | 6,236 | Tanzil/quran-json | ayahs.json |
| Translations | 6,236 | Quran.com API | translations.json |
| Hadith Books | 6 | Manual | hadith_books.json |
| Hadiths | ~34,000 | Sunnah.com/GitHub | hadith_*.json |
| Dua Categories | 15 | Manual | dua_categories.json |
| Duas | 200+ | Hisnul Muslim | duas.json |
| Events | 18+ | Manual | islamic_events.json |
| Tasbih Presets | 8+ | Manual | tasbih_presets.json |

**Final Output**: `nimaz_prepopulated.db` (~15-25 MB)
