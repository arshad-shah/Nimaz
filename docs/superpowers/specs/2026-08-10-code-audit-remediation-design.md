# Code audit remediation — design

**Date:** 2026-08-10
**Status:** approved, not yet implemented
**Scope:** the `dev` code audit, Parts 1–5 plus §6.1
**Epic branch:** `epic/audit`
**Related:** Part 6 (features) is deliberately excluded and gets its own epic.

---

## 1. What this is

A code audit of `dev` (965 Kotlin files, 169k lines, 281 test files, schema v24, ~95 routes)
produced nine recommended workstreams. Its central observation is worth keeping in front of us,
because it shapes every choice below:

> The tech-debt registry looks at architecture and correctness. It has no lens for performance,
> execution cost, test coverage below the ViewModel layer, or accessibility.

Almost every finding falls into one of those four blind spots. This epic closes them.

**In scope:** audit Parts 1 (duplication), 2 (performance), 3 (testing), 4 (accessibility and
font scaling), 5 (structure and platform), and §6.1 (Arabic search broken on existing installs).

**Out of scope, tracked separately:** all of Part 6's new features — Hifz mode, Wear OS tile and
complication, Jamaat/Iqamah times, the countdown notification, the Quick Settings tile, catalog
expansion, the surah-info route collapse, and Ask-with-Proof follow-ups. §6.2 (audio repeat and
playback speed) moves there too, even though it shares a file with the §2.1/§2.2 audio work.

---

## 2. Two corrections to the audit

Both were verified against `dev` at `2b91828f`. Both change what gets built, so they are recorded
here rather than discovered mid-implementation.

### 2.1 §1.2 — `AyahActionsBottomSheet` no longer exists

The audit recommends wiring `AyahActionsBottomSheet` in as the shared per-verse action host,
closing registry Open #14 in the "wire it" direction and removing the `MushafPage` /
`MushafLinePage` clone as a side effect.

The sheet was **retired**, not wired. The only remaining trace is a comment at
`AyahTranslationBottomSheet.kt:243`: *"Moved here when `AyahActionsBottomSheet` was retired: the
sheet itself was never composed."* The decision the audit asks us to make has already been made,
in the other direction.

**What we do instead:** the 93-line clone across the two Mushaf renderers is real and still worth
removing. Extract a shared per-verse action host directly from the two renderers — tooltip
position, bookmark override, favourites map, translation sheet, tajweed sheet, copy/share
handlers — rather than resurrecting a retired component. Registry Open #14 is closed by
recording that the retirement stands.

### 2.2 §6.1 — the app does not build the FTS index

The audit prescribes building the folded FTS index on-device in a `WorkManager` job, on the
theory that installs predating the index never receive one.

The app has no index-building code and should not acquire any. `ContentSearchIndex` only
*probes* — `Ready` / `Absent` / `Mismatched`, keyed on `search_meta.fold_version` against
`ArabicSearchNormaliser.FOLD_VERSION` — and reads. The index is compiled by **nimaz-data**
(`nimaz_data/build/search.py`, stage 5 of the pipeline; FTS4 rather than FTS5 because AOSP has
never enabled the FTS5 module) and ships **inside the content artifact**.

And the delivery path already covers existing installs: `ContentArtifactInstaller.installIfChanged()`
runs at database-provision time in `DatabaseModule` and **replaces the content database on
update**, which is precisely how a release reaches an existing install now that the content
database stopped holding user data at schemaVersion 23.

Building a second index on-device would therefore duplicate a path that already exists.

**The actual exposure** is narrower. `installIfChanged()` can return
`Outcome.DeferredForLegacyData(table)` when `UserDataMigrator` has not yet moved user data out of
the content database. A device in that state **never receives a new artifact**, so it keeps an
index-less database indefinitely — `status()` returns `Absent`, search falls back to `LIKE`, and
`LIKE` returns zero rows for every Arabic query. `Mismatched` has the same effect by a different
route when a stamped `fold_version` disagrees with the app's.

**What we do instead:** instrument the installer's outcomes so the deferred and mismatched
populations are measurable, then unblock the deferred path. This fixes stale *content* as well as
search, and it is a fraction of the work.

---

## 3. Cross-repo ordering

Exactly one item crosses repositories: §2.4's precomputed ayah-division columns.

