package com.arshadshah.nimaz.ui.components.ui.prayerTimes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun PrayerTimesListUI(modifier: Modifier = Modifier, prayerTimesMap: Map<String, LocalDateTime?>) {
    //the UI for the prayer times list
    //padding for the list and rounded corners
    //a drop shadow is added to the list
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(0.dp, 8.dp)
            .shadow(5.dp, shape = MaterialTheme.shapes.medium, clip = true)
    ) {
        LazyColumn {
            items(prayerTimesMap.size) {
                Column {
                    //if the element is first then dont add a divider else add a divider on top
                    if (it != 0) {
                        Divider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    PrayerTimesRow(
                        prayerName = prayerTimesMap.keys.elementAt(it),
                        prayerTime = prayerTimesMap.values.elementAt(it)
                    )
                }
            }
        }
    }
}

//the row for the prayer times
@Composable
fun PrayerTimesRow(prayerName: String, prayerTime: LocalDateTime?) {
    //format the date to time based on device format
    val formatter = DateTimeFormatter.ofPattern("hh:mm a")
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(text = prayerName, fontSize = 20.sp, modifier = Modifier.padding(10.dp))
        Text(
            text = prayerTime!!.format(formatter),
            fontSize = 20.sp,
            modifier = Modifier.padding(10.dp)
        )
    }
}
