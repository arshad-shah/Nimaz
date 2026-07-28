# Licenses & attribution — line-accurate Mushaf layout data

Records the exact source, version, and licensing for every line-accurate Mushaf edition the
app ships, i.e. the files in `nimaz-pro-data/json/mushaf/` mirrored into
`app/src/main/assets/quran/mushaf/`.

All of it is acquired by the re-runnable `nimaz-pro-data/scripts/download_mushaf_layout.py`
(the source and normalisation are fully described there and in `docs/nimaz-pro-data-guide.md`).

| Script key   | QUL resource                                  | Pages | Lines | Text source  |
|--------------|-----------------------------------------------|-------|-------|--------------|
| `INDOPAK_16` | 11 — "Indopak 16 lines layout (Taj company)"   | 548   | 16    | `INDOPAK`    |
| `INDOPAK_15` | 12 — "Indopak 15 lines layout (Qudratullah)"   | 610   | 15    | `INDOPAK`    |
| `INDOPAK_13` | 313 — "Indopak 13 lines layout (Taj company)"  | 847   | 13    | `INDOPAK_13` |

Files emitted: `<text_source>_text.json` (glyph text, one per **text source**) and
`<script>_layout.json` (page/line map, one per **edition**).

- `INDOPAK_16` originally acquired **2026-07-25** (issue #265, then named `ayahs_indopak.json` +
  `mushaf_layout_indopak16.json`). Re-generated **2026-07-27** under the generalised naming and
  verified byte-for-byte identical to the originals.
- `INDOPAK_15` and `INDOPAK_13` acquired **2026-07-27**.

## Source and license (applies to all three)

- **Source:** Quranic Universal Library (QUL) by Tarteel, Inc. — https://qul.tarteel.ai
- **How acquired:** parsed from QUL's public per-page preview, which renders every word with its
  `surah:ayah:word` location and glyph text. No account/authentication was used; only the public
  preview pages were read.
- **License:**
  - The **Arabic Qur'anic text** itself is the revealed text and is in the **public domain**.
  - The **QUL platform** (code + tooling) is published by Tarteel, Inc. under the **MIT License**
    (https://github.com/TarteelAI/quranic-universal-library/blob/main/LICENSE). QUL exists to make
    Qur'anic resources reusable by Qur'an apps (Quran for Android/iOS, Tarteel and others build on
    QUL data). QUL does not stamp a per-resource SPDX license on these resource pages.
- **Redistribution for:** offline, open-source Android app, bundled as shipped assets.
- **Nature of the layout data:** these files are **QUL's own structured line-break data**
  (per-word page and line positions) — **not** a scan, image, or transcription of any publisher's
  typeset page artwork. "Taj company" / "Qudratullah" name the *layout standards* the data follows
  (the editions Ḥuffāẓ memorise from); QUL's structured line-break determination is the
  redistributable artefact, and is the "safer path" issue #265 itself points to over copying a
  commercial edition's typesetting.
- **Notes:** the final word of most ayahs is QUL's end-of-ayah marker glyph, which — like the
  IndoPak diacritics — renders correctly only with a matching IndoPak font. Ids reconcile 1:1 with
  the existing 6,236-ayah space in `ayahs.json` / `AyahEntity`.

## Text sources

`INDOPAK_16` and `INDOPAK_15` were verified to ship **byte-identical glyph text for all 6,236
ayahs**, so they share the single `INDOPAK` text source and the 15-line edition costs only its
layout file. `INDOPAK_13` differs from them in the vowel marks of **28 ayahs** (fatha vs shadda,
superscript alef vs fatha, and similar), so it carries its own `INDOPAK_13` text source rather
than silently rendering 28 ayahs with another print's orthography. The generator enforces this:
`write_outputs` refuses to reuse a text source whose glyphs do not match exactly, and
`MushafLayoutFidelityTest` re-checks it on the shipped bytes.

## ✅ License sign-off (human decision — issue #265 explicitly scoped this; it now covers three editions)

The data above is sourced from QUL, the reputable open library issue #265 recommends, and the
underlying Qur'anic text is public domain. QUL does **not** publish a single explicit SPDX license
covering redistribution of these specific bulk datasets inside a shipped app, so the final
go/no-go for bundling them belonged to the project owner — that is what this section records.

**Status: signed off.** The project owner (@arshad-shah) confirmed on **2026-07-28** that the material below is approved for shipping in a release build.

Adding `INDOPAK_15` and `INDOPAK_13` did not change the nature of the question — it widened it
from one QUL resource to three from the same library under the same terms, and the sign-off
covers all three (resources **11, 12 and 313**).

The description above of *what* the data is and where it came from stands unchanged and remains
the record; only the go/no-go has been resolved.

## Validation performed (all passing — see the acquisition script's validators and `MushafLayoutFidelityTest`)

Enforced for **every** edition automatically, rather than per-edition by hand:

- [x] All 114 surahs / 6,236 ayahs present in the text source; ids match `AyahEntity.id`
      (no missing, no extra, no empty text, no empty word tokens).
- [x] All pages present and contiguous, each within the edition's line count — and the page count
      matches what `MushafScript` declares, so catalogue and data prove each other.
- [x] 114 surah-header lines (one per surah); 112 basmalah lines (every surah except Al-Fatiha,
      where the basmalah is ayah 1, and At-Tawbah, which has none) — no gaps, no duplicates.
- [x] Every ayah's word positions are exactly covered by its layout line-segments; the glyph text
      splits 1:1 into `words[]`.
- [x] Editions sharing a text source ship identical glyphs.
- [x] End-to-end: every page reconstructs word-for-word from `words[]` + the layout map.
