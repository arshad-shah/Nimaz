package com.arshadshah.nimaz.presentation.screens.prayer

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.tracker.PrayerTrackerEvent
import com.arshadshah.nimaz.presentation.viewmodel.tracker.PrayerTrackerViewModel

/**
 * The make-up list: every prayer the user has **explicitly** marked missed.
 *
 * Delegates to [QadaPrayerList] -- the summary card, month grouping and empty state are shared
 * with the qada tab inside [PrayerTrackerScreen] on purpose (see the comment on
 * [QadaPrayerList]); this screen only supplies the top bar and its own padding. Reads
 * [PrayerTrackerViewModel] rather than owning a ViewModel of its own: the qada state, its event
 * and its use cases already live there, and a second ViewModel would mean a second collector on
 * the same Room flow for no gain.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QadaPrayersScreen(
    onNavigateBack: () -> Unit,
    viewModel: PrayerTrackerViewModel = hiltViewModel()
) {
    val qadaState by viewModel.qadaState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    NimazScreenScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.qada_prayers),
                onBackClick = onNavigateBack,
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        QadaPrayerList(
            state = qadaState,
            onMarkCompleted = { viewModel.onEvent(PrayerTrackerEvent.MarkQadaCompleted(it)) },
            modifier = Modifier.padding(paddingValues),
        )
    }
}
