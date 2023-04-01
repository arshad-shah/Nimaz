package com.arshadshah.nimaz.ui.screens.introduction

import android.content.Intent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.activities.Introduction
import com.arshadshah.nimaz.activities.MainActivity
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import kotlinx.coroutines.launch


@OptIn(ExperimentalAnimationApi::class , ExperimentalFoundationApi::class)
@Composable
fun IntroPage1()
{
	val pages = listOf(
			OnBoardingPage.First ,
			OnBoardingPage.Second ,
			OnBoardingPage.Third ,
			OnBoardingPage.Fourth ,
			OnBoardingPage.Fifth ,
			OnBoardingPage.Sixth ,
			OnBoardingPage.Seventh ,
			OnBoardingPage.Eighth ,
			OnBoardingPage.Ninth ,
					  )

	val pagerState = rememberPagerState()

	val context = LocalContext.current
	val sharedPref = PrivateSharedPreferences(context)
	val scope = rememberCoroutineScope()


	val areSettingsComplete = remember {
		mutableStateOf(false)
	}

	LaunchedEffect(key1 = pagerState.currentPage) {
		val isLocationSet =
			sharedPref.getData(AppConstants.LOCATION_INPUT , "").isNotBlank()
		val isNotificationSet =
			sharedPref.getDataBoolean(AppConstants.NOTIFICATION_ALLOWED , false)
		areSettingsComplete.value = pagerState.currentPage == pages.size - 1 && isLocationSet && isNotificationSet
	}

	Column(
			modifier = Modifier
				.padding(bottom = 20.dp)
				.fillMaxSize()
		  ) {
		HorizontalPager(
				userScrollEnabled = false ,
				modifier = Modifier
					.weight(10f)
					.testTag("introPager") ,
				pageCount = pages.size ,
				state = pagerState ,
				verticalAlignment = Alignment.Top
					   ) { position ->
			PagerScreen(onBoardingPage = pages[position] , position)
		}
		Row(
				Modifier
					.padding(vertical = 8.dp)
					.testTag("introPagerIndicator")
					.height(30.dp)
					.fillMaxWidth() ,
				horizontalArrangement = Arrangement.Center ,
				verticalAlignment = Alignment.CenterVertically
		   ) {
			repeat(pages.size) { iteration ->
				val color =
					if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary
					else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
				Box(
						modifier = Modifier
							.padding(4.dp)
							.clip(CircleShape)
							.background(color)
							.size(14.dp)

				   )
			}
		}


		Row(
				modifier = Modifier
					.padding(horizontal = 16.dp)
					.fillMaxWidth()
					.testTag("introButtons") ,
				//if we are on firts or last page than use space between else use end for page 1 and start for last page
				horizontalArrangement = when (pagerState.currentPage)
				{
					0 -> Arrangement.End
					else -> Arrangement.SpaceBetween
				} ,
				verticalAlignment = Alignment.CenterVertically ,
		   ) {
			if (pagerState.currentPage == pages.size - 1)
			{
				BackButton(
						modifier = Modifier ,
						pagerState = pagerState,
						  ) {
					scope.launch {
						pagerState.animateScrollToPage(pagerState.currentPage - 1)
					}
				}
				FinishButton(
						modifier = Modifier ,
						pagerState = pagerState,
						areSettingsComplete = areSettingsComplete.value,
							) {
					sharedPref.saveDataBoolean(AppConstants.IS_FIRST_INSTALL , false)
					context.startActivity(Intent(context , MainActivity::class.java))
					//remove the activity from the back stack
					(context as Introduction).finish()
				}
			} else
			{
				BackButton(
						modifier = Modifier.padding(horizontal = 20.dp) ,
						pagerState = pagerState ,
						  )
				{
					scope.launch {
						pagerState.animateScrollToPage(pagerState.currentPage - 1)
					}
				}
				NextButton(
						pagerState = pagerState,
						  ) {
					scope.launch {
						pagerState.animateScrollToPage(pagerState.currentPage + 1)
					}
				}
			}
		}
	}
}

@Preview
@Composable
fun IntroPage1Preview()
{
	NimazTheme {
		IntroPage1()
	}
}