package com.arshadshah.nimaz.presentation.viewmodel.quran

import java.time.LocalDate
import com.arshadshah.nimaz.domain.time.FakeTodayProvider
import com.arshadshah.nimaz.core.text.FakeStringProvider
import android.content.Context
import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.data.audio.QuranAudioManager
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.usecase.GetSurahWithAyahsUseCase
import com.arshadshah.nimaz.domain.usecase.KhatamUseCases
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * What the Quran surfaces show *before* their data arrives.
 *
 * Both defects here are invisible in a test that hands the ViewModel data instantly, which is
 * why neither was caught: they only exist in the window between construction and the first
 * real emission. So the fakes below are deliberately slow, and the assertions are taken
 * *inside* that window.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QuranReaderHydrationTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var useCases: QuranUseCases
    private lateinit var audioManager: QuranAudioManager
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var khatamUseCases: KhatamUseCases
    private lateinit var context: Context
    private lateinit var getSurahWithAyahs: GetSurahWithAyahsUseCase

    /**
     * The translator id every reader query was actually issued with, in order.
     *
     * Recorded rather than verified through mockk: `getSurahWithAyahs` is an `operator fun
     * invoke` reached through a property of a mock, so a `verify` block records the property
     * getter *and* the call and its counts do not mean what they look like.
     */
    private val requestedTranslators = mutableListOf<String?>()

    private val surahs = listOf(
        surah(1, "The Opening", "Al-Fatihah", "الفاتحة"),
        surah(2, "The Cow", "Al-Baqarah", "البقرة"),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        useCases = mockk(relaxed = true)
        audioManager = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)
        khatamUseCases = mockk(relaxed = true)
        context = mockk(relaxed = true)
        every { settingsRepository.quranTranslatorId } returns MutableStateFlow("en.sahih")
        every { settingsRepository.quranMushafScript } returns MutableStateFlow("madani")
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
        every { useCases.getSurahList() } returns flowOf(surahs)
        getSurahWithAyahs = mockk()
        every { useCases.getSurahWithAyahs } returns getSurahWithAyahs
        every { getSurahWithAyahs(any(), any()) } answers {
            requestedTranslators += secondArg<String?>()
            flowOf(null)
        }
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() =
        QuranViewModel(useCases, audioManager, settingsRepository, khatamUseCases, RecordingTelemetry(), FakeTodayProvider(LocalDate.now()), FakeStringProvider())

    // R1 — the home list must stay on its skeleton until Room answers.

    @Test
    fun `the surah list is still loading before Room has emitted`() = runTest {
        every { useCases.getSurahList() } returns flow {
            delay(100)
            emit(surahs)
        }

        val vm = viewModel()
        advanceTimeBy(50)

        // `stateIn(…, SharingStarted.Eagerly, emptyList())` published its seed here — an empty
        // list with `isLoading = false`, which Quran Home renders as "nothing here" — one or
        // more frames before the database produced a single row.
        assertThat(vm.homeState.value.isLoading).isTrue()
        assertThat(vm.homeState.value.surahs).isEmpty()
    }

    @Test
    fun `the surah list resolves once Room emits`() = runTest {
        every { useCases.getSurahList() } returns flow {
            delay(100)
            emit(surahs)
        }

        val vm = viewModel()
        advanceUntilIdle()

        assertThat(vm.homeState.value.isLoading).isFalse()
        assertThat(vm.homeState.value.surahs.map { it.number }).containsExactly(1, 2)
    }

    // R2 — the reader must open in the user's translation, once.

    @Test
    fun `the reader loads the persisted translation, never the compiled-in default`() = runTest {
        // Slow enough that `LoadSurah` lands before the settings collector's first emission —
        // which is the ordering on a real cold open, DataStore's first read being disk-bound.
        every { settingsRepository.quranTranslatorId } returns flow {
            delay(100)
            emit("urdu_jalandhry")
        }

        val vm = viewModel()
        vm.onEvent(QuranEvent.LoadSurah(2))
        advanceUntilIdle()

        // `QuranReaderUiState.selectedTranslatorId` defaults to `sahih_international`, so
        // reading the translator off the state served English to every non-default user —
        // and this is the whole list of queries, so it also pins the second half of the
        // defect: the wrong-translation load landed first, then hydration looked like a
        // translation *change* and `reloadReaderContent()` re-issued the entire surah.
        assertThat(requestedTranslators).containsExactly("urdu_jalandhry")
    }

    @Test
    fun `a real translation change still reloads`() = runTest {
        val translator = MutableStateFlow("en.sahih")
        every { settingsRepository.quranTranslatorId } returns translator

        val vm = viewModel()
        vm.onEvent(QuranEvent.LoadSurah(2))
        advanceUntilIdle()
        requestedTranslators.clear()

        translator.value = "urdu_jalandhry"
        advanceUntilIdle()

        // The hydration guard suppresses the *first* emission only. A preference the user
        // actually changes must still invalidate the reader.
        assertThat(requestedTranslators).contains("urdu_jalandhry")
    }
}

private fun surah(number: Int, english: String, transliteration: String, arabic: String) = Surah(
    number = number,
    nameArabic = arabic,
    nameEnglish = english,
    nameTransliteration = transliteration,
    revelationType = RevelationType.MECCAN,
    ayahCount = 7,
    orderInMushaf = number,
    startPage = number,
)
