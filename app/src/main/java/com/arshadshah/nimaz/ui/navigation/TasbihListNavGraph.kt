package com.arshadshah.nimaz.ui.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.arshadshah.nimaz.ui.screens.tasbih.ChapterList
import com.arshadshah.nimaz.ui.screens.tasbih.DuaList

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun TasbihListNavGraph(navController : NavController , paddingValues : PaddingValues)
{
	NavHost(
			navController = navController as NavHostController ,
			startDestination = "chapterList"
		   ) {
		composable("chapterList") {
			ChapterList(
					paddingValues ,
					onNavigateToChapter = { chapterId : Int ->
						navController.navigate("chapter/$chapterId")
					}
					   )
		}
		composable("chapter/{chapterId}") {
			DuaList(
					chapterId = it.arguments?.getString("chapterId")?.toInt() ?: 0 ,
					paddingValues = paddingValues
					  )
		}
	}
}