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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.tooling.preview.Preview
import com.arshadshah.nimaz.presentation.theme.NimazTheme

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

@Preview(showBackground = true, name = "Islamic Event Card - Eid")
@Composable
private fun IslamicEventCardEidPreview() {
    NimazTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            IslamicEventCard(
                eventName = "Eid al-Fitr",
                eventNameArabic = "عيد الفطر",
                eventType = HijriDateCalculator.EventType.EID,
                hijriDate = "1 Shawwal 1447",
                gregorianDate = "30 March 2026",
                daysUntil = 58,
                description = "Festival of Breaking the Fast, marking the end of Ramadan.",
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Islamic Event Card - Ramadan")
@Composable
private fun IslamicEventCardRamadanPreview() {
    NimazTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            IslamicEventCard(
                eventName = "Ramadan Begins",
                eventNameArabic = "بداية رمضان",
                eventType = HijriDateCalculator.EventType.RAMADAN,
                hijriDate = "1 Ramadan 1447",
                gregorianDate = "1 March 2026",
                daysUntil = 29
            )
        }
    }
}

@Preview(showBackground = true, name = "Islamic Event Card - Today")
@Composable
private fun IslamicEventCardTodayPreview() {
    NimazTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            IslamicEventCard(
                eventName = "Laylat al-Qadr",
                eventNameArabic = "ليلة القدر",
                eventType = HijriDateCalculator.EventType.SPECIAL_NIGHT,
                hijriDate = "27 Ramadan 1447",
                gregorianDate = "27 March 2026",
                daysUntil = 0,
                description = "The Night of Decree, better than a thousand months."
            )
        }
    }
}
