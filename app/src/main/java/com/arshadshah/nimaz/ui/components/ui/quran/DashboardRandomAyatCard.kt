package com.arshadshah.nimaz.ui.components.ui.quran

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.viewModel.QuranViewModel
import com.arshadshah.nimaz.ui.components.ui.compass.CustomText
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.arshadshah.nimaz.ui.theme.urduFont
import com.arshadshah.nimaz.ui.theme.utmaniQuranFont
import com.arshadshah.nimaz.utils.LocalDataStore
import com.arshadshah.nimaz.utils.PrivateSharedPreferences

@Composable
fun DashboardRandomAyatCard(onNavigateToAyatScreen : (String , Boolean , String , Int) -> Unit)
{
	val context = LocalContext.current
	val viewModel = viewModel(
			key = "QuranViewModel" ,
			initializer = { QuranViewModel(context) } ,
			viewModelStoreOwner = context as ComponentActivity)
	//make sure this is called only once a day
	val randomAyatLastFetched = PrivateSharedPreferences(context).getDataLong(
			AppConstants.RANDOM_AYAT_LAST_FETCHED
																			 )
	//ayat last fetched number in surah
	val randomAyatLastFetchedNumber = PrivateSharedPreferences(context).getDataInt(
			AppConstants.RANDOM_AYAT_NUMBER_IN_SURAH_LAST_FETCHED
																				  )

	Log.d("RandomAyat" , "Random Ayat Last Fetched: $randomAyatLastFetched")
	Log.d("RandomAyat" , "Random Ayat Last Fetched Number: $randomAyatLastFetchedNumber")

	//check if it has been more than a 3 hours since last fetch and fetch new ayat else use the old one from local storage
	if (System.currentTimeMillis() - randomAyatLastFetched > 10800000 || randomAyatLastFetchedNumber == 0)
	{
		viewModel.getRandomAya()
		PrivateSharedPreferences(context).saveDataLong(
				AppConstants.RANDOM_AYAT_LAST_FETCHED , System.currentTimeMillis()
													  )
	} else
	{
		viewModel.getAyatByAyaNumberInSurah(randomAyatLastFetchedNumber)
	}
	val stateOfRandomAyat = remember {
		viewModel.randomAyaState
	}.collectAsState()
	val stateOfRandomAyatJuz = remember {
		viewModel.randomAyaJuzState
	}.collectAsState()
	val stateOfRandomAyatSurah = remember {
		viewModel.randomAyaSurahState
	}.collectAsState()

	ElevatedCard(
			shape = MaterialTheme.shapes.extraLarge ,
			modifier = Modifier
				.padding(8.dp)
				.fillMaxWidth()
				.clickable {
					onNavigateToAyatScreen(
							//number : String , isSurah : Boolean , language : String , scrollToAya : Int?
							stateOfRandomAyat.value.suraNumber.toString() ,
							true ,
							PrivateSharedPreferences(context).getData(
									AppConstants.TRANSLATION_LANGUAGE ,
									"English"
																	 ) ,
							stateOfRandomAyat.value.ayaNumberInSurah
										  )
				} ,
				) {
		Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(4.dp) ,
				verticalAlignment = Alignment.CenterVertically ,
				horizontalArrangement = Arrangement.SpaceBetween
		   ) {
			ElevatedCard(
					shape = MaterialTheme.shapes.extraLarge ,
						) {
				Row(
						modifier = Modifier.padding(4.dp) ,
				   ) {
					CustomText(
							modifier = Modifier ,
							heading = "Verse" ,
							text = stateOfRandomAyat.value.ayaNumber.toString()
							  )
					Spacer(modifier = Modifier.width(4.dp))
					CustomText(
							modifier = Modifier ,
							heading = "Chapter" ,
							text = stateOfRandomAyat.value.suraNumber.toString()
							  )
				}
			}
			Spacer(modifier = Modifier.width(4.dp))
			Text(
					text = stateOfRandomAyatSurah.value.name ,
					style = MaterialTheme.typography.titleLarge ,
					fontSize = 26.sp ,
					fontFamily = utmaniQuranFont ,
					modifier = Modifier
						.padding(4.dp)
				)
		}
		Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(4.dp)
		   ) {

			Column(
					modifier = Modifier
						.weight(0.90f)
				  ) {
				SelectionContainer {
					CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
						stateOfRandomAyat.value.ayaArabic.let {
							Text(
									text = it ,
									style = MaterialTheme.typography.titleLarge ,
									fontSize = 26.sp ,
									fontFamily = utmaniQuranFont ,
									textAlign = if (stateOfRandomAyat.value.ayaNumber != 0) TextAlign.Justify else TextAlign.Center ,
									modifier = Modifier
										.fillMaxWidth()
										.padding(4.dp)
								)
						}
					}
				}
				Spacer(modifier = Modifier.height(4.dp))
				if (PrivateSharedPreferences(context).getData(
							AppConstants.TRANSLATION_LANGUAGE ,
							"English"
															 ) == "Urdu"
				)
				{
					CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
						Text(
								text = "${stateOfRandomAyat.value.ayaTranslationUrdu} ۔" ,
								style = MaterialTheme.typography.titleSmall ,
								fontSize = 16.sp ,
								fontFamily = urduFont ,
								textAlign = if (stateOfRandomAyat.value.ayaNumber != 0) TextAlign.Justify else TextAlign.Center ,
								modifier = Modifier
									.fillMaxWidth()
									.padding(horizontal = 4.dp)
							)
					}
				}
				if (PrivateSharedPreferences(context).getData(
							AppConstants.TRANSLATION_LANGUAGE ,
							"English"
															 ) == "English"
				)
				{
					stateOfRandomAyat.value.ayaTranslationEnglish.let {
						Text(
								text = it ,
								style = MaterialTheme.typography.bodySmall ,
								fontSize = 16.sp ,
								textAlign = if (stateOfRandomAyat.value.ayaNumber != 0) TextAlign.Justify else TextAlign.Center ,
								modifier = Modifier
									.fillMaxWidth()
									.padding(horizontal = 4.dp)
							)
					}
				}
			}
		}
	}

}

//preview
@Preview(showBackground = true)
@Composable
fun DashboardRandomAyatCardPreview()
{
	LocalDataStore.init(LocalContext.current)
	NimazTheme {
		DashboardRandomAyatCard { surahNumber , surahName , ayaNumber , ayaText -> }
	}
}