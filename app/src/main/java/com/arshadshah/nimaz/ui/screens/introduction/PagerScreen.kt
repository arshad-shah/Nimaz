package com.arshadshah.nimaz.ui.screens.introduction

import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.viewModel.SettingsViewModel


@Composable
fun PagerScreen(onBoardingPage: OnBoardingPage, position: Int) {
    val hasExtra = onBoardingPage.extra != null
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(
                color = MaterialTheme.colorScheme.background
            )
            .testTag("pagerScreen $position"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Image(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                //if onBoardingPage.extra is not {} then fill the height with 0.4f else fill the height with 0.6f
                .fillMaxHeight(if (hasExtra) 0.5f else if (position == 4) 0.4f else 0.6f)
                .testTag("pagerScreenImage"),
            painter = painterResource(id = onBoardingPage.image),
            contentDescription = "Pager Image",
        )

        Text(
            modifier = Modifier
                .fillMaxWidth()
                //if fourth page than padding is 8 else 20 on bottom
                .padding(bottom = if (position == 4) 0.dp else 20.dp)
                .testTag("pagerScreenTitle"),
            text = onBoardingPage.title,
            fontSize = MaterialTheme.typography.headlineMedium.fontSize,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .padding(top = 0.dp, bottom = 20.dp)
                .testTag("pagerScreenDescription"),
            text = onBoardingPage.description,
            fontSize = MaterialTheme.typography.bodyMedium.fontSize,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
        )
        //if onBoardingPage.extra is not {} then show the extra content
        if (onBoardingPage.extra != null) {
            ElevatedCard(
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .fillMaxWidth()
                    .testTag("pagerScreenExtra"),
            ) {
                //the extra functionality compose
                onBoardingPage.extra.invoke()
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@ExperimentalAnimationApi
@Composable
fun FinishButton(
    modifier: Modifier,
    pagerState: PagerState,
    onClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = pagerState.currentPage == 7
    ) {
        Button(
            modifier = modifier
                .padding(horizontal = 8.dp)
                .testTag("introFinishButton"),
            onClick = onClick,
        ) {
            Text(
                text = "Finish",
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@ExperimentalAnimationApi
@Composable
fun NextButton(
    modifier: Modifier,
    pagerState: PagerState,
    onClick: () -> Unit,
) {

    val context = LocalContext.current
    val viewModel = viewModel(
        key = AppConstants.SETTINGS_VIEWMODEL_KEY,
        initializer = { SettingsViewModel(context) },
        viewModelStoreOwner = context as ComponentActivity
    )

    val locationName = remember {
        viewModel.locationName
    }.collectAsState()
    val longitude = remember {
        viewModel.longitude
    }.collectAsState()
    val latitude = remember {
        viewModel.latitude
    }.collectAsState()
    val notificationAllowed = remember {
        viewModel.areNotificationsAllowed
    }.collectAsState()

    //is location page
    val isLocationPage = pagerState.currentPage == 4
    val isNotificationPage = pagerState.currentPage == 3

    val isButtonEnabled = remember {
        mutableStateOf(!isLocationPage)
    }

    val textForButton = remember {
        mutableStateOf("Next")
    }

    LaunchedEffect(locationName.value, longitude.value, latitude.value, isLocationPage) {
        if (isLocationPage) {
            //if locationName is not empty then go to next page
            isButtonEnabled.value =
                locationName.value.isNotEmpty() || (longitude.value != 0.0 && latitude.value != 0.0)
        }
    }

    LaunchedEffect(isNotificationPage, notificationAllowed.value) {
        if (isNotificationPage) {
            textForButton.value = if (notificationAllowed.value) "Next" else "Skip"
        } else {
            textForButton.value = "Next"
        }
    }
    AnimatedVisibility(
        visible = pagerState.currentPage != 7
    ) {
        Button(
            modifier = modifier
                .padding(horizontal = 8.dp)
                .testTag("introNextButton"),
            onClick = onClick,
            enabled = isButtonEnabled.value
        ) {
            Text(text = textForButton.value)
        }
    }
}