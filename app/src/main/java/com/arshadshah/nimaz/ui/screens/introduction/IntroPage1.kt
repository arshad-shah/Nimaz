package com.arshadshah.nimaz.ui.screens.introduction

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.accompanist.pager.rememberPagerState


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