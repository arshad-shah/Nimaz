package com.arshadshah.nimaz.presentation.screens.quran

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.SurahTopic
import com.arshadshah.nimaz.domain.model.TopicTree
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazLoadingState
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.molecules.NimazEmptyState
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuGroup
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuItem
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconVariant
import com.arshadshah.nimaz.presentation.components.molecules.NimazTreeRow
import com.arshadshah.nimaz.presentation.components.organisms.NimazSearchBar
import com.arshadshah.nimaz.presentation.components.organisms.NimazTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.quran.QuranTopicsEvent
import com.arshadshah.nimaz.presentation.viewmodel.quran.QuranTopicsViewModel

/**
 * What one surah speaks about — its own subjects, weightiest here first.
 *
 * "Subjects in this surah" used to open the global browser at the top of the thematic tree: the
 * same twenty roots — Doctrine, Stories, The Unseen — whichever surah you had just been reading,
 * with nothing carrying the surah across. The reader was holding a specific question and the
 * screen answered a general one.
 *
 * This is the specific answer. A flat list, not a tree, because the hierarchies place a subject
 * relative to *other subjects*, and that is not the question here; the ordering is by how many
 * of this surah's verses the subject actually takes, so Al-Baqarah leads with what it is about
 * rather than with whichever subject is busiest Qur'an-wide. Each row says how far the subject
 * reaches beyond these verses, which is what separates a subject this surah owns from one that
 * is everywhere and touches here once. The whole index is still one tap away, at the bottom,
 * where a reader who wanted the general question can go and ask it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahSubjectsScreen(
    surahNumber: Int,
    onNavigateBack: () -> Unit,
    onOpenTopic: (topicId: Int, tree: TopicTree) -> Unit,
    onBrowseAllSubjects: () -> Unit,
    viewModel: QuranTopicsViewModel = hiltViewModel(),
) {
    val state by viewModel.surahSubjects.collectAsStateWithLifecycle()

    LaunchedEffect(surahNumber) {
        viewModel.onEvent(QuranTopicsEvent.LoadSurahSubjects(surahNumber))
    }

    NimazScreenScaffold(
        topBar = {
            NimazTopAppBar(
                title = state.surahName.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.surah_subjects_title),
                subtitle = stringResource(R.string.surah_subjects_subtitle),
                navigationIcon = {
                    NimazIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = onNavigateBack,
                        contentDescription = stringResource(R.string.cd_back),
                    )
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.isLoading -> NimazLoadingState()

                // No rows at all is not an empty search. Almost always it is an install whose
                // artifact predates the thematic layer, which is a sentence about the install
                // and not about the surah — so the two cases say different things.
                state.subjects.isEmpty() -> NimazEmptyState(
                    title = if (state.isAvailable) {
                        stringResource(R.string.surah_subjects_none_title)
                    } else {
                        stringResource(R.string.quran_topics_unavailable_title)
                    },
                    message = if (state.isAvailable) {
                        stringResource(R.string.surah_subjects_none)
                    } else {
                        stringResource(R.string.quran_topics_unavailable)
                    },
                    icon = Icons.Default.Category,
                    // The way out, offered where the list would have been. Only where there is
                    // an index to browse — on an install without one it is a button onto the
                    // same sentence.
                    actionLabel = stringResource(R.string.surah_subjects_browse_all)
                        .takeIf { state.isAvailable },
                    onAction = onBrowseAllSubjects.takeIf { state.isAvailable },
                    modifier = Modifier.padding(20.dp),
                )

                else -> {
                    // Filtering only where there is enough to lose something in. Under a
                    // screenful, a filter box is a control that costs more room than it saves.
                    if (state.subjects.size >= FILTER_THRESHOLD) {
                        NimazSearchBar(
                            query = state.query,
                            onQueryChange = {
                                viewModel.onEvent(QuranTopicsEvent.FilterSurahSubjects(it))
                            },
                            onClear = {
                                viewModel.onEvent(QuranTopicsEvent.ClearSurahSubjectsFilter)
                            },
                            placeholder = stringResource(R.string.surah_subjects_filter_hint),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }

                    SubjectList(
                        subjects = state.visible,
                        totalCount = state.subjects.size,
                        citations = state.citations,
                        query = state.query,
                        onOpenTopic = onOpenTopic,
                        onBrowseAllSubjects = onBrowseAllSubjects,
                    )
                }
            }
        }
    }
}

@Composable
private fun SubjectList(
    subjects: List<SurahTopic>,
    totalCount: Int,
    citations: Int,
    query: String,
    onOpenTopic: (topicId: Int, tree: TopicTree) -> Unit,
    onBrowseAllSubjects: () -> Unit,
) {
    if (subjects.isEmpty()) {
        NimazEmptyState(
            title = stringResource(R.string.quran_topics_no_results_title),
            message = stringResource(R.string.surah_subjects_no_match, query, totalCount),
            icon = Icons.Default.SearchOff,
            modifier = Modifier.padding(20.dp),
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 24.dp),
    ) {
        item(key = "count") {
            NimazSectionHeader(
                title = stringResource(R.string.surah_subjects_count, totalCount),
                trailingText = stringResource(
                    R.string.surah_subjects_citations,
                    citations,
                ),
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp),
            )
        }

        items(subjects, key = { it.topic.id }) { subject ->
            NimazTreeRow(
                label = subject.topic.name,
                secondaryLabel = subject.topic.arabicName.takeIf { it.isNotBlank() },
                supportingText = reachLabel(subject),
                badgeText = subject.versesInSurah.toString(),
                // The tree the subject actually sits in, not the tab that happened to be
                // selected somewhere else — otherwise the 1,817 subjects the thematic outline
                // does not place open with no breadcrumb and no subtopics.
                onClick = { onOpenTopic(subject.topic.id, subject.topic.homeTree) },
                // A chevron, because these rows go somewhere and nothing said so: the count
                // badge reads as a fact about the row, not as an invitation to open it.
                trailingContent = {
                    NimazIcon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        variant = NimazIconVariant.MUTED,
                        size = NimazIconSize.SMALL,
                    )
                },
            )
        }

        // The general question, kept reachable. Below the specific answer rather than instead
        // of it, which is the whole of what this screen changes.
        item(key = "browse-all") {
            NimazMenuGroup(modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp)) {
                NimazMenuItem(
                    title = stringResource(R.string.surah_subjects_browse_all),
                    subtitle = stringResource(R.string.surah_subjects_browse_all_subtitle),
                    icon = Icons.Default.AccountTree,
                    onClick = onBrowseAllSubjects,
                )
            }
        }
    }
}

/**
 * How far the subject reaches past these verses.
 *
 * The number that makes the list readable: "Patience — 12 here, 91 elsewhere" is a subject the
 * Qur'an returns to, and "The cow — 7 here, none elsewhere" is what this surah is named for.
 * The subtraction is clamped because a subject's stored `ayahCount` and its citation rows are
 * two fields in the artifact, and a negative remainder would be a number about a disagreement.
 */
@Composable
private fun reachLabel(subject: SurahTopic): String {
    val elsewhere = (subject.topic.ayahCount - subject.versesInSurah).coerceAtLeast(0)
    return if (elsewhere == 0) {
        stringResource(R.string.surah_subjects_only_here)
    } else {
        stringResource(R.string.surah_subjects_elsewhere, elsewhere)
    }
}

/** Below this many subjects the whole list fits in a scroll or two, and a filter is clutter. */
private const val FILTER_THRESHOLD = 12
