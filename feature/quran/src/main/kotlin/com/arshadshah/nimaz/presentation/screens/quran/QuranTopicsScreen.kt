package com.arshadshah.nimaz.presentation.screens.quran

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.derivedStateOf
import com.arshadshah.nimaz.presentation.components.atoms.NimazDivider
import com.arshadshah.nimaz.presentation.components.molecules.NimazIndexRail
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.QuranTopic
import com.arshadshah.nimaz.domain.model.TopicTree
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazFilterChip
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazSegmentedControl
import com.arshadshah.nimaz.presentation.components.atoms.NimazSegmentedPurpose
import com.arshadshah.nimaz.presentation.components.atoms.NimazSegmentedWidth
import com.arshadshah.nimaz.presentation.components.atoms.asSegments
import com.arshadshah.nimaz.presentation.components.molecules.NimazEmptyState
import com.arshadshah.nimaz.presentation.components.molecules.NimazLoadingState
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuDivider
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuItem
import com.arshadshah.nimaz.presentation.components.organisms.KindTile
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.components.organisms.NimazSearchBar
import com.arshadshah.nimaz.presentation.components.organisms.ThemeChapterCard
import com.arshadshah.nimaz.presentation.components.organisms.formatCount
import com.arshadshah.nimaz.presentation.components.organisms.verseCountLabel
import com.arshadshah.nimaz.presentation.viewmodel.quran.QuranTopicsEvent
import com.arshadshah.nimaz.presentation.viewmodel.quran.QuranTopicsViewModel
import com.arshadshah.nimaz.presentation.viewmodel.quran.TopicBrowseState
import com.arshadshah.nimaz.presentation.viewmodel.quran.TopicIndexSort
import com.arshadshah.nimaz.presentation.viewmodel.quran.TopicTally
import kotlinx.coroutines.launch

