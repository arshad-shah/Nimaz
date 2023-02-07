package com.arshadshah.nimaz.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.ui.theme.quranFont

@Composable
fun ShahadahScreen(paddingValues : PaddingValues)
{

	Column(
			modifier = Modifier
				.padding(paddingValues)
				.fillMaxSize() ,
			horizontalAlignment = Alignment.CenterHorizontally ,
			verticalArrangement = Arrangement.Center
		  ) {

		ElevatedCard(
				modifier = Modifier
					.padding(8.dp) ,
					) {

			//Shadaah in arabic with diacritics
			CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
				Text(
						text = "أَشْهَدُ أَنْ لَا إِلَٰهَ إِلَّا ٱللَّٰهُ وَأَشْهَدُ أَنَّ مُحَمَّدًا رَسُولُ ٱللَّٰهِ" ,
						style = MaterialTheme.typography.headlineLarge ,
						fontFamily = quranFont ,
						textAlign = TextAlign.Center ,
						modifier = Modifier
							.padding(4.dp)
							.fillMaxWidth() ,
						color = MaterialTheme.colorScheme.onSurface ,
					)
			}
			//Shadaah transliteration
			Text(
					text = "Ash-hadu an la ilaha illa Allah wa ash-hadu anna Muhammadan Rasulullah" ,
					style = MaterialTheme.typography.titleLarge ,
					textAlign = TextAlign.Left ,
					modifier = Modifier
						.padding(4.dp)
						.fillMaxWidth() ,
					color = MaterialTheme.colorScheme.onSurface ,
				)

			//Shadaah in english
			Text(
					text = "I bear witness that there is no god but Allah and I bear witness that Muhammad is the messenger of Allah" ,
					style = MaterialTheme.typography.titleLarge ,
					textAlign = TextAlign.Left ,
					modifier = Modifier
						.padding(4.dp)
						.fillMaxWidth() ,
					color = MaterialTheme.colorScheme.onSurface ,
				)

		}
	}
}