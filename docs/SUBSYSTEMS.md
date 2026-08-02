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
12. [Engagement announcements (FCM)](#12-engagement-announcements-fcm)
13. [Keeping this doc updated](#keeping-this-doc-updated)

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

**Media3/ExoPlayer specifics (Quran).** Ayahs are downloaded first then added as a `List<MediaItem>` for gapless sequential playback. ExoPlayer reports `0` duration for unloaded items, so durations are pre-extracted with `MediaMetadataRetriever`; a `ForwardingPlayer` (`getPlayer()`) translates per-item ExoPlayer position/duration into **whole-surah** ("total playlist") coordinates so the lock-screen scrubber reflects the surah, not one ayah. Recitations stream from `cdn.islamic.network`, cached under `filesDir/quran_audio/`. **Who** the reciters are is the `QuranReciter` catalogue in `domain/model/QuranReciter.kt` (frozen `id` + `aliases` for ids older builds persisted, display name, country, `RecitationStyle`); only the CDN wiring — which edition slug at which bitrate — stays in the data layer, as `RECITER_CDN_MAP`, now keyed by the enum. Before that the catalogue existed three times over (a `popularReciters` list in `SelectReciterScreen`, the map plus a `getReciterDisplayName` `when` in `QuranAudioManager`, and a third `when` in `QuranSettingsScreen`) and they disagreed: the settings row matched on ids the picker never writes, so eight of the ten reciters left it showing a raw id ("hussary") instead of a name. Three reciters that had working CDN editions and display names but were missing from the picker's hardcoded list — Muhammad Ayyoub, Muhammad Jibreel, Abdullah Basfar — are selectable now that one list drives everything. Adhan files cache under `filesDir/adhan/`, Qaida clips fall back to the bundled asset `file:///android_asset/qaida/audio/{key}.mp3`.

**How ViewModels consume it.** ViewModels inject the manager **directly** and re-expose its flow (e.g. `QuranViewModel.audioState = audioManager.audioState`; `QaidaReaderViewModel.audioState = audioManager.state`; `SettingsViewModel` injects `adhanAudioManager` for preview/download). They drive playback via `onEvent`. *Deviation:* this bypasses the "ViewModels inject `XxxUseCases`" rule (§ARCHITECTURE), and `QuranViewModel` even exposes the manager as a public field — a known clean-architecture deviation, not a pattern to copy.

**Gotchas.**
- Two player APIs: ExoPlayer everywhere **except** `AdhanAudioManager.preview()` (legacy `MediaPlayer`).
- `QuranAudioManager.stop()` deliberately does **not** send a stop intent to the service — it sets `isActive=false` and lets the service's state-observer self-stop after a 500 ms debounce, avoiding a start/stop race.
- Adhan downloads are heavily validated (MP3/WAV magic bytes, content-type, min size, URL-version invalidation via `ADHAN_URL_VERSION`) because the external CDNs sometimes serve HTML error pages; on corrupt/missing files playback falls back to the generated beep, **never** to the wrong adhan variant.
- `SIMPLE_BEEP` is **synthesized locally** (`generateBeepSound` → WAV), not downloaded.

---

## 2. Glance widgets

All in `widget/`. Six Jetpack **Glance** AppWidgets, each in its own subpackage, plus a
shared `widget/core/` package and two top-level helpers.

| Widget | Package | Refresh | State type |
|---|---|---|---|
| Next Prayer | `widget/nextprayer/` | Worker 15 min + AlarmManager 1 min tick | `NextPrayerWidgetState` |
| Prayer Times | `widget/prayertimes/` | Worker 15 min + 1 min tick | `PrayerTimesWidgetState` |
| Prayer Tracker | `widget/prayertracker/` | Worker 30 min + immediate on toggle | `PrayerTrackerWidgetState` |
| Hijri Date | `widget/hijridate/` | Worker 6 hr | `HijriDateWidgetState` |
| Hijri Calendar | `widget/hijricalendar/` | Worker 6 hr | `HijriCalendarWidgetState` |
| Khatam | `widget/khatam/` | Worker 30 min | `KhatamWidgetState` |

Each widget = a `GlanceAppWidget` subclass (`provideGlance` → `provideContent { GlanceTheme { … } }`, reads `currentState<T>()`) + a `GlanceAppWidgetReceiver` (the manifest-registered `BroadcastReceiver`; `onEnabled` starts refresh, `onDisabled` cancels). State is a `@Serializable sealed interface` with `Loading`/`Success(data)`/`Error(message)`. Colors come from `res/color` via `ColorProvider(R.color.widget_*)` — no hardcoded colors.

**Data access — two patterns.**
1. **`@HiltWorker` injection (main path).** Workers inject real deps directly (e.g. `NextPrayerWorker` injects `PrayerTimeCalculator` + `PreferencesDataStore`; `PrayerTrackerWorker` injects `PrayerDao`; `HijriDateWorker` + `HijriCalendarWorker` inject `PreferencesDataStore` to read the `hijriDayOffset`). Each `doWork()` returns `Result.success()` early if no widgets are placed, computes fresh data, persists via `setWidgetState(...) → Success`, and on failure persists `Error` + `Result.retry()` for the first 3 attempts. This only works because `NimazApp` provides the `HiltWorkerFactory` (§3).
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
text colour so the text stays legible in both light and dark mode. The **Khatam**
widget is an editorial stat card: a name eyebrow, a gold **juz medallion** (a
`cornerRadius`-circled `Box` on `widget_gold_container` with the juz number in
`widget_on_gold_container`), an ayahs-remaining line and a "N/day · M-day streak"
pace line, closed by a progress rule with the percentage in `widget_gold`. Glance
cannot draw the app's serpentine juz trail (no canvas), so the medallion carries the
"where am I" glance and the numbers fill what used to be a near-empty card. The
`" · "` separator is a middot (U+00B7), which sits outside the guard's forbidden
range — emoji such as 🔥 are **not** allowed and would fail the build. A JVM
regression test (`WidgetGlyphGuardTest`) fails the build if any widget source
reintroduces a forbidden glyph. Widgets with a static `previewLayout` (Khatam) keep
a hand-authored XML mirror of the runtime layout in `res/layout/` for the picker.

**Localization.** Prayer names and weekday captions come from resources, resolved **at render
time** rather than baked into the persisted widget state — a language change then lands on the
next redraw instead of waiting up to 30 minutes for the worker. `WidgetUi.prayerShortNameRes` /
`Context.prayerShortName` map a name onto `widget_prayer_short_*` (returning null for anything
that is not one of the five daily prayers or Sunrise, so an unknown string is shown raw rather
than mislabelled as Dhuhr), and `weekdayInitials(locale)` derives the Hijri grid's Sunday-first
column captions from CLDR. `NextPrayerData.prayerName` deliberately stays the **canonical
English** name: it is the key both `prayerIconRes` and `prayerShortName` look up, so localizing
it in the worker would break the icon. `NextPrayerData.isTomorrow` replaces the literal
`"Tomorrow"` that used to be stored as display text. Guarded by `WidgetPrayerNameTest`, which
scans `widget/` for English prayer/weekday literals outside comments.

**Manifest/res.** Six `<receiver>`s + the non-exported `WidgetTickReceiver` in `AndroidManifest.xml`; provider-info XMLs in `res/xml/*_widget_info.xml`.

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
adhan playback, pre-prayer reminders, a nightly daily summary, a **Khatam daily reminder**,
and re-scheduling on midnight rollover and boot.

**Key files.**
- `core/util/PrayerNotificationScheduler.kt` — `@Singleton`; schedules/cancels alarms, owns the channels.
- `core/util/BootReceiver.kt` — `@AndroidEntryPoint BroadcastReceiver`; fires for **all** alarms and actually posts notifications / triggers adhan.
- `core/util/NotificationContentHelper.kt` — pure title/message/summary text generator.
- `data/audio/AdhanPlaybackService.kt` — plays the adhan and posts the merged prayer+adhan notification (§1).

**Channels** (created in `PrayerNotificationScheduler.init`, API 26+): `CHANNEL_ID_PRAYER` = `prayer_notifications` (HIGH), `CHANNEL_ID_ADHAN` = `adhan_notifications` (HIGH), `CHANNEL_ID_DAILY_SUMMARY` = `daily_summary_notifications` (DEFAULT), `CHANNEL_ID_KHATAM` = `khatam_notifications` (DEFAULT — a nudge, not an alarm; it is also the only channel whose name/description come from string resources rather than English literals), plus no-vibration siblings `CHANNEL_ID_PRAYER_SILENT` / `CHANNEL_ID_ADHAN_SILENT`. **Vibration is a channel property** — Android ignores `enableVibration()` changes after a channel exists, so the `notificationVibration` preference is honoured by *posting on the matching channel* via `channelForPrayer(vibrate)` / `channelForAdhan(vibrate)`, **not** by per-notification `setVibrate` (that's kept only as the pre-O fallback). `AdhanPlaybackService` also creates `adhan_playback_channel` but **posts on `CHANNEL_ID_ADHAN`** — so the playback channel is effectively unused for the visible notification.

**Scheduling.** `scheduleTodaysPrayerNotifications(...)` cancels everything then re-arms enabled prayers, using `setExactAndAllowWhileIdle(RTC_WAKEUP, …)` with `PendingIntent.getBroadcast` targeting `BootReceiver` (explicit intent). Request codes: prayer `1000 + ordinal`, pre-reminder `2000 + ordinal`, midnight reschedule `9999` (00:01), daily summary `8889` (23:00), Friday reminder `8890`, Khatam reminder `8891`. Pre-reminders fire at `prayerTime − preReminderMinutes` (skipped for Sunrise); the lead time is **user-editable** (the pre-adhan stepper → `SetReminderMinutes`). The **Friday (Jummah) reminder** (`scheduleFridayReminder`, gated on `fridayReminderEnabled`) is a one-shot at the upcoming Friday's Dhuhr − `fridayReminderMinutes`, re-armed on every reschedule so it always targets the next Friday.

**Firing.** `BootReceiver.onReceive` dispatches on action: boot → reschedule; `ACTION_MIDNIGHT_RESCHEDULE` → mark missed prayers + reschedule (self-perpetuating daily chain); `ACTION_PRAYER_NOTIFICATION` → post notification &/or play adhan; `ACTION_DAILY_SUMMARY` → summary; `ACTION_FRIDAY_REMINDER` → post the Jummah reminder; `ACTION_KHATAM_REMINDER` → post the Khatam nudge. If adhan should play and the file exists, it calls `AdhanPlaybackService.playAdhan(...)` and the service's foreground notification **doubles as** the prayer notification (shared id `prayerName.hashCode()`); if the file is missing it triggers a download for next time and falls back to beep. **Do Not Disturb:** when `adhanRespectDnd` is on and the system is in a DND mode, `dndBlocksAdhan` gates only the **adhan audio** (`shouldPlayAdhan`/`shouldPlayBeep`) — the visual prayer notification is still posted, and the OS silences its channel sound under DND. The Friday reminder (no adhan audio) always posts and is likewise silenced by the OS under DND.

**Khatam daily reminder.** `scheduleKhatamReminder()` is a one-shot at the user's stored
`khatamReminderTime` ("HH:mm", default 06:00), gated on `khatamReminderEnabled`. Like every
other alarm here it is armed **inside `scheduleTodaysPrayerNotifications`**, which is what keeps
the midnight chain and the boot path re-arming it — a reminder scheduled outside that call would
fire once and never again. `BootReceiver.handleKhatamReminder` re-checks the preference at fire
time, **returns without posting when no khatam is active**, and derives the day's target from
`KhatamProgressCalculator`. It **re-applies the saved locale as its first statement**: below API
33 the per-app locale is process-local and applied asynchronously by `AppInitializer`, so an
alarm firing in a cold process would otherwise resolve strings in the *system* language. Settings
live in Notification settings (toggle + 24-hour time picker).

**App Bundle language splits are disabled** (`android { bundle { language { enableSplit = false } } }`
in `app/build.gradle.kts`). Settings lets the user pick an app language independently of the device
locale (`core/util/LocaleHelper.kt`), but Play's default language splitting only delivers the
resources matching the *device* locale — so on a Play install every other language would silently
fall back to English. Disabling the split ships all locales in the base APK. This never reproduces
on a locally built APK, only on an Play-installed build, so **do not re-enable it** without moving
to Play Core's on-demand language download.

**Extended worship reminders (epic #300).** Optional, **off-by-default** Sunnah/fasting nudges —
Tahajjud, Witr, Suhoor, Iftar, Taraweeh, Laylatul Qadr, morning/evening adhkar, Mon/Thu &
White-Days & Arafah/Ashura fasting. Data-driven off the `WorshipReminderType` enum
(`domain/model/WorshipReminder.kt`) — **not** added to `PrayerType` (derived times, not fard
prayers). The pure `WorshipReminderCalculator` (`core/util/`, JVM-tested) computes each type's
**next** occurrence from prayer times + adhan2 `SunnahTimes` (exposed via
`PrayerTimeCalculator.getSunnahTimes`) + the Hijri calendar; `PrayerNotificationScheduler`
`scheduleWorshipReminders(...)` loops all enabled types and arms one alarm each
(`ACTION_WORSHIP_REMINDER`, request-code block **9000+ordinal**), **inside
`scheduleTodaysPrayerNotifications`** so the midnight/boot chain re-arms them daily. They post on
the DEFAULT-importance `worship_reminders` channel (`CHANNEL_ID_WORSHIP`) via
`BootReceiver.handleWorshipReminder` (re-checks the per-type pref + re-applies the saved locale),
with copy from `WorshipReminderContent`. Prefs are generic dynamic keys
(`worship_<key>_enabled` / `_offset` / `_mode`) on `SettingsRepository`/`PreferencesDataStore`. The
notification settings screen is now a **hub** (`NotificationSettingsScreen`, #301) linking to focused
subscreens — `PrayerNotificationsScreen`, `WorshipRemindersScreen`, `NotificationWeeklyScreen`,
`NotificationSoundScreen`, `NotificationTroubleshootingScreen` (all new `Route`s), each rendering a
slice of the shared `SettingsViewModel` state. Settings live
on the dedicated `WorshipRemindersScreen` (`Route.SettingsWorshipReminders`), and the Home
"Next Worship" card (`WorshipEventCard`, fed by `NextWorshipResolver`) surfaces the nearest enabled
one. Ramadan-category reminders are Ramadan-gated and their settings group auto-hides outside
Ramadan.

> **Occurrences are windows, not instants.** `WorshipReminderOccurrence` carries an optional
> `[windowStart, windowEnd)` span; `isActiveAt(now)` is true while `eventAt ≤ now < windowEnd`, and
> `isLiveAt(now)` means *upcoming or active*. This closed a shipped bug: with the old
> `triggerAt.isAfter(now)` gate an occurrence became "spent" the instant its trigger passed, so its
> next occurrence jumped ~24 h out and fell off the resolver's 14 h near-window — leaving the
> **opposite** adhkar card on Home all day (and hiding Iftar at Maghrib, Tahajjud at the last third).
> `nextOccurrence` now accepts an occurrence that `isLiveAt(now)`, and `NextWorshipResolver.nearest()`
> keeps any *active* occurrence regardless of the near-window and ranks active above upcoming.
> Windows come straight from `DayWorshipTimes`: night types (Tahajjud, Taraweeh, Laylatul Qadr,
> eve-of fasts) close at the **next** day's Fajr (`timesFor(day.plusDays(1))?.fajr` — never
> `t.lastThirdOfNight`, which would zero-length Tahajjud's window); Iftar spans Asr→Isha; Suhoor is a
> hard stop at Fajr. **Adhkar close is a religious-content choice** — the accommodating view ships
> (morning→Dhuhr, evening→Isha); the strict sunrise/Maghrib bounds are a natural future
> `worshipReminderMode`. **Witr is deliberately left instantaneous**: its `eventAt` is the same
> morning's Fajr (a pre-existing eventAt/triggerAt inconsistency scoped out of this fix), so a night
> window would falsely mark a spent occurrence active. `timesFor` in `nearest()` is memoised per call
> (`mutableMapOf<LocalDate, DayWorshipTimes?>`) since each night type now also asks for the next day's
> Fajr. Covered by `WorshipDayWalkTest` (a zone-independent hour-by-hour walk of a synthetic day).

> **Scheduler must arm a *future* trigger only (`requireFutureTrigger`).** The `isLiveAt(now)`
> acceptance above is what the Home card wants, but the *scheduler* shares the same `nextOccurrence`
> — and `AlarmManager.setExactAndAllowWhileIdle` at a **past** instant fires *immediately*. So
> accepting an already-*active* occurrence (trigger passed, window still open) made
> `scheduleWorshipReminders` re-post the worship notification on **every** reschedule, i.e. every
> time the app was opened during the event's window. `nextOccurrence(requireFutureTrigger = true)`
> (passed only by the scheduler; the resolver keeps the default) demands `triggerAt.isAfter(now)`,
> skipping an active occurrence and rolling forward to the next future trigger. Regression-covered in
> `WorshipReminderCalculatorTest`.

> **Home refresh cadence — time is derived, not pushed.** ViewModels publish *facts* (prayer
> `Instant`s); everything clock-derived is computed in the composable from one shared ticker.
>
> `presentation/components/atoms/NimazClock.kt` installs that ticker (`ProvideNimazClock` in
> `MainActivity`, inside `NimazTheme`). `rememberNow(resolution)` reads it truncated to the caller's
> resolution, so a card counting whole minutes invalidates once a minute rather than 60×.
> `rememberCountdownTo` escalates to seconds only within `fineGrainedWithin` (default: the final
> quarter-hour).
>
> **Tick resolution must follow the *displayed* resolution.** These are two separate knobs and
> letting them disagree renders a digit the ticker never updates: the Home hero and `JumuahCard`
> showed seconds while ticking once a minute, so their seconds froze for 60 s and jumped — and
> because any unrelated recomposition refreshed them, the timers looked like they only moved when
> you navigated around the app. `NimazCountdownText` now derives `fineGrainedWithin` from its own
> `showSeconds` flag (`Duration.INFINITE` when true), so a seconds-showing countdown ticks at 1 Hz
> at any distance and a minute-granularity one stays cheap. Hand-rolled `rememberCountdownTo`
> callers must keep the two in step themselves.
>
> **The ticker is testable.** `ProvideNimazClock(timeSource = …)` takes the clock as a parameter
> (default `SystemTimeSource`), because with `Clock.System.now()` hardcoded nothing could assert
> that a timer ever advances — which is how a frozen countdown shipped unnoticed past a component
> suite that pins `mainClock.autoAdvance = false` and only ever checks the first frame.
> `NimazClockTest` drives a substituted source and covers: a countdown advancing with no external
> recomposition, seconds ticking hours from the target, minute-resolution readers *not* recomposing
> within a minute, one shared instant across consumers, and the no-provider fallback. Note the
> harness detail: under a manual clock you must `advanceTimeByFrame()` after `advanceTimeBy(…)`,
> since the `delay` resuming and writing state does not itself draw the frame that renders it.
>
> Removed by this migration: `HomeViewModel.startTimeUpdates()` (1s) and `startWorshipUpdates()`
> (60s), `PrayerTimesViewModel.applyTick()` (1s), the `HomeHero` 30s clock loop, the `WidgetsScreen`
> 1s preview loop, and all four `private var use24HourFormat` mirrors plus their `observeTimeFormat()`
> collectors (Home, PrayerTimes, MonthlyPrayerTimes, Fasting). Times format at the leaf from
> `LocalUse24HourFormat` (`clockTimeText`/`NimazClockText`), so the 12/24-hour toggle is a pure
> recomposition instead of two astronomical passes.
>
> `PrayerTimeDisplay` carries `timeAt: Instant`; `List<PrayerTimeDisplay>.withClockState(now)`
> re-derives `isPassed`/`isCurrent`/`isNext`, and `core/util/PrayerClock.kt` holds the pure
> `nextPrayerIndexAt` / `currentPrayerIndexAt` / `prayerTimelineProgressAt`. This also fixed a real
> bug: the old derivation compared `LocalTime` (dropping the date), so after Isha no row highlighted
> and before Fajr today's Isha rendered as "current".
>
> **The worship card is a destination, not decoration.** It shipped inert — `WorshipEventCard`
> accepted an `onAction` that Home never passed, so the card counted down and then did nothing.
> The whole card surface is now the tap target (no CTA button: the carousel page is a fixed 170dp
> and a button would cost scarce height), with `onClickLabel` carrying the per-type action wording
> for screen readers. `core/navigation/WorshipDestinations.kt` maps type → route as a **pure
> function**, so every row is asserted in a JVM test rather than only through the UI; a new
> reminder type is a compile error rather than a silently inert card. Nine of the eleven types land
> on screens that already existed (dua categories, the fast tracker); Tahajjud and Witr open
> `Route.NightWorship`, built because those two had nowhere useful to go.
>
> Two things still need a schedule rather than a tick. **Date rollover** is watched by `HomeScreen`
> off the ticker's local date (which also covers timezone and manual time changes) and fires
> `HomeEvent.RefreshPrayerTimes`. **The worship card** re-resolves on an event-driven sleep until the
> current occurrence's window closes — one wake per transition instead of 1,440 polls a day, each
> costing ~30 DataStore reads — guarded end-to-end so nothing escapes to the uncaught handler.

**Wiring.** `PrayerNotificationScheduler` is constructor-injected (`@Singleton @Inject`, deps: `PrayerTimeCalculator`, `SettingsRepository`). Called by `AppInitializer` on startup and by `SettingsViewModel.rescheduleNotifications()` when prayer/notification settings change. Permissions in `AndroidManifest.xml`: `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.

**Gotchas.**
- Uses `USE_EXACT_ALARM` (auto-granted, alarm-clock class app) and does **not** check `canScheduleExactAlarms()` or catch `SecurityException`.
- **Doze / battery optimization** is the main "fired late" failure mode; the code measures `deliveryLatencySeconds` for telemetry but doesn't request the battery exemption itself.
- `POST_NOTIFICATIONS` (API 33+) isn't checked before `notify()` — denied → silent no-op.
- Channels exist only once the scheduler singleton is first instantiated by DI.
- `BootReceiver` handles `LOCKED_BOOT_COMPLETED`/QUICKBOOT in code but only `BOOT_COMPLETED` has a manifest intent-filter.

---

## 5. Database & migrations

Two Room `@Database`es, both provided in `core/di/DatabaseModule.kt`:

- `data/local/database/NimazDatabase.kt` (`nimaz_database`, `NIMAZ_DATABASE_VERSION`) — shipped
  content. Read-only in practice and disposable: it arrives as a fetched artifact (§7) and is
  replaced wholesale by a release.
- `data/local/user/NimazUserDatabase.kt` (`nimaz_user_database`, `NIMAZ_USER_DATABASE_VERSION`) —
  everything the user made. Created by Room on the device, never shipped. Split out at content
  `schemaVersion 23`; `MIGRATION_22_23` is deliberately empty, because the old tables are **left
  in place** rather than dropped so the original rows stay recoverable.

**Legacy user-data import.** `LegacyUserDataImport` copies an existing install's rows out of the
content database into the user database the first time it is opened, driven by `UserDataMigrator`
from `AppInitializer` (§9) and awaited before the splash screen lifts. It is one transaction of
`INSERT OR IGNORE`s over two `ATTACH`ed files, so an interrupted copy leaves nothing half-written
and a second attempt is a no-op.

> **Never `ATTACH` on a connection Room owns.** This has now caused the same crash twice, by two
> different routes, and both are worth knowing:
>
> 1. Writing from a Room `Callback.onOpen` fires invalidation triggers before Room has created
>    `room_table_modification_log`. This is why `provideNimazUserDatabase` has no `addCallback`.
> 2. `SQLiteDatabase.execSQL` inspects every statement, and on the first `ATTACH` a connection
>    ever sees it clears `ENABLE_WRITE_AHEAD_LOGGING` and reconfigures the pool — which **closes
>    the primary connection and opens a new one**. Room builds its whole tracker in that
>    connection's temporary schema (`room_table_modification_log` is a `CREATE TEMP TABLE`, the
>    per-table triggers are `CREATE TEMP TRIGGER`) and only does so when *it* opens a connection,
>    so after the swap they are gone for the life of the process.
>
> Either way the next Flow to start observing a table dies in `syncTriggers` with
> `no such table: room_table_modification_log`. The import therefore attaches **both** files to a
> throwaway in-memory database it owns and closes: an in-memory `main` has no journal mode to be
> taken out of and is capped at one connection anyway, so the reconfigure is a no-op and both
> real files keep the journal mode Room gave them. The cost of the separate connection is that
> Room does not observe the copy — hence the ordering guarantee above.

**Prepopulated DB.** The app ships a prebuilt DB in `app/src/main/assets/database/nimaz_prepopulated.db`, wired via `.createFromAsset("database/nimaz_prepopulated.db", NimazDatabase.PREPACKAGED_CALLBACK)`. **Room copies this asset only on a fresh install** — it is *not* re-copied on app update. That single fact drives both the migration discipline here and the entire content-seeding subsystem (§7).

**`PREPACKAGED_CALLBACK`** repairs the shipped asset right after copy and before Room validates its schema (the bundled asset was stamped at `user_version 12` while still missing the `updatedAt` columns and shipping tafseer indices under the wrong names). The same idempotent repair is also exposed as `MIGRATION_12_13`, because devices already sitting at v12 never re-run the copy callback.

**Tajweed data verification (issue #292).** The artifact's `ayahs.text_tajweed` column is built by the tajweed pipeline in **arshad-shah/nimaz-data** (`upstream/scripts/generate_database.py`), and verified there rather than here — `upstream/scripts/verify_tajweed.py` runs as a fail-the-build post-step, and the corpus rules engine (`data/rules/`) re-checks it on every `nz build`. The app-side `tajweed_data_checks.yml` was deleted at versionCode 385 (`docs/retirement.yaml`): it triggered on a `nimaz-pro-data/**` path that can no longer change in this repo. It asserts coverage, well-formedness (no leaked markup), the round-trip against `text_arabic`, the v3 code whitelist, character-coverage conservation, cross-source drift vs cpfair, and a golden-ayah fixture.

**Migration chain** (registered in `DatabaseModule.provideNimazDatabase`): `MIGRATION_7_8` (khatam) → `8_9` (asma/prophets) → `9_10` (a *missing* migration restored after the original release bumped the version without registering it) → `10_11` (`updatedAt` columns) → `11_12` (surah start_page fix) → `12_13` (legacy asset repair) → `13_14` (Help tables) → `14_15` (Qaida tables) → `15_16` (tasbih `category`) → `16_17` (hadith `narrator_chain`) → `17_18` (16-line IndoPak: `ayahs.text_indopak` column + `mushaf_layout_indopak16` table) → `18_19` (translations: dedupe + unique index on `(ayah_id, translator_id)`) → `19_20` (generalised mushaf storage: `mushaf_ayah_texts` + `mushaf_layout_lines`, drops `mushaf_layout_indopak16`) → `20_21` (tafseer range blocks: drops `tafseer_texts`, creates `tafseer_blocks`).

**Tafseer range blocks (`v21`, #329).** Tafseer is range-based, not ayah-based — a single commentary passage (e.g. Ibn Kathir discussing 43:81-89) is one block, not nine identical rows. `tafseer_texts` (one row per ayah, `ayah_id`/`surah_number`/`ayah_number`) is replaced by `tafseer_blocks` (`tafseer_id`, `surah_number`, `ayah_start`, `ayah_end`, `text`), indexed on `(tafseer_id, surah_number, ayah_start, ayah_end)`. `MIGRATION_20_21` drops the old table outright — it is shipped content, not user data, replaced wholesale by the schemaVersion 21 artifact (`nimaz-data` issue #1) — and creates the new one empty; the block rows arrive with that artifact. `TafseerDao.getTafseerForAyah(surahNumber, ayahNumber, tafseerId)` now matches by containment (`ayah_start <= ? AND ayah_end >= ?`) instead of equality. `tafseer_highlights`/`tafseer_notes` (user data) are untouched: they stay keyed by the single `ayah_id` they were made on — the offsets they store index into the block text, which is unchanged for that ayah — but the reader now gathers every highlight/note whose ayah falls inside the *displayed block's* range (`TafseerDao.getHighlightsForRange`/`getNotesForRange`, joined against `ayahs`) so an annotation shows whenever its block is on screen, not only on the exact ayah it was created on. `TafseerPageContent` renders a "Commentary on 43:81-89" header from the block's own range, and `TafseerViewModel` hoists the reader's content-page index into `TafseerUiState.currentTafseerPage` so swiping to the next ayah of the same block holds reading position instead of reopening the block from page 1.

**16-line IndoPak layout (`v18`, sub-task 2/7 of #263).** `MIGRATION_17_18` adds the nullable `ayahs.text_indopak` column and creates the `mushaf_layout_indopak16` table (columns `page`, `line`, `line_type` ∈ {`ayah`, `surah_header`, `basmalah`}, `surah_id`, `ayah_id` = global 1–6236 or null, `first_word_position`/`last_word_position`; indexed on `(page, line)`). The table stores the layout as **line segments** (one row per contiguous run of an ayah's words on a printed line, ~13,970 rows), not one row per word — the glyph text is reconstructed by slicing `text_indopak` (split on space) with the stored positions. The migration only creates the empty column/table (for both fresh installs and upgraders). At the time the data was **not** baked into the prepackaged DB — it shipped as bundled JSON and was seeded at runtime, because regenerating the then ~147 MB Git-LFS asset would have bloated it *and* never reached existing installs. That trade-off ended when the DB became a fetched artifact: the layouts ride in it, and the seeder retired at versionCode 385 (§7). See `docs/ARCHITECTURE.md` §9.

**Generalised mushaf storage (`v20`).** `v18`'s shape could only ever hold one edition — the table name and the `ayahs.text_indopak` column both hardcoded the 16-line IndoPak. `MIGRATION_19_20` replaces it with two script-keyed tables so **an edition is data, not schema**:
- `mushaf_ayah_texts(text_source, ayah_id, text)` — glyph text, PK `(text_source, ayah_id)`. A *text source* is the script an edition sets its words in, and editions that set identical glyphs **share** one: `INDOPAK_16` and `INDOPAK_15` are verified byte-identical across all 6,236 ayahs and both read `INDOPAK`, so the 15-line edition costs only its layout file. `INDOPAK_13` differs in the vowel marks of 28 ayahs and carries its own `INDOPAK_13`.
- `mushaf_layout_lines(script, page, line, line_type, surah_id, ayah_id, first_word_position, last_word_position)` — the same line-segment encoding as before, now with a `script` column; indexed on `(script, page, line)` and `(script)`.

The old table is dropped and `ayahs.text_indopak` is set to `NULL` (kept as an inert column — dropping one in SQLite means rebuilding a 6,236-row table for no functional gain). Nothing is lost: the dropped table held only derived content, which the artifact carries (it was repopulated by `MushafLayoutSeeder` from bundled assets until versionCode 385). `MigrationTest.migrate18To20_...` runs the real 18 → 20 path and validates the result against the exported v20 schema.

**Translation uniqueness (`v19`).** `translations.id` is auto-generated and had no uniqueness constraint, so a re-seed that inserted without deleting first would silently double every verse and the reader would pick an arbitrary copy. `MIGRATION_18_19` collapses any existing duplicates (keeping the lowest `id` per `(ayah_id, translator_id)`) and adds the unique index that makes the class of bug impossible.

**16-line IndoPak read path (sub-task 4/7 of #263).** The renderer needs a page grouped **by printed line**, not by ayah. `QuranDao.getMushafLayoutByPage(script, textSource, page)` LEFT-JOINs `mushaf_layout_lines` onto `mushaf_ayah_texts` (for the glyph text) and `ayahs` (for `number_in_surah`) and returns ordered `MushafLayoutLineRow` segments; `MushafLayoutMapper` (data layer, pure/Android-free) groups them by `line` and reconstructs each segment's glyph words by slicing that text with the stored `first/last_word_position`, yielding the domain model `MushafPageLayout(page, lines: List<MushafLine>)` where each `MushafLine` carries typed segments (`AYAH` words, or a word-less `SURAH_HEADER`/`BASMALAH` line + `surahId`). Ayah segments on one `line_number` concatenate into a single `AYAH` line, but each **structural** row (`SURAH_HEADER`/`BASMALAH`) maps 1:1 to its own `MushafLine` — even when the source data places a header and its basmalah on the *same* `line_number` (81 of the 112 basmalah-bearing surahs do; see the 7/7 verification note below). It surfaces through `QuranRepository.getMushafPageLayout` → `GetMushafPageLayoutUseCase` → `QuranViewModel` (`QuranEvent.LoadMushafPageLayout`, `QuranReaderUiState.mushafPageLayout` + the per-page `mushafPageLayoutCache`). Page-count totals are script-aware via `MushafScript` (`MADANI` = 604, `INDOPAK_16` = 548, `INDOPAK_15` = 610, `INDOPAK_13` = 847); `ReadingProgressCalculator.TOTAL_QURAN_PAGES` is single-sourced from `MushafScript.MADANI`.

**16-line IndoPak renderer (sub-task 5/7 of #263).** The line-accurate view is drawn by two Compose components, the counterparts to the default Uthmani `MushafContinuousText`/`MushafPage`:
- `presentation/components/molecules/MushafLineLayout.kt` (molecule) — draws **exactly the lines** of a `MushafPageLayout`, one row per printed line in `line_number` order, RTL. `AYAH` lines are justified to full width (`Arrangement.SpaceBetween`) except the page's last ayah line and a surah's last line (the one before a `SURAH_HEADER`), which sit at natural width; `SURAH_HEADER` lines render a bismillah-suppressed `SurahHeaderCartouche`; `BASMALAH` lines render a centred basmalah. Each ayah line **auto-fits its font down** (never above the requested size, via `rememberTextMeasurer`) so a dense line never overflows — the "fixed-fit" half of #269's fixed-fit-vs-reflow trade-off. Highlight and tap resolve **per word** (`MushafWord.ayahId`) because one printed line can span multiple ayahs. Tajweed colouring is *not* applied here (the layout carries only IndoPak glyphs, which have no per-letter tajweed spans — that path stays on `MushafContinuousText`). Because of this, the "Show Tajweed Colors" toggle in `QuranSettingsScreen` is **disabled with a reason** ("Available in the Madani layout") for any edition other than `MADANI`, rather than silently doing nothing (#293).

**Tajweed rendering (Uthmani path, #293).** `MushafContinuousText` and `QuranAyahItem` colour `text_tajweed` via `TajweedParser.parse`. Parsing is **hoisted out of the per-highlight/selection recomposition**: the continuous renderer parses each ayah once into a `Map<ayahId, AnnotatedString>` keyed only on `(ayahs, showTajweed, isDarkTheme, textColor)`, then the cheap assembly re-applies highlight/selection spans on top. The bismillah header is stripped from the parsed **segments** (`TajweedParser.stripLeadingPrefix`, matching on the canonical text since #290) so surah-opening ayahs don't render it twice. `TajweedParser` reports a malformed ayah to `CrashReporter` at most once per process (not once per recomposition × ayahs-on-page).

**Tap-to-explain + legend (#294).** Both Uthmani renderers pass `annotateRules = true` to `TajweedParser.parse`, which tags each coloured span with `RULE_TAG`; a `detectTapGestures` handler maps the tap offset to the rule code and opens `TajweedRuleSheet`. This is wired in **both** readers — the continuous `MushafPage` and the verse-list `QuranAyahItem` (the latter renders its own `TajweedRuleSheet` self-contained, so no callback threads through `QuranReaderScreen`). The full `TajweedLegendSheet` is reachable from `QuranSettingsScreen` **and** from the reader's own overflow menu (`QuranReaderScreen`, shown only when `showTajweed` is on). Rule **display names and explanations are localized**: `tajweedRuleName`/`tajweedRuleExplanation` (in `TajweedLegendSheet.kt`) resolve `R.string.tajweed_rule_<code>_name|_desc` per app language, falling back to the English baked into `TajweedParser.rules`. All 24 rules are translated into the 5 shipped locales (de/fr/id/ms/tr); the transliterated technical terms (Ghunnah, Idgham, Madd…) are shared, and only the plain-word names (Silent, Waqf sign) plus every explanation differ per locale.
- `presentation/components/organisms/MushafLinePage.kt` (organism) — hosts `MushafLineLayout` in the shared `QuranFrame` and layers on the identical interactions as `MushafPage` (tap → highlight + `AyahTooltip` → play/bookmark/favorite/copy/share/tafseer/khatam + `AyahTranslationBottomSheet`). Ayah *content* for the translation sheet / copy / share is resolved via an `ayahLookup(ayahId)` seam; when the host can't supply a full `Ayah`, the page reconstructs a minimal one from the layout so every id-only action still works.

The reader pager (`QuranReaderScreen`) selects the renderer per page through the private `ReaderMushafPage` helper: it draws `MushafLinePage` when `QuranReaderUiState.useLineAccurateLayout` is set (lazily loading each visible page's layout into `mushafPageLayoutCache`), otherwise `MushafPage` (lazily loading the page's ayahs into `pageCache`). Either way the helper owns the fetch and the loading state for the page it draws — see "How the reader fetches pages" below.

**Settings toggle & script-aware pagination (sub-task 6/7 of #263, #270).** The renderer choice is now user-selectable and persisted. `QuranReaderUiState.mushafScript: MushafScript` is the single seam: `useLineAccurateLayout` and `totalPages` are computed from it (`useLineAccurateLayout = mushafScript.isLineAccurate`; `totalPages = mushafScript.totalPages`). The old `use16LineLayout` boolean tested for one specific entry and stopped being equivalent the moment a second line-accurate edition existed. `QuranViewModel.observeQuranSettings` folds `SettingsRepository.quranMushafScript` (a `MushafScript`-name string, key `quran_mushaf_script`, default `MADANI`) into both `readerState` and `homeState`, so switching the setting reflows the reader live. `QuranReaderScreen`'s pager page-count and dual-page spread count derive from `state.mushafScript.totalPages`; the Quran-home "jump to page" validates against the same total. The control is a "Mushaf Script" dropdown in `QuranSettingsScreen`, driven from `MushafScript.entries` (so a new edition needs no screen change) via `SettingsEvent.SetMushafScript` → `SettingsRepository.setQuranMushafScript`. It stays **off by default** so the Uthmani/604 view is unchanged unless the user opts in. Deep-link page bounds (`announcementRoute`, `quran/page/N`) validate against `MushafScript.MAX_TOTAL_PAGES` (the largest edition) and the reader clamps to the active edition.

**Script-aware page↔ayah mapping (#325).** 6/7 made the page *count* script-aware but left every page→*content* mapping Madani-only, so selecting the 16-line layout still listed 604 tiles on the Page tab and pointed khatam page progress at the wrong ayahs. `MushafPagination` (domain, pure) is now the single source of truth: built from one ordered `List<PageAyahRange>` per edition, it answers `totalPages`, `rangeFor(page)`, `pageForAyah(id)`, `juzStartPage/juzEndPage/juzPages` and `juzForPage`. The ranges come from `QuranRepository.getPageAyahRanges(script)` — `ayahs.page` grouped for Madani, and `QuranDao.getLayoutPageAyahRanges(script)` (grouped over `mushaf_layout_lines`, header/basmalah rows excluded) for any line-accurate edition, seeded on demand and memoised per edition in the repository. `GetMushafPaginationUseCase` wraps it; `QuranViewModel.observeMushafPagination` re-derives it whenever `quranMushafScript` changes and publishes it as `QuranHomeUiState.pagination`, so the Page tab's tiles, its juz sections and header indices, the surah→page badges/ranges, the Juz tab's page badges, the surah list's juz badge and the Continue-Reading percentage all reflow live. Before an edition's ranges load, `MushafPagination.fallback(script)` serves the printed Madani juz table for `MADANI` and reports `isReady = false` for any other edition (the Page tab shows a spinner rather than printing wrong page numbers). Reader page *content* is script-aware too: `getAyahsByPage(page, translatorId, script)` resolves an IndoPak page through that edition's span and fetches it via `getAyahsByIdRange`, which fixes the page info bar, the ayah-action lookups and "mark this page read" for khatam — all of which previously read the unrelated Madani page. The per-page caches (`pageCache`, `mushafPageLayoutCache`) are cleared on a script change, since page *N* no longer holds the same ayahs.

**How the reader fetches pages.** The pager keeps the settled page *and* its neighbours composed (`beyondViewportPageCount = 1`, plus both halves of a dual-page spread), so several pages ask for content at once. Two rules follow, and both had to be fixed after the reader's blank-page reports:

- **One collector per page, not one per reader.** `QuranViewModel` keeps `pageJobs: Map<Int, Job>` for page loads, separate from the `contentJob` that surah/juz mode cancels on every load. When page loads shared `contentJob`, each pager page cancelled its neighbour's fetch and only the last page requested in a frame reached `pageCache`; the rest rendered an empty `MushafPage` — a blank Mushaf frame. It only showed on the ayah-flow editions (Madani), because the line-accurate ones render from `mushafPageLayoutCache`, whose loader never shared a job. A page already in flight is not re-requested, and `pageJobs` is cancelled wholesale when the pages stop being valid — a script or translation change (whose queries capture those as *parameters* at subscription, so a live collector would keep serving the old edition and re-populate the cache that was just cleared) or a switch back to surah/juz mode. A script change also re-issues the current page, so switching edition in the reader repaints instead of emptying it.
- **Composed ≠ current.** `QuranEvent.LoadPage` makes a page the one the reader is *on* (title, `ayahs`, the saved reading position, the target a settings change re-issues); `QuranEvent.PrefetchPage` only fills the cache. The pager's settle handler sends `LoadPage`; `ReaderMushafPage` sends `PrefetchPage` for whichever page it is drawing. Both renderers show a spinner while their page's content is missing from the cache, so an unfetched page reads as *loading* rather than as a blank page — an empty page that has actually been fetched still renders, framed and empty.

**Quran translations.** The app ships a catalogue of **15 translations across 11 languages**, defined once in `domain/model/QuranTranslation.kt` — the single source of truth, in the same spirit as `QuranArabicFont` for fonts. Each entry carries a frozen `id`, the translator's name and a `TranslationLanguage` (code, English + native name, `isRtl`).

- **Storage.** `translations(ayah_id, text, translator_id)`, with a unique index on `(ayah_id, translator_id)` since `v19`.
- **Delivery.** All 15 arrive in the content artifact as `tr.<id>` collections, 6,236 rows each, imported in **arshad-shah/nimaz-data** by `upstream/scripts/download_translations.py` from the Al Quran Cloud API; the importer hard-fails unless the upstream edition aligns with the corpus verse for verse, and each collection carries a `rows_min: 6236` floor. They used to ship as ~18 MB of bundled JSON seeded per translation on first read — that went with `QuranTranslationSeeder` at versionCode 385 (`docs/retirement.yaml`).
- **Seeding.** Lazy and per translation (§7): a translation's 6,236 rows are written the first time it is *selected*, not all 15 up front.
- **Selection.** `SettingsRepository.quranTranslatorId` (key `quran_translator_id`, default `sahih_international`). `QuranSettingsScreen` shows a single `NimazSettingsItem` row carrying the current translator; it navigates to **`Route.SelectTranslation`** → `SelectTranslationScreen`, built in the shape of `SelectReciterScreen` (search bar, "currently selected" hero card, grouped list). 15 translations across 11 languages therefore cost one row on the settings screen rather than ~23 inlined items, and the dedicated screen has room for the live preview below. Each list row passes `selected =` to `NimazMenuItem`, so the active translation is filled + checked in place — before that the list gave no indication of which of the fifteen you were on, and the only feedback for a tap was the hero card updating off-screen at the top.
- **Size.** `SettingsRepository.quranTranslationFontSize` (key `quran_translation_font_size`, default 16f). The Quran reader has always read it (`QuranViewModel` → `AyahItem.fontSize`) and `SettingsEvent.SetTranslationFontSize` has always persisted it, but until the settings screen was regrouped **no screen ever raised that event** — unlike the Dua and Hadith settings screens, which both had the slider — so Quran translation size was stuck at 16f for every user. It now sits in the Translation section, disabled while "Show Translation" is off.
- **Live preview.** Both the settings preview card and `SelectTranslationScreen`'s hero card render the *selected translation's actual text* for the same sample ayah (`SettingsViewModel.PREVIEW_AYAH_ID` = 1, the Bismillah, shared with the renderers via the `BISMILLAH_TEXT` constant). On the selection screen it updates as you tap down the list, so a translation can be judged by reading it rather than by its translator's name. `observeQuranPreviewTranslation()` watches the persisted preference and resolves it through `GetAyahTranslationUseCase`; `flatMapLatest` keeps a slow earlier load from overwriting a newer pick, which matters because the first read of a translation also seeds its 6,236 rows. A null result keeps the previous text rather than blanking the card.
- **RTL + script.** The catalogue includes Urdu, which needs two things the default body text does not give it:
  - *Direction* — `QuranAyahItem`, `AyahTranslationBottomSheet`, the settings preview and the selection screen all set `textDirection = TextDirection.Content`, resolving direction from the text rather than the app locale.
  - *Face* — the app's Latin body fonts (Outfit / Plus Jakarta) carry **no Arabic-script glyphs**, so an Urdu translation would fall back to whatever Naskh face the system happens to have. `res/font/noto_nastaliq_urdu.ttf` (Noto Nastaliq Urdu, variable weight) ships for this, exposed as `NotoNastaliqUrduFontFamily` and selected by `translationFontFamily(language)` in `presentation/theme/Type.kt` — keyed on `TranslationLanguage`, so a second Urdu translation needs no change. It only ever styles translation *prose*; the Quran's own Arabic keeps using the selected `QuranArabicFont`.
  - *One helper, not four copies.* Direction, face and leading are resolved together by
    `TextStyle.asTranslationText(language, fontSize = …)` (`theme/Type.kt`), and short **endonym
    labels** ("اردو" beside "Urdu" in the picker) by `TextStyle.asLanguageLabel(language)`. The
    renderers take a `translationLanguage: TranslationLanguage` rather than a bare
    `FontFamily?`, so a call site cannot pass the face and forget the leading — which is how the
    reader (2.1×/1.5× of font size) and the settings preview (a fixed 34sp/22sp) came to disagree.
    `QuranReaderUiState.translationLanguage` derives it once from the selected translator id.
    The endonyms were the last Arabic-script text still drawn in a Latin body font.
- **Adding one** is two steps — an entry in the Python `CATALOGUE` (then run the script) and a matching enum entry. `download_translations.py --check` fails the pair if they drift.

**Go-to-page and the reader pager now read the derived count (#325 follow-up).** `MushafPagination`'s
own KDoc names *the jump-to-page validation* and *the reader's pager bounds* as the two places
`MushafScript` was consulted for a raw page count — and those were the two the pass missed. The
Page tab's grid, juz sections and surah badges all read `state.pagination.totalPages` (derived
from the edition's real page ranges) while the jump field validated against
`state.mushafScript.totalPages` (the constant declared on the enum) and `QuranReaderUiState.totalPages`
bounded the pager the same way. The two disagree whenever an edition's data paginates differently
from its declaration, and always for a non-Madani edition before its ranges load — the window in
which the grid shows a spinner while the field happily accepted page numbers. `MushafPagination`
now owns `contains(page)` and `pageFromInput(text)` (trim → `toIntOrNull` → range check, so blank,
non-numeric, overflowing and out-of-range input all resolve to null), `QuranReaderUiState` carries
the same `pagination` object the home state does, and `totalPages` reads off it. The field also
shows the valid range and disables its go button instead of silently ignoring a bad number — the
old `if (page in 1..total) navigate(page)` did nothing at all outside the range, which is what made
a stale bound invisible. Pinned by `MushafPageInputTest`, whose last case scans the sources so no
consumer outside the domain layer can take a page bound from the enum constant again.

**Switching edition keeps the reader's place, not their page number (#325 follow-up).**
`reloadReaderContent` re-issued `loadPage(target.number)` after a script change — the same
integer against a repaginated Quran. Madani page 500 and 13-line IndoPak page 500 are hundreds of
ayahs apart, and Madani page 600 does not exist in the 548-page edition at all, so changing the
layout mid-read threw the reader onto unrelated text or onto a page that loads nothing and renders
blank. `MushafPagination.pageMatching(page, other)` resolves the page to an ayah in the old
edition and asks the new one which page carries it; surah and juz targets are still re-issued
as-is, since those numbers mean the same thing in every edition. Both mappings are resolved
inside `repaginate` rather than read off `QuranReaderUiState.pagination`: `observeMushafPagination`
writes that same field from a **separate collector** on the same preference, and the two are not
ordered against each other, so the "previous" mapping on state may already have been replaced —
which silently turned the remap into a no-op the first time it was wired that way. The repository
memoises a line-accurate edition's ranges, so resolving both is cheap. Pinned by
`MushafPageRemapTest` (the landed page must actually carry the ayah the source page opened with,
plus monotonicity across all 604 pages) and by `QuranViewModelPageLoadTest`.

**Juz ayah boundaries corrected (#325).** `KhatamConstants.JUZ_AYAH_RANGES` — the hand-maintained juz→global-ayah-id table the Juz tab's khatam rings read — had drifted: juz 7 was off by one and juz 15-30 were wrong by hundreds of ayahs (juz 30 started at 4090 rather than 5673 = An-Naba 78:1, claiming a third of the Quran). The Khatam detail screen was unaffected because `KhatamDao.observeJuzProgress` groups by the database's own `ayahs.juz` column, so the two surfaces disagreed. `KhatamJuzBoundariesTest` now re-derives every boundary from the 114 surah ayah counts and the classical juz start references, so the table cannot silently drift again.

**Khatam streak derived from read stamps, not the daily-log table.** `khatam_daily_log` is
written by exactly one function, `KhatamRepositoryImpl.logDailyProgress`, reached only through
`LogDailyProgressUseCase` — whose sole reference in the whole codebase is its own DI
construction in `RepositoryModule`. **Nothing ever called it**, so the table was empty on every
install, `KhatamProgressCalculator.currentStreak/longestStreak` always ran on an empty list, and
the "streak" stat on the Khatam detail screen and in `KhatamStats` was permanently 0. The data
was on disk the whole time: `khatam_ayahs.read_at` is stamped on every mark, is indexed, and
travels over sync as `SyncKhatamAyah.readAt` — but no query read the column.
`KhatamProgressCalculator.dailyLogsFrom(readAt, syncedLogs)` (pure domain) now buckets those
stamps by local start-of-day and unions them with whatever the table holds, taking `maxOf` per
day so a day present in both sources is not double-counted. `KhatamDao.observeReadTimestamps` /
`observeAllReadTimestamps` feed it from `observeDailyLogs` and `observeKhatamStats`. Deriving
rather than back-filling means the streak works for reading already done, not just for reading
done after the fix ships; the table is still read because a peer's daily-log rows can arrive
over sync without its ayah rows. Pinned by `KhatamProgressCalculatorTest`.

**Fidelity verification, tests & the basmalah fix (sub-task 7/7 of #263, #271).** The 7/7 pass validated the shipped layout line-for-line against the invariants any faithful 16-line IndoPak edition must satisfy and fixed one defect it surfaced:

- **Defect fixed — dropped basmalah on shared header lines.** The renderer's contract is that the header cartouche is drawn with its bismillah *suppressed* and the basmalah is its own centred line. But **81 of the 112** basmalah-bearing surahs ship the `surah_header` and `basmalah` on the *same* `line_number` (QUL folds the bismillah band onto the name banner; the remaining 31 give it a dedicated line). The old `MushafLayoutMapper` grouped strictly by `line` and kept only the first row's type, so those 81 pages rendered **header-only — the basmalah vanished**. `MushafLayoutMapper.toPageLayout` now emits every structural row as its own `MushafLine` (header then basmalah), so all 112 basmalahs render regardless of how the source packs them onto a line.
- **Data-fidelity suite** — `MushafLayoutFidelityTest` (unit, reads the shipped `assets/quran/*.json` directly) pins: 548 contiguous pages, 6,236 ayahs each laid out with **every word covered exactly once, in order, with no gaps/duplicates/reorders** (the core "no wrong line break loses text" guarantee), 114 headers, 112 basmalahs (all surahs except 1 and 9), ≤16 lines/page, and the 81-surah shared-line count; it also round-trips the real data through `MushafLayoutMapper` and asserts all 112 basmalah lines survive. `MushafLayoutMapperTest` adds the shared-header/basmalah regression cases; `MushafLinePageTest` (Robolectric) renders the organism and asserts the header, the standalone basmalah line, per-word ayah lines, and lines spanning two ayahs all draw.
- **Edge cases verified** — Al-Fātiḥah (p.1) and At-Tawbah (surah 9) carry no basmalah line; the decorative **opening two-page spread** (p.1 top-half, p.2 offset to line 10) is faithful, not a gap; short closing surahs (108/112/114) open with their header; RTL and font-down auto-fit hold; dual-page spread parity follows the script-aware 548-page count.
- **Known limitation** — the shipped glyph text and layouts carry **no sajda (۩) or rukūʿ (۞) glyphs or line types**, so the line-accurate views render the printed word glyphs faithfully but overlays no sajda/rukūʿ medallions (sajda metadata still lives on the ayah entity for the ayah-keyed reader). Tracked in `ARCHITECTURE.md` §9 Open.

The full per-page pass/fail sheet is generated at [`docs/quran/16-line-fidelity-sheet.md`](quran/16-line-fidelity-sheet.md).

**Ayah-number ornament (Uthmani/Madani path).** On the default (non-16-line) layouts the ayah number is *appended* by the renderer as an ornamental end-marker `﴿n﴾` (open bracket U+FD3F + Arabic-Indic digits + close bracket U+FD3E), built once in `presentation/components/atoms/QuranTextFormat.kt` (`appendAyahEndMarker`/`annotatedAyahEndMarker`) and reused by all three Madani render paths (`ArabicText.QuranVerseText`, the tajweed branch of `QuranAyahItem`, and `MushafContinuousText`). Those bracket glyphs exist only in the naskh faces (Amiri/Scheherazade); the IndoPak Nastaʿlīq font (`QuranArabicFont.INDOPAK`) has **no** glyphs for them — its own ayah numbers are Private-Use-Area ornaments baked into the IndoPak glyph text, which the Madani/Uthmani text never carries. So the marker span is **pinned to `AmiriFontFamily`** (the `markerFontFamily` default) instead of inheriting the selected verse font; without this pin, choosing the IndoPak font on the Madani layout dropped the marker to a missing glyph and the ayah number rendered as nothing. The 16-line renderer (`MushafLineLayout`) is unaffected — there the number is part of the word glyph text and `appendAyahEndMarker` is never called.

**Full-text search, shipped inside the artifact (#330, `nimaz-data`#7).** Arabic search returned
**zero results for every query**, always: `LIKE '%الله%'` matched nothing while الله is in 1,746
verses. The stored text is fully vocalised (`ٱلرَّحْمَٰنِ` is twelve codepoints where a keyboard
gives six) and the first letter of 77% of ayahs is U+0671 ALEF WASLA — a different *letter* from
U+0627, so stripping marks was never enough on its own. Nothing looked broken, because an empty
result list reads as "no results".

The index is **compiled into the content artifact** by `nimaz-data`'s build, never on the device:

- **Three tables, none of them Room entities.** `search_index` (contentless FTS4 over a folded
  `body`), `search_docs(docid, kind, ref, source)` — the join key, which a contentless FTS table
  cannot hold itself — and `search_meta`. `createFromAsset` passes tables Room does not declare
  straight through, so they arrive already built, change **no identity hash** and need **no
  migration**. That is what makes this affordable: the previous attempt built the index on-device
  over 200,000 rows at first launch.
- **FTS4, not FTS5.** FTS5 is an optional compile-time module AOSP has never enabled — `USING
  fts5` fails with *"no such module: fts5"* on a stock device. `SqliteFtsSupportTest`
  (instrumentation) records what each device actually supports; flipping the data repo's
  `search.flavour` would need that evidence.
- **One folding, two implementations.** `domain/search/ArabicSearchNormaliser` folds a typed
  query; `nimaz_data/normalise/arabic.py` folded the indexed text. They must agree exactly or
  every query matches nothing *and no test fails*, so both are held to the generated
  `app/src/test/resources/search/fold-fixtures.json` (`nz search fixtures`, exported by `nz app
  sync`). `search_meta.fold_version` is checked at runtime; a mismatch makes the app refuse the
  index rather than under-match silently.
- **Reading it.** `data/local/search/ContentSearchIndex` is the only reader, via `@RawQuery`. The
  query shape matters: `WHERE d.docid IN (SELECT docid FROM search_index WHERE … MATCH ?)` takes
  0.6 ms where the equivalent `JOIN` takes 595 ms, because the join lets SQLite drive from
  `search_docs` and interrogate a contentless index one docid at a time.
- **The `LIKE` queries stay, and are not dead code.** `createFromAsset` copies the artifact
  **once**, so an install made before the index shipped has no index and cannot get one without a
  reinstall. `QuranRepositoryImpl`, `HadithRepositoryImpl` and `DuaRepositoryImpl` ask
  `searchIndex.isAvailable()` and fall back to exactly the search those installs already had.
  `LocationDao`'s `LIKE` is untouched — a small user-facing table with no Arabic in it.

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

**Hijri date offset.** `hijri_day_offset: Int` (range −2 to +2, default 0) allows users to adjust the displayed Hijri date relative to the system calculation. Stored in `PreferencesDataStore`, read by both Hijri widgets (`HijriDateWorker`, `HijriCalendarWorker`) and passed to `HijriDateCalculator.today(offsetDays)` to compute today's Hijri date for event matching and display. Wired via the "Adjust Hijri date" stepper in `AppearanceSettingsScreen`.

**Mushaf script / layout.** `quran_mushaf_script: String` (a `MushafScript` enum name, default `MADANI`) selects the Mushaf edition the page reader renders — ayah-flow Uthmani/Madani (604 pages) vs a line-accurate IndoPak edition (16-line/548, 15-line/610 or 13-line/847; #270). Stored raw and mapped to the domain enum at the boundary (mirrors `quran_arabic_font`/`pattern_style`); read by `QuranViewModel` (drives `useLineAccurateLayout` + script-aware page counts) and written from the "Mushaf Script" dropdown in `QuranSettingsScreen`. Off by default. See §5 (16-line renderer).

**Aggregate.** `val userPreferences: Flow<UserPreferences>` maps a curated subset of keys into a top-level `data class UserPreferences(...)` for one-shot reads of the common cross-cutting settings (used by `SettingsViewModel`, `LocationViewModel`, `AppInitializer`, `BootReceiver`, `WidgetsScreen`).

**Bulk ops.** `clearAllData()`, `exportAllPreferences(): Map<String,String>`, `importPreferences(map)` — used by the sync subsystem (§10).

**The wire loses the type, so the type is declared.** The export flattens every value with `toString()`, so the payload is `Map<String,String>`. The import used to *guess* the type back from the shape of the value and substrings of the key name; DataStore keys are typed and reading one at the wrong type throws, so the six keys the heuristic missed — `tasbih_preset_seed_version`, `content_patch_version`, `ai_consent_timestamp`, `tasbih_selected_preset`, `current_location_id` (Long guessed as Int) and `tasbih_favorites` — did not merely import wrong, they **crashed on next read after any sync**. `tasbih_preset_seed_version` is read with `.first()` in `TasbihViewModel`'s init and `current_location_id` resolves the active location for prayer times.

`data/local/datastore/PreferenceCodec.kt` now holds the declared type of all 91 named keys plus three shape patterns for the runtime-composed `worship_<type>_{enabled,offset,mode}`. Sets are joined on the ASCII unit separator rather than `Set.toString()` (`[a, b]` cannot be split back safely), with the bracket form still accepted so payloads from older builds land. An unknown key from a newer sender is kept as a string rather than dropped. `onboarding_completed` is never imported. `PreferenceCodecTest` reads the key declarations straight out of `PreferencesDataStore.kt` and fails if the registry drifts from them, so a new preference cannot be added without registering its type.

**Wiring.** Provided in `core/di/DataStoreModule.kt` via `@Provides @Singleton`. (Minor: it already has an `@Inject constructor(context)`, so the explicit provider is redundant with constructor injection.)

It also stores the **content-version flags** that drive seeding — see §7.

---

## 7. Content seeding & versioning

**Why this exists.** The prepopulated DB (§5) is **not re-copied on app update**, and schema
migrations only create empty tables. So content that changes in an update would never reach
existing users. There is now exactly **one** mechanism for that: `ContentPatchSeeder`.

> **The six per-feature content seeders are gone.** `DuaContentSeeder`, `HelpContentSeeder`,
> `QaidaContentSeeder`, `HadithBackfillSeeder`, `MushafLayoutSeeder` and
> `QuranTranslationSeeder` — with about 31 MB of bundled JSON assets — were retired at
> **versionCode 385**. Each existed to carry content the prepopulated asset lacked; the fetched
> artifact (§5) now carries all of it at `schemaVersion 23`, so they had become no-ops.
> `app/src/main/assets/` is down to an empty `adhan/.gitkeep` as a result — no bundled content
> assets remain. See
> [`DATA_RETIREMENT.md`](DATA_RETIREMENT.md) and [`retirement.yaml`](retirement.yaml) for what
> was checked before each deletion.

**How content reaches a device now.**

| Path | Reaches | Mechanism |
|---|---|---|
| Fresh install | new users | `createFromAsset` copies the fetched, sha256-pinned artifact (§5) |
| Update | existing installs | `ContentPatchSeeder` applies the cumulative patch shipped beside it |

**`ContentPatchSeeder`** (`data/local/content/ContentPatchSeeder.kt`) is the generalisation of
the six. The patch is a **build output, not hand-authored**: `nz patch emit` diffs the published
baseline artifact against the current one, and `nz patch verify` applies the result to the
baseline and asserts every collection's content hash equals the target's — so a patch that does
not reconstruct the artifact cannot be published. Three properties make applying it safe:

1. **It cannot touch user data.** Ops are only emitted for declared content collections;
   `USER_TABLES` re-asserts that here rather than trusting it. Since `schemaVersion 23` user
   tables are not in this database at all (§5) — they live in `NimazUserDatabase`.
2. **It is cumulative from the baseline**, so which version a device upgrades from does not
   matter. Every op is an idempotent keyed write.
3. **It is version-gated** on `PreferencesKeys.CONTENT_PATCH_VERSION`, so the common case costs
   one DataStore read. A missing asset is not an error — a release with nothing to correct
   ships no patch.

> **IndoPak font (issue #267, 3/7).** The IndoPak glyph text embeds per-ayah number ornaments as
> Private Use Area glyphs (U+F500…U+F6FF) that only render in the matching face. That face is bundled
> at `app/src/main/res/font/indopak_nastaleeq.ttf` (*AlQuran IndoPak by QuranWBW* v2.100) and exposed
> as `QuranArabicFont.INDOPAK` in `presentation/theme/Type.kt`. Licence/attribution + release sign-off
> flag: `docs/FONT_LICENSES.md`.

**Adding or correcting content** is now a change in **arshad-shah/nimaz-data**, not here: edit
the collection, `nz build`, publish, then `nz app sync` updates `data.lock.json`. Nothing in
this repo carries content any more, so there is no `contentVersion` to bump and no asset to
regenerate. Adding a Quran translation additionally needs a matching `QuranTranslation` enum
entry with the same id — `nz import --check` fails if the two catalogues drift.

**Gotchas.**
- **Editing the artifact alone reaches fresh installs only.** Corrections that must reach
  existing users have to ride the patch — that is the whole reason it exists.
- Content tables carry no FK from user tables, and cannot: they are in a different database.
- Qaida `conceptTags` are stored JSON-encoded-as-string, a convention inherited from the
  prepopulated DB.
- `DeviceStateCorpusTest` is the standing guard that the artifact actually carries everything
  (§ "Verifying a retirement was safe" in `DATA_RETIREMENT.md`). It needs the fetched artifact,
  so it needs a data-repo credential; `compileDebugKotlin` does not.

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

**Hijri conversion** — `core/util/HijriDateCalculator.kt`, a stateless Kotlin `object` (no Hilt). It does **not** use `ummalqura`; it delegates to the platform `java.time.chrono.HijrahChronology.INSTANCE` (OS-updated Umm al-Qura). Provides `toHijri`/`toGregorian`, Ramadan helpers, validity checks, and a hardcoded Islamic-events calendar (`getIslamicEvents`/`getUpcomingEvents`). **Day-offset support:** `today(offsetDays = 0)` returns today's Hijri date adjusted by the user's `hijriDayOffset` preference (§6), used for local event matching and both Hijri widgets. Other `now()` helpers (`isTodayRamadan`, `daysUntilNextRamadan`, …) currently ignore the offset — see deferred follow-up in §9.

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

**`core/init/AppInitializer.kt`** — `@Singleton`. `initialize()` launches an IO coroutine that runs four tasks **in parallel under a 5 s `withTimeout`** (then proceeds to UI regardless): apply saved locale, schedule today's prayer notifications (§4), download the default adhan/beep if missing (§1), and bootstrap FCM announcements (§12 — create the Updates channel + topic subscribe). It exposes `val isReady: StateFlow<Boolean>` (the splash gate). Failures are reported to monitoring but never block startup.

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

**Conflict handling.** `SyncDataImporter` does a per-table **last-write-wins** merge on `updatedAt`. `Json { ignoreUnknownKeys = true }` tolerates app-version skew.

**Merge keys are natural, never the row id.** Every table below has an `autoGenerate` primary key, so two phones that have both been used each hold a row with id 1 — a collision is the ordinary case, not an edge case. Seven importers merged on that id, which silently **overwrote** an unrelated local record when the incoming one was newer, and silently **dropped** the incoming one when it was older. Each now matches on what actually identifies the record across devices, and anything genuinely new is inserted with `id = 0` so Room assigns a fresh local id rather than the sender's:

| table | merge key |
|---|---|
| khatams | `createdAt` (when the user started it) |
| khatam ayahs / daily logs | the parent's **remapped** local id — see below |
| tafseer highlights | `(ayahId, tafseerId, startOffset, endOffset)` — the span highlighted |
| tafseer notes | `(ayahId, tafseerId, createdAt)` |
| zakat history | `calculatedAt` |
| tasbih presets | `name` |
| tasbih sessions | `startedAt` |
| makeup fasts | `originalDate` (the missed day) |
| prayer records | `(date, prayerName)` — was already correct |
| fast records | `date` — was already correct |
| locations | `(latitude, longitude)` — was already correct |

`importKhatams` returns a **sender-id → local-id map**, and `importKhatamAyahs` /
`importKhatamDailyLogs` take it: without the remap those children were written with the
sender's `khatamId` verbatim, so another device's read ayahs attached to whichever local khatam
held that id and inflated its progress. A child whose parent is not in the map is dropped.

**Bookmarks and favourites are a union, not a race.** `bookmarks` holds one row per
(kind, target) with **two** independent flags (`bookmarked`, `favourite`) and a single
`updatedAt`. Gating the whole write on that one timestamp dropped whichever act happened
earlier — favourite on Monday, bookmark on Tuesday, sync, and the favourite is gone. The
payload carries no tombstones (it lists what the sender *has*), so a flag set on either side
stays set; the timestamp decides only whose note and colour win. The three name catalogues
(Asma ul Husna, Asma un Nabi, Prophets) share one `importNameBookmarks` helper and union the
same way — `SyncNameBookmark` has no `updatedAt` at all, so a union is the only merge
available. All of this is pinned by `SyncDataImporterBookmarkMergeTest` and
`SyncDataImporterIdentityTest`.

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

## 12. Engagement announcements (FCM)

**Pure FCM, no backend.** Announcements (feature nudges, changelog items, privacy/T&C notices)
are sent from the **Firebase console Notifications composer** as notification+data messages,
broadcast to topic **`announcements`**. No server, no Remote Config, no token storage. With no
message ever sent there is zero behaviour change — the feature is inert by default.

**Delivery model.**
- **App foreground** → `NimazMessagingService.onMessageReceived` fires: custom data →
  `AnnouncementPayloadMapper` → `Announcement` → persisted via `AnnouncementRepository`. **No
  system notification is posted**; the Home screen observes the repository and renders a
  dismissable `AnnouncementBanner`.
- **App backgrounded/killed** → the **OS** posts the tray notification itself (composer
  title/body) on the `nimaz_announcements` channel (manifest meta-data
  `default_notification_channel_id` / `_icon` / `_color`); `onMessageReceived` is **not** called.
  Tapping copies the custom data onto the launcher intent — `MainActivity.handleIntent` maps the
  extras, persists the announcement (banner shows on Home) and deep-links to its `route` if valid.
  *Accepted gap:* opening the app without tapping the notification shows no banner.

**Key files.**
- `data/announcement/NimazMessagingService.kt` — the app's **only** `FirebaseMessagingService`
  (`@AndroidEntryPoint`; parse-and-write only; `onNewToken` logs only).
- `data/announcement/AnnouncementPayloadMapper.kt` — `Map<String,String>`/intent-extras →
  `Announcement?`; null (never throws) on missing/blank required fields (`id`,`type`,`title`,`body`)
  or any malformed optional (`min_version_code`, `max_version_code`, ISO-8601 `expires_at`,
  `dismissable`).
- `data/announcement/AnnouncementBootstrap.kt` — per-launch channel create + idempotent
  `subscribeToTopic("announcements")`, called from `AppInitializer` (§9); no-ops when Firebase
  isn't initialized (no `google-services.json`).
- `data/local/datastore/AnnouncementLocalDataSource.kt` — own Preferences DataStore
  (`nimaz_announcements`): JSON-serialized current announcement + `dismissed_announcement_ids`
  string-set (dismissal is **permanent**; re-sending the same `id` never resurfaces).
- `data/repository/AnnouncementRepositoryImpl.kt` → `domain/repository/AnnouncementRepository`.
- `domain/model/Announcement.kt`, `domain/usecase/AnnouncementUseCases.kt` —
  `ObserveActiveAnnouncementUseCase` gates on dismissed (repo) + expiry + `versionCode` window;
  `ResolveAnnouncementRouteUseCase` classifies `route` into https-URL / allowlisted feature key / none.
- `core/navigation/AnnouncementRoutes.kt` — `announcementRoute(key)` resolves feature keys via
  **two tiers**: static allowlist (exact matches like `settings/appearance`, `tasbih/stats`)
  checked first, then parameterised grammar (e.g. `quran/surah/{1-114}`, `tafseer/{n}`,
  `prayer/tracker/{tab}`, `dua/category/{slug}`, …). Integer parameters are range-checked;
  malformed/out-of-range keys resolve to `null` (CTA hidden). URLs (https://) open via
  `ACTION_VIEW`. See [`NAVIGATION.md` announcement-route-grammar](NAVIGATION.md#announcement-route-grammar)
  for the complete grammar table.
- `core/di/AnnouncementModule.kt`; banner UI in
  `presentation/components/molecules/AnnouncementBanner.kt`, state in `HomeViewModel.announcement`
  (`StateFlow<AnnouncementUiState>`).

**Channel.** `nimaz_announcements` ("Updates & Announcements"), **IMPORTANCE_LOW** — visible,
silent, and strictly separate from the prayer/adhan channels (§4), which are never touched.

**Payload contract** (console → Additional options → Custom data): required `id`, `type`
(`feature|privacy|tos|changelog|celebration`), `title`, `body`; optional `cta_label`, `route` (allowlist key
or `https://…`), `min_version_code`, `max_version_code`, `expires_at` (ISO-8601 UTC),
`dismissable` (default `true`). Never use reserved keys (`from`, `message_type`, `google.*`,
`gcm.*`). FCM is not E2E-encrypted — public content only.

**Celebration type (new).** When `type = celebration`, the following **8 optional payload keys** 
are parsed and stored in the `Announcement` domain model + `AnnouncementEntity` (both via `AnnouncementPayloadMapper`):
`event` (key matching `CelebrationEvent` enum, e.g. `eid_al_fitr`), `arabic` (event name Arabic), 
`transliteration` (romanized name), `proof_ref` (Quranic/Hadith reference), `proof_text` (proof snippet), 
`cta2_label` (secondary CTA text), `route2` (secondary navigation destination), `starts_at` (ISO-8601 or 
Unix epoch ms; validates and gates display). All 8 keys are registered in `PAYLOAD_KEYS` alongside 
the existing keys. Malformed `starts_at` rejects the entire payload (mapper returns `null`). Missing/blank 
`event` is accepted (payload may be title/body only, no event type). Proof pairs (ref/text) drop both if 
only one is present (all-or-nothing). Celebrations are **excluded from the banner** — they render as 
`EventCard`s in Home's `EventsCarousel` instead, avoiding double-render against pushed announcements.

**Analytics.** `announcement_shown` / `announcement_cta_clicked` / `announcement_dismissed`
(helpers in `AppAnalytics`), on top of FCM's own delivery/open reports. A new event
`announcement_route_rejected` (params: `announcement_id`, `route`) is logged from `HomeViewModel`
when a non-empty announcement route resolves to `null` (e.g. unparseable key or integer
out of range); this tracks incomplete content or malformed payloads without coupling the domain
use case to analytics.

**Home event cards (local + pushed celebrations).** The Home screen's `EventsCarousel` displays celebration occasions from two sources, merged by `ObserveEventCardsUseCase`: 
1. **Local events** — `ObserveLocalEventsUseCase` matches the static `IslamicEvents.events` calendar against today's Hijri date (via `HijriDateCalculator.today(hijriDayOffset)`) and emits `CelebrationEvent.toOccasion()` presentation models. 
2. **Pushed celebrations** — FCM announcements with `type = celebration` are mapped to `EventOccasion` and merged by the use case; **pushed wins on same-event match** (by event key). 
The merge caps at 2 total cards. Celebrations are rendered as cards only — they are **never included in the dismissable `AnnouncementBanner`** to avoid double-rendering the same occasion.

**Gotchas.**
- The console can only send notification-bearing messages, so the foreground banner and the
  background tray are mutually exclusive surfaces per delivery (see accepted gap above).
- `POST_NOTIFICATIONS` denied → no tray in background; the banner still works for foreground
  receipt and notification-tap entry is simply never exercised.
- Old app versions ignore route keys they don't know — never serialize `Route` objects into
  payloads; extend the allowlist instead.

---

## Keeping this doc updated

This file is a **living map of the subsystems**, not a one-time snapshot. When you change a
subsystem — add/rename a Worker or service, change a notification channel or alarm scheme, add
a migration (and bump `NIMAZ_DATABASE_VERSION`), add a content seeder or content-version key, change a
DataStore key surface, alter the sync payload/protocol, or swap a monitoring backend — **update
the corresponding section here in the same change**, and add a row to the relevant table. Keep
claims grounded in the code (read the file, cite the path in backticks). If a subsystem grows
large enough to warrant its own doc, link it from here rather than letting this overview drift.
