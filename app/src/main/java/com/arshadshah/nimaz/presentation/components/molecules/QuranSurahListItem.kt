package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize

private val SurahNumberSlotWidth = 40.dp
private val SurahNumberSlotSpacing = 12.dp

private fun getJuzForPage(page: Int): Int {
    val juzStartPages = listOf(
        1, 22, 42, 62, 82, 102, 121, 142, 162, 182,
        201, 222, 242, 262, 282, 302, 322, 342, 362, 382,
        402, 422, 442, 462, 482, 502, 522, 542, 562, 582
    )
    for (i in juzStartPages.indices.reversed()) {
        if (page >= juzStartPages[i]) return i + 1
    }
    return 1
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SurahListItem(
    surah: Surah,
    onClick: () -> Unit,
    onInfoClick: () -> Unit = {},
    khatamReadCount: Int = 0,
    khatamTotalAyahs: Int = 0,
    isKhatamActive: Boolean = false,
    isSelected: Boolean = false,
    startPage: Int = 0,
    endPage: Int = 0,
    modifier: Modifier = Modifier
) {
    val isComplete = isKhatamActive && khatamTotalAyahs > 0 && khatamReadCount == khatamTotalAyahs
    Card(
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
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            // Top row: number + English name + Arabic name + info button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Surah number indicator
                Box(
                    modifier = Modifier.size(SurahNumberSlotWidth),
                    contentAlignment = Alignment.Center
                ) {
                    if (isComplete) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = stringResource(R.string.quran_home_completed),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .rotate(45f)
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                        Text(
                            text = surah.number.toString(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(SurahNumberSlotSpacing))

                // English name — takes remaining space, truncates if needed
                Text(
                    text = surah.nameEnglish,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Arabic name — intrinsic width, never truncated
                ArabicText(
                    text = surah.nameArabic,
                    size = ArabicTextSize.MEDIUM,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Info button
                IconButton(
                    onClick = onInfoClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = stringResource(R.string.quran_home_surah_info),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Metadata badges row — aligned with English name (40dp box + 12dp spacer = 52dp start)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = SurahNumberSlotWidth + SurahNumberSlotSpacing, end = 14.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val isMeccan = surah.revelationType == RevelationType.MECCAN
                MetadataBadge(
                    text = if (isMeccan) stringResource(R.string.quran_home_makkah) else stringResource(R.string.quran_home_madinah),
                    modifier = Modifier.weight(1f)
                )
                MetadataBadge(
                    text = stringResource(R.string.quran_home_verses_count, surah.ayahCount),
                    modifier = Modifier.weight(1f)
                )
                if (startPage > 0) {
                    MetadataBadge(
                        text = stringResource(R.string.quran_home_page_range_format, startPage, endPage),
                        modifier = Modifier.weight(1f)
                    )
                    MetadataBadge(
                        text = stringResource(R.string.quran_home_juz_indicator, getJuzForPage(startPage)),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Khatam progress bar (unchanged)
            if (isKhatamActive && khatamTotalAyahs > 0 && khatamReadCount > 0) {
                LinearProgressIndicator(
                    progress = { khatamReadCount.toFloat() / khatamTotalAyahs },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .padding(horizontal = 14.dp),
                    color = if (isComplete) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun MetadataBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SurahListItemPreview() {
    NimazTheme {
        SurahListItem(
            surah = Surah(
                number = 1,
                nameArabic = "\u0627\u0644\u0641\u0627\u062A\u062D\u0629",
                nameEnglish = "Al-Fatihah",
                nameTransliteration = "The Opening",
                revelationType = RevelationType.MECCAN,
                ayahCount = 7,
                juzStart = 1,
                orderInMushaf = 5,
                startPage = 1
            ),
            onClick = {},
            onInfoClick = {},
            startPage = 1,
            endPage = 1
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
                nameArabic = "\u0627\u0644\u0645\u062C\u0627\u062F\u0644\u0629",
                nameEnglish = "Al-Mujadilah",
                nameTransliteration = "The Pleading Woman",
                revelationType = RevelationType.MEDINAN,
                ayahCount = 22,
                juzStart = 28,
                orderInMushaf = 105,
                startPage = 542
            ),
            onClick = {},
            onInfoClick = {},
            startPage = 542,
            endPage = 545
        )
    }
}
