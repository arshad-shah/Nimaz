package com.arshadshah.nimaz.ui.components.quran

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.data.local.models.LocalSurah
import com.arshadshah.nimaz.data.local.models.ReadingProgress

/**
 * Compact row-style card for displaying Surah reading progress.
 * Designed to fit within dropdown lists and take minimal vertical space (max 2 rows).
 */
@Composable
fun ReadingProgressCard(
    progress: ReadingProgress,
    surah: LocalSurah?,
    onContinueReading: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val completionFraction = progress.completionPercentage / 100f
    val isComplete = progress.completionPercentage >= 100f

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Surah Number Badge
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isComplete)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isComplete) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        Text(
                            text = "${progress.surahNumber}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // Info Section
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Row 1: Title + Percentage
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = surah?.englishNameTranslation ?: "Surah ${progress.surahNumber}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Surface(
                        color = if (isComplete)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else
                            MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${progress.completionPercentage.toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isComplete)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                // Row 2: Position + Progress Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Aya ${progress.lastReadAyaNumber}" + (surah?.let { " / ${it.numberOfAyahs}" } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LinearProgressIndicator(
                        progress = { completionFraction },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (isComplete)
                            MaterialTheme.colorScheme.tertiary
                        else
                            MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            // Action Buttons
            FilledIconButton(
                onClick = onContinueReading,
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = "Continue reading",
                    modifier = Modifier.size(20.dp)
                )
            }

            FilledTonalIconButton(
                onClick = onDelete,
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                )
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete progress",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// =============================================================================
// PREVIEWS
// =============================================================================

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun ReadingProgressCardPreview_InProgress() {
    MaterialTheme {
        ReadingProgressCard(
            progress = ReadingProgress(
                surahNumber = 2,
                lastReadAyaNumber = 142,
                completionPercentage = 49.5f,
                lastReadDate = "2024-03-15",
                totalReadingTimeMinutes = 45
            ),
            surah = LocalSurah(
                number = 2,
                numberOfAyahs = 286,
                startAya = 8,
                name = "البقرة",
                englishName = "Al-Baqarah",
                englishNameTranslation = "The Cow",
                revelationType = "Medinan",
                revelationOrder = 87,
                rukus = 40
            ),
            onContinueReading = {},
            onDelete = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun ReadingProgressCardPreview_Complete() {
    MaterialTheme {
        ReadingProgressCard(
            progress = ReadingProgress(
                surahNumber = 1,
                lastReadAyaNumber = 7,
                completionPercentage = 100f,
                lastReadDate = "2024-03-14",
                totalReadingTimeMinutes = 5
            ),
            surah = LocalSurah(
                number = 1,
                numberOfAyahs = 7,
                startAya = 1,
                name = "الفاتحة",
                englishName = "Al-Fatihah",
                englishNameTranslation = "The Opening",
                revelationType = "Meccan",
                revelationOrder = 5,
                rukus = 1
            ),
            onContinueReading = {},
            onDelete = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun ReadingProgressCardPreview_LongName() {
    MaterialTheme {
        ReadingProgressCard(
            progress = ReadingProgress(
                surahNumber = 18,
                lastReadAyaNumber = 45,
                completionPercentage = 40.9f,
                lastReadDate = "2024-03-13",
                totalReadingTimeMinutes = 30
            ),
            surah = LocalSurah(
                number = 18,
                numberOfAyahs = 110,
                startAya = 2511,
                name = "الكهف",
                englishName = "Al-Kahf",
                englishNameTranslation = "The Cave",
                revelationType = "Meccan",
                revelationOrder = 69,
                rukus = 12
            ),
            onContinueReading = {},
            onDelete = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun ReadingProgressCardPreview_NoSurahInfo() {
    MaterialTheme {
        ReadingProgressCard(
            progress = ReadingProgress(
                surahNumber = 36,
                lastReadAyaNumber = 40,
                completionPercentage = 48.8f,
                lastReadDate = "2024-03-12",
                totalReadingTimeMinutes = 20
            ),
            surah = null,
            onContinueReading = {},
            onDelete = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun ReadingProgressCardPreview_EarlyProgress() {
    MaterialTheme {
        ReadingProgressCard(
            progress = ReadingProgress(
                surahNumber = 55,
                lastReadAyaNumber = 5,
                completionPercentage = 6.4f,
                lastReadDate = "2024-03-15",
                totalReadingTimeMinutes = 3
            ),
            surah = LocalSurah(
                number = 55,
                numberOfAyahs = 78,
                startAya = 4901,
                name = "الرحمن",
                englishName = "Ar-Rahman",
                englishNameTranslation = "The Most Gracious",
                revelationType = "Medinan",
                revelationOrder = 97,
                rukus = 3
            ),
            onContinueReading = {},
            onDelete = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ReadingProgressCardPreview_Dark() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ReadingProgressCard(
                progress = ReadingProgress(
                    surahNumber = 67,
                    lastReadAyaNumber = 15,
                    completionPercentage = 50f,
                    lastReadDate = "2024-03-15",
                    totalReadingTimeMinutes = 10
                ),
                surah = LocalSurah(
                    number = 67,
                    numberOfAyahs = 30,
                    startAya = 5334,
                    name = "الملك",
                    englishName = "Al-Mulk",
                    englishNameTranslation = "The Dominion",
                    revelationType = "Meccan",
                    revelationOrder = 77,
                    rukus = 2
                ),
                onContinueReading = {},
                onDelete = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun ReadingProgressCardPreview_MultipleCards() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReadingProgressCard(
                progress = ReadingProgress(
                    surahNumber = 2,
                    lastReadAyaNumber = 142,
                    completionPercentage = 49.5f,
                    lastReadDate = "2024-03-15",
                    totalReadingTimeMinutes = 45
                ),
                surah = LocalSurah(
                    number = 2,
                    numberOfAyahs = 286,
                    startAya = 8,
                    name = "البقرة",
                    englishName = "Al-Baqarah",
                    englishNameTranslation = "The Cow",
                    revelationType = "Medinan",
                    revelationOrder = 87,
                    rukus = 40
                ),
                onContinueReading = {},
                onDelete = {}
            )
            ReadingProgressCard(
                progress = ReadingProgress(
                    surahNumber = 18,
                    lastReadAyaNumber = 75,
                    completionPercentage = 68.2f,
                    lastReadDate = "2024-03-14",
                    totalReadingTimeMinutes = 25
                ),
                surah = LocalSurah(
                    number = 18,
                    numberOfAyahs = 110,
                    startAya = 2511,
                    name = "الكهف",
                    englishName = "Al-Kahf",
                    englishNameTranslation = "The Cave",
                    revelationType = "Meccan",
                    revelationOrder = 69,
                    rukus = 12
                ),
                onContinueReading = {},
                onDelete = {}
            )
            ReadingProgressCard(
                progress = ReadingProgress(
                    surahNumber = 36,
                    lastReadAyaNumber = 83,
                    completionPercentage = 100f,
                    lastReadDate = "2024-03-13",
                    totalReadingTimeMinutes = 18
                ),
                surah = LocalSurah(
                    number = 36,
                    numberOfAyahs = 83,
                    startAya = 3705,
                    name = "يس",
                    englishName = "Ya-Sin",
                    englishNameTranslation = "Ya-Sin",
                    revelationType = "Meccan",
                    revelationOrder = 41,
                    rukus = 5
                ),
                onContinueReading = {},
                onDelete = {}
            )
        }
    }
}