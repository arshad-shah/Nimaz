package com.arshadshah.nimaz.presentation.components.templates

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import kotlinx.coroutines.launch

/**
 * Shared scaffold for the swipe-through readers (hadith, dua). Both previously
 * copy-pasted the same pager-state setup, "scroll to the requested index once the
 * collection loads" effect, loading/empty/content branching, and prev/next page
 * animation wiring. Only the page content and bottom bar differed.
 *
 * The screen keeps ownership of *loading* its data (its own `LaunchedEffect` that
 * dispatches the load event); this template only owns the pager + chrome.
 *
 * @param targetIndex the index the pager should settle on once [items] is loaded.
 * @param title produces the top-bar title from the currently-visible item (null
 *        until loaded), so readers whose title tracks the page stay in sync.
 * @param pageContent renders a single page for the given item.
 * @param bottomBar renders the per-item action bar; receives the current item,
 *        the current/total page counts, and ready-made prev/next callbacks.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> ReaderPagerScaffold(
    items: List<T>,
    targetIndex: Int,
    isLoading: Boolean,
    title: @Composable (currentItem: T?) -> String,
    onNavigateBack: () -> Unit,
    onSettingsClick: () -> Unit,
    settingsContentDescription: String,
    emptyText: String,
    itemKey: (T) -> Any,
    pageContent: @Composable (T) -> Unit,
    bottomBar: @Composable (item: T, currentPage: Int, pageCount: Int, onPrev: () -> Unit, onNext: () -> Unit) -> Unit,
    subtitle: String? = null,
) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = targetIndex.coerceAtLeast(0),
        pageCount = { items.size }
    )

    LaunchedEffect(targetIndex, items.size) {
        if (items.isNotEmpty()) {
            pagerState.scrollToPage(targetIndex.coerceIn(0, items.lastIndex))
        }
    }

    val current = items.getOrNull(pagerState.currentPage)

    Scaffold(
        topBar = {
            NimazBackTopAppBar(
                title = title(current),
                subtitle = subtitle,
                onBackClick = onNavigateBack,
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = settingsContentDescription,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading && items.isEmpty() -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                items.isEmpty() -> {
                    Text(
                        text = emptyText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            key = { itemKey(items[it]) }
                        ) { page ->
                            pageContent(items[page])
                        }

                        current?.let { item ->
                            bottomBar(
                                item,
                                pagerState.currentPage,
                                items.size,
                                {
                                    scope.launch {
                                        pagerState.animateScrollToPage(
                                            (pagerState.currentPage - 1).coerceAtLeast(0)
                                        )
                                    }
                                },
                                {
                                    scope.launch {
                                        pagerState.animateScrollToPage(
                                            (pagerState.currentPage + 1).coerceAtMost(items.lastIndex)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
