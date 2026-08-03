package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.util.ThematicLink
import com.arshadshah.nimaz.domain.model.AyahTheme
import com.arshadshah.nimaz.domain.model.SurahOverview
import com.arshadshah.nimaz.domain.model.SurahOverviewGroup
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionTitle
import com.arshadshah.nimaz.presentation.components.molecules.NimazAccordion
import com.arshadshah.nimaz.presentation.components.molecules.ThematicText
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import androidx.compose.ui.tooling.preview.Preview

/**
 * A surah's long-form background, as the source's own sections.
 *
 * Collapsed by default, and that is not a stylistic choice: the longest of these runs to 47 KB
 * of prose for one surah, and rendering every section expanded would measure most of a book
 * chapter to draw a screen whose first fold is three stat cards. The first section opens so the
 * block never reads as inert.
 *
 * The `quran:` links inside the prose are the reason this is worth building at all — the source
 * says "see 2:153-251" and here that is a tap into the reader, not a reference to copy out.
 */
@Composable
fun SurahBackgroundSections(
    overview: SurahOverview?,
    onOpenAyah: (surah: Int, ayah: Int) -> Unit,
    onOpenTopic: (topicId: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (overview == null || overview.sections.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NimazSectionTitle(
            text = stringResource(R.string.surah_info_background),
            uppercase = false,
        )
        overview.sections.forEachIndexed { index, section ->
            NimazAccordion(
                title = section.heading.ifBlank { stringResource(section.group.labelRes) },
                leadingIcon = section.group.icon,
                initiallyExpanded = index == 0,
            ) {
                ThematicText(
                    html = section.body,
                    onLinkClick = { link ->
                        when (link) {
                            is ThematicLink.Verses -> onOpenAyah(link.surah, link.from ?: 1)
                            is ThematicLink.Topic -> onOpenTopic(link.id)
                        }
                    },
                )
            }
        }
    }
}

/**
 * The surah's passage outline — the mushaf's own division of it into subjects.
 *
 * Al-Baqarah has 282 of these, so this is a plain `Column` inside the screen's scroll rather
 * than a nested `LazyColumn`: a lazy list inside a scrolling parent has unbounded height and
 * Compose throws on it. The rows are cheap (two `Text`s), and the screen already carries the
 * overview prose above them.
 */
@Composable
fun SurahPassageOutline(
    passages: List<AyahTheme>,
    onOpenAyah: (ayah: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (passages.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NimazSectionTitle(
            text = stringResource(R.string.surah_info_passages),
            uppercase = false,
        )
        Text(
            text = stringResource(R.string.surah_info_passages_subtitle, passages.size),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        passages.forEach { passage ->
            PassageRow(passage = passage, onClick = { onOpenAyah(passage.ayahFrom) })
        }
    }
}

@Composable
private fun PassageRow(passage: AyahTheme, onClick: () -> Unit) {
    NimazCard(
        style = NimazCardStyle.FILLED,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.width(72.dp)) {
                Text(
                    text = passage.reference.substringAfter(':'),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = if (passage.isSingleAyah) {
                        stringResource(R.string.surah_info_passage_verse)
                    } else {
                        stringResource(R.string.surah_info_passage_verses, passage.ayahCount)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = passage.theme,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * The icon and fallback label for a section group.
 *
 * The fallback matters for exactly two rows in the corpus — the shared note on surahs 113 and
 * 114, which the source prints with no heading at all — and for any future section whose
 * heading is blank.
 */
private val SurahOverviewGroup.icon: ImageVector
    get() = when (this) {
        SurahOverviewGroup.NAME -> Icons.Default.Badge
        SurahOverviewGroup.REVELATION -> Icons.Default.CalendarMonth
        SurahOverviewGroup.THEME -> Icons.Default.Lightbulb
        SurahOverviewGroup.BACKGROUND -> Icons.Default.History
        SurahOverviewGroup.NOTE -> Icons.Default.Info
        SurahOverviewGroup.OTHER -> Icons.AutoMirrored.Filled.MenuBook
    }

private val SurahOverviewGroup.labelRes: Int
    get() = when (this) {
        SurahOverviewGroup.NAME -> R.string.surah_info_section_name
        SurahOverviewGroup.REVELATION -> R.string.surah_info_section_revelation
        SurahOverviewGroup.THEME -> R.string.surah_info_section_theme
        SurahOverviewGroup.BACKGROUND -> R.string.surah_info_section_background
        SurahOverviewGroup.NOTE -> R.string.surah_info_section_note
        SurahOverviewGroup.OTHER -> R.string.surah_info_about
    }

@Preview(showBackground = true)
@Composable
private fun SurahPassageOutlinePreview() {
    NimazTheme {
        SurahPassageOutline(
            passages = listOf(
                AyahTheme(2, 1, 5, "The Qur'an is guidance for the God-fearing", 5),
                AyahTheme(2, 8, 16, "Hypocrites and the consequences of hypocrisy", 9),
            ),
            onOpenAyah = {},
        )
    }
}

/**
 * The heading the reader prints where a new passage opens.
 *
 * Deliberately quiet — a label and a line of prose, no card and no chevron. It sits between two
 * verses of scripture, and anything with a container would read as content of the same weight
 * as what it is annotating.
 */
@Composable
fun PassageHeading(passage: AyahTheme, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = passage.reference,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = passage.theme,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
