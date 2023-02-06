package com.arshadshah.nimaz.ui.components.bLogic.quran


import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.platform.LocalContext
import com.arshadshah.nimaz.data.remote.viewModel.QuranViewModel
import com.arshadshah.nimaz.ui.components.ui.quran.JuzListUI
import es.dmoral.toasty.Toasty

@Composable
fun JuzList(
	onNavigateToAyatScreen : (String , Boolean , String) -> Unit ,
	state : State<QuranViewModel.JuzState> ,
		   )
{
	when (val juzState = state.value)
	{
		is QuranViewModel.JuzState.Loading ->
		{
			JuzListUI(
					juz = ArrayList(6) ,
					onNavigateToAyatScreen = onNavigateToAyatScreen ,
					loading = true
					 )
		}

		is QuranViewModel.JuzState.Success ->
		{
			JuzListUI(
					juz = juzState.data ,
					onNavigateToAyatScreen = onNavigateToAyatScreen ,
					loading = false
					 )
		}

		is QuranViewModel.JuzState.Error ->
		{
			JuzListUI(
					juz = ArrayList(5) ,
					onNavigateToAyatScreen = onNavigateToAyatScreen ,
					loading = true
					 )
			Toasty.error(LocalContext.current , juzState.errorMessage , Toast.LENGTH_SHORT , true)
				.show()
		}
	}
}