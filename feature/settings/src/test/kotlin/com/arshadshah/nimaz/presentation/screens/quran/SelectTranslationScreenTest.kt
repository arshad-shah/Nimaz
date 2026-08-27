package com.arshadshah.nimaz.presentation.screens.quran

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.QuranTranslation
import com.arshadshah.nimaz.presentation.viewmodel.settings.QuranSettingsUiState
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
 * The translation picker — fifteen translations across eleven languages, grouped by language.
 *
 * The hero card is what the screen is for: it renders the Bismillah in the selected translation,
 * so a translation can be judged by *reading* it rather than by its translator's name. Two things
 * about that are worth pinning. It falls back to the bundled sample only until the first real read
 * resolves — a card permanently on the fallback would show every translation as identical. And the
 * previous text is kept while a newly-tapped one loads, because the first read of a translation
 * also seeds its 6,236 rows, so the card would otherwise flash empty on every tap.
 *
 * The search is deliberately three-way: translator, English language name, and endonym. Someone
 * looking for the Urdu translation may type "Urdu", "اردو" or "Maududi", and a search that
 * matched only the translator's name fails two of those three — which reads as the translation not
 * being there at all.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h4000dp")
class SelectTranslationScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val viewModel = FakeSettingsScreenViewModel()
    private var backs = 0

    private val first = QuranTranslation.entries.first()
    private val other = QuranTranslation.entries.first { it.language != first.language }

    private fun setContent(state: QuranSettingsUiState = QuranSettingsUiState()) {
        viewModel.quranState.value = state
        composeRule.setThemedContent {
            SelectTranslationScreen(onNavigateBack = { backs++ }, viewModel = viewModel.mock)
        }
    }

    private fun string(@StringRes res: Int, vararg args: Any): String =
        context.getString(res, *args)

    private fun search(query: String) {
        composeRule.onNodeWithContentDescription(string(R.string.select_translation_search_hint))
            .performTextInput(query)
    }

    @Test
    fun `the hero names the selected translation and marks it active`() {
        setContent(QuranSettingsUiState(selectedTranslatorId = other.id))

        composeRule.onAllNodesWithText(other.translator).onFirst().assertExists()
        composeRule.onNodeWithText(string(R.string.active)).assertExists()
    }

    @Test
    fun `the hero shows the real text of the selected translation`() {
        setContent(
            QuranSettingsUiState(
                selectedTranslatorId = first.id,
                previewTranslation = "A distinctive rendering",
            )
        )

        composeRule.onNodeWithText("A distinctive rendering").assertExists()
        composeRule.onNodeWithText(string(R.string.quran_settings_preview_translation))
            .assertDoesNotExist()
    }

    @Test
    fun `the hero falls back to the sample only before the first read resolves`() {
        // A card permanently on the fallback shows every translation as identical, which
        // defeats the entire purpose of the screen.
        setContent(QuranSettingsUiState(previewTranslation = null))

        composeRule.onNodeWithText(string(R.string.quran_settings_preview_translation))
            .assertExists()
    }

    @Test
    fun `every translation is listed under its own language`() {
        setContent()

        val languages = QuranTranslation.entries.map { it.language }.distinct()
        assertThat(languages.size).isAtLeast(2)
        composeRule.onAllNodesWithText(languages.first().englishName).onFirst().assertExists()
        composeRule.onAllNodesWithText(first.translator).onFirst().assertExists()
    }

    @Test
    fun `each language heading carries its endonym as well as its English name`() {
        // The endonym is written in its own script, in its own face — someone who does not read
        // English finds their language by the endonym or not at all.
        setContent()

        val language = QuranTranslation.entries
            .map { it.language }
            .first { it.nativeName != it.englishName }

        composeRule.onAllNodesWithText(language.nativeName).onFirst().assertExists()
    }

    @Test
    fun `picking a translation dispatches that translator's id`() {
        setContent(QuranSettingsUiState(selectedTranslatorId = first.id))

        composeRule.onAllNodesWithText(other.translator).onLast().performClick()

        assertThat(viewModel.only<SettingsEvent.SetTranslator>().translatorId)
            .isEqualTo(other.id)
    }

    @Test
    fun `searching by the translator's name finds the translation`() {
        setContent()

        search(other.translator)

        composeRule.onAllNodesWithText(other.translator).onFirst().assertExists()
    }

    @Test
    fun `searching by the English language name finds it too`() {
        setContent()

        search(other.language.englishName)

        composeRule.onAllNodesWithText(other.translator).onFirst().assertExists()
    }

    @Test
    fun `searching by the endonym finds it as well`() {
        // "اردو" and "Urdu" must reach the same row. A search over the translator alone fails
        // this, and the translation reads as missing.
        val language = QuranTranslation.entries
            .map { it.language }
            .first { it.nativeName != it.englishName }
        val translation = QuranTranslation.entries.first { it.language == language }
        setContent()

        search(language.nativeName)

        composeRule.onAllNodesWithText(translation.translator).onFirst().assertExists()
    }

    @Test
    fun `a search that matches nothing says so rather than showing an empty screen`() {
        setContent()

        search("zzzzzzzz")

        composeRule.onNodeWithText(string(R.string.picker_no_matches)).assertExists()
        composeRule.onNodeWithText(string(R.string.no_results_hint)).assertExists()
    }

    @Test
    fun `the hero stays visible while a search matches nothing`() {
        // It is the only thing on screen naming what is currently selected, and clearing an
        // over-narrow search is easier when you can still see where you started.
        setContent(QuranSettingsUiState(selectedTranslatorId = first.id))

        search("zzzzzzzz")

        composeRule.onAllNodesWithText(first.translator).onFirst().assertExists()
    }

    @Test
    fun `the title is Translation and the back button navigates back`() {
        setContent()

        composeRule.onAllNodesWithText(string(R.string.translation)).onFirst().assertIsDisplayed()
        composeRule.onNodeWithContentDescription(string(R.string.cd_back)).performClick()

        assertThat(backs).isEqualTo(1)
    }
}
