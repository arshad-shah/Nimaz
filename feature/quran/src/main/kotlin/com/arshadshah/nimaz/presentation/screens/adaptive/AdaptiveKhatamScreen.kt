package com.arshadshah.nimaz.presentation.screens.adaptive

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.arshadshah.nimaz.core.navigation.Route
import com.arshadshah.nimaz.presentation.screens.khatam.KhatamDetailScreen
import com.arshadshah.nimaz.presentation.screens.khatam.KhatamListScreen
import com.arshadshah.nimaz.presentation.theme.currentWindowSizeClass
import com.arshadshah.nimaz.presentation.theme.isCompact
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AdaptiveKhatamScreen(
    onNavigate: (Route) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToCreate: () -> Unit,
) {
    val windowSizeClass = currentWindowSizeClass()

    if (windowSizeClass.isCompact) {
        KhatamListScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToDetail = { khatamId ->
                onNavigate(Route.KhatamDetail(khatamId))
            },
            onNavigateToCreate = onNavigateToCreate,
            onNavigateToRead = { surahNumber, ayahNumber ->
                onNavigate(Route.QuranReader(surahNumber, ayahNumber))
            },
        )
    } else {
        val navigator = rememberListDetailPaneScaffoldNavigator<KhatamDetailNavArgs>()
        val scope = rememberCoroutineScope()

        NavigableListDetailPaneScaffold(
            navigator = navigator,
            listPane = {
                AnimatedPane {
                    KhatamListScreen(
                        onNavigateBack = onNavigateBack,
                        onNavigateToDetail = { khatamId ->
                            scope.launch {
                                navigator.navigateTo(
                                    ListDetailPaneScaffoldRole.Detail,
                                    KhatamDetailNavArgs(khatamId = khatamId)
                                )
                            }
                        },
                        onNavigateToCreate = onNavigateToCreate,
                        onNavigateToRead = { surahNumber, ayahNumber ->
                            onNavigate(Route.QuranReader(surahNumber, ayahNumber))
                        },
                    )
                }
            },
            detailPane = {
                AnimatedPane {
                    val args = navigator.currentDestination?.contentKey
                    if (args != null) {
                        KhatamDetailScreen(
                            khatamId = args.khatamId,
                            onNavigateBack = { scope.launch { navigator.navigateBack() } },
                            onNavigateToRead = { surahNumber, ayahNumber ->
                                onNavigate(
                                    Route.QuranReader(surahNumber, ayahNumber)
                                )
                            },
                            onNavigateToEdit = { khatamId ->
                                onNavigate(Route.KhatamEdit(khatamId))
                            },
                        )
                    }
                }
            }
        )
    }
}

@kotlinx.parcelize.Parcelize
data class KhatamDetailNavArgs(
    val khatamId: Long,
) : android.os.Parcelable
