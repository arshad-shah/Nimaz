package com.arshadshah.nimaz.ui.components.ui.prayerTimes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.arshadshah.nimaz.data.remote.models.CountDownTime
import com.arshadshah.nimaz.data.remote.models.PrayerTimes
import com.arshadshah.nimaz.data.remote.viewModel.PrayerTimesViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Composable
fun PrayerTimesListUI(
	modifier : Modifier = Modifier ,
	prayerTimesMap : Map<String , LocalDateTime?> ,
	name : String ,
	timerState : LiveData<CountDownTime> ,
	viewModel : PrayerTimesViewModel ,
	prayertimes : PrayerTimes? ,
	paddingValues : PaddingValues ,
					 )
{
	ElevatedCard(
			modifier = Modifier
				.fillMaxWidth()
				.padding(vertical = 8.dp)
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
						isHighlighted = isHighlighted ,
						timerState = timerState ,
						prayertimes = prayertimes ,
						viewmodel = viewModel
							  )
			}
		}
	}
}

//the row for the prayer times
@Composable
fun PrayerTimesRow(
	prayerName : String ,
	prayerTime : LocalDateTime? ,
	isHighlighted : Boolean ,
	timerState : LiveData<CountDownTime>? ,
	prayertimes : PrayerTimes? ,
	viewmodel : PrayerTimesViewModel?
				  )
{
	var countDownTime by remember { mutableStateOf(CountDownTime(0 , 0 , 0)) }
	LaunchedEffect(key1 = timerState) {
		timerState?.observeForever {
			countDownTime = it
		}
	}
	val context = LocalContext.current
	//format the date to time based on device format
	val formatter = DateTimeFormatter.ofPattern("hh:mm a")
	val sentenceCase =
		prayerName.lowercase(Locale.ROOT)
			.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
	Row(
			horizontalArrangement = Arrangement.SpaceBetween ,
			verticalAlignment = Alignment.CenterVertically ,
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
		if (isHighlighted){
			val timeToNextPrayerLong =
				prayertimes?.nextPrayer?.time?.atZone(java.time.ZoneId.systemDefault())?.toInstant()
					?.toEpochMilli()
			val currentTime =
				LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toInstant()
					.toEpochMilli()
			val difference = timeToNextPrayerLong?.minus(currentTime)
			viewmodel?.startTimer(context , difference !!)
			Text(
					modifier = Modifier.padding(16.dp),
					text = " -${countDownTime.hours} : ${countDownTime.minutes} : ${countDownTime.seconds}" ,
					textAlign = TextAlign.Center ,
					style = MaterialTheme.typography.titleSmall
				)
		}
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
fun PrayerTimesRowPreview()
{
	PrayerTimesRow(
			prayerName = "FAJR" ,
			prayerTime = LocalDateTime.now() ,
			isHighlighted = true ,
			timerState = null ,
			prayertimes = null ,
			viewmodel = null
				  )
}