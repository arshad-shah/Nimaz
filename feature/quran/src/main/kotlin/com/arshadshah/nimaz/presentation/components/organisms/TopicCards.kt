package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Brightness3
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.EmojiNature
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.EventSeat
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Handyman
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.QuranTopic
import com.arshadshah.nimaz.domain.model.TopicTree
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazAssistChip
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardDefaults
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconWell
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconWellSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazProgressTrack
import com.arshadshah.nimaz.presentation.components.atoms.NimazProgressSize
import com.arshadshah.nimaz.presentation.viewmodel.quran.CitationGroup
import com.arshadshah.nimaz.presentation.viewmodel.quran.TopicTally
import com.arshadshah.nimaz.presentation.viewmodel.quran.TopicThemeCard
import java.text.NumberFormat

/**
 * The pieces the Topics browser and subject screen are built from.
 *
 * Each hierarchy gets the shape that suits it rather than one indented list for all three: the
 * curated outline as chapter cards, the ontology as a grid of kinds, and — on the subject
 * screen — a hero that says how much of the Qur'an sits here before anything else does.
 */
object TopicIcons {

    /**
     * An icon for a subject, taken from the root of the hierarchy it sits in.
     *
     * Keyed on the roots' **ids**, which are content (arshad-shah/nimaz-data), because the names
     * are content too and are the less stable of the two — "Doctraine" is a typo waiting to be
     * fixed. An id this map does not know, in the index or in a future corpus, gets the plain
     * tag rather than nothing.
     */
    fun forRoot(rootId: Int, tree: TopicTree): ImageVector = when (tree) {
        TopicTree.THEMATIC -> THEMES[rootId]
        TopicTree.ONTOLOGY -> KINDS[rootId]
        TopicTree.INDEX -> null
    } ?: Icons.Outlined.Sell

    private val THEMES = mapOf(
        1882 to Icons.Outlined.AutoAwesome, // Doctrine
        1883 to Icons.AutoMirrored.Outlined.MenuBook, // Stories
        1884 to Icons.Outlined.Visibility, // The Unseen
    )

    private val KINDS = mapOf(
        1 to Icons.Outlined.Brightness3, // Allah
        73 to Icons.Outlined.EventSeat, // Allah's Throne
        159 to Icons.Outlined.Translate, // Language
        246 to Icons.Outlined.Handyman, // Artifact
        249 to Icons.Outlined.WbSunny, // Astronomical Body
        250 to Icons.Outlined.Event, // Event
        255 to Icons.Outlined.Block, // False Deity
        256 to Icons.AutoMirrored.Outlined.MenuBook, // Holy Book
        257 to Icons.Outlined.EmojiNature, // Living Creation
        273 to Icons.Outlined.Place, // Location
        279 to Icons.Outlined.Face, // Physical Attribute
        280 to Icons.Outlined.WaterDrop, // Physical Substance
        283 to Icons.Outlined.AccountBalance, // Religion
        284 to Icons.Outlined.Cloud, // Weather Phenomena
    )
}

/** "1,041" — counts on these cards run to the thousands, and they read as figures. */
internal fun formatCount(n: Int): String = NumberFormat.getIntegerInstance().format(n)

@Composable
internal fun verseCountLabel(count: Int): String =
    if (count == 1) stringResource(R.string.quran_topics_verse)
    else stringResource(R.string.quran_topics_verse_count, formatCount(count))

/**
 * The shades a Themes card keys its branches with: the brand colour stepped towards the card.
 *
 * Derived from the theme rather than a palette, so the bar and its chips follow light, dark and
 * any future brand colour without a second set of hex values to keep in step.
 */
@Composable
private fun branchShades(): List<Color> {
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    return listOf(1f, 0.72f, 0.5f, 0.34f, 0.22f, 0.14f).map { primary.copy(alpha = it).compositeOver(surface) }
}

