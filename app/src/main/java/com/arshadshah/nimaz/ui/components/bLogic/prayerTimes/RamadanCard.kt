package com.arshadshah.nimaz.ui.components.bLogic.prayerTimes

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField

@Composable
fun RamadanCard() {
	//a card that shows the time left for ramadan
	//it should only show when 40 days are left for ramadan
	//it should show the time left for ramadan in days, hours, minutes and seconds
	val ramadanTimeLeft = remember { mutableStateOf(0L) }

	val today = LocalDate.now()
	val todayHijri = HijrahDate.from(today)
	val ramadanStart = HijrahDate.of(todayHijri[ChronoField.YEAR], 9, 1)
	//get date of ramadan start in gregorian
	val ramadanStartGregorian = LocalDate.from(ramadanStart)
	//format the date to show day and month and year
	//wednesday, 1st of september 2021
	val formatter = java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")
	val ramadanStartFormatted = ramadanStartGregorian.format(formatter)

	val isAfterRamadanStart = todayHijri.isAfter(ramadanStart)
	if (isAfterRamadanStart) {
		val ramadanEnd = HijrahDate.of(todayHijri[ChronoField.YEAR], 10, 1)
		if (todayHijri.isBefore(ramadanEnd)) {
			ramadanTimeLeft.value = ramadanEnd.toEpochDay() - todayHijri.toEpochDay()
		}
	}else{
		ramadanTimeLeft.value = ramadanStart.toEpochDay() - todayHijri.toEpochDay()
	}
	//list of images to pick from
	//we will pick a random image from the list
	val imagesToShow = listOf(R.drawable.ramadan, R.drawable.ramadan2, R.drawable.ramadan3, R.drawable.ramadan4)
	//pick a random image
	val randomImage = imagesToShow.random()

	//is ramadan time left less than 40 days
	//if yes then show the card
	if (ramadanTimeLeft.value < 40) {
		//show the card
		ElevatedCard(
				modifier = Modifier
					.fillMaxWidth()
					.padding(16.dp),
					) {
			Column(
					modifier = Modifier.padding(16.dp) ,
					verticalArrangement = Arrangement.Center ,
					horizontalAlignment = Alignment.CenterHorizontally
				  ) {
				Text(text ="Ramadan is coming soon", style = MaterialTheme.typography.titleLarge)
				Row(
						modifier = Modifier.fillMaxWidth().padding(top = 16.dp) ,
						verticalAlignment = Alignment.CenterVertically,
						horizontalArrangement = Arrangement.SpaceBetween
				   ) {
					Box(
							modifier = Modifier
								.clip(MaterialTheme.shapes.extraLarge)
								.padding(8.dp)
								.size(80.dp)
					   ) {
						Image(
								painter = painterResource(id = randomImage) ,
								contentDescription = "Moon" ,
								modifier = Modifier
									.size(80.dp)
							 )
					}
					Column(
							modifier = Modifier.fillMaxWidth() ,
							verticalArrangement = Arrangement.Center ,
							horizontalAlignment = Alignment.CenterHorizontally
						  ) {
						//estimated start
						Text(text = "Estimated start")
						Text(text = "-${ramadanTimeLeft.value} days", style = MaterialTheme.typography.headlineMedium)
						//todays date
						Text(text = ramadanStartFormatted.toString(), style = MaterialTheme.typography.titleSmall)
					}
				}
			}
		}
	}
}

@Preview
@Composable
fun RamadanCardPreview() {
	RamadanCard()
}