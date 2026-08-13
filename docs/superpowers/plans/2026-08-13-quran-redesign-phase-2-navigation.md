# Qur'an Redesign — Phase 2: Navigation & Lists Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the Qur'an home's in-screen tabs with four real destinations, merge the surah/juz/page browse into one searchable list, consolidate saving into one app-wide screen, and demote surah info to a bottom sheet.

**Architecture:** Additive first, subtractive last. New routes and screens land and are wired while the old ones still work; only once every entry point is repointed are `QuranBookmarks` and `SurahInfo` retired, in a single task that also fixes the announcement routes and the docs. This ordering means the branch is never in a state where a documented deep link has no destination.

**Tech Stack:** Kotlin, Jetpack Compose, Hilt, type-safe Navigation Compose, Robolectric + Truth, Espresso for the instrumented navigation suite.

**Spec:** [`docs/superpowers/specs/2026-08-13-quran-redesign-design.md`](../specs/2026-08-13-quran-redesign-design.md) §4, §5.1–5.3, §5.5

**Depends on:** Phase 1 (`NimazSegmentedTabs` is used by Saved).

## Global Constraints

- **Type-safe navigation only.** Every destination is a `@Serializable` `Route` in `core/navigation/Routes.kt`, a `const val` in `core/navigation/ScreenTags.kt`, and a `taggedComposable<Route.X>(ScreenTags.X)` in `NavGraph` — **never a bare `composable`**, which leaves the screen untestable. (CLAUDE.md rule 6.)
- **ViewModels inject `XxxUseCases`, not repositories or DAOs.** (Rule 2.)
- **ViewModels expose `StateFlow<XxxUiState>` plus a single `onEvent(event: XxxEvent)`.** No exposed `MutableStateFlow`/`LiveData`. (Rule 3.)
- **Presentation never imports Room entities or DAOs.** (Rule 1.)
- **No hardcoded `Color(0xFF…)` in screens.** (Rule 7.)
- **A whole-card tap target is `NimazCard(onClick = …)`**, never a `Modifier.clickable` wrapped around the card. (Rule 8.)
- **`ScreenTags.QuranSurahList` must survive.** `app/src/androidTest/.../behavior/QuranOpenSurahTest.kt:33` scrolls by that tag; dropping it turns the instrumented suite red while all four local gates stay green.
- This phase **must** run `./gradlew :app:assembleDebugAndroidTest` — it changes routes and `ScreenTags`.

---

## File Structure

| File | Responsibility |
|------|----------------|
| `core/navigation/Routes.kt` (modify) | Add `QuranBrowse`, `QuranSaved`; later remove `QuranBookmarks`, `SurahInfo` |
| `core/navigation/ScreenTags.kt` (modify) | Matching tags; keep `QuranSurahList` |
| `core/navigation/NavGraph.kt` (modify) | Wire the new destinations with `taggedComposable` |
| `presentation/screens/quran/QuranBrowseScreen.kt` (create) | The merged, searchable surah list with juz headers |
| `presentation/viewmodel/quran/QuranBrowseViewModel.kt` + `…UiState.kt` + `…Event.kt` (create) | Browse state: query, parsed jump target, grouped rows |
| `presentation/screens/quran/QuranSavedScreen.kt` (create) | App-wide saved items with kind + content filters |
| `presentation/viewmodel/quran/QuranSavedViewModel.kt` + state/event (create) | Saved state |
| `presentation/components/molecules/SurahInfoSheet.kt` (create) | Surah info as a bottom sheet, carrying counts and summary |
| `presentation/components/molecules/QuranSurahListItem.kt` (modify) | Compress to the ~64px row |
| `presentation/screens/quran/QuranHomeScreen.kt` (modify) | Remove tabs; four destination rows; keep Recommended, add Recently saved |
| `domain/model/QuranSearchQuery.kt` (create) | Parsing `juz 15` / `page 299` / a number / a name — pure, unit-testable |

The query parser is deliberately a **domain** type rather than logic inside the screen: it is the one piece of Browse with real branching, and putting it in `domain/model` makes it testable without Compose.

---

### Task 1: The browse query parser

