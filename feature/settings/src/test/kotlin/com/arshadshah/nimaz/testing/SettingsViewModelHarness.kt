package com.arshadshah.nimaz.testing

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.data.audio.AdhanAudioManager
import com.arshadshah.nimaz.data.audio.AdhanSound
import com.arshadshah.nimaz.data.audio.DownloadState
import com.arshadshah.nimaz.domain.model.AudioState
import com.arshadshah.nimaz.domain.model.Location
import com.arshadshah.nimaz.domain.prayer.PrayerTimeCalculator
import com.arshadshah.nimaz.domain.repository.AdhanDownloader
import com.arshadshah.nimaz.domain.repository.AppLocale
import com.arshadshah.nimaz.domain.repository.PrayerAlarmScheduler
import com.arshadshah.nimaz.domain.repository.PrayerNotificationTester
import com.arshadshah.nimaz.domain.repository.QuranPlayback
import com.arshadshah.nimaz.domain.time.FakeTodayProvider
import com.arshadshah.nimaz.domain.usecase.ClearAllUserDataUseCase
import com.arshadshah.nimaz.domain.usecase.PrayerUseCases
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
import com.arshadshah.nimaz.domain.usecase.notification.RescheduleNotificationsUseCase
import com.arshadshah.nimaz.presentation.viewmodel.settings.SettingsViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate

/**
 * One assembled [SettingsViewModel] and every seam it was built from, held for assertion.
 *
 * Fourteen constructor arguments is a lot to re-declare per test class, and the cost of doing it
 * per class is not the typing — it is that each copy stubs only what its own assertions touch, so
 * the ViewModel under test is subtly a different object in every file. This builds the same one
 * everywhere: preferences that actually emit (see [SettingsRepositoryStub]), a recording telemetry,
 * a fixed today, and relaxed mocks for the ports.
 */
class SettingsViewModelHarness(
    val repo: SettingsRepositoryStub = SettingsRepositoryStub(),
    val telemetry: RecordingTelemetry = RecordingTelemetry(),
    today: LocalDate = LocalDate.of(2026, 8, 26),
) {
    val todayProvider = FakeTodayProvider(today)

    /**
     * A real flow behind `audioState`: `reciterPreview` combines it with the previewing id, and a
     * relaxed mock's flow never emits, so the combine would have nothing to produce.
     */
    val playbackAudioState = MutableStateFlow(AudioState())
    val quranPlayback = mockk<QuranPlayback>(relaxed = true) {
        every { audioState } returns this@SettingsViewModelHarness.playbackAudioState
    }

    val adhanDownloadState = MutableStateFlow<Map<AdhanSound, DownloadState>>(emptyMap())
    val adhanIsPlaying = MutableStateFlow(false)
    val adhanCurrentlyPlaying = MutableStateFlow<AdhanSound?>(null)
    val adhanAudioManager = mockk<AdhanAudioManager>(relaxed = true) {
        every { downloadState } returns this@SettingsViewModelHarness.adhanDownloadState
        every { isPlaying } returns this@SettingsViewModelHarness.adhanIsPlaying
        every { currentlyPlaying } returns this@SettingsViewModelHarness.adhanCurrentlyPlaying
    }

    val currentLocation = MutableStateFlow<Location?>(null)
    val allLocations = MutableStateFlow<List<Location>>(emptyList())
    val favoriteLocations = MutableStateFlow<List<Location>>(emptyList())

    val prayerUseCases = mockk<PrayerUseCases>(relaxed = true) {
        every { getCurrentLocation() } returns this@SettingsViewModelHarness.currentLocation
        every { getAllLocations() } returns this@SettingsViewModelHarness.allLocations
        every { getFavoriteLocations() } returns this@SettingsViewModelHarness.favoriteLocations
        every { observeCalculationSettings() } returns flowOf()
    }

    val quranUseCases = mockk<QuranUseCases>(relaxed = true)
    val appLocale = mockk<AppLocale>(relaxed = true)
    val adhanDownloader = mockk<AdhanDownloader>(relaxed = true)
    val prayerAlarmScheduler = mockk<PrayerAlarmScheduler>(relaxed = true)
    val prayerNotificationTester = mockk<PrayerNotificationTester>(relaxed = true)
    val rescheduleNotifications = mockk<RescheduleNotificationsUseCase>(relaxed = true)
    val clearAllUserData = mockk<ClearAllUserDataUseCase>(relaxed = true)
    val prayerTimeCalculator = mockk<PrayerTimeCalculator>(relaxed = true)

    /**
     * The text the Quran settings preview card resolves to for the selected translation.
     *
     * `getAyahTranslation` is a *property* holding a use case whose `invoke` is an operator, so
     * the stub has to name `invoke` rather than the property — `coEvery { …getAyahTranslation(…) }`
     * would stub nothing and read as if it had.
     */
    fun previewTranslationIs(text: String?) {
        coEvery { quranUseCases.getAyahTranslation.invoke(any(), any()) } returns text
    }

    /** Make the preview read fail, which is the arm `catchAndReport` exists for. */
    fun previewTranslationThrows(error: Throwable = IllegalStateException("no translation")) {
        coEvery { quranUseCases.getAyahTranslation.invoke(any(), any()) } throws error
    }

    fun build(): SettingsViewModel = SettingsViewModel(
        prayerTimeCalculator = prayerTimeCalculator,
        appLocale = appLocale,
        adhanDownloader = adhanDownloader,
        prayerUseCases = prayerUseCases,
        settingsRepository = repo.mock,
        quranUseCases = quranUseCases,
        prayerAlarmScheduler = prayerAlarmScheduler,
        prayerNotificationTester = prayerNotificationTester,
        rescheduleNotificationsUseCase = rescheduleNotifications,
        telemetry = telemetry,
        adhanAudioManager = adhanAudioManager,
        quranPlayback = quranPlayback,
        clearAllUserData = clearAllUserData,
        todayProvider = todayProvider,
    )
}