/**
 * Browsing the Qur'an's 2,512 subjects — three hierarchies, three shapes.
 *
 * The tabs are not filters over one list; they are three different editors' answers to "how is
 * the Qur'an organised", and each is drawn the way that answer is best read. **Themes** is the
 * curated outline, so it is chapters: three cards whose bars show how each divides and whose
 * chips are the ways in. **Kinds** is the ontology — fourteen kinds of thing — so it is a grid
 * of tiles. **Index** is a printed concordance, so it is A–Z with a letter rail, or its most
 * cited entries.
 *
 * Every tap opens a subject. The tree that used to expand in place here — with a crumb bar and a
 * "focus this branch" button for when the indent ran out — is now the subject screen's job: its
 * subtopics, its place in the hierarchy, its verses.
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

    // A live query is a mode over the page, so back leaves the mode before it leaves the page.
    BackHandler(enabled = state.isSearchMode) {
        viewModel.onEvent(QuranTopicsEvent.ClearSearch)
    }

    NimazScreenScaffold(
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.quran_topics_title),
                subtitle = stringResource(R.string.quran_topics_subtitle),
                onBackClick = onNavigateBack,
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

            val open = { topic: QuranTopic, tree: TopicTree -> onOpenTopic(topic.id, tree) }
            when {
                state.isLoading -> NimazLoadingState()
                state.isSearchMode -> SearchResults(state = state, onOpen = open)
                state.tree == TopicTree.THEMATIC -> ThemesTab(state, onOpen = open)
                state.tree == TopicTree.ONTOLOGY -> KindsTab(state, onOpen = open)
                else -> IndexTab(
                    state = state,
                    onSort = { viewModel.onEvent(QuranTopicsEvent.SetIndexSort(it)) },
                    onOpen = open,
                )
            }
        }
    }
}

@Composable
private fun ThemesTab(state: TopicBrowseState, onOpen: (QuranTopic, TopicTree) -> Unit) {
    val byId = remember(state.themes) {
        state.themes.flatMap { listOf(it.root) + it.branches }.associate { it.topic.id to it.topic }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(state.themes, key = { it.root.topic.id }) { card ->
            ThemeChapterCard(
                card = card,
                onOpenTopic = { id -> byId[id]?.let { onOpen(it, TopicTree.THEMATIC) } },
            )
        }
    }
}

@Composable
private fun KindsTab(state: TopicBrowseState, onOpen: (QuranTopic, TopicTree) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(state.kinds, key = { it.topic.id }) { tally ->
            KindTile(tally = tally, onClick = { onOpen(tally.topic, TopicTree.ONTOLOGY) })
        }
    }
}

/**
 * The concordance: A–Z under letter headings, or the most-cited entries. Two ways into 1,780
 * entries, because nobody scrolls a list that long from the top.
 *
 * The heading of the section on screen **sticks** to the top of the list, solid while it is
 * pinned, and the rail beside it **follows** — the same letter filled on both — so "where am I"
 * is answered twice without looking for anything. Dragging the rail scrubs the list, a bubble
 * showing the letter and how many entries it holds.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun IndexTab(
    state: TopicBrowseState,
    onSort: (TopicIndexSort) -> Unit,
    onOpen: (QuranTopic, TopicTree) -> Unit,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val alphabetical = state.indexSort == TopicIndexSort.A_TO_Z
    val sections = state.indexSections
    // Where each letter's heading sits in the list, and where its first entry sits in the index.
    val headingIndex = remember(sections) {
        var position = 0
        sections.associate { section ->
            val at = position
            position += 1 + section.entries.size
            section.letter to at
        }
    }
    val entryOffset = remember(sections) {
        var before = 0
        sections.associate { section -> section.letter to before.also { before += section.entries.size } }
    }
    val current by remember(sections) {
        derivedStateOf {
            val first = listState.firstVisibleItemIndex
            sections.lastOrNull { (headingIndex[it.letter] ?: 0) <= first }?.letter
        }
    }
    // The current heading is pinned whenever the list is off its very top — including right
    // after a rail jump, which lands the heading exactly at the top edge, stuck.
    val pinned by remember(sections) {
        derivedStateOf {
            current.takeIf {
                listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
            }
        }
    }
    LaunchedEffect(state.indexSort) { listState.scrollToItem(0) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NimazFilterChip(
            selected = alphabetical,
            onClick = { onSort(TopicIndexSort.A_TO_Z) },
            label = stringResource(R.string.quran_topics_sort_az),
            showSelectedIcon = false,
        )
        NimazFilterChip(
            selected = !alphabetical,
            onClick = { onSort(TopicIndexSort.MOST_CITED) },
            label = stringResource(R.string.quran_topics_sort_most_cited),
            showSelectedIcon = false,
        )
        if (alphabetical) {
            val letter = current
            if (letter != null) {
                Text(
                    text = stringResource(
                        R.string.quran_topics_index_position,
                        letter.toString(),
                        formatCount((entryOffset[letter] ?: 0) + 1),
                        formatCount(state.index.size),
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = if (alphabetical) 40.dp else 16.dp,
                top = 0.dp,
                bottom = 24.dp,
            ),
        ) {
            if (alphabetical) {
                sections.forEach { section ->
                    stickyHeader(key = "letter-${section.letter}") {
                        LetterHeading(
                            letter = section.letter,
                            count = section.entries.size,
                            pinned = pinned == section.letter,
                        )
                    }
                    indexRows(section.entries, onOpen)
                }
            } else {
                indexRows(state.mostCited, onOpen)
                item(key = "most-cited-footer") {
                    Text(
                        text = stringResource(
                            R.string.quran_topics_most_cited_footer,
                            state.mostCited.size,
                            formatCount(state.index.size),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    )
                }
            }
        }

        if (alphabetical) {
            val counts = remember(sections) { sections.associate { it.letter to it.entries.size } }
            NimazIndexRail(
                letters = RAIL_LETTERS,
                available = counts.keys,
                current = current,
                onSelect = { letter ->
                    headingIndex[letter]?.let { scope.launch { listState.scrollToItem(it) } }
                },
                bubbleLabel = { letter -> counts[letter]?.let { entriesLabel(it) } },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(end = 6.dp, top = 4.dp, bottom = 16.dp),
            )
        }
    }
}

/**
 * A letter's heading: the letter in a well, its entry count, a rule. Solid while it is the one
 * pinned to the top, so the stuck heading reads as "you are here" rather than as a stray row.
 */