**Files:**
- Create: `app/src/main/java/com/arshadshah/nimaz/domain/model/QuranSearchQuery.kt`
- Test: `app/src/test/java/com/arshadshah/nimaz/domain/model/QuranSearchQueryTest.kt`

**Interfaces:**
- Consumes: nothing.
- Produces:

```kotlin
sealed interface QuranSearchQuery {
    data object Empty : QuranSearchQuery
    data class Juz(val number: Int) : QuranSearchQuery
    data class Page(val number: Int) : QuranSearchQuery
    data class SurahNumber(val number: Int) : QuranSearchQuery
    data class Name(val text: String) : QuranSearchQuery

    companion object {
        fun parse(raw: String): QuranSearchQuery
    }
}
```

  Task 2's ViewModel consumes `QuranSearchQuery.parse`.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/arshadshah/nimaz/domain/model/QuranSearchQueryTest.kt`:

```kotlin
package com.arshadshah.nimaz.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class QuranSearchQueryTest {

    @Test
    fun `blank input is empty`() {
        assertThat(QuranSearchQuery.parse("")).isEqualTo(QuranSearchQuery.Empty)
        assertThat(QuranSearchQuery.parse("   ")).isEqualTo(QuranSearchQuery.Empty)
    }

    @Test
    fun `juz is recognised in its long and short forms`() {
        listOf("juz 15", "Juz15", "para 15", "j 15", "  JUZ  15 ").forEach { raw ->
            assertThat(QuranSearchQuery.parse(raw)).isEqualTo(QuranSearchQuery.Juz(15))
        }
    }

    @Test
    fun `page is recognised in its long and short forms`() {
        listOf("page 299", "Page299", "pg 299", "p 299").forEach { raw ->
            assertThat(QuranSearchQuery.parse(raw)).isEqualTo(QuranSearchQuery.Page(299))
        }
    }

    @Test
    fun `a bare number is a surah number`() {
        assertThat(QuranSearchQuery.parse("18")).isEqualTo(QuranSearchQuery.SurahNumber(18))
    }

    @Test
    fun `out-of-range juz falls back to a name search`() {
        assertThat(QuranSearchQuery.parse("juz 31")).isEqualTo(QuranSearchQuery.Name("juz 31"))
        assertThat(QuranSearchQuery.parse("juz 0")).isEqualTo(QuranSearchQuery.Name("juz 0"))
    }

    @Test
    fun `out-of-range page falls back to a name search`() {
        assertThat(QuranSearchQuery.parse("page 9999")).isEqualTo(QuranSearchQuery.Name("page 9999"))
    }

    @Test
    fun `out-of-range surah number falls back to a name search`() {
        assertThat(QuranSearchQuery.parse("115")).isEqualTo(QuranSearchQuery.Name("115"))
        assertThat(QuranSearchQuery.parse("0")).isEqualTo(QuranSearchQuery.Name("0"))
    }

    @Test
    fun `anything else is a name search, trimmed and lowercased`() {
        assertThat(QuranSearchQuery.parse("  Al-Kahf ")).isEqualTo(QuranSearchQuery.Name("al-kahf"))
    }

    @Test
    fun `a name that merely starts with p or j is not a page or juz`() {
        assertThat(QuranSearchQuery.parse("patience")).isEqualTo(QuranSearchQuery.Name("patience"))
        assertThat(QuranSearchQuery.parse("Jonah")).isEqualTo(QuranSearchQuery.Name("jonah"))
    }
}
```

That last test is the reason for the parser: `j 15` must mean juz 15 while `Jonah` must not.

- [ ] **Step 2: Run the test to verify it fails**

```bash
./gradlew :app:testDebugUnitTest --tests "*QuranSearchQueryTest*"
```

Expected: FAIL to compile — `Unresolved reference: QuranSearchQuery`.

- [ ] **Step 3: Implement the parser**

Create `app/src/main/java/com/arshadshah/nimaz/domain/model/QuranSearchQuery.kt`:

