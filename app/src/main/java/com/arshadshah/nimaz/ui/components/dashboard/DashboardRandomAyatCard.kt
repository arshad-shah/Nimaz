package com.arshadshah.nimaz.ui.components.ui.quran

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.ui.components.common.CustomText
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.arshadshah.nimaz.ui.theme.urduFont
import com.arshadshah.nimaz.ui.theme.utmaniQuranFont
import com.arshadshah.nimaz.utils.LocalDataStore
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.viewModel.QuranViewModel

@Composable
fun DashboardRandomAyatCard(onNavigateToAyatScreen : (String , Boolean , String , Int) -> Unit)
{
	val context = LocalContext.current
	val viewModel = viewModel(
			key = AppConstants.QURAN_VIEWMODEL_KEY ,
			initializer = { QuranViewModel(context) } ,
			viewModelStoreOwner = context as ComponentActivity)

	//only get the random aya once every time the screen is opened
	LaunchedEffect(key1 = true) {
		viewModel.getRandomAya()
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

	val translationSelected = PrivateSharedPreferences(context).getData(
			AppConstants.TRANSLATION_LANGUAGE ,
			"English"
																	   )

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
					.padding(top = 8.dp , bottom = 8.dp , end = 16.dp) ,
				verticalAlignment = Alignment.CenterVertically ,
				horizontalArrangement = Arrangement.SpaceBetween
		   ) {
			Row(
					modifier = Modifier.padding(8.dp) ,
			   ) {
				CustomText(
						modifier = Modifier ,
						heading = "Chapter" ,
						text = stateOfRandomAyat.value.suraNumber.toString()
						  )
				Spacer(modifier = Modifier.width(8.dp))
				CustomText(
						modifier = Modifier ,
						heading = "Verse" ,
						text = stateOfRandomAyat.value.ayaNumber.toString()
						  )
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
			Spacer(modifier = Modifier.width(4.dp))
			IconButton(
					onClick = {
						//share the aya
						val shareIntent = Intent(Intent.ACTION_SEND)
						shareIntent.type = "text/plain"
						//create the share message
						//with the aya text, aya translation
						//the sura number followed by the aya number
						shareIntent.putExtra(
								Intent.EXTRA_TEXT ,
								"Aya of the Day - Chapter ${stateOfRandomAyat.value.suraNumber}: Verse ${stateOfRandomAyat.value.ayaNumberInSurah}\n\n" +
										"${stateOfRandomAyat.value.ayaArabic} \n\n" +
										"${if (translationSelected == "Urdu") stateOfRandomAyat.value.ayaTranslationUrdu else stateOfRandomAyat.value.ayaTranslationEnglish} " +
										"\n\n${stateOfRandomAyat.value.suraNumber}:${stateOfRandomAyat.value.ayaNumberInSurah}" +
										"\n\nDownload the app to read more: https://play.google.com/store/apps/details?id=com.arshadshah.nimaz"
											)
						shareIntent.putExtra(Intent.EXTRA_SUBJECT , "Aya of the Day")

						//start the share intent
						context.startActivity(
								Intent.createChooser(
										shareIntent ,
										"Share Ramadan Times"
													)
											 )
					} , modifier = Modifier.size(24.dp)) {
				Icon(
						painter = painterResource(id = R.drawable.share_icon) ,
						contentDescription = "Share Ramadan Times" ,
					)
			}
		}
		Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(8.dp)
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
				if (translationSelected == "Urdu")
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
				if (translationSelected == "English")
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