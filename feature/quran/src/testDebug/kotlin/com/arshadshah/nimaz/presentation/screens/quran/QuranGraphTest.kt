package com.arshadshah.nimaz.presentation.screens.quran

import android.content.Context
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.createGraph
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.navigation.Route
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.reflect.KClass

/**
 * The Qur'an feature's slice of the route graph.
 *
 * Nineteen destinations were lifted out of `NavGraph.kt` when the feature moved into its own
 * module, and a destination that fails to register does not fail anything: it throws
 * `IllegalArgumentException: navigation destination … is not a direct child of this NavGraph`
 * the first time a *user* taps the thing that opens it, which for `KhatamEdit` or
 * `SurahBackground` can be a long way in.
 *
 * The graph is built here without a `NavHost`, so nothing is composed and no screen needs its
 * Hilt view model — this asserts registration, which is the part that can silently go missing.
 * That the screens themselves render is each screen test's job.
 */
@RunWith(RobolectricTestRunner::class)
class QuranGraphTest {

    private lateinit var navController: NavHostController

    @Before
    fun setUp() {
        // A controller without a host: enough to build the graph, and nothing is composed, so
        // no screen has to find a Hilt view model that is not there.
        navController = NavHostController(ApplicationProvider.getApplicationContext<Context>())
        navController.navigatorProvider.addNavigator(ComposeNavigator())
    }

    /** The graph as `:app`'s `NavHost` builds it, minus the composing. */
    private fun graph() = navController.createGraph(startDestination = Route.Quran) {
        quranGraph(navController)
    }

    private fun destinations(): List<NavDestination> = graph().toList()

    private fun hasDestination(route: KClass<*>): Boolean =
        destinations().any { it.route?.contains(route.qualifiedName.orEmpty()) == true }

    @Test
    fun `every Quran destination is registered`() {
        val registered = destinations().size

        // The count is asserted rather than eyeballed: a destination dropped in a refactor is
        // invisible until someone taps the row that opens it, and the individual checks below
        // only cover the ones a test thought to name.
        assertThat(registered).isEqualTo(19)
    }

    @Test
    fun `the reader, the mushaf and the juz reader are all reachable`() {
        // Three ways into the same screen, and each is its own route because each carries a
        // different argument.
        assertThat(hasDestination(Route.QuranReader::class)).isTrue()
        assertThat(hasDestination(Route.QuranPage::class)).isTrue()
        assertThat(hasDestination(Route.QuranJuz::class)).isTrue()
    }

    @Test
    fun `the thematic screens are reachable`() {
        assertThat(hasDestination(Route.SurahBackground::class)).isTrue()
        assertThat(hasDestination(Route.SurahPassages::class)).isTrue()
        assertThat(hasDestination(Route.SurahSubjects::class)).isTrue()
        assertThat(hasDestination(Route.QuranTopics::class)).isTrue()
        assertThat(hasDestination(Route.QuranTopicDetail::class)).isTrue()
    }

    @Test
    fun `the khatam screens are reachable, including the two that take an id`() {
        assertThat(hasDestination(Route.KhatamList::class)).isTrue()
        assertThat(hasDestination(Route.KhatamCreate::class)).isTrue()
        assertThat(hasDestination(Route.KhatamDetail::class)).isTrue()
        assertThat(hasDestination(Route.KhatamEdit::class)).isTrue()
    }

    @Test
    fun `the commentary screens are reachable`() {
        assertThat(hasDestination(Route.TafseerChapters::class)).isTrue()
        assertThat(hasDestination(Route.Tafseer::class)).isTrue()
    }

    @Test
    fun `all three saved surfaces are reachable`() {
        assertThat(hasDestination(Route.QuranSaved::class)).isTrue()
        assertThat(hasDestination(Route.HadithBookmarks::class)).isTrue()
        assertThat(hasDestination(Route.AllBookmarks::class)).isTrue()
    }

    @Test
    fun `no destination is registered twice`() {
        val routes = destinations().mapNotNull { it.route }

        // Room for a copy-paste to register one route under two tags, which resolves to
        // whichever won and is not the one anybody meant.
        assertThat(routes).containsNoDuplicates()
    }
}
