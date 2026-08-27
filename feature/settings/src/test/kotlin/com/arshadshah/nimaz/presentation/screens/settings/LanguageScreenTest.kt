package com.arshadshah.nimaz.presentation.screens.settings

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.presentation.viewmodel.settings.AppLanguage
import com.arshadshah.nimaz.presentation.viewmodel.settings.GeneralSettingsUiState
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
 * The language picker — six rows built from `AppLanguage.entries`.
 *
 * The list being enum-driven is what makes it worth testing: a seventh language is added by
 * appending an enum constant, and the only thing standing between that and a row that never
 * appears is that the screen iterates the entries rather than listing them. Asserting against
 * `entries` rather than against six hardcoded names is the whole point — a test that named the
 * six would pass forever while the seventh was invisible.
 *
 * The native name is the second half. A picker that shows only English names is unusable by
 * exactly the people who need it: someone looking for Türkçe is not searching for "Turkish".
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class LanguageScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val viewModel = FakeSettingsScreenViewModel()
    private var backs = 0

    private fun setContent(language: AppLanguage = AppLanguage.ENGLISH) {
        viewModel.generalState.value = GeneralSettingsUiState(language = language)
        composeRule.setThemedContent {
            LanguageScreen(onNavigateBack = { backs++ }, viewModel = viewModel.mock)
        }
    }

    private fun string(@StringRes res: Int, vararg args: Any): String =
        context.getString(res, *args)

    @Test
    fun `every declared language is offered`() {
        setContent()

        AppLanguage.entries.forEach { language ->
            composeRule.onNodeWithText(language.displayName).assertExists()
        }
    }

    @Test
    fun `each language shows its own name in its own script`() {
        // "Türkçe" and "Bahasa Indonesia" are what someone who does not read English will
        // recognise. A row showing only the English name is a picker they cannot use.
        setContent()

        AppLanguage.entries
            .filter { it.nativeName != it.displayName }
            .forEach { composeRule.onNodeWithText(it.nativeName).assertExists() }
    }

    @Test
    fun `each language shows its country code badge`() {
        setContent()

        AppLanguage.entries.forEach { composeRule.onNodeWithText(it.flag).assertExists() }
    }

    @Test
    fun `picking a language sends that language, not its neighbour in the list`() {
        // Six identical rows in an indexed loop. Capturing the loop's `index` instead of its
        // `language` — or reusing a variable across the iteration — sends the wrong one, and the
        // app restarts into a language nobody chose.
        setContent(AppLanguage.ENGLISH)

        composeRule.onNodeWithText(AppLanguage.MALAY.displayName).performClick()

        assertThat(viewModel.only<SettingsEvent.SetLanguage>().language)
            .isEqualTo(AppLanguage.MALAY)
    }

    @Test
    fun `picking the last language in the list works too`() {
        // The loop skips the divider after the final row (`index < size - 1`), which is exactly
        // the kind of index arithmetic that gets applied to the row itself by mistake.
        setContent()

        composeRule.onNodeWithText(AppLanguage.entries.last().displayName).performClick()

        assertThat(viewModel.only<SettingsEvent.SetLanguage>().language)
            .isEqualTo(AppLanguage.entries.last())
    }

    @Test
    fun `re-picking the language already in use still dispatches`() {
        // The row is not disabled when selected, and it should not be: tapping your current
        // language is how someone confirms it is applied after a failed restart.
        setContent(AppLanguage.GERMAN)

        composeRule.onNodeWithText(AppLanguage.GERMAN.displayName).performClick()

        assertThat(viewModel.only<SettingsEvent.SetLanguage>().language)
            .isEqualTo(AppLanguage.GERMAN)
    }

    @Test
    fun `the screen warns that changing the language restarts the app`() {
        // A restart with no warning reads as a crash.
        setContent()

        composeRule.onNodeWithText(string(R.string.language_change_info)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.app_language_section)).assertIsDisplayed()
    }

    @Test
    fun `the back button navigates back`() {
        setContent()

        composeRule.onNodeWithContentDescription(string(R.string.cd_back)).performClick()

        assertThat(backs).isEqualTo(1)
    }
}
