package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.domain.model.KhatamConstants
import com.arshadshah.nimaz.domain.model.MushafPagination
import com.arshadshah.nimaz.domain.model.MushafScript
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeEmphasis
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeShape
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcons
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.components.atoms.ShamsaMedallion
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/** Traditional Juz names — the Arabic first-word(s) of each of the 30 juz. */
internal val juzNames = listOf(
    "الم", "سَيَقُولُ", "تِلْكَ الرُّسُلُ", "لَنْ تَنَالُوا", "وَالْمُحْصَنَاتُ",
    "لَا يُحِبُّ اللَّهُ", "وَإِذَا سَمِعُوا", "وَلَوْ أَنَّنَا", "قَالَ الْمَلَأُ", "وَاعْلَمُوا",
    "يَعْتَذِرُونَ", "وَمَا مِنْ دَابَّةٍ", "وَمَا أُبَرِّئُ", "رُبَمَا", "سُبْحَانَ الَّذِي",
    "قَالَ أَلَمْ", "اقْتَرَبَ لِلنَّاسِ", "قَدْ أَفْلَحَ", "وَقَالَ الَّذِينَ", "أَمَّنْ خَلَقَ",
    "اتْلُ مَا أُوحِيَ", "وَمَنْ يَقْنُتْ", "وَمَا لِيَ", "فَمَنْ أَظْلَمُ", "إِلَيْهِ يُرَدُّ",
    "حم", "قَالَ فَمَا خَطْبُكُمْ", "قَدْ سَمِعَ اللَّهُ", "تَبَارَكَ الَّذِي", "عَمَّ"
)

/** Name of a juz (Arabic first-word), 1-based. */
internal fun getJuzName(juz: Int): String = juzNames.getOrElse(juz - 1) { "" }

/**
 * @param pagination the active edition's page mapping — the juz page badges follow it, so
 *   they show IndoPak-16 page numbers when that layout is selected rather than the Madani
 *   ones they were previously hardcoded to (#325).
 */
@Composable
internal fun JuzGrid(
    onNavigateToJuz: (Int) -> Unit,
    pagination: MushafPagination = MushafPagination.fallback(MushafScript.DEFAULT),
    khatamReadAyahIds: Set<Int> = emptySet(),
    isKhatamActive: Boolean = false,
    selectedJuzNumber: Int? = null,
    modifier: Modifier = Modifier
) {
    val juzNumbers = (1..30).toList()

    // Pre-compute juz progress
    val juzProgress = remember(khatamReadAyahIds, isKhatamActive) {
        if (!isKhatamActive) emptyMap()
        else KhatamConstants.JUZ_AYAH_RANGES.mapIndexed { index, (start, end) ->
            val total = end - start + 1
            val read = khatamReadAyahIds.count { it in start..end }
            (index + 1) to (read to total)
        }.toMap()
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Two-column ornamental cards: shamsa medallion (number) + Juz name + page-range badges
        juzNumbers.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { juzNumber ->
                    val (readCount, totalCount) = juzProgress[juzNumber] ?: (0 to 0)
                    val progress = if (totalCount > 0) readCount.toFloat() / totalCount else 0f
                    val isComplete = isKhatamActive && totalCount > 0 && readCount == totalCount
                    val isSelected = selectedJuzNumber == juzNumber

                    NimazCard(
                        onClick = { onNavigateToJuz(juzNumber) },
                        modifier = Modifier.weight(1f),
                        selected = isSelected || isComplete,
                        // Shared tile language with the Pages tab grid
                        colors = quranTileCardColors()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp, horizontal = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Medallion (with progress ring behind it while khatam is active)
                            Box(contentAlignment = Alignment.Center) {
                                if (isKhatamActive && progress > 0f && !isComplete) {
                                    CircularProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier.size(52.dp),
                                        color = MaterialTheme.colorScheme.tertiary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                        strokeWidth = 3.dp
                                    )
                                }
                                ShamsaMedallion(number = juzNumber, size = 46.dp)
                            }
                            // Juz name — Arabic first-word, gold to match the ornament language
                            ArabicText(
                                text = getJuzName(juzNumber),
                                size = ArabicTextSize.MEDIUM,
                                color = NimazColors.GoldDark,
                                maxLines = 1
                            )
                            JuzRangeBadges(
                                startPage = pagination.juzStartPage(juzNumber),
                                endPage = pagination.juzEndPage(juzNumber)
                            )
                        }
                    }
                }
                // Keep the last odd card left-aligned at half width
                if (row.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/** Start → arrow → end page-range "cutout" badges, mirroring the Pages-tab header. */
@Composable
private fun JuzRangeBadges(startPage: Int, endPage: Int) {
    val fill = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        NimazBadge(
            text = startPage.toString(),
            tone = NimazTone.ACCENT,
            emphasis = NimazBadgeEmphasis.CUTOUT,
            shape = NimazBadgeShape.ROUNDED,
            size = NimazBadgeSize.SMALL
        )
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(fill),
            contentAlignment = Alignment.Center
        ) {
            NimazIcon(
                imageVector = NimazIcons.Forward,
                contentDescription = null,
                variant = NimazIconVariant.PRIMARY,
                iconSize = 13.dp
            )
        }
        NimazBadge(
            text = endPage.toString(),
            tone = NimazTone.ACCENT,
            emphasis = NimazBadgeEmphasis.CUTOUT,
            shape = NimazBadgeShape.ROUNDED,
            size = NimazBadgeSize.SMALL
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun JuzGridPreview() {
    NimazTheme {
        JuzGrid(
            onNavigateToJuz = {},
            isKhatamActive = false
        )
    }
}
