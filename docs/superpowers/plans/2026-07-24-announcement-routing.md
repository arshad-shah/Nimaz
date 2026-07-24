# Parameterised Announcement Routing — Implementation Plan (Plan 2 of 3)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make parameterised deep routes (e.g. `quran/surah/18/ayah/1`, `names/allah/40`, `khatam/7`) reachable from an announcement, resolve the route once, and log rejected keys.

**Architecture:** Add a parameterised tier to the pure `announcementRoute(key)` function (static allowlist first, then a segment-based matcher with range-checked int args). Refactor `ResolveAnnouncementRouteUseCase` to inject `(String) -> Route?` and have `AnnouncementAction.NavigateToFeature` carry the resolved `Route`. Add an `announcement_route_rejected` analytics event fired where the announcement id is available.

**Tech Stack:** Kotlin, type-safe Navigation Compose (`Route` sealed interface), Hilt DI, plain JUnit4 + Google Truth.

## Global Constraints

- Package root `com.arshadshah.nimaz`. App **Nimaz**.
- `announcementRoute` stays a **pure function** returning `Route?` — no Android/Compose deps, no I/O (it's tested under plain JUnit4, `app/src/test/java`, no Robolectric).
- **No invented routes.** Every arm must map to a `Route` type that already exists in `core/navigation/Routes.kt`. If a candidate key has no existing `Route`, skip it and record the skip — never create a new `Route` in this plan.
- Static allowlist is checked **before** the parameterised tier; a parameterised key must never be shadowed by a static one (they differ by segment count today, but assert it in tests).
- Integer args are range-checked at resolve time; an out-of-range or malformed arg fails the whole key (returns `null`), exactly like an unknown key — the CTA hides rather than crashing a screen.
- String-id routes (`DuaReader`, `HadithReader`, `HadithBook`, `HadithChapter`, `DuaCategory`) are accepted **syntactically** (non-blank). Whether their destination screens have empty states is out of scope here — see the "Deferred" note.
- Tests: `app/src/test/java/...`, plain JUnit4 (`import org.junit.Test`, no `@RunWith`, no Robolectric), Google Truth `assertThat`.
- Verify: `./gradlew :app:testDebugUnitTest` and `./gradlew :app:compileDebugKotlin`. (JDK 21 + sdk.dir configured.)
- Do not push to `dev`. Branch `feat/event-cards-celebration-routing` (continues from Plan 1).

## File Structure

- Modify `core/navigation/AnnouncementRoutes.kt` — add `parameterisedAnnouncementRoute`, rename the existing `when` body to `staticAnnouncementRoute`, keep `announcementRoute` as the single entry point.
- Modify `app/src/test/java/.../core/navigation/AnnouncementRoutesTest.kt` — extend, don't replace.
- Modify `domain/model/Announcement.kt` — `NavigateToFeature` gains `route: Route`.
- Modify `domain/usecase/AnnouncementUseCases.kt` — `ResolveAnnouncementRouteUseCase` injects `(String) -> Route?`.
- Modify `core/di/AnnouncementModule.kt` — pass `::announcementRoute` instead of the boolean wrapper.
- Modify `app/src/test/java/.../domain/usecase/AnnouncementUseCasesTest.kt` — update `NavigateToFeature` expectations.
- Modify `core/monitoring/AppAnalytics.kt` — add `logAnnouncementRouteRejected`.
- Modify `presentation/viewmodel/HomeViewModel.kt` — fire the rejection log when a non-empty route resolves to `None`.
- Modify docs: `docs/NAVIGATION.md`, `docs/ARCHITECTURE.md` (§9), `docs/SUBSYSTEMS.md`.

---

## Task 1: Parameterised `announcementRoute` grammar + tests

**Files:**
- Modify: `app/src/main/java/com/arshadshah/nimaz/core/navigation/AnnouncementRoutes.kt`
- Test: `app/src/test/java/com/arshadshah/nimaz/core/navigation/AnnouncementRoutesTest.kt`

**Interfaces:**
- Consumes: `Route.*` types from `core/navigation/Routes.kt` (all confirmed present): `QuranReader(surahNumber: Int, ayahNumber: Int = 1)`, `QuranPage(pageNumber: Int)`, `QuranJuz(juzNumber: Int)`, `Tafseer(surahNumber: Int, ayahNumber: Int = 1)`, `SurahInfo(surahNumber: Int)`, `DuaCategory(categoryId: String)`, `DuaReader(duaId: String)`, `HadithBook(bookId: String)`, `HadithChapter(bookId: String, chapterId: String)`, `HadithReader(hadithId: String)`, `TasbihCounter(presetId: Long? = null)`, `PrayerTracker(initialTab: Int = 0)`, `QaidaReader(lessonId: Int)`, `IslamicMonth(month: Int, year: Int)`, `AsmaUlHusnaDetail(nameId: Int)`, `AsmaUnNabiDetail(nameId: Int)`, `ProphetDetail(prophetId: Int)`, `KhatamDetail(khatamId: Long)`, plus objects `AllBookmarks`, `FastingTracker`, `FastingStats`, `MonthlyPrayerTimes`, `ZakatHistory`, `TasbihPresets`, `TasbihStats`, `TasbihHistory`.
- Produces: `announcementRoute(key: String?): Route?` — unchanged public signature, now resolves parameterised keys too.

**Design notes:**
- Keep the existing static `when` verbatim, renamed to `private fun staticAnnouncementRoute(key: String): Route?`.
- `announcementRoute` trims and strips leading/trailing `/`, returns `null` on null/blank, tries static then parameterised.
- The `hadith/{id}` arm (size 2) must NOT swallow `hadith/book` — guard the reserved second segment.

- [ ] **Step 1: Write the failing tests (extend the existing file)**

Replace the contents of `app/src/test/java/com/arshadshah/nimaz/core/navigation/AnnouncementRoutesTest.kt` with (keeps the 3 original tests, adds the parameterised/boundary/malformed suite):

```kotlin
package com.arshadshah.nimaz.core.navigation

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AnnouncementRoutesTest {

    @Test
    fun `known static keys resolve to routes`() {
        assertThat(announcementRoute("home")).isEqualTo(Route.Home)
        assertThat(announcementRoute("quran")).isEqualTo(Route.Quran)
        assertThat(announcementRoute("search/ask")).isEqualTo(Route.GlobalSearch)
        assertThat(announcementRoute("search/settings")).isEqualTo(Route.SearchSettings)
        assertThat(announcementRoute("prayer/tracker")).isEqualTo(Route.PrayerTracker())
        assertThat(announcementRoute("settings/about")).isEqualTo(Route.SettingsAbout)
        assertThat(announcementRoute("khatam")).isEqualTo(Route.KhatamList)
    }

    @Test
    fun `unknown key resolves to null`() {
        assertThat(announcementRoute("brand/new/feature")).isNull()
        assertThat(announcementRoute("")).isNull()
        assertThat(announcementRoute(null)).isNull()
    }

    @Test
    fun `urls are not feature keys`() {
        assertThat(announcementRoute("https://nimaz.arshadshah.com/privacy")).isNull()
    }

    @Test
    fun `parameterised quran keys resolve`() {
        assertThat(announcementRoute("quran/surah/18")).isEqualTo(Route.QuranReader(18))
        assertThat(announcementRoute("quran/surah/18/ayah/10"))
            .isEqualTo(Route.QuranReader(18, 10))
        assertThat(announcementRoute("quran/surah/18/info")).isEqualTo(Route.SurahInfo(18))
        assertThat(announcementRoute("quran/page/300")).isEqualTo(Route.QuranPage(300))
        assertThat(announcementRoute("quran/juz/30")).isEqualTo(Route.QuranJuz(30))
    }

    @Test
    fun `parameterised tafseer keys resolve`() {
        assertThat(announcementRoute("tafseer/2")).isEqualTo(Route.Tafseer(2))
        assertThat(announcementRoute("tafseer/2/ayah/255")).isEqualTo(Route.Tafseer(2, 255))
    }

    @Test
    fun `parameterised string-id keys resolve`() {
        assertThat(announcementRoute("dua/category/morning"))
            .isEqualTo(Route.DuaCategory("morning"))
        assertThat(announcementRoute("dua/reader/istikhara"))
            .isEqualTo(Route.DuaReader("istikhara"))
        assertThat(announcementRoute("hadith/book/bukhari"))
            .isEqualTo(Route.HadithBook("bukhari"))
        assertThat(announcementRoute("hadith/book/bukhari/chapter/1"))
            .isEqualTo(Route.HadithChapter("bukhari", "1"))
        assertThat(announcementRoute("hadith/12345"))
            .isEqualTo(Route.HadithReader("12345"))
    }

    @Test
    fun `parameterised numeric detail keys resolve`() {
        assertThat(announcementRoute("names/allah/40")).isEqualTo(Route.AsmaUlHusnaDetail(40))
        assertThat(announcementRoute("names/prophet/5")).isEqualTo(Route.AsmaUnNabiDetail(5))
        assertThat(announcementRoute("prophets/3")).isEqualTo(Route.ProphetDetail(3))
        assertThat(announcementRoute("qaida/lesson/7")).isEqualTo(Route.QaidaReader(7))
        assertThat(announcementRoute("prayer/tracker/2")).isEqualTo(Route.PrayerTracker(2))
        assertThat(announcementRoute("calendar/9/1447")).isEqualTo(Route.IslamicMonth(9, 1447))
    }

    @Test
    fun `parameterised long-id keys resolve`() {
        assertThat(announcementRoute("khatam/7")).isEqualTo(Route.KhatamDetail(7L))
        assertThat(announcementRoute("tasbih/counter")).isEqualTo(Route.TasbihCounter(null))
        assertThat(announcementRoute("tasbih/counter/42")).isEqualTo(Route.TasbihCounter(42L))
    }

    @Test
    fun `new static object keys resolve`() {
        assertThat(announcementRoute("bookmarks")).isEqualTo(Route.AllBookmarks)
        assertThat(announcementRoute("fasting/tracker")).isEqualTo(Route.FastingTracker)
        assertThat(announcementRoute("fasting/stats")).isEqualTo(Route.FastingStats)
        assertThat(announcementRoute("prayer/monthly")).isEqualTo(Route.MonthlyPrayerTimes)
        assertThat(announcementRoute("zakat/history")).isEqualTo(Route.ZakatHistory)
        assertThat(announcementRoute("tasbih/presets")).isEqualTo(Route.TasbihPresets)
        assertThat(announcementRoute("tasbih/stats")).isEqualTo(Route.TasbihStats)
        assertThat(announcementRoute("tasbih/history")).isEqualTo(Route.TasbihHistory)
    }

    @Test
    fun `integer args are range checked`() {
        assertThat(announcementRoute("quran/surah/0")).isNull()
        assertThat(announcementRoute("quran/surah/1")).isEqualTo(Route.QuranReader(1))
        assertThat(announcementRoute("quran/surah/114")).isEqualTo(Route.QuranReader(114))
        assertThat(announcementRoute("quran/surah/115")).isNull()
        assertThat(announcementRoute("quran/page/0")).isNull()
        assertThat(announcementRoute("quran/page/604")).isEqualTo(Route.QuranPage(604))
        assertThat(announcementRoute("quran/page/605")).isNull()
        assertThat(announcementRoute("quran/juz/31")).isNull()
        assertThat(announcementRoute("names/allah/99")).isEqualTo(Route.AsmaUlHusnaDetail(99))
        assertThat(announcementRoute("names/allah/100")).isNull()
    }

    @Test
    fun `malformed keys resolve to null`() {
        assertThat(announcementRoute("quran/surah/")).isNull()
        assertThat(announcementRoute("quran/surah/abc")).isNull()
        assertThat(announcementRoute("quran//18")).isNull()
        assertThat(announcementRoute("quran/surah/18/ayah")).isNull()
        assertThat(announcementRoute("khatam/notanumber")).isNull()
    }

    @Test
    fun `leading and trailing slashes are tolerated`() {
        assertThat(announcementRoute("/home/")).isEqualTo(Route.Home)
        assertThat(announcementRoute("/quran/surah/18/")).isEqualTo(Route.QuranReader(18))
    }

    @Test
    fun `hadith slash id does not swallow reserved segments`() {
        // "hadith/book" is a prefix, not a hadith id — must not become HadithReader("book")
        assertThat(announcementRoute("hadith/book")).isNull()
    }

    @Test
    fun `non-https urls are not feature keys`() {
        assertThat(announcementRoute("http://example.com")).isNull()
        assertThat(announcementRoute("javascript:alert(1)")).isNull()
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.arshadshah.nimaz.core.navigation.AnnouncementRoutesTest"`
Expected: FAIL — new parameterised assertions return `null` (grammar not implemented yet).

- [ ] **Step 3: Implement the grammar**

Edit `app/src/main/java/com/arshadshah/nimaz/core/navigation/AnnouncementRoutes.kt`. Rename the existing top-level `fun announcementRoute(key: String?): Route? = when (key) { … }` to `private fun staticAnnouncementRoute(key: String): Route? = when (key) { … }` (body unchanged — keep all existing arms). Then add the new entry point and matcher, and the new static object keys:

```kotlin
fun announcementRoute(key: String?): Route? {
    val k = key?.trim()?.trim('/')?.takeIf { it.isNotEmpty() } ?: return null
    staticAnnouncementRoute(k)?.let { return it }
    return parameterisedAnnouncementRoute(k)
}

private fun parameterisedAnnouncementRoute(key: String): Route? {
    val s = key.split('/')
    fun int(i: Int, range: IntRange): Int? =
        s.getOrNull(i)?.toIntOrNull()?.takeIf { it in range }
    fun str(i: Int): String? = s.getOrNull(i)?.takeIf { it.isNotBlank() }
    fun long(i: Int): Long? = s.getOrNull(i)?.toLongOrNull()

    return when {
        s.size == 3 && s[0] == "quran" && s[1] == "surah" ->
            int(2, 1..114)?.let { Route.QuranReader(it) }

        s.size == 5 && s[0] == "quran" && s[1] == "surah" && s[3] == "ayah" ->
            int(2, 1..114)?.let { su -> int(4, 1..300)?.let { Route.QuranReader(su, it) } }

        s.size == 4 && s[0] == "quran" && s[1] == "surah" && s[3] == "info" ->
            int(2, 1..114)?.let { Route.SurahInfo(it) }

        s.size == 3 && s[0] == "quran" && s[1] == "page" ->
            int(2, 1..604)?.let { Route.QuranPage(it) }

        s.size == 3 && s[0] == "quran" && s[1] == "juz" ->
            int(2, 1..30)?.let { Route.QuranJuz(it) }

        s.size == 2 && s[0] == "tafseer" ->
            int(1, 1..114)?.let { Route.Tafseer(it) }

        s.size == 4 && s[0] == "tafseer" && s[2] == "ayah" ->
            int(1, 1..114)?.let { su -> int(3, 1..300)?.let { Route.Tafseer(su, it) } }

        s.size == 3 && s[0] == "dua" && s[1] == "category" ->
            str(2)?.let { Route.DuaCategory(it) }

        s.size == 3 && s[0] == "dua" && s[1] == "reader" ->
            str(2)?.let { Route.DuaReader(it) }

        s.size == 5 && s[0] == "hadith" && s[1] == "book" && s[3] == "chapter" ->
            str(2)?.let { b -> str(4)?.let { Route.HadithChapter(b, it) } }

        s.size == 3 && s[0] == "hadith" && s[1] == "book" ->
            str(2)?.let { Route.HadithBook(it) }

        s.size == 2 && s[0] == "hadith" && s[1] !in RESERVED_HADITH_SEGMENTS ->
            str(1)?.let { Route.HadithReader(it) }

        s.size == 2 && s[0] == "tasbih" && s[1] == "counter" ->
            Route.TasbihCounter(null)

        s.size == 3 && s[0] == "tasbih" && s[1] == "counter" ->
            long(2)?.let { Route.TasbihCounter(it) }

        s.size == 3 && s[0] == "prayer" && s[1] == "tracker" ->
            int(2, 0..10)?.let { Route.PrayerTracker(it) }

        s.size == 3 && s[0] == "qaida" && s[1] == "lesson" ->
            int(2, 1..Int.MAX_VALUE)?.let { Route.QaidaReader(it) }

        s.size == 3 && s[0] == "calendar" ->
            int(1, 1..12)?.let { m ->
                s.getOrNull(2)?.toIntOrNull()?.let { y -> Route.IslamicMonth(m, y) }
            }

        s.size == 3 && s[0] == "names" && s[1] == "allah" ->
            int(2, 1..99)?.let { Route.AsmaUlHusnaDetail(it) }

        s.size == 3 && s[0] == "names" && s[1] == "prophet" ->
            int(2, 1..99)?.let { Route.AsmaUnNabiDetail(it) }

        s.size == 2 && s[0] == "prophets" ->
            int(1, 1..99)?.let { Route.ProphetDetail(it) }

        s.size == 2 && s[0] == "khatam" ->
            long(1)?.let { Route.KhatamDetail(it) }

        else -> null
    }
}

private val RESERVED_HADITH_SEGMENTS = setOf("book", "search", "bookmarks")
```

Add these arms to the **static** `when` in `staticAnnouncementRoute` (only these — their `Route` objects are confirmed to exist):

```kotlin
    "bookmarks" -> Route.AllBookmarks
    "fasting/tracker" -> Route.FastingTracker
    "fasting/stats" -> Route.FastingStats
    "prayer/monthly" -> Route.MonthlyPrayerTimes
    "zakat/history" -> Route.ZakatHistory
    "tasbih/presets" -> Route.TasbihPresets
    "tasbih/stats" -> Route.TasbihStats
    "tasbih/history" -> Route.TasbihHistory
```

- [ ] **Step 4: Verify candidate keys from the spec that need a Route check**

The spec §1.2 also lists `hadith/search`, `hadith/bookmarks`, `dua/favorites`, `dua/search`, `settings/appearance`, `settings/location`, `settings/language`, `settings/prayer-calculation`, `settings/widgets`, `settings/sync`, `qaida/letters`. For each, grep `Routes.kt` for a matching `Route` object:
Run: `grep -nE 'data object (HadithSearch|HadithBookmarks|DuaFavorites|DuaSearch|SettingsAppearance|SettingsLocation|SettingsLanguage|SettingsPrayerCalculation|SettingsWidgets|SettingsSync|QaidaLetters)' app/src/main/java/com/arshadshah/nimaz/core/navigation/Routes.kt`
For any that exists, add its key→Route arm to the static `when` AND a resolving assertion to the test. For any that does NOT exist, do not add a key (no invented routes); note the skipped keys in your report so a later plan can add both the screen and the key together.

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.arshadshah.nimaz.core.navigation.AnnouncementRoutesTest"`
Expected: PASS (all tests). Then `./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/core/navigation/AnnouncementRoutes.kt \
        app/src/test/java/com/arshadshah/nimaz/core/navigation/AnnouncementRoutesTest.kt
git commit -m "feat(nav): parameterised announcement route grammar"
```

---

## Task 2: Resolve announcement routes once (§1.4)

**Files:**
- Modify: `app/src/main/java/com/arshadshah/nimaz/domain/model/Announcement.kt`
- Modify: `app/src/main/java/com/arshadshah/nimaz/domain/usecase/AnnouncementUseCases.kt`
- Modify: `app/src/main/java/com/arshadshah/nimaz/core/di/AnnouncementModule.kt`
- Test: `app/src/test/java/com/arshadshah/nimaz/domain/usecase/AnnouncementUseCasesTest.kt`

**Interfaces:**
- Consumes: `announcementRoute(key: String?): Route?` (Task 1); `Route` (`core/navigation/Routes.kt`).
- Produces:
  - `AnnouncementAction.NavigateToFeature(routeKey: String, route: Route)`.
  - `ResolveAnnouncementRouteUseCase(resolveFeatureKey: (String) -> Route?)` returning `NavigateToFeature(value, route)` / `OpenUrl` / `None`.

**Design note:** `AnnouncementAction` (in `domain/model`) now references `core.navigation.Route` — a deliberate domain→navigation coupling recorded in ARCHITECTURE §9 (Task 4). It is permitted: CLAUDE.md's inward-dependency rule forbids domain→*data* (entities/DAOs/DataStore), not domain→`core/navigation`. The existing `showCta` gate in `HomeViewModel` only checks `!= None`, so the added field does not break it.

- [ ] **Step 1: Write the failing test (update existing)**

Read `app/src/test/java/com/arshadshah/nimaz/domain/usecase/AnnouncementUseCasesTest.kt`. Find the tests for `ResolveAnnouncementRouteUseCase`. Update every construction of the use case to inject a resolver, and every `NavigateToFeature` expectation to include the resolved `Route`. Add/adjust cases so they read:

```kotlin
// helper resolver mirroring the real one
private val resolve = ResolveAnnouncementRouteUseCase(resolveFeatureKey = ::announcementRoute)

@Test
fun `known feature key resolves to NavigateToFeature with route`() {
    val action = resolve("quran/surah/18")
    assertThat(action).isEqualTo(
        AnnouncementAction.NavigateToFeature("quran/surah/18", Route.QuranReader(18))
    )
}

@Test
fun `https url resolves to OpenUrl`() {
    assertThat(resolve("https://nimaz.arshadshah.com/privacy"))
        .isEqualTo(AnnouncementAction.OpenUrl("https://nimaz.arshadshah.com/privacy"))
}

@Test
fun `unknown key resolves to None`() {
    assertThat(resolve("brand/new")).isEqualTo(AnnouncementAction.None)
    assertThat(resolve("")).isEqualTo(AnnouncementAction.None)
    assertThat(resolve(null)).isEqualTo(AnnouncementAction.None)
}
```

Add imports as needed: `com.arshadshah.nimaz.core.navigation.announcementRoute`, `com.arshadshah.nimaz.core.navigation.Route`. Keep any other existing use-case tests in the file unchanged.

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew :app:testDebugUnitTest --tests "com.arshadshah.nimaz.domain.usecase.AnnouncementUseCasesTest"`
Expected: FAIL to compile — `NavigateToFeature` takes one arg / constructor signature mismatch.

- [ ] **Step 3: Update the model**

In `app/src/main/java/com/arshadshah/nimaz/domain/model/Announcement.kt`, add the import `import com.arshadshah.nimaz.core.navigation.Route` and change the subtype:

```kotlin
sealed interface AnnouncementAction {
    data class OpenUrl(val url: String) : AnnouncementAction
    data class NavigateToFeature(val routeKey: String, val route: Route) : AnnouncementAction
    data object None : AnnouncementAction
}
```

- [ ] **Step 4: Update the use case**

In `app/src/main/java/com/arshadshah/nimaz/domain/usecase/AnnouncementUseCases.kt`, add `import com.arshadshah.nimaz.core.navigation.Route` and replace the class:

```kotlin
class ResolveAnnouncementRouteUseCase(
    private val resolveFeatureKey: (String) -> Route?,
) {
    operator fun invoke(route: String?): AnnouncementAction {
        val value = route?.trim().orEmpty()
        return when {
            value.isEmpty() -> AnnouncementAction.None
            value.startsWith("https://") -> AnnouncementAction.OpenUrl(value)
            else -> resolveFeatureKey(value)
                ?.let { AnnouncementAction.NavigateToFeature(value, it) }
                ?: AnnouncementAction.None
        }
    }
}
```

- [ ] **Step 5: Update the DI binding**

In `app/src/main/java/com/arshadshah/nimaz/core/di/AnnouncementModule.kt`, change the wrapper to pass the resolver directly:

```kotlin
        resolveAnnouncementRoute = ResolveAnnouncementRouteUseCase(
            resolveFeatureKey = ::announcementRoute,
        ),
```

(The `import ...announcementRoute` already exists in this file.)

- [ ] **Step 6: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.arshadshah.nimaz.domain.usecase.AnnouncementUseCasesTest"` → PASS.
Then `./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL (confirms `HomeViewModel`'s `showCta` gate and any other `AnnouncementAction` consumer still compile — the added field is not destructured anywhere).

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/domain/model/Announcement.kt \
        app/src/main/java/com/arshadshah/nimaz/domain/usecase/AnnouncementUseCases.kt \
        app/src/main/java/com/arshadshah/nimaz/core/di/AnnouncementModule.kt \
        app/src/test/java/com/arshadshah/nimaz/domain/usecase/AnnouncementUseCasesTest.kt
git commit -m "refactor(nav): resolve announcement routes once"
```

---

## Task 3: `announcement_route_rejected` telemetry (§1.5)

**Files:**
- Modify: `app/src/main/java/com/arshadshah/nimaz/core/monitoring/AppAnalytics.kt`
- Modify: `app/src/main/java/com/arshadshah/nimaz/presentation/viewmodel/HomeViewModel.kt`

**Interfaces:**
- Consumes: `AppAnalytics.logEvent(name, vararg params)` (existing); the `showCta` computation in `HomeViewModel` that already calls `announcementUseCases.resolveAnnouncementRoute(active.route)` and has `active.id` + `active.route`.
- Produces: `AppAnalytics.logAnnouncementRouteRejected(id: String, route: String?)`.

**Design note:** the spec §1.5 said "fire from the use case." We fire from `HomeViewModel` instead, to keep the domain use case free of a dependency on `core/monitoring/AppAnalytics` (domain purity) — the ViewModel already holds both the announcement id and the resolution result, so this is the natural, clean site. Recorded in ARCHITECTURE §9 (Task 4).

- [ ] **Step 1: Add the analytics helper**

In `app/src/main/java/com/arshadshah/nimaz/core/monitoring/AppAnalytics.kt`, alongside the existing announcement helpers (`logAnnouncementShown`, `logAnnouncementCtaClicked`, `logAnnouncementDismissed`), add:

```kotlin
fun logAnnouncementRouteRejected(id: String, route: String?) {
    logEvent("announcement_route_rejected", "announcement_id" to id, "route" to route)
}
```

- [ ] **Step 2: Fire it from HomeViewModel**

In `app/src/main/java/com/arshadshah/nimaz/presentation/viewmodel/HomeViewModel.kt`, at the point where `showCta` is computed from the active announcement (the block that calls `announcementUseCases.resolveAnnouncementRoute(active.route)`), capture the resolution once and log a rejection when a non-empty route failed to resolve:

```kotlin
val routeAction = announcementUseCases.resolveAnnouncementRoute(active.route)
if (!active.route.isNullOrBlank() && routeAction == AnnouncementAction.None) {
    AppAnalytics.logAnnouncementRouteRejected(id = active.id, route = active.route)
}
// …existing state build…
showCta = active.ctaLabel != null && routeAction != AnnouncementAction.None,
```

Read the surrounding code first to place this correctly (the exact field is `active.route`, id is `active.id`; confirm names against the `Announcement` model). Reuse the single `routeAction` value for the `showCta` check rather than resolving twice. Ensure `AppAnalytics` and `AnnouncementAction` are imported.

**Guard against repeat logging:** this computation may run on every state emission. If the block runs more than once per announcement (e.g. inside a `combine`/`map` that re-emits), log the rejection only when the active announcement id changes — track the last-logged id in a private `var lastRejectedAnnouncementId: String? = null` on the ViewModel and skip if it equals `active.id`. Read the emission structure and apply this guard if the block is not already once-per-announcement.

- [ ] **Step 3: Compile + focused check**

Run: `./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL.
There is no existing unit test harness for `HomeViewModel` analytics side effects; do not fabricate one. Instead confirm by reading that: (a) the rejection fires only for non-empty routes that resolve to `None`; (b) `showCta` uses the same `routeAction`; (c) the repeat-log guard is present if the block re-emits. State this reasoning in your report.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/core/monitoring/AppAnalytics.kt \
        app/src/main/java/com/arshadshah/nimaz/presentation/viewmodel/HomeViewModel.kt
git commit -m "feat(analytics): announcement_route_rejected"
```

---

## Task 4: Docs

**Files:**
- Modify: `docs/NAVIGATION.md`, `docs/ARCHITECTURE.md` (§9), `docs/SUBSYSTEMS.md`

- [ ] **Step 1: Update NAVIGATION.md**

Add a subsection documenting the announcement route grammar: the static allowlist plus the parameterised keys table (from Task 1's grammar), noting integer range-checks and that malformed/out-of-range keys resolve to `null` (CTA hidden). If NAVIGATION.md has a mermaid map that changes, validate it.

- [ ] **Step 2: Update ARCHITECTURE.md §9**

Add a §9 row recording the deliberate domain→`core.navigation.Route` coupling introduced by `AnnouncementAction.NavigateToFeature(route: Route)` and `ResolveAnnouncementRouteUseCase(resolveFeatureKey: (String)->Route?)` — permitted (not a data-layer dependency), noted so the pattern is intentional. Add a second note that `announcement_route_rejected` is fired from `HomeViewModel` (presentation), not the use case, to keep the domain layer free of `AppAnalytics`.

- [ ] **Step 3: Update SUBSYSTEMS.md**

In the notifications/announcements section, document that announcement route keys now support a parameterised grammar resolved by `announcementRoute`, and the new `announcement_route_rejected` analytics event.

- [ ] **Step 4: Commit**

```bash
git add docs/NAVIGATION.md docs/ARCHITECTURE.md docs/SUBSYSTEMS.md
git commit -m "docs: parameterised announcement routing + resolve-once + rejection telemetry"
```

---

## Deferred (not in this plan)

- **Empty states for string-id destinations** (`DuaReader`, `HadithReader`, `HadithBook`, `HadithChapter`): spec §1.2 asks that these screens degrade gracefully on an unknown id. Routing produces the `Route` regardless; verifying/adding screen empty states is screen work, tracked for a follow-up. Flag in the final review whether any of these screens currently crash on a missing id.
- **Speculative static keys with no `Route`** (whichever of `hadith/search`, `dua/favorites`, `settings/appearance`, … don't exist per Task 1 Step 4): add the screen + key together in a later plan.

---

## Self-Review

**Spec coverage (spec §1):**
- §1.2 grammar table → Task 1 grammar. ✓ (parameterised arms for all confirmed routes; string ids accepted syntactically)
- §1.3 signature change (static then parameterised, range-checked ints) → Task 1. ✓
- §1.4 resolve-once (`(String)->Route?`, `NavigateToFeature` carries `Route`, DI update) → Task 2. ✓
- §1.5 `announcement_route_rejected` → Task 3 (fired from HomeViewModel, not the use case — deviation documented with rationale). ✓
- §1.6 tests (static, parameterised, boundary, malformed, slash-tolerance, non-shadowing, non-https) → Task 1 test suite. ✓

**Placeholder scan:** no TBD/TODO; grammar and tests are complete code; Task 1 Step 4 and Task 3's guard are explicit verify-and-adapt steps, not vague directives.

**Type consistency:** `announcementRoute`/`staticAnnouncementRoute`/`parameterisedAnnouncementRoute`, `RESERVED_HADITH_SEGMENTS`, `NavigateToFeature(routeKey, route)`, `ResolveAnnouncementRouteUseCase(resolveFeatureKey)`, `logAnnouncementRouteRejected(id, route)` are referenced identically across tasks. All `Route.*` types match the verified signatures in `Routes.kt`.
