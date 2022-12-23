package com.arshadshah.nimaz.ui.components.ui.prayerTimes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.ui.theme.NimazTheme
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Composable
fun PrayerTimesListUI(
	modifier : Modifier = Modifier ,
	prayerTimesMap : Map<String , LocalDateTime?> ,
	name : String ,
	paddingValues : PaddingValues ,
					 )
{
	ElevatedCard(
			modifier = Modifier
				.fillMaxWidth()
				.padding(paddingValues)
				.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
				) {
		Column {
			//iterate over the map
			for ((key , value) in prayerTimesMap)
			{
				//if the element is first then dont add a divider else add a divider on top
				if (key != prayerTimesMap.keys.first())
				{
					Divider(
							modifier = Modifier.fillMaxWidth() ,
							thickness = 1.dp ,
							color = MaterialTheme.colorScheme.outline
						   )
				}
				//check if the row is to be highlighted
				val isHighlighted = getHighlightRow(name , key)
				PrayerTimesRow(
						prayerName = key ,
						prayerTime = value ,
						isHighlighted = isHighlighted
							  )
			}
		}
	}
}

//the row for the prayer times
@Composable
fun PrayerTimesRow(prayerName : String , prayerTime : LocalDateTime? , isHighlighted : Boolean)
{
	//format the date to time based on device format
	val formatter = DateTimeFormatter.ofPattern("hh:mm a")
	val sentenceCase =
		prayerName.lowercase(Locale.ROOT)
			.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
	Row(
			horizontalArrangement = Arrangement.SpaceBetween ,
			modifier = if (isHighlighted)
			{
				Modifier
					.fillMaxWidth()
					.background(MaterialTheme.colorScheme.secondaryContainer)
			} else
			{
				Modifier
					.fillMaxWidth()
			}
	   ) {
		Text(text = sentenceCase ,
			 modifier = Modifier.padding(16.dp) ,
			 style = MaterialTheme.typography.titleLarge)
		Text(
				text = prayerTime !!.format(formatter) ,
				modifier = Modifier.padding(16.dp) ,
				style = MaterialTheme.typography.titleLarge
			)
	}
}

//fnction to figure out which row to highlight
fun getHighlightRow(prayerName : String , name : String) : Boolean
{
	//convert to lower case
	val prayerNameLower = name.uppercase(Locale.ROOT)
	return prayerName == prayerNameLower
}


@Preview
@Composable
fun PrayerTimesListUIPreview()
{
	NimazTheme {
		PrayerTimesListUI(
				prayerTimesMap = mapOf(
						"fajr" to LocalDateTime.now() ,
						"sunrise" to LocalDateTime.now() ,
						"dhuhr" to LocalDateTime.now() ,
						"asr" to LocalDateTime.now() ,
						"maghrib" to LocalDateTime.now() ,
						"isha" to LocalDateTime.now()
									  ) ,
				name = "fajr" ,
				paddingValues = PaddingValues(16.dp)
						 )
	}
}