package com.arshadshah.nimaz.presentation.screens.quran

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.navigation.ScreenTags
import com.arshadshah.nimaz.domain.model.QuranSearchQuery
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.molecules.NimazEmptyState
import com.arshadshah.nimaz.presentation.components.atoms.NimazErrorState
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazLoadingState
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.molecules.SurahListItem
import com.arshadshah.nimaz.presentation.components.organisms.NimazSearchBar
import com.arshadshah.nimaz.presentation.components.organisms.NimazTopAppBar
import com.arshadshah.nimaz.presentation.components.organisms.getJuzName
import com.arshadshah.nimaz.presentation.viewmodel.quran.QuranBrowseEvent
import com.arshadshah.nimaz.presentation.viewmodel.quran.QuranBrowseUiState
import com.arshadshah.nimaz.presentation.viewmodel.quran.QuranBrowseViewModel

/**
 * One place to find a place in the Qur'an.
 *
 * Surah, Juz and Page were three tabs answering the same question, and the reader had to pick
 * which kind of answer they wanted before they could ask. Here there is one list, in mushaf
 * order, sectioned by juz, and one field that understands a name, a number, `juz 15` and
 * `page 299` alike. Page stops being a browse tab because it never was an index — it was the
 * door to a different *reading mode*, which lives in the reader now.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranBrowseScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSurah: (Int) -> Unit,
    onNavigateToJuz: (Int) -> Unit,
    onNavigateToPage: (Int) -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Raise the surah-info sheet on arrival. Carries the published announcement key
     * `quran/surah/{n}/info`, which lost its screen when surah info became a sheet.
     */
    initialInfoForSurah: Int? = null,
    onOpenBackground: (Int) -> Unit = {},
    onOpenPassages: (Int) -> Unit = {},
    onOpenSubjects: (Int) -> Unit = {},
    /** Highlighted row on a tablet's list pane; null on a phone. */
    selectedSurahNumber: Int? = null,
    viewModel: QuranBrowseViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    QuranBrowseContent(
        state = state,
        onNavigateBack = onNavigateBack,
        onNavigateToSurah = onNavigateToSurah,
        onNavigateToJuz = onNavigateToJuz,
        onNavigateToPage = onNavigateToPage,
        onQueryChange = { viewModel.onEvent(QuranBrowseEvent.QueryChanged(it)) },
        onClearQuery = { viewModel.onEvent(QuranBrowseEvent.ClearQuery) },
        initialInfoForSurah = initialInfoForSurah,
        onOpenBackground = onOpenBackground,
        onOpenPassages = onOpenPassages,
        onOpenSubjects = onOpenSubjects,
        selectedSurahNumber = selectedSurahNumber,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun QuranBrowseContent(
    state: QuranBrowseUiState,
    onNavigateBack: () -> Unit,
    onNavigateToSurah: (Int) -> Unit,
    onNavigateToJuz: (Int) -> Unit,
    onNavigateToPage: (Int) -> Unit,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    modifier: Modifier = Modifier,
    initialInfoForSurah: Int? = null,
    onOpenBackground: (Int) -> Unit = {},
    onOpenPassages: (Int) -> Unit = {},
    onOpenSubjects: (Int) -> Unit = {},
    selectedSurahNumber: Int? = null,
) {
    var infoForSurah by rememberSaveable { mutableStateOf(initialInfoForSurah) }

    NimazScreenScaffold(
        modifier = modifier,
        topBar = {
            NimazTopAppBar(
                title = stringResource(R.string.quran_home_tab_browse),
                navigationIcon = {
                    NimazIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = onNavigateBack,
                        contentDescription = stringResource(R.string.cd_back)
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            NimazSearchBar(
                query = state.query,
                onQueryChange = onQueryChange,
                onClear = onClearQuery,
                placeholder = stringResource(R.string.quran_browse_search_hint),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            val error = state.error
            when {
                error != null -> NimazErrorState(
                    title = stringResource(error.message),
                    message = error.details,
                    modifier = Modifier.fillMaxSize()
                )

                state.isLoading -> NimazLoadingState()

                state.rows.isEmpty() && state.jumpTarget == null -> NimazEmptyState(
                    title = stringResource(R.string.quran_browse_no_matches),
                    message = stringResource(R.string.quran_browse_search_hint),
                    icon = Icons.Default.SearchOff,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                )

                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(ScreenTags.QuranSurahList),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    state.jumpTarget?.let { target ->
                        item(key = "jump") {
                            JumpToCard(
                                target = target,
                                onNavigateToJuz = onNavigateToJuz,
                                onNavigateToPage = onNavigateToPage,
                                onNavigateToSurah = onNavigateToSurah,
                            )
                        }
                    }

                    // The juz header is printed by comparing each row with the one before it,
                    // rather than by grouping the state into a nested list: the list is already
                    // in mushaf order, so the boundary is a property of adjacency and the state
                    // stays a flat list a test can assert on.
                    state.rows.forEachIndexed { index, surah ->
                        val span = state.juzSpans[surah.number] ?: 1..1
                        val previousJuz = state.rows.getOrNull(index - 1)
                            ?.let { state.juzSpans[it.number]?.first }
                        if (previousJuz != span.first) {
                            // Sticky: the header stays under the search field while its own
                            // surahs scroll past, so "which juz am I looking at" is answered at
                            // every scroll position and not only at the boundary.
                            stickyHeader(key = "juz_${span.first}_${surah.number}") {
                                JuzSectionHeader(juz = span.first)
                            }
                        }
                        item(key = surah.number) {
                            SurahListItem(
                                surah = surah,
                                onClick = { onNavigateToSurah(surah.number) },
                                onInfoClick = { infoForSurah = surah.number },
                                isSelected = selectedSurahNumber == surah.number,
                                startPage = state.startPages[surah.number] ?: surah.startPage,
                                // Named on the row because the header cannot say it: a surah
                                // that runs from juz 1 into juz 3 sits under the "Juz 1"
                                // header and is mostly not in juz 1.
                                juzLabel = if (span.first == span.last) {
                                    stringResource(R.string.quran_home_juz_indicator, span.first)
                                } else {
                                    stringResource(
                                        R.string.quran_browse_juz_span, span.first, span.last
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    infoForSurah?.let { surahNumber ->
        SurahInfoSheetHost(
            surahNumber = surahNumber,
            onDismiss = { infoForSurah = null },
            onReadSurah = {
                infoForSurah = null
                onNavigateToSurah(surahNumber)
            },
            onOpenBackground = { infoForSurah = null; onOpenBackground(it) },
            onOpenPassages = { infoForSurah = null; onOpenPassages(it) },
            onOpenSubjects = { infoForSurah = null; onOpenSubjects(it) },
        )
    }
}

/**
 * Where a juz begins, named the way a printed mushaf names it — by its opening words.
 *
 * The number alone is an index entry; the Arabic is what a reader recognises, and it is the
 * one thing the old Juz *tab* had that folding it into this list would otherwise have cost.
 */
@Composable
private fun JuzSectionHeader(juz: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Opaque, because it is sticky: a transparent header would have the surah rows
            // scroll visibly through its own text.
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 4.dp, end = 4.dp, top = 12.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.quran_home_juz_indicator, juz),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        ArabicText(
            text = getJuzName(juz),
            size = ArabicTextSize.SMALL,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The exact match a query named, offered above the list it also filtered. */
@Composable
private fun JumpToCard(
    target: QuranSearchQuery,
    onNavigateToJuz: (Int) -> Unit,
    onNavigateToPage: (Int) -> Unit,
    onNavigateToSurah: (Int) -> Unit,
) {
    val (label, icon, onClick) = when (target) {
        is QuranSearchQuery.Juz -> Triple(
            stringResource(R.string.quran_browse_open_juz, target.number),
            Icons.Default.Layers,
        ) { onNavigateToJuz(target.number) }

        is QuranSearchQuery.Page -> Triple(
            stringResource(R.string.quran_browse_open_page, target.number),
            Icons.AutoMirrored.Filled.MenuBook,
        ) { onNavigateToPage(target.number) }

        is QuranSearchQuery.SurahNumber -> Triple(
            stringResource(R.string.quran_home_surah_fallback, target.number),
            Icons.AutoMirrored.Filled.MenuBook,
        ) { onNavigateToSurah(target.number) }

        // Empty and Name never reach here — the ViewModel only sets a jump target for a query
        // that names a place. `when` must still be exhaustive.
        else -> return
    }

    NimazCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NimazIcon(
                imageVector = icon,
                contentDescription = null,
                variant = NimazIconVariant.PRIMARY,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            NimazIcon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                variant = NimazIconVariant.MUTED,
            )
        }
    }
}