```kotlin
package com.arshadshah.nimaz.domain.model

/**
 * What the Browse search field was asking for.
 *
 * One field serves four questions — a surah name, a surah number, a juz and a
 * mushaf page — because they are all ways of naming a place, and making the
 * reader pick the right tab first is the thing this redesign removes.
 *
 * Out-of-range numbers fall back to [Name] rather than erroring: "juz 31" is
 * far more likely to be someone typing than someone expecting juz 31 to exist,
 * and a name search shows them that nothing matches.
 */
sealed interface QuranSearchQuery {

    data object Empty : QuranSearchQuery
    data class Juz(val number: Int) : QuranSearchQuery
    data class Page(val number: Int) : QuranSearchQuery
    data class SurahNumber(val number: Int) : QuranSearchQuery
    data class Name(val text: String) : QuranSearchQuery

    companion object {
        private const val SURAH_COUNT = 114
        private const val JUZ_COUNT = 30

        /**
         * The largest page count of any shipped mushaf edition. Validated
         * against the largest so a query resolves whichever script is active;
         * the reader then clamps to that script's real count.
         */
        private const val MAX_PAGE = 847

        private val JUZ = Regex("""^(?:juz|para|j)\s*(\d{1,2})$""")
        private val PAGE = Regex("""^(?:page|pg|p)\s*(\d{1,3})$""")
        private val NUMBER = Regex("""^\d{1,3}$""")

        fun parse(raw: String): QuranSearchQuery {
            val text = raw.trim().lowercase()
            if (text.isEmpty()) return Empty

            JUZ.find(text)?.let { match ->
                val n = match.groupValues[1].toInt()
                if (n in 1..JUZ_COUNT) return Juz(n)
                return Name(text)
            }
            PAGE.find(text)?.let { match ->
                val n = match.groupValues[1].toInt()
                if (n in 1..MAX_PAGE) return Page(n)
                return Name(text)
            }
            if (NUMBER.matches(text)) {
                val n = text.toInt()
                return if (n in 1..SURAH_COUNT) SurahNumber(n) else Name(text)
            }
            return Name(text)
        }
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

```bash
./gradlew :app:testDebugUnitTest --tests "*QuranSearchQueryTest*"
```

Expected: PASS, 9 tests.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/domain/model/QuranSearchQuery.kt \
        app/src/test/java/com/arshadshah/nimaz/domain/model/QuranSearchQueryTest.kt
git commit -m "feat(domain): one browse query covering name, number, juz and page

Browse stops asking the reader to choose a tab before typing, so the single
field has to tell 'j 15' from 'Jonah'. Out-of-range numbers fall back to a
name search rather than erroring."
```

---

### Task 2: The Browse route, screen and ViewModel

**Files:**
- Modify: `app/src/main/java/com/arshadshah/nimaz/core/navigation/Routes.kt`
- Modify: `app/src/main/java/com/arshadshah/nimaz/core/navigation/ScreenTags.kt`
- Modify: `app/src/main/java/com/arshadshah/nimaz/core/navigation/NavGraph.kt`
- Create: `app/src/main/java/com/arshadshah/nimaz/presentation/viewmodel/quran/QuranBrowseUiState.kt`
- Create: `app/src/main/java/com/arshadshah/nimaz/presentation/viewmodel/quran/QuranBrowseEvent.kt`
- Create: `app/src/main/java/com/arshadshah/nimaz/presentation/viewmodel/quran/QuranBrowseViewModel.kt`
- Create: `app/src/main/java/com/arshadshah/nimaz/presentation/screens/quran/QuranBrowseScreen.kt`
- Test: `app/src/test/java/com/arshadshah/nimaz/presentation/viewmodel/quran/QuranBrowseViewModelTest.kt`

**Interfaces:**
- Consumes: `QuranSearchQuery.parse` (Task 1).
- Produces:

```kotlin
// Routes.kt
data object QuranBrowse : Route

// ScreenTags.kt
const val QuranBrowse = "screen_quran_browse"

// QuranBrowseUiState.kt
data class QuranBrowseRow(
    val surahNumber: Int,
    val englishName: String,
    val arabicName: String,
    val isMeccan: Boolean,
    val ayahCount: Int,
    val startPage: Int,
    val juzNumber: Int,
)

data class QuranBrowseUiState(
    val query: String = "",
    val isLoading: Boolean = true,
    val rows: List<QuranBrowseRow> = emptyList(),
    val jumpTarget: QuranSearchQuery? = null,
)

// QuranBrowseEvent.kt
sealed interface QuranBrowseEvent {
    data class QueryChanged(val text: String) : QuranBrowseEvent
    data object ClearQuery : QuranBrowseEvent
}
```

  Task 3 (home) navigates to `Route.QuranBrowse`. Task 5 (surah info sheet) is raised from this screen.

