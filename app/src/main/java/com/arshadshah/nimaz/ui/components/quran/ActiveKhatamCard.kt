package com.arshadshah.nimaz.ui.components.quran

import android.content.res.Configuration
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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
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
import com.arshadshah.nimaz.data.local.models.KhatamSession
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun ActiveKhatamCard(
    khatam: KhatamSession,
    todayProgress: Int,
    onContinueReading: () -> Unit,
    onComplete: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = khatam.totalAyasRead.toFloat() / 6236f
    val progressPercent = (progress * 100).toInt()
    val isNearlyComplete = progress >= 0.99f

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Icon + Title
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Progress Ring with Icon
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 4.dp,
                            color = when {
                                isNearlyComplete -> MaterialTheme.colorScheme.tertiary
                                khatam.isActive -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.outline
                            },
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Icon(
                            imageVector = when {
                                isNearlyComplete -> Icons.Default.CheckCircle
                                khatam.isActive -> Icons.AutoMirrored.Filled.MenuBook
                                else -> Icons.Default.Pause
                            },
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = when {
                                isNearlyComplete -> MaterialTheme.colorScheme.tertiary
                                khatam.isActive -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.outline
                            }
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = khatam.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Started ${formatDate(khatam.startDate)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Status Badge
                Surface(
                    color = when {
                        isNearlyComplete -> MaterialTheme.colorScheme.tertiaryContainer
                        khatam.isActive -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when {
                                isNearlyComplete -> Icons.Default.Star
                                khatam.isActive -> Icons.Default.PlayArrow
                                else -> Icons.Default.Pause
                            },
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = when {
                                isNearlyComplete -> MaterialTheme.colorScheme.onTertiaryContainer
                                khatam.isActive -> MaterialTheme.colorScheme.onPrimaryContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Text(
                            text = when {
                                isNearlyComplete -> "Almost Done!"
                                khatam.isActive -> "Active"
                                else -> "Paused"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = when {
                                isNearlyComplete -> MaterialTheme.colorScheme.onTertiaryContainer
                                khatam.isActive -> MaterialTheme.colorScheme.onPrimaryContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }

            // Progress Section
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Progress Stats Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Current Position
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Surah ${khatam.currentSurah}, Aya ${khatam.currentAya}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Percentage
                        Text(
                            text = "$progressPercent%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Progress Bar
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = when {
                            isNearlyComplete -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.primary
                        },
                        trackColor = MaterialTheme.colorScheme.surface
                    )

                    // Ayat count
                    Text(
                        text = "${khatam.totalAyasRead.formatWithCommas()} of 6,236 ayat read",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Daily Target (if set)
            khatam.dailyTarget?.let { target ->
                val targetMet = todayProgress >= target
                val dailyProgress = (todayProgress.toFloat() / target).coerceAtMost(1f)

                Surface(
                    color = if (targetMet)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (targetMet) Icons.Default.CheckCircle else Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (targetMet)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (targetMet) "Daily goal reached!" else "Today's progress",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (targetMet) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (targetMet)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                            if (!targetMet) {
                                LinearProgressIndicator(
                                    progress = { dailyProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = MaterialTheme.colorScheme.secondary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }

                        Text(
                            text = "$todayProgress / $target",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (targetMet)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Primary Action Button
                Button(
                    onClick = if (isNearlyComplete) onComplete else onContinueReading,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isNearlyComplete)
                            MaterialTheme.colorScheme.tertiary
                        else
                            MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = if (isNearlyComplete)
                            Icons.Default.CheckCircle
                        else
                            Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isNearlyComplete) "Complete" else "Continue",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                // Secondary Actions
                if (!isNearlyComplete) {
                    FilledTonalIconButton(
                        onClick = if (khatam.isActive) onPause else onResume,
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = if (khatam.isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (khatam.isActive) "Pause" else "Resume",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                FilledTonalIconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        modifier = Modifier.size(20.dp)
                    )
                }

                FilledTonalIconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// Helper extension function
private fun Int.formatWithCommas(): String {
    return String.format("%,d", this)
}

// Helper function for date formatting (customize as needed)
private fun formatDate(dateString: String): String {
    return try {
        val date = LocalDate.parse(dateString)
        date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    } catch (e: Exception) {
        dateString
    }
}

// ==================== Previews ====================

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun ActiveKhatamCardPreview_Active() {
    MaterialTheme {
        ActiveKhatamCard(
            khatam = KhatamSession(
                id = 1,
                name = "Ramadan Khatam 2024",
                startDate = "2024-03-10",
                isActive = true,
                currentSurah = 18,
                currentAya = 45,
                totalAyasRead = 2150,
                dailyTarget = 100,
                createdAt = "2024-03-10T08:00:00"
            ),
            todayProgress = 67,
            onContinueReading = {},
            onComplete = {},
            onPause = {},
            onResume = {},
            onEdit = {},
            onDelete = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun ActiveKhatamCardPreview_TargetMet() {
    MaterialTheme {
        ActiveKhatamCard(
            khatam = KhatamSession(
                id = 1,
                name = "Daily Reading",
                startDate = "2024-01-15",
                isActive = true,
                currentSurah = 36,
                currentAya = 83,
                totalAyasRead = 3800,
                dailyTarget = 50,
                createdAt = "2024-01-15T08:00:00"
            ),
            todayProgress = 65,
            onContinueReading = {},
            onComplete = {},
            onPause = {},
            onResume = {},
            onEdit = {},
            onDelete = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun ActiveKhatamCardPreview_Paused() {
    MaterialTheme {
        ActiveKhatamCard(
            khatam = KhatamSession(
                id = 2,
                name = "Weekend Khatam",
                startDate = "2024-02-01",
                isActive = false,
                currentSurah = 5,
                currentAya = 30,
                totalAyasRead = 750,
                dailyTarget = null,
                createdAt = "2024-02-01T10:00:00"
            ),
            todayProgress = 0,
            onContinueReading = {},
            onComplete = {},
            onPause = {},
            onResume = {},
            onEdit = {},
            onDelete = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun ActiveKhatamCardPreview_NearlyComplete() {
    MaterialTheme {
        ActiveKhatamCard(
            khatam = KhatamSession(
                id = 3,
                name = "My First Khatam",
                startDate = "2023-12-01",
                isActive = true,
                currentSurah = 114,
                currentAya = 4,
                totalAyasRead = 6200,
                dailyTarget = 20,
                createdAt = "2023-12-01T08:00:00"
            ),
            todayProgress = 36,
            onContinueReading = {},
            onComplete = {},
            onPause = {},
            onResume = {},
            onEdit = {},
            onDelete = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun ActiveKhatamCardPreview_NoTarget() {
    MaterialTheme {
        ActiveKhatamCard(
            khatam = KhatamSession(
                id = 4,
                name = "Casual Reading",
                startDate = "2024-03-01",
                isActive = true,
                currentSurah = 2,
                currentAya = 255,
                totalAyasRead = 400,
                dailyTarget = null,
                createdAt = "2024-03-01T08:00:00"
            ),
            todayProgress = 0,
            onContinueReading = {},
            onComplete = {},
            onPause = {},
            onResume = {},
            onEdit = {},
            onDelete = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ActiveKhatamCardPreview_Dark() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ActiveKhatamCard(
                khatam = KhatamSession(
                    id = 1,
                    name = "Night Reading",
                    startDate = "2024-03-10",
                    isActive = true,
                    currentSurah = 55,
                    currentAya = 13,
                    totalAyasRead = 4500,
                    dailyTarget = 75,
                    createdAt = "2024-03-10T22:00:00"
                ),
                todayProgress = 42,
                onContinueReading = {},
                onComplete = {},
                onPause = {},
                onResume = {},
                onEdit = {},
                onDelete = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}