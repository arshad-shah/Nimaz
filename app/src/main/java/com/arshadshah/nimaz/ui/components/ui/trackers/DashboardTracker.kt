package com.arshadshah.nimaz.ui.components.ui.trackers

import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.data.remote.models.PrayerTracker
import com.arshadshah.nimaz.data.remote.viewModel.TrackerViewModel
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer
import es.dmoral.toasty.Toasty
import java.time.LocalDate
import kotlin.reflect.KFunction1

@Composable
fun DashboardPrayerTracker(onNavigateToTracker : () -> Unit)
{

	val viewModel = viewModel(
			key = "TrackerViewModel" ,
			initializer = { TrackerViewModel() } ,
			viewModelStoreOwner = LocalContext.current as ComponentActivity
							 )

	val dateState = remember {
		viewModel.dateState
	}.collectAsState()

	LaunchedEffect(key1 = "getTrackerForDate") {
		viewModel.onEvent(TrackerViewModel.TrackerEvent.SHOW_DATE_SELECTOR(false))
		viewModel.onEvent(TrackerViewModel.TrackerEvent.GET_TRACKER_FOR_DATE(dateState.value))
	}

	val stateOfTrackerForToday = remember {
		viewModel.trackerState
	}.collectAsState()

	val fajrState = remember {
		viewModel.fajrState
	}.collectAsState()

	val zuhrState = remember {
		viewModel.zuhrState
	}.collectAsState()

	val asrState = remember {
		viewModel.asrState
	}.collectAsState()

	val maghribState = remember {
		viewModel.maghribState
	}.collectAsState()

	val ishaState = remember {
		viewModel.ishaState
	}.collectAsState()

	val progressState = remember {
		viewModel.progressState
	}.collectAsState()

	val context = LocalContext.current

	//a list of booleans to keep track of the state of the toggleable items
	val fajrChecked = remember { mutableStateOf(false) }
	val zuhrChecked = remember { mutableStateOf(false) }
	val asrChecked = remember { mutableStateOf(false) }
	val maghribChecked = remember { mutableStateOf(false) }
	val ishaChecked = remember { mutableStateOf(false) }
	val progress = remember { mutableStateOf(0f) }

	Box(
			modifier = Modifier.clickable{
				onNavigateToTracker()
			}
	   ){
		when (stateOfTrackerForToday.value)
		{
			is TrackerViewModel.TrackerState.Loading ->
			{
				Log.d("Tracker" , "Loading")
				PrayerTrackerListItemsRow(
						loading = true ,
						fajrChecked = fajrChecked ,
						zuhrChecked = zuhrChecked ,
						asrChecked = asrChecked ,
						maghribChecked = maghribChecked ,
						ishaChecked = ishaChecked ,
						handleEvent = viewModel::onEvent ,
						dateState = dateState ,
						progress = progress
											   )
			}

			is TrackerViewModel.TrackerState.Tracker ->
			{
				fajrChecked.value = fajrState.value
				zuhrChecked.value = zuhrState.value
				asrChecked.value = asrState.value
				maghribChecked.value = maghribState.value
				ishaChecked.value = ishaState.value
				progress.value = progressState.value.toFloat()
				PrayerTrackerListItemsRow(
						loading = false ,
						fajrChecked = fajrChecked ,
						zuhrChecked = zuhrChecked ,
						asrChecked = asrChecked ,
						maghribChecked = maghribChecked ,
						ishaChecked = ishaChecked ,
						handleEvent = viewModel::onEvent ,
						dateState = dateState ,
						progress = progress
											   )
			}

			is TrackerViewModel.TrackerState.Error ->
			{
				Toasty.error(
						context ,
						(stateOfTrackerForToday.value as TrackerViewModel.TrackerState.Error).message ,
						Toast.LENGTH_SHORT ,
						true
							).show()
			}

		}
	}
}

//the prayer tracker list items
@Composable
fun PrayerTrackerListItemsRow(
	loading : Boolean ,
	fajrChecked : MutableState<Boolean> ,
	zuhrChecked : MutableState<Boolean> ,
	asrChecked : MutableState<Boolean> ,
	maghribChecked : MutableState<Boolean> ,
	ishaChecked : MutableState<Boolean> ,
	handleEvent : KFunction1<TrackerViewModel.TrackerEvent , Unit> ,
	dateState : State<String> ,
	progress : MutableState<Float>
						  )
{
	val context = LocalContext.current
	val items = listOf("Fajr" , "Dhuhr" , "Asr" , "Maghrib" , "Isha")
	val dateForTracker = LocalDate.parse(dateState.value)
	val isAfterToday = dateForTracker.isAfter(LocalDate.now())

	ElevatedCard(
			modifier = Modifier
				.padding(8.dp)
				.fillMaxWidth()
				) {
			//if not then show the items
			Row(
					modifier = Modifier
						.padding(8.dp)
						.fillMaxWidth() ,
					horizontalArrangement = Arrangement.SpaceEvenly ,
					verticalAlignment = Alignment.CenterVertically
			   ) {
				items.forEachIndexed { index , item ->
					//the toggleable item
					ToggleableItemRow(
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
									return@ToggleableItemRow
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
							} ,
							modifier = Modifier
								.placeholder(
										visible = loading ,
										color = MaterialTheme.colorScheme.outline ,
										shape = RoundedCornerShape(4.dp) ,
										highlight = PlaceholderHighlight.shimmer(
												highlightColor = Color.White ,
																				)
											) ,
									 )
				}
			}
		}
}