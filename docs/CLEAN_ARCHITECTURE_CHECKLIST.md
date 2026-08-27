# Clean Architecture — Anti-Pattern Checklist

> **Owns:** the tick-box backlog of clean-architecture violations still present in the codebase,
> each with a detection command that reproduces its count.
> **Update when:** you fix an anti-pattern (tick the box, re-run its detection command), or find
> a new one (add it with a detector).
> **Verified by:** review only — no mechanical check. Each item carries **its own** detection
> command; run it rather than trusting the recorded count.
> **Related:** [`ARCHITECTURE.md`](ARCHITECTURE.md) for the canonical patterns and its §9
> resolved/open registry, [`DOCUMENTATION.md`](DOCUMENTATION.md) for the update contract.

> A **living, tick-box backlog** of clean-architecture violations to chip away at over time.
> Each item is small enough to land in its own PR. When you fix one, check its box and, if it
> removes the last instance of an anti-pattern, note it in `ARCHITECTURE.md` §9.
>
> **Never copy an open item as a pattern to follow** — this list exists to be shrunk.

**How to use this**
1. Pick any unchecked box (they're independent — no required order).
2. Apply the fix, run `./gradlew :app:compileDebugKotlin && ./gradlew :app:testDebugUnitTest`.
3. Tick the box in the same PR. Re-run the **detection command** for that section to confirm the
   count dropped (and to catch any new instances that crept in).

Counts below were captured during the architecture-consistency pass — treat them as a starting
snapshot, not gospel. Re-run the detection commands to refresh.

---

## 0. What the module graph now makes impossible

Several rows below describe anti-patterns you can no longer write, because the compiler or a
`check` task rejects them. They are kept rather than deleted, so the detection commands still
document what to look for in a module that has not been split.

| Was a convention | Is now |
|---|---|
| domain must not import `data` | **`:core:domain` is a JVM module.** `import android.*` is a compile error; `androidFreeClasspath` fails on an `androidx` artifact |
| presentation must not import entities or DAOs | **`:core:database` is not on a feature module's classpath.** A screen that reaches for a DAO does not resolve |
| a feature must not reach into another feature | **`moduleBoundary`**, wired into `check` on every Android module, fails the build. Demonstrated failing on all three of its rules in PR 22 of #551 |
| a helper must not leak a persistence type | **`PublicApiHasNoPersistenceTypesTest`** in `:core:data` |
| a test must live with its subject | **`FeatureTestsLiveWithSubjectTest`** in `:app` |

`grep` still finds the shapes; the point is that a PR introducing one now fails before review.


## Contents

| # | Anti-pattern | Detector |
|---|---|---|
| [AP-1](#ap-1--domain-depends-on-the-data-layer) | Domain depends on the data layer | scripted |
| [AP-2](#ap-2--presentation-reaches-into-the-data-layer) | Presentation reaches into the data layer | scripted |
| [AP-3](#ap-3--repositories-expose-room-entities-instead-of-domain-models) | Repositories expose Room entities instead of domain models | scripted |
| [AP-4](#ap-4--business-logic-living-in-viewmodels) | Business logic living in ViewModels | scripted |
| [AP-5](#ap-5--hardcoded-colors--dimensions-in-screens-theme-bypass) | Hardcoded colors / dimensions in screens | scripted |
| [AP-6](#ap-6--data-layer-infrastructure-injected-straight-into-viewmodels) | Data-layer infrastructure injected straight into ViewModels | scripted |
| [AP-7](#ap-7--general-watchlist-no-scripted-detector--review-during-prs) | General watchlist (nested collects, stacking collectors, Get/Observe twins, stubs) | review |
| [AP-8](#ap-8--design-system-drift-hand-rolled-surfaces-bypassed-tones) | Design-system drift (hand-rolled surfaces, bypassed tones) | scripted |
| [AP-9](#ap-9--derived-state-stored-instead-of-computed) | Derived state stored instead of computed | scripted |
| [AP-10](#ap-10--non-lifecycle-aware-state-collection) | Non-lifecycle-aware state collection | scripted |
| [AP-11](#ap-11--lazy-list-items-without-a-stable-key) | Lazy list items without a stable key | scripted |
| [AP-12](#ap-12--shipped-but-unreachable-state-and-settings) | Shipped-but-unreachable state and settings | scripted |
| — | [Quick full re-scan](#quick-full-re-scan) | all of the above |

---

## AP-1 · Domain depends on the data layer

**Rule:** `domain/` must import nothing from `data/` — no Room `*Entity`, no `*Dao`, no
DAO-defined helper types, no DataStore. The domain layer is pure Kotlin + coroutines.

**Why it hurts:** couples business rules to the database schema; you can't change storage or
unit-test the domain without Room on the classpath.

**Detect:**
```bash
grep -rlnE "import com.arshadshah.nimaz.data\." core/domain/src/main/kotlin/
```

Since #556 this is belt-and-braces: `:core:domain` is a `kotlin-jvm` module and `data/` is not on
its classpath, so the import would not compile. The grep stays because it also covers
`src/test` and reads in one second.

- [x] ~~**`PageAyahRange` leak.**~~ **Resolved.** Added `PageAyahRange` to
  `domain/model/QuranModels.kt`; the Room projection was renamed to `PageAyahRangeRow` (kept in
  `QuranDao`) and `QuranRepositoryImpl` maps row → domain via `toDomain()`. `QuranRepository`,
  `QuranUseCases`, `QuranViewModel`, and `QuranPageGrid` now use the domain type. `domain/`
  imports nothing from `data/`.

---

## AP-2 · Presentation reaches into the data layer

**Rule:** ViewModels, screens, and components import only `domain/` types (+ Compose/UI). No Room
entities, no DAOs. ViewModels inject `XxxUseCases`, never a `Dao` or `RepositoryImpl`.

**Why it hurts:** the UI becomes coupled to storage details; swapping or refactoring the data
layer ripples into screens.

**Detect:**
```bash
grep -rlnE "import com.arshadshah.nimaz.data.local.database.(dao|entity)" \
  app/src/main/java/com/arshadshah/nimaz/presentation/
grep -rlnE "private val [a-zA-Z]+: [A-Za-z]+(Dao|RepositoryImpl)" \
  app/src/main/java/com/arshadshah/nimaz/presentation/viewmodel/
```

- [x] ~~**`HomeViewModel` injects three DAOs** (`FastingDao`, `HadithDao`, `DuaDao`).~~
  **Resolved.** Home now injects `FastingUseCases`/`HadithUseCases`/`DuaUseCases`; the daily
  content goes through `GetDailyHadithUseCase`/`GetDailyDuaUseCase` (see AP-4), and seeding moved
  into the repositories. Home no longer imports any `data.local.database.*`.
- [x] ~~**`QuranViewModel` imports `PageAyahRange`** (DAO type).~~ **Resolved** with AP-1.
- [x] ~~**`QuranPageGrid` (organism) imports `PageAyahRange`** (DAO type).~~ **Resolved** with AP-1.
- [x] ~~**`SettingsViewModel` injects `NimazUserDatabase`** — a whole Room database in the
  presentation layer — and writes out eleven `xxxDao()` clears inline for "delete all my data".~~
  **Resolved.** The operation is now `UserDataRepository.clearAllUserData()` behind
  `ClearAllUserDataUseCase`; `UserDataRepositoryImpl` owns the DAO list, and the doc comment about
  never reaching the content corpus moved with it. A new table added to `NimazUserDatabase` now has
  exactly one place to be registered, instead of a list a ViewModel could quietly fall behind on.
  **Detect a regression:**
  `grep -rn "NimazUserDatabase\|RoomDatabase\|Dao\b" app/src/main/java/com/arshadshah/nimaz/presentation --include='*ViewModel.kt'`
  — should return nothing.

---

## AP-3 · Repositories expose Room entities instead of domain models

**Rule:** repository **interfaces** return/accept `domain/model` types only; mapping
(`Entity.toDomain()` / `Model.toEntity()`) lives in the `*RepositoryImpl`.

**Why it hurts:** leaks persistence shapes up through every layer; a column rename becomes a UI
change.

**Detect:**
```bash
# Domain repository interfaces importing a Room entity:
grep -rlnE "import com.arshadshah.nimaz.data.local.database.entity" \
  core/domain/src/main/kotlin/com/arshadshah/nimaz/domain/repository/
```

- [x] ~~`ZakatRepository` exposed `ZakatHistoryEntity`~~ — **resolved** (now `ZakatHistoryEntry`).
- [ ] **Audit pass (watchlist).** Re-run the detect command after any new repository lands; keep
  this at zero. No other current offenders.

---

## AP-4 · Business logic living in ViewModels

**Rule:** orchestration / domain rules belong in use cases. ViewModels translate state ⇄ events
and call use cases; they shouldn't compute domain results from raw data sources.

**Why it hurts:** logic isn't reusable or unit-testable in isolation, and the VM grows into a
god-object.

- [x] ~~**Home "content of the day" rotation.**~~ **Resolved.** Extracted
  `GetDailyHadithUseCase` (Knuth-hash day scatter) and `GetDailyDuaUseCase` (time-of-day category
  + day-of-year rotation, returning a `DailyDuaSelection` domain model). The daily reads go
  through repository methods (`getHadithCount`/`getHadithByOffset`, `getDuasByCategoryOnce`),
  which used to seed-then-read until the content seeders retired at versionCode 385. Behaviour preserved (identical
  queries/selection math; field mappings verified); full unit suite green.
- [x] ~~**Prayer-time calculation assembled in five ViewModels.**~~ **Resolved.** Each injected the
  concrete `PrayerTimeCalculator` (then in `core/util`, now `domain/prayer`) and built its own
  arguments: three of them ran a
  near-identical tower of `combine`s over six preference flows and parsed the persisted strings
  themselves, and the fourth — `FastingViewModel` — skipped the block and took the calculator's
  four defaults, so Fast Tracker ignored every calculation preference the user had set. The
  assembly moved behind `PrayerRepository.observeCalculationSettings()` and the ViewModels now
  observe one `PrayerCalculationSettings`. Pinned by `FastingPrayerSettingsTest`.
- [ ] **General watch:** when a `private fun` in a ViewModel does multi-step computation over
  repository/use-case results (filtering, combining, ranking), consider whether it's a use case.

---

## AP-5 · Hardcoded colors / dimensions in screens (theme bypass)

**Rule:** use `MaterialTheme.colorScheme.*` or `NimazColors.*`; never raw `Color(0xFF…)` in
screens. If a value is missing, add a named token to `NimazColors`.

**Why it hurts:** breaks theming/dark-mode, scatters the palette, and makes rebrands impossible.

**Detect (per-file counts, highest first):**
```bash
grep -rlE 'Color\(0x[0-9A-Fa-f]{6,8}\)' app/src/main/java/com/arshadshah/nimaz/presentation/screens/ \
 | while read f; do echo "$(grep -coE 'Color\(0x[0-9A-Fa-f]{6,8}\)' "$f")  $f"; done | sort -rn
```

Original snapshot was **21 screen files**. Now down to **3**, and those are accepted
design-token / illustration files (see below).

- [x] ~~All 18 feature screens with scattered literals~~ — **Resolved.** Literals across
  `fasting/FastTrackerScreen`, `prayer/{PrayerStats,PrayerTracker,MonthlyPrayerTimes,QadaPrayers}`,
  `settings/{Notification,Appearance,Widgets}`, `help/{HelpContentUi,HelpGuide}`,
  `dua/{DuasCollection,DuaReader}`, `calendar/IslamicCalendar`, `hadith/{HadithCollection,HadithGradeChip,HadithReader}`,
  `quran/QuranHomeScreen`, `onboarding/OnboardingScreen`, and `zakat/*` were relocated into
  `NimazColors` (exact hex preserved — zero visual change). New semantic/categorical tokens were
  added: `Success`, `Warning`, `Info`, `InfoSoft`, `Emerald`, `Sky`, `Purple`, `Pink`, `Amber`,
  `OrangeDark`, `IndigoLight`, `Gray300`, `OnboardingBg*`, plus `HadithCollectionColors`. Prayer
  palette usages map to `NimazColors.PrayerColors.*`.
- [ ] **Accepted (not scattered screen literals) — leave unless a redesign touches them:**
  `tasbih/BeadDesign.kt` and `tasbih/TasbihBeads.kt` (bespoke bead-style gradient palettes — this
  *is* their design-token file) and `onboarding/OnboardingArt.kt` (illustration art). If you do
  tokenize them, add a `NimazColors.TasbihBeadStyles` group under visual review.

- [x] ~~**Ad-hoc `MaterialTheme.colorScheme.*` container colours passed per card.**~~ **Resolved.**
  Screens no longer pick a container role by hand (the sweep found the same "muted panel" written
  as `surfaceVariant.copy(alpha = 0.4f)`, `0.5f` and `0.6f` in different features). Card and badge
  surfaces now take a semantic `NimazTone` and resolve through `NimazCardDefaults.tone()` /
  `NimazBadgeDefaults.colors()`. See `ARCHITECTURE.md` §8.1–§8.2.

- [x] ~~**Per-field `shape`, `textStyle` and `colors` on twelve Material text fields.**~~
  **Resolved.** The app had a dropdown field and an amount field but no *text* field, so every
  form built its own: `AddPresetScreen` set `RoundedCornerShape(14.dp)` on four fields and gave
  the Arabic one a hand-written `TextStyle` plus an `OutlinedTextFieldDefaults.colors` block;
  `KhatamFormScreen`, `TafseerPageContent` and `ChooseDhikrScreen` each picked their own radius.
  All twelve now call `NimazTextField`, which has **no** `shape`/`colors`/`textStyle` parameter —
  geometry belongs to `NimazFieldShell` and typography to `NimazFieldVariant`.
  `MaterialTextFieldGuardTest` fails the build on a raw Material text-field primitive outside the
  family, and on one of those parameters reappearing. See `ARCHITECTURE.md` §8.

  ```bash
  # -P (not -E): the lookbehind is what stops `NimazTextField(` matching. Should print only
  # NimazTextField.kt, NimazSearchBar.kt and NimazNumberStepper.kt — the family's own owners.
  grep -rnP '(?<![A-Za-z0-9_.])(Outlined|Basic)?TextField\s*\(' \
    app/src/main/java/com/arshadshah/nimaz/presentation/
  ```

> Components (`presentation/components/`) also contain literals; many are intentional gradient
> stops. Prefer named tokens, but a dedicated design-token file (e.g. `BeadDesign.kt`) holding
> grouped palettes is acceptable — the anti-pattern is *scattered* literals inside screen logic.

---

## AP-6 · Data-layer infrastructure injected straight into ViewModels

**Rule (aspirational):** cross-cutting infrastructure should sit behind a domain abstraction.

**Why it hurts:** ties many ViewModels to a concrete data/infra class; harder to fake in tests.
This one is **pervasive and lower priority** — listed so it's tracked, not because it's urgent.

- [x] ~~**`PreferencesDataStore` injected directly** into many ViewModels.~~ **Resolved.**
  Extracted a `domain/repository/SettingsRepository` interface (180 members); `PreferencesDataStore`
  now implements it, and `UserPreferences` moved to `domain/model`. All 13 ViewModels + `MainActivity`
  inject `SettingsRepository`; bound via `@Binds` in `SettingsBindingsModule` (`:core:datastore`). Data-layer consumers
  (seeders, sync, workers, `AppInitializer`, `BootReceiver`) keep the concrete class.
- [x] ~~**`SettingsRepository` itself injected whole into 15 feature ViewModels.**~~ **Resolved.**
  The interface that replaced `PreferencesDataStore` was still the entire preference surface, so a
  ViewModel reading three fields could reach all 179 members.
  `domain/repository/settings/SettingsSeams.kt` splits it into nine feature-scoped interfaces that
  `SettingsRepository` extends; DI binds each to the same singleton, so the implementation and the
  DataStore are unchanged. 14 ViewModels now take one seam, `OnboardingViewModel` takes two
  (`AppSettings` + `LocationSettings`), and `PrayerTimesViewModel`/`FastingViewModel` — which
  injected the repository and never read it — take none. `SettingsViewModel` keeps the full surface
  by design. **Detect a regression:**
  `grep -rln "SettingsRepository" app/src/main --include='*ViewModel.kt'` — only `SettingsViewModel`
  should match.
  - [ ] **Minor leftover:** `settings/WidgetsScreen.kt` still *instantiates* `PreferencesDataStore(context)`
    inline instead of going through DI/a ViewModel — convert when that screen is next touched.
    Partially mitigated: the read is now a one-off (`loadPreviewLocation`) rather than being repeated
    inside the preview's 1-second refresh loop, which was constructing the class and re-reading the
    DataStore file every second. The per-tick `buildWidgetPreviewData` is now pure.
- [ ] **Audio managers expose data-layer `AudioState`.** `QuranViewModel` /
  `QaidaReaderViewModel` surface `audioManager.state` (a `data.audio` type) to the UI. This is an
  **accepted pattern** for playback features (see `ARCHITECTURE.md` §9). If desired, mirror the
  fields the UI needs into a domain/UI-state type to drop the `data.audio` import — optional.
- [x] ~~**The managers themselves were public.** `QuranViewModel.audioManager` and
  `SettingsViewModel.adhanAudioManager` were public `val`s, so the accepted "forward the engine's
  flow" pattern had in fact handed screens `play`, `stop`, `setReciter` and `downloadAdhan` —
  and `SelectReciterScreen` used it.~~ **Resolved.** Both are `private`; previewing a reciter is
  `QuranEvent.PreviewReciter` and the adhan screen reads three named `StateFlow`s.
  **Detect a regression:**
  `grep -rn "val \(audioManager\|adhanAudioManager\)" app/src/main --include='*ViewModel.kt'`
  — every match must be `private val`.

---

## AP-7 · General watchlist (no scripted detector — review during PRs)

- [x] ~~**`LocalDate.now()` as a UiState data-class default.** Nine state types defaulted a date
  field to `LocalDate.now()`. A default is evaluated once, when the state object is constructed,
  so the value freezes for the life of the ViewModel — the calendar grid built at 23:59 kept
  highlighting yesterday, and the fasting month grid built on 31 March stayed on March.~~
  **Resolved.** Those fields are required constructor parameters now, so the compiler forces the
  ViewModel to anchor them through `TodayProvider`. **Detect a regression:**
  `grep -rn "= LocalDate.now()" app/src/main --include='*UiState.kt'` — must be empty. (The
  bare name still appears in those files' KDoc, explaining why the parameter is required.)
- [ ] **God ViewModels / mega-state:** a single VM owning many unrelated `StateFlow`s is fine for
  distinct sub-screens (house style), but watch for one VM serving several *features*.
- [ ] **`!!` and unsafe casts** on domain/data boundaries — prefer safe mapping + defaults (see
  the `runCatching { … }.getOrDefault(…)` enum mapping in `ZakatRepositoryImpl`).
- [ ] **Mapping duplicated across VMs:** if two ViewModels both map the same entity→UI shape,
  push the mapping down into the repository (`toDomain()`).
- [ ] **Use cases that only re-expose a repository verbatim across a whole feature:** acceptable
  (the wrapper is the seam the UI depends on), but if a use case adds no value and the feature is
  trivial, that's fine — don't over-engineer net-new tiny features.

### AP-7.15 · `@ApplicationContext` in a ViewModel

A ViewModel holding the application `Context` can reach the entire platform — system services,
the package manager, WorkManager, `Service` start. Six held one; **none do now**.

- [x] ~~**String resolution** (`Bookmarks`, `Quran`, `Home`).~~ **Resolved** via
  `core/text/StringProvider`. Note the first instinct — delete the derived `title`/`subtitle`
  from `UnifiedBookmark` and resolve them in the screen — is **wrong here**: `BookmarksViewModel`
  *searches* and *alphabetically sorts* on those labels, so the comparison happens where the list
  is filtered, not where it is drawn. Moving them would have quietly changed what "Quran" matches.
- [x] ~~**Permissions and battery** (`Home`, `Onboarding`).~~ **Resolved** — the existing
  `PermissionChecker` / `PowerSettings` seams, which already existed and were simply not used here.
- [x] ~~**`getBatteryOptimizationIntent(): Intent`** in `Home` and `Onboarding`.~~ **Resolved** —
  built in the screens. A ViewModel returning an `android.content.Intent` is the arrow backwards.
- [x] ~~**Widget refresh** (`Home` called `PrayerTrackerWorker.enqueueImmediateWork(context)`).~~
  **Resolved** — `WidgetRefresher`. What Home wants to say is "the tracker changed, redraw".
- [x] ~~**Locale and adhan download** (`Settings`).~~ **Resolved** — `AppLocale`, `AdhanDownloader`.
- [x] ~~**A domain use case constructor-injecting an Android class.**~~ **Resolved** —
  `PrayerAlarmScheduler`. `RescheduleNotificationsUseCase` injected the concrete
  `core/util/PrayerNotificationScheduler`, which imports `AlarmManager`, `Context`,
  `NotificationCompat`, `R` and `@ApplicationContext`. The detection command everyone reaches for
  — grep `^import android` under `domain/` — reports **clean**, because the dependency is one hop
  away. Worth remembering as a class of miss: an import census only sees direct edges. The port
  is a sibling of `WidgetRefresher` / `CompassSensors`; the implementation did not move, only the
  direction of the arrow. `preReminderMinutesByPrayer` / `enabledPrayerTypes` moved from
  `core/util/` to `domain/repository/PrayerNotificationPrefs.kt` in the same change — they were
  always pure `SettingsRepository` extensions over domain types.
- [x] ~~**`QiblaViewModel` — the last one, and the only hard one.**~~ **Resolved.**
  `CompassSensors` emits finished orientation (azimuth/pitch/roll + accuracy) and `Haptics` the
  confirmation buzz. The low-pass filtering and the `getRotationMatrix`/`getOrientation` fusion
  are platform math and moved to `AndroidCompassSensors`; the ViewModel kept what is actually
  about the qibla — unwrapping the azimuth past 360→0, applying declination, and deciding when
  the user faces the Kaaba. `smoothInto` was **already** a top-level `internal fun` tested
  directly by `CompassSmoothingTest`, so it moved with the fusion for the cost of one import —
  the concern that the test pinned it inside the ViewModel was wrong.
  Sensor lifetime is now the flow's: `awaitClose { unregisterListener }` replaces an
  `onCleared()` that had to remember. Six new tests cover the unwrap, calibration prompts, stop,
  restart, and the no-compass device — none of which could be written before.
  **Detect:** `grep -rn "ApplicationContext" app/src/main --include='*ViewModel.kt'` — now zero.

### AP-7.14 · A ViewModel that cannot be constructed on the JVM has no tests, and that is why

- [x] ~~**`OnboardingViewModel` built a `FusedLocationProviderClient` in a property initializer.**~~
  **Resolved.** Constructing the ViewModel therefore reached into Play Services before a single
  line of its own logic ran, so no JVM unit test could exist — `DeviceLocationRepository`'s KDoc
  had already named it as the second of the two stuck that way, `LocationViewModel` having been
  freed in #435. It now injects `DeviceLocationRepository`, `PermissionChecker` and
  `PowerSettings`; `Context` is gone entirely, and `getBatteryOptimizationIntent(): Intent` — a
  ViewModel handing the UI an `android.content.Intent` — moved to `OnboardingScreen`, which is
  what `PowerSettings`' KDoc asks for. Seven tests now cover permission reads, detection,
  persistence, the empty fix, the throwing fix, completion and error dismissal.
  A second, quieter blocker went with it: `detectLocation` wrapped its geocode in
  `withContext(Dispatchers.IO)`, a **real** dispatcher no test scheduler can advance. The
  repository implementation already does its own `withContext(ioDispatcher)`, so the wrapper was
  both redundant and untestable.

### AP-7.13 · Raw `viewModelScope.launch` (an uncaught throw is a crash)

`viewModelScope` is a `SupervisorJob` + `Dispatchers.Main.immediate`. A `SupervisorJob`
isolates *siblings* — it does **not** contain an exception thrown inside a child `launch`.
That reaches the thread's uncaught handler, which on Android is a crash, and nothing is
reported on the way out.

- [x] ~~**229 raw `viewModelScope.launch` calls across 21 ViewModels.**~~ **Resolved.** All are
  now `launchSafely(telemetry, feature, "label")`, so a throw is caught, reported to both
  monitoring channels, and cancellation still propagates untouched. Labels are the enclosing
  function in snake_case, so a failure names the operation that produced it.
  `KhatamViewModel` and `OnboardingViewModel` were still calling the static `AppAnalytics` /
  `CrashReporter` rather than the injected seam; both now take `Telemetry`, which is what made
  them convertible. Every ViewModel in the package is now on the seam.
  Twelve of the converted sites set `isLoading = true` and so could strand a spinner on
  failure — those got an `onFailure` that clears it, per the triage in AP-7.12. The rest are
  telemetry-only for the reasons recorded there.
  **Detect:** `grep -rn "viewModelScope.launch" app/src/main --include='*ViewModel.kt'` — should
  return nothing.

### AP-7.12 · A `launchSafely` without `onFailure` is not automatically a defect

`launchSafely` **always reports to telemetry** — that is what it does. A call site without
`onFailure` is therefore silent *to the user*, not silent to monitoring, and whether that is
wrong depends entirely on what the ViewModel does with its state. Triaged all 25 such sites:

- [x] ~~**`BookmarksViewModel.deleteBookmark` wrote the undo state outside the coroutine.**~~
  **Resolved.** The pending-restore entry and the "Deleted — Undo" snackbar were set the moment
  the event arrived, so a delete that threw left the row on screen *with* an Undo beside it —
  and that undo would have re-inserted a bookmark which was never removed. Both now happen
  inside the block, after the delete returns.
- [x] ~~**`ZakatViewModel.loadHistory` could strand its spinner.**~~ **Resolved.**
  `ZakatHistoryUiState.isLoading` defaults to `true` and was cleared only *inside* the collect,
  so a stream failing before first emission span for ever. `onFailure` now clears it.
- [ ] **The remaining 23 are correct as telemetry-only — do not "fix" them.** Their state is
  driven by observed settings/data flows, so a failed write simply does not move the UI: the
  switch stays where it was, the row stays in the list, the entry never joins history. That is
  truthful and self-correcting, and it already reports. Adding `onFailure` to set an `error`
  would in most cases be **error production with no rendering** — `BookmarksScreen`,
  `ZakatScreen`, `SearchSettingsScreen` (outside its consent sheet), `TafseerScreen` and
  `CatalogScreen` never read `state.error`. Only `DuaCategoryScreen`/`DuaReaderScreen` and
  `IslamicCalendarScreen` render one.

**The test to apply per site**, in order: (1) does the ViewModel write state optimistically,
*outside* the coroutine? Then move it inside — the bug is the UI claiming success. (2) Can a
failure strand a loading flag, including one that is a `UiState` **default** rather than an
assignment? Then clear it in `onFailure`. (3) Otherwise, does a screen actually render the
error field? If not, wiring one changes nothing a user can see. Leave it, and record why.

**Detect new call sites:** `grep -rn "launchSafely(" app/src/main --include='*ViewModel.kt'`,
then check each against the three questions above. Note a site whose *inner* flow uses
`catchAndReport` needs no outer `onFailure` — that is the documented pattern, not an omission.

### AP-7.1 · Nested `collect` (silently kills reactivity)

- [x] ~~**Khatam observers in `QuranViewModel` and `KhatamViewModel`.**~~ **Resolved.** Both
  nested a `collect` on one Room Flow *inside* the `collect` of another. `collect` is terminal
  and suspends until the flow completes — a Room Flow never completes — so the outer flow could
  never process a second emission. The UI looked reactive and passed review, but Home and the
  Quran reader stayed pinned to the first active khatam until process death. Use
  `flatMapLatest` (or `combine`) whenever an inner stream depends on an outer stream's value.

Detect:

```bash
# Nested collect within ~12 lines — every hit needs a human look
rg -U --multiline-dotall -n '\.collect\s*\{(?:[^}]|
){0,400}?\.collect\s*\{'   app/src/main/java --glob '*ViewModel.kt'
```

### AP-7.1b · Un-cancelled collectors that stack per navigation

- [x] ~~**`QuranViewModel.loadSurah` / `loadJuz` / `loadPage`.**~~ **Resolved.** Each did a bare
  `viewModelScope.launch { flow.collect { … } }` with no handle kept, so nothing was ever
  cancelled. These collect Room flows, which never complete, so **every surah/juz/page the user
  had opened kept a live collector for the lifetime of the ViewModel**, all writing the same
  `_readerState`. Symptoms are timing-dependent and easy to misread: previous content flicking
  back over the new, or a stale value winning a race after a settings change. Only `searchJob`
  was being tracked and cancelled.

  Fixed by giving the three loaders one shared `contentJob` that each cancels before launching.
  A related fix landed with it: a translation change now re-issues the current load, because the
  ayah queries take the translator id as a *parameter* captured at subscription — an already
  running collector keeps serving the old translation forever.

  Rule of thumb: a `launch { … .collect { … } }` in a ViewModel that can be triggered **more than
  once for the same state** needs a `Job` you cancel, or `flatMapLatest` off the trigger.

  **Follow-up (resolved).** That one shared `contentJob` was right for surah and juz and wrong
  for pages. Page mode is not single-target: the reader's pager keeps the settled page *and* its
  neighbours composed, and all of them request content in the same frame, so each request
  cancelled the one before it and only the last page requested in a frame ever reached
  `pageCache`. The losers rendered as a blank Mushaf frame and stayed blank until they left and
  re-entered composition. Only the ayah-flow editions (Madani) showed it — the line-accurate
  IndoPak layouts render from `mushafPageLayoutCache`, loaded by `loadMushafPageLayout`, which
  never shared a job. Fixed by giving page loads their own `pageJobs: Map<Int, Job>` (one
  collector per page, deduped while in flight, cancelled wholesale when the pages stop being
  valid), and by splitting `QuranEvent.PrefetchPage` off `LoadPage` so a composed-but-not-swiped-to
  neighbour cannot retitle the reader or move the saved reading position.

  Corollary to the rule of thumb: **cancellation scope must match the identity of the request.**
  One handle per *screen* is only correct when the screen shows one thing at a time; a surface
  that legitimately holds several live requests needs one handle each, keyed the way the
  requests are.

- [x] ~~**Same shape in `HadithViewModel`.**~~ **Resolved.** `search` / `searchInBook` were
  re-launched per query with no handle, so typing stacked a collector per keystroke and an
  earlier, slower query could land last and win. `loadBook` / `loadChapter` / `loadHadithById` /
  `filterByGrade` had the same shape per navigation, all writing the same `_readerState`. Fixed
  with three handles chosen by the **identity of the request** (the corollary from the Quran fix
  above): `searchJob`, `chaptersJob`, `readerJob` — one each, because the hadith reader shows one
  chapter at a time, unlike the Quran pager which legitimately holds several pages live. Clearing
  the query now cancels too: without that, the last collector's next emission repopulated the
  results the user had just cleared.
- [x] ~~**The rest of the ViewModel layer — untriaged.**~~ **Swept.** A script split every
  ViewModel into top-level functions and flagged those that `collect` inside an un-assigned
  `viewModelScope.launch`, then split the hits by whether the function takes a parameter (so it
  is re-invoked per value) or not (so it is a one-shot lifetime observer started from `init`).
  The parameterised ones were the bugs; all are now fixed and pinned by tests:
  - `TafseerViewModel.loadTafseerForCurrentAyah` — the worst of them. It runs on **every ayah
    swipe** and launched *two* collectors (highlights, notes) each time, so reading a surah left
    a pair per ayah visited, and annotating any ayah woke all of them. Last writer won, so the
    reader showed another ayah's highlights over the one being read. One `ayahAnnotationsJob`.
    (`TafseerViewModelAnnotationScopeTest`)
  - `DuaViewModel` — five: `loadCategory`, `loadDua`, `loadDuasByOccasion`, `search`,
    `loadProgressForDate`. Handles are keyed by **surface**, not function: `loadCategory` and
    `loadDuasByOccasion` both fill `_categoryState`, so they share one.
    (`DuaViewModelLoadScopeTest`)
  - `HelpViewModel.loadTopic` / `loadGuide` — these collect `language.flatMapLatest { … }`, and
    `language` is a `StateFlow`, so the collector never completes however the inner repository
    flow behaves. (`HelpViewModelLoadScopeTest`)
  - `PrayerTrackerViewModel.loadHistory` — the one range loader without a handle, beside a
    `loadForDate` that has always had `dateRecordsJob`. (`PrayerTrackerViewModelHistoryScopeTest`)
  - `HadithViewModel` — `search` / `searchInBook` / `loadBook` / `loadChapter` /
    `loadHadithById` / `filterByGrade` (fixed earlier, see above).
- [x] ~~**`PrayerTrackerViewModel.loadStats` / `loadQadaPrayers` — the two the sweep's own
  heuristic could not see.**~~ **Resolved.** The sweep above split hits by *"takes a parameter,
  so it is re-invoked per value"* versus *"takes none, so it is a one-shot lifetime observer"*.
  These two take no parameter and are re-invoked anyway: `loadStats` reads its input from
  `_statsState.value.period` and runs from `init`, `SetStatsPeriod`, `LoadStats`,
  `updatePrayerStatus` and `markQadaCompleted`; `loadQadaPrayers` from `init`,
  `LoadQadaPrayers` and `markQadaCompleted`. Two completed qada prayers left **three** live
  collectors on the missed-prayer list (asserted directly, via `subscriptionCount`), and a
  period switch racing an in-flight stats read left week numbers under a MONTH chip.

  **The heuristic to use instead: re-invocability is about the call graph, not the signature.**
  A function whose input comes from `_state.value` is parameterised — the parameter is just
  implicit — and a private loader called from a sibling handler is re-invoked even if no event
  reaches it directly. Both handles are now cancel-and-replace, and the loaders are no longer
  called imperatively after a write: the collector observes the same table, so Room re-emits.
  (`PrayerTrackerStatsTest`)
- [x] ~~**`SyncViewModel` — untriaged.**~~ **Not a defect.** Its single `collect` is a lifetime
  observer started once from `init`, which is exactly what the detect command cannot distinguish
  and what the parameterised/not split above is for. Left as-is.
- [x] ~~**`HadithViewModel.loadHadithByNumber` reads state it just asked for.**~~ **Resolved —
  and it was shipped, not latent.** It called `loadChapter(...)`, which launches, then read
  `_readerState.value.hadiths` on the next line: still the *previous* chapter's list, so
  `indexOfFirst` was always -1 and the branch setting the index could never run. It also passed
  the chapter id raw where `getChapterById` is keyed on the composite `bookId_chapterId`, so the
  header resolved to null.

  This entry previously recorded the path as unreachable because no screen dispatches the event.
  **That was the wrong question.** A `HadithBookmark` stores `bookId` and `hadithNumber` and no
  hadith id, so opening a bookmarked hadith can only go through this path — and lacking a route
  for it, three bookmark screens navigated to `Route.HadithReader(hadithNumber.toString())`,
  putting a *number* in an *id* slot. `getHadithById` resolves that against the **primary key**,
  so every hadith bookmark opened a real hadith from an arbitrary book, with no error.

  The lesson generalises: "no screen emits this event" establishes that the *event* is unused, not
  that the *capability* is. Check what the feature's data model can express first — here the
  bookmark's shape proved the capability was mandatory. Fixed with a `Route.HadithByNumber`
  carrying both parts, and one reader load path shared by all three entry points.
  (`HadithReaderTest`)

Detect:

```bash
# ViewModel launches that collect without keeping a Job handle — each hit needs a human look
rg -n -U --multiline-dotall \
  'viewModelScope\.launch\s*\{(?:[^}]|\n){0,300}?\.collect\s*\{' \
  app/src/main/java --glob '*ViewModel.kt' | grep -v 'Job = viewModelScope'
```

### AP-7.11 · Events nothing dispatches

- [x] ~~**29 event branches that log and that no screen reaches.**~~ **Resolved: 21 deleted, 8
  wired.** Found by `AnalyticsReachabilityTest`, which asks a question no ordinary test can —
  *is there a producer for this branch anywhere in the UI?* — by scanning the source tree. Its
  accepted-backlog set is now **empty**, so the test is a pure ratchet: the next unreachable
  analytics branch fails the PR that introduces it.

  The nineteen deleted as **superseded** each had a wired sibling doing the same job: the fasting
  transitions (`StartFast`/`CompleteFast`/`BreakFast`/`MissFast`/…) against the sheet's
  `SaveFastForDate`, the tasbih session lifecycle against `Increment` managing it automatically,
  `PrayerTrackerEvent.UpdatePrayerStatus` against `MarkPrayerPrayed`/`MarkPrayerMissed`, the Dua
  and Hadith in-feature searches against the global `SearchViewModel`. Two more went with them by
  decision rather than supersession.

  **The tests went with the code.** Five tests and two whole files covered these branches — and a
  test for a branch no user can reach asserts nothing about the product. `TasbihViewModelPresetFilterTest`
  is the clearest case: #364 had already noticed it tested "a preset filter the screen does not
  use".

  The rule: **an event is not reachable because a test dispatches it.** A test is a second
  producer, and it is the one that keeps dead branches looking alive.

- [x] ~~**Three more unreachable branches, found by instrumenting them.**~~ **Deleted.**
  `QuranEvent.MarkAyahsReadForKhatam`, `QuranEvent.UnmarkAyahReadForKhatam` and
  `ZakatEvent.Calculate` had no producer in any screen. They were invisible to the ratchet
  while they logged nothing, and failed it the moment #359 §5's instrumentation was added —
  which is the test working as intended, and a useful warning about the order of operations:
  **instrument, then check reachability, then keep the instrumentation.** All three are
  superseded by a wired sibling (`ToggleKhatamAyah` calls the same two use cases; the zakat
  screen recalculates through `recalculate()` on every entry), so they were deleted rather than
  wired, along with two private handlers left with no caller.

- [x] ~~**Prayer-time computation on `Dispatchers.Main`.**~~ **Resolved.** `calculateMonth` ran
  28–31 passes of solar geometry plus 28–31 Hijri conversions with no dispatcher, on every
  settings emission and every month tap; `recomputeDay` ran a day's geometry synchronously from
  a `Main.immediate` collector, and `publishDisplays` ran a **second** full day for tomorrow's
  Fajr on every publish — including every Room re-emission of the tracker statuses, so toggling
  one prayer recomputed a whole day's astronomy on the UI thread. Worst of all, `ramadanDays()`
  was a **public synchronous** function computing ~30 days, called straight from a click handler.
  All of it moved behind the injected `@DefaultDispatcher`, and the Ramadan export became an
  event whose result lands in state — which fixes the UDF violation at the same time.
- [x] ~~**The widget data sources take `PrayerTimeCalculator`'s defaults.**~~ **Resolved — both
  prayer widgets go through `PrayerRepository`.** `NextPrayerWidgetDataSource` and
  `PrayerTimesWidgetDataSource` called `getPrayerTimes(latitude, longitude)` and took all four
  calculation defaults, exactly as `FastingViewModel` did before the seam existed — Muslim World
  League, Shafi asr, no high-latitude rule, no per-prayer adjustments. Every argument has a
  default, so forgetting them compiled and produced plausible times for the wrong configuration;
  what made this the worst instance is that the widget shows those times on the home screen, with
  no settings screen beside them to contradict it. They inject `PrayerRepository` now and call
  `observeCalculationSettings()` + `getDaySchedule(date, settings)`. Widget workers take the
  repository directly rather than a `XxxUseCases` wrapper — they are not ViewModels, and the rule
  in question is about the presentation layer.
  ```bash
  # Nothing in widget/ should name the calculator at all.
  grep -rn "PrayerTimeCalculator" app/src/main/java/com/arshadshah/nimaz/widget/
  ```
- [ ] **`WidgetsScreen` constructs `PrayerTimeCalculator()` directly.** The five ViewModels that
  injected it now go through `PrayerUseCases`, but the widget-preview screen still calls
  `PrayerTimeCalculator()` in a composable — a screen instantiating a `@Singleton` and computing
  astronomy inline, one layer further out than the deviation that was just closed. It renders a
  preview rather than anything the user acts on, which is why it is listed rather than fixed
  here; route it through the same use case. **This now also diverges from the widgets themselves:**
  the two prayer widgets honour the user's calculation settings, and the preview of them still
  takes the defaults, so the picker can show times the placed widget will not.
  ```bash
  grep -rn "PrayerTimeCalculator()" app/src/main/java/com/arshadshah/nimaz/presentation/
  ```
- [x] ~~**`ZakatEvent.SetCurrency` has no producer.**~~ **Resolved — a picker was wired.** A `NimazListPicker` over `ZakatDefaults.CURRENCIES`, 32 ISO 4217 codes. The list carries **codes only** — `java.util.Currency` resolves the display name and symbol in the user's locale, so nothing there needs translating and no English is added. It was wired into `ZakatCalculatorScreen` first and has since moved, with the event, to `ZakatSettingsScreen` / `ZakatSettingsEvent.SetCurrency` — the currency is a preference, not a figure typed per calculation. Original text follows.

   Not an analytics finding — its branch is a
  `persist { … }` that logs no usage — so the ratchet does not see it, and it surfaced only
  because a neighbouring branch's new logging spilled into the old flat scan window. The
  calculator formats every figure with `state.currency` and offers no way to change it, so a
  user outside the default currency reads someone else's symbol on their own zakat. Wire a
  currency picker or delete the event; it is a product decision, like the eight of #357.
  ```bash
  cd app/src/main/java/com/arshadshah/nimaz
  grep -rn "ZakatEvent.SetCurrency" presentation/screens presentation/components
  ```

  The **eight signed off as features** were wired rather than deleted: a Previous/Continue/Next
  footer in the Qaida reader (`PreviousLesson`, `Resume`), the zakat breakdown's disclosure
  (`ToggleBreakdown`), an all-prayers pre-reminder switch and lead-time picker
  (`SetShowReminderBefore`, `SetReminderMinutes`), grade browsing from the hadith collection
  (`FilterByGrade`, via `Route.HadithByGrade`), editing a custom tasbih (`UpdateCustomPreset`,
  via `Route.TasbihAddPreset`'s new `presetId`), and browsing duas by occasion
  (`LoadDuasByOccasion`, via `Route.DuaOccasion`).

  Two of them needed **more than a producer**, which the wire-or-delete framing had not
  anticipated. `ZakatCalculatorUiState.showBreakdown` was described as already read by the
  screen and was not read anywhere — an AP-12 field as well as an AP-7.11 event. And the
  app-wide pre-reminder pair had been superseded by the per-prayer reminders of the
  notifications rework: nothing but a `BootReceiver` fallback default still reads
  `notificationReminderMinutes`, and the alarms are scheduled from
  `PrayerNotificationPrefs`'s per-prayer map. A control writing only the app-wide pair would
  have changed no notification, so the bulk control writes the five per-prayer settings too.
  **Wiring a producer is not the same as making the branch do something**; where they came
  apart, the second half was the work.

### AP-7.10 · A cache marker recording the request instead of the result

- [x] ~~**`SurahThematicViewModel.load`.**~~ **Resolved.** `loadedSurah` was assigned on *entry*,
  so it named a surah that might never arrive, and the guard that short-circuits on it could not
  tell "already showing this" from "already asked for this". Split into `loadedSurah` (assigned
  on success) and `loadingSurah` (assigned on entry, cleared on failure), which is what lets the
  guard answer both questions correctly: re-sending `Load` for the surah in flight is a no-op,
  and a load that never completed blocks nothing.

  **Correction to #364 R12, which predicts a permanent strand from this.** That state is not
  reachable in the shipped code: `loadJob?.cancel()` appears at exactly one site, inside
  `load()`, on the line *after* the marker is reassigned — so a cancel is always followed by a
  relaunch for the surah just recorded, and the two cannot diverge. The failure path did not
  strand either, because `onFailure` already reset the marker. The real defect is the one the
  issue lists second: a second `Load` for the surah already in flight fell through and re-ran
  the whole three-call load, despite the event's KDoc calling it idempotent. Verified by test in
  both directions. (`SurahThematicLoadGuardTest`)

  The general rule is still worth having: **a marker used as a cache key must record what
  completed, not what was attempted.** Getting that right removes a class of "why is this screen
  stuck" bug rather than one instance of it.

### AP-7.9 · One handle for requests that are not alternatives

- [x] ~~**`QuranTopicsViewModel.toggle` / `focus` / `rebaseTo`, and the catalogue detail load.**~~
  **Resolved.** AP-7.1b says one `Job` per *identity of the request*. The follow-on question is
  what counts as one identity, and it is not "one per function":

  - `focus` and `rebaseTo` are **alternatives** — the browser is at one place at a time, and each
    ends in a whole-state update setting `focus` *and* `level` together. They share `browseJob`,
    cancel-and-replace. Without it, tapping crumb 0 then crumb 2 let the slower query write last
    and the reader landed on the crumb they did not tap.
  - `toggle` is **not** an alternative to another toggle: opening two rows is two intentions, and
    a shared handle would throw away the first row's children. It keeps no handle, and instead
    guards the part of its write that is position-dependent — caching a node's children is valid
    wherever the browser is, *expanding* it is not, so a toggle resolving after a rebase caches
    and does not open.

  And the standing rule from the tracker's stats race: **cancelling is necessary, not
  sufficient.** A coroutine cancelled after its last suspension point still runs to the end of
  its block, so the write is checked against a `requested…Id` set synchronously at the call —
  which `loadSurahSubjects` in the same file already did, while `loadDetail` next to it used a
  whole-object assign that wiped the winner's already-landed previews.

  Note on evidence: the browse races are pinned by failing-first tests
  (`QuranTopicsRaceTest`). The post-suspension-point guard is **not** — awaiting a gate is a
  cancellable suspension point, so a test cannot deterministically hold a coroutine between "read
  returned" and "state written". It is defence, and labelled as such where it appears.

### AP-7.8 · A UiState default answering a question only the repository can

- [x] ~~**`QuranViewModel`'s reader loads read the translator and Mushaf edition off
  `_readerState`.**~~ **Resolved.** A persisted preference has exactly one source of truth — the
  repository. A `UiState` field that *mirrors* one carries a compiled-in default until the
  settings collector's first emission lands, and that emission is disk-bound, so on a cold open
  it generally arrives **after** the screen has already asked for content. Every non-default user
  therefore got the default's content first, then a second full query when the real preference
  turned up: a visible flash and a wasted read on every reader open.

  Two rules come out of it, and the second is the one that bites on the way to fixing the first:

  - Resolve the preference where it lives (`settingsRepository.x.first()`), inside the load. The
    file already did this in `loadVerseOfTheDay`; the reader paths did not.
  - Once the loads read DataStore, **the settings collector's first emission is hydration, not a
    change.** Comparing it against the state's defaults reports a change that never happened —
    which re-issues the load, and for a non-default Mushaf edition repaginates a page number
    *from* an edition the reader was never on. Guard the first emission.

  Detect: a `_state.value.<field>` read as a *query argument*, where `<field>` is also written by
  a settings collector.

  ```bash
  rg -n '_\w+State\.value\.\w+' app/src/main/java --glob '*ViewModel.kt' | grep -i 'get\|load\|search'
  ```

### AP-7.7 · The same ViewModel written three times

- [x] ~~**`AsmaUlHusnaViewModel`, `AsmaUnNabiViewModel`, `ProphetViewModel`.**~~ **Resolved.**
  171, 171 and 172 lines that were **byte-identical after identifier substitution** — same two
  states, same five events, same six handlers, same filter, differing only in what the thing is
  called.

  Triplication does not merely cost lines, it costs correctness twice over. A fix lands in one
  copy and rots in the other two — and nobody writes the same test three times, so between them
  the three had four tests and **none touched the filter**, the one piece with any logic in it.

  Collapsed onto `CatalogViewModel<T>` + a `CatalogSource<T>` per feature, which is the only
  genuinely varying part (where the rows come from, and which fields a search reads). The
  filter now applies inside the single state mutator, so a new emission cannot arrive
  unfiltered — the bug all three were one forgotten `applyFilters()` call away from.

  Detect: two ViewModels whose bodies match after substituting their feature nouns.

### AP-7.6 · A search path nobody can reach

- [x] ~~**Three `search*` use cases, three repository methods, three DAO queries.**~~ **Resolved by
  deletion.** `ProphetUseCases.searchProphets` and the `searchNames` pair on `AsmaUlHusnaUseCases`
  / `AsmaUnNabiUseCases` were declared, constructed by Hilt on every ViewModel creation, backed by
  a repository method and an indexed DAO query — and invoked by nothing. All three ViewModels
  filter in memory instead.

  #357 poses the choice as *delete the SQL path or use it*, and calls the middle state — built,
  reachable by nothing, tested by nothing — the worst option. Deleted, because the corpora are 99
  names, 99 names and 25 prophets: an index earns nothing over a `contains()` sweep at that size,
  and the in-memory filter is the one that actually ships.

### AP-7.5 · Two `when`s over one sealed hierarchy

- [x] ~~**The dual-`when` `onEvent` shape, in 20 of 31 ViewModels.**~~ **Resolved.** An analytics
  `when (event)` ending in `else -> {}`, followed by the real dispatch `when (event)`. The `else`
  means the compiler checks exhaustiveness on the behaviour table only, so **every event added
  afterwards ships with working behaviour and no telemetry** — silently, and permanently, because
  nothing ever fails.

  Not hypothetical: `SettingsViewModel`'s `else` dropped **63 of its 78** events, `ZakatViewModel`
  logged 3 of 24, `TasbihViewModel` 5 of 23, `FastingViewModel` 10 of 24, `HomeViewModel` 2 of 6.

  It also produced logged actions that never happened — Home logged `toggle_prayer_status` before
  `togglePrayerStatus` returned early for Sunrise.

  Collapsed to one exhaustive table per `onEvent`, with the analytics call inside the branch that
  owns it. Detect:

  ```bash
  # Any onEvent with more than one dispatch table:
  for f in app/src/main/java/com/arshadshah/nimaz/presentation/viewmodel/*.kt; do
    n=$(awk '/fun onEvent\(/,/^    }$/' "$f" | grep -c "when (event)")
    [ "$n" -gt 1 ] && echo "$n $f"
  done
  # Any else-fallthrough over an Event hierarchy:
  grep -rn "else -> {}" app/src/main/java/com/arshadshah/nimaz/presentation/viewmodel/
  ```

### AP-7.4 · Pure domain logic held hostage inside a ViewModel

- [x] ~~**`CalendarViewModel`'s seven generators.**~~ **Resolved.** 75 lines of pure functions
  over domain models — `generateGregorianMonth`, `generateHijriMonthDays`,
  `generateGregorianYearMonths`, `getEventsForDate`, `getEventsForMonth`,
  `getUpcomingIslamicEvents`, `getApproximateGregorianDate` — all `private`, all reachable only
  through a ViewModel with mocked use cases. Nothing in them needed a ViewModel: they took
  values and returned values.

  That is why two of the app's sharpest bugs shipped and stayed. Every Islamic event was
  projected into the **current** Hijri year only, so in the last weeks of a Hijri year the
  "upcoming events" list dropped Islamic New Year and Ashura — precisely the events that were
  upcoming — and the month grid dropped every Muharram event from the Gregorian month that
  straddles the boundary. Both are three-line tests once the code is reachable, and neither
  had one.

  Moved to `domain/usecase/calendar/`, with the projection in **one** `IslamicEventProjection`
  so the two callers cannot drift apart. `today` is a parameter, not a clock read, so a grid
  can be rebuilt for a new day. (`CalendarUseCasesTest` — three of its tests fail against the
  single-year projection, verified by restoring it.)

  Rule of thumb: **if a private ViewModel function neither reads state nor writes it, it does
  not belong to the ViewModel.** It is domain logic that happens to live in the wrong file,
  and it is untested for exactly that reason.

### AP-7.2 · `Get*` and `Observe*` variants of the same read

- [x] ~~**Khatam had both `GetActiveKhatamUseCase` and `ObserveActiveKhatamUseCase`**~~ (also
  `getReadAyahIds`/`observeReadAyahIds`, `getJuzProgress` with no Flow variant). **Resolved** by
  **deleting** the one-shot variants rather than documenting them. When both exist, a call site
  can silently pick the stale one and nothing flags it. Keep the one-shot form only where a read
  genuinely cannot be a Flow (e.g. `getNextUnreadPosition`, which joins the ayah table).

Detect:

```bash
# A Get* use case whose Observe* twin also exists
rg -o -N 'class (Get|Observe)(\w+)UseCase' core/domain/src/main/kotlin/com/arshadshah/nimaz/domain/usecase   | sed -E 's/.*class (Get|Observe)(\w+)UseCase//' | sort | uniq -d
```

### AP-7.3 · Stub implementations that satisfy a signature

- [x] ~~**`KhatamRepositoryImpl.getKhatamStats()` returned all zeros**~~ with a "Simplified
  stats" comment while being fully wired through DI. **Resolved** with a real Flow-backed
  implementation. A *missing* method fails at build time; a *lying* one type-checks, ships, and
  fails silently months later in whichever screen finally consumes it. Prefer `TODO()` over
  plausible-looking placeholder data.

---

## AP-8 · Design-system drift (hand-rolled surfaces, bypassed tones)

**Rule:** every card-like surface is a `NimazCard` and every small label is a `NimazBadge`, and
both are coloured by a semantic `NimazTone` — not by a `colors =` / `containerColor =` argument
chosen at the call site. See `ARCHITECTURE.md` §8.1–§8.2.

**Why it hurts:** colour decisions spread back out across ~200 call sites, so a theme change stops
being a one-file edit and light/dark contrast quietly diverges per screen.

**Detect:**
```bash
# Hand-rolled card containers that should be a NimazCard tone:
rg -n 'Card\(|Surface\(|Box\(.*\.background\(' app/src/main/java/com/arshadshah/nimaz/presentation/screens/
```

- [x] ~~**Hand-rolled `Surface`/`Box(clip+background)` cards.**~~ **Resolved.** `NimazSurfaceCard`
  and the private per-screen surfaces were removed; separation is now context-driven (page-level
  `NEUTRAL` + `ELEVATED`, nested `OUTLINED` + `elevation = 0.dp`, selected item = fill).
- [x] ~~**Hand-rolled badge/pill duplicates.**~~ **Resolved.** `NimazLabelChip` (+ its test),
  `TabPill`, `CategoryTab`, `ExampleQuestionChip`, `CitedChip` and `CutoutBadge` all collapsed into
  `NimazBadge`.
- [x] ~~**`JumuahCard`'s hand-rolled `Box` icon well and `Box` divider.**~~ **Resolved.** Replaced
  with `NimazIcon(CONTAINED)` well and `QuranOrnamentalDivider` atoms, improving design-system
  consistency.
- [x] ~~**`PrayerCheckItem`'s hand-rolled `Box` checkbox circle.**~~ **Resolved.** The prayer
  tracker's row drew its own 24dp `Box` with `.background()` / `.border()` and a raw `Check` icon.
  Replaced by `NimazAccordion(style = FLAT)` with a `NimazBadge` status and a
  `NimazSegmentedControl` picker, so the row's tap target and ripple come from the design system.
- [ ] **Cards that need a border still bypass `tone`.** A `NimazTone` resolves container + content
  but **not** a stroke, so any bordered card falls back to an explicit
  `NimazCardDefaults.colors(container = …, border = …)`. 10 files today. Fix by teaching the tone
  resolver about borders, then converting these back.
  ```bash
  rg -n -A3 'NimazCardDefaults\.colors\(' app/src/main/java/com/arshadshah/nimaz/presentation/ | rg 'border'
  ```
- [ ] **`BadgeType` labels are hardcoded English.** Blocks `StatusBadge` adoption anywhere
  localized strings are needed. Convert `label: String` to `@StringRes Int`.
  ```bash
  rg -n 'object \w+ : BadgeType\("' app/src/main/java/com/arshadshah/nimaz/presentation/components/atoms/NimazBadge.kt
  ```
- [ ] **`NimazCard(style = OUTLINED)` silently ignores `elevation`.** The OUTLINED branch renders
  Material's `OutlinedCard`, which has no elevation slot, so the parameter is dropped without a
  warning — the same "lying signature" failure mode as AP-7.3.
  ```bash
  rg -n -B2 -A2 'NimazCardStyle\.OUTLINED' app/src/main/java/com/arshadshah/nimaz/presentation/ | rg 'elevation'
  ```
- [ ] **Untriaged explicit `style = NimazCardStyle.FILLED` call sites** (~47). FILLED is already
  the default; each one is either a deliberate flat card or a page-level card that should be
  `ELEVATED` (in light mode a filled card barely separates from the background). Triage per screen.
  ```bash
  rg -c 'style = NimazCardStyle\.FILLED' app/src/main/java/com/arshadshah/nimaz/presentation/ | sort -t: -k2 -rn
  ```
- [ ] **No visual verification of the tone migration.** The sweep is verified by
  `compileDebugKotlin` + unit tests only. Walk the migrated screens in light and dark (or render
  the `NimazCard`/`NimazBadge` `@Preview` showcases, which cover both themes) before release.

---

## AP-9 · Derived state stored instead of computed

**Rule:** if a value is a pure function of other state (a filtered list, a resolved language, a
total), expose it as a computed `val` **on the UI-state class** rather than storing a field that
every mutation site has to remember to refresh.

**Why it hurts:** stored derived state has to be recomputed at *every* site that touches an
input. Miss one and the UI shows a filter that is on next to a list that ignores it — and
because the two halves disagree silently, it reads as a rendering glitch rather than a bug.

**Detect:**
```bash
# A `filtered*` field on a UI state — each one needs checking against every writer of its inputs
rg -n 'val filtered[A-Za-z]*:' app/src/main/java --glob '*ViewModel.kt'
```

- [x] ~~**`TasbihPresetsUiState.filteredPresets`.**~~ **Resolved.** Recomputed by hand at three
  sites; the two Room collectors in `loadPresets` rebuilt it as `defaults + customs` and never
  consulted `selectedCategory`. Saving or deleting a custom dhikr while a category was selected
  re-emitted the presets flow and silently reset the list to everything, with the category chip
  still reading as selected. Now a computed property. (`TasbihViewModelPresetFilterTest`)
- [x] ~~**`HadithChaptersUiState.filteredChapters`.**~~ **Resolved.** Same defect, second
  feature: `loadBook`'s collector set `filteredChapters = chapters`, ignoring `searchQuery`, so
  any write to the chapters table wiped the user's search while the field kept their text.
  (`HadithViewModelChapterFilterTest`)
- [ ] **Audited and correct — leave alone:** `SearchUiState.filteredResults`,
  `BookmarksUiState.filteredBookmarks`, `DuaCollectionUiState.filteredCategories`,
  `QuranHomeUiState.filteredSurahs`, and the `filteredNames`/`filteredProphets` trio all pass
  the current filter inputs at every write site. They are still *stored*, so they carry the same
  latent risk; convert opportunistically when one is next touched.

---

## AP-10 · Non-lifecycle-aware state collection

**Rule:** composables collect ViewModel state with `collectAsStateWithLifecycle()`.

**Why it hurts:** `collectAsState()` subscribes for the life of the composition, so a
backgrounded screen keeps collecting, keeps waking Room, and keeps recomposing state nobody can
see. It also defeats `SharingStarted.WhileSubscribed()` — the subscriber count never drops, so
those upstreams never actually stop.

- [x] ~~**87 call sites across 54 files** were on `collectAsState()`~~ against 11 files already
  using the lifecycle-aware form — the same class of screen behaving differently depending on
  which spelling its author copied. **Resolved.** Every ViewModel here exposes `StateFlow`, which
  replays to a new collector, so nothing is missed across the pause. Guarded by
  `StateCollectionGuardTest`, which also documents the one case the rule does *not* fit: a
  replay-less `SharedFlow`/`Channel` of one-shot events, which needs `repeatOnLifecycle` and an
  entry in that test's allowlist.

---

## AP-11 · Lazy list items without a stable key

**Rule:** every `items(<collection>)` declares a `key`.

**Why it hurts:** without one, Compose identifies a row by **position**. State remembered inside
a row belongs to the slot rather than the item, so inserting or deleting above it hands that
state to a different row; and every row after a change is treated as new, so the whole tail
recomposes. On lists that rebuild per keystroke (search, locations) that is the difference
between recomposing one row and all of them.

- [x] ~~16 sites~~ — **Resolved**, guarded by `LazyListKeyGuardTest`. Two types gained a stable
  identity rather than having one invented at the call site: `UnifiedSearchResult.key` (prefixed
  per variant, because ayah 12 and dua "12" must not collide in a mixed list) and
  `SearchLocation.key` (coordinates — the same city name recurs across countries).

---

## AP-12 · Shipped-but-unreachable state and settings

**Rule:** a field on a `XxxUiState`, an event on a `XxxEvent`, or a use case must have a reader.
If nothing consumes it, either wire it or delete it — do not leave it as an intention.

**Why it hurts:** it reads as a working feature in review and in the diff, so the missing half
never gets written. Three shipped defects had exactly this shape:

- `KhatamRepositoryImpl.logDailyProgress` was the only writer of `khatam_daily_log` and nothing
  called it, so the streak the detail screen renders was **0 for every user**.
- `QiblaSettingsUiState.trueNorthMode` defaulted to `true` and was settable, but nothing read it
  — the magnetic-declination correction it names was never applied, leaving the qibla needle off
  by up to 25°.
- `WidgetSettingsUiState` exposed a `StateFlow`, four events and four fields that no screen
  collected or dispatched, and that no widget read.

The first two were user-visible wrong values; the third was only weight. The pattern is the same,
and the difference is invisible until someone looks.

**Detect:**

```bash
cd app/src/main/java/com/arshadshah/nimaz
# a UiState field whose name appears in no other file
for n in $(grep -rhoP '(?<=    val )\w+(?=:)' presentation/viewmodel/*.kt | sort -u); do
  [ "$(grep -rl "\b$n\b" --include=*.kt . | wc -l)" -le 1 ] && echo "unread: $n"
done
# a use case referenced only by its own DI construction
for f in domain/usecase/*UseCase.kt; do
  n=$(basename "$f" .kt)
  [ "$(grep -rl "\b$n\b" --include=*.kt . | grep -vc -e "$f" -e core/di/)" -eq 0 ] && echo "only-DI: $n"
done
```

Both are heuristics: a field read only inside its own ViewModel is legitimate (`SearchUiState`
keeps per-type result lists to derive counts), and a use case invoked through a lambda in a DI
module (`ObserveLocalEventsUseCase`) is a false positive. Read the hit before acting on it.

- [x] `khatam_daily_log` streak — **Resolved**, derived from `khatam_ayahs.read_at`; pinned by
  `KhatamProgressCalculatorTest`.
- [x] `trueNorthMode` — **Resolved**, declination applied; pinned by `QiblaCalculatorTest`.
- [x] `WidgetSettingsUiState` (state + flow + 4 events + handlers) — **Removed**.
- [x] Tafseer text export (`ExportAnnotations`/`ClearExport`/`exportedText` →
  `ExportAnnotationsUseCase` → `TafseerRepository.exportAnnotations`) — **Removed**; the live
  path is `TafseerPdfExporter`, which the screen calls directly.
- [x] `QuranReaderUiState.mushafPageLayout` — **Removed**, superseded by
  `mushafPageLayoutCache`, which is what the reader actually reads.
- [x] `QiblaCalculator.calculateQiblaAngle` — **Removed**, unused.
- [x] `ZakatCalculatorUiState.showBreakdown` — **Resolved**, now read. It was settable (by an
  event with no producer) and read by nothing, so the breakdown was permanently expanded and
  the flag was decoration. The calculator's breakdown is now a disclosure driven by it, and it
  defaults to `true` so the working stays visible on first open.
- [x] ~~**`DuaOccasion.displayName()` is unused hardcoded English.**~~ **Resolved — deleted.** Its two call sites were the
  dua reader and the category list, which now resolve the label through
  `presentation/screens/dua/DuaOccasionLabels.kt` so a German reader sees German. The domain
  function has no callers left; delete it with the rest of #356's user-facing English rather
  than leaving an English fallback for someone to reach for.
  ```bash
  grep -rn "displayName()" app/src/main/java/com/arshadshah/nimaz --include=*.kt | grep -i occasion
  ```
- [x] ~~**`AyahActionsBottomSheet` — an unreachable *organism*, not just an unread field.**~~ **Resolved — retired**, following the precedent `2748002` set. The organism, its `buildAyahActions` table and its Robolectric suite are deleted. One caveat worth remembering for the next sweep of this kind: the file also hosted `SajdaIndicator`, which **was** reachable — `AyahTranslationBottomSheet` composes it — so deleting the file wholesale broke the build. That composable moved rather than died. Original text follows.

   The
  sheet, its `buildAyahActions` table and its Robolectric suite all exist and no screen composes
  it: the reader's per-verse actions are an inline row inside `QuranAyahItem`, and the sheet a
  verse actually opens is `AyahTranslationBottomSheet` (from `MushafLinePage`'s tooltip). The
  detectors above are field- and use-case-shaped, so a whole composable slipped through both.
  Either wire it into the reader or remove it — see `ARCHITECTURE.md` §9 row 14.
  ```bash
  # a screen-facing organism nothing outside its own file (or a test) composes
  cd app/src/main/java/com/arshadshah/nimaz
  for f in presentation/components/organisms/*.kt; do
    n=$(basename "$f" .kt)
    [ "$(grep -rl "\b$n(" --include=*.kt presentation/screens/ | wc -l)" -eq 0 ] && echo "uncomposed: $n"
  done
  ```

---

### AP-7.16 · Screen states improvised per screen

- [x] ~~**19 screens spin their own spinner, 11 `UiState`s carry an error nothing reads, and 9
  ViewModels `launchSafely` without an `onFailure`.**~~ **Resolved.** Three separate defects, one
  missing rule. A failure that reaches only telemetry left the abandoned state saying
  `isLoading = true`, so the screen span forever; a failure that reached state nothing renders
  was invisible; and `SurahSubjects`/`Passages`/`Background` evaluated `isEmpty()` before
  `error`, so a failed load was reported to the reader as "there is nothing here".

  Held by `ScreenStateConventionTest`, whose three backlogs were seeded with exactly the above,
  asserted in both directions — a stale entry fails as loudly as a new violation — and are
  **all now empty**. 25 spinner call sites across 19 screens converted; 7 states given a screen
  that renders them and 4 deleted for having no producer; the failing-load check narrowed to the
  question it can assert (see below) and its two real sites fixed.

  Two things came out of it that were not in the plan. **`launchBestEffort`** exists so a
  deliberately-quiet failure is distinguishable from a forgotten `onFailure` — the ratchet
  flagged three such sites, which was the right question and the wrong answer. And the
  unread-error check had to stop asking "does any screen in the feature's *package* read
  `.error`": `quran/` serves four ViewModels, so two states passed without a line of their code
  changing. It maps each `UiState` to the screen files that render it now.

  The failing-load check asks **"if a launch sets `isLoading = true`, can it set it false with a
  reason?"** — deliberately narrower than "every `launchSafely` has an `onFailure`", which after
  #441 was 211 call sites and would have meant rubber-stamping. That narrowing agrees with
  AP-7.12's triage: a `launchSafely` performing only a repository write is fire-and-forget, and
  nothing is showing a spinner for it.

  The contract and the six delivery layers are in
  `docs/superpowers/specs/2026-08-05-screen-state-migration-design.md`; the rule itself is
  `ARCHITECTURE.md` §8.

  ```bash
  # All three should print nothing but the two determinate rings.
  cd app/src/main/java/com/arshadshah/nimaz
  grep -rn --include='*.kt' 'CircularProgressIndicator(' presentation/screens
  ./gradlew :app:testDebugUnitTest --tests '*ScreenStateConventionTest*'
  ```

---

## Quick full re-scan

```bash
cd app/src/main/java/com/arshadshah/nimaz
echo "AP-1 domain->data:";      grep -rlnE "import com.arshadshah.nimaz.data\." domain/ || echo "  clean"
echo "AP-2 presentation->dao/entity:"; grep -rlnE "import com.arshadshah.nimaz.data.local.database.(dao|entity)" presentation/ || echo "  clean"
echo "AP-2 VM injects Dao/Impl:"; grep -rlnE "private val [a-zA-Z]+: [A-Za-z]+(Dao|RepositoryImpl)" presentation/viewmodel/ || echo "  clean"
echo "AP-3 domain repo->entity:"; grep -rlnE "import com.arshadshah.nimaz.data.local.database.entity" domain/repository/ || echo "  clean"
echo "AP-5 screen color literals:"; grep -rlE 'Color\(0x[0-9A-Fa-f]{6,8}\)' presentation/screens/ | wc -l
echo "AP-8 explicit FILLED cards:";  grep -rc 'style = NimazCardStyle.FILLED' presentation/ | grep -v ':0$' | wc -l
echo "AP-8 bordered card bypasses:"; grep -rl 'NimazCardDefaults.colors(' presentation/ | wc -l
```

*Keep this file honest: tick boxes as you go, and add new anti-patterns/instances when you spot
them.*
</content>
