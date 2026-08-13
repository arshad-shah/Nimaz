package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize

/**
 * What a surah is, raised where you are rather than pushed as a screen.
 *
 * The screen this replaces was one you backed out of immediately: you opened it from a list to
 * decide whether to read the surah, and the decision put you two taps from where you started.
 *
 * It keeps everything the screen did better than a sheet normally would. The **summary
 * paragraph** in the source's own words. **Counted** onward rows — "Passages · 3 across 7
 * verses" tells a reader whether to tap, where a bare "Passages" does not. And a fourth fact
 * tile the screen never had: **where it was revealed**, which is the first thing anyone asks
 * about a surah and was previously only a chip on a list row.
 *
 * One primary action, not two: "Read surah" carries the section accent and Listen is a text
 * button beside it. The screen put a teal button next to a yellow one, which is two accents in
 * one row and — since yellow stopped meaning "act" (spec §6.5) — one of them lying.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahInfoSheet(
    surah: Surah,
    summary: String?,
    sectionCount: Int,
    passageCount: Int,
    subjectCount: Int,
    startPage: Int,
    juzNumber: Int,
    onDismiss: () -> Unit,
    onReadSurah: () -> Unit,
    onListen: () -> Unit,
    onOpenBackground: () -> Unit,
    onOpenPassages: () -> Unit,
    onOpenSubjects: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NimazBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = surah.nameEnglish,
        // Only when it is a second name. The `surahs` table carries no translated meaning —
        // `name_english` and `name_transliteration` are both romanisations and are equal for
        // most surahs — so printing both put "At-Tawbah" over "At-Tawbah". Where they do
        // differ the second one is worth showing; where they do not, saying it twice is not
        // extra information, and inventing an English meaning the data does not hold would be
        // worse than saying nothing.
        subtitle = surah.nameTransliteration
            .takeIf { it.isNotBlank() && !it.equals(surah.nameEnglish, ignoreCase = true) },
        icon = Icons.AutoMirrored.Filled.MenuBook,
        // The Arabic name is not a badge. A chip sets it in the UI's Latin face at label size,
        // where the diacritics collide and the ligatures break; it belongs in the Arabic face,
        // at Arabic size, and it gets that at the top of the body.
        onClose = onDismiss,
        footer = {
            NimazSheetFooterButtons(
                primaryText = stringResource(R.string.surah_info_read_surah),
                onPrimary = onReadSurah,
                secondaryText = stringResource(R.string.listen),
                onSecondary = onListen,
            )
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            surah.nameArabic.takeIf { it.isNotBlank() }?.let {
                ArabicText(
                    text = it,
                    size = ArabicTextSize.LARGE,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            SurahMetaStrip(
                stats = listOf(
                    SurahMetaStat(
                        icon = Icons.Default.FormatListNumbered,
                        label = stringResource(R.string.quran_verses_label),
                        value = surah.ayahCount.toString(),
                    ),
                    SurahMetaStat(
                        icon = Icons.Default.Layers,
                        label = stringResource(R.string.quran_juz_label),
                        value = juzNumber.toString(),
                    ),
                    SurahMetaStat(
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        label = stringResource(R.string.quran_page_label),
                        value = (if (startPage > 0) startPage else surah.startPage).toString(),
                    ),
                    SurahMetaStat(
                        icon = Icons.Default.Mosque,
                        label = stringResource(R.string.surah_info_revealed_in),
                        value = if (surah.revelationType == RevelationType.MECCAN) {
                            stringResource(R.string.quran_home_makkah)
                        } else {
                            stringResource(R.string.quran_home_madinah)
                        },
                    ),
                )
            )

            summary?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Each row is drawn only where there is something behind it. An install whose
            // artifact predates the thematic layer simply has fewer rows — that gap is normal,
            // and an inert row would only advertise something the reader cannot open.
            if (sectionCount > 0 || passageCount > 0 || subjectCount > 0) {
                NimazMenuGroup {
                    if (sectionCount > 0) {
                        NimazMenuItem(
                            title = stringResource(R.string.surah_info_background),
                            subtitle = stringResource(
                                R.string.surah_info_background_subtitle,
                                sectionCount
                            ),
                            icon = Icons.AutoMirrored.Filled.Article,
                            onClick = onOpenBackground
                        )
                    }
                    if (passageCount > 0) {
                        NimazMenuItem(
                            title = stringResource(R.string.surah_info_passages),
                            subtitle = pluralStringResource(
                                R.plurals.surah_info_passages_row_subtitle,
                                passageCount,
                                passageCount,
                                surah.ayahCount
                            ),
                            icon = Icons.AutoMirrored.Filled.List,
                            onClick = onOpenPassages
                        )
                    }
                    if (subjectCount > 0) {
                        NimazMenuItem(
                            title = stringResource(R.string.surah_info_subjects),
                            subtitle = stringResource(
                                R.string.surah_info_subjects_subtitle,
                                subjectCount
                            ),
                            icon = Icons.Default.AccountTree,
                            onClick = onOpenSubjects
                        )
                    }
                }
            }
        }
    }
}
