package com.arshadshah.nimaz.ui.screens.introduction

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import kotlinx.coroutines.launch


@OptIn(ExperimentalPagerApi::class)
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
					  )

	val pagerState = rememberPagerState()

	val scope = rememberCoroutineScope()

	Column(modifier = Modifier.fillMaxSize()) {
		HorizontalPager(
				modifier = Modifier
					.weight(10f)
					.testTag("introPager") ,
				count = pages.size ,
				state = pagerState ,
				verticalAlignment = Alignment.Top
					   ) { position ->
			PagerScreen(onBoardingPage = pages[position])
		}
		HorizontalPagerIndicator(
				pagerState = pagerState ,
				modifier = Modifier
					.align(Alignment.CenterHorizontally)
					.padding(20.dp)
					.testTag("introPagerIndicator") ,
				activeColor = MaterialTheme.colorScheme.secondary ,
				inactiveColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f) ,
				indicatorWidth = 12.dp ,
				indicatorHeight = 12.dp
								)
		Row(
				modifier = Modifier
					.weight(1f)
					.fillMaxWidth()
					.testTag("introButtons") ,
				//if we are on firts or last page than use space between else use end for page 1 and start for last page
				horizontalArrangement = when(pagerState.currentPage)
				{
					0 -> Arrangement.End
					else -> Arrangement.SpaceBetween
				} ,
				verticalAlignment = Alignment.CenterVertically ,
		   ) {
			//next and back buttons
			//next button
			//back button
			//back button
			//only show back button if not on first page
			if (pagerState.currentPage != 0)
			{
				Button(
						modifier = Modifier
							.padding(horizontal = 20.dp)
							.testTag("introBackButton") ,
						shape = MaterialTheme.shapes.medium ,
						onClick = {
							scope.launch {
								pagerState.animateScrollToPage(if (pagerState.currentPage == 0) pages.size - 1 else pagerState.currentPage - 1)
							}
						}
					  ) {
					Text(text = "Back")
				}
			}

			if (pagerState.currentPage != pages.size - 1)
			{
				Button(
						modifier = Modifier
							.padding(horizontal = 20.dp)
							.testTag("introNextButton") ,
						shape = MaterialTheme.shapes.medium ,
						onClick = {
							scope.launch {
								pagerState.animateScrollToPage(if (pagerState.currentPage == pages.size - 1) 0 else pagerState.currentPage + 1)
							}
						}
					  ) {
					Text(text = "Next")
				}
			}
			if (pagerState.currentPage == pages.size - 1)
			{
				val context = LocalContext.current
				val sharedPref = PrivateSharedPreferences(context)
				//a button to navigate to the main screen
				Button(
						onClick = {
							sharedPref.saveDataBoolean(AppConstants.IS_FIRST_INSTALL , false)
							context.startActivity(Intent(context , MainActivity::class.java))
							//remove the activity from the back stack
							(context as Introduction).finish()
						} ,
						modifier = Modifier
							.padding(horizontal = 20.dp),
						shape = MaterialTheme.shapes.medium ,
					  ) {
					Text(text = "Let's Get Started")
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