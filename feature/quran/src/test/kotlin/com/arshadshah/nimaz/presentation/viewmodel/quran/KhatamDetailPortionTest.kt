package com.arshadshah.nimaz.presentation.viewmodel.quran

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.core.text.FakeStringProvider
import com.arshadshah.nimaz.domain.model.Khatam
import com.arshadshah.nimaz.domain.model.KhatamDetailSnapshot
import com.arshadshah.nimaz.domain.model.KhatamInsights
import com.arshadshah.nimaz.domain.model.KhatamStats
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.domain.usecase.KhatamUseCases
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
import com.arshadshah.nimaz.domain.usecase.khatam.GetTodaysPortion
import com.arshadshah.nimaz.domain.usecase.khatam.KhatamPortion
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
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
 * The detail screen's "where you are and what today asks for" line.
 *
 * Two vocabularies meet here and nowhere else. `getNextUnreadPosition` answers in surah/ayah,
 * because that is what a reader recognises; [GetTodaysPortion] answers in global ayah ids,
 * because a day's portion is a span of the *book* and crosses surah boundaries as a matter of
 * course. Translating between them off by one surah is not a visible crash — it is a khatam that
 * quietly tells the reader to start in the wrong place.
 *
 * The label has two shapes for the same reason: a portion inside one surah names it once, one
 * that crosses names both ends. Getting that wrong reads as "Al-Kahf 1 → Al-Kahf 300".
 */
@OptIn(ExperimentalCoroutinesApi::class)
class KhatamDetailPortionTest {

    private val dispatcher = StandardTestDispatcher()
    private val telemetry = RecordingTelemetry()

    private lateinit var khatamUseCases: KhatamUseCases
    private lateinit var quranUseCases: QuranUseCases
    private lateinit var getTodaysPortion: GetTodaysPortion
    private lateinit var viewModel: KhatamViewModel

    private val detail = MutableStateFlow<KhatamDetailSnapshot?>(null)

    /** The first four surahs, so a global id up to 493 has a surah to land in. */
    private val surahs = listOf(
        surah(1, "Al-Fatihah", 7),
        surah(2, "Al-Baqarah", 286),
        surah(3, "Al-Imran", 200),
        surah(4, "An-Nisa", 176),
    )

    private fun surah(number: Int, name: String, ayahCount: Int) = Surah(
        number = number,
        nameArabic = "سورة",
        nameEnglish = name,
        nameTransliteration = name,
        revelationType = RevelationType.MEDINAN,
        ayahCount = ayahCount,
        orderInMushaf = number,
        startPage = number,
    )

    private val khatam = Khatam(id = 7, name = "Ramadan", totalAyahsRead = 300)

    private val snapshot = KhatamDetailSnapshot(
        khatam = khatam,
        juzProgress = emptyList(),
        dailyLogs = emptyList(),
        insights = KhatamInsights(),
        readAyahIds = emptySet(),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)

        khatamUseCases = mockk(relaxed = true)
        quranUseCases = mockk(relaxed = true)
        getTodaysPortion = mockk(relaxed = true)

        every { khatamUseCases.observeInProgressKhatams() } returns flowOf(emptyList())
        every { khatamUseCases.observeCompletedKhatams() } returns flowOf(emptyList())
        every { khatamUseCases.observeAbandonedKhatams() } returns flowOf(emptyList())
        every { khatamUseCases.observeKhatamStats() } returns flowOf(KhatamStats(0, 0, 0, 0, 0))
        every { khatamUseCases.observeActiveKhatam() } returns flowOf(null)
        every { khatamUseCases.observeKhatamDetail(7) } returns detail
        every { quranUseCases.getSurahList() } returns flowOf(surahs)
        coEvery { quranUseCases.getSurahByNumber(any()) } answers {
            surahs.firstOrNull { it.number == firstArg<Int>() }
        }

        viewModel = KhatamViewModel(
            khatamUseCases = khatamUseCases,
            quranUseCases = quranUseCases,
            getTodaysPortion = getTodaysPortion,
            strings = FakeStringProvider(),
            telemetry = telemetry,
        )
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun open() {
        detail.value = snapshot
        viewModel.onEvent(KhatamEvent.LoadKhatamDetail(7))
    }

