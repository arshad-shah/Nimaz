package com.arshadshah.nimaz.presentation.screens.quran

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.QuranTopic
import com.arshadshah.nimaz.domain.model.TopicTree
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.molecules.NimazEmptyState
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuItem
import com.arshadshah.nimaz.presentation.components.organisms.NimazPillTabs
import com.arshadshah.nimaz.presentation.components.organisms.NimazSearchBar
import com.arshadshah.nimaz.presentation.components.organisms.NimazTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.QuranTopicsEvent
import com.arshadshah.nimaz.presentation.viewmodel.QuranTopicsViewModel

/**
 * Browsing the Qur'an's 2,512 subjects — three hierarchies, one screen.
 *
 * The tree tabs are not filters over one list; they are three different editors' answers to
 * "how is the Qur'an organised", and the app keeps all three rather than picking one. Themes
 * is the curated outline (Doctrine, Stories, The Unseen), Kinds is the ontology (Location,
 * Living Creation, …), Index is the shape a printed concordance has.
 *
 * Descent happens *in place*: the list becomes the children and the top bar grows a breadcrumb.
 * The system back gesture pops one level before it leaves the screen, which is the behaviour a
 * list that redraws itself has to have — otherwise back from five levels down exits entirely.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranTopicsScreen(
    onNavigateBack: () -> Unit,
    onOpenTopic: (topicId: Int, tree: TopicTree) -> Unit,
    viewModel: QuranTopicsViewModel = hiltViewModel(),
) {
    val state by viewModel.browseState.collectAsStateWithLifecycle()

    BackHandler(enabled = !state.isBrowsingRoots) {
        viewModel.onEvent(QuranTopicsEvent.Ascend)
    }

    NimazScreenScaffold(
        topBar = {
            NimazTopAppBar(
                title = state.current?.name ?: stringResource(R.string.quran_topics_title),
                subtitle = state.current?.let { topic ->
                    state.path.dropLast(1).joinToString(" › ") { it.name }
                        .ifBlank { topic.arabicName.ifBlank { null } }
                } ?: stringResource(R.string.quran_topics_subtitle),
                navigationIcon = {
                    NimazIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = {
                            if (state.isBrowsingRoots) {
                                onNavigateBack()
                            } else {
                                viewModel.onEvent(QuranTopicsEvent.Ascend)
                            }
                        },
                        contentDescription = if (state.isBrowsingRoots) {
                            stringResource(R.string.cd_back)
                        } else {
                            stringResource(R.string.cd_topic_up)
                        },
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
            if (!state.isAvailable) {
                NimazEmptyState(
                    title = stringResource(R.string.quran_topics_unavailable_title),
                    message = stringResource(R.string.quran_topics_unavailable),
                    icon = Icons.Default.Category,
                    modifier = Modifier.padding(20.dp),
                )
                return@Column
            }

            NimazSearchBar(
                query = state.searchQuery,
                onQueryChange = { viewModel.onEvent(QuranTopicsEvent.Search(it)) },
                onClear = { viewModel.onEvent(QuranTopicsEvent.ClearSearch) },
                placeholder = stringResource(R.string.quran_topics_search_hint),
                isLoading = state.isSearching,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            // Searching spans all three hierarchies, so which one is selected stops meaning
            // anything while a query is live — and a tab row that no longer filters the list
            // below it is a control that lies.
            if (state.searchQuery.isBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    NimazPillTabs(
                        tabs = TREES.map { stringResource(it.second) },
                        selectedIndex = TREES.indexOfFirst { it.first == state.tree },
                        onTabSelect = { index ->
                            viewModel.onEvent(QuranTopicsEvent.SelectTree(TREES[index].first))
                        },
                    )
                }
            }

            val topics = state.visibleTopics
            when {
                state.isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }

                topics.isEmpty() && state.searchQuery.isNotBlank() && !state.isSearching ->
                    NimazEmptyState(
                        title = stringResource(R.string.quran_topics_no_results_title),
                        message = stringResource(
                            R.string.quran_topics_no_results,
                            state.searchQuery,
                        ),
                        icon = Icons.Default.SearchOff,
                        modifier = Modifier.padding(20.dp),
                    )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp,
                        vertical = 8.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(topics, key = { it.id }) { topic ->
                        TopicRow(
                            topic = topic,
                            tree = state.tree,
                            searching = state.searchQuery.isNotBlank(),
                            onOpen = { onOpenTopic(topic.id, state.tree) },
                            onDescend = { viewModel.onEvent(QuranTopicsEvent.Descend(topic)) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * One subject in the list.
 *
 * Whether a tap descends or opens is decided by the *data*, not by a chevron the user has to
 * interpret: a topic with children is a level, a leaf is a destination. A search result always
 * opens, because a result is an answer and descending from it would discard the search.
 */
@Composable
private fun TopicRow(
    topic: QuranTopic,
    tree: TopicTree,
    searching: Boolean,
    onOpen: () -> Unit,
    onDescend: () -> Unit,
) {
    val verses = if (topic.ayahCount == 1) {
        stringResource(R.string.quran_topics_verse)
    } else {
        stringResource(R.string.quran_topics_verses, topic.ayahCount)
    }
    val subtitle = listOfNotNull(
        topic.arabicName.takeIf { it.isNotBlank() },
        verses.takeIf { topic.ayahCount > 0 },
    ).joinToString(" · ")

    NimazMenuItem(
        title = topic.name,
        subtitle = subtitle.ifBlank { null },
        onClick = if (searching) onOpen else onDescend,
        trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
        icon = when (tree) {
            TopicTree.THEMATIC -> Icons.Default.AccountTree
            TopicTree.ONTOLOGY -> Icons.Default.Category
            TopicTree.INDEX -> Icons.Default.ListAlt
        },
    )
}

/** Tab order, and the label each tree is shown under. */
private val TREES = listOf(
    TopicTree.THEMATIC to R.string.quran_topics_tree_thematic,
    TopicTree.ONTOLOGY to R.string.quran_topics_tree_ontology,
    TopicTree.INDEX to R.string.quran_topics_tree_index,
)

/**
 * A one-line label for a topic, shared by the browser and the ayah sheet.
 *
 * Kept here rather than on the model because "3 verses" is a localised string and the domain
 * layer has no resources.
 */
@Composable
fun topicVerseCountLabel(topic: QuranTopic): String =
    if (topic.ayahCount == 1) {
        stringResource(R.string.quran_topics_verse)
    } else {
        stringResource(R.string.quran_topics_verses, topic.ayahCount)
    }

@Composable
internal fun TopicNameWithCount(topic: QuranTopic, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = topic.name,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = topicVerseCountLabel(topic),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
