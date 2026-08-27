# Multi-module migration — epic, issue breakdown and PR stack

**Repo:** `arshad-shah/nimaz` · **Integration branch:** `epic/multi-module`
**Spec:** [`SPEC.md`](SPEC.md) — the assessment and the reasoning. This document is the
execution plan: the issue tree, the branch topology, and the exit criteria for each PR.
**Status:** code complete. Issues #552–#573 are all closed and **all 22 PRs have landed** on
`epic/multi-module`. The integration PR to `dev` is **#596**. Outcome against the definition of
done, including the two criteria still unmet, is §6.

---

## 0. How this is meant to run

One epic issue, seven milestones, **22 pull requests** stacked onto a single long-lived
integration branch. Merging that branch into `dev` once gives a fully decoupled Nimaz with
documentation that matches the code — not a half-migrated tree that someone has to finish
later under pressure.

The stack exists because these PRs are **not independent**. PR *n* is cut from the branch of
PR *n−1*, not from `dev`. Reviewing them in isolation would mean reviewing a tree that does
not compile.

```
dev
 └── epic/multi-module                 ← integration branch, never merged into until the end
      ├── mm/00-build-logic            PR  1
      ├── mm/01-check-docs-roots       PR  2   ─┐ phase 0
      ├── mm/02-baseline-metrics       PR  3   ─┘
      ├── mm/03-domain-route-inversion PR  4   ─┐ phase 1
      ├── mm/04-core-domain            PR  5   ─┘
      ├── mm/05-core-common            PR  6   ─┐
      ├── mm/06-core-database          PR  7    │ phase 2
      ├── mm/07-core-datastore         PR  8    │
      ├── mm/08-core-data              PR  9   ─┘
      ├── mm/09-core-ui                PR 10   ── phase 3
      ├── mm/10-core-navigation        PR 11   ─┐ phase 4
      ├── mm/11-navgraph-decompose     PR 12   ─┘
      ├── mm/12-feature-widget         PR 13   ─┐
      ├── mm/13-feature-onboarding-about PR 14  │
      ├── mm/14-feature-tools-calendar PR 15    │
      ├── mm/15-feature-search         PR 16    │ phase 5
      ├── mm/16-feature-content        PR 17    │
      ├── mm/17-feature-tracker        PR 18    │
      ├── mm/18-feature-quran          PR 19    │
      ├── mm/19-feature-prayer         PR 20    │
      ├── mm/20-feature-settings       PR 21   ─┘
      └── mm/21-di-split-and-guardrails PR 22  ── phase 6
```

Each retargets to `epic/multi-module` on merge,
so the integration branch always holds every merged step and nothing else.

**Rebase discipline.** When `dev` moves, rebase `epic/multi-module` onto it once and
force-push the stack, rather than merging `dev` into each open branch. With 22 branches the
merge-commit approach produces an unreviewable history.

---

## 1. The epic issue

> **Title:** Split Nimaz into feature and core Gradle modules
>
> **Body:**
>
> 190,505 lines across 1,121 files sit in one `:app` module. The layering `CLAUDE.md` claims —
> `presentation → domain → data` — is real in the code but enforced by nothing, because a single
> module cannot enforce it. This epic converts that convention into compile errors, and takes
> the build-time improvement as a secondary benefit.
>
> Full assessment and reasoning: `docs/specs/multi-module-migration/SPEC.md`.
> Execution plan and PR stack: `docs/specs/multi-module-migration/EPIC.md`.
>
> **Target:** 11 feature modules, 7 core modules, `:app` reduced to a shell.
>
> **Integration branch:** `epic/multi-module`. Merged into `dev` once, at the end.
>
> **Definition of done**
> - [ ] `:app` contains only `MainActivity`, `NimazApp`, the NavHost shell, `screens/adaptive`,
>       `screens/home`, the manifest, Firebase and signing config
> - [ ] `:core:domain` builds with the `kotlin-jvm` plugin and no Android on its classpath
> - [ ] No `:feature:*` module depends on another `:feature:*` module, enforced by a `check` task
> - [ ] All 2,333 unit and 121 instrumented tests green
> - [ ] `python3 scripts/check_docs.py` green **and** asserting non-zero scan counts
> - [ ] Release AAB diffed against baseline: manifest, DEX, resources, R8 output
> - [ ] `ARCHITECTURE.md`, `SUBSYSTEMS.md`, `NAVIGATION.md`, `TESTING.md`,
>       `CLEAN_ARCHITECTURE_CHECKLIST.md` and `docs/README.md` describe the module structure
> - [ ] Phase 5 build-time measurements recorded against the Phase 0 baseline
>
> **Explicit non-goals:** splitting `strings.xml`; moving `screens/adaptive` out of `:app`;
> any behaviour, UI or schema change.

