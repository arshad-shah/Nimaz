package com.arshadshah.nimaz.ui.components.quran

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.data.local.models.LocalSurah
import com.arshadshah.nimaz.ui.components.common.placeholder.material.PlaceholderHighlight
import com.arshadshah.nimaz.ui.components.common.placeholder.material.placeholder
import com.arshadshah.nimaz.ui.components.common.placeholder.material.shimmer
import com.arshadshah.nimaz.ui.theme.utmaniQuranFont

/**
 * Hero info card displayed at the top of the Ayat list screen.
 * Shows comprehensive Surah information before the ayat begin.
 *
 * Design System Alignment:
 * - ElevatedCard with extraLarge shape
 * - 4dp elevation
 * - 8dp inner padding
 * - 8dp section spacing (compact)
 * - Header: primaryContainer with 16dp corners
 * - Content: surfaceVariant @ 0.5 alpha with 16dp corners
 */
@Composable
fun SurahHeader(
    surah: LocalSurah,
    loading: Boolean = false,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Section - Horizontal layout
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Surah Number Badge
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${surah.number}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.placeholder(
                                    visible = loading,
                                    highlight = PlaceholderHighlight.shimmer()
                                )
                            )
                        }
                    }

                    // English Name & Translation
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = surah.englishName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.placeholder(
                                visible = loading,
                                highlight = PlaceholderHighlight.shimmer()
                            )
                        )
                        Text(
                            text = surah.englishNameTranslation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.placeholder(
                                visible = loading,
                                highlight = PlaceholderHighlight.shimmer()
                            )
                        )
                    }

                    // Arabic Name
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Text(
                                text = surah.name,
                                fontFamily = utmaniQuranFont,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .placeholder(
                                        visible = loading,
                                        highlight = PlaceholderHighlight.shimmer()
                                    )
                            )
                        }
                    }
                }
            }

            // Content Section - Metadata Chips
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Verses Count
                    MetadataChip(
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        text = "${surah.numberOfAyahs} Ayat",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        loading = loading
                    )

                    // Revelation Type
                    MetadataChip(
                        icon = Icons.Default.Place,
                        text = surah.revelationType,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        loading = loading
                    )

                    // Revelation Order
                    MetadataChip(
                        icon = Icons.Default.Numbers,
                        text = "${surah.revelationOrder}",
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        loading = loading
                    )
                }
            }
        }
    }
}

/**
 * Metadata chip for displaying surah info.
 * Uses 8dp corners and semantic container colors.
 */
@Composable
private fun MetadataChip(
    icon: ImageVector,
    text: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    loading: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )

            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = contentColor,
                modifier = Modifier.placeholder(
                    visible = loading,
                    highlight = PlaceholderHighlight.shimmer()
                )
            )
        }
    }
}

// =============================================================================
// PREVIEWS
// =============================================================================

private val previewSurah = LocalSurah(
    number = 1,
    numberOfAyahs = 7,
    startAya = 1,
    name = "الفاتحة",
    englishName = "Al-Fatihah",
    englishNameTranslation = "The Opening",
    revelationType = "Meccan",
    revelationOrder = 5,
    rukus = 1
)

private val previewSurahLong = LocalSurah(
    number = 2,
    numberOfAyahs = 286,
    startAya = 8,
    name = "البقرة",
    englishName = "Al-Baqarah",
    englishNameTranslation = "The Cow",
    revelationType = "Medinan",
    revelationOrder = 87,
    rukus = 40
)

private val previewSurahYaseen = LocalSurah(
    number = 36,
    numberOfAyahs = 83,
    startAya = 3705,
    name = "يس",
    englishName = "Ya-Sin",
    englishNameTranslation = "Ya-Sin",
    revelationType = "Meccan",
    revelationOrder = 41,
    rukus = 5
)

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun SurahHeaderPreview() {
    MaterialTheme {
        SurahHeader(
            surah = previewSurah,
            loading = false,
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun SurahHeaderPreview_Baqarah() {
    MaterialTheme {
        SurahHeader(
            surah = previewSurahLong,
            loading = false,
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun SurahHeaderPreview_Yaseen() {
    MaterialTheme {
        SurahHeader(
            surah = previewSurahYaseen,
            loading = false,
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun SurahHeaderPreview_Loading() {
    MaterialTheme {
        SurahHeader(
            surah = previewSurah,
            loading = true,
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SurahHeaderPreview_Dark() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(color = MaterialTheme.colorScheme.background) {
            SurahHeader(
                surah = previewSurah,
                loading = false,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}