package com.arshadshah.nimaz.presentation.viewmodel.home

import com.arshadshah.nimaz.domain.model.PrayerType

sealed interface HomeEvent {
    data object RefreshPrayerTimes : HomeEvent
    data object RefreshPermissions : HomeEvent
    data class TogglePrayerStatus(val prayerType: PrayerType) : HomeEvent
    data object DismissAnnouncement : HomeEvent
    data object AnnouncementCtaClicked : HomeEvent
}
