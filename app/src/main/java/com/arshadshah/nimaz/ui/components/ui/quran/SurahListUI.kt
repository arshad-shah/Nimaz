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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.data.remote.models.Surah
import com.arshadshah.nimaz.ui.theme.quranFont
import com.arshadshah.nimaz.utils.PrivateSharedPreferences

@Composable
fun SurahListUI(
	surahs : ArrayList<Surah> ,
	onNavigateToAyatScreen : (String , Boolean , String) -> Unit ,
			   )
{
	LazyColumn(userScrollEnabled = true) {
		items(surahs.size) { index ->
			SurahListItemUI(
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
			PrivateSharedPreferences(context).getData(key = "Translation" , s = "English")
		val language = when (translationType)
		{
			"English" -> "english"
			"Urdu" -> "urdu"
			else -> "en"
		}
		Row(
				modifier = Modifier
					.padding(8.dp)
					.fillMaxWidth()
					.clickable(
							enabled = true ,
							onClick = {
								onNavigateToAyatScreen(surahNumber , true , language)
							}
							  )
		   ) {

			Text(
					modifier = Modifier
						.align(Alignment.CenterVertically)
						.weight(0.15f) ,
					text = "$surahNumber." ,
					style = MaterialTheme.typography.bodyLarge
				)

			Column(
					modifier = Modifier
						.padding(16.dp , 0.dp)
						.align(Alignment.CenterVertically)
						.weight(0.50f)
				  ) {
				Text(text = englishName , style = MaterialTheme.typography.titleSmall)
				//apply quran font
				Text(
						text = surahName ,
						style = MaterialTheme.typography.titleLarge ,
						fontFamily = quranFont
					)
				Text(text = englishNameTranslation , style = MaterialTheme.typography.titleSmall)
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
					)
				Text(
						text = "Ayat: $surahAyaAmount" ,
						style = MaterialTheme.typography.titleSmall ,
						modifier = Modifier
							.padding(4.dp)
							.fillMaxWidth()
					)
				Text(
						text = "Ruku: $rukus" ,
						style = MaterialTheme.typography.titleSmall ,
						modifier = Modifier
							.padding(4.dp)
							.fillMaxWidth()
					)
			}
			//an arrow right icon
			Icon(
					imageVector = Icons.Rounded.KeyboardArrowRight ,
					contentDescription = "Clear" ,
					modifier = Modifier
						.align(Alignment.CenterVertically)
						.weight(0.05f)
						.fillMaxWidth()
				)
		}
	}
}