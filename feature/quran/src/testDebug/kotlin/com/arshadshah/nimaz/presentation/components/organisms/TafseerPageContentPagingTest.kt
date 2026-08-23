package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.QuranTopic
import com.arshadshah.nimaz.domain.model.TafseerHighlight
import com.arshadshah.nimaz.domain.model.TafseerSource
import com.arshadshah.nimaz.domain.model.TafseerText
import com.arshadshah.nimaz.presentation.screens.str
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Paging a commentary block, and the things that hang off the page it is on.
 *
 * A block is not a page: Ibn Kathir on 43:81–89 is one passage, split into readable pages here
 * rather than in the database. Three things follow, and each has been wrong at some point — the
 * page number has to survive an ayah-by-ayah swipe *within* the same block (kept locally, every
 * swipe reopened the passage at page 1), the range label has to say what the block actually
 * covers rather than the ayah on screen, and the verse and its subject chips belong to the
 * *verse*, so they appear on the first content page only.
 *
 * `TafseerPageContentTest` covers the source chips and the empty state; this is the rest.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class TafseerPageContentPagingTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private var contentPage: Int? = null
    private var shared = false

    private fun ayah(number: Int = 81, surah: Int = 43, translation: String? = "a translation") =
        Ayah(
            id = surah * 1000 + number,
            surahNumber = surah,
            ayahNumber = number,
            textArabic = "نص عربي",
            textSimple = "nass arabi",
            juzNumber = 25,
            hizbNumber = 49,
            rubNumber = 0,
            pageNumber = 495,
            sajdaType = null,
            sajdaNumber = null,
            translation = translation,
        )

    /** Long enough that the splitter has to make more than one page of it. */
    private fun longCommentary(ayahStart: Int = 81, ayahEnd: Int = 89) = TafseerText(
        id = 1,
        tafseerId = TafseerSource.IBN_KATHIR.id,
        surahNumber = 43,
        ayahStart = ayahStart,
        ayahEnd = ayahEnd,
        text = (1..400).joinToString(" ") { "word$it" },
    )

    private fun render(
        tafseer: TafseerText? = longCommentary(),
        currentContentPage: Int = 0,
        highlights: List<TafseerHighlight> = emptyList(),
        topics: List<QuranTopic> = emptyList(),
    ) {
        composeRule.setThemedContent {
            TafseerPageContent(
                ayah = ayah(),
                tafseer = tafseer,
                highlights = highlights,
                selectedSource = TafseerSource.IBN_KATHIR,
                availableSources = setOf(TafseerSource.IBN_KATHIR),
                currentContentPage = currentContentPage,
                onContentPageChanged = { contentPage = it },
                onSourceSwitch = {},
                onHighlightCreated = { _, _, _, _ -> },
                onHighlightUpdated = { _, _, _ -> },
                onHighlightDeleted = {},
                onShare = { shared = true },
                topics = topics,
            )
        }
    }

    // ---- What the block says it covers ----

    @Test
    fun `a block spanning several verses says so, not just the verse on screen`() {
        render(tafseer = longCommentary(ayahStart = 81, ayahEnd = 89))

        composeRule.onNodeWithText(str(R.string.tafseer_commentary_range_span, 43, 81, 89))
            .assertIsDisplayed()
    }

    @Test
    fun `a block covering one verse says that instead`() {
        render(tafseer = longCommentary(ayahStart = 81, ayahEnd = 81))

        composeRule.onNodeWithText(str(R.string.tafseer_commentary_range_single, 43, 81))
            .assertIsDisplayed()
    }

    // ---- Paging ----

    @Test
    fun `turning the page asks the caller to move it, rather than moving it here`() {
        render(currentContentPage = 0)

        composeRule.onNodeWithContentDescription(str(R.string.cd_next_page)).performClick()

        assertThat(contentPage).isEqualTo(1)
    }

    @Test
    fun `going back a page asks for the page before it`() {
        render(currentContentPage = 1)

        composeRule.onNodeWithContentDescription(str(R.string.cd_previous_page)).performClick()

        assertThat(contentPage).isEqualTo(0)
    }

    @Test
    fun `the first page cannot go back`() {
        render(currentContentPage = 0)

        composeRule.onNodeWithContentDescription(str(R.string.cd_previous_page)).performClick()

        assertThat(contentPage).isNull()
    }

    @Test
    fun `a content page beyond the end is clamped rather than left blank`() {
        // The page is hoisted, so a shorter block can arrive under a page number left over from
        // a longer one. An unclamped index there is an empty reader.
        render(tafseer = longCommentary(), currentContentPage = 99)

        composeRule.onNodeWithContentDescription(str(R.string.cd_next_page)).assertIsDisplayed()
    }

    // ---- What belongs to the verse rather than to the block ----

    @Test
    fun `the verse's translation is on the first content page`() {
        render(currentContentPage = 0)

        composeRule.onNodeWithText("a translation").assertIsDisplayed()
    }

    @Test
    fun `page two is commentary, without the verse repeated above it`() {
        render(currentContentPage = 1)

        // A block can span nine verses; reprinting one of them above every page says nothing.
        composeRule.onNodeWithText("a translation").assertDoesNotExist()
    }

    @Test
    fun `a verse's subjects are offered under it, on the first page`() {
        render(currentContentPage = 0, topics = listOf(topic()))

        composeRule.onNodeWithText("Revelation").assertIsDisplayed()
    }

    @Test
    fun `the subject chips do not follow the reader onto page two`() {
        render(currentContentPage = 1, topics = listOf(topic()))

        composeRule.onNodeWithText("Revelation").assertDoesNotExist()
    }

    // ---- The bottom bar ----

    @Test
    fun `sharing the commentary is the caller's business`() {
        render()

        composeRule.onNodeWithContentDescription(str(R.string.cd_share_tafseer)).performClick()

        assertThat(shared).isTrue()
    }

    @Test
    fun `the notes sheet opens from the bottom bar`() {
        render()

        composeRule.onNodeWithContentDescription(str(R.string.cd_notes)).performClick()

        composeRule.onNodeWithText(str(R.string.tafseer_highlight_notes)).assertIsDisplayed()
    }

    @Test
    fun `a reader with no highlights is told how to make one`() {
        render(highlights = emptyList())

        composeRule.onNodeWithText(str(R.string.tafseer_highlight_hint)).assertIsDisplayed()
    }

    @Test
    fun `the hint goes once there is a highlight to look at`() {
        render(highlights = listOf(highlight()))

        composeRule.onNodeWithText(str(R.string.tafseer_highlight_hint)).assertDoesNotExist()
    }

    private fun topic() = QuranTopic(
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

    private fun highlight() = TafseerHighlight(
        id = 1,
        ayahId = 43081,
        tafseerId = TafseerSource.IBN_KATHIR.id,
        startOffset = 0,
        endOffset = 10,
        color = "yellow",
        note = "worth returning to",
        createdAt = 0,
        updatedAt = 0,
    )
}
