package com.arshadshah.nimaz.ui.components.trackers

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.data.remote.models.PrayerTracker
import com.arshadshah.nimaz.ui.components.common.ToggleableItemColumn
import com.arshadshah.nimaz.viewModel.TrackerViewModel
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer
import es.dmoral.toasty.Toasty
import java.time.DayOfWeek
import java.time.LocalDate

@Composable
fun PrayerTrackerListItems(
	items : List<String> ,
	loading : Boolean ,
	fajrChecked : MutableState<Boolean> ,
	zuhrChecked : MutableState<Boolean> ,
	asrChecked : MutableState<Boolean> ,
	maghribChecked : MutableState<Boolean> ,
	ishaChecked : MutableState<Boolean> ,
	handleEvent : (TrackerViewModel.TrackerEvent) -> Unit ,
	showDateSelector : State<Boolean> ,
	dateState : State<String> ,
	progress : MutableState<Float> ,
	isMenstrauting : State<Boolean> ,
						  )
{
	val context = LocalContext.current
	val dateForTracker = LocalDate.parse(dateState.value)
	val isAfterToday = dateForTracker.isAfter(LocalDate.now())
	if (showDateSelector.value)
	{
		DateSelector(
				 handleEvent = handleEvent
					)
	}
	ElevatedCard(
			 shape = MaterialTheme.shapes.extraLarge ,
			 modifier = Modifier
				 .fillMaxWidth()
				 .padding(8.dp)
				) {
		items.forEachIndexed { index , item ->
			//if not the first item add a divider
			if (index != 0)
			{
				Divider(
						 color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f) ,
						 thickness = 1.dp
					   )
			}
			//the toggleable item
			ToggleableItemColumn(
					 enabled = ! isMenstrauting.value ,
					 modifier = Modifier
						 .fillMaxWidth()
						 .padding(start = 16.dp , end = 16.dp , top = 8.dp , bottom = 8.dp)
						 .placeholder(
								  visible = loading ,
								  color = MaterialTheme.colorScheme.outline ,
								  shape = RoundedCornerShape(4.dp) ,
								  highlight = PlaceholderHighlight.shimmer(
										   highlightColor = Color.White ,
																		  )
									 ) ,
					 text = item ,
					 checked = when (item)
					 {
						 //if the date is after today then disable the toggle
						 "Fajr" -> if (isAfterToday) false else fajrChecked.value
						 "Dhuhr" -> if (isAfterToday) false else zuhrChecked.value
						 "Asr" -> if (isAfterToday) false else asrChecked.value
						 "Maghrib" -> if (isAfterToday) false else maghribChecked.value
						 "Isha" -> if (isAfterToday) false else ishaChecked.value
						 else -> false
					 } ,
					 onCheckedChange = {
						 if (isAfterToday)
						 {
							 Toasty.info(
									  context ,
									  "Oops! you cant update the tracker for a date in the future" ,
									  Toasty.LENGTH_SHORT ,
									  true
										).show()
							 return@ToggleableItemColumn
						 }
						 when (item)
						 {
							 //if the date is after today then disable the toggle
							 "Fajr" -> fajrChecked.value = it
							 "Dhuhr" -> zuhrChecked.value = it
							 "Asr" -> asrChecked.value = it
							 "Maghrib" -> maghribChecked.value = it
							 "Isha" -> ishaChecked.value = it
						 }

						 //for each of the checked items add 20 to the progress any unchecked item subtracts 20
						 progress.value = when (item)
						 {
							 "Fajr" -> if (it) progress.value + 20 else progress.value - 20
							 "Dhuhr" -> if (it) progress.value + 20 else progress.value - 20
							 "Asr" -> if (it) progress.value + 20 else progress.value - 20
							 "Maghrib" -> if (it) progress.value + 20 else progress.value - 20
							 "Isha" -> if (it) progress.value + 20 else progress.value - 20
							 else -> 0f
						 }

						 handleEvent(
								  TrackerViewModel.TrackerEvent.UPDATE_TRACKER(
										   PrayerTracker(
													fajr = fajrChecked.value ,
													dhuhr = zuhrChecked.value ,
													asr = asrChecked.value ,
													maghrib = maghribChecked.value ,
													isha = ishaChecked.value ,
													date = dateState.value ,
													progress = progress.value.toInt()
														)
																			  )
									)
						 val firstDayOfWeek = LocalDate.now().with(DayOfWeek.MONDAY)
						 val lastDayOfWeek = LocalDate.now().with(DayOfWeek.SUNDAY)
						 if (LocalDate.parse(dateState.value) in firstDayOfWeek .. lastDayOfWeek)
						 {
							 handleEvent(
									  TrackerViewModel.TrackerEvent.UPDATE_PROGRESS_FOR_DAY(
											   LocalDate.parse(dateState.value).dayOfWeek ,
											   progress.value.toInt()
																						   )
										)
						 }
						 handleEvent(
								  TrackerViewModel.TrackerEvent.GET_PROGRESS_FOR_MONTH(
										   LocalDate.now().toString()
																					  )
									)
					 } ,
								)
		}
	}
}