package com.arshadshah.nimaz.presentation.screens.quran

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.arshadshah.nimaz.core.util.ThematicLink
import com.arshadshah.nimaz.domain.model.TopicCitation
import com.arshadshah.nimaz.domain.model.TopicTree
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionTitle
import com.arshadshah.nimaz.presentation.components.molecules.NimazEmptyState
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuItem
import com.arshadshah.nimaz.presentation.components.molecules.ThematicText
import com.arshadshah.nimaz.presentation.components.organisms.NimazTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.QuranTopicsEvent
import com.arshadshah.nimaz.presentation.viewmodel.QuranTopicsViewModel

/**
 * One subject: what it is, where it sits, and every verse that speaks to it.
 *
 * The citation list is the whole point — "Allah" cites 153 verses, "Patience" a dozen — so it
 * is the body of the screen rather than a section buried under prose, and every row opens the
 * reader at that verse. The description's own `topic:` cross-links navigate here too, which is
 * what turns 2,512 rows into something you can actually wander through.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranTopicDetailScreen(
    topicId: Int,
    tree: TopicTree,
    onNavigateBack: () -> Unit,
    onOpenAyah: (surah: Int, ayah: Int) -> Unit,
    onOpenTopic: (topicId: Int, tree: TopicTree) -> Unit,
    viewModel: QuranTopicsViewModel = hiltViewModel(),
) {
    val state by viewModel.detailState.collectAsStateWithLifecycle()

    LaunchedEffect(topicId, tree) {
        viewModel.onEvent(QuranTopicsEvent.LoadDetail(topicId, tree))
    }

    val detail = state.detail

    NimazScreenScaffold(
        topBar = {
            NimazTopAppBar(
                title = detail?.topic?.name ?: stringResource(R.string.quran_topics_title),
                subtitle = detail?.breadcrumb
                    ?.takeIf { it.isNotEmpty() }
                    ?.joinToString(" › ") { it.name }
                    ?: detail?.topic?.arabicName?.takeIf { it.isNotBlank() },
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

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (detail.topic.hasDescription) {
                    item(key = "description") {
                        NimazCard(
                            style = NimazCardStyle.FILLED,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            ThematicText(
                                html = detail.topic.description,
                                modifier = Modifier.padding(16.dp),
                                onLinkClick = { link ->
                                    when (link) {
                                        is ThematicLink.Topic ->
                                            onOpenTopic(link.id, detail.tree)

                                        is ThematicLink.Verses ->
                                            onOpenAyah(link.surah, link.from ?: 1)
                                    }
                                },
                            )
                        }
                    }
                }

                if (detail.children.isNotEmpty()) {
                    item(key = "subtopics-title") {
                        NimazSectionTitle(
                            text = stringResource(R.string.quran_topic_subtopics),
                            uppercase = false,
                        )
                    }
                    items(detail.children, key = { "child-${it.id}" }) { child ->
                        NimazMenuItem(
                            title = child.name,
                            subtitle = topicVerseCountLabel(child),
                            icon = Icons.Default.AccountTree,
                            onClick = { onOpenTopic(child.id, detail.tree) },
                        )
                    }
                }

                if (detail.related.isNotEmpty()) {
                    item(key = "related-title") {
                        NimazSectionTitle(
                            text = stringResource(R.string.quran_topic_related),
                            uppercase = false,
                        )
                    }
                    items(detail.related, key = { "related-${it.id}" }) { related ->
                        NimazMenuItem(
                            title = related.name,
                            subtitle = topicVerseCountLabel(related),
                            icon = Icons.Default.Link,
                            onClick = { onOpenTopic(related.id, detail.tree) },
                        )
                    }
                }

                if (detail.citations.isNotEmpty()) {
                    item(key = "citations-title") {
                        Column {
                            NimazSectionTitle(
                                text = stringResource(R.string.quran_topic_verses_section),
                                uppercase = false,
                            )
                            Text(
                                text = topicVerseCountLabel(detail.topic),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    items(detail.citations, key = { "ayah-${it.ayahId}" }) { citation ->
                        CitationRow(
                            citation = citation,
                            onClick = { onOpenAyah(citation.surahNumber, citation.ayahNumber) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CitationRow(citation: TopicCitation, onClick: () -> Unit) {
    NimazMenuItem(
        title = citation.reference,
        subtitle = stringResource(R.string.quran_topic_open_reader),
        icon = Icons.AutoMirrored.Filled.MenuBook,
        onClick = onClick,
    )
}
