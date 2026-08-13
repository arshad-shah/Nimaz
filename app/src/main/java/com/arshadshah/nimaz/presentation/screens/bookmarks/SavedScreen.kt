package com.arshadshah.nimaz.presentation.screens.bookmarks

import androidx.annotation.StringRes
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.IconButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.molecules.NimazDialog
import com.arshadshah.nimaz.presentation.components.molecules.NimazDialogCancelButton
import com.arshadshah.nimaz.presentation.components.molecules.NimazDialogDestructiveButton
import com.arshadshah.nimaz.presentation.components.molecules.NimazDropdownMenu
import com.arshadshah.nimaz.presentation.components.molecules.NimazDropdownRow
import com.arshadshah.nimaz.presentation.components.organisms.NimazSearchBar
import com.arshadshah.nimaz.presentation.viewmodel.quran.BookmarkSortOrder
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.share.ContentShareManager
import com.arshadshah.nimaz.core.share.Shareables
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeEmphasis
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.components.atoms.NimazErrorDefaults
import com.arshadshah.nimaz.presentation.components.atoms.NimazErrorState
import com.arshadshah.nimaz.presentation.components.atoms.NimazLoadingState
import com.arshadshah.nimaz.presentation.components.molecules.NimazEmptyState
import com.arshadshah.nimaz.presentation.components.molecules.NoteEditorSheet
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.components.organisms.NimazMenuAction
import com.arshadshah.nimaz.presentation.components.atoms.NimazSegmentedControl
import com.arshadshah.nimaz.presentation.components.atoms.NimazSegmentedPurpose
import com.arshadshah.nimaz.presentation.components.atoms.NimazSegmentedWidth
import com.arshadshah.nimaz.presentation.components.atoms.asSegments
import com.arshadshah.nimaz.presentation.components.organisms.SwipeableSavedCard
import com.arshadshah.nimaz.domain.model.BookmarkType
import com.arshadshah.nimaz.domain.model.SavedKind
import com.arshadshah.nimaz.presentation.viewmodel.quran.BookmarksEvent
import com.arshadshah.nimaz.presentation.viewmodel.quran.BookmarksViewModel
import com.arshadshah.nimaz.domain.model.UnifiedBookmark

