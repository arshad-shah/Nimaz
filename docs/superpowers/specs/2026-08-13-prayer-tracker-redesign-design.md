# Prayer tracker redesign — design

**Date:** 2026-08-13 · **Branch:** `feat/prayer-tracker-redesign`
**Base:** `feat/fasting-screen-redesign` @ `d49b0b49` (not `dev` — see §6)
**Worktree:** `.claude/worktrees/feat+prayer-tracker-redesign`
**Prototype:** supplied by the user as a single self-contained HTML page — week rail, day card with
a prayer timeline and inline status pickers, an "unrecorded" review banner, a month grid with
per-day fill bars, and a qada sub-screen.

The current screen **assumes**: a prayer whose time has passed with nothing logged is silently
rewritten to `missed`, every midnight, by a broadcast receiver. The redesign makes the screen
**report what it knows**: prayed, late, made up, missed — or *not recorded*, which is a fifth thing
and not a synonym for the fourth.

---

## 0. What is on the base branch today

Verified by reading the files at `d49b0b49`, not remembered.

| Thing | State |
|---|---|
| `PrayerTrackerScreen.kt` | 566 lines; `SecondaryTabRow` (Tracker / Qada) over a `NimazPager`, then a `LazyColumn` |
| Streak | `GradientCard` hero, full width, gold, ~120dp tall |
| Calendar | `NimazCalendar` with `SelectionStyle.BORDER` and a coloured indicator dot per day |
| Day detail | `SelectedDayDetail` → five `PrayerCheckItem` rows |
| `PrayerCheckItem` | Hand-rolled `Box` + `.background()` + `.border()` checkbox circle — violates CLAUDE.md rule 8 |
| Logging | One tap toggles prayed ↔ missed. `LATE` and `QADA` are displayable but **not settable** |
| `Route.QadaPrayers` | Exists, tagged `ScreenTags.QadaPrayers` — but renders `PrayerTrackerScreen(initialTab = 1)` |
| `PrayerDao:28` | `getMissedPrayersRequiringQada` already filters `status = 'missed' AND isQadaFor IS NULL` |
| `PrayerDao:94` | `markPastPrayersAsMissed` — bulk `UPDATE … SET status='missed' WHERE date < :today AND status IN ('pending','not_prayed')` |
| `PrayerRescheduler:50` | Calls it when `markPastAsMissed = true` |
| `BootReceiver:114` | Passes `true` on `ACTION_MIDNIGHT_RESCHEDULE` — **so it fires every midnight** |
| `BootReceiver:103` | Passes `false` after a reboot (correct already) |
| `PrayerStatsScreen` | Computes `total = prayed + missed` for every rate it shows |

### 0.1 Two documentation facts worth recording

- `docs/NAVIGATION.md:240` already states `QadaPrayers` → `QadaPrayersScreen`. That file does not
  exist; the route renders `PrayerTrackerScreen`. **The doc is currently wrong**, and this change
  makes it right rather than needing a new entry.
- `docs/SUBSYSTEMS.md:416` and `:432` document the midnight chain as "mark missed prayers +
  reschedule". That is the behaviour §1 retires, so §4 of that doc must change in the same commit.

### 0.2 What the design system already provides

Reused **unchanged**: `NimazCard`, `NimazButton`, `NimazIcon`, `NimazIconButton`, `NimazBadge`
(it already has an `OUTLINED` emphasis), `NimazBanner` (already takes `actionLabel` + `onAction`),
`NimazMenuItem` (already takes a `trailing` slot), `NimazSectionHeader`, `NimazEmptyState`,
`NimazScreenScaffold`, `NimazBackTopAppBar`, `NimazQadaPrayerItem`, `NimazLegendItem`,
`rememberNow` / `TickResolution`.

Reused **from the fasting branch**, which is the reason this branch is based on it:
`NimazDayRail` + `NimazDayRailItem`, `NimazStatusDot` + `NimazStatusDotSpec` +
`NimazStatusDotStyle`, `NimazToneColors`, `NimazSegmentedControl` + `NimazSegmentedOption`.
`NimazWindowTrack` and `NimazProgressTrack` are *not* used here — see §2.1 for why the timeline is
neither of them.

**One** primitive the prototype needs and nothing in the system provides — §2.1.

---

## 1. The semantic split: `NOT_RECORDED` is not `MISSED`

### 1.1 The rule

