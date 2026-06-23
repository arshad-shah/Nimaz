package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.KhatamConstants
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardDefaults
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.theme.NimazTheme

// Juz to page mapping (approximate start pages for each Juz)
internal val juzStartPages = listOf(
    1, 22, 42, 62, 82, 102, 121, 142, 162, 182,
    201, 222, 242, 262, 282, 302, 322, 342, 362, 382,
    402, 422, 442, 462, 482, 502, 522, 542, 562, 582
)

internal fun getJuzForPage(page: Int): Int {
    for (i in juzStartPages.indices.reversed()) {
        if (page >= juzStartPages[i]) return i + 1
    }
    return 1
}

internal fun getJuzStartPage(juz: Int): Int = juzStartPages.getOrElse(juz - 1) { 1 }

internal fun getJuzEndPage(juz: Int): Int = if (juz < 30) juzStartPages[juz] - 1 else 604

@Composable
internal fun JuzGrid(
    onNavigateToJuz: (Int) -> Unit,
    khatamReadAyahIds: Set<Int> = emptySet(),
    isKhatamActive: Boolean = false,
    selectedJuzNumber: Int? = null,
    modifier: Modifier = Modifier
) {
    val columns = 5
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
        juzNumbers.chunked(columns).forEach { row ->
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
                        style = NimazCardStyle.FILLED,
                        onClick = { onNavigateToJuz(juzNumber) },
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .then(
                                if (isSelected) Modifier.border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(12.dp)
                                ) else Modifier
                            ),
                        shape = RoundedCornerShape(12.dp),
                        colors = NimazCardDefaults.colors(
                            container = when {
                                isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                isComplete -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            }
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            // Show progress ring behind number when khatam is active
                            if (isKhatamActive && progress > 0f && !isComplete) {
                                CircularProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.size(48.dp),
                                    color = MaterialTheme.colorScheme.tertiary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    strokeWidth = 3.dp
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = juzNumber.toString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isComplete || isSelected)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(R.string.quran_home_juz_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isComplete || isSelected)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = stringResource(
                                        R.string.quran_home_page_range_format,
                                        getJuzStartPage(juzNumber),
                                        getJuzEndPage(juzNumber)
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    color = if (isComplete || isSelected)
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
                repeat(columns - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
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
