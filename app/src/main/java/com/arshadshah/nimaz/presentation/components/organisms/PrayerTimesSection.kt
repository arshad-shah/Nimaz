package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.presentation.components.atoms.PrayerStatus
import com.arshadshah.nimaz.presentation.components.atoms.getPrayerColor
import com.arshadshah.nimaz.presentation.components.molecules.CountdownTimer
import com.arshadshah.nimaz.presentation.components.molecules.PrayerTimeCard
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Data class representing a single prayer time entry.
 */
data class PrayerTimeEntry(
    val type: PrayerType,
    val time: LocalDateTime,
    val status: PrayerStatus = PrayerStatus.PENDING,
    val isNotificationEnabled: Boolean = true,
    val isAdhanEnabled: Boolean = false
)

/**
 * Complete prayer times section with all five daily prayers.
 */
@Composable
fun PrayerTimesSection(
    prayers: List<PrayerTimeEntry>,
    currentPrayer: PrayerType?,
    nextPrayer: PrayerType,
    nextPrayerTime: LocalDateTime,
    modifier: Modifier = Modifier,
    locationName: String? = null,
    showCountdown: Boolean = true,
    onPrayerClick: (PrayerType) -> Unit = {},
    onNotificationToggle: (PrayerType, Boolean) -> Unit = { _, _ -> },
    onRefreshClick: (() -> Unit)? = null
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("hh:mm a") }

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
                .padding(16.dp)
        ) {
            // Header with location
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Prayer Times",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (locationName != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.height(16.dp)
                            )
                            Text(
                                text = locationName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (onRefreshClick != null) {
                    IconButton(onClick = onRefreshClick) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh prayer times",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Countdown to next prayer
            if (showCountdown) {
                Spacer(modifier = Modifier.height(16.dp))
                CountdownTimer(
                    targetTimeMillis = nextPrayerTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    label = "Until ${nextPrayer.displayName}"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Prayer times list
            prayers.forEach { prayer ->
                val isCurrent = prayer.type == currentPrayer
                val isNext = prayer.type == nextPrayer
                val isPassed = prayer.time.isBefore(LocalDateTime.now()) && !isCurrent

                PrayerTimeCard(
                    prayerType = prayer.type,
                    prayerTime = prayer.time.format(timeFormatter),
                    status = prayer.status,
                    isCurrent = isCurrent,
                    isNext = isNext,
                    isPassed = isPassed,
                    notificationEnabled = prayer.isNotificationEnabled,
                    onClick = { onPrayerClick(prayer.type) },
                    onNotificationToggle = { enabled ->
                        onNotificationToggle(prayer.type, enabled)
                    },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Compact prayer times list without card wrapper.
 */
@Composable
fun PrayerTimesList(
    prayers: List<PrayerTimeEntry>,
    currentPrayer: PrayerType?,
    nextPrayer: PrayerType,
    modifier: Modifier = Modifier,
    onPrayerClick: (PrayerType) -> Unit = {}
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("hh:mm a") }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        prayers.forEach { prayer ->
            val isCurrent = prayer.type == currentPrayer
            val isNext = prayer.type == nextPrayer
            val isPassed = prayer.time.isBefore(LocalDateTime.now()) && !isCurrent

            PrayerTimeCard(
                prayerType = prayer.type,
                prayerTime = prayer.time.format(timeFormatter),
                status = prayer.status,
                isCurrent = isCurrent,
                isNext = isNext,
                isPassed = isPassed,
                notificationEnabled = prayer.isNotificationEnabled,
                onClick = { onPrayerClick(prayer.type) }
            )
        }
    }
}

/**
 * Expandable prayer times section with toggle.
 */
@Composable
fun ExpandablePrayerTimesSection(
    prayers: List<PrayerTimeEntry>,
    currentPrayer: PrayerType?,
    nextPrayer: PrayerType,
    nextPrayerTime: LocalDateTime,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    onPrayerClick: (PrayerType) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("hh:mm a") }

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
            // Header with next prayer info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Next: ${nextPrayer.displayName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = getPrayerColor(nextPrayer)
                    )
                    Text(
                        text = nextPrayerTime.format(timeFormatter),
                        style = MaterialTheme.typography.bodyLarge,
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

            // Expandable list
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    prayers.forEach { prayer ->
                        val isCurrent = prayer.type == currentPrayer
                        val isNext = prayer.type == nextPrayer
                        val isPassed = prayer.time.isBefore(LocalDateTime.now()) && !isCurrent

                        PrayerTimeCard(
                            prayerType = prayer.type,
                            prayerTime = prayer.time.format(timeFormatter),
                            status = prayer.status,
                            isCurrent = isCurrent,
                            isNext = isNext,
                            isPassed = isPassed,
                            notificationEnabled = prayer.isNotificationEnabled,
                            onClick = { onPrayerClick(prayer.type) },
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Mini prayer times display for widgets or compact views.
 */
@Composable
fun MiniPrayerTimesDisplay(
    prayers: List<PrayerTimeEntry>,
    currentPrayer: PrayerType?,
    modifier: Modifier = Modifier
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        prayers.filter { it.type != PrayerType.SUNRISE }.take(5).forEach { prayer ->
            val isCurrent = prayer.type == currentPrayer

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = prayer.type.displayName.take(3),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isCurrent) {
                        getPrayerColor(prayer.type)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = prayer.time.format(timeFormatter),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCurrent) {
                        getPrayerColor(prayer.type)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

// Previews
@Preview(showBackground = true)
@Composable
private fun PrayerTimesSectionPreview() {
    NimazTheme {
        val now = LocalDateTime.now()
        val prayers = listOf(
            PrayerTimeEntry(PrayerType.FAJR, now.withHour(5).withMinute(30), PrayerStatus.PRAYED),
            PrayerTimeEntry(PrayerType.DHUHR, now.withHour(12).withMinute(45), PrayerStatus.PENDING),
            PrayerTimeEntry(PrayerType.ASR, now.withHour(15).withMinute(30), PrayerStatus.PENDING),
            PrayerTimeEntry(PrayerType.MAGHRIB, now.withHour(18).withMinute(15), PrayerStatus.PENDING),
            PrayerTimeEntry(PrayerType.ISHA, now.withHour(19).withMinute(45), PrayerStatus.PENDING)
        )
        PrayerTimesSection(
            prayers = prayers,
            currentPrayer = PrayerType.DHUHR,
            nextPrayer = PrayerType.ASR,
            nextPrayerTime = now.withHour(15).withMinute(30),
            locationName = "Dublin, Ireland",
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MiniPrayerTimesDisplayPreview() {
    NimazTheme {
        val now = LocalDateTime.now()
        val prayers = listOf(
            PrayerTimeEntry(PrayerType.FAJR, now.withHour(5).withMinute(30)),
            PrayerTimeEntry(PrayerType.DHUHR, now.withHour(12).withMinute(45)),
            PrayerTimeEntry(PrayerType.ASR, now.withHour(15).withMinute(30)),
            PrayerTimeEntry(PrayerType.MAGHRIB, now.withHour(18).withMinute(15)),
            PrayerTimeEntry(PrayerType.ISHA, now.withHour(19).withMinute(45))
        )
        MiniPrayerTimesDisplay(
            prayers = prayers,
            currentPrayer = PrayerType.DHUHR,
            modifier = Modifier.padding(16.dp)
        )
    }
}
