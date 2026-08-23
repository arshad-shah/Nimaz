package com.arshadshah.nimaz.presentation.screens.quran

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.AudioState
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.MushafLine
import com.arshadshah.nimaz.domain.model.MushafLineType
import com.arshadshah.nimaz.domain.model.MushafPageLayout
import com.arshadshah.nimaz.domain.model.MushafPagination
import com.arshadshah.nimaz.domain.model.MushafScript
import com.arshadshah.nimaz.domain.model.MushafWord
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.domain.model.SurahWithAyahs
import com.arshadshah.nimaz.presentation.screens.str
import com.arshadshah.nimaz.presentation.viewmodel.quran.QuranBookmarksUiState
import com.arshadshah.nimaz.presentation.viewmodel.quran.QuranEvent
import com.arshadshah.nimaz.presentation.viewmodel.quran.QuranHomeUiState
import com.arshadshah.nimaz.presentation.viewmodel.quran.QuranReaderUiState
import com.arshadshah.nimaz.presentation.viewmodel.quran.QuranViewModel
import com.arshadshah.nimaz.presentation.viewmodel.quran.ReadingMode
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
 * The reader's other half: the Mushaf page, and the two ways a page is drawn.
 *
 * A Madani page is *flowed* — the verses on it are laid out by the text engine. An IndoPak page
 * is **reproduced line for line** from stored layout data, because the whole point of those
 * editions is that a reader who has memorised a page can find the word they are looking for in
 * the place it is printed. The two read entirely different fields of the same state —
 * `pageCache` for one, `mushafPageLayoutCache` for the other — and picking the wrong one
 * renders a blank frame rather than raising anything.
 *
 * `QuranReaderScreenRenderTest` covers the list reader and the app bar.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class QuranReaderPageModeTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val readerState = MutableStateFlow(QuranReaderUiState(isLoading = false))
    private val bookmarksState = MutableStateFlow(QuranBookmarksUiState())
    private val audioState = MutableStateFlow(AudioState())
    private val homeState = MutableStateFlow(QuranHomeUiState())
    private val events = mutableListOf<QuranEvent>()

    private val viewModel: QuranViewModel = mockk(relaxed = true) {
        every { readerState } returns this@QuranReaderPageModeTest.readerState
        every { bookmarksState } returns this@QuranReaderPageModeTest.bookmarksState
        every { audioState } returns this@QuranReaderPageModeTest.audioState
        every { homeState } returns this@QuranReaderPageModeTest.homeState
        every { onEvent(any()) } answers { events += firstArg<QuranEvent>() }
    }

    private val cave = Surah(
        number = 18,
        nameArabic = "الكهف",
        nameEnglish = "The Cave",
        nameTransliteration = "Al-Kahf",
        revelationType = RevelationType.MECCAN,
        ayahCount = 110,
        orderInMushaf = 18,
        startPage = 293,
    )

    private fun ayah(number: Int, page: Int = 293) = Ayah(
        id = 18_000 + number,
        surahNumber = 18,
        ayahNumber = number,
        textArabic = "نص الآية $number",
        textSimple = "nass $number",
        juzNumber = 15,
        hizbNumber = 29,
        rubNumber = 0,
        pageNumber = page,
        sajdaType = null,
        sajdaNumber = null,
        translation = "verse $number translated",
    )

    private fun word(number: Int, position: Int) = MushafWord(
        text = "كلمة$position",
        ayahId = 18_000 + number,
        ayahNumber = number,
        position = position,
    )

    private fun layout(page: Int) = MushafPageLayout(
        page = page,
        lines = listOf(
            MushafLine(page, lineNumber = 1, type = MushafLineType.SURAH_HEADER, surahId = 18),
            MushafLine(page, lineNumber = 2, type = MushafLineType.BASMALAH, surahId = 18),
            MushafLine(
                page, lineNumber = 3, type = MushafLineType.AYAH, surahId = 18,
                words = listOf(word(1, 1), word(1, 2)),
            ),
            MushafLine(
                page, lineNumber = 4, type = MushafLineType.AYAH, surahId = 18,
                words = listOf(word(2, 1)),
            ),
        ),
    )

    private fun render(pageNumber: Int? = 293, onPageModeChanged: (Boolean) -> Unit = {}) {
        composeRule.setThemedContent {
            QuranReaderScreen(
                surahNumber = null,
                pageNumber = pageNumber,
                onNavigateBack = {},
                onPageModeChanged = onPageModeChanged,
                viewModel = viewModel,
            )
        }
    }

    // ---- A flowed page ----

    @Test
    fun `a Madani page draws the verses cached for it`() {
        readerState.value = QuranReaderUiState(
            isLoading = false,
            readingMode = ReadingMode.PAGE,
            mushafScript = MushafScript.MADANI,
            ayahs = listOf(ayah(1), ayah(2)),
            pageCache = mapOf(293 to listOf(ayah(1), ayah(2))),
        )

        render()

        composeRule.onNodeWithText("نص الآية 1", substring = true).assertIsDisplayed()
    }

    @Test
    fun `opening in page mode tells the caller, so a tablet can drop its side pane`() {
        readerState.value = QuranReaderUiState(
            isLoading = false,
            readingMode = ReadingMode.PAGE,
            mushafScript = MushafScript.MADANI,
            pageCache = mapOf(293 to listOf(ayah(1))),
        )
        var pageMode: Boolean? = null

        render(onPageModeChanged = { pageMode = it })

        assertThat(pageMode).isTrue()
    }

    @Test
    fun `a page with nothing cached for it yet asks for it`() {
        readerState.value = QuranReaderUiState(
            isLoading = false,
            readingMode = ReadingMode.PAGE,
            mushafScript = MushafScript.MADANI,
            pageCache = emptyMap(),
        )

        render(pageNumber = 293)

        assertThat(events).contains(QuranEvent.LoadPage(293))
    }

    // ---- A line-accurate page ----

    @Test
    fun `an IndoPak page is reproduced from its stored layout`() {
        readerState.value = QuranReaderUiState(
            isLoading = false,
            readingMode = ReadingMode.PAGE,
            mushafScript = MushafScript.INDOPAK_16,
            mushafPageLayoutCache = mapOf(293 to layout(293)),
        )

        render()

        // The words are the page: a reader who has memorised it finds them where they are
        // printed, which is the entire reason these editions exist.
        // The words are the page: a reader who has memorised it finds them where they are
        // printed, which is the entire reason these editions exist. Twice, because the layout
        // puts the same word on two of its lines.
        composeRule.onAllNodes(hasText("كلمة1", substring = true)).onFirst().assertIsDisplayed()
    }

    @Test
    fun `a line-accurate edition does not fall back to the flowed cache`() {
        // The two caches are keyed the same way and hold different things. Reading the wrong
        // one renders a page from an unrelated pagination — 604 pages against 548.
        readerState.value = QuranReaderUiState(
            isLoading = false,
            readingMode = ReadingMode.PAGE,
            mushafScript = MushafScript.INDOPAK_16,
            mushafPageLayoutCache = mapOf(293 to layout(293)),
            pageCache = mapOf(293 to listOf(ayah(99))),
        )

        render()

        composeRule.onNodeWithText("verse 99 translated").assertDoesNotExist()
    }

    @Test
    fun `a line-accurate page that has not arrived asks for its layout`() {
        readerState.value = QuranReaderUiState(
            isLoading = false,
            readingMode = ReadingMode.PAGE,
            mushafScript = MushafScript.INDOPAK_16,
            mushafPageLayoutCache = emptyMap(),
        )

        render(pageNumber = 293)

        assertThat(events.filterIsInstance<QuranEvent.LoadMushafPageLayout>()).isNotEmpty()
    }

    @Test
    fun `the reader's bounds come from the active edition, not from the enum's default`() {
        // Madani is 604 pages and IndoPak-16 is 548. A pager bounded by the wrong one lets the
        // reader swipe past the end of the book they are actually reading.
        readerState.value = QuranReaderUiState(
            isLoading = false,
            readingMode = ReadingMode.PAGE,
            mushafScript = MushafScript.INDOPAK_16,
            mushafPageLayoutCache = mapOf(548 to layout(548)),
            pagination = MushafPagination.fallback(MushafScript.INDOPAK_16),
        )

        render(pageNumber = 548)

        // Read off the pagination the edition resolved, not off the enum: the two agree today
        // and the pagination is the one that reflows when the setting changes.
        assertThat(readerState.value.totalPages).isEqualTo(548)
        composeRule.onAllNodes(hasText("كلمة1", substring = true)).onFirst().assertIsDisplayed()
    }

    // ---- Switching between the two readers ----

    @Test
    fun `a surah reader can be switched into the mushaf`() {
        readerState.value = QuranReaderUiState(
            isLoading = false,
            readingMode = ReadingMode.SURAH,
            surahWithAyahs = SurahWithAyahs(cave, listOf(ayah(1), ayah(2))),
            mushafScript = MushafScript.MADANI,
            pageCache = mapOf(293 to listOf(ayah(1))),
        )
        var pageMode: Boolean? = null
        composeRule.setThemedContent {
            QuranReaderScreen(
                surahNumber = 18,
                onNavigateBack = {},
                onPageModeChanged = { pageMode = it },
                viewModel = viewModel,
            )
        }

        composeRule.onNodeWithContentDescription(str(R.string.reader_mode)).performClick()

        // Both readers are offered, and the one already showing is the translation list.
        composeRule.onNodeWithText(str(R.string.reader_mode_mushaf)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.reader_mode_translation)).assertIsDisplayed()
        assertThat(pageMode).isFalse()
    }

    @Test
    fun `a page reader is not offered a reading-mode switch`() {
        // Arriving *on* a page is already the mushaf; offering to switch to it is a control
        // that cannot do anything.
        readerState.value = QuranReaderUiState(
            isLoading = false,
            readingMode = ReadingMode.PAGE,
            mushafScript = MushafScript.MADANI,
            pageCache = mapOf(293 to listOf(ayah(1))),
        )

        render()

        composeRule.onNodeWithContentDescription(str(R.string.reader_mode)).assertDoesNotExist()
    }
}
