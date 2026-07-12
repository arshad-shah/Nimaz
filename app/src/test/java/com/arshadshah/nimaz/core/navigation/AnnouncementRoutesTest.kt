package com.arshadshah.nimaz.core.navigation

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AnnouncementRoutesTest {

    @Test
    fun `known keys resolve to routes`() {
        assertThat(announcementRoute("home")).isEqualTo(Route.Home)
        assertThat(announcementRoute("quran")).isEqualTo(Route.Quran)
        assertThat(announcementRoute("search/ask")).isEqualTo(Route.GlobalSearch)
        assertThat(announcementRoute("search/settings")).isEqualTo(Route.SearchSettings)
        assertThat(announcementRoute("prayer/tracker")).isEqualTo(Route.PrayerTracker())
        assertThat(announcementRoute("settings/about")).isEqualTo(Route.SettingsAbout)
        assertThat(announcementRoute("khatam")).isEqualTo(Route.KhatamList)
    }

    @Test
    fun `unknown key resolves to null`() {
        assertThat(announcementRoute("brand/new/feature")).isNull()
        assertThat(announcementRoute("")).isNull()
        assertThat(announcementRoute(null)).isNull()
    }

    @Test
    fun `urls are not feature keys`() {
        assertThat(announcementRoute("https://nimaz.arshadshah.com/privacy")).isNull()
    }
}
