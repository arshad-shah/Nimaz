package com.arshadshah.nimaz.presentation.screens.quran

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.Khatam
import com.arshadshah.nimaz.domain.model.QuranBookmark
import com.arshadshah.nimaz.domain.model.ReadingProgress
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.presentation.screens.str
import com.arshadshah.nimaz.presentation.viewmodel.quran.QuranBookmarksUiState
import com.arshadshah.nimaz.presentation.viewmodel.quran.QuranHomeUiState
import com.arshadshah.nimaz.presentation.viewmodel.quran.QuranViewModel
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The Qur'an section's front door: where you were, where you can go, and something to read.
 *
 * Two of its decisions are conditional on data the screen cannot assume it has, and both fail
 * quietly rather than loudly when they are wrong. The resume card and the start-reading hero are
 * mutually exclusive — exactly one carries the teal gradient, and showing both, or neither, is a
 * layout nobody designed. And the Subjects row is drawn only where the install's artifact
 * actually carries the thematic layer: between the migration that creates those tables and the
 * release that fills them, a row promising 2,512 subjects opens onto an explanation.
 */
@RunWith(RobolectricTestRunner::class)
// A tall window, so a screen's whole scrolling content composes and the test can reach a row
// without first driving the list to it. The default Robolectric display is a phone, and on one
// a LazyColumn composes about a screenful — which turns "does this row open that screen" into a
// test about scroll offsets.
@Config(qualifiers = "w411dp-h2200dp")
class QuranHomeScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val homeState = MutableStateFlow(QuranHomeUiState(isLoading = false))
    private val bookmarksState = MutableStateFlow(QuranBookmarksUiState())

    private val viewModel: QuranViewModel = mockk(relaxed = true) {
        every { this@mockk.homeState } returns this@QuranHomeScreenTest.homeState
        every { this@mockk.bookmarksState } returns this@QuranHomeScreenTest.bookmarksState
    }

    private var navigatedToSurah: Int? = null
    private var navigatedToAyah: Pair<Int, Int>? = null
    private var wentToBrowse = false
    private var wentToSaved = false
    private var wentToTopics = false
    private var wentToKhatam = false

    private fun render() {
        composeRule.setThemedContent {
            QuranHomeScreen(
                onNavigateToSurah = { navigatedToSurah = it },
                onNavigateToBrowse = { wentToBrowse = true },
                onNavigateToSaved = { wentToSaved = true },
                onNavigateToTopics = { wentToTopics = true },
                onNavigateToQuranAyah = { s, a -> navigatedToAyah = s to a },
                onNavigateToKhatam = { wentToKhatam = true },
                viewModel = viewModel,
            )
        }
    }

    private fun surah(number: Int, name: String) = Surah(
        number = number,
        nameArabic = "سورة",
        nameEnglish = name,
        nameTransliteration = name,
        revelationType = RevelationType.MECCAN,
        ayahCount = 7,
        orderInMushaf = number,
        startPage = number,
    )

    private val surahs = listOf(
        surah(1, "The Opening"),
        surah(18, "The Cave"),
        surah(36, "Ya-Sin"),
        surah(67, "The Sovereignty"),
    )

    // ---- Loading ----

    @Test
    fun `nothing but the title is offered while the surahs are still loading`() {
        homeState.value = QuranHomeUiState(isLoading = true)

        render()

        composeRule.onNodeWithText(str(R.string.quran_home_recommended)).assertDoesNotExist()
    }

    // ---- Resume, or start ----

    @Test
    fun `a reader who has read before is offered where they left off`() {
        homeState.value = QuranHomeUiState(
            isLoading = false,
            surahs = surahs,
            readingProgress = ReadingProgress(
                lastReadSurah = 18,
                lastReadAyah = 10,
                lastReadPage = 293,
                lastReadJuz = 15,
                totalAyahsRead = 400,
                currentKhatmaCount = 0,
                updatedAt = 0,
            ),
        )

        render()

        composeRule.onNodeWithText(str(R.string.quran_home_continue_reading)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.quran_home_verse_format, 10)).assertIsDisplayed()
    }

    @Test
    fun `resuming opens the verse it stopped on, not the surah's first`() {
        homeState.value = QuranHomeUiState(
            isLoading = false,
            surahs = surahs,
            readingProgress = ReadingProgress(
                lastReadSurah = 18,
                lastReadAyah = 10,
                lastReadPage = 293,
                lastReadJuz = 15,
                totalAyahsRead = 400,
                currentKhatmaCount = 0,
                updatedAt = 0,
            ),
        )
        render()

        composeRule.onNodeWithText(str(R.string.quran_home_continue_reading)).performClick()

        assertThat(navigatedToAyah).isEqualTo(18 to 10)
    }

    @Test
    fun `a reader who has never read is offered a place to start`() {
        homeState.value = QuranHomeUiState(isLoading = false, surahs = surahs)

        render()

        // Exactly one card on the screen carries the hero treatment; with no progress it is
        // this one, and tapping it opens Al-Fatihah.
        composeRule.onNodeWithText(str(R.string.quran_home_start_reading)).performClick()

        assertThat(navigatedToSurah).isEqualTo(1)
    }

    @Test
    fun `the start hero is inert until the surahs have arrived`() {
        homeState.value = QuranHomeUiState(isLoading = false, surahs = emptyList())

        render()
        composeRule.onNodeWithText(str(R.string.quran_home_start_reading)).performClick()

        assertThat(navigatedToSurah).isNull()
    }

    // ---- The four destinations ----

    @Test
    fun `every destination pushes its own screen`() {
        homeState.value = QuranHomeUiState(
            isLoading = false,
            surahs = surahs,
            hasThematicContent = true,
        )
        render()

        composeRule.onNodeWithText(str(R.string.quran_home_tab_browse)).performClick()
        composeRule.onNodeWithText(str(R.string.saved)).performClick()
        composeRule.onNodeWithText(str(R.string.quran_home_browse_subjects)).performClick()
        composeRule.onNodeWithText(str(R.string.khatam)).performClick()

        assertThat(listOf(wentToBrowse, wentToSaved, wentToTopics, wentToKhatam))
            .containsExactly(true, true, true, true)
    }

    @Test
    fun `subjects is offered only where the artifact carries them`() {
        // Between the migration that creates the thematic tables and the release that fills
        // them the tables exist and are empty, and a row promising 2,512 subjects would open
        // onto an explanation.
        homeState.value = QuranHomeUiState(
            isLoading = false,
            surahs = surahs,
            hasThematicContent = false,
        )

        render()

        composeRule.onNodeWithText(str(R.string.quran_home_browse_subjects)).assertDoesNotExist()
    }

    @Test
    fun `a destination that has a count says so`() {
        homeState.value = QuranHomeUiState(isLoading = false, surahs = surahs)
        bookmarksState.value = QuranBookmarksUiState(
            bookmarks = listOf(bookmark(1, surah = 2, ayah = 255)),
        )

        render()

        // "Saved · 12" is the difference between a menu and a dashboard: a row that only names
        // a place cannot say whether it is worth opening.
        composeRule.onNodeWithText("1").assertIsDisplayed()
    }

    @Test
    fun `an active khatam shows how far through it is`() {
        homeState.value = QuranHomeUiState(
            isLoading = false,
            surahs = surahs,
            activeKhatam = Khatam(
                id = 1,
                name = "Ramadan",
                status = com.arshadshah.nimaz.domain.model.KhatamStatus.ACTIVE,
                isActive = true,
                dailyTarget = 20,
                totalAyahsRead = 3118,
                createdAt = 0,
                updatedAt = 0,
            ),
        )

        render()

        composeRule.onNodeWithText("50%").assertIsDisplayed()
    }

    // ---- The rest of the page ----

    @Test
    fun `recommended surahs are offered to a reader with nothing saved`() {
        homeState.value = QuranHomeUiState(isLoading = false, surahs = surahs)

        render()

        // The only thing on this screen that works before anyone has saved anything.
        composeRule.onNodeWithText(str(R.string.quran_home_recommended)).assertIsDisplayed()
    }

    @Test
    fun `the recently-saved strip appears only once something is saved`() {
        homeState.value = QuranHomeUiState(isLoading = false, surahs = surahs)

        render()

        composeRule.onNodeWithText(str(R.string.quran_home_recently_saved)).assertDoesNotExist()
    }

    @Test
    fun `see all opens the saved screen`() {
        homeState.value = QuranHomeUiState(isLoading = false, surahs = surahs)
        bookmarksState.value = QuranBookmarksUiState(
            bookmarks = listOf(bookmark(1, surah = 18, ayah = 10)),
        )
        render()

        composeRule.onNodeWithText(str(R.string.quran_home_see_all)).performClick()

        assertThat(wentToSaved).isTrue()
    }

    @Test
    fun `the verse of the day opens the verse it quotes`() {
        homeState.value = QuranHomeUiState(
            isLoading = false,
            surahs = surahs,
            verseOfTheDay = Ayah(
                id = 262,
                surahNumber = 18,
                ayahNumber = 10,
                textArabic = "نص الآية",
                textSimple = "nass",
                juzNumber = 15,
                hizbNumber = 29,
                rubNumber = 0,
                pageNumber = 293,
                sajdaType = null,
                sajdaNumber = null,
                translation = "a translated verse",
            ),
        )

        render()
        composeRule.onNodeWithText("a translated verse", substring = true).performClick()

        assertThat(navigatedToAyah).isEqualTo(18 to 10)
    }

    // ---- The app bar ----

    @Test
    fun `search and settings are reachable from the app bar`() {
        var searched = false
        var settings = false
        homeState.value = QuranHomeUiState(isLoading = false, surahs = surahs)
        composeRule.setThemedContent {
            QuranHomeScreen(
                onNavigateToSurah = {},
                onNavigateToBrowse = {},
                onNavigateToSaved = {},
                onNavigateToSettings = { settings = true },
                onNavigateToSearch = { searched = true },
                viewModel = viewModel,
            )
        }

        composeRule.onNodeWithContentDescription(str(R.string.quran_home_search)).performClick()
        composeRule.onNodeWithContentDescription(str(R.string.quran_home_quran_settings))
            .performClick()

        assertThat(searched).isTrue()
        assertThat(settings).isTrue()
    }

    private fun bookmark(id: Long, surah: Int, ayah: Int) = QuranBookmark(
        id = id,
        ayahId = surah * 1000 + ayah,
        surahNumber = surah,
        ayahNumber = ayah,
        surahName = "The Cave",
        ayahText = "نص",
        note = null,
        color = null,
        createdAt = 0,
        updatedAt = 0,
    )
}
