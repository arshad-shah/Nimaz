package com.arshadshah.nimaz.presentation.screens.quran

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.AudioState
import com.arshadshah.nimaz.domain.model.Ayah
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
 * What a reader can do to the verse in front of them, and what the reader shows while a
 * recitation is running.
 *
 * The per-verse actions used to be a row of pills on every verse. They are a sheet now, which
 * means the row itself has to open it and the sheet has to know which verse it was opened on —
 * a sheet showing the wrong verse's bookmark state is a tap that marks something else.
 *
 * The khatam controls are conditional on a khatam being active, and the audio bar on a session
 * running. Both are the kind of thing that renders happily when it should not: a "mark as read"
 * that writes to no khatam, and a transport over nothing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class QuranReaderActionsTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val readerState = MutableStateFlow(QuranReaderUiState(isLoading = false))
    private val bookmarksState = MutableStateFlow(QuranBookmarksUiState())
    private val audioState = MutableStateFlow(AudioState())
    private val homeState = MutableStateFlow(QuranHomeUiState())
    private val events = mutableListOf<QuranEvent>()

    private val viewModel: QuranViewModel = mockk(relaxed = true) {
        every { readerState } returns this@QuranReaderActionsTest.readerState
        every { bookmarksState } returns this@QuranReaderActionsTest.bookmarksState
        every { audioState } returns this@QuranReaderActionsTest.audioState
        every { homeState } returns this@QuranReaderActionsTest.homeState
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

    private fun ayah(number: Int) = Ayah(
        id = 18_000 + number,
        surahNumber = 18,
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

    private fun surahReader(
        activeKhatamId: Long? = null,
        khatamReadAyahIds: Set<Int> = emptySet(),
        ayahNotes: Map<Int, String> = emptyMap(),
        favouriteAyahIds: Set<Int> = emptySet(),
    ) = QuranReaderUiState(
        isLoading = false,
        readingMode = ReadingMode.SURAH,
        surahWithAyahs = SurahWithAyahs(cave, verses),
        showTranslation = true,
        activeKhatamId = activeKhatamId,
        khatamReadAyahIds = khatamReadAyahIds,
        ayahNotes = ayahNotes,
        favoriteAyahIds = favouriteAyahIds,
    )

    private fun render() {
        composeRule.setThemedContent {
            QuranReaderScreen(
                surahNumber = 18,
                onNavigateBack = {},
                viewModel = viewModel,
            )
        }
    }

    /** Open the per-verse sheet on the first verse. */
    private fun openSheetOnFirstVerse() {
        composeRule.onNodeWithText("verse 1 translated").performClick()
    }

    // ---- The per-verse sheet ----

    @Test
    fun `tapping a verse opens its actions`() {
        readerState.value = surahReader()
        render()

        openSheetOnFirstVerse()

        composeRule.onNodeWithText(str(R.string.ayah_action_play_from_here)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.ayah_action_tafseer)).assertIsDisplayed()
    }

    @Test
    fun `the sheet plays from the verse it was opened on`() {
        readerState.value = surahReader()
        render()
        openSheetOnFirstVerse()

        composeRule.onNodeWithText(str(R.string.ayah_action_play_from_here)).performClick()

        val played = events.filterIsInstance<QuranEvent.PlayAyahAudio>().single()
        assertThat(played.ayahGlobalId).isEqualTo(18_001)
        assertThat(played.ayahNumber).isEqualTo(1)
    }

    @Test
    fun `bookmarking from the sheet marks that verse`() {
        readerState.value = surahReader()
        render()
        openSheetOnFirstVerse()

        composeRule.onNodeWithText(str(R.string.ayah_action_bookmark)).performClick()

        assertThat(events.filterIsInstance<QuranEvent.ToggleBookmark>().single().ayahId)
            .isEqualTo(18_001)
    }

    @Test
    fun `favouriting from the sheet marks that verse`() {
        readerState.value = surahReader()
        render()
        openSheetOnFirstVerse()

        composeRule.onNodeWithText(str(R.string.ayah_action_favourite)).performClick()

        assertThat(events.filterIsInstance<QuranEvent.ToggleFavorite>().single().ayahId)
            .isEqualTo(18_001)
    }

    @Test
    fun `the sheet names the verse's place in the book`() {
        readerState.value = surahReader()
        render()

        openSheetOnFirstVerse()

        // Twice: once under the sheet's title and once on the row it was opened from.
        composeRule.onAllNodesWithText(str(R.string.juz_page_dot_format, 15, 293))
            .onFirst()
            .assertIsDisplayed()
    }

    // ---- Khatam ----

    @Test
    fun `without an active khatam the sheet offers no khatam action`() {
        readerState.value = surahReader(activeKhatamId = null)
        render()

        openSheetOnFirstVerse()

        // A "mark as read" with no khatam behind it writes nowhere.
        composeRule.onNodeWithText(str(R.string.ayah_action_mark_read)).assertDoesNotExist()
    }

    @Test
    fun `with an active khatam a verse can be marked read from the sheet`() {
        readerState.value = surahReader(activeKhatamId = 7)
        render()
        openSheetOnFirstVerse()

        composeRule.onNodeWithText(str(R.string.ayah_action_mark_read)).performClick()

        assertThat(events.filterIsInstance<QuranEvent.ToggleKhatamAyah>().single().ayahId)
            .isEqualTo(18_001)
    }

    @Test
    fun `a khatam reader gets a per-verse tick on the row itself`() {
        readerState.value = surahReader(activeKhatamId = 7, khatamReadAyahIds = setOf(18_001))
        render()

        // Marking a page's worth of verses one sheet at a time is three taps each.
        composeRule.onAllNodesWithContentDescription(str(R.string.cd_mark_as_unread))
            .onFirst()
            .assertIsDisplayed()
    }

    @Test
    fun `ticking a verse from the row marks it for the khatam`() {
        readerState.value = surahReader(activeKhatamId = 7)
        render()

        composeRule.onAllNodesWithContentDescription(str(R.string.cd_mark_as_read))
            .onFirst()
            .performClick()

        assertThat(events.filterIsInstance<QuranEvent.ToggleKhatamAyah>()).isNotEmpty()
    }

    // ---- The recitation ----

    @Test
    fun `with no recitation running there is no transport`() {
        readerState.value = surahReader()
        audioState.value = AudioState(isActive = false)
        render()

        composeRule.onNodeWithContentDescription(str(R.string.cd_next_ayah_audio))
            .assertDoesNotExist()
    }

    @Test
    fun `a running recitation puts its transport on the reader`() {
        readerState.value = surahReader()
        audioState.value = AudioState(
            isActive = true,
            isPlaying = true,
            currentAyahId = 18_001,
            currentSurahNumber = 18,
            duration = 120_000,
            position = 30_000,
        )
        render()

        composeRule.onNodeWithContentDescription(str(R.string.cd_next_ayah_audio))
            .assertIsDisplayed()
    }

    @Test
    fun `the transport steps the recitation rather than the reader`() {
        readerState.value = surahReader()
        audioState.value = AudioState(isActive = true, isPlaying = true, currentAyahId = 18_001)
        render()

        composeRule.onNodeWithContentDescription(str(R.string.cd_next_ayah_audio)).performClick()

        assertThat(events).contains(QuranEvent.NextAyahAudio)
    }

    @Test
    fun `pausing from the reader pauses the recitation`() {
        readerState.value = surahReader()
        audioState.value = AudioState(isActive = true, isPlaying = true, currentAyahId = 18_001)
        render()

        composeRule.onNodeWithContentDescription(str(R.string.cd_pause)).performClick()

        assertThat(events).contains(QuranEvent.PauseAudio)
    }

    // ---- Notes ----

    @Test
    fun `a verse the reader has written on shows its note`() {
        readerState.value = surahReader(ayahNotes = mapOf(18_001 to "read again in Ramadan"))
        render()

        // Carried in the reader's state so the editor opens on what is written rather than on a
        // blank field that would silently overwrite it.
        composeRule.onAllNodesWithContentDescription(str(R.string.ayah_action_note))
            .onFirst()
            .assertIsDisplayed()
    }
}