    @Test
    fun `a khatam that is not there stops the spinner rather than hanging on it`() = runTest {
        detail.value = null
        viewModel.onEvent(KhatamEvent.LoadKhatamDetail(7))
        advanceUntilIdle()

        val state = viewModel.detailState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.notFound).isTrue()
    }

    @Test
    fun `opening a khatam fills the detail from its snapshot`() = runTest {
        open()
        advanceUntilIdle()

        val state = viewModel.detailState.value
        assertThat(state.khatam?.id).isEqualTo(7)
        assertThat(state.notFound).isFalse()
        assertThat(state.isLoading).isFalse()
    }

    @Test
    fun `the next unread verse is named as well as numbered`() = runTest {
        coEvery { khatamUseCases.getNextUnreadPosition(7) } returns (3 to 12)
        open()
        advanceUntilIdle()

        val state = viewModel.detailState.value
        assertThat(state.nextUnreadSurah).isEqualTo(3)
        assertThat(state.nextUnreadAyah).isEqualTo(12)
        assertThat(state.nextUnreadSurahName).isEqualTo("Al-Imran")
    }

    @Test
    fun `today's portion is asked for from the global id of that verse`() = runTest {
        // Al-Imran 3:12 is 7 + 286 + 12 = 305 verses into the book.
        coEvery { khatamUseCases.getNextUnreadPosition(7) } returns (3 to 12)
        every { getTodaysPortion(any(), any()) } returns KhatamPortion(305, 324)

        open()
        advanceUntilIdle()

        io.mockk.verify { getTodaysPortion(khatam, 305) }
        assertThat(viewModel.detailState.value.todaysPortion?.ayahCount).isEqualTo(20)
    }

    @Test
    fun `a portion inside one surah names it once`() = runTest {
        coEvery { khatamUseCases.getNextUnreadPosition(7) } returns (3 to 12)
        every { getTodaysPortion(any(), any()) } returns KhatamPortion(305, 324)

        open()
        advanceUntilIdle()

        // string:khatam_portion_within(Al-Imran, 12, 31)
        val label = viewModel.detailState.value.todaysPortionLabel
        assertThat(label).contains("Al-Imran, 12, 31")
    }

    @Test
    fun `a portion that crosses a surah names both ends`() = runTest {
        // 300 → Al-Baqarah 293? no: 7 + 286 = 293, so 300 is Al-Imran 7; 500 is An-Nisa 7.
        coEvery { khatamUseCases.getNextUnreadPosition(7) } returns (3 to 7)
        every { getTodaysPortion(any(), any()) } returns KhatamPortion(300, 500)

        open()
        advanceUntilIdle()

        val label = viewModel.detailState.value.todaysPortionLabel
        assertThat(label).contains("Al-Imran, 7, An-Nisa, 7")
    }

    @Test
    fun `a portion past the end of the book is left unlabelled`() = runTest {
        // Only four surahs are known here, so 6000 lands nowhere: better no label than a wrong
        // one built from the last surah that happened to fit.
        coEvery { khatamUseCases.getNextUnreadPosition(7) } returns (3 to 7)
        every { getTodaysPortion(any(), any()) } returns KhatamPortion(300, 6000)

        open()
        advanceUntilIdle()

        assertThat(viewModel.detailState.value.todaysPortionLabel).isNull()
    }

    @Test
    fun `nothing read yet still asks for a portion, from the start of the book`() = runTest {
        coEvery { khatamUseCases.getNextUnreadPosition(7) } returns null
        every { getTodaysPortion(any(), any()) } returns KhatamPortion(1, 20)

        open()
        advanceUntilIdle()

        io.mockk.verify { getTodaysPortion(khatam, null) }
        assertThat(viewModel.detailState.value.nextUnreadSurah).isNull()
    }

    @Test
    fun `a surah list that cannot be read leaves the portion unlabelled rather than failing`() =
        runTest {
            every { quranUseCases.getSurahList() } returns kotlinx.coroutines.flow.flow {
                throw IllegalStateException("content database missing")
            }
            coEvery { khatamUseCases.getNextUnreadPosition(7) } returns (3 to 12)
            every { getTodaysPortion(any(), any()) } returns KhatamPortion(305, 324)

            open()
            advanceUntilIdle()

            assertThat(viewModel.detailState.value.todaysPortionLabel).isNull()
            assertThat(viewModel.detailState.value.notFound).isFalse()
        }
}
