package com.arshadshah.nimaz.ui.screens.introduction

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.ui.theme.NimazTheme

@Composable
fun PagerScreen(onBoardingPage : OnBoardingPage)
{
	Column(
			modifier = Modifier
				.fillMaxWidth()
				.fillMaxHeight()
				.testTag("pagerScreen") ,
			horizontalAlignment = Alignment.CenterHorizontally ,
			verticalArrangement = Arrangement.Center
		  ) {

		Text(
				modifier = Modifier
					.fillMaxWidth()
					.testTag("pagerScreenTitle") ,
				text = onBoardingPage.title ,
				fontSize = MaterialTheme.typography.headlineMedium.fontSize ,
				fontWeight = FontWeight.Bold ,
				textAlign = TextAlign.Center
			)

		Image(
				modifier = Modifier
					.fillMaxWidth(0.5f)
					.fillMaxHeight(0.6f)
					.testTag("pagerScreenImage") ,
				imageVector = onBoardingPage.image ,
				contentDescription = "Pager Image" ,
				colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
			 )
		Text(
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = 40.dp)
					.padding(top = 20.dp , bottom = 20.dp)
					.testTag("pagerScreenDescription") ,
				text = onBoardingPage.description ,
				fontSize = MaterialTheme.typography.bodyMedium.fontSize ,
				fontWeight = FontWeight.Medium ,
				textAlign = TextAlign.Center
			)
		//if onBoardingPage.extra is not {} then show the extra content
		if (onBoardingPage.extra != {})
		{
			ElevatedCard(
					modifier = Modifier
						.padding(8.dp)
						.testTag("pagerScreenExtra") ,
						) {
				//the extra functionality compose
				onBoardingPage.extra.invoke()
			}
		}
	}
}

@Preview
@Composable
fun PagerScreenPreview()
{
	val pages = listOf(
			OnBoardingPage.Fourth ,
					  )
	NimazTheme {
		PagerScreen(pages[0])
	}
}