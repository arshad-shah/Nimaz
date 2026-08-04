package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.ui.graphics.vector.ImageVector
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.SurahOverviewGroup

/**
 * How a background section's *group* is drawn.
 *
 * The source's own [com.arshadshah.nimaz.domain.model.SurahOverviewSection.heading] is what the
 * reader sees as the section title — 65 spellings across 114 surahs — so it cannot label a
 * navigation control that has to mean the same thing on every surah. The group can: it is the
 * heading folded onto a handful of stable buckets at import, and it is what the index pills and
 * the section eyebrows are labelled and iconed from.
 *
 * Shared rather than private to one screen, because the background screen names the group twice
 * — once in its index, once above each section — and a second copy would be a second answer.
 */
val SurahOverviewGroup.icon: ImageVector
    get() = when (this) {
        SurahOverviewGroup.NAME -> Icons.Default.Badge
        SurahOverviewGroup.REVELATION -> Icons.Default.CalendarMonth
        SurahOverviewGroup.THEME -> Icons.Default.Lightbulb
        SurahOverviewGroup.BACKGROUND -> Icons.Default.History
        SurahOverviewGroup.NOTE -> Icons.Default.Info
        SurahOverviewGroup.OTHER -> Icons.AutoMirrored.Filled.MenuBook
    }

/**
 * The group's own label.
 *
 * Also the fallback title for the two rows in the corpus the source prints with no heading at
 * all — the shared note on surahs 113 and 114 — and for any future section whose heading is
 * blank.
 */
val SurahOverviewGroup.labelRes: Int
    get() = when (this) {
        SurahOverviewGroup.NAME -> R.string.surah_info_section_name
        SurahOverviewGroup.REVELATION -> R.string.surah_info_section_revelation
        SurahOverviewGroup.THEME -> R.string.surah_info_section_theme
        SurahOverviewGroup.BACKGROUND -> R.string.surah_info_section_background
        SurahOverviewGroup.NOTE -> R.string.surah_info_section_note
        SurahOverviewGroup.OTHER -> R.string.surah_info_section_other
    }
