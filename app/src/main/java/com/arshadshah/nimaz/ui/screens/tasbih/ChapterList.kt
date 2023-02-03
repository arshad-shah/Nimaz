package com.arshadshah.nimaz.ui.screens.tasbih

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.arshadshah.nimaz.data.remote.viewModel.DuaViewModel
import com.arshadshah.nimaz.ui.components.bLogic.tasbih.ChapterListItem
import com.arshadshah.nimaz.ui.components.ui.loaders.CircularLoaderCard
import es.dmoral.toasty.Toasty

@Composable
fun ChapterList(paddingValues : PaddingValues , onNavigateToChapter : (Int) -> Unit)
{
	val context = LocalContext.current

	val viewModel = DuaViewModel(context)

	val chapterState = remember { viewModel.chapterState }.collectAsState()

	viewModel.getChapterList()

	//if a new item is viewed, then scroll to that item
	val sharedPref = context.getSharedPreferences("dua" , 0)
	val listState = rememberLazyListState()
	val visibleItemIndex = remember { mutableStateOf(sharedPref.getInt("visibleItemIndex" , - 1)) }

	//when we close the app, we want to save the index of the last item viewed so that we can scroll to it when we open the app again
	LaunchedEffect(listState.firstVisibleItemIndex)
	{
		sharedPref.edit().putInt("visibleItemIndex" , listState.firstVisibleItemIndex).apply()
	}

	//when we reopen the app, we want to scroll to the last item viewed
	LaunchedEffect(visibleItemIndex.value)
	{
		if (visibleItemIndex.value != - 1)
		{
			listState.scrollToItem(visibleItemIndex.value)
			//set the value back to -1 so that we don't scroll to the same item again
			visibleItemIndex.value = - 1
		}
	}
	when (val chapters = chapterState.value)
	{
		is DuaViewModel.ChapterState.Loading ->
		{
			CircularLoaderCard()
		}

		is DuaViewModel.ChapterState.Success ->
		{
			LazyColumn(
					contentPadding = paddingValues ,
					state = listState
					  )
			{
				items(chapters.chapterList.size)
				{
					ChapterListItem(
							chapter = chapters.chapterList[it] ,
							onNavigateToChapter = onNavigateToChapter
								   )
				}
			}
		}

		is DuaViewModel.ChapterState.Error ->
		{
			Toasty.error(context , chapters.error).show()
		}
	}
}