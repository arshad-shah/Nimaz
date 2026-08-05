# More, Zakat and Fasting — design

**Date:** 2026-08-04 · **Revalidated:** 2026-08-05 against `dev` at `29fea675`
**Branch:** `claude/pr-spec-system-build-j30iwr` (was `feat/more-zakat-fasting`) · **Base:** `dev`
**Prototype:** `nimaz-navigable.html` (MORE / ZAKAT / FASTING sections)
**Companion:** decisions taken visually in `.superpowers/brainstorm/80019-1785852307/content/`

Three screens, one theme: **stop describing, start reporting.** Every one of them currently
restates what it contains; each should instead say what is true right now.

---

## 0. Pre-flight — done, and what it found

This spec was written against `dev` at `88c517ac`. §0 required re-validating every claim before
writing code, and **stopping to report rather than adapting silently**. That pre-flight ran on
**2026-08-05 against `dev` at `29fea675`**; this section is the report, and the rest of the spec
has been amended to match. It is kept rather than deleted because it is the record of *why* the
design below differs from the one that was reviewed.

### 0.1 The seven original claims

| Claim | Finding on `29fea675` |
|---|---|
| `MoreMenuScreen` has no ViewModel | ✅ **still true** — no match for `ViewModel`; all 20 subtitles are static `stringResource` |
| ViewModels inject `XxxUseCases`, not repositories | ✅ still the rule, and now enforced harder — see §0.2 |
| Every subtitle source exists | ✅ all seven — `PrayerUseCases.getTodayPrayerRecords`, `FastingRepository.getPendingMakeupFasts`, `NextWorshipResolver`, `KhatamProgressCalculator`, `QaidaLessonProgress`, `ZakatRepository.getAllHistory`, `HijriDateCalculator` |
| Zakat hand-rolls its amount input | ✅ one `BasicTextField`, now at `ZakatCalculatorScreen.kt:848` (the file grew to 1081 lines) |
| `NimazAccordion` has `subtitle` + `trailing` | ✅ both present, `NimazAccordion.kt:71`/`:73` |
| Share goes through one catalogue | ✅ `Shareables` has `ayah`/`favorite`/`hadith`/`dua`/`bookmark`/`appInvite`/`text` and still no `zakat(` |
| `PreferenceCodec` guards new keys | ✅ `more_pinned_shortcuts` is absent from `TYPES`, so the codec test fails until it is registered |

**No original claim was falsified.** What did move is the ground underneath them.

### 0.2 What `dev` changed, and what this spec adopts instead of building

