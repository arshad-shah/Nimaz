# Licenses & attribution — 16-line IndoPak Quran data

Records the exact source, version, and licensing for `ayahs_indopak.json` and
`mushaf_layout_indopak16.json` (sub-task 1/7 of #263, tracked in issue #265).

Data acquired **2026-07-25** by the automated agent working #265, using
`nimaz-pro-data/scripts/download_indopak_mushaf_data.py` (re-runnable; the source and
normalisation are fully described there and in `docs/nimaz-pro-data-guide.md`).

## `ayahs_indopak.json` (IndoPak ayah text)

- **Source:** Quranic Universal Library (QUL) by Tarteel, Inc. — resource **"Indopak 16 lines
  layout (Taj company)"**, resource id 11.
- **URL:** https://qul.tarteel.ai/resources/mushaf-layout/11
- **How acquired:** parsed from QUL's public per-page preview (pages 1–548), which renders every
  word with its `surah:ayah:word` location and IndoPak glyph text. No account/authentication was
  used; only the public preview pages were read.
- **Version / export date:** QUL resource 11 as served on 2026-07-25.
- **License:**
  - The **Arabic Qur'anic text** itself is the revealed text and is in the **public domain**.
  - The **QUL platform** (code + tooling) is published by Tarteel, Inc. under the **MIT License**
    (https://github.com/TarteelAI/quranic-universal-library/blob/main/LICENSE). QUL exists to make
    Qur'anic resources reusable by Qur'an apps (Quran for Android/iOS, Tarteel and others build on
    QUL data). QUL does not stamp a per-resource SPDX license on this resource page.
- **Redistribution for:** offline, open-source Android app, bundled as a shipped asset.
- **Notes:** Each ayah is stored as `text_indopak` plus a `words[]` array indexed 1:1 with the
  layout's word positions (see schema in `docs/nimaz-pro-data-guide.md`). The final word of most
  ayahs is QUL's end-of-ayah marker glyph, which — like the IndoPak diacritics — renders correctly
  only with a matching IndoPak font (font sourcing is a later sub-task of #263). Ids reconcile 1:1
  with the existing 6,236-ayah space in `ayahs.json` / `AyahEntity`.

## `mushaf_layout_indopak16.json` (16-line page/line layout map)

- **Source:** same QUL resource 11, "Indopak 16 lines layout (Taj company)" (548 pages, ≤16 lines
  per page). https://qul.tarteel.ai/resources/mushaf-layout/11
- **How acquired / version:** as above (public preview, 2026-07-25).
- **License:** as above. This file is **QUL's own structured line-break data** (per-word page and
  line positions) — it is **not** a scan, image, or transcription of Taj Company's typeset page
  artwork. "Taj company" names the 548-page 16-line *layout standard* the data follows (the
  edition Ḥuffāẓ memorise from); QUL's structured line-break determination is the redistributable
  artefact, and is the "safer path" the issue itself points to over copying a commercial edition's
  typesetting.

## ⚠️ License sign-off (human decision — issue #265 explicitly scoped this)

The data above is sourced from QUL, the reputable open library the issue recommends, and the
underlying Qur'anic text is public domain. QUL does **not** publish a single explicit SPDX license
covering redistribution of this specific bulk dataset inside a shipped app, so the final
go/no-go for bundling it belongs to the project owner. Before an app release that ships these
files, confirm with QUL/Tarteel (Discord / GitHub) that bulk redistribution of resource 11 in an
open-source app is permitted, and record the outcome here. The data files can stay in the repo for
development regardless; this note gates the *shipping* decision, not the acquisition.

## Validation performed (all passing — see the acquisition script's validators)

- [x] All 114 surahs / 6,236 ayahs present in `ayahs_indopak.json`; ids match `AyahEntity.id`
      (no missing, no extra, no empty text, no empty word tokens).
- [x] All 548 pages present in `mushaf_layout_indopak16.json`, each with ≤16 lines.
- [x] 114 surah-header lines (one per surah); 112 basmalah lines (every surah except Al-Fatiha,
      where the basmalah is ayah 1, and At-Tawbah, which has none) — no gaps, no duplicates.
- [x] Every ayah's word positions are exactly covered by its layout line-segments; `text_indopak`
      splits 1:1 into `words[]`.
- [x] End-to-end: all 548 pages reconstruct word-for-word from `words[]` + the layout map.
