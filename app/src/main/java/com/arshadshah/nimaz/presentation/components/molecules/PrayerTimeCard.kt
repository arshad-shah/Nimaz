package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.PrayerColorBar
import com.arshadshah.nimaz.presentation.components.atoms.PrayerStatus
import com.arshadshah.nimaz.presentation.components.atoms.StatusIndicator
import com.arshadshah.nimaz.presentation.components.atoms.getPrayerColor
import com.arshadshah.nimaz.presentation.components.atoms.getPrayerGradientEnd
import androidx.compose.ui.tooling.preview.Preview

/**
 * Single prayer time display card.
 */
@Composable
fun PrayerTimeCard(
    prayerType: PrayerType,
    prayerTime: String,
    modifier: Modifier = Modifier,
    isPassed: Boolean = false,
    isCurrent: Boolean = false,
    isNext: Boolean = false,
    status: PrayerStatus? = null,
    notificationEnabled: Boolean = true,
    onNotificationToggle: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val prayerColor = getPrayerColor(prayerType)
    val backgroundColor = when {
        isCurrent -> prayerColor.copy(alpha = 0.15f)
        isNext -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        isPassed -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color indicator bar
            PrayerColorBar(
                prayerType = prayerType,
                height = 48.dp,
                width = 4.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Prayer name and status
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = prayerType.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isCurrent || isNext) FontWeight.Bold else FontWeight.Medium,
                        color = if (isPassed) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    if (isCurrent) {
                        Spacer(modifier = Modifier.width(8.dp))
                        CurrentPrayerBadge()
                    } else if (isNext) {
                        Spacer(modifier = Modifier.width(8.dp))
                        NextPrayerBadge()
                    }
                }

                if (status != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    StatusIndicator(status = status, showLabel = true)
                }
            }

            // Time display
            Text(
                text = prayerTime,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isCurrent) prayerColor else {
                    if (isPassed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.onSurface
                }
            )

            // Notification toggle
            if (onNotificationToggle != null) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { onNotificationToggle(!notificationEnabled) }) {
                    Icon(
                        imageVector = if (notificationEnabled) {
                            Icons.Default.Notifications
                        } else {
                            Icons.Default.NotificationsOff
                        },
                        contentDescription = if (notificationEnabled) "Disable notification" else "Enable notification",
                        tint = if (notificationEnabled) prayerColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Current prayer indicator badge.
 */
@Composable
private fun CurrentPrayerBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = "NOW",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Next prayer indicator badge.
 */
@Composable
private fun NextPrayerBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.secondary)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = "NEXT",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondary,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Compact prayer time row for lists.
 */
@Composable
fun CompactPrayerTimeRow(
    prayerType: PrayerType,
    prayerTime: String,
    modifier: Modifier = Modifier,
    isPassed: Boolean = false,
    prayed: Boolean = false
) {
    val prayerColor = getPrayerColor(prayerType)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(prayerColor)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = prayerType.displayName,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isPassed) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = prayerTime,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (isPassed) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            if (prayed) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Prayed",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Featured current prayer card with gradient background.
 */
@Composable
fun FeaturedPrayerCard(
    prayerType: PrayerType,
    prayerTime: String,
    timeUntilNext: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val gradientColors = listOf(
        getPrayerColor(prayerType),
        getPrayerGradientEnd(prayerType)
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(gradientColors))
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "Current Prayer",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = prayerType.displayName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "Started at",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = prayerTime,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Time remaining",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = timeUntilNext,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}


@Preview(showBackground = true, name = "Prayer Time Card")
@Composable
private fun PrayerTimeCardPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            PrayerTimeCard(
                prayerType = PrayerType.FAJR,
                prayerTime = "5:30 AM",
                isCurrent = true
            )
        }
    }
}

@Preview(showBackground = true, name = "Prayer Time Cards States")
@Composable
private fun PrayerTimeCardsStatesPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PrayerTimeCard(prayerType = PrayerType.FAJR, prayerTime = "5:30 AM", isPassed = true, status = PrayerStatus.PRAYED)
            PrayerTimeCard(prayerType = PrayerType.DHUHR, prayerTime = "12:30 PM", isCurrent = true)
            PrayerTimeCard(prayerType = PrayerType.ASR, prayerTime = "3:45 PM", isNext = true)
            PrayerTimeCard(prayerType = PrayerType.MAGHRIB, prayerTime = "6:15 PM")
        }
    }
}

@Preview(showBackground = true, name = "Compact Prayer Row")
@Composable
private fun CompactPrayerTimeRowPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            CompactPrayerTimeRow(prayerType = PrayerType.FAJR, prayerTime = "5:30 AM", prayed = true)
            CompactPrayerTimeRow(prayerType = PrayerType.DHUHR, prayerTime = "12:30 PM")
            CompactPrayerTimeRow(prayerType = PrayerType.ASR, prayerTime = "3:45 PM", isPassed = true)
        }
    }
}

@Preview(showBackground = true, name = "Featured Prayer Card")
@Composable
private fun FeaturedPrayerCardPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            FeaturedPrayerCard(
                prayerType = PrayerType.DHUHR,
                prayerTime = "12:30 PM",
                timeUntilNext = "2h 15m"
            )
        }
    }
}