Between `88c517ac` and `29fea675` the ViewModel audit (#352) landed as ten PRs. Four of its
outcomes are seams this design must sit on rather than beside — §0 step 4's "adopt the better
seam and amend the spec" case, all four times.

| Landed on `dev` | What this spec now does |
|---|---|
| **#353** ViewModels live in `presentation/viewmodel/<feature>/` with `XxxUiState.kt` and `XxxEvent.kt` as **separate files** | `MoreViewModel` is born as `viewmodel/more/{MoreViewModel,MoreUiState,MoreEvent}.kt`, not one inline file in `viewmodel/` |
| **#363** `core/time/TodayProvider` — `today()` plus a `todayChanges` flow that ticks over midnight | The three date-scoped subtitles ("4 of 5 logged today", "13 Sha'ban 1447", "not calculated this year") hang off `todayChanges`, so More does not report yesterday after midnight |
| **#448/#441** ViewModels take `Telemetry` and use `launchSafely(telemetry, feature, op, onFailure)`; no bare `viewModelScope.launch`, no `Context` | `MoreViewModel` follows `NightWorshipViewModel` exactly |
| **#454** the loading/empty/error contract: `UiError(@StringRes message, kind, details)` on the UiState, rendered by `NimazErrorState` | More has **no error state** — see §2.4. That is a deliberate reading of the contract, not an omission |
| **#436** `SettingsRepository` is sliced into feature seams in `domain/repository/settings/SettingsSeams.kt` | The pin preference goes on a **new `MoreSettings` seam**, not on the 179-member `SettingsRepository` |

### 0.3 The three findings from the PR-#368 review, re-checked

The review comment on #368 reported three spec claims as already false, plus two known-wrong
numbers. **`dev` has since fixed all of them** — #357 and #366 landed.

| Reported as broken | State on `29fea675` |
|---|---|
| "no such thing as a Zakat currency setting" | **Fixed.** `ZakatSettings` seam (`SettingsSeams.kt:107`) has `zakatCurrency` + `setZakatCurrency`, persisted as `zakat_currency`, and `ZakatCalculatorScreen` has a currency picker (#445). §3.2's claim is true again |
| `formatCurrency(amount, currency)` ignores its currency argument | **Fixed.** `NumberFormatUtils.kt:31` sets `format.currency` from the ISO code and falls back to grouped digits + the code rather than throwing |
| hardcoded metal prices no screen can change | **Fixed.** `zakat_gold_price_per_gram` / `zakat_silver_price_per_gram` are persisted `Double` preferences, editable in the form — and they are the first callers this spec's `NimazAmountInput` should serve |
| the fasting toggle is silently dead on a 4-value `FastStatus` | **Fixed, and it settles §4's open question.** `FastingTrackerUiState` now exposes `canToggleToday` and `toggleBlockedReason`; `EXEMPTED`/`MAKEUP_DUE` render the control **disabled with a reason**, not live-and-inert. §4 below adopts that instead of inventing an answer |
| stale makeup-fast count after a deletion | **Fixed** in the #366 series; the count is safe to promote to a badge |
| no Zakat ViewModel tests | **Still true** — `ZakatViewModel` has no test file. §6 no longer claims otherwise |

### 0.4 What is still open

- **`ZakatViewModel` has no tests.** Out of scope here (this spec changes Zakat's presentation,
  not its arithmetic), but §6 must stop claiming the coverage exists.
- **`recalculate()` skipping when every field is blank** (#366 T2) is fixed, so the sticky hero is
  safe to pin the total to the top. Verify on device that clearing the last asset field zeroes it.

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

It is born on the seams `dev` now has (§0.2), not alongside them:

- **`viewmodel/more/{MoreViewModel,MoreUiState,MoreEvent}.kt`** — three files, per #353. A brand
  new ViewModel starting in the shape #353 is migrating everything else *into* means #353 has
  nothing left to move.
- **`TodayProvider.todayChanges`** is one of the combined flows. Three of the seven subtitles are
  date-scoped, and a `LocalDate.now()` read at construction is exactly the frozen-today bug #363
  removed everywhere else; a screen added after that fix must not reintroduce it.
- **`Telemetry` + `launchSafely`**, per #448/#441 — no bare `viewModelScope.launch`, no `Context`
  in the constructor.
- **`NextWorshipResolver`, `KhatamProgressCalculator` and `HijriDateCalculator` stay inside
  `MoreUseCases`**, never in the ViewModel's constructor. Two are concrete `core/util` classes and
  the third is a static `object`; injecting them directly is the category #360 is removing, and it
  would leave `MoreSubtitles` testable while the ViewModel feeding it could not be constructed in
  a JVM test at all.

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
- Reached through a **new `MoreSettings` seam** in `domain/repository/settings/SettingsSeams.kt`,
  not through `SettingsRepository` itself. #436 sliced that 179-member interface up precisely so a
  feature reading one preference cannot see the whole app's configuration; a new preference added
  after that fix belongs on a seam from the start.
- The pencil opens a `NimazBottomSheet` listing pinnable destinations with `NimazCheckbox`.
  **At the cap, unpinned rows are disabled** and the sheet header reads "5 of 5 pinned", so
  unpinning is the obvious next move. Tapping a disabled row does nothing — no silent
  ignore, no "which one should I replace?" interruption.

### 2.3 Unchanged deliberately

Settings stays a top-right `NimazIconButton` — the Android convention, it costs no list
space, and in the list it would compete with destinations. Zakat sits permanently under
Tools with no Ramadan gating.

### 2.4 More carries no error state

#454 gave every screen one contract for loading, empty and error, and the evaluation order is
*error beats empty, loading only wins on a bare screen*. Applied here, all three lose:

- **Loading never wins.** More's content is twenty static rows; it is never a bare screen.
  A subtitle that has not resolved is absent (§2.1's loading rule), which is the whole point.
- **Empty is impossible.** The menu is a fixed list, not a query.
- **Error must not win.** If `getPendingMakeupFasts()` throws, the honest outcome is *the fasting
  row has no subtitle* — not a full-screen `NimazErrorState` covering nineteen rows that are
  perfectly navigable. Blocking a working menu on a failed decoration is a worse screen.

So `MoreUiState` holds **no `UiError`**. Failures go to `Telemetry.failure` and leave the field
null, and the row falls back to absent. This is the contract being followed, not skipped: #454's
rule is that a ViewModel which sets an error must have a screen that reads it, and the corollary
is that a screen with nothing to show for an error must not set one.

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
  preference. Verified 2026-08-05: `ZakatSettings.zakatCurrency` (`SettingsSeams.kt:107`),
  persisted as `zakat_currency`, surfaced by the picker added in #445, and already flowing into
  `ZakatUiState.currency`. The screen passes `Currency.getInstance(state.currency).symbol` — the
  same code `formatCurrency` resolves, so the symbol in the field and the symbol beside the total
  cannot disagree.
- **Also serves the metal-price fields.** `zakat_gold_price_per_gram` and
  `zakat_silver_price_per_gram` became user-editable in #357, and they are money fields typed the
  same way. They get this atom too — one input rule for every amount on the screen.

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

  **The four-state problem is already answered on `dev`.** `FastStatus` has four values, and a
  binary switch can only express two. #366 settled it before this spec was picked up:
  `FastingTrackerUiState.canToggleToday` is true only for `null`/`FASTED`/`NOT_FASTED`, and
  `toggleBlockedReason` names the status when it is false. So on an exempted or makeup-due day the
  switch is **`enabled = false` with the reason as its subtitle** — those are considered states
  recorded in the day sheet, with an exemption reason attached, and one tap must not overwrite
  them. The redesign inherits that; it does not invent an answer, and it does not convert a silent
  no-op into a switch that visibly refuses to move without saying why. Changing such a day stays
  where it already is: the day sheet.
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
| Amount grouping mid-typing (trailing point, second point, three decimals) | `test/.../AmountFormattingTest` | JVM, pure function |
| Each More subtitle maps its source to the right string, and absent state renders no subtitle | `test/.../MoreSubtitlesTest` | JVM, pure mapper |
| `MoreViewModel` assembles state from its use cases, and a failing source leaves that field null | `test/.../MoreViewModelTest` | JVM, fakes |
| Pin cap and ordering as a pure codec (encode/decode, cap on write, unknown key dropped) | `test/.../PinnedShortcutCodecTest` | JVM |
| The new key is registered, so a sync cannot read it back at the wrong type | existing `PreferenceCodecTest` | JVM — fails until registered |
| Pin round-trip through a real DataStore | `androidTest/preferences/PinnedShortcutsTest` | instrumented |
| `Shareables.zakat` builds the expected card + plain text | `test/.../ShareablesZakatTest` | JVM, Robolectric |
| Zakat arithmetic (net zakatable, nisab comparison, 2.5%) | existing domain `ZakatCalculatorTest` | already covered **at the domain layer only** — `ZakatViewModel` still has no test (§0.4). This spec changes Zakat's presentation, not its arithmetic, so that gap is not closed here; it must not be claimed as closed either |
| Hero collapse threshold | not tested — scroll-linked animation, validated on device | — |

The pin cap is tested **twice on purpose**: the rule itself (cap, order, unknown keys) is a pure
codec and belongs in a JVM test that runs on every commit, while the instrumented test proves only
that a real DataStore round-trips it. Putting the rule solely behind an emulator means a broken cap
ships whenever the instrumented suite is skipped.

The subtitle mapper is deliberately a **pure function** from state to
`@StringRes Int` + args, exactly like `NotificationHubSubtitles` from #351, so "the row says
the wrong thing" is catchable off-device.

---

## 7. Documentation

- `docs/ARCHITECTURE.md` — `MoreViewModel` / `MoreUseCases` follow the existing recipe; no
  new pattern, so §9 only changes if something deviates.
- `docs/SUBSYSTEMS.md` §6 — the new `more_pinned_shortcuts` preference key **and** the new
  `MoreSettings` seam alongside the other feature seams.
- `docs/NAVIGATION.md` — **no change expected.** No route is added; the pinned pills
  navigate to routes that already exist. If a route appears, something has gone wrong.
- `scripts/check_docs.py` must pass.

---

## 8. Out of scope

- Any new route.
- Reworking Zakat's calculation rules — only its presentation changes.
- The Ramadan screen behind "Ramadan 1447"; this spec covers the row, not the destination.
- Touching the notifications or Qur'an work merged in #351.
