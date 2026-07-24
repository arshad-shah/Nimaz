# Licenses & attribution — 16-line IndoPak Quran data

Tracks the exact source, version, and license for `ayahs_indopak.json` and
`mushaf_layout_indopak16.json` (sub-task 1/7 of #263, tracked in issue #265).

> **Template only.** No data has been acquired yet in this pass (no outbound network access was
> available — see the issue/PR discussion). Every field below must be filled in with the *real*
> values from the source actually used, verified against that source's current terms at the time
> of acquisition — do not assume the notes in `docs/nimaz-pro-data-guide.md` are still accurate by
> then; re-check.

## `ayahs_indopak.json` (IndoPak ayah text)

- **Source:** _TBD — e.g. Quranic Universal Library (QUL) / Tarteel.ai_
- **URL:** _TBD_
- **Version / commit / export date:** _TBD_
- **License:** _TBD — quote or link the exact license/terms text_
- **Redistribution confirmed for:** offline, open-source Android app, bundled as a shipped asset
- **Acquired by / date:** _TBD_
- **Notes:** _TBD — any normalisation applied, diacritic handling, encoding_

## `mushaf_layout_indopak16.json` (16-line page/line layout map)

- **Source:** _TBD — e.g. QUL mushaf-layout export or Quran Foundation `page-layout` API
  (`INDOPAK_16_LINES`)_
- **URL:** _TBD_
- **Version / commit / API response date:** _TBD_
- **License:** _TBD — quote or link the exact license/terms text_
- **Redistribution confirmed for:** offline, open-source Android app, bundled as a shipped asset
  (note: API-sourced data may only license *live* per-request use — confirm bulk bake-in is
  covered before shipping)
- **Acquired by / date:** _TBD_
- **Notes:** _TBD — confirm this is a QUL/Quran Foundation line-break determination, not a
  transcription of a specific commercial edition's typesetting (e.g. Taj Company), which is
  copyrighted and must not be copied_

## Validation performed

- [ ] All 114 surahs / 6,236 ayahs present in `ayahs_indopak.json`, ids match `AyahEntity.id`
- [ ] All 548 pages present in `mushaf_layout_indopak16.json`, each with ≤16 lines
- [ ] Spot-checked against a printed/reference 16-line Mushaf for a sample of pages
