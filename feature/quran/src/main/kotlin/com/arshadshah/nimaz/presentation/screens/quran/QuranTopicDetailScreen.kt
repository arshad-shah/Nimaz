package com.arshadshah.nimaz.presentation.screens.quran

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.core.common.ThematicLink
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.QuranTopic
import com.arshadshah.nimaz.domain.model.TopicTree
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazChip
import com.arshadshah.nimaz.presentation.components.atoms.NimazChipVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.components.molecules.CitationRow
import com.arshadshah.nimaz.presentation.components.molecules.NimazAccordion
import com.arshadshah.nimaz.presentation.components.molecules.NimazEmptyState
import com.arshadshah.nimaz.presentation.components.molecules.NimazLoadingState
import com.arshadshah.nimaz.presentation.components.molecules.ThematicText
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.components.organisms.SubjectHero
import com.arshadshah.nimaz.presentation.components.organisms.SubtopicCard
import com.arshadshah.nimaz.presentation.components.organisms.SurahDistribution
import com.arshadshah.nimaz.presentation.components.organisms.TopicIcons
import com.arshadshah.nimaz.presentation.components.organisms.formatCount
import com.arshadshah.nimaz.presentation.components.organisms.verseCountLabel
import com.arshadshah.nimaz.presentation.viewmodel.quran.CitationGroup
import com.arshadshah.nimaz.presentation.viewmodel.quran.QuranTopicsEvent
import com.arshadshah.nimaz.presentation.viewmodel.quran.QuranTopicsViewModel
import com.arshadshah.nimaz.presentation.viewmodel.quran.TopicDetailState

