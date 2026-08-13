package com.arshadshah.nimaz.presentation.screens.names

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.molecules.NameCard
import com.arshadshah.nimaz.presentation.components.molecules.NamesAccents
import com.arshadshah.nimaz.presentation.components.molecules.NimazEmptyState
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazSpacing
import com.arshadshah.nimaz.presentation.viewmodel.content.AsmaUlHusnaViewModel
import com.arshadshah.nimaz.presentation.viewmodel.content.AsmaUnNabiViewModel
import com.arshadshah.nimaz.presentation.viewmodel.content.CatalogEvent
import com.arshadshah.nimaz.presentation.viewmodel.content.ProphetViewModel

/**
 * Everything the user has starred, in one place.
 *
 * Favourites used to be an all/favourites filter chip *inside* each of the three name screens,
 * which meant there was no answer to "what have I saved" — only "what have I saved in this one
 * catalogue", asked three times. This is the answer, reached from the Names top bar.
 *
 * **Built for the rest of the app.** Duas, ayat and hadiths all carry an `isFavorite` too, and
 * they are meant to end up here. The screen is a list of sections, one per kind of thing, each
 * hidden when it is empty — so adding duas is a [favouriteSection] call and a ViewModel, not a
 * rewrite. The empty state is the whole screen's, not a section's, for the same reason: a user
 * with two starred duas and no starred names should see the duas, not "no favourites yet".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavouritesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAsmaUlHusna: (Int) -> Unit,
    onNavigateToAsmaUnNabi: (Int) -> Unit,
    onNavigateToProphet: (Int) -> Unit,
    asmaUlHusnaViewModel: AsmaUlHusnaViewModel = hiltViewModel(),
    asmaUnNabiViewModel: AsmaUnNabiViewModel = hiltViewModel(),
    prophetViewModel: ProphetViewModel = hiltViewModel(),
) {
    val allah by asmaUlHusnaViewModel.listState.collectAsStateWithLifecycle()
    val prophetNames by asmaUnNabiViewModel.listState.collectAsStateWithLifecycle()
    val prophets by prophetViewModel.listState.collectAsStateWithLifecycle()

    // Resolved here, not at the call sites: `LazyColumn`'s content lambda is a plain
    // `LazyListScope.() -> Unit`, so `stringResource` cannot be called inside it — only
    // inside the `item {}` blocks it builds.
    val allahTitle = stringResource(R.string.names_tab_allah)
    val prophetNameTitle = stringResource(R.string.names_tab_prophet)
    val prophetsTitle = stringResource(R.string.names_tab_prophets)

    val isEmpty = allah.favorites.isEmpty() &&
        prophetNames.favorites.isEmpty() &&
        prophets.favorites.isEmpty()

    NimazScreenScaffold(
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.favorites),
                onBackClick = onNavigateBack,
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                horizontal = NimazSpacing.Large,
                vertical = NimazSpacing.Small,
            ),
            verticalArrangement = Arrangement.spacedBy(NimazSpacing.Small),
        ) {
            if (isEmpty) {
                item {
                    NimazEmptyState(
                        title = stringResource(R.string.no_favorites_yet),
                        message = stringResource(R.string.favourites_empty_message),
                        icon = Icons.Filled.FavoriteBorder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                    )
                }
                return@LazyColumn
            }

            favouriteSection(
                title = allahTitle,
                items = allah.favorites,
                key = { "allah:${it.id}" },
            ) { name ->
                val accent = NamesAccents.allah()
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

            favouriteSection(
                title = prophetNameTitle,
                items = prophetNames.favorites,
                key = { "prophet-name:${it.id}" },
            ) { name ->
                val accent = NamesAccents.prophetNames()
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

            favouriteSection(
                title = prophetsTitle,
                items = prophets.favorites,
                key = { "prophet:${it.id}" },
            ) { prophet ->
                val accent = NamesAccents.prophets()
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

/**
 * One kind of favourite, with its header — and nothing at all when there is none of it.
 *
 * A section that renders an empty header is what turns a consolidated favourites screen into a
 * wall of headings, so the emptiness check lives here rather than at each call site.
 */
private fun <T> LazyListScope.favouriteSection(
    title: String,
    items: List<T>,
    key: (T) -> Any,
    card: @Composable (T) -> Unit,
) {
    if (items.isEmpty()) return
    item(key = "header:$title") { NimazSectionHeader(title = title) }
    items(items = items, key = key) { item -> card(item) }
}
