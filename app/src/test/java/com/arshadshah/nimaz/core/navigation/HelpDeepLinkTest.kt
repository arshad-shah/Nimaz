package com.arshadshah.nimaz.core.navigation

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HelpDeepLinkTest {
    @Test
    fun mapsKnownKeys() {
        assertThat(helpDeepLinkRoute("prayer_settings")).isEqualTo(Route.SettingsPrayerCalculation)
        assertThat(helpDeepLinkRoute("notifications")).isEqualTo(Route.SettingsNotifications)
        assertThat(helpDeepLinkRoute("location")).isEqualTo(Route.SettingsLocation)
        assertThat(helpDeepLinkRoute("qibla")).isEqualTo(Route.Qibla)
        assertThat(helpDeepLinkRoute("quran_settings")).isEqualTo(Route.SettingsQuran)
    }

    @Test
    fun unknownOrNullKeyReturnsNull() {
        assertThat(helpDeepLinkRoute("nope")).isNull()
        assertThat(helpDeepLinkRoute(null)).isNull()
    }
}
