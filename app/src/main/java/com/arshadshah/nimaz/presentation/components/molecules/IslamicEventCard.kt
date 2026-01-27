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
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.core.util.HijriDateCalculator
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.ContainedIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconContainerShape
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.theme.NimazColors

/**
 * Get icon and color for event type.
 */
@Composable
private fun getEventTypeDetails(eventType: HijriDateCalculator.EventType): Pair<ImageVector, Color> {
    return when (eventType) {
        HijriDateCalculator.EventType.EID -> Icons.Default.Celebration to MaterialTheme.colorScheme.secondary
        HijriDateCalculator.EventType.HOLIDAY -> Icons.Default.Star to MaterialTheme.colorScheme.primary
        HijriDateCalculator.EventType.RAMADAN -> Icons.Default.Nightlight to NimazColors.PrayerColors.Isha
        HijriDateCalculator.EventType.SPECIAL_NIGHT -> Icons.Default.Nightlight to MaterialTheme.colorScheme.tertiary
        HijriDateCalculator.EventType.RECOMMENDED_FAST -> Icons.Default.Restaurant to NimazColors.FastingColors.Fasted
        HijriDateCalculator.EventType.COMMEMORATION -> Icons.Default.Event to MaterialTheme.colorScheme.primary
    }
}

/**
 * Islamic event card for upcoming events display.
 */
@Composable
fun IslamicEventCard(
    eventName: String,
    eventNameArabic: String,
    eventType: HijriDateCalculator.EventType,
    hijriDate: String,
    gregorianDate: String,
    modifier: Modifier = Modifier,
    daysUntil: Int? = null,
    description: String? = null,
    onClick: (() -> Unit)? = null
) {
    val (icon, color) = getEventTypeDetails(eventType)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Event type icon
            ContainedIcon(
                imageVector = icon,
                size = NimazIconSize.LARGE,
                containerShape = NimazIconContainerShape.ROUNDED_SQUARE,
                backgroundColor = color.copy(alpha = 0.15f),
                iconColor = color
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = eventName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        ArabicText(
                            text = eventNameArabic,
                            size = ArabicTextSize.SMALL,
                            color = color
                        )
                    }

                    if (daysUntil != null) {
                        DaysUntilBadge(daysUntil = daysUntil, color = color)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Dates
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NimazBadge(
                        text = hijriDate,
                        backgroundColor = color.copy(alpha = 0.15f),
                        textColor = color,
                        size = NimazBadgeSize.SMALL
                    )
                    Text(
                        text = gregorianDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Description
                if (description != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Islamic event card that accepts an IslamicEvent domain model.
 */
@Composable
fun IslamicEventCard(
    event: com.arshadshah.nimaz.domain.model.IslamicEvent,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val eventType = when (event.eventType) {
        com.arshadshah.nimaz.domain.model.IslamicEventType.HOLIDAY -> HijriDateCalculator.EventType.HOLIDAY
        com.arshadshah.nimaz.domain.model.IslamicEventType.FAST -> HijriDateCalculator.EventType.RECOMMENDED_FAST
        com.arshadshah.nimaz.domain.model.IslamicEventType.NIGHT -> HijriDateCalculator.EventType.SPECIAL_NIGHT
        com.arshadshah.nimaz.domain.model.IslamicEventType.HISTORICAL -> HijriDateCalculator.EventType.COMMEMORATION
    }

    val hijriDate = "${event.hijriDay} ${com.arshadshah.nimaz.domain.model.HijriMonth.fromNumber(event.hijriMonth)?.displayName() ?: ""}"
    val gregorianDateStr = event.gregorianDate?.let {
        "${it.dayOfMonth} ${it.month.name.lowercase().replaceFirstChar { c -> c.uppercase() }} ${it.year}"
    } ?: ""

    IslamicEventCard(
        eventName = event.nameEnglish,
        eventNameArabic = event.nameArabic,
        eventType = eventType,
        hijriDate = hijriDate,
        gregorianDate = gregorianDateStr,
        modifier = modifier,
        description = event.description,
        onClick = onClick
    )
}

/**
 * Days until event badge.
 */
@Composable
private fun DaysUntilBadge(
    daysUntil: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val text = when (daysUntil) {
        0 -> "Today"
        1 -> "Tomorrow"
        else -> "$daysUntil days"
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

/**
 * Compact event list item.
 */
@Composable
fun CompactEventItem(
    eventName: String,
    eventType: HijriDateCalculator.EventType,
    date: String,
    modifier: Modifier = Modifier,
    daysUntil: Int? = null,
    onClick: (() -> Unit)? = null
) {
    val (icon, color) = getEventTypeDetails(eventType)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = eventName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = date,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (daysUntil != null) {
            Text(
                text = when (daysUntil) {
                    0 -> "Today"
                    1 -> "Tomorrow"
                    else -> "$daysUntil days"
                },
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Featured event card for home screen (Eid, Ramadan).
 */
@Composable
fun FeaturedEventCard(
    eventName: String,
    eventNameArabic: String,
    eventType: HijriDateCalculator.EventType,
    daysUntil: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val (icon, color) = getEventTypeDetails(eventType)
    val gradientColors = listOf(color, color.copy(alpha = 0.7f))

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "Upcoming",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = eventName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        ArabicText(
                            text = eventNameArabic,
                            size = ArabicTextSize.MEDIUM,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }

                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = daysUntil.toString(),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (daysUntil == 1) "day away" else "days away",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

/**
 * Ramadan countdown card.
 */
@Composable
fun RamadanCountdownCard(
    daysUntil: Int,
    modifier: Modifier = Modifier,
    isRamadan: Boolean = false,
    daysRemaining: Int = 0,
    onClick: (() -> Unit)? = null
) {
    val color = NimazColors.PrayerColors.Isha

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Nightlight,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isRamadan) {
                Text(
                    text = "Ramadan Mubarak",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = "$daysRemaining days remaining",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Ramadan in",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$daysUntil days",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
    }
}
