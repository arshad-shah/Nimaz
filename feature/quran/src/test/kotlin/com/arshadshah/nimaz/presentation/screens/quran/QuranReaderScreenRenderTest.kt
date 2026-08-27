package com.arshadshah.nimaz.presentation.screens.quran

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.AudioState
import com.arshadshah.nimaz.domain.model.AyahTheme
import com.arshadshah.nimaz.domain.model.MushafScript
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.domain.model.SurahWithAyahs
import com.arshadshah.nimaz.presentation.screens.str
import com.arshadshah.nimaz.presentation.viewmodel.quran.QuranBookmarksUiState
import com.arshadshah.nimaz.presentation.viewmodel.quran.QuranEvent
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
 * The reader itself — the screen a reader spends their time in, and the one with the most ways
 * to be opened.
 *
 * It is really three readers behind one entry point: a surah, a juz, or a page. Which one is
 * decided by *which argument was not null*, and the three take different content out of the
 * state — `surahWithAyahs` for a surah, `ayahs` for the other two. Getting that wrong renders an
 * empty page rather than an error, which is why each mode is opened here and asked what it
 * loaded and what it put in its title.
 *
 * The app bar's conditional actions are the other half: the passages entry exists only where the
 * artifact carries a passage outline, and the subject index points at *this* surah when there is
 * one on screen and at the whole index when there is not — which in page and juz mode, before a
 * layout has resolved a surah, is the only honest answer.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class QuranReaderScreenRenderTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val readerState = MutableStateFlow(QuranReaderUiState(isLoading = false))
    private val bookmarksState = MutableStateFlow(QuranBookmarksUiState())
    private val audioState = MutableStateFlow(AudioState())
    private val homeState = MutableStateFlow(com.arshadshah.nimaz.presentation.viewmodel.quran.QuranHomeUiState())
    private val events = mutableListOf<QuranEvent>()

    private val viewModel: QuranViewModel = mockk(relaxed = true) {
        every { readerState } returns this@QuranReaderScreenRenderTest.readerState
        every { bookmarksState } returns this@QuranReaderScreenRenderTest.bookmarksState
        every { audioState } returns this@QuranReaderScreenRenderTest.audioState
        every { homeState } returns this@QuranReaderScreenRenderTest.homeState
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

    private fun ayah(number: Int, surah: Int = 18) = Ayah(
        id = surah * 1000 + number,
        surahNumber = surah,
        ayahNumber = number,
        textArabic = "نص الآية $number",
        textSimple = "nass $number",
        juzNumber = 15,
        hizbNumber = 29,
        rubNumber = 0,
        pageNumber = 293,
        sajdaType = null,
        sajdaNumber = null,
        translation = "verse $number translated",
    )

    private val verses = (1..3).map { ayah(it) }

    private fun render(
        surahNumber: Int? = 18,
        juzNumber: Int? = null,
        pageNumber: Int? = null,
        onBack: () -> Unit = {},
        onSubjects: (Int?) -> Unit = {},
        onPassages: (Int, Int) -> Unit = { _, _ -> },
        onSettings: () -> Unit = {},
    ) {
        composeRule.setThemedContent {
            QuranReaderScreen(
                surahNumber = surahNumber,
                juzNumber = juzNumber,
                pageNumber = pageNumber,
                onNavigateBack = onBack,
                onNavigateToQuranSettings = onSettings,
                onNavigateToPassages = onPassages,
                onNavigateToSubjects = onSubjects,
                viewModel = viewModel,
            )
        }
    }

    // ---- Which reader was opened ----

    @Test
    fun `opening a surah loads that surah`() {
        readerState.value = QuranReaderUiState(
            isLoading = false,
            readingMode = ReadingMode.SURAH,
            surahWithAyahs = SurahWithAyahs(cave, verses),
        )

        render(surahNumber = 18)

        assertThat(events).contains(QuranEvent.LoadSurah(18))
    }

    @Test
    fun `opening a juz loads the juz, not a surah`() {
        readerState.value = QuranReaderUiState(
            isLoading = false,
            readingMode = ReadingMode.JUZ,
            ayahs = verses,
            title = "Juz 15",
        )

        render(surahNumber = null, juzNumber = 15)

        assertThat(events).contains(QuranEvent.LoadJuz(15))
        assertThat(events.filterIsInstance<QuranEvent.LoadSurah>()).isEmpty()
    }

    @Test
    fun `opening a page loads the page`() {
        readerState.value = QuranReaderUiState(
            isLoading = false,
            readingMode = ReadingMode.PAGE,
            ayahs = verses,
            mushafScript = MushafScript.MADANI,
        )

        render(surahNumber = null, pageNumber = 293)

        assertThat(events).contains(QuranEvent.LoadPage(293))
    }

    // ---- What it puts in the header ----

    @Test
    fun `a surah reader is titled with the surah`() {
        readerState.value = QuranReaderUiState(
            isLoading = false,
            readingMode = ReadingMode.SURAH,
            surahWithAyahs = SurahWithAyahs(cave, verses),
            subtitle = "Meccan · 110 verses",
        )

        render(surahNumber = 18)

        // Twice on purpose: the app bar names it, and the surah heading above verse 1 repeats it.
        composeRule.onAllNodes(hasText("The Cave")).assertCountEquals(2)
    }

    @Test
    fun `a juz reader is titled with the juz`() {
        readerState.value = QuranReaderUiState(
            isLoading = false,
            readingMode = ReadingMode.JUZ,
            ayahs = verses,
            title = "Juz 15",
        )

        render(surahNumber = null, juzNumber = 15)

        composeRule.onNodeWithText("Juz 15").assertIsDisplayed()
    }

    // ---- The verses ----

    @Test
    fun `a surah's verses are rendered with their translations`() {
        readerState.value = QuranReaderUiState(
            isLoading = false,
            readingMode = ReadingMode.SURAH,
            surahWithAyahs = SurahWithAyahs(cave, verses),
            showTranslation = true,
        )

        render(surahNumber = 18)

        composeRule.onNodeWithText("verse 1 translated").assertIsDisplayed()
    }

    @Test
    fun `a juz reader takes its verses from the flat list, not from a surah`() {
        // The two modes read different fields of the same state; crossing them renders nothing
        // and reports nothing.
        readerState.value = QuranReaderUiState(
            isLoading = false,
            readingMode = ReadingMode.JUZ,
            ayahs = verses,
            surahWithAyahs = null,
            title = "Juz 15",
            showTranslation = true,
        )

        render(surahNumber = null, juzNumber = 15)

        composeRule.onNodeWithText("verse 2 translated").assertIsDisplayed()
    }

    @Test
    fun `translations are left out when the reader has turned them off`() {
        readerState.value = QuranReaderUiState(
            isLoading = false,
            readingMode = ReadingMode.SURAH,
            surahWithAyahs = SurahWithAyahs(cave, verses),
            showTranslation = false,
        )

        render(surahNumber = 18)

        composeRule.onNodeWithText("verse 1 translated").assertDoesNotExist()
    }

    // ---- The app bar ----

    @Test
    fun `the subject index points at this surah when there is one on screen`() {
        readerState.value = QuranReaderUiState(
            isLoading = false,
            readingMode = ReadingMode.SURAH,
            surahWithAyahs = SurahWithAyahs(cave, verses),
        )
        var subjectsFor: Int? = -1
        render(surahNumber = 18, onSubjects = { subjectsFor = it })

        composeRule.onNodeWithContentDescription(str(R.string.cd_more_options)).performClick()
        composeRule.onNodeWithText(str(R.string.surah_info_subjects)).performClick()

        assertThat(subjectsFor).isEqualTo(18)
    }

    @Test
    fun `with no surah resolved the subject index is the whole index`() {
        readerState.value = QuranReaderUiState(
            isLoading = false,
            readingMode = ReadingMode.PAGE,
            ayahs = emptyList(),
        )
        var subjectsFor: Int? = -1
        render(surahNumber = null, pageNumber = 293, onSubjects = { subjectsFor = it })

        composeRule.onNodeWithContentDescription(str(R.string.cd_more_options)).performClick()
        composeRule.onNodeWithText(str(R.string.quran_topics_title)).performClick()

        assertThat(subjectsFor).isNull()
    }

    @Test
    fun `passages are offered only where this surah has an outline`() {
        readerState.value = QuranReaderUiState(
            isLoading = false,
            readingMode = ReadingMode.SURAH,
            surahWithAyahs = SurahWithAyahs(cave, verses),
            passages = emptyList(),
        )

        render(surahNumber = 18)
        composeRule.onNodeWithContentDescription(str(R.string.cd_more_options)).performClick()

        composeRule.onNodeWithText(str(R.string.surah_info_passages)).assertDoesNotExist()
    }

    @Test
    fun `a surah with an outline offers it, and opens it at the verse being read`() {
        readerState.value = QuranReaderUiState(
            isLoading = false,
            readingMode = ReadingMode.SURAH,
            surahWithAyahs = SurahWithAyahs(cave, verses),
            passages = listOf(
                AyahTheme(surahNumber = 18, ayahFrom = 1, ayahTo = 8, theme = "Purpose", ayahCount = 8),
            ),
        )
        var passagesFor: Pair<Int, Int>? = null
        render(surahNumber = 18, onPassages = { s, a -> passagesFor = s to a })

        composeRule.onNodeWithContentDescription(str(R.string.cd_more_options)).performClick()
        composeRule.onNodeWithText(str(R.string.surah_info_passages)).performClick()

        assertThat(passagesFor?.first).isEqualTo(18)
    }

    @Test
    fun `the reader's settings are reachable from the overflow`() {
        readerState.value = QuranReaderUiState(
            isLoading = false,
            readingMode = ReadingMode.SURAH,
            surahWithAyahs = SurahWithAyahs(cave, verses),
        )
        var settings = false
        render(surahNumber = 18, onSettings = { settings = true })

        composeRule.onNodeWithContentDescription(str(R.string.cd_more_options)).performClick()
        composeRule.onNodeWithText(str(R.string.cd_settings)).performClick()

        assertThat(settings).isTrue()
    }

    @Test
    fun `the tajweed guide is offered only when tajweed colouring is on`() {
        readerState.value = QuranReaderUiState(
            isLoading = false,
            readingMode = ReadingMode.SURAH,
            surahWithAyahs = SurahWithAyahs(cave, verses),
            showTajweed = false,
        )

        render(surahNumber = 18)
        composeRule.onNodeWithContentDescription(str(R.string.cd_more_options)).performClick()

        // A colour key for colours that are not on the page.
        composeRule.onNodeWithText(str(R.string.tajweed_colour_guide)).assertDoesNotExist()
    }

    @Test
    fun `going back is the caller's business`() {
        readerState.value = QuranReaderUiState(
            isLoading = false,
            readingMode = ReadingMode.SURAH,
            surahWithAyahs = SurahWithAyahs(cave, verses),
        )
        var back = false

        render(surahNumber = 18, onBack = { back = true })
        composeRule.onNodeWithContentDescription(str(R.string.cd_back)).performClick()

        assertThat(back).isTrue()
    }

}
