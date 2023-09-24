package com.arshadshah.nimaz.ui.screens.more

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants

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
	val listOfLinks = listOf(
			 mapOf(
					  "title" to "Tasbih" ,
					  "icon" to R.drawable.counter_icon ,
				  ) ,
			 mapOf(
					  "title" to "Tasbih List" ,
					  "icon" to R.drawable.tasbih ,
				  ) ,
			 mapOf(
					  "title" to "Qibla" ,
					  "icon" to R.drawable.qibla ,
				  ) ,
			 mapOf(
					  "title" to "Names of Allah" ,
					  "icon" to R.drawable.names_of_allah ,
				  ) ,
			 mapOf(
					  "title" to "Duas" ,
					  "icon" to R.drawable.dua ,
				  ) ,
			 mapOf(
					  "title" to "Trackers" ,
					  "icon" to R.drawable.tracker_icon ,
				  ) ,
			 mapOf(
					  "title" to "Calender" ,
					  "icon" to R.drawable.calendar_icon ,
				  ) ,
			 mapOf(
					  "title" to "Shahadah" ,
					  "icon" to R.drawable.shahadah ,
				  ) ,
							)
	LazyColumn(
			 modifier = Modifier
				 .fillMaxSize()
				 .padding(paddingValues)
				 .testTag(AppConstants.TEST_TAG_MORE) ,
			 horizontalAlignment = Alignment.CenterHorizontally ,
			 verticalArrangement = Arrangement.Center
			  ) {
		items(listOfLinks.size) { index ->
			val item = listOfLinks[index]
			MoreScreenLink(
					 title = item["title"] as String ,
					 icon = {
						 Image(
								  painter = painterResource(id = item["icon"] as Int) ,
								  contentDescription = item["title"] as String ,
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
								  contentDescription = item["title"] as String ,
							 )
					 } ,
					 onClick = {
						 when (index)
						 {
							 0 -> onNavigateToTasbihScreen(
									  " " ,
									  " " ,
									  " " ,
									  " "
														  )

							 1 -> onNavigateToTasbihListScreen()
							 2 -> onNavigateToQibla()
							 3 -> onNavigateToNames()
							 4 -> onNavigateToListOfTasbeeh()
							 5 -> onNavigateToPrayerTracker()
							 6 -> onNavigateToCalender()
							 7 -> onNavigateToShadah()
						 }
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
	Card(
			 colors = CardDefaults.cardColors(
					  containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(elevation = 8.dp) ,
					  contentColor = MaterialTheme.colorScheme.onSurface ,
					  disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) ,
					  disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f) ,
											 ) ,
			 shape = MaterialTheme.shapes.extraLarge ,
			 modifier = Modifier
				 .padding(8.dp)
				 .fillMaxWidth()
				 .testTag(AppConstants.TEST_TAG_MORE_LINK.replace("{title}" , title))
				 .clip(MaterialTheme.shapes.extraLarge)
				 .clickable {
					 onClick()
				 } ,
		) {
		Row(
				 modifier = Modifier
					 .fillMaxWidth()
					 .padding(8.dp) ,
				 verticalAlignment = Alignment.CenterVertically ,
		   ) {
			icon()
			Spacer(modifier = Modifier.width(16.dp))
			Text(
					 text = title ,
					 style = MaterialTheme.typography.titleLarge ,
					 modifier = Modifier.weight(1f) ,
				)
			action()
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
