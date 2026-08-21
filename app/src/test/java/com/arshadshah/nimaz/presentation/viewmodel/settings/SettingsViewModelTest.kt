package com.arshadshah.nimaz.presentation.viewmodel.settings

import com.arshadshah.nimaz.domain.model.PrayerAlertStyle
import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.domain.time.FakeTodayProvider
import com.arshadshah.nimaz.core.util.PrayerNotificationScheduler
import com.arshadshah.nimaz.domain.prayer.PrayerTimeCalculator
import com.arshadshah.nimaz.data.audio.AdhanAudioManager
import com.arshadshah.nimaz.data.audio.AdhanSound
import com.arshadshah.nimaz.data.audio.DownloadState
import com.arshadshah.nimaz.domain.repository.AdhanDownloader
import com.arshadshah.nimaz.domain.repository.AppLocale
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.usecase.ClearAllUserDataUseCase
import com.arshadshah.nimaz.domain.usecase.PrayerUseCases
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
import com.arshadshah.nimaz.domain.usecase.notification.RescheduleNotificationsUseCase
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
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val telemetry = RecordingTelemetry()
    private val todayProvider = FakeTodayProvider(LocalDate.of(2026, 8, 14))

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)

        val prayerTimeCalculator = mockk<PrayerTimeCalculator>(relaxed = true)
        val appLocale = mockk<AppLocale>(relaxed = true)
        val adhanDownloader = mockk<AdhanDownloader>(relaxed = true)
        val prayerUseCases = mockk<PrayerUseCases>(relaxed = true)
        val quranUseCases = mockk<QuranUseCases>(relaxed = true)
        val prayerNotificationScheduler = mockk<PrayerNotificationScheduler>(relaxed = true)
        val rescheduleNotificationsUseCase = mockk<RescheduleNotificationsUseCase>(relaxed = true)
        val clearAllUserData = mockk<ClearAllUserDataUseCase>(relaxed = true)

        val settingsRepository = mockk<SettingsRepository>(relaxed = true).also { repo ->
            every { repo.fajrNotificationEnabled } returns flowOf(true)
            every { repo.dhuhrNotificationEnabled } returns flowOf(true)
            every { repo.asrNotificationEnabled } returns flowOf(true)
            every { repo.maghribNotificationEnabled } returns flowOf(true)
            every { repo.ishaNotificationEnabled } returns flowOf(true)
            every { repo.prayerNotificationsEnabled } returns flowOf(true)
            every { repo.prayerReminderEnabled(any()) } returns flowOf(true)
            every { repo.prayerReminderMinutes(any()) } returns flowOf(15)
            every { repo.prayerAlertStyle(any()) } returns flowOf(PrayerAlertStyle.NOTIFICATION)
            every { repo.quranTranslatorId } returns flowOf("sahih_international")
            every { repo.quranArabicFont } returns flowOf("uthmani")
            every { repo.selectedReciterId } returns flowOf(null)
            every { repo.quranMushafScript } returns flowOf("")
            every { repo.showTranslation } returns flowOf(true)
            every { repo.showTransliteration } returns flowOf(false)
            every { repo.quranArabicFontSize } returns flowOf(22f)
            every { repo.quranTranslationFontSize } returns flowOf(16f)
            every { repo.continuousReading } returns flowOf(false)
            every { repo.keepScreenOn } returns flowOf(false)
            every { repo.showTajweed } returns flowOf(false)
            every { repo.tajweedUnderline } returns flowOf(false)
            every { repo.hijriDayOffset } returns flowOf(0)
        }

        val adhanAudioManager = mockk<AdhanAudioManager>(relaxed = true).also { mgr ->
            every { mgr.downloadState } returns MutableStateFlow(emptyMap<AdhanSound, DownloadState>())
            every { mgr.isPlaying } returns MutableStateFlow(false)
            every { mgr.currentlyPlaying } returns MutableStateFlow(null)
        }

        every { prayerUseCases.observeCalculationSettings() } returns flowOf()
        every { prayerUseCases.getCurrentLocation() } returns flowOf(null)

        viewModel = SettingsViewModel(
            prayerTimeCalculator = prayerTimeCalculator,
            appLocale = appLocale,
            adhanDownloader = adhanDownloader,
            prayerUseCases = prayerUseCases,
            settingsRepository = settingsRepository,
            quranUseCases = quranUseCases,
            prayerNotificationScheduler = prayerNotificationScheduler,
            rescheduleNotificationsUseCase = rescheduleNotificationsUseCase,
            telemetry = telemetry,
            adhanAudioManager = adhanAudioManager,
            clearAllUserData = clearAllUserData,
            todayProvider = todayProvider,
        )
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `ViewModel initialises without throwing`() = runTest {
        advanceUntilIdle()
        assertThat(viewModel.generalState.value).isNotNull()
    }

    @Test
    fun `prayer state is non-null on init`() = runTest {
        advanceUntilIdle()
        assertThat(viewModel.prayerState.value).isNotNull()
    }

    @Test
    fun `notification state is non-null on init`() = runTest {
        advanceUntilIdle()
        assertThat(viewModel.notificationState.value).isNotNull()
    }

    @Test
    fun `quran state is non-null on init`() = runTest {
        advanceUntilIdle()
        assertThat(viewModel.quranState.value).isNotNull()
    }

    @Test
    fun `shouldRestart starts as false`() = runTest {
        advanceUntilIdle()
        assertThat(viewModel.shouldRestart.value).isFalse()
    }
}
