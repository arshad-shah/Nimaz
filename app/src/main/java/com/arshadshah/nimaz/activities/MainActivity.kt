package com.arshadshah.nimaz.activities

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.ABOUT_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.APP_UPDATE_REQUEST_CODE
import com.arshadshah.nimaz.constants.AppConstants.CALENDER_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.CHAPTERS_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.CHAPTER_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.LICENCES_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.MAIN_ACTIVITY_TAG
import com.arshadshah.nimaz.constants.AppConstants.MY_QURAN_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.NAMESOFALLAH_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.NAMES_OF_ALLAH_VIEWMODEL_KEY
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_TIMES_SETTINGS_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_TRACKER_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.QIBLA_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.QURAN_AYA_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.QURAN_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.SCREEN_ANIMATION_DURATION
import com.arshadshah.nimaz.constants.AppConstants.SCREEN_ANIMATION_DURATION_Exit
import com.arshadshah.nimaz.constants.AppConstants.SETTINGS_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.SHAHADAH_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.TASBIH_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.TASBIH_VIEWMODEL_KEY
import com.arshadshah.nimaz.constants.AppConstants.WEB_VIEW_SCREEN_ROUTE
import com.arshadshah.nimaz.ui.components.quran.MoreMenu
import com.arshadshah.nimaz.ui.components.quran.MoreMenuMain
import com.arshadshah.nimaz.ui.components.quran.TopBarMenu
import com.arshadshah.nimaz.ui.navigation.BottomNavigationBar
import com.arshadshah.nimaz.ui.navigation.NavigationGraph
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.arshadshah.nimaz.utils.*
import com.arshadshah.nimaz.viewModel.NamesOfAllahViewModel
import com.arshadshah.nimaz.viewModel.SettingsViewModel
import com.arshadshah.nimaz.viewModel.TasbihViewModel
import com.arshadshah.nimaz.viewModel.TrackerViewModel
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity()
{

	//on resume to check if the update is stalled
	override fun onResume()
	{
		super.onResume()
		Log.d(MAIN_ACTIVITY_TAG , "onResume:  called")

		if (PrivateSharedPreferences(this).getDataBoolean(AppConstants.LOCATION_TYPE , true))
		{
			if (! AutoLocationUtils.isInitialized())
			{
				AutoLocationUtils.init(this)
				Log.d(MAIN_ACTIVITY_TAG , "onResume:  location is initialized")
			}
			AutoLocationUtils.startLocationUpdates()
		}

		val appUpdateManager = AppUpdateManagerFactory.create(this)
		appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
			if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS)
			{
				Log.d(MAIN_ACTIVITY_TAG , "onResume:  update is installed")
				appUpdateManager.startUpdateFlowForResult(
						appUpdateInfo ,
						AppUpdateType.IMMEDIATE ,
						this ,
						APP_UPDATE_REQUEST_CODE
														 )
			}
		}
	}

	override fun onDestroy()
	{
		super.onDestroy()
		if (PrivateSharedPreferences(this).getDataBoolean(AppConstants.LOCATION_TYPE , true))
		{
			AutoLocationUtils.stopLocationUpdates()
			Log.d(MAIN_ACTIVITY_TAG , "onDestroy:  location is stopped")
		}
	}

	override fun onPause()
	{
		super.onPause()
		if (PrivateSharedPreferences(this).getDataBoolean(AppConstants.LOCATION_TYPE , true))
		{
			AutoLocationUtils.stopLocationUpdates()
			Log.d(MAIN_ACTIVITY_TAG , "onPause:  location is stopped")
		}
	}

	@OptIn(
			ExperimentalMaterial3Api::class , ExperimentalAnimationApi::class
		  )
	@RequiresApi(Build.VERSION_CODES.S)
	override fun onCreate(savedInstanceState : Bundle?)
	{
		this.actionBar?.hide()

		if (! LocalDataStore.isInitialized())
		{
			LocalDataStore.init(this)
			Log.d(MAIN_ACTIVITY_TAG , "onCreate:  called and local data store initialized")
		}

		if (! FirebaseLogger.isInitialized())
		{
			FirebaseLogger.init()
			Log.d(MAIN_ACTIVITY_TAG , "onCreate:  called and firebase logger initialized")
		}



		super.onCreate(savedInstanceState)

		//this is used to show the full activity on the screen
		setContent {

			val viewModelSettings = viewModel(
					key = AppConstants.SETTINGS_VIEWMODEL_KEY ,
					initializer = { SettingsViewModel(this@MainActivity) } ,
					viewModelStoreOwner = this as ComponentActivity
											 )
			val themeState = remember {
				viewModelSettings.theme
			}.collectAsState()
			val isDarkTheme = remember {
				viewModelSettings.isDarkMode
			}.collectAsState()

			val darkTheme = remember {
				mutableStateOf(false)
			}
			val dynamicTheme = remember {
				mutableStateOf(false)
			}
			val themeName = remember {
				mutableStateOf("Default")
			}

			when (themeState.value)
			{
				"SYSTEM" ->
				{
					dynamicTheme.value = true
					darkTheme.value = isSystemInDarkTheme()
					themeName.value = "Default"
				}

				"DEFAULT" ->
				{
					dynamicTheme.value = false
					darkTheme.value = isDarkTheme.value
					themeName.value = "Default"
				}

				"Raisin_Black" ->
				{
					dynamicTheme.value = false
					darkTheme.value = isDarkTheme.value
					themeName.value = "Raisin_Black"
				}

				"Dark_Red" ->
				{
					dynamicTheme.value = false
					darkTheme.value = isDarkTheme.value
					themeName.value = "Dark_Red"
				}

				"Rustic_brown" ->
				{
					dynamicTheme.value = false
					darkTheme.value = isDarkTheme.value
					themeName.value = "Rustic_brown"
				}
			}

			NimazTheme(
					darkTheme = darkTheme.value ,
					dynamicColor = dynamicTheme.value ,
					ThemeName = themeName.value
					  ) {
				val navController = rememberAnimatedNavController()
				val route =
					remember(navController) { mutableStateOf(navController.currentDestination?.route) }
				navController.addOnDestinationChangedListener { _ , destination , _ ->
					route.value = destination.route
				}
				val (menuOpen , setMenuOpen) = remember { mutableStateOf(false) }
				val (menuOpen2 , setMenuOpen2) = remember { mutableStateOf(false) }
				val CustomAnimation = remember { CustomAnimation() }

				val vibrationAllowed = remember { mutableStateOf(true) }
				val rOrl = remember { mutableStateOf(true) }

				val snackbarHostState = remember { SnackbarHostState() }
				val scope = rememberCoroutineScope()

				//check for network connection
				val networkConnection =
					remember { mutableStateOf(NetworkChecker().networkCheck(this@MainActivity)) }

				LaunchedEffect(networkConnection.value) {
					if (! networkConnection.value)
					{
						scope.launch {
							snackbarHostState.showSnackbar(
									"No internet connection" ,
									duration = SnackbarDuration.Indefinite ,
									withDismissAction = true
														  )
						}
					}
				}

				val viewModelTasbih = viewModel(
						key = TASBIH_VIEWMODEL_KEY ,
						initializer = { TasbihViewModel(this@MainActivity) } ,
						viewModelStoreOwner = LocalContext.current as ComponentActivity
											   )
				val viewModelNames = viewModel(
						key = NAMES_OF_ALLAH_VIEWMODEL_KEY ,
						initializer = { NamesOfAllahViewModel() } ,
						viewModelStoreOwner = LocalContext.current as ComponentActivity
											  )

				val viewModelTracker = viewModel(
						key = AppConstants.TRACKING_VIEWMODEL_KEY ,
						initializer = { TrackerViewModel() } ,
						viewModelStoreOwner = LocalContext.current as androidx.activity.ComponentActivity
												)

				val isMenstruatingState = remember {
					viewModelTracker.isMenstrauting
				}.collectAsState()


				val isPlaying = remember {
					viewModelNames.isPlaying
				}.collectAsState()
				val isPaused = remember {
					viewModelNames.isPaused
				}.collectAsState()
				val isStopped = remember {
					viewModelNames.isStopped
				}.collectAsState()
				val isPlayingState = remember {
					mutableStateOf(false)
				}
				val isPausedState = remember {
					mutableStateOf(false)
				}
				val isStoppedState = remember {
					mutableStateOf(false)
				}
				LaunchedEffect(
						key1 = isPlaying.value ,
						key2 = isPaused.value ,
						key3 = isStopped.value
							  ) {
					Log.d(
							MAIN_ACTIVITY_TAG ,
							"onCreate: isPlaying: ${isPlaying.value} isPaused: ${isPaused.value} isStopped: ${isStopped.value}"
						 )
					isPlayingState.value = isPlaying.value
					isPausedState.value = isPaused.value
					isStoppedState.value = isStopped.value
				}
				Scaffold(
						modifier = Modifier.testTag("mainActivity") ,
						snackbarHost = { SnackbarHost(snackbarHostState) } ,
						topBar = {
							AnimatedVisibility(
									visible = checkRoute(route.value.toString()) ,
									enter = CustomAnimation.fadeIn(duration = SCREEN_ANIMATION_DURATION) ,
									exit = CustomAnimation.fadeOut(duration = SCREEN_ANIMATION_DURATION_Exit) ,
									content = {
										TopAppBar(
												modifier = Modifier
													.testTag("topAppBar") ,
												title = {
													if (route.value == MY_QURAN_SCREEN_ROUTE || route.value == QURAN_AYA_SCREEN_ROUTE)
													{
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
																number = number !!.toInt() ,
																isSurah = isSurah
																  )
													} else
													{
														Text(
																modifier = Modifier
																	.testTag("topAppBarText")
																	.padding(start = 8.dp) ,
																text = processPageTitle(
																		route.value.toString() ,
																		navController
																					   ) ,
																style = MaterialTheme.typography.titleLarge
															)
													}
												} ,
												navigationIcon = {
													OutlinedIconButton(
															modifier = Modifier
																.testTag("backButton")
																.padding(start = 8.dp) ,
															onClick = {
																Log.d(
																		MAIN_ACTIVITY_TAG ,
																		"onCreate:  back button pressed"
																	 )
																Log.d(
																		MAIN_ACTIVITY_TAG ,
																		"onCreate:  navigating to ${navController.previousBackStackEntry?.destination?.route}"
																	 )
																navController.navigateUp()
															}) {
														Icon(
																modifier = Modifier.size(24.dp) ,
																painter = painterResource(id = R.drawable.back_icon) ,
																contentDescription = "Back"
															)
													}
												} ,
												actions = {
													//only show the menu button if the title is Quran
													when (route.value)
													{
														QURAN_AYA_SCREEN_ROUTE ,
														MY_QURAN_SCREEN_ROUTE ,
														->
														{
															//open the menu
															IconButton(onClick = { setMenuOpen(true) }) {
																Icon(
																		modifier = Modifier.size(24.dp) ,
																		painter = painterResource(id = R.drawable.settings_sliders_icon) ,
																		contentDescription = "Menu"
																	)
															}
															MoreMenu(
																	menuOpen = menuOpen ,
																	setMenuOpen = setMenuOpen ,
																	)
														}

														QURAN_SCREEN_ROUTE ->
														{
															IconButton(onClick = {
																setMenuOpen2(true)
															}) {
																Icon(
																		modifier = Modifier.size(24.dp) ,
																		painter = painterResource(id = R.drawable.settings_sliders_icon) ,
																		contentDescription = "Menu"
																	)
															}
															MoreMenuMain(
																	menuOpen = menuOpen2 ,
																	setMenuOpen = setMenuOpen2 ,
																		)
														}

														NAMESOFALLAH_SCREEN_ROUTE ->
														{
															if (! isStoppedState.value)
															{
																IconButton(onClick = {
																	viewModelNames.handleAudioEvent(
																			NamesOfAllahViewModel.AudioEvent.Stop
																								   )
																}
																		  ) {
																	Icon(
																			modifier = Modifier.size(
																					24.dp
																									) ,
																			painter = painterResource(
																					id = R.drawable.stop_icon
																									 ) ,
																			contentDescription = "Stop playing"
																		)
																}
															}
															IconButton(onClick = {
																if (isPlayingState.value.not())
																{
																	viewModelNames.handleAudioEvent(
																			NamesOfAllahViewModel.AudioEvent.Play(
																					this@MainActivity
																												 )
																								   )
																} else
																{
																	viewModelNames.handleAudioEvent(
																			NamesOfAllahViewModel.AudioEvent.Pause
																								   )
																}
															}
																	  ) {
																if (isPlayingState.value)
																{
																	Icon(
																			modifier = Modifier.size(
																					24.dp
																									) ,
																			painter = painterResource(
																					id = R.drawable.pause_icon
																									 ) ,
																			contentDescription = "Pause playing"
																		)
																} else
																{
																	Icon(
																			modifier = Modifier.size(
																					24.dp
																									) ,
																			painter = painterResource(
																					id = R.drawable.play_icon
																									 ) ,
																			contentDescription = "Play"
																		)
																}
															}

														}

														TASBIH_SCREEN_ROUTE ->
														{
															//icon button to change the position of the button for right or left
															IconButton(onClick = {
																rOrl.value = ! rOrl.value
																viewModelTasbih.handleEvent(
																		TasbihViewModel.TasbihEvent.UpdateOrientationButtonState(
																				rOrl.value
																																)
																						   )
															}) {
																Icon(
																		modifier = Modifier.size(24.dp) ,
																		painter = if (rOrl.value)
																			painterResource(
																					id = R.drawable.corner_right_down_icon
																						   )
																		else painterResource(id = R.drawable.corner_left_down_icon) ,
																		contentDescription = "Change the position of the button"
																	)
															}
															//vibration toggle button for tasbih to provide feedback
															IconButton(onClick = {
																vibrationAllowed.value =
																	! vibrationAllowed.value
																viewModelTasbih.handleEvent(
																		TasbihViewModel.TasbihEvent.UpdateVibrationButtonState(
																				vibrationAllowed.value
																															  )
																						   )
															}) {
																Icon(
																		modifier = Modifier.size(24.dp) ,
																		painter = if (vibrationAllowed.value) painterResource(
																				id = R.drawable.phone_vibration_off_icon
																															 )
																		else painterResource(
																				id = R.drawable.phone_vibration_on_icon
																							) ,
																		contentDescription = "Vibration"
																	)
															}

															//a reset button to reset the count
															IconButton(onClick = {
																viewModelTasbih.handleEvent(
																		TasbihViewModel.TasbihEvent.UpdateResetButtonState(
																				true
																														  )
																						   )
															}) {
																Icon(
																		modifier = Modifier.size(24.dp) ,
																		painter = painterResource(id = R.drawable.refresh_icon) ,
																		contentDescription = "Reset" ,
																	)
															}
														}
//
//														//trackers screen
														PRAYER_TRACKER_SCREEN_ROUTE,
														CALENDER_SCREEN_ROUTE
														->
														{
															IconButton(onClick = {
																viewModelTracker.onEvent(TrackerViewModel.TrackerEvent.UPDATE_MENSTRAUTING_STATE(
																		! isMenstruatingState.value
																																				))
															}) {
																Icon(
																		modifier = Modifier.size(24.dp) ,
																		painter = painterResource(id = R.drawable.menstruation_icon) ,
																		contentDescription = "Menstruation",
																	//color it pink
																	tint = Color(0xFFE91E63)
																	)
															}
														}
													}
												}
												 )
									}
											  )
						} ,
						bottomBar = {
							AnimatedVisibility(
									visible = ! checkRoute(route.value.toString()) ,
									enter = CustomAnimation.fadeIn(duration = SCREEN_ANIMATION_DURATION) ,
									exit = CustomAnimation.fadeOut(duration = SCREEN_ANIMATION_DURATION_Exit) ,
									content = {
										BottomNavigationBar(navController = navController)
									})

						}
						) { it ->
					NavigationGraph(navController = navController , it)
				}
			}
		}
	}

	private fun processPageTitle(route : String , navController : NavHostController) : String
	{
		return when (route)
		{
			SETTINGS_SCREEN_ROUTE -> "Settings"
			ABOUT_SCREEN_ROUTE -> "About"
			PRAYER_TIMES_SETTINGS_SCREEN_ROUTE -> "Prayer Times Settings"
			QURAN_SCREEN_ROUTE -> "Quran"

			QURAN_AYA_SCREEN_ROUTE ->
			{
				//check if the url of the route is for surah or juz using the nav controller
				val isSurah = navController.currentBackStackEntry?.arguments?.getString("isSurah")
				val number = navController.currentBackStackEntry?.arguments?.getString("number")
				if (isSurah == "true")
				{
					"Surah $number"
				} else
				{
					"Juz $number"
				}
			}

			SHAHADAH_SCREEN_ROUTE -> "Shahadah"
			CHAPTERS_SCREEN_ROUTE -> "Categories of Dua"

			CHAPTER_SCREEN_ROUTE ->
			{
				//check if the url of the route is for surah or juz using the nav controller
				val chapterId =
					navController.currentBackStackEntry?.arguments?.getString("chapterId")
				"Chapter $chapterId"
			}

			TASBIH_SCREEN_ROUTE -> "Tasbih"
			NAMESOFALLAH_SCREEN_ROUTE -> "Allah"
			PRAYER_TRACKER_SCREEN_ROUTE -> "Trackers"
			CALENDER_SCREEN_ROUTE -> "Calender"
			QIBLA_SCREEN_ROUTE -> "Qibla"
			AppConstants.TASBIH_LIST_SCREEN -> "Tasbih List"

			MY_QURAN_SCREEN_ROUTE ->
			{
				//check if the url of the route is for surah or juz using the nav controller
				val isSurah =
					navController.currentBackStackEntry?.arguments?.getString("isSurah").toBoolean()
				val number = navController.currentBackStackEntry?.arguments?.getString("number")
				if (isSurah)
				{
					"Surah $number"
				} else
				{
					"Juz $number"
				}
			}

			WEB_VIEW_SCREEN_ROUTE ->
			{
				//check if the url of the route is privacy_policy using the nav controller
				when (navController.currentBackStackEntry?.arguments?.getString("url"))
				{
					"privacy_policy" ->
					{
						"Privacy Policy"
					}

					"help" ->
					{
						"Help"
					}

					else ->
					{
						"Terms and Conditions"
					}
				}
			}

			LICENCES_SCREEN_ROUTE -> "Open Source Libraries"
			AppConstants.DEBUG_MODE -> "Debug Tools"

			else -> ""
		}
	}

	//a fuinction to check a givenm route and return a boolean
	private fun checkRoute(route : String) : Boolean
	{
		val routeToCheck = listOf(
				SETTINGS_SCREEN_ROUTE ,
				ABOUT_SCREEN_ROUTE ,
				PRAYER_TIMES_SETTINGS_SCREEN_ROUTE ,
				QURAN_SCREEN_ROUTE ,
				QURAN_AYA_SCREEN_ROUTE ,
				SHAHADAH_SCREEN_ROUTE ,
				CHAPTERS_SCREEN_ROUTE ,
				CHAPTER_SCREEN_ROUTE ,
				TASBIH_SCREEN_ROUTE ,
				NAMESOFALLAH_SCREEN_ROUTE ,
				PRAYER_TRACKER_SCREEN_ROUTE ,
				CALENDER_SCREEN_ROUTE ,
				QIBLA_SCREEN_ROUTE ,
				AppConstants.TASBIH_LIST_SCREEN ,
				MY_QURAN_SCREEN_ROUTE ,
				WEB_VIEW_SCREEN_ROUTE ,
				LICENCES_SCREEN_ROUTE,
				AppConstants.DEBUG_MODE
								 )
		//if the route is in the list then return true
		return routeToCheck.contains(route)
	}
}

class CustomAnimation
{

	fun fadeIn(duration : Int) : EnterTransition =
		expandVertically(
				expandFrom = Alignment.Top ,
				animationSpec = tween(durationMillis = duration)
						)

	fun fadeOut(duration : Int) : ExitTransition =
		shrinkVertically(
				shrinkTowards = Alignment.Top ,
				animationSpec = tween(durationMillis = duration)
						)

	fun expandHorizontally(duration : Int) : EnterTransition =
		expandHorizontally(
				expandFrom = Alignment.CenterHorizontally ,
				animationSpec = tween(durationMillis = duration)
						  )

	fun shrinkHorizontally(duration : Int) : ExitTransition =
		shrinkHorizontally(
				shrinkTowards = Alignment.CenterHorizontally ,
				animationSpec = tween(durationMillis = duration)
						  )
}
