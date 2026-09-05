# Slimming `:app` — the `:feature:home` extraction — design

**Date:** 2026-09-05 · **Branch:** `refactor/slim-app-module` · **Base:** `dev` @ `c3513204`

`:app` holds 53 files and 11,597 lines. About half of that is the Home screen, and **nothing pins
it there**: no manifest entry, no `BuildConfig` field, no reference to the app's `R`, and not one
import of `:app`-only code. It is in `:app` because #551's 22-PR stack ran out before reaching it,
not because it belongs.

This is the twelfth feature module, and the `:feature:onboarding` case — *"nothing had to be
unpicked to extract it"* — which CLAUDE.md names as the one to copy.

---

## 0. What is in `:app` today

Every file classified against one question: **what actually pins it?** Verified by reading, at
`c3513204`.

| Cluster | Files | Lines | Pin |
|---|---:|---:|---|
| `presentation/components` | 21 | 4,053 | **none** |
| `presentation/viewmodel/home` | 3 | 813 | **none** |
| `presentation/screens/home` | 2 | 850 | **none** |
| `data/audio` | 7 | 2,333 | app `R` on the three `<service>` classes |
| `core/util` | 6 | 2,293 | app `R` on `BootReceiver`, `PrayerNotificationScheduler` |
| `core/di` | 6 | 351 | `BuildConfig` on `AiModule`, `AnnouncementModule`, `ContentArtifactModule` |
| `core/navigation/NavGraph.kt` | 1 | 272 | the `NavHost` itself — belongs in `:app` |
| `MainActivity`, `NimazApp`, `core/init` | 3 | 448 | entry points |
| `data/{announcement,platform,repository,widget}` | 4 | 184 | app `R` on `LibraryRepositoryImpl` |

### 0.1 Only two constraints actually pin anything

- **`BuildConfig`** — 4 files (`AiModule`, `AnnouncementModule`, `ContentArtifactModule`,
  `MainActivity`). A library module gets its *own* `BuildConfig` carrying `DEBUG`, but never the
  application identity or `AI_WORKER_BASE_URL`. This is genuine.
- **The app's `R`** — 7 files (`RepositoryModule`, `BootReceiver`, `PrayerNotificationScheduler`,
  the three audio services, `LibraryRepositoryImpl`). Movable only if the resources move too.

**Nothing else in `:app` is pinned by anything.**

### 0.2 A rule the docs state that is false

CLAUDE.md and `ARCHITECTURE.md` §2 both say a manifest entry point can only live in the module
that owns the manifest. **`:feature:widget` already disproves it**:
`feature/widget/src/main/AndroidManifest.xml` declares six `<receiver>` entries with
fully-qualified names, merged into the app's manifest at build time. Android library modules have
their own manifests.

This matters beyond tidiness: it is the sentence that would stop the *next* extraction — audio,
notifications — before it started, by asserting a barrier that is not there. §5 corrects it.

### 0.3 Three pieces of residue #551 left behind

Found while classifying, each verified:

- **`QuranSurahBanner`** (115 lines) is referenced by nothing — no screen, no component, no test
  but its own.
- **`TafseerNoteCard`** (144 lines) is orphaned. Its only consumer, `TafseerChaptersScreen`, moved
  to `:feature:quran`, which cannot import from `:app` — so it declares a **private copy** at
  `TafseerChaptersScreen.kt:164`. The `:app` original kept its Robolectric test, so it still looks
  alive.
- **Four tests in `app/src/testDebug` test `:core:ui` components** —
  `DuaOfTheMomentCardTest`, `HadithOfTheDayCardTest`, `HijriPrimaryTest`, `QaidaCoursePathTest`.
  Their subjects moved to `:core:ui` and they did not, so `:core:ui`'s own coverage floor has never
  measured them.

---

## 1. What moves

### 1.1 To `:feature:home` — 20 files, ~4,616 lines

