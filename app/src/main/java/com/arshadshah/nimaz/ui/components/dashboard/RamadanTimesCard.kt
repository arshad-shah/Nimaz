package com.arshadshah.nimaz.ui.components.dashboard

import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField

@Composable
fun RamadanTimesCard(
    isFasting: Boolean,
    location: String,
    fajrTime: LocalDateTime,
    maghribTime: LocalDateTime,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val today = LocalDate.now()
    val todayHijri = HijrahDate.from(today)
    val isRamadan = todayHijri[ChronoField.MONTH_OF_YEAR] == 9 &&
            todayHijri[ChronoField.DAY_OF_MONTH] <= 29

    if (isRamadan || isFasting) {
        ElevatedCard(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = 4.dp,
                pressedElevation = 8.dp
            ),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Fasting Times Today",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    FilledIconButton(
                        onClick = {
                            shareRamadanTimes(
                                context,
                                location,
                                today,
                                fajrTime,
                                maghribTime
                            )
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.share_icon),
                            contentDescription = "Share",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val timeFormat =
                            if (DateFormat.is24HourFormat(context)) "HH:mm" else "hh:mm a"
                        val formatter = DateTimeFormatter.ofPattern(timeFormat)

                        FastingTimeRow(
                            title = "Fajr (Imsak)",
                            time = formatter.format(fajrTime),
                            icon = Icons.Default.NightsStay,
                            color = MaterialTheme.colorScheme.primary
                        )

                        FastingTimeRow(
                            title = "Maghrib (Iftar)",
                            time = formatter.format(maghribTime),
                            icon = Icons.Default.WbTwilight,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FastingTimeRow(
    title: String,
    time: String,
    icon: ImageVector,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = color.copy(alpha = 0.2f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = color.copy(alpha = 0.1f)
        ) {
            Text(
                text = time,
                style = MaterialTheme.typography.bodyMedium,
                color = color,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

private fun shareRamadanTimes(
    context: Context,
    location: String,
    date: LocalDate,
    fajrTime: LocalDateTime,
    maghribTime: LocalDateTime
) {
    val shareText = buildString {
        append("Ramadan Fasting Times for $location\n")
        append(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy").format(date))
        append("\nImsak (Fajr): ${DateTimeFormatter.ofPattern("hh:mm a").format(fajrTime)}")
        append("\nIftar (Maghrib): ${DateTimeFormatter.ofPattern("hh:mm a").format(maghribTime)}")
        append("\nTimes provided by Nimaz: https://play.google.com/store/apps/details?id=com.arshadshah.nimaz")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
        putExtra(Intent.EXTRA_SUBJECT, "Ramadan Times")
        putExtra(Intent.EXTRA_TITLE, "Ramadan Times")
    }

    context.startActivity(Intent.createChooser(intent, "Share Ramadan Times"))
}