package com.arshadshah.nimaz.ui.components.common

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.activities.MainActivity
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.ui.components.quran.MoreMenu
import com.arshadshah.nimaz.ui.components.quran.TopBarMenu
import com.arshadshah.nimaz.utils.CustomAnimation
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.RouteUtils.checkRoute
import com.arshadshah.nimaz.utils.RouteUtils.processPageTitle
import com.arshadshah.nimaz.viewModel.NamesOfAllahViewModel
import com.arshadshah.nimaz.viewModel.PrayerTimesViewModel
import com.arshadshah.nimaz.viewModel.QuranViewModel
import com.arshadshah.nimaz.viewModel.SettingsViewModel
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTopBar(
    route: MutableState<String?>,
    navController: NavHostController,
    context: MainActivity
) {

    val sharedPreferencesRepository = remember { PrivateSharedPreferences(context) }
    val viewModelQuran = viewModel(
        key = AppConstants.QURAN_VIEWMODEL_KEY,
        initializer = { QuranViewModel(sharedPreferencesRepository) },
        viewModelStoreOwner = context
    )

    val viewModelNames = viewModel(
        key = AppConstants.NAMES_OF_ALLAH_VIEWMODEL_KEY,
        initializer = { NamesOfAllahViewModel() },
        viewModelStoreOwner = context
    )
    val settingViewModel = viewModel(
        key = AppConstants.SETTINGS_VIEWMODEL_KEY,
        initializer = { SettingsViewModel(context) },
        viewModelStoreOwner = context
    )

    val viewModel = viewModel(
        key = AppConstants.PRAYER_TIMES_VIEWMODEL_KEY,
        initializer = { PrayerTimesViewModel(context) },
        viewModelStoreOwner = context
    )

    val (menuOpen, setMenuOpen) = remember { mutableStateOf(false) }


    val playBackState = viewModelNames.playbackState.collectAsState()

    val isLoading = viewModel.isLoading.collectAsState()

    val locationName = settingViewModel.locationName.collectAsState()


    AnimatedVisibility(
        visible = checkRoute(route.value.toString()) && checkRouteForNotShow(route.value.toString()),
        enter = CustomAnimation.fadeIn(duration = AppConstants.SCREEN_ANIMATION_DURATION),
        exit = CustomAnimation.fadeOut(duration = AppConstants.SCREEN_ANIMATION_DURATION_Exit),
        content = {
            TopAppBar(
                modifier = Modifier

                    .testTag("topAppBar"),
                title = {
                    if (route.value == AppConstants.MY_QURAN_SCREEN_ROUTE || route.value == AppConstants.QURAN_AYA_SCREEN_ROUTE) {
                        val isSurah =
                            navController.currentBackStackEntry?.arguments?.getString(
                                "isSurah"
                            )
                                .toBoolean()
                        val number =
                            navController.currentBackStackEntry?.arguments?.getString(
                                "number"
                            )
                        TopBarMenu(
                            number = number!!.toInt(),
                            isSurah = isSurah,
                            getAllAyats = if (isSurah) viewModelQuran::getAllAyaForSurah else viewModelQuran::getAllAyaForJuz
                        )
                    } else {
                        if (route.value == AppConstants.DASHBOARD_SCREEN || route.value == AppConstants.PRAYER_TIMES_SCREEN_ROUTE) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 2.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {

                                Column(
                                    verticalArrangement = Arrangement.SpaceBetween,
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Row(
                                        modifier = Modifier.padding(2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.current_location_icon),
                                            contentDescription = "Current Location",
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            modifier = Modifier.padding(start = 4.dp),
                                            text = if (isLoading.value) "Loading..." else locationName.value,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.padding(2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.current_date_icon),
                                            contentDescription = "Current Location",
                                            modifier = Modifier.size(14.dp)
                                        )

                                        Text(
                                            modifier = Modifier.padding(start = 4.dp),
                                            textAlign = TextAlign.Start,
                                            text = "${
                                                LocalDate.now().format(
                                                    DateTimeFormatter.ofPattern(
                                                        "dd MMMM yyyy"
                                                    )
                                                )
                                            }, ${
                                                HijrahDate.from(
                                                    LocalDate.now()
                                                )
                                                    .format(
                                                        DateTimeFormatter.ofPattern("dd MMMM yyyy")
                                                    )
                                            }",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        } else {
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
                        }
                    }
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
                                navController.navigateUp()
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
                        AppConstants.QURAN_AYA_SCREEN_ROUTE,
                        AppConstants.MY_QURAN_SCREEN_ROUTE,
                        -> {
                            //open the menu
                            IconButton(onClick = {
                                setMenuOpen(
                                    true
                                )
                            }) {
                                Icon(
                                    modifier = Modifier.size(
                                        24.dp
                                    ),
                                    painter = painterResource(
                                        id = R.drawable.settings_sliders_icon
                                    ),
                                    contentDescription = "Menu"
                                )
                            }
                            MoreMenu(
                                menuOpen = menuOpen,
                                setMenuOpen = setMenuOpen,
                            )
                        }

                        AppConstants.NAMESOFALLAH_SCREEN_ROUTE -> {
                            if (
                                playBackState.value == NamesOfAllahViewModel.PlaybackState.PLAYING
                                ||
                                playBackState.value == NamesOfAllahViewModel.PlaybackState.PAUSED
                            ) {
                                IconButton(onClick = {
                                    viewModelNames.handleAudioEvent(
                                        NamesOfAllahViewModel.AudioEvent.Stop
                                    )
                                }
                                ) {
                                    Icon(
                                        modifier = Modifier.size(
                                            24.dp
                                        ),
                                        painter = painterResource(
                                            id = R.drawable.stop_icon
                                        ),
                                        contentDescription = "Stop playing"
                                    )
                                }
                            }
                            IconButton(onClick = {
                                if (playBackState.value != NamesOfAllahViewModel.PlaybackState.PLAYING) {
                                    viewModelNames.handleAudioEvent(
                                        NamesOfAllahViewModel.AudioEvent.Play(
                                            context
                                        )
                                    )
                                } else {
                                    viewModelNames.handleAudioEvent(
                                        NamesOfAllahViewModel.AudioEvent.Pause
                                    )
                                }
                            }
                            ) {
                                if (playBackState.value == NamesOfAllahViewModel.PlaybackState.PLAYING) {
                                    Icon(
                                        modifier = Modifier.size(
                                            24.dp
                                        ),
                                        painter = painterResource(
                                            id = R.drawable.pause_icon
                                        ),
                                        contentDescription = "Pause playing"
                                    )
                                } else {
                                    Icon(
                                        modifier = Modifier.size(
                                            24.dp
                                        ),
                                        painter = painterResource(
                                            id = R.drawable.play_icon
                                        ),
                                        contentDescription = "Play"
                                    )
                                }
                            }

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
        "Intro"
    )
    return list.none { it == route }
}