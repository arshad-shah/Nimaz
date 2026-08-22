package com.arshadshah.nimaz.presentation.screens.quran

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.QuranTopic
import com.arshadshah.nimaz.domain.model.TopicTree
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconButtonSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazSegmentedControl
import com.arshadshah.nimaz.presentation.components.atoms.NimazSegmentedPurpose
import com.arshadshah.nimaz.presentation.components.atoms.NimazSegmentedWidth
import com.arshadshah.nimaz.presentation.components.atoms.asSegments
import com.arshadshah.nimaz.presentation.components.molecules.NimazBreadcrumbBar
import com.arshadshah.nimaz.presentation.components.molecules.NimazEmptyState
import com.arshadshah.nimaz.presentation.components.molecules.NimazLoadingState
import com.arshadshah.nimaz.presentation.components.molecules.NimazTreeRow
import com.arshadshah.nimaz.presentation.components.organisms.NimazSearchBar
import com.arshadshah.nimaz.presentation.components.organisms.NimazTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.quran.QuranTopicsEvent
import com.arshadshah.nimaz.presentation.viewmodel.quran.QuranTopicsViewModel
import com.arshadshah.nimaz.presentation.viewmodel.quran.TopicBrowseState

/**
 * Browsing the Qur'an's 2,512 subjects — three hierarchies, one screen.
 *
 * The tree tabs are not filters over one list; they are three different editors' answers to
 * "how is the Qur'an organised", and the app keeps all three rather than picking one. Themes
 * is the curated outline (Doctrine, Stories, The Unseen), Kinds is the ontology (Location,
 * Living Creation, …), Index is the shape a printed concordance has. Each carries a line of
 * plain words saying so, because three opaque nouns are not an explanation.
 *
 * The list is **one tree that opens in place**. Descent used to replace the list wholesale, so
 * every sibling on the way down was discarded and the only record of where you were was a
 * breadcrumb string in the top bar that truncated by level three. Now the children insert
 * beneath their parent, the crumb bar is a control, and the one thing that still re-roots the
 * list — "focus this branch" — is offered deliberately, at the depth where indenting again
 * would leave no text column.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranTopicsScreen(
    onNavigateBack: () -> Unit,
    onOpenTopic: (topicId: Int, tree: TopicTree) -> Unit,
    viewModel: QuranTopicsViewModel = hiltViewModel(),
) {
    val state by viewModel.browseState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.onEvent(QuranTopicsEvent.OpenBrowser) }

    BackHandler(enabled = state.canGoBack) {
        viewModel.onEvent(QuranTopicsEvent.Back)
    }

    NimazScreenScaffold(
        topBar = {
            NimazTopAppBar(
                title = stringResource(R.string.quran_topics_title),
                subtitle = stringResource(R.string.quran_topics_subtitle),
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
            if (!state.isAvailable) {
                NimazEmptyState(
                    title = stringResource(R.string.quran_topics_unavailable_title),
                    message = stringResource(R.string.quran_topics_unavailable),
                    icon = Icons.Default.Category,
                    modifier = Modifier.padding(20.dp),
                )
                return@Column
            }

            // Only where the tree is actually rooted somewhere. At the top of a hierarchy a
            // crumb bar would be a control with one thing in it and nothing to say.
            if (state.focus.isNotEmpty()) {
                NimazBreadcrumbBar(
                    home = stringResource(R.string.quran_topics_crumb_home),
                    crumbs = state.focus.map { it.name },
                    homeIcon = Icons.Default.AccountTree,
                    onCrumbClick = { index ->
                        viewModel.onEvent(QuranTopicsEvent.RebaseTo(index))
                    },
                )
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
            // anything while a query is live — but *removing* the tabs shifted every control
            // below them up by their height the instant a reader started typing. They stay,
            // dimmed and inert, and the line beneath says why.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .alpha(if (state.isSearchMode) DIMMED_ALPHA else 1f),
                horizontalArrangement = Arrangement.Center,
            ) {
                NimazSegmentedControl(
                    options = TREES.map { stringResource(it.second) }.asSegments(),
                    selectedIndex = TREES.indexOfFirst { it.first == state.tree },
                    onSelect = { index ->
                        if (!state.isSearchMode) {
                            viewModel.onEvent(QuranTopicsEvent.SelectTree(TREES[index].first))
                        }
                    },
                    width = NimazSegmentedWidth.WRAP,
                    purpose = NimazSegmentedPurpose.VIEW,
                )
            }

            ScopeNote(
                text = if (state.isSearchMode) {
                    stringResource(R.string.quran_topics_scope_search)
                } else {
                    stringResource(scopeNoteFor(state.tree))
                },
            )

            when {
                state.isLoading -> NimazLoadingState()

                state.isSearchMode -> SearchResults(
                    state = state,
                    onOpenTopic = onOpenTopic,
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 8.dp,
                        end = 8.dp,
                        top = 4.dp,
                        bottom = 24.dp,
                    ),
                ) {
                    items(state.rows, key = { it.topic.id }) { row ->
                        val atCap = state.isAtIndentCap(row.depth)
                        val branch = state.isBranch(row.topic)
                        NimazTreeRow(
                            label = row.topic.name,
                            secondaryLabel = row.topic.arabicName.takeIf { it.isNotBlank() },
                            // The subtree's total, not the node's own citations: a branch is
                            // cited against its children, so its own count is usually zero.
                            badgeText = (state.rolledUpCounts[row.topic.id] ?: row.topic.ayahCount)
                                .takeIf { it > 0 }
                                ?.toString(),
                            depth = row.depth,
                            expandable = branch && !atCap,
                            expanded = row.topic.id in state.expanded,
                            onToggleExpanded = {
                                viewModel.onEvent(QuranTopicsEvent.Toggle(row.topic))
                            },
                            trailingContent = if (branch && atCap) {
                                {
                                    // No room to indent again on a 390dp screen, so the way in
                                    // is to make this the root and pay for it with a crumb.
                                    NimazIconButton(
                                        icon = Icons.Default.CenterFocusStrong,
                                        size = NimazIconButtonSize.SMALL,
                                        contentDescription = stringResource(
                                            R.string.cd_topic_focus,
                                            row.topic.name,
                                        ),
                                        onClick = {
                                            viewModel.onEvent(
                                                QuranTopicsEvent.Focus(row.topic)
                                            )
                                        },
                                    )
                                }
                            } else null,
                            onClick = { onOpenTopic(row.topic.id, state.tree) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Matches, flat, each under the path it sits on.
 *
 * Flat and always opening: a result is an answer, and expanding one would quietly discard the
 * query that produced it. The path is what stops sixty matched words from being sixty words.
 */