A prayer's displayed status is:

```
assertedStatus  ?:  if (its time has passed) NOT_RECORDED else UPCOMING
```

where `assertedStatus` is the record's status **only when the user actually asserted something** —
`PRAYED`, `LATE`, `MISSED`, `QADA`. A missing row, a `PENDING` row and a `NOT_PRAYED` row are all
the same fact — *nobody has said* — and all three derive.

That equivalence is what makes tap-to-clear free: clearing writes `NOT_PRAYED` through the existing
`UpdatePrayerStatusUseCase` rather than needing a delete path, and the row immediately reads back as
`NOT_RECORDED` (or `UPCOMING`). It is also why `PENDING` must be included: rows created ahead of
time by the scheduler carry it, and treating it as an assertion would show every future prayer as
recorded.

`NOT_RECORDED` is **derived at presentation time and never persisted**. No Room migration, no new
`PrayerStatus` value, no change to sync export/import, no widget state change.

### 1.2 What changes to make it true

**(a) The midnight auto-missing stops.** `BootReceiver:114` changes from
`rescheduleToday(markPastAsMissed = true)` to `rescheduleToday()`. The `markPastAsMissed` parameter
is then dead at every call site, so it is removed from `PrayerRescheduler.rescheduleToday()`
entirely — a parameter that is `false` everywhere is a trap for the next reader.

The rescheduling itself is untouched. This is the point to be careful: `PrayerRescheduler`'s own
KDoc explains that if it regresses, prayer notifications stop *silently*. The change removes three
lines at the top of the method and nothing else.

**(b) The bulk update becomes scoped and explicit.** `markPastPrayersAsMissed()` gains a range:

```kotlin
// PrayerRepository
suspend fun markPastPrayersAsMissed(from: LocalDate, to: LocalDate): Int
```

```sql
-- PrayerDao
UPDATE prayer_records SET status = 'missed', updatedAt = :timestamp
WHERE date BETWEEN :from AND :to
  AND status IN ('pending', 'not_prayed')
  AND prayerName != 'sunrise'
```

Its only caller becomes the review banner, on an explicit tap, over the seven days the banner
counted. The unbounded "everything before today" form is deleted — an unbounded destructive update
with no caller is a loaded gun in a drawer.

**(c) Qada needs no change.** `PrayerDao:28` already reads only explicit `status = 'missed'` rows.
It was always the honest query; the midnight job was feeding it dishonest data.

### 1.3 Accepted consequence

`PrayerStatsScreen` computes `total = prayed + missed`. With fewer rows auto-marked `missed`, its
denominators shrink and completion percentages rise. This is deliberate: the rate becomes "of what
you logged" rather than "of everything the app assumed on your behalf". It is recorded here so that
the number moving is a documented decision and not a bug report.

Stats are **not** otherwise reworked on this branch. `getPerfectDaysCount` counts days with five
completed prayers and is unaffected by whether the other days say `missed` or nothing.

---

## 2. Components

### 2.1 New atom — `NimazTimelineTrack`

`presentation/components/atoms/NimazTimelineTrack.kt`.

A hairline track carrying N nodes at proportional positions, an optional "now" marker, and two edge
labels. The prototype's signature element.

**Why not `NimazWindowTrack`:** that atom is a *span* — two named, differently-tinted ends and a
marker inside a filled band. This has five interior nodes each carrying its own status colour, and
its fill means "elapsed", not "complete". Bending the window track to do both would give it a node
list, a per-node tone, and a second meaning for its fill — three parameters that each only apply in
one of its two modes, which is two atoms wearing one name.

**Why not `NimazProgressTrack`:** a progress bar has one meaningful end and no interior structure.

```kotlin
data class NimazTimelineNode(
    /** Position along the track in 0f..1f. Coerced; NaN reads as 0f. */
    val position: Float,
    val spec: NimazStatusDotSpec,
    val label: String,
)

@Composable
fun NimazTimelineTrack(
    nodes: List<NimazTimelineNode>,
    startLabel: String,
    endLabel: String,
    modifier: Modifier = Modifier,
    /** Position of the "now" marker in 0f..1f; null draws no marker and fills the whole track. */
    progress: Float? = null,
    contentDescription: String? = null,
)
```

Colours resolve through `NimazToneColors`, nodes render as `NimazStatusDot` — so a node that is
"recorded as not happening" gets the ring style for free, which is exactly the `NOT_RECORDED` case.
Node placement clamps to the track bounds so a node at `0f` or `1f` is not half-clipped.

