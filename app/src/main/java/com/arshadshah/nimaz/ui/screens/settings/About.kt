package com.arshadshah.nimaz.ui.screens.settings

import androidx.compose.foundation.Image
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.BuildConfig
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_ABOUT_PAGE
import com.arshadshah.nimaz.ui.components.settings.AuthorDetails

@Composable
fun About(paddingValues : PaddingValues)
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
		AppDetails()
		AuthorDetails()
	}
}

@Composable
fun AppDetails()
{
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
					//ceircluar image
					Image(
							painter = painterResource(id = R.drawable.logo) ,
							contentDescription = "App Icon" ,
							modifier = Modifier
								.padding(8.dp)
								.size(100.dp)
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
