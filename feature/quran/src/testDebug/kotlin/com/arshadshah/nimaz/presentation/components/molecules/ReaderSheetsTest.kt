package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.RecitationRepeat
import com.arshadshah.nimaz.domain.model.RecitationSpeed
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.Surah
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
 * The three sheets the reader raises over itself: recitation settings, one surah's card, and
 * "go to…".
 *
 * They are grouped because they share a failure mode. A sheet is a modal over content the reader
 * is in the middle of, so anything it offers that leads nowhere costs them their place for
 * nothing: a "Passages" row on a surah with no outline, a repeat range on a recitation that is
 * not repeating a range, a "Go" for a verse number the surah does not have.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class ReaderSheetsTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    // ---- Recitation ----

    private var stopped = false
    private var dismissed = false
    private var openedReciters = false
    private val repeats = mutableListOf<RecitationRepeat>()
    private val speeds = mutableListOf<RecitationSpeed>()
    private val followAlongs = mutableListOf<Boolean>()

    private fun renderRecitation(
        repeat: RecitationRepeat = RecitationRepeat.Off,
        speed: RecitationSpeed = RecitationSpeed.NORMAL,
        followAlong: Boolean = true,
        ayahCount: Int = 110,
    ) {
        composeRule.setThemedContent {
            RecitationSheet(
                reciterName = "Mishary Alafasy",
                repeat = repeat,
                speed = speed,
                followAlong = followAlong,
                ayahCount = ayahCount,
                onOpenReciters = { openedReciters = true },
                onRepeatChange = { repeats += it },
                onSpeedChange = { speeds += it },
                onFollowAlongChange = { followAlongs += it },
                onStop = { stopped = true },
                onDismiss = { dismissed = true },
            )
        }
    }

    @Test
    fun `the reciter who is reading is named, and can be changed`() {
        renderRecitation()

        composeRule.onNodeWithText("Mishary Alafasy").assertIsDisplayed()
        composeRule.onNodeWithText("Mishary Alafasy").performClick()

        assertThat(openedReciters).isTrue()
    }

    @Test
    fun `turning on verse repeat asks for a count, not a loop`() {
        renderRecitation(repeat = RecitationRepeat.Off)

        composeRule.onNodeWithText(str(R.string.recitation_repeat_ayah)).performClick()

        // A repeat of one is not a repeat, so the default has to clear the minimum.
        val ayah = repeats.filterIsInstance<RecitationRepeat.Ayah>().single()
        assertThat(ayah.times).isAtLeast(RecitationRepeat.MIN_TIMES)
    }

    @Test
    fun `the repeat count is only offered while verse repeat is on`() {
        renderRecitation(repeat = RecitationRepeat.Off)

        composeRule.onNodeWithText(str(R.string.recitation_repeat_times)).assertDoesNotExist()
    }

    @Test
    fun `a repeating verse shows how many times`() {
        renderRecitation(repeat = RecitationRepeat.Ayah(times = 3))

        composeRule.onNodeWithText(str(R.string.recitation_repeat_times)).assertIsDisplayed()
    }

    @Test
    fun `a range repeat asks for both ends of it`() {
        renderRecitation(repeat = RecitationRepeat.Range(fromAyah = 1, toAyah = 10))

        composeRule.onNodeWithText(str(R.string.recitation_repeat_from)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.recitation_repeat_to)).assertIsDisplayed()
    }

    @Test
    fun `turning repeat off says so`() {
        renderRecitation(repeat = RecitationRepeat.Ayah(times = 3))

        composeRule.onNodeWithText(str(R.string.recitation_repeat_off)).performClick()

        assertThat(repeats).contains(RecitationRepeat.Off)
    }

    @Test
    fun `stopping and dismissing are two different things`() {
        renderRecitation()

        composeRule.onNodeWithText(str(R.string.recitation_stop)).performClick()
        assertThat(stopped).isTrue()
        assertThat(dismissed).isFalse()
    }

    @Test
    fun `done closes the sheet without stopping the recitation`() {
        renderRecitation()

        composeRule.onNodeWithText(str(R.string.recitation_done)).performClick()

        assertThat(dismissed).isTrue()
        assertThat(stopped).isFalse()
    }

    @Test
    fun `follow-along can be turned off without leaving the sheet`() {
        renderRecitation(followAlong = true)

        // The label is a label; the switch beside it is the control.
        composeRule.onNode(isToggleable()).performClick()

        assertThat(followAlongs).contains(false)
    }

    // ---- One surah's card ----

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

    private var readSurah = false
    private var listened = false
    private var openedBackground = false
    private var openedPassages = false
    private var openedSubjects = false

    private fun renderSurahInfo(
        summary: String? = "A Meccan surah about trial and shelter.",
        sectionCount: Int = 3,
        passageCount: Int = 12,
        subjectCount: Int = 8,
    ) {
        composeRule.setThemedContent {
            SurahInfoSheet(
                surah = cave,
                summary = summary,
                sectionCount = sectionCount,
                passageCount = passageCount,
                subjectCount = subjectCount,
                startPage = 293,
                juzNumber = 15,
                onDismiss = {},
                onReadSurah = { readSurah = true },
                onListen = { listened = true },
                onOpenBackground = { openedBackground = true },
                onOpenPassages = { openedPassages = true },
                onOpenSubjects = { openedSubjects = true },
            )
        }
    }

    @Test
    fun `the surah's facts are on its card`() {
        renderSurahInfo()

        composeRule.onNodeWithText("The Cave").assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.quran_home_makkah)).assertIsDisplayed()
    }

    @Test
    fun `reading and listening are both offered`() {
        renderSurahInfo()

        composeRule.onNodeWithText(str(R.string.surah_info_read_surah)).performClick()
        assertThat(readSurah).isTrue()

        composeRule.onNodeWithText(str(R.string.listen)).performClick()
        assertThat(listened).isTrue()
    }

    @Test
    fun `the thematic rows are offered where there is something behind them`() {
        renderSurahInfo(sectionCount = 3, passageCount = 12, subjectCount = 8)

        composeRule.onNodeWithText(str(R.string.surah_info_background)).performClick()
        assertThat(openedBackground).isTrue()

        composeRule.onNodeWithText(str(R.string.surah_info_passages)).performClick()
        assertThat(openedPassages).isTrue()

        composeRule.onNodeWithText(str(R.string.surah_info_subjects)).performClick()
        assertThat(openedSubjects).isTrue()
    }

    @Test
    fun `a surah with no thematic content is offered no thematic rows`() {
        // A sheet is a modal over the verse the reader was on. A row that opens onto an
        // explanation costs them their place for nothing.
        renderSurahInfo(summary = null, sectionCount = 0, passageCount = 0, subjectCount = 0)

        composeRule.onNodeWithText(str(R.string.surah_info_background)).assertDoesNotExist()
        composeRule.onNodeWithText(str(R.string.surah_info_passages)).assertDoesNotExist()
        composeRule.onNodeWithText(str(R.string.surah_info_subjects)).assertDoesNotExist()
    }

    // ---- Go to ----

    private var wentToVerse: Int? = null
    private var wentToJuz: Int? = null
    private var wentToPage: Int? = null

    private fun renderGoTo(maxVerse: Int = 110, maxPage: Int = 604) {
        composeRule.setThemedContent {
            ReaderGoToSheet(
                maxVerse = maxVerse,
                maxPage = maxPage,
                onGoToVerse = { wentToVerse = it },
                onGoToJuz = { wentToJuz = it },
                onGoToPage = { wentToPage = it },
                onDismiss = {},
            )
        }
    }

    @Test
    fun `the sheet says what range the number may be in`() {
        renderGoTo(maxVerse = 110)

        // Otherwise "Go" on 300 in a 110-verse surah is a button that does nothing and says
        // nothing about why.
        composeRule.onNodeWithText(str(R.string.reader_go_to_range, 1, 110)).assertIsDisplayed()
    }

    @Test
    fun `going nowhere is not offered before a number is typed`() {
        renderGoTo()

        composeRule.onNodeWithText(str(R.string.reader_go_to_action)).performClick()

        assertThat(listOf(wentToVerse, wentToJuz, wentToPage)).containsExactly(null, null, null)
    }
}
