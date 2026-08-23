package com.arshadshah.nimaz.presentation.screens.quran

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.domain.model.TafseerNoteItem
import com.arshadshah.nimaz.presentation.screens.str
import com.arshadshah.nimaz.presentation.viewmodel.UiError
import com.arshadshah.nimaz.presentation.viewmodel.quran.TafseerChaptersUiState
import com.arshadshah.nimaz.presentation.viewmodel.quran.TafseerChaptersViewModel
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
 * The commentary's front door: a surah picker, and the notes the reader has written on it.
 *
 * The failure case is the one worth having. Without an error in the state, a content-database
 * failure rendered as an empty picker with the spinner turned off — which on the notes tab is
 * indistinguishable from "you have no notes yet", and on the surah tab from a Qur'an with no
 * surahs in it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class TafseerChaptersScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val state = MutableStateFlow(TafseerChaptersUiState())
    private var opened: Pair<Int, Int>? = null

    private val viewModel: TafseerChaptersViewModel = mockk(relaxed = true) {
        every { this@mockk.state } returns this@TafseerChaptersScreenTest.state
    }

    private fun render() {
        composeRule.setThemedContent {
            TafseerChaptersScreen(
                onNavigateBack = {},
                onOpenTafseer = { s, a -> opened = s to a },
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

    private val note = TafseerNoteItem(
        highlightId = 1,
        surahNumber = 2,
        ayahNumber = 153,
        sourceLabel = "Ibn Kathir",
        color = "yellow",
        note = "on bearing what is difficult",
    )

    @Test
    fun `the surahs are listed`() {
        state.value = TafseerChaptersUiState(
            isLoading = false,
            surahs = listOf(surah(1, "The Opening"), surah(2, "The Cow")),
        )

        render()

        composeRule.onNodeWithText("The Opening").assertIsDisplayed()
        composeRule.onNodeWithText("The Cow").assertIsDisplayed()
    }

    @Test
    fun `opening a surah opens its commentary at the first verse`() {
        state.value = TafseerChaptersUiState(isLoading = false, surahs = listOf(surah(2, "The Cow")))
        render()

        composeRule.onNodeWithText("The Cow").performClick()

        assertThat(opened).isEqualTo(2 to 1)
    }

    @Test
    fun `the notes tab counts what is on it`() {
        state.value = TafseerChaptersUiState(
            isLoading = false,
            surahs = listOf(surah(2, "The Cow")),
            notes = listOf(note, note.copy(highlightId = 2)),
        )

        render()

        composeRule.onNodeWithText(str(R.string.tafseer_tab_notes) + " · 2").assertIsDisplayed()
    }

    @Test
    fun `a reader with no notes is told how to make one`() {
        state.value = TafseerChaptersUiState(
            isLoading = false,
            surahs = listOf(surah(2, "The Cow")),
            notes = emptyList(),
        )
        render()

        composeRule.onNodeWithText(str(R.string.tafseer_tab_notes)).performClick()

        composeRule.onNodeWithText(str(R.string.tafseer_no_notes)).assertIsDisplayed()
    }

    @Test
    fun `a written note is listed with the verse it is about`() {
        state.value = TafseerChaptersUiState(
            isLoading = false,
            surahs = listOf(surah(2, "The Cow")),
            notes = listOf(note),
        )
        render()

        composeRule.onNodeWithText(str(R.string.tafseer_tab_notes) + " · 1").performClick()

        composeRule.onNodeWithText("on bearing what is difficult").assertIsDisplayed()
    }

    @Test
    fun `opening a note opens the verse it was written on`() {
        state.value = TafseerChaptersUiState(
            isLoading = false,
            surahs = listOf(surah(2, "The Cow")),
            notes = listOf(note),
        )
        render()
        composeRule.onNodeWithText(str(R.string.tafseer_tab_notes) + " · 1").performClick()

        composeRule.onNodeWithText("on bearing what is difficult").performClick()

        assertThat(opened).isEqualTo(2 to 153)
    }

    @Test
    fun `a failed load says so rather than showing a Quran with no surahs`() {
        state.value = TafseerChaptersUiState(
            isLoading = false,
            surahs = emptyList(),
            error = UiError(message = R.string.tafseer_load_failed),
        )

        render()

        composeRule.onNodeWithText(str(R.string.tafseer_load_failed)).assertIsDisplayed()
    }

    @Test
    fun `a first load shows neither the list nor the failure`() {
        state.value = TafseerChaptersUiState(isLoading = true)

        render()

        composeRule.onNodeWithText(str(R.string.tafseer_load_failed)).assertDoesNotExist()
    }
}
