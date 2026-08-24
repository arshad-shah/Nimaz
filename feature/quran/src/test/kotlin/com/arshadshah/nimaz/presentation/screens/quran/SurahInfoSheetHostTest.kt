package com.arshadshah.nimaz.presentation.screens.quran

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.AyahTheme
import com.arshadshah.nimaz.domain.model.MushafPagination
import com.arshadshah.nimaz.domain.model.MushafScript
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.domain.model.SurahOverview
import com.arshadshah.nimaz.presentation.screens.str
import com.arshadshah.nimaz.presentation.viewmodel.quran.QuranEvent
import com.arshadshah.nimaz.presentation.viewmodel.quran.QuranHomeUiState
import com.arshadshah.nimaz.presentation.viewmodel.quran.QuranViewModel
import com.arshadshah.nimaz.presentation.viewmodel.quran.SurahThematicEvent
import com.arshadshah.nimaz.presentation.viewmodel.quran.SurahThematicUiState
import com.arshadshah.nimaz.presentation.viewmodel.quran.SurahThematicViewModel
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
 * The seam between the surah card and the two ViewModels that fill it.
 *
 * The sheet itself is stateless and tested elsewhere; what is worth pinning here is the wiring
 * around it. It asks both ViewModels to load, it renders nothing at all until the surah it was
 * raised for is actually in the list, and it works out the surah's opening page from the
 * pagination rather than from `Surah.startPage` — which is the Madani column and names the wrong
 * page under a line-accurate edition (#325).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class SurahInfoSheetHostTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val homeState = MutableStateFlow(QuranHomeUiState())
    private val thematic = MutableStateFlow(SurahThematicUiState())

    private val quranEvents = mutableListOf<QuranEvent>()
    private val thematicEvents = mutableListOf<SurahThematicEvent>()

    private val quranViewModel: QuranViewModel = mockk(relaxed = true) {
        every { this@mockk.homeState } returns this@SurahInfoSheetHostTest.homeState
        every { onEvent(any()) } answers { quranEvents += firstArg<QuranEvent>() }
    }

    private val thematicViewModel: SurahThematicViewModel = mockk(relaxed = true) {
        every { this@mockk.thematic } returns this@SurahInfoSheetHostTest.thematic
        every { onEvent(any()) } answers { thematicEvents += firstArg<SurahThematicEvent>() }
    }

    private fun surah(number: Int, english: String, ayahCount: Int) = Surah(
        number = number,
        nameArabic = "سورة",
        nameEnglish = english,
        nameTransliteration = english,
        revelationType = RevelationType.MECCAN,
        ayahCount = ayahCount,
        orderInMushaf = number,
        startPage = number,
    )

    private val surahs = listOf(
        surah(1, "The Opening", 7),
        surah(2, "The Heifer", 286),
        surah(3, "The Family of Imran", 200),
    )

    private var dismissed = false
    private var read = false
    private val background = mutableListOf<Int>()
    private val passages = mutableListOf<Int>()
    private val subjects = mutableListOf<Int>()

    private fun render(surahNumber: Int = 3) {
        composeRule.setThemedContent {
            SurahInfoSheetHost(
                surahNumber = surahNumber,
                onDismiss = { dismissed = true },
                onReadSurah = { read = true },
                onOpenBackground = { background += it },
                onOpenPassages = { passages += it },
                onOpenSubjects = { subjects += it },
                viewModel = quranViewModel,
                thematicViewModel = thematicViewModel,
            )
        }
    }

    private fun loaded(
        summary: String? = "A Medinan surah answering the delegation of Najran.",
        passageCount: Int = 4,
        subjectCount: Int = 6,
    ) {
        homeState.value = QuranHomeUiState(
            surahs = surahs,
            pagination = MushafPagination.fallback(MushafScript.INDOPAK_16),
            isLoading = false,
        )
        thematic.value = SurahThematicUiState(
            overview = summary?.let {
                SurahOverview(surahNumber = 3, summary = it, sections = emptyList())
            },
            passages = List(passageCount) { index ->
                AyahTheme(
                    surahNumber = 3,
                    ayahFrom = index * 10 + 1,
                    ayahTo = index * 10 + 9,
                    theme = "Passage ${index + 1}",
                    ayahCount = 9,
                )
            },
            subjectCount = subjectCount,
            isLoading = false,
        )
    }

    @Test
    fun `raising the sheet asks both view models for the surah`() {
        render(surahNumber = 3)

        assertThat(quranEvents).contains(QuranEvent.LoadSurahInfo(3))
        assertThat(thematicEvents).contains(SurahThematicEvent.Load(3))
    }

    @Test
    fun `nothing is drawn until the surah is actually known`() {
        // A sheet that slides up empty and fills in reads as a fault on a control raised by a
        // tap on the row the reader is already looking at.
        render(surahNumber = 3)

        composeRule.onNodeWithText(str(R.string.surah_info_read_surah)).assertDoesNotExist()
    }

    @Test
    fun `a surah missing from the list draws nothing rather than an empty card`() {
        loaded()

        render(surahNumber = 114)

        composeRule.onNodeWithText(str(R.string.surah_info_read_surah)).assertDoesNotExist()
    }

    @Test
    fun `the surah's card appears once it is loaded`() {
        loaded()

        render(surahNumber = 3)

        composeRule.onNodeWithText("The Family of Imran").assertIsDisplayed()
    }

    @Test
    fun `reading the surah is handed back to the caller`() {
        loaded()
        render(surahNumber = 3)

        composeRule.onNodeWithText(str(R.string.surah_info_read_surah)).performClick()

        assertThat(read).isTrue()
    }

    @Test
    fun `listening plays the surah rather than leaving the sheet`() {
        loaded()
        render(surahNumber = 3)

        composeRule.onNodeWithText(str(R.string.listen)).performClick()

        assertThat(quranEvents).contains(QuranEvent.PlaySurahFromInfo(3))
    }

    @Test
    fun `the thematic rows carry the surah number the sheet was raised for`() {
        loaded()
        render(surahNumber = 3)

        composeRule.onNodeWithText(str(R.string.surah_info_passages)).performClick()
        composeRule.onNodeWithText(str(R.string.surah_info_subjects)).performClick()

        assertThat(passages).containsExactly(3)
        assertThat(subjects).containsExactly(3)
    }
}
