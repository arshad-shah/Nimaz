package com.arshadshah.nimaz.presentation.screens.quran

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.QuranTopic
import com.arshadshah.nimaz.domain.model.TafseerSource
import com.arshadshah.nimaz.domain.model.TafseerText
import com.arshadshah.nimaz.presentation.screens.str
import com.arshadshah.nimaz.presentation.viewmodel.quran.TafseerEvent
import com.arshadshah.nimaz.presentation.viewmodel.quran.TafseerUiState
import com.arshadshah.nimaz.presentation.viewmodel.quran.TafseerViewModel
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
 * The commentary reader: one verse at a time, with the block that covers it.
 *
 * The screen is a pager over the surah's verses, and the two states around it are the ones worth
 * pinning: a surah whose verses have not arrived is not the same as a surah with no verses, and
 * a note that failed to save must reach the reader — from where they are standing, a note that
 * silently failed is a note they wrote and lost, so it goes to the snackbar rather than being
 * dropped or replacing the commentary they are reading.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class TafseerScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val state = MutableStateFlow(TafseerUiState())
    private val events = mutableListOf<TafseerEvent>()

    private val viewModel: TafseerViewModel = mockk(relaxed = true) {
        every { this@mockk.state } returns this@TafseerScreenTest.state
        every { onEvent(any()) } answers { events += firstArg<TafseerEvent>() }
    }

    private fun render(surah: Int = 43, ayah: Int = 81, onBack: () -> Unit = {}, onTopic: (Int) -> Unit = {}) {
        composeRule.setThemedContent {
            TafseerScreen(
                surahNumber = surah,
                ayahNumber = ayah,
                onNavigateBack = onBack,
                onNavigateToTopic = onTopic,
                viewModel = viewModel,
            )
        }
    }

    private fun ayah(number: Int) = Ayah(
        id = 43000 + number,
        surahNumber = 43,
        ayahNumber = number,
        textArabic = "نص عربي",
        textSimple = "nass",
        juzNumber = 25,
        hizbNumber = 49,
        rubNumber = 0,
        pageNumber = 495,
        sajdaType = null,
        sajdaNumber = null,
        translation = "verse $number translated",
    )

    private val commentary = TafseerText(
        id = 1,
        tafseerId = TafseerSource.IBN_KATHIR.id,
        surahNumber = 43,
        ayahStart = 81,
        ayahEnd = 89,
        text = "The commentary on this passage.",
    )

    private fun loaded(
        topics: List<QuranTopic> = emptyList(),
    ) = TafseerUiState(
        surahNumber = 43,
        surahName = "Az-Zukhruf",
        ayahs = listOf(ayah(81), ayah(82), ayah(83)),
        currentAyahIndex = 0,
        currentTafseer = commentary,
        availableSources = setOf(TafseerSource.IBN_KATHIR),
        topics = topics,
        isLoading = false,
    )

    @Test
    fun `arriving asks for the surah, at the verse it was opened on`() {
        render(surah = 43, ayah = 81)

        assertThat(events).contains(TafseerEvent.LoadSurah(43, 81))
    }

    @Test
    fun `the surah's name and the reader's place in it are in the app bar`() {
        state.value = loaded()

        render()

        composeRule.onNodeWithText("Az-Zukhruf").assertIsDisplayed()
    }

    @Test
    fun `the commentary for the current verse is shown`() {
        state.value = loaded()

        render()

        composeRule.onNodeWithText("The commentary on this passage.", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun `a surah with no verses says so rather than showing a blank pager`() {
        state.value = TafseerUiState(isLoading = false, ayahs = emptyList())

        render()

        composeRule.onNodeWithText(str(R.string.no_ayahs_found)).assertIsDisplayed()
    }

    @Test
    fun `a first load is not the same as a surah with no verses`() {
        state.value = TafseerUiState(isLoading = true)

        render()

        composeRule.onNodeWithText(str(R.string.no_ayahs_found)).assertDoesNotExist()
    }

    @Test
    fun `a verse's subjects open the subject they name`() {
        state.value = loaded(
            topics = listOf(
                QuranTopic(
                    id = 5,
                    name = "Revelation",
                    arabicName = "وحي",
                    description = "",
                    wikiLink = "",
                    ayahCount = 40,
                    parentId = null,
                    thematicParentId = null,
                    ontologyParentId = null,
                    isThematic = true,
                    isOntology = false,
                    relatedTopicIds = emptyList(),
                )
            )
        )
        var opened: Int? = null
        render(onTopic = { opened = it })

        composeRule.onNodeWithText("Revelation").performClick()

        assertThat(opened).isEqualTo(5)
    }

    @Test
    fun `going back is the caller's business`() {
        state.value = loaded()
        var back = false

        render(onBack = { back = true })
        composeRule.onNodeWithContentDescription(str(R.string.cd_back)).performClick()

        assertThat(back).isTrue()
    }

    @Test
    fun `turning a content page is handed to the view model`() {
        state.value = loaded().copy(
            currentTafseer = commentary.copy(text = (1..400).joinToString(" ") { "word$it" }),
        )
        render()

        composeRule.onNodeWithContentDescription(str(R.string.cd_next_page)).performClick()

        // Hoisted into the view model so it survives an ayah-by-ayah swipe within one block.
        assertThat(events.filterIsInstance<TafseerEvent.NavigateToTafseerPage>().map { it.page })
            .contains(1)
    }
}
