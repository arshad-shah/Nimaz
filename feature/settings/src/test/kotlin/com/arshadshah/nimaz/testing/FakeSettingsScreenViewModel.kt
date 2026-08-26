package com.arshadshah.nimaz.testing

import com.arshadshah.nimaz.data.audio.AdhanSound
import com.arshadshah.nimaz.data.audio.DownloadState
import com.arshadshah.nimaz.domain.model.PrayerTimes
import com.arshadshah.nimaz.domain.model.UserPreferences
import com.arshadshah.nimaz.presentation.viewmodel.settings.DuaSettingsUiState
import com.arshadshah.nimaz.presentation.viewmodel.settings.GeneralSettingsUiState
import com.arshadshah.nimaz.presentation.viewmodel.settings.HadithSettingsUiState
import com.arshadshah.nimaz.presentation.viewmodel.settings.LocationSettingsUiState
import com.arshadshah.nimaz.presentation.viewmodel.settings.NotificationSettingsUiState
import com.arshadshah.nimaz.presentation.viewmodel.settings.NotificationSummary
import com.arshadshah.nimaz.presentation.viewmodel.settings.PrayerSettingsUiState
import com.arshadshah.nimaz.presentation.viewmodel.settings.QuranSettingsUiState
import com.arshadshah.nimaz.presentation.viewmodel.settings.ReciterPreviewUiState
import com.arshadshah.nimaz.presentation.viewmodel.settings.SettingsEvent
import com.arshadshah.nimaz.presentation.viewmodel.settings.SettingsViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A `SettingsViewModel` for a **screen** test: every state holder a screen might collect, and a
 * list of the events it dispatched.
 *
 * Nineteen of this module's twenty-four screens take `SettingsViewModel`, and each collects two or
 * three of its eleven state flows. Mocking only the ones a given screen reads works right up until
 * a refactor makes it read a fourth, at which point the test fails inside Compose with a null flow
 * and no indication which one. Stubbing all of them once is both shorter and steadier.
 *
 * [events] is the assertion surface. A settings screen's whole job is to turn a tap into the right
 * `SettingsEvent`, so what the tests check is the event, not a repository call — the ViewModel's
 * own tests own the half after that.
 */
class FakeSettingsScreenViewModel {

    val generalState = MutableStateFlow(GeneralSettingsUiState())
    val prayerState = MutableStateFlow(PrayerSettingsUiState())
    val notificationState = MutableStateFlow(NotificationSettingsUiState())
    val quranState = MutableStateFlow(QuranSettingsUiState())
    val duaState = MutableStateFlow(DuaSettingsUiState())
    val hadithState = MutableStateFlow(HadithSettingsUiState())
    val locationState = MutableStateFlow(LocationSettingsUiState(isLoading = false))
    val notificationSummary = MutableStateFlow(NotificationSummary())
    val reciterPreview = MutableStateFlow(ReciterPreviewUiState())
    val todayPrayerTimes = MutableStateFlow<PrayerTimes?>(null)
    val widgetPreviewPreferences = MutableStateFlow<UserPreferences?>(null)
    val shouldRestart = MutableStateFlow(false)
    val adhanPreviewError = MutableStateFlow<String?>(null)
    val adhanDownloadState = MutableStateFlow<Map<AdhanSound, DownloadState>>(emptyMap())
    val isAdhanPlaying = MutableStateFlow(false)
    val currentlyPlayingAdhan = MutableStateFlow<AdhanSound?>(null)

    /** Everything the screen under test dispatched, in order. */
    val events = mutableListOf<SettingsEvent>()

    val mock: SettingsViewModel = mockk(relaxed = true) {
        every { generalState } returns this@FakeSettingsScreenViewModel.generalState
        every { prayerState } returns this@FakeSettingsScreenViewModel.prayerState
        every { notificationState } returns this@FakeSettingsScreenViewModel.notificationState
        every { quranState } returns this@FakeSettingsScreenViewModel.quranState
        every { duaState } returns this@FakeSettingsScreenViewModel.duaState
        every { hadithState } returns this@FakeSettingsScreenViewModel.hadithState
        every { locationState } returns this@FakeSettingsScreenViewModel.locationState
        every {
            notificationSummary
        } returns this@FakeSettingsScreenViewModel.notificationSummary
        every { reciterPreview } returns this@FakeSettingsScreenViewModel.reciterPreview
        every { todayPrayerTimes } returns this@FakeSettingsScreenViewModel.todayPrayerTimes
        every {
            widgetPreviewPreferences
        } returns this@FakeSettingsScreenViewModel.widgetPreviewPreferences
        every { shouldRestart } returns this@FakeSettingsScreenViewModel.shouldRestart
        every { adhanPreviewError } returns this@FakeSettingsScreenViewModel.adhanPreviewError
        every { adhanDownloadState } returns this@FakeSettingsScreenViewModel.adhanDownloadState
        every { isAdhanPlaying } returns this@FakeSettingsScreenViewModel.isAdhanPlaying
        every {
            currentlyPlayingAdhan
        } returns this@FakeSettingsScreenViewModel.currentlyPlayingAdhan
        every { onEvent(any()) } answers {
            this@FakeSettingsScreenViewModel.events += firstArg<SettingsEvent>()
        }
    }

    /** The single event of type [T] the screen dispatched, failing the test if there is not one. */
    inline fun <reified T : SettingsEvent> only(): T {
        val matches = events.filterIsInstance<T>()
        check(matches.size == 1) {
            "expected exactly one ${T::class.simpleName}, got ${matches.size} in $events"
        }
        return matches.single()
    }
}