/**
 * Everything the user has marked, whatever it is about.
 *
 * App-wide, and always was: the store is one `bookmarks` table keyed by `(kind, target_id)`, so
 * the Qur'an section's own "Favorites" tab and this screen were two views of overlapping rows
 * with no way to see them together. Two axes now — **what** (Qur'an / Hadith / Dua) and **how**
 * (bookmarked / favourited / annotated) — because those are the two questions a reader arrives
 * with, and one of them had no answer at all before: a note was invisible unless you happened to
 * open the row that carried it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedScreen(
    onNavigateBack: () -> Unit,
    onNavigateToQuranAyah: (Int, Int) -> Unit,
    onNavigateToHadith: (String, Int) -> Unit,
    onNavigateToDua: (String) -> Unit,
    viewModel: BookmarksViewModel = hiltViewModel()
) {
    val state by viewModel.bookmarksState.collectAsStateWithLifecycle()
    val statsState by viewModel.statsState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }

    // The bookmark whose note is being edited (null = no editor showing). The
    // overflow menu is an anchored dropdown owned by each card.
    var noteTarget by remember { mutableStateOf<UnifiedBookmark?>(null) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Drive the Undo snackbar off the most recently deleted bookmark.
    val removedMessage = stringResource(R.string.bookmark_removed)
    val undoLabel = stringResource(R.string.undo)
    val deletedId = state.recentlyDeleted?.id
    LaunchedEffect(deletedId) {
        if (deletedId != null) {
            val result = snackbarHostState.showSnackbar(
                message = removedMessage,
                actionLabel = undoLabel,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.onEvent(BookmarksEvent.UndoDelete)
            } else {
                viewModel.onEvent(BookmarksEvent.DismissUndo)
            }
        }
    }

    // A failed write goes to the same snackbar, and only there: the bookmarks on screen
    // are still correct, so a delete that did not go through must not take the list with
    // it. No action — there is nothing useful to offer beyond trying again by hand.
    val writeError = state.writeError
    // Resolved in composition, not with `context.getString` inside the effect. A Context read
    // through LocalContext.current does not re-resolve when the configuration changes, so a
    // locale or font-scale change would leave the snackbar reading from the old resources —
    // which is what `LocalContextGetResourceValueCall` is pointing at. The Undo snackbar above
    // already does it this way; this is the same shape, with a @StringRes that varies.
    val writeErrorMessage = writeError?.let { stringResource(it.message) }
    LaunchedEffect(writeError) {
        if (writeErrorMessage != null) {
            snackbarHostState.showSnackbar(
                message = writeErrorMessage,
                duration = SnackbarDuration.Short,
            )
            viewModel.onEvent(BookmarksEvent.DismissWriteError)
        }
    }

    if (showClearAllDialog) {
        ClearAllBookmarksDialog(
            total = statsState.totalBookmarks,
            onConfirm = { viewModel.onEvent(BookmarksEvent.ClearAllBookmarks) },
            onDismiss = { showClearAllDialog = false },
        )
    }

    NimazScreenScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.saved),
                subtitle = if (statsState.totalBookmarks > 0) {
                    stringResource(R.string.bookmarks_count, statsState.totalBookmarks)
                } else null,
                onBackClick = onNavigateBack,
                scrollBehavior = scrollBehavior,
                actions = {
                    if (state.allBookmarks.isNotEmpty()) {
                        IconButton(onClick = { sortMenuExpanded = true }) {
                            NimazIcon(
                                Icons.AutoMirrored.Filled.Sort,
                                contentDescription = stringResource(R.string.bookmarks_sort)
                            )
                        }
                        NimazDropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false },
                        ) {
                            BookmarkSortOrder.entries.forEach { order ->
                                NimazDropdownRow(
                                    text = stringResource(order.labelRes()),
                                    selected = state.sortOrder == order,
                                    onClick = {
                                        sortMenuExpanded = false
                                        viewModel.onEvent(BookmarksEvent.SetSortOrder(order))
                                    },
                                )
                            }
                            NimazDropdownRow(
                                text = stringResource(R.string.bookmarks_clear_all),
                                leadingIcon = Icons.Filled.DeleteSweep,
                                destructive = true,
                                onClick = {
                                    sortMenuExpanded = false
                                    showClearAllDialog = true
                                },
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        val readError = state.error
        when {
            state.isLoading && state.allBookmarks.isEmpty() -> {
                NimazLoadingState(modifier = Modifier.padding(paddingValues))
            }

            // Before the empty branch: an empty list is also what a failed load leaves
            // behind, and "no bookmarks yet" is a painful thing to tell someone who has
            // saved a hundred of them.
            readError != null -> NimazErrorState(
                title = stringResource(readError.message),
                message = stringResource(R.string.bookmarks_load_failed_body),
                kind = readError.kind,
                details = readError.details,
                primaryAction = NimazErrorDefaults.retry(
                    onRetry = { viewModel.onEvent(BookmarksEvent.Retry) },
                    label = stringResource(R.string.try_again),
                ),
                modifier = Modifier.padding(paddingValues),
            )

            state.allBookmarks.isEmpty() -> {
                NimazEmptyState(
                    title = stringResource(R.string.no_bookmarks_yet),
                    message = stringResource(R.string.no_bookmarks_hint),
                    icon = Icons.Default.Bookmark,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(20.dp)
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        NimazSearchBar(
                            query = state.searchQuery,
                            onQueryChange = {
                                viewModel.onEvent(BookmarksEvent.SetSearchQuery(it))
                            },
                            onClear = { viewModel.onEvent(BookmarksEvent.SetSearchQuery("")) },
                            placeholder = stringResource(R.string.bookmarks_search_placeholder)
                        )
                    }

                    // Two rows, not one combined chip set: kind and corpus are independent —
                    // "my notes on hadith" is a real question — and folding them into a single
                    // row of seven chips would make that combination unreachable.
                    item {
                        SavedKindTabs(
                            selectedKind = state.selectedKind,
                            allCount = statsState.totalBookmarks,
                            bookmarkCount = statsState.bookmarkCount,
                            favouriteCount = statsState.favouriteCount,
                            noteCount = statsState.noteCount,
                            onKindSelected = { viewModel.onEvent(BookmarksEvent.SetKind(it)) }
                        )
                    }

                    item {
                        BookmarkFilterTabs(
                            selectedFilter = state.selectedFilter,
                            allCount = statsState.totalBookmarks,
                            quranCount = statsState.quranCount,
                            hadithCount = statsState.hadithCount,
                            duaCount = statsState.duaCount,
                            onFilterSelected = { viewModel.onEvent(BookmarksEvent.SetFilter(it)) }
                        )
                    }

                    if (state.filteredBookmarks.isEmpty()) {
                        item {
                            NimazEmptyState(
                                title = stringResource(R.string.bookmarks_no_matches),
                                message = stringResource(R.string.bookmarks_no_matches_hint),
                                icon = Icons.Default.Bookmark,
                                iconTint = MaterialTheme.colorScheme.onSurfaceVariant
                                    .copy(alpha = 0.5f),
                                modifier = Modifier.padding(vertical = 32.dp)
                            )
                        }
                    }

                    items(
                        items = state.filteredBookmarks,
                        key = { it.id }
                    ) { bookmark ->
                        BookmarkSavedCard(
                            bookmark = bookmark,
                            onClick = {
                                bookmark.navigate(
                                    onNavigateToQuranAyah,
                                    onNavigateToHadith,
                                    onNavigateToDua
                                )
                            },
                            onDelete = { viewModel.onEvent(BookmarksEvent.DeleteBookmark(bookmark.id)) },
                            onEditNote = { noteTarget = bookmark },
                            onShare = {
                                ContentShareManager.shareText(
                                    context,
                                    Shareables.bookmark(
                                        context,
                                        title = bookmark.title,
                                        arabicText = bookmark.arabicText,
                                        note = bookmark.note,
                                    )
                                )
                            },
                        )
                    }

                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        }
    }

    // Note editor sheet.
    noteTarget?.let { target ->
        NoteEditorSheet(
            subject = target.title,
            initialNote = target.note,
            onDismiss = { noteTarget = null },
            onSave = { note ->
                viewModel.onEvent(BookmarksEvent.EditNote(target.id, note))
                noteTarget = null
            }
        )
    }
}

/**
 * The **how** axis: bookmarked, favourited, annotated.
 *
 * Favouriting only exists for the Qur'an, so this row is honest about being a Qur'an-heavy
 * filter — the count says so rather than the label needing to.
 */
