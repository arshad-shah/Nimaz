# Help & About Redesign + Data-Driven Help Content System

**Date:** 2026-06-18
**Issue:** #164 — "the about and help page need a redesign"
**Status:** Approved design, ready for implementation planning

## Problem

The Help screen (`HelpSupportScreen.kt`, 408 lines) is a wall of **40 long accordion
items** (22 FAQ + 10 feature guides + 8 troubleshooting) with multi-sentence answers
hard-coded in Kotlin. It is hard to navigate and too wordy. The About screen
(`AboutScreen.kt`, 514 lines) has good content but a cluttered, dated layout.

Beyond visuals, all Help text is hard-coded and **English-only**, while the app
supports 6 languages. We want Help content to be **data-driven** (authored as JSON,
seeded into the DB by the existing regenerator) and **localized** (rendered in the
app's selected language), with a **content-agnostic UI** that renders whatever the
DB contains — adding/editing content never touches UI code.

## Goals

1. Redesign Help for simple, scannable, search-first UX using the app design system.
2. Redesign About: same information, far less clutter.
3. Make Help content data-driven: `help.json` → regenerator → DB → generic UI.
4. Localize Help content to all 6 supported languages with English fallback.
5. Reuse existing conventions exactly (Room + prepopulated asset, MVVM, Hilt, type-safe nav).

## Non-goals

- About does **not** become DB-driven. Its few translatable strings (tagline, section
  labels, button labels) use standard `res/values-<lang>/strings.xml`; links/version/
  developer/credits are app metadata, not localized content.
- No in-app authoring/CMS. Content is authored in JSON and shipped via the regenerated DB.
- No live language switching without restart beyond what the app already does (the app
  restarts on language change); the Help VM still re-resolves reactively from `appLanguage`.

## Approved UX (mockups in `.superpowers/brainstorm/`)

**Help — Direction A (topic grid → drill in):**
- **Help home** (`HelpScreen`): search bar; a 2-column grid of ~6 topic tiles with
  tinted icon boxes (real Material Symbols icons); a teal "Still need help?" contact card.
- **Topic detail** (`HelpTopicDetailScreen`): gradient hero (icon + title + one-liner);
  "Common questions" card with short 1–2 sentence answers (tap to expand);
  "Step-by-step" list of tappable guide cards (`N steps · about M min`).
- **Guide** (`HelpGuideScreen`): numbered **timeline stepper** (reusing the existing
  Today-progress stepper pattern); each step = short instruction + optional tappable
  **path chip** that deep-links into the relevant screen; green "That's it!" footer.

**About — redesign (`AboutScreen` rebuilt):**
- Branded hero (logo tile, name, version chip, one-line tagline — long description trimmed).
- Quick actions row: Rate / Share / Updates.
- Links card (Website, Privacy, Terms, Open-source licenses, Check-for-updates with inline
  "Up to date" badge).
- Compact developer card (avatar + role + social tiles).
- Credits as a 2-column `source · provider` grid (replacing 6 text lines).
- Footer: "Made with ♥ for the Ummah · © year".

## Architecture decisions

- **Localization storage:** single `help_string` side-table keyed by
  `(owner_type, owner_id, field_key, lang_code)`. Adding a 7th language never changes the
  schema; the renderer stays content-agnostic; per-language search is clean.
- **Step deep-links:** path chips navigate now. A small `route_key → Route` resolver maps
  stored `deeplink_route` strings to existing type-safe routes, wired in `NavGraph`.

## Data model

All Help tables are **seeded / read-only** (shipped in the prepopulated DB). No user-data
tables in v1 (bookmarks/"was this helpful?" are out of scope).

**Structural tables (no human text):**

- `help_topic`: `id TEXT PK`, `display_order INT`, `icon_key TEXT`, `color_key TEXT`
- `help_item`: `id TEXT PK`, `topic_id TEXT` (FK→help_topic), `type TEXT` (`QUESTION`|`GUIDE`),
  `display_order INT`, `icon_key TEXT?`, `estimated_minutes INT?`
- `help_step`: `id TEXT PK`, `item_id TEXT` (FK→help_item, guides only), `display_order INT`,
  `deeplink_route TEXT?`, `path_labels TEXT?` (JSON array string, e.g. `["More","Prayer Settings"]`)

**Localized text table:**

- `help_string`: `owner_type TEXT` (`TOPIC`|`ITEM`|`STEP`), `owner_id TEXT`,
  `field_key TEXT` (e.g. `title`, `subtitle`, `question`, `answer`, `body`),
  `lang_code TEXT`, `value TEXT`. Composite PK `(owner_type, owner_id, field_key, lang_code)`.
  Index on `(owner_type, owner_id, lang_code)` and on `(lang_code, value)` for search.

Indices: FK columns indexed (`help_item.topic_id`, `help_step.item_id`).

## JSON authoring format — `nimaz-pro-data/json/help.json`

```jsonc
{ "topics": [{
  "id": "prayer_times", "order": 1, "icon": "schedule", "color": "indigo",
  "title":    { "en": "Prayer Times", "tr": "Namaz Vakitleri" },
  "subtitle": { "en": "Calculation, Asr, adjustments" },
  "items": [
    { "id": "pt_q1", "type": "question", "order": 1,
      "question": { "en": "Why are my times slightly different?" },
      "answer":   { "en": "They follow your location plus the calculation method you pick." } },
    { "id": "pt_g1", "type": "guide", "order": 2, "icon": "tune", "estimatedMinutes": 1,
      "title": { "en": "Change calculation method" },
      "steps": [
        { "id": "pt_g1_s1", "order": 1, "deeplink": "prayer_settings",
          "pathLabels": ["More", "Prayer Settings"],
          "title": { "en": "Open Prayer Settings" },
          "body":  { "en": "From the bottom bar, go to More → Prayer Settings." } }
      ] }
  ] }
] }
```

- Every `{ locale: value }` map is flattened by the regenerator into `help_string` rows.
- Only provided languages are inserted; missing languages fall back to `en` at runtime.
- `icon` values are Material Symbols names; `color` values map to existing theme color keys.
- `deeplink` values are route keys resolved by the deep-link resolver (see below).

## Regenerator changes — `nimaz-pro-data/scripts/generate_database.py`

- `create_tables`: add `CREATE TABLE` for `help_topic`, `help_item`, `help_step`, `help_string`
  (+ indices), matching the Room entity schema exactly.
- `populate_database`: add a `load_json('help.json')` block that walks topics → items → steps,
  inserts structural rows, and flattens every locale map into `help_string` inserts
  (`json.dumps(path_labels)` for the array column).
- Bump `PRAGMA user_version` to the new schema version.
- Regenerate `nimaz_prepopulated.db` and copy to `app/src/main/assets/database/`.

## Room / DB changes

- Add 4 `@Entity` classes in `data/local/database/entity/HelpEntities.kt` with
  `@ColumnInfo` snake_case names matching the table columns; FKs + indices as above.
- `NimazDatabase`: add the 4 entities + `abstract fun helpDao(): HelpDao`; bump
  **version 13 → 14**; add `MIGRATION_13_14`.
- **Seeding existing users:** `createFromAsset` only runs on fresh installs. `MIGRATION_13_14`
  must (a) create the 4 tables and (b) populate them. Mirror however the most recent seeded
  content (v12→v13: asma_un_nabi / prophets) reached existing users — confirm during planning
  whether that path embeds INSERTs in the migration (regenerator emits the SQL) or another
  mechanism, and reuse it so no existing user sees an empty Help screen.
- `DatabaseModule`: `@Provides @Singleton fun provideHelpDao(db): HelpDao = db.helpDao()`.

## DAO — `data/local/database/dao/HelpDao.kt`

- `getTopics(): Flow<List<HelpTopicEntity>>` ordered by `display_order`.
- `getItemsForTopic(topicId): Flow<List<HelpItemEntity>>`.
- `getStepsForItem(itemId): Flow<List<HelpStepEntity>>`.
- `getStringsFor(ownerType, ownerId): Flow<List<HelpStringEntity>>` (or batched per screen).
- `searchHelp(lang, query): Flow<List<...>>` — `help_string LIKE` filtered by `lang_code`
  (fallback handled in repo), joined back to owner ids/types.
- `Flow` for reads, `suspend` for any single-shot reads, consistent with existing DAOs.

## Domain — `domain/model/HelpModels.kt`

- `HelpTopic(id, iconKey, colorKey, title, subtitle, order, itemCount)`
- sealed `HelpItem`:
  - `HelpQuestion(id, question, answer, order)`
  - `HelpGuide(id, iconKey, title, estimatedMinutes, order, steps: List<HelpStep>)`
- `HelpStep(id, order, title, body, deeplinkRoute: String?, pathLabels: List<String>)`
- `HelpSearchResult(topicId, itemId?, type, title, snippet)`

## Repository — `domain/repository/HelpRepository.kt` + `data/repository/HelpRepositoryImpl.kt`

- Interface methods take `langCode: String`:
  `getTopics(lang)`, `getTopicDetail(topicId, lang)`, `getGuide(guideId, lang)`,
  `search(query, lang)` — all return `Flow`.
- Impl injects `HelpDao`. Resolves strings: prefer `lang_code == lang`, else `en` fallback,
  per field. Maps entities → domain models; JSON-parses `path_labels` (mirror the
  `AsmaUlHusnaRepositoryImpl` `JSONArray` parsing style).
- `@Singleton`, bound via `@Binds` in `RepositoryModule`.

## Use cases — `domain/usecase/HelpUseCases.kt`

`HelpUseCases(getTopics, getTopicDetail, getGuide, searchHelp)`, each a thin class wrapping
the repository, provided via `@Provides` in `UseCaseModule` (matches `QuranUseCases`).

## ViewModel — `presentation/viewmodel/HelpViewModel.kt`

- `@HiltViewModel`, injects `HelpUseCases` + `PreferencesDataStore`.
- `currentLanguage: StateFlow<String>` from `preferencesDataStore.appLanguage` (default `en`).
- State: `homeState` (topics, searchQuery, results, isLoading, error),
  `topicDetailState`, `guideState` — each a data class with `isLoading`/`error`.
- Content flows are `combine(content, currentLanguage)` so they re-resolve if language changes.
- `onEvent(HelpEvent)` dispatcher: `LoadTopics`, `Search(query)`, `LoadTopic(id)`, `LoadGuide(id)`.

## Screens — `presentation/screens/help/`

- `HelpScreen.kt`: `NimazBackTopAppBar` + search field + topic grid (`NimazCard` tiles,
  tinted icon boxes) + contact card. Callbacks: `onNavigateBack`, `onNavigateToTopic(id)`,
  `onContact()`.
- `HelpTopicDetailScreen.kt`: hero + "Common questions" (expandable short answers) +
  "Step-by-step" guide list. Args: `topicId`. Callbacks: `onNavigateBack`, `onOpenGuide(id)`.
- `HelpGuideScreen.kt`: timeline stepper (reuse Today-progress stepper pattern) with
  tappable path chips. Args: `guideId`. Callbacks: `onNavigateBack`, `onDeepLink(routeKey)`.
- All obtain VM via `hiltViewModel()`, collect with `.collectAsState()`, load via
  `LaunchedEffect(args) { viewModel.onEvent(...) }` — consistent with existing screens.

## Navigation — `core/navigation/Routes.kt` + `NavGraph.kt`

- Keep `@Serializable data object SettingsHelp : Route` → `HelpScreen`.
- Add `@Serializable data class HelpTopicDetail(val topicId: String) : Route`.
- Add `@Serializable data class HelpGuide(val guideId: String) : Route`.
- Register all three with `composable<Route.X>` + `toRoute<>()` arg extraction.
- **Deep-link resolver:** a `when(routeKey)` mapping stored `deeplink_route` strings
  (e.g. `"prayer_settings"`) to existing routes (e.g. `Route.SettingsPrayer`), invoked from
  the guide screen's `onDeepLink` callback in `NavGraph`. Unknown keys are no-ops (logged).

## DI summary

- `RepositoryModule`: `@Binds HelpRepository ← HelpRepositoryImpl`.
- `UseCaseModule`: `@Provides HelpUseCases`.
- `DatabaseModule`: `@Provides HelpDao`.

## About screen

Rebuild `AboutScreen.kt` to the approved layout using design-system atoms; trim the
description to a one-line tagline; move translatable labels/tagline into
`res/values*/strings.xml`. No data-layer changes.

## Testing

- Repo unit tests: language resolution + English fallback; `path_labels` JSON parsing;
  search filtering by language.
- DAO tests (in-memory Room): topic/item/step queries, search query.
- ViewModel tests: state transitions for load/search; re-resolution when `appLanguage` emits.
- Migration test: `MIGRATION_13_14` creates tables and (existing-user path) populates them.
- Regenerator: a sanity check that `help.json` round-trips into expected row counts.

## Phasing

1. **Schema + regenerator + seed**: entities, DB v14 + migration, `help.json` (English only),
   regenerator block, prepopulated DB rebuild.
2. **Data layer**: DAO, repository (+ localization resolution), use cases, DI.
3. **Help UI**: ViewModel + 3 screens + navigation + deep-link resolver.
4. **About redesign**: static rebuild + string resources.
5. **Localization content**: translate `help.json` into tr/id/ms/fr/de (content task, not code).

## Open items to confirm in planning

- Exact mechanism for delivering newly-seeded content to existing users (mirror v12→v13).
- Final topic list and the migration of today's 40 items into concise JSON answers (content pass).
- Mapping table of `deeplink_route` keys → existing `Route`s.
