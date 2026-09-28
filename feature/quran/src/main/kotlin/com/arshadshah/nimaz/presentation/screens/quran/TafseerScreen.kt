package com.arshadshah.nimaz.presentation.screens.quran

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.filled.TextFields
import com.arshadshah.nimaz.presentation.components.molecules.NimazBottomSheet
import com.arshadshah.nimaz.presentation.components.molecules.NimazSettingsSlider
import com.arshadshah.nimaz.presentation.components.organisms.TafseerNotesSheet
import com.arshadshah.nimaz.domain.model.TafseerSource
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.core.ui.R
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
    viewModel: TafseerViewModel = hiltViewModel(),
    sourceId: String? = null,
    highlightOffset: Int? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showNotes by remember { mutableStateOf(false) }
    var showTypography by remember { mutableStateOf(false) }
    var textSize by rememberSaveable { androidx.compose.runtime.mutableFloatStateOf(16f) }
    var selecting by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(surahNumber, ayahNumber, sourceId, highlightOffset) {
        viewModel.onEvent(TafseerEvent.LoadSurah(surahNumber, ayahNumber,
            TafseerSource.entries.firstOrNull { it.id == sourceId }, highlightOffset))
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
            val startedAt = System.nanoTime()
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
            }.onSuccess {
                viewModel.onEvent(
                    TafseerEvent.ExportCompleted((System.nanoTime() - startedAt) / 1_000_000)
                )
            }.onFailure { viewModel.onEvent(TafseerEvent.ExportFailed(it)) }
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
            NimazBackTopAppBar(
                title = state.surahName,
                subtitle = state.ayahs.getOrNull(state.currentAyahIndex)?.ayahNumber
                    ?.takeIf { state.ayahs.isNotEmpty() }
                    ?.let { stringResource(R.string.audio_position_ayah_format, it, state.ayahs.size) },
                onBackClick = onNavigateBack,
                actions = {
                    if (!state.isLoading && state.currentTafseer != null) {
                        IconButton(onClick = { showTypography = true }) {
                            NimazIcon(
                                imageVector = Icons.Default.TextFields,
                                contentDescription = stringResource(R.string.tafseer_text_size),
                            )
                        }
                    }
                },
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
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = !selecting,
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
                        textSize = textSize,
                        onNotesClick = { showNotes = true },
                        onSelectionActive = { selecting = it },
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

    val ayah = state.ayahs.getOrNull(state.currentAyahIndex)
    if (showNotes && ayah != null) TafseerNotesSheet(
        surahNumber = surahNumber,
        ayahId = ayah.id,
        ayahNumber = ayah.ayahNumber,
        ayahStart = state.currentTafseer?.ayahStart ?: ayah.ayahNumber,
        ayahEnd = state.currentTafseer?.ayahEnd ?: ayah.ayahNumber,
        source = state.selectedSource,
        onDismiss = { showNotes = false },
        onOpen = { note ->
            viewModel.onEvent(TafseerEvent.LoadSurah(note.surahNumber, note.ayahNumber,
                TafseerSource.entries.firstOrNull { it.id == note.tafseerId }, note.highlight?.startOffset))
        },
    )
    if (showTypography) NimazBottomSheet(
        onDismissRequest = { showTypography = false },
        title = stringResource(R.string.tafseer_text_size),
        onClose = { showTypography = false },
    ) {
        NimazSettingsSlider(
            title = stringResource(R.string.tafseer_text_size),
            valueLabel = textSize.toInt().toString(),
            value = textSize,
            onValueChange = { textSize = it },
            valueRange = 14f..24f,
            steps = 9,
        )
    }
}