`presentation/screens/home/` (`HomeScreen.kt`, `HomeGraph.kt`),
`presentation/viewmodel/home/` (`HomeViewModel.kt`, `HomeUiState.kt`, `HomeEvent.kt`),
and the twelve Home-specific components:

`HomeHero`, `HomeHeader`, `HomeDynamicTopBar`, `HomePrayerCard`, `HomeBannerCarousel`,
`HomeBannerSlot`, `HomeAlsoTodaySection`, `HomeOccasionsSection`, `TodayCarousel`,
`TodayInfoCards`, `TodaysProgressCard`, `EventsCarousel`, plus `AnnouncementBanner`,
`FastingStatusCard` and `PrayerTimesSectionHeader`.

`PrayerTimesSectionHeader` is **not** a generic section header despite the name — it takes
`passedCount`, `upcomingCount` and `onSettingsClick`. It is Home's, and it stays with Home.

### 1.2 To `:core:ui` — 4 files, 841 lines

Each has a claim on design-system membership that its location currently contradicts:

| Component | Lines | Why it goes up |
|---|---:|---|
| `NimazCarousel` | 143 | Carries the `Nimaz*` prefix — a claim to be part of the design system — while sitting in the one module nothing else can import |
| `WorshipEventCard` | 392 | `EventCard`, its sibling, is already in `:core:ui`, and CLAUDE.md rule 8 names the two **together** as the components that take `onClick`/`onClickLabel` |
| `JumuahCard` | 134 | `CLEAN_ARCHITECTURE_CHECKLIST.md` already records its icon well and divider as a resolved design-system fix |
| `CountdownTimer` | 172 | Generic: an `Instant` and a `Modifier` in, `HH:MM:SS` out, built on `rememberNow`. No Home coupling of any kind |

### 1.3 Deleted — 2 files + 2 tests, 259 lines

`QuranSurahBanner` and `TafseerNoteCard` (§0.3), with `QuranSurahBannerTest` and
`TafseerNoteCardTest`. **`:feature:quran`'s private copy is left exactly as it is** — promoting it
to a shared component is a Qur'an decision, not a boundary one, and this change must not smuggle
one in.

### 1.4 The result

`:app` goes from **11,597 to ~5,881 lines**, and everything left is pinned by §0.1 or is an entry
point.

---

## 2. What stays, and why

`MainActivity`, `NimazApp`, `AppInitializer`, `NavGraph.kt`, `data/audio`, `core/util`, `core/di`,
and the four small `data/*` files. Not "until a later PR" — these are held by `BuildConfig`, by the
app's `R`, or by being the entry point itself.

**A later epic could still move audio and notifications** now that §0.2 is corrected: their real
pin is `R`, and resources can travel with the code that uses them. That is explicitly **not** this
change (§6).

---

## 3. Build wiring

`feature/home/build.gradle.kts` mirrors `feature/calendar`:

```kotlin
plugins {
    id("nimaz.android.feature")
    jacoco
}

android {
    namespace = "com.arshadshah.nimaz.feature.home"

    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }
}
```

- `settings.gradle.kts` gains `include(":feature:home")`.
- `:app` gains `implementation(project(":feature:home"))`.
- **`:app`'s `coverageModules` gains a `CoverageModule` entry** — `gradlePath = ":feature:home"`,
  `sourceDir = "src/main/kotlin"`, `packageRoot = "com/arshadshah/nimaz/presentation"` (the same
  root `:feature:onboarding` uses, because a module move does not change package names). This is
  **not optional**: `feature/calendar/build.gradle.kts` carries a comment saying a module that
  leaves `:app` without it makes reported coverage *rise by measuring less*.
- `NavGraph.kt` keeps calling `homeGraph()` and does not change.

**Package names do not change.** Every import reads the same either side of the boundary. Two
consequences CLAUDE.md flags and this change will meet: Kotlin will not smart-cast a `val` across
a module boundary, and a fake used on both sides belongs in `testFixtures`.

---

## 4. Testing

35 files live in `app/src/testDebug`. They follow their subjects:

