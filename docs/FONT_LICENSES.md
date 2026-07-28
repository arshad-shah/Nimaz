# Bundled font licenses & attribution

Every `.ttf` shipped in `app/src/main/res/font/` and its licence. Keep this in
sync when adding or removing a font (the font picker is driven by
`QuranArabicFont` in `presentation/theme/Type.kt`).

| File | Family | Used for | Licence |
|------|--------|----------|---------|
| `outfit_variable.ttf` | Outfit | UI display / headings | SIL Open Font License 1.1 |
| `plus_jakarta_sans_variable.ttf` | Plus Jakarta Sans | UI body / labels | SIL Open Font License 1.1 |
| `amiri_regular.ttf`, `amiri_bold.ttf` | Amiri | Arabic (Quran/Hadith/Dua), default | SIL Open Font License 1.1 |
| `scheherazade_new_regular.ttf`, `scheherazade_new_bold.ttf` | Scheherazade New | Arabic (selectable) | SIL Open Font License 1.1 |
| `indopak_nastaleeq.ttf` | AlQuran IndoPak by QuranWBW | 16-line IndoPak Quran (selectable) | QuranWBW — Sadaqa-e-Jaria terms — **see below** |

---

## `indopak_nastaleeq.ttf` — AlQuran IndoPak by QuranWBW (issue #267, part of #263)

- **Family / version:** `AlQuran IndoPak by QuranWBW`, Version 2.100 (2022-11-26).
- **Why this exact font:** it is the companion face for the QUL *"Indopak 16
  lines layout (Taj company)"* text/layout bundled in
  `app/src/main/assets/quran/mushaf/indopak_text.json` +
  `indopak_16_layout.json` (sourced in sub-task 1/7, issue #265). The
  source text embeds each ayah's decorative numbered end-marker as **Private
  Use Area glyphs (U+F500…U+F6FF)**; verified that this font covers **all 346
  PUA codepoints (346/346)** used in the data plus the full IndoPak Arabic
  letterforms and diacritics. Any other Arabic font (Amiri, Scheherazade) would
  render the ayah numbers as tofu. This is the font QuranWBW.com / Quran.com use
  for their IndoPak script, so what renders in-app is pixel-faithful to the
  source.
- **Source (unmodified):** QUL / Tarteel CDN,
  `https://static-cdn.tarteel.ai/qul/fonts/nastaleeq/Hanafi/normal-v4.2.2/with-waqf-lazmi/font.ttf`
  (the Hanafi-waqf variant — its PUA marker set matches resource 11 exactly).
  Origin per the font's own credits: based on *Al Qalam Quran Majeed* /
  *Al Qalam Quran Majeed Web*; ayah-number glyphs from the KFGQPC Nastaleeq
  font; assembled by Ayman Siddiqui for QuranWBW.com.
- **Embedding bit:** OS/2 `fsType = 0` → Installable Embedding permitted.
- **APK impact:** 310 KB uncompressed / ≈0.15–0.2 MB in the compressed APK.
  Single Regular weight (the file has no bold; Compose synthesises bold if ever
  requested — the Quran reader uses Normal weight only).
- **Ships unmodified:** the font's terms forbid modification, so it is **not**
  subset or re-hinted — the file is the byte-for-byte upstream `font.ttf`.

### Licence text (from the font's `name` table, verbatim)

> This font AlQuran-IndoPak-by-QuranWBW is made by Ayman Siddiqui and is based on
> Al Qalam Quran Majeed and Al Qalam Quran Majeed Web fonts, specially adjusted
> and edited to work with Special Naskh Nastaleeq (IndoPak) Quran Text of
> www.QuranWBW.com. The Ayah Numbers have been made using glyps of Nastaleeq
> Font by King Fahad Glorious Quran Printing Complex (KFGQPC), and other unknown
> contributors may also have made efforts either directly or indirectly.
>
> [Made only for Sadaqa-e-Jaria purposes. No Copyright infringement intended.]
>
> NOT FOR SALE, NOT FOR MODIFICATION, NOT FOR DISTRIBUTION OR NOT FOR
> DEVELOPMENT WITHOUT WRITTEN NOTICE BY QURANWBW.COM
>
> E-mail: quranwbw@gmail.com
>
> Copyright: © Al Qalam © Ghandhara © KFGQPC © Ayman Siddiqui © Credits: Abdul
> Majeed Khan, Arif Karim, Shakir-ul-Qadree, Jawad

### ⚠️ Shipping sign-off (human decision — mirrors the 1/7 data note)

Nimaz is a **free, non-commercial, open-source** app, i.e. exactly the
Sadaqa-e-Jaria (charitable, not-for-sale) use this font was released for, and
the font is bundled **unmodified**. However, the font's own terms ask for
**written notice from QuranWBW.com before distribution**. As with the QUL
IndoPak *data* (see `nimaz-pro-data/json/LICENSES_MUSHAF_LAYOUTS.md`), the final
go/no-go for a public release belongs to the project owner:

- Before an app-store / F-Droid release that ships `indopak_nastaleeq.ttf`,
  email **quranwbw@gmail.com** to notify/confirm redistribution in a free
  open-source app, and record the outcome here.
- The file may stay in the repo for development regardless; this note gates the
  *release* decision, not the integration.

If sign-off is declined, the picker entry can be repointed to an OFL fallback in
one line in `Type.kt` (e.g. `ScheherazadeFontFamily`) — the ayah-number
ornaments would then not render, but the IndoPak orthography still would.

## Noto Nastaliq Urdu (`res/font/noto_nastaliq_urdu.ttf`)

- **Used for:** Urdu **translation** prose only (the Quran's Arabic keeps the selected
  `QuranArabicFont`). Selected via `translationFontFamily(TranslationLanguage.URDU)` in
  `presentation/theme/Type.kt`.
- **Why bundled:** the app's body faces (Outfit / Plus Jakarta Sans) contain no Arabic-script
  glyphs, so an Urdu translation otherwise falls back to whatever the device provides —
  usually Naskh, which is the wrong script convention for Urdu.
- **Source:** Google Fonts — `ofl/notonastaliqurdu/NotoNastaliqUrdu[wght].ttf`, fetched from the
  google/fonts repository. Variable weight axis, shipped unmodified; loaded at its default
  Regular instance (variable fonts need API 26, the app's minSdk is 29).
- **Licence:** SIL Open Font License 1.1, as published with the family in google/fonts.
- **Release sign-off:** OFL permits bundling and redistribution in an application; no further
  action expected. Confirm the OFL text ships with the app's licence list if that list is
  intended to enumerate bundled fonts.

