package com.arshadshah.nimaz.presentation.screens.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.atoms.NimazLoadingState
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.molecules.FavoriteFab
import com.arshadshah.nimaz.presentation.components.molecules.NameDetailHeader
import com.arshadshah.nimaz.presentation.components.molecules.NamesAccent
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazSpacing
import com.arshadshah.nimaz.presentation.viewmodel.content.CatalogDetailState

/**
 * One catalog item: a calligraphic header, a favourite button, and the prose sections that
 * item happens to have.
 *
 * The counterpart of [CatalogListScreen] for the detail half (audit §1.3). Everything above
 * the sections was written twice — the scaffold, the back bar titled by the item, the
 * favourite FAB, the loading branch, the `LazyColumn` and the header — and the sections
 * themselves are the only part that is really per-catalog: the Names of Allah have a meaning,
 * an explanation, benefits, Qur'an references and a use in duʿāʾ; the Names of the Prophet ﷺ
 * have a meaning, an explanation and a source.
 *
 * So the sections are a [LazyListScope] slot. A caller writes the `item { … }` blocks it needs
 * and inherits the rest, which is why this takes a slot rather than a list of (title, body)
 * pairs — the Qur'an-references section is a `FlowRow` of chips, not prose, and a data-shaped
 * API would have had to grow a variant for it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : Any> CatalogDetailScreen(
    state: CatalogDetailState<T>,
    onNavigateBack: () -> Unit,
    onToggleFavorite: (T) -> Unit,
    title: String,
    accent: NamesAccent,
    isFavorite: (T) -> Boolean,
    header: CatalogDetailHeader<T>,
    sections: LazyListScope.(T) -> Unit,
) {
    NimazScreenScaffold(
        topBar = {
            NimazBackTopAppBar(title = title, onBackClick = onNavigateBack)
        },
        floatingActionButton = {
            state.item?.let { item ->
                FavoriteFab(
                    isFavorite = isFavorite(item),
                    accent = accent,
                    onClick = { onToggleFavorite(item) }
                )
            }
        }
    ) { paddingValues ->
        val entry = state.item
        if (state.isLoading || entry == null) {
            NimazLoadingState(modifier = Modifier.padding(paddingValues))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(
                    horizontal = NimazSpacing.Large,
                    vertical = NimazSpacing.Small
                ),
                verticalArrangement = Arrangement.spacedBy(NimazSpacing.Medium)
            ) {
                item {
                    NameDetailHeader(
                        arabicName = header.arabicName(entry),
                        accent = accent,
                        number = header.number(entry),
                        primaryLabel = header.primaryLabel(entry),
                        secondaryLabel = header.secondaryLabel(entry),
                    )
                }
                sections(entry)
                // Clearance for the favourite FAB, which floats over the last section.
                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }
    }
}

/** Which of an item's fields the calligraphic header shows. */
data class CatalogDetailHeader<T>(
    val number: (T) -> Int,
    val arabicName: (T) -> String,
    val primaryLabel: (T) -> String,
    val secondaryLabel: (T) -> String,
)