The Quran projection duplicated eight times in `QuranDao` is expensive by construction. Its
`hizb_quarters` and `rukus` joins are **range joins** (`a.id BETWEEN hq.start_ayah_id AND
hq.end_ayah_id`), which SQLite cannot serve from an index the way it serves an equality join; and
its `(SELECT surah_id, MIN(number) … GROUP BY surah_id)` subquery re-scans and re-groups the whole
`rukus` table on every call, including the single-ayah lookup `getAyahWithTextById`.

This is shipped, read-only reference data. It should not be computed at read time at all.

**nimaz-data goes first.** Add `ruku_number`, `ruku_end_ayah_id`, `rub_number` and
`rub_start_ayah_id` as build-time derived columns on `ayahs`, with a pipeline validation check
asserting the derived values agree with the source `rukus` and `hizb_quarters` tables. Bump the
schema, cut a `data-v9` release. `AyahDivisionsTest` already pins this logic on the Android side —
this moves where it runs, not what it means.

**Android follows.** The `@DatabaseView` half of §1.1 is independent of the data release and lands
first (PR 8). Only PR 9 — dropping the range joins in favour of the new columns — is gated on
`data-v9` being tagged and the app's pinned artifact version bumped to it. Nothing else in the
stack waits.

nimaz-data as of `69ca182` (tag `data-v8`) already carries the thematic additions
(`ayah_themes.json`, `quran_topics.json`, `surah_overview_en.json`); the derived-column work
builds on that head.

---

## 4. The stack

Twelve stacked PRs, managed with the `gh stack` extension (`github/gh-stack`). Each targets the
one below it; PR 1 targets `epic/audit`. Branches are `epic/audit-01-ci` … `epic/audit-12-a11y`.

Grouping rationale: thirty PRs would be unreviewable and every low insertion restacks everything
above it; six would each be too large to review well. Twelve is the point where each PR stands
alone, keeps `epic/audit` green, and is reviewable in one sitting. Order puts the cheap
green-keeping infrastructure at the bottom, the tests in the middle, and the refactors at the top
where a broken rebase costs least.

| # | PR | Audit refs |
|---|---|---|
| — | **nimaz-data**: derived division columns, validation check, `data-v9` | §2.4 |
| 1 | CI and build config | §3.7, §2.8, §3.1 |
| 2 | Baseline profile module, Compose compiler reports | §2.5, §2.6 |
| 3 | Audio correctness and frame-clock cost | §2.2, §2.1, §2.3, §2.7 |
| 4 | Content artifact installer telemetry and deferred unblock | §6.1 |
| 5 | Worker tests, `BootReceiver` test | §3.2, §3.3 |
| 6 | Repository and audio-manager tests | §3.4, §3.5 |
| 7 | ViewModel and DAO tests, list-key guard | §3.6, §2.10 |
| 8 | `@DatabaseView` for the ayah projection | §1.1 |
| 9 | Consume precomputed columns — **gated on `data-v9`** | §2.4 |
| 10 | Screen and scaffolding dedupe | §1.2–§1.6 |
| 11 | `FastTrackerScreen` split, calculator seams | §5.1 |
| 12 | Accessibility and font scaling | §4, §4.1 |

### PR 1 — CI and build config

- `android_instrumented_tests.yml` triggers on `push: branches: [main]`. This repo's branches are
  `dev` and `master`, so **that trigger has never fired**; only `pull_request` does anything.
  Change it to `dev`.
- Add `epic/**` to `internal_testing.yml`'s push-branch filter. It currently lists `claude/**`,
  `feature/**` and `fix/**`, so a `[deploy]` marker commit on `epic/audit` would silently do
  nothing. (`workflow_dispatch` works regardless, since the workflow file is on `dev`.)
- `gradle.properties`: enable `org.gradle.parallel`, `org.gradle.caching` and
  `org.gradle.configuration-cache`; raise `-Xmx2048m`, which is tight for a 965-file Kotlin module
  with KSP.
- Add `jacocoTestReport` to the PR lane and post the number as a PR comment. `pr_checks.yml`
  currently runs `fastlane android test` — `gradle test` plus `gradle lint` — so **no coverage
  number ever appears on a PR** despite four jacoco tasks and a 90% rule existing in
  `app/build.gradle.kts`. Do not gate on a global threshold; gate on not going down.
- `README.md`'s tech-stack version list is stale (claims AGP 8.12.0, Kotlin 2.3.0, Compose BOM
  2026.01.00, Media3 1.9.0, WorkManager 2.11.0; actual 9.2.1, 2.3.21, 2026.05.01, 1.10.1, 2.11.2).
  `scripts/check_docs.py` does not cover the README. Delete the list and point at
  `libs.versions.toml`.

