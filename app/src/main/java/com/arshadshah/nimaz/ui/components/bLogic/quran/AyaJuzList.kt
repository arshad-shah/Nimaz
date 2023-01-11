package com.arshadshah.nimaz.ui.components.bLogic.quran


import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.platform.LocalContext
import com.arshadshah.nimaz.data.remote.viewModel.QuranViewModel
import com.arshadshah.nimaz.ui.components.ui.loaders.CircularLoaderCard
import com.arshadshah.nimaz.ui.components.ui.quran.AyaListUI
import com.arshadshah.nimaz.ui.components.ui.quran.Page
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import es.dmoral.toasty.Toasty

@Composable
fun AyaJuzList(
	paddingValues : PaddingValues ,
	number : Int ,
	language : String ,
	state : State<QuranViewModel.AyaJuzState> ,
			  )
{
	when (val ayatJuzListState = state.value)
	{
		is QuranViewModel.AyaJuzState.Loading ->
		{
			CircularLoaderCard()
		}

		is QuranViewModel.AyaJuzState.Success ->
		{

			//get the translation type from shared preferences
			val pageType =
				PrivateSharedPreferences(LocalContext.current).getData(key = "PageType" ,
																	   s = "List")
			var isList = true
			if (pageType != "List")
			{
				isList = false
			}

			if (isList)
			{
				AyaListUI(ayaList = ayatJuzListState.data ,
						  paddingValues = paddingValues ,
						  language = language)
			} else
			{
				Page(ayatJuzListState.data , paddingValues)
			}
		}

		is QuranViewModel.AyaJuzState.Error ->
		{
			Toasty.error(LocalContext.current ,
						 ayatJuzListState.errorMessage ,
						 Toast.LENGTH_SHORT ,
						 true)
				.show()
		}
	}
}