@Composable
private fun LetterHeading(letter: Char, count: Int, pinned: Boolean) {
    val well by animateColorAsState(
        if (pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
        label = "letter_well",
    )
    val ink by animateColorAsState(
        if (pinned) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
        label = "letter_ink",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 4.dp, top = 10.dp, bottom = 6.dp)
            .semantics(mergeDescendants = true) { heading() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(well),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = letter.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ink,
            )
        }
        Text(
            text = entriesLabel(count),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        NimazDivider(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun entriesLabel(count: Int): String =
    if (count == 1) stringResource(R.string.quran_topics_entry)
    else stringResource(R.string.quran_topics_entries, formatCount(count))

/** The rail always shows the whole alphabet, greying letters the index has nothing under. */
private val RAIL_LETTERS = ('A'..'Z').toList() + TopicBrowseState.OTHER_LETTER

private fun LazyListScope.indexRows(
    entries: List<TopicTally>,
    onOpen: (QuranTopic, TopicTree) -> Unit,
) {
    itemsIndexed(entries, key = { _, it -> "ix-${it.topic.id}" }) { i, entry ->
        GroupedRow(isFirst = i == 0, isLast = i == entries.lastIndex) {
            NimazMenuItem(
                title = entry.topic.name,
                subtitle = buildString {
                    append(verseCountLabel(entry.verseCount))
                    if (entry.childCount > 0) {
                        append(" · ")
                        append(
                            if (entry.childCount == 1) stringResource(R.string.quran_topics_sub_entry)
                            else stringResource(R.string.quran_topics_sub_entries, entry.childCount)
                        )
                    }
                },
                trailingIcon = null,
                trailing = if (entry.topic.hasArabicName) {
                    {
                        ArabicText(
                            text = entry.topic.arabicName,
                            size = ArabicTextSize.SMALL,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                } else null,
                onClick = { onOpen(entry.topic, TopicTree.INDEX) },
            )
        }
    }
}

/**
 * A row inside a run of rows drawn as one rounded group.
 *
 * Each row is its own lazy item — a letter can hold two hundred entries, and one item holding
 * all of them would compose every one — so the group's rounded ends are clipped onto the first
 * and last rows rather than drawn by a container around them.
 */
@Composable
private fun GroupedRow(isFirst: Boolean, isLast: Boolean, content: @Composable () -> Unit) {
    val corner = 16.dp
    Column(
        Modifier.clip(
            RoundedCornerShape(
                topStart = if (isFirst) corner else 0.dp,
                topEnd = if (isFirst) corner else 0.dp,
                bottomStart = if (isLast) corner else 0.dp,
                bottomEnd = if (isLast) corner else 0.dp,
            )
        )
    ) {
        content()
        if (!isLast) NimazMenuDivider(inset = false)
    }
}

/**
 * Matches, flat, each under the path it sits on.
 *
 * Flat and always opening: a result is an answer. The path is what stops sixty matched words
 * from being sixty words, and the hierarchy's name after it says which tab it would open in.
 */
@Composable
private fun SearchResults(
    state: TopicBrowseState,
    onOpen: (QuranTopic, TopicTree) -> Unit,
) {
    if (state.searchResults.isEmpty() && !state.isSearching) {
        NimazEmptyState(
            title = stringResource(R.string.quran_topics_no_results_title),
            message = stringResource(R.string.quran_topics_no_match, state.searchQuery),
            icon = Icons.Default.SearchOff,
            modifier = Modifier.padding(20.dp),
        )
        return
    }
    val treeLabels = TREES.associate { it.first to stringResource(it.second) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
    ) {
        itemsIndexed(state.searchResults, key = { _, it -> it.topic.id }) { i, hit ->
            GroupedRow(isFirst = i == 0, isLast = i == state.searchResults.lastIndex) {
                NimazMenuItem(
                    title = hit.topic.name,
                    subtitle = (hit.path.map { it.name } + treeLabels.getValue(hit.tree))
                        .joinToString(" › "),
                    trailingIcon = null,
                    trailing = {
                        Text(
                            text = formatCount(hit.verseCount),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                    onClick = { onOpen(hit.topic, hit.tree) },
                )
            }
        }
    }
}

/** One quiet line saying what the page below is, and what a query would search instead. */
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
internal val TREES = listOf(
    TopicTree.THEMATIC to R.string.quran_topics_tree_thematic,
    TopicTree.ONTOLOGY to R.string.quran_topics_tree_ontology,
    TopicTree.INDEX to R.string.quran_topics_tree_index,
)
