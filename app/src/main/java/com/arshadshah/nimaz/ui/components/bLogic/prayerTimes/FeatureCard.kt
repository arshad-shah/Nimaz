package com.arshadshah.nimaz.ui.components.bLogic.prayerTimes

import androidx.compose.runtime.Composable
import com.arshadshah.nimaz.ui.components.ui.prayerTimes.FeatureCardUI

@Composable
fun FeatureCard(
	onNavigateToTasbihScreen : (String) -> Unit ,
	onNavigateToNames : () -> Unit ,
			   )
{
	FeatureCardUI(
			onNavigateToTasbihScreen = onNavigateToTasbihScreen ,
			onNavigateToNames = onNavigateToNames ,
				 )
}