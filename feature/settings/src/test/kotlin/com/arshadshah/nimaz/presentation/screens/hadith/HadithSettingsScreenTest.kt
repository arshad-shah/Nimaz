package com.arshadshah.nimaz.presentation.screens.hadith

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.presentation.viewmodel.settings.HadithSettingsUiState
import com.arshadshah.nimaz.presentation.viewmodel.settings.SettingsEvent
import com.arshadshah.nimaz.testing.FakeSettingsScreenViewModel
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The Hadith reader's display settings — the Dua screen's twin, plus two rows of its own.
 *
 * Written as a separate file rather than a parameterisation of the Dua one on purpose. The two
 * screens are deliberate copies, and the bug a shared test cannot catch is precisely the one a copy
 * invites: a Hadith row still dispatching the `SetDua…` event it was copied from. Asserting the
 * event *type* per screen is the only thing that catches that, and it only works if each screen
 * has its own assertions.
 *
 * The grade badge is the interesting extra. It renders in the preview's header row rather than in
 * the body, so a toggle that dropped the wrong `if` would leave the badge showing with the whole
 * hadith hidden.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class HadithSettingsScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val viewModel = FakeSettingsScreenViewModel()
    private var backs = 0

    private fun setContent(state: HadithSettingsUiState = HadithSettingsUiState()) {
        viewModel.hadithState.value = state
        composeRule.setThemedContent {
            HadithSettingsScreen(onNavigateBack = { backs++ }, viewModel = viewModel.mock)
        }
    }

    private fun string(@StringRes res: Int, vararg args: Any): String =
        context.getString(res, *args)

    @Test
    fun `the screen opens on the hadith settings, not the dua ones`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.hadith_settings)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.hadith_show_grade)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.hadith_show_chain)).assertIsDisplayed()
    }

    @Test
    fun `the preview shows the hadith, its translation and its grade badge`() {
        setContent(
            HadithSettingsUiState(showArabic = true, showTranslation = true, showGrade = true)
        )

        composeRule.onNodeWithText(string(R.string.hadith_settings_preview_arabic))
            .assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.hadith_settings_preview_translation))
            .assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.hadith_grade_sahih)).assertIsDisplayed()
    }

    @Test
    fun `switching the grade off removes only the badge`() {
        // The badge lives in the header row, not the body — so the `if` that hides it is the one
        // furthest from the two it sits beside.
        setContent(
            HadithSettingsUiState(showArabic = true, showTranslation = true, showGrade = false)
        )

        composeRule.onNodeWithText(string(R.string.hadith_grade_sahih)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.hadith_settings_preview_arabic))
            .assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.hadith_settings_preview_translation))
            .assertIsDisplayed()
    }

    @Test
    fun `switching the Arabic off leaves the translation and the badge`() {
        setContent(
            HadithSettingsUiState(showArabic = false, showTranslation = true, showGrade = true)
        )

        composeRule.onNodeWithText(string(R.string.hadith_settings_preview_arabic))
            .assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.hadith_settings_preview_translation))
            .assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.hadith_grade_sahih)).assertIsDisplayed()
    }

    @Test
    fun `switching the translation off leaves the Arabic`() {
        setContent(
            HadithSettingsUiState(showArabic = true, showTranslation = false, showGrade = true)
        )

        composeRule.onNodeWithText(string(R.string.hadith_settings_preview_translation))
            .assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.hadith_settings_preview_arabic))
            .assertIsDisplayed()
    }

    @Test
    fun `the show-Arabic row dispatches the Hadith event, not the Dua one it was copied from`() {
        setContent(HadithSettingsUiState(showArabic = true))

        composeRule.onNodeWithText(string(R.string.show_arabic)).performClick()

        assertThat(viewModel.only<SettingsEvent.SetHadithShowArabic>().enabled).isFalse()
        assertThat(viewModel.events.filterIsInstance<SettingsEvent.SetDuaShowArabic>()).isEmpty()
    }

    @Test
    fun `the show-translation row toggles the hadith translation`() {
        setContent(HadithSettingsUiState(showTranslation = true))

        composeRule.onNodeWithText(string(R.string.show_translation)).performClick()

        assertThat(viewModel.only<SettingsEvent.SetHadithShowTranslation>().enabled).isFalse()
        assertThat(viewModel.events).hasSize(1)
    }

    @Test
    fun `the grade row toggles the grade and the chain row toggles the chain`() {
        // Adjacent rows, adjacent event names, one letter apart in the string resources. This is
        // the pair most likely to be crossed on this screen.
        setContent(HadithSettingsUiState(showGrade = true, showChain = false))

        composeRule.onNodeWithText(string(R.string.hadith_show_grade)).performClick()
        composeRule.onNodeWithText(string(R.string.hadith_show_chain)).performClick()

        assertThat(viewModel.only<SettingsEvent.SetHadithShowGrade>().enabled).isFalse()
        assertThat(viewModel.only<SettingsEvent.SetHadithShowChain>().enabled).isTrue()
    }

    @Test
    fun `the typography rows show this reader's own sizes`() {
        setContent(HadithSettingsUiState(arabicFontSize = 26f, translationFontSize = 14f))

        composeRule.onNodeWithText(string(R.string.arabic_font_size_value, 26)).assertExists()
        composeRule.onNodeWithText(string(R.string.arabic_font_size_value, 14)).assertExists()
    }

    @Test
    fun `the back button navigates back`() {
        setContent()

        composeRule.onNodeWithContentDescription(string(R.string.cd_back)).performClick()

        assertThat(backs).isEqualTo(1)
    }
}
