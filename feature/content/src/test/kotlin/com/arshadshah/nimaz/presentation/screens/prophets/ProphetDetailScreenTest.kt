package com.arshadshah.nimaz.presentation.screens.prophets

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.Prophet
import com.arshadshah.nimaz.presentation.screens.names.prophet
import com.arshadshah.nimaz.presentation.viewmodel.content.CatalogDetailState
import com.arshadshah.nimaz.presentation.viewmodel.content.CatalogEvent
import com.arshadshah.nimaz.presentation.viewmodel.content.ProphetViewModel
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
 * One prophet's page — the only catalogue detail screen that does **not** use the shared shell.
 *
 * It has its own scaffold because its sections are not prose cards: a bullet list appears twice,
 * the Qur'an mentions are chips, and the timeline is a two-by-two grid of label/value pairs. That
 * makes it the largest single file in the module and the one place where the shell's guarantees
 * have to be re-established by hand — the loading branch, the null-item branch, the FAB that
 * appears only when there is something to favourite.
 *
 * **Three of its six sections are conditional**, and the content artifact is what decides: a
 * prophet the dataset records no miracles for, no key lessons for, or no cited verses for must
 * render *no card* rather than a titled card with nothing under it. That is the failure mode
 * this whole module shares — shipped content that does not carry a field, rendering as a hole.
 *
 * **The timeline always renders, with four values that are all plain strings.** Era, lineage,
 * years lived and place of preaching are four `TimelineItem`s laid out in two rows, which is
 * exactly the shape where two labels get swapped and nothing complains.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class ProphetDetailScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val detailState = MutableStateFlow(CatalogDetailState<Prophet>())
    private val events = mutableListOf<CatalogEvent>()

    private val viewModel: ProphetViewModel = mockk(relaxed = true) {
        every { this@mockk.detailState } returns this@ProphetDetailScreenTest.detailState
        every { onEvent(any()) } answers { events += firstArg<CatalogEvent>() }
    }

    private var backs = 0

    private fun setContent(prophetId: Int = 1) {
        composeRule.setThemedContent {
            ProphetDetailScreen(
                prophetId = prophetId,
                onNavigateBack = { backs++ },
                viewModel = viewModel,
            )
        }
    }

    private fun string(@StringRes res: Int): String = context.getString(res)

    private fun loaded(item: Prophet) {
        detailState.value = CatalogDetailState(isLoading = false, item = item)
    }

    @Test
    fun `opening a prophet asks for the id the route carried`() {
        loaded(prophet(1))

        setContent(prophetId = 25)

        assertThat(events).containsExactly(CatalogEvent.LoadDetail(25))
    }

    @Test
    fun `the header leads with the English name and the title beneath it`() {
        // No number medallion for prophets — the header is handed `number = null`, the one
        // place in the app that path is taken.
        loaded(prophet(nameArabic = "إبراهيم", nameEnglish = "Abraham", titleEnglish = "Friend of Allah"))

        setContent()

        composeRule.onNodeWithText("إبراهيم").assertIsDisplayed()
        composeRule.onNodeWithText("Friend of Allah").assertExists()
    }

    @Test
    fun `the story is always shown`() {
        loaded(prophet(storySummary = "Called his people away from idols"))

        setContent()

        composeRule.onNodeWithText(string(R.string.prophets_story)).assertExists()
        composeRule.onNodeWithText("Called his people away from idols").assertExists()
    }

    @Test
    fun `the key lessons are listed one per bullet`() {
        loaded(prophet(keyLessons = listOf("Tawhid before all", "Patience under trial")))

        setContent()

        composeRule.onNodeWithText(string(R.string.prophets_key_lessons)).assertExists()
        composeRule.onNodeWithText("Tawhid before all").assertExists()
        composeRule.onNodeWithText("Patience under trial").assertExists()
    }

    @Test
    fun `a prophet the dataset records no lessons for shows no lessons card`() {
        loaded(prophet(keyLessons = emptyList()))

        setContent()

        composeRule.onNodeWithText(string(R.string.prophets_key_lessons)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.prophets_story)).assertExists()
    }

    @Test
    fun `the cited verses are chips, and absent when the dataset cites none`() {
        loaded(prophet(quranMentions = listOf("2:124", "6:74")))

        setContent()

        composeRule.onNodeWithText(string(R.string.prophets_quran_mentions)).assertExists()
        composeRule.onNodeWithText("2:124").assertExists()
        composeRule.onNodeWithText("6:74").assertExists()
    }

    @Test
    fun `no cited verses means no mentions card at all`() {
        loaded(prophet(quranMentions = emptyList()))

        setContent()

        composeRule.onNodeWithText(string(R.string.prophets_quran_mentions)).assertDoesNotExist()
    }

    @Test
    fun `the miracles card appears only for a prophet the dataset records some for`() {
        loaded(prophet(miracles = emptyList()))

        setContent()

        composeRule.onNodeWithText(string(R.string.prophets_miracles)).assertDoesNotExist()
    }

    @Test
    fun `recorded miracles are listed as bullets of their own`() {
        loaded(prophet(miracles = listOf("Unburned by the fire", "The birds returned to him")))

        setContent()

        composeRule.onNodeWithText(string(R.string.prophets_miracles)).assertExists()
        composeRule.onNodeWithText("Unburned by the fire").assertExists()
        composeRule.onNodeWithText("The birds returned to him").assertExists()
    }

    @Test
    fun `each timeline value sits under its own label`() {
        // Four label/value pairs in two rows: the shape where two get swapped and the page
        // still reads as a perfectly well-formed timeline.
        loaded(
            prophet(
                era = "circa 2000 BCE",
                lineage = "Son of Azar",
                yearsLived = "175 years",
                placeOfPreaching = "Ur and Canaan",
            )
        )

        setContent()

        composeRule.onNodeWithText(string(R.string.prophets_timeline)).assertExists()
        composeRule.onNodeWithText(string(R.string.prophets_era)).assertExists()
        composeRule.onNodeWithText("circa 2000 BCE").assertExists()
        composeRule.onNodeWithText(string(R.string.prophets_lineage)).assertExists()
        composeRule.onNodeWithText("Son of Azar").assertExists()
        composeRule.onNodeWithText(string(R.string.prophets_years_lived)).assertExists()
        composeRule.onNodeWithText("175 years").assertExists()
        composeRule.onNodeWithText(string(R.string.prophets_place)).assertExists()
        composeRule.onNodeWithText("Ur and Canaan").assertExists()
    }

    @Test
    fun `starring a prophet dispatches that prophet's id`() {
        loaded(prophet(id = 4, nameEnglish = "Moses", isFavorite = false))

        setContent(prophetId = 4)
        events.clear()
        composeRule.onNodeWithContentDescription(string(R.string.add_to_favorites)).performClick()

        assertThat(events).containsExactly(CatalogEvent.ToggleFavorite(4))
    }

    @Test
    fun `an already-starred prophet offers to remove`() {
        loaded(prophet(id = 4, isFavorite = true))

        setContent(prophetId = 4)

        composeRule.onNodeWithContentDescription(string(R.string.remove_from_favorites))
            .assertExists()
    }

    @Test
    fun `the bar carries a generic title until the prophet arrives, and offers nothing to star`() {
        composeRule.mainClock.autoAdvance = false
        detailState.value = CatalogDetailState(isLoading = true)

        setContent()

        composeRule.onNodeWithText(string(R.string.prophet_detail)).assertExists()
        composeRule.onNodeWithContentDescription(string(R.string.add_to_favorites))
            .assertDoesNotExist()
    }

    @Test
    fun `an id that resolves to nothing does not render a page of nulls`() {
        // `isLoading = false` with no item — a stale deep link, or a prophet the content
        // artifact no longer ships. This screen re-establishes the shared shell's guard by
        // hand, so it is worth asserting here separately.
        composeRule.mainClock.autoAdvance = false
        detailState.value = CatalogDetailState(isLoading = false, item = null)

        setContent(prophetId = 999)

        composeRule.onNodeWithText(string(R.string.prophets_story)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.prophets_timeline)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.prophet_detail)).assertExists()
    }
}
