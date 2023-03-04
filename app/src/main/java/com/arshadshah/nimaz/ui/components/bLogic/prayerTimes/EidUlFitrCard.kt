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
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField

//a card to show days left to eid ul fitr it shows only if the current date is 3 days before eid ul fitr
@Composable
fun EidUlFitrCard(onNavigateToCalender : () -> Unit)
{
	//a card that shows the time left for ramadan
	//it should only show when 40 days are left for ramadan
	//it should show the time left for ramadan in days, hours, minutes and seconds
	val eidUlFitrTimeLeft = remember { mutableStateOf(0L) }

	val today = LocalDate.now()
	val todayHijri = HijrahDate.from(today)
	val eidUlFitrStart = HijrahDate.of(todayHijri[ChronoField.YEAR] , 10 , 1)
	val eidUlFitrEnd = HijrahDate.of(todayHijri[ChronoField.YEAR] , 10 , 3)
	//get date of ramadan start in gregorian
	val eidUlFitrStartGregorian = LocalDate.from(eidUlFitrStart)
	val eidUlFitrEndGregorian = LocalDate.from(eidUlFitrEnd)
	//format the date to show day and month and year
	//wednesday, 1st of september 2021
	val formatter = java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")
	val eidUlFitrStartFormatted = eidUlFitrStartGregorian.format(formatter)
	val eidUlFitrEndFormatted = eidUlFitrEndGregorian.format(formatter)

	val isAfterEidUlFitrStart = today.isAfter(eidUlFitrStartGregorian)
	if (isAfterEidUlFitrStart)
	{
		if (todayHijri.isBefore(eidUlFitrEnd))
		{
			eidUlFitrTimeLeft.value = eidUlFitrEnd.toEpochDay() - todayHijri.toEpochDay()
		}
	} else
	{
		eidUlFitrTimeLeft.value = eidUlFitrEnd.toEpochDay() - todayHijri.toEpochDay()
	}
	//list of images to pick from
	//we will pick a random image from the list
	val imagesToShow =
		listOf(
				R.drawable.eid ,
				R.drawable.eid2 ,
				R.drawable.eid3 ,
				R.drawable.eid4,
				R.drawable.eid5,
			  )
	//pick a random image
	val randomImage = imagesToShow.random()
	//save the image to show in the card
	val imageToShow = remember { mutableStateOf(randomImage) }

	//show card if its before month 10 and 40 days are left for ramadan
	val showCard = todayHijri[ChronoField.MONTH_OF_YEAR] < 10 && todayHijri[ChronoField.DAY_OF_MONTH] < 3

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
				if (isAfterEidUlFitrStart)
				{
					Text(text = "Eid ul Fitr" , style = MaterialTheme.typography.titleLarge)
				} else
				{
					Text(text = "Eid ul Fitr is coming" , style = MaterialTheme.typography.titleLarge)
				}

				Row(
						modifier = Modifier
							.fillMaxWidth()
							.padding(top = 16.dp) ,
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
						if (isAfterEidUlFitrStart)
						{
							//estimated end
							Text(
									text = "Estimated end" ,
									style = MaterialTheme.typography.titleSmall
								)
						} else
						{
							//estimated start
							Text(
									text = "Estimated start" ,
									style = MaterialTheme.typography.titleSmall
								)
						}
						Text(
								text = "${eidUlFitrTimeLeft.value} days" ,
								style = MaterialTheme.typography.headlineMedium
							)
						if (isAfterEidUlFitrStart)
						{
							//estimated end
							Text(
									text = eidUlFitrEndFormatted.toString() ,
									style = MaterialTheme.typography.titleSmall
								)
						} else
						{
							//estimated start
							Text(
									text = eidUlFitrStartFormatted.toString() ,
									style = MaterialTheme.typography.titleSmall
								)
						}
					}
				}
			}
		}
	}
}

@Preview
@Composable
fun EidUlFitrCardPreview()
{
	EidUlFitrCard(onNavigateToCalender = {})
}