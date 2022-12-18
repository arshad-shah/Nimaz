package com.arshadshah.nimaz.ui.components.bLogic.quran


import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.platform.LocalContext
import com.arshadshah.nimaz.data.remote.viewModel.QuranViewModel
import com.arshadshah.nimaz.ui.components.ui.loaders.CircularLoaderCard
import com.arshadshah.nimaz.ui.components.ui.quran.JuzListUI
import es.dmoral.toasty.Toasty

@Composable
fun JuzList(
	onNavigateToAyatScreen : (String , Boolean , Boolean) -> Unit ,
	state : State<QuranViewModel.JuzState> ,
		   )
{
	when (val juzState = state.value)
	{
		is QuranViewModel.JuzState.Loading ->
		{
			CircularLoaderCard()
		}

		is QuranViewModel.JuzState.Success ->
		{
			JuzListUI(
					juz = juzState.data ,
					onNavigateToAyatScreen = onNavigateToAyatScreen
					 )
		}

		is QuranViewModel.JuzState.Error ->
		{
			JuzListUI(
					juz = ArrayList(5) ,
					onNavigateToAyatScreen = onNavigateToAyatScreen
					 )
			Toasty.error(LocalContext.current , juzState.errorMessage , Toast.LENGTH_SHORT , true).show()
		}
	}
}