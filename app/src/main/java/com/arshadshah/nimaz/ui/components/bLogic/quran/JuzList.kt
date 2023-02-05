package com.arshadshah.nimaz.ui.components.bLogic.quran


import android.widget.Toast
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
import com.arshadshah.nimaz.ui.components.ui.loaders.ItemSkeleton
import com.arshadshah.nimaz.ui.components.ui.loaders.loadingShimmerEffect
import com.arshadshah.nimaz.ui.components.ui.quran.JuzListItemUI
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
			Toasty.error(LocalContext.current , juzState.errorMessage , Toast.LENGTH_SHORT , true)
				.show()
		}
	}
}