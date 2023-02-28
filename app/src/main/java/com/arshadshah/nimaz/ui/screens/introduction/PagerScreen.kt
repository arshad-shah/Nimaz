package com.arshadshah.nimaz.ui.screens.introduction

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.PagerState

@OptIn(ExperimentalPagerApi::class)
@Composable
fun PagerScreen(onBoardingPage : OnBoardingPage , position : Int)
{
	val hasExtra = onBoardingPage.extra != null
	Column(
			modifier = Modifier
				.fillMaxWidth()
				.fillMaxHeight()
				.testTag("pagerScreen $position") ,
			horizontalAlignment = Alignment.CenterHorizontally ,
			verticalArrangement = Arrangement.Center
		  ) {

		Text(
				modifier = Modifier
					.fillMaxWidth()
					//if fourth page than padding is 8 else 20 on bottom
					.padding(bottom = if (position == 4) 0.dp else 20.dp)
					.testTag("pagerScreenTitle") ,
				text = onBoardingPage.title ,
				fontSize = MaterialTheme.typography.headlineMedium.fontSize ,
				fontWeight = FontWeight.Bold ,
				textAlign = TextAlign.Center
			)

			Image(
					modifier = Modifier
						.fillMaxWidth(0.6f)
						//if onBoardingPage.extra is not {} then fill the height with 0.4f else fill the height with 0.6f
						.fillMaxHeight(if (hasExtra) 0.5f else if (position == 4) 0.4f else 0.6f)
						.testTag("pagerScreenImage") ,
					painter = painterResource(id = onBoardingPage.image) ,
					contentDescription = "Pager Image" ,
				 )

		Text(
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = 16.dp)
					.padding(top = 0.dp , bottom = 20.dp)
					.testTag("pagerScreenDescription") ,
				text = onBoardingPage.description ,
				fontSize = MaterialTheme.typography.bodyMedium.fontSize ,
				fontWeight = FontWeight.Medium ,
				textAlign = TextAlign.Center
			)
		//if onBoardingPage.extra is not {} then show the extra content
		if (onBoardingPage.extra != null)
		{
			ElevatedCard(
					modifier = Modifier
						.fillMaxWidth()
						.testTag("pagerScreenExtra") ,
						) {
				//the extra functionality compose
				onBoardingPage.extra.invoke()
			}
		}
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
				.padding(horizontal = 8.dp) ,
			verticalAlignment = Alignment.Top ,
			horizontalArrangement = Arrangement.Center
	   ) {
		AnimatedVisibility(
				visible = pagerState.currentPage == 7
						  ) {
			Button(
					onClick = onClick ,
					shape = MaterialTheme.shapes.medium ,
				  ) {
				Text(text = "Let's Get Started")
			}
		}
	}
}

@ExperimentalAnimationApi
@ExperimentalPagerApi
@Composable
fun BackButton(
	modifier : Modifier ,
	pagerState : PagerState ,
	onClick : () -> Unit ,
				)
{
	Row(
			modifier = modifier ,
			verticalAlignment = Alignment.Top ,
			horizontalArrangement = Arrangement.Start
	   ) {
		AnimatedVisibility(
				visible = pagerState.currentPage != 0
						  ) {
			Button(
					modifier = Modifier
						.padding(horizontal = 8.dp)
						.testTag("introBackButton") ,
					shape = MaterialTheme.shapes.medium ,
					onClick = onClick ,
				  ) {
				Text(text = "Back")
			}
		}
	}
}

@ExperimentalAnimationApi
@ExperimentalPagerApi
@Composable
fun NextButton(
	modifier : Modifier ,
	pagerState : PagerState ,
	onClick : () -> Unit ,
				)
{

		AnimatedVisibility(
				visible = pagerState.currentPage != 7
						  ) {
			Button(
					modifier = Modifier
						.padding(horizontal = 8.dp)
						.testTag("introNextButton") ,
					shape = MaterialTheme.shapes.medium ,
					onClick = onClick ,
				  ) {
				Text(text = "Next")
			}
		}
}