package com.arshadshah.nimaz.presentation.screens.quran

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.util.ThematicLink
import com.arshadshah.nimaz.domain.model.TopicTree
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazChip
import com.arshadshah.nimaz.presentation.components.atoms.NimazChipVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.components.molecules.CitationRow
import com.arshadshah.nimaz.presentation.components.molecules.HOME_INDEX
import com.arshadshah.nimaz.presentation.components.molecules.NimazBreadcrumbBar
import com.arshadshah.nimaz.presentation.components.molecules.NimazEmptyState
import com.arshadshah.nimaz.presentation.components.molecules.NimazTreeRow
import com.arshadshah.nimaz.presentation.components.molecules.ThematicText
import com.arshadshah.nimaz.presentation.components.organisms.NimazTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.quran.QuranTopicsEvent
import com.arshadshah.nimaz.presentation.viewmodel.quran.QuranTopicsViewModel

/**
 * One subject: what it is, where it sits, and every verse that speaks to it.
 *
 * Four kinds of content, four shapes. They used to be one shape — `NimazMenuItem` rows that
 * differed only by icon, so a subtopic, a related subject and a cited verse all looked like the
 * same kind of thing, and every one of up to 153 citations repeated "Open in reader" as its
 * subtitle. That is a hundred and fifty-three identical lines saying what a tap does.
 *
 * Now: the description is body prose rather than something boxed in a card, because it is the
 * body and not an aside; subtopics are the same tree rows the browser uses, because they are
 * the same content; related subjects are chips, because they are lateral moves and not content;
 * and the citations — the substance — are grouped under the surahs they fall in, with a line of
 * each verse so the list can be read instead of merely opened.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranTopicDetailScreen(
    topicId: Int,
    tree: TopicTree,
    onNavigateBack: () -> Unit,
    onOpenAyah: (surah: Int, ayah: Int) -> Unit,
    onOpenTopic: (topicId: Int, tree: TopicTree) -> Unit,
    fromSurah: Int? = null,
    viewModel: QuranTopicsViewModel = hiltViewModel(),
) {
    val state by viewModel.detailState.collectAsStateWithLifecycle()

    LaunchedEffect(topicId, tree, fromSurah) {
        viewModel.onEvent(QuranTopicsEvent.LoadDetail(topicId, tree, fromSurah))
    }

    val detail = state.detail

    NimazScreenScaffold(
        topBar = {
            NimazTopAppBar(
                title = detail?.topic?.name ?: stringResource(R.string.quran_topics_title),
                subtitle = detail?.topic?.arabicName?.takeIf { it.isNotBlank() },
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
        when {
            state.isLoading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            detail == null -> NimazEmptyState(
                title = stringResource(R.string.quran_topics_no_results_title),
                message = stringResource(R.string.quran_topic_not_found),
                icon = Icons.Default.Category,
                modifier = Modifier
                    .padding(padding)
                    .padding(20.dp),
            )

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                // The same crumb bar the browser carries, so "where does this sit" is answered
                // the same way and with the same control in both places. Every crumb goes back
                // to the browser, which is the only screen that can show a level.
                if (detail.breadcrumb.isNotEmpty()) {
                    NimazBreadcrumbBar(
                        home = stringResource(R.string.quran_topics_crumb_home),
                        crumbs = detail.breadcrumb.map { it.name },
                        onCrumbClick = { index ->
                            if (index == HOME_INDEX) onNavigateBack()
                            else onOpenTopic(detail.breadcrumb[index].id, detail.tree)
                        },
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                ) {
                    item(key = "count") {
                        Row(
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            NimazBadge(
                                text = topicVerseCountLabel(detail.topic),
                                tone = NimazTone.ACCENT,
                            )
                            // How much of this subject is in the surah the reader came from.
                            // Beside the total rather than instead of it: the point of the pair
                            // is the ratio — 12 of 153 is a passing mention, 12 of 14 is not.
                            state.surahContext?.let { context ->
                                NimazBadge(
                                    text = stringResource(
                                        R.string.quran_topic_in_surah,
                                        context.verseCount,
                                        context.surahName,
                                    ),
                                    tone = NimazTone.PROMINENT,
                                )
                            }
                        }
                    }

                    if (detail.topic.hasDescription) {
                        item(key = "description") {
                            ThematicText(
                                html = detail.topic.description,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(
                                    start = 20.dp,
                                    end = 20.dp,
                                    top = 12.dp,
                                ),
                                onLinkClick = { link ->
                                    when (link) {
                                        is ThematicLink.Topic -> onOpenTopic(link.id, detail.tree)
                                        is ThematicLink.Verses ->
                                            onOpenAyah(link.surah, link.from ?: 1)
                                    }
                                },
                            )
                        }
                    }

                    if (detail.children.isNotEmpty()) {
                        item(key = "subtopics-title") {
                            SectionHeader(stringResource(R.string.quran_topic_subtopics))
                        }
                        items(detail.children, key = { "child-${it.id}" }) { child ->
                            NimazTreeRow(
                                label = child.name,
                                secondaryLabel = child.arabicName.takeIf { it.isNotBlank() },
                                badgeText = child.ayahCount.takeIf { it > 0 }?.toString(),
                                modifier = Modifier.padding(horizontal = 8.dp),
                                onClick = { onOpenTopic(child.id, detail.tree) },
                            )
                        }
                    }

                    if (detail.related.isNotEmpty()) {
                        item(key = "related") {
                            SectionHeader(stringResource(R.string.quran_topic_related))
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(detail.related, key = { it.id }) { related ->
                                    NimazChip(
                                        text = related.name,
                                        variant = NimazChipVariant.SUGGESTION,
                                        leadingIcon = Icons.Default.Link,
                                        onClick = { onOpenTopic(related.id, detail.tree) },
                                    )
                                }
                            }
                        }
                    }

                    if (state.citationGroups.isNotEmpty()) {
                        item(key = "citations-title") {
                            SectionHeader(
                                title = stringResource(R.string.quran_topic_verses_section),
                                trailing = stringResource(
                                    R.string.quran_topic_verses_across,
                                    detail.citations.size,
                                    state.citationGroups.size,
                                ),
                            )
                        }
                        state.citationGroups.forEach { group ->
                            stickyHeader(key = "surah-${group.surahNumber}") {
                                CitationGroupHeader(
                                    surahName = group.surahName,
                                    verseCount = group.citations.size,
                                    isFromSurah = group.isFromSurah,
                                )
                            }
                            items(
                                items = group.citations,
                                key = { "ayah-${it.ayahId}" },
                            ) { citation ->
                                CitationRow(
                                    reference = citation.reference,
                                    preview = state.previews[citation.ayahId],
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    onClick = {
                                        onOpenAyah(citation.surahNumber, citation.ayahNumber)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Which surah the verses beneath belong to.
 *
 * Sticky, and opaque rather than translucent: it sits over scrolling verse text, and a header
 * you can read the list through is a header that stops answering the question it is there for.
 *
 * [isFromSurah] marks the one group that was lifted out of Qur'anic order — the surah the
 * reader arrived from. Lifting it silently would be the worse half of the change: a list that
 * looks like it is in the mushaf's sequence and is not.
 */
@Composable
private fun CitationGroupHeader(
    surahName: String,
    verseCount: Int,
    isFromSurah: Boolean = false,
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 6.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = surahName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (isFromSurah) {
                NimazBadge(
                    text = stringResource(R.string.quran_topic_surah_you_came_from),
                    tone = NimazTone.PROMINENT,
                )
            }
            Text(
                text = if (verseCount == 1) {
                    stringResource(R.string.quran_topics_verse)
                } else {
                    stringResource(R.string.quran_topics_verses, verseCount)
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, trailing: String? = null) {
    NimazSectionHeader(
        title = title,
        trailingText = trailing,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp),
    )
}
