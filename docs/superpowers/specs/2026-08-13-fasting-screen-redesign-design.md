# Fasting screen redesign — design

**Date:** 2026-08-13 · **Branch:** `feat/fasting-screen-redesign` · **Base:** `dev`
**Prototype:** supplied by the user as a single self-contained HTML page (week rail, day card with
a fasting-window band and a three-way status control, month grid, "Coming up" carousel, make-up
sub-screen).

The current screen **manages records**. The redesign makes it **report the day**: what the window
is, how far through it you are, and what you have logged — with everything reachable in one scroll
instead of behind a tab row and two expanders.

---

## 0. What is on `dev` today

Verified against `dev` at the branch point. Every claim below was read, not remembered.

| Thing | State |
|---|---|
| `FastTrackerScreen.kt` | 1249 lines; `PrimaryTabRow` with **Tracker** / **Make-up**, then one `LazyColumn` |
| Day logging | `NimazSwitch` inside `TodayFastSection`, disabled for `EXEMPTED`/`MAKEUP_DUE` |
| Suhoor / iftar | Two stacked `NimazCard`s with a countdown each; **today only** |
| Calendar + Recommended | Both folded behind `FastingGoDeeperGroup` (`NimazMenuGroup` of reporting rows) |
| Make-up | `MakeupFastsTab.kt` (472 lines), rendered as tab index 1 |
| Day sheet | `FastManagementBottomSheet.kt` — status chips + fast-type dropdown + reason chips + note + delete |
| Routes | `Route.FastingHome`, `Route.FastingTracker`, `Route.FastingStats` all render **the same screen** |
| Records in range | `FastingRepository.getFastRecordsInRange(start, end)` exists, exposed as `GetFastRecordsInRangeUseCase` |
| Per-day prayer times | `prayerUseCases.getDaySchedule(date, settings)` takes a date; the ViewModel only ever passes `todayProvider.today()` |

### 0.1 What the design system already provides

Reused unchanged: `NimazCard`, `NimazButton`, `NimazIcon`, `NimazIconButton`, `NimazBadge`,
`NimazChip`/`NimazFilterChip`, `NimazSectionHeader` (already has `trailingText`), `NimazCalendar`
(already has a fasting-tracker preview), `NimazSheet`/`NimazBottomSheet`, `NimazDivider`,
`NimazScreenScaffold`, `NimazBackTopAppBar`, `NimazEmptyState`, `RamadanCountdownCard`,
`rememberNow`/`rememberCountdownTo`/`countdownText`/`clockTimeText`.

Five primitives the prototype needs and the system does **not** have — §1.

---

## 1. New atoms

All five land in `presentation/components/atoms/`, each with light **and** dark `@Preview`s and a
Robolectric test in `app/src/testDebug/.../components/atoms/`, following the `NimazSwitch` /
`NimazCheckbox` precedent. **They are built and reviewed as one self-contained phase, before any
screen touches them.**

Colours come from `MaterialTheme.colorScheme.*` / `NimazColors.*` and tones from the existing
`NimazTone` enum (`NEUTRAL, MUTED, ACCENT, PROMINENT, SUCCESS, WARNING, ERROR, TRANSPARENT`). No
`Color(0xFF…)` literals (CLAUDE.md rule 7).

### 1.1 `NimazSegmentedControl`

**Why not `NimazPillTabs`:** that is an organism, text-only, and paints every selected tab with the
one `primary` colour. The prototype's control carries an icon per option and tints the *selected*
option differently per option (fasted → success, not fasting → neutral, exempt → warning).

```kotlin
data class NimazSegmentedOption(
    val label: String,
    val icon: ImageVector? = null,
    val selectedTone: NimazTone = NimazTone.ACCENT,
    val contentDescription: String? = null,
)

enum class NimazSegmentedSize { SMALL, MEDIUM }

@Composable
fun NimazSegmentedControl(
    options: List<NimazSegmentedOption>,
    selectedIndex: Int?,          // null = nothing selected (day not logged)
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    size: NimazSegmentedSize = NimazSegmentedSize.MEDIUM,
    enabled: Boolean = true,
)
```

