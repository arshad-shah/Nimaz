package com.arshadshah.nimaz.presentation.screens.dua

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.presentation.viewmodel.settings.DuaSettingsUiState
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
 * The Dua reader's display settings, and the preview card that is supposed to show the result.
 *
 * Two things go wrong on a screen like this and neither shows up in review. The first is the one
 * #615 names: three toggles whose rows are byte-identical apart from a string, so a row wired to
 * its neighbour's event moves the switch the user tapped and changes the *other* preference. The
 * second is the preview: the card exists so someone can see the effect of a setting before leaving
 * the screen, and a preview that ignores a toggle is a preview that lies.
 *
 * The Dua and Hadith screens deliberately mirror each other and share `readerTypographySettings`,
 * so the crossing that matters most is between *screens* — `SetDuaArabicFontSize` against
 * `SetHadithArabicFontSize`. That is asserted by type here and again in the Hadith file.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class DuaSettingsScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val viewModel = FakeSettingsScreenViewModel()
    private var backs = 0

    private fun setContent(state: DuaSettingsUiState = DuaSettingsUiState()) {
        viewModel.duaState.value = state
        composeRule.setThemedContent {
            DuaSettingsScreen(onNavigateBack = { backs++ }, viewModel = viewModel.mock)
        }
    }

    private fun string(@StringRes res: Int, vararg args: Any): String =
        context.getString(res, *args)

    @Test
    fun `the screen opens on the dua settings, not the shared reader ones`() {
        // Four screens in this module carry the same three rows. The title is the only thing on
        // screen that says which corpus's preferences are being edited.
        setContent()

        composeRule.onNodeWithText(string(R.string.dua_settings)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.display_options)).assertIsDisplayed()
    }

    @Test
    fun `the preview renders all three parts when all three are switched on`() {
        setContent(
            DuaSettingsUiState(
                showArabic = true,
                showTransliteration = true,
                showTranslation = true,
            )
        )

        composeRule.onNodeWithText(string(R.string.dua_settings_preview_arabic))
            .assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.dua_settings_preview_transliteration))
            .assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.dua_settings_preview_translation))
            .assertIsDisplayed()
    }

    @Test
    fun `switching Arabic off removes it from the preview and leaves the rest`() {
        // Three `if` blocks in one card. Dropping the wrong one is invisible until someone reads
        // the preview against the toggles — which is exactly what the preview is for.
        setContent(
            DuaSettingsUiState(
                showArabic = false,
                showTransliteration = true,
                showTranslation = true,
            )
        )

        composeRule.onNodeWithText(string(R.string.dua_settings_preview_arabic))
            .assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.dua_settings_preview_transliteration))
            .assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.dua_settings_preview_translation))
            .assertIsDisplayed()
    }

    @Test
    fun `switching transliteration off removes only the transliteration`() {
        setContent(
            DuaSettingsUiState(
                showArabic = true,
                showTransliteration = false,
                showTranslation = true,
            )
        )

        composeRule.onNodeWithText(string(R.string.dua_settings_preview_arabic))
            .assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.dua_settings_preview_transliteration))
            .assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.dua_settings_preview_translation))
            .assertIsDisplayed()
    }

    @Test
    fun `switching translation off removes only the translation`() {
        setContent(
            DuaSettingsUiState(
                showArabic = true,
                showTransliteration = true,
                showTranslation = false,
            )
        )

        composeRule.onNodeWithText(string(R.string.dua_settings_preview_translation))
            .assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.dua_settings_preview_arabic))
            .assertIsDisplayed()
    }

    @Test
    fun `the show-Arabic row toggles Arabic and nothing else`() {
        setContent(DuaSettingsUiState(showArabic = true))

        composeRule.onNodeWithText(string(R.string.show_arabic)).performClick()

        assertThat(viewModel.only<SettingsEvent.SetDuaShowArabic>().enabled).isFalse()
        assertThat(viewModel.events).hasSize(1)
    }

    @Test
    fun `the show-transliteration row toggles transliteration and nothing else`() {
        setContent(DuaSettingsUiState(showTransliteration = false))

        composeRule.onNodeWithText(string(R.string.show_transliteration)).performClick()

        assertThat(viewModel.only<SettingsEvent.SetDuaShowTransliteration>().enabled).isTrue()
        assertThat(viewModel.events).hasSize(1)
    }

    @Test
    fun `the show-translation row toggles translation and nothing else`() {
        setContent(DuaSettingsUiState(showTranslation = true))

        composeRule.onNodeWithText(string(R.string.show_translation)).performClick()

        assertThat(viewModel.only<SettingsEvent.SetDuaShowTranslation>().enabled).isFalse()
        assertThat(viewModel.events).hasSize(1)
    }

    @Test
    fun `a toggle sends the opposite of what is stored, not a fixed value`() {
        // `onCheckedChange = { onEvent(Set…(!state.value)) }` ignores the boolean the switch
        // hands it and reads state instead. Wiring it to a constant makes the row a one-way
        // switch: it turns the setting on and can never turn it off again.
        setContent(DuaSettingsUiState(showArabic = false))

        composeRule.onNodeWithText(string(R.string.show_arabic)).performClick()

        assertThat(viewModel.only<SettingsEvent.SetDuaShowArabic>().enabled).isTrue()
    }

    @Test
    fun `the typography rows show the sizes this screen's own state holds`() {
        // `readerTypographySettings` is shared with the Hadith screen, so it is handed the state
        // by the caller. A caller that passed the wrong field would render the other reader's
        // size and write it back on the first drag.
        setContent(DuaSettingsUiState(arabicFontSize = 30f, translationFontSize = 18f))

        composeRule.onNodeWithText(string(R.string.arabic_font_size_value, 30)).assertExists()
        composeRule.onNodeWithText(string(R.string.arabic_font_size_value, 18)).assertExists()
    }

    @Test
    fun `the back button navigates back`() {
        setContent()

        composeRule.onNodeWithContentDescription(string(R.string.cd_back)).performClick()

        assertThat(backs).isEqualTo(1)
    }
}
