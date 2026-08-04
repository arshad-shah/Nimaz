# Notifications rework — implementation spec

**Repo:** `arshad-shah/nimaz` · **Base:** `dev` · **Written against:** `3b039ca`
**Prototype:** `nimaz-notif-system.html` — the system map in it is the source of truth for
screen shape, row copy and which surfaces are sheets.
**Scope:** the notifications subsystem only. More, Zakat, Fasting and the Qur'an thematic work
are separate specs and must not be touched here.

---

## 0. How to work from this document

This spec was written by reading the repo at `3b039ca`. **`dev` has probably moved.** Every
factual claim below is a claim to re-check against the working tree, not a fact to build on.

Precedence, in order:

1. **`CLAUDE.md` and `docs/` win over this document.** If this spec asks for something that
   breaks a non-negotiable rule (layer direction, `NimazCard`/`NimazButton` for anything
   tappable, no raw `Color(0xFF…)`, `XxxUseCases` injection, type-safe routes), the rule wins.
   Say so and stop — don't satisfy both.
2. **Where this spec states current behaviour, verify it first.** Each phase opens with a
   *Verify* block giving the exact check and the expected finding. A mismatch means someone
   changed this area — stop, report, ask. Never adapt silently.
3. **Where this spec states intent, follow it.** The design is settled and signed off against
   the prototype. If something turns out to be unbuildable, say why and propose an alternative
   before writing around it.
4. **Uncertainty is reportable, not guessable.** Missing component, conflicting sections,
   ambiguous check — ask. A wrong guess costs more than a question.

---

## 1. Progress ledger — do this before anything else

Sessions run out of context. The ledger is what lets a fresh session pick up mid-work without
re-deriving everything or redoing finished phases.

**Path:** `docs/specs/notifications-rework/PROGRESS.md`, committed to the branch.

### At the start of every session, without exception

1. `git log --oneline -15` on the working branch.
2. Read `PROGRESS.md` in full.
3. Read the *Verify* block of the phase the ledger says is current, and run it.
4. State back, in one short paragraph: which phase is current, what the last session finished,
   what the next action is, and whether the verify block still holds.

Do not write code before those four steps. If `PROGRESS.md` does not exist, this is session one
— create it from the template below as your first commit.

### Format

```markdown
# Notifications rework — progress

Branch: <branch>
Spec: docs/specs/notifications-rework/SPEC.md
Last updated: <ISO date> by <session note>

## Status
Current phase: P3 — Prayers screen
Awaiting: device validation from Arshad on P2
Blocked on: nothing

## Phases
- [x] P0  Orientation                      commit abc1234  validated 2026-08-05
- [x] P1  Shared component extensions      commit def5678  validated 2026-08-05
- [x] P2  Hub reshape                      commit 9abcdef  AWAITING VALIDATION
- [ ] P3  Prayers screen
- [ ] P4  Adhan & sound
- [ ] P5  Worship reminders
- [ ] P6  Weekly
- [ ] P7  Diagnostics
- [ ] P8  Full-flow tests

## Decisions taken
- <date> Sunrise moved from the global section to under Fajr — approved by Arshad.
- <date> Per-prayer offset stored as <mechanism> — see P3 investigation note.

## Open questions
- <question, who it is for, when it was raised>

## Notes for the next session
<anything a fresh session would otherwise have to rediscover: a surprising file location,
a test that is flaky, a workaround and why>
```

### Rules

- Update it **at every phase boundary and before any long-running step**, not at the end of the
  session. A session that dies with unwritten progress has lost the work twice.
- One phase is "current" at a time. Never mark a phase `[x]` before its gate has passed.
- `## Decisions taken` is append-only. It is the record of what Arshad approved, and a later
  session must not silently reverse it.
- If you discover this spec is wrong about something, write it under `## Notes` **and** say so
  in chat. The ledger outlives the conversation; chat does not.

---

## 2. Gate protocol — how each phase ends

Every phase ends by stopping and asking for device validation. This is not optional and it is
not a formality.

When a phase's code is complete and its automated gates pass:

1. Update `PROGRESS.md`: mark the phase `AWAITING VALIDATION`, record the commit.
2. Post a short message with exactly three things:
   - **what changed**, in one or two sentences;
   - **what to look at on device** — the specific screens and the specific interactions,
     including both themes and the largest font scale;
   - **what you did not do** and why, if anything.
3. **Stop.** Do not begin the next phase. Do not "get a head start". Do not refactor while
   waiting.