Track is `surfaceContainer` with inset padding; the selected cell is a raised `surface` pill.
`selectedIndex = null` renders every cell unselected — the "not logged yet" state, which a boolean
toggle cannot express and which is the reason the switch is being replaced.

**Accessibility:** the row carries `Modifier.selectableGroup()`; each cell is
`Modifier.selectable(role = Role.RadioButton)` so TalkBack announces "selected" rather than
"button", and the whole control reads as one group.

### 1.2 `NimazProgressTrack`

**Why:** there is no progress atom in the system at all. **Eight** files hand-roll
`LinearProgressIndicator` with their own height, shape and colours — five molecules
(`QaidaCourseHeader`, `QuranAudioBottomBar`, `QuranSurahInfoComponents`, `QuranSurahListItem`,
`RamadanCards`) and three screens (`search/AskComponents`, `settings/SyncScreen`,
`settings/WidgetsScreen`).

```kotlin
enum class NimazProgressSize(val height: Dp) { THIN(4.dp), MEDIUM(6.dp), THICK(10.dp) }

@Composable
fun NimazProgressTrack(
    progress: Float,              // coerced into 0f..1f at the atom
    modifier: Modifier = Modifier,
    tone: NimazTone = NimazTone.ACCENT,
    size: NimazProgressSize = NimazProgressSize.MEDIUM,
    gradient: Boolean = false,    // gold ramp for the Ramadan strip
    trackColor: Color? = null,    // null = tone's own container colour
    contentDescription: String? = null,
)
```

`progress` is coerced inside the atom: eight hand-rolled call sites is eight chances for a NaN or an
out-of-range float to throw, and a progress bar should never be the thing that crashes a screen.

**Scope note:** migrating the eight existing `LinearProgressIndicator` sites is **explicitly out of
scope** for this branch and recorded as a follow-up. Only `RamadanCards`, which this redesign
rewrites anyway, moves onto it.

### 1.3 `NimazWindowTrack`

**Why not `NimazProgressTrack` with labels:** it is not a progress bar. It is a *span* — a band
with a gradient fill, a "now" marker inside it, and two labelled, differently-tinted ends. Reusing
the progress atom would mean bolting a marker and two label slots onto a primitive that other
callers do not want.

```kotlin
@Composable
fun NimazWindowTrack(
    startLabel: String,           // "Suhoor ends"
    startValue: String,           // "04:31"
    endLabel: String,             // "Iftar"
    endValue: String,             // "20:58"
    modifier: Modifier = Modifier,
    progress: Float? = null,      // null = whole band lit, no marker (a day that is not today)
    startTone: NimazTone = NimazTone.ACCENT,
    endTone: NimazTone = NimazTone.WARNING,
    contentDescription: String? = null,
)
```

`progress = null` is the not-today rendering, and it is a first-class state rather than an
afterthought: most days you look at are not today.

**Accessibility:** the band is `Modifier.clearAndSetSemantics` with one spoken sentence built from
the four strings — four separate unlabelled text nodes read as noise.

### 1.4 `NimazDayRail`

**Why:** nothing in the repo renders a horizontal day strip. It is a leaf with no domain knowledge —
it receives already-formatted labels and a marker per day.

```kotlin
data class NimazDayRailItem(
    val weekdayLabel: String,     // "M"
    val dayLabel: String,         // "13"
    val marker: NimazStatusDotSpec? = null,
    val isToday: Boolean = false,
    val enabled: Boolean = true,  // future days render dimmed
    val contentDescription: String,
)

@Composable
fun NimazDayRail(
    days: List<NimazDayRailItem>,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
)
```

Seven equal-weight cells in a `Row` — not a `LazyRow`. The week is a fixed seven, and a lazy list
here would only add scroll state to lose. Each cell is `selectable(role = Role.Tab)`;
`selectableGroup()` on the row.

### 1.5 `NimazStatusDot`

**Why:** `NimazLegendItem` hand-draws a *filled* dot. The prototype needs an **outlined** dot for
"not fasting" — a day logged as not fasted is not the same as a day with no record, and a missing
dot cannot say that. Used in three places: rail markers, calendar indicators, legend swatches.

