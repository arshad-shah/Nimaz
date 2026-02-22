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
import com.arshadshah.nimaz.presentation.screens.asmaunnabi.AsmaUnNabiDetailScreen
import com.arshadshah.nimaz.presentation.screens.asmaunnabi.AsmaUnNabiListScreen
import com.arshadshah.nimaz.presentation.theme.currentWindowSizeClass
import com.arshadshah.nimaz.presentation.theme.isCompact

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AdaptiveAsmaUnNabiScreen(
    navController: NavController,
    onNavigateBack: () -> Unit,
) {
    val windowSizeClass = currentWindowSizeClass()

    if (windowSizeClass.isCompact) {
        AsmaUnNabiListScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToDetail = { nameId ->
                navController.navigate(Route.AsmaUnNabiDetail(nameId))
            },
        )
    } else {
        val navigator = rememberListDetailPaneScaffoldNavigator<AsmaUnNabiDetailArgs>()
        val scope = rememberCoroutineScope()

        NavigableListDetailPaneScaffold(
            navigator = navigator,
            listPane = {
                AnimatedPane {
                    AsmaUnNabiListScreen(
                        onNavigateBack = onNavigateBack,
                        onNavigateToDetail = { nameId ->
                            scope.launch {
                                navigator.navigateTo(
                                    ListDetailPaneScaffoldRole.Detail,
                                    AsmaUnNabiDetailArgs(nameId = nameId)
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
                        AsmaUnNabiDetailScreen(
                            nameId = args.nameId,
                            onNavigateBack = { scope.launch { navigator.navigateBack() } },
                        )
                    }
                }
            }
        )
    }
}

@kotlinx.parcelize.Parcelize
data class AsmaUnNabiDetailArgs(
    val nameId: Int,
) : android.os.Parcelable