/**
 * One subject: how much of the Qur'an speaks to it, where it sits, and every verse.
 *
 * Read top to bottom it answers the questions in the order a reader has them. The **hero** says
 * how much — verses, surahs, subtopics — counted through the whole branch, because a branch's
 * own citations are usually none and the old screen opened "Prophets" on "0 verses". The **path**
 * says where it sits, each step a way back up. The **description** is the editors' note, clamped
 * so it does not push the verses off the first screen. **Subtopics** are cards you swipe through,
 * each with its own count. **Where it appears** shows which surahs carry the subject — Moses is
 * Ta-Ha and Al-A'raf before he is anywhere else. Then the **verses**, grouped under their surahs
 * and folded, the first surah open, so 431 verses are a page you can scan rather than a scroll.
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
            NimazBackTopAppBar(
                title = detail?.topic?.name ?: stringResource(R.string.quran_topics_title),
                subtitle = detail?.let { stringResource(treeLabel(it.tree)) },
                onBackClick = onNavigateBack,
            )
        },
    ) { padding ->
        when {
            state.isLoading && detail == null -> NimazLoadingState(modifier = Modifier.padding(padding))

            detail == null -> NimazEmptyState(
                title = stringResource(R.string.quran_topics_no_results_title),
                message = stringResource(R.string.quran_topic_not_found),
                icon = Icons.Default.Category,
                modifier = Modifier
                    .padding(padding)
                    .padding(20.dp),
            )

            else -> SubjectBody(
                state = state,
                contentPadding = padding,
                onOpenAyah = onOpenAyah,
                onOpenTopic = { id -> onOpenTopic(id, detail.tree) },
            )
        }
    }
}

@Composable
private fun SubjectBody(
    state: TopicDetailState,
    contentPadding: PaddingValues,
    onOpenAyah: (surah: Int, ayah: Int) -> Unit,
    onOpenTopic: (topicId: Int) -> Unit,
) {
    val detail = state.detail ?: return
    val topic = detail.topic
    // Which surah groups are open, and which show every verse. Saved, so coming back from the
    // reader lands on the list as it was left rather than refolded.
    var toggled by rememberSaveable(topic.id) { mutableStateOf(emptyList<Int>()) }
    var showingAll by rememberSaveable(topic.id) { mutableStateOf(emptyList<Int>()) }
    val firstSurah = state.citationGroups.firstOrNull()?.surahNumber
    fun isOpen(group: CitationGroup) = (group.surahNumber == firstSurah) != (group.surahNumber in toggled)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 4.dp,
            bottom = contentPadding.calculateBottomPadding() + 32.dp,
        ),
    ) {
        item(key = "hero") {
            SubjectHero(
                topic = topic,
                icon = TopicIcons.forRoot(detail.breadcrumb.firstOrNull()?.id ?: topic.id, detail.tree),
                verseCount = detail.citations.size,
                surahCount = state.citationGroups.size,
                subtopicCount = detail.children.size,
            )
        }

        // How much of this subject is in the surah the reader came from. Beside the totals
        // rather than instead of them: the point of the pair is the ratio — 12 of 153 is a
        // passing mention, 12 of 14 is not.
        state.surahContext?.let { context ->
            item(key = "surah-context") {
                NimazBadge(
                    text = stringResource(R.string.quran_topic_in_surah, context.verseCount, context.surahName),
                    tone = NimazTone.PROMINENT,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }

        if (detail.breadcrumb.isNotEmpty()) {
            item(key = "path") {
                SubjectPath(path = detail.breadcrumb, onOpenTopic = onOpenTopic)
            }
        }

        if (topic.hasDescription) {
            item(key = "description") {
                Description(
                    html = topic.description,
                    onLink = { link ->
                        when (link) {
                            is ThematicLink.Topic -> onOpenTopic(link.id)
                            is ThematicLink.Verses -> onOpenAyah(link.surah, link.from ?: 1)
                        }
                    },
                )
            }
        }

        if (detail.children.isNotEmpty()) {
            item(key = "subtopics") {
                // Counted subtopics once the catalogue lands; the bare children until then, in
                // the order the corpus gives them, so the row is never empty while it counts.
                val subtopics = state.subtopics.ifEmpty { null }
                Section(
                    title = stringResource(R.string.quran_topic_subtopics),
                    trailing = formatCount(detail.children.size),
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (subtopics != null) {
                        items(subtopics, key = { it.topic.id }) { sub ->
                            SubtopicCard(sub.topic.name, sub.verseCount, onClick = { onOpenTopic(sub.topic.id) })
                        }
                    } else {
                        items(detail.children, key = { it.id }) { child ->
                            SubtopicCard(child.name, null, onClick = { onOpenTopic(child.id) })
                        }
                    }
                }
            }
        }

        if (detail.related.isNotEmpty()) {
            item(key = "related") {
                Section(title = stringResource(R.string.quran_topic_related))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(detail.related, key = { it.id }) { related ->
                        NimazChip(
                            text = related.name,
                            variant = NimazChipVariant.SUGGESTION,
                            leadingIcon = Icons.Default.Link,
                            onClick = { onOpenTopic(related.id) },
                        )
                    }
                }
            }
        }

        if (state.topSurahs.isNotEmpty()) {
            item(key = "where") {
                Section(
                    title = stringResource(R.string.quran_topic_where_it_appears),
                    trailing = stringResource(
                        R.string.quran_topic_top_of,
                        state.topSurahs.size,
                        state.citationGroups.size,
                    ),
                )
                SurahDistribution(groups = state.topSurahs)
            }
        }

        if (state.citationGroups.isEmpty()) {
            item(key = "no-verses") {
                Text(
                    text = stringResource(R.string.quran_topic_no_verses),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
        } else {
            item(key = "verses-title") {
                Section(
                    title = stringResource(R.string.quran_topic_verses_section),
                    trailing = stringResource(
                        R.string.quran_topic_verses_across,
                        detail.citations.size,
                        state.citationGroups.size,
                    ),
                )
            }
            items(state.citationGroups, key = { "surah-${it.surahNumber}" }) { group ->
                val open = isOpen(group)
                val all = group.surahNumber in showingAll
                SurahVerses(
                    group = group,
                    expanded = open,
                    showAll = all,
                    previews = state.previews,
                    onExpandedChange = {
                        toggled = if (group.surahNumber in toggled) toggled - group.surahNumber
                        else toggled + group.surahNumber
                    },
                    onShowAll = { showingAll = showingAll + group.surahNumber },
                    onOpenAyah = onOpenAyah,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
        }
    }
}

/** "In Stories › Prophets" — every step a way back up the hierarchy. */
@Composable
private fun SubjectPath(path: List<QuranTopic>, onOpenTopic: (Int) -> Unit) {
    val prefix = stringResource(R.string.quran_topic_path_prefix)
    val linkStyle = TextLinkStyles(
        style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold),
    )
    val text = buildAnnotatedString {
        append(prefix)
        append(" ")
        path.forEachIndexed { i, step ->
            if (i > 0) append(" › ")
            withLink(LinkAnnotation.Clickable(tag = "topic-${step.id}", styles = linkStyle) { onOpenTopic(step.id) }) {
                append(step.name)
            }
        }
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 12.dp),
    )
}