```kotlin
enum class NimazStatusDotStyle { FILLED, OUTLINED }
enum class NimazStatusDotSize(val diameter: Dp) { SMALL(6.dp), MEDIUM(7.dp), LARGE(10.dp) }

data class NimazStatusDotSpec(
    val tone: NimazTone,
    val style: NimazStatusDotStyle = NimazStatusDotStyle.FILLED,
)

@Composable
fun NimazStatusDot(
    spec: NimazStatusDotSpec,
    modifier: Modifier = Modifier,
    size: NimazStatusDotSize = NimazStatusDotSize.MEDIUM,
    contentDescription: String? = null,
)
```

`NimazLegendItem` is refactored to delegate to it — same public API, so no call site changes.

### 1.6 One additive extension, not a new component

`CalendarDayState` gains `indicatorStyle: NimazStatusDotStyle = FILLED` and
`CalendarLegendItem` gains the same, so `NimazCalendar` can draw the hollow "not fasting" dot. Both
default to today's behaviour, so every existing caller is byte-for-byte unaffected.

---

## 2. Screen structure

### 2.1 `FastTrackerScreen` — one scroll, no tabs

`PrimaryTabRow` is removed. The `LazyColumn` becomes, in order:

1. **Ramadan strip** — Ramadan only. A `NimazTone.PROMINENT` `NimazCard`: "Ramadan" + "Day 12 of 30",
   a gradient `NimazProgressTrack`, and three inline stats (fasted / missed / to go). Replaces
   today's three-item stack of `RamadanBanner` + `NimazStatsGrid` + `RamadanMissedFastsTracker` —
   one strip saying what three components said across a screenful.
   Outside Ramadan and within 30 days, `RamadanCountdownCard` is unchanged.
2. **Week rail** — `NimazDayRail` over the Monday–Sunday containing `selectedDate`.
3. **Day card** — §2.2.
4. **Your month** — `NimazSectionHeader(title, trailingText = "N fasted")` + `NimazCalendar` with
   fasting day states, Ramadan-day backgrounds and the legend (now with an outlined "Not fasting"
   swatch).
5. **Coming up** — `NimazSectionHeader` + a horizontal `LazyRow` of `UpcomingFastCard`s
   (`NimazCard`: when / name / why / footer action). The **derivation is reused verbatim** from
   `RecommendedFastsSection` — next Monday, next Thursday, Ayyām al-Bīḍ, and up to three of
   Ashura / Arafah / six of Shawwāl / mid-Sha'bān. Only the presentation changes.
6. **Make-up row** — a `NimazCard(onClick = …)` with an icon well, "N days owed · £X fidya paid",
   a count `NimazBadge(tone = WARNING)`, and a chevron. Navigates to `Route.MakeupFasts`.

`FastingGoDeeperGroup`, `LogFastButton` and `FastingSubtitles.kt` are **retired**, along with
`FastingSubtitlesTest.kt`. Everything they gated is now visible, and the segmented control replaces
the "Log a fast" button for the selected day.

**A card is a card:** every tappable card uses `NimazCard(onClick = …)`, never a wrapping
`Modifier.clickable` — a wrapping clickable paints a sharp-cornered ripple over the card radius
(CLAUDE.md rule 8).

### 2.2 The day card

```
Thursday 13 August                        [Back to today]
29 Safar 1448  ٢٩ صفر
──────────────────────────────────────────────────────────
🕐 Iftar in 3h 18m
[▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓╎░░░░░]
SUHOOR ENDS 04:31                              IFTAR 20:58
──────────────────────────────────────────────────────────
THIS DAY
[ ✓ Fasted │ ○ Not fasting │ ⊘ Exempt ]
──────────────────────────────────────────────────────────
(Travel) (Owed — make up later)              Add a note ›
```

- Gregorian date + Hijri (Latin **and** Arabic numerals, as today's screen already shows via
  `HijriDateCalculator`). Inside Ramadan the day number takes the slot as a `NimazBadge`, which is
  the existing behaviour and the better fact.
