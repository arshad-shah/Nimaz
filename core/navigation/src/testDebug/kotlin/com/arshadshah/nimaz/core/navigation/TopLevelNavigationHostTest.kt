package com.arshadshah.nimaz.core.navigation

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Where a link actually lands, on a real `NavHost`.
 *
 * The bottom bar shows only on the five tab routes. An announcement for `tasbih` used to push
 * `TasbihHome` — the same screen under another name, which is not a tab — so the bar vanished and
 * the Tasbih screen, having no back button, left the reader stuck. These pin that every way in
 * lands on the tab itself, and that a link to Home over Settings lands on Home rather than on the
 * saved Settings the bar's `restoreState` would bring back.
 */
@RunWith(RobolectricTestRunner::class)
class TopLevelNavigationHostTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var nav: NavHostController

    private fun host() {
        composeRule.setContent {
            nav = rememberNavController()
            NavHost(navController = nav, startDestination = Route.Home) {
                taggedComposable<Route.Home>(ScreenTags.Home) { Text("home") }
                taggedComposable<Route.Tasbih>(ScreenTags.Tasbih) { Text("tasbih tab") }
                taggedComposable<Route.TasbihHome>(ScreenTags.TasbihHome) { Text("tasbih copy") }
                taggedComposable<Route.QiblaNav>(ScreenTags.QiblaNav) { Text("qibla tab") }
                taggedComposable<Route.Qibla>(ScreenTags.Qibla) { Text("qibla copy") }
                taggedComposable<Route.Settings>(ScreenTags.Settings) { Text("settings") }
                taggedComposable<Route.TasbihHistory>(ScreenTags.TasbihHistory) { Text("history") }
            }
        }
        composeRule.waitForIdle()
    }

    private fun go(block: NavHostController.() -> Unit) {
        composeRule.runOnIdle { nav.block() }
        composeRule.waitForIdle()
    }

    private inline fun <reified T : Any> isShowing(): Boolean =
        nav.currentBackStackEntry?.destination?.hasRoute(T::class) == true

    private fun stack(): List<String?> =
        nav.currentBackStack.value.mapNotNull { it.destination.route?.substringAfterLast('.') }

    @Test
    fun `a link to the tasbih duplicate lands on the tasbih tab`() {
        host()
        go { navigateFromOutside(Route.TasbihHome) }
        assertThat(isShowing<Route.Tasbih>()).isTrue()
    }

    @Test
    fun `a link to the qibla duplicate lands on the qibla tab`() {
        host()
        go { navigateFromOutside(Route.Qibla) }
        assertThat(isShowing<Route.QiblaNav>()).isTrue()
    }

    @Test
    fun `a link to home over settings lands on home, not on a restored settings`() {
        host()
        go { navigate(Route.Settings) }
        go { navigateFromOutside(Route.Home) }
        assertThat(isShowing<Route.Home>()).isTrue()
        assertThat(stack()).doesNotContain("Settings")
    }

    @Test
    fun `an ordinary link lands on top of home, once`() {
        host()
        go { navigateFromOutside(Route.TasbihHistory) }
        go { navigateFromOutside(Route.TasbihHistory) }
        assertThat(isShowing<Route.TasbihHistory>()).isTrue()
        assertThat(stack().count { it == "TasbihHistory" }).isEqualTo(1)
        assertThat(stack().first()).isEqualTo("Home")
    }

    @Test
    fun `in the app a tab duplicate becomes the tab, and a screen is a push`() {
        host()
        go { navigateInApp(Route.TasbihHome) }
        assertThat(isShowing<Route.Tasbih>()).isTrue()
        go { navigateInApp(Route.TasbihHistory) }
        assertThat(isShowing<Route.TasbihHistory>()).isTrue()
    }

    @Test
    fun `switching tabs keeps a single copy of the tab`() {
        host()
        go { navigateToTab(Route.Tasbih) }
        go { navigateToTab(Route.Tasbih) }
        assertThat(stack().count { it == "Tasbih" }).isEqualTo(1)
    }
}
