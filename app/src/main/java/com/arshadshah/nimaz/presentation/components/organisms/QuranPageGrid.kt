package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.ui.res.stringResource
import com.arshadshah.nimaz.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.domain.model.PageAyahRange

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
 * Resolves the surface fill colour for a page tile based on its state.
 */
@Composable
private fun pageSurfaceColor(isSelected: Boolean, isComplete: Boolean) = when {
    isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
    isComplete -> MaterialTheme.colorScheme.primaryContainer
    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
}

/**
 * Resolves the border for a page tile.
 */
@Composable
private fun pageBorder(isSelected: Boolean) = BorderStroke(
    width = if (isSelected) 2.dp else 1.dp,
    color = if (isSelected)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
)

/**
 * Resolves the text colour for the page number.
 */
@Composable
private fun pageNumberColor(highlighted: Boolean) =
    if (highlighted) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.primary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ),
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
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
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
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        // End page cutout badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
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

                    Surface(
                        onClick = { onNavigateToPage(pageNumber) },
                        shape = RoundedCornerShape(12.dp),
                        color = pageSurfaceColor(isSelected, isComplete),
                        border = pageBorder(isSelected)
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
                                    fontWeight = FontWeight.Bold,
                                    color = pageNumberColor(isComplete || isSelected)
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

                            Surface(
                                onClick = { onNavigateToPage(pg) },
                                shape = RoundedCornerShape(8.dp),
                                color = pageSurfaceColor(isSelected, isComplete),
                                border = pageBorder(isSelected)
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
                                        fontWeight = FontWeight.Bold,
                                        color = pageNumberColor(isComplete || isSelected)
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
