package com.arshadshah.nimaz.activities

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants.ABOUT_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.APP_UPDATE_REQUEST_CODE
import com.arshadshah.nimaz.constants.AppConstants.CALENDER_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.CHAPTERS_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.CHAPTER_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.MAIN_ACTIVITY_TAG
import com.arshadshah.nimaz.constants.AppConstants.NAMESOFALLAH_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_TIMES_SETTINGS_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_TRACKER_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.QURAN_AYA_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.QURAN_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.SCREEN_ANIMATION_DURATION
import com.arshadshah.nimaz.constants.AppConstants.SETTINGS_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.SHAHADAH_SCREEN_ROUTE
import com.arshadshah.nimaz.constants.AppConstants.TASBIH_SCREEN_ROUTE
import com.arshadshah.nimaz.ui.components.ui.quran.MoreMenu
import com.arshadshah.nimaz.ui.navigation.BottomNavigationBar
import com.arshadshah.nimaz.ui.navigation.NavigationGraph
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.arshadshah.nimaz.utils.LocalDataStore
import com.arshadshah.nimaz.utils.location.NetworkChecker
import com.arshadshah.nimaz.widgets.Nimaz
import com.arshadshah.nimaz.widgets.updateAppWidget
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

	private val mediaPlayer = MediaPlayer()

	override fun onDestroy()
	{
		super.onDestroy()
		mediaPlayer.release()
	}

	override fun onPause()
	{
		super.onPause()
		mediaPlayer.release()
	}

	@OptIn(ExperimentalMaterial3Api::class , ExperimentalAnimationApi::class)
	@RequiresApi(Build.VERSION_CODES.S)
	override fun onCreate(savedInstanceState : Bundle?)
	{
		this.actionBar?.hide()

		LocalDataStore.init(this@MainActivity)
		Log.d(MAIN_ACTIVITY_TAG , "onCreate:  called and local data store initialized")

		prepareMediaPlayer(this@MainActivity)

		Log.d(MAIN_ACTIVITY_TAG , "onCreate:  media player prepared")

		val appWidgetManager = AppWidgetManager.getInstance(this)
		val appWidgetIds : IntArray = appWidgetManager.getAppWidgetIds(
				ComponentName(
						this ,
						Nimaz::class.java
							 )
																	  )
		for (appWidgetId in appWidgetIds)
		{
			updateAppWidget(this , appWidgetManager , appWidgetId)
		}
		Log.d(MAIN_ACTIVITY_TAG , "onCreate:  app widget updated")

		val appUpdateManager = AppUpdateManagerFactory.create(this)

		// Returns an intent object that you use to check for an update.
		val appUpdateInfoTask = appUpdateManager.appUpdateInfo

		// Checks that the platform will allow the specified type of update.
		appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
			if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
				&& appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
			)
			{
				Log.d(
						MAIN_ACTIVITY_TAG ,
						"onCreate:  update is available and immediate is allowed"
					 )
				// Request the update.
				appUpdateManager.startUpdateFlowForResult(
						appUpdateInfo ,
						AppUpdateType.IMMEDIATE ,
						this ,
						APP_UPDATE_REQUEST_CODE
														 )

			}
		}
		super.onCreate(savedInstanceState)

		//this is used to show the full activity on the screen
		setContent {
			NimazTheme {
				val isPlaying = remember { mutableStateOf(false) }
				val isPaused = remember { mutableStateOf(false) }
				val isStopped = remember { mutableStateOf(true) }
				val navController = rememberAnimatedNavController()
				val route =
					remember(navController) { mutableStateOf(navController.currentDestination?.route) }
				navController.addOnDestinationChangedListener { _ , destination , _ ->
					route.value = destination.route
				}
				val (menuOpen , setMenuOpen) = remember { mutableStateOf(false) }
				val CustomAnimation = remember { CustomAnimation() }

				val showResetDialog = remember { mutableStateOf(false) }

				val vibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
				val vibrationAllowed = remember { mutableStateOf(true) }
				val rOrl = remember { mutableStateOf(0) }

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


				Scaffold(
						snackbarHost = { SnackbarHost(snackbarHostState) } ,
						topBar = {
							AnimatedVisibility(
									visible = checkRoute(route.value.toString()) ,
									enter = CustomAnimation.fadeIn(duration = SCREEN_ANIMATION_DURATION) ,
									exit = CustomAnimation.fadeOut(duration = SCREEN_ANIMATION_DURATION) ,
									content = {

										TopAppBar(
												title = {
													Text(
															text = processPageTitle(route.value.toString()) ,
															style = MaterialTheme.typography.titleMedium
														)
												} ,
												navigationIcon = {
													IconButton(onClick = { navController.navigateUp() }) {
														Icon(
																modifier = Modifier.size(24.dp) ,
																painter = painterResource(id = R.drawable.angle_left_icon) ,
																contentDescription = "Back"
															)
													}
												} ,
												actions = {
													//only show the menu button if the title is Quran
													if (route.value == QURAN_SCREEN_ROUTE)
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
																setMenuOpen = setMenuOpen
																)
													} else if (route.value == NAMESOFALLAH_SCREEN_ROUTE)
													{
														if (! isStopped.value)
														{
															IconButton(onClick = {
																mediaPlayer.stop()
																mediaPlayer.reset()
																prepareMediaPlayer(this@MainActivity)
																isPlaying.value = false
																isPaused.value = false
																isStopped.value = true
															}
																	  ) {
																Icon(
																		modifier = Modifier.size(24.dp) ,
																		painter = painterResource(id = R.drawable.stop_icon) ,
																		contentDescription = "Stop playing"
																	)
															}
														}
														IconButton(onClick = {
															if (isPlaying.value.not())
															{
																//start the audio
																mediaPlayer.start()
																isPlaying.value = true
																isPaused.value = false
																isStopped.value = false
															} else
															{
																mediaPlayer.pause()
																isPlaying.value = false
																isPaused.value = true
																isStopped.value = false
															}
														}
																  ) {
															if (isPlaying.value)
															{
																Icon(
																		modifier = Modifier.size(24.dp) ,
																		painter = painterResource(id = R.drawable.pause_icon) ,
																		contentDescription = "Pause playing"
																	)
															} else
															{
																Icon(
																		modifier = Modifier.size(24.dp) ,
																		painter = painterResource(id = R.drawable.play_icon) ,
																		contentDescription = "Play"
																	)
															}
														}

													} else if (route.value == TASBIH_SCREEN_ROUTE)
													{
														//icon button to chenge the position of the button for right or left
														IconButton(onClick = {
															rOrl.value =
																if (rOrl.value == 0) 1 else 0
														}) {
															Icon(
																	modifier = Modifier.size(24.dp) ,
																	painter = if (rOrl.value == 0) painterResource(
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
															//mute the vibration
															if (! vibrationAllowed.value)
															{
																vibrator.cancel()
															}
														}) {
															Icon(
																	modifier = Modifier.size(24.dp) ,
																	painter = if (vibrationAllowed.value) painterResource(
																			id = R.drawable.vibration
																														 )
																	else painterResource(
																			id = R.drawable.cross_icon
																						) ,
																	contentDescription = "Vibration"
																)
														}

														//a reset button to reset the count
														IconButton(onClick = {
															showResetDialog.value = true
														}) {
															Icon(
																	modifier = Modifier.size(24.dp) ,
																	painter = painterResource(id = R.drawable.refresh_icon) ,
																	contentDescription = "Reset" ,
																)
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
									exit = CustomAnimation.fadeOut(duration = SCREEN_ANIMATION_DURATION) ,
									content = {
										BottomNavigationBar(navController = navController)
									})
						}
						) { it ->
					NavigationGraph(
							navController = navController ,
							it ,
							showResetDialog ,
							vibrator ,
							vibrationAllowed ,
							rOrl ,
							mediaPlayer ,
								   )
				}
			}
		}
	}

	fun processPageTitle(route : String) : String
	{
		return when (route)
		{
			SETTINGS_SCREEN_ROUTE -> "Settings"
			ABOUT_SCREEN_ROUTE -> "About"
			PRAYER_TIMES_SETTINGS_SCREEN_ROUTE -> "Prayer Times Customization"
			QURAN_SCREEN_ROUTE -> "Quran"
			QURAN_AYA_SCREEN_ROUTE -> "Aya"
			SHAHADAH_SCREEN_ROUTE -> "Shahadah"
			CHAPTERS_SCREEN_ROUTE -> "Categories of Dua"
			CHAPTER_SCREEN_ROUTE -> "Dua"
			TASBIH_SCREEN_ROUTE -> "Tasbih"
			NAMESOFALLAH_SCREEN_ROUTE -> "Allah"
			PRAYER_TRACKER_SCREEN_ROUTE -> "Prayer Tracker"
			CALENDER_SCREEN_ROUTE -> "Calender"
			else -> ""
		}
	}

	//a fuinction to check a givenm route and return a boolean
	fun checkRoute(route : String) : Boolean
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
				PRAYER_TRACKER_SCREEN_ROUTE,
				CALENDER_SCREEN_ROUTE
								 )
		//if the route is in the list then return true
		return routeToCheck.contains(route)
	}

	private fun prepareMediaPlayer(context : Context)
	{
		val myUri : Uri =
			Uri.parse("android.resource://" + context.packageName + "/" + R.raw.asmaulhusna)
		mediaPlayer.apply {
			setAudioAttributes(
					AudioAttributes.Builder()
						.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
						.setUsage(AudioAttributes.USAGE_MEDIA)
						.build()
							  )
			setDataSource(context , myUri)
			prepare()
		}
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
}
