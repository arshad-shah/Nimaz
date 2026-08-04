# Notifications rework — progress

Branch: feat/notifications-rework
Spec: docs/specs/notifications-rework/SPEC.md
Last updated: 2026-08-04 by session 1

## Status
Current phase: P1 — Shared component extensions
Awaiting: Arshad's approval of the P1 `NimazPickerItem` trailing-action shape and the
P3 storage/migration proposal (both are "propose before implementing" in the spec)
Blocked on: nothing

## Phases
- [x] P0  Orientation                      verified 2026-08-04
- [ ] P1  Shared component extensions
- [ ] P2  Hub reshape
- [ ] P3  Prayers screen
- [ ] P4  Adhan & sound
- [ ] P5  Worship reminders
- [ ] P6  Weekly
- [ ] P7  Diagnostics
- [ ] P8  Full-flow tests

## Decisions taken
- 2026-08-04 P0 verify holds. Six screens exist under
  `presentation/screens/settings/` — `NotificationSettingsScreen`,
  `PrayerNotificationsScreen`, `NotificationSoundScreen`, `NotificationWeeklyScreen`,
  `NotificationTroubleshootingScreen`, `WorshipRemindersScreen` — and each has a route
  (`Route.SettingsNotifications`, `SettingsNotificationsPrayers`, `SettingsNotificationsSound`,
  `SettingsNotificationsWeekly`, `SettingsNotificationsTroubleshooting`,
  `SettingsWorshipReminders`). No new route is needed.

## Open questions
- P1 `NimazPickerItem` trailing action shape — proposed to Arshad 2026-08-04.
- P3 alert-style + per-prayer offset storage and migration — proposed to Arshad 2026-08-04.

## Notes for the next session

**The spec is stale in three places — verified against the working tree, not assumed:**

1. **P1's `NimazListPicker` checkbox change is already done.** `PickerRow` already renders
   selection with `NimazCheckbox(type = CIRCLE, variant = PRIMARY, size = LARGE)`
   (`NimazListPicker.kt:302`), not a `NimazIcon`. Nothing to change; no picker screenshots
   needed for that item.
2. **P4's "Play at alarm volume" setting does not exist.** `grep` for
   `alarm volume|ALARM_VOLUME|STREAM_ALARM|alarmVolume` across `app/src/main` returns nothing.
   Per §8, it will not be invented. The prototype drew it; the app does not have it.
3. **"All six locales" means base + five.** `app/src/main/res/` has `values`, `values-de`,
   `values-fr`, `values-id`, `values-ms`, `values-tr` (`values-night` is a theme qualifier,
   not a locale).

**How prayer notification state is actually persisted (the P3 investigation):**

- `isSoundOn` in `PrayerNotificationsScreen.kt:61` is screen-local only. It is computed as
  `notificationState.adhanEnabled && notificationState.<prayer>AdhanEnabled` and is never stored.
- Real storage is Preferences DataStore (`data/local/datastore/PreferencesDataStore.kt`),
  reached through `SettingsRepository`:
  - per-prayer visibility: `{FAJR,SUNRISE,DHUHR,ASR,MAGHRIB,ISHA}_NOTIFICATION_ENABLED`
    (`Boolean`, default true; sunrise false), written by `setPrayerNotificationEnabled(prayer, enabled)`;
  - per-prayer adhan: `{FAJR,DHUHR,ASR,MAGHRIB,ISHA}_ADHAN_ENABLED` (`Boolean`, default true),
    written by `setPrayerAdhanEnabled(prayer, enabled)`, read at fire time through
    `isAdhanEnabledForPrayer(prayer)` — sunrise hardcoded to `false`;
  - global gate `ADHAN_ENABLED` (default **false**);
  - pre-adhan: `SHOW_REMINDER_BEFORE` (`Boolean`) + `NOTIFICATION_REMINDER_MINUTES` (`Int`,
    default 15) — **one global pair, exactly as the spec says**.
- Two consumers read these, and they read them at *different* times:
  - **schedule time** — `PrayerNotificationScheduler.scheduleTodaysPrayerNotifications(...)`
    takes `enabledPrayers: Set<PrayerType>`, `preReminderEnabled: Boolean`,
    `preReminderMinutes: Int`. The callers (`AppInitializer`, `BootReceiver`,
    `SettingsViewModel`) read the prefs and pass them in.
  - **fire time** — `BootReceiver.handlePrayerNotification` re-reads `adhanEnabled`,
    `isAdhanEnabledForPrayer`, `adhanRespectDnd`, `notificationVibration`,
    `notificationReminderMinutes` from DataStore when the alarm goes off, and decides
    `shouldPlayAdhan` / `shouldPlayBeep` there.
- Consequence for P3: **alert style is a fire-time decision** (no scheduler signature change),
  but **per-prayer offset is a schedule-time decision** (the scheduler currently applies one
  `preReminderMinutes` to every prayer in one loop, `PrayerNotificationScheduler.kt:266-272`).
- Silent delivery already has channels: `CHANNEL_ID_PRAYER_SILENT` / `CHANNEL_ID_ADHAN_SILENT`.
  They are *no-vibration* siblings, **not** no-sound — both are `IMPORTANCE_HIGH` with a
  channel sound. A genuinely silent alert style needs its own channel; Android will not let
  an existing channel's importance be lowered from code.

**Pre-existing deviation, not introduced here:** `SettingsViewModel` injects
`SettingsRepository` directly rather than a `XxxUseCases` bundle (non-negotiable rule 2).
It is out of scope per §8 ("rewriting `SettingsViewModel` beyond what P3's storage answer
requires") — do not fix it in passing, but do not copy the pattern into anything new either.
