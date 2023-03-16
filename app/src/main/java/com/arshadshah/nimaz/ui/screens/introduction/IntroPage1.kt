package com.arshadshah.nimaz.ui.screens.introduction

import android.content.Intent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.activities.Introduction
import com.arshadshah.nimaz.activities.MainActivity
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.accompanist.pager.rememberPagerState
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.launch


@OptIn(ExperimentalPagerApi::class , ExperimentalAnimationApi::class)
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

	Column(modifier = Modifier
		.fillMaxSize()
		.padding(if (pages[pagerState.currentPage].extra == {}) 8.dp else 20.dp)
		  ) {
		HorizontalPager(
				modifier = Modifier
					.weight(10f)
					.testTag("introPager") ,
				count = pages.size ,
				state = pagerState ,
				verticalAlignment = Alignment.Top
					   ) { position ->
			PagerScreen(onBoardingPage = pages[position] , position)
		}
		HorizontalPagerIndicator(
				pagerState = pagerState ,
				modifier = Modifier
					.align(Alignment.CenterHorizontally)
					//if the onBoardingPage.extra is not {} then add 20.dp padding else add 0.dp padding
					.padding(16.dp)
					.testTag("introPagerIndicator") ,
				activeColor = MaterialTheme.colorScheme.primary ,
				inactiveColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f) ,
				indicatorWidth = 10.dp ,
				indicatorHeight = 10.dp
								)


		Row(
				modifier = Modifier
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
						modifier = Modifier.padding(horizontal = 20.dp) ,
						pagerState = pagerState
						  ) {
					scope.launch {
						pagerState.animateScrollToPage(pagerState.currentPage - 1)
					}
				}
				FinishButton(
						modifier = Modifier , pagerState = pagerState
							) {
					//check if the settings were completed or not
					val isLocationSet =
						sharedPref.getData(AppConstants.LOCATION_INPUT , "").isNotBlank()
					val isNotificationSet =
						sharedPref.getDataBoolean(AppConstants.NOTIFICATION_ALLOWED , false)
					if (isLocationSet && isNotificationSet)
					{
						sharedPref.saveDataBoolean(AppConstants.IS_FIRST_INSTALL , false)
						context.startActivity(Intent(context , MainActivity::class.java))
						//remove the activity from the back stack
						(context as Introduction).finish()
					} else if (!isLocationSet)
					{
						Toasty.error(context , "Please set your location in settings" , Toasty.LENGTH_SHORT)
							.show()
						sharedPref.saveDataBoolean(AppConstants.IS_FIRST_INSTALL , false)
						context.startActivity(Intent(context , MainActivity::class.java))
						//remove the activity from the back stack
						(context as Introduction).finish()
					} else if (!isNotificationSet)
					{
						Toasty.error(
								context ,
								"Please allow notifications in settings" ,
								Toasty.LENGTH_SHORT
									)
							.show()
						sharedPref.saveDataBoolean(AppConstants.IS_FIRST_INSTALL , false)
						context.startActivity(Intent(context , MainActivity::class.java))
						//remove the activity from the back stack
						(context as Introduction).finish()
					}
				}
			} else
			{
				BackButton(
						modifier = Modifier.padding(horizontal = 20.dp) ,
						pagerState = pagerState
						  ) {
					scope.launch {
						pagerState.animateScrollToPage(pagerState.currentPage - 1)
					}
				}
				NextButton(
						modifier = Modifier
							.padding(horizontal = 20.dp)
							.testTag("introNextButton") ,
						pagerState = pagerState
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