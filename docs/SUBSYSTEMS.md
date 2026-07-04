# Nimaz — Subsystems Guide

> **Audience:** developers and AI agents working on this codebase.
> **Purpose:** documents the app's **cross-cutting subsystems** — the runtime machinery
> (audio, widgets, background work, notifications, database, preferences, content
> seeding, prayer-time calc, init/monitoring, sync) that the feature screens depend on.
> For *how features are layered* (`presentation → domain → data`, MVVM/UDF, DI, navigation,
> theming) read **[`ARCHITECTURE.md`](ARCHITECTURE.md)** first — this doc assumes it.

App package root: `com.arshadshah.nimaz`
Source root: `app/src/main/java/com/arshadshah/nimaz/`

---

## Table of contents

1. [Audio playback](#1-audio-playback)
2. [Glance widgets](#2-glance-widgets)
3. [Background work (WorkManager)](#3-background-work-workmanager)
4. [Prayer-time / adhan notifications](#4-prayer-time--adhan-notifications)
5. [Database & migrations](#5-database--migrations)
6. [Preferences (DataStore)](#6-preferences-datastore)
7. [Content seeding & versioning](#7-content-seeding--versioning)
8. [Prayer-time calculation](#8-prayer-time-calculation)
9. [App initialization & monitoring](#9-app-initialization--monitoring)
10. [Device-to-device sync](#10-device-to-device-sync)
11. [Content sharing](#11-content-sharing)
12. [Keeping this doc updated](#keeping-this-doc-updated)

---

## 1. Audio playback

All in `data/audio/`. There are **three independent playback engines** (Quran recitation,
Adhan, Qaida tap-to-hear) plus an Adhan **download** pipeline. They share no player instance.

**Manager / service split.** The *manager* owns the player + playback logic and exposes a
`StateFlow`; the *service* is a foreground `Service` that only owns the notification /
`MediaSession` lifecycle and routes control intents back into the manager.

| Class | Role |
|---|---|
| `data/audio/QuranAudioManager.kt` | `@Singleton`; one `ExoPlayer`, gapless ayah playlist, exposes `val audioState: StateFlow<AudioState>` |
| `data/audio/QuranAudioService.kt` | `@AndroidEntryPoint` foreground `mediaPlayback` service; `MediaStyle` notification (channel `quran_audio_channel`, id 1001) over the manager's `ForwardingPlayer` |
| `data/audio/AdhanAudioManager.kt` | `@Singleton`; legacy `MediaPlayer` for in-app `preview()`, plus adhan **download** logic; exposes `isPlaying`, `currentlyPlaying`, `downloadState` flows |
| `data/audio/AdhanPlaybackService.kt` | foreground `mediaPlayback` service; plays the adhan when a prayer fires (works app-closed) using `ExoPlayer` with `USAGE_ALARM` + wake lock + audio focus |
| `data/audio/AdhanDownloadService.kt` | foreground `dataSync` service that downloads both adhan variants with a progress notification (channel `adhan_download_channel`, id 7777) |
| `data/audio/AdhanDownloadWorker.kt` | `@HiltWorker` background fallback for the download (see §3) |
| `data/audio/QaidaAudioManager.kt` | `@Singleton`; stripped-down `ExoPlayer` for single Qaida tokens — **no service/notification/MediaSession/CDN**; exposes `val state: StateFlow<QaidaAudioState>` |
| `data/audio/AdhanSound.kt` | enum of adhans (MISHARY, ABDUL_BASIT, MAKKAH, SIMPLE_BEEP) with per-variant file names + download URLs |

**Wiring.** None of these have a DI module — the managers are `@Singleton @Inject constructor(@ApplicationContext …)` (Hilt provides them automatically) and the services are `@AndroidEntryPoint` field-injecting their manager. Services are declared in `AndroidManifest.xml` with `foregroundServiceType` `mediaPlayback`/`dataSync`; permissions `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `FOREGROUND_SERVICE_DATA_SYNC`.

**Media3/ExoPlayer specifics (Quran).** Ayahs are downloaded first then added as a `List<MediaItem>` for gapless sequential playback. ExoPlayer reports `0` duration for unloaded items, so durations are pre-extracted with `MediaMetadataRetriever`; a `ForwardingPlayer` (`getPlayer()`) translates per-item ExoPlayer position/duration into **whole-surah** ("total playlist") coordinates so the lock-screen scrubber reflects the surah, not one ayah. Recitations stream from `cdn.islamic.network` (reciter→CDN in `RECITER_CDN_MAP`), cached under `filesDir/quran_audio/`. Adhan files cache under `filesDir/adhan/`, Qaida clips fall back to the bundled asset `file:///android_asset/qaida/audio/{key}.mp3`.

**How ViewModels consume it.** ViewModels inject the manager **directly** and re-expose its flow (e.g. `QuranViewModel.audioState = audioManager.audioState`; `QaidaReaderViewModel.audioState = audioManager.state`; `SettingsViewModel` injects `adhanAudioManager` for preview/download). They drive playback via `onEvent`. *Deviation:* this bypasses the "ViewModels inject `XxxUseCases`" rule (§ARCHITECTURE), and `QuranViewModel` even exposes the manager as a public field — a known clean-architecture deviation, not a pattern to copy.

**Gotchas.**
- Two player APIs: ExoPlayer everywhere **except** `AdhanAudioManager.preview()` (legacy `MediaPlayer`).
- `QuranAudioManager.stop()` deliberately does **not** send a stop intent to the service — it sets `isActive=false` and lets the service's state-observer self-stop after a 500 ms debounce, avoiding a start/stop race.
- Adhan downloads are heavily validated (MP3/WAV magic bytes, content-type, min size, URL-version invalidation via `ADHAN_URL_VERSION`) because the external CDNs sometimes serve HTML error pages; on corrupt/missing files playback falls back to the generated beep, **never** to the wrong adhan variant.
- `SIMPLE_BEEP` is **synthesized locally** (`generateBeepSound` → WAV), not downloaded.

---

## 2. Glance widgets

All in `widget/`. Five Jetpack **Glance** AppWidgets, each in its own subpackage, plus a
shared `widget/core/` package and two top-level helpers.

| Widget | Package | Refresh | State type |
|---|---|---|---|
| Next Prayer | `widget/nextprayer/` | Worker 15 min + AlarmManager 1 min tick | `NextPrayerWidgetState` |
| Prayer Times | `widget/prayertimes/` | Worker 15 min + 1 min tick | `PrayerTimesWidgetState` |
| Prayer Tracker | `widget/prayertracker/` | Worker 30 min + immediate on toggle | `PrayerTrackerWidgetState` |
| Hijri Date | `widget/hijridate/` | Worker 6 hr | `HijriDateWidgetState` |
| Hijri Calendar | `widget/hijricalendar/` | Worker 6 hr | `HijriCalendarWidgetState` |

Each widget = a `GlanceAppWidget` subclass (`provideGlance` → `provideContent { GlanceTheme { … } }`, reads `currentState<T>()`) + a `GlanceAppWidgetReceiver` (the manifest-registered `BroadcastReceiver`; `onEnabled` starts refresh, `onDisabled` cancels). State is a `@Serializable sealed interface` with `Loading`/`Success(data)`/`Error(message)`. Colors come from `res/color` via `ColorProvider(R.color.widget_*)` — no hardcoded colors.

**Data access — two patterns.**
1. **`@HiltWorker` injection (main path).** Workers inject real deps directly (e.g. `NextPrayerWorker` injects `PrayerTimeCalculator` + `PreferencesDataStore`; `PrayerTrackerWorker` injects `PrayerDao`). Each `doWork()` returns `Result.success()` early if no widgets are placed, computes fresh data, persists via `setWidgetState(...) → Success`, and on failure persists `Error` + `Result.retry()` for the first 3 attempts. This only works because `NimazApp` provides the `HiltWorkerFactory` (§3).
2. **Hilt `@EntryPoint`** — `widget/WidgetEntryPoint.kt` exposes `prayerDao()` via `EntryPointAccessors.fromApplication(...)`. Used by the **only interactive widget** (Prayer Tracker): its checkbox click handler (`togglePrayerStatus` in `PrayerTrackerWidget.kt`) writes to Room from inside the composable click callback (not a Worker), then re-renders via `PrayerTrackerWorker.enqueueImmediateWork(context)`.

**Update mechanism — three layers.**
- **Periodic WorkManager** via `widget/core/WidgetWork.kt` (`enqueuePeriodic`/`enqueueImmediate`/`cancel`), enqueued in each receiver's `onEnabled`.
- **Per-minute AlarmManager tick** via `widget/WidgetUpdateScheduler.kt` (WorkManager's 15-min floor is too coarse for a live countdown). `setInexactRepeating(ELAPSED_REALTIME, …, 60_000)` fires `WidgetTickReceiver`, which just calls `updateAll(context)` on the two countdown widgets — it does **not** recompute prayer times; the composable recomputes the live values from the stored absolute prayer instants.
  - **Prayer Times "next prayer" highlight is render-time, not worker-time.** The worker stores each prayer's absolute `…EpochMillis` (not pre-computed "passed" flags). The composable picks which pill to highlight via `widget/core/PrayerHighlight.kt#nextPrayerIndex(epochs, now)` — the first prayer whose instant is still in the future, or `-1` (none) after Isha — and derives the header "X in Ym" from the same index. So every redraw tracks the wall clock; the highlight no longer lags behind the 15-min worker or gets stuck on a passed prayer under Doze throttling. (Pure function, unit-tested in `PrayerHighlightTest`.)
- **Immediate refresh** on prayer-status change, from the tracker toggle and from `HomeViewModel` (keeps the widget in sync with in-app tracking).

**Shared `widget/core/`.** `JsonGlanceStateDefinition.kt` (generic JSON-over-DataStore `GlanceStateDefinition`, one DataStore per file via a process-wide map), `WidgetStateUpdater.kt` (`updateWidgetState(...)`), `WidgetFormatters.kt` (time/countdown), `WidgetUi.kt` (`WidgetPalette`, `WidgetMessageBox`, `WidgetLoadingBox`, plus the redesign atoms `WidgetCard`, `WidgetIcon`, `WidgetLabel`, `WidgetPill`, `prayerIconRes`), `WidgetWork.kt`.

**Widget UI design ("Refined Minimal").** Solid `widget_background` surface, `16dp`
corners, teal `widget_primary` accent. **No emoji/ASCII/unicode glyphs** — all icons
are monochrome vector drawables in `res/drawable/ic_widget_*.xml` drawn via `WidgetIcon`
(`Image` + `ColorFilter.tint`), so they follow light/dark + accent. The Next Prayer
widget picks a celestial icon per prayer via `prayerIconRes`; the Tracker uses a teal
disc + `ic_widget_check` when prayed and a two-disc outline ring when not (Glance has no
stroke modifier); Prayer Times is a clean 5-cell pill row with the next prayer filled
teal, past prayers tinted gold (`widget_gold_container` / `widget_on_gold_container`),
and upcoming ones plain; every state pairs an explicit container with an on-container
text colour so the text stays legible in both light and dark mode. A JVM regression test (`WidgetGlyphGuardTest`) fails the
build if any widget source reintroduces a glyph.

**Manifest/res.** Five `<receiver>`s + the non-exported `WidgetTickReceiver` in `AndroidManifest.xml`; provider-info XMLs in `res/xml/*_widget_info.xml`.

**Gotchas.**
- Default state is `Success(emptyData)`, not `Loading` → widgets show em-dash skeletons, not a spinner, before the first worker run.
- The 1-min tick only recomposes; prayer time/name only refresh on the 15-min worker.
- NextPrayer and PrayerTimes share one AlarmManager request code (`9876`); removing one countdown widget can silently cancel the tick for the other (documented in `NextPrayerWidget.onDisabled`).
- `togglePrayerStatus` writes a Room `PrayerRecordEntity` directly from the widget layer via the EntryPoint — a layering deviation, noted but intentional for interactivity.

---

## 3. Background work (WorkManager)

`NimazApp` (`NimazApp.kt`) is `@HiltAndroidApp` **and** `Configuration.Provider`: it injects
`HiltWorkerFactory` and supplies it via `workManagerConfiguration`, which is what lets every
`@HiltWorker` be constructed with injected dependencies. Without this, worker injection fails
at runtime.

**Workers in the app:**
- `widget/*/{NextPrayer,PrayerTimes,PrayerTracker,HijriDate,HijriCalendar}Worker.kt` — widget refresh (§2), all `@HiltWorker CoroutineWorker`, enqueued through `widget/core/WidgetWork.kt`.
- `data/audio/AdhanDownloadWorker.kt` — `@HiltWorker CoroutineWorker`; the background fallback for adhan downloads when a foreground service can't be started (see below). `enqueue(...)` builds a `OneTimeWorkRequest` with a `CONNECTED` constraint, `ExistingWorkPolicy.KEEP`, unique name `adhan_download_work`, retrying up to 3 times.

**Foreground-service-from-background gotcha.** On Android 12+ starting a foreground service from the background throws `ForegroundServiceStartNotAllowedException`. `AdhanDownloadService.startServiceWithFallback` gates on `ActivityManager` process importance: start the foreground service only if the app is foregrounded, otherwise **degrade to `AdhanDownloadWorker`**. The two share the same `AdhanAudioManager` download logic.

**Note: prayer notifications do NOT use WorkManager** — they use `AlarmManager` exact alarms (§4). WorkManager here is widgets + the adhan-download fallback.

**Boot.** `core/util/BootReceiver.kt` re-runs scheduling on `BOOT_COMPLETED` (§4); widget periodic work survives reboots via WorkManager's own persistence.

---

## 4. Prayer-time / adhan notifications

Built on **`AlarmManager` exact alarms** (no WorkManager). Per-prayer notifications, optional
adhan playback, pre-prayer reminders, a nightly daily summary, and re-scheduling on midnight
rollover and boot.

**Key files.**
- `core/util/PrayerNotificationScheduler.kt` — `@Singleton`; schedules/cancels alarms, owns the channels.
- `core/util/BootReceiver.kt` — `@AndroidEntryPoint BroadcastReceiver`; fires for **all** alarms and actually posts notifications / triggers adhan.
- `core/util/NotificationContentHelper.kt` — pure title/message/summary text generator.
- `data/audio/AdhanPlaybackService.kt` — plays the adhan and posts the merged prayer+adhan notification (§1).

**Channels** (created in `PrayerNotificationScheduler.init`, API 26+): `CHANNEL_ID_PRAYER` = `prayer_notifications` (HIGH), `CHANNEL_ID_ADHAN` = `adhan_notifications` (HIGH), `CHANNEL_ID_DAILY_SUMMARY` = `daily_summary_notifications` (DEFAULT), plus no-vibration siblings `CHANNEL_ID_PRAYER_SILENT` / `CHANNEL_ID_ADHAN_SILENT`. **Vibration is a channel property** — Android ignores `enableVibration()` changes after a channel exists, so the `notificationVibration` preference is honoured by *posting on the matching channel* via `channelForPrayer(vibrate)` / `channelForAdhan(vibrate)`, **not** by per-notification `setVibrate` (that's kept only as the pre-O fallback). `AdhanPlaybackService` also creates `adhan_playback_channel` but **posts on `CHANNEL_ID_ADHAN`** — so the playback channel is effectively unused for the visible notification.

**Scheduling.** `scheduleTodaysPrayerNotifications(...)` cancels everything then re-arms enabled prayers, using `setExactAndAllowWhileIdle(RTC_WAKEUP, …)` with `PendingIntent.getBroadcast` targeting `BootReceiver` (explicit intent). Request codes: prayer `1000 + ordinal`, pre-reminder `2000 + ordinal`, midnight reschedule `9999` (00:01), daily summary `8889` (23:00), Friday reminder `8890`. Pre-reminders fire at `prayerTime − preReminderMinutes` (skipped for Sunrise); the lead time is **user-editable** (the pre-adhan stepper → `SetReminderMinutes`). The **Friday (Jummah) reminder** (`scheduleFridayReminder`, gated on `fridayReminderEnabled`) is a one-shot at the upcoming Friday's Dhuhr − `fridayReminderMinutes`, re-armed on every reschedule so it always targets the next Friday.

**Firing.** `BootReceiver.onReceive` dispatches on action: boot → reschedule; `ACTION_MIDNIGHT_RESCHEDULE` → mark missed prayers + reschedule (self-perpetuating daily chain); `ACTION_PRAYER_NOTIFICATION` → post notification &/or play adhan; `ACTION_DAILY_SUMMARY` → summary; `ACTION_FRIDAY_REMINDER` → post the Jummah reminder. If adhan should play and the file exists, it calls `AdhanPlaybackService.playAdhan(...)` and the service's foreground notification **doubles as** the prayer notification (shared id `prayerName.hashCode()`); if the file is missing it triggers a download for next time and falls back to beep. **Do Not Disturb:** when `adhanRespectDnd` is on and the system is in a DND mode, `dndBlocksAdhan` gates only the **adhan audio** (`shouldPlayAdhan`/`shouldPlayBeep`) — the visual prayer notification is still posted, and the OS silences its channel sound under DND. The Friday reminder (no adhan audio) always posts and is likewise silenced by the OS under DND.

**Wiring.** `PrayerNotificationScheduler` is constructor-injected (`@Singleton @Inject`, deps: `PrayerTimeCalculator`). Called by `AppInitializer` on startup and by `SettingsViewModel.rescheduleNotifications()` when prayer/notification settings change. Permissions in `AndroidManifest.xml`: `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.

**Gotchas.**
- Uses `USE_EXACT_ALARM` (auto-granted, alarm-clock class app) and does **not** check `canScheduleExactAlarms()` or catch `SecurityException`.
- **Doze / battery optimization** is the main "fired late" failure mode; the code measures `deliveryLatencySeconds` for telemetry but doesn't request the battery exemption itself.
- `POST_NOTIFICATIONS` (API 33+) isn't checked before `notify()` — denied → silent no-op.
- Channels exist only once the scheduler singleton is first instantiated by DI.
- `BootReceiver` handles `LOCKED_BOOT_COMPLETED`/QUICKBOOT in code but only `BOOT_COMPLETED` has a manifest intent-filter.

---

## 5. Database & migrations

`data/local/database/NimazDatabase.kt` — a single Room `@Database` (`version = 17`), provided
in `core/di/DatabaseModule.kt`.

**Prepopulated DB.** The app ships a prebuilt DB in `app/src/main/assets/database/nimaz_prepopulated.db`, wired via `.createFromAsset("database/nimaz_prepopulated.db", NimazDatabase.PREPACKAGED_CALLBACK)`. **Room copies this asset only on a fresh install** — it is *not* re-copied on app update. That single fact drives both the migration discipline here and the entire content-seeding subsystem (§7).

**`PREPACKAGED_CALLBACK`** repairs the shipped asset right after copy and before Room validates its schema (the bundled asset was stamped at `user_version 12` while still missing the `updatedAt` columns and shipping tafseer indices under the wrong names). The same idempotent repair is also exposed as `MIGRATION_12_13`, because devices already sitting at v12 never re-run the copy callback.

**Migration chain** (registered in `DatabaseModule.provideNimazDatabase`): `MIGRATION_7_8` (khatam) → `8_9` (asma/prophets) → `9_10` (a *missing* migration restored after the original release bumped the version without registering it) → `10_11` (`updatedAt` columns) → `11_12` (surah start_page fix) → `12_13` (legacy asset repair) → `13_14` (Help tables) → `14_15` (Qaida tables) → `15_16` (tasbih `category`) → `16_17` (hadith `narrator_chain`).

**Rules / patterns.**
- **A schema change requires a migration.** Bump `@Database(version = …)` and add a `MIGRATION_x_y`. Room runs migrations **even after `createFromAsset`**, so every migration must work for both fresh installs (asset already has newer tables → `CREATE TABLE IF NOT EXISTS` is a no-op) and upgraders (tables created empty / columns added).
- **Migrations must be idempotent.** SQLite has no `ADD COLUMN IF NOT EXISTS`, so column adds go through the `SupportSQLiteDatabase.addColumnIfMissing(...)` helper at the bottom of the file; table creates use `CREATE TABLE IF NOT EXISTS`.
- **Content tables vs user-data tables.** Content tables (help/qaida/dua) are seeded; user-progress tables (`*_progress`, bookmarks) are always created empty.

**One version constant.** The schema version lives in a single top-level `const val
NIMAZ_DATABASE_VERSION` (in `NimazDatabase.kt`). It drives **both** the Room
`@Database(version = …)` annotation and `NimazDatabase.SCHEMA_VERSION` (the latter only tags crash
reports via `NimazApp`'s `db_schema_version` key). **Bump `NIMAZ_DATABASE_VERSION` in that one
place** whenever you add a migration — the two can no longer drift.

---

## 6. Preferences (DataStore)

`data/local/datastore/PreferencesDataStore.kt` — the app's **single central settings store**,
backed by a Jetpack Preferences DataStore (`preferencesDataStore(name = "nimaz_preferences")`).

**It implements the `domain/repository/SettingsRepository` interface.** Presentation code
(ViewModels, `MainActivity`) injects **`SettingsRepository`**, not the concrete class (bound via
`@Binds` in `RepositoryModule`); the combined snapshot model `UserPreferences` lives in
`domain/model`. Data-layer consumers (both sync classes, all content seeders, `AppInitializer`,
`BootReceiver`, widget workers) inject the concrete `PreferencesDataStore` directly — that's fine,
they're in the data layer. When you add a new setting, add it to **both** the class and the
`SettingsRepository` interface.

**API shape — Flow getter + `suspend` setter per setting:**
```kotlin
val calculationMethod: Flow<String> = preference(PreferencesKeys.CALCULATION_METHOD, "MUSLIM_WORLD_LEAGUE")
suspend fun setCalculationMethod(method: String) = put(PreferencesKeys.CALCULATION_METHOD, method)
```
Getters expose `Flow<…>` only (never `MutableStateFlow`/`LiveData`); writes are `suspend`. Internal helpers `preference(key, default)` / `preference(key)` / `put(key, value)` keep the surface uniform. `private object PreferencesKeys` holds all typed keys and is private to the class — consumers never touch raw keys.

**Aggregate.** `val userPreferences: Flow<UserPreferences>` maps a curated subset of keys into a top-level `data class UserPreferences(...)` for one-shot reads of the common cross-cutting settings (used by `SettingsViewModel`, `LocationViewModel`, `AppInitializer`, `BootReceiver`, `WidgetsScreen`).

**Bulk ops.** `clearAllData()`, `exportAllPreferences(): Map<String,String>`, `importPreferences(map)` (type-infers values back to keys, skips `onboarding_completed`) — used by the sync subsystem (§10).

**Wiring.** Provided in `core/di/DataStoreModule.kt` via `@Provides @Singleton`. (Minor: it already has an `@Inject constructor(context)`, so the explicit provider is redundant with constructor injection.)

It also stores the **content-version flags** that drive seeding — see §7.

---

## 7. Content seeding & versioning

**Why this exists.** The prepopulated DB (§5) is **not re-copied on app update**, and schema
migrations only create empty tables. So new *bundled content* shipped in an update would never
reach existing users. **Seeders** read a versioned JSON asset at runtime and upsert content
idempotently, so fresh installs and upgraders converge on the same content.

**Four content types use seeders:**

| Content | Seeder | JSON asset | Pattern |
|---|---|---|---|
| Dua | `data/local/dua/DuaContentSeeder.kt` | `duas/duas.json` | full content replace |
| Help | `data/local/help/HelpContentSeeder.kt` | `help/help.json` | full content replace |
| Qaida | `data/local/qaida/QaidaContentSeeder.kt` | `qaida/qaida_content.json` | full content replace |
| Hadith | `data/local/hadith/HadithBackfillSeeder.kt` | `hadith/hadith_fills.json` | keyed UPDATE backfill |

**Content-version pattern.** The version is stored in **DataStore** (not a file, not a table):
`PreferencesKeys.{DUA,HELP,QAIDA}_CONTENT_VERSION` and `HADITH_BACKFILL_VERSION` (default `0` = never seeded). Each JSON root carries a `contentVersion: Int` field. `seedIfNeeded()`:
1. parse the JSON asset;
2. if the table is already populated **and** stored version ≥ JSON `contentVersion` → skip;
3. otherwise seed and write the new stored version.

Content seeders do an **atomic full-content replace** (`dao.replaceAllContent(...)` / clear-then-insert), touching only content tables — user data (bookmarks/progress) lives in separate tables with no FK into content, so it survives a re-seed. The Hadith backfill is different: the JSON `id` is the stable PK of the `hadiths` row, so each fix is a keyed UPDATE (`backfillHadith`/`updateNarratorChain`); it also has a fast-path gap detector (`emptyArabicCount()`). All seeders serialize concurrent calls with a `Mutex`.

**Where seeding is triggered — lazy "seed-then-read," NOT `AppInitializer`.** Three are triggered inside repositories at first content access (`DuaRepositoryImpl`/`HelpRepositoryImpl` use a `seededFlow { seeder.seedIfNeeded(); emitAll(...) }` wrapper; `HadithRepositoryImpl` calls `backfillSeeder.seedIfNeeded()` at the top of each suspend read). **Qaida is the exception** — seeded in `QaidaReaderViewModel.init`.

**Wiring.** The `XxxAssetReader` and `XxxVersionStore` interfaces (which exist to make seeders testable without Android/DataStore) are bound in `core/di/RepositoryModule.kt` via `@Binds` (→ `AndroidXxxAssetReader` / `DataStoreXxxVersionStore`); the seeder classes are `@Singleton @Inject`.

**Gotchas.**
- **To ship new content to existing users you must bump `contentVersion` in the JSON asset.** Editing the prepopulated DB alone does nothing for upgraders; editing JSON without bumping the version does nothing once the table is populated.
- Content seeders **fully replace** content tables — anything not in the JSON is wiped. Adding an FK from a user table into a content table would make re-seeding destructive.
- Qaida seeds only when `QaidaReaderViewModel` is created, not on first repository access.
- Qaida `conceptTags` are stored JSON-encoded-as-string to match the prepopulated-DB convention.

---

## 8. Prayer-time calculation

`core/util/PrayerTimeCalculator.kt` — `@Singleton @Inject constructor()` (pure compute,
no deps). All third-party usage is isolated here.

**Library.** **Adhan2** by Batoul Apps (`com.batoulapps.adhan:adhan2:0.0.6`, `libs.adhan`). Adhan2 types are import-aliased (e.g. `CalculationMethod as AdhanMethod`) to avoid colliding with the app's own domain enums.

**Integration.** The calculator maps domain enums → Adhan2 `CalculationParameters`:
- **Method** — `adhanMethodFor(method)` maps 11 domain `CalculationMethod`s to `AdhanMethod` (MWL, Egyptian, Karachi, Umm al-Qura, Dubai, MoonSighting, North America, Kuwait, Qatar, Singapore, Turkey).
- **Madhab / Asr** — `AsrCalculation.STANDARD → SHAFI`, `HANAFI → HANAFI`.
- **High-latitude rule** — optional; maps MIDDLE_OF_THE_NIGHT / SEVENTH_OF_THE_NIGHT / TWILIGHT_ANGLE, else library default.

**Inputs/outputs.** `getPrayerTimes(lat, lon, date, method, asr, highLat, adjustments)` → `List<PrayerTime>` (raw `Instant`s; supports per-prayer minute `adjustments`). `calculatePrayerTimes(date, location)` / `…ForRange(...)` take a domain `Location` and return `PrayerTimes`/`List<PrayerTimes>` (Adhan `Instant`s converted to `LocalDateTime` in the location's zone). Returns **domain models** (`domain/model/PrayerModels.kt`), never Adhan types. Settings come from `PreferencesDataStore` (string prefs parsed to enums by the caller).

**Hijri conversion** — `core/util/HijriDateCalculator.kt`, a stateless Kotlin `object` (no Hilt). It does **not** use `ummalqura`; it delegates to the platform `java.time.chrono.HijrahChronology.INSTANCE` (OS-updated Umm al-Qura). Provides `toHijri`/`toGregorian`, Ramadan helpers, validity checks, and a hardcoded Islamic-events calendar (`getIslamicEvents`/`getUpcomingEvents`).

**Wiring.** No module — both are constructor-injected / static. `PrayerTimeCalculator` is injected into `PrayerRepositoryImpl` and (a deviation from the use-case rule) directly into several ViewModels, widget workers, and `PrayerNotificationScheduler`.

**Display formatting.** Wall-clock times are rendered through `core/util/TimeFormatting.kt`
(`formatClockTime(hour, minute, use24Hour)` + `LocalTime`/`LocalDateTime.formatClock(...)`),
never via ad-hoc `String.format("%d:%02d %s", …, "AM"/"PM")` or `Locale.US`-pinned formatters.
It uses the **default locale** (localized am/pm marker and digits — Nimaz is worldwide) and
honors the user's `use24HourFormat` preference. In composables read the preference from
`LocalUse24HourFormat.current`; in ViewModels collect `settingsRepository.use24HourFormat`
(see `HomeViewModel.observeTimeFormat`) and recompute. Durations (e.g. fasting length) use
`formatFastLength(minutes)` and are computed from **raw** times — never by re-parsing already
formatted strings.

---

## 9. App initialization & monitoring

**`NimazApp.kt`** (`@HiltAndroidApp`, `Configuration.Provider`). `onCreate` calls
`AppAnalytics.init(this)`, logs a crash breadcrumb, tags the crash DB-schema key, and runs
`appInitializer.initialize()`. It also supplies the `HiltWorkerFactory` (§3).

**`core/init/AppInitializer.kt`** — `@Singleton`. `initialize()` launches an IO coroutine that runs three tasks **in parallel under a 5 s `withTimeout`** (then proceeds to UI regardless): apply saved locale, schedule today's prayer notifications (§4), download the default adhan/beep if missing (§1). It exposes `val isReady: StateFlow<Boolean>` (the splash gate). Failures are reported to monitoring but never block startup.

**`core/monitoring/`** — three thin Kotlin `object` wrappers over Firebase, each guarded so **every call is wrapped in `runCatching` and no-ops if Firebase isn't initialized** (debug/PR-check builds without `google-services.json` run unchanged). They are static singletons, never Hilt-injected.
- `AppAnalytics.kt` → Firebase **Analytics**. The only one with `init(context)` (called from `NimazApp`) — it caches `applicationContext` so any caller can log without a `Context`. Provides semantic helpers + name catalogs (`Event`/`Param`/`UserProperty`), notably the notification pipeline (`notification_scheduled`/`_displayed`/`_suppressed`/`_opened`) and `logDiagnostics()` (records OS-level notification/exact-alarm/battery state as durable user properties).
- `CrashReporter.kt` → Firebase **Crashlytics**. `recordException`, `log` (breadcrumb), `setCustomKey`. Pairs with `AppAnalytics.logError` (frequency) for the stack trace.
- `PerfMonitor.kt` → Firebase **Performance**. Custom traces via `newTrace`/`stop` + inline `trace { }` / `traceSuspend { }`; catalog `Traces` (`app_initialize`, `notification_schedule`).

**Instrumentation conventions (apply these as you write code):**
- **Error-swallowing `catch`/`runCatching`** that hides a real failure (data load, IO, parse,
  audio, network, background work) should call `CrashReporter.recordException(e)` — rename
  `catch (_: …)` to `catch (e: …)`. In ViewModels, also call `AppAnalytics.logError(feature, type, e.message)`
  for frequency. Skip *expected* control-flow catches (permission probes, optional system
  services, "no data yet"). All monitoring calls are safe no-ops when Firebase is absent.
- **Significant user actions** (open a reader/detail, toggle favorite/bookmark, create/complete/delete,
  play audio, run a search) get one `AppAnalytics.logFeatureUsed("<feature>", "<action>")` in the
  relevant `onEvent` branch — never log trivial state churn (text edits, scroll). Coverage is broad:
  every ViewModel logs usage, and error paths across data/widget/worker/util/VM layers report to Crashlytics.

---

## 10. Device-to-device sync

`data/sync/` — offline peer-to-peer transfer of a user's app data directly between two phones
over Google's **Nearby Connections** API. No server, no account: one device sends, the other
receives.

| File | Role |
|---|---|
| `data/sync/NearbyConnectionsManager.kt` | `@Singleton`; transport (advertise/discover/connect/payload) |
| `data/sync/SyncDataExporter.kt` | `@Singleton`; DB + DataStore → `SyncPayload` |
| `data/sync/SyncDataImporter.kt` | `@Singleton`; `SyncPayload` → DB + DataStore (last-write-wins merge) |
| `data/sync/SyncData.kt` | `SyncPayload` + per-table DTOs + `categories()` |
| `data/sync/SyncSignal.kt` | control-message protocol |
| `presentation/viewmodel/SyncViewModel.kt` | orchestrator (`@HiltViewModel`, proper UDF) |

**What syncs.** `SyncDataExporter.export()` pulls 14 DAOs + the full DataStore dump (`exportAllPreferences()`) into one `SyncPayload`: Quran bookmarks/favorites/progress, prayer/fast/makeup records, tasbih, khatam, tafseer highlights/notes, zakat, name/prophet favorites, hadith/dua bookmarks + dua progress, qaida progress, and **favorite locations only** (the active-location flag is deliberately never carried, so a sync never changes the receiver's prayer times). `SyncData.categories()` is the single source of truth for the UI summary; a reflection-based test fails the build if a `SyncPayload` field is added without a matching category.

**Transport.** `kotlinx.serialization` JSON, GZIP-compressed, over `Strategy.P2P_POINT_TO_POINT` (`SERVICE_ID = "com.arshadshah.nimaz.sync"`). Handshake: sender advertises, receiver discovers → auto `requestConnection` → user confirms via `authenticationDigits`. State is `StateFlow<ConnectionState>` (sealed). Payloads multiplex over one channel: a `0x1F` prefix (GZIP magic byte) distinguishes compressed **data** from `SyncSignal` JSON; **BYTES** is used when ≤ 31 KB, else a **FILE** payload (`cacheDir/sync_export.gz`). `SyncSignal` (sealed) carries control messages (`Ready`/`Cancel`/`ImportStarted`/`ImportProgress`/`ImportComplete`/`Ack`) so the sender mirrors the receiver's import progress.

**Conflict handling.** `SyncDataImporter` does a per-table, keyed **last-write-wins** merge (compare `updatedAt`, preserve local row IDs); name/prophet favorites are insert-if-absent. Never a blind overwrite. `Json { ignoreUnknownKeys = true }` tolerates app-version skew.

**Wiring.** No DI module — all three classes are `@Singleton @Inject constructor`; the Nearby client is `by lazy { Nearby.getConnectionsClient(context) }`. Reached from `SyncScreen.kt`.

**Gotchas.**
- Runtime permissions required (`AndroidManifest.xml`): `ACCESS_FINE_LOCATION`, the `BLUETOOTH_*` set, `ACCESS_WIFI_STATE`, `NEARBY_WIFI_DEVICES` — without them advertising/discovery fails as `ConnectionState.Error`.
- `P2P_POINT_TO_POINT` is strictly one-to-one; the manager's single-slot callbacks assume one sync at a time.
- The 31 KB BYTES threshold and the `0x1F` data/signal prefix are load-bearing — changing compression breaks disambiguation.
- `appVersion = 11` is hardcoded in the exporter and not validated on import (skew tolerated via `ignoreUnknownKeys` + field-level merge).

---

## 11. Content sharing

`core/share/` — the **single, branded path** every feature uses to share content through the
system share sheet. Before this existed, ~13 call sites each hand-rolled their own
`Intent(ACTION_SEND)`, rebuilt the share-body string, and passed a hardcoded chooser title;
now no screen constructs a share `Intent` directly (enforceable: `grep -rn "ACTION_SEND\|createChooser\|ACTION_SENDTO" app/src/main/java` should only hit `core/share/`).

| File | Role |
|---|---|
| `core/share/Shareable.kt` | `data class Shareable(plainText, subject?, card?)` — domain-agnostic description of something to share. `ShareCard` is the structured payload (eyebrow / arabic / body / transliteration / attribution) for the branded image. |
| `core/share/Shareables.kt` | `object` factory — the **one** place each content type's share-body string is built (`ayah`, `favorite`, `hadith`, `dua`, `bookmark`, `appInvite`, `text`). Takes `Context` so bodies/attribution stay localized. |
| `core/share/ContentShareManager.kt` | `object` entry point. `shareText` (text/plain), `shareFile` (FileProvider + grant flag; PDFs & images), `sendEmail` (`ACTION_SENDTO` `mailto:`), and `shareBranded` (suspend — render card → PNG → `shareFile`, text fallback). Owns MIME, `EXTRA_SUBJECT/TEXT`, and the localized chooser title (`R.string.share_chooser_title`). |
| `core/share/ShareCardRenderer.kt` | Draws a `ShareCard` into a teal/gold **Nimaz-branded PNG** (Amiri Arabic, wordmark, app-icon monogram) via `Canvas`, written to the `exports/` cache dir. Deliberately mirrors the PDF exporters' visual language. |

**Branded image path.** `ayah`, `favorite`, `hadith` and `dua` carry a `ShareCard`, so their
share button calls `ContentShareManager.shareBranded(...)` from a `rememberCoroutineScope()` —
the bitmap is rendered on `Dispatchers.Default`, then shared as `image/png` with `plainText` as
the caption/fallback; if rendering fails it degrades to `shareText`. Bookmarks, the app-invite and
the contact-us email are text/`mailto:` only.

**File sharing.** `shareFile` centralizes the `FileProvider.getUriForFile(context, "${packageName}.fileprovider", file)` + `FLAG_GRANT_READ_URI_PERMISSION` wiring that
`TafseerPdfExporter` and `PrayerTimesPdfExporter` used to each duplicate in their own
`buildShareIntent`. Those exporters now only *produce the `File`*; the caller passes it to
`ContentShareManager.shareFile(context, file, "application/pdf")`. All shared files (PDF + share
PNGs) live under the `exports/` cache dir exposed by `res/xml/file_paths.xml`.

**Wiring.** Plain `object`s (no DI), matching the existing `core/util` exporters — call sites use
`LocalContext.current`. `shareBranded` is `suspend`; every branded call site launches it from a
composable coroutine scope.

**Gotchas.**
- `ShareCardRenderer` measures then draws in one `draw(canvas)` walk (null canvas = measure pass) so the bitmap height can't drift from the content; Arabic/body text is length-capped for the card, but the full text always survives in the `plainText` fallback.
- Chooser title is a single shared `R.string.share_chooser_title` — the old per-feature titles (`share_hadith`, `dua_reader_share`, `tafseer_share_chooser`) are no longer wired to the chooser.

---

## Keeping this doc updated

This file is a **living map of the subsystems**, not a one-time snapshot. When you change a
subsystem — add/rename a Worker or service, change a notification channel or alarm scheme, add
a migration (and bump `NIMAZ_DATABASE_VERSION`), add a content seeder or content-version key, change a
DataStore key surface, alter the sync payload/protocol, or swap a monitoring backend — **update
the corresponding section here in the same change**, and add a row to the relevant table. Keep
claims grounded in the code (read the file, cite the path in backticks). If a subsystem grows
large enough to warrant its own doc, link it from here rather than letting this overview drift.
