package com.arshadshah.nimaz.presentation.screens.names

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazSegmentedControl
import com.arshadshah.nimaz.presentation.components.atoms.NimazSegmentedPurpose
import com.arshadshah.nimaz.presentation.components.atoms.asSegments
import com.arshadshah.nimaz.presentation.components.molecules.NameCard
import com.arshadshah.nimaz.presentation.components.molecules.NamesAccents
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.components.organisms.NimazSearchBar
import com.arshadshah.nimaz.presentation.screens.catalog.CatalogList
import com.arshadshah.nimaz.presentation.theme.NimazSpacing
import com.arshadshah.nimaz.presentation.viewmodel.content.AsmaUlHusnaViewModel
import com.arshadshah.nimaz.presentation.viewmodel.content.AsmaUnNabiViewModel
import com.arshadshah.nimaz.presentation.viewmodel.content.CatalogEvent
import com.arshadshah.nimaz.presentation.viewmodel.content.ProphetViewModel

/**
 * The three name catalogues, in the order a reader would name them.
 *
 * The ordinal is what `Route.Names(tab)` carries, so the order is part of the deep link and
 * reordering these would silently repoint every saved link and every announcement.
 */
enum class NamesTab(@param:StringRes val label: Int) {
    ASMA_UL_HUSNA(R.string.names_tab_allah),
    ASMA_UN_NABI(R.string.names_tab_prophet),
    PROPHETS(R.string.names_tab_prophets);

    companion object {
        /** [ordinal] back to a tab, tolerating an index from a build that had more of them. */
        fun fromOrdinal(index: Int): NamesTab = entries.getOrElse(index) { ASMA_UL_HUSNA }
    }
}

/**
 * Names — the ninety-nine Names of Allah, the names of the Prophet ﷺ, and the Prophets.
 *
 * These were **three destinations**: three More entries, three top bars, three search boxes and
 * three all/favourites filter rows, for what a reader thinks of as one place. The ViewModels
 * were already one generic `CatalogViewModel` and the list bodies already one `CatalogList`;
 * only the framing was in triplicate.
 *
 * So there is now one of each:
 *
 *  - **one search box**, above the tabs, whose query goes to all three catalogues at once. The
 *    tab labels carry the per-catalogue match count while a query is active, which is the same
 *    thing global search does with its source chips — you can see that "Rahman" hits two of the
 *    three before you tap into either.
 *  - **one favourites area**, reached from the top bar rather than a filter chip per tab. A
 *    per-tab filter could only ever show one catalogue's favourites; the destination it
 *    replaces shows all of them, and is built to take the rest of the app's favourites later.
 *
 * Each tab keeps its own ViewModel — they are separate catalogues with separate favourites and
 * separate detail routes — but `hiltViewModel()` scopes all three to this back-stack entry, so
 * switching tabs is free and the search state survives it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NamesScreen(
    initialTab: NamesTab,
    onNavigateBack: () -> Unit,
    onNavigateToFavourites: () -> Unit,
    onNavigateToAsmaUlHusna: (Int) -> Unit,
    onNavigateToAsmaUnNabi: (Int) -> Unit,
    onNavigateToProphet: (Int) -> Unit,
    asmaUlHusnaViewModel: AsmaUlHusnaViewModel = hiltViewModel(),
    asmaUnNabiViewModel: AsmaUnNabiViewModel = hiltViewModel(),
    prophetViewModel: ProphetViewModel = hiltViewModel(),
) {
    // Saveable rather than remembered: a rotation should not drop you back on the first tab.
    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab.ordinal) }
    var query by rememberSaveable { mutableStateOf("") }

    val allahState by asmaUlHusnaViewModel.listState.collectAsStateWithLifecycle()
    val prophetNamesState by asmaUnNabiViewModel.listState.collectAsStateWithLifecycle()
    val prophetsState by prophetViewModel.listState.collectAsStateWithLifecycle()

    /** One box, three catalogues — the whole point of combining the screens. */
    fun dispatch(event: CatalogEvent) {
        asmaUlHusnaViewModel.onEvent(event)
        asmaUnNabiViewModel.onEvent(event)
        prophetViewModel.onEvent(event)
    }

    fun search(text: String) {
        query = text
        dispatch(CatalogEvent.Search(text))
    }

    fun clearSearch() {
        query = ""
        // `ClearSearch` rather than `Search("")`: it is the event that exists for this, and
        // it also stops the debounced analytics flow from recording an empty query.
        dispatch(CatalogEvent.ClearSearch)
    }

    val counts = listOf(
        allahState.filteredItems.size,
        prophetNamesState.filteredItems.size,
        prophetsState.filteredItems.size,
    )

    NimazScreenScaffold(
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.names_title),
                onBackClick = onNavigateBack,
                actions = {
                    IconButton(onClick = onNavigateToFavourites) {
                        NimazIcon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = stringResource(R.string.cd_open_favourites),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            NimazSearchBar(
                query = query,
                onQueryChange = ::search,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = NimazSpacing.Large, vertical = NimazSpacing.Small),
                placeholder = stringResource(R.string.names_search_hint),
                showClearButton = query.isNotEmpty(),
                onClear = ::clearSearch,
                // The Prophets copy left this off, so its keyboard's search key did nothing
                // while the other two searched. One screen, one answer.
                onSearch = ::search,
            )

            NimazSegmentedControl(
                options = NamesTab.entries.mapIndexed { index, tab ->
                    val label = stringResource(tab.label)
                    // The count is only meaningful against a query — without one it would
                    // just restate how big each catalogue is, on every tab, forever.
                    if (query.isBlank()) label else "$label (${counts[index]})"
                }.asSegments(),
                selectedIndex = selectedTab,
                onSelect = { selectedTab = it },
                purpose = NimazSegmentedPurpose.VIEW,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = NimazSpacing.Large,
                        vertical = NimazSpacing.ExtraSmall,
                    ),
            )

            when (NamesTab.fromOrdinal(selectedTab)) {
                NamesTab.ASMA_UL_HUSNA -> {
                    val accent = NamesAccents.allah()
                    CatalogList(
                        state = allahState,
                        emptyMessage = stringResource(R.string.asma_ul_husna_no_names_found),
                        accent = accent,
                        itemKey = { it.id },
                    ) { name ->
                        NameCard(
                            number = name.id,
                            arabicName = name.nameArabic,
                            primaryLabel = name.nameTransliteration,
                            secondaryLabel = name.nameEnglish,
                            isFavorite = name.isFavorite,
                            accent = accent,
                            onClick = { onNavigateToAsmaUlHusna(name.id) },
                            onFavoriteClick = {
                                asmaUlHusnaViewModel.onEvent(CatalogEvent.ToggleFavorite(name.id))
                            },
                        )
                    }
                }

                NamesTab.ASMA_UN_NABI -> {
                    val accent = NamesAccents.prophetNames()
                    CatalogList(
                        state = prophetNamesState,
                        emptyMessage = stringResource(R.string.asma_un_nabi_no_names_found),
                        accent = accent,
                        itemKey = { it.id },
                    ) { name ->
                        NameCard(
                            number = name.id,
                            arabicName = name.nameArabic,
                            primaryLabel = name.nameTransliteration,
                            secondaryLabel = name.nameEnglish,
                            isFavorite = name.isFavorite,
                            accent = accent,
                            onClick = { onNavigateToAsmaUnNabi(name.id) },
                            onFavoriteClick = {
                                asmaUnNabiViewModel.onEvent(CatalogEvent.ToggleFavorite(name.id))
                            },
                        )
                    }
                }

                NamesTab.PROPHETS -> {
                    val accent = NamesAccents.prophets()
                    CatalogList(
                        state = prophetsState,
                        emptyMessage = stringResource(R.string.prophets_no_found),
                        accent = accent,
                        itemKey = { it.id },
                    ) { prophet ->
                        // The one card that differs: the English name leads, and a title and
                        // era chip ride along.
                        NameCard(
                            number = prophet.id,
                            arabicName = prophet.nameArabic,
                            primaryLabel = prophet.nameEnglish,
                            secondaryLabel = prophet.nameTransliteration,
                            isFavorite = prophet.isFavorite,
                            accent = accent,
                            onClick = { onNavigateToProphet(prophet.id) },
                            onFavoriteClick = {
                                prophetViewModel.onEvent(CatalogEvent.ToggleFavorite(prophet.id))
                            },
                            titleLabel = prophet.titleEnglish,
                            eraChip = prophet.era,
                        )
                    }
                }
            }
        }
    }
}