- **Back to today** — `NimazButton(variant = TEXT)`, shown only when `selectedDate != today`.
- **Window** — `NimazWindowTrack`. `progress` is non-null only for today; on any other day the band
  is fully lit with no marker and the lede reads "Fasting window · 16h 27m long".
- **Lede** — driven by `rememberNow(TickResolution.MINUTES)`: before suhoor → "Suhoor ends in …",
  between → "Iftar in …", after → "Fasting window closed".
- **Status** — `NimazSegmentedControl`, `selectedIndex` derived from the selected day's record and
  `null` when there is none.
- **Footer** — `NimazChip`s for the exemption reason, "Owed — make up later" (`MAKEUP_DUE`), or
  "Not logged yet", plus an "Add a note" text button.

### 2.3 Interaction

| Action | Effect |
|---|---|
| Tap **Fasted** / **Not fasting** | Writes immediately: `FastingEvent.SetFastStatus(selectedDate, status)`. Tapping the already-selected one clears the record (matching the prototype's toggle-off). |
| Tap **Exempt** | Opens the **reason sheet** — `ExemptionReason` chips + Save. Saving writes `EXEMPTED` with the reason. |
| Tap **Add a note** | Opens the **note sheet** — one text field + Save. |
| Tap a rail day / calendar day | `SelectDate` — the day card retitles and rebinds. It does **not** open a sheet. |
| Tap a Coming up card | Selects that date and logs it as `FASTED`. |
| Tap the make-up row | Navigates to `Route.MakeupFasts`. |

**Fast type becomes inferred**, not picked: a Ramadan day writes `FastType.RAMADAN`, any other day
`FastType.VOLUNTARY`. The consequence, stated rather than buried: **`FastType.EXPIATION` and
`FastType.VOW` become unreachable from the UI.** Existing records keep their stored type and nothing
is migrated or deleted, but those two can no longer be *set*. This was a deliberate call to keep the
day sheet from staying heavy; if either turns out to matter, the note sheet is where a type picker
would go back.

`FastManagementBottomSheet.kt` is replaced by two small sheets — `FastExemptionSheet` and
`FastNoteSheet` — both on `NimazBottomSheet`. The delete affordance survives as clearing the status
by re-tapping it.

### 2.4 `MakeupFastsScreen` — a real destination

`MakeupFastsTab.kt` becomes `MakeupFastsScreen.kt`, wrapped in `NimazScreenScaffold` +
`NimazBackTopAppBar("Make-up fasts")`, and split into two `NimazSectionHeader`ed groups:

- **Owed** — pending fasts, each with the original date, its reason, and a "Mark done" action.
- **Settled** — completed ("Fasted 12 June") and fidya-paid ("£24.00") fasts.

The existing complete / update / pay-fidya behaviour is carried over unchanged; only the chrome and
the grouping are new.

---

## 3. Navigation

One new destination:

- `Route.MakeupFasts` — `@Serializable data object`, in the fasting group.
- `ScreenTags.MakeupFasts`.
- `taggedComposable<Route.MakeupFasts>(ScreenTags.MakeupFasts)` in `NavGraph`, wired with
  `onNavigateBack = { navController.popBackStack() }`.

`Route.FastingHome`, `Route.FastingTracker` and `Route.FastingStats` keep rendering
`FastTrackerScreen` exactly as today — this branch does not untangle that three-route knot, and
pretending it does would hide a second change inside this one.

`docs/NAVIGATION.md` §3 gains the route and the destination count is bumped. The §2 mermaid map
gains one edge and is validated with `node scripts/check_mermaid.mjs`.

Because `ScreenTags` changed, `./gradlew :app:assembleDebugAndroidTest` is part of the gate —
`FeatureNavigationTest` names those constants directly and none of the four usual gates compile the
instrumented source set.

---

## 4. State and the ViewModel

`FastingTrackerUiState` gains:

```kotlin
val selectedRecord: FastRecord? = null,        // the record for selectedDate
val weekRecords: List<FastRecord> = emptyList(),
val selectedSuhoorAt: kotlin.time.Instant? = null,
val selectedIftarAt: kotlin.time.Instant? = null,
val isSelectedToday: Boolean = true,
```

`todayRecord`, `suhoorAt` and `iftarAt` stay for today's own facts; the `selected*` fields are what
the day card binds to.

**New events:** `SetFastStatus(date, status)`, `SaveExemption(date, reason)`, `SaveNote(date, note)`.
**Retired:** `ToggleTodayFast`, `OpenFastSheet`, `DismissFastSheet`, `SaveFastForDate` — the last of
which is superseded by the three above.

`selectDate(date)` additionally:
- loads that date's record,
- loads that Monday–Sunday's records through `getFastRecordsInRange` (a week spans two months, which
  `calendarState.records` structurally cannot cover),
