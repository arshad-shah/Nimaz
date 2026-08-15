package com.arshadshah.nimaz.presentation.screens.khatam

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.Khatam
import com.arshadshah.nimaz.domain.model.KhatamStatus
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.molecules.KhatamHeroCard
import com.arshadshah.nimaz.presentation.components.molecules.NimazDropdownMenu
import com.arshadshah.nimaz.presentation.components.molecules.NimazDropdownRow
import com.arshadshah.nimaz.presentation.components.molecules.NimazLoadingState
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuItem
import com.arshadshah.nimaz.presentation.components.organisms.KhatamJourneyTrail
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.components.organisms.NimazStatData
import com.arshadshah.nimaz.presentation.components.organisms.NimazStatsGrid
import com.arshadshah.nimaz.presentation.foundation.tokens.rememberKhatamAccent
import com.arshadshah.nimaz.presentation.theme.NimazSpacing
import com.arshadshah.nimaz.presentation.viewmodel.quran.KhatamDetailUiState
import com.arshadshah.nimaz.presentation.viewmodel.quran.KhatamEvent
import com.arshadshah.nimaz.presentation.viewmodel.quran.KhatamViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KhatamDetailScreen(
    khatamId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToRead: (Int, Int) -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    viewModel: KhatamViewModel = hiltViewModel(),
) {
    val state by viewModel.detailState.collectAsStateWithLifecycle()
    var menuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(khatamId) {
        viewModel.onEvent(KhatamEvent.LoadKhatamDetail(khatamId))
    }

    // A deleted khatam resolves to notFound rather than an endless spinner.
    LaunchedEffect(state.notFound) {
        if (state.notFound) onNavigateBack()
    }

    val khatam = state.khatam

    NimazScreenScaffold(
        topBar = {
            NimazBackTopAppBar(
                title = khatam?.name ?: stringResource(R.string.khatam_detail),
                onBackClick = onNavigateBack,
                actions = {
                    if (khatam != null) {
                        NimazIconButton(
                            icon = Icons.Default.Edit,
                            onClick = { onNavigateToEdit(khatam.id) },
                            contentDescription = stringResource(R.string.khatam_edit),
                        )
                        // Making a khatam active from here is the only quick path when
                        // another one is already running.
                        if (!khatam.isActive && khatam.status == KhatamStatus.ACTIVE) {
                            Box {
                                NimazIconButton(
                                    icon = Icons.Default.MoreVert,
                                    onClick = { menuExpanded = true },
                                    contentDescription =
                                        stringResource(R.string.khatam_more_actions),
                                )
                                NimazDropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false },
                                ) {
                                    NimazDropdownRow(
                                        text = stringResource(R.string.khatam_set_active),
                                        leadingIcon = Icons.Default.Star,
                                        onClick = {
                                            menuExpanded = false
                                            viewModel.onEvent(
                                                KhatamEvent.SetActiveKhatam(khatam.id)
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> NimazLoadingState(modifier = Modifier.padding(padding))

            khatam == null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.khatam_not_found),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> KhatamDetailContent(
                khatam = khatam,
                state = state,
                contentPadding = padding,
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
private fun KhatamDetailContent(
    khatam: Khatam,
    state: KhatamDetailUiState,
    contentPadding: PaddingValues,
    onContinue: () -> Unit,
) {
    val accent = rememberKhatamAccent()
    val insights = state.insights
    val hasNextPosition = state.nextUnreadSurah != null && state.nextUnreadAyah != null

    val surahLabel = state.nextUnreadSurahName
        ?: state.nextUnreadSurah?.let { stringResource(R.string.surah_number_format, it) }
    val continueText = if (hasNextPosition && surahLabel != null) {
        stringResource(R.string.khatam_continue_at, surahLabel, state.nextUnreadAyah!!)
    } else {
        stringResource(R.string.khatam_continue_reading)
    }

    val stats = listOf(
        NimazStatData(
            value = insights.currentStreak.toString(),
            label = stringResource(R.string.khatam_stat_streak),
        ),
        NimazStatData(
            value = insights.averagePace.roundToInt().toString(),
            label = stringResource(R.string.khatam_avg_pace),
        ),
        NimazStatData(
            value = insights.juzCompleted.toString(),
            label = stringResource(R.string.khatam_stat_juz_done),
        ),
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = NimazSpacing.Large,
            end = NimazSpacing.Large,
            top = contentPadding.calculateTopPadding() + NimazSpacing.Small,
            bottom = contentPadding.calculateBottomPadding() + NimazSpacing.ExtraLarge,
        ),
        verticalArrangement = Arrangement.spacedBy(NimazSpacing.Medium),
    ) {
        item(key = "hero") {
            KhatamHeroCard(
                khatam = khatam,
                insights = insights,
                accent = accent,
                // The top bar already names the khatam.
                showName = false,
                showActiveBadge = khatam.isActive,
                // The plan's own instruction leads. A khatam exists to assign a daily portion;
                // where you happened to stop is the fallback, and it was the headline here
                // because it was the only thing the screen knew how to say.
                continueLabel = state.todaysPortionLabel?.let {
                    stringResource(R.string.khatam_read_todays_portion)
                } ?: continueText,
                onContinue = onContinue.takeIf { hasNextPosition },
            )
        }

        // What today asks for, named. Only where there is a portion — a finished plan should
        // stop giving orders.
        state.todaysPortionLabel?.let { portion ->
            item(key = "todays-portion") {
                NimazMenuItem(
                    title = stringResource(R.string.khatam_todays_portion),
                    subtitle = portion,
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    onClick = onContinue,
                    trailingIcon = null,
                )
            }
        }

        item(key = "stats") {
            NimazStatsGrid(stats = stats)
        }

        item(key = "journey-header") {
            NimazSectionHeader(
                title = stringResource(R.string.khatam_section_journey),
                trailingText = stringResource(
                    R.string.khatam_journey_progress,
                    insights.juzCompleted,
                    Khatam.TOTAL_JUZ,
                ),
            )
        }

        item(key = "journey") {
            KhatamJourneyTrail(
                juzProgress = state.juzProgress,
                accent = accent,
            )
        }
    }
}
