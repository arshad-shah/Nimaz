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
import com.arshadshah.nimaz.presentation.screens.quran.QuranHomeScreen
import com.arshadshah.nimaz.presentation.screens.quran.QuranReaderScreen
import com.arshadshah.nimaz.presentation.theme.currentWindowSizeClass
import com.arshadshah.nimaz.presentation.theme.isCompact

/**
 * Adaptive Quran screen that shows:
 * - Phone (Compact): QuranHomeScreen only, navigates to QuranReader via NavController
 * - Tablet (Medium/Expanded): Two-pane layout with surah list on left, reader on right
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AdaptiveQuranScreen(
    navController: NavController,
    onNavigateToSearch: () -> Unit,
    onNavigateToBookmarks: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSurahInfo: (Int) -> Unit,
    onNavigateToKhatam: () -> Unit,
    onNavigateToKhatamDetail: (Long) -> Unit,
) {
    val windowSizeClass = currentWindowSizeClass()

    if (windowSizeClass.isCompact) {
        // Phone: Unchanged behavior — delegate to QuranHomeScreen directly
        QuranHomeScreen(
            onNavigateToSearch = onNavigateToSearch,
            onNavigateToSurah = { surahNumber ->
                navController.navigate(Route.QuranReader(surahNumber))
            },
            onNavigateToJuz = { juzNumber ->
                navController.navigate(Route.QuranJuz(juzNumber))
            },
            onNavigateToPage = { pageNumber ->
                navController.navigate(Route.QuranPage(pageNumber))
            },
            onNavigateToBookmarks = onNavigateToBookmarks,
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToSurahInfo = onNavigateToSurahInfo,
            onNavigateToQuranAyah = { surahNumber, ayahNumber ->
                navController.navigate(Route.QuranReader(surahNumber, ayahNumber))
            },
            onNavigateToKhatam = onNavigateToKhatam,
            onNavigateToKhatamDetail = onNavigateToKhatamDetail,
        )
    } else {
        // Tablet: Two-pane list-detail layout
        // Content key is a pair of (surahNumber, ayahNumber)
        val navigator = rememberListDetailPaneScaffoldNavigator<QuranDetailArgs>()
        val scope = rememberCoroutineScope()

        // Extract current selection from navigator for list pane highlighting
        val currentArgs = navigator.currentDestination?.contentKey

        NavigableListDetailPaneScaffold(
            navigator = navigator,
            listPane = {
                AnimatedPane {
                    QuranHomeScreen(
                        onNavigateToSearch = onNavigateToSearch,
                        onNavigateToSurah = { surahNumber ->
                            scope.launch {
                                navigator.navigateTo(
                                    ListDetailPaneScaffoldRole.Detail,
                                    QuranDetailArgs(surahNumber = surahNumber)
                                )
                            }
                        },
                        onNavigateToJuz = { juzNumber ->
                            scope.launch {
                                navigator.navigateTo(
                                    ListDetailPaneScaffoldRole.Detail,
                                    QuranDetailArgs(juzNumber = juzNumber)
                                )
                            }
                        },
                        onNavigateToPage = { pageNumber ->
                            scope.launch {
                                navigator.navigateTo(
                                    ListDetailPaneScaffoldRole.Detail,
                                    QuranDetailArgs(pageNumber = pageNumber)
                                )
                            }
                        },
                        onNavigateToBookmarks = onNavigateToBookmarks,
                        onNavigateToSettings = onNavigateToSettings,
                        onNavigateToSurahInfo = onNavigateToSurahInfo,
                        onNavigateToQuranAyah = { surahNumber, ayahNumber ->
                            scope.launch {
                                navigator.navigateTo(
                                    ListDetailPaneScaffoldRole.Detail,
                                    QuranDetailArgs(
                                        surahNumber = surahNumber,
                                        ayahNumber = ayahNumber
                                    )
                                )
                            }
                        },
                        onNavigateToKhatam = onNavigateToKhatam,
                        onNavigateToKhatamDetail = onNavigateToKhatamDetail,
                        selectedSurahNumber = currentArgs?.surahNumber,
                        selectedJuzNumber = currentArgs?.juzNumber,
                        selectedPageNumber = currentArgs?.pageNumber,
                    )
                }
            },
            detailPane = {
                AnimatedPane {
                    val args = navigator.currentDestination?.contentKey
                    if (args != null) {
                        QuranReaderScreen(
                            surahNumber = args.surahNumber,
                            juzNumber = args.juzNumber,
                            pageNumber = args.pageNumber,
                            initialAyahNumber = args.ayahNumber,
                            onNavigateBack = { scope.launch { navigator.navigateBack() } },
                            onNavigateToQuranSettings = onNavigateToSettings,
                            onNavigateToTafseer = { surah, ayah ->
                                navController.navigate(Route.Tafseer(surah, ayah))
                            },
                            onNavigateToNextSurah = { nextSurah ->
                                scope.launch {
                                    navigator.navigateTo(
                                        ListDetailPaneScaffoldRole.Detail,
                                        QuranDetailArgs(surahNumber = nextSurah)
                                    )
                                }
                            }
                        )
                    }
                }
            }
        )
    }
}

/**
 * Arguments for the Quran detail pane. Only one of surahNumber/juzNumber/pageNumber
 * should be set at a time (matching QuranReaderScreen's parameter pattern).
 */
@kotlinx.serialization.Serializable
data class QuranDetailArgs(
    val surahNumber: Int? = null,
    val juzNumber: Int? = null,
    val pageNumber: Int? = null,
    val ayahNumber: Int = 1,
)
