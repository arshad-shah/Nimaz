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
import kotlinx.coroutines.launch


@OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
fun IntroPage1() {
    val pages = listOf(
        OnBoardingPage.First,
        OnBoardingPage.Second,
        OnBoardingPage.Third,
        OnBoardingPage.Fourth,
        OnBoardingPage.Fifth,
        OnBoardingPage.Sixth,
        OnBoardingPage.Seventh,
        OnBoardingPage.Eighth,
    )

    val pagerState = rememberPagerState(
        0,
        0F
    ) { pages.size }

    val context = LocalContext.current
    val sharedPref = PrivateSharedPreferences(context)
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .background(color = MaterialTheme.colorScheme.background)
            .padding(bottom = 20.dp)
            .fillMaxSize()
    ) {

        HorizontalPager(
            modifier = Modifier
                .weight(10f)
                .testTag("introPager"),
            state = pagerState,
            userScrollEnabled = false,
            pageSize = PageSize.Fill,
            verticalAlignment = Alignment.Top
        ) { position ->
            PagerScreen(onBoardingPage = pages[position], position)
        }


        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .background(color = MaterialTheme.colorScheme.background)
                .fillMaxWidth()
                .testTag("introButtons"),
            //if we are on firts or last page than use space between else use end for page 1 and start for last page
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (pagerState.currentPage == pages.size - 1) {
                FinishButton(
                    modifier = Modifier.fillMaxWidth(),
                    pagerState = pagerState,
                ) {
                    sharedPref.saveDataBoolean(
                        AppConstants.IS_FIRST_INSTALL,
                        false
                    )
                    context.startActivity(
                        Intent(
                            context,
                            MainActivity::class.java
                        )
                    )
                    //remove the activity from the back stack
                    (context as Introduction).finish()
                }
            } else {
                NextButton(
                    modifier = Modifier.fillMaxWidth(),
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