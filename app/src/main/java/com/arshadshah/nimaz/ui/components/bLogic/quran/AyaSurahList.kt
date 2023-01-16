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
fun AyaSurahList(
	paddingValues : PaddingValues ,
	number : Int ,
	language : String ,
	state : State<QuranViewModel.AyaSurahState> ,
				)
{
	when (val ayatSurahListState = state.value)
	{
		is QuranViewModel.AyaSurahState.Loading ->
		{
			CircularLoaderCard()
		}

		is QuranViewModel.AyaSurahState.Success ->
		{
			//get the translation type from shared preferences
			val pageType =
				PrivateSharedPreferences(LocalContext.current).getData(
						key = "PageType" ,
						s = "List"
																	  )
			var isList = true
			if (pageType != "List")
			{
				isList = false
			}

			if (isList)
			{
				AyaListUI(
						ayaList = ayatSurahListState.data ,
						paddingValues = paddingValues ,
						language = language
						 )
			} else
			{
				Page(ayatSurahListState.data , paddingValues)
			}
		}

		is QuranViewModel.AyaSurahState.Error ->
		{
			Toasty.error(
					LocalContext.current ,
					ayatSurahListState.errorMessage ,
					Toast.LENGTH_SHORT ,
					true
						).show()
		}
	}
}