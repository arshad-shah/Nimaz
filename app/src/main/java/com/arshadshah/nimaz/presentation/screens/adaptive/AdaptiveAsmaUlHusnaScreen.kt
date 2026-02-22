package com.arshadshah.nimaz.presentation.screens.adaptive

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import com.arshadshah.nimaz.core.navigation.Route
import com.arshadshah.nimaz.presentation.screens.asma.AsmaUlHusnaDetailScreen
import com.arshadshah.nimaz.presentation.screens.asma.AsmaUlHusnaListScreen
import com.arshadshah.nimaz.presentation.theme.currentWindowSizeClass
import com.arshadshah.nimaz.presentation.theme.isCompact

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AdaptiveAsmaUlHusnaScreen(
    navController: NavController,
    onNavigateBack: () -> Unit,
) {
    val windowSizeClass = currentWindowSizeClass()

    if (windowSizeClass.isCompact) {
        AsmaUlHusnaListScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToDetail = { nameId ->
                navController.navigate(Route.AsmaUlHusnaDetail(nameId))
            },
        )
    } else {
        val navigator = rememberListDetailPaneScaffoldNavigator<AsmaUlHusnaDetailArgs>()
        val scope = rememberCoroutineScope()

        NavigableListDetailPaneScaffold(
            navigator = navigator,
            listPane = {
                AnimatedPane {
                    AsmaUlHusnaListScreen(
                        onNavigateBack = onNavigateBack,
                        onNavigateToDetail = { nameId ->
                            scope.launch {
                                navigator.navigateTo(
                                    ListDetailPaneScaffoldRole.Detail,
                                    AsmaUlHusnaDetailArgs(nameId = nameId)
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
                        AsmaUlHusnaDetailScreen(
                            nameId = args.nameId,
                            onNavigateBack = { scope.launch { navigator.navigateBack() } },
                        )
                    }
                }
            }
        )
    }
}

@kotlinx.serialization.Serializable
data class AsmaUlHusnaDetailArgs(
    val nameId: Int,
)
