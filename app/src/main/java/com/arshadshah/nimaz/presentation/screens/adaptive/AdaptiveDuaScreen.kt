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
import com.arshadshah.nimaz.presentation.screens.dua.DuaCategoryScreen
import com.arshadshah.nimaz.presentation.screens.dua.DuaReaderScreen
import com.arshadshah.nimaz.presentation.screens.dua.DuasCollectionScreen
import com.arshadshah.nimaz.presentation.theme.currentWindowSizeClass
import com.arshadshah.nimaz.presentation.theme.isCompact

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AdaptiveDuaScreen(
    navController: NavController,
    onNavigateBack: () -> Unit,
    onNavigateToBookmarks: () -> Unit,
) {
    val windowSizeClass = currentWindowSizeClass()

    if (windowSizeClass.isCompact) {
        DuasCollectionScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToCategory = { categoryId ->
                navController.navigate(Route.DuaCategory(categoryId))
            },
            onNavigateToBookmarks = onNavigateToBookmarks,
        )
    } else {
        val navigator = rememberListDetailPaneScaffoldNavigator<DuaDetailArgs>()
        val scope = rememberCoroutineScope()

        NavigableListDetailPaneScaffold(
            navigator = navigator,
            listPane = {
                AnimatedPane {
                    DuasCollectionScreen(
                        onNavigateBack = onNavigateBack,
                        onNavigateToCategory = { categoryId ->
                            scope.launch {
                                navigator.navigateTo(
                                    ListDetailPaneScaffoldRole.Detail,
                                    DuaDetailArgs(categoryId = categoryId)
                                )
                            }
                        },
                        onNavigateToBookmarks = onNavigateToBookmarks,
                    )
                }
            },
            detailPane = {
                AnimatedPane {
                    val args = navigator.currentDestination?.contentKey
                    if (args != null) {
                        if (args.duaId != null) {
                            DuaReaderScreen(
                                duaId = args.duaId,
                                onNavigateBack = { scope.launch { navigator.navigateBack() } },
                            )
                        } else {
                            DuaCategoryScreen(
                                categoryId = args.categoryId,
                                onNavigateBack = { scope.launch { navigator.navigateBack() } },
                                onNavigateToDua = { duaId ->
                                    scope.launch {
                                        navigator.navigateTo(
                                            ListDetailPaneScaffoldRole.Detail,
                                            DuaDetailArgs(
                                                categoryId = args.categoryId,
                                                duaId = duaId
                                            )
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            }
        )
    }
}

@kotlinx.parcelize.Parcelize
data class DuaDetailArgs(
    val categoryId: String,
    val duaId: String? = null,
) : android.os.Parcelable