**Accessibility:** supplying `contentDescription` clears the children from the tree and speaks one
sentence, matching `NimazWindowTrack`'s precedent — five unlabelled dots read as noise.

**Light and dark `@Preview`s** plus a Robolectric test, built and reviewed **before any screen
imports it**. This is a standing requirement from the user and matches how the six fasting atoms
were done.

### 2.2 Extension — `NimazAccordion` gains hoisted expansion and a flat style

The prayer row *is* an accordion: title (`Fajr`), subtitle (time), `trailing` (status badge),
expanding content (the picker). `NimazAccordion` already has a preview built from prayer rows.

Two gaps:

1. It owns `expanded` in a private `remember`, so "only one row open at a time" — which the
   prototype specifies and which keeps the day card from unfolding into a wall — is impossible.
2. It always draws a `NimazCard`. The day card needs flat, divider-separated rows inside one card,
   not five nested cards.

```kotlin
/** Hoisted overload. The existing self-managing overload delegates to this one. */
@Composable
fun NimazAccordion(
    title: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    style: NimazAccordionStyle = NimazAccordionStyle.CARD,
    content: @Composable ColumnScope.() -> Unit,
)

enum class NimazAccordionStyle { CARD, FLAT }
```

Every existing call site keeps working: the current signature stays as an overload that owns its
state and defaults to `CARD`.

### 2.3 Extension — `CalendarDayState` gains a fill bar

```kotlin
data class CalendarDayState(
    // … existing fields unchanged …
    /** Fraction of the day completed, 0f..1f. Null draws no bar. Renders under the day number. */
    val indicatorBar: Float? = null,
    val indicatorBarColor: Color? = null,
)
```

The month grid's at-a-glance "how much of this day" reading, which a single dot cannot carry. The
fasting branch extended this same file with `indicatorStyle`; this follows that precedent, and the
fasting calendar can adopt the bar later at no cost.

`indicatorBar` and `indicatorColor` are independent — a caller may use either, both, or neither.

### 2.4 Nothing else is new

| Prototype element | Existing component |
|---|---|
| Week rail | `NimazDayRail` — exact fit, including the marker dot and the disabled future day |
| Rail markers, timeline nodes, legend | `NimazStatusDot` / `NimazStatusDotSpec` |
| Status pills | `NimazBadge(emphasis = OUTLINED)` for not-recorded, `SOFT` otherwise |
| Inline status picker | `NimazSegmentedControl` — its **nullable** `selectedIndex` is literally the not-recorded state, and `onSelect` firing on an already-selected cell gives tap-to-clear |
| Review banner | `NimazBanner(variant = WARNING, actionLabel = …, onAction = …)` |
| Qada summary row | `NimazMenuItem(trailing = { NimazBadge(…) })` |
| Qada list rows | `NimazQadaPrayerItem` |
| Month grid | `NimazCalendar` + §2.3 |

### 2.5 Deleted

- `StreakCard` and its `GradientCard` — the streak becomes an inline gold `NimazBadge` in the day
  card header. It is context, not a headline, and the hero card costs a full screen of scroll.
- `PrayerCheckItem` — replaced by the accordion row. Its hand-rolled `Box`/`background`/`border`
  checkbox circle violates CLAUDE.md rule 8 and is not worth porting.
- `SecondaryTabRow` + `NimazPager` from this screen.

---

## 3. Screen architecture

### 3.1 `PrayerTrackerScreen.kt`

One `LazyColumn` under `NimazScreenScaffold` + `NimazBackTopAppBar` (stats action retained), in
prototype order:

1. **Week rail** — `NimazDayRail`, seven days centred on the selected date, markers derived from
   `historyState.records`. Future days `enabled = false`.
2. **Day card** — `NimazCard` containing:
   - header: full date, inline gold streak `NimazBadge`, "*n* of 5 recorded", and a "Back to today"
     `NimazButton` shown only when the selection is not today;
   - `NimazTimelineTrack` with a node per prayer and a `now` marker only when the selected day is
     today;
   - five flat `NimazAccordion` rows — dot, name, time, status badge — expanding to a
     `NimazSegmentedControl`. Exactly one row open at a time, held in the screen's `rememberSaveable`.
     The picker offers On time / Late / Missed, plus **Made up** only when the prayer's time has
     passed. A `NOT_RECORDED` row shows the prototype's explanatory line.
