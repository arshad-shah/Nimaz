package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.ui.res.stringResource
import com.arshadshah.nimaz.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import android.content.res.Configuration
import androidx.compose.foundation.border
import com.arshadshah.nimaz.domain.model.PageAyahRange
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardDefaults
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardTone
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconVariant
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * Pre-computes the LazyColumn item index for each Juz header (1..30).
 * Must mirror the same grouping logic used in [pageGridItems].
 */
internal fun computeJuzHeaderIndices(
    surahStartPageMap: Map<Int, List<String>>
): Map<Int, Int> {
    val indices = mutableMapOf<Int, Int>()
    var itemIndex = 0
    for (juz in 1..30) {
        indices[juz] = itemIndex
        itemIndex++ // juz header item

        val startPage = getJuzStartPage(juz)
        val endPage = getJuzEndPage(juz)
        val pages = (startPage..endPage).toList()
        var i = 0
        while (i < pages.size) {
            if (surahStartPageMap[pages[i]] != null) {
                itemIndex++ // individual full-width item
                i++
            } else {
                // consecutive no-badge pages → one grid item
                var j = i + 1
                while (j < pages.size && surahStartPageMap[pages[j]] == null) {
                    j++
                }
                itemIndex++
                i = j
            }
        }
    }
    return indices
}

/**
 * The shared card colours for a Quran page/juz tile: a quiet inactive surface with
 * a hairline outline, switching to the primary container plus a 2.dp primary border
 * once the tile is selected or its khatam progress is complete. Content colour is
 * published by [NimazCard], so tile labels inherit it.
 */
@Composable
internal fun quranTileCardColors() = NimazCardDefaults.selectable(
    border = MaterialTheme.colorScheme.outlineVariant,
    activeBorder = MaterialTheme.colorScheme.primary,
    activeBorderWidth = 2.dp
)

@OptIn(ExperimentalLayoutApi::class)
internal fun LazyListScope.pageGridItems(
    onNavigateToPage: (Int) -> Unit,
    khatamReadAyahIds: Set<Int> = emptySet(),
    isKhatamActive: Boolean = false,
    pageAyahRanges: List<PageAyahRange> = emptyList(),
    selectedPageNumber: Int? = null,
    surahStartPageMap: Map<Int, List<String>> = emptyMap()
) {
    // Pre-compute page progress map
    val pageProgressMap = if (isKhatamActive && pageAyahRanges.isNotEmpty()) {
        pageAyahRanges.associate { range ->
            val readCount = khatamReadAyahIds.count { it in range.minAyahId..range.maxAyahId }
            range.page to (readCount to range.ayahCount)
        }
    } else {
        emptyMap()
    }

    // Add items for each Juz section
    (1..30).forEach { juz ->
        val startPage = getJuzStartPage(juz)
        val endPage = getJuzEndPage(juz)

        // Juz header card with cutout page range badges
        item(key = "page_juz_header_$juz") {
            NimazCard(
                tone = NimazCardTone.ACCENT,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.quran_juz_number_format, juz),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        // Start page cutout badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = startPage.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        // Arrow cutout circle
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    CircleShape
                                )
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            NimazIcon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                variant = NimazIconVariant.PRIMARY,
                                iconSize = 14.dp
                            )
                        }
                        // End page cutout badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    RoundedCornerShape(6.dp)
                                )
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = endPage.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Group pages: consecutive no-badge pages go into compact grid items,
        // pages with surah badges stay as full-width row cards
        val pages = (startPage..endPage).toList()
        var i = 0
        while (i < pages.size) {
            val pageNumber = pages[i]
            val surahNames = surahStartPageMap[pageNumber]

            if (surahNames != null) {
                // Full-width card for pages with surah badges
                item(key = "page_$pageNumber") {
                    val (readCount, totalCount) = pageProgressMap[pageNumber] ?: (0 to 0)
                    val progress = if (totalCount > 0) readCount.toFloat() / totalCount else 0f
                    val isComplete = isKhatamActive && totalCount > 0 && readCount == totalCount
                    val isSelected = selectedPageNumber == pageNumber

                    NimazCard(
                        onClick = { onNavigateToPage(pageNumber) },
                        selected = isSelected || isComplete,
                        colors = quranTileCardColors()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Page number badge
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isKhatamActive && progress > 0f && !isComplete) {
                                    CircularProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier.size(32.dp),
                                        color = MaterialTheme.colorScheme.tertiary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                        strokeWidth = 2.dp
                                    )
                                }
                                Text(
                                    text = pageNumber.toString(),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            // Surah chips
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                surahNames.forEach { name ->
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                    ) {
                                        Text(
                                            text = name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(
                                                horizontal = 6.dp,
                                                vertical = 1.dp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                i++
            } else {
                // Collect consecutive no-badge pages into a compact grid
                val gridPages = mutableListOf(pageNumber)
                var j = i + 1
                while (j < pages.size && surahStartPageMap[pages[j]] == null) {
                    gridPages.add(pages[j])
                    j++
                }

                item(key = "page_grid_${gridPages.first()}_${gridPages.last()}") {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        gridPages.forEach { pg ->
                            val (readCount, totalCount) = pageProgressMap[pg] ?: (0 to 0)
                            val progress =
                                if (totalCount > 0) readCount.toFloat() / totalCount else 0f
                            val isComplete =
                                isKhatamActive && totalCount > 0 && readCount == totalCount
                            val isSelected = selectedPageNumber == pg

                            NimazCard(
                                onClick = { onNavigateToPage(pg) },
                                selected = isSelected || isComplete,
                                shape = RoundedCornerShape(8.dp),
                                colors = quranTileCardColors()
                            ) {
                                Box(
                                    modifier = Modifier.size(44.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isKhatamActive && progress > 0f && !isComplete) {
                                        CircularProgressIndicator(
                                            progress = { progress },
                                            modifier = Modifier.size(40.dp),
                                            color = MaterialTheme.colorScheme.tertiary,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                            strokeWidth = 2.dp
                                        )
                                    }
                                    Text(
                                        text = pg.toString(),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
                i = j
            }
        }
    }
}


// ==================== PREVIEWS ====================

/**
 * Showcase of [pageGridItems] rendered inside a [LazyColumn]. The sample
 * [surahStartPageMap] forces both layout branches to appear: full-width cards
 * with surah chips for pages with badges, and the compact wrapping grid of plain
 * page tiles for runs of no-badge pages. Rendered in both light and dark themes
 * by the previews below.
 */
@Composable
private fun QuranPageGridShowcase() {
    val surahStartPageMap = mapOf(
        1 to listOf("Al-Fatihah"),
        2 to listOf("Al-Baqarah")
    )
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        pageGridItems(
            onNavigateToPage = {},
            selectedPageNumber = 3,
            surahStartPageMap = surahStartPageMap
        )
    }
}

@Preview(showBackground = true, name = "Quran Page Grid — Light", heightDp = 700)
@Composable
private fun QuranPageGridLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        QuranPageGridShowcase()
    }
}

@Preview(
    showBackground = true, name = "Quran Page Grid — Dark", heightDp = 700,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun QuranPageGridDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        QuranPageGridShowcase()
    }
}
