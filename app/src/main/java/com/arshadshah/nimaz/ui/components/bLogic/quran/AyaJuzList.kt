package com.arshadshah.nimaz.ui.components.bLogic.quran


import android.media.MediaPlayer
import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.LiveData
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.viewModel.QuranViewModel
import com.arshadshah.nimaz.ui.components.ui.quran.AyaListUI
import com.arshadshah.nimaz.ui.components.ui.quran.Page
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import es.dmoral.toasty.Toasty
import kotlin.reflect.KFunction1

@Composable
fun AyaJuzList(
	paddingValues : PaddingValues ,
	number : Int ,
	language : String ,
	state : State<QuranViewModel.AyaJuzState> ,
	handleEvents : KFunction1<QuranViewModel.AyaEvent , Unit> ,
	noteState : LiveData<String> ,
	type : String ,
	mediaPlayer : MediaPlayer ,
	handleMenuEvents : KFunction1<QuranViewModel.QuranMenuEvents , Unit> ,
	pageMode : Any ,
			  )
{
	when (val ayatJuzListState = state.value)
	{
		is QuranViewModel.AyaJuzState.Loading ->
		{
			//get the translation type from shared preferences
			val pageType =
				PrivateSharedPreferences(LocalContext.current).getData(
						key = AppConstants.PAGE_TYPE ,
						s = "List"
																	  )
			if (pageMode == "List")
			{
				AyaListUI(
						ayaList = ArrayList(6) ,
						paddingValues = paddingValues ,
						language = language ,
						loading = true ,
						handleEvents = handleEvents ,
						noteState = noteState ,
						type = type ,
						number = number ,
						 )
			} else
			{
				Page(ArrayList(10) , paddingValues , loading = true , handleEvents)
			}
		}

		is QuranViewModel.AyaJuzState.Success ->
		{

			//get the translation type from shared preferences
			val pageType =
				PrivateSharedPreferences(LocalContext.current).getData(
						key = AppConstants.PAGE_TYPE ,
						s = "List"
																	  )
			if (pageMode == "List")
			{
				AyaListUI(
						ayaList = ayatJuzListState.data ,
						paddingValues = paddingValues ,
						language = language ,
						loading = false ,
						handleEvents = handleEvents ,
						noteState = noteState ,
						type = type ,
						number = number ,
						 )
			} else
			{
				Page(ayatJuzListState.data , paddingValues , false , handleEvents)
			}
		}

		is QuranViewModel.AyaJuzState.Error ->
		{
			Toasty.error(
					LocalContext.current ,
					ayatJuzListState.errorMessage ,
					Toast.LENGTH_SHORT ,
					true
						)
				.show()
		}
	}
}