package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.domain.model.FastRecord
import com.arshadshah.nimaz.domain.model.FastStatus
import com.arshadshah.nimaz.domain.model.FastType
import com.arshadshah.nimaz.domain.model.FastingStats
import com.arshadshah.nimaz.domain.model.RamadanProgress
import com.arshadshah.nimaz.presentation.theme.NimazColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Ramadan fasting calendar with progress tracking.
 */
@Composable
fun RamadanCalendar(
    progress: RamadanProgress,
    modifier: Modifier = Modifier,
    selectedDate: LocalDate? = null,
    onDayClick: (FastRecord) -> Unit = {},
    onEditClick: (FastRecord) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(true) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header with progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Ramadan ${progress.year}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${progress.fastedDays}/${progress.totalDays} days fasted",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress bar
            RamadanProgressBar(progress = progress)

            // Calendar grid
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Day of week headers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                            Text(
                                text = day,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Calendar grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Add empty cells for alignment
                        val firstRecord = progress.records.firstOrNull()
                        val startOffset = if (firstRecord != null) {
                            val date = Instant.ofEpochMilli(firstRecord.date)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            date.dayOfWeek.value % 7
                        } else 0

                        items(startOffset) {
                            Box(modifier = Modifier.aspectRatio(1f))
                        }

                        items(progress.records) { record ->
                            val recordDate = Instant.ofEpochMilli(record.date)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()

                            FastingDayCell(
                                dayNumber = recordDate.dayOfMonth,
                                status = record.status,
                                isSelected = selectedDate == recordDate,
                                onClick = { onDayClick(record) },
                                modifier = Modifier.aspectRatio(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Legend
                    FastingLegend()
                }
            }
        }
    }
}

/**
 * Ramadan progress bar with animation.
 */
@Composable
private fun RamadanProgressBar(
    progress: RamadanProgress,
    modifier: Modifier = Modifier
) {
    var animationPlayed by remember { mutableStateOf(false) }
    val totalProgress = if (progress.totalDays > 0) {
        progress.fastedDays.toFloat() / progress.totalDays
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) totalProgress else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "progress_animation"
    )

    LaunchedEffect(Unit) {
        animationPlayed = true
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${(animatedProgress * 100).toInt()}% Complete",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = NimazColors.FastingColors.Fasted
            )
            Text(
                text = "${progress.remainingDays} days remaining",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp)),
            color = NimazColors.FastingColors.Fasted,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

/**
 * Single fasting day cell.
 */
@Composable
private fun FastingDayCell(
    dayNumber: Int,
    status: FastStatus,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (status) {
        FastStatus.FASTED -> NimazColors.FastingColors.Fasted.copy(alpha = 0.15f)
        FastStatus.NOT_FASTED -> NimazColors.StatusColors.Missed.copy(alpha = 0.1f)
        FastStatus.EXEMPTED -> NimazColors.FastingColors.Exempted.copy(alpha = 0.1f)
        FastStatus.MAKEUP_DUE -> NimazColors.FastingColors.Makeup.copy(alpha = 0.1f)
    }

    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        status == FastStatus.FASTED -> NimazColors.FastingColors.Fasted
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(
                width = if (isSelected || status == FastStatus.FASTED) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = dayNumber.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (status == FastStatus.FASTED) FontWeight.Bold else FontWeight.Normal,
                color = when (status) {
                    FastStatus.FASTED -> NimazColors.FastingColors.Fasted
                    FastStatus.NOT_FASTED -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )

            when (status) {
                FastStatus.FASTED -> Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Fasted",
                    tint = NimazColors.FastingColors.Fasted,
                    modifier = Modifier.size(12.dp)
                )
                FastStatus.NOT_FASTED -> Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Not fasted",
                    tint = NimazColors.StatusColors.Missed.copy(alpha = 0.5f),
                    modifier = Modifier.size(12.dp)
                )
                FastStatus.MAKEUP_DUE -> Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(NimazColors.FastingColors.Makeup)
                )
                FastStatus.EXEMPTED -> Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(NimazColors.FastingColors.Exempted)
                )
            }
        }
    }
}

/**
 * Fasting status legend.
 */
@Composable
private fun FastingLegend(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        LegendItem(
            color = NimazColors.FastingColors.Fasted,
            label = "Fasted"
        )
        LegendItem(
            color = NimazColors.StatusColors.Missed,
            label = "Missed"
        )
        LegendItem(
            color = NimazColors.FastingColors.Makeup,
            label = "Makeup"
        )
        LegendItem(
            color = NimazColors.FastingColors.Exempted,
            label = "Exempt"
        )
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Fasting statistics card.
 */
@Composable
fun FastingStatsCard(
    stats: FastingStats,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Fasting Statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = stats.totalFasted.toString(),
                    label = "Total\nFasted",
                    color = NimazColors.FastingColors.Fasted
                )
                StatItem(
                    value = stats.ramadanFasted.toString(),
                    label = "Ramadan\nDays",
                    color = NimazColors.Primary
                )
                StatItem(
                    value = stats.voluntaryFasted.toString(),
                    label = "Voluntary\nFasts",
                    color = NimazColors.Secondary
                )
                StatItem(
                    value = stats.currentStreak.toString(),
                    label = "Current\nStreak",
                    color = NimazColors.Tertiary
                )
            }

            if (stats.pendingMakeupCount > 0) {
                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NimazColors.FastingColors.Makeup.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = NimazColors.FastingColors.Makeup,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Makeup Fasts Due",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = NimazColors.FastingColors.Makeup
                            )
                            Text(
                                text = "${stats.pendingMakeupCount} days remaining",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Voluntary fasting tracker.
 */
@Composable
fun VoluntaryFastingTracker(
    currentMonth: List<FastRecord>,
    modifier: Modifier = Modifier,
    onDayClick: (LocalDate) -> Unit = {},
    onLogFastClick: () -> Unit = {}
) {
    val today = LocalDate.now()
    val daysInMonth = today.lengthOfMonth()
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy")

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Voluntary Fasts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = today.format(formatter),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    onClick = onLogFastClick
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Log Fast",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mini calendar for month
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.height(180.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Week day headers
                items(listOf("S", "M", "T", "W", "T", "F", "S")) { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // Days
                val firstDayOfMonth = today.withDayOfMonth(1)
                val startOffset = firstDayOfMonth.dayOfWeek.value % 7

                items(startOffset) {
                    Box(modifier = Modifier.aspectRatio(1f))
                }

                items(daysInMonth) { index ->
                    val dayOfMonth = index + 1
                    val date = today.withDayOfMonth(dayOfMonth)
                    val record = currentMonth.find { record ->
                        Instant.ofEpochMilli(record.date)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate() == date
                    }

                    VoluntaryDayCell(
                        dayNumber = dayOfMonth,
                        isFasted = record?.status == FastStatus.FASTED,
                        isToday = date == today,
                        isFuture = date.isAfter(today),
                        onClick = { onDayClick(date) },
                        modifier = Modifier.aspectRatio(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Recommended fasting days info
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ) {
                Text(
                    text = "Recommended: Mondays, Thursdays, and the 13th, 14th, 15th of each Hijri month",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun VoluntaryDayCell(
    dayNumber: Int,
    isFasted: Boolean,
    isToday: Boolean,
    isFuture: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                when {
                    isFasted -> NimazColors.FastingColors.Fasted.copy(alpha = 0.15f)
                    isToday -> MaterialTheme.colorScheme.primaryContainer
                    else -> Color.Transparent
                }
            )
            .border(
                width = if (isToday) 2.dp else 0.dp,
                color = if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = CircleShape
            )
            .clickable(enabled = !isFuture, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isFasted) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Fasted",
                tint = NimazColors.FastingColors.Fasted,
                modifier = Modifier.size(16.dp)
            )
        } else {
            Text(
                text = dayNumber.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    isFuture -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    isToday -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                },
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
