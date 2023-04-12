package com.arshadshah.nimaz.ui.components.dashboard

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.viewModel.PrayerTimesViewModel
import com.arshadshah.nimaz.viewModel.SettingsViewModel
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField


@Composable
fun RamadanTimesCard(isFasting : Boolean)
{

	val context = LocalContext.current
	val viewModel = viewModel(
			key = AppConstants.PRAYER_TIMES_VIEWMODEL_KEY ,
			initializer = { PrayerTimesViewModel() } ,
			viewModelStoreOwner = context as ComponentActivity
							 )
	val settingViewModel = viewModel(
			key = AppConstants.SETTINGS_VIEWMODEL_KEY ,
			initializer = { SettingsViewModel(context) } ,
			viewModelStoreOwner = context
									)
	val fajrPrayerTime = remember {
		viewModel.fajrTime
	}.collectAsState()
	val maghribPrayerTime = remember {
		viewModel.maghribTime
	}.collectAsState()
	val location = remember {
		settingViewModel.locationName
	}.collectAsState()
	//a card that shows the time left for ramadan
	//it should only show when 40 days are left for ramadan
	//it should show the time left for ramadan in days, hours, minutes and seconds
	val ramadanTimeLeft = remember { mutableStateOf(0L) }

	val today = LocalDate.now()
	val todayHijri = HijrahDate.from(today)
	val ramadanStart = HijrahDate.of(todayHijri[ChronoField.YEAR] , 9 , 1)
	val ramadanEnd = HijrahDate.of(todayHijri[ChronoField.YEAR] , 9 , 29)

	val isAfterRamadanStart = todayHijri.isAfter(ramadanStart)
	if (isAfterRamadanStart)
	{
		if (todayHijri.isBefore(ramadanEnd))
		{
			ramadanTimeLeft.value = ramadanEnd.toEpochDay() - todayHijri.toEpochDay()
		}
	} else
	{
		val diff = ramadanStart.toEpochDay() - todayHijri.toEpochDay()
		ramadanTimeLeft.value = diff
	}

	//show card if it is the month of ramadan
	val showCard =
		todayHijri[ChronoField.MONTH_OF_YEAR] == 9 && todayHijri[ChronoField.DAY_OF_MONTH] <= 29 || isFasting

	//is ramadan time left less than 40 days
	//if yes then show the card
	if (showCard)
	{
		//show the card
		ElevatedCard(
				colors = CardDefaults.elevatedCardColors(
						containerColor = MaterialTheme.colorScheme.secondaryContainer ,
						contentColor = MaterialTheme.colorScheme.onSecondaryContainer
														) ,
				shape = MaterialTheme.shapes.extraLarge ,
				modifier = Modifier
					.fillMaxWidth()
					.padding(top = 8.dp , start = 8.dp , end = 8.dp) ,
					) {
			Column(
					modifier = Modifier.padding(16.dp) ,
					verticalArrangement = Arrangement.Center ,
					horizontalAlignment = Alignment.CenterHorizontally
				  ) {
				Row(
						modifier = Modifier
							.fillMaxWidth()
							.padding(8.dp) ,
						verticalAlignment = Alignment.CenterVertically ,
						horizontalArrangement = Arrangement.SpaceBetween
				   ) {
					Text(text = "Fasting Times" , style = MaterialTheme.typography.titleMedium)
					IconButton(onClick = {
						//share the aya
						val shareIntent = Intent(Intent.ACTION_SEND)
						shareIntent.type = "text/plain"
						//create the share message
						//with the aya text, aya translation
						//the sura number followed by the aya number
						shareIntent.putExtra(
								Intent.EXTRA_TEXT ,
								"Ramadan Fasting Times for ${location.value} \n${
									DateTimeFormatter.ofPattern(
											"EEEE, d MMMM yyyy"
															   ).format(today)
								} \n" +
										"Imsak (Fajr): ${
											DateTimeFormatter.ofPattern("hh:mm a")
												.format(fajrPrayerTime.value)
										} \n" +
										"Iftar (Maghrib): ${
											DateTimeFormatter.ofPattern("hh:mm a")
												.format(maghribPrayerTime.value)
										} \n" +
										"Times are Provided by Nimaz : https://play.google.com/store/apps/details?id=com.arshadshah.nimaz"
											)
						shareIntent.putExtra(Intent.EXTRA_SUBJECT , "Ramadan Times")
						shareIntent.putExtra(Intent.EXTRA_TITLE , "Ramadan Times")

						//start the share intent
						context.startActivity(
								Intent.createChooser(
										shareIntent ,
										"Share Ramadan Times"
													)
											 )
					} , modifier = Modifier.size(24.dp)) {
						Icon(
								painter = painterResource(id = R.drawable.share_icon) ,
								contentDescription = "Share Ramadan Times" ,
							)
					}
				}

				val deviceTimeFormat =
					android.text.format.DateFormat.is24HourFormat(LocalContext.current)
				//if the device time format is 24 hour then use the 24 hour format
				val formatter = if (deviceTimeFormat)
				{
					DateTimeFormatter.ofPattern("HH:mm")
				} else
				{
					DateTimeFormatter.ofPattern("hh:mm a")
				}

				Row(
						modifier = Modifier
							.fillMaxWidth() ,
						verticalAlignment = Alignment.CenterVertically ,
						horizontalArrangement = Arrangement.SpaceBetween
				   ) {
					Column(
							modifier = Modifier.fillMaxWidth() ,
							verticalArrangement = Arrangement.Center ,
							horizontalAlignment = Alignment.CenterHorizontally
						  ) {
						TimeComponent(
								title = "Imsak (Fajr)" ,
								fajrPrayerTime = formatter.format(fajrPrayerTime.value)
									 )
						TimeComponent(
								title = "Iftar (Maghrib)" ,
								fajrPrayerTime = formatter.format(maghribPrayerTime.value)
									 )
					}
				}
			}
		}
	}
}

//compoennt to show the fajr time with a label
@Composable
fun TimeComponent(title : String = "Suhoor Time" , fajrPrayerTime : String)
{
	Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(8.dp) ,
			verticalAlignment = Alignment.CenterVertically ,
			horizontalArrangement = Arrangement.SpaceBetween
	   ) {
		Text(text = title , style = MaterialTheme.typography.titleLarge)
		Text(text = fajrPrayerTime , style = MaterialTheme.typography.titleLarge)
	}
}