**Notes.** `rows` is a flat, already-filtered list in juz order; the screen inserts a juz header whenever `juzNumber` changes between adjacent rows. Keeping grouping in the screen avoids a nested list type and keeps the state trivially assertable.

- [ ] **Step 1: Read the existing Quran ViewModel to match conventions**

```bash
sed -n '1,120p' app/src/main/java/com/arshadshah/nimaz/presentation/viewmodel/quran/QuranViewModel.kt
```

Note how it injects use cases, exposes `StateFlow`, and handles `onEvent`. Match it. Find which use case already returns the surah list:

```bash
grep -rn "getAllSurahs\|GetSurahs\|surahs" app/src/main/java/com/arshadshah/nimaz/domain/usecase/quran/ | head
```

Use the existing one; do **not** add a repository call to the ViewModel (rule 2).

- [ ] **Step 2: Write the failing ViewModel test**

Create `app/src/test/java/com/arshadshah/nimaz/presentation/viewmodel/quran/QuranBrowseViewModelTest.kt`. Follow the fake/use-case-double style of the neighbouring ViewModel tests:

```bash
ls app/src/test/java/com/arshadshah/nimaz/presentation/viewmodel/quran/
```

The suite must cover:

```kotlin
@Test fun `state starts loading and then lists every surah in juz order`()
@Test fun `a name query filters by english name and meaning`()
@Test fun `a juz query filters to that juz and sets the jump target`()
@Test fun `a page query sets a page jump target`()
@Test fun `a surah number query sets a surah jump target`()
@Test fun `clearing the query restores the full list and clears the jump target`()
@Test fun `a query matching nothing yields an empty list and no jump target`()
```

Write real assertions with real expected values — no `TODO()`.

- [ ] **Step 3: Run the test to verify it fails**

```bash
./gradlew :app:testDebugUnitTest --tests "*QuranBrowseViewModelTest*"
```

Expected: FAIL to compile.

- [ ] **Step 4: Add the route and tag**

In `Routes.kt`, beside the other Qur'an routes (around line 33):

```kotlin
/**
 * The merged browse surface: every surah in juz order, under juz headers, with
 * one field that also understands "juz 15" and "page 299".
 *
 * Replaces the Surah/Juz/Page sub-tabs that used to live inside [Quran]. Page
 * stops being a browse tab here because it was never really an index — it was
 * the door to a different *reading mode*, which now lives in the reader.
 */
@Serializable
data object QuranBrowse : Route
```

In `ScreenTags.kt`, beside the other Qur'an tags:

```kotlin
const val QuranBrowse = "screen_quran_browse"
```

Leave `QuranSurahList` exactly as it is — `QuranOpenSurahTest` scrolls by it.

- [ ] **Step 5: Implement state, event, ViewModel and screen**

Write the three ViewModel files against the interfaces above, then the screen. The screen must:

- Apply `Modifier.testTag(ScreenTags.QuranSurahList)` to the scrollable list, so `QuranOpenSurahTest` keeps working.
- Insert a juz header row whenever `juzNumber` differs from the previous row's.
- Render each surah with `QuranSurahListItem` (compressed in Task 6).
- Show the jump-to card above the list when `jumpTarget != null`.
- Raise the surah info sheet from the row's info affordance (Task 5).

- [ ] **Step 6: Wire the destination**

In `NavGraph.kt`, beside the other Qur'an destinations:

```kotlin
taggedComposable<Route.QuranBrowse>(ScreenTags.QuranBrowse) {
    QuranBrowseScreen(
        onNavigateBack = { navController.popBackStack() },
        onNavigateToSurah = { surah -> navController.navigate(Route.QuranReader(surah)) },
        onNavigateToJuz = { juz -> navController.navigate(Route.QuranJuz(juz)) },
        onNavigateToPage = { page -> navController.navigate(Route.QuranPage(page)) },
    )
}
```

