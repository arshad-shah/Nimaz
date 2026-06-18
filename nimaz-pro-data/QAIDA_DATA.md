# Qaida Data (Noorani Qaida content)

This documents the four Qaida JSON files in `json/` and the script that
generates them. It covers sub-issue **A** of the
[Qaida Reader epic (#171)](https://github.com/arshad-shah/Nimaz/issues/171) —
**content & data authoring only**. Loading this data into the prepopulated
Room database is sub-issue **C**; audio production is sub-issue **B**
(see [`QAIDA_AUDIO.md`](QAIDA_AUDIO.md)).

## Files

| File | Rows | What it is |
|------|------|------------|
| `json/qaida_letters.json` | 29 | The letter reference table (28 letters + hamzah). |
| `json/qaida_lessons.json` | 17 | The lesson definitions. |
| `json/qaida_lines.json` | 137 | The printed rows inside each lesson. |
| `json/qaida_cells.json` | 434 | The individual **tappable** tokens within each line. |

Hierarchy:

```
qaida_lessons (17)
   └─ qaida_lines        (rows printed on a lesson page)
        └─ qaida_cells   (tappable tokens: a letter, letter+harakah, syllable, or word)

qaida_letters (29)        (reference table, linked from cells via letter_id)
```

## Regenerating

Everything is derived deterministically — re-running produces byte-identical
files, so the content is reproducible rather than hand-maintained:

```bash
cd nimaz-pro-data
python3 scripts/generate_qaida_data.py
```

The script validates the data before writing (sequential unique ids, valid FK
references line→lesson and cell→line/lesson/letter, unique non-empty
`audio_key` per cell, 29 letters / 17 lessons, null forms for non-connectors)
and exits non-zero on any failure.

Lesson 17 (revision) reads real ayahs for Surah Al-Fatihah (1) and An-Nas (114)
straight out of the existing `json/ayahs.json`.

## Schemas

### `qaida_letters.json`

```json
{
  "id": 2,
  "letter_arabic": "ب",
  "name_arabic": "بَاء",
  "name_transliteration": "baa",
  "isolated_form": "ب",
  "initial_form": "بـ",
  "medial_form": "ـبـ",
  "final_form": "ـب",
  "is_connecting": true,
  "makhraj_area": "SHAFATAIN",
  "makhraj_detail": "The inner part of both lips pressed together.",
  "phonetic_hint": "like 'b' in 'book'",
  "audio_key": "letter_ba",
  "display_order": 2
}
```

- **Positional forms are generated programmatically** from the Arabic joining
  model. The tatweel/kashida (`ـ`, U+0640) marks a connection point, standing
  in for the neighbouring glyph: `initial = letter + ـ`, `medial = ـ + letter + ـ`,
  `final = ـ + letter`.
- The **6 non-connecting letters** `ا د ذ ر ز و` have `initial_form` and
  `medial_form` = `null` and `is_connecting` = `false` (they still join to a
  preceding letter, so `final_form` is present).
- **Hamzah** `ء` never joins in any direction, so all of
  `initial/medial/final_form` are `null`.
- `makhraj_area` is one of the 5 articulation regions
  `JAWF | HALQ | LISAN | SHAFATAIN | KHAYSHUM`; the finer 17-point detail is in
  `makhraj_detail`.

### `qaida_lessons.json`

```json
{
  "id": 1,
  "lesson_number": 1,
  "title_english": "The Letters",
  "title_arabic": "الحُرُوفُ المُفْرَدَة",
  "title_transliteration": "Al-Huroof Al-Mufradah",
  "description": "Learn the 29 Arabic letters and their names in their isolated form.",
  "concept_tags": ["letters", "isolated"],
  "icon": "🔤",
  "display_order": 1
}
```

The 17 lessons follow the canonical Noorani Qaida progression from #171:
letters → joined letters → muqatta'at → harakat → tanween → standing harakat →
madd/leen → sukoon → shadda → integrated rules → comprehensive revision.

### `qaida_lines.json`

```json
{
  "id": 101,
  "lesson_id": 1,
  "line_number": 1,
  "line_type": "PRACTICE",
  "instruction_english": null,
  "instruction_arabic": null,
  "display_order": 1
}
```

- `line_type` ∈ `HEADING | EXAMPLE | PRACTICE | EXERCISE`.
- `HEADING` lines are labels (instruction text, no cells); every other line
  type carries at least one cell.

### `qaida_cells.json`

```json
{
  "id": 1069,
  "line_id": 117,
  "lesson_id": 4,
  "position": 1,
  "text_arabic": "بَ",
  "transliteration": "ba",
  "token_type": "HARAKAH",
  "audio_key": "l4_ba_a",
  "highlight_group": "fatha",
  "letter_id": 2,
  "notes": null
}
```

- `token_type` ∈ `LETTER | HARAKAH | TANWEEN | MADD | LEEN | SUKOON | SHADDA | MUQATTAAT | SYLLABLE | WORD`.
- `audio_key` is **unique across all cells** and stable. Naming convention:
  `letter_<slug>` for the per-letter reference sound, and `l<lesson>_<token>`
  for lesson tokens (a numeric suffix is appended only to break collisions).
  The actual audio clips are produced in sub-issue B.
- `highlight_group` lets the UI colour harakat/rules consistently, aligned with
  the colour philosophy in
  `app/src/main/java/com/arshadshah/nimaz/core/util/TajweedParser.kt`. Values:
  `fatha`, `kasra`, `damma`, `tanween_fath`, `tanween_kasr`, `tanween_damm`,
  `standing_fatha`, `standing_kasra`, `inverted_damma`, `madd`, `leen`,
  `sukoon`, `shadda`, `muqattaat`, `word`, or `null` for plain glyphs.
- `letter_id` links a cell back to `qaida_letters` (null for multi-letter
  syllables/words).

## Lesson content notes

- **L1–L2** are driven directly off `qaida_letters.json` (isolated letters,
  then doubled letters showing how shapes change when joined).
- **L3** holds the disjoined letters that open surahs
  (الٓمٓ، الٓمٓصٓ، الٓرٰ، الٓمٓرٰ، كٓهٰيٰعٓصٓ، طٰهٰ، طٰسٓمٓ، طٰسٓ، يٰسٓ، صٓ، حٰمٓ، عٓسٓقٓ، قٓ، نٓ).
- **L4–L5** apply every letter × {fatha, kasra, damma} then × tanween.
- **L6** mixes harakat and tanween in two-letter drills.
- **L7** covers the standing/superscript harakat (dagger alif, standing kasra,
  inverted damma) with classic example words (هَٰذَا، ذَٰلِكَ، الرَّحْمَٰن).
- **L8–L9** cover madd (بَا، بُو، بِي) and leen (بَوْ، بَيْ), then mixed drills.
- **L10–L16** build up sukoon, shadda, and their combinations — including the
  classic نَزَّلَ vs نَزَلَ contrast and words with two shaddas.
- **L17** is real-word revision: Surah Al-Fatihah and An-Nas, tokenised into
  per-word tappable cells, pulled from `json/ayahs.json`.

## Sources

Curriculum / lesson mapping:
- Bayan al-Quran Academy — Noorani Qaidah lessons 1–16:
  https://bayanulquran-academy.com/noorani-qaidah-lessons-from-1-16/
- Islamic Quran Center — full lesson index:
  https://islamicqurancenter.com/tutorial/noorani-qaida/noorani-qaida-table-content-index/
- Knowledge Quran — lesson overview: https://www.knowledgequran.com/noorani-qaida-lessons/

Makharij (articulation points) for the letter table:
- https://www.qiratulquran.com/17-places-of-articulation/
- https://kalimah-center.com/makharij-of-arabic-letters/

Structure references (verify licensing before reusing any asset):
- `nooresahar/noorani` — https://github.com/nooresahar/noorani
- `Nooraniqaida` org — https://github.com/Nooraniqaida

No third-party assets are copied here; the JSON is authored/derived in this repo.
