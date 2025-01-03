package com.arshadshah.nimaz.ui.components.dashboard

import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
    maghribTime: LocalDateTime
) {
    val context = LocalContext.current
    val today = LocalDate.now()
    val todayHijri = HijrahDate.from(today)
    val isRamadan = todayHijri[ChronoField.MONTH_OF_YEAR] == 9 &&
            todayHijri[ChronoField.DAY_OF_MONTH] <= 29

    if (isRamadan || isFasting) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Fasting Times Today",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    IconButton(
                        onClick = {
                            shareRamadanTimes(context, location, today, fajrTime, maghribTime)
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.share_icon),
                            contentDescription = "Share Times",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                val timeFormat = if (DateFormat.is24HourFormat(context)) "HH:mm" else "hh:mm a"
                val formatter = DateTimeFormatter.ofPattern(timeFormat)

                FastingTimeRow(
                    title = "Fajr (Imsak)",
                    time = formatter.format(fajrTime),
                    iconId = R.drawable.fajr_icon
                )

                FastingTimeRow(
                    title = "Maghrib (Iftar)",
                    time = formatter.format(maghribTime),
                    iconId = R.drawable.maghrib_icon
                )
            }
        }
    }
}

@Composable
private fun FastingTimeRow(
    title: String,
    time: String,
    iconId: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(iconId),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Text(
            text = time,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
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