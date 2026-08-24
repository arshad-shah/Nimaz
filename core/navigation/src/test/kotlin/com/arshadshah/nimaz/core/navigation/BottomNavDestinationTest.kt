package com.arshadshah.nimaz.core.navigation

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The five tabs, and the one thing about them that is load-bearing beyond looking right.
 *
 * `ScreenTags.bottomNav(label)` derives a tab's test tag from its **title**, so two tabs sharing
 * a title collapse onto one tag — and every instrumented test that taps a tab by tag then taps
 * whichever of the two the tree happens to yield first. That failure surfaces on an emulator, in
 * a test that looks unrelated, which is the most expensive place to find it.
 *
 * Distinct routes matter for the ordinary reason: two tabs pointing at one destination leaves a
 * tab that never appears selected.
 */
class BottomNavDestinationTest {

    @Test
    fun `there are five tabs, in the order the bar draws them`() {
        assertThat(BottomNavDestination.entries.map { it.name })
            .containsExactly("HOME", "QURAN", "TASBIH", "QIBLA", "MORE")
            .inOrder()
    }

    @Test
    fun `no two tabs share a title, because the test tag is derived from it`() {
        val titles = BottomNavDestination.entries.map { it.title }

        assertThat(titles.toSet()).hasSize(titles.size)
    }

    @Test
    fun `every tab produces its own tag`() {
        val tags = BottomNavDestination.entries.map { ScreenTags.bottomNav(it.title) }

        assertThat(tags.toSet()).hasSize(BottomNavDestination.entries.size)
        assertThat(tags).contains("bottomnav_Home")
    }

    @Test
    fun `no two tabs lead to the same destination`() {
        val routes = BottomNavDestination.entries.map { it.route }

        assertThat(routes.toSet()).hasSize(routes.size)
    }

    @Test
    fun `every tab has a title and an icon`() {
        BottomNavDestination.entries.forEach {
            assertThat(it.title).isNotEmpty()
            assertThat(it.icon).isNotEmpty()
        }
    }

    @Test
    fun `no two tabs share an icon`() {
        val icons = BottomNavDestination.entries.map { it.icon }

        assertThat(icons.toSet()).hasSize(icons.size)
    }

    @Test
    fun `the qibla tab uses its own nav route, not the qibla screen's`() {
        // A bottom-nav tab and the screen it opens are two destinations: the tab has to be a
        // stable back-stack root, which is why `QiblaNav` exists beside `Qibla`.
        assertThat(BottomNavDestination.QIBLA.route).isEqualTo(Route.QiblaNav)
        assertThat(BottomNavDestination.QIBLA.route).isNotEqualTo(Route.Qibla)
    }
}
