package com.arshadshah.nimaz.ui.components.common

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.activities.MainActivity
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.utils.CustomAnimation
import com.arshadshah.nimaz.utils.RouteUtils.checkRoute
import com.arshadshah.nimaz.utils.RouteUtils.processPageTitle
import com.arshadshah.nimaz.viewModel.NamesOfAllahViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTopBar(
    route: MutableState<String?>,
    navController: NavHostController,
    context: MainActivity
) {

    val viewModelNames = viewModel(
        key = AppConstants.NAMES_OF_ALLAH_VIEWMODEL_KEY,
        initializer = { NamesOfAllahViewModel() },
        viewModelStoreOwner = context
    )
    val playBackState = viewModelNames.playbackState.collectAsState()
    AnimatedVisibility(
        visible = checkRoute(route.value.toString()) && checkRouteForNotShow(route.value.toString()),
        enter = CustomAnimation.fadeIn(duration = AppConstants.SCREEN_ANIMATION_DURATION),
        exit = CustomAnimation.fadeOut(duration = AppConstants.SCREEN_ANIMATION_DURATION_Exit),
        content = {
            TopAppBar(
                modifier = Modifier

                    .testTag("topAppBar"),
                title = {
                    Text(
                        modifier = Modifier
                            .testTag("topAppBarText")
                            .padding(start = 8.dp),
                        text = processPageTitle(
                            route.value.toString(),
                            navController.currentBackStackEntry?.arguments
                        ),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    if (route.value.toString() != AppConstants.DASHBOARD_SCREEN && route.value.toString() != AppConstants.PRAYER_TIMES_SCREEN_ROUTE) {
                        OutlinedIconButton(
                            modifier = Modifier
                                .testTag("backButton")
                                .padding(start = 8.dp),
                            onClick = {
                                Log.d(
                                    AppConstants.MAIN_ACTIVITY_TAG,
                                    "onCreate:  back button pressed"
                                )
                                Log.d(
                                    AppConstants.MAIN_ACTIVITY_TAG,
                                    "onCreate:  navigating to ${navController.previousBackStackEntry?.destination?.route}"
                                )
                                navController.popBackStack()
                            }) {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                painter = painterResource(id = R.drawable.back_icon),
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    //only show the menu button if the title is Quran
                    when (route.value) {

                        AppConstants.NAMESOFALLAH_SCREEN_ROUTE -> {


                        }
                    }
                }
            )
        }
    )
}

//function that returns true for all routes that are not to show the top bar
fun checkRouteForNotShow(route: String): Boolean {
    val list = listOf(
        AppConstants.CALENDER_SCREEN_ROUTE,
        AppConstants.PRAYER_TRACKER_SCREEN_ROUTE,
        AppConstants.TASBIH_SCREEN_ROUTE,
        AppConstants.TASBIH_SCREEN_ROUTE,
        AppConstants.DASHBOARD_SCREEN,
        AppConstants.PRAYER_TIMES_SCREEN_ROUTE,
        "Intro"
    )
    return list.none { it == route }
}