- loads that date's schedule through `prayerUseCases.getDaySchedule(date, settings)` — the use case
  already takes a date; only the ViewModel's hardcoded `todayProvider.today()` stood in the way.

Layer rules hold throughout: the ViewModel injects `FastingUseCases`/`PrayerUseCases`, exposes
`StateFlow<…UiState>` with a single `onEvent`, and no entity or DAO reaches the screen.

**Clock discipline:** no `LocalDate.now()` at composition. "Today" comes from `TodayProvider` in the
ViewModel; "now" comes from `rememberNow` at the leaf. This is the shape §0 of the fasting registry
entries already fought for, and the redesign must not reintroduce a frozen today.

---

## 5. Strings

Every new label is a `strings.xml` entry with a `fasting_` prefix, and any string read inside a
composable uses `stringResource`, never `context.getString` — lint's
`LocalContextGetResourceValueCall` is an error and does not re-resolve across a configuration
change. New strings must be added to every shipped locale or `MissingTranslation` fails `lintDebug`.

---

## 6. Testing

| Layer | What |
|---|---|
| Atoms | Robolectric tests per atom: renders, reports the selected index, invokes `onSelect`, coerces out-of-range progress, `selectedIndex = null` selects nothing, outlined vs filled dot |
| ViewModel | `SetFastStatus` writes and re-tapping clears; `SaveExemption` stores the reason; `selectDate` loads the record, the week and that day's schedule; `isSelectedToday` flips |
| Screen | The existing `presentation/screens/fasting` tests, updated; `FastingSubtitlesTest` deleted with its subject |
| Instrumented | `FeatureNavigationTest` gains `ScreenTags.MakeupFasts` |

---

## 7. Gates

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:assembleDebugAndroidTest    # ScreenTags changed
python3 scripts/check_docs.py
npm install --no-save mermaid jsdom && node scripts/check_mermaid.mjs
```

Docs updated **in the same commits**: `docs/NAVIGATION.md` §2/§3 (route + count),
`docs/ARCHITECTURE.md` §8 (the five atoms and the `NimazCard(onClick)` / `NimazButton` rules they
must obey).

---

## 8. Explicitly out of scope

- Migrating the eight existing `LinearProgressIndicator` sites onto `NimazProgressTrack` — a follow-up.
- Retiring `NimazPillTabs` in favour of `NimazSegmentedControl` — they are different components with
  different jobs; consolidating them is its own decision.
- Untangling `Route.FastingHome` / `FastingTracker` / `FastingStats` all rendering one screen.
- Ramadan-mode visual theming beyond the strip in §2.1.

---

## 9. Order of work

The atoms are phase one and are **reviewed on their previews before anything integrates them**.
Nothing in §2 onwards begins until that review passes.

1. Five atoms + `CalendarDayState`/`CalendarLegendItem` extension + previews + tests. **Review gate.**
2. ViewModel state, events and per-day loading + tests.
3. `MakeupFastsScreen` + route + `ScreenTags` + `NavGraph` + `NAVIGATION.md`.
4. `FastTrackerScreen` rebuild; retire `FastingGoDeeperGroup`, `FastingSubtitles`,
   `FastManagementBottomSheet`; add the two small sheets.
5. Docs, then the full gate list in §7.
