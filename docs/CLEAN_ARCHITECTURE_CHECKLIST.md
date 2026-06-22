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

- [ ] **`HomeViewModel` injects three DAOs** (`FastingDao`, `HadithDao`, `DuaDao`) for the
  "daily hadith / daily dua of the day" features. This is entangled with seeders and DB integer
  ids — **see AP-4** for the proper fix (extract use cases). Removing the DAO injections here
  resolves both AP-2 and AP-4 for Home.
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

- [ ] **Home "content of the day" rotation.** `HomeViewModel.loadDailyHadith()` /
  `loadDailyDua()` pull total counts + offsets from DAOs and compute the daily pick inline
  (Knuth-hash day scatter, time-of-day category, day-of-year rotation). **Fix:** extract
  `GetDailyHadithUseCase` and `GetDailyDuaUseCase` (owning the seeders + the selection logic),
  returning domain models. ⚠️ Behaviour-bearing — validate against the current output (the domain
  models differ from entities: `Hadith.grade` is an enum, `DuaCategory.iconName` vs `icon`,
  String vs Int ids), ideally with a runtime/visual check.
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

Snapshot: **21 screen files** still hold raw literals. Tackle a few per PR (relocate into
`NimazColors`, preserving exact hex — zero visual change). Top offenders:

- [ ] `fasting/FastTrackerScreen.kt` (15)
- [ ] `tasbih/BeadDesign.kt` (14 — bespoke bead-style gradients; relocate to a
  `NimazColors.TasbihBeadStyles` token group, under visual review)
- [ ] `hadith/HadithCollectionScreen.kt` (11 — `getBookGradient`; relocate to a
  `NimazColors.HadithCollectionColors` group)
- [ ] `prayer/PrayerStatsScreen.kt` (10)
- [ ] `settings/NotificationSettingsScreen.kt` (9)
- [ ] `prayer/PrayerTrackerScreen.kt` (8)
- [ ] `onboarding/OnboardingArt.kt` (8 — illustration art; may legitimately keep brand literals,
  but prefer named tokens)
- [ ] `help/HelpContentUi.kt` (7), `dua/DuasCollectionScreen.kt` (7),
  `calendar/IslamicCalendarScreen.kt` (7)
- [ ] Remaining lower-count files (run the detect command for the live list):
  `settings/AppearanceSettingsScreen.kt`, `quran/QuranHomeScreen.kt`,
  `prayer/MonthlyPrayerTimesScreen.kt`, `hadith/HadithGradeChip.kt`,
  `onboarding/OnboardingScreen.kt`, …
- [x] ~~`zakat/ZakatCalculatorScreen.kt`, `zakat/ZakatHistoryScreen.kt`~~ — **resolved**.

> Components (`presentation/components/`) also contain literals; many are intentional gradient
> stops. Prefer named tokens, but a dedicated design-token file (e.g. `BeadDesign.kt`) holding
> grouped palettes is acceptable — the anti-pattern is *scattered* literals inside screen logic.

---

## AP-6 · Data-layer infrastructure injected straight into ViewModels

**Rule (aspirational):** cross-cutting infrastructure should sit behind a domain abstraction.

**Why it hurts:** ties many ViewModels to a concrete data/infra class; harder to fake in tests.
This one is **pervasive and lower priority** — listed so it's tracked, not because it's urgent.

- [ ] **`PreferencesDataStore` injected directly** into many ViewModels (Home, Quran, Settings,
  Dua, Hadith, Tasbih, Onboarding, Location, …). Consider a `SettingsRepository` /
  `UserPreferences` domain abstraction so ViewModels depend on an interface, not the DataStore
  class. Large blast radius — do incrementally or leave as an accepted infra dependency.
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

---

## Quick full re-scan

```bash
cd app/src/main/java/com/arshadshah/nimaz
echo "AP-1 domain->data:";      grep -rlnE "import com.arshadshah.nimaz.data\." domain/ || echo "  clean"
echo "AP-2 presentation->dao/entity:"; grep -rlnE "import com.arshadshah.nimaz.data.local.database.(dao|entity)" presentation/ || echo "  clean"
echo "AP-2 VM injects Dao/Impl:"; grep -rlnE "private val [a-zA-Z]+: [A-Za-z]+(Dao|RepositoryImpl)" presentation/viewmodel/ || echo "  clean"
echo "AP-3 domain repo->entity:"; grep -rlnE "import com.arshadshah.nimaz.data.local.database.entity" domain/repository/ || echo "  clean"
echo "AP-5 screen color literals:"; grep -rlE 'Color\(0x[0-9A-Fa-f]{6,8}\)' presentation/screens/ | wc -l
```

*Keep this file honest: tick boxes as you go, and add new anti-patterns/instances when you spot
them.*
</content>
