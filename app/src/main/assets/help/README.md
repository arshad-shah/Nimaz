# Help / FAQ / Guide content

All in-app **Help, FAQ and Guide** content (the Help & About screens — topics,
questions, and step-by-step guides) is data-driven from **`help.json`** in this
folder. It is **not** in `res/values*/strings.xml`. Do not add FAQ/guide copy to
`strings.xml` — those keys are not read by the help screens and are dead weight.

> If you came here because you edited a `faq_*` / `guide_*` string and nothing
> changed in the app: that's why. Edit `help.json` instead.

## How it reaches the app

`help.json` → `HelpContentSeeder.seedIfNeeded()` → Room `help_*` tables → repository → UI.

- `HelpJsonDto.kt` — the schema (`HelpJsonRoot` → `HelpTopicDto` → `HelpItemDto` → `HelpStepDto`).
- `HelpContentSeeder.kt` — parses the JSON and replaces the `help_*` tables.
- Localized text is stored per-field, per-language in `HelpStringEntity` rows.

### ⚠️ Bump `contentVersion` on every content change

The seeder only re-seeds when the tables are empty **or** when `help.json`'s
`contentVersion` is **greater than** the version last stored in DataStore. Fresh
installs always get the latest; **existing users only get your change if you
increment `contentVersion`.** Forgetting this means your edit ships to nobody who
already has the app. (Always-from-JSON seeding is deliberately used here instead
of the prepopulated DB asset, precisely so updates can reach existing users.)

So: **edit content → bump `"contentVersion"` by 1.**

## Schema

```jsonc
{
  "contentVersion": 4,            // bump on EVERY content change
  "topics": [
    {
      "id": "tt",                 // stable unique id (don't reuse/rename casually)
      "order": 1,
      "icon": "self_improvement", // material icon name
      "color": "#4CAF50",         // hex accent
      "title":    { "en": "...", "tr": "...", ... },
      "subtitle": { "en": "...", ... },
      "items": [
        { "id": "tt_q_fasting", "type": "question", "order": 4,
          "question": { "en": "...", ... },
          "answer":   { "en": "...", ... } },

        { "id": "tt_g_qada", "type": "guide", "order": 6,
          "icon": "task_alt", "estimatedMinutes": 1,
          "title": { "en": "...", ... },
          "steps": [
            { "id": "tt_g_qada_s1", "order": 1,
              "deeplink": "nimaz://prayer-tracker",   // optional
              "pathLabels": ["More", "Prayer Tracker"], // optional breadcrumb
              "title": { "en": "...", ... },
              "body":  { "en": "...", ... } }
          ] }
      ]
    }
  ]
}
```

- `type` is `"question"` (question + answer) or `"guide"` (title + ordered steps).
- A step's `deeplink` is a **key**, not a route. Keys are mapped to in-app routes
  in `core/navigation/HelpDeepLink.kt` (`helpDeepLinkRoute`). Currently supported:
  `prayer_settings`, `notifications`, `location`, `qibla`, `quran_settings`,
  `language`, `appearance`, `calendar`, `fasting`, `tasbih`, `hadith`, `settings`,
  `home`. To deep-link somewhere new, add the key → `Route` there first; an
  unknown key just renders the step without a jump button (`pathLabels` still show).
- Icon and color strings (`icon`, `color`) are resolved in
  `presentation/screens/help/HelpContentUi.kt`. Use a name already handled there,
  or it falls back to a default.
- Every user-facing string is a language map. **Supported languages:** `en`,
  `tr`, `id`, `ms`, `fr`, `de`. Provide `en` at minimum; the UI falls back to it.
- `id`s are stable keys — keep them consistent across edits.

## Updating content — checklist

1. Edit the relevant `title` / `question` / `answer` / `body` maps (all languages).
2. Keep tab/screen names in sync with the actual UI labels (e.g. the Fasting
   answer names the **Tracker** and **Makeup** tabs — match `fasting_tab_*`).
3. **Bump `contentVersion`.**
4. Keep the file valid JSON: `python3 -c "import json;json.load(open('app/src/main/assets/help/README.md'.replace('README.md','help.json')))"`.