---

## 2. Milestones and sub-issues

Each sub-issue below is one PR. "Exit" is what a reviewer checks before approving.

### Milestone 0 — Foundations (PRs 1–3)

Nothing moves. This milestone exists so that the checks which would otherwise silently stop
working are fixed *before* anything can break them.

**#1 — Add `build-logic` convention plugins** · `mm/00-build-logic`
Included build with `nimaz.android.library`, `nimaz.android.feature`, `nimaz.jvm.library`,
`nimaz.android.hilt`, `nimaz.android.compose`. Each carries compileSdk 37 / minSdk 29, Java 21,
the `-Xannotation-default-target=param-property` arg, and the Hilt/KSP wiring. Add
`android-library` and `kotlin-jvm` aliases to `libs.versions.toml`. Apply the plugins to `:app`
itself so they are exercised immediately. Move the `fetchNimazData` lint/asset ordering
workaround into the convention plugin rather than leaving it inline — every module that
consumes those generated assets will need it.
*Exit:* `:app` builds; no change to the produced APK; `./gradlew :app:assembleRelease` green.

**#2 — Make `scripts/check_docs.py` module-aware** · `mm/01-check-docs-roots`
**This is the highest-priority item in the whole epic and must land before any file moves.**
The script roots every scan at `APP = ROOT / "app/src/main/java/com/arshadshah/nimaz"`.
SUB-02 (Workers), SUB-03 (Services), SUB-05 (notification channels) and SUB-06 (DataStore
files) scan `APP.rglob(...)`. Six of the seven Workers live in `widget/` and three of the four
Services in `data/audio/` — both of which move in Phase 5. When they do, those globs return a
smaller set and "every Worker is documented" passes because there are fewer Workers to find.
Change the roots to scan all modules, and add a floor assertion per scan (`>= 7 Workers`,
`>= 4 Services`, `>= 12 channels`, `>= 3 DataStore files`) so an empty or shrunken scan fails
rather than passes.
*Exit:* deliberately move one Worker to a scratch directory and confirm the check goes **red**.
Revert. This negative test is the point of the PR.

**#3 — Record the baseline and enable configuration cache** · `mm/02-baseline-metrics`
Add `org.gradle.configuration-cache=true` (plus `…problems=fail`). Commit a `BASELINE.md` in
this spec folder holding the pre-migration measurements from SPEC §6.5: clean `assembleDebug`,
the four incremental scenarios, `testDebugUnitTest` wall time, and configuration time — **with
the protocol they were taken under**, since a number without one is folklore. Enabling the cache
means converting `fetchNimazData` into a typed task class in `build-logic`, and fixing two
untracked configuration-time reads in `app/build.gradle.kts` that would otherwise let a stale
`CONTENT_ARTIFACT_SHA256` be baked into the APK.
*Exit:* [`BASELINE.md`](BASELINE.md) committed, and `--configuration-cache` green on two
consecutive runs — the second reporting reuse — for `:app:assembleDebug` **and**
`:app:assembleRelease`. Without the numbers the epic cannot demonstrate it worked.

### Milestone 1 — `:core:domain` (PRs 4–5)

**#4 — Remove the domain → navigation inversion** · `mm/03-domain-route-inversion`
Five domain files import `core.navigation.Route`: `UnifiedBookmark.kt`, `AiModels.kt`,
`Announcement.kt`, `AskWithProofUseCase.kt`, `AnnouncementUseCases.kt`. Introduce a domain-level
target type and map it to `Route` at the presentation edge. Also fix the three KDoc references
from `domain/model` into `data`/`presentation` (`QuranReciter.kt`, `MushafLayout.kt`,
`QuranTranslation.kt`).
*Exit:* `grep -rl 'core\.navigation\.Route' domain/` returns nothing. Announcement deep links
still resolve — NAV-06 through NAV-08 green.

**#5 — Extract `:core:domain` as a pure JVM module** · `mm/04-core-domain`
Move everything under `domain/` to `core/domain/` under the `kotlin-jvm` plugin. No Android on
the classpath.

**Also folds in the first slice of the `core/util` triage,** which #6 below used to own. Domain
imports `HijriDateCalculator`, `TodayProvider`, `NextWorshipResolver` and — transitively through
the resolver — `PrayerTimeCalculator` and `WorshipReminderCalculator`. They cannot wait for
`:core:common`: that module depends on `:core:domain`, so parking them there reverses the arrow.
They move into `:core:domain` as `domain/{calendar,time,worship,prayer}`, which is a repackaging
and therefore an import rewrite across ~60 files. `SPEC.md` §3.6 ("triage `core/util` first") was
right and the PR-6 scheduling below was wrong.

