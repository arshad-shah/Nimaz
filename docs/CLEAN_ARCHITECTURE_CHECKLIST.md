# Clean Architecture — Anti-Pattern Checklist

> A **living, tick-box backlog** of clean-architecture violations to chip away at over time.
> Pair it with [`ARCHITECTURE.md`](ARCHITECTURE.md) (the canonical patterns) and its §9 registry
> (resolved vs open). Each item is small enough to land in its own PR. When you fix one, check
> its box and, if it removes the last instance of an anti-pattern, note it in `ARCHITECTURE.md` §9.

**How to use this**
1. Pick any unchecked box (they're independent — no required order).
2. Apply the fix, run `./gradlew :app:compileDebugKotlin && ./gradlew :app:testDebugUnitTest`.
3. Tick the box in the same PR. Re-run the **detection command** for that section to confirm the
   count dropped (and to catch any new instances that crept in).

Counts below were captured during the architecture-consistency pass — treat them as a starting
snapshot, not gospel. Re-run the detection commands to refresh.

---

## AP-1 · Domain depends on the data layer

**Rule:** `domain/` must import nothing from `data/` — no Room `*Entity`, no `*Dao`, no
DAO-defined helper types, no DataStore. The domain layer is pure Kotlin + coroutines.

**Why it hurts:** couples business rules to the database schema; you can't change storage or
unit-test the domain without Room on the classpath.

**Detect:**
```bash
grep -rlnE "import com.arshadshah.nimaz.data\." app/src/main/java/com/arshadshah/nimaz/domain/
```

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
  app/src/main/java/com/arshadshah/nimaz/domain/repository/
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
  + day-of-year rotation, returning a `DailyDuaSelection` domain model). The seeders moved into
  the repositories (seed-then-read), and the daily reads use seeded repository methods
  (`getHadithCount`/`getHadithByOffset`, `getDuasByCategoryOnce`). Behaviour preserved (identical
  queries/selection math; field mappings verified); full unit suite green.
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

> Components (`presentation/components/`) also contain literals; many are intentional gradient
> stops. Prefer named tokens, but a dedicated design-token file (e.g. `BeadDesign.kt`) holding
> grouped palettes is acceptable — the anti-pattern is *scattered* literals inside screen logic.

---

## AP-6 · Data-layer infrastructure injected straight into ViewModels

**Rule (aspirational):** cross-cutting infrastructure should sit behind a domain abstraction.

**Why it hurts:** ties many ViewModels to a concrete data/infra class; harder to fake in tests.
This one is **pervasive and lower priority** — listed so it's tracked, not because it's urgent.

- [x] ~~**`PreferencesDataStore` injected directly** into many ViewModels.~~ **Resolved.**
  Extracted a `domain/repository/SettingsRepository` interface (147 members); `PreferencesDataStore`
  now implements it, and `UserPreferences` moved to `domain/model`. All 13 ViewModels + `MainActivity`
  inject `SettingsRepository`; bound via `@Binds` in `RepositoryModule`. Data-layer consumers
  (seeders, sync, workers, `AppInitializer`, `BootReceiver`) keep the concrete class.
  - [ ] **Minor leftover:** `settings/WidgetsScreen.kt` still *instantiates* `PreferencesDataStore(context)`
    inline (line ~793) instead of going through DI/a ViewModel — convert when that screen is next touched.
- [ ] **Audio managers expose data-layer `AudioState`.** `QuranViewModel` /
  `QaidaReaderViewModel` surface `audioManager.state` (a `data.audio` type) to the UI. This is an
  **accepted pattern** for playback features (see `ARCHITECTURE.md` §9). If desired, mirror the
  fields the UI needs into a domain/UI-state type to drop the `data.audio` import — optional.

---

## AP-7 · General watchlist (no scripted detector — review during PRs)

- [ ] **God ViewModels / mega-state:** a single VM owning many unrelated `StateFlow`s is fine for
  distinct sub-screens (house style), but watch for one VM serving several *features*.
- [ ] **`!!` and unsafe casts** on domain/data boundaries — prefer safe mapping + defaults (see
  the `runCatching { … }.getOrDefault(…)` enum mapping in `ZakatRepositoryImpl`).
- [ ] **Mapping duplicated across VMs:** if two ViewModels both map the same entity→UI shape,
  push the mapping down into the repository (`toDomain()`).
- [ ] **Use cases that only re-expose a repository verbatim across a whole feature:** acceptable
  (the wrapper is the seam the UI depends on), but if a use case adds no value and the feature is
  trivial, that's fine — don't over-engineer net-new tiny features.

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

### AP-7.2 · `Get*` and `Observe*` variants of the same read

- [x] ~~**Khatam had both `GetActiveKhatamUseCase` and `ObserveActiveKhatamUseCase`**~~ (also
  `getReadAyahIds`/`observeReadAyahIds`, `getJuzProgress` with no Flow variant). **Resolved** by
  **deleting** the one-shot variants rather than documenting them. When both exist, a call site
  can silently pick the stale one and nothing flags it. Keep the one-shot form only where a read
  genuinely cannot be a Flow (e.g. `getNextUnreadPosition`, which joins the ayah table).

Detect:

```bash
# A Get* use case whose Observe* twin also exists
rg -o -N 'class (Get|Observe)(\w+)UseCase' app/src/main/java/com/arshadshah/nimaz/domain/usecase   | sed -E 's/.*class (Get|Observe)(\w+)UseCase//' | sort | uniq -d
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
