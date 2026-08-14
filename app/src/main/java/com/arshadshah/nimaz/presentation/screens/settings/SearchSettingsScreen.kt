package com.arshadshah.nimaz.presentation.screens.settings

import androidx.annotation.StringRes
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.LibrarySource
import com.arshadshah.nimaz.domain.model.MatchStrictness
import com.arshadshah.nimaz.domain.model.SearchPreferences
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazDivider
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.molecules.NimazAccordion
import com.arshadshah.nimaz.presentation.components.molecules.NimazDialog
import com.arshadshah.nimaz.presentation.components.molecules.NimazDialogCancelButton
import com.arshadshah.nimaz.presentation.components.molecules.NimazDialogDestructiveButton
import com.arshadshah.nimaz.presentation.components.molecules.NimazListPicker
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuGroup
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuItem
import com.arshadshah.nimaz.presentation.components.molecules.NimazNumberStepper
import com.arshadshah.nimaz.presentation.components.molecules.NimazNumberStepperVariant
import com.arshadshah.nimaz.presentation.components.molecules.NimazPickerItem
import com.arshadshah.nimaz.presentation.components.molecules.NimazSettingsItem
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.settings.SearchSettingsEvent
import com.arshadshah.nimaz.presentation.viewmodel.settings.SearchSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SearchSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showStrictnessPicker by remember { mutableStateOf(false) }
    var showScopePicker by remember { mutableStateOf(false) }

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
            // ── Local search ──────────────────────────────────────────────
            // First, and above the AI section, because this is the search everyone has:
            // "Ask with Proof" is off by default and stays off for most installs.
            item { NimazSectionHeader(title = stringResource(R.string.search_results_section)) }
            item {
                NimazMenuGroup {
                    NimazNumberStepper(
                        value = state.search.resultsPerSource,
                        onValueChange = {
                            viewModel.onEvent(SearchSettingsEvent.SetResultsPerSource(it))
                        },
                        variant = NimazNumberStepperVariant.INLINE,
                        label = stringResource(R.string.search_results_per_source),
                        minValue = SearchPreferences.MIN_RESULTS_PER_SOURCE,
                        maxValue = SearchPreferences.MAX_RESULTS_PER_SOURCE,
                        step = 10,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    Text(
                        text = stringResource(
                            R.string.search_results_per_source_subtitle,
                            state.search.resultsPerSource * state.search.sources.size,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 12.dp),
                    )
                    NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    NimazSettingsItem(
                        title = stringResource(R.string.search_strictness),
                        subtitle = stringResource(strictnessDescription(state.search.strictness)),
                        value = stringResource(strictnessLabel(state.search.strictness)),
                        onClick = { showStrictnessPicker = true },
                        showArrow = true,
                    )
                    NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    NimazSettingsItem(
                        title = stringResource(R.string.search_default_scope),
                        subtitle = stringResource(R.string.search_default_scope_subtitle),
                        value = state.search.defaultScope
                            ?.let { stringResource(sourceLabel(it)) }
                            ?: stringResource(R.string.search_scope_everything),
                        onClick = { showScopePicker = true },
                        showArrow = true,
                    )
                }
            }

            // ── Where to search ───────────────────────────────────────────
            item { NimazSectionHeader(title = stringResource(R.string.search_sources_section)) }
            item {
                NimazMenuGroup {
                    LibrarySource.entries.forEachIndexed { index, source ->
                        if (index > 0) {
                            NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                        val isOn = source in state.search.sources
                        // The last source left on cannot be switched off: an empty set is a
                        // search that returns nothing for every query, and the sanitiser would
                        // read it straight back as "everything" — a switch that flips itself.
                        val isLastOn = isOn && state.search.sources.size == 1
                        NimazSettingsItem(
                            title = stringResource(sourceLabel(source)),
                            subtitle = if (isLastOn) {
                                stringResource(R.string.search_source_last_one)
                            } else {
                                stringResource(sourceDescription(source))
                            },
                            checked = isOn,
                            enabled = !isLastOn,
                            onCheckedChange = {
                                viewModel.onEvent(SearchSettingsEvent.ToggleSource(source))
                            },
                        )
                    }
                }
            }

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

    if (showStrictnessPicker) {
        NimazListPicker(
            title = stringResource(R.string.search_strictness),
            items = MatchStrictness.entries.map { strictness ->
                NimazPickerItem(
                    value = strictness,
                    title = stringResource(strictnessLabel(strictness)),
                    description = stringResource(strictnessDescription(strictness)),
                )
            },
            selected = state.search.strictness,
            onSelected = { viewModel.onEvent(SearchSettingsEvent.SetStrictness(it)) },
            onDismiss = { showStrictnessPicker = false },
        )
    }

    if (showScopePicker) {
        // "Everything" is not a source, so it cannot come from LibrarySource.entries — it is
        // the absence of a scope, and the picker models that as a nullable value.
        NimazListPicker(
            title = stringResource(R.string.search_default_scope),
            items = buildList<NimazPickerItem<LibrarySource?>> {
                add(
                    NimazPickerItem(
                        value = null,
                        title = stringResource(R.string.search_scope_everything),
                        description = stringResource(R.string.search_scope_everything_desc),
                    )
                )
                // Only sources that are actually searched: opening filtered to a switched-off
                // source would show an empty list that reads as "nothing matched".
                state.search.sources.forEach { source ->
                    add(
                        NimazPickerItem(
                            value = source,
                            title = stringResource(sourceLabel(source)),
                            description = stringResource(sourceDescription(source)),
                        )
                    )
                }
            },
            selected = state.search.defaultScope,
            onSelected = { viewModel.onEvent(SearchSettingsEvent.SetDefaultScope(it)) },
            onDismiss = { showScopePicker = false },
        )
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
                if (state.consentFailed) {
                    // The write did not commit. Saying so beats the sheet closing over a
                    // switch that has quietly stayed off.
                    Text(
                        text = stringResource(R.string.ai_consent_save_failed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
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

// ── enum → string resource ────────────────────────────────────────────────────
// Exhaustive `when`s rather than a name-keyed lookup: adding a source or a strictness level
// stops this file compiling until it has been given a label, which is the only thing that
// keeps a new option from shipping as a blank row.

@StringRes
private fun sourceLabel(source: LibrarySource): Int = when (source) {
    LibrarySource.QURAN -> R.string.quran
    LibrarySource.HADITH -> R.string.hadith
    LibrarySource.DUAS -> R.string.duas
    LibrarySource.NAMES -> R.string.names_title
}

@StringRes
private fun sourceDescription(source: LibrarySource): Int = when (source) {
    LibrarySource.QURAN -> R.string.search_source_quran_desc
    LibrarySource.HADITH -> R.string.search_source_hadith_desc
    LibrarySource.DUAS -> R.string.search_source_duas_desc
    LibrarySource.NAMES -> R.string.search_source_names_desc
}

@StringRes
private fun strictnessLabel(strictness: MatchStrictness): Int = when (strictness) {
    MatchStrictness.EXACT -> R.string.search_strictness_exact
    MatchStrictness.BALANCED -> R.string.search_strictness_balanced
    MatchStrictness.BROAD -> R.string.search_strictness_broad
}

@StringRes
private fun strictnessDescription(strictness: MatchStrictness): Int = when (strictness) {
    MatchStrictness.EXACT -> R.string.search_strictness_exact_desc
    MatchStrictness.BALANCED -> R.string.search_strictness_balanced_desc
    MatchStrictness.BROAD -> R.string.search_strictness_broad_desc
}
