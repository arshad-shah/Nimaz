package com.arshadshah.nimaz.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants

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
			//website link
			LinkButton(
					painter = painterResource(id = R.drawable.counter_icon) ,
					content = "Tasbih" ,
					onClick = {
						onNavigateToTasbihScreen(" " , " " , " " , " ")
					} ,
					title = "Tasbih" ,
					  )
			LinkButton(
					painter = painterResource(id = R.drawable.tasbih) ,
					content = "Tasbih List" ,
					onClick = {
						onNavigateToTasbihListScreen()
					} ,
					title = "Tasbih List" ,
					  )
			LinkButton(
					painter = painterResource(id = R.drawable.qibla) ,
					content = "Qibla" ,
					onClick = {
						onNavigateToQibla()
					} ,
					title = "Qibla" ,
					  )

			//linkdIn link
			LinkButton(
					painter = painterResource(id = R.drawable.names_of_allah) ,
					content = "Names of Allah" ,
					onClick = {
						onNavigateToNames()
					} ,
					title = "Names of Allah" ,
					  )
			//email link
			LinkButton(
					painter = painterResource(id = R.drawable.dua) ,
					content = "Dua" ,
					onClick = {
						onNavigateToListOfTasbeeh()
					} ,
					title = "Duas" ,
					  )
			LinkButton(
					painter = painterResource(id = R.drawable.tracker_icon) ,
					content = "Trackers" ,
					onClick = {
						onNavigateToPrayerTracker()
					} ,
					title = "Trackers" ,
					  )

			LinkButton(
					painter = painterResource(id = R.drawable.calendar_icon) ,
					content = "Calender" ,
					onClick = {
						onNavigateToCalender()
					} ,
					title = "Calender" ,
					  )
			//TODO: IN PROGRESS
//		LinkButton(
//				icon = {
//					//get the icon from drawable folder
//					Icon(
//							modifier = Modifier.size(48.dp) ,
//							imageVector = FeatherIcons.Github ,
//							contentDescription = "Zakat Calculator" ,
//						)
//				} ,
//				onClick = {
//					onNavigateToZakat()
//				},
//				title = "Zakah Calculator"
//				  )
			LinkButton(
					painter = painterResource(id = R.drawable.shahadah) ,
					content = "Shahadah" ,
					onClick = {
						onNavigateToShadah()
					} ,
					title = "Shahadah"
					  )
		}

	}
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkButton(
	painter : Painter ,
	content : String ,
	onClick : () -> Unit ,
	title : String = "" ,
			  )
{
	ElevatedCard(
			shape = MaterialTheme.shapes.extraLarge ,
			modifier = Modifier
				.padding(8.dp)
				.fillMaxWidth()
				.testTag(AppConstants.TEST_TAG_MORE_LINK.replace("{title}" , title)) ,
			onClick = onClick
				) {
		Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(10.dp) ,
				horizontalArrangement = Arrangement.SpaceBetween ,
				verticalAlignment = Alignment.CenterVertically
		   ) {
			Image(
					modifier = Modifier
						.padding(start = 8.dp)
						.size(48.dp) ,
					painter = painter ,
					contentDescription = content
				 )

			Text(
					text = title ,
					modifier = Modifier.padding(start = 8.dp) ,
					style = MaterialTheme.typography.titleLarge ,
				)
			//an icon of arrow to indicate that it is a link
			Icon(
					modifier = Modifier
						.size(24.dp) ,
					painter = painterResource(id = R.drawable.angle_small_right_icon) ,
					contentDescription = "Link" ,
				)
		}
	}
}