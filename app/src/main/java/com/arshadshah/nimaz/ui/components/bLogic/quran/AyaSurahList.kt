package com.arshadshah.nimaz.ui.components.bLogic.quran


import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.platform.LocalContext
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.viewModel.QuranViewModel
import com.arshadshah.nimaz.ui.components.ui.quran.AyaListUI
import com.arshadshah.nimaz.ui.components.ui.quran.Page
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import es.dmoral.toasty.Toasty
import kotlin.reflect.KFunction1

@Composable
fun AyaSurahList(
	paddingValues : PaddingValues ,
	number : Int ,
	language : String ,
	state : State<QuranViewModel.AyaSurahState> ,
	handleEvents : KFunction1<QuranViewModel.AyaEvent , Unit> ,
	ayaState : State<QuranViewModel.AyaState> ,
				)
{
	when (val ayatSurahListState = state.value)
	{
		is QuranViewModel.AyaSurahState.Loading ->
		{
			//get the translation type from shared preferences
			val pageType =
				PrivateSharedPreferences(LocalContext.current).getData(
						key = AppConstants.PAGE_TYPE ,
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
						ayaList = ArrayList(6) ,
						paddingValues = paddingValues ,
						language = language ,
						loading = true ,
						handleEvents = handleEvents ,
						ayaState = ayaState
						 )
			} else
			{
				Page(ArrayList(10) , paddingValues , loading = true)
			}
		}

		is QuranViewModel.AyaSurahState.Success ->
		{
			//get the translation type from shared preferences
			val pageType =
				PrivateSharedPreferences(LocalContext.current).getData(
						key = AppConstants.PAGE_TYPE ,
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
						language = language ,
						loading = false ,
						handleEvents = handleEvents ,
						ayaState = ayaState ,
						 )
			} else
			{
				Page(ayatSurahListState.data , paddingValues , false)
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