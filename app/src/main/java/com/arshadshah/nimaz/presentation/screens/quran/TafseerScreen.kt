package com.arshadshah.nimaz.presentation.screens.quran

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.core.monitoring.CrashReporter
import com.arshadshah.nimaz.core.share.ContentShareManager
import com.arshadshah.nimaz.core.util.TafseerPdfExporter
import com.arshadshah.nimaz.domain.model.TafseerNote
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazPager
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.rememberNimazPagerState
import com.arshadshah.nimaz.presentation.components.molecules.NimazDialog
import com.arshadshah.nimaz.presentation.components.molecules.NimazDialogCancelButton
import com.arshadshah.nimaz.presentation.components.molecules.NimazFieldVariant
import com.arshadshah.nimaz.presentation.components.molecules.NimazLoadingState
import com.arshadshah.nimaz.presentation.components.molecules.NimazTextField
import com.arshadshah.nimaz.presentation.components.organisms.TafseerPageContent
import com.arshadshah.nimaz.presentation.viewmodel.quran.TafseerEvent
import com.arshadshah.nimaz.presentation.viewmodel.quran.TafseerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TafseerScreen(
    surahNumber: Int,
    ayahNumber: Int = 1,
    onNavigateBack: () -> Unit,
    onNavigateToTopic: (topicId: Int) -> Unit = {},
    viewModel: TafseerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showNotes by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(surahNumber, ayahNumber) {
        viewModel.onEvent(TafseerEvent.LoadSurah(surahNumber, ayahNumber))
    }

    // Build a branded, print/share-ready PDF of the current ayah's tafseer and
    // hand it to the system share sheet. Generation runs off the main thread.
    fun shareTafseerPdf() {
        val ayah = state.ayahs.getOrNull(state.currentAyahIndex) ?: return
        val tafseer = state.currentTafseer
        if (tafseer == null || tafseer.text.isBlank()) return
        val surahName = state.surahName
        val sourceLabel = state.selectedSource.displayName
        val highlights = state.highlights
        scope.launch(Dispatchers.Default) {
            runCatching {
                val file = TafseerPdfExporter.export(
                    context = context,
                    surahName = surahName,
                    ayah = ayah,
                    sourceLabel = sourceLabel,
                    tafseerText = tafseer.text,
                    highlights = highlights
                )
                withContext(Dispatchers.Main) {
                    ContentShareManager.shareFile(
                        context,
                        file,
                        mimeType = "application/pdf",
                    )
                }
            }.onFailure { CrashReporter.recordException(it) }
        }
    }

    // A note that failed to save is reported here and nowhere else: it must not take away
    // the commentary being read, but it is not droppable either — from the reader's side, a
    // note that silently failed to save is a note they wrote and lost.
    val noteError = state.noteError
    // Resolved in composition rather than with `context.getString` inside the effect — see the
    // same fix in BookmarksScreen. LocalContext.current does not re-resolve across a
    // configuration change, so the message could come from the previous locale's resources.
    val noteErrorMessage = noteError?.let { stringResource(it.message) }
    LaunchedEffect(noteError) {
        if (noteErrorMessage != null) {
            snackbarHostState.showSnackbar(noteErrorMessage)
            viewModel.onEvent(TafseerEvent.DismissNoteError)
        }
    }

    NimazScreenScaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.surahName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val ayahNumber = state.ayahs.getOrNull(state.currentAyahIndex)?.ayahNumber
                        if (ayahNumber != null && state.ayahs.isNotEmpty()) {
                            Text(
                                text = stringResource(
                                    R.string.audio_position_ayah_format,
                                    ayahNumber,
                                    state.ayahs.size
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        NimazIcon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                actions = {
                    // The affordance that makes AddNote/UpdateNote/DeleteNote reachable at
                    // all. The handlers and the Room collector behind them have been in the
                    // ViewModel the whole time; nothing emitted the events, so the notes list
                    // on TafseerChaptersScreen was permanently empty.
                    IconButton(onClick = { showNotes = true }) {
                        NimazIcon(
                            imageVector = Icons.Outlined.EditNote,
                            contentDescription = stringResource(R.string.tafseer_notes)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        // Opts out of the app ornament: long-form Arabic needs a plain backdrop.
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.isLoading) {
                NimazLoadingState()
            } else if (state.ayahs.isNotEmpty()) {
                val pagerState = rememberNimazPagerState(
                    initialPage = state.currentAyahIndex,
                    pageCount = { state.ayahs.size }
                )

                // Sync pager with ViewModel
                LaunchedEffect(pagerState.settledPage) {
                    if (pagerState.settledPage != state.currentAyahIndex) {
                        viewModel.onEvent(TafseerEvent.NavigateToAyah(pagerState.settledPage))
                    }
                }

                NimazPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val ayah = state.ayahs[page]
                    val isCurrentPage = page == state.currentAyahIndex

                    TafseerPageContent(
                        ayah = ayah,
                        tafseer = if (isCurrentPage) state.currentTafseer else null,
                        highlights = if (isCurrentPage) state.highlights else emptyList(),
                        selectedSource = state.selectedSource,
                        availableSources = if (isCurrentPage) state.availableSources else emptySet(),
                        currentContentPage = state.currentTafseerPage,
                        onContentPageChanged = { contentPage ->
                            viewModel.onEvent(TafseerEvent.NavigateToTafseerPage(contentPage))
                        },
                        onSourceSwitch = { source ->
                            viewModel.onEvent(TafseerEvent.SwitchSource(source))
                        },
                        onHighlightCreated = { start, end, color, note ->
                            viewModel.onEvent(TafseerEvent.AddHighlight(start, end, color, note))
                        },
                        onHighlightUpdated = { id, color, note ->
                            viewModel.onEvent(TafseerEvent.UpdateHighlight(id, color, note))
                        },
                        onHighlightDeleted = { id ->
                            viewModel.onEvent(TafseerEvent.DeleteHighlight(id))
                        },
                        onShare = { shareTafseerPdf() },
                        topics = if (isCurrentPage) state.topics else emptyList(),
                        onTopicClick = onNavigateToTopic,
                        translationLanguage = state.translationLanguage,
                    )
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_ayahs_found),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showNotes) {
        TafseerNotesDialog(
            notes = state.notes,
            onAdd = { viewModel.onEvent(TafseerEvent.AddNote(it)) },
            onUpdate = { viewModel.onEvent(TafseerEvent.UpdateNote(it)) },
            onDelete = { viewModel.onEvent(TafseerEvent.DeleteNote(it)) },
            onDismiss = { showNotes = false },
        )
    }
}

/**
 * The notes for the commentary block on screen: read them, write one, edit or delete one.
 *
 * `TafseerEvent.AddNote`, `UpdateNote` and `DeleteNote` had handlers, a use case, a repository
 * method and a Room collector — and no producer, so none of them had ever run and the notes
 * list on `TafseerChaptersScreen` was permanently empty. This is the missing producer.
 */
@Composable
private fun TafseerNotesDialog(
    notes: List<TafseerNote>,
    onAdd: (String) -> Unit,
    onUpdate: (TafseerNote) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    // Non-null while editing an existing note; null while composing a new one.
    var editing by remember { mutableStateOf<TafseerNote?>(null) }
    var draft by remember { mutableStateOf("") }

    NimazDialog(
        title = stringResource(R.string.tafseer_notes),
        titleIcon = Icons.Outlined.EditNote,
        onDismiss = onDismiss,
        wrapContent = false,
        actions = {
            NimazDialogCancelButton(
                text = stringResource(R.string.close),
                onClick = onDismiss,
            )
            NimazButton(
                text = stringResource(
                    if (editing == null) R.string.tafseer_note_add else R.string.save
                ),
                onClick = {
                    val current = editing
                    if (current == null) onAdd(draft) else onUpdate(current.copy(text = draft))
                    draft = ""
                    editing = null
                },
                enabled = draft.isNotBlank(),
                variant = NimazButtonVariant.FILLED,
            )
        },
    ) {
        NimazTextField(
            value = draft,
            onValueChange = { draft = it },
            label = stringResource(R.string.tafseer_note_hint),
            variant = NimazFieldVariant.NOTE,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        )

        Spacer(modifier = Modifier.height(12.dp))

        notes.forEach { note ->
            NimazCard(
                style = NimazCardStyle.FILLED,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = note.text, style = MaterialTheme.typography.bodyMedium)
                    Row {
                        TextButton(onClick = {
                            editing = note
                            draft = note.text
                        }) { Text(stringResource(R.string.tafseer_note_edit)) }
                        TextButton(onClick = {
                            if (editing?.id == note.id) {
                                editing = null
                                draft = ""
                            }
                            onDelete(note.id)
                        }) { Text(stringResource(R.string.delete)) }
                    }
                }
            }
        }
    }
}
