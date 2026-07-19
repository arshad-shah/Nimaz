package com.arshadshah.nimaz.presentation.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.navigation.Route
import com.arshadshah.nimaz.domain.model.CitationId
import com.arshadshah.nimaz.domain.model.Proof
import com.arshadshah.nimaz.domain.model.ProofSource
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeDefaults
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeEmphasis
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeShape
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconWell
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconWellShape
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconWellSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconVariant
import com.arshadshah.nimaz.presentation.components.molecules.NimazEmptyState
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.components.organisms.NimazSearchBar
import com.arshadshah.nimaz.presentation.viewmodel.AskEvent
import com.arshadshah.nimaz.presentation.viewmodel.AskPhase
import com.arshadshah.nimaz.presentation.viewmodel.AskViewModel
import com.arshadshah.nimaz.presentation.viewmodel.SearchEvent
import com.arshadshah.nimaz.presentation.viewmodel.SearchFilter
import com.arshadshah.nimaz.presentation.viewmodel.SearchViewModel
import com.arshadshah.nimaz.presentation.viewmodel.UnifiedSearchResult

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onNavigateToQuranAyah: (Int, Int) -> Unit,
    onNavigateToSurah: (Int) -> Unit,
    onNavigateToHadith: (String, String) -> Unit,
    onNavigateToDua: (String) -> Unit,
    initialFilter: SearchFilter? = null,
    enableAsk: Boolean = false,
    onNavigateToSearchSettings: () -> Unit = {},
    onNavigateToProof: (Route) -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel(),
    askViewModel: AskViewModel = hiltViewModel(),
) {
    val state by viewModel.searchState.collectAsState()
    val statsState by viewModel.statsState.collectAsState()
    val askState by askViewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // Scope the search (e.g. to duas) when launched from a section screen.
    LaunchedEffect(initialFilter) {
        if (initialFilter != null) {
            viewModel.onEvent(SearchEvent.SetFilter(initialFilter))
        }
    }

    // When the AI answers it also returns related search terms — drive the
    // results list from them so the list dynamically shows what the AI judged
    // relevant (no extra call). Only on global search with AI enabled.
    LaunchedEffect(askState.relatedTerms) {
        if (enableAsk && askState.relatedTerms.isNotEmpty()) {
            viewModel.onEvent(SearchEvent.ApplyAiTerms(askState.relatedTerms))
        }
    }

    val askEnabled = enableAsk && askState.aiEnabled
    val answerPhase = if (enableAsk) askState.phase as? AskPhase.Answer else null

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.search_title),
                onBackClick = onNavigateBack,
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = onNavigateToSearchSettings) {
                        NimazIcon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = stringResource(R.string.search_settings),
                            size = NimazIconSize.MEDIUM
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Pinned controls: the single search bar and the source filter live
            // above the list and never scroll away, so the filter always scopes
            // whatever is on screen. A single bar drives both keyword search and
            // — when the user has opted into AI answers on global search — the
            // "Ask with Proof" question: keyword results update as-you-type, the
            // AI ask fires only from the Ask pill / IME action.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)
            ) {
                NimazSearchBar(
                    query = state.query,
                    onQueryChange = {
                        viewModel.onEvent(SearchEvent.UpdateQuery(it))
                        // Keep the AI question in sync so a submit uses the current text;
                        // emptying the bar also clears any lingering AI answer.
                        if (enableAsk) {
                            if (it.isBlank()) askViewModel.onEvent(AskEvent.Clear)
                            else askViewModel.onEvent(AskEvent.UpdateQuestion(it))
                        }
                    },
                    placeholder = stringResource(
                        if (askEnabled) R.string.search_or_ask_placeholder
                        else R.string.search_placeholder
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    showClearButton = state.query.isNotEmpty(),
                    onClear = {
                        viewModel.onEvent(SearchEvent.ClearSearch)
                        if (enableAsk) askViewModel.onEvent(AskEvent.Clear)
                    },
                    onSearch = { viewModel.onEvent(SearchEvent.ExecuteSearch) },
                    showAskButton = enableAsk,
                    askEnabled = askState.aiEnabled,
                    onAsk = { askViewModel.onEvent(AskEvent.Submit) },
                )

                // The filter appears only when there is a list to scope — while
                // typing or once an answer (with its merged list) is on screen.
                if (state.query.isNotEmpty() || answerPhase != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SearchFilter.entries.forEach { filter ->
                            FilterChip(
                                selected = state.selectedFilter == filter,
                                onClick = { viewModel.onEvent(SearchEvent.SetFilter(filter)) },
                                label = {
                                    Text(
                                        when (filter) {
                                            SearchFilter.ALL -> stringResource(R.string.all)
                                            SearchFilter.QURAN -> stringResource(R.string.quran)
                                            SearchFilter.HADITH -> stringResource(R.string.hadith)
                                            SearchFilter.DUA -> stringResource(R.string.duas)
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // AI-off discovery card — global search, resting state only.
                if (enableAsk && !askState.aiEnabled && !askState.hintDismissed &&
                    state.query.isEmpty()
                ) {
                    item(key = "ai_discover") {
                        AskDiscoveryCard(
                            onOpenSettings = onNavigateToSearchSettings,
                            onDismiss = { askViewModel.onEvent(AskEvent.DismissHint) },
                        )
                    }
                }

                // The AI hero card for the current phase (thinking / answer / error).
                if (askEnabled) {
                    when (val phase = askState.phase) {
                        AskPhase.Idle -> Unit
                        AskPhase.Loading -> item(key = "ai_card") { AskLoadingCard() }
                        is AskPhase.Answer -> item(key = "ai_card") {
                            AskAnswerCard(answer = phase.answer, confidence = phase.confidence)
                        }

                        is AskPhase.Error -> item(key = "ai_card") {
                            AskErrorCard(
                                error = phase.error,
                                onRetry = { askViewModel.onEvent(AskEvent.Submit) },
                            )
                        }
                    }
                }

                if (answerPhase != null) {
                    // Answer state: ONE merged list under the pinned filter —
                    // cited proofs first (marked "Cited"), then the related
                    // results driven by the AI's terms. A related result that is
                    // also cited is dropped so nothing appears twice. While the
                    // AI-terms lookup is still running, the keyword results the
                    // user was already looking at stay on screen below the cited
                    // rows and are swapped in place when it lands — the list
                    // never blanks into a separate loading stage.
                    val citedVisible = answerPhase.proofs
                        .filter { it.source.matchesFilter(state.selectedFilter) }
                    val citedIds = answerPhase.proofs.map { it.citationId }.toSet()
                    val related = state.filteredResults
                        .filterNot { it.citationKey() in citedIds }

                    if (!state.isSearching) {
                        item {
                            Text(
                                text = stringResource(
                                    R.string.search_answer_count_format,
                                    citedVisible.size + related.size,
                                    citedVisible.size
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    items(citedVisible, key = { it.citationId }) { proof ->
                        CitedProofCard(
                            proof = proof,
                            query = state.query,
                            onClick = { onNavigateToProof(proof.route) }
                        )
                    }

                    items(related) { result ->
                        UnifiedResultCard(
                            result = result,
                            query = state.query,
                            onNavigateToQuranAyah = onNavigateToQuranAyah,
                            onNavigateToSurah = onNavigateToSurah,
                            onNavigateToHadith = onNavigateToHadith,
                            onNavigateToDua = onNavigateToDua
                        )
                    }

                    if (state.isSearching && related.isEmpty()) {
                        item { SearchingIndicator() }
                    }
                } else if (state.query.isEmpty()) {
                    // Resting: example questions to ask (AI on), then recent
                    // searches and questions merged into one list.
                    if (askEnabled) {
                        item {
                            SectionHeader(title = stringResource(R.string.search_try_asking))
                        }
                        item {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    stringResource(R.string.ask_example_1),
                                    stringResource(R.string.ask_example_2),
                                    stringResource(R.string.ask_example_3),
                                    stringResource(R.string.ask_example_4),
                                ).forEach { example ->
                                    NimazBadge(
                                        text = example,
                                        icon = Icons.Default.AutoAwesome,
                                        shape = NimazBadgeShape.ROUNDED,
                                        size = NimazBadgeSize.LARGE,
                                        colors = NimazBadgeDefaults.colors(
                                            tone = NimazTone.ACCENT,
                                            emphasis = NimazBadgeEmphasis.OUTLINED
                                        ),
                                        onClick = {
                                            viewModel.onEvent(SearchEvent.UpdateQuery(example))
                                            askViewModel.onEvent(AskEvent.SelectRecent(example))
                                        }
                                    )
                                }
                            }
                        }
                    }

                    val mergedRecent =
                        if (askEnabled) {
                            (state.recentSearches + askState.recentQuestions).distinct().take(10)
                        } else {
                            state.recentSearches
                        }
                    if (mergedRecent.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = stringResource(R.string.search_recent),
                                actionLabel = stringResource(R.string.cd_clear),
                                onAction = { viewModel.onEvent(SearchEvent.ClearRecentSearches) }
                            )
                        }
                        items(mergedRecent) { recentSearch ->
                            RecentSearchItem(
                                query = recentSearch,
                                onClick = {
                                    viewModel.onEvent(SearchEvent.SelectRecentSearch(recentSearch))
                                    if (enableAsk) {
                                        askViewModel.onEvent(AskEvent.UpdateQuestion(recentSearch))
                                    }
                                },
                                onRemove = {
                                    viewModel.onEvent(SearchEvent.RemoveRecentSearch(recentSearch))
                                }
                            )
                        }
                    }
                } else {
                    // Typing: keyword results as-you-type (also the fallback list
                    // while an ask is loading or errored).
                    if (askEnabled && askState.phase is AskPhase.Idle) {
                        item {
                            Text(
                                text = stringResource(R.string.search_typing_hint),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (state.isSearching) {
                        item { SearchingIndicator() }
                    }

                    if (!state.isSearching && state.filteredResults.isNotEmpty()) {
                        item {
                            Text(
                                text = pluralStringResource(
                                    R.plurals.search_matches_format,
                                    statsState.totalResults,
                                    statsState.totalResults
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    items(state.filteredResults) { result ->
                        UnifiedResultCard(
                            result = result,
                            query = state.query,
                            onNavigateToQuranAyah = onNavigateToQuranAyah,
                            onNavigateToSurah = onNavigateToSurah,
                            onNavigateToHadith = onNavigateToHadith,
                            onNavigateToDua = onNavigateToDua
                        )
                    }

                    if (!state.isSearching && state.filteredResults.isEmpty()) {
                        item {
                            NimazEmptyState(
                                title = stringResource(R.string.no_results_format, state.query),
                                message = stringResource(R.string.no_results_hint),
                                icon = Icons.Default.Search,
                                iconTint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Source accents: Qur'an = teal (primary), Hadith = gold (secondary), Dua = deep purple (tertiary). */
@Composable
private fun sourceAccent(source: ProofSource): Color = when (source) {
    ProofSource.QURAN -> MaterialTheme.colorScheme.primary
    ProofSource.HADITH -> MaterialTheme.colorScheme.secondary
    ProofSource.DUA -> MaterialTheme.colorScheme.tertiary
}

private fun sourceIcon(source: ProofSource): ImageVector = when (source) {
    ProofSource.QURAN -> Icons.AutoMirrored.Filled.MenuBook
    ProofSource.HADITH -> Icons.Default.Book
    ProofSource.DUA -> Icons.Default.Mosque
}

private fun ProofSource.matchesFilter(filter: SearchFilter): Boolean = when (filter) {
    SearchFilter.ALL -> true
    SearchFilter.QURAN -> this == ProofSource.QURAN
    SearchFilter.HADITH -> this == ProofSource.HADITH
    SearchFilter.DUA -> this == ProofSource.DUA
}

/**
 * The citation id this result would carry if the AI cited it — used to drop
 * results that already appear as cited proofs. Surah results have no citation
 * form, so they never dedup away.
 */
private fun UnifiedSearchResult.citationKey(): String? = when (this) {
    is UnifiedSearchResult.QuranResult ->
        CitationId.Quran(result.ayah.surahNumber, result.ayah.ayahNumber).raw

    is UnifiedSearchResult.HadithResult -> CitationId.Hadith(result.hadith.id).raw
    is UnifiedSearchResult.DuaResult -> CitationId.Dua(result.dua.id).raw
    is UnifiedSearchResult.SurahResult -> null
}

@Composable
private fun SearchingIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        if (actionLabel != null && onAction != null) {
            Surface(onClick = onAction, shape = RoundedCornerShape(8.dp), color = Color.Transparent) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecentSearchItem(
    query: String,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    NimazCard(
        style = NimazCardStyle.ELEVATED,
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        tone = NimazTone.NEUTRAL
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NimazIcon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                variant = NimazIconVariant.MUTED,
                size = NimazIconSize.MEDIUM
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = query,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(24.dp)
            ) {
                NimazIcon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.remove),
                    size = NimazIconSize.SMALL
                )
            }
        }
    }
}

@Composable
private fun UnifiedResultCard(
    result: UnifiedSearchResult,
    query: String,
    onNavigateToQuranAyah: (Int, Int) -> Unit,
    onNavigateToSurah: (Int) -> Unit,
    onNavigateToHadith: (String, String) -> Unit,
    onNavigateToDua: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    when (result) {
        is UnifiedSearchResult.QuranResult -> SearchResultCard(
            icon = sourceIcon(ProofSource.QURAN),
            iconColor = sourceAccent(ProofSource.QURAN),
            type = stringResource(R.string.quran_type),
            title = stringResource(
                R.string.surah_result_format,
                result.result.ayah.surahNumber,
                result.result.ayah.ayahNumber
            ),
            subtitle = result.result.surahName,
            highlightedText = result.result.matchedText,
            query = query,
            onClick = {
                onNavigateToQuranAyah(
                    result.result.ayah.surahNumber,
                    result.result.ayah.ayahNumber
                )
            },
            modifier = modifier
        )

        is UnifiedSearchResult.SurahResult -> SurahSearchResultCard(
            surah = result.surah,
            onClick = { onNavigateToSurah(result.surah.number) },
            modifier = modifier
        )

        is UnifiedSearchResult.HadithResult -> SearchResultCard(
            icon = sourceIcon(ProofSource.HADITH),
            iconColor = sourceAccent(ProofSource.HADITH),
            type = stringResource(R.string.hadith_type),
            title = stringResource(R.string.hadith_result_format, result.result.hadith.hadithNumber),
            subtitle = result.result.bookName,
            highlightedText = result.result.matchedText,
            query = query,
            onClick = {
                onNavigateToHadith(
                    result.result.hadith.bookId,
                    result.result.hadith.id
                )
            },
            modifier = modifier
        )

        is UnifiedSearchResult.DuaResult -> SearchResultCard(
            icon = sourceIcon(ProofSource.DUA),
            iconColor = sourceAccent(ProofSource.DUA),
            type = stringResource(R.string.dua_type),
            title = result.result.dua.titleEnglish,
            subtitle = result.result.categoryName,
            highlightedText = result.result.matchedText,
            query = query,
            onClick = { onNavigateToDua(result.result.dua.id) },
            modifier = modifier
        )
    }
}

/**
 * A record the AI cited in its answer, rendered with the same shared result
 * card as every other result — identical title, subtitle, source tag and
 * English snippet as the keyword equivalent — just marked "Cited" and sorted
 * to the top. Tapping deep-links into the reader like any keyword result.
 */
@Composable
private fun CitedProofCard(
    proof: Proof,
    query: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (proof) {
        is Proof.Quran -> SearchResultCard(
            icon = sourceIcon(ProofSource.QURAN),
            iconColor = sourceAccent(ProofSource.QURAN),
            type = stringResource(R.string.quran_type),
            title = stringResource(
                R.string.surah_result_format,
                proof.surahNumber,
                proof.ayahNumber
            ),
            subtitle = proof.surahName,
            highlightedText = proof.displayText,
            query = query,
            onClick = onClick,
            cited = true,
            modifier = modifier
        )

        is Proof.Hadith -> SearchResultCard(
            icon = sourceIcon(ProofSource.HADITH),
            iconColor = sourceAccent(ProofSource.HADITH),
            type = stringResource(R.string.hadith_type),
            title = stringResource(R.string.hadith_result_format, proof.hadithNumber),
            subtitle = proof.bookName,
            highlightedText = proof.displayText,
            query = query,
            onClick = onClick,
            cited = true,
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SurahSearchResultCard(
    surah: com.arshadshah.nimaz.domain.model.Surah,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = sourceAccent(ProofSource.QURAN)
    NimazCard(
        style = NimazCardStyle.FILLED,
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = surah.number.toString(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = surah.nameEnglish,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.surah_ayahs_format,
                        surah.numberOfAyahs,
                        surah.nameTransliteration,
                        surah.numberOfAyahs
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ArabicText(
                text = surah.nameArabic,
                size = ArabicTextSize.SMALL,
                color = accent
            )
        }
    }
}

/**
 * The single result-card component shared by keyword results, cited proofs and
 * AI-related results. [cited] adds a solid teal "Cited" chip next to the source
 * tag and a teal left edge; everything else — badge, title, subtitle, source
 * tag, highlighted snippet — is identical across the three uses, so cited rows
 * read as part of the same list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchResultCard(
    icon: ImageVector,
    iconColor: Color,
    type: String?,
    title: String,
    subtitle: String?,
    highlightedText: String,
    query: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cited: Boolean = false
) {
    NimazCard(
        style = NimazCardStyle.FILLED,
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            if (cited) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                NimazIconWell(
                    icon = icon,
                    accent = iconColor,
                    size = NimazIconWellSize.MEDIUM,
                    shape = NimazIconWellShape.ROUNDED
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (type != null) {
                            NimazBadge(
                                text = type,
                                shape = NimazBadgeShape.ROUNDED,
                                size = NimazBadgeSize.SMALL,
                                colors = NimazBadgeDefaults.feature(
                                    color = iconColor,
                                    emphasis = NimazBadgeEmphasis.SOFT
                                )
                            )
                        }
                        if (cited) {
                            NimazBadge(
                                text = stringResource(R.string.search_cited_chip),
                                icon = Icons.Default.FormatQuote,
                                shape = NimazBadgeShape.ROUNDED,
                                size = NimazBadgeSize.SMALL,
                                colors = NimazBadgeDefaults.colors(
                                    tone = NimazTone.ACCENT,
                                    emphasis = NimazBadgeEmphasis.FILLED
                                ),
                                modifier = Modifier.padding(start = if (type != null) 6.dp else 0.dp)
                            )
                        }
                    }

                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    HighlightedText(
                        text = highlightedText,
                        query = query,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
private fun HighlightedText(
    text: String,
    query: String,
    style: androidx.compose.ui.text.TextStyle,
    maxLines: Int = Int.MAX_VALUE
) {
    // Nothing to highlight (and indexOf("") would loop forever) — plain text.
    if (query.isBlank()) {
        Text(
            text = text,
            style = style,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    val annotatedString = buildAnnotatedString {
        val lowerText = text.lowercase()
        val lowerQuery = query.lowercase()
        var startIndex = 0

        while (true) {
            val index = lowerText.indexOf(lowerQuery, startIndex)
            if (index < 0) {
                append(text.substring(startIndex))
                break
            }

            append(text.substring(startIndex, index))
            withStyle(
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                    background = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
            ) {
                append(text.substring(index, index + query.length))
            }
            startIndex = index + query.length
        }
    }

    Text(
        text = annotatedString,
        style = style,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
