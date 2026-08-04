# Notifications rework — progress

Branch: feat/notifications-rework
Spec: docs/specs/notifications-rework/SPEC.md
Last updated: 2026-08-04 by session 1

## Status
Current phase: complete — all phases implemented, PR open
Awaiting: device validation from Arshad across all seven screens
Blocked on: nothing

## Phases
- [x] P0  Orientation                      commit 74095672  verified 2026-08-04
- [x] P1  Shared component extensions      commit 854b0f2a  AWAITING VALIDATION
- [x] P2  Hub reshape                      commit bf6ede0a  AWAITING VALIDATION
- [x] P3  Prayers screen                   commit bf6ede0a  AWAITING VALIDATION
- [x] P4  Adhan & sound                    commit 6a2b3732  AWAITING VALIDATION
- [x] P5  Worship reminders                commit 6a2b3732  AWAITING VALIDATION
- [x] P6  Weekly                           commit 6a2b3732  AWAITING VALIDATION
- [x] P7  Diagnostics                      commit bf6ede0a  AWAITING VALIDATION
- [x] P8  Full-flow tests                  AWAITING VALIDATION

## Decisions taken

- 2026-08-04 P0 verify holds. Six screens exist under
  `presentation/screens/settings/` — `NotificationSettingsScreen`,
  `PrayerNotificationsScreen`, `NotificationSoundScreen`, `NotificationWeeklyScreen`,
  `NotificationTroubleshootingScreen`, `WorshipRemindersScreen` — and each has a route
  (`Route.SettingsNotifications`, `SettingsNotificationsPrayers`, `SettingsNotificationsSound`,
  `SettingsNotificationsWeekly`, `SettingsNotificationsTroubleshooting`,
  `SettingsWorshipReminders`). **No route was added.**

- 2026-08-04 **P1 picker trailing action — approved by Arshad: a slot on the picker.**
  `NimazListPicker` gains `trailingContent: (@Composable (NimazPickerItem<T>) -> Unit)? = null`.
  `NimazPickerItem` is unchanged, so its equality and the `LazyColumn` keys are unaffected, and
  the play/stop state that moves while the sheet is open stays with the caller. Rejected: fields
  or a composable slot on the data class.

- 2026-08-04 **P3 storage and migration — approved by Arshad: new keys + a one-shot migration.**
  Per-prayer `<prayer>_alert_style` (String), `<prayer>_reminder_enabled` (Boolean),
  `<prayer>_reminder_minutes` (Int), guarded by `notification_prefs_migration_version` (Int).
  `PrayerNotificationPrefsMigration.plan` is pure; `PreferencesDataStore
  .migratePrayerNotificationPreferences()` applies it once from `AppInitializer`. A prayer that
  was calling the adhan keeps calling it, the global lead time is copied onto all five, and
  **nothing migrates to `SILENT`** — the old model had no way to ask for silence. The legacy
  keys stay in place, read-only, so the migration has something to read.

- 2026-08-04 **`SILENT` needed a new channel**, `prayer_notifications_muted`
  (`IMPORTANCE_LOW`, `setSound(null, null)`). The existing `*_SILENT` channels are
  *no-vibration* siblings that still carry a sound at `IMPORTANCE_HIGH`, and Android will not
  let an existing channel's importance be lowered from code. Documented in `SUBSYSTEMS.md` §0.6
  and §4.

- 2026-08-04 **Phase gates — approved by Arshad: build through, validate once at the end.**
  Each phase still got its own commit and its own compile/test/docs gates; the device
  validation is a single pass over all seven screens against the PR.

## Open questions

- **`NimazListPicker` cannot express a real Cancel.** Its footer's Cancel and Done both call
  `onDismiss`, so they are indistinguishable to the caller. The adhan picker therefore commits
  on selection (which is what the screen did before this change) and its Cancel behaves as
  Close. Giving it a true Cancel means adding separate `onConfirm`/`onCancel` callbacks to the
  shared component, which is beyond the P1 change Arshad approved — raised rather than done.

## Notes for the next session

**The spec was stale in three places — verified against the working tree, not assumed:**

1. **P1's `NimazListPicker` checkbox change was already done.** `PickerRow` already rendered
   selection with `NimazCheckbox(type = CIRCLE, …)`. Nothing to change.
2. **P4's "Play at alarm volume" setting does not exist.** No match for
   `alarm volume|ALARM_VOLUME|STREAM_ALARM|alarmVolume` anywhere in `app/src/main`. Per §8 it
   was not invented; the prototype drew it, the app does not have it.
3. **"All six locales" means base + five** (`values`, `values-de`, `-fr`, `-id`, `-ms`, `-tr`;
   `values-night` is a theme qualifier).

**A correction to an earlier note in this file:** injecting `SettingsRepository` into
`SettingsViewModel` is **not** a deviation from non-negotiable rule 2. It is the *resolved*
state recorded in `CLEAN_ARCHITECTURE_CHECKLIST.md` — `SettingsRepository` is a domain
interface (`domain/repository/`), and all 13 ViewModels inject it by design. Nothing to fix.

**Local build gotcha.** `:app:testDebugUnitTest` (and anything that needs merged assets) runs
`:app:fetchNimazData`, which needs a credential that can see the private `arshad-shah/nimaz-data`
repo. The active `gh` account on this machine is the work one (`ShahA_hmh`), which gets a 404.
Run gradle with the personal account's token:

```bash
export NIMAZ_DATA_TOKEN=$(gh auth token -h github.com -u arshad-shah)
```

`:app:compileDebugKotlin` alone does not need it.

**What the code now looks like, in one paragraph.** Alert style is read at **fire time**
(`BootReceiver`), so changing it needs no rescheduling; the reminder lead time is read at
**schedule time** and rides on the alarm intent as `EXTRA_REMINDER_MINUTES`, so the fired text
states the right number. `scheduleTodaysPrayerNotifications` takes
`preReminders: Map<PrayerType, Int>` — a prayer absent from the map gets no reminder, rather
than a zero offset. All three schedule call sites build it through
`SettingsRepository.preReminderMinutesByPrayer()` so they cannot drift. The fire-time rules live
on `PrayerAlertStyle` (`playsAdhan`, `isMuted`) so the receiver and the tests read one rule.

**What still needs a device.** Everything under `## Status`. The riskiest is P3: a per-prayer
setting that persists correctly but schedules nothing would look fine in every screenshot.
