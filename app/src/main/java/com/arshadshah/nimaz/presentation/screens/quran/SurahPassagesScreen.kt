package com.arshadshah.nimaz.presentation.screens.quran

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.AyahTheme
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazErrorDefaults
import com.arshadshah.nimaz.presentation.components.atoms.NimazErrorState
import com.arshadshah.nimaz.presentation.components.atoms.NimazLoadingState
import com.arshadshah.nimaz.presentation.components.molecules.NimazEmptyState
import com.arshadshah.nimaz.presentation.components.molecules.NimazRangeRow
import com.arshadshah.nimaz.presentation.components.organisms.NimazSearchBar
import com.arshadshah.nimaz.presentation.components.organisms.NimazTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.quran.SurahThematicEvent
import com.arshadshah.nimaz.presentation.viewmodel.quran.SurahThematicViewModel

/**
 * The surah's table of contents — the mushaf's own division of it into subjects.
 *
 * Al-Baqarah has 282 of these, which is why they are no longer the tail of the info screen:
 * a table of contents is a thing you arrive at on purpose, filter, and leave from. It is
 * reachable from the reader as well as from surah info, and when it is opened from the reader
 * it is handed the verse being read, so the passage containing it is marked and scrolled to
 * rather than left for the reader to find among the other 281.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahPassagesScreen(
    surahNumber: Int,
    currentAyah: Int?,
    onNavigateBack: () -> Unit,
    onOpenAyah: (surah: Int, ayah: Int) -> Unit,
    viewModel: SurahThematicViewModel = hiltViewModel(),
) {
    val state by viewModel.passagesState.collectAsStateWithLifecycle()

    LaunchedEffect(surahNumber) {
        viewModel.onEvent(SurahThematicEvent.Load(surahNumber))
    }

    val passages = state.visiblePassages
    val listState = rememberLazyListState()

    // Open on the passage being read, once, when the list first has rows to scroll through.
    // Guarded on the filter so typing does not yank the list back to where the reader was.
    LaunchedEffect(currentAyah, state.passages, state.isFiltered) {
        if (currentAyah == null || state.isFiltered) return@LaunchedEffect
        val index = state.passages.indexOfFirst { it.contains(currentAyah) }
        if (index > 0) listState.scrollToItem(index)
    }

    NimazScreenScaffold(
        topBar = {
            NimazTopAppBar(
                title = stringResource(R.string.surah_info_passages),
                subtitle = state.surah?.let { surah ->
                    // A plural, because a surah with one passage read "1 passages across 7
                    // verses" — and the surahs with exactly one are the short ones every
                    // reader opens.
                    pluralStringResource(
                        R.plurals.surah_info_passages_row_subtitle,
                        state.passages.size,
                        state.passages.size,
                        surah.ayahCount,
                    )
                },
                navigationIcon = {
                    NimazIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = onNavigateBack,
                        contentDescription = stringResource(R.string.cd_back),
                    )
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (state.isLoading) {
                NimazLoadingState()
                return@Column
            }

            // Before the search bar and the empty branch: with an unfiltered list, an empty
            // result is indistinguishable from a failed load, and the empty copy says
            // "clear the filter to see all 0" — advice for a filter the reader never set.
            val error = state.error
            if (error != null) {
                NimazErrorState(
                    title = stringResource(error.message),
                    message = stringResource(R.string.surah_thematic_load_failed_body),
                    kind = error.kind,
                    details = error.details,
                    primaryAction = NimazErrorDefaults.retry(
                        onRetry = { viewModel.onEvent(SurahThematicEvent.Load(surahNumber)) },
                        label = stringResource(R.string.try_again),
                    ),
                )
                return@Column
            }

            NimazSearchBar(
                query = state.query,
                onQueryChange = { viewModel.onEvent(SurahThematicEvent.Filter(it)) },
                onClear = { viewModel.onEvent(SurahThematicEvent.ClearFilter) },
                placeholder = stringResource(R.string.surah_passages_filter_hint),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            if (passages.isEmpty()) {
                // Names the total, because an empty result is an invitation to act and the
                // useful next act is "clear this and see all 282", not "try harder".
                NimazEmptyState(
                    title = stringResource(R.string.surah_passages_none_title),
                    message = stringResource(
                        R.string.surah_passages_none,
                        state.passages.size,
                    ),
                    icon = Icons.Default.SearchOff,
                    modifier = Modifier.padding(20.dp),
                )
                return@Column
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 4.dp, bottom = 32.dp),
            ) {
                items(passages, key = { "passage-${it.ayahFrom}" }) { passage ->
                    PassageRow(
                        passage = passage,
                        isCurrent = currentAyah != null && passage.contains(currentAyah),
                        onClick = { onOpenAyah(surahNumber, passage.ayahFrom) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PassageRow(passage: AyahTheme, isCurrent: Boolean, onClick: () -> Unit) {
    val verses = if (passage.isSingleAyah) {
        stringResource(R.string.surah_info_passage_verse)
    } else {
        stringResource(R.string.surah_info_passage_verses, passage.ayahCount)
    }
    NimazRangeRow(
        // The surah number is already in the app bar; repeating it on all 282 rows would push
        // the subject in from the edge to say nothing new.
        reference = passage.reference.substringAfter(':'),
        supportingText = verses,
        label = passage.theme,
        marked = isCurrent,
        markerLabel = if (isCurrent) stringResource(R.string.surah_passages_reading) else null,
        contentDescription = stringResource(R.string.cd_passage_open, passage.theme),
        onClick = onClick,
    )
}
