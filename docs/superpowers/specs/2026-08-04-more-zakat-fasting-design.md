# More, Zakat and Fasting — design

**Date:** 2026-08-04 · **Branch:** `feat/more-zakat-fasting` · **Base:** `dev` (at `88c517ac`)
**Prototype:** `nimaz-navigable.html` (MORE / ZAKAT / FASTING sections)
**Companion:** decisions taken visually in `.superpowers/brainstorm/80019-1785852307/content/`

Three screens, one theme: **stop describing, start reporting.** Every one of them currently
restates what it contains; each should instead say what is true right now.

---

## 1. What is being decided, and what was rejected

Each of these was chosen against alternatives in the visual companion. Recording the losers
because "why not the obvious thing" is the question a later reader will have.

| Decision | Chosen | Rejected, and why |
|---|---|---|
| Amount entry | **Input-only atom** — screen supplies its own label | A full label+input row atom. Zakat is the only caller today, and a row atom fixes an arrangement that only one screen needs. |
| Pinned shortcuts | **Scrolling pill row, capped at 5** | 4-up icon-over-label grid (breaks at the largest font scale and in German/Turkish); 2-up rows (reads as more menu). Uncapped was rejected outright — pinning everything defeats pinning. |
| Zakat total | **Collapsing sticky hero + bottom action bar** | Hero that scrolls away (loses the total during the task). Bottom bar showing the *total* (sits under the keyboard exactly when typing changes it). Showing the total in both places (says €1,284.50 twice; the eye never settles). |
| Zakat share | **Branded `ShareCard`** | Plain text. `shareBranded` already falls back to text when rendering fails, so consistency costs nothing. |
| Fasting log | **One `NimazSwitch`** | Two buttons. A switch is a fact about the day; two buttons are commands, and they never made undo obvious. |

---

## 2. More

### 2.1 The plumbing is the work

`MoreMenuScreen` has **no ViewModel**. Every subtitle is a static `stringResource`
(`R.string.prayer_tracker_subtitle` and friends) that restates the title. Making the subtitles
report live state is therefore not a copy change — it is a new presentation slice.

**New:** `MoreViewModel` exposing `StateFlow<MoreUiState>` + `onEvent(MoreEvent)`, injecting a
`MoreUseCases` bundle (rule 2 — never repositories directly). `MoreUseCases` is assembled in
`core/di` with `@Provides`, from use cases that already exist.

Every subtitle must name a real source. Where a row has no true thing to say, it has **no
subtitle** — padding it out by restating the title is the thing being removed.

| Row | Subtitle | Source | Exists? |
|---|---|---|---|
| Prayer tracker | "4 of 5 logged today" | `PrayerUseCases.getTodayPrayerRecords` | ✅ |
| Fasting | "3 makeup fasts pending" | `FastingRepository.getPendingMakeupFasts()` | ✅ |
| Night worship | "Tahajjud in 5h 12m" | `core/util/NextWorshipResolver` | ✅ |
| Khatam | "Juz 7 · 4 days ahead of pace" | `KhatamProgressCalculator` | ✅ |
| Qaida | "Lesson 4 of 21" | `QaidaRepository` lesson progress (`QaidaLessonProgress`) | ✅ |
| Zakat | "Not calculated this year" / "€1,284.50 due" | `ZakatRepository.getAllHistory()`, newest entry vs current lunar year | ✅ |
| Islamic calendar | "13 Sha'ban 1447" | `HijriDateCalculator.today()` | ✅ |
| Allah's / Prophet's names, Prophets, Hadith, Duas, Tafseer, Qibla, Monthly times | *(none)* | — | — |

No new repository or DAO work. Every figure is already computed somewhere; this screen is the
first to ask for them together.

**Loading:** a subtitle resolves asynchronously. It renders as **absent** until its value
arrives, never as a spinner or a placeholder dash — a row that briefly says nothing is
honest; one that says "—" reads as a value.

### 2.2 Pinned shortcuts

- A horizontally scrolling row of pills above the first section, **capped at 5**.
- Each pill is a `NimazCard(onClick)` so the ripple respects the corner radius (rule 8).
- Default set: Tasbih, Prayer tracker, Khatam, Zakat.
- Persisted as an ordered `List<String>` of route keys in DataStore
  (`more_pinned_shortcuts`, a `stringSetPreferencesKey` will not do — **order matters**, so
  it is a delimited string, registered in `PreferenceCodec` like every other key).
- The pencil opens a `NimazBottomSheet` listing pinnable destinations with `NimazCheckbox`.
  **At the cap, unpinned rows are disabled** and the sheet header reads "5 of 5 pinned", so
  unpinning is the obvious next move. Tapping a disabled row does nothing — no silent
  ignore, no "which one should I replace?" interruption.

### 2.3 Unchanged deliberately

Settings stays a top-right `NimazIconButton` — the Android convention, it costs no list
space, and in the list it would compete with destinations. Zakat sits permanently under
Tools with no Ramadan gating.

---

## 3. Zakat

### 3.1 Layout

```
NimazBackTopAppBar  ─ title "Zakat", subtitle "Lunar year ending 1 Ramadan"
                      action: History → NimazIconButton (an archive, not a step in the form)
┌─ sticky ────────────────────────────────────────────┐
│ ZakatSummaryHero    full: amount + nisab pill +      │
│                     3 stat tiles + "2.5% of …"      │
│                     compact (scrolled): one line     │
└─────────────────────────────────────────────────────┘
  NimazAccordion  Assets    → subtotal in header
  NimazAccordion  Deducted  → subtotal in header
  NimazAccordion  Nisab     → threshold in header
┌─ bottom bar ────────────────────────────────────────┐
│ [ Save this year's zakat ]  [ ↗ share ]             │
└─────────────────────────────────────────────────────┘
```

