package com.arshadshah.nimaz.presentation.viewmodel.settings

import com.arshadshah.nimaz.domain.model.AudioState
import app.cash.turbine.test
import io.mockk.verify
import io.mockk.slot
import com.arshadshah.nimaz.domain.repository.AyahAudioItem
import com.arshadshah.nimaz.domain.model.PrayerAlertStyle
import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.domain.time.FakeTodayProvider
import com.arshadshah.nimaz.domain.repository.PrayerAlarmScheduler
import com.arshadshah.nimaz.domain.repository.PrayerNotificationTester
import com.arshadshah.nimaz.domain.repository.QuranPlayback
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

    /**
     * A real [MutableStateFlow] behind `audioState`, not a relaxed mock's.
     *
     * `reciterPreview` is a `combine` of this and the previewing id, so a mock flow that never
     * emits leaves the combine with nothing to produce and the assertion below waiting on an item
     * that cannot arrive. The mock is relaxed for everything else.
     */
    private val playbackAudioState = MutableStateFlow(AudioState())
    private val quranPlayback = mockk<QuranPlayback>(relaxed = true) {
        every { audioState } returns playbackAudioState
    }

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)

        val prayerTimeCalculator = mockk<PrayerTimeCalculator>(relaxed = true)
        val appLocale = mockk<AppLocale>(relaxed = true)
        val adhanDownloader = mockk<AdhanDownloader>(relaxed = true)
        val prayerUseCases = mockk<PrayerUseCases>(relaxed = true)
        val quranUseCases = mockk<QuranUseCases>(relaxed = true)
        // Three seams where there was one concrete class. `PrayerNotificationScheduler` cannot
        // leave `:app` — one `AppR.drawable` reference pins 917 lines — so PR 21 of #551 split
        // what this ViewModel actually calls into two `:core:domain` ports, and reciter preview
        // into a third.
        val prayerAlarmScheduler = mockk<PrayerAlarmScheduler>(relaxed = true)
        val prayerNotificationTester = mockk<PrayerNotificationTester>(relaxed = true)

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
            prayerAlarmScheduler = prayerAlarmScheduler,
            prayerNotificationTester = prayerNotificationTester,
            rescheduleNotificationsUseCase = rescheduleNotificationsUseCase,
            telemetry = telemetry,
            adhanAudioManager = adhanAudioManager,
            quranPlayback = quranPlayback,
            clearAllUserData = clearAllUserData,
            todayProvider = todayProvider,
        )
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `previewing a reciter plays an explicit one-ayah playlist`() = runTest {
        advanceUntilIdle()

        viewModel.onEvent(SettingsEvent.PreviewReciter("mishary"))
        advanceUntilIdle()

        verify { quranPlayback.setReciter("mishary", restartIfPlaying = false) }

        // The assertion that matters is the *contents* of the playlist, not that a call happened.
        // The previous implementation called `playFromAyah` too — with a list built from a fresh
        // `QuranViewModel`'s reader state, which on this destination is empty. `playFromAyah`
        // does `allAyahs.indexOfFirst { … }` and returns when that is -1, so the preview button
        // played silence. A `verify { … any() }` would have passed against that bug.
        val playlist = slot<List<AyahAudioItem>>()
        verify { quranPlayback.playFromAyah(ayahGlobalId = 1, allAyahs = capture(playlist), title = any()) }
        assertThat(playlist.captured).hasSize(1)
        assertThat(playlist.captured.single().ayahGlobalId).isEqualTo(1)

        // Collected rather than read off `.value`: `reciterPreview` is `stateIn(…,
        // WhileSubscribed(5s), …)`, so with no subscriber it never runs the `combine` and holds
        // its seed. Reading `.value` here asserted `null` and would have kept asserting `null`
        // however the ViewModel behaved.
        viewModel.reciterPreview.test {
            // `stateIn` replays its seed to a new subscriber before the `combine` runs, so the
            // first item is always the empty default.
            assertThat(awaitItem().reciterId).isNull()
            assertThat(awaitItem().reciterId).isEqualTo("mishary")
        }
    }

    @Test
    fun `stopping a reciter preview clears the previewing id`() = runTest {
        advanceUntilIdle()
        viewModel.onEvent(SettingsEvent.PreviewReciter("mishary"))
        advanceUntilIdle()

        viewModel.onEvent(SettingsEvent.StopReciterPreview)
        advanceUntilIdle()

        verify { quranPlayback.stop() }
        viewModel.reciterPreview.test {
            assertThat(awaitItem().reciterId).isNull()
        }
    }

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
