package com.arshadshah.nimaz.ui.components.ui.prayerTimes

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.data.remote.viewModel.PrayerTimesViewModel
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.util.*

@Composable
fun PrayerTimesListUI(
	prayerTimesMap : Map<String , LocalDateTime?> ,
	name : String ,
	loading : Boolean ,
					 )
{
	val today = LocalDate.now()
	val todayHijri = HijrahDate.from(today)
	val ramadanStart = HijrahDate.of(todayHijri[ChronoField.YEAR] , 9 , 1)
	val ramadanEnd = HijrahDate.of(todayHijri[ChronoField.YEAR] , 9 , 29)
	val isRamadan = todayHijri.isAfter(ramadanStart) && todayHijri.isBefore(ramadanEnd)
	ElevatedCard(
			shape = MaterialTheme.shapes.extraLarge ,
			modifier = Modifier
				.fillMaxWidth()
				.padding(vertical = 8.dp)
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
							color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f) ,
						   )
				}
				//check if the row is to be highlighted
				val isHighlighted = key == name
				val isBoldText = if (isRamadan)
				{
					key == "Fajr" || key == "Maghrib"
				} else
				{
					false
				}
				PrayerTimesRow(
						prayerName = key ,
						prayerTime = value ,
						isHighlighted = isHighlighted ,
						loading = loading ,
						isBoldText = isBoldText ,
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
	loading : Boolean ,
	isBoldText : Boolean ,
				  )
{
	val viewModel = viewModel(
			key = "PrayerTimesViewModel" ,
			initializer = { PrayerTimesViewModel() } ,
			viewModelStoreOwner = LocalContext.current as ComponentActivity
							 )
	val countDownTime = remember { viewModel.timer }.collectAsState()
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
					.clip(RoundedCornerShape(topStart = 8.dp , topEnd = 8.dp , bottomStart = 8.dp , bottomEnd = 8.dp))
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
				style = MaterialTheme.typography.titleLarge,
				fontWeight = if (isBoldText) FontWeight.ExtraBold else MaterialTheme.typography.titleLarge.fontWeight
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
					text = " -${countDownTime.value.hours} : ${countDownTime.value.minutes} : ${countDownTime.value.seconds}" ,
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
				style = MaterialTheme.typography.titleLarge,
				fontWeight = if (isBoldText) FontWeight.ExtraBold else MaterialTheme.typography.titleLarge.fontWeight
			)
	}
}