/**
 * One root of the curated outline: its size, its branches, and how the size divides among them.
 *
 * The bar is the point of the card. "Doctrine, 2,412 verses" is a number; the bar shows that a
 * third of it is Relationship with Allah, which is the thing a reader deciding where to start
 * actually wants to know. Its colours key into the chips beneath, which are the ways in.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThemeChapterCard(
    card: TopicThemeCard,
    onOpenTopic: (topicId: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shades = branchShades()
    NimazCard(
        style = NimazCardStyle.OUTLINED,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            // The header opens the root itself; the chips open its branches. Two targets, so the
            // card is not one — a card-wide tap would swallow the chips.
            NimazCard(
                style = NimazCardStyle.FILLED,
                colors = NimazCardDefaults.colors(
                    container = Color.Transparent,
                    content = MaterialTheme.colorScheme.onSurface,
                ),
                elevation = 0.dp,
                shape = RoundedCornerShape(14.dp),
                onClick = { onOpenTopic(card.root.topic.id) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NimazIconWell(
                        icon = TopicIcons.forRoot(card.root.topic.id, TopicTree.THEMATIC),
                        color = MaterialTheme.colorScheme.primary,
                        size = NimazIconWellSize.LARGE,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = card.root.topic.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(
                            text = verseCountLabel(card.root.verseCount) + " · " +
                                stringResource(R.string.quran_topics_branches, card.branches.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (card.branches.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 8.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    card.branches.forEachIndexed { i, branch ->
                        Box(
                            Modifier
                                .weight(branch.verseCount.coerceAtLeast(1).toFloat())
                                .height(6.dp)
                                .background(shades[i % shades.size]),
                        )
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    card.branches.forEachIndexed { i, branch ->
                        NimazAssistChip(
                            onClick = { onOpenTopic(branch.topic.id) },
                            label = branch.topic.name,
                            trailingText = formatCount(branch.verseCount),
                            swatch = shades[i % shades.size],
                            shape = RoundedCornerShape(10.dp),
                        )
                    }
                }
            }
        }
    }
}

/** One kind of thing in the ontology — a whole-tile tap target, two to a row. */
@Composable
fun KindTile(
    tally: TopicTally,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NimazCard(
        style = NimazCardStyle.OUTLINED,
        shape = RoundedCornerShape(18.dp),
        onClick = onClick,
        modifier = modifier.heightIn(min = 124.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // The Arabic name sits beside the icon rather than under the English one: only some
            // kinds have one, and a line that comes and goes made the two tiles of a row differ
            // in height.
            Row(verticalAlignment = Alignment.CenterVertically) {
                NimazIconWell(
                    icon = TopicIcons.forRoot(tally.topic.id, TopicTree.ONTOLOGY),
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.weight(1f))
                if (tally.topic.hasArabicName) {
                    ArabicText(
                        text = tally.topic.arabicName,
                        size = ArabicTextSize.SMALL,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            Text(
                text = tally.topic.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = verseCountLabel(tally.verseCount),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/**
 * The top of a subject: what it is and how much of the Qur'an speaks to it.
 *
 * Three figures, because each answers a different question — how much (verses), how widely
 * (surahs), how deep (subtopics). The old screen led with a single "0 verses" badge, which for a
 * branch was the count of its own citations and so, for most branches, zero.
 */
@Composable
fun SubjectHero(
    topic: QuranTopic,
    icon: ImageVector,
    verseCount: Int,
    surahCount: Int,
    subtopicCount: Int,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val ink = MaterialTheme.colorScheme.onPrimary
    NimazCard(
        style = NimazCardStyle.GRADIENT,
        gradient = listOf(primary, Color.Black.copy(alpha = 0.22f).compositeOver(primary)),
        colors = NimazCardDefaults.colors(container = Color.Transparent, content = ink),
        shape = RoundedCornerShape(22.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            NimazIconWell(icon = icon, color = ink)
            Text(
                text = topic.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = ink,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .semantics { heading() },
            )
            if (topic.hasArabicName) {
                ArabicText(
                    text = topic.arabicName,
                    size = ArabicTextSize.SMALL,
                    color = ink.copy(alpha = 0.88f),
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                HeroStat(formatCount(verseCount), stringResource(R.string.quran_topic_stat_verses), ink, Modifier.weight(1f))
                HeroStat(formatCount(surahCount), stringResource(R.string.quran_topic_stat_surahs), ink, Modifier.weight(1f))
                HeroStat(formatCount(subtopicCount), stringResource(R.string.quran_topic_stat_subtopics), ink, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HeroStat(value: String, label: String, ink: Color, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ink.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = ink)
        Text(label, style = MaterialTheme.typography.labelSmall, color = ink.copy(alpha = 0.85f))
    }
}

/** A subtopic, with its whole subtree counted — one card in a row you swipe through. */
@Composable
fun SubtopicCard(
    name: String,
    verseCount: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NimazCard(
        style = NimazCardStyle.OUTLINED,
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
        modifier = modifier.width(150.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (verseCount != null) {
                Text(
                    text = verseCountLabel(verseCount),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/**
 * Where a subject is concentrated: its busiest surahs, as bars against the busiest.
 *
 * Ta-Ha 90 and Al-A'raf 62 of Moses' 431 verses says something the verse list, in mushaf order,
 * never can — that his story is told at length twice and in passing everywhere else.
 */
@Composable
fun SurahDistribution(
    groups: List<CitationGroup>,
    modifier: Modifier = Modifier,
) {
    val max = groups.maxOfOrNull { it.citations.size }?.coerceAtLeast(1) ?: return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        groups.forEach { group ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = group.surahName,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(96.dp),
                )
                NimazProgressTrack(
                    progress = group.citations.size / max.toFloat(),
                    size = NimazProgressSize.MEDIUM,
                    modifier = Modifier.weight(1f),
                    contentDescription = "${group.surahName}: ${group.citations.size}",
                )
                Text(
                    text = formatCount(group.citations.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .width(28.dp),
                )
            }
        }
    }
}
