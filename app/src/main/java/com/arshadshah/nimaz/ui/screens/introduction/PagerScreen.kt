package com.arshadshah.nimaz.ui.screens.introduction

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.PagerState

@Composable
fun PagerScreen(onBoardingPage : OnBoardingPage)
{
	Column(
			modifier = Modifier
				.fillMaxWidth() ,
			horizontalAlignment = Alignment.CenterHorizontally ,
			verticalArrangement = Arrangement.Top
		  ) {
		Image(
				modifier = Modifier
					.fillMaxWidth(0.5f)
					.fillMaxHeight(0.7f) ,
				imageVector = onBoardingPage.image ,
				contentDescription = "Pager Image"
			 )
		Text(
				modifier = Modifier
					.fillMaxWidth() ,
				text = onBoardingPage.title ,
				fontSize = MaterialTheme.typography.titleMedium.fontSize ,
				fontWeight = FontWeight.Bold ,
				textAlign = TextAlign.Center
			)
		Text(
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = 40.dp)
					.padding(top = 20.dp) ,
				text = onBoardingPage.description ,
				fontSize = MaterialTheme.typography.bodyMedium.fontSize ,
				fontWeight = FontWeight.Medium ,
				textAlign = TextAlign.Center
			)
		//the extra functionality compose
		onBoardingPage.extra.invoke()
	}
}

@ExperimentalAnimationApi
@ExperimentalPagerApi
@Composable
fun FinishButton(
	modifier : Modifier ,
	pagerState : PagerState ,
	onClick : () -> Unit ,
				)
{
	Row(
			modifier = modifier
				.padding(horizontal = 40.dp) ,
			verticalAlignment = Alignment.Top ,
			horizontalArrangement = Arrangement.Center
	   ) {
		AnimatedVisibility(
				modifier = Modifier.fillMaxWidth() ,
				visible = pagerState.currentPage == 5
						  ) {
			Button(
					onClick = onClick ,
					colors = ButtonDefaults.buttonColors(
							contentColor = Color.White
														)
				  ) {
				Text(text = "Finish")
			}
		}
	}
}