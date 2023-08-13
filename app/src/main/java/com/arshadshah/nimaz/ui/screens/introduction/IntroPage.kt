package com.arshadshah.nimaz.ui.screens.introduction

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.arshadshah.nimaz.activities.Introduction
import com.arshadshah.nimaz.activities.MainActivity
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.compose.OnParticleSystemUpdateListener
import nl.dionsegijn.konfetti.core.Angle
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.PartySystem
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.core.models.Size
import java.util.concurrent.*


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
					  )

	val pagerState = rememberPagerState(
			0 ,
			0F
									   ) { pages.size }

	val context = LocalContext.current
	val sharedPref = PrivateSharedPreferences(context)
	val scope = rememberCoroutineScope()

	val party = Party(
			speed = 20f ,
			maxSpeed = 40f ,
			damping = 0.9f ,
			angle = Angle.RIGHT - 45 ,
			spread = 45 ,
			size = listOf(
					Size.SMALL ,
					Size.MEDIUM ,
					Size.LARGE ,
						 ) ,
			colors = listOf(
					0xfce18a ,
					0xff726d ,
					0xf4306d ,
					0xb48def ,
					0x6a4c93 ,
					0x3f2c6f ,
					0x1d1b3d
						   ) ,
			emitter = Emitter(duration = 150 , TimeUnit.MILLISECONDS).max(150) ,
			position = Position.Relative(0.0 , 0.5)
					 )

	val startParty = remember {
		mutableStateOf(false)
	}

	Column(
			modifier = Modifier
				.background(color = MaterialTheme.colorScheme.background)
				.padding(bottom = 20.dp)
				.fillMaxSize()
		  ) {

		HorizontalPager(
				modifier = Modifier
					.weight(10f)
					.testTag("introPager") ,
				state = pagerState ,
				userScrollEnabled = false ,
				pageSize = PageSize.Fill ,
				verticalAlignment = Alignment.Top
					   ) { position ->
			PagerScreen(onBoardingPage = pages[position] , position)
			if (startParty.value)
			{
				KonfettiView(
						modifier = Modifier
							.fillMaxSize()
							.zIndex(2f) ,
						parties = listOf(
								party ,
								party.copy(
										angle = party.angle - 90 , // flip angle from right to left
										position = Position.Relative(1.0 , 0.5)
										  )
										) ,
						updateListener = object : OnParticleSystemUpdateListener
						{
							override fun onParticleSystemEnded(
								system : PartySystem ,
								activeSystems : Int ,
															  )
							{
								if (activeSystems == 0)
								{
									startParty.value = false
									sharedPref.saveDataBoolean(
											AppConstants.IS_FIRST_INSTALL ,
											false
															  )
									context.startActivity(
											Intent(
													context ,
													MainActivity::class.java
												  )
														 )
									//remove the activity from the back stack
									(context as Introduction).finish()
								}
							}
						}
							)
			}
		}


		Row(
				modifier = Modifier
					.padding(horizontal = 16.dp)
					.background(color = MaterialTheme.colorScheme.background)
					.fillMaxWidth()
					.testTag("introButtons") ,
				//if we are on firts or last page than use space between else use end for page 1 and start for last page
				horizontalArrangement = Arrangement.Center ,
				verticalAlignment = Alignment.CenterVertically ,
		   ) {
			if (pagerState.currentPage == pages.size - 1)
			{
				FinishButton(
						modifier = Modifier.fillMaxWidth() ,
						pagerState = pagerState ,
							) {
					startParty.value = true
				}
			} else
			{
				NextButton(
						modifier = Modifier.fillMaxWidth() ,
						pagerState = pagerState ,
						  ) {
					scope.launch {
						pagerState.animateScrollToPage(pagerState.currentPage + 1)
					}
				}
			}
		}
	}
}

@Preview(
		showBackground = true , showSystemUi = true ,
		uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL ,
		device = "id:S20 Fe"
		)
@Composable
fun IntroPage1Preview()
{
	NimazTheme {
		IntroPage1()
	}
}