Never a bare `composable` (rule 6).

- [ ] **Step 7: Run the tests to verify they pass**

```bash
./gradlew :app:testDebugUnitTest --tests "*QuranBrowseViewModelTest*"
./gradlew :app:compileDebugKotlin
```

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/arshadshah/nimaz/core/navigation/Routes.kt \
        app/src/main/java/com/arshadshah/nimaz/core/navigation/ScreenTags.kt \
        app/src/main/java/com/arshadshah/nimaz/core/navigation/NavGraph.kt \
        app/src/main/java/com/arshadshah/nimaz/presentation/viewmodel/quran/QuranBrowse*.kt \
        app/src/main/java/com/arshadshah/nimaz/presentation/screens/quran/QuranBrowseScreen.kt \
        app/src/test/java/com/arshadshah/nimaz/presentation/viewmodel/quran/QuranBrowseViewModelTest.kt
git commit -m "feat(quran): Browse becomes a destination, not a tab

Surah, juz and page were three tabs answering one question - where is this.
They become one list in juz order with a field that understands all three.
The surah list keeps its QuranSurahList tag so the behaviour suite still
scrolls to a surah by name."
```

---

### Task 3: Qur'an home — four destinations, no tabs

**Files:**
- Modify: `app/src/main/java/com/arshadshah/nimaz/presentation/screens/quran/QuranHomeScreen.kt`
- Modify: `app/src/main/java/com/arshadshah/nimaz/core/navigation/NavGraph.kt`
- Test: `app/src/testDebug/java/com/arshadshah/nimaz/presentation/screens/quran/QuranHomeScreenTest.kt` (create)

**Interfaces:**
- Consumes: `Route.QuranBrowse` (Task 2), `Route.QuranSaved` (Task 4).
- Produces: nothing later tasks depend on.

- [ ] **Step 1: Write the failing screen test**

Assert the four rows exist, the tab row does not, and each row invokes the right navigation callback. Use `createComponentComposeRule()` + `setThemedContent {}`.

```kotlin
@Test fun `home shows four destination rows`()
@Test fun `home no longer shows the browse and favorites tabs`()
@Test fun `tapping browse navigates to browse`()
@Test fun `tapping saved navigates to saved`()
@Test fun `tapping themes navigates to topics`()
@Test fun `tapping khatam navigates to the khatam list`()
@Test fun `the continue-reading card is a single tap target`()
```

- [ ] **Step 2: Run to verify it fails**

```bash
./gradlew :app:testDebugUnitTest --tests "*QuranHomeScreenTest*"
```

- [ ] **Step 3: Remove the tab row and its two tab bodies**

`QuranHomeScreen.kt` currently holds a sticky `TabRow` (search for the comment `Sticky TabRow outside LazyColumn - only Surah/Juz/Page`, around line 543) plus the browse and favourites bodies. Delete the tab row, the `Surah/Juz/Page` sub-tabs, and the browse/favourites branches. The juz and page grids move to the reader in phase 3 — **do not delete `QuranJuzGrid` / `QuranPageGrid` composables**, only their use here.

- [ ] **Step 4: Add the four-row card**

One `NimazCard` containing four `NimazMenuItem` rows: Browse, Saved (badge = saved count), Themes, Khatam (badge = percent). Each row navigates. Use `NimazMenuItem`, not a `Modifier.clickable` row (rule 8).

- [ ] **Step 5: Make the hero one tap target**

The continue-reading card currently has both a card click and a separate Resume button doing the same thing. Keep the card's `onClick` and drop the button.

- [ ] **Step 6: Keep both strips**

Keep the Recommended strip. Keep the bookmarks strip but retitle it "Recently saved" and point "See all" at `Route.QuranSaved`.

- [ ] **Step 7: Run tests and commit**

```bash
./gradlew :app:testDebugUnitTest --tests "*QuranHomeScreenTest*"
git add app/src/main/java/com/arshadshah/nimaz/presentation/screens/quran/QuranHomeScreen.kt \
        app/src/main/java/com/arshadshah/nimaz/core/navigation/NavGraph.kt \
        app/src/testDebug/java/com/arshadshah/nimaz/presentation/screens/quran/QuranHomeScreenTest.kt
