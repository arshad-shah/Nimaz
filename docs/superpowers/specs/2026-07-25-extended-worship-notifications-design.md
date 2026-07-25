# Extended Worship & Fasting Notifications — Design Spec

> Status: **approved** (brainstormed + visually locked 2026-07-25).
> Scope categories: **A · Night worship**, **C · Ramadan**, **D · Sunnah fasting + Dhikr**.
> Delivery: **notification-only nudges** (no adhan audio). Home surface: **single "Next Worship" card** (Direction A).
> Companion mockups: [`docs/design/worship-notifications/`](../../design/worship-notifications/)
> (`settings-screens.html`, `home-card-and-copy.html`).

## 1. Goal

Extend Nimaz's prayer-notification subsystem (`docs/SUBSYSTEMS.md` §4) with **optional,
off-by-default** reminders for Sunnah worship and fasting occasions, surface the nearest
upcoming enabled one on Home as a card (like the existing Jumu'ah card), and restructure the
now-oversized notification settings screen into a hub + focused subscreens.

## 2. Architecture — reuse, don't reinvent

Every reminder follows the **existing 5-point alarm pattern** already used by the Khatam and
Friday reminders:

1. A DataStore pref (`Flow<Boolean>`/`Flow<String>` getter + `suspend` setter) in
   `PreferencesDataStore` **and** `SettingsRepository`.
2. A `schedule*()`/`cancel*()` pair in `PrayerNotificationScheduler`, **armed inside
   `scheduleTodaysPrayerNotifications(...)`** so the midnight-reschedule chain + boot path re-arm it.
   A reminder scheduled anywhere else fires once and never again.
3. An `ACTION_*` constant + a unique request code (new block **9000+**, see §5).
4. A dispatch branch in `BootReceiver.onReceive` that re-checks the pref at fire time, applies
   the saved locale, and posts the notification.
5. Content in a new `WorshipReminderContent` helper (mirrors `NotificationContentHelper`),
   shared by the notification **and** the Home card so copy stays consistent.

**No new `PrayerType` enum entries** — these are *derived* times, not fard prayers. Adding them to
`PrayerType` would pollute prayer tracking/stats. Model them as their own `WorshipReminderType`.

**Channel:** one new DEFAULT-importance channel `worship_reminders` ("Worship Reminders"), a
gentle nudge like `khatam_notifications` — never a HIGH alarm.

**Derived-time sources (already available):**
| Time | Source |
|---|---|
| Tahajjud / last third, middle of night | `adhan2` `SunnahTimes` (class exists; expose via `PrayerTimeCalculator`) |
| Suhoor / Iftar | Fajr / Maghrib (already computed; `FastingViewModel` shows them) |
| Ramadan gate, White Days, Arafah, Ashura, Mon/Thu | `HijriDateCalculator` (`isTodayRamadan()`, events, day-of-week) |
| Adhkar, Witr, Taraweeh | fixed offsets from Fajr/Asr/Isha |

## 3. Settings information architecture

`NotificationSettingsScreen` (one long scroll) → a **hub** + 5 subscreens. All state already lives
in `SettingsViewModel.notificationState`; subscreens render slices of it. Each subscreen is a new
`@Serializable Route` + `composable<Route.X>` (nav rule #6).

| Route | Screen | Holds |
|---|---|---|
| `Route.SettingsNotifications` (hub, exists) | Notifications | master switch + 5 links |
| `Route.SettingsNotificationsPrayers` (new) | Prayer notifications | 5 prayers + per-prayer adhan, pre-adhan + lead time, sunrise |
| `Route.SettingsWorshipReminders` (new) | Worship reminders | Night / Ramadan / Fasting & Dhikr toggles |
| `Route.SettingsNotificationsWeekly` (new) | Weekly & reading | Jumu'ah, Khatam, daily summary |
| `Route.SettingsNotificationsSound` (new) | Sound & delivery | adhan master, muezzin picker, vibration, DND |
| `Route.SettingsNotificationsTroubleshooting` (new) | Troubleshooting | test/reset, battery optimization |

The **Ramadan group auto-hides outside Ramadan** via `HijriDateCalculator.isTodayRamadan()`.

## 4. Home "Next Worship" card (Direction A)

A single `WorshipEventCard` built on the existing `EventCard`/`EventOccasion` system (teal/gold,
eyebrow + Arabic + body + trailing time + countdown highlight), added to the existing
`EventsCarousel` alongside Jumu'ah/celebrations. `HomeViewModel` picks the **nearest upcoming
enabled** worship reminder (within a "near" window, default a few hours / same night) and exposes it
in `HomeUiState`. Renders nothing when nothing is enabled or near.

## 5. Request-code allocation (avoid collisions with §4 scheme)

Existing: prayer `1000+ord`, pre-reminder `2000+ord`, summary `8889`, Friday `8890`, Khatam `8891`,
midnight `9999`. **New worship block: `9000 + WorshipReminderType.ordinal`** (well clear of the above).

## 6. Dynamic routing / deep-links — must keep working

Both `announcementRoute` (FCM) and `helpDeepLinkRoute` (Help) map `settings/notifications` →
`Route.SettingsNotifications`. The hub keeps that route, so **existing FCM + Help deep-links stay
valid**. We only **add** subscreen keys:

- `announcementRoute` (`AnnouncementRoutes.kt`): `settings/notifications/prayers`, `.../worship`,
  `.../weekly`, `.../sound`, `.../troubleshooting` → the new routes.
- `helpDeepLinkRoute` (`HelpDeepLink.kt`): `worship_reminders` → `Route.SettingsWorshipReminders`
  (+ optionally the others).
- Update `docs/NAVIGATION.md` route table **and** the announcement-route-grammar section.

## 7. Help documentation

`app/src/main/assets/help/help.json` (localized ×6, seeded by `HelpContentSeeder`, currently
`contentVersion: 7`). Add a **"Worship reminders"** topic (or items under an existing notifications
topic) explaining each reminder and how to enable it, with `deeplink` keys pointing at the new
routes. **Bump `contentVersion` to 8** or the seeder won't reach existing users (`docs/SUBSYSTEMS.md`
§7). Update `docs/SUBSYSTEMS.md` §4 and §7 as part of the work.

## 8. Reminder catalogue (copy = string-resource drafts, localize ×6)

See [`home-card-and-copy.html`](../../design/worship-notifications/home-card-and-copy.html) for the
rendered cards. All bodies are authentic hadith/ayah paraphrases (matching `NotificationContentHelper`).

| # | Reminder | Trigger | Notif title | Notif body (draft) |
|---|---|---|---|---|
| A1 | Tahajjud / Qiyam | `SunnahTimes.lastThirdOfTheNight` | Tahajjud — the last third of the night | A blessed time for duʿāʾ has begun. "Our Lord descends… Who is calling upon Me, that I may answer him?" |
| A2 | Witr | after Isha (delay) **or** Fajr − 90m (user picks) | Have you prayed Witr? | Seal your night with the Witr prayer before Fajr. "Make Witr the last of your prayers at night." |
| C1 | Suhoor end | Fajr − N min (def 30) · Ramadan | Suhoor ends soon | ~30 minutes until Fajr. Finish your suhoor and make your intention. "Take suhoor, for in it there is blessing." |
| C2 | Iftar | Maghrib (or −N) · Ramadan | Time to break your fast 🌙 | Maghrib has entered. "The fasting person has two joys: when he breaks his fast, and when he meets his Lord." |
| C3 | Taraweeh | Isha + N min (def 15) · Ramadan | Taraweeh tonight | Isha is complete — stand for the night prayer. "Whoever prays the nights of Ramadan with faith… his past sins are forgiven." |
| C4 | Laylatul Qadr | 21/23/25/27/29 Ramadan after Isha | Seek Laylatul Qadr tonight | An odd night of the last ten. "The Night of Decree is better than a thousand months." |
| D1 | Morning adhkar | Fajr + N min | Morning remembrance | Begin your day in the remembrance of Allah. A few minutes of adhkar to guard your day. |
| D1 | Evening adhkar | Asr + N min | Evening remembrance | Fortify your evening with the adhkar. |
| D2 | Mon & Thu fasting | Sun/Wed evening | Fasting tomorrow? It's Monday/Thursday | A Sunnah day to fast. "Deeds are presented on Monday and Thursday…" |
| D3 | White Days | eve of 13 Hijri | The White Days begin tomorrow | Fast the 13th–15th — the bright days. "Fasting three days of each month is like fasting for a lifetime." |
| D4 | Arafah & Ashura | eve of 9 Dhul-Hijjah / 10 Muharram | Day of Arafah / Ashura tomorrow | Fasting Arafah expiates two years of sins; Ashura, the previous year. |

## 9. Definition of done (every reminder issue)

- [ ] Pref in `PreferencesDataStore` + `SettingsRepository` (+ export/import + sync category if user-facing)
- [ ] `schedule*/cancel*` armed inside `scheduleTodaysPrayerNotifications`
- [ ] `ACTION_*` + request code (9000+ block) + `BootReceiver` dispatch (re-check pref, re-apply locale)
- [ ] Content in `WorshipReminderContent`, string resources ×6 locales
- [ ] Settings row on the correct subscreen + `SettingsEvent` + VM state
- [ ] Home card variant fed by the shared content helper
- [ ] Unit tests (scheduler arming, content, Hijri/Ramadan gating) + VM tests
- [ ] Docs: `SUBSYSTEMS.md` §4, and `NAVIGATION.md`/help if routes/help touched

## 10. Issue tree

- **Master epic** — umbrella
- **Foundation:** F1 settings hub + subscreen split (routes, nav, deep-link allowlist, docs) ·
  F2 scheduler + content core (SunnahTimes, reminder helper, channel, Ramadan gating) ·
  F3 Home "Next Worship" card + HomeViewModel selector · F4 help content + i18n + docs
- **Epic A (Night):** A1 Tahajjud · A2 Witr
- **Epic C (Ramadan):** C1 Suhoor · C2 Iftar · C3 Taraweeh · C4 Laylatul Qadr
- **Epic D (Fasting & Dhikr):** D1 Adhkar · D2 Mon/Thu · D3 White Days · D4 Arafah & Ashura

Foundation (F1–F3) blocks all reminders; F4 can run in parallel.
