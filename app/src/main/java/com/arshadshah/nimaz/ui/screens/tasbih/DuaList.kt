package com.arshadshah.nimaz.ui.screens.tasbih

import android.util.Log
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.arshadshah.nimaz.data.remote.viewModel.DuaViewModel
import com.arshadshah.nimaz.ui.components.bLogic.tasbih.DuaListItem
import com.arshadshah.nimaz.ui.components.ui.loaders.CircularLoaderCard
import es.dmoral.toasty.Toasty

@Composable
fun DuaList(chapterId : Int , paddingValues : PaddingValues)
{
	val context = LocalContext.current
	val viewModel = DuaViewModel(context)

	val duaState = remember {
		viewModel.duaState
	}.collectAsState()

	viewModel.getChapterById(chapterId)

	//if a new item is viewed, then scroll to that item
	val sharedPref = context.getSharedPreferences("dua" , 0)
	val listState = rememberLazyListState()
	val visibleItemIndex =
		remember { mutableStateOf(sharedPref.getInt("visibleItemIndexDua-${chapterId}" , - 1)) }

	//when we close the app, we want to save the index of the last item viewed so that we can scroll to it when we open the app again
	LaunchedEffect(listState.firstVisibleItemIndex)
	{
		sharedPref.edit()
			.putInt("visibleItemIndexDua-${chapterId}" , listState.firstVisibleItemIndex).apply()
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

	when (val duas = duaState.value)
	{
		is DuaViewModel.DuaState.Loading ->
		{
			CircularLoaderCard()

		}

		is DuaViewModel.DuaState.Success ->
		{
			LazyColumn(
					contentPadding = paddingValues ,
					state = listState ,
					content = {
						items(duas.duaList.duas.size)
						{
							DuaListItem(dua = duas.duaList.duas[it])
						}
					})

		}

		is DuaViewModel.DuaState.Error ->
		{
			Toasty.error(context , duas.error).show()
			Log.e("Nimaz: DuaList" , duas.error)

		}
	}
}