Only an explicit approval from Arshad ("looks good", "approved", "go ahead") advances the
phase. Silence is not approval. A comment on a different topic is not approval. If he reports a
problem, fix it within the same phase and re-submit for validation — do not roll it into the
next phase.

Sample message:

> **P2 done — hub reshape.** `NotificationSettingsScreen` is now five rows, each showing live
> state pulled from the existing settings flows. The prayer accordions moved to P3's screen.
>
> On device: open Settings → Notifications. Check the subtitle on every row reads correctly
> against your real settings, that the diagnostic banner appears only when battery optimisation
> is actually on, and that nothing truncates in dark mode at the largest font scale.
>
> Not done: the Prayers screen still has its old contents — that's P3.

---

## 3. Phases

### P0 — Orientation

**Verify**

```bash
git log --oneline -20
ls app/src/main/java/com/arshadshah/nimaz/presentation/screens/settings/
grep -rn "Route.Notification\|Route.Prayer\|Route.Worship" app/src/main/java/com/arshadshah/nimaz/core/navigation/Routes.kt
```

**Expected:** six notification-related screens exist —
`NotificationSettingsScreen`, `PrayerNotificationsScreen`, `NotificationSoundScreen`,
`NotificationWeeklyScreen`, `NotificationTroubleshootingScreen`, `WorshipRemindersScreen` —
and each already has a route. **This rework adds no new routes.** If it looks like you need
one, you have misread the design; stop and ask.

**Do:** create the branch, create `PROGRESS.md`, copy this spec to
`docs/specs/notifications-rework/SPEC.md`, commit. No code.

**Gate:** confirm the six screens and their routes back to Arshad, then proceed to P1 without
waiting — P0 is bookkeeping and needs no device check.

---

### P1 — Shared component extensions

These land in every screen that uses them, so they go first and get validated on their own.

**Verify**

```bash
grep -n "fun NimazAccordion" -A 8 app/src/main/java/com/arshadshah/nimaz/presentation/components/molecules/NimazAccordion.kt
grep -n "data class NimazPickerItem" -A 10 app/src/main/java/com/arshadshah/nimaz/presentation/components/molecules/NimazListPicker.kt
grep -rln "NimazAccordion\|NimazListPicker" app/src/main/java/com/arshadshah/nimaz/presentation/
```

**Expected:** `NimazAccordion(title, modifier, leadingIcon, initiallyExpanded, content)` — no
trailing slot. `NimazPickerItem(value, title, description, icon, iconTint, group)` — no trailing
action. `NimazListPicker` is already rendered inside a `NimazBottomSheet`. Note every existing
call site; you are changing shared components and each one must still compile and still look right.

**Three changes, all additive:**

1. `NimazAccordion` gains `trailing: (@Composable RowScope.() -> Unit)? = null`, rendered in the
   header after the title. Needed so a prayer row can carry its time and switch. Default null
   keeps every existing call site identical.
2. `NimazAccordion` gains `subtitle: String? = null` under the title, for the summary line
   ("Adhan · 10 min before"). Same reasoning.
3. `NimazPickerItem` gains an optional trailing action so the adhan voice rows can carry a
   preview button. Shape is yours to propose — a `trailingIcon` + `onTrailingClick`, or a slot.
   **Propose it before implementing**; a slot on a data class is awkward and there may be a
   better fit given how `NimazListPicker` builds its rows.

**Also in P1:** `NimazListPicker` draws selection with a `NimazIcon`. Change it to
`NimazCheckbox`. This is a single change that lands in every picker in the app — location
method, calculation method, translation, adhan — which is the point. Screenshot two or three
other pickers before and after; if it looks wrong anywhere, report before continuing.

**Gate:** device validation. Ask Arshad to check the accordions and at least two unrelated
pickers.

---

### P2 — Hub reshape

**File:** `NotificationSettingsScreen.kt`

**Verify**

```bash
sed -n '100,150p' app/src/main/java/com/arshadshah/nimaz/presentation/screens/settings/NotificationSettingsScreen.kt
grep -rn "notif_hub_" app/src/main/res/values/strings.xml
```

**Expected:** five `NimazMenuItem`/`NimazSettingsItem` rows with static `notif_hub_*` subtitles
that describe rather than report.

**Target:** five rows, each with a subtitle carrying **live state**:

