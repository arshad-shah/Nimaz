package com.arshadshah.nimaz.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import com.arshadshah.nimaz.ui.components.bLogic.prayerTimes.FeatureCard

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
	FeatureCard(
			paddingValues = paddingValues ,
			onNavigateToTasbihScreen = onNavigateToTasbihScreen ,
			onNavigateToNames = onNavigateToNames ,
			onNavigateToListOfTasbeeh = onNavigateToListOfTasbeeh ,
			onNavigateToShadah = onNavigateToShadah ,
			onNavigateToZakat = onNavigateToZakat ,
			onNavigateToPrayerTracker = onNavigateToPrayerTracker ,
			onNavigateToCalender = onNavigateToCalender ,
			onNavigateToQibla = onNavigateToQibla ,
			onNavigateToTasbihListScreen = onNavigateToTasbihListScreen ,
			   )
}