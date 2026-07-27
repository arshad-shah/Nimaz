# Licenses & attribution — Quran translations

One block per translation the app ships, in the same shape as
[`LICENSES_INDOPAK.md`](LICENSES_INDOPAK.md). Every entry in `manifest.json` → `translations` and
in `QuranEditions.translations` must have a block here before the edition ships.

> **Translations are not like the Arabic text.** The Qur'anic Arabic is the revealed text and is
> in the public domain. A *translation* is a copyrighted work of its translator or publisher, and
> most of the well-known English ones are **not** freely redistributable inside an app — the fact
> that an API serves the text says nothing about the right to bundle it. Treat "we can fetch it"
> and "we can ship it" as two separate questions, and answer the second one here before adding
> the catalogue entry.

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
- **License:** ⚠️ **not established.** alquran.cloud serves the text but does not publish a
  redistribution licence for this edition, and Saheeh International is a copyrighted translation.
- **Redistribution for:** offline, open-source Android app — currently bundled **inside the
  prepopulated DB asset** (`assets/database/nimaz_prepopulated.db`) rather than as a seeded JSON
  asset, because it predates the content registry.
- **Sign-off:** ⚠️ **OPEN — owner decision.** This edition ships today; the block records that
  its licensing was never established, not that it was cleared. Same class of open question as
  the QUL sign-off in `LICENSES_INDOPAK.md`, and it is **not** for an agent to close. Before the
  next release, either obtain written permission from the rights holder, or evaluate replacing it
  with a translation whose licence is explicit.
- **Notes:** 6,236 rows in `translations.json`, keyed `translator_id = "sahih_international"`.
  That id is persisted in user preferences and in `translations.translator_id`, so it cannot be
  renamed — see `docs/quran/content-registry.md` §3.

---

## Candidate editions with clearer licensing

Recorded here so the next translation added starts from a licence rather than an API endpoint.
None of these is shipped or vetted yet; the sign-off line above still applies to each.

| Edition | Rights position |
|---|---|
| Tanzil-sourced translations | Tanzil publishes per-translation terms; several are marked freely redistributable with attribution. Check the specific translation, not Tanzil as a whole. |
| Public-domain translations (e.g. Rodwell, Sale) | Copyright expired. Dated language, but unambiguous to ship. |
| Quran.com / QUL translation resources | Same platform as the IndoPak layout, and subject to the same open sign-off question in `LICENSES_INDOPAK.md`. |
