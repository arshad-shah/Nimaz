package com.arshadshah.nimaz.presentation.screens.quran

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.QuranSearchQuery
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.presentation.screens.str
import com.arshadshah.nimaz.presentation.viewmodel.UiError
import com.arshadshah.nimaz.presentation.viewmodel.quran.QuranBrowseUiState
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Every surah in order, under juz headers — and the search box that is also a jump.
 *
 * The juz headers are the interesting part, because the state is a flat list and the screen
 * decides where a header goes by comparing adjacent rows' *opening* juz. Two consequences fall
 * out of that and both were bugs: filing each surah under the one juz it opens in made juz 2
 * disappear from the index entirely (no surah begins in it), and it made the row for Al-Baqarah
 * — which runs from juz 1 into juz 3 — claim to be juz 1.
 *
 * `QuranBrowseContent` is state-hoisted, so these drive it directly. The view model's own
 * behaviour is `QuranBrowseViewModelTest`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class QuranBrowseScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private var openedSurah: Int? = null
    private var openedJuz: Int? = null
    private var openedPage: Int? = null
    private var wentBack = false
    private val queries = mutableListOf<String>()
    private var cleared = false

    private fun render(state: QuranBrowseUiState) {
        composeRule.setThemedContent {
            QuranBrowseContent(
                state = state,
                onNavigateBack = { wentBack = true },
                onNavigateToSurah = { openedSurah = it },
                onNavigateToJuz = { openedJuz = it },
                onNavigateToPage = { openedPage = it },
                onQueryChange = { queries += it },
                onClearQuery = { cleared = true },
            )
        }
    }

    private fun surah(number: Int, name: String, ayahs: Int = 7, page: Int = number) = Surah(
        number = number,
        nameArabic = "سورة",
        nameEnglish = name,
        nameTransliteration = name,
        revelationType = RevelationType.MECCAN,
        ayahCount = ayahs,
        orderInMushaf = number,
        startPage = page,
    )

    private val listed = QuranBrowseUiState(
        isLoading = false,
        rows = listOf(
            surah(1, "The Opening", ayahs = 7, page = 1),
            surah(2, "The Cow", ayahs = 286, page = 2),
            surah(3, "The Family of Imran", ayahs = 200, page = 50),
        ),
        startPages = mapOf(1 to 1, 2 to 2, 3 to 50),
        juzSpans = mapOf(1 to 1..1, 2 to 1..3, 3 to 3..4),
    )

    // ---- The list ----

    @Test
    fun `the surahs are listed`() {
        render(listed)

        composeRule.onNodeWithText("The Opening").assertIsDisplayed()
        composeRule.onNodeWithText("The Cow").assertIsDisplayed()
    }

    @Test
    fun `opening a surah is the caller's job`() {
        render(listed)

        composeRule.onNodeWithText("The Cow").performClick()

        assertThat(openedSurah).isEqualTo(2)
    }

    @Test
    fun `a juz header is printed where the opening juz changes, and not otherwise`() {
        render(listed)

        // Al-Fatihah and Al-Baqarah both open in juz 1, so one header covers them; Al-Imran
        // opens in juz 3 and gets its own.
        composeRule.onNodeWithText(str(R.string.quran_home_juz_indicator, 1)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.quran_home_juz_indicator, 3)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.quran_home_juz_indicator, 2)).assertDoesNotExist()
    }

    // ---- Search, and jump ----

    @Test
    fun `typing hands the query up rather than filtering in place`() {
        render(listed)

        composeRule.onNodeWithText(str(R.string.quran_browse_search_hint)).performTextInput("cow")

        assertThat(queries).contains("cow")
    }

    @Test
    fun `a query naming a juz offers to open it`() {
        render(listed.copy(query = "juz 15", jumpTarget = QuranSearchQuery.Juz(15)))

        composeRule.onNodeWithText(str(R.string.quran_browse_open_juz, 15)).performClick()

        assertThat(openedJuz).isEqualTo(15)
    }

    @Test
    fun `a query naming a page offers to open it`() {
        render(listed.copy(query = "page 299", jumpTarget = QuranSearchQuery.Page(299)))

        composeRule.onNodeWithText(str(R.string.quran_browse_open_page, 299)).performClick()

        assertThat(openedPage).isEqualTo(299)
    }

    @Test
    fun `a query naming a surah number offers to open that surah`() {
        render(listed.copy(query = "18", jumpTarget = QuranSearchQuery.SurahNumber(18)))

        composeRule.onNodeWithText(str(R.string.quran_home_surah_fallback, 18)).performClick()

        assertThat(openedSurah).isEqualTo(18)
    }

    @Test
    fun `a name query that matches nothing says so`() {
        render(QuranBrowseUiState(isLoading = false, rows = emptyList(), query = "zzz"))

        composeRule.onNodeWithText(str(R.string.quran_browse_no_matches)).assertIsDisplayed()
    }

    @Test
    fun `a jump target is offered even when no row matches the words`() {
        // "juz 15" names a place, not a name, so an empty list is the right list — and the card
        // above it is the whole answer.
        render(
            QuranBrowseUiState(
                isLoading = false,
                rows = emptyList(),
                query = "juz 15",
                jumpTarget = QuranSearchQuery.Juz(15),
            )
        )

        composeRule.onNodeWithText(str(R.string.quran_browse_no_matches)).assertDoesNotExist()
        composeRule.onNodeWithText(str(R.string.quran_browse_open_juz, 15)).assertIsDisplayed()
    }

    // ---- Loading and failure ----

    @Test
    fun `a first load shows neither an empty list nor a failure`() {
        render(QuranBrowseUiState(isLoading = true))

        composeRule.onNodeWithText(str(R.string.quran_browse_no_matches)).assertDoesNotExist()
    }

    @Test
    fun `a failed load is reported instead of an empty index`() {
        render(
            QuranBrowseUiState(
                isLoading = false,
                rows = emptyList(),
                error = UiError(message = R.string.quran_browse_load_failed),
            )
        )

        composeRule.onNodeWithText(str(R.string.quran_browse_no_matches)).assertDoesNotExist()
        composeRule.onNodeWithText(str(R.string.quran_browse_load_failed)).assertIsDisplayed()
    }

    @Test
    fun `going back is the caller's business`() {
        render(listed)

        composeRule.onNodeWithContentDescription(str(R.string.cd_back)).performClick()

        assertThat(wentBack).isTrue()
    }
}
