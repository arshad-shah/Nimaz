package com.arshadshah.nimaz.ui.screens.introduction

import android.content.Intent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.activities.Introduction
import com.arshadshah.nimaz.activities.MainActivity
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.accompanist.pager.rememberPagerState


@OptIn(ExperimentalPagerApi::class , ExperimentalAnimationApi::class)
@Composable
fun IntroPage1()
{
	val context = LocalContext.current
	val pages = listOf(
			OnBoardingPage.First ,
			OnBoardingPage.Second ,
			OnBoardingPage.Third ,
			OnBoardingPage.Fourth ,
			OnBoardingPage.Fifth ,
			OnBoardingPage.Sixth ,
					  )

	val pagerState = rememberPagerState()

	Column(modifier = Modifier.fillMaxSize()) {
		HorizontalPager(
				modifier = Modifier.weight(10f) ,
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
					.padding(16.dp) ,
								)
		FinishButton(
				modifier = Modifier.weight(1f) ,
				pagerState = pagerState ,
				onClick = {
					//save the lock to storage
					val sharedPref = PrivateSharedPreferences(context)
					sharedPref.saveDataBoolean("isFirstInstall" , false)
					context.startActivity(Intent(context , MainActivity::class.java))
					//remove the activity from the back stack
					(context as Introduction).finish()
				}
					)
	}
}