@Composable
private fun SavedKindTabs(
    selectedKind: SavedKind?,
    allCount: Int,
    bookmarkCount: Int,
    favouriteCount: Int,
    noteCount: Int,
    onKindSelected: (SavedKind?) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        "${stringResource(R.string.all)}  $allCount",
        "${stringResource(R.string.bookmarks)}  $bookmarkCount",
        "${stringResource(R.string.favorites)}  $favouriteCount",
        "${stringResource(R.string.notes)}  $noteCount",
    )
    val selectedIndex = when (selectedKind) {
        null -> 0
        SavedKind.BOOKMARK -> 1
        SavedKind.FAVOURITE -> 2
        SavedKind.NOTE -> 3
    }
    NimazSegmentedControl(
        options = tabs.asSegments(),
        selectedIndex = selectedIndex,
        onSelect = { index ->
            onKindSelected(
                when (index) {
                    1 -> SavedKind.BOOKMARK
                    2 -> SavedKind.FAVOURITE
                    3 -> SavedKind.NOTE
                    else -> null
                }
            )
        },
        width = NimazSegmentedWidth.WRAP,
        purpose = NimazSegmentedPurpose.VIEW,
        modifier = modifier.horizontalScroll(rememberScrollState())
    )
}

@Composable
private fun BookmarkFilterTabs(
    selectedFilter: BookmarkType?,
    allCount: Int,
    quranCount: Int,
    hadithCount: Int,
    duaCount: Int,
    onFilterSelected: (BookmarkType?) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        "${stringResource(R.string.all)}  $allCount",
        "${stringResource(R.string.quran_type)}  $quranCount",
        "${stringResource(R.string.hadith_type)}  $hadithCount",
        "${stringResource(R.string.dua_type)}  $duaCount"
    )
    val selectedIndex = when (selectedFilter) {
        null -> 0
        BookmarkType.QURAN -> 1
        BookmarkType.HADITH -> 2
        BookmarkType.DUA -> 3
    }
    NimazSegmentedControl(
        options = tabs.asSegments(),
        selectedIndex = selectedIndex,
        onSelect = { index ->
            onFilterSelected(
                when (index) {
                    1 -> BookmarkType.QURAN
                    2 -> BookmarkType.HADITH
                    3 -> BookmarkType.DUA
                    else -> null
                }
            )
        },
        width = NimazSegmentedWidth.WRAP,
        purpose = NimazSegmentedPurpose.VIEW,
        modifier = modifier.horizontalScroll(rememberScrollState())
    )
}

@Composable
private fun BookmarkSavedCard(
    bookmark: UnifiedBookmark,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onEditNote: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typeTone = bookmark.type.tone()
    val typeLabel = bookmark.type.label()
    SwipeableSavedCard(
        title = bookmark.title,
        timestamp = bookmark.createdAt,
        // Source / subtitle — only when it adds information beyond the badge.
        subtitle = bookmark.subtitle.takeIf {
            it.isNotBlank() && !it.equals(typeLabel, ignoreCase = true)
        },
        arabicText = bookmark.arabicText,
        note = bookmark.note,
        onClick = onClick,
        onDelete = onDelete,
        enableSwipeToDelete = false,
        menuActions = listOf(
            NimazMenuAction(
                text = stringResource(R.string.edit_note),
                icon = Icons.Default.Edit,
                onClick = onEditNote,
            ),
            NimazMenuAction(
                text = stringResource(R.string.share),
                icon = Icons.Default.Share,
                onClick = onShare,
            ),
            NimazMenuAction(
                text = stringResource(R.string.delete),
                icon = Icons.Default.Delete,
                onClick = onDelete,
                destructive = true,
            ),
        ),
        modifier = modifier,
        leading = {
            NimazBadge(
                text = typeLabel,
                tone = typeTone,
                emphasis = NimazBadgeEmphasis.FILLED,
                size = NimazBadgeSize.SMALL
            )
        }
    )
}

/**
 * The label for a sort order. Lives here rather than on the enum so the enum stays a
 * plain domain-shaped value with no Android dependency.
 */
@StringRes
private fun BookmarkSortOrder.labelRes(): Int = when (this) {
    BookmarkSortOrder.DATE_NEWEST -> R.string.bookmarks_sort_newest
    BookmarkSortOrder.DATE_OLDEST -> R.string.bookmarks_sort_oldest
    BookmarkSortOrder.TYPE -> R.string.bookmarks_sort_type
    BookmarkSortOrder.ALPHABETICAL -> R.string.bookmarks_sort_alphabetical
}

/**
 * Confirmation for clearing every bookmark.
 *
 * Destructive, irreversible, and — by decision — **not undoable**: the dialog is the
 * safety net, so it names exactly what disappears rather than saying "are you sure".
 * Quran, Hadith and Dua bookmarks all go, which is not obvious from a button on a
 * screen that is currently filtered to one of them.
 */
@Composable
private fun ClearAllBookmarksDialog(
    total: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    NimazDialog(
        title = stringResource(R.string.bookmarks_clear_all_title),
        titleIcon = Icons.Filled.DeleteSweep,
        accentColor = MaterialTheme.colorScheme.error,
        onDismiss = onDismiss,
        actions = {
            NimazDialogCancelButton(onClick = onDismiss)
            NimazDialogDestructiveButton(
                text = stringResource(R.string.bookmarks_clear_all_confirm),
                onClick = {
                    onConfirm()
                    onDismiss()
                },
            )
        },
    ) {
        Text(
            text = stringResource(R.string.bookmarks_clear_all_body, total),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---- UnifiedBookmark presentation helpers ----

/** Badge tone per bookmark type — mirrors the old primary/tertiary/secondary trio. */
private fun BookmarkType.tone(): NimazTone = when (this) {
    BookmarkType.QURAN -> NimazTone.ACCENT
    BookmarkType.HADITH -> NimazTone.SUCCESS
    BookmarkType.DUA -> NimazTone.WARNING
}

@Composable
private fun BookmarkType.label(): String = when (this) {
    BookmarkType.QURAN -> stringResource(R.string.quran_type)
    BookmarkType.HADITH -> stringResource(R.string.hadith_type)
    BookmarkType.DUA -> stringResource(R.string.dua_type)
}

private fun UnifiedBookmark.navigate(
    onNavigateToQuranAyah: (Int, Int) -> Unit,
    onNavigateToHadith: (String, Int) -> Unit,
    onNavigateToDua: (String) -> Unit
) {
    when (type) {
        BookmarkType.QURAN -> if (surahNumber != null && ayahNumber != null) {
            onNavigateToQuranAyah(surahNumber, ayahNumber)
        }

        BookmarkType.HADITH -> if (hadithBookId != null && hadithNumber != null) {
            onNavigateToHadith(hadithBookId, hadithNumber)
        }

        BookmarkType.DUA -> duaId?.let(onNavigateToDua)
    }
}
