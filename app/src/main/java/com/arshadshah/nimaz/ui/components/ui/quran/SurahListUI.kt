package com.arshadshah.nimaz.ui.components.ui.quran

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_SURAH_ITEM
import com.arshadshah.nimaz.data.remote.models.Surah
import com.arshadshah.nimaz.ui.theme.utmaniQuranFont
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer

@Composable
fun SurahListUI(
	surahs : ArrayList<Surah> ,
	onNavigateToAyatScreen : (String , Boolean , String) -> Unit ,
	loading : Boolean ,
			   )
{
	LazyColumn(
			userScrollEnabled = !loading,
			modifier = Modifier.testTag(AppConstants.TEST_TAG_QURAN_SURAH)
			  ) {
		items(surahs.size) { index ->
			SurahListItemUI(
					loading = loading ,
					surahNumber = surahs[index].number.toString() ,
					surahAyaAmount = surahs[index].numberOfAyahs.toString() ,
					surahName = surahs[index].name ,
					englishName = surahs[index].englishName ,
					englishNameTranslation = surahs[index].englishNameTranslation ,
					type = surahs[index].revelationType ,
					rukus = surahs[index].rukus.toString() ,
					onNavigateToAyatScreen = onNavigateToAyatScreen
						   )
		}
	}
}

@Composable
fun SurahListItemUI(
	surahNumber : String ,
	surahAyaAmount : String ,
	surahName : String ,
	englishName : String ,
	englishNameTranslation : String ,
	type : String ,
	rukus : String ,
	onNavigateToAyatScreen : (String , Boolean , String) -> Unit ,
	context : Context = LocalContext.current ,
	loading : Boolean ,
				   )
{
	ElevatedCard(
			modifier = Modifier
				.padding(4.dp)
				.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
				.fillMaxWidth()
				.background(color = MaterialTheme.colorScheme.surface) ,
			shape = RoundedCornerShape(8.dp)
				) {
		//get the translation type from shared preferences
		val translationType =
			PrivateSharedPreferences(context).getData(
					key = AppConstants.TRANSLATION_LANGUAGE ,
					s = "English"
													 )
		val language = when (translationType)
		{
			"English" -> "english"
			"Urdu" -> "urdu"
			else -> "english"
		}
		Row(
				modifier = Modifier
					.padding(8.dp)
					.fillMaxWidth()
					.testTag(TEST_TAG_SURAH_ITEM + surahNumber)
					.clickable(
							enabled = !loading,
							onClick = {
								onNavigateToAyatScreen(surahNumber , true , language)
							}
							  )
		   ) {
			Text(
					modifier = Modifier
						.align(Alignment.CenterVertically)
						.weight(0.15f)
						.placeholder(
								visible = loading ,
								color = MaterialTheme.colorScheme.outline ,
								shape = RoundedCornerShape(4.dp) ,
								highlight = PlaceholderHighlight.shimmer(
										highlightColor = Color.White ,
																		)
									) ,
					text = "$surahNumber." ,
					style = MaterialTheme.typography.bodyLarge
				)

			Column(
					modifier = Modifier
						.padding(16.dp , 0.dp)
						.align(Alignment.CenterVertically)
						.weight(0.50f)
				  ) {
				Text(
						text = englishName ,
						style = MaterialTheme.typography.titleSmall ,
						modifier = Modifier.placeholder(
								visible = loading ,
								color = MaterialTheme.colorScheme.outline ,
								shape = RoundedCornerShape(4.dp) ,
								highlight = PlaceholderHighlight.shimmer(
										highlightColor = Color.White ,
																		)
													   )
					)
				//apply quran font
				Text(
						text = surahName ,
						style = MaterialTheme.typography.titleLarge ,
						fontFamily = utmaniQuranFont ,
						modifier = Modifier
							.padding(vertical = 4.dp)
							.placeholder(
									visible = loading ,
									color = MaterialTheme.colorScheme.outline ,
									shape = RoundedCornerShape(4.dp) ,
									highlight = PlaceholderHighlight.shimmer(
											highlightColor = Color.White ,
																			)
										)
					)
				Text(
						text = englishNameTranslation ,
						style = MaterialTheme.typography.titleSmall ,
						modifier = Modifier.placeholder(
								visible = loading ,
								color = MaterialTheme.colorScheme.outline ,
								shape = RoundedCornerShape(4.dp) ,
								highlight = PlaceholderHighlight.shimmer(
										highlightColor = Color.White ,
																		)
													   )
					)
			}

			Column(
					modifier = Modifier
						.align(Alignment.CenterVertically)
						.weight(0.30f)
						.fillMaxWidth()
				  ) {
				Text(
						text = "Type: $type" ,
						style = MaterialTheme.typography.titleSmall ,
						modifier = Modifier
							.padding(4.dp)
							.fillMaxWidth()
							.placeholder(
									visible = loading ,
									color = MaterialTheme.colorScheme.outline ,
									shape = RoundedCornerShape(4.dp) ,
									highlight = PlaceholderHighlight.shimmer(
											highlightColor = Color.White ,
																			)
										)
					)
				Text(
						text = "Ayat: $surahAyaAmount" ,
						style = MaterialTheme.typography.titleSmall ,
						modifier = Modifier
							.padding(4.dp)
							.fillMaxWidth()
							.placeholder(
									visible = loading ,
									color = MaterialTheme.colorScheme.outline ,
									shape = RoundedCornerShape(4.dp) ,
									highlight = PlaceholderHighlight.shimmer(
											highlightColor = Color.White ,
																			)
										)
					)
				Text(
						text = "Ruku: $rukus" ,
						style = MaterialTheme.typography.titleSmall ,
						modifier = Modifier
							.padding(4.dp)
							.fillMaxWidth()
							.placeholder(
									visible = loading ,
									color = MaterialTheme.colorScheme.outline ,
									shape = RoundedCornerShape(4.dp) ,
									highlight = PlaceholderHighlight.shimmer(
											highlightColor = Color.White ,
																			)
										)
					)
			}
			//an arrow right icon
			Icon(
					painter = painterResource(id = R.drawable.angle_small_right_icon) ,
					contentDescription = "Clear" ,
					modifier = Modifier
						.align(Alignment.CenterVertically)
						.weight(0.05f)
						.fillMaxWidth()
						.placeholder(
								visible = loading ,
								color = MaterialTheme.colorScheme.outline ,
								shape = RoundedCornerShape(4.dp) ,
								highlight = PlaceholderHighlight.shimmer(
										highlightColor = Color.White ,
																		)
									)
				)
		}
	}
}