package com.arshadshah.nimaz.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_HOME
import com.arshadshah.nimaz.data.remote.viewModel.TasbihViewModel
import com.arshadshah.nimaz.ui.components.bLogic.prayerTimes.DashboardPrayertimesCard
import com.arshadshah.nimaz.ui.components.bLogic.prayerTimes.RamadanCard
import com.arshadshah.nimaz.ui.components.ui.FeatureDropdownItem
import com.arshadshah.nimaz.ui.components.ui.FeaturesDropDown
import com.arshadshah.nimaz.ui.components.ui.trackers.DashboardPrayerTracker
import com.arshadshah.nimaz.ui.screens.tasbih.DropDownHeader
import com.arshadshah.nimaz.ui.theme.NimazTheme
import java.time.LocalDate

@Composable
fun Dashboard(
	onNavigateToTracker : () -> Unit ,
	onNavigateToCalender : () -> Unit ,
	onNavigateToPrayerTimes : () -> Unit ,
	onNavigateToTasbihScreen : (String , String , String , String) -> Unit ,
	paddingValues : PaddingValues ,
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
				DashboardTasbihTracker(onNavigateToTasbihScreen = onNavigateToTasbihScreen)
			}
		}
	}

}

@Composable
fun DashboardTasbihTracker(onNavigateToTasbihScreen : (String , String , String , String) -> Unit)
{
	val context = LocalContext.current
	val viewModel = viewModel(
			key = "TasbihViewModel" ,
			initializer = { TasbihViewModel(context) } ,
			viewModelStoreOwner = LocalContext.current as ComponentActivity
							 )
	viewModel.handleEvent(TasbihViewModel.TasbihEvent.RecreateTasbih(LocalDate.now().toString()))
	val listOfTasbih = remember {
		viewModel.tasbihList
	}.collectAsState()

	if(listOfTasbih.value.isEmpty())
	{
		return
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
				FeatureDropdownItem(
						item = it ,
						onClick = { tasbih ->
							onNavigateToTasbihScreen(
									tasbih.id.toString() ,
									tasbih.arabicName ,
									tasbih.englishName ,
									tasbih.translationName
													)
						} ,
						itemContent = { tasbih ->
							//trim the text if it is too long
							val trimmedText =
								if (tasbih.englishName.length > 20)
								{
									tasbih.englishName.substring(
											0 ,
											20
																) + "..."
								} else
								{
									tasbih.englishName
								}
							Row(
									modifier = Modifier
										.fillMaxWidth() ,
									verticalAlignment = Alignment.CenterVertically
							   ) {
								//an icon to indicate if the tasbih is completed
								if(tasbih.count == tasbih.goal)
								{
									Icon(
											imageVector = Icons.Default.CheckCircle ,
											contentDescription = "Completed" ,
											modifier = Modifier
												.size(24.dp)
										)
								}
								//name
								Text(
										modifier = Modifier
											.weight(1f) ,
										text = trimmedText ,
										textAlign = TextAlign.Center ,
										maxLines = 2 ,
										overflow = TextOverflow.Ellipsis ,
										style = MaterialTheme.typography.bodySmall
									)
								//divider
								Divider(
										modifier = Modifier
											.width(1.dp)
											.height(24.dp) ,
										color = MaterialTheme.colorScheme.onSurface.copy(
												alpha = 0.08f
																						) ,
										thickness = 1.dp ,
									   )
								//goal
								Text(
										modifier = Modifier
											.weight(1f) ,
										text = tasbih.goal.toString() ,
										textAlign = TextAlign.Center ,
										maxLines = 2 ,
										overflow = TextOverflow.Ellipsis ,
										style = MaterialTheme.typography.bodySmall
									)
								//divider
								Divider(
										modifier = Modifier
											.width(1.dp)
											.height(24.dp) ,
										color = MaterialTheme.colorScheme.onSurface.copy(
												alpha = 0.08f
																						) ,
										thickness = 1.dp ,
									   )
								//count
								Text(
										modifier = Modifier
											.weight(1f) ,
										text = tasbih.count.toString() ,
										textAlign = TextAlign.Center ,
										maxLines = 2 ,
										overflow = TextOverflow.Ellipsis ,
										style = MaterialTheme.typography.bodySmall
									)
							}
						} ,
								   )
			}

					)
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
				 )
	}
}