package com.arshadshah.nimaz.ui.components.bLogic.quran


import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.platform.LocalContext
import com.arshadshah.nimaz.data.remote.viewModel.QuranViewModel
import com.arshadshah.nimaz.ui.components.ui.quran.SurahListUI
import es.dmoral.toasty.Toasty

@Composable
fun SurahList(
	onNavigateToAyatScreen : (String , Boolean , String) -> Unit ,
	state : State<QuranViewModel.SurahState> ,
			 )
{
	when (val surahState = state.value)
	{
		is QuranViewModel.SurahState.Loading ->
		{
			SurahListUI(
					surahs = ArrayList(6) ,
					onNavigateToAyatScreen = onNavigateToAyatScreen ,
					loading = true
					   )
		}

		is QuranViewModel.SurahState.Success ->
		{
			SurahListUI(
					surahs = surahState.data !! ,
					onNavigateToAyatScreen = onNavigateToAyatScreen ,
					loading = false
					   )
		}

		is QuranViewModel.SurahState.Error ->
		{
			SurahListUI(
					surahs = ArrayList(6) ,
					onNavigateToAyatScreen = onNavigateToAyatScreen ,
					loading = true
					   )
			Toasty.error(LocalContext.current , surahState.errorMessage).show()
		}
	}
}