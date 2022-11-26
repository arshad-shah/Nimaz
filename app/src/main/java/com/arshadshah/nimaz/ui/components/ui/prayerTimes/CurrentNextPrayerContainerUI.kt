package com.arshadshah.nimaz.ui.components.ui.prayerTimes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import com.arshadshah.nimaz.data.remote.models.CountDownTime

@Composable
fun CurrentNextPrayerContainerUI(
    nextPrayerName : String ,
    state : LiveData<CountDownTime> ,
                                )
{
	var countDownTime by remember { mutableStateOf(CountDownTime(0 , 0 , 0)) }
	LaunchedEffect(key1 = state) {
		state.observeForever {
			countDownTime = it
		}
	}
	ElevatedCard(
			modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp , 8.dp)
                .height(IntrinsicSize.Max)
                .shadow(5.dp , shape = MaterialTheme.shapes.medium , clip = true)
				) {
		val nextPrayerNameSentenceCase = nextPrayerName
			.lowercase()
			.replaceFirstChar { it.uppercase() }

		Row(
				horizontalArrangement = Arrangement.Center ,
				modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer)
		   ) {
			Text(
					text = getTimerString(
							countDownTime.hours.toInt() ,
							countDownTime.minutes.toInt() ,
							countDownTime.seconds.toInt() ,
							nextPrayerNameSentenceCase
										 ) ,
					modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp) ,
					textAlign = TextAlign.Center ,
				)
		}
	}
}

fun getTimerString(
    hours : Int ,
    minutes : Int ,
    seconds : Int ,
    nextPrayerNameSentenceCase : String ,
                  ) : String
{
	return when
	{
		hours > 1 && minutes != 0 ->
		{
			"$hours hrs $minutes mins $seconds secs to $nextPrayerNameSentenceCase"
		}

		hours > 1 && minutes == 0 ->
		{
			"$hours hrs to $nextPrayerNameSentenceCase"
		}

		hours == 1 && minutes in 2 .. 59 ->
		{
			"$hours hr $minutes mins $seconds secs to $nextPrayerNameSentenceCase"
		}

		hours == 1 && minutes < 2 && minutes > 0 ->
		{
			"$hours hr $minutes mins to $nextPrayerNameSentenceCase"
		}

		hours == 1 && minutes == 0 ->
		{
			"$hours hr to $nextPrayerNameSentenceCase"
		}

		hours == 0 && minutes in 2 .. 59 ->
		{
			"$minutes mins $seconds secs to $nextPrayerNameSentenceCase"
		}

		hours == 0 && minutes < 2 && minutes > 0 ->
		{
			"$minutes mins $seconds seconds to $nextPrayerNameSentenceCase"
		}

		hours == 0 && minutes == 0 ->
		{
			"$seconds seconds to $nextPrayerNameSentenceCase"
		}
		//if there is less than 1 minute left
		else ->
		{
			"$seconds seconds to $nextPrayerNameSentenceCase"
		}
	}
}