git commit -m "feat(quran): home's tabs become four destinations

Four rows of chrome preceded any content: an app bar, a tab row, a sub-tab
row and a search field. Browse and Saved become real screens with their own
back arrows, and the hero stops offering the same action twice."
```

---

### Task 4: Saved — one app-wide screen

**Files:**
- Modify: `core/navigation/Routes.kt`, `ScreenTags.kt`, `NavGraph.kt`
- Create: `presentation/viewmodel/quran/QuranSavedUiState.kt`, `QuranSavedEvent.kt`, `QuranSavedViewModel.kt`
- Create: `presentation/screens/quran/QuranSavedScreen.kt`
- Test: `app/src/test/java/.../QuranSavedViewModelTest.kt`

**Interfaces:**
- Consumes: `NimazSegmentedTabs` (Phase 1).
- Produces:

```kotlin
data object QuranSaved : Route
const val QuranSaved = "screen_quran_saved"

enum class SavedKind { BOOKMARK, FAVOURITE, NOTE }
enum class SavedCorpus { QURAN, HADITH, DUA }

data class SavedItem(
    val id: Long,
    val kind: SavedKind,
    val corpus: SavedCorpus,
    val reference: String,
    val excerpt: String?,
)

data class QuranSavedUiState(
    val isLoading: Boolean = true,
    val kindFilter: SavedKind? = null,
    val corpusFilter: SavedCorpus? = null,
    val items: List<SavedItem> = emptyList(),
)
```

**Notes.** This screen is **app-wide**, not Qur'an-scoped — the underlying bookmark store already spans Qur'an, Hadith and Dua, and scoping it down would strand a user's existing hadith and dua bookmarks. It absorbs the current `QuranBookmarks` screen entirely.

`SavedItem.id` must be a genuinely unique id. **Do not key any list by `QuranBookmark.id`** — see the crash in the spec §10; that field is hardcoded to `0`. If the underlying crash is not yet fixed on `dev`, key by `reference` here and note it.

- [ ] **Step 1: Write the failing ViewModel test** covering: both filters independently, both together, clearing, and empty states per filter.
- [ ] **Step 2: Run to verify it fails.**
- [ ] **Step 3: Add route + tag.**
- [ ] **Step 4: Implement state/event/ViewModel.**
- [ ] **Step 5: Implement the screen** — two `NimazSegmentedTabs` rows (kind, corpus), rows with a coloured spine, reference, excerpt and a **small coloured kind chip** (not an uppercase word).
- [ ] **Step 6: Wire with `taggedComposable`.**
- [ ] **Step 7: Run tests, commit.**

```bash
git commit -m "feat(quran): one Saved screen for bookmarks, favourites and notes

