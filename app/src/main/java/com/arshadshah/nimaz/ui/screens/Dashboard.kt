package com.arshadshah.nimaz.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_HOME
import com.arshadshah.nimaz.ui.components.common.BannerSmall
import com.arshadshah.nimaz.ui.components.dashboard.DashboardFastTracker
import com.arshadshah.nimaz.ui.components.dashboard.DashboardPrayerTracker
import com.arshadshah.nimaz.ui.components.dashboard.DashboardPrayertimesCard
import com.arshadshah.nimaz.ui.components.dashboard.DashboardQuranTracker
import com.arshadshah.nimaz.ui.components.dashboard.DashboardRandomAyatCard
import com.arshadshah.nimaz.ui.components.dashboard.DashboardTasbihTracker
import com.arshadshah.nimaz.ui.components.dashboard.EidUlAdhaCard
import com.arshadshah.nimaz.ui.components.dashboard.EidUlFitrCard
import com.arshadshah.nimaz.ui.components.dashboard.RamadanCard
import com.arshadshah.nimaz.ui.components.dashboard.RamadanTimesCard
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.viewModel.SettingsViewModel
import com.arshadshah.nimaz.viewModel.TrackerViewModel
import java.time.LocalDateTime

@Composable
fun Dashboard(
	onNavigateToTracker : () -> Unit ,
	onNavigateToCalender : () -> Unit ,
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
	val viewModelTracker = viewModel(
			 key = AppConstants.TRACKING_VIEWMODEL_KEY ,
			 initializer = { TrackerViewModel() } ,
			 viewModelStoreOwner = LocalContext.current as ComponentActivity
									)
	LaunchedEffect(Unit) {
		viewModelSettings.handleEvent(SettingsViewModel.SettingsEvent.CheckUpdate(context , false))
	}

	val updateAvailable = remember {
		viewModelSettings.isUpdateAvailable
	}.collectAsState()

	val isFasting = remember {
		viewModelTracker.isFasting
	}.collectAsState()

	val stateScroll = rememberLazyListState()

	LazyColumn(
			 state = stateScroll ,
			 modifier = Modifier
				 .testTag(TEST_TAG_HOME) ,
			 contentPadding = paddingValues
			  ) {
		item {
			DashboardPrayertimesCard()
		}
		item {
			RamadanTimesCard(isFasting.value)
		}
		item {
			if (updateAvailable.value)
			{
				val sharedPref = PrivateSharedPreferences(context)
				val bannerShownLastTime =
					sharedPref.getData(
							 "Update Available-bannerIsOpen-time" ,
							 LocalDateTime.now().toString()
									  )
				//has it been 24 hours since the last time the banner was shown
				val has24HoursPassed = LocalDateTime.now().isAfter(
						 LocalDateTime.parse(bannerShownLastTime).plusHours(2)
																  )
				val isOpen = remember { mutableStateOf(true) }
				if (has24HoursPassed)
				{
					sharedPref.saveDataBoolean("Update Available-bannerIsOpen" , true)
				}
				BannerSmall(
						 title = "Update Available" ,
						 message = "Tap here to update the app" ,
						 isOpen = isOpen ,
						 onClick = {
							 viewModelSettings.handleEvent(
									  SettingsViewModel.SettingsEvent.CheckUpdate(context , true)
														  )
						 } ,
						 dismissable = true ,
						 paddingValues = PaddingValues(
								  top = 8.dp ,
								  bottom = 0.dp ,
								  start = 8.dp ,
								  end = 8.dp
													  )
						   )
			}
		}
		item {
			Card(
					 modifier = Modifier
						 .padding(top = 8.dp , bottom = 0.dp , start = 8.dp , end = 8.dp)
						 .testTag(AppConstants.TEST_TAG_EVENTS_CARD)
						 .clip(shape = MaterialTheme.shapes.extraLarge)
						 .clickable {
							 onNavigateToCalender()
						 } ,
					 colors = CardDefaults.cardColors(
							  containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp) ,
							  contentColor = MaterialTheme.colorScheme.onSurface ,
							  disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) ,
							  disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f) ,
													 ) ,
					 shape = MaterialTheme.shapes.extraLarge ,
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
			Card(
					 modifier = Modifier
						 .fillMaxWidth()
						 .padding(top = 8.dp , bottom = 0.dp , start = 8.dp , end = 8.dp)
						 .testTag(AppConstants.TEST_TAG_TRACKERS_CARD)
						 .clickable {
							 onNavigateToTracker()
						 } ,
					 colors = CardDefaults.elevatedCardColors(
							  containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp) ,
							  contentColor = MaterialTheme.colorScheme.onSurface ,
							  disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) ,
							  disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f) ,
															 ) ,
					 shape = MaterialTheme.shapes.extraLarge ,
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
				DashboardFastTracker()
			}
		}
		//quick links to the tasbih and quran
		item {
			Card(
					 colors = CardDefaults.elevatedCardColors(
							  containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp) ,
							  contentColor = MaterialTheme.colorScheme.onSurface ,
							  disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) ,
							  disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f) ,
															 ) ,
					 shape = MaterialTheme.shapes.extraLarge ,
					 modifier = Modifier
						 .fillMaxWidth()
						 .padding(top = 8.dp , bottom = 0.dp , start = 8.dp , end = 8.dp)
				) {
				Text(
						 text = "Quick Links" ,
						 modifier = Modifier
							 .padding(8.dp)
							 .fillMaxWidth() ,
						 textAlign = TextAlign.Center ,
						 style = MaterialTheme.typography.titleMedium
					)
				DashboardQuranTracker(onNavigateToAyatScreen = onNavigateToAyatScreen)
				DashboardTasbihTracker(
						 onNavigateToTasbihScreen = onNavigateToTasbihScreen ,
						 onNavigateToTasbihListScreen = onNavigateToTasbihListScreen
									  )
			}
		}
		item {
			Card(
					 colors = CardDefaults.elevatedCardColors(
							  containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp) ,
							  contentColor = MaterialTheme.colorScheme.onSurface ,
							  disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) ,
							  disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f) ,
															 ) ,
					 shape = MaterialTheme.shapes.extraLarge ,
					 modifier = Modifier
						 .fillMaxWidth()
						 .padding(8.dp)
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
				 onNavigateToTasbihScreen = { _ , _ , _ , _ -> } ,
				 paddingValues = PaddingValues(8.dp) ,
				 onNavigateToTasbihListScreen = { } ,
				 onNavigateToAyatScreen = { _ , _ , _ , _ -> } ,
				 )
	}
}