package com.arshadshah.nimaz.ui.screens.tasbih

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.models.Dua
import com.arshadshah.nimaz.ui.components.tasbih.DuaListItem
import com.arshadshah.nimaz.viewModel.DuaViewModel
import es.dmoral.toasty.Toasty

@Composable
fun DuaList(chapterId : Int , paddingValues : PaddingValues)
{
	val context = LocalContext.current
	val viewModel = viewModel(
			key = AppConstants.DUA_CHAPTERS_VIEWMODEL_KEY ,
			initializer = { DuaViewModel() } ,
			viewModelStoreOwner = LocalContext.current as ComponentActivity
							 )

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
	LaunchedEffect(remember { derivedStateOf { listState.firstVisibleItemIndex } })
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
			LazyColumn(
					contentPadding = paddingValues ,
					state = listState ,
					content = {
						items(3)
						{
							DuaListItem(
									dua = Dua(
											- 1 ,
											- 1 ,
											- 1 ,
											"" ,
											"" ,
											"" ,
											 ) ,
									loading = true
									   )
						}
					})

		}

		is DuaViewModel.DuaState.Success ->
		{
			LazyColumn(
					modifier = Modifier.testTag(AppConstants.TEST_TAG_CHAPTER) ,
					contentPadding = paddingValues ,
					state = listState ,
					content = {
						items(duas.duaList.duas.size)
						{
							DuaListItem(dua = duas.duaList.duas[it] , loading = false)
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