### PR 2 — Baseline profile and Compose reports

Nothing named `baseline` exists outside a test fixture, and `settings.gradle.kts` includes only
`:app`. Across 88k lines of Compose and ~95 routes this is the largest cheap win available.

Add the `androidx.baselineprofile` plugin and a `:baselineprofile` module. Generate over one
journey: cold start → Home → Quran home → open a surah → scroll the reader → global search. Emit a
startup profile too.

Turn on Compose compiler metrics and reports in the same PR, and **change nothing on their basis
yet**. The presentation layer has 4 `@Immutable`, 1 `@Stable` and no immutable collections across
375 files, against 48 `List<…>` and 6 `Map<…>` parameters in `presentation/components` alone.
Kotlin 2.3 strong skipping compares unstable parameters by identity rather than treating them as
always-changed, but a `List` rebuilt on every emission still fails that comparison. Fix only what
the report names as restartable-but-not-skippable on a hot path — the reader, the home carousel,
the search results list — in a later pass. Do not rewrite on suspicion.

### PR 3 — Audio correctness and frame-clock cost

**The cancellation defect (§2.2) is the one live correctness bug in this epic.** Inside
`downloadAllAyahs`'s `withContext(Dispatchers.IO)`, per-file jobs are started with
`scope.launch(Dispatchers.IO)` — `scope` being the manager's own
`CoroutineScope(SupervisorJob() + Dispatchers.Main)`. That makes them **siblings** of
`downloadJob`, not children. So `downloadJob?.cancel()` stops the waiting and leaves the downloads
running; they keep writing `downloadedCount` and `downloadProgress` into the shared audio flow.
Switch surah mid-download and the old surah's progress overwrites the new one's, then jumps
backwards. Fix: `coroutineScope { launch { … } }` inside the `withContext`, so cancellation
propagates.

This sits in exactly the class the registry declared handled — its audio entry is about
encapsulation (making `audioManager` private), and nobody looked at what the manager does.

**Streaming (§2.1).** `downloadAllAyahs` downloads *every* file in the playlist before playback
starts — 286 for Al-Baqarah. The concurrency is also barrier-shaped: `chunked(5)` then
`jobs.forEach { it.join() }`, so the slowest file in each group of five stalls the next group.
**Decision: delete the function and let Media3 do it.** Wire a `CacheDataSource.Factory` over a
`SimpleCache` and hand ExoPlayer the URLs directly, so it streams, caches to disk and prefetches
the next item — which is what the `playAyahsSequentially` gapless path exists for. The manual
alternative (download two or three ayahs, start playing, keep a rolling window of three ahead of
the play head, and replace `chunked(5) + join` with a `Semaphore(5)`) is recorded here only as the
fallback if the Media3 cache path proves unworkable against the CDN; it is not the plan.

**Position tracking (§2.3).** `startPositionTracking` loops every 100 ms, recomputing
`computeTotalPosition` and `computeTotalDuration` across the whole playlist and pushing a new
object into the audio flow — ten times a second, forever, including background playback with the
screen off. Tick at 400–500 ms and let the progress bar animate between ticks. Suspend the loop
when no screen is collecting.

**`PrayerSkyScene` (§2.7).** The drawing itself is good — `drawWithCache` with baked sprite
layers, not per-frame vector work. But `rememberInfiniteTransition` with a 120-second `tween`
produces a frame *every frame, forever*, for as long as Home is visible, and it still runs when
`cloudsEnabled` is false (animating 0f → 0f). Skip creating the transition when clouds are off,
and drive the phase from a `LaunchedEffect` ticking about once a second. At a 120-second cycle
nobody can see the difference and the compositor gets to idle.

### PR 4 — Content artifact installer

Per §2.2 above. Instrument `installIfChanged()`'s outcomes — `DeferredForLegacyData`,
`Failed`, `Mismatched` index state — so the affected populations are measurable rather than
theoretical, then unblock the deferred path so those devices receive an artifact. Verify with a
test that an install carrying legacy user data eventually migrates and then installs.

### PR 5 — Workers and `BootReceiver`

