# Licenses & attribution — Quran translations

One block per translation the app ships, in the same shape as
[`LICENSES_INDOPAK.md`](LICENSES_INDOPAK.md). Every entry in `manifest.json` → `translations` and
in `QuranEditions.translations` must have a block here before the edition ships.

> **Translations are not like the Arabic text.** The Qur'anic Arabic is the revealed text and is
> in the public domain (Tanzil publishes its text under CC BY 3.0). A *translation* is a
> copyrighted work of its translator or publisher, and the fact that a site serves the text says
> nothing about the right to bundle it. Treat "we can fetch it" and "we can ship it" as two
> separate questions, and answer the second one here before adding the catalogue entry.

## ⚠️ The basis everything below rests on: Nimaz is non-commercial

Tanzil **hosts** these translations; it does not relicense them. Its blanket term is:

> *"The translations provided at this page are for non-commercial purposes only. If used
> otherwise, you need to obtain necessary permission from the translator or the publisher."*
> — <https://tanzil.net/trans/>

Every Tanzil-sourced entry below relies on that term. It holds because **Nimaz is free, with no
advertising and no in-app purchases** (owner confirmation, July 2026). Two consequences:

1. **If Nimaz ever monetises** — ads, a paid tier, a subscription, sponsored placement — every
   entry marked *"Tanzil non-commercial term"* below needs written permission from its translator
   or publisher **before** that release ships. The public-domain entries are unaffected.
2. **Tanzil requires a link back** from any app shipping three or more of its translations, so
   readers can reach the current versions. Nimaz ships eight, and the About screen's *Quran Text*
   and *Translations* credits are both tappable links to <https://tanzil.net/trans/>. Do not
   turn those into plain text.

Tanzil also asks that redistributors not republish its translation *list* elsewhere without
permission — that constrains the pipeline, not the app, and `manifest.json` names only the eight
editions actually shipped.

## How to add a block

1. Fetch the edition (`manifest.json` → run the pipeline for it).
2. Copy the template below, fill in every field, and resolve the sign-off line.
3. Only then add the `TranslationEdition` to `QuranEditions.translations` and its
   `QuranContentAssets` binding — see `docs/quran/content-registry.md` §2.

```markdown
## `<translator_id>` — <Translation name>

- **Translator / rights holder:**
- **Source:**                     (API or dataset, with URL)
- **Edition identifier:**         (e.g. alquran.cloud `en.sahih`)
- **Version / export date:**
- **License:**                    (SPDX id, or the exact permission relied on)
- **Redistribution for:** offline, open-source Android app, bundled as a shipped asset.
- **Sign-off:**                   (who confirmed, when, and how — or ⚠️ OPEN)
- **Notes:**
```

---

## `sahih_international` — Sahih International

- **Translator / rights holder:** Saheeh International (Abul-Qasim Publishing House).
- **Source:** alquran.cloud API — `https://api.alquran.cloud/v1/quran/en.sahih`, fetched by
  `nimaz-pro-data/scripts/download_and_generate.py` (`download_quran_data`).
- **Edition identifier:** `en.sahih`
- **Version / export date:** as served when `translations.json` was generated; predates this
  register.
- **License:** Tanzil non-commercial term (see above). Tanzil serves the same edition as
  `en.sahih`; the rows currently on devices came via alquran.cloud, which publishes no
  redistribution terms at all, so the Tanzil term is the stated basis relied on.
- **Redistribution for:** offline, open-source Android app — currently bundled **inside the
  prepopulated DB asset** (`assets/database/nimaz_prepopulated.db`) rather than as a seeded JSON
  asset, because it predates the content registry.
- **Sign-off:** ✅ Under the non-commercial basis (owner decision, July 2026). Saheeh
  International remains a copyrighted publisher work, so this entry is among those that would
  need written permission if Nimaz ever monetises.
- **Notes:** 6,236 rows in `translations.json`, keyed `translator_id = "sahih_international"`.
  That id is persisted in user preferences and in `translations.translator_id`, so it cannot be
  renamed — see `docs/quran/content-registry.md` §3.

---

## `pickthall` — Pickthall

- **Translator / rights holder:** Mohammed Marmaduke William Pickthall (d. 1936). **Public domain.**
- **Source:** Tanzil — <https://tanzil.net/trans/en.pickthall>
- **Edition identifier:** `en.pickthall`
- **Version / export date:** Tanzil "Last Update: September 4, 2010"; fetched 2026-07-27.
- **License:** **Public domain by age.** Pickthall died in 1936, so copyright expired in 2007 in
  life+70 jurisdictions, and the 1930 publication is out of its 95-year US term as of 2025.
  Tanzil's non-commercial term is therefore not the operative constraint here.
