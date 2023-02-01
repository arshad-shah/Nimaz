package com.arshadshah.nimaz.ui.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.arshadshah.nimaz.ui.screens.auth.EmailPasswordScreenSignin
import com.arshadshah.nimaz.ui.screens.auth.EmailPasswordScreenSignup

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun AuthNavigationGraph(navController : NavController , paddingValues : PaddingValues)
{
	NavHost(
			navController = navController as NavHostController ,
			startDestination = "login"
		   ) {
		composable("login") {
			EmailPasswordScreenSignin(
					paddingValues,
					onNavigateToSignup = {
						navController.navigate("signup")
					}
									 )
		}

		composable("signup") {
			EmailPasswordScreenSignup(paddingValues)
		}
	}
}