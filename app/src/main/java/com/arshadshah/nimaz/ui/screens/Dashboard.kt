package com.arshadshah.nimaz.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_EVENTS_CARD
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_HOME
import com.arshadshah.nimaz.data.remote.viewModel.SettingsViewModel
import com.arshadshah.nimaz.ui.components.bLogic.prayerTimes.*
import com.arshadshah.nimaz.ui.components.ui.BannerSmall
import com.arshadshah.nimaz.ui.components.ui.dashboard.DashboardTasbihTracker
import com.arshadshah.nimaz.ui.components.ui.quran.DashboardQuranTracker
import com.arshadshah.nimaz.ui.components.ui.quran.DashboardRandomAyatCard
import com.arshadshah.nimaz.ui.components.ui.trackers.DashboardFastTracker
import com.arshadshah.nimaz.ui.components.ui.trackers.DashboardPrayerTracker
import com.arshadshah.nimaz.ui.theme.NimazTheme

@Composable
fun Dashboard(
	onNavigateToTracker : () -> Unit ,
	onNavigateToCalender : () -> Unit ,
	onNavigateToPrayerTimes : () -> Unit ,
	onNavigateToTasbihScreen : (String , String , String , String) -> Unit ,
	paddingValues : PaddingValues ,
	onNavigateToTasbihListScreen : () -> Unit ,
	onNavigateToAyatScreen : (String , Boolean , String , Int) -> Unit ,
			 )
{
	val context = LocalContext.current
	val viewModelSettings = viewModel(
			key = AppConstants.SETTINGS_VIEWMODEL_KEY ,
			initializer = { SettingsViewModel(context) } ,
			viewModelStoreOwner = context as ComponentActivity
									 )

	val updateAvailabile = remember {
		viewModelSettings.isUpdateAvailable
	}.collectAsState()
	val stateScroll = rememberLazyListState()
	LazyColumn(
			state = stateScroll ,
			modifier = Modifier
				.testTag(TEST_TAG_HOME),
			contentPadding = paddingValues
			  ) {
		item {
			DashboardPrayertimesCard(
					onNavigateToPrayerTimes = onNavigateToPrayerTimes
									)
		}
		item {
			RamadanTimesCard()
		}
		item {
			if (updateAvailabile.value) {
				val isOpen = remember { mutableStateOf(true) }
				BannerSmall(
						title = "Update Available" ,
						message = "Tap here to update to the latest version" ,
						isOpen = isOpen ,
						onClick = {
							viewModelSettings.handleEvent(
									SettingsViewModel.SettingsEvent.CheckUpdate(context, true)
														 )
						},
						dismissable = true,
						paddingValues = PaddingValues(top = 8.dp , bottom = 0.dp, start = 8.dp, end = 8.dp)
						   )
			}
		}
		item {
			ElevatedCard(
					shape = MaterialTheme.shapes.extraLarge ,
					modifier = Modifier
						.padding(top = 8.dp , bottom = 0.dp , start = 8.dp , end = 8.dp)
						.testTag(TEST_TAG_EVENTS_CARD)
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
		item {
			ElevatedCard(
					shape = MaterialTheme.shapes.extraLarge ,
					modifier = Modifier
						.fillMaxWidth()
						.padding(top = 8.dp , bottom = 0.dp , start = 8.dp , end = 8.dp)
						.testTag(AppConstants.TEST_TAG_TRACKERS_CARD)
						) {
				Text(
						text = "Trackers" ,
						modifier = Modifier
							.padding(8.dp)
							.fillMaxWidth() ,
						textAlign = TextAlign.Center ,
						style = MaterialTheme.typography.titleMedium
					)
				DashboardPrayerTracker(onNavigateToTracker = onNavigateToTracker)
				//if its ramaadan then show the fast tracker
				//DashboardFastTracker
				DashboardFastTracker()
				DashboardQuranTracker(onNavigateToAyatScreen = onNavigateToAyatScreen)
				DashboardTasbihTracker(
						onNavigateToTasbihScreen = onNavigateToTasbihScreen ,
						onNavigateToTasbihListScreen = onNavigateToTasbihListScreen
									  )
			}
		}
		item {
			ElevatedCard(
					shape = MaterialTheme.shapes.extraLarge ,
					modifier = Modifier
						.fillMaxWidth()
						.padding(top = 8.dp , bottom = 0.dp , start = 8.dp , end = 8.dp)
						) {
				Text(
						text = "Daily Verses" ,
						modifier = Modifier
							.padding(8.dp)
							.fillMaxWidth() ,
						textAlign = TextAlign.Center ,
						style = MaterialTheme.typography.titleMedium
					)
				DashboardRandomAyatCard(onNavigateToAyatScreen = onNavigateToAyatScreen)
			}
		}
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
				onNavigateToTasbihScreen = { _ , _ , _ , _ -> } ,
				paddingValues = PaddingValues(8.dp) ,
				onNavigateToTasbihListScreen = { } ,
				onNavigateToAyatScreen = { _ , _ , _ , _ -> } ,
				 )
	}
}