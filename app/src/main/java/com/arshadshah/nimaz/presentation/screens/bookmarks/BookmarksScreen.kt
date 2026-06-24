package com.arshadshah.nimaz.presentation.screens.bookmarks

import android.content.Intent
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconVariant
import com.arshadshah.nimaz.presentation.components.molecules.NimazBottomSheet
import com.arshadshah.nimaz.presentation.components.molecules.NimazDropdownMenu
import com.arshadshah.nimaz.presentation.components.molecules.NimazDropdownRow
import com.arshadshah.nimaz.presentation.components.molecules.NimazEmptyState
import com.arshadshah.nimaz.presentation.components.molecules.NimazSheetFooterButtons
import com.arshadshah.nimaz.presentation.components.molecules.NimazSheetSectionLabel
import com.arshadshah.nimaz.presentation.components.molecules.TafseerOrnamentalDivider
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.components.organisms.NimazPillTabs
import com.arshadshah.nimaz.presentation.viewmodel.BookmarkType
import com.arshadshah.nimaz.presentation.viewmodel.BookmarksEvent
import com.arshadshah.nimaz.presentation.viewmodel.BookmarksViewModel
import com.arshadshah.nimaz.presentation.viewmodel.UnifiedBookmark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    onNavigateBack: () -> Unit,
    onNavigateToQuranAyah: (Int, Int) -> Unit,
    onNavigateToHadith: (String, Int) -> Unit,
    onNavigateToDua: (String) -> Unit,
    viewModel: BookmarksViewModel = hiltViewModel()
) {
    val state by viewModel.bookmarksState.collectAsState()
    val statsState by viewModel.statsState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }

    // The bookmark whose note is being edited (null = no editor showing). The
    // overflow menu is now an anchored dropdown owned by each card.
    var noteTarget by remember { mutableStateOf<UnifiedBookmark?>(null) }
    val context = LocalContext.current
    val shareChooser = stringResource(R.string.share)

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

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.bookmarks_title),
                subtitle = if (statsState.totalBookmarks > 0) {
                    stringResource(R.string.bookmarks_count, statsState.totalBookmarks)
                } else null,
                onBackClick = onNavigateBack,
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when {
            state.isLoading && state.allBookmarks.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

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
                        BookmarkFilterTabs(
                            selectedFilter = state.selectedFilter,
                            allCount = statsState.totalBookmarks,
                            quranCount = statsState.quranCount,
                            hadithCount = statsState.hadithCount,
                            duaCount = statsState.duaCount,
                            onFilterSelected = { viewModel.onEvent(BookmarksEvent.SetFilter(it)) }
                        )
                    }

                    items(
                        items = state.filteredBookmarks,
                        key = { it.id }
                    ) { bookmark ->
                        SwipeableBookmarkCard(
                            bookmark = bookmark,
                            onClick = { bookmark.navigate(onNavigateToQuranAyah, onNavigateToHadith, onNavigateToDua) },
                            onDelete = { viewModel.onEvent(BookmarksEvent.DeleteBookmark(bookmark.id)) },
                            onEditNote = { noteTarget = bookmark },
                            onShare = {
                                context.startActivity(
                                    Intent.createChooser(bookmark.shareIntent(), shareChooser)
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
            bookmark = target,
            onDismiss = { noteTarget = null },
            onSave = { note ->
                viewModel.onEvent(BookmarksEvent.EditNote(target.id, note))
                noteTarget = null
            }
        )
    }
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
    NimazPillTabs(
        tabs = tabs,
        selectedIndex = selectedIndex,
        onTabSelect = { index ->
            onFilterSelected(
                when (index) {
                    1 -> BookmarkType.QURAN
                    2 -> BookmarkType.HADITH
                    3 -> BookmarkType.DUA
                    else -> null
                }
            )
        },
        modifier = modifier.horizontalScroll(rememberScrollState())
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableBookmarkCard(
    bookmark: UnifiedBookmark,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onEditNote: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Swipe end→start to delete; the deletion fires immediately and the Undo
    // snackbar lets the user reverse it. Reset so the row settles before the
    // list flow removes it.
    val dismissState = rememberSwipeToDismissBoxState(
        initialValue = SwipeToDismissBoxValue.Settled,
        positionalThreshold = SwipeToDismissBoxDefaults.positionalThreshold
    )
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onDelete()
            dismissState.reset()
        }
    }
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(16.dp))
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                NimazIcon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    ) {
        BookmarkCard(
            bookmark = bookmark,
            onClick = onClick,
            onEditNote = onEditNote,
            onShare = onShare,
            onDelete = onDelete,
        )
    }
}

@Composable
private fun BookmarkCard(
    bookmark: UnifiedBookmark,
    onClick: () -> Unit,
    onEditNote: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val typeColor = bookmark.type.color()
    val onTypeColor = bookmark.type.onColor()
    val typeLabel = bookmark.type.label()

    NimazCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        elevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: type badge + time + overflow.
            Row(verticalAlignment = Alignment.CenterVertically) {
                NimazBadge(
                    text = typeLabel,
                    backgroundColor = typeColor,
                    textColor = onTypeColor,
                    size = NimazBadgeSize.SMALL,
                    shape = RoundedCornerShape(50)
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(
                        R.string.added_format,
                        DateUtils.getRelativeTimeSpanString(
                            bookmark.createdAt,
                            System.currentTimeMillis(),
                            DateUtils.DAY_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_RELATIVE
                        )
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box {
                    IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(36.dp)) {
                        NimazIcon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.cd_more_options),
                            variant = NimazIconVariant.MUTED,
                            size = NimazIconSize.MEDIUM
                        )
                    }
                    NimazDropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        NimazDropdownRow(
                            text = stringResource(R.string.edit_note),
                            leadingIcon = Icons.Default.Edit,
                            onClick = {
                                menuExpanded = false
                                onEditNote()
                            },
                        )
                        NimazDropdownRow(
                            text = stringResource(R.string.share),
                            leadingIcon = Icons.Default.Share,
                            onClick = {
                                menuExpanded = false
                                onShare()
                            },
                        )
                        NimazDropdownRow(
                            text = stringResource(R.string.delete),
                            leadingIcon = Icons.Default.Delete,
                            destructive = true,
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title (locator) — bold.
            Text(
                text = bookmark.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Source / subtitle — only when it adds information beyond the badge.
            if (bookmark.subtitle.isNotBlank() && !bookmark.subtitle.equals(typeLabel, ignoreCase = true)) {
                Text(
                    text = bookmark.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Arabic preview, set off by a gold ornamental divider.
            bookmark.arabicText?.let { arabic ->
                TafseerOrnamentalDivider(modifier = Modifier.padding(vertical = 4.dp))
                ArabicText(
                    text = arabic,
                    size = ArabicTextSize.SMALL,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Note preview.
            bookmark.note?.let { note ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteEditorSheet(
    bookmark: UnifiedBookmark,
    onDismiss: () -> Unit,
    onSave: (String?) -> Unit
) {
    var text by remember(bookmark.id) { mutableStateOf(bookmark.note.orEmpty()) }
    NimazBottomSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.edit_note),
        subtitle = bookmark.title,
        icon = Icons.Default.Edit,
        onClose = onDismiss,
        footer = {
            NimazSheetFooterButtons(
                primaryText = stringResource(R.string.save),
                onPrimary = { onSave(text) },
                secondaryText = stringResource(R.string.cancel),
                onSecondary = onDismiss
            )
        }
    ) {
        NimazSheetSectionLabel(text = stringResource(R.string.edit_note))
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            placeholder = { Text(stringResource(R.string.note_hint)) }
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ---- UnifiedBookmark presentation helpers ----

@Composable
private fun BookmarkType.color(): Color = when (this) {
    BookmarkType.QURAN -> MaterialTheme.colorScheme.primary
    BookmarkType.HADITH -> MaterialTheme.colorScheme.tertiary
    BookmarkType.DUA -> MaterialTheme.colorScheme.secondary
}

@Composable
private fun BookmarkType.onColor(): Color = when (this) {
    BookmarkType.QURAN -> MaterialTheme.colorScheme.onPrimary
    BookmarkType.HADITH -> MaterialTheme.colorScheme.onTertiary
    BookmarkType.DUA -> MaterialTheme.colorScheme.onSecondary
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

private fun UnifiedBookmark.shareIntent(): Intent {
    val body = buildString {
        append(title)
        arabicText?.let { append("\n\n$it") }
        note?.let { append("\n\n$it") }
    }
    return Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, body)
        type = "text/plain"
    }
}
