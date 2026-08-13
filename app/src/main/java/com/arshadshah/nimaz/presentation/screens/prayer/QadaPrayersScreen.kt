package com.arshadshah.nimaz.presentation.screens.prayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restore
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
import com.arshadshah.nimaz.presentation.components.molecules.NimazEmptyState
import com.arshadshah.nimaz.presentation.components.molecules.NimazQadaPrayerItem
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.tracker.PrayerTrackerEvent
import com.arshadshah.nimaz.presentation.viewmodel.tracker.PrayerTrackerViewModel

/**
 * The make-up list: every prayer the user has **explicitly** marked missed.
 *
 * A pushed destination rather than a tab, and it shares [PrayerTrackerViewModel] rather than
 * owning one: the qada state, its event and its use cases already live there, and a second
 * ViewModel would mean a second collector on the same Room flow for no gain.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QadaPrayersScreen(
    onNavigateBack: () -> Unit,
    viewModel: PrayerTrackerViewModel = hiltViewModel(),
) {
    val qadaState by viewModel.qadaState.collectAsStateWithLifecycle()

    NimazScreenScaffold(
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.qada_prayers),
                onBackClick = onNavigateBack,
            )
        }
    ) { paddingValues ->
        if (qadaState.missedPrayers.isEmpty()) {
            NimazEmptyState(
                icon = Icons.Default.Restore,
                title = stringResource(R.string.qada_empty_title),
                message = stringResource(R.string.qada_empty_body),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    NimazSectionHeader(
                        title = stringResource(R.string.qada_outstanding),
                        trailingText = stringResource(
                            R.string.qada_count_format,
                            qadaState.missedPrayers.size
                        ),
                    )
                }
                items(qadaState.missedPrayers, key = { "${it.date}-${it.prayerName}" }) { prayer ->
                    NimazQadaPrayerItem(
                        prayer = prayer,
                        actionText = stringResource(R.string.qada_mark_made_up),
                        onMarkCompleted = {
                            viewModel.onEvent(PrayerTrackerEvent.MarkQadaCompleted(prayer))
                        },
                    )
                }
            }
        }
    }
}