**Two things the issue did not anticipate, both of which are the point of the boundary:**

- **Cross-module smart casts stop working.** Kotlin will not smart-cast a `val` declared in
  another module, so 27 sites of the form `if (x.prop != null) use(x.prop)` over a domain model
  became compile errors. Each is fixed by binding to a local first. Expect this in every
  extraction PR that moves a model with nullable fields.
- **Test resources and shared fakes have to cross the boundary deliberately.**
  `fold-fixtures.json` moved with the test that reads it, and `FakeTodayProvider` /
  `FakeSearchSettings` became `src/testFixtures` published to `:app`, rather than being
  duplicated.

*Exit:* `:core:domain:test` runs without AGP or Robolectric and completes in seconds, and the
purity is **enforced by a task, not by a demonstration** — `androidFreeClasspath`, registered by
`nimaz.jvm.library` and wired into `check`, fails on any `com.android` / `androidx` /
`com.google.android` component on any resolvable classpath. `:core:domain` is also wired into the
merged coverage report, with `assertEveryModuleIsMeasured` failing the report if a module
contributes no classes. The screenshot of a failing `import android.content.Context` is PR
narrative; the task is the criterion.

### Milestone 2 — Data-side core modules (PRs 6–9)

**#6 — `:core:common`** · `mm/05-core-common`
`core/util` was 24 files and 5,336 LOC reaching into domain (11 files), presentation, data and
widget. **PR 5 already took the five that domain imports** — they are now `domain/{calendar,
time,worship,prayer}` in `:core:domain`, and `core/time` no longer exists.

Of the nineteen left, **seven** move: `CountdownFormatting`, `DateTimeExtensions`,
`TimeFormatting`, `NumberFormatUtils`, `ThematicMarkup`, `LocaleHelper` and `PrayerClock`,
repackaged `core.util` → `core.common` so the package is not split across two modules. Plus
`core/monitoring` (6) and `core/text` (1), both wholesale and unrenamed. About 920 LOC, not the
~1,400 the triage projected, because two of its `:core:common` rows had already left for
`:core:domain`.

The other twelve are **pushed down, and stay in `:app` until their module exists** —
`TajweedParser` → `:core:ui` (PR 10), `PrayerTimesPdfExporter` → `:feature:prayer` (PR 20),
`TafseerPdfExporter` → `:feature:quran` (PR 19), `NotificationDiagnostics` → `:feature:settings`
(PR 21). `BootReceiver`, `PrayerRescheduler`, `InAppUpdateManager` and `core/init` stay in `:app`
for good — and **PR 20 found that `PrayerNotificationScheduler`, `PrayerAlarmTimes` and
`NotificationContentHelper` belong on that list too**, not in `:feature:prayer` as this line
originally read: their consumers are `SettingsViewModel`, `AppInitializer` and `RepositoryModule`,
none of which is a prayer-times caller. `core/share` goes to `:core:ui`
(it is a Canvas/bitmap renderer that uses `R`) and `core/feedback` to `:feature:tracker`, so
neither moves here. `SPEC.md` §4's "`:core:common` = util, time, monitoring, share, text" was
wrong about `share`; `EPIC.md`'s extra `init` and `feedback` were wrong too.

*Exit:* `:core:common` depends on `:core:domain` and Android only — never on data or
presentation, and never on `R`. Enforced by **`moduleBoundary`**, registered by
`nimaz.android.library` on every Android module and wired into `check`: it reads declared project
dependencies and fails when a `:core:*` module points at `:app` or a `:feature:*`, or a
`:feature:*` points sideways. The `R` half needs no task — the module's namespace is
`com.arshadshah.nimaz.core.common`, so with `nonTransitiveRClass=true` the app's `R` is not on
its classpath to import.

> **Corrected twice.** This PR used to be described as owning the whole `core/util` triage *and*
> as depending on `:core:domain`. Both could not hold: `HijriDateCalculator` and friends are
> imported **by** domain, so putting them in a module that depends on domain is a cycle — fixed
> in PR 5. And `PrayerClock` was recorded as *"imported by nothing; confirm it is not dead
> code"*: it is not dead. All three of its functions are called from `presentation/`. The grep
> that suggested otherwise looked for the file name, and everything in the file is a top-level
> function.

**#7 — `:core:database`** · `mm/06-core-database`
Both `@Database` classes, 15 entities, 22 DAOs, all migrations, **and the `schemas/`
directory**. The `room.schemaLocation` KSP arg and the `androidTest` `assets.srcDir(schemas)`
wiring move with it.
*Exit:* **the exported schema JSON for version 25 is byte-identical to the pre-move file.** If
Room regenerates it differently the identity hash changed and every install would attempt a
destructive migration. `MigrationTestHelper` tests green. SUB-01 green.

**#8 — `:core:datastore`** · `mm/07-core-datastore`
`PreferencesDataStore` (990 LOC) and the 11 `SettingsSeams` implementations — plus four files the
issue does not name and cannot be left behind: `PreferenceCodec` (the type registry
`PreferenceCodecTest` checks against), `PrayerNotificationPrefsMigration`,
`AnnouncementLocalDataSource`, and **`data/ai/DeviceIdProvider`**, which owns the
`nimaz_ai_device` store and would otherwise split SUB-06's subject across two modules.

**One blocker first.** `WidgetsScreen` read its preview location by constructing
`PreferencesDataStore(context)` inside a composable helper — a second instance of a `@Singleton`,
built outside Hilt, reading the file the injected one owns. It routes through the ViewModel's
`LocationSettings` seam instead. That is the only such site.

*Exit:* SUB-06 green with its floor assertion, and **"no preference key renamed" made
verifiable**: a golden file of all 106 `name<TAB>type` pairs in `:core:datastore`'s test
resources, compared as a whole list so a rename shows as one removal plus one addition.
Additions regenerate freely; **removals need an entry in `retired-preference-keys.txt`** with a
versionCode and a reason, because a removed key silently resets that setting for every existing
user. The six runtime-composed keys are recorded in their literal `${'$'}{key}` template form.

> **Corrected.** The original exit criterion — "no preference key renamed" — named the right
> property and no way to check it. `PreferenceCodecTest` looked like half a guard already, but it
> asserts that two files *in this repo* agree with each other, and an IDE rename updates both in
> lockstep. The golden file is a third copy that nothing automatic touches.

**#9 — `:core:data`** · `mm/08-core-data` — **landed**
19 repository implementations and their mappers, plus the `data/{device,text,ai,announcement,
widget,platform}` slices and `data/local/help`. 221 tests in the new module; `:app` 1707 → the
totals below.
*Exit:* no persistence type in the module's public API, checked by
`PublicApiHasNoPersistenceTypesTest`.

> **Corrected, and this one had teeth.** The issue offered two exit criteria. The first — *every
> repository still returns domain models* — is **already true and checks nothing**: those
> repositories implement interfaces declared in `:core:domain`, a `kotlin-jvm` module, so an
> entity in one of their signatures would not compile. The second — *no `*Entity` in any public
> API* — was **already false when the issue was written**, at
> `MushafLayoutMapper.toPageLayout(page, rows: List<MushafLayoutLineRow>)`: a public `object`
> taking a `QuranDao` projection. That is the shape worth guarding, because it is the quiet one —
> a repository leaking an entity is loud, a *helper* leaking one involves `:core:domain` not at
> all, so nothing objects until a feature module reaches for the helper and drags a database type
> into presentation with it. `MushafLayoutMapper` is now `internal`.
>
> The guard took three runs to become true, and each failure is the standing rule earning its
> keep. **Run 1** reported the very declaration whose fix motivated it — a line-local visibility
> read cannot see that a member of an `internal object` is internal, and the "fix" it pushed
> toward was a redundant modifier on every member. **Run 2**, after adding a container stack,
> reported three *local `val`s inside function bodies* holding Room entities — the ordinary
> business of a mapping layer. **Run 3** passed the leak check but tripped the scan floor: 16
> public declarations found where there are 293, because a wrapped constructor closes on
> `) : SomeInterface {` at the class's own indent and popped the class off the stack, leaving
> every member below it unchecked. Two of those three would have shipped a green, useless guard;
> the floor caught the third. It also now joins wrapped signatures, which a first-line-only read
> would have been blind to — and a signature long enough to wrap is exactly the one most likely
> to carry a persistence type.
>
> `IntegrityTokenProvider` needed the other inversion this milestone teaches: a library's
> `BuildConfig` carries only its own fields, so its two reads of the app's became constructor
> parameters passed by `AiModule` in `:app`.

**`internal` is scoped to a module, and that is a finding generator.** `CompassSmoothingTest` lived
in `:app` under `presentation.viewmodel.prayer` and called `smoothInto`, an `internal` function in
`AndroidCompassSensors`. The moment its subject moved here the test stopped compiling — not because
anything broke, but because a boundary finally existed to notice that a *presentation* test was
reaching into a data-layer implementation detail. Expect this on every remaining extraction: each
one converts "same module, so `internal` is visible" into a compile error that names a coupling
nobody had written down. The fix is to move the test with its subject, not to widen the visibility.

**One file was very nearly deleted as dead code and is not.** `HelpJsonDto.kt` declares a
top-level `val helpJson: Json` that `HelpRepositoryImpl` uses; both #560's validation and my own
first pass grepped the *file name*, which finds nothing, because nothing is named `HelpJsonDto`.
Recorded here because it is the second time in this epic that a filename grep has been mistaken
for a usage search.

### Milestone 3 — `:core:ui` (PR 10)

> **The shrink risk this milestone was written around does not exist.** #561 names the eight Quran
> fonts as "selected at runtime from a settings value" and therefore vulnerable to
> `isShrinkResources`. The selection is real; selection *by resource name* is not. Every one of the
> eight has a compile-time `R.font.*` reference in `theme/Type.kt` (16 `R.font.` sites in all), and
> the settings value picks between already-constructed `FontFamily` objects. A compile-time `R`
> reference is exactly what the shrinker keeps.
>
> The one by-name lookup in the app is AboutLibraries' `getIdentifier("aboutlibraries", "raw", …)`,
> which **has** already caused a silent release-only failure and is fixed twice over — the code
> passes `R.raw.aboutlibraries` by id, and `res/raw/keep.xml` names it. Its resource is *generated*
> by the Gradle plugin into the applying project, so it and the keep rule stay in `:app`.
>
> **"Every resource" was also too broad.** `res/xml/` is manifest-referenced app configuration
> (backup rules, locales config, six widget-provider descriptors); `res/drawable/` and `res/layout/`
> are entirely widget and notification assets; `values/themes.xml` references the splash-screen
> theme and the launcher foreground, so it is startup identity — it went to `:core:ui` and had to
> come straight back when AAPT could not link it. What moved is `strings.xml` + five translations,
> `colors.xml`, and `font/`.
>
> **What the milestone actually costs is the `R` rename.** `nonTransitiveRClass=true` means
> `com.arshadshah.nimaz.R` loses `R.string.*` the moment strings leave, so 229 files swap their
> import and 2,431 usages keep their spelling. Ten files need both `R` classes and alias the app's
> as `AppR`. That is the bulk of the diff, and it is mechanical.
>
> **Two things this milestone taught that the remaining PRs will hit.** First, `internal` is
> module-scoped: 30 compile errors named couplings nobody had written down. Where the consumer
> legitimately lives elsewhere the symbol becomes public with a comment saying why; where the
> consumer is a *test*, the test moves — 62 component tests and `UiError` did. Second, a
> source-scanning guard breaks silently on a move: `MaterialTextFieldGuardTest` asserted only that
> its directory *existed*, and `app/…/presentation` still does, so it would have gone on passing
> while scanning a fraction of the surface. It now walks both roots and carries a floor. That is
> the seventh guard in this epic found green against what it was meant to catch.


**#10 — Extract the design system** · `mm/09-core-ui`
52 atoms, the generic `Nimaz*` molecules, `theme/` (18 files), `foundation/` (17 files), and
**every resource including the full 1,910-entry `strings.xml` and all five translation
directories**. Fix the three design-system → feature leaks. Feature-specific molecules and
organisms stay put; they travel with their feature in Milestone 5.
*Exit:* `:core:ui` has no import from `presentation.screens` or `presentation.viewmodel`.
Resource resolution verified in a release build — see the R8 note in SPEC §6.4.

### Milestone 4 — Navigation (PRs 11–12) — **the critical path**

**#11 — `:core:navigation`** · `mm/10-core-navigation`
The `Route` hierarchy, `ScreenTags`, the `taggedComposable` helper, `HelpDeepLink`,
`AnnouncementRoutes`, `WorshipDestinations`. Nothing that imports a screen.
*Exit:* NAV-01, NAV-02, NAV-05, NAV-09, NAV-10 green against the new location.

**#12 — Decompose `NavGraph.kt`** · `mm/11-navgraph-decompose`
1,442 LOC importing 70 screens and registering 94 destinations. Convert each destination into a
per-feature `fun NavGraphBuilder.quranGraph(onNavigate: (Route) -> Unit)` extension placed
beside the screens it registers — **still inside `:app`**. `:app` keeps the `NavHost` and
`NavigationSuiteScaffold` shell and calls each feature's graph function.
**This PR moves no files between modules.** It is reviewable on its own merits and is the
change that makes Milestone 5 possible.
*Exit:* all 94 destinations still registered with `taggedComposable`, never a bare
`composable` — NAV-03 and NAV-04 green. Every route still reachable; deep links unchanged.

### Milestone 5 — Feature modules (PRs 13–21)

Ordered least-coupled first, so the pipeline is proven on something that cannot break a screen.
Each PR moves: screens, ViewModels, that feature's molecules and organisms, its Hilt module
slice, its nav-graph extension, and its tests.

| PR | Issue | Branch | Contents |
|---:|---|---|---|
| 13 | **#13** `:feature:widget` | `mm/12-feature-widget` | **landed.** 6 Glance widgets, 7 receivers, 6 workers. Found two layering violations (a widget injecting `PrayerDao`, a Worker building a `PrayerRecordEntity`) and a `@HiltWorker` failure that only the emulator suite could see. |
| 14 | **#14** `:feature:onboarding`, `:feature:about` | `mm/13-feature-onboarding-about` | about, help, licenses, more. Onboarding needed **nothing** unpicked; about needed **six** couplings resolved. `AdaptiveMoreScreen` moves here — it composes all three and is the one adaptive screen that is not `:app`'s. |
| 15 | **#15** `:feature:tools`, `:feature:calendar` | `mm/14-feature-tools-calendar` | zakat; Islamic calendar + events. Neither had a coupling to unpick; the work was two components finding their right homes and an `api`/`implementation` slip in `:core:ui`. |
| 16 | **#16** `:feature:search` | `mm/15-feature-search` | search + AI ask-with-proof. `worker/` untouched. The `BuildConfig` trap the issue names was already defused in PR 9 — both fields are read only in `core/di/AiModule`, so it becomes live again at **PR 22**, not here. |
| 17 | **#17** `:feature:content` | `mm/16-feature-content` | dua, hadith, qaida, asma, asmaunnabi, names, prophets, catalog — **moved together**, their ViewModels are one package. Four components in the issue's list went to `:core:ui` instead, and two *settings* screens stayed in `:app` — the ViewModel axis cuts both ways. `QaidaAudioManager` came too. |
| 18 | **#18** `:feature:tracker` | `mm/17-feature-tracker` | prayer tracker, fasting, tasbih — likewise. `screens/prayer` split 6/2 by ViewModel; `CounterFeedback` became a `:core:domain` port with its Android half in `:core:data`. The `dua -> tracker` edge was already gone, resolved in PR 17. |
| 19 | **#19** `:feature:quran` | `mm/18-feature-quran` | quran, khatam, bookmarks. `QuranDao` stays in `:core:database` (4 repos use it); `QuranAudioManager` stays in `:app` behind the `QuranPlayback` port, because `MainActivity` holds one too. `TajweedParser` came here, not to `:core:ui` as §2 guessed. |
| 20 | **#20** `:feature:prayer` | `mm/19-feature-prayer` | prayer times, qibla, night worship. **No Service moved and SUB-03 is untouched** — the adhan players and the whole prayer notification stack stayed in `:app`, because no file in the move set names them and their consumers are the settings surface plus `:app` init. Sending them here would have created the `:feature:settings -> :feature:prayer` edge #571 itself forbids. `PrayerTimeCard` and `PrayerSkyScene` went down to `:core:ui`. |
| 21 | **#21** `:feature:settings` | `mm/20-feature-settings` | 24 screens (18 in `screens/settings` plus five parked in other features' directories and `AdaptiveSettingsScreen`), the `SettingsViewModel` (1,324 lines when this row was written; 1,400 after the reciter-preview repair below), location and sync. **`data/sync` went to `:core:data`, not here** — 21 DAOs and 14 entities. Three ports built rather than three moves: `PrayerAlarmScheduler` gained `cancelAllPrayerNotifications`, `PrayerNotificationTester` is new, and reciter preview moved onto `QuranPlayback`. The boundary found a dead pair of methods pinning `AdhanAudioManager` to `:app`, and a preview button that had never played a sound. |
| 22 | **#22** DI guardrails | `mm/21-di-guardrails` | `RepositoryModule` **905 -> 109 lines**: every binding moved to the module that owns its implementation, `UseCaseModule` to `:core:domain`, `DatabaseModule` to `:core:database`. The **twenty** Compose test-harness copies collapsed into one `core/ui/src/testFixtures/`. `moduleBoundary` already enforced the graph lock #573 asks for — shown failing on all three of its rules rather than assumed. `:core:domain` gained `hilt-core` **and its KSP processor**: the third forgotten-plugin trap of the epic, now guarded by `DaggerModuleProcessorTest`. |

`screens/home` stays in `:app`. `screens/adaptive` **mostly** does: six of its seven files each
compose screens from a single feature and go with it (`AdaptiveMoreScreen` went in PR 14), so the
directory empties out rather than staying whole.

**There are 29 `screens/` directories, not 26.** The figure was exactly right when written —
`git ls-tree` at the commit before PR 12 counts 26 — and PR 12 itself added three more:
`content/`, `tracker/` and `tools/`, each holding a single `<Feature>Graph.kt`. They read like
long-standing screen packages because they are named after *ViewModel* packages, which is why the
count drifted unnoticed. Re-derive it rather than incrementing: the inventory now moves in two
directions at once, as features leave and graph directories arrive.

**Extracting a feature module means registering it in four places**, and
`FeatureModuleRegistrationTest` fails if you miss one:

| Register in | Missing it means |
|---|---|
| `PresentationSourceRoots` | four cross-module scans stop covering the module |
| `inputs.dir` in `app/build.gradle.kts` | those scans stay `UP-TO-DATE` and do not run at all |
| `coverageModules` in `app/build.gradle.kts` | reported coverage *rises*, by measuring less |
| `CrossFeatureViewModelGuardTest.MODULE_OF` | the module's screens are exempt from the rule |

This was a prose checklist first and **it did not survive two milestones**: PR 14 missed the
second, PR 15 missed the fourth, and both were found by accident while starting the next module.
Neither failed anything — a scan that quietly stops covering a module is silent by construction.
The test reads `settings.gradle.kts` as the source of truth for which modules exist, because that
is the one registration nobody can forget.

*Exit for every PR in this milestone:* the moved feature's tests compile **without being
relaxed**. A test that will not compile after the move is a real coupling signal, not migration
noise — fix the coupling, do not weaken the test.

### Milestone 6 — Guardrails and docs (PR 22)

**#22 — Split the DI god-modules and lock the graph** · `mm/21-di-split-and-guardrails`
`RepositoryModule.kt` (863 LOC) dissolves; each module owns its `@Binds`/`@Provides`.
`DatabaseModule`'s DAO providers move to `:core:database`. Add the dependency-graph `check`
task: fail the build if any `:core:*` depends on a `:feature:*`, or any `:feature:*` depends on
another `:feature:*`.
*Exit:* deliberately add a forbidden dependency and confirm the build fails. Revert.

---

## 3. Documentation, and which PR owns each change

`CLAUDE.md` requires docs to be updated **in the same commit** as the change they describe, and
`scripts/check_docs.py` enforces 23 of those rules on every PR. This epic invalidates a lot of
prose, so ownership is assigned explicitly rather than left to a cleanup PR at the end.

| Doc | What goes stale | Owning PR |
|---|---|---|
| `ARCHITECTURE.md` §DI | "All modules live in `core/di`" becomes false | PR 22 |
| `ARCHITECTURE.md` §layers | The layer diagram gains module boundaries | PR 5 (`:core:domain`), extended each milestone |
| `ARCHITECTURE.md` §9 registry | Records **nine** settings seams; there are **11** (`HijriSettings`, `SearchSettings` were added without a doc update) | PR 1 — fix the pre-existing drift up front |
| `ARCHITECTURE.md` new-feature recipe | "Add it to `NimazDatabase` … bind it in `RepositoryModule`" — both move | PR 7, PR 22 |
| `NAVIGATION.md` | Route graph gains per-feature graph functions; destination count claim | PR 12 |
| `SUBSYSTEMS.md` | Worker, Service, widget and DataStore tables gain module columns | PR 7, 13, 20 |
| `TESTING.md` | Test invocation moves from `:app:testDebugUnitTest` to all-module | PR 3, PR 22 |
| `CLEAN_ARCHITECTURE_CHECKLIST.md` | Detection commands grep paths that move; several anti-patterns become compile errors and can be ticked | every PR that moves the path a command greps |
| `docs/README.md` | Index gains this spec folder if any doc here is promoted to top level | PR 22 |
| `DOCUMENTATION.md` §1 | Ownership matrix gains the module-structure owner | PR 22 |

Two rules for the stack:

1. **A PR that moves a path referenced by a detection command in
   `CLEAN_ARCHITECTURE_CHECKLIST.md` updates that command in the same PR.** Otherwise the
   checklist rots into a list of commands that return nothing and therefore "pass".
2. **A PR that resolves a checklist anti-pattern ticks the box** rather than leaving it for
   PR 22. Several of them — the layering assertions in particular — stop being assertable
   because the compiler now enforces them; say so in the checklist instead of deleting the row.

---

## 4. What runs on every PR in the stack

Non-negotiable, in this order. A PR that cannot show all five does not merge into
`epic/multi-module`.

1. `./gradlew testDebugUnitTest` — all 2,333 unit tests, no test relaxed to compile
2. `./gradlew lintDebug` across all modules
3. `python3 scripts/check_docs.py` — 23 checks, **with the floor assertions from PR 2**
4. `./gradlew :app:assembleRelease` — R8 and resource shrinking exercised, not just debug
5. The docs listed in §3 for that PR, updated in the same commit

