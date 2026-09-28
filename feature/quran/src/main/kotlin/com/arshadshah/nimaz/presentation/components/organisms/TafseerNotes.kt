package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.core.share.ContentShareManager
import com.arshadshah.nimaz.core.share.Shareables
import com.arshadshah.nimaz.domain.model.*
import com.arshadshah.nimaz.presentation.components.atoms.*
import com.arshadshah.nimaz.presentation.components.molecules.*
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.viewmodel.quran.TafseerChaptersViewModel

/** The same saved card and action menu used by Bookmarks and Favourites. */
@Composable
fun TafseerSavedNoteCard(
    note: TafseerNoteItem,
    surahName: String,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    SwipeableSavedCard(
        title = stringResource(R.string.tafseer_note_location, surahName, note.ayahNumber),
        timestamp = note.createdAt,
        quote = note.quote,
        note = note.note,
        onClick = onOpen,
        onDelete = onDelete,
        enableSwipeToDelete = false,
        accent = if (note.reflection != null) NimazColors.Purple else parseColor(note.color),
        kindLabel = stringResource(if (note.reflection != null) R.string.tafseer_reflection else R.string.tafseer_highlight_note),
        leading = { Text(note.sourceLabel, style = MaterialTheme.typography.labelSmall) },
        menuActions = listOf(
            NimazMenuAction(stringResource(R.string.tafseer_open_passage), Icons.Default.MenuBook, onOpen),
            NimazMenuAction(stringResource(R.string.edit_note), Icons.Default.Edit, onEdit),
            NimazMenuAction(stringResource(R.string.share), Icons.Default.Share, {
                ContentShareManager.shareText(context, Shareables.tafseerNote(context, note, surahName))
            }),
            NimazMenuAction(stringResource(R.string.delete), Icons.Default.Delete, onDelete, destructive = true),
        ),
    )
}

/** All note-write feedback and editor state live here, shared by the index and reader. */
@Composable
fun TafseerNotesEditor(viewModel: TafseerChaptersViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    state.editing?.let { note ->
        val surah = state.surahs.firstOrNull { it.number == note.surahNumber }?.nameEnglish
            ?: note.surahNumber.toString()
        key(note.key) {
            NoteEditorSheet(
                subject = stringResource(R.string.tafseer_note_location, surah, note.ayahNumber) + " · " + note.sourceLabel,
                initialNote = note.note,
                onDismiss = { viewModel.edit(null) },
                onSave = { it?.let(viewModel::save) },
                fullScreen = true,
                saving = state.writing,
                error = state.writeError?.let { stringResource(it.message) },
                allowEmpty = false,
            )
        }
    }
}

@Composable
fun TafseerNotesFeedback(viewModel: TafseerChaptersViewModel, snackbar: SnackbarHostState) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val removed = stringResource(R.string.tafseer_note_deleted)
    val undo = stringResource(R.string.undo)
    LaunchedEffect(state.deleted) {
        if (state.deleted != null) {
            if (snackbar.showSnackbar(removed, undo) == SnackbarResult.ActionPerformed) viewModel.undo()
            else viewModel.dismissUndo()
        }
    }
    val error = state.writeError
    val errorText = error?.let { stringResource(it.message) }
    LaunchedEffect(error, state.editing) {
        if (errorText != null && state.editing == null) {
            snackbar.showSnackbar(errorText)
            viewModel.dismissWriteError()
        }
    }
}

/** The reader opens ONE notes list. It never nests the editor over that sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TafseerNotesSheet(
    surahNumber: Int,
    ayahId: Int,
    ayahNumber: Int,
    ayahStart: Int,
    ayahEnd: Int,
    source: TafseerSource,
    onDismiss: () -> Unit,
    onOpen: (TafseerNoteItem) -> Unit,
    viewModel: TafseerChaptersViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val notes = state.notes.filter {
        it.surahNumber == surahNumber && it.ayahNumber in ayahStart..ayahEnd && it.tafseerId == source.id
    }
    val snackbar = remember { SnackbarHostState() }
    TafseerNotesFeedback(viewModel, snackbar)
    if (state.editing == null) NimazBottomSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.tafseer_tab_notes),
        subtitle = source.displayName,
        onClose = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        footer = {
            Column {
                SnackbarHost(snackbar)
                NimazSheetFooterButtons(
                    primaryText = stringResource(R.string.tafseer_note_add),
                    onPrimary = {
                        viewModel.edit(TafseerNote(0, ayahId, source.id, "", 0, 0).toNoteItem(surahNumber, ayahNumber))
                    },
                    primaryEnabled = !state.isLoading && state.error == null,
                    secondaryText = stringResource(R.string.close),
                    onSecondary = onDismiss,
                )
            }
        },
    ) {
        when {
            state.isLoading -> NimazLoadingState()
            state.error != null -> Text(stringResource(state.error!!.message), color = MaterialTheme.colorScheme.error)
            notes.isEmpty() -> Text(stringResource(R.string.tafseer_no_notes), modifier = Modifier.padding(vertical = 20.dp))
            else -> notes.forEach { note ->
                TafseerSavedNoteCard(
                    note, state.surahs.firstOrNull { it.number == note.surahNumber }?.nameEnglish ?: note.surahNumber.toString(),
                    onOpen = { onOpen(note); onDismiss() },
                    onEdit = { viewModel.edit(note) },
                    onDelete = { viewModel.delete(note) },
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
    TafseerNotesEditor(viewModel)
}
