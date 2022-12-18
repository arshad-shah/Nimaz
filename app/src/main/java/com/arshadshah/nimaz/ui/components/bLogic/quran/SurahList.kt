package com.arshadshah.nimaz.ui.components.bLogic.quran


import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.platform.LocalContext
import com.arshadshah.nimaz.data.remote.viewModel.QuranViewModel
import com.arshadshah.nimaz.ui.components.ui.loaders.CircularLoaderCard
import com.arshadshah.nimaz.ui.components.ui.quran.JuzListUI
import com.arshadshah.nimaz.ui.components.ui.quran.SurahListUI
import es.dmoral.toasty.Toasty

@Composable
fun SurahList(
	onNavigateToAyatScreen : (String , Boolean , Boolean) -> Unit ,
	state : State<QuranViewModel.SurahState> ,
			 )
{
	when (val surahState = state.value)
	{
		is QuranViewModel.SurahState.Loading ->
		{
			CircularLoaderCard()
		}

		is QuranViewModel.SurahState.Success ->
		{
			SurahListUI(
					surahs = surahState.data !! ,
					onNavigateToAyatScreen = onNavigateToAyatScreen
					   )
		}

		is QuranViewModel.SurahState.Error ->
		{
			JuzListUI(
					juz = ArrayList(5) ,
					onNavigateToAyatScreen = onNavigateToAyatScreen
					 )
			Toasty.error(LocalContext.current , surahState.errorMessage).show()
		}
	}
}