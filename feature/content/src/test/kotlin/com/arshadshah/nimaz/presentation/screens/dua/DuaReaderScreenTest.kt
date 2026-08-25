package com.arshadshah.nimaz.presentation.screens.dua

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.DuaOccasion
import com.arshadshah.nimaz.presentation.viewmodel.UiError
import com.arshadshah.nimaz.presentation.viewmodel.content.DuaEvent
import com.arshadshah.nimaz.presentation.viewmodel.content.DuaReaderUiState
import com.arshadshah.nimaz.presentation.viewmodel.content.DuaViewModel
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

/**
 * The dua reader: one supplication per page, with three display toggles over it.
 *
 * **The three toggles are what `:core:datastore` (#603) persists and nothing asserted.**
 * `duaShowTransliteration` off must actually remove the transliteration — and the
 * transliteration is doubly guarded, because the content artifact does not carry one for every
 * dua, so "the setting is on but the field is empty" and "the setting is off" have to produce
 * the same page without either being mistaken for the other.
 *
 * **Every optional field below the text is guarded, and the guards interact.** The meta row
 * appears when there is a reference *or* a positive repeat count, and each chip inside it is
 * guarded again; the virtue card appears only for a dua that ships benefits. Ungated, a dua
 * with none of them renders an empty card and two empty pills under a supplication.
 *
 * **"Not found" and "failed to load" reach the same branch.** `duas.isEmpty()` is true for both,
 * and the state's `error` is what separates them: a missing dua keeps the not-found wording, a
 * failure says what failed. They looked identical before the error carried a kind.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class DuaReaderScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val readerState = MutableStateFlow(DuaReaderUiState())
    private val events = mutableListOf<DuaEvent>()

    private val viewModel: DuaViewModel = mockk(relaxed = true) {
        every { this@mockk.readerState } returns this@DuaReaderScreenTest.readerState
        every { onEvent(any()) } answers { events += firstArg<DuaEvent>() }
        every { isDuaFavorite(any()) } returns flowOf(false)
    }

    private var settingsOpened = 0

    private fun setContent(duaId: String = "dua_1") {
        composeRule.setThemedContent {
            DuaReaderScreen(
                duaId = duaId,
                onNavigateBack = {},
                onNavigateToSettings = { settingsOpened++ },
                viewModel = viewModel,
            )
        }
    }

    private fun string(@StringRes res: Int, vararg args: Any): String =
        context.getString(res, *args)

    private fun loaded(
        vararg duas: com.arshadshah.nimaz.domain.model.Dua,
        showArabic: Boolean = true,
        showTransliteration: Boolean = true,
        showTranslation: Boolean = true,
    ) {
        readerState.value = DuaReaderUiState(
            duas = duas.toList(),
            isLoading = false,
            showArabic = showArabic,
            showTransliteration = showTransliteration,
            showTranslation = showTranslation,
        )
    }

    @Test
    fun `opening the reader asks for the dua named in the route`() {
        readerState.value = DuaReaderUiState(isLoading = false)

        setContent(duaId = "dua_42")

        assertThat(events).containsExactly(DuaEvent.LoadDua("dua_42"))
    }

    @Test
    fun `a dua renders its Arabic, transliteration and translation together`() {
        loaded(
            dua(
                textArabic = "بسم الله",
                textTransliteration = "Bismillah",
                textEnglish = "In the name of Allah",
            )
        )

        setContent()

        composeRule.onNodeWithText("بسم الله").assertExists()
        composeRule.onNodeWithText("Bismillah").assertExists()
        composeRule.onNodeWithText("In the name of Allah").assertExists()
    }

    @Test
    fun `hiding the Arabic removes the Arabic`() {
        loaded(
            dua(textArabic = "بسم الله", textEnglish = "In the name of Allah"),
            showArabic = false,
        )

        setContent()

        composeRule.onNodeWithText("بسم الله").assertDoesNotExist()
        composeRule.onNodeWithText("In the name of Allah").assertExists()
    }

    @Test
    fun `hiding the transliteration removes the transliteration`() {
        loaded(
            dua(textTransliteration = "Bismillah", textEnglish = "In the name of Allah"),
            showTransliteration = false,
        )

        setContent()

        composeRule.onNodeWithText("Bismillah").assertDoesNotExist()
        composeRule.onNodeWithText("In the name of Allah").assertExists()
    }

    @Test
    fun `a dua that ships no transliteration renders as if the setting were off`() {
        // The second half of the guard. Without it the divider and the spacing appear over an
        // empty line, which reads as a missing translation rather than as absent data.
        loaded(dua(textTransliteration = null, textEnglish = "In the name of Allah"))

        setContent()

        composeRule.onNodeWithText("In the name of Allah").assertExists()
    }

    @Test
    fun `hiding the translation removes the translation`() {
        loaded(
            dua(textArabic = "بسم الله", textEnglish = "In the name of Allah"),
            showTranslation = false,
        )

        setContent()

        composeRule.onNodeWithText("In the name of Allah").assertDoesNotExist()
        composeRule.onNodeWithText("بسم الله").assertExists()
    }

    @Test
    fun `a reference and a recitation count are shown as their own chips`() {
        loaded(dua(reference = "Sahih Muslim 2723", repeatCount = 3))

        setContent()

        composeRule.onNodeWithText("Sahih Muslim 2723").assertExists()
        composeRule.onNodeWithText(
            context.resources.getQuantityString(R.plurals.dua_reader_recite_count, 3, 3)
        ).assertExists()
    }

    @Test
    fun `a dua with neither a reference nor a count shows no meta row at all`() {
        loaded(dua(reference = null, repeatCount = null, textEnglish = "Bare supplication"))

        setContent()

        composeRule.onNodeWithText("Bare supplication").assertExists()
        composeRule.onNodeWithText(
            context.resources.getQuantityString(R.plurals.dua_reader_recite_count, 1, 1)
        ).assertDoesNotExist()
    }

    @Test
    fun `a repeat count of zero is not a chip saying zero`() {
        // The content artifact stores 0 for "no prescribed count", and `repeatCount > 0` is what
        // keeps that out of the reader.
        loaded(dua(reference = "Sunan Abi Dawud 5090", repeatCount = 0))

        setContent()

        composeRule.onNodeWithText("Sunan Abi Dawud 5090").assertExists()
        composeRule.onNodeWithText(
            context.resources.getQuantityString(R.plurals.dua_reader_recite_count, 0, 0)
        ).assertDoesNotExist()
    }

    @Test
    fun `a reference with no recitation count still gets its own chip`() {
        // The meta row appears when there is a reference **or** a count, and each chip inside
        // it is guarded again. Collapsing the two guards into one renders an empty second pill
        // beside the reference.
        loaded(dua(reference = "Sahih Muslim 2723", repeatCount = null))

        setContent()

        composeRule.onNodeWithText("Sahih Muslim 2723").assertExists()
        composeRule.onNodeWithText(
            context.resources.getQuantityString(R.plurals.dua_reader_recite_count, 3, 3)
        ).assertDoesNotExist()
    }

    @Test
    fun `a recitation count with no reference still gets its own chip`() {
        // The other half, and the one the content artifact produces more often: a prescribed
        // count with no citation recorded.
        loaded(dua(reference = null, repeatCount = 3))

        setContent()

        composeRule.onNodeWithText(
            context.resources.getQuantityString(R.plurals.dua_reader_recite_count, 3, 3)
        ).assertExists()
    }

    @Test
    fun `an empty reference string counts as no reference`() {
        // `isNullOrEmpty`, not `== null`: the artifact stores `""` as readily as null, and an
        // empty chip is a pill with nothing in it.
        loaded(dua(reference = "", repeatCount = null, textEnglish = "Bare supplication"))

        setContent()

        composeRule.onNodeWithText("Bare supplication").assertExists()
    }

    @Test
    fun `a dua that ships a virtue shows the virtue card`() {
        loaded(dua(benefits = "Whoever says this is protected until evening"))

        setContent()

        composeRule.onNodeWithText(string(R.string.dua_reader_virtue)).assertExists()
        composeRule.onNodeWithText("Whoever says this is protected until evening").assertExists()
    }

    @Test
    fun `a dua with no recorded virtue shows no empty virtue card`() {
        loaded(dua(benefits = null))

        setContent()

        composeRule.onNodeWithText(string(R.string.dua_reader_virtue)).assertDoesNotExist()
    }

    @Test
    fun `the occasion badge is localised, and absent when the dua has no occasion`() {
        loaded(dua(occasion = DuaOccasion.RAIN))

        setContent()

        composeRule.onNodeWithText(string(R.string.dua_occasion_rain)).assertExists()
    }

    @Test
    fun `the app bar carries the open dua's title`() {
        loaded(dua(titleEnglish = "Dua for rain"))

        setContent()

        composeRule.onNodeWithText("Dua for rain").assertExists()
    }

    @Test
    fun `favouriting the open dua cites its id and its category`() {
        // The category id travels with the write because a `DuaBookmark` is keyed on both;
        // sending the dua's id alone files the favourite under no collection.
        loaded(dua(id = "dua_7", categoryId = "evening"))

        setContent()
        composeRule.onNodeWithContentDescription(string(R.string.dua_reader_favorite))
            .performClick()

        assertThat(events).contains(DuaEvent.ToggleFavorite("dua_7", "evening"))
    }

    @Test
    fun `adding to tasbih writes through this feature's own ViewModel and says it did`() {
        // The screen used to reach `TasbihViewModel` through a `hiltViewModel()` of its own —
        // a cross-feature edge `moduleBoundary` now forbids, and one that never held the tasbih
        // screen's instance anyway. The toast is the only feedback the reader gets.
        val target = dua(id = "dua_9", titleEnglish = "Tasbih-able")
        loaded(target)

        setContent()
        composeRule.onNodeWithContentDescription(string(R.string.dua_reader_add_tasbih))
            .performClick()

        assertThat(events).contains(DuaEvent.AddToTasbih(target))
        assertThat(ShadowToast.getTextOfLatestToast())
            .isEqualTo(string(R.string.dua_reader_added_tasbih))
    }

    @Test
    fun `a single-dua collection offers no page arrows`() {
        loaded(dua())

        setContent()

        composeRule.onNodeWithContentDescription(string(R.string.dua_reader_prev))
            .assertDoesNotExist()
        composeRule.onNodeWithContentDescription(string(R.string.dua_reader_share)).assertExists()
    }

    @Test
    fun `a multi-dua collection offers arrows, and the first page cannot go back`() {
        loaded(dua(id = "d1"), dua(id = "d2"), dua(id = "d3"))

        setContent()

        composeRule.onNodeWithContentDescription(string(R.string.dua_reader_prev))
            .assertIsNotEnabled()
        composeRule.onNodeWithContentDescription(string(R.string.dua_reader_next))
            .assertIsEnabled()
    }

    @Test
    fun `a dua id that resolves to nothing says not found`() {
        readerState.value = DuaReaderUiState(duas = emptyList(), isLoading = false)

        setContent()

        composeRule.onNodeWithText(string(R.string.dua_reader_not_found)).assertIsDisplayed()
    }

    @Test
    fun `a load that failed says what failed rather than not found`() {
        readerState.value = DuaReaderUiState(
            duas = emptyList(),
            isLoading = false,
            error = UiError(message = R.string.dua_category_load_failed, details = "disk I/O"),
        )

        setContent()

        composeRule.onNodeWithText(string(R.string.dua_category_load_failed)).assertExists()
        composeRule.onNodeWithText(string(R.string.dua_reader_not_found)).assertDoesNotExist()
    }

    @Test
    fun `the reader shows a spinner before it has anything to read`() {
        composeRule.mainClock.autoAdvance = false
        readerState.value = DuaReaderUiState(isLoading = true)

        setContent()

        composeRule.onNodeWithText(string(R.string.dua_reader_loading)).assertExists()
        composeRule.onNodeWithText(string(R.string.dua_reader_not_found)).assertDoesNotExist()
    }

    @Test
    fun `dua settings are reachable from the reader`() {
        loaded(dua())

        setContent()
        composeRule.onNodeWithContentDescription(string(R.string.dua_settings)).performClick()

        assertThat(settingsOpened).isEqualTo(1)
    }
}
