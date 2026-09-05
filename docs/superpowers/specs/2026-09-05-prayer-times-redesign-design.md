# Prayer Times screen redesign — design

**Date:** 2026-09-05 · **Branch:** `feat/prayer-times-redesign` · **Base:** `dev` @ `06357894`
**Design method:** derived from the shipped redesigns (fasting, prayer tracker, Qur'an, zakat)
rather than from a supplied prototype, then iterated through the brainstorming visual companion.
Mockups persist in `.superpowers/brainstorm/2688-1788613609/content/`.

Prayer Times is the last screen the design revisions did not reach. It also does something no
other screen does any more: it **writes prayer records**, with a vocabulary the prayer-tracker
redesign has since retired.

The redesign gives the screen one job. **Prayer Times answers *when*. The tracker answers *what
you did*.**

---

## 0. What is on `dev` today

Verified by reading the files at `06357894`, not remembered.

| Thing | State |
|---|---|
| `PrayerTimesScreen.kt` | 474 lines. No `NimazScreenScaffold`; a bare `Column` with `.background(surface)` |
| Hero | `PrayerSkyScene`, `260.dp + statusBarTop`, edge-to-edge behind the status bar, glass top bar |
| "Today" chip | `NimazBadge` absolutely positioned at `statusBarTop + 60.dp`; the file comments that it must dodge the top bar's actions |
| Day navigation | `DayNavBar` — prev/next `FilledTonalIconButton`s around a tappable date, inside a `NimazCard` pulled up `28.dp` by a custom `layout {}` |
| Date jump | Tapping the date opens a `NimazBottomSheet` holding `NimazCalendar` |
| Day list | `LazyColumn` of `PrayerTimeCard` (one card per prayer) + a `DayInfoCard` of three `InfoRow`s |
| Day paging | `detectHorizontalDragGestures` + `AnimatedContent` slide, 64.dp threshold |
| **Logging** | `PrayerTimesEvent.TogglePrayer` → `togglePrayer()` (`PrayerTimesViewModel:276`) writes `PRAYED ⇄ NOT_PRAYED` via `prayerUseCases.updatePrayerStatus`, and emits `telemetry.prayerTracked` |
| `PrayerTimesUiState` | 13 fields; already carries `sunriseFraction`, `sunsetFraction`, `moonFraction`, `sunriseAt`, `sunsetAt`, `daylight`, `methodLabel` |
| `daysFromToday()` | Hardcoded English — `"Today"`, `"in $diff days"`. The file's own KDoc admits it |
| `relativeLabel()` | The *same* idea, four lines away, correctly using `pluralStringResource` |
| `DayNavBar` date | A `Row` with `.clip().clickable()` inside a `NimazCard` — the sharp-ripple pattern CLAUDE.md rule 8 names |
| `PrayerTimesScreenTest` | 310 lines, 14 tests; three assert toggle behaviour |
| `PrayerTimesTrackingTest` | 210 lines, entirely about the toggle |
| Routes | `Route.PrayerTimes` / `ScreenTags.PrayerTimes`, registered in `prayerGraph`. **Unchanged by this design** |

### 0.1 What the design system already provides

Reused **unchanged**: `NimazScreenScaffold`, `NimazCard`, `NimazSectionHeader` (has
`trailingText`), `NimazDayRail` + `NimazDayRailItem`, `NimazIconButton`, `NimazIcons`,
`NimazBadge`, `NimazBottomSheet`, `NimazCalendar`, `NimazDivider`, `PrayerSkyScene`,
`rememberNow` / `TickResolution` / `clockTimeText` / `countdownText` / `rememberCountdownTo`,
`getPrayerIcon` / `getPrayerColor` / `getArabicPrayerName`.

**Not** used, deliberately: `NimazWindowTrack` — see §2.4. `NimazTimelineTrack` — the day is a
curve here, not a line.

**Two** primitives the design needs and the system does not have — §2 and §3.

### 0.2 Two documentation facts

- **`docs/NAVIGATION.md:186` is wrong today.** Its mermaid map asserts
  `PrayerTimes --> PrayerTracker & PrayerStats & QadaPrayers & MonthlyPrayerTimes`.
  `PrayerGraph.kt:35` wires `PrayerTimesScreen` with exactly two callbacks — `onNavigateBack`
  and `onNavigateToSettings` — so four of those five edges do not exist and never have on this
  branch's history. This change makes the doc right rather than needing a new entry.
- **Issue #359 names two prayer-tracking surfaces.** `PrayerTimesViewModel:289` carries a comment
  saying it is the *third*, and that a dashboard built on `prayer_tracked` would under-count even
  after the other two were fixed. §1 removes this one, which makes the count correct by
  subtraction rather than by instrumentation.

---

## 1. The reframe: this screen stops writing

### 1.1 The rule

**Prayer Times is a reference surface.** It reads the schedule and renders it. It writes nothing.

Today it writes a **binary** status, `PRAYED ⇄ NOT_PRAYED`. The prayer-tracker redesign
established that `NOT_RECORDED` is not `MISSED`, that `PENDING` / `NOT_PRAYED` / a missing row are
all the same fact — *nobody has said* — and that `LATE` and `QADA` are first-class settable
statuses. Against that vocabulary the toggle here is actively destructive:

- Tapping a prayer the reader logged as `LATE` on the tracker rewrites it to plain `PRAYED`.
  The distinction is gone and nothing announced it.
- Tapping again writes `NOT_PRAYED`, which the tracker now reads back as `NOT_RECORDED` — so a
  double-tap silently converts an assertion into an absence.
- `QADA` is unreachable and un-preservable here.

Two screens writing the same rows in two vocabularies is the defect. Removing the weaker
vocabulary is a smaller change than teaching this screen the stronger one, and it leaves the
tracker as the single place a prayer is asserted.

### 1.2 What changes to make it true

**(a)** `PrayerTimesEvent.TogglePrayer` is deleted from the sealed interface.

**(b)** `PrayerTimesViewModel.togglePrayer()` (lines 276–296) is deleted, along with its branch in
`onEvent`. `prayerUseCases.updatePrayerStatus` and `telemetry.prayerTracked` then have no caller
in this ViewModel.

**(c)** The `statuses` map (`:50`), the `statusJob` / `observe_statuses` subscription that fills it
(`:236–238`) and the `prayerStatus = statuses[…]` assignment (`:262`) are all deleted. Those are
its only three uses besides the toggle, so removing the toggle makes the whole chain dead — and
with it a Room subscription that re-emits on every tracker write, which `:184`'s comment already
names as a source of redundant recomposition on this screen.

`PrayerTimeDisplay.prayerStatus` is a shared presentation model that `HomeScreen` also populates,
so the **field stays**; this ViewModel stops filling it and this screen stops reading it. Nothing
else on this screen consumed it — `PrayerTimeCard`'s `isPrayed` was the only reader, and §3
removes that call site.

**(d)** `PrayerTimesTrackingTest.kt` is deleted in full — it tests only the removed path.

**(e)** `PrayerTimesScreenTest`'s three toggle tests
(`tapping a prayer row toggles that prayer`, `a future day offers no tracking toggles`,
`today's rows do offer them`) are deleted. The remaining eleven are rewritten against the new
composition (§5.2).

**No Room migration, no `PrayerStatus` change, no sync or widget change.** Nothing about how a
prayer is *stored* moves; one screen stops writing.

### 1.3 What the reader loses, and why that is acceptable

A one-tap "I prayed that" from the times screen. It is replaced by nothing on this screen — not by
a navigation affordance either, because a row that looks tappable and merely navigates is the
weaker version of both screens. The tracker is one destination away and is where the other four
statuses already live.

**This is a deliberate, reader-visible removal.** If it proves wrong, the cheap reversal is to
give the row an `onClick` that navigates to the tracker's day card focused on that prayer — noted
in §7 rather than built.

---

## 2. New atom: `NimazSolarArc`

The screen's centrepiece and the one genuinely new drawing in the app. It lands in
`core/ui/.../components/atoms/NimazSolarArc.kt`, following the `NimazWindowTrack` precedent — an
atom built for one feature but general in shape, with light and dark `@Preview`s and a Robolectric
test in `core/ui/src/testDebug/`.

### 2.1 Why an arc at all

The prayer times **are** solar-position events. Dhuhr is solar noon. Sunrise and Maghrib are the
horizon crossings. Asr is a shadow-length ratio on the descending limb. Fajr and Isha sit below
the horizon. A curve with the six prayers marked on it is not a chart *about* the times — it is a
picture of *why they are when they are*, and it is the one thing this screen can show that a list
of six rows cannot.

Three treatments were mocked. The chosen one shows **the whole 24 hours**: a horizon line, the
daylight limb above it drawn solid and gradient-filled, the night limbs below it dashed and muted,
and all six prayers marked. The two rejected alternatives drew daylight only, which silently omits
Fajr and Isha — two of the five prayers, on the screen whose subject is the five prayers.

### 2.2 The geometry — exact, not eyeballed

A symmetric parabola is **wrong**. Sunrise at day-fraction 0.27 and sunset at 0.80 are not
symmetric about clock-noon, so the apex cannot sit at the midpoint of the drawing.

Solar altitude is sinusoidal in the hour angle, and solar noon is midway between sunrise and
sunset *by definition*. So with

```
tDhuhr = (tSunrise + tSunset) / 2
h(t)   = A · cos(2π · (t − tDhuhr)) + B
```

requiring `h = 0` at both crossings and `h = 1` at the apex gives a closed form:

```
c = cos(2π · (tSunrise − tDhuhr))
A = 1 / (1 − c)
B = −c / (1 − c)
```

`h(t)` is then **1 at Dhuhr, exactly 0 at sunrise and sunset, and negative at night** — with no
latitude, declination or date arithmetic. It has a property worth having: the arc's height is a
function of daylight length, so a short December day renders as a genuinely flatter curve than a
June one, for free.

Worked check with `tSunrise = 0.27`, `tSunset = 0.80`: `tDhuhr = 0.535`, `c = −0.0943`,
`A = 0.9138`, `B = 0.0862`; `h(0) = −0.803` (deep night), `h(0.535) = 1.0`, `h(0.27) = 0.0`.

**Night is compressed for drawing.** Raw `h` reaches ≈ −0.8, which would give the trough almost
the visual weight of the day. Negative values are scaled by a `NightCompression` constant
(`0.45f`) before mapping to pixels. This is a drawing decision and is documented as such at the
constant.

**The curve is suggestive, not simulated.** True Fajr and Isha depend on twilight angle, and the
real solar path changes shape with latitude and season in ways this does not model. The atom's
KDoc must say so, and the arc carries no numeric altitude axis, precisely so it cannot be read as
a claim of accuracy.

### 2.3 API

```kotlin
/** One marked point on the arc. `position` is a day fraction in 0f..1f (00:00 → 24:00). */
data class NimazSolarNode(
    val position: Float,
    val label: String? = null,          // null = an unlabelled dot
    val tone: NimazTone = NimazTone.ACCENT,
    val contentDescription: String,
)

@Composable
fun NimazSolarArc(
    nodes: List<NimazSolarNode>,
    sunriseFraction: Float,
    sunsetFraction: Float,
    contentDescription: String,
    modifier: Modifier = Modifier,
    sunPosition: Float? = null,         // null = not today: no sun dot
    litSpan: ClosedFloatingPointRange<Float>? = null,
    height: Dp = NimazSolarArcDefaults.Height,   // 108.dp
)
```

- `sunPosition = null` is the not-today rendering, and **most days a reader looks at are not
  today** — the same reasoning `NimazWindowTrack.progress` documents.
- `litSpan` is the current prayer's window, drawn as a brightened segment of the curve between two
  node positions. This is how the window reaches the reader (§2.4).
- Every colour from `MaterialTheme.colorScheme.*` / `NimazToneColors`. No `Color(0xFF…)`.

**The curve maths is a pure function, not a composable:**

```kotlin
internal fun solarAltitude(t: Float, sunriseFraction: Float, sunsetFraction: Float): Float
```

so §5.1 can test the geometry in a plain JVM test with no Compose, Robolectric or screenshot
involved. Degenerate inputs — `sunrise == sunset`, either outside `0f..1f`, `NaN` — return a flat
zero curve rather than throwing; an arc must never be the thing that crashes the screen, the same
rule `NimazProgressTrack` and `NimazWindowTrack` already coerce for.

### 2.4 Why not `NimazWindowTrack`

The locked layout was "a window card over grouped rows", and `NimazWindowTrack` was the obvious
band for it. The arc **absorbs** it: the current prayer's window is the segment of the curve
between the current and next node, so drawing both a band and an arc would state the same span
twice in one card. `litSpan` replaces the band.

`NimazWindowTrack` keeps its existing fasting caller and is untouched.

### 2.5 Accessibility and font scaling

- The arc is **one** node in the accessibility tree with one spoken sentence; per-node labels are
  cleared from semantics, exactly as `NimazWindowTrack` does for its four text nodes.
- Six labels in ~280dp will collide at large font scales. The labels **drop out** above a
  `fontScale` threshold, leaving the marked dots and the card's text; the drawing must degrade to
  a legible diagram, never to overlapping text. This is asserted in a test at `fontScale = 2.0`.

---

## 3. New molecule: `NimazPrayerRow`

`core/ui/.../components/molecules/NimazPrayerRow.kt`.

The chosen layout groups the six prayers as **rows inside one card**, not as six cards.
`PrayerTimeCard` is a card *per prayer* with a tracking checkbox, and **`HomeScreen` renders it in
its two-column adaptive layout** — so it cannot simply be restyled, and it carries a toggle this
screen no longer wants.

`NimazPrayerRow` is the row form: prayer icon well, English name, optional Arabic name, optional
qualifier (`Jumu'ah`), the time, and a `passed` dimming state. No `onClick`, no toggle — it is a
reference row (§1.1). Rows are separated by `NimazDivider`; the containing `NimazCard` owns the
padding.

`PrayerTimeCard` is **left exactly as it is** for `HomeScreen`. Two prayer idioms coexist because
they do two different jobs; migrating Home to rows is §7, not this branch.

---

## 4. The screen, top to bottom

`PrayerTimesScreen.kt` is rebuilt as `NimazScreenScaffold` + one `LazyColumn`, matching the
shipped screens: `spacedBy(18.dp)`, horizontal padding applied per item so the sky can opt out.

**The sky stays.** It is the app's most distinctive surface and the reason this screen does not
simply become the tracker with different data.

**One departure from the mockups, called out because it was not explicitly chosen:** the sky
becomes the list's **first item** and scrolls, where today it is pinned above a nested scroller.
That is what lets the body be a single `LazyColumn` — the shape every redesigned screen uses —
instead of a `Column` wrapping one. It also reclaims 260dp for the content on a small phone, which
is the fold problem the layout mockup flagged. The cost is that the clock and countdown scroll
away. If the sky should stay pinned, the body stays a `Column` + `LazyColumn` and everything else
in this spec is unchanged; the decision is isolated to this item.

| # | Item | Today | Any other day |
|---|---|---|---|
| 1 | `PrayerSkyScene` | Clock + "Asr in 2h 41m" | Date + "in 7 days" |
| 2 | `NimazDayRail` + jump | Seven days centred on the selection | same |
| 3 | Solar day card | Window sentence, arc **with** sun dot and `litSpan` | Daylight sentence + delta, arc without |
| 4 | `NimazSectionHeader` | "Today · 5 Sep" / trailing `13h 16m daylight` | "Friday 12 Sep" / trailing "Jumu'ah" |
| 5 | Prayer rows card | Six `NimazPrayerRow` | same |
| 6 | `NimazSectionHeader` + info card | Daylight / Sun / Method | same |

**The sky and the card stop repeating each other.** Both currently want to say "Asr in 2h 41m".
The sky keeps the *moment* — the clock and the countdown; the card owns the *window* — which
prayer's time the reader is inside and how far through it they are. The screen says two different
things instead of one thing twice.

**The "Today" chip moves into the glass top bar.** It is a hand-positioned `NimazBadge` at
`statusBarTop + 60.dp` today, with a source comment about dodging the top bar's actions. As a
third pill beside back and settings it needs no magic offset. `PrayerSkyScene` gains an optional
trailing-action slot; its existing callers pass nothing and are unaffected.

**Day navigation.** `DayNavBar` is deleted. `NimazDayRail` replaces it — which is also what
removes the rule-8 `.clickable`-inside-a-card. A rail reaches ±3 days; today's screen reaches any
month through the calendar sheet, so a **jump button at the rail's end opens the existing
`NimazBottomSheet` + `NimazCalendar` unchanged**. Horizontal swipe paging and its `AnimatedContent`
transition are kept — they are the fastest way through a week and cost nothing.

**`daysFromToday()` is deleted.** `relativeLabel()`, four lines away in the same file, already does
the same job correctly with `pluralStringResource`. The English-only duplicate is removed and its
one caller uses the localised one. Its `@StringRes` needs are already satisfied.

---

## 5. Testing

### 5.1 The atom

- **Plain JVM** (`core/ui/src/test/`): `solarAltitude` — apex is exactly 1 at `tDhuhr`; zero at
  both crossings within 1e-4; negative across midnight; a short winter day yields a lower apex-to-
  crossing spread than a long summer day; degenerate inputs (`sunrise == sunset`, out of range,
  `NaN`) return a flat curve and do not throw.
- **Robolectric** (`core/ui/src/testDebug/`): renders with six nodes; `sunPosition = null` draws no
  sun dot; `litSpan` brightens only between its bounds; one merged accessibility node with the
  supplied sentence; labels drop out at `fontScale = 2.0`; light and dark previews.

### 5.2 The screen

`PrayerTimesScreenTest` keeps its eleven non-toggle tests, rewritten against the new composition:
the six prayers are listed as rows; the header names the place and flags a default; the rail
selects a day; the jump button opens the month picker and picking a day selects it; the "Today"
pill appears only off today and returns; swipe pages both ways; missing sun times render
placeholders; the sky's pills navigate. **New:** the card states a window on today and a daylight
delta on any other day; no row exposes a toggle or a click action on any day.

### 5.3 Regression guard

`PrayerTimesViewModelTest` gains one test: **no `PrayerTimesEvent` causes a write through
`PrayerUseCases`.** Verified with a `mockk` that fails on any `updatePrayerStatus` call, and a
`RecordingTelemetry` asserting no `prayerTracked` event — this is the assertion that keeps §1 true
when someone later adds an event to the sealed interface.

---

## 6. Documentation obligations (same commit)

- **`docs/NAVIGATION.md`** §2.3 — correct the mermaid map (§0.2). `PrayerTimes` has no outgoing
  edges to `PrayerTracker`, `PrayerStats`, `QadaPrayers` or `MonthlyPrayerTimes`. Validate with
  `node scripts/check_mermaid.mjs`. No route table change: no `Route` is added, removed or renamed.
- **`docs/ARCHITECTURE.md`** §9 — record the resolved deviation: prayer tracking had three write
  surfaces with two vocabularies; this removes the third and its binary one. §8 gains the
  `NimazSolarArc` / `NimazPrayerRow` bullets alongside the existing `NimazButton` / `NimazCard`
  ones, and the note that `PrayerTimeCard` is Home's idiom and `NimazPrayerRow` the reference one.
- **`docs/CLEAN_ARCHITECTURE_CHECKLIST.md`** — tick the rule-8 `.clickable`-wrapping-a-card item
  for this file; add the "one write surface per fact" observation if not already present.
- **`docs/TESTING.md:880`** — delete the `PrayerTimesTrackingTest` row. It reads *"…it reports as
  `prayer_tracked` — #359's third site, the one that made dashboards under-count"*, describing a
  test and a behaviour this branch removes. Checked, not assumed: this is the only line in `docs/`
  naming that file.
- **`docs/SUBSYSTEMS.md`** — **no change needed.** `:1476` mentions `prayerTracked` only as a
  telemetry event name, not a list of its call sites, so removing one site does not falsify it.
  No Service, Worker, widget, channel, DataStore file or schema version changes.
- `python3 scripts/check_docs.py` before finishing.

## 6.1 Strings and i18n

New strings for: the window sentence, the daylight sentence and its delta, the arc's spoken
description, the rail jump button's `contentDescription`, and the Jumu'ah qualifier. Every one
lands in `core/ui/src/main/res/values/strings.xml` **and all five translations**
(`values-de`, `values-fr`, `values-id`, `values-ms`, `values-tr`) — `lintDebug` fails on
`MissingTranslation`, and it is a real gate via `fastlane`'s `test` lane.

Deleted with `daysFromToday()`: nothing — it was hardcoded English, not resources.

---

## 7. Out of scope, recorded as follow-ups

- **Migrating `HomeScreen` to `NimazPrayerRow`.** Home's two-column layout is tuned to
  `PrayerTimeCard`'s proportions; changing it is a Home change, not a Prayer Times one.
- **A row that navigates to the tracker.** The cheap reversal of §1.3 if the removal proves wrong.
- **`MonthlyPrayerTimesScreen` (843 lines) overlaps this screen more than it used to**, now that
  both are reference-only. Whether the jump button should open it instead of the calendar sheet —
  or whether it should absorb it — is a separate design.
- **Adopting `NimazSolarArc` in `HomeHero`.** Plausible, unasked.

---

## 8. Build sequence

Each phase compiles, tests and is reviewable on its own.

1. **`NimazSolarArc` + its tests**, alone, before any screen touches it — the `NimazSegmentedControl`
   precedent. Includes the pure `solarAltitude` and its JVM tests.
2. **`NimazPrayerRow` + its Robolectric test.**
3. **`PrayerSkyScene` trailing-action slot** — additive, existing callers unchanged.
4. **The write removal** (§1.2 a–e): event, ViewModel method, both test files. Screen still old.
5. **The screen rebuild** (§4) and its rewritten tests.
6. **Docs** (§6) and `check_docs.py`.

**Verification gates:** `:core:ui:check`, `:feature:prayer:check`, `:app:compileDebugKotlin`,
`:app:testDebugUnitTest`, `lintDebug`, `scripts/check_docs.py`. No `Route` or `ScreenTags` entry
changes, so `:app:assembleDebugAndroidTest` is not required — but `PrayerTimesScreen`'s signature
is unchanged too, so nothing in `androidTest` should break either way.
