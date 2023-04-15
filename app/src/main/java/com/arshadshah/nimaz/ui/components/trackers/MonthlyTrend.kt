package com.arshadshah.nimaz.ui.components.trackers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.data.remote.models.PrayerTracker
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun PrayerTrackerGrid(prayerTrackerList: List<PrayerTracker>) {
	val currentDate = LocalDate.now()
	val yearMonth = YearMonth.of(currentDate.year , currentDate.month)
	val daysInMonth = yearMonth.lengthOfMonth()
	val prayers = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")

	LazyColumn(
			modifier = Modifier.fillMaxWidth() ,
			verticalArrangement = Arrangement.Center ,
			horizontalAlignment = Alignment.Start
			  ) {
		items(prayers.size) { prayer ->
			Row(
					modifier = Modifier
						.fillMaxWidth(),
					horizontalArrangement = Arrangement.SpaceBetween,
					verticalAlignment = Alignment.CenterVertically
			   ) {
				// Render the name of the prayer on the left
				Text(
						text = prayers[prayer] ,
						style = MaterialTheme.typography.labelSmall ,
						modifier = Modifier.padding(end = 8.dp)
					)
				// Render the small boxes (dots) for each day of the month
				for (i in 0 until daysInMonth) {
					val date = yearMonth.atDay(i + 1)
					val prayerTracker = prayerTrackerList.find { it.date == date.toString() }
					Box(
							modifier = Modifier
								.size(8.dp)
								.background(
										color = if (prayerTracker != null && prayerTracker.isPrayerCompleted(prayers[prayer])) Color.Green else Color.Gray ,
										shape = CircleShape
										   )
					   )
				}
			}
		}
	}
}

// Extension function to check if a prayer is completed for a specific day
fun PrayerTracker.isPrayerCompleted(prayer: String): Boolean {
	return when (prayer) {
		"Fajr" -> fajr
		"Dhuhr" -> dhuhr
		"Asr" -> asr
		"Maghrib" -> maghrib
		"Isha" -> isha
		else -> false
	}
}


@Preview(device = "id:S20 Fe" , showSystemUi = true , showBackground = true)
@Composable
fun PrayerTrackerGridPreview() {
	val prayerTrackerList = listOf(
			PrayerTracker(
					date = "2021-09-01" ,
					fajr = true ,
					dhuhr = true ,
					asr = true ,
					maghrib = true ,
					isha = true
					 ) ,
			PrayerTracker(
					date = "2021-09-02" ,
					fajr = true ,
					dhuhr = true ,
					asr = true ,
					maghrib = true ,
					isha = true
					 ) ,
			PrayerTracker(
					date = "2021-09-03" ,
					fajr = true ,
					dhuhr = true ,
					asr = true ,
					maghrib = true ,
					isha = true
					 ) ,
			PrayerTracker(
					date = "2021-09-04" ,
					fajr = true ,
					dhuhr = true ,
					asr = true ,
					maghrib = true ,
					isha = true
					 ) ,
			PrayerTracker(
					date = "2021-09-05" ,
					fajr = true ,
					dhuhr = true ,
					asr = true ,
					maghrib = true ,
					isha = true
					 ) ,
			PrayerTracker(
					date = "2021-09-06" ,
					fajr = true ,
					dhuhr = true ,
					asr = true ,
					maghrib = true ,
					isha = true
					 ) ,
			PrayerTracker(
					date = "2021-09-07" ,
					fajr = true ,
					dhuhr = true ,
					asr = true ,
					maghrib = true ,
					isha = true
					 ) ,
			PrayerTracker(
					date = "2021-09-08" ,
					fajr = true ,
					dhuhr = true ,
					asr = true ,
					maghrib = true ,
					isha = true
					 ) ,
			PrayerTracker(
					date = "2021-09-09" ,
					fajr = true ,
					dhuhr = true ,
					asr = true ,
					maghrib = true ,
					isha = true
					 )
			 )
	PrayerTrackerGrid(prayerTrackerList = prayerTrackerList)
}