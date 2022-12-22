package com.arshadshah.nimaz.ui.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.arshadshah.nimaz.ui.screens.quran.AyatScreen
import com.arshadshah.nimaz.ui.screens.quran.QuranScreen

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun QuranNavGraph(navController : NavController , paddingValues : PaddingValues)
{
	NavHost(
			navController = navController as NavHostController ,
			startDestination = "quran"
		   ) {
		composable("quran") {
			QuranScreen(
					paddingValues ,
					onNavigateToAyatScreen = { number : String , isSurah : Boolean , language : String ->
						navController.navigate("ayatScreen/$number/$isSurah/$language")
					})
		}
		composable("ayatScreen/{number}/{isSurah}/{language}") {
			AyatScreen(
					number = it.arguments?.getString("number") ,
					isSurah = it.arguments?.getString("isSurah") !! ,
					language = it.arguments?.getString("language") !! ,
					paddingValues = paddingValues
					  )
		}
	}
}