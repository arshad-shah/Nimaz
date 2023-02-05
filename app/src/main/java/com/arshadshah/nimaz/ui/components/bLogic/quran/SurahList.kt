package com.arshadshah.nimaz.ui.components.bLogic.quran


import androidx.compose.foundation.background
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
import com.arshadshah.nimaz.data.remote.viewModel.QuranViewModel
import com.arshadshah.nimaz.ui.components.ui.loaders.BigcardLoader
import com.arshadshah.nimaz.ui.components.ui.loaders.CircularLoaderCard
import com.arshadshah.nimaz.ui.components.ui.loaders.loadingShimmerEffect
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

		is QuranViewModel.SurahState.Success ->
		{
			SurahListUI(
					surahs = surahState.data !! ,
					onNavigateToAyatScreen = onNavigateToAyatScreen
					   )
		}

		is QuranViewModel.SurahState.Error ->
		{
			SurahListUI(
					surahs = ArrayList() ,
					onNavigateToAyatScreen = onNavigateToAyatScreen
					   )
			Toasty.error(LocalContext.current , surahState.errorMessage).show()
		}
	}
}