| Row | Subtitle source | Trailing |
|---|---|---|
| Prayers | the Fajr summary — style and offset | `n of 5` |
| Adhan & sound | voice name, vibration, DND | — |
| Worship reminders | static category list | `n on` |
| Weekly | Jumu'ah and khatam on/off | — |
| Diagnostics | static | warning `NimazBadge` when a check fails |

Plus a `NimazBanner(variant = WARNING)` above them, rendered **only when a delivery problem is
actually detected**. If the state needed to detect that isn't reachable from this screen's
ViewModel, say so rather than faking it — a banner that always shows is worse than none.

Delete the `notif_hub_*` strings that no longer describe anything, in all six locales.

**Gate:** device validation.

---

### P3 — Prayers screen (the only phase with new capability)

**File:** `PrayerNotificationsScreen.kt`

**Verify — and this one is an investigation, not a check**

```bash
sed -n '55,70p' app/src/main/java/com/arshadshah/nimaz/presentation/screens/settings/PrayerNotificationsScreen.kt
grep -rn "SetPrayerNotification\|SetAdhanEnabled\|isSoundOn" --include=*.kt app/src/main/java/com/arshadshah/nimaz/
```

`isSoundOn` appears to be a screen-local UI model field. **Find out how per-prayer notification
state is actually persisted and how the scheduler reads it, and report that before writing
anything.** Everything below depends on it.

**Target**

- Five `NimazAccordion` rows, one per prayer, using the P1 trailing slot for the time and the
  enable switch, and the P1 subtitle for the summary line.
- Body of each: **Alert style** and **Reminder before**, both opening a `NimazListPicker` sheet
  with `autoDismiss = true`. Fajr additionally carries **Sunrise**.
- The global "Additional" section is removed; sunrise moves under Fajr, and the pre-adhan
  reminder becomes per-prayer.

**Two genuinely new capabilities — treat them as such:**

1. **Alert style** replaces the current sound on/off binary with three states: adhan,
   notification only, silent. This changes what the scheduler must honour.
2. **Reminder offset becomes per-prayer.** Today `pre_adhan` is one global value.

Both need a storage and migration answer. **Propose it and get approval before implementing.**
Specifically: what happens to someone who already has a global pre-adhan value set — the
migration must carry it to all five prayers, not silently reset it. Write the decision into
`PROGRESS.md` under `## Decisions taken`.

**Gate:** device validation, and specifically ask Arshad to confirm a notification actually
fires with the new per-prayer settings — this is the phase where the app can break silently.

---

### P4 — Adhan & sound

**File:** `NotificationSoundScreen.kt`

**Verify:** the adhan list is currently rendered inline with preview playback wired to
`SettingsEvent.PreviewAdhanSound` / `StopAdhanPreview`.

**Target:** the inline list becomes a `NimazListPicker` sheet using the P1 trailing action for
preview. `autoDismiss = false` here — you audition several before committing, so the sheet keeps
Cancel/Done. Vibration and DND rows stay exactly as they are.

Preview must stop when the sheet is dismissed by any route, including swipe-down and back.

**Note:** the prototype shows a "Play at alarm volume" row. **I did not verify that setting
exists.** Check; if it doesn't, leave it out and say so — do not invent a preference.

**Gate:** device validation.

---

### P5 — Worship reminders

**File:** `WorshipRemindersScreen.kt`

**Target:** structurally as it is — three sections keyed to `WorshipReminderCategory`. Two changes:

- Reminders with `hasOffset` become `NimazAccordion` rows whose body holds the offset stepper,
  instead of a separate row.
- Ramadan-gated reminders (`ramadanOnly`) currently hide outside Ramadan. Instead, show them
  under a `NimazBanner(variant = INFO)` explaining they stay quiet until Ramadan. **Verify the
  hiding behaviour before changing it** — if something else depends on them being absent, report.

**Gate:** device validation.

---

### P6 — Weekly

**File:** `NotificationWeeklyScreen.kt`

**Target:** Jumu'ah and khatam become `NimazAccordion` rows with their time picker in the body
rather than behind a separate dialog flow. Substance unchanged.

**Gate:** device validation.

---

### P7 — Diagnostics

**File:** `NotificationTroubleshootingScreen.kt`

**Target:**

- Rename to Diagnostics in copy (the file and route stay — no new routes).
- Keep the three existing buttons.
- Add a status list: battery optimisation, notification permission, exact alarms, next scheduled
  alarm. Each row states its real state with a `NimazBadge`. **Only include checks you can
  actually read** — a row that always says OK is a lie.
- **Reset gains a `NimazDialog` confirm.** It currently fires immediately.
- The P2 banner links here.

**Gate:** device validation.

---

### P8 — Full-flow tests

Runs after Arshad has approved every phase. Per `CLAUDE.md` and `docs/TESTING.md` — read both
first and match what is already there rather than introducing a new pattern.

Cover the **flow**, not the composables:

1. **Per-prayer settings round-trip** — set alert style and offset on one prayer, confirm they
   persist across process death and that the other four are untouched.
2. **Migration** — an install with a global pre-adhan value ends up with that value on all five
   prayers, and nobody's setting is lost.
3. **Scheduling honours alert style** — silent schedules no sound; notification-only schedules
   the standard tone; adhan schedules the adhan. Assert against whatever the scheduler exposes.
4. **Offset arithmetic** — a 10-minute offset on Fajr schedules at Fajr minus ten, including
   across a day boundary and a DST change.
5. **Ramadan gating** — `ramadanOnly` reminders are visible in settings year-round but schedule
   nothing outside Ramadan.
6. **Reset** — cancels and rebuilds every alarm, and preferences survive it.
7. **Hub state** — each hub subtitle reflects the underlying setting. A ViewModel-level test is
   enough; don't drive this through the UI.

If a flow can't be tested because the seam isn't there, **say so and propose the seam** rather
than writing a test that asserts nothing.

**Gate:** device validation of the whole subsystem end to end, then the spec is done.

---

## 4. Components

**Reuse. Nothing here is hand-rolled.** The screens are built from:

`NimazScreenScaffold`, `NimazBackTopAppBar`, `NimazSettingsSection`, `NimazSettingsItem`,
`NimazSectionHeader`, `NimazMenuItem`, `NimazMenuGroup`, `NimazAccordion`, `NimazBanner`,
`NimazSwitch`, `NimazCheckbox`, `NimazBadge`, `NimazButton`, `NimazIconButton`, `NimazCard`,
`NimazListPicker`, `NimazBottomSheet`, `NimazDialog`, `NimazNumberStepper`, `NimazTimePicker`.

**No new component is expected.** If you believe one is needed:

1. Search the design system first — it is large, and the assumption that something is missing is
   a claim to verify.
2. If it genuinely doesn't exist, **ask before building it**, with what you searched and why
   nothing fits.
3. Any new or extended component ships with `@Preview` composables in **both light and dark**,
   matching the preview convention in its neighbours. A component without previews is not done.
4. Extended components need a preview of the **new** configuration, not just the old one — the
   `NimazAccordion` trailing slot needs a preview showing a trailing switch.

Rule 8 applies throughout: tappable regions are `NimazCard(onClick)` / `NimazMenuItem` /
`NimazButton` / `NimazIconButton`. Never `Modifier.clickable` on a card.

---

## 5. Strings

- Every new string in `values/strings.xml`, sentence case, active voice.
- **Titles label; subtitles carry the detail.** "Honor Do Not Disturb" → title **Do Not Disturb**,
  subtitle *Silence the adhan while DND is on — banner still shows*. No subtitle where there is
  nothing true to say.
- The existing notification strings are Title Case. Fix them as you touch each screen; don't do
  a separate sweep.
- New strings need the five translations (`de`, `fr`, `id`, `ms`, `tr`); deleted strings come out
  of all six. Check what the last localised commit actually covered rather than assuming.

---

## 6. Documentation

- `docs/SUBSYSTEMS.md` — the notifications section, once P3 changes what is stored.
- `docs/TESTING.md` — if P8 introduces a new test seam.
- `docs/ARCHITECTURE.md` §9 — only if this resolves or introduces a deviation.
- No `NAVIGATION.md` change is expected; if you think there is one, you have added a route,
  which this spec does not permit.

---

## 7. Gates, every phase

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest
```

Plus screenshots of every touched screen in **both themes at the largest supported font scale**.
The prayer accordion header carries name, summary, time and switch — it is the first thing that
will break, and the summary line is the first thing to drop if it does.

Feature branch. **Do not push to `dev`.**

---

## 8. Out of scope — say something rather than doing these

- Adding any route.
- Touching More, Zakat, Fasting, or the Qur'an thematic screens.
- Changing the adhan audio pipeline, the scheduler's alarm mechanism, or `WorshipReminderType`
  itself.
- Rewriting `SettingsViewModel` beyond what P3's storage answer requires.
- Inventing a preference that does not exist because the prototype drew it.