3. **Review banner** — rendered only when the derived `NOT_RECORDED` count over the last 7 days
   is > 0. Says what the count is and that those prayers are *not* in the qada list until marked.
4. **Month section** — `NimazSectionHeader` with a complete-days count, `NimazCalendar` with fill
   bars, and a four-entry legend (all five / some / none / not recorded, the last a ring).
5. **Qada summary row** — `NimazMenuItem` with the outstanding count, navigating to
   `Route.QadaPrayers`.

### 3.2 `QadaPrayersScreen.kt` — new file

`presentation/screens/prayer/QadaPrayersScreen.kt`. Its own `NimazBackTopAppBar`, a
`NimazSectionHeader` carrying the total, and a `LazyColumn` of `NimazQadaPrayerItem`;
`NimazEmptyState` when nothing is outstanding, with copy that says explicitly that marking a prayer
missed is what puts it here.

`NavGraph` line ~873 loses its `// Redirect QadaPrayers to PrayerTracker` comment and renders the
real screen. It reuses `PrayerTrackerViewModel` — the qada state, events and use cases already live
there, and splitting the ViewModel would duplicate the record flow for no gain.

### 3.3 Status derivation — `PrayerDayStatus.kt`

New file, `presentation/screens/prayer/PrayerDayStatus.kt`. No Compose, no Android imports, so
plain JVM unit tests cover it.

```kotlin
enum class PrayerDisplayStatus { PRAYED, LATE, QADA, MISSED, NOT_RECORDED, UPCOMING }

fun resolvePrayerStatuses(
    records: List<PrayerRecord>,
    times: PrayerTimes?,
    date: LocalDate,
    now: LocalDateTime,
): Map<PrayerName, PrayerDisplayStatus>

fun PrayerDisplayStatus.isDone(): Boolean   // PRAYED, LATE, QADA
fun PrayerDisplayStatus.tone(): NimazTone
```

`SUNRISE` is excluded throughout. When `times` is null (no location yet) nothing on a *past* date
can be `UPCOMING`, so a past date with no times still derives `NOT_RECORDED`; a *today* with no
times derives `UPCOMING` for everything, because without times there is no basis to claim a prayer
has passed.

The screen calls it with `rememberNow(TickResolution.MINUTES)`, which the current screen already
does — and its existing comment explains why a bare `LocalDateTime.now()` would not re-resolve.
That comment is preserved.

### 3.4 ViewModel

`PrayerTrackerEvent` gains:

```kotlin
/** [status] of null clears the record — the segmented control's tap-to-clear. */
data class SetPrayerStatus(val prayerName: PrayerName, val status: PrayerStatus?) : PrayerTrackerEvent
data class ConfirmUnrecordedAsMissed(val from: LocalDate, val to: LocalDate) : PrayerTrackerEvent
```

and loses `MarkPrayerPrayed` / `MarkPrayerMissed`, whose only callers are this screen.
`SetPrayerStatus` subsumes both and additionally makes `LATE` and `QADA` settable, which they have
never been from the UI despite being displayable.

Telemetry: `SetPrayerStatus` reports the resolved status name through the existing
`telemetry.prayerTracked(...)`; a clear reports `"cleared"`.

`ConfirmUnrecordedAsMissed` calls the scoped repository method from §1.2(b), guarded by the same
`launchSafely` + job-handle pattern the rest of the ViewModel uses (the file's comments record why
every re-entrant loader needs its own handle).

### 3.5 Navigation

`Route.PrayerTracker` loses `initialTab`:

```kotlin
data object PrayerTracker : Route
```

Consequences, all mechanically checked by `scripts/check_docs.py`:

| Site | Change |
|---|---|
| `Routes.kt:115` | `data class` → `data object` |
| `NavGraph.kt:858` | Drops `toRoute` + `initialTab` |
| `NavGraph.kt:873` | Renders `QadaPrayersScreen` |
| `AnnouncementRoutes.kt:30` | `Route.PrayerTracker()` → `Route.PrayerTracker` |
| `AnnouncementRoutes.kt:136` | `prayer/tracker/{tab}` — tab `1` maps to `Route.QadaPrayers`, everything else to `Route.PrayerTracker`. The key stays valid so shipped announcements do not break |
| `HelpDeepLink.kt:18` | `Route.PrayerTracker()` → `Route.PrayerTracker` |
| `docs/NAVIGATION.md:238` | Params column `initialTab: Int = 0` → `—` |
| `docs/NAVIGATION.md:240` | Now accurate without editing — `QadaPrayersScreen` exists |
| `docs/NAVIGATION.md:462` | Announcement grammar entry rewritten for the tab→route mapping |