/**
 * The editors' note on the subject, with a gold rule down its edge — the mushaf's marginalia,
 * not the text. Clamped past a few lines, because a long note would push every verse below the
 * first screen, and the verses are what the reader came for.
 */
@Composable
private fun Description(html: String, onLink: (ThematicLink) -> Unit) {
    var expanded by rememberSaveable(html) { mutableStateOf(false) }
    val clampable = html.length > DESCRIPTION_CLAMP_CHARS
    val gold = MaterialTheme.colorScheme.secondary
    Column(Modifier.padding(top = 12.dp)) {
        NimazCard(
            style = NimazCardStyle.OUTLINED,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                Modifier
                    .drawBehind {
                        drawLine(gold, Offset(0f, 0f), Offset(0f, size.height), strokeWidth = 3.dp.toPx())
                    }
                    .animateContentSize()
                    .then(if (clampable && !expanded) Modifier.heightIn(max = 150.dp).clipToBounds() else Modifier)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                ThematicText(
                    html = html,
                    style = MaterialTheme.typography.bodyMedium,
                    onLinkClick = onLink,
                )
            }
        }
        if (clampable) {
            NimazButton(
                text = stringResource(if (expanded) R.string.quran_topic_read_less else R.string.quran_topic_read_more),
                onClick = { expanded = !expanded },
                variant = NimazButtonVariant.TEXT,
                size = NimazButtonSize.SMALL,
            )
        }
    }
}

/**
 * One surah's share of the subject, folded. Three verses show when it opens and "Show all" gives
 * the rest — a surah can hold ninety of them, and ninety rows is not a group any more.
 */
@Composable
private fun SurahVerses(
    group: CitationGroup,
    expanded: Boolean,
    showAll: Boolean,
    previews: Map<Int, String>,
    onExpandedChange: (Boolean) -> Unit,
    onShowAll: () -> Unit,
    onOpenAyah: (surah: Int, ayah: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    NimazAccordion(
        title = "${group.surahNumber}. ${group.surahName}",
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier,
        subtitle = verseCountLabel(group.citations.size),
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (group.isFromSurah) {
                    NimazBadge(text = stringResource(R.string.quran_topic_surah_you_came_from), tone = NimazTone.PROMINENT)
                }
                if (group.surahArabicName.isNotBlank()) {
                    ArabicText(text = group.surahArabicName, size = ArabicTextSize.SMALL, maxLines = 1)
                }
            }
        },
    ) {
        val shown = if (showAll) group.citations else group.citations.take(VERSES_PER_GROUP)
        shown.forEach { citation ->
            CitationRow(
                reference = citation.reference,
                preview = previews[citation.ayahId],
                onClick = { onOpenAyah(citation.surahNumber, citation.ayahNumber) },
            )
        }
        if (!showAll && group.citations.size > VERSES_PER_GROUP) {
            NimazButton(
                text = stringResource(R.string.quran_topic_show_all, group.citations.size),
                onClick = onShowAll,
                variant = NimazButtonVariant.TEXT,
                size = NimazButtonSize.SMALL,
            )
        }
    }
}

@Composable
private fun Section(title: String, trailing: String? = null) {
    NimazSectionHeader(
        title = title,
        trailingText = trailing,
        modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 20.dp, bottom = 4.dp),
    )
}

private fun treeLabel(tree: TopicTree): Int = TREES.first { it.first == tree }.second

private const val VERSES_PER_GROUP = 3
private const val DESCRIPTION_CLAMP_CHARS = 420
