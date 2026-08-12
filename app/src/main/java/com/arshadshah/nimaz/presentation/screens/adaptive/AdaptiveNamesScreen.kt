package com.arshadshah.nimaz.presentation.screens.adaptive

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavController
import com.arshadshah.nimaz.core.navigation.Route
import com.arshadshah.nimaz.domain.model.NameCatalog
import com.arshadshah.nimaz.presentation.screens.asma.AsmaUlHusnaDetailScreen
import com.arshadshah.nimaz.presentation.screens.asmaunnabi.AsmaUnNabiDetailScreen
import com.arshadshah.nimaz.presentation.screens.names.NamesScreen
import com.arshadshah.nimaz.presentation.screens.names.NamesTab
import com.arshadshah.nimaz.presentation.screens.prophets.ProphetDetailScreen
import com.arshadshah.nimaz.presentation.theme.currentWindowSizeClass
import com.arshadshah.nimaz.presentation.theme.isCompact
import kotlinx.coroutines.launch

/**
 * Names, two-pane on anything wider than a phone.
 *
 * This replaces `AdaptiveAsmaUlHusnaScreen`, `AdaptiveAsmaUnNabiScreen` and
 * `AdaptiveProphetsScreen` — three files that differed only in which list and which detail
 * they named. One scaffold serves all three tabs because the detail pane is keyed by
 * [NameDetailArgs], which carries the catalogue as well as the id.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AdaptiveNamesScreen(
    navController: NavController,
    initialTab: NamesTab,
    onNavigateBack: () -> Unit,
) {
    val windowSizeClass = currentWindowSizeClass()

    if (windowSizeClass.isCompact) {
        NamesScreen(
            initialTab = initialTab,
            onNavigateBack = onNavigateBack,
            onNavigateToFavourites = { navController.navigate(Route.Favourites) },
            onNavigateToAsmaUlHusna = { navController.navigate(Route.AsmaUlHusnaDetail(it)) },
            onNavigateToAsmaUnNabi = { navController.navigate(Route.AsmaUnNabiDetail(it)) },
            onNavigateToProphet = { navController.navigate(Route.ProphetDetail(it)) },
        )
        return
    }

    val navigator = rememberListDetailPaneScaffoldNavigator<NameDetailArgs>()
    val scope = rememberCoroutineScope()

    fun openDetail(catalog: NameCatalog, id: Int) {
        scope.launch {
            navigator.navigateTo(
                ListDetailPaneScaffoldRole.Detail,
                NameDetailArgs(catalog = catalog, id = id),
            )
        }
    }

    NavigableListDetailPaneScaffold(
        navigator = navigator,
        listPane = {
            AnimatedPane {
                NamesScreen(
                    initialTab = initialTab,
                    onNavigateBack = onNavigateBack,
                    onNavigateToFavourites = { navController.navigate(Route.Favourites) },
                    onNavigateToAsmaUlHusna = { openDetail(NameCatalog.ASMA_UL_HUSNA, it) },
                    onNavigateToAsmaUnNabi = { openDetail(NameCatalog.ASMA_UN_NABI, it) },
                    onNavigateToProphet = { openDetail(NameCatalog.PROPHETS, it) },
                )
            }
        },
        detailPane = {
            AnimatedPane {
                val args = navigator.currentDestination?.contentKey ?: return@AnimatedPane
                val back = { scope.launch { navigator.navigateBack() }; Unit }
                when (args.catalog) {
                    NameCatalog.ASMA_UL_HUSNA ->
                        AsmaUlHusnaDetailScreen(nameId = args.id, onNavigateBack = back)

                    NameCatalog.ASMA_UN_NABI ->
                        AsmaUnNabiDetailScreen(nameId = args.id, onNavigateBack = back)

                    NameCatalog.PROPHETS ->
                        ProphetDetailScreen(prophetId = args.id, onNavigateBack = back)
                }
            }
        },
    )
}

@kotlinx.parcelize.Parcelize
data class NameDetailArgs(
    val catalog: NameCatalog,
    val id: Int,
) : android.os.Parcelable
