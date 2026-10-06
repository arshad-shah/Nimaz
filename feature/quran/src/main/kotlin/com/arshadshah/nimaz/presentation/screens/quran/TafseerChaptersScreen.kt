package com.arshadshah.nimaz.presentation.screens.quran

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.automirrored.filled.Sort
import com.arshadshah.nimaz.domain.model.TafseerSource
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconVariant
import com.arshadshah.nimaz.presentation.components.molecules.NimazDropdownMenu
import com.arshadshah.nimaz.presentation.components.molecules.NimazDropdownRow
import com.arshadshah.nimaz.presentation.components.organisms.NimazSearchBar
import com.arshadshah.nimaz.presentation.components.organisms.TafseerSavedNoteCard
import com.arshadshah.nimaz.presentation.components.organisms.TafseerNotesEditor
import com.arshadshah.nimaz.presentation.components.organisms.TafseerNotesFeedback
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.TafseerNoteItem
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazSegmentedControl
import com.arshadshah.nimaz.presentation.components.atoms.NimazSegmentedPurpose
import com.arshadshah.nimaz.presentation.components.atoms.asSegments
import com.arshadshah.nimaz.presentation.components.molecules.NimazErrorState
import com.arshadshah.nimaz.presentation.components.molecules.NimazLoadingState
import com.arshadshah.nimaz.presentation.components.molecules.SurahListItem
import com.arshadshah.nimaz.presentation.components.molecules.parseColor
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.quran.TafseerChaptersViewModel
import com.arshadshah.nimaz.presentation.components.molecules.NimazScrollbarBox
import androidx.compose.foundation.lazy.rememberLazyListState

/**
 * Surah picker shown before the Tafseer reader when entered from the More menu —
 * mirrors the Hadith/Dua/Quran browse flow. A "My notes" tab surfaces the user's
 * annotated tafseer for quick access. Reuses [SurahListItem], [NimazSegmentedControl] and
 * [NimazCard]; tapping a surah or note opens the reader at the right ayah.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TafseerChaptersScreen(
    onNavigateBack: () -> Unit,
    onOpenTafseer: (surahNumber: Int, ayahNumber: Int) -> Unit,
    viewModel: TafseerChaptersViewModel = hiltViewModel(),
    onOpenNote: (TafseerNoteItem) -> Unit = { onOpenTafseer(it.surahNumber, it.ayahNumber) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    var query by rememberSaveable { mutableStateOf("") }
    var sourceId by rememberSaveable { mutableStateOf<String?>(null) }
    var oldestFirst by rememberSaveable { mutableStateOf(false) }
    var filterExpanded by remember { mutableStateOf(false) }
    var sortExpanded by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    TafseerNotesFeedback(viewModel, snackbar)
    TafseerNotesEditor(viewModel)
    val names = remember(state.surahs) { state.surahs.associate { it.number to it.nameEnglish } }
    val notes = remember(state.notes, query, sourceId, oldestFirst, names) {
        filterTafseerNotes(state.notes, query, sourceId, oldestFirst, names)
    }

    val notesTabLabel = stringResource(R.string.tafseer_tab_notes) +
            if (state.notes.isNotEmpty()) " · ${state.notes.size}" else ""

    NimazScreenScaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.tafseer),
                onBackClick = onNavigateBack,
                actions = {
                    // Scope menus to the notes tab. Keep them during a zero-result filter
                    // so the user can clear it, but hide them for empty/loading/error states.
                    if (selectedTab == 1 && !state.isLoading && state.error == null && state.notes.isNotEmpty()) {
                        Box {
                            IconButton(onClick = { filterExpanded = true }) {
                                NimazIcon(Icons.Default.FilterList, contentDescription = stringResource(R.string.cd_filter),
                                    variant = if (sourceId != null) NimazIconVariant.PRIMARY else NimazIconVariant.DEFAULT)
                            }
                            NimazDropdownMenu(filterExpanded, { filterExpanded = false }) {
                                NimazDropdownRow(stringResource(R.string.all), selected = sourceId == null,
                                    onClick = { sourceId = null; filterExpanded = false })
                                TafseerSource.entries.forEach { source ->
                                    NimazDropdownRow(source.displayName, selected = sourceId == source.id,
                                        onClick = { sourceId = source.id; filterExpanded = false })
                                }
                            }
                        }
                        Box {
                            IconButton(onClick = { sortExpanded = true }) {
                                NimazIcon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.bookmarks_sort))
                            }
                            NimazDropdownMenu(sortExpanded, { sortExpanded = false }) {
                                listOf(false, true).forEach { oldest ->
                                    NimazDropdownRow(stringResource(if (oldest) R.string.bookmarks_sort_oldest else R.string.bookmarks_sort_newest),
                                        selected = oldestFirst == oldest, onClick = { oldestFirst = oldest; sortExpanded = false })
                                }
                            }
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            NimazSegmentedControl(
                options = listOf(
                    stringResource(R.string.tafseer_tab_surahs),
                    notesTabLabel
                ).asSegments(),
                selectedIndex = selectedTab,
                onSelect = { selectedTab = it; filterExpanded = false; sortExpanded = false },
                purpose = NimazSegmentedPurpose.VIEW,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )

            val error = state.error
            when {
                state.isLoading -> NimazLoadingState()

                // Before either tab: an empty surah picker and an empty notes list are both
                // what a failed load leaves behind, and the notes tab would call that
                // "you have no notes yet".
                error != null -> NimazErrorState(
                    title = stringResource(error.message),
                    message = stringResource(R.string.tafseer_load_failed_body),
                    kind = error.kind,
                    details = error.details,
                )

                selectedTab == 0 -> {
                    val scrollbarState = rememberLazyListState()
                    NimazScrollbarBox(
                        state = scrollbarState,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        LazyColumn(
                            state = scrollbarState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.surahs, key = { it.number }) { surah ->
                                SurahListItem(
                                    surah = surah,
                                    onClick = { onOpenTafseer(surah.number, 1) },
                                    showInfo = false,
                                    startPage = surah.startPage
                                )
                            }
                        }
                    }
                }

                state.notes.isEmpty() -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.tafseer_no_notes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> {
                    val nameBySurah = remember(state.surahs) {
                        state.surahs.associate { it.number to it.nameEnglish }
                    }
                    val scrollbarState = rememberLazyListState()
                    NimazScrollbarBox(
                        state = scrollbarState,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        LazyColumn(
                            state = scrollbarState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                NimazSearchBar(query = query, onQueryChange = { query = it },
                                    onClear = { query = "" }, placeholder = stringResource(R.string.bookmarks_search_placeholder))
                            }
                            if (notes.isEmpty()) item {
                                Text(stringResource(R.string.no_results_hint), modifier = Modifier.padding(20.dp))
                            }
                            items(notes, key = { it.key }) { note ->
                                TafseerSavedNoteCard(
                                    note = note,
                                    surahName = nameBySurah[note.surahNumber] ?: note.surahNumber.toString(),
                                    onOpen = { onOpenNote(note) },
                                    onEdit = { viewModel.edit(note) },
                                    onDelete = { viewModel.delete(note) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Search and ordering shared with tests; stable keys distinguish the two note stores. */
internal fun filterTafseerNotes(
    notes: List<TafseerNoteItem>, query: String, sourceId: String?, oldestFirst: Boolean,
    names: Map<Int, String>,
): List<TafseerNoteItem> {
    val term = query.trim()
    val filtered = notes.filter { note ->
        (sourceId == null || note.tafseerId == sourceId) &&
            listOf(note.note, note.quote.orEmpty(), note.sourceLabel, names[note.surahNumber].orEmpty(),
                "${note.surahNumber}:${note.ayahNumber}").any { it.contains(term, ignoreCase = true) }
    }
    return if (oldestFirst) filtered.sortedBy { it.createdAt } else filtered.sortedByDescending { it.createdAt }
}
