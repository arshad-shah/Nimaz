package com.arshadshah.nimaz.presentation.screens.khatam

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.Khatam
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.atoms.NimazSegmentedControl
import com.arshadshah.nimaz.presentation.components.atoms.NimazSegmentedPurpose
import com.arshadshah.nimaz.presentation.components.atoms.asSegments
import com.arshadshah.nimaz.presentation.components.molecules.KhatamHeroCard
import com.arshadshah.nimaz.presentation.components.molecules.KhatamRowCard
import com.arshadshah.nimaz.presentation.components.molecules.NimazEmptyState
import com.arshadshah.nimaz.presentation.components.molecules.NimazLoadingState
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.foundation.tokens.rememberKhatamAccent
import com.arshadshah.nimaz.presentation.theme.NimazSpacing
import com.arshadshah.nimaz.presentation.viewmodel.quran.KhatamListUiState
import com.arshadshah.nimaz.presentation.viewmodel.quran.KhatamViewModel

/** Which status bucket the list is filtered to. */
private enum class KhatamTab { IN_PROGRESS, COMPLETED, ARCHIVED }

/** Vertical room reserved so the extended FAB never covers the last card. */
private val FabClearance = 88.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KhatamListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToCreate: () -> Unit,
    // No default: a no-op default silently swallowed the hero's continue tap when a
    // call site forgot to wire it. Required means the compiler catches that.
    onNavigateToRead: (Int, Int) -> Unit,
    viewModel: KhatamViewModel = hiltViewModel(),
) {
    val state by viewModel.listState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableStateOf(KhatamTab.IN_PROGRESS) }

    NimazScreenScaffold(
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.khatam_title),
                onBackClick = onNavigateBack,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToCreate,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.khatam_start_new)) },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> NimazLoadingState(modifier = Modifier.padding(padding))

            !state.hasAnyKhatam -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(NimazSpacing.Large),
                contentAlignment = Alignment.Center,
            ) {
                NimazEmptyState(
                    title = stringResource(R.string.khatam_no_started),
                    message = stringResource(R.string.khatam_start_journey),
                    icon = Icons.Default.MenuBook,
                    iconTint = MaterialTheme.colorScheme.primary,
                    actionLabel = stringResource(R.string.khatam_start_new),
                    onAction = onNavigateToCreate,
                )
            }

            else -> KhatamListContent(
                state = state,
                selectedTab = selectedTab,
                onTabSelect = { selectedTab = it },
                contentPadding = padding,
                onKhatamClick = onNavigateToDetail,
                onContinue = {
                    val surah = state.nextUnreadSurah
                    val ayah = state.nextUnreadAyah
                    if (surah != null && ayah != null) onNavigateToRead(surah, ayah)
                },
            )
        }
    }
}

@Composable
private fun KhatamListContent(
    state: KhatamListUiState,
    selectedTab: KhatamTab,
    onTabSelect: (KhatamTab) -> Unit,
    contentPadding: PaddingValues,
    onKhatamClick: (Long) -> Unit,
    onContinue: () -> Unit,
) {
    val accent = rememberKhatamAccent()
    val tabs = listOf(
        stringResource(R.string.khatam_section_in_progress),
        stringResource(R.string.khatam_section_completed),
        stringResource(R.string.khatam_section_archived),
    )

    val visible = when (selectedTab) {
        KhatamTab.IN_PROGRESS -> state.inProgressKhatams
        KhatamTab.COMPLETED -> state.completedKhatams
        KhatamTab.ARCHIVED -> state.abandonedKhatams
    }

    val active = state.activeKhatam
    val activeInsights = state.activeInsights
    val hasNextPosition = state.nextUnreadSurah != null && state.nextUnreadAyah != null
    val continueText = continueLabel(state)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = NimazSpacing.Large,
            end = NimazSpacing.Large,
            top = contentPadding.calculateTopPadding() + NimazSpacing.Small,
            bottom = contentPadding.calculateBottomPadding() + FabClearance,
        ),
        verticalArrangement = Arrangement.spacedBy(NimazSpacing.Small),
    ) {
        if (active != null && activeInsights != null) {
            item(key = "hero-${active.id}") {
                KhatamHeroCard(
                    khatam = active,
                    insights = activeInsights,
                    accent = accent,
                    continueLabel = continueText,
                    onContinue = onContinue.takeIf { hasNextPosition },
                    onClick = { onKhatamClick(active.id) },
                )
            }
        }

        item(key = "tabs") {
            NimazSegmentedControl(
                options = tabs.asSegments(),
                selectedIndex = selectedTab.ordinal,
                onSelect = { onTabSelect(KhatamTab.entries[it]) },
                purpose = NimazSegmentedPurpose.VIEW,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = NimazSpacing.Small),
            )
        }

        if (visible.isEmpty()) {
            item(key = "empty-${selectedTab.name}") {
                NimazEmptyState(
                    title = stringResource(emptyTitleRes(selectedTab)),
                    message = stringResource(R.string.khatam_start_journey),
                    icon = Icons.Default.MenuBook,
                    iconTint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = NimazSpacing.Medium),
                )
            }
        } else {
            item(key = "header-${selectedTab.name}") {
                NimazSectionHeader(
                    title = tabs[selectedTab.ordinal],
                    trailingText = visible.size.toString(),
                )
            }
            items(visible, key = { it.id }) { khatam ->
                KhatamRowCard(
                    khatam = khatam,
                    accent = accent,
                    subtitle = rowSubtitle(khatam),
                    onClick = { onKhatamClick(khatam.id) },
                )
            }
        }
    }
}

/** "Continue · Surah 8 12", or a plain label when no position is known yet. */
@Composable
private fun continueLabel(state: KhatamListUiState): String {
    val ayah = state.nextUnreadAyah
    // Prefer the surah's name; fall back to "Surah N" only if the lookup failed.
    val surahLabel = state.nextUnreadSurahName
        ?: state.nextUnreadSurah?.let { stringResource(R.string.surah_number_format, it) }
    return if (surahLabel != null && ayah != null) {
        stringResource(R.string.khatam_continue_at, surahLabel, ayah)
    } else {
        stringResource(R.string.khatam_continue_reading)
    }
}

/** "1,240 ayahs read · Started 12 Feb 2026" — or the finish date once complete. */
@Composable
private fun rowSubtitle(khatam: Khatam): String {
    val read = pluralStringResource(
        R.plurals.khatam_ayahs_read_plural,
        khatam.totalAyahsRead,
        khatam.totalAyahsRead,
    )
    val formatter = rememberKhatamDateFormatter()
    val date = khatam.completedAt ?: khatam.startedAt ?: khatam.createdAt
    val dateLabel = stringResource(
        if (khatam.completedAt != null) R.string.khatam_finished_on else R.string.khatam_started_on,
        formatter.format(date),
    )
    return "$read · $dateLabel"
}

private fun emptyTitleRes(tab: KhatamTab): Int = when (tab) {
    KhatamTab.IN_PROGRESS -> R.string.khatam_no_in_progress
    KhatamTab.COMPLETED -> R.string.khatam_no_completed
    KhatamTab.ARCHIVED -> R.string.khatam_no_archived
}
