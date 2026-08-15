@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.arshadshah.nimaz.presentation.viewmodel.quran

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.core.time.FakeTodayProvider
import com.arshadshah.nimaz.data.audio.QuranAudioManager
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.MushafPagination
import com.arshadshah.nimaz.domain.model.MushafScript
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.domain.model.SurahWithAyahs
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.usecase.KhatamUseCases
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
import com.arshadshah.nimaz.presentation.viewmodel.FakeStringProvider
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Playing from one verse — the reader's ayah tooltip and the sheet and audio bar that reach the
 * same event.
 *
 * The tooltip's play button did nothing at all for any verse the reader's `ayahs` list did not
 * happen to hold. `playFromAyah` looked the verse up in the playlist it was handed, found no
 * index, and returned: no sound, no error, no state change — a dead button. `ayahs` holds one
 * page, so in dual-page mode it holds only the left half of the spread, and in the line-accurate
 * layout the page is drawn from `mushafPageLayoutCache` and can be fully on screen before
 * `getAyahsByPage` has landed for it at all.
 *
 * What these pin down is that **something always plays**: the reader's own list when it holds the
 * verse (so a page plays as a page and a juz as a juz), that verse's surah when it does not, and
 * the single verse when even the surah cannot account for it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QuranViewModelAyahPlaybackTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var useCases: QuranUseCases
    private lateinit var audioManager: QuranAudioManager
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var khatamUseCases: KhatamUseCases

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)

        useCases = mockk(relaxed = true)
        audioManager = mockk(relaxed = true)
        khatamUseCases = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)

        every { settingsRepository.quranMushafScript } returns
                MutableStateFlow(MushafScript.MADANI.name)
        every { settingsRepository.quranTranslatorId } returns MutableStateFlow("sahih_international")
        every { settingsRepository.showTranslation } returns MutableStateFlow(true)
        every { settingsRepository.showTransliteration } returns MutableStateFlow(false)
        every { settingsRepository.quranArabicFontSize } returns MutableStateFlow(28f)
        every { settingsRepository.quranArabicFont } returns MutableStateFlow("amiri")
        every { settingsRepository.quranTranslationFontSize } returns MutableStateFlow(16f)
        every { settingsRepository.continuousReading } returns MutableStateFlow(true)
        every { settingsRepository.keepScreenOn } returns MutableStateFlow(true)
        every { settingsRepository.selectedReciterId } returns MutableStateFlow(null)
        every { settingsRepository.showTajweed } returns MutableStateFlow(false)
        every { settingsRepository.tajweedUnderline } returns MutableStateFlow(false)

        // Page N holds the one verse with id N — enough to put a verse on the page the reader is
        // on, and to leave every other verse off it.
        every { useCases.getAyahsByPage(any(), any(), any()) } answers {
            MutableStateFlow(listOf(ayah(firstArg())))
        }

        // A real pagination rather than a relaxed mock: the reader writes it into its state on
        // every settings emission, and nothing here should rest on what a stub invents.
        coEvery { useCases.getMushafPagination(any()) } answers {
            MushafPagination.fallback(firstArg())
        }

        // Al-Baqarah, standing in for any surah: ten verses, ids 101..110.
        every { useCases.getSurahWithAyahs(SURAH, any()) } returns flowOf(
            SurahWithAyahs(
                surah = Surah(
                    number = SURAH,
                    nameArabic = "البقرة",
                    nameEnglish = "Al-Baqarah",
                    nameTransliteration = "Al-Baqarah",
                    revelationType = RevelationType.MEDINAN,
                    ayahCount = SURAH_AYAH_IDS.size,
                    orderInMushaf = 87,
                ),
                ayahs = SURAH_AYAH_IDS.mapIndexed { index, id ->
                    ayah(id).copy(surahNumber = SURAH, ayahNumber = index + 1)
                },
            )
        )

        // The manager's real contract, which is what the fix is about: it starts only when the
        // verse asked for is actually in the playlist handed to it.
        every { audioManager.playFromAyah(any(), any(), any()) } answers {
            val id = firstArg<Int>()
            secondArg<List<QuranAudioManager.AyahAudioItem>>().any { it.ayahGlobalId == id }
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = QuranViewModel(
        useCases,
        audioManager,
        settingsRepository,
        khatamUseCases,
        RecordingTelemetry(),
        FakeTodayProvider(LocalDate.now()),
        FakeStringProvider(),
    )

    private fun ayah(id: Int) = Ayah(
        id = id,
        surahNumber = SURAH,
        ayahNumber = id,
        textArabic = "ayah $id",
        textSimple = "ayah $id",
        juzNumber = 1,
        hizbNumber = 1,
        rubNumber = 1,
        pageNumber = id,
        sajdaType = null,
        sajdaNumber = null,
    )

    @Test
    fun `a verse on the page the reader is on plays from the page`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onEvent(QuranEvent.LoadPage(4))
        advanceUntilIdle()

        viewModel.onEvent(QuranEvent.PlayAyahAudio(ayahGlobalId = 4, surahNumber = SURAH, ayahNumber = 4))
        advanceUntilIdle()

        // The page's own playlist, so playing from a page still plays that page rather than
        // silently widening to the whole surah.
        verify {
            audioManager.playFromAyah(
                4,
                match { items -> items.map { it.ayahGlobalId } == listOf(4) },
                any(),
            )
        }
        verify(exactly = 0) { audioManager.playAyah(any(), any(), any()) }
    }

    @Test
    fun `a verse the reader's list does not hold plays from its own surah`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onEvent(QuranEvent.LoadPage(4))
        advanceUntilIdle()

        // The dead button: verse 107 is on screen — the right page of a spread, or a
        // line-accurate page drawn ahead of its ayah fetch — and is not in `ayahs`.
        viewModel.onEvent(
            QuranEvent.PlayAyahAudio(ayahGlobalId = 107, surahNumber = SURAH, ayahNumber = 7)
        )
        advanceUntilIdle()

        verify {
            audioManager.playFromAyah(
                107,
                match { items -> items.map { it.ayahGlobalId } == SURAH_AYAH_IDS },
                "Al-Baqarah",
            )
        }
        // Its surah accounts for it, so it plays as a reading and not as one stranded verse.
        verify(exactly = 0) { audioManager.playAyah(any(), any(), any()) }
    }

    @Test
    fun `a verse its surah cannot account for still plays on its own`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onEvent(QuranEvent.LoadPage(4))
        advanceUntilIdle()

        viewModel.onEvent(
            QuranEvent.PlayAyahAudio(ayahGlobalId = 9999, surahNumber = SURAH, ayahNumber = 99)
        )
        advanceUntilIdle()

        // Nothing to start a playlist at, which is exactly where this used to give up.
        verify { audioManager.playAyah(9999, SURAH, 99) }
    }

    @Test
    fun `the reader's list is preferred over the surah when it holds the verse`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            viewModel.onEvent(QuranEvent.LoadPage(107))
            advanceUntilIdle()

            viewModel.onEvent(
                QuranEvent.PlayAyahAudio(ayahGlobalId = 107, surahNumber = SURAH, ayahNumber = 7)
            )
            advanceUntilIdle()

            // Page 107 holds verse 107, so no surah fetch happens at all: the reader's own
            // context wins, and the fallback is a fallback.
            assertThat(viewModel.readerState.value.ayahs.map { it.id }).containsExactly(107)
            verify(exactly = 0) { useCases.getSurahWithAyahs(SURAH, any()) }
        }

    private companion object {
        const val SURAH = 2
        val SURAH_AYAH_IDS = (101..110).toList()
    }
}
