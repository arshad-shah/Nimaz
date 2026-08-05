package com.arshadshah.nimaz.presentation.viewmodel

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
import com.arshadshah.nimaz.domain.usecase.TafseerUseCases
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
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
 * `TafseerChaptersViewModel` had **zero instrumentation** — neither `AppAnalytics`
 * nor `CrashReporter` was imported — and its `.catch { }` did not even bind the
 * throwable. A failure loading the 114-surah list produced an empty screen with the
 * spinner turned off, no message, no Crashlytics record and no `app_error` event.
 *
 * The first test here could not pass before this change, because the state had no
 * `error` field at all. Refs #358, #359.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TafseerChaptersViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val telemetry = RecordingTelemetry()
    private lateinit var quranUseCases: QuranUseCases
    private lateinit var tafseerUseCases: TafseerUseCases

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        quranUseCases = mockk(relaxed = true)
        tafseerUseCases = mockk(relaxed = true)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = TafseerChaptersViewModel(quranUseCases, tafseerUseCases, telemetry)

    @Test
    fun `a failing surah list surfaces an error instead of an empty screen`() = runTest {
        val boom = IllegalStateException("no such table: surah")
        every { quranUseCases.getSurahList() } returns flow { throw boom }
        every { tafseerUseCases.getTafseerNotes() } returns flowOf(emptyList())

        val state = viewModel().state
        advanceUntilIdle()

        assertThat(state.value.isLoading).isFalse()
        assertThat(state.value.error).isNotNull()
        // Identity is not asserted: coroutine stacktrace recovery hands the catch a
        // copy of the original throwable, so compare what survives that copy.
        assertThat(telemetry.exceptions.single()).hasMessageThat().isEqualTo(boom.message)
        assertThat(telemetry.errors.single().domain).isEqualTo("tafseer_chapters")
    }

    @Test
    fun `a successful load clears loading and reports nothing`() = runTest {
        val surahs = listOf(
            Surah(
                number = 1,
                nameArabic = "الفاتحة",
                nameEnglish = "The Opening",
                nameTransliteration = "Al-Fatihah",
                revelationType = RevelationType.MECCAN,
                ayahCount = 7,
                juzStart = 1,
                orderInMushaf = 1,
            )
        )
        every { quranUseCases.getSurahList() } returns flowOf(surahs)
        every { tafseerUseCases.getTafseerNotes() } returns flowOf(emptyList())

        val state = viewModel().state
        advanceUntilIdle()

        assertThat(state.value.surahs).hasSize(1)
        assertThat(state.value.isLoading).isFalse()
        assertThat(state.value.error).isNull()
        assertThat(telemetry.calls).isEmpty()
    }
}
