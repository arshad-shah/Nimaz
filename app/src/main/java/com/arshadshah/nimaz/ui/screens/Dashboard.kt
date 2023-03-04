package com.arshadshah.nimaz.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_HOME
import com.arshadshah.nimaz.data.remote.models.Tasbih
import com.arshadshah.nimaz.data.remote.viewModel.TasbihViewModel
import com.arshadshah.nimaz.ui.components.bLogic.prayerTimes.DashboardPrayertimesCard
import com.arshadshah.nimaz.ui.components.bLogic.prayerTimes.EidUlAdhaCard
import com.arshadshah.nimaz.ui.components.bLogic.prayerTimes.EidUlFitrCard
import com.arshadshah.nimaz.ui.components.bLogic.prayerTimes.RamadanCard
import com.arshadshah.nimaz.ui.components.ui.FeaturesDropDown
import com.arshadshah.nimaz.ui.components.ui.trackers.DashboardPrayerTracker
import com.arshadshah.nimaz.ui.components.ui.trackers.DropDownHeader
import com.arshadshah.nimaz.ui.components.ui.trackers.GoalEditDialog
import com.arshadshah.nimaz.ui.components.ui.trackers.TasbihDropdownItem
import com.arshadshah.nimaz.ui.theme.NimazTheme
import java.time.LocalDate

@Composable
fun Dashboard(
	onNavigateToTracker : () -> Unit ,
	onNavigateToCalender : () -> Unit ,
	onNavigateToPrayerTimes : () -> Unit ,
	onNavigateToTasbihScreen : (String , String , String , String) -> Unit ,
	paddingValues : PaddingValues ,
	onNavigateToTasbihListScreen : () -> Unit ,
			 )
{

	LazyColumn(
			modifier = Modifier.testTag(TEST_TAG_HOME),
			contentPadding = paddingValues
			  ) {
		item {
			DashboardPrayertimesCard(
					onNavigateToPrayerTimes = onNavigateToPrayerTimes
									)
		}
		item {
			ElevatedCard(
					modifier = Modifier
						.padding(8.dp)
						.clickable {
							onNavigateToCalender()
						}
						) {
				Text(
						text = "Events" ,
						modifier = Modifier
							.padding(8.dp)
							.fillMaxWidth() ,
						textAlign = TextAlign.Center ,
						style = MaterialTheme.typography.titleMedium
					)
				RamadanCard(
						onNavigateToCalender = onNavigateToCalender
						   )
				EidUlFitrCard {
					onNavigateToCalender()
				}
				EidUlAdhaCard {
					onNavigateToCalender()
				}
			}
		}
		item{
			ElevatedCard(
					modifier = Modifier
						.fillMaxWidth()
						.padding(8.dp)
						.clickable {
							onNavigateToTracker()
						}
						) {
				Text(
						text = "Trackers" ,
						modifier = Modifier
							.padding(8.dp)
							.fillMaxWidth() ,
						textAlign = TextAlign.Center ,
						style = MaterialTheme.typography.titleMedium
					)
				DashboardPrayerTracker()
				DashboardTasbihTracker(onNavigateToTasbihScreen = onNavigateToTasbihScreen,onNavigateToTasbihListScreen = onNavigateToTasbihListScreen)
			}
		}
	}

}

@Composable
fun DashboardTasbihTracker(
	onNavigateToTasbihScreen : (String , String , String , String) -> Unit ,
	onNavigateToTasbihListScreen : () -> Unit
						  )
{
	val context = LocalContext.current
	val viewModel = viewModel(
			key = "TasbihViewModel" ,
			initializer = { TasbihViewModel(context) } ,
			viewModelStoreOwner = LocalContext.current as ComponentActivity
							 )
	//run only once
	LaunchedEffect(key1 = true) {
		viewModel.handleEvent(TasbihViewModel.TasbihEvent.RecreateTasbih(LocalDate.now().toString()))
	}
	val listOfTasbih = remember {
		viewModel.tasbihList
	}.collectAsState()

	if(listOfTasbih.value.isEmpty())
	{
		//a message to the user that there are no tasbih for the day
		ElevatedCard(
				modifier = Modifier
					.padding(8.dp)
					.fillMaxWidth()
					.clickable {
						onNavigateToTasbihListScreen()
					}
					) {
			Text(
					text = "No Tasbih set for today" ,
					modifier = Modifier
						.padding(8.dp)
						.fillMaxWidth() ,
					textAlign = TextAlign.Center ,
					style = MaterialTheme.typography.titleMedium
				)
			Text(
					text = "Click here to add a tasbih" ,
					modifier = Modifier
						.padding(8.dp)
						.fillMaxWidth() ,
					textAlign = TextAlign.Center ,
					style = MaterialTheme.typography.bodyMedium
				)
		}
	}else{
		val showTasbihDialog = remember {
			mutableStateOf(false)
		}
		val tasbihToEdit = remember {
			mutableStateOf(
					Tasbih(
							0 ,
							"" ,
							"" ,
							"" ,
							"" ,
							0 ,
							0 ,
						  )
						  )
		}

		FeaturesDropDown(
				header = {
					DropDownHeader(
							headerLeft = "Name" ,
							headerRight = "Count" ,
							headerMiddle = "Goal"
								  )
				} ,
				//the list of tasbih for the date at the index
				items = listOfTasbih.value,
				label = "Tasbih" ,
				dropDownItem = {
					TasbihDropdownItem(
							it ,
							onClick = {tasbih ->
								onNavigateToTasbihScreen(
										tasbih.id.toString() ,
										tasbih.arabicName ,
										tasbih.englishName ,
										tasbih.translationName
														)
							},
							onDelete = { tasbih ->
								viewModel.handleEvent(
										TasbihViewModel.TasbihEvent.DeleteTasbih(
												tasbih
																				)
													 )
							},
							onEdit = { tasbih ->
								showTasbihDialog.value = true
								tasbihToEdit.value = tasbih
							} ,
									  )
				}

						)

		GoalEditDialog(tasbihToEdit.value, showTasbihDialog)
	}
}


@Preview
@Composable
fun DashboardPreview()
{
	NimazTheme(
			darkTheme = true
			  ) {
		Dashboard(
				onNavigateToTracker = { } ,
				onNavigateToCalender = { } ,
				onNavigateToPrayerTimes = { } ,
				onNavigateToTasbihScreen = { _, _, _, _ -> } ,
				paddingValues = PaddingValues(8.dp) ,
				onNavigateToTasbihListScreen = { } ,
				 )
	}
}