Seven workers, zero tests: `AdhanDownloadWorker`, `PrayerTimesWorker`, `PrayerTrackerWorker`,
`NextPrayerWorker`, `KhatamWorker`, `HijriDateWorker`, `HijriCalendarWorker`. These keep the
widgets correct, and when one fails the user does not file a bug — they stop trusting the widget.
`TestListenableWorkerBuilder` runs a `CoroutineWorker` in a plain JVM test with fake dependencies;
each is roughly 30 lines. **Highest return per line of test code in the repo.**

`BootReceiver` (`core/util/BootReceiver.kt`, 851 lines) is the riskiest untested file in the app:
if it regresses, prayer notifications silently stop after every reboot and the failure is
invisible until a user complains weeks later. It also holds a 21-line internal clone at `:96`/`:128`
— two near-identical reschedule paths that can drift. Test it, and collapse the clone.
`PrayerNotificationScheduler` (910 lines) *is* tested, which makes this gap worse, not better.

### PR 6 — Repositories and audio managers

Fifteen of nineteen repositories are untested. This is where database rows become domain objects,
so it is where a bad content artifact shows up first — and since the content database is a
separately built artifact swapped in by `ContentArtifactInstaller`, repository tests against an
in-memory Room database are the early warning that a data-pipeline change broke the app. Start
with `QuranRepositoryImpl` (900 lines) and `UserDataRepositoryImpl` (the migration path).

Six audio managers are untested, `QuranAudioManager` among them at 794 lines. A test that starts a
download, cancels, and asserts no further writes to the audio flow would have caught PR 3's
defect; write that test as part of this PR so the fix is regression-proof.

### PR 7 — ViewModels, DAOs, list keys

Seven untested ViewModels: `HadithViewModel`, `KhatamViewModel`, `TasbihViewModel`,
`SurahThematicViewModel`, `AsmaUlHusnaViewModel`, `AsmaUnNabiViewModel`, `ProphetViewModel`.
Eleven of twenty-one DAOs untested, notably `SearchIndexDao` (the FTS path, which PR 4 depends on
behaving) and `ReadingProgressDao`. Also `WidgetUpdateScheduler`, `NimazMessagingService`,
`AdhanPlaybackService`, `QuranAudioService`.

Twenty-two of sixty-two `items(` calls have no `key`. `LazyListKeyGuardTest` already exists —
extend it to cover the rest rather than fixing them by hand, so the guard holds for new code too.

### PR 8 — `@DatabaseView`

The projection beginning `SELECT a.*, u.text AS text_uthmani, …`, with five `LEFT JOIN`s and the
`rukus`/`MIN(number)` subquery, is written out eight times in `QuranDao`, differing only in the
`WHERE` clause. There are zero `@DatabaseView` declarations in the project. Declare it once as
`ayah_with_text`; every query becomes `SELECT * FROM ayah_with_text WHERE …`. Eight query bodies
collapse to eight one-liners with one place to change when the projection grows.

### PR 9 — Precomputed columns (gated)

Replace the range joins and the regrouping subquery with plain indexed selects over the columns
nimaz-data now ships. **Blocked on `data-v9`.** Updates `docs/SUBSYSTEMS.md` §5 including the
schema-version line, which `scripts/check_docs.py` checks against `NIMAZ_DATABASE_VERSION`.

### PR 10 — Dedupe

Copy-paste detection over `app/src/main` measures 0.33% at a 25-line threshold and 2.36% at 10 —
low, and clustered:

- **Mushaf renderers** (§1.2): 65-line exact clone at `MushafPage.kt:202` / `MushafLinePage.kt:202`
  plus 28 more at `:274`/`:287`. Extract the shared per-verse action host, per §2.1 of this doc.
- **Catalog screens** (§1.3): `AsmaUlHusnaListScreen` ↔ `AsmaUnNabiListScreen` (59 lines plus 30
  identical imports), ↔ `ProphetsListScreen` (30 lines); `AsmaUlHusnaDetailScreen` ↔
  `AsmaUnNabiDetailScreen` (28 lines plus 20 more). Three list screens and two detail screens
  differing only in which repository they read and what the header says. One
  `CatalogListScreen(config)` plus `CatalogDetailScreen(config)` removes roughly 200 lines and
  makes a fourth catalog a config entry rather than a screen.
- **Prayer grids** (§1.4): `PrayerTrackerScreen:242` ↔ `QadaPrayersScreen:68` (46 lines) and
  `:320`/`:142` (31 lines). Extract the five-prayer grid.
