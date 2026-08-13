package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeEmphasis
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazProgressTrack
import com.arshadshah.nimaz.presentation.components.atoms.NimazProgressSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.components.atoms.ShamsaMedallion
import com.arshadshah.nimaz.presentation.theme.NimazTheme

private val SurahNumberSlotWidth = 40.dp
private val SurahNumberSlotSpacing = 10.dp

/** Between the parts of the meta line — punctuation, not prose, so it is not translated. */
private const val MetaSeparator = " · "

/**
 * One surah, at the section's shared row density (spec §6.6): a medallion, the name, and one
 * meta line.
 *
 * It used to stand ~200 px tall — two rows of chips under the name, carrying the page *range*,
 * the juz, the rukūʿ count and the verse count — so three surahs filled a phone screen. The
 * range answered "where does this sit in the mushaf" with two numbers where one does, the juz
 * is now the section header this row sits under, and the rukūʿ count is reference data no other
 * redesigned surface shows. What is left is what a reader browsing for a surah actually reads:
 * where it was revealed, how long it is, and where it starts.
 *
 * @param startPage the page the surah opens on in the *active* Mushaf edition — resolved by the
 *   caller, because `Surah.startPage` is the Madani column and names the wrong page under any
 *   other edition (#325). 0 omits it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SurahListItem(
    surah: Surah,
    onClick: () -> Unit,
    onInfoClick: () -> Unit = {},
    showInfo: Boolean = true,
    khatamReadCount: Int = 0,
    khatamTotalAyahs: Int = 0,
    isKhatamActive: Boolean = false,
    isSelected: Boolean = false,
    startPage: Int = 0,
    modifier: Modifier = Modifier
) {
    val isComplete = isKhatamActive && khatamTotalAyahs > 0 && khatamReadCount == khatamTotalAyahs
    val isMeccan = surah.revelationType == RevelationType.MECCAN

    NimazCard(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isSelected) Modifier.border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(14.dp)
                ) else Modifier
            ),
        style = NimazCardStyle.OUTLINED,
        selected = isSelected,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 64.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(SurahNumberSlotWidth),
                    contentAlignment = Alignment.Center
                ) {
                    if (isComplete) {
                        NimazIcon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = stringResource(R.string.quran_home_completed),
                            variant = NimazIconVariant.PRIMARY,
                            iconSize = 30.dp
                        )
                    } else {
                        ShamsaMedallion(
                            number = surah.number,
                            size = SurahNumberSlotWidth
                        )
                    }
                }

                Spacer(modifier = Modifier.width(SurahNumberSlotSpacing))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = surah.nameEnglish,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Revelation stays a chip — it is a category, not a measurement, and
                        // the pair is deliberate: gold for the sanctuary at Makkah, green for
                        // Madinah. Teal is not available to either any more; it means
                        // "selected" everywhere in this section now (spec §6.5).
                        NimazBadge(
                            text = if (isMeccan) stringResource(R.string.quran_home_makkah)
                            else stringResource(R.string.quran_home_madinah),
                            size = NimazBadgeSize.SMALL,
                            tone = if (isMeccan) NimazTone.WARNING else NimazTone.SUCCESS,
                            emphasis = NimazBadgeEmphasis.SOFT
                        )
                        val verses = pluralStringResource(
                            R.plurals.quran_home_verses_count,
                            surah.ayahCount,
                            surah.ayahCount
                        )
                        val page = if (startPage > 0) {
                            stringResource(R.string.quran_browse_page_start, startPage)
                        } else {
                            null
                        }
                        Text(
                            text = listOfNotNull(verses, page).joinToString(MetaSeparator),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                ArabicText(
                    text = surah.nameArabic,
                    size = ArabicTextSize.SMALL,
                    color = MaterialTheme.colorScheme.primary
                )

                if (showInfo) {
                    IconButton(
                        onClick = onInfoClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        NimazIcon(
                            imageVector = Icons.Default.Info,
                            contentDescription = stringResource(R.string.quran_home_surah_info),
                            variant = NimazIconVariant.MUTED,
                            iconSize = 18.dp
                        )
                    }
                }
            }

            if (isKhatamActive && khatamTotalAyahs > 0 && khatamReadCount > 0) {
                NimazProgressTrack(
                    progress = khatamReadCount.toFloat() / khatamTotalAyahs,
                    tone = if (isComplete) NimazTone.ACCENT else NimazTone.SUCCESS,
                    size = NimazProgressSize.THIN,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 8.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SurahListItemPreview() {
    NimazTheme {
        SurahListItem(
            surah = Surah(
                number = 1,
                nameArabic = "الفاتحة",
                nameEnglish = "Al-Fatihah",
                nameTransliteration = "The Opening",
                revelationType = RevelationType.MECCAN,
                ayahCount = 7,
                orderInMushaf = 5,
                startPage = 1
            ),
            onClick = {},
            onInfoClick = {},
            startPage = 1,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SurahListItemLongNamePreview() {
    NimazTheme {
        SurahListItem(
            surah = Surah(
                number = 58,
                nameArabic = "المجادلة",
                nameEnglish = "Al-Mujadilah",
                nameTransliteration = "The Pleading Woman",
                revelationType = RevelationType.MEDINAN,
                ayahCount = 22,
                orderInMushaf = 105,
                startPage = 542
            ),
            onClick = {},
            onInfoClick = {},
            startPage = 542,
        )
    }
}
