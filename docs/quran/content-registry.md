# Quran content registry

How to add a Quran translation, mushaf layout, tafseer, reciter or Arabic font — and the record
of what the codebase actually looked like when this work started.

Design decisions live in [`docs/adr/`](../adr/): ADR-001 (generic `mushaf_layouts` table),
ADR-002 (catalogue in `domain`, bindings in their own layer), ADR-003 (preference values are
catalogue ids with legacy aliases).

---

## 1. Spec validation (verified against `dev` @ `3d35eba`)

The originating spec was checked claim by claim before any code was written. Most of it held.
What follows is what did **not**, because those corrections changed the design.

### 1.1 Corrections

**Tafseers were already a domain catalogue, not scattered literals.**
The spec listed tafseers as "`tafseer_id` strings scattered as literals (`TafseerPageContent.kt:831`
etc.)". `domain/model/TafseerModels.kt:33` already held `enum class TafseerSource(val id, val
displayName)` — the same good pattern as the font enum. Every literal the spec cited
(`TafseerPageContent.kt:831`, `TafseerNoteCard.kt:108`, `TafseerHighlightableText.kt:374/385`) is
inside an `@Preview` fixture, not production code. Tafseers were folded into the registry for
uniformity, but this was near-zero work rather than a cleanup.

**Which catalogues actually lived in presentation.**
The spec said "three of the five catalogues live in the presentation layer" and counted tafseers
among them while separately marking fonts as "✅ the good pattern". The real count is still three,
but a different three: **fonts, translations and reciters**. Mushaf layouts (`MushafScript`) and
tafseers (`TafseerSource`) were already in `domain`. Fonts arguably belonged in presentation all
along, since a `FontFamily` is a Compose type — hence the split in ADR-002 rather than a move.

**Reciter data was triplicated and had already drifted.** *(the spec missed this entirely)*
Three copies existed:

| Copy | Contents |
|---|---|
| `SelectReciterScreen.popularReciters` | 10 reciters — id, name, location, style |
| `QuranAudioManager.RECITER_CDN_MAP` | 15 keys — id → (CDN id, bitrate) |
| `QuranAudioManager.getReciterDisplayName` | a third copy of the names, as a `when` |

They disagreed. The CDN map carried five reciters (`ayyoub`, `jibreel`, `basfar`, plus the
`alafasy`/`muaiqly` aliases) that the picker never offered, so they were streamable but
unreachable. And Maher Al-Muaiqly rendered as **"Maher Al Muaiqly"** in the picker and **"Maher
Al-Muaiqly"** in the now-playing bar — a visible inconsistency depending on which copy drew it.
This is the strongest single argument for the registry, and it is why ADR-002's pairing test
asserts every reciter has exactly one CDN binding.

**Reciters shipped: 10, not "~6".**

**DAO methods: 6, not 14.** The spec's "every one of the 14 methods gains a `layoutId` parameter"
conflates methods with references. `QuranDao.kt` has **14 references** to
`mushaf_layout_indopak16` / `MushafLayoutIndopak16Entity` (two of them an import and a KDoc link)
across **6 methods**.

**`getAvailableTranslations()` does not exist.** The method is
`QuranRepositoryImpl.getAvailableTranslators(): List<Translator>` (line 222). The spec's substantive
claim is right — it returns `name = translatorId` with the comment *"Use id as name since we don't
have name info"* — only the name was wrong.

**The seeder framework as specified does not fit all five seeders.** See §1.2; this is the largest
correction.

**`NimazListPicker` is not what the settings screen uses.** §9 of the spec said the pickers "become
`NimazListPicker`". The mushaf picker is a `NimazDropdownField` and the translation list is a
bespoke `TranslationItem` row. Both are design-system components already, so swapping the widget
would be a gratuitous UI change on top of a refactor. The registry work changes the **data source**
of these pickers and leaves the widgets alone.

### 1.2 The seeder framework needs a different shape than specified

The spec claims all five seeders "implement the same mutex + content-version + idempotent-replace
shape by hand" and that `AssetContentSeeder<T>` can reimplement them "verbatim in behaviour". They
are not uniform:

| Seeder | Version source | Populated check | Write | `@Volatile seeded` |
|---|---|---|---|---|
| `DuaContentSeeder` | `root.contentVersion` (from JSON) | `categoryCount() > 0` | replace-all | no |
| `HelpContentSeeder` | `root.contentVersion` (from JSON) | `topicCount() > 0` | replace-all | no |
| `QaidaContentSeeder` | `root.contentVersion` (from JSON) | `lessonCount() > 0` | replace-all | no |
| `QuranIndopakSeeder` | `INDOPAK_CONTENT_VERSION` (Kotlin const) | `countMushafLayoutIndopak16() > 0` | replace-all | **yes** |
| `HadithBackfillSeeder` | `root.contentVersion` (from JSON) | `emptyArabicCount() > 0` (**inverted**) | incremental backfill | no |

Three consequences for the design:

1. **`contentVersion` cannot be an abstract `val`.** Three of the five read it from the parsed JSON,
   so they must parse *before* comparing versions. `QuranIndopakSeeder` reads a Kotlin constant and
   deliberately checks the row count *first*, so it can skip parsing a 4 MB asset on every call.
   The framework must support both — version-from-parsed-content and version-from-constant — or it
   silently makes the IndoPak seeder parse 6.3 MB of JSON on every page fetch.
2. **`@Volatile seeded` is not existing behaviour for four of the five.** Adding it is an
   improvement (it is why #280 added it to the IndoPak seeder), but it is a behaviour *change*, not
   a verbatim extraction, and should be described as such.
3. **`HadithBackfillSeeder` does not fit `replace(parsed: T)` at all.** It applies per-row repairs,
   never deletes, and gates on the *presence of gaps* rather than the absence of rows. Forcing it
   into a replace-shaped base class would change what it does. It should stay as it is, or the base
   class needs a narrower contract than "replace".

### 1.3 Confirmed as specified

Line references, table names and sizes all checked out: `Type.kt:61`, `MushafLayout.kt:67`,
`QuranSettingsScreen.kt:93/223/289/333`, `SelectReciterScreen.kt:68`, `AboutScreen.kt:565`,
`QuranAyahItem.kt:284` (translation rendered with no `textDirection` and no per-translation font),
`QuranRepositoryImpl` seeding at lines 165/179. The prepopulated DB is a Git-LFS blob of exactly
**147,292,160 bytes**. `LICENSES_INDOPAK.md:44` carries the open human sign-off gate, unchanged.
`MushafPagination`, the `MushafPageLayout`/`MushafLine`/`MushafWord` models and the line renderers
are genuinely line-count agnostic and were not touched. Room DB version was **18** (so the
`mushaf_layouts` migration is 18 → 19), and `translations` is keyed on `translator_id` with an
index only on `ayah_id`.

---

## 2. How to add X

### A translation

1. `nimaz-pro-data/manifest.json` → add entry; run the fetcher for it.
2. Copy the output to `app/src/main/assets/quran/translations/<id>.json`.
3. `QuranEditions.translations` += one `TranslationEdition`.
4. `QuranContentAssets.translations` += `id → AssetBinding(path, contentVersion)`.
5. Licence block in `nimaz-pro-data/json/LICENSES_TRANSLATIONS.md`.

### A mushaf layout

The same five steps against `QuranEditions.mushafLayouts` / `QuranContentAssets.mushafLayouts` /
`LICENSES_INDOPAK.md`, plus a fidelity sheet in `docs/quran/`.

A layout whose word positions index into an ayah text column the app does not already ship needs
its text asset too — that is what `LayoutAssets.ayahText` is for, and
`QuranEditionRegistryTest` fails the build if it is missing.

### A reciter

1. `QuranEditions.reciters` += one `ReciterEdition`.
2. `QuranContentAssets.reciterAudio` += its CDN id and bitrate.

No asset, no seeder, no migration.

### An Arabic font

1. Drop the `.ttf` into `app/src/main/res/font/`.
2. Declare a `FontFamily` in `presentation/theme/Type.kt`.
3. `QuranEditions.arabicFonts` += one `ArabicFontEdition`.
4. Add the matching `QuranArabicFont` entry (id → `FontFamily`).
5. Record the licence in `docs/FONT_LICENSES.md`.

---

## 3. Ids are permanent

Ids are persisted — in DataStore preferences and in `translations.translator_id` rows. Renaming one
orphans user selections and seeded rows. To rename an edition, change its `displayName`; leave the
`id` alone. Values persisted before the registry existed are carried as `legacyKeys` rather than
migrated (ADR-003).