- **Redistribution for:** offline, open-source Android app, bundled as a seeded asset.
- **Sign-off:** ✅ Clear — no permission needed, and unaffected if Nimaz ever monetises.
- **Notes:** 6,236 verses, `quran/translations/pickthall.json`.

## `jalandhry` — جالندہری (Urdu)

- **Translator / rights holder:** Fateh Muhammad Jalandhry (d. 1954).
- **Source:** Tanzil — <https://tanzil.net/trans/ur.jalandhry>
- **Edition identifier:** `ur.jalandhry`
- **Version / export date:** Tanzil "Last Update: December 24, 2010"; fetched 2026-07-27.
- **License:** Tanzil non-commercial term (see above). Likely also public domain by age in
  life+70 jurisdictions (d. 1954 → 2024), but that has not been independently confirmed, so the
  non-commercial basis is what is relied on.
- **Redistribution for:** offline, open-source Android app, bundled as a seeded asset.
- **Sign-off:** ✅ Under the non-commercial basis.
- **Notes:** Right-to-left; rendered in the bundled IndoPak Nastaʿlīq face (`fontId = "indopak"`),
  since the Latin body font has no Urdu coverage.

## `diyanet` — Diyanet İşleri (Turkish)

- **Translator / rights holder:** Diyanet İşleri Başkanlığı (Turkish Presidency of Religious
  Affairs). Tanzil's own metadata credits "Diyanet Isleri".
- **Source:** Tanzil — <https://tanzil.net/trans/tr.diyanet>
- **Edition identifier:** `tr.diyanet`
- **Version / export date:** Tanzil "Last Update: December 27, 2011"; fetched 2026-07-27.
- **License:** Tanzil non-commercial term (see above).
- **Redistribution for:** offline, open-source Android app, bundled as a seeded asset.
- **Sign-off:** ✅ Under the non-commercial basis.

## `indonesian` — Bahasa Indonesia

- **Translator / rights holder:** Kementerian Agama Republik Indonesia (Indonesian Ministry of
  Religious Affairs).
- **Source:** Tanzil — <https://tanzil.net/trans/id.indonesian>
- **Edition identifier:** `id.indonesian`
- **Version / export date:** Tanzil "Last Update: June 4, 2010"; fetched 2026-07-27.
- **License:** Tanzil non-commercial term (see above).
- **Redistribution for:** offline, open-source Android app, bundled as a seeded asset.
- **Sign-off:** ✅ Under the non-commercial basis.

## `basmeih` — Bahasa Melayu

- **Translator / rights holder:** Abdullah Muhammad Basmeih.
- **Source:** Tanzil — <https://tanzil.net/trans/ms.basmeih>
- **Edition identifier:** `ms.basmeih`
- **Version / export date:** Tanzil "Last Update: September 7, 2012"; fetched 2026-07-27.
- **License:** Tanzil non-commercial term (see above).
- **Redistribution for:** offline, open-source Android app, bundled as a seeded asset.
- **Sign-off:** ✅ Under the non-commercial basis.

## `hamidullah` — Hamidullah (French)

- **Translator / rights holder:** Muhammad Hamidullah (d. 2002).
- **Source:** Tanzil — <https://tanzil.net/trans/fr.hamidullah>
- **Edition identifier:** `fr.hamidullah`
- **Version / export date:** Tanzil "Last Update: July 18, 2011"; fetched 2026-07-27.
- **License:** Tanzil non-commercial term (see above). Still in copyright (d. 2002), so the
  non-commercial basis is load-bearing for this one.
- **Redistribution for:** offline, open-source Android app, bundled as a seeded asset.
- **Sign-off:** ✅ Under the non-commercial basis.

## `bubenheim` — Bubenheim & Elyas (German)

- **Translator / rights holder:** Frank Bubenheim and Nadeem Elyas. Tanzil's metadata credits
  "A. S. F. Bubenheim and N. Elyas".
- **Source:** Tanzil — <https://tanzil.net/trans/de.bubenheim>
- **Edition identifier:** `de.bubenheim`
- **Version / export date:** Tanzil "Last Update: July 17, 2011"; fetched 2026-07-27.
- **License:** Tanzil non-commercial term (see above). Living authors, so the non-commercial
  basis is load-bearing for this one.
- **Redistribution for:** offline, open-source Android app, bundled as a seeded asset.
- **Sign-off:** ✅ Under the non-commercial basis.