- **Hero collapse** is driven by the list's `firstVisibleItemScrollOffset` crossing a
  threshold, not by a scroll-delta accumulator — the latter drifts and can leave the hero
  stuck half-collapsed. `animateFloatAsState` between the two states; the tiles and the
  percentage line fold away, the amount shrinks, the amount never disappears.
- Each accordion header carries its **running subtotal**, so the structure is legible
  before anything is opened.
- Reuses `NimazAccordion`'s `subtitle` and `trailing` slots added in #351 — no component
  change needed.

### 3.2 `NimazAmountInput` (new atom)

Currency-aware numeric input, **input only**; the screen composes its own label and hint.
Replaces the hand-rolled `BasicTextField` + `decorationBox` at `ZakatCalculatorScreen.kt:677`.

- `value: String`, `onValueChange: (String) -> Unit`, `currencySymbol: String`,
  `enabled`, `placeholder` defaulting to `0.00`.
- Decimal keypad; thousands separators applied as you type; an empty field shows a muted
  `0.00` rather than sitting blank.
- Ships `@Preview` in light **and** dark, including the empty state.
- The currency symbol comes from the user's existing Zakat setting — it is not a new
  preference.

### 3.3 Save and share

The bottom bar is where Save and Share live, because the screen has nowhere good for them
today. An action under the keyboard is fine: it is pressed *after* typing, not during.

- **Save** → `ZakatRepository.insertCalculation(entry)`; already exists.
- **Share** → follows the app's one pattern exactly: a new
  `Shareables.zakat(context, …)` returning a `Shareable` with a `ShareCard`, handed to
  `ContentShareManager.shareBranded(...)`. `ShareCard.arabic` is nullable, so the card is
  eyebrow "Zakat", the breakdown as `body`, and the lunar year as `attribution`. Every
  string resolved from resources inside `Shareables`, like every other builder — call
  sites never assemble share text.

**Privacy:** this shares someone's personal finances. It is reachable only by an explicit
tap, nothing is pre-filled, and the figures are exactly what is on screen.

---

## 4. Fasting

- **Countdown hero** — `NimazCard`, Hijri date, time to Maghrib, and when suhoor ended.
  Driven by the existing prayer-times source; no new calculation.
- **"Fasting today"** — one `NimazSwitch` replacing today's two buttons. Subtitle reports
  "Logged · tap to undo" or "Not logged yet".
- **Go deeper** — `NimazMenuGroup`: Fasting calendar ("18 fasted this month"), Recommended
  fasts ("Ayyam al-Beed starts in 4 days"), Makeup fasts with a **`NimazBadge` count**
  rather than the number buried in prose.
- **Ramadan** — its own group; "Begins in 17 days" from `HijriDateCalculator`.

Same subtitle rule as More: every one names a source, and a row with nothing true to say
gets none.

---

## 5. Strings and locales

- New strings in `values/strings.xml`, sentence case, active voice; **titles label,
  subtitles report**.
- Translated into the five shipped app locales: `de`, `fr`, `id`, `ms`, `tr`.
- Counts use `plurals`, never `"%1$d fasts"` — Turkish and Malay do not pluralise like
  English.
- Store release notes are a **different set**: `fastlane/metadata/android/<locale>/changelogs/`
  covers **nine** locales (`ar`, `de-DE`, `en-GB`, `en-US`, `fr-FR`, `id`, `ms`, `tr-TR`,
  `ur`) — wider than the app's UI locales. Written once the screens land, describing only
  what actually shipped.

---

## 6. Testing

Per `docs/TESTING.md`, matching what is already there.

| What | Where | Kind |
|---|---|---|
| Each More subtitle maps its source to the right string, and absent state renders no subtitle | `test/.../MoreSubtitlesTest` | JVM, pure mapper |
| Pin cap: adding at 5 is refused, order is preserved, round-trips through DataStore | `androidTest/preferences/PinnedShortcutsTest` | instrumented |
| `Shareables.zakat` builds the expected card + plain text | `test/.../ShareablesZakatTest` | JVM |
| Zakat arithmetic (net zakatable, nisab comparison, 2.5%) | existing `ZakatDaoTest` + Zakat VM tests | already covered |
| Hero collapse threshold | not tested — scroll-linked animation, validated on device | — |

The subtitle mapper is deliberately a **pure function** from state to
`@StringRes Int` + args, exactly like `NotificationHubSubtitles` from #351, so "the row says
the wrong thing" is catchable off-device.

---

## 7. Documentation

- `docs/ARCHITECTURE.md` — `MoreViewModel` / `MoreUseCases` follow the existing recipe; no
  new pattern, so §9 only changes if something deviates.
- `docs/SUBSYSTEMS.md` §6 — the new `more_pinned_shortcuts` preference key.
- `docs/NAVIGATION.md` — **no change expected.** No route is added; the pinned pills
  navigate to routes that already exist. If a route appears, something has gone wrong.
- `scripts/check_docs.py` must pass.

---

## 8. Out of scope

- Any new route.
- Reworking Zakat's calculation rules — only its presentation changes.
- The Ramadan screen behind "Ramadan 1447"; this spec covers the row, not the destination.
- Touching the notifications or Qur'an work merged in #351.
