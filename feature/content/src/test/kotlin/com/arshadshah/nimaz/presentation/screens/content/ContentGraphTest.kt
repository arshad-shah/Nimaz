package com.arshadshah.nimaz.presentation.screens.content

import android.content.Context
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.createGraph
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.navigation.NamesTab
import com.arshadshah.nimaz.core.navigation.Route
import com.arshadshah.nimaz.domain.model.DuaOccasion
import com.arshadshah.nimaz.domain.model.HadithGrade
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The Content feature's slice of the route graph — the largest single graph in the app.
 *
 * Nineteen destinations across five surfaces (qaida, hadith, dua, the name catalogues, the
 * prophets), and **eleven of them carry arguments**, which is what makes a graph test worth
 * more here than in most modules: an unregistered destination throws
 * `IllegalArgumentException: navigation destination … is not a direct child of this NavGraph`
 * at the moment a **user taps the row** — not at build time, and not in any of the four gates
 * `CLAUDE.md` lists.
 *
 * Four of the hadith destinations are the *same screen* reached four ways — chapter, hadith id,
 * hadith number, grade — and they exist as separate routes precisely so the reader can tell
 * which it was asked for (see `HadithReaderScreenTest`). Losing one of them does not break the
 * others, so the count is asserted as well as the membership.
 *
 * The graph is built without a `NavHost`, so nothing composes and none of these screens' Hilt
 * ViewModels is constructed. This asserts registration only; the screen tests beside it assert
 * what each destination renders.
 */
@RunWith(RobolectricTestRunner::class)
class ContentGraphTest {

    private lateinit var navController: NavHostController

    @Before
    fun setUp() {
        navController = NavHostController(ApplicationProvider.getApplicationContext<Context>())
        navController.navigatorProvider.addNavigator(ComposeNavigator())
    }

    private fun destinations(): List<NavDestination> =
        navController.createGraph(startDestination = Route.DuaHome) {
            contentGraph(navController)
        }.toList()

    @Test
    fun `all nineteen content destinations are registered`() {
        val routes = destinations().mapNotNull { it.route }

        assertThat(routes).hasSize(19)
        listOf(
            Route.QaidaHome::class,
            Route.QaidaReader::class,
            Route.QaidaLetters::class,
            Route.HadithHome::class,
            Route.HadithBook::class,
            Route.HadithChapter::class,
            Route.HadithReader::class,
            Route.HadithByNumber::class,
            Route.HadithByGrade::class,
            Route.DuaHome::class,
            Route.DuaCategory::class,
            Route.DuaOccasion::class,
            Route.DuaReader::class,
            Route.DuaFavorites::class,
            Route.Names::class,
            Route.Favourites::class,
            Route.AsmaUlHusnaDetail::class,
            Route.AsmaUnNabiDetail::class,
            Route.ProphetDetail::class,
        ).forEach { route ->
            assertThat(routes.any { it.contains(route.qualifiedName!!) }).isTrue()
        }
    }

    @Test
    fun `the three qaida destinations resolve in their own right`() {
        // The course map is opened from the More menu, the letter explorer from the map's app
        // bar, and a lesson from either the trail or the map's "continue" button.
        assertStartDestination(Route.QaidaHome)
        assertStartDestination(Route.QaidaLetters)
        assertStartDestination(Route.QaidaReader(lessonId = 3))
    }

    @Test
    fun `every way into the hadith reader resolves`() {
        // Four routes onto one screen. Each is reached from somewhere different — the chapter
        // list, search, a bookmark, and the collection screen's grade pills — so losing one
        // breaks exactly one of those and none of the others.
        assertStartDestination(Route.HadithChapter(bookId = "bukhari", chapterId = "bukhari_1"))
        assertStartDestination(Route.HadithReader(hadithId = "bukhari-1-1"))
        assertStartDestination(Route.HadithByNumber(bookId = "muslim", hadithNumber = 2564))
        assertStartDestination(Route.HadithByGrade(grade = HadithGrade.SAHIH.name))
    }

    @Test
    fun `the hadith library and one collection resolve`() {
        assertStartDestination(Route.HadithHome)
        assertStartDestination(Route.HadithBook(bookId = "bukhari"))
    }

    @Test
    fun `the dua library, a category, an occasion and one dua resolve`() {
        // `DuaOccasion` is the cross-cut that shipped in the database and the repository before
        // anything could reach it; its registration here is what makes the badge on a dua row
        // do something.
        assertStartDestination(Route.DuaHome)
        assertStartDestination(Route.DuaCategory(categoryId = "morning"))
        assertStartDestination(Route.DuaOccasion(occasion = DuaOccasion.TRAVELING.name))
        assertStartDestination(Route.DuaReader(duaId = "dua_1"))
        assertStartDestination(Route.DuaFavorites)
    }

    @Test
    fun `the names tabs and the consolidated favourites resolve`() {
        // `Route.Names` carries the tab ordinal, so all three tabs are one destination and a
        // deep link naming any of them lands here.
        assertStartDestination(Route.Names(tab = NamesTab.ASMA_UL_HUSNA.ordinal))
        assertStartDestination(Route.Names(tab = NamesTab.PROPHETS.ordinal))
        assertStartDestination(Route.Favourites)
    }

    @Test
    fun `all three catalogue detail routes resolve`() {
        assertStartDestination(Route.AsmaUlHusnaDetail(nameId = 1))
        assertStartDestination(Route.AsmaUnNabiDetail(nameId = 1))
        assertStartDestination(Route.ProphetDetail(prophetId = 1))
    }

    private fun assertStartDestination(route: Route) {
        // `createGraph` throws here if the route it is handed is not among the destinations the
        // builder registered — the same failure the app would hit on the first tap.
        val graph = navController.createGraph(startDestination = route) {
            contentGraph(navController)
        }

        assertThat(graph.findNode(graph.startDestinationId)).isNotNull()
    }
}
