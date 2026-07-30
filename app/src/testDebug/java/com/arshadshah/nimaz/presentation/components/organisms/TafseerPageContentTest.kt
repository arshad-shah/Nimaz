package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.TafseerHighlight
import com.arshadshah.nimaz.domain.model.TafseerSource
import com.arshadshah.nimaz.domain.model.TafseerText
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TafseerPageContentTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private fun ayah(
        ayahNumber: Int = 1,
        translation: String? = "In the name of Allah."
    ) = Ayah(
        id = 1,
        surahNumber = 1,
        ayahNumber = ayahNumber,
        textArabic = "بِسْمِ ٱللَّهِ",
        textSimple = "بسم الله",
        juzNumber = 1,
        hizbNumber = 1,
        rubNumber = 1,
        pageNumber = 1,
        sajdaType = null,
        sajdaNumber = null,
        translation = translation,
        isBookmarked = false
    )

    private fun tafseer(text: String) = TafseerText(
        id = 1L,
        surahNumber = 1,
        ayahStart = 1,
        ayahEnd = 1,
        tafseerId = TafseerSource.IBN_KATHIR.id,
        text = text
    )

    // Note: the ayah indicator moved to the screen-level app bar (de-duplicated),
    // so it is no longer rendered by TafseerPageContent and is covered at the
    // screen level instead.

    @Test
    fun `renders both source filter chips`() {
        composeRule.setThemedContent {
            TafseerPageContent(
                ayah = ayah(),
                tafseer = tafseer("Short commentary."),
                highlights = emptyList(),
                selectedSource = TafseerSource.IBN_KATHIR,
                availableSources = setOf(
                    TafseerSource.IBN_KATHIR,
                    TafseerSource.MAARIFUL_QURAN
                ),
                currentContentPage = 0,
                onContentPageChanged = {},
                onSourceSwitch = {},
                onHighlightCreated = { _, _, _, _ -> },
                onHighlightDeleted = {},
                onHighlightUpdated = { _, _, _ -> },
                onShare = {}
            )
        }

        composeRule.onNodeWithText("Ibn Kathir").assertExists()
        composeRule.onNodeWithText("Ma'arif al-Qur'an").assertExists()
    }

    @Test
    fun `renders translation on first page`() {
        composeRule.setThemedContent {
            TafseerPageContent(
                ayah = ayah(translation = "In the name of Allah."),
                tafseer = tafseer("Short commentary."),
                highlights = emptyList(),
                selectedSource = TafseerSource.IBN_KATHIR,
                availableSources = setOf(TafseerSource.IBN_KATHIR),
                currentContentPage = 0,
                onContentPageChanged = {},
                onSourceSwitch = {},
                onHighlightCreated = { _, _, _, _ -> },
                onHighlightDeleted = {},
                onHighlightUpdated = { _, _, _ -> },
                onShare = {}
            )
        }

        composeRule.onNodeWithText("In the name of Allah.").assertExists()
    }

    @Test
    fun `renders short tafseer text`() {
        composeRule.setThemedContent {
            TafseerPageContent(
                ayah = ayah(),
                tafseer = tafseer("This is the commentary body."),
                highlights = emptyList(),
                selectedSource = TafseerSource.IBN_KATHIR,
                availableSources = setOf(TafseerSource.IBN_KATHIR),
                currentContentPage = 0,
                onContentPageChanged = {},
                onSourceSwitch = {},
                onHighlightCreated = { _, _, _, _ -> },
                onHighlightDeleted = {},
                onHighlightUpdated = { _, _, _ -> },
                onShare = {}
            )
        }

        composeRule.onNodeWithText("This is the commentary body.").assertExists()
    }

    @Test
    fun `shows empty state when tafseer is null`() {
        composeRule.setThemedContent {
            TafseerPageContent(
                ayah = ayah(),
                tafseer = null,
                highlights = emptyList(),
                selectedSource = TafseerSource.IBN_KATHIR,
                availableSources = setOf(TafseerSource.IBN_KATHIR),
                currentContentPage = 0,
                onContentPageChanged = {},
                onSourceSwitch = {},
                onHighlightCreated = { _, _, _, _ -> },
                onHighlightDeleted = {},
                onHighlightUpdated = { _, _, _ -> },
                onShare = {}
            )
        }

        composeRule.onNodeWithText("No Ibn Kathir commentary for this ayah").assertExists()
    }

    @Test
    fun `empty state offers alternate source when available`() {
        composeRule.setThemedContent {
            TafseerPageContent(
                ayah = ayah(),
                tafseer = null,
                highlights = emptyList(),
                selectedSource = TafseerSource.IBN_KATHIR,
                availableSources = setOf(
                    TafseerSource.IBN_KATHIR,
                    TafseerSource.MAARIFUL_QURAN
                ),
                currentContentPage = 0,
                onContentPageChanged = {},
                onSourceSwitch = {},
                onHighlightCreated = { _, _, _, _ -> },
                onHighlightDeleted = {},
                onHighlightUpdated = { _, _, _ -> },
                onShare = {}
            )
        }

        // Empty-state CTA button uses the alternate source's displayName
        composeRule.onNodeWithText("Read in Ma'arif al-Qur'an").assertExists()
    }

    @Test
    fun `clicking a source chip invokes onSourceSwitch`() {
        var switchedTo: TafseerSource? = null
        composeRule.setThemedContent {
            TafseerPageContent(
                ayah = ayah(),
                tafseer = tafseer("Short commentary."),
                highlights = emptyList(),
                selectedSource = TafseerSource.IBN_KATHIR,
                availableSources = setOf(
                    TafseerSource.IBN_KATHIR,
                    TafseerSource.MAARIFUL_QURAN
                ),
                currentContentPage = 0,
                onContentPageChanged = {},
                onSourceSwitch = { switchedTo = it },
                onHighlightCreated = { _, _, _, _ -> },
                onHighlightDeleted = {},
                onHighlightUpdated = { _, _, _ -> },
                onShare = {}
            )
        }

        composeRule.onNodeWithText("Ma'arif al-Qur'an").performClick()
        assertThat(switchedTo).isEqualTo(TafseerSource.MAARIFUL_QURAN)
    }

    @Test
    fun `notes control is present`() {
        composeRule.setThemedContent {
            TafseerPageContent(
                ayah = ayah(),
                tafseer = tafseer("Short commentary."),
                highlights = listOf(
                    TafseerHighlight(
                        id = 1L,
                        ayahId = 1,
                        tafseerId = TafseerSource.IBN_KATHIR.id,
                        startOffset = 0,
                        endOffset = 4,
                        color = "#FDE68A",
                        note = "my note",
                        createdAt = 0L,
                        updatedAt = 0L
                    )
                ),
                selectedSource = TafseerSource.IBN_KATHIR,
                availableSources = setOf(TafseerSource.IBN_KATHIR),
                currentContentPage = 0,
                onContentPageChanged = {},
                onSourceSwitch = {},
                onHighlightCreated = { _, _, _, _ -> },
                onHighlightDeleted = {},
                onHighlightUpdated = { _, _, _ -> },
                onShare = {}
            )
        }

        composeRule.onNodeWithContentDescription("Notes").assertExists()
    }
}
