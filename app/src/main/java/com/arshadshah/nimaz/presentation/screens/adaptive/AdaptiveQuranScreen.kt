package com.arshadshah.nimaz.presentation.screens.adaptive

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.arshadshah.nimaz.core.navigation.Route
import com.arshadshah.nimaz.presentation.screens.quran.QuranBrowseScreen
import com.arshadshah.nimaz.presentation.screens.quran.QuranHomeScreen
import com.arshadshah.nimaz.presentation.screens.quran.QuranReaderScreen
import com.arshadshah.nimaz.presentation.theme.currentWindowSizeClass
import com.arshadshah.nimaz.presentation.theme.isCompact
import kotlinx.coroutines.launch

/**
 * Adaptive Quran screen that shows:
 * - Phone (Compact): QuranHomeScreen only, navigates to QuranReader via NavController
 * - Tablet (Medium/Expanded): Two-pane layout with surah list on left, reader on right
 *   - Collapses to single pane when reader is in page/mushaf mode
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AdaptiveQuranScreen(
    navController: NavController,
    onNavigateToSearch: () -> Unit,
    onNavigateToTopics: () -> Unit,
    onNavigateToBrowse: () -> Unit,
    onNavigateToSaved: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToKhatam: () -> Unit,
) {
    val windowSizeClass = currentWindowSizeClass()

    if (windowSizeClass.isCompact) {
        // Phone: Unchanged behavior — delegate to QuranHomeScreen directly
        QuranHomeScreen(
            onNavigateToSearch = onNavigateToSearch,
            onNavigateToTopics = onNavigateToTopics,
            onNavigateToSurah = { surahNumber ->
                navController.navigate(Route.QuranReader(surahNumber))
            },
            onNavigateToBrowse = onNavigateToBrowse,
            onNavigateToSaved = onNavigateToSaved,
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToQuranAyah = { surahNumber, ayahNumber ->
                navController.navigate(Route.QuranReader(surahNumber, ayahNumber))
            },
            onNavigateToKhatam = onNavigateToKhatam,
        )
    } else {
        // Tablet: Two-pane list-detail layout
        val scope = rememberCoroutineScope()

        // Track whether the detail pane is in page/mushaf mode
        var isDetailInPageMode by remember { mutableStateOf(false) }

        // When page mode is active, force single-pane to give the reader full width
        val defaultDirective = calculatePaneScaffoldDirective(currentWindowAdaptiveInfo())
        val scaffoldDirective = if (isDetailInPageMode) {
            PaneScaffoldDirective(
                maxHorizontalPartitions = 1,
                horizontalPartitionSpacerSize = 0.dp,
                maxVerticalPartitions = defaultDirective.maxVerticalPartitions,
                verticalPartitionSpacerSize = defaultDirective.verticalPartitionSpacerSize,
                defaultPanePreferredWidth = defaultDirective.defaultPanePreferredWidth,
                excludedBounds = defaultDirective.excludedBounds
            )
        } else {
            defaultDirective
        }

        val navigator = rememberListDetailPaneScaffoldNavigator<QuranDetailArgs>(
            scaffoldDirective = scaffoldDirective
        )

        // Extract current selection from navigator for list pane highlighting
        val currentArgs = navigator.currentDestination?.contentKey

        NavigableListDetailPaneScaffold(
            navigator = navigator,
            listPane = {
                AnimatedPane {
                    // The list pane is Browse, not home: a two-pane layout wants a *list* on
                    // the left, and home stopped being one when its 114-row browse tab became
                    // a destination. Home's other content is a phone-shaped front door.
                    QuranBrowseScreen(
                        onNavigateBack = { navController.popBackStack() },
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
                        selectedSurahNumber = currentArgs?.surahNumber,
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
                            onNavigateBack = {
                                isDetailInPageMode = false
                                scope.launch { navigator.navigateBack() }
                            },
                            onNavigateToQuranSettings = onNavigateToSettings,
                            onNavigateToTafseer = { surah, ayah ->
                                navController.navigate(Route.Tafseer(surah, ayah))
                            },
                            onNavigateToPassages = { surah, ayah ->
                                navController.navigate(Route.SurahPassages(surah, ayah))
                            },
                            onNavigateToSubjects = { surah ->
                                navController.navigate(
                                    if (surah != null) {
                                        Route.SurahSubjects(surah)
                                    } else {
                                        Route.QuranTopics
                                    }
                                )
                            },
                            onNavigateToNextSurah = { nextSurah ->
                                scope.launch {
                                    navigator.navigateTo(
                                        ListDetailPaneScaffoldRole.Detail,
                                        QuranDetailArgs(surahNumber = nextSurah)
                                    )
                                }
                            },
                            onPageModeChanged = { inPageMode ->
                                isDetailInPageMode = inPageMode
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
@kotlinx.parcelize.Parcelize
data class QuranDetailArgs(
    val surahNumber: Int? = null,
    val juzNumber: Int? = null,
    val pageNumber: Int? = null,
    val ayahNumber: Int = 1,
) : android.os.Parcelable
