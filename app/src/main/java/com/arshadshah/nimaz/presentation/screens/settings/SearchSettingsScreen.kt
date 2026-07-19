package com.arshadshah.nimaz.presentation.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazDivider
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.molecules.NimazAccordion
import com.arshadshah.nimaz.presentation.components.molecules.NimazDialog
import com.arshadshah.nimaz.presentation.components.molecules.NimazDialogCancelButton
import com.arshadshah.nimaz.presentation.components.molecules.NimazDialogDestructiveButton
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuGroup
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuItem
import com.arshadshah.nimaz.presentation.components.molecules.NimazSettingsItem
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.SearchSettingsEvent
import com.arshadshah.nimaz.presentation.viewmodel.SearchSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SearchSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    NimazScreenScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.search_settings),
                onBackClick = onNavigateBack,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── AI answers ────────────────────────────────────────────────
            item { NimazSectionHeader(title = stringResource(R.string.ai_answers)) }
            item {
                NimazMenuGroup {
                    NimazSettingsItem(
                        title = stringResource(R.string.ai_answers_enable),
                        subtitle = stringResource(R.string.ai_answers_enable_subtitle),
                        checked = state.aiEnabled,
                        onCheckedChange = {
                            viewModel.onEvent(SearchSettingsEvent.ToggleAiRequested)
                        },
                    )
                }
            }

            // ── Privacy ───────────────────────────────────────────────────
            item { NimazSectionHeader(title = stringResource(R.string.ai_privacy)) }
            item {
                NimazAccordion(title = stringResource(R.string.ai_what_gets_shared)) {
                    Text(
                        text = stringResource(R.string.ai_disclosure_full),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
            item {
                NimazMenuGroup {
                    NimazSettingsItem(
                        title = stringResource(R.string.ai_history),
                        subtitle = stringResource(R.string.ai_history_subtitle),
                        checked = state.historyEnabled,
                        onCheckedChange = {
                            viewModel.onEvent(SearchSettingsEvent.SetHistoryEnabled(it))
                        },
                    )
                    NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    // An in-place destructive action, not navigation — no
                    // trailing arrow; disabled while there is nothing to clear.
                    NimazMenuItem(
                        title = stringResource(R.string.ai_clear_history),
                        subtitle = stringResource(R.string.ai_clear_history_subtitle),
                        trailingIcon = null,
                        enabled = state.savedQuestions.isNotEmpty(),
                        onClick = { showClearHistoryDialog = true },
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showClearHistoryDialog) {
        NimazDialog(
            title = stringResource(R.string.ai_clear_history_dialog_title),
            titleIcon = Icons.Default.Delete,
            accentColor = MaterialTheme.colorScheme.error,
            showCloseButton = false,
            onDismiss = { showClearHistoryDialog = false },
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.ai_clear_history_dialog_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    state.savedQuestions.forEach { question ->
                        Text(
                            text = "•  $question",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            },
            actions = {
                NimazDialogCancelButton(
                    text = stringResource(R.string.cancel),
                    onClick = { showClearHistoryDialog = false },
                )
                NimazDialogDestructiveButton(
                    text = stringResource(R.string.delete),
                    onClick = {
                        viewModel.onEvent(SearchSettingsEvent.ClearHistory)
                        showClearHistoryDialog = false
                    },
                )
            },
        )
    }

    if (state.showConsentSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel.onEvent(SearchSettingsEvent.ConsentDismissed) },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.ai_consent_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(R.string.ai_disclosure_full),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                NimazButton(
                    text = stringResource(R.string.ai_consent_enable),
                    onClick = { viewModel.onEvent(SearchSettingsEvent.ConsentAccepted) },
                    variant = NimazButtonVariant.FILLED,
                    fullWidth = true,
                )
                NimazButton(
                    text = stringResource(R.string.cancel),
                    onClick = { viewModel.onEvent(SearchSettingsEvent.ConsentDismissed) },
                    variant = NimazButtonVariant.TEXT,
                    fullWidth = true,
                )
            }
        }
    }
}
