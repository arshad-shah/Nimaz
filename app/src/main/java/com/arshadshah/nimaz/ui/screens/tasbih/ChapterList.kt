package com.arshadshah.nimaz.ui.screens.tasbih

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
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

	when (val chapters = chapterState.value)
	{
		is DuaViewModel.ChapterState.Loading -> {
			CircularLoaderCard()
		}
		is DuaViewModel.ChapterState.Success ->
		{
			LazyColumn(contentPadding = paddingValues)
			{
				items(chapters.chapterList.size)
				{
					ChapterListItem(chapter = chapters.chapterList[it] , onNavigateToChapter = onNavigateToChapter)
				}
			}
		}
		is DuaViewModel.ChapterState.Error ->
		{
			Toasty.error(context , chapters.error).show()
		}
	}
}