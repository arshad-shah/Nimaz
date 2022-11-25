package com.arshadshah.nimaz.ui.components.ui.prayerTimes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Composable
fun PrayerTimesListUI(
    modifier: Modifier = Modifier,
    prayerTimesMap: Map<String, LocalDateTime?>,
    name: String
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(5.dp, shape = MaterialTheme.shapes.medium, clip = true)
    ) {
        Column {
            //iterate over the map
            for ((key, value) in prayerTimesMap) {
                //if the element is first then dont add a divider else add a divider on top
                if (key != prayerTimesMap.keys.first()) {
                    Divider(
                        modifier = Modifier.fillMaxWidth(),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                //check if the row is to be highlighted
                val isHighlighted = getHighlightRow(name, key)
                PrayerTimesRow(
                    prayerName = key,
                    prayerTime = value,
                    isHighlighted = isHighlighted
                )
            }
        }
    }
}

//the row for the prayer times
@Composable
fun PrayerTimesRow(prayerName: String, prayerTime: LocalDateTime?, isHighlighted: Boolean) {
    //format the date to time based on device format
    val formatter = DateTimeFormatter.ofPattern("hh:mm a")
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = if (isHighlighted) {
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
        } else {
            Modifier
                .fillMaxWidth()
        }
    ) {
        Text(text = prayerName, fontSize = 20.sp, modifier = Modifier.padding(16.dp))
        Text(
            text = prayerTime!!.format(formatter),
            fontSize = 20.sp,
            modifier = Modifier.padding(16.dp)
        )
    }
}

//fnction to figure out which row to highlight
fun getHighlightRow(prayerName: String, name: String): Boolean {
    //convert to lower case
    val prayerNameLower = name.uppercase(Locale.ROOT)
    return prayerName == prayerNameLower
}


@Preview
@Composable
fun PrayerTimesListUIPreview() {
    PrayerTimesListUI(
        prayerTimesMap = mapOf(
            "FAJR" to LocalDateTime.now(),
            "SUNRISE" to LocalDateTime.now(),
            "DHUHR" to LocalDateTime.now(),
            "ASR" to LocalDateTime.now(),
            "MAGHRIB" to LocalDateTime.now(),
            "ISHA" to LocalDateTime.now()
        ),
        name = "FAJR"
    )
}