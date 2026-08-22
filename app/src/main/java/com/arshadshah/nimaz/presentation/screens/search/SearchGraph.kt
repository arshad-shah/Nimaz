package com.arshadshah.nimaz.presentation.screens.search

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.toRoute
import com.arshadshah.nimaz.core.navigation.Route
import com.arshadshah.nimaz.core.navigation.ScreenTags
import com.arshadshah.nimaz.core.navigation.taggedComposable
import com.arshadshah.nimaz.core.navigation.toRoute
import com.arshadshah.nimaz.domain.model.NameCatalog
import com.arshadshah.nimaz.presentation.viewmodel.search.SearchFilter

/**
 * The 4 Search destinations — search and the Ask-with-Proof flow.
 *
 * Split out of `NavGraph.kt` in PR 12 of #551. That file registered all 94 destinations and
 * imported 69 screen composables, which meant every screen in the app was reachable from one
 * place — and no feature could move into its own module while that was true, because `:app`
 * would have had to import from all eleven feature modules at once.
 *
 * The bodies are unchanged; only their location is. `:app` keeps the `NavHost` and calls this.
 *
 * It takes a `NavController` rather than the `onNavigate` lambda #563 sketches because 11 of the
 * 158 `navigate` calls in these blocks pass a `NavOptionsBuilder` — `popUpTo`, `launchSingleTop` —
 * which `(Route) -> Unit` cannot express, and flattening them would change back-stack behaviour
 * silently. A graph function *is* navigation wiring, so holding the controller is what it is for;
 * the rule that matters is that a **screen** must not, which `NavControllerConfinementTest`
 * enforces.
 */
fun NavGraphBuilder.searchGraph(navController: NavController) {
    taggedComposable<Route.QuranSearch>(ScreenTags.QuranSearch) {
        SearchScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToQuranAyah = { surah, ayah ->
                navController.navigate(Route.QuranReader(surah, ayah))
            },
            onNavigateToSurah = { surah ->
                navController.navigate(Route.QuranReader(surah))
            },
            onNavigateToHadith = { bookId, hadithId ->
                navController.navigate(Route.HadithReader(hadithId))
            },
            onNavigateToDua = { duaId ->
                navController.navigate(Route.DuaReader(duaId))
            },
            onNavigateToName = { catalog, id ->
                navController.navigate(
                    when (catalog) {
                        NameCatalog.ASMA_UL_HUSNA -> Route.AsmaUlHusnaDetail(id)
                        NameCatalog.ASMA_UN_NABI -> Route.AsmaUnNabiDetail(id)
                        NameCatalog.PROPHETS -> Route.ProphetDetail(id)
                    }
                )
            },
        )
    }

    taggedComposable<Route.HadithSearch>(ScreenTags.HadithSearch) {
        SearchScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToQuranAyah = { surah, ayah ->
                navController.navigate(Route.QuranReader(surah, ayah))
            },
            onNavigateToSurah = { surah ->
                navController.navigate(Route.QuranReader(surah))
            },
            onNavigateToHadith = { bookId, hadithId ->
                navController.navigate(Route.HadithReader(hadithId))
            },
            onNavigateToDua = { duaId ->
                navController.navigate(Route.DuaReader(duaId))
            },
            onNavigateToName = { catalog, id ->
                navController.navigate(
                    when (catalog) {
                        NameCatalog.ASMA_UL_HUSNA -> Route.AsmaUlHusnaDetail(id)
                        NameCatalog.ASMA_UN_NABI -> Route.AsmaUnNabiDetail(id)
                        NameCatalog.PROPHETS -> Route.ProphetDetail(id)
                    }
                )
            },
        )
    }

    taggedComposable<Route.DuaSearch>(ScreenTags.DuaSearch) {
        SearchScreen(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToQuranAyah = { surah, ayah ->
                navController.navigate(Route.QuranReader(surah, ayah))
            },
            onNavigateToSurah = { surah ->
                navController.navigate(Route.QuranReader(surah))
            },
            onNavigateToHadith = { bookId, hadithId ->
                navController.navigate(Route.HadithReader(hadithId))
            },
            onNavigateToDua = { duaId ->
                navController.navigate(Route.DuaReader(duaId))
            },
            onNavigateToName = { catalog, id ->
                navController.navigate(
                    when (catalog) {
                        NameCatalog.ASMA_UL_HUSNA -> Route.AsmaUlHusnaDetail(id)
                        NameCatalog.ASMA_UN_NABI -> Route.AsmaUnNabiDetail(id)
                        NameCatalog.PROPHETS -> Route.ProphetDetail(id)
                    }
                )
            },
            initialFilter = SearchFilter.DUA
        )
    }

    // Global Search
    taggedComposable<Route.GlobalSearch>(ScreenTags.GlobalSearch) {
        SearchScreen(
            enableAsk = true,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToQuranAyah = { surah, ayah ->
                navController.navigate(Route.QuranReader(surah, ayah))
            },
            onNavigateToSurah = { surah ->
                navController.navigate(Route.QuranReader(surah))
            },
            onNavigateToHadith = { bookId, hadithId ->
                navController.navigate(Route.HadithReader(hadithId))
            },
            onNavigateToDua = { duaId ->
                navController.navigate(Route.DuaReader(duaId))
            },
            onNavigateToName = { catalog, id ->
                navController.navigate(
                    when (catalog) {
                        NameCatalog.ASMA_UL_HUSNA -> Route.AsmaUlHusnaDetail(id)
                        NameCatalog.ASMA_UN_NABI -> Route.AsmaUnNabiDetail(id)
                        NameCatalog.PROPHETS -> Route.ProphetDetail(id)
                    }
                )
            },
            onNavigateToSearchSettings = { navController.navigate(Route.SearchSettings) },
            onNavigateToProof = { target -> navController.navigate(target.toRoute()) }
        )
    }
}
