package com.arshadshah.nimaz.core.navigation

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Which routes are tabs. A link that lands on a tab's *duplicate* — `TasbihHome`, `Qibla` — hid the
 * bottom bar, because the bar shows only on the five [BottomNavDestination] routes; and the Tasbih
 * screen, being a tab, has no back button. So every duplicate must resolve to its tab.
 */
class TopLevelNavigationTest {

    @Test
    fun `every bottom-bar tab is its own tab`() {
        BottomNavDestination.entries.forEach { dest ->
            assertThat(dest.route.asTab()).isEqualTo(dest.route)
        }
    }

    @Test
    fun `the tasbih and qibla duplicates resolve to their tabs`() {
        assertThat(Route.TasbihHome.asTab()).isEqualTo(Route.Tasbih)
        assertThat(Route.Qibla.asTab()).isEqualTo(Route.QiblaNav)
    }

    @Test
    fun `every tab asTab returns is one the bottom bar shows`() {
        val tabs = BottomNavDestination.entries.map { it.route }.toSet()
        listOf(Route.Home, Route.Quran, Route.Tasbih, Route.TasbihHome, Route.QiblaNav, Route.Qibla, Route.More)
            .forEach { assertThat(it.asTab()).isIn(tabs) }
    }

    @Test
    fun `an ordinary screen is not a tab`() {
        assertThat(Route.TasbihHistory.asTab()).isNull()
        assertThat(Route.QuranReader(2).asTab()).isNull()
        assertThat(Route.Settings.asTab()).isNull()
    }

    @Test
    fun `the announcement and help keys for tasbih and qibla land on tabs`() {
        assertThat(announcementRoute("tasbih")?.asTab()).isEqualTo(Route.Tasbih)
        assertThat(announcementRoute("qibla")?.asTab()).isEqualTo(Route.QiblaNav)
        assertThat(helpDeepLinkRoute("tasbih")?.asTab()).isEqualTo(Route.Tasbih)
        assertThat(helpDeepLinkRoute("qibla")?.asTab()).isEqualTo(Route.QiblaNav)
    }
}