| Destination | Count | Which |
|---|---:|---|
| `feature/home/src/testDebug` | 21 | every `Home*`, `Today*`, `EventsCarousel`, `AnnouncementBanner*`, `FastingStatusCard`, `PrayerTimesSectionHeader` test |
| `core/ui/src/testDebug` | 10 | `CountdownTimerTest`, `JumuahCardTest`, `NimazCarouselTest`, `WorshipEventCardTest`, `WorshipEventCardProximityTest`, `WorshipCardClickTest`, plus the four §0.3 strays — `DuaOfTheMomentCardTest`, `HadithOfTheDayCardTest`, `HijriPrimaryTest`, `QaidaCoursePathTest` |
| deleted | 2 | `QuranSurahBannerTest`, `TafseerNoteCardTest` |
| stays in `:app` | 2 | `AdhanDownloadServiceStartTest`, `SyncPayloadCoverageTest` |

**The four strays are moved to `:core:ui` too.** They test `:core:ui` components; leaving them
behind would keep `:app` holding tests for code it does not contain, which is the thing this change
exists to stop. This is the one piece of scope beyond a pure move, and it is four file moves with
no edits.

**`assembleDebugAndroidTest` is required for this change.** `app/src/androidTest/.../behavior/
WorshipCardNavigationTest.kt` references `WorshipEventCard`, which is moving to `:core:ui`. The
four unit-test gates do not compile `androidTest`, so a branch that skips it goes out clean locally
and red on the emulator.

**Coverage floors are the risk to watch.** Moving 841 lines into `:core:ui` moves their tests with
them, but the ratios will shift in both modules. If `:core:ui:coverageFloor` fails, the answer is
tests, not a lowered floor — the task's own message says so.

---

## 5. Documentation (same commit)

- **`CLAUDE.md`** — add `:feature:home` to the module list; correct the manifest claim (§0.2) and
  say what the real pins are; update the `:app` line ("52 files / 11,484 lines, 8%") to the new
  figures; add `./gradlew :feature:home:check` to the verify list.
- **`ARCHITECTURE.md`** — §2 the same correction; §8 note that `WorshipEventCard`, `NimazCarousel`,
  `JumuahCard` and `CountdownTimer` are now design-system components; §9 record the extraction and
  the deleted residue.
- **`docs/NAVIGATION.md`** — the graph table lists `homeGraph | 1`; its **file path changes**, so
  check §1's file table. No `Route` or `ScreenTags` change, so NAV-01…NAV-05 are unaffected.
- **`docs/TESTING.md`** — the moved tests' module column.
- **`docs/CLEAN_ARCHITECTURE_CHECKLIST.md`** — add the residue pattern: *a consumer moves module,
  the producer stays, a private copy appears, and the orphan keeps its test*. It has a detection
  command: a component whose only references are its own file and its own test.
- `python3 scripts/check_docs.py` (via `py -3` on this machine — `python3` is not on PATH).

---

## 6. Out of scope

- **Collapsing `PrayerTimeCard` into `NimazPrayerRow`.** This extraction unblocks it; the decision
  is Home's redesign, not this boundary.
- **Extracting audio and notifications.** Possible now that §0.2 is corrected; a separate epic.
- **Promoting `:feature:quran`'s private `TafseerNoteCard`.**

---

## 7. Build sequence

Each step compiles and is reviewable alone.

1. **Delete the residue** (§1.3) — two components and two tests. Smallest possible first commit,
   and it shrinks what step 3 has to move.
2. **Move the four design-system components to `:core:ui`** (§1.2) with their six tests, plus the
   four stray tests (§4). `:app` still holds Home.
3. **Create `:feature:home`** (§3) and move the 17 files and 21 tests into it.
4. **Docs** (§5).

**Gates:** `:core:ui:check`, `:feature:home:check`, `:app:compileDebugKotlin`,
`:app:testDebugUnitTest`, **`:app:assembleDebugAndroidTest`**, `lintDebug`, `check_docs.py`.