Destination count is unchanged, so the §3 count assertion still holds.

---

## 4. Testing

| Test | Kind | Covers |
|---|---|---|
| `PrayerDayStatusTest` | JVM | Past date, today before/after each time, null `PrayerTimes`, explicit record overriding derivation, `SUNRISE` excluded, `isDone`/`tone` mappings |
| `NimazTimelineTrackTest` | Robolectric | Node count, position clamping, `NaN`/out-of-range `progress`, marker absent when `progress == null`, merged content description |
| `NimazAccordionTest` | Robolectric | Hoisted overload calls `onExpandedChange`; `FLAT` draws no card; existing self-managing overload still works |
| `NimazCalendarTest` | Robolectric | `indicatorBar` renders; bar and dot are independent |
| `PrayerTrackerViewModelTest` | JVM | `SetPrayerStatus` for each value **and** for `null`; `ConfirmUnrecordedAsMissed` passes the right range; job handles cancel prior collectors |
| `PrayerReschedulerTest` | JVM | Rescheduling still happens; `markPastPrayersAsMissed` is never called |
| `FeatureNavigationTest` | androidTest | `ScreenTags.QadaPrayers` now resolves to a real screen |

The last row is why `assembleDebugAndroidTest` is **mandatory** on this branch, not optional: the
other four gates do not compile `androidTest`.

---

## 5. Documentation obligations (same commit)

- **`docs/NAVIGATION.md`** §3 (the two route rows above) and §4 (announcement route grammar for
  `prayer/tracker/{tab}`).
- **`docs/SUBSYSTEMS.md`** §4 — lines 416 and 432 describe the midnight chain as "mark missed
  prayers + reschedule". Both the mermaid sequence and the prose must drop the marking step. The
  §0 inventory is unchanged: no Service, Worker, widget, channel or DataStore file is added,
  removed or renamed, and the schema version does not move.
- **`docs/ARCHITECTURE.md`** §8 — the `NimazAccordion` and `CalendarDayState` extensions, if either
  changes a documented pattern.
- **`docs/CLEAN_ARCHITECTURE_CHECKLIST.md`** — **AP-8** (design-system drift: hand-rolled surfaces,
  bypassed tones). `PrayerCheckItem`'s `Box` + `.background()` + `.border()` checkbox circle is an
  instance of it; add and tick an entry in the AP-8 list the way `JumuahCard`'s resolved entry is
  written.

---

## 6. Branching

Based on `feat/fasting-screen-redesign`, not `dev`, because the four fasting atoms this design
reuses (§0.2) exist only there. The alternative — cherry-picking them onto a `dev`-based branch —
puts the same six files in two PRs, and whichever merges second conflicts.

**This branch therefore merges after fasting does.** Nothing here modifies a fasting file, so the
merge is additive: the only shared files touched are `CalendarModels.kt` (a new field appended) and
`NimazAccordion.kt` (a new overload), neither of which the fasting branch is editing.

---

## 7. Phasing

1. **Atoms only** — `NimazTimelineTrack`, the `NimazAccordion` extension, the `CalendarDayState`
   extension, with light and dark previews and their tests. Nothing else compiles against them.
   **The user reviews the previews before phase 2 starts.**
2. **Semantics** — §1: the rescheduler change, the scoped repository/DAO method, `PrayerDayStatus.kt`
   and its tests. No UI yet.
3. **ViewModel** — the event changes and their tests.
4. **Screens** — `PrayerTrackerScreen` rewrite and `QadaPrayersScreen`.
5. **Navigation + docs** — §3.5 and §5, then all five verification gates.

---

## 8. Verification

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug                 # CI-blocking; catches MissingTranslation on new strings
./gradlew :app:assembleDebugAndroidTest  # mandatory here — navigation changed
python3 scripts/check_docs.py
```

New user-facing strings ("Not recorded", the banner copy, the empty-state copy, "Back to today")
go in `strings.xml` and must be present in every shipped locale, or `lintDebug` fails on
`MissingTranslation`.
