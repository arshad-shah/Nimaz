package com.arshadshah.nimaz.ui.screens.tasbih

import android.util.Log
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
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

	when (val duas = duaState.value)
	{
		is DuaViewModel.DuaState.Loading -> {
			CircularLoaderCard()

		}
		is DuaViewModel.DuaState.Success ->
		{
			LazyColumn(
					contentPadding = paddingValues ,
					content ={
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