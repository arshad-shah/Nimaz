package com.arshadshah.nimaz.presentation.screens.dua

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.DuaOccasion
import com.arshadshah.nimaz.presentation.components.atoms.NimazLoadingState
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.content.DuaEvent
import com.arshadshah.nimaz.presentation.viewmodel.content.DuaViewModel

/**
 * Every dua for one occasion, gathered across the curated categories.
 *
 * The categories answer "where in the collection is this"; an occasion answers "what do I say
 * *now*" — and the same supplication is filed under a category whose name a reader looking for
 * it at that moment would not think of. `DuaEvent.LoadDuasByOccasion`, its repository method
 * and its query all shipped for this; nothing dispatched the event, so the cross-cut existed in
 * the database and nowhere a reader could reach.
 *
 * It renders from `categoryState`, the same surface `LoadCategory` fills — the ViewModel shares
 * one job between them precisely so an occasion list and a category list can never be live at
 * once. `state.category` is null here, so the header card the category screen shows is absent
 * and the occasion's own name carries the title.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuaOccasionScreen(
    occasion: DuaOccasion,
    onNavigateBack: () -> Unit,
    onNavigateToDua: (String) -> Unit,
    viewModel: DuaViewModel = hiltViewModel()
) {
    val state by viewModel.categoryState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    LaunchedEffect(occasion) {
        viewModel.onEvent(DuaEvent.LoadDuasByOccasion(occasion))
    }

    NimazScreenScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = occasion.label(),
                onBackClick = onNavigateBack,
                scrollBehavior = scrollBehavior,
                subtitle = pluralStringResource(
                    R.plurals.duas_count_format,
                    state.duas.size,
                    state.duas.size
                )
            )
        }
    ) { paddingValues ->
        val errorRes = state.error
        when {
            state.isLoading -> NimazLoadingState(modifier = Modifier.padding(paddingValues))

            errorRes != null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(errorRes),
                    color = MaterialTheme.colorScheme.error
                )
            }

            state.duas.isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.dua_occasion_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items = state.duas, key = { it.id }) { dua ->
                    DuaListItem(
                        dua = dua,
                        onClick = { onNavigateToDua(dua.id) }
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}
