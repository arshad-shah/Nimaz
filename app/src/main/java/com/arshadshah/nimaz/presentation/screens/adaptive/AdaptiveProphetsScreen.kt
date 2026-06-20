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
import com.arshadshah.nimaz.presentation.screens.prophets.ProphetDetailScreen
import com.arshadshah.nimaz.presentation.screens.prophets.ProphetsListScreen
import com.arshadshah.nimaz.presentation.theme.currentWindowSizeClass
import com.arshadshah.nimaz.presentation.theme.isCompact
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AdaptiveProphetsScreen(
    navController: NavController,
    onNavigateBack: () -> Unit,
) {
    val windowSizeClass = currentWindowSizeClass()

    if (windowSizeClass.isCompact) {
        ProphetsListScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToDetail = { prophetId ->
                navController.navigate(Route.ProphetDetail(prophetId))
            },
        )
    } else {
        val navigator = rememberListDetailPaneScaffoldNavigator<ProphetDetailArgs>()
        val scope = rememberCoroutineScope()

        NavigableListDetailPaneScaffold(
            navigator = navigator,
            listPane = {
                AnimatedPane {
                    ProphetsListScreen(
                        onNavigateBack = onNavigateBack,
                        onNavigateToDetail = { prophetId ->
                            scope.launch {
                                navigator.navigateTo(
                                    ListDetailPaneScaffoldRole.Detail,
                                    ProphetDetailArgs(prophetId = prophetId)
                                )
                            }
                        },
                    )
                }
            },
            detailPane = {
                AnimatedPane {
                    val args = navigator.currentDestination?.contentKey
                    if (args != null) {
                        ProphetDetailScreen(
                            prophetId = args.prophetId,
                            onNavigateBack = { scope.launch { navigator.navigateBack() } },
                        )
                    }
                }
            }
        )
    }
}

@kotlinx.parcelize.Parcelize
data class ProphetDetailArgs(
    val prophetId: Int,
) : android.os.Parcelable
