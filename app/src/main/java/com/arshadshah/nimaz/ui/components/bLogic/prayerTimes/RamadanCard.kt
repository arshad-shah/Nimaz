package com.arshadshah.nimaz.ui.components.bLogic.prayerTimes

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import com.arshadshah.nimaz.ui.theme.NimazTheme
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField

@Composable
fun RamadanCard(onNavigateToCalender : () -> Unit)
{
	//a card that shows the time left for ramadan
	//it should only show when 40 days are left for ramadan
	//it should show the time left for ramadan in days, hours, minutes and seconds
	val ramadanTimeLeft = remember { mutableStateOf(0L) }

	val today = LocalDate.now()
	val todayHijri = HijrahDate.from(today)
	val ramadanStart = HijrahDate.of(todayHijri[ChronoField.YEAR] , 9 , 1)
	val ramadanEnd = HijrahDate.of(todayHijri[ChronoField.YEAR] , 9 , 29)
	//get date of ramadan start in gregorian
	val ramadanStartGregorian = LocalDate.from(ramadanStart)
	val ramadanEndGregorian = LocalDate.from(ramadanEnd)
	//format the date to show day and month and year
	//wednesday, 1st of september 2021
	val formatter = java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")
	val ramadanStartFormatted = ramadanStartGregorian.format(formatter)
	val ramadanEndFormatted = ramadanEndGregorian.format(formatter)

	val isAfterRamadanStart = todayHijri.isAfter(ramadanStart)
	if (isAfterRamadanStart)
	{
		if (todayHijri.isBefore(ramadanEnd))
		{
			ramadanTimeLeft.value = ramadanEnd.toEpochDay() - todayHijri.toEpochDay()
		}
	} else
	{
		val diff = ramadanStart.toEpochDay() - todayHijri.toEpochDay()
		ramadanTimeLeft.value = diff
	}

	//list of images to pick from
	//we will pick a random image from the list
	val imagesToShow =
		listOf(
				R.drawable.ramadan ,
				R.drawable.ramadan2 ,
				R.drawable.ramadan3 ,
				R.drawable.ramadan4 ,
				R.drawable.ramadan5
			  )
	//pick a random image
	val randomImage = imagesToShow.random()
	//save the image to show in the card
	val imageToShow = remember { mutableStateOf(randomImage) }

	//show card if its before month 10 and 40 days are left for ramadan
	val showCard = todayHijri[ChronoField.MONTH_OF_YEAR] < 10 && ramadanTimeLeft.value < 40

	//is ramadan time left less than 40 days
	//if yes then show the card
	if (showCard)
	{
		//show the card
		ElevatedCard(
				modifier = Modifier
					.fillMaxWidth()
					.padding(8.dp)
					.clickable { onNavigateToCalender() } ,
					) {
			Column(
					modifier = Modifier.padding(16.dp) ,
					verticalArrangement = Arrangement.Center ,
					horizontalAlignment = Alignment.CenterHorizontally
				  ) {

					if (todayHijri[ChronoField.DAY_OF_MONTH] == 1 && todayHijri[ChronoField.MONTH_OF_YEAR] == 9)
					{
						Text(text = "Ramadan Mubarak" , style = MaterialTheme.typography.titleLarge)
					} else
					{
						Text(text = "Ramadan" , style = MaterialTheme.typography.titleLarge)
					}

				Row(
						modifier = Modifier
							.fillMaxWidth(),
						verticalAlignment = Alignment.CenterVertically ,
						horizontalArrangement = Arrangement.SpaceBetween
				   ) {
					Box(
							modifier = Modifier
								.clip(MaterialTheme.shapes.extraLarge)
								.padding(8.dp)
								.size(80.dp)
					   ) {
						Image(
								painter = painterResource(id = imageToShow.value) ,
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
						if (isAfterRamadanStart)
						{
							//estimated end
							Text(text = "Estimated end",style = MaterialTheme.typography.titleSmall)
						} else
						{
							//estimated start
							Text(text = "Estimated start",style = MaterialTheme.typography.titleSmall)
						}
						//if ramaadan time left is 1 then show that it ends today if its 2 then show that it ends tomorrow
						Text(
								text = if (ramadanTimeLeft.value == 0L) "Today" else if (ramadanTimeLeft.value == 1L) "Tomorrow" else "In ${ramadanTimeLeft.value} days" ,
								style = MaterialTheme.typography.headlineMedium
							)
						if (isAfterRamadanStart)
						{
							//estimated end
							Text(
									text = ramadanEndFormatted.toString() ,
									style = MaterialTheme.typography.titleSmall
								)
						} else
						{
							//estimated start
							Text(
									text = ramadanStartFormatted.toString() ,
									style = MaterialTheme.typography.titleSmall
								)
						}
					}
				}
			}
		}
	}
}

@Preview(showBackground = true)
@Composable
fun RamadanCardPreview()
{
	NimazTheme {
		RamadanCard(onNavigateToCalender = { })
	}
}