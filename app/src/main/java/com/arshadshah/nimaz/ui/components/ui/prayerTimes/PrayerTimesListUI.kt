package com.arshadshah.nimaz.ui.components.ui.prayerTimes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.data.remote.models.CountDownTime
import com.arshadshah.nimaz.data.remote.viewModel.PrayerTimesViewModel
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Composable
fun PrayerTimesListUI(
	prayerTimesMap : Map<String , LocalDateTime?> ,
	name : String ,
	state : PrayerTimesViewModel.PrayerTimesState ,
	countDownTime : CountDownTime ,
	loading : Boolean ,
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
						state = state ,
						countDownTime = countDownTime ,
						loading = loading ,
							  )
			}
		}
	}
}

//the row for the prayer times
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerTimesRow(
	prayerName : String ,
	prayerTime : LocalDateTime? ,
	isHighlighted : Boolean ,
	state : PrayerTimesViewModel.PrayerTimesState ,
	countDownTime : CountDownTime ,
	loading : Boolean ,
				  )
{
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
		Text(
				text = sentenceCase ,
				modifier = Modifier
					.padding(16.dp)
					.placeholder(
							visible = loading ,
							color = MaterialTheme.colorScheme.outline ,
							shape = RoundedCornerShape(4.dp) ,
							highlight = PlaceholderHighlight.shimmer(
									highlightColor = Color.White ,
																	)
								) ,
				style = MaterialTheme.typography.titleLarge
			)
		if (isHighlighted)
		{
			Text(
					modifier = Modifier
						.padding(16.dp)
						.placeholder(
								visible = loading ,
								color = MaterialTheme.colorScheme.outline ,
								shape = RoundedCornerShape(4.dp) ,
								highlight = PlaceholderHighlight.shimmer(
										highlightColor = Color.White ,
																		)
									) ,
					text = " -${countDownTime.hours} : ${countDownTime.minutes} : ${countDownTime.seconds}" ,
					textAlign = TextAlign.Center ,
					style = MaterialTheme.typography.titleSmall
											   )
		}
		Text(
				text = prayerTime !!.format(formatter) ,
				modifier = Modifier
					.padding(16.dp)
					.placeholder(
							visible = loading ,
							color = MaterialTheme.colorScheme.outline ,
							shape = RoundedCornerShape(4.dp) ,
							highlight = PlaceholderHighlight.shimmer(
									highlightColor = Color.White ,
																	)
								) ,
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