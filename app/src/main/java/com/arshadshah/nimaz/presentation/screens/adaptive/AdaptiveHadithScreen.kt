package com.arshadshah.nimaz.presentation.screens.adaptive

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.arshadshah.nimaz.core.navigation.Route
import com.arshadshah.nimaz.domain.model.HadithGrade
import com.arshadshah.nimaz.presentation.screens.hadith.HadithChaptersScreen
import com.arshadshah.nimaz.presentation.screens.hadith.HadithCollectionScreen
import com.arshadshah.nimaz.presentation.screens.hadith.HadithReaderScreen
import com.arshadshah.nimaz.presentation.theme.currentWindowSizeClass
import com.arshadshah.nimaz.presentation.theme.isCompact
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AdaptiveHadithScreen(
    onNavigate: (Route) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToBookmarks: () -> Unit,
    onNavigateToGrade: (HadithGrade) -> Unit = {},
) {
    val windowSizeClass = currentWindowSizeClass()

    if (windowSizeClass.isCompact) {
        HadithCollectionScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToBook = { bookId ->
                onNavigate(Route.HadithBook(bookId))
            },
            onNavigateToSearch = onNavigateToSearch,
            onNavigateToBookmarks = onNavigateToBookmarks,
            onNavigateToGrade = onNavigateToGrade,
        )
    } else {
        val navigator = rememberListDetailPaneScaffoldNavigator<HadithDetailArgs>()
        val scope = rememberCoroutineScope()

        NavigableListDetailPaneScaffold(
            navigator = navigator,
            listPane = {
                AnimatedPane {
                    HadithCollectionScreen(
                        onNavigateBack = onNavigateBack,
                        onNavigateToBook = { bookId ->
                            scope.launch {
                                navigator.navigateTo(
                                    ListDetailPaneScaffoldRole.Detail,
                                    HadithDetailArgs(bookId = bookId)
                                )
                            }
                        },
                        onNavigateToSearch = onNavigateToSearch,
                        onNavigateToBookmarks = onNavigateToBookmarks,
                        onNavigateToGrade = onNavigateToGrade,
                    )
                }
            },
            detailPane = {
                AnimatedPane {
                    val args = navigator.currentDestination?.contentKey
                    if (args != null) {
                        if (args.chapterId != null) {
                            HadithReaderScreen(
                                bookId = args.bookId,
                                chapterId = args.chapterId,
                                onNavigateBack = { scope.launch { navigator.navigateBack() } },
                                onNavigateToSettings = { onNavigate(Route.HadithSettings) },
                            )
                        } else {
                            HadithChaptersScreen(
                                bookId = args.bookId,
                                onNavigateBack = { scope.launch { navigator.navigateBack() } },
                                onNavigateToChapter = { bookId, chapterId ->
                                    scope.launch {
                                        navigator.navigateTo(
                                            ListDetailPaneScaffoldRole.Detail,
                                            HadithDetailArgs(
                                                bookId = bookId,
                                                chapterId = chapterId
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
data class HadithDetailArgs(
    val bookId: String,
    val chapterId: String? = null,
) : android.os.Parcelable