- **Settings, widgets, workers** (§1.5): `DuaSettingsScreen` ↔ `HadithSettingsScreen` (26),
  `HijriDateWidget` ↔ `NextPrayerWidget` (28), `KhatamWidget` ↔ `PrayerTrackerWidget` (26),
  `HijriCalendarWorker` ↔ `HijriDateWorker` (21). `widget/core/WidgetWork.kt` already exists as
  the shared scheduling helper — push the Glance provider boilerplate and worker bodies into it.
- **Within-file repeats** (§1.6), cheap while in the file: `HomeScreen.kt` (`:398`/`:549`,
  `:311`/`:478`), `KhatamCards.kt` (`:190`/`:375`), `WidgetsScreen.kt` (`:333`/`:475`),
  `NavGraph.kt` (`:603`/`:708`), `ArabicText.kt` (`:53`/`:91`), `QuranReaderScreen.kt`
  (`:980`/`:1032`), `QuranRepositoryImpl.kt` (`:149`/`:191`), `SyncDataExporter` ↔
  `SyncDataImporter` (`:31`/`:58`). `BootReceiver.kt` (`:96`/`:128`) is handled in PR 5.

### PR 11 — `FastTrackerScreen` and calculator seams

`FastTrackerScreen.kt` is 1,779 lines with 15 private composables. The registry records exactly
this shape being fixed for Khatam — "14 inline private composables across the Khatam screens
collapsed into 4 shared components" — and this one file is larger than all the Khatam screens
combined were.

- `MakeupFastsContent` (`:1308`–`:1646`, ~340 lines) plus its four helper composables is a **whole
  second screen living as private functions**. The registry notes `Route.MakeupFasts` was deleted
  because makeup fasts is a tab inside `FastTrackerScreen` — but a tab that large belongs in its
  own file with its own tests, route or no route. Move it to
  `screens/fasting/MakeupFastsTab.kt`.
- `calculateAyyamAlBeedDays` (`:1091`) is a pure function over `LocalDate` doing business logic
  inside a screen file. Move it to `domain/` beside the other Hijri calculations, where it can be
  tested and where it will honour the `hijriDayOffset` preference that registry Open #10 says the
  Hijri helpers currently ignore. It also reads "today" directly — the same pattern the registry
  removed from ten ViewModels via `TodayProvider`. Use the seam.
- `RamadanBanner`, `RamadanCountdownCard` and `RamadanMissedFastsTracker` move to the shared
  component layer; a Ramadan countdown is not fasting-tab-specific.
- `WidgetsScreen` (1,144 lines) constructs `PrayerTimeCalculator()` directly — the last site of a
  pattern the checklist already removed from five ViewModels. Give it the seam.

`SettingsViewModel` (1,171 lines) is left alone: the registry argues persuasively that it is
legitimately large.

### PR 12 — Accessibility and font scaling

The one area with no coverage at all. Verified on `dev`: **370+ `contentDescription`s, and zero
`heading()` and zero `stateDescription` in the entire presentation layer** (374 / 0 / 0 measured).
Those two absences are the whole TalkBack story.

- **Headings.** A TalkBack user on `SettingsScreen`, `FastTrackerScreen` or `SurahInfo` has no way
  to jump between sections and must swipe through every element in order.
  `Modifier.semantics { heading() }` on each section title is one line per site, and it is the
  difference between the app being usable with a screen reader and merely operable.
- **State descriptions.** The prayer tracker is the app's core daily interaction and its toggles
  announce as "checked" / "not checked". They should say "Fajr, prayed on time" or "Asr, not yet
  recorded". Same for fasting toggles, bookmark buttons and khatam read markers.
- **Audit the nulls.** 133 of the `contentDescription`s are `= null`. Often correct — a decorative
  icon beside a label should be silent — but 133 is a lot never to have audited, and a null on a
  genuinely informative icon is invisible in review. Likewise the 19 `clearAndSetSemantics` calls,
  each deliberately hiding a subtree.
- **`AccessibilityChecks.enable()`** in the Espresso setup, so missing labels and sub-48dp touch
  targets are caught automatically on the instrumented lane we already run.