@Composable
private fun SearchResults(
    state: TopicBrowseState,
    onOpenTopic: (topicId: Int, tree: TopicTree) -> Unit,
) {
    if (state.searchResults.isEmpty() && !state.isSearching) {
        NimazEmptyState(
            title = stringResource(R.string.quran_topics_no_results_title),
            message = stringResource(R.string.quran_topics_no_results, state.searchQuery),
            icon = Icons.Default.SearchOff,
            modifier = Modifier.padding(20.dp),
        )
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 24.dp),
    ) {
        items(state.searchResults, key = { it.id }) { topic ->
            NimazTreeRow(
                label = topic.name,
                secondaryLabel = topic.arabicName.takeIf { it.isNotBlank() },
                supportingText = state.searchPaths[topic.id]
                    ?.takeIf { it.isNotEmpty() }
                    ?.joinToString(" · ") { it.name },
                badgeText = topic.ayahCount.takeIf { it > 0 }?.toString(),
                onClick = { onOpenTopic(topic.id, state.tree) },
            )
        }
    }
}

/** One quiet line saying what the list below is, and what a query would search instead. */
@Composable
private fun ScopeNote(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        NimazIcon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            size = NimazIconSize.SMALL,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun scopeNoteFor(tree: TopicTree): Int = when (tree) {
    TopicTree.THEMATIC -> R.string.quran_topics_scope_thematic
    TopicTree.ONTOLOGY -> R.string.quran_topics_scope_ontology
    TopicTree.INDEX -> R.string.quran_topics_scope_index
}

private const val DIMMED_ALPHA = 0.42f

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
