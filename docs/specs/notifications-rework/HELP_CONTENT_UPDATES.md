# Help content the notifications rework invalidates

**Status:** not fixable in this repo. Help content stopped shipping from `app/src/main/assets`
when `HelpContentSeeder` was retired at versionCode 385 (`docs/retirement.yaml`); it now arrives
in the content-database artifact built from **`arshad-shah/nimaz-data`**. These corrections have
to land there and reach devices via `ContentArtifactInstaller`.

Audited against the shipped artifact (`help_string`, `lang_code = 'en'`, 1458 rows) after the
rework. Six entries now describe a UI that no longer exists. Each needs the same edit in the
five translated locales.

| `owner_id` | `field_key` | Ships today | Why it is now wrong | Proposed |
|---|---|---|---|---|
| `na_g_reminder` | `title` | Set a pre-adhan reminder | The reminder is per prayer; there is no single pre-adhan setting. | Set a reminder before a prayer |
| `na_g_reminder_s1` | `body` | Go to More → Settings → Notifications. | The path has another level now. | Go to More → Settings → Notifications → Prayers. |
| `na_g_reminder_s2` | `title` | Enable Pre-Adhan Reminder | That row is gone. | Choose a reminder for each prayer |
| `na_g_reminder_s2` | `body` | Turn it on and choose how many minutes before each prayer you want the alert. | Describes one global toggle and one global lead time. | Open a prayer, tap Reminder before, and pick how far ahead it should arrive. Each prayer keeps its own. |
| `na_q_reminder` | `answer` | Yes — turn on Pre-Adhan Reminder in Settings → Notifications to be alerted a set number of minutes before each prayer. | Same. | Yes — open Settings → Notifications → Prayers, tap a prayer, and set Reminder before. Every prayer can have a different lead time, or none. |
| `na_g_adhan_s2` | `body` | Turn on Enable Adhan Sound and choose a reciter — it downloads the first time it's used. **Mute individual prayers with the sound icon.** | The per-prayer sound icon no longer exists; the voice is a picker sheet. | Turn on Enable Adhan Sound and pick a voice from the list — it downloads the first time it's used. To change what a single prayer does, use its Alert style under Prayers. |

Two more are **inaccurate rather than broken**, and are worth the same pass:

| `owner_id` | `field_key` | Note |
|---|---|---|
| `na_g_enable_s3` | `body` | Names the buttons "Disable Battery Optimization" and "Test Notification". Diagnostics now states each check as a row you tap to fix, and the button reads "Send a test notification". |
| `ts_q_no_notif` | `answer` | Ends "Use Test Notification." — same rename. Otherwise still correct, and the Diagnostics rows now show each of these three prerequisites directly, which is worth mentioning. |

**Not wrong, deliberately left:** `troubleshooting.subtitle` is still "Troubleshooting". The
in-app *screen* is now called Diagnostics, but this is the help *topic* name ("Fix a problem" /
"Troubleshooting"), and someone looking for help with a problem searches for the latter. Renaming
the topic to match the screen would make the help harder to find, not easier. Flagging the
mismatch rather than changing it.

**Also verified as still correct** and needing no change: `na_q_dnd` (Do Not Disturb behaviour is
unchanged — it still gates only the audio) and `ts_q_silent_adhan` (the download/volume advice is
unaffected).

## Checking this again after a future change

```bash
sqlite3 app/build/generated/nimazData/assets/database/nimaz_prepopulated.db \
  "select owner_id, field_key, value from help_string
   where lang_code='en' and owner_id like 'na_%' or owner_id like 'ts_%';"
```

The artifact is fetched by `:app:fetchNimazData`, so the database only exists after a build.
