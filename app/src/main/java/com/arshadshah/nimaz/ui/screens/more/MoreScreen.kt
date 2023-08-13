package com.arshadshah.nimaz.ui.screens.more

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.ui.components.settings.SettingsMenuLink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
	paddingValues : PaddingValues ,
	onNavigateToTasbihScreen : (String , String , String , String) -> Unit ,
	onNavigateToNames : () -> Unit ,
	onNavigateToListOfTasbeeh : () -> Unit ,
	onNavigateToShadah : () -> Unit ,
	onNavigateToZakat : () -> Unit ,
	onNavigateToPrayerTracker : () -> Unit ,
	onNavigateToCalender : () -> Unit ,
	onNavigateToQibla : () -> Unit ,
	onNavigateToTasbihListScreen : () -> Unit ,
			  )
{
	LazyColumn(
			modifier = Modifier
				.fillMaxSize()
				.padding(paddingValues)
				.testTag(AppConstants.TEST_TAG_MORE) ,
			horizontalAlignment = Alignment.CenterHorizontally ,
			verticalArrangement = Arrangement.Center
			  ) {
		item {
			MoreScreenLink(
					title = "Tasbih" ,
					icon = {
						Image(
								painter = painterResource(id = R.drawable.counter_icon) ,
								contentDescription = "Tasbih" ,
								modifier = Modifier
									.size(48.dp)
									.padding(4.dp)
							 )
					} ,
					action = {
						Icon(
								modifier = Modifier
									.size(24.dp) ,
								painter = painterResource(id = R.drawable.angle_small_right_icon) ,
								contentDescription = "Tasbih page" ,
							)
					} ,
					onClick = {
						onNavigateToTasbihScreen(
								" " ,
								" " ,
								" " ,
								" "
												)
					}
						  )
		}
		item {
			//tasbih list link
			MoreScreenLink(
					title = "Tasbih List" ,
					icon = {
						Image(
								painter = painterResource(id = R.drawable.tasbih) ,
								contentDescription = "Tasbih List" ,
								modifier = Modifier
									.size(48.dp)
									.padding(4.dp)
							 )
					} ,
					action = {
						Icon(
								modifier = Modifier
									.size(24.dp) ,
								painter = painterResource(id = R.drawable.angle_small_right_icon) ,
								contentDescription = "Tasbih List page" ,
							)
					} ,
					onClick = {
						onNavigateToTasbihListScreen()
					}
						  )
		}
		item {
			//Qibla link
			MoreScreenLink(
					title = "Qibla" ,
					icon = {
						Image(
								painter = painterResource(id = R.drawable.qibla) ,
								contentDescription = "Qibla" ,
								modifier = Modifier
									.size(48.dp)
									.padding(4.dp)
							 )
					} ,
					action = {
						Icon(
								modifier = Modifier
									.size(24.dp) ,
								painter = painterResource(id = R.drawable.angle_small_right_icon) ,
								contentDescription = "Qibla page" ,
							)
					} ,
					onClick = {
						onNavigateToQibla()
					}
						  )
		}
		item {
			//names of allah link
			MoreScreenLink(
					title = "Names of Allah" ,
					icon = {
						Image(
								painter = painterResource(id = R.drawable.names_of_allah) ,
								contentDescription = "Names of Allah" ,
								modifier = Modifier
									.size(48.dp)
									.padding(4.dp)
							 )
					} ,
					action = {
						Icon(
								modifier = Modifier
									.size(24.dp) ,
								painter = painterResource(id = R.drawable.angle_small_right_icon) ,
								contentDescription = "Names of Allah page" ,
							)
					} ,
					onClick = {
						onNavigateToNames()
					}
						  )
		}
		item {
			//Duas link
			MoreScreenLink(
					title = "Duas" ,
					icon = {
						Image(
								painter = painterResource(id = R.drawable.dua) ,
								contentDescription = "Duas" ,
								modifier = Modifier
									.size(48.dp)
									.padding(4.dp)
							 )
					} ,
					action = {
						Icon(
								modifier = Modifier
									.size(24.dp) ,
								painter = painterResource(id = R.drawable.angle_small_right_icon) ,
								contentDescription = "Duas page" ,
							)
					} ,
					onClick = {
						onNavigateToListOfTasbeeh()
					}
						  )
		}
		item {
			//Trackers link
			MoreScreenLink(
					title = "Trackers" ,
					icon = {
						Image(
								painter = painterResource(id = R.drawable.tracker_icon) ,
								contentDescription = "Trackers" ,
								modifier = Modifier
									.size(48.dp)
									.padding(4.dp)
							 )
					} ,
					action = {
						Icon(
								modifier = Modifier
									.size(24.dp) ,
								painter = painterResource(id = R.drawable.angle_small_right_icon) ,
								contentDescription = "Trackers page" ,
							)
					} ,
					onClick = {
						onNavigateToPrayerTracker()
					}
						  )
		}
		item {
			//Calender link
			MoreScreenLink(
					title = "Calender" ,
					icon = {
						Image(
								painter = painterResource(id = R.drawable.calendar_icon) ,
								contentDescription = "Calender" ,
								modifier = Modifier
									.size(48.dp)
									.padding(4.dp)
							 )
					} ,
					action = {
						Icon(
								modifier = Modifier
									.size(24.dp) ,
								painter = painterResource(id = R.drawable.angle_small_right_icon) ,
								contentDescription = "Calender page" ,
							)
					} ,
					onClick = {
						onNavigateToCalender()
					}
						  )
		}
		item {
			//Shahadah link
			MoreScreenLink(
					title = "Shahadah" ,
					icon = {
						Image(
								painter = painterResource(id = R.drawable.shahadah) ,
								contentDescription = "Shahadah" ,
								modifier = Modifier
									.size(48.dp)
									.padding(4.dp)
							 )
					} ,
					action = {
						Icon(
								modifier = Modifier
									.size(24.dp) ,
								painter = painterResource(id = R.drawable.angle_small_right_icon) ,
								contentDescription = "Shahadah page" ,
							)
					} ,
					onClick = {
						onNavigateToShadah()
					}
						  )
		}
	}
}


//component to display the link to avoid code duplication
@Composable
fun MoreScreenLink(
	title : String ,
	icon : @Composable () -> Unit ,
	action : @Composable () -> Unit ,
	onClick : () -> Unit ,
				  )
{
	//Shahadah link
	ElevatedCard(
			shape = MaterialTheme.shapes.extraLarge ,
			modifier = Modifier
				.padding(8.dp)
				.fillMaxWidth()
				.testTag(AppConstants.TEST_TAG_MORE_LINK.replace("{title}" , title)) ,
				) {
		SettingsMenuLink(
				title = {
					Text(
							text = title ,
						)
				} ,
				icon = {
					icon()
				} ,
				action = {
					action()
				}
						) {
			onClick()
		}
	}
}

@Preview
@Composable
fun MoreScreenPreview()
{
	MoreScreen(
			paddingValues = PaddingValues(0.dp) ,
			onNavigateToTasbihScreen = { s1 , s2 , s3 , s4 -> } ,
			onNavigateToNames = { } ,
			onNavigateToListOfTasbeeh = { } ,
			onNavigateToShadah = { } ,
			onNavigateToZakat = { } ,
			onNavigateToPrayerTracker = { } ,
			onNavigateToCalender = { } ,
			onNavigateToQibla = { } ,
			onNavigateToTasbihListScreen = { } ,
			  )
}
