package com.arshadshah.nimaz.ui.components.dashboard

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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
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
    fajrPrayerTime: LocalDateTime,
    maghribPrayerTime: LocalDateTime
) {

    val context = LocalContext.current

    //a card that shows the time left for ramadan
    //it should only show when 40 days are left for ramadan
    //it should show the time left for ramadan in days, hours, minutes and seconds
    val ramadanTimeLeft = remember { mutableLongStateOf(0L) }

    val today = LocalDate.now()
    val todayHijri = HijrahDate.from(today)
    val ramadanStart = HijrahDate.of(todayHijri[ChronoField.YEAR], 9, 1)
    val ramadanEnd = HijrahDate.of(todayHijri[ChronoField.YEAR], 9, 29)

    val isAfterRamadanStart = todayHijri.isAfter(ramadanStart)
    if (isAfterRamadanStart) {
        if (todayHijri.isBefore(ramadanEnd)) {
            ramadanTimeLeft.longValue = ramadanEnd.toEpochDay() - todayHijri.toEpochDay()
        }
    } else {
        val diff = ramadanStart.toEpochDay() - todayHijri.toEpochDay()
        ramadanTimeLeft.longValue = diff
    }

    //show card if it is the month of ramadan
    val showCard =
        todayHijri[ChronoField.MONTH_OF_YEAR] == 9 && todayHijri[ChronoField.DAY_OF_MONTH] <= 29 || isFasting

    //is ramadan time left less than 40 days
    //if yes then show the card
    if (showCard) {
        //show the card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(16.dp),
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        modifier = Modifier.padding(start= 4.dp),
                        text = "Fasting Times Today",
                        style = MaterialTheme.typography.titleMedium
                    )
                    IconButton(
                        modifier = Modifier.size(32.dp),
                        onClick = {
                            //share the aya
                            val shareIntent = Intent(Intent.ACTION_SEND)
                            shareIntent.type = "text/plain"
                            //create the share message
                            //with the aya text, aya translation
                            //the sura number followed by the aya number
                            shareIntent.putExtra(
                                Intent.EXTRA_TEXT,
                                "Ramadan Fasting Times for $location \n${
                                    DateTimeFormatter.ofPattern(
                                        "EEEE, d MMMM yyyy"
                                    ).format(today)
                                } \n" +
                                        "Imsak (Fajr): ${
                                            DateTimeFormatter.ofPattern("hh:mm a")
                                                .format(fajrPrayerTime)
                                        } \n" +
                                        "Iftar (Maghrib): ${
                                            DateTimeFormatter.ofPattern("hh:mm a")
                                                .format(maghribPrayerTime)
                                        } \n" +
                                        "Times are Provided by Nimaz : https://play.google.com/store/apps/details?id=com.arshadshah.nimaz"
                            )
                            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Ramadan Times")
                            shareIntent.putExtra(Intent.EXTRA_TITLE, "Ramadan Times")

                            //start the share intent
                            context.startActivity(
                                Intent.createChooser(
                                    shareIntent,
                                    "Share Ramadan Times"
                                )
                            )
                        }) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            painter = painterResource(id = R.drawable.share_icon),
                            contentDescription = "Share Ramadan Times",
                        )
                    }
                }

                val deviceTimeFormat =
                    DateFormat.is24HourFormat(LocalContext.current)
                //if the device time format is 24 hour then use the 24 hour format
                val formatter = if (deviceTimeFormat) {
                    DateTimeFormatter.ofPattern("HH:mm")
                } else {
                    DateTimeFormatter.ofPattern("hh:mm a")
                }

                Row(
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        TimeComponent(
                            title = "Fajr (Imsak)",
                            time = formatter.format(fajrPrayerTime)
                        )
                        TimeComponent(
                            title = "Maghrib (Iftar)",
                            time = formatter.format(maghribPrayerTime)
                        )
                    }
                }
            }
        }
    }
}

//compoennt to show the fajr time with a label
@Composable
fun TimeComponent(title: String = "Suhoor Time", time: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        Text(text = time, style = MaterialTheme.typography.titleLarge)
    }
}
