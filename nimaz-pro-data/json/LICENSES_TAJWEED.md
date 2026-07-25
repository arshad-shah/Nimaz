# Licenses & attribution — tajweed colour-coding data

Records the source, acquisition method, and licensing for the two tajweed
datasets shipped in the app's `ayahs.text_tajweed` column (tajweed overhaul,
issue #287). See `docs/nimaz-pro-data-guide.md` for the pipeline that turns
these into the stored spans.

## `tajweed.json` (quran.com uthmani_tajweed)

- **Source:** Quran.com API v4 — the **`uthmani_tajweed`** verse text, which wraps
  the Uthmani Qur'anic text in `<tajweed class="…">…</tajweed>` spans.
- **URL / endpoint:** `https://api.quran.com/api/v4/quran/verses/uthmani_tajweed`
- **How acquired:** downloaded by `nimaz-pro-data/scripts/fetch_tajweed.py`
  (re-runnable; the endpoint and per-ayah keying are documented there). No
  account/authentication is used — the endpoint is public.
- **Role:** provides the **rule span boundaries** (which characters carry which
  rule). The rule taxonomy is then corrected/split against the cpfair dataset
  below (issue #289) and normalised onto the canonical `text_arabic` (issue #290).
- **License:**
  - The **Arabic Qur'anic text** is the revealed text and is in the **public
    domain**.
  - The **tajweed annotation layer** and the API are published by the
    **Quran.com Foundation** (Quranic Association). Quran.com publishes these
    resources for reuse by Qur'an applications; the platform code is open source
    (https://github.com/quran/quran.com-api). Quran.com does not stamp a
    per-endpoint SPDX license on the `uthmani_tajweed` text.
- **Redistribution for:** offline, open-source Android app, bundled (after
  pre-parsing) into the shipped `nimaz_prepopulated.db`.

## `tajweed_cpfair.json` (cpfair / quran-tajweed)

- **Source:** the **cpfair `quran-tajweed`** project — a rule-derived tajweed
  annotation of the Qur'an. Used as the **independent second source** that
  cross-validates and refines the quran.com taxonomy (splitting Madd Jaiz
  Munfasil vs Madd Wajib Muttasil, re-labelling Madd 'Aarid; issue #289).
- **URL:** https://github.com/cpfair/quran-tajweed
- **How acquired:** the project's generated per-ayah rule annotations
  (`{surah, ayah, annotations:[{start, end, rule}]}`), offsets into the Tanzil
  Uthmani text. Committed to the repo as `tajweed_cpfair.json`.
- **Role:** **classification only** — its per-ayah *ordering* of madd rules is
  used to split the merged quran.com class (see `reclassify_madd_obligatory` in
  `preparse_tajweed.py`). Its character offsets are **not** used directly, because
  it indexes a different ayah segmentation (e.g. it prepends the basmala to ayah 1
  of each surah).
- **License:**
  - The underlying **Tanzil Qur'anic text** is public domain / distributed under
    the **Tanzil terms** (https://tanzil.net/docs/text_license) — non-commercial
    or with permission, unmodified text, attribution to Tanzil.
  - The **rule-derivation code and generated annotations** are published by the
    cpfair `quran-tajweed` project on GitHub. **Confirm the repository's current
    LICENSE file before a commercial release** — this file records provenance,
    not a warranty of terms.
- **Redistribution for:** offline, open-source Android app; the file is used at
  build time for classification and is not itself shipped in the DB.

## Notes

- Neither dataset's *rule spans* are shipped verbatim: the pipeline re-derives
  the coloured segments over the canonical `text_arabic` (issue #290) so the
  stored `text_tajweed` round-trips to the app's own Qur'anic text.
- Beat/count values used in the in-app rule names follow the **Hafs 'an 'Asim**
  reading; scholarly reference in `TajweedParser` KDoc (Kareema Czerepinski,
  *Tajweed Rules of the Qur'an*; al-Jazari, *al-Muqaddimah al-Jazariyyah*).