- **Font scaling.** `fontSize = …dp` appears zero times — text sizing is correct throughout. But
  there are 183 fixed `.height(…)` calls in `presentation/components` and 195 more in screens,
  against only 17 references to `fontScale` or `LocalDensity` anywhere. Android 14+ scales fonts to
  200%; a row with a hardcoded height and two lines of text clips at 150%, and it clips *silently*.
  Use `Modifier.heightIn(min = …)` wherever the container holds text. This matters more here than
  in most apps because the Amiri and Nastaliq faces have taller line boxes than Latin ones at the
  same nominal size. Validate by walking the ten most-used screens at 200% font scale and 200%
  display size.

`docs/ARCHITECTURE.md` gains an accessibility obligation in this PR, so the pattern is prescribed
rather than merely applied once.

---

## 5. Out of stack, tracked on the epic

Filed as sub-issues so nothing is lost, but not part of the twelve PRs and not blocking the
internal build.

**§2.9 — fonts.** 3 MB across 8 TTFs in `res/font`, all in the base APK (language splits are
correctly disabled, so nothing is stripped). `noto_nastaliq_urdu.ttf` (690 KB) and the two
Scheherazade faces (925 KB together) are needed only for particular scripts and translations, so
they are candidates for an on-demand feature module. Held out because this is a delivery-mechanism
change that can strand a user mid-flight with no Nastaliq face — it needs a spike, not a refactor
PR.

**§5.2 — permissions.** Twenty-two declared; two need a decision.
`USE_EXACT_ALARM` alongside `SCHEDULE_EXACT_ALARM` is Play-restricted: apps whose core function is
alarms or reminders may use it without the runtime prompt, and prayer apps have been approved, but
it needs an explicit Play Console declaration and a **rejection blocks the release** rather than
degrading it. Confirm the declaration is on file rather than discovering it at submission — a
Console action, not a code change. Separately, eight Bluetooth/Nearby/WiFi permissions plus
`CAMERA` exist for device-to-device sync and appear on the Play listing; "this prayer app wants
Bluetooth and your camera" is a real install-conversion cost for a feature most users never touch.
Check whether the pre-API-31 `BLUETOOTH` / `BLUETOOTH_ADMIN` pair still needs declaring at
`minSdk = 29` given `BLUETOOTH_SCAN` / `CONNECT` / `ADVERTISE` are also present. Needs a device
test on an API 29 target.

---

## 6. Process

### Issues

One `epic`-labelled issue in `arshad-shah/Nimaz` linking roughly thirty sub-issues, each labelled
with its audit section. The nimaz-data derived-columns issue is filed in `arshad-shah/nimaz-data`
and cross-referenced from the epic body **by full URL**, since GitHub sub-issues cannot span
repositories. A second epic issue is created for Part 6's features, so the deferred work has a
home before this epic starts.

`gh` must be on the `arshad-shah` account for both repos; the EMU account cannot write here.

### Definition of done, per PR

```bash
./gradlew :app:compileDebugKotlin     # KSP → validates Hilt + Room wiring
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug              # slow (~10 min) and CI-blocking — not optional
python3 scripts/check_docs.py
```

`lintDebug` is a real gate: `fastlane`'s `test` lane runs `gradle test` *and* `gradle lint`, and
lint catches a class of defect the other three cannot — `LocalContextGetResourceValueCall` and
`MissingTranslation` among them.

Per the CLAUDE.md documentation contract, the doc that owns each area is updated **in the same
commit**, not as a follow-up:

| PR | Doc obligation |
|---|---|
| 3, 4, 5 | `docs/SUBSYSTEMS.md` — audio, content delivery, background work |
| 9 | `docs/SUBSYSTEMS.md` §5 including the schema-version line |
| 10, 11 | `docs/ARCHITECTURE.md` §9 registry; `docs/CLEAN_ARCHITECTURE_CHECKLIST.md` ticks |
| 12 | `docs/ARCHITECTURE.md` — accessibility becomes prescribed house style |

Registry Open #14 is closed in PR 10 by recording that the `AyahActionsBottomSheet` retirement
stands.

### Landing and validation

1. Cut `epic/audit` from `dev`. Build the stack on it with `gh stack init` / `add` / `submit`.
2. Land all twelve into `epic/audit`. PR 9 waits for `data-v9`; nothing else does.
3. `git commit --allow-empty -m "chore: internal build [deploy]"` on `epic/audit` ships it to the
   Play internal track (this needs PR 1's `epic/**` filter addition, or a `workflow_dispatch` run).
4. Validate on device.
5. Only then does `epic/audit` → `dev` open. `deploy.yml` is `push: [dev]`-only and tags a GitHub
   release, so nothing reaches production before that sign-off.
