package com.arshadshah.nimaz.presentation.viewmodel.quran

import java.time.LocalDate
import com.arshadshah.nimaz.core.time.FakeTodayProvider
import com.arshadshah.nimaz.presentation.viewmodel.FakeStringProvider
import android.content.Context
import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.data.audio.QuranAudioManager
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.usecase.KhatamUseCases
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
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

/**
 * The surah filter on Quran Home.
 *
 * `QuranEvent.Search` / `ClearSearch` were emitted by nobody — `SearchScreen` drives a
 * different ViewModel entirely — so `filterSurahs` only ever ran with a blank query and
 * `filteredSurahs` was **permanently identical to `surahs`**, while `QuranHomeScreen` rendered
 * it as though it were filtered.
 *
 * This is deliberately a *list filter*, not the global content search: it narrows the 114
 * surahs in place rather than navigating away.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QuranSurahFilterTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var useCases: QuranUseCases
    private lateinit var audioManager: QuranAudioManager
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var khatamUseCases: KhatamUseCases
    private lateinit var context: Context

    private val surahs = listOf(
        surah(1, "The Opening", "Al-Fatihah", "الفاتحة"),
        surah(2, "The Cow", "Al-Baqarah", "البقرة"),
        surah(18, "The Cave", "Al-Kahf", "الكهف"),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        useCases = mockk(relaxed = true)
        audioManager = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)
        khatamUseCases = mockk(relaxed = true)
        context = mockk(relaxed = true)
        // The reader/home init combines a wall of settings flows, and a relaxed mock hands
        // back a Flow that emits nothing — on which `.first()` throws. Only the ones the init
        // path actually reads need real values.
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
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() =
        QuranViewModel(useCases, audioManager, settingsRepository, khatamUseCases, RecordingTelemetry(), FakeTodayProvider(LocalDate.now()), FakeStringProvider())

    @Test
    fun `the list narrows to the transliterated name`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(QuranEvent.Search("kahf"))
        advanceUntilIdle()

        assertThat(vm.homeState.value.filteredSurahs.map { it.number }).containsExactly(18)
        assertThat(vm.homeState.value.searchQuery).isEqualTo("kahf")
    }

    @Test
    fun `the English name matches too, case-insensitively`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(QuranEvent.Search("THE COW"))
        advanceUntilIdle()

        assertThat(vm.homeState.value.filteredSurahs.map { it.number }).containsExactly(2)
    }

    @Test
    fun `the Arabic name matches`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(QuranEvent.Search("الكهف"))
        advanceUntilIdle()

        assertThat(vm.homeState.value.filteredSurahs.map { it.number }).containsExactly(18)
    }

    @Test
    fun `clearing restores the whole list`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(QuranEvent.Search("kahf"))
        advanceUntilIdle()

        vm.onEvent(QuranEvent.ClearSearch)
        advanceUntilIdle()

        assertThat(vm.homeState.value.filteredSurahs.map { it.number })
            .containsExactly(1, 2, 18)
        assertThat(vm.homeState.value.searchQuery).isEmpty()
    }

    @Test
    fun `a query matching nothing leaves an empty list, not the whole list`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(QuranEvent.Search("zzzz"))
        advanceUntilIdle()

        // The failure mode this whole feature had: filteredSurahs == surahs regardless.
        assertThat(vm.homeState.value.filteredSurahs).isEmpty()
    }
}

private fun surah(number: Int, english: String, transliteration: String, arabic: String) = Surah(
    number = number,
    nameArabic = arabic,
    nameEnglish = english,
    nameTransliteration = transliteration,
    revelationType = RevelationType.MECCAN,
    ayahCount = 7,
    juzStart = 1,
    orderInMushaf = number,
    startPage = number,
)