Instrumented tests (121) run per milestone rather than per PR, on the existing
`android_instrumented_tests.yml` lane.

Artifact diffing (SPEC §6.4 — merged manifest, DEX class list, merged resources, R8 output)
runs at each **milestone** boundary against the Phase 0 baseline, not per PR. Per PR it is too
slow to be useful; per milestone it is the thing that catches a stripped font or a dropped
`values-tr`.

---

## 5. Risks specific to running this as a stack

- **The stack goes stale.** 22 PRs against a moving `dev` is the main practical risk. Rebase
  `epic/multi-module` onto `dev` weekly and force-push; do not let it drift a month.
- **Milestone 5 PRs conflict with feature work on `dev`.** A PR touching `screens/settings`
  while PR 21 is open will conflict badly. Either freeze the feature area for the duration of
  its PR, or land that PR first. This is worth agreeing before starting Milestone 5.
- **The integration branch is never itself released.** It merges to `dev` once, at the end,
  after a full release-build diff. Do not cut a release from `epic/multi-module`.
- **Reviewer fatigue.** PRs 13–21 are nine mechanically similar moves. The interesting review
  happens in PRs 2, 5, 7, 12 and 22; the rest should be reviewed for *what did not move* — a
  file that quietly stayed behind in `:app` is the common failure.

---

## 6. Outcome — the definition of done, checked

Written when the last PR landed, against the eight boxes in §1. Three of them are **not** ticked,
and two of those cannot be ticked from CI at all. Recorded here rather than quietly dropped,
because an unmet criterion that nobody writes down is indistinguishable from one that was met.

| # | Criterion | State |
|---|---|---|
| 1 | `:app` reduced to a shell | **superseded — see below** |
| 2 | `:core:domain` on `kotlin-jvm`, no Android on its classpath | met — `androidFreeClasspath`, wired into `check` |
| 3 | No `:feature:*` → `:feature:*`, enforced by a task | met — `moduleBoundary`, prefix-based, both rules |
| 4 | All unit and instrumented tests green | met — 2,464 unit (up from 2,333; none deleted), 121 instrumented |
| 5 | `check_docs.py` green **and** asserting non-zero scan counts | met — 23 checks, 13 floors |
| 6 | Release AAB diffed against baseline | **not done** |
| 7 | The six docs describe the module structure | met |
| 8 | Phase 5 build-time measurements against the Phase 0 baseline | **not done** |

**1 — superseded, not met as written.** The box says `:app` keeps `screens/adaptive`. It does not:
all seven adaptive screens went with the feature each composes, six in Milestone 5 and
`AdaptiveMoreScreen` in PR 14, so the directory emptied rather than staying. `:app` also still
holds `data/audio` (7 files), `core/util` (6), the 20 home-screen components, `core/init` and four
one-file `data/` slices — each justified in §2 where it was decided, none of them anticipated by
this box. `:app` ended at **52 files / 11,484 lines, 8%** of ~141,800. The intent was met; the
enumeration in the box was written before the reasons existed and was never reconciled.

**6 — not done.** SPEC §6.4's artifact diff (merged manifest, DEX class list, merged resources,
R8 output) needs a signed release build, and `KEYSTORE_FILE` is a CI-only secret. §4 asks for
`:app:assembleRelease` per PR and the diff per milestone; neither ran locally, and no milestone
diff was published. The signed release build *does* pass in CI — `internal_testing.yml` built and
uploaded 3.0.128 from this branch — so the build is exercised; what is missing is the **comparison
against the pre-migration artifact**, which is what would catch a stripped font or a dropped
`values-tr`. This is the single highest-value check still outstanding.

**8 — not done, and the stopping rule was therefore never exercised.** [`BASELINE.md`](BASELINE.md)
§5 defines the gate precisely — the `inc_leaf_screen` row, cache-on column, measured back to back
with the baseline commit in one sitting, at Milestone 5 half done (PRs 13–16). That measurement was
never taken, so the epic's secondary benefit is **unverified**: the build-time improvement is
claimed by no number in this repo. The driver still exists
(`python3 scripts/measure_build_baseline.py results.json inc_leaf_screen`) and §6 of that document
warns the row swung 37% between two sessions on an unchanged tree, which is why the protocol insists
on one sitting. Anyone closing this out should run it before quoting a speedup.

Neither gap blocks the merge on correctness grounds — the boundaries are enforced by tasks, and
every test and doc check is green. Both are evidence gaps, and they are the two things a device
pass cannot settle either.
