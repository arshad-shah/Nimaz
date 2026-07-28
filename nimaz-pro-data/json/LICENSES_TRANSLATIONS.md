# Licenses & attribution — Quran translations

Records the source and licensing for the translation assets in
`nimaz-pro-data/json/translations/`, mirrored into
`app/src/main/assets/quran/translations/`.

Acquired **2026-07-27** by the re-runnable
`nimaz-pro-data/scripts/download_translations.py`. Re-verify at any time with
`python3 download_translations.py --check`, which also fails if this catalogue and the Kotlin
`QuranTranslation` enum drift apart.

## Source

- **API:** Al Quran Cloud — https://alquran.cloud/api (`GET /v1/quran/<edition>`)
- **Upstream corpus:** Al Quran Cloud republishes the **Tanzil.net** translation collection
  (https://tanzil.net/trans/), the same corpus used by most open Qur'an apps.
- **How acquired:** one public HTTP request per edition. No account/authentication was used.
- **Verification at acquisition:** each edition is checked verse-for-verse against the app's
  canonical 6,236-ayah order (`ayahs.json`) and rejected on any misalignment, wrong verse count,
  or empty verse — the shipped format is a positional array, so a silent off-by-one would
  mis-attribute every verse.

## Editions shipped

| App id                | Upstream edition | Translator | Language |
|-----------------------|------------------|------------|----------|
| `sahih_international` | `en.sahih`       | Saheeh International | English |
| `en_yusuf_ali`        | `en.yusufali`    | Abdullah Yusuf Ali | English |
| `en_pickthall`        | `en.pickthall`   | Marmaduke Pickthall | English |
| `en_clear_quran`      | `en.itani`       | Talal Itani (Clear Qur'an) | English |
| `ur_maududi`          | `ur.maududi`     | Abul A'ala Maududi | Urdu |
| `ur_jalandhry`        | `ur.jalandhry`   | Fateh Muhammad Jalandhry | Urdu |
| `id_indonesian`       | `id.indonesian`  | Kementerian Agama | Indonesian |
| `tr_diyanet`          | `tr.diyanet`     | Diyanet İşleri | Turkish |
| `fr_hamidullah`       | `fr.hamidullah`  | Muhammad Hamidullah | French |
| `bn_bengali`          | `bn.bengali`     | Muhiuddin Khan | Bengali |
| `hi_hindi`            | `hi.hindi`       | Farooq Khan & Nadwi | Hindi |
| `es_garcia`           | `es.garcia`      | Isa García | Spanish |
| `ru_kuliev`           | `ru.kuliev`      | Elmir Kuliev | Russian |
| `ms_basmeih`          | `ms.basmeih`     | Abdullah Muhammad Basmeih | Malay |
| `de_bubenheim`        | `de.bubenheim`   | Bubenheim & Elyas | German |

`sahih_international` keeps its unprefixed legacy id because it predates this script and is
already persisted on every existing install — see the note in `QuranTranslation.kt`. **App ids are
frozen:** they are written to `translations.translator_id` and to the user's
`quran_translator_id` preference.

## ✅ License sign-off (human decision — same posture as the mushaf layout data)

Qur'an *translations* are, unlike the Arabic text, **modern copyrighted works**. Tanzil.net
distributes them for non-commercial and app use under its own terms
(https://tanzil.net/docs/tanzil_terms_of_use), which require that the text be used unmodified
and attributed; some individual translations carry additional publisher restrictions. Al Quran
Cloud does not attach per-edition SPDX licenses.

The editions that most warranted a look before shipping:

- **Saheeh International** — published by Al-Muntada Al-Islami; the most commonly restricted of
  the set. Already shipped by this app before this change.
- **Clear Qur'an (Talal Itani)** — generally distributed freely by ClearQuran.com, worth
  confirming for bundled redistribution.
- The remaining editions are long-standing Tanzil corpus entries widely redistributed in
  open-source Qur'an apps.

**Status: signed off.** The project owner (@arshad-shah) confirmed on **2026-07-28** that the material below is approved for shipping in a release build.

The app already shipped Saheeh International before this change, so the question was newly
relevant for the 14 added editions; the sign-off covers all 15.

The notes above on Tanzil's terms and on the individually-restricted editions stand unchanged
and remain the record; only the go/no-go has been resolved.

## Attribution in-app

The translator's name is shown against each translation in the Quran settings picker, sourced
from the `QuranTranslation` catalogue.
