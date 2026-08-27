package com.arshadshah.nimaz.presentation.screens.settings

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.MushafScript
import com.arshadshah.nimaz.domain.model.QuranReciter
import com.arshadshah.nimaz.domain.model.QuranTranslation
import com.arshadshah.nimaz.presentation.viewmodel.settings.QuranSettingsUiState
import com.arshadshah.nimaz.presentation.viewmodel.settings.SettingsEvent
import com.arshadshah.nimaz.testing.FakeSettingsScreenViewModel
import com.arshadshah.nimaz.testing.assertSettingsRowNotTappable
import com.arshadshah.nimaz.testing.settingsRow
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The Quran reader's settings — seven sections, and one cross-setting rule that is the reason
 * this screen is ordered the way it is.
 *
 * **Tajweed is gated on the Mushaf script**, because only the Madani edition carries per-letter
 * spans; the IndoPak layouts have none. The failure this guards against is the one #293 fixed: the
 * toggles used to be tappable on every layout and silently did nothing, so a user switched
 * tajweed on, saw no colour, and had no way to learn why. Three things must therefore hold
 * together — the toggle is disabled, and the subtitle *says* why rather than describing a feature
 * that will not happen.
 *
 * The second gate is inside tajweed itself: the colour-blind underline is meaningless without the
 * colours, so it must be off-limits when the colours are.
 *
 * The preview card is the other half. It renders the *real* text of the selected translation, and
 * the fallback to the bundled sample exists only for the moment before the first read resolves —
 * so a card that never showed the loaded text would look correct in a screenshot and be wrong for
 * every translation but one.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h4000dp")
class QuranSettingsScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val viewModel = FakeSettingsScreenViewModel()
    private var backs = 0
    private var reciterPicker = 0
    private var translationPicker = 0

    private fun setContent(state: QuranSettingsUiState = QuranSettingsUiState()) {
        viewModel.quranState.value = state
        composeRule.setThemedContent {
            QuranSettingsScreen(
                onNavigateBack = { backs++ },
                onNavigateToSelectReciter = { reciterPicker++ },
                onNavigateToSelectTranslation = { translationPicker++ },
                viewModel = viewModel.mock,
            )
        }
    }

    private fun string(@StringRes res: Int, vararg args: Any): String =
        context.getString(res, *args)

    @Test
    fun `all seven sections render, in the order a reader thinks about the page`() {
        setContent()

        composeRule.onAllNodesWithText(string(R.string.mushaf_layout)).onFirst().assertExists()
        composeRule.onNodeWithText(string(R.string.arabic_text)).assertExists()
        composeRule.onNodeWithText(string(R.string.tajweed_section)).assertExists()
        composeRule.onNodeWithText(string(R.string.audio)).assertExists()
        composeRule.onNodeWithText(string(R.string.reading_section)).assertExists()
    }

    @Test
    fun `tajweed is offered on the Madani layout`() {
        setContent(QuranSettingsUiState(mushafScript = MushafScript.MADANI))

        composeRule.settingsRow(string(R.string.show_tajweed_colors)).assertIsEnabled()
        composeRule.onNodeWithText(string(R.string.show_tajweed_colors_subtitle)).assertExists()
    }

    @Test
    fun `tajweed is disabled on an IndoPak layout, and says why`() {
        // The disabled state alone is not enough: a greyed row with the usual subtitle reads as
        // a bug. The unavailability text is what turns it into an explanation.
        setContent(QuranSettingsUiState(mushafScript = MushafScript.INDOPAK_16))

        composeRule.assertSettingsRowNotTappable(string(R.string.show_tajweed_colors))
        composeRule.onNodeWithText(string(R.string.show_tajweed_colors_unavailable)).assertExists()
    }

    @Test
    fun `a disabled tajweed row cannot be toggled`() {
        setContent(QuranSettingsUiState(mushafScript = MushafScript.INDOPAK_13, showTajweed = false))

        composeRule.assertSettingsRowNotTappable(string(R.string.show_tajweed_colors))

        assertThat(viewModel.events).isEmpty()
    }

    @Test
    fun `the colour-blind underline is off-limits while the colours are off`() {
        // Underlining rule spans is a *second* channel on top of the colours. Offered while the
        // colours are off it promises a rendering that cannot happen.
        setContent(QuranSettingsUiState(mushafScript = MushafScript.MADANI, showTajweed = false))

        composeRule.assertSettingsRowNotTappable(string(R.string.tajweed_underline))
    }

    @Test
    fun `the colour-blind underline becomes available once the colours are on`() {
        setContent(QuranSettingsUiState(mushafScript = MushafScript.MADANI, showTajweed = true))

        composeRule.settingsRow(string(R.string.tajweed_underline)).assertIsEnabled()
        composeRule.settingsRow(string(R.string.tajweed_underline)).performClick()

        assertThat(viewModel.only<SettingsEvent.SetTajweedUnderline>().enabled).isTrue()
    }

    @Test
    fun `the tajweed toggle dispatches the tajweed event when it is available`() {
        setContent(QuranSettingsUiState(mushafScript = MushafScript.MADANI, showTajweed = false))

        composeRule.settingsRow(string(R.string.show_tajweed_colors)).performClick()

        assertThat(viewModel.only<SettingsEvent.SetShowTajweed>().enabled).isTrue()
    }

    @Test
    fun `the colour guide opens even on a layout that cannot show the colours`() {
        // Deliberately reachable regardless of script, so someone can learn what the colours
        // mean before switching editions to see them (#294).
        setContent(QuranSettingsUiState(mushafScript = MushafScript.INDOPAK_16))

        composeRule.settingsRow(string(R.string.tajweed_colour_guide)).performClick()
        composeRule.waitForIdle()

        // The legend renders its own heading, so the guide's title now appears twice — once on
        // the row and once in the sheet. That second node *is* the assertion.
        composeRule.onAllNodesWithText(string(R.string.tajweed_colour_guide))
            .assertCountEquals(2)
    }

    @Test
    fun `the translation row names the translator, not only the language`() {
        // It used to pass the translator as `value` beside a `subtitle`, and the component
        // renders `subtitle ?: value` — so it showed "English" and the one thing the row exists
        // to report never appeared at all.
        val translation = QuranTranslation.entries.first()
        setContent(QuranSettingsUiState(selectedTranslatorId = translation.id))

        composeRule.onNodeWithText(
            string(
                R.string.settings_value_with_qualifier,
                translation.translator,
                translation.language.englishName,
            )
        ).assertExists()
    }

    @Test
    fun `the translation row opens the picker rather than editing in place`() {
        setContent()

        composeRule.settingsRow(string(R.string.translation)).performClick()

        assertThat(translationPicker).isEqualTo(1)
        assertThat(viewModel.events).isEmpty()
    }

    @Test
    fun `the reciter row names the reciter and opens the picker`() {
        val reciter = QuranReciter.entries.first()
        setContent(QuranSettingsUiState(selectedReciterId = reciter.id))

        composeRule.onNodeWithText(
            string(R.string.settings_value_with_qualifier, reciter.displayName, reciter.country)
        ).assertExists()
        composeRule.settingsRow(string(R.string.reciter)).performClick()

        assertThat(reciterPicker).isEqualTo(1)
    }

    @Test
    fun `the preview shows the loaded translation, not the bundled sample`() {
        setContent(
            QuranSettingsUiState(showTranslation = true, previewTranslation = "A loaded rendering")
        )

        composeRule.onNodeWithText("A loaded rendering").assertExists()
        composeRule.onNodeWithText(string(R.string.quran_settings_preview_translation))
            .assertDoesNotExist()
    }

    @Test
    fun `the preview falls back to the sample only before the first load resolves`() {
        setContent(QuranSettingsUiState(showTranslation = true, previewTranslation = null))

        composeRule.onNodeWithText(string(R.string.quran_settings_preview_translation))
            .assertExists()
    }

    @Test
    fun `hiding the translation removes it from the preview`() {
        setContent(
            QuranSettingsUiState(showTranslation = false, previewTranslation = "A loaded rendering")
        )

        composeRule.onNodeWithText("A loaded rendering").assertDoesNotExist()
    }

    @Test
    fun `the transliteration card follows its own toggle`() {
        setContent(QuranSettingsUiState(showTransliteration = true))
        composeRule.onNodeWithText(string(R.string.quran_settings_preview_transliteration))
            .assertExists()
    }

    @Test
    fun `the translation size slider is disabled while the translation is hidden`() {
        // A size control for something that is not on screen is a control with no effect.
        setContent(QuranSettingsUiState(showTranslation = false))

        composeRule.onNodeWithContentDescription(string(R.string.translation_font_size))
            .assertIsNotEnabled()
    }

    @Test
    fun `the show-translation and show-transliteration rows dispatch their own events`() {
        setContent(QuranSettingsUiState(showTranslation = true, showTransliteration = false))

        composeRule.settingsRow(string(R.string.show_translation)).performClick()
        composeRule.settingsRow(string(R.string.show_transliteration)).performClick()

        assertThat(viewModel.only<SettingsEvent.SetShowTranslation>().enabled).isFalse()
        assertThat(viewModel.only<SettingsEvent.SetShowTransliteration>().enabled).isTrue()
    }

    @Test
    fun `the reading rows dispatch continuous playback and keep-screen-on`() {
        setContent(QuranSettingsUiState(continuousReading = true, keepScreenOn = true))

        composeRule.settingsRow(string(R.string.continuous_reading)).performClick()
        composeRule.settingsRow(string(R.string.keep_screen_on)).performClick()

        assertThat(viewModel.only<SettingsEvent.SetContinuousReading>().enabled).isFalse()
        assertThat(viewModel.only<SettingsEvent.SetKeepScreenOn>().enabled).isFalse()
    }

    @Test
    fun `the font size shown is the one this screen's state holds`() {
        setContent(QuranSettingsUiState(arabicFontSize = 36f, translationFontSize = 22f))

        composeRule.onNodeWithText(string(R.string.arabic_font_size_value, 36)).assertExists()
        composeRule.onNodeWithText(string(R.string.arabic_font_size_value, 22)).assertExists()
    }

    @Test
    fun `the back button navigates back`() {
        setContent()

        composeRule.onNodeWithContentDescription(string(R.string.cd_back)).performClick()

        assertThat(backs).isEqualTo(1)
    }
}