Saving was split three ways - a Favorites tab, an app-wide Bookmarks screen
behind an app-bar icon, and nowhere for notes. The bookmark store was always
app-wide, so Saved is too; scoping it to the Quran would have stranded hadith
and dua bookmarks."
```

---

### Task 5: Surah info as a bottom sheet

**Files:**
- Create: `presentation/components/molecules/SurahInfoSheet.kt`
- Modify: `presentation/screens/quran/QuranBrowseScreen.kt`, `QuranReaderScreen.kt`
- Test: `app/src/testDebug/java/.../molecules/SurahInfoSheetTest.kt`

**Interfaces:**
- Consumes: the existing `SurahInfoScreen`'s state source — reuse its ViewModel rather than writing a new one.
- Produces:

```kotlin
@Composable
fun SurahInfoSheet(
    surahNumber: Int,
    onDismiss: () -> Unit,
    onReadSurah: (Int) -> Unit,
    onListen: (Int) -> Unit,
    onOpenBackground: (Int) -> Unit,
    onOpenPassages: (Int) -> Unit,
    onOpenSubjects: (Int) -> Unit,
)
```

**It must carry over what the screen does better than the prototype's sheet:**

- The **summary paragraph**.
- **Counted** onward rows — "Background · 3 sections", "Passages · 1 across 7 verses", "Subjects · 14, most-cited first". A bare "Subjects" does not tell a reader whether to tap.
- Four fact tiles **including "Revealed in"**, which the current screen lacks.
- **One primary action** — "Read surah". Listen is secondary; today's yellow-beside-teal pair puts two accents in one row and yellow is being retired as an accent (spec §6.5).

- [ ] **Step 1–7:** failing test → route the sheet from Browse and the reader → implement → verify → commit.

---

### Task 6: Compress the surah row

**Files:**
- Modify: `presentation/components/molecules/QuranSurahListItem.kt`
- Test: `app/src/testDebug/java/.../molecules/QuranSurahListItemTest.kt`

Target ~64 px: a flat number chip, the name, and one meta line (place · ayahs · page). **Keep the page information** — folding the Page tab must not cost the reader the ability to see where a surah sits. The place indicator stays a chip, with a deliberate Meccan/Medinan pair replacing the current purple/teal.

Open from the spec, to settle here with a screenshot: page *range* vs start page, and whether the ruku count survives.

---

### Task 7: Retire the old routes and fix the docs

**Do this task last.** Until now, both the old and new surfaces exist.

**Files:**
- Modify: `Routes.kt`, `ScreenTags.kt`, `NavGraph.kt`
- Delete: `presentation/screens/quran/SurahInfoScreen.kt`, the bookmarks screen
- Modify: `core/navigation/AnnouncementRoutes.kt` (or wherever `quran/bookmarks` resolves)
- Modify: `docs/NAVIGATION.md`

- [ ] **Step 1: Find every reference**

```bash
grep -rn "SurahInfo\|QuranBookmarks" app/src/main app/src/test app/src/testDebug app/src/androidTest docs/
```

- [ ] **Step 2: Repoint the two announcement routes**

`docs/NAVIGATION.md` §4 documents `quran/surah/{n}/info` → `SurahInfo` and `quran/bookmarks` → `QuranBookmarks`. Both lose their destination. Repoint:

- `quran/bookmarks` → `Route.QuranSaved`.
- `quran/surah/{n}/info` → `Route.QuranReader(n)`, which raises the sheet. If the reader cannot raise the sheet from a deep link without extra state, route it to `Route.QuranBrowse` instead and record the choice in `docs/NAVIGATION.md`.

An undocumented route **and** a documented route that no longer exists both fail `check_docs.py`.

- [ ] **Step 3: Delete the routes, tags and screens.**

- [ ] **Step 4: Update `docs/NAVIGATION.md`**

§3.2 route table (add `QuranBrowse`, `QuranSaved`; remove `QuranBookmarks`, `SurahInfo`; restate `QuranPage`/`QuranJuz` as reader entry points), the §2 mermaid map, §4 announcement keys, and the destination count.

- [ ] **Step 5: Validate the diagram**

```bash
npm install --no-save mermaid jsdom && node scripts/check_mermaid.mjs
```

- [ ] **Step 6: Run every gate, including the instrumented build**

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
python3 scripts/check_docs.py
./gradlew :app:assembleDebugAndroidTest
```

The last one is **not optional in this phase**. `FeatureNavigationTest` names `ScreenTags` constants directly, so removing two of them leaves the instrumented source set broken while all four other gates stay green — a branch that goes out clean locally and red on the emulator.

- [ ] **Step 7: Commit**

```bash
git commit -m "refactor(quran): retire SurahInfo and QuranBookmarks

Both are now reached differently - info as a sheet, bookmarks inside Saved -
so the routes go, along with the two announcement keys that pointed at them.
Repointed rather than dropped, because an announcement targeting a route that
no longer exists is a dead link on a user's device."
```

---

## Phase exit criteria

- [ ] Qur'an home has four destination rows and no tab row.
- [ ] `QuranBrowse` and `QuranSaved` exist, tagged and `taggedComposable`-wired.
- [ ] `QuranBookmarks` and `SurahInfo` are gone, and their announcement keys resolve elsewhere.
- [ ] `ScreenTags.QuranSurahList` still exists and `QuranOpenSurahTest` still passes.
- [ ] `docs/NAVIGATION.md` updated; `check_docs.py` and `check_mermaid.mjs` both pass.
- [ ] `assembleDebugAndroidTest` builds.
