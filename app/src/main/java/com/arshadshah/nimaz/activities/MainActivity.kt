package com.arshadshah.nimaz.activities

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.arshadshah.nimaz.ui.navigation.BottomNavigationBar
import com.arshadshah.nimaz.ui.navigation.NavigationGraph
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.arshadshah.nimaz.utils.Location
import com.arshadshah.nimaz.widgets.Nimaz
import com.arshadshah.nimaz.widgets.updateAppWidget

class MainActivity : ComponentActivity()
{

	@OptIn(ExperimentalMaterial3Api::class)
	@RequiresApi(Build.VERSION_CODES.S)
	override fun onCreate(savedInstanceState : Bundle?)
	{
		this.actionBar?.hide()

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
		super.onCreate(savedInstanceState)
		//this is used to show the full activity on the screen
		setContent {
			NimazTheme {
				val navController = rememberNavController()
				Location().getAutomaticLocation(this)

				Scaffold(
						bottomBar = { BottomNavigationBar(navController = navController) }
						) { it ->
					NavigationGraph(navController = navController , it)
				}
			}
		}
	}
}

//this is a main component to show everything else
@RequiresApi(Build.VERSION_CODES.S)
@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun DefaultPreview(modifier : Modifier = Modifier)
{
	val navController = rememberNavController()
	Scaffold(
			bottomBar = { BottomNavigationBar(navController = navController) }
			) { it ->
		NavigationGraph(navController = navController , it)
	}
}

