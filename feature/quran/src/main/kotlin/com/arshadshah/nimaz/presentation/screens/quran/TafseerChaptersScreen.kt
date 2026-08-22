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
    viewModel: TafseerChaptersViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

    val notesTabLabel = stringResource(R.string.tafseer_tab_notes) +
            if (state.notes.isNotEmpty()) " · ${state.notes.size}" else ""

    NimazScreenScaffold(
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.tafseer),
                onBackClick = onNavigateBack
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
                onSelect = { selectedTab = it },
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

                selectedTab == 0 -> LazyColumn(
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
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.notes, key = { it.highlightId }) { note ->
                            TafseerNoteCard(
                                note = note,
                                surahName = nameBySurah[note.surahNumber]
                                    ?: stringResource(
                                        R.string.surah_number_format,
                                        note.surahNumber
                                    ),
                                onClick = { onOpenTafseer(note.surahNumber, note.ayahNumber) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TafseerNoteCard(
    note: TafseerNoteItem,
    surahName: String,
    onClick: () -> Unit
) {
    NimazCard(onClick = onClick) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 3.dp)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(parseColor(note.color))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(
                            R.string.tafseer_note_location,
                            surahName,
                            note.ayahNumber
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = note.sourceLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = note.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
