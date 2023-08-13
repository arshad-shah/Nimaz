package com.arshadshah.nimaz.ui.screens.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.BuildConfig
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_ABOUT_PAGE
import com.arshadshah.nimaz.ui.components.settings.AuthorDetails
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import es.dmoral.toasty.Toasty
import io.ktor.utils.io.concurrent.*

@Composable
fun About(paddingValues : PaddingValues , onImageClicked : () -> Unit)
{
	Column(
			modifier = Modifier
				.verticalScroll(rememberScrollState() , true)
				.padding(paddingValues)
				.fillMaxWidth()
				.fillMaxHeight()
				.testTag(TEST_TAG_ABOUT_PAGE) ,
			verticalArrangement = Arrangement.Center ,
			horizontalAlignment = Alignment.CenterHorizontally
		  ) {
		AppDetails(onImageClicked)
		AuthorDetails()
	}
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDetails(onImageClicked : () -> Unit)
{
	val context = LocalContext.current
	val sharedPref = PrivateSharedPreferences(LocalContext.current)
	//multiple click count
	val clickCount = remember {
		mutableStateOf(0)
	}
	val updateClickCount = {
		Toasty.info(context , "Click ${clickCount.value + 1} more times to enable debug mode")
			.show()
		clickCount.value = clickCount.value + 1
	}
	ElevatedCard(
			shape = MaterialTheme.shapes.extraLarge ,
			modifier = Modifier
				.padding(8.dp)
				.fillMaxWidth() ,
			content = {
				Column(
						modifier = Modifier.padding(8.dp) ,
						verticalArrangement = Arrangement.Center ,
						horizontalAlignment = Alignment.CenterHorizontally
					  ) {
					//circular image
					Image(
							painter = painterResource(id = R.drawable.logo) ,
							contentDescription = "App Icon" ,
							modifier = Modifier
								.padding(8.dp)
								.size(100.dp)
								.combinedClickable(
										onClick = {
											if (clickCount.value == 5)
											{
												Toasty
													.success(context , "Debug Mode Enabled")
													.show()
												sharedPref.saveDataBoolean("debug" , true)
												onImageClicked()
											} else
											{
												updateClickCount()
											}
										} ,
										onLongClick = {
											Toasty
												.info(context , "Debug Mode Disabled")
												.show()
											sharedPref.saveDataBoolean("debug" , false)
											clickCount.value = 0
										}
												  )
						 )
					Text(
							modifier = Modifier.padding(8.dp) ,
							text = "Nimaz" ,
							style = MaterialTheme.typography.titleLarge ,
						)
					Text(
							modifier = Modifier.padding(8.dp) ,
							text = "Version ${BuildConfig.VERSION_NAME}" ,
							style = MaterialTheme.typography.bodyMedium ,
						)
					Text(
							modifier = Modifier
								.padding(8.dp)
								.fillMaxWidth() ,
							text = "A free ,Ad-free , app for calculating prayer times, qibla direction, and more." ,
							style = MaterialTheme.typography.bodyMedium ,
							textAlign = TextAlign.Center
						)
				}
			})
}
