package com.arshadshah.nimaz.ui.components.bLogic.quran


import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.viewModel.QuranViewModel
import com.arshadshah.nimaz.ui.components.ui.loaders.BigcardLoader
import com.arshadshah.nimaz.ui.components.ui.loaders.CircularLoaderCard
import com.arshadshah.nimaz.ui.components.ui.loaders.loadingShimmerEffect
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
			LazyColumn(userScrollEnabled = true) {
				items(6) { index ->
					ElevatedCard(
							modifier = Modifier
								.padding(8.dp)
								.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
								.fillMaxWidth()
								.background(color = MaterialTheme.colorScheme.surface) ,
							shape = RoundedCornerShape(8.dp)
								) {
						BigcardLoader(brush = loadingShimmerEffect())
					}
				}
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
			var isList = true
			if (pageType != "List")
			{
				isList = false
			}

			if (isList)
			{
				AyaListUI(
						ayaList = ayatJuzListState.data ,
						paddingValues = paddingValues ,
						language = language
						 )
			} else
			{
				Page(ayatJuzListState.data , paddingValues)
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