package com.arshadshah.nimaz.presentation.screens.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.molecules.NamesAccent
import com.arshadshah.nimaz.presentation.components.molecules.NimazEmptyState
import com.arshadshah.nimaz.presentation.components.molecules.NimazLoadingState
import com.arshadshah.nimaz.presentation.theme.NimazSpacing
import com.arshadshah.nimaz.presentation.viewmodel.content.CatalogEvent
import com.arshadshah.nimaz.presentation.viewmodel.content.CatalogListState

/**
 * A list of catalog items — one tab of the Names screen.
 *
 * The Names of Allah, the Names of the Prophet ﷺ and the Prophets are three catalogs with the
 * same shape. The *ViewModel* layer already knew this — [com.arshadshah.nimaz.presentation.viewmodel.content.CatalogViewModel]
 * is generic in the item type and all three share [CatalogEvent] and [CatalogListState]. Only
 * the screens had been copied, three times, 136 lines each and differing in a ViewModel, an
 * accent, three string resources and how a card labels itself (audit §1.3).
 *
 * It used to own a scaffold, a top bar, a search box and an all/favourites filter of its own,
 * because it was a whole destination. It is now a tab body: the search box and the favourites
 * area moved up to [com.arshadshah.nimaz.presentation.screens.names.NamesScreen], which has
 * one of each for all three catalogs rather than three of each.
 *
 * The card is a slot rather than a config field because it is the one difference that is not a
 * value: the Prophets list puts the English name first and adds a title and an era chip, and a
 * `NameCard(...)` call expresses that better than five more parameters would.
 */
@Composable
fun <T : Any> CatalogList(
    state: CatalogListState<T>,
    emptyMessage: String,
    accent: NamesAccent,
    itemKey: (T) -> Any,
    modifier: Modifier = Modifier,
    card: @Composable (T) -> Unit,
) {
    if (state.isLoading) {
        NimazLoadingState()
        return
    }

    val displayList = state.filteredItems

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = NimazSpacing.Large,
            vertical = NimazSpacing.Small,
        ),
        verticalArrangement = Arrangement.spacedBy(NimazSpacing.Small),
    ) {
        items(items = displayList, key = itemKey) { item -> card(item) }

        if (displayList.isEmpty()) {
            item {
                NimazEmptyState(
                    title = emptyMessage,
                    message = "",
                    icon = Icons.Filled.Favorite,
                    iconTint = accent.contentTint,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                )
            }
        }
    }
}
