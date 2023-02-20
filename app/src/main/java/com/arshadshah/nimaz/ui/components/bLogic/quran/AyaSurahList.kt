package com.arshadshah.nimaz.ui.components.bLogic.quran


import android.media.MediaPlayer
import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.LiveData
import com.arshadshah.nimaz.data.remote.viewModel.QuranViewModel
import com.arshadshah.nimaz.ui.components.ui.quran.AyaListUI
import com.arshadshah.nimaz.ui.components.ui.quran.Page
import es.dmoral.toasty.Toasty
import kotlin.reflect.KFunction1

@Composable
fun AyaSurahList(
	paddingValues : PaddingValues ,
	number : Int ,
	language : String ,
	state : State<QuranViewModel.AyaSurahState> ,
	handleEvents : KFunction1<QuranViewModel.AyaEvent , Unit> ,
	noteState : LiveData<String> ,
	type : String ,
	mediaPlayer : MediaPlayer ,
	handleMenuEvents : KFunction1<QuranViewModel.QuranMenuEvents , Unit> ,
	pageMode : State<String> ,
				)
{
	when (val ayatSurahListState = state.value)
	{
		is QuranViewModel.AyaSurahState.Loading ->
		{
			if (pageMode.value == "List")
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
				Page(ArrayList(10) , paddingValues , loading = true,handleEvents = handleEvents)
			}
		}

		is QuranViewModel.AyaSurahState.Success ->
		{

			if (pageMode.value == "List")
			{
				AyaListUI(
						ayaList = ayatSurahListState.data ,
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
				Page(ayatSurahListState.data , paddingValues , false , handleEvents)
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