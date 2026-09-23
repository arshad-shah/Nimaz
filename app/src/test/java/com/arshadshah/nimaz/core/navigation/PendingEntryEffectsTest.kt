package com.arshadshah.nimaz.core.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * What each outside entry point does once `MainActivity` has handed it over.
 *
 * Every one of these was a `LaunchedEffect` inside `NavGraph` that no test reached, and two of
 * them were wrong in ways only a device showed: an announcement for `tasbih` landed on a
 * duplicate of the Tasbih tab with no bottom bar, and a worship reminder opened nothing at all.
 * Here they run on a real `NavHost`, and each is checked to navigate once and be consumed.
 */
@RunWith(RobolectricTestRunner::class)
class PendingEntryEffectsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var nav: NavHostController
    private var quranSurah by mutableStateOf<Int?>(null)
    private var calendar by mutableStateOf(false)
    private var announcement by mutableStateOf<String?>(null)
    private var route by mutableStateOf<Route?>(null)
    private val opened = mutableListOf<String>()
    private val consumed = mutableListOf<String>()

    private fun host() {
        composeRule.setContent {
            nav = rememberNavController()
            NavHost(navController = nav, startDestination = Route.Home) {
                composable<Route.Home> { Text("home") }
                composable<Route.Tasbih> { Text("tasbih tab") }
                composable<Route.TasbihHome> { Text("tasbih copy") }
                composable<Route.QuranReader> { Text("reader") }
                composable<Route.IslamicCalendar> { Text("calendar") }
                composable<Route.NightWorship> { Text("night") }
                composable<Route.SettingsAbout> { Text("about") }
            }
            PendingEntryEffects(
                navController = nav,
                pendingQuranSurah = quranSurah,
                onPendingQuranSurahConsumed = { consumed += "quran"; quranSurah = null },
                pendingIslamicCalendar = calendar,
                onPendingIslamicCalendarConsumed = { consumed += "calendar"; calendar = false },
                pendingAnnouncementRoute = announcement,
                onPendingAnnouncementRouteConsumed = { consumed += "announcement"; announcement = null },
                pendingRoute = route,
                onPendingRouteConsumed = { consumed += "route"; route = null },
                openUrl = { opened += it },
            )
        }
        composeRule.waitForIdle()
    }

    private fun set(block: () -> Unit) {
        composeRule.runOnIdle(block)
        composeRule.waitForIdle()
    }

    private inline fun <reified T : Any> showing(): Boolean =
        nav.currentBackStackEntry?.destination?.hasRoute(T::class) == true

    private fun stack() = nav.currentBackStack.value.mapNotNull { it.destination.route?.substringAfterLast('.') }

    @Test
    fun `nothing pending navigates nowhere`() {
        host()
        assertThat(showing<Route.Home>()).isTrue()
        assertThat(consumed).isEmpty()
    }

    @Test
    fun `the quran audio notification opens the surah playing, over home`() {
        host()
        set { quranSurah = 18 }
        assertThat(showing<Route.QuranReader>()).isTrue()
        assertThat(stack().first()).isEqualTo("Home")
        assertThat(consumed).containsExactly("quran")
    }

    @Test
    fun `the calendar widget opens the calendar`() {
        host()
        set { calendar = true }
        assertThat(showing<Route.IslamicCalendar>()).isTrue()
        assertThat(consumed).containsExactly("calendar")
    }

    @Test
    fun `a worship reminder opens the screen it is about`() {
        host()
        set { route = Route.NightWorship }
        assertThat(showing<Route.NightWorship>()).isTrue()
        assertThat(consumed).containsExactly("route")
    }

    @Test
    fun `an announcement for tasbih lands on the tasbih tab, not its duplicate`() {
        host()
        set { announcement = "tasbih" }
        assertThat(showing<Route.Tasbih>()).isTrue()
        assertThat(consumed).containsExactly("announcement")
    }

    @Test
    fun `an announcement for a screen lands on it`() {
        host()
        set { announcement = "settings/about" }
        assertThat(showing<Route.SettingsAbout>()).isTrue()
    }

    @Test
    fun `an unknown announcement key is consumed without navigating`() {
        host()
        set { announcement = "not/a/route" }
        assertThat(showing<Route.Home>()).isTrue()
        assertThat(consumed).containsExactly("announcement")
    }

    @Test
    fun `an https announcement opens the browser rather than a screen`() {
        host()
        set { announcement = "https://nimaz.arshadshah.com/changelog" }
        assertThat(opened).containsExactly("https://nimaz.arshadshah.com/changelog")
        assertThat(showing<Route.Home>()).isTrue()
    }
}
