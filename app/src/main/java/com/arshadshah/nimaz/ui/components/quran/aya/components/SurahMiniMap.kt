package com.arshadshah.nimaz.ui.components.quran.aya.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SurahMiniMap(
    currentAya: Int,
    totalAyas: Int,
    bookmarkedAyas: List<Int>,
    favoriteAyas: List<Int>,
    notedAyas: List<Int>,
    onAyaClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reading Progress",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "$currentAya / $totalAyas",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress visualization
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
            ) {
                // Background track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .align(Alignment.Center)
                        .background(
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            RoundedCornerShape(3.dp)
                        )
                )

                // Progress fill
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (totalAyas > 0) currentAya.toFloat() / totalAyas else 0f)
                        .height(6.dp)
                        .align(Alignment.CenterStart)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(3.dp)
                        )
                )

                // Markers for special ayas (only show if there aren't too many)
                if (totalAyas <= 50) {
                    val screenWidth = LocalConfiguration.current.screenWidthDp
                    val markerWidth = (screenWidth - 32) // Account for padding

                    // Bookmark markers
                    bookmarkedAyas.forEach { ayaNumber ->
                        if (ayaNumber <= totalAyas) {
                            val position = (ayaNumber.toFloat() / totalAyas) * markerWidth
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .offset(x = position.dp - 7.dp)
                                    .align(Alignment.CenterStart)
                                    .background(
                                        MaterialTheme.colorScheme.tertiary,
                                        CircleShape
                                    )
                                    .clickable { onAyaClick(ayaNumber) }
                            )
                        }
                    }

                    // Current position marker
                    if (currentAya <= totalAyas) {
                        val currentPosition = (currentAya.toFloat() / totalAyas) * markerWidth
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .offset(x = currentPosition.dp - 9.dp)
                                .align(Alignment.CenterStart)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    CircleShape
                                )
                                .clickable { onAyaClick(currentAya) }
                        )
                    }
                }
            }

            // Legend
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LegendItem(
                    color = MaterialTheme.colorScheme.tertiary,
                    label = "Bookmarks (${bookmarkedAyas.size})"
                )
                LegendItem(
                    color = Color.Red,
                    label = "Favorites (${favoriteAyas.size})"
                )
                LegendItem(
                    color = MaterialTheme.colorScheme.secondary,
                    label = "Notes (${notedAyas.size})"
                )
            }
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}