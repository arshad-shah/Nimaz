package com.arshadshah.nimaz.ui.components.ui.quran

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.data.remote.models.Aya
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.arshadshah.nimaz.ui.theme.quranFont
import com.arshadshah.nimaz.ui.theme.urduFont
import com.arshadshah.nimaz.utils.PrivateSharedPreferences


@Composable
fun AyaListUI(ayaList : ArrayList<Aya> , paddingValues : PaddingValues , language : String)
{
	LazyColumn(userScrollEnabled = true , contentPadding = paddingValues) {
		items(ayaList.size) { index ->
			AyaListItemUI(
					ayaNumber = ayaList[index].ayaNumber.toString() ,
					ayaArabic = ayaList[index].ayaArabic ,
					ayaTranslation = ayaList[index].translation ,
					language = language
						 )
		}
	}
}

@Composable
fun AyaListItemUI(
	ayaNumber : String ,
	ayaArabic : String ,
	ayaTranslation : String ,
	language : String ,
				 )
{
	val cardBackgroundColor = if (ayaNumber == "0")
	{
		MaterialTheme.colorScheme.outline
	} else
	{
		//use default color
		MaterialTheme.colorScheme.surface
	}
	val context = LocalContext.current

	//get font size from shared preferences#
	val sharedPreferences = PrivateSharedPreferences(context)
	val arabicFontSize = sharedPreferences.getDataFloat("ArabicFontSize")
	val translationFontSize = sharedPreferences.getDataFloat("TranslationFontSize")
	ElevatedCard(
			modifier = Modifier
				.padding(4.dp)
				.fillMaxHeight()
				.fillMaxWidth()
				.border(2.dp , cardBackgroundColor , RoundedCornerShape(8.dp)) ,
			shape = RoundedCornerShape(8.dp)
				) {
		Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(8.dp)
		   ) {


			Column(
					modifier = Modifier
						.weight(0.90f)
				  ) {
				CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
					Text(
							text = ayaArabic ,
							style = MaterialTheme.typography.titleLarge ,
							fontSize = arabicFontSize.sp ,
							fontFamily = quranFont ,
							textAlign = if (ayaNumber != "0") TextAlign.Justify else TextAlign.Center ,
							modifier = Modifier
								.fillMaxWidth()
								.padding(4.dp)
						)
				}
				Spacer(modifier = Modifier.height(4.dp))
				if (language == "urdu")
				{
					CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
						Text(
								text = "$ayaTranslation ۔" ,
								style = MaterialTheme.typography.titleSmall ,
								fontSize = translationFontSize.sp ,
								fontFamily = urduFont ,
								textAlign = if (ayaNumber != "0") TextAlign.Justify else TextAlign.Center ,
								modifier = Modifier
									.fillMaxWidth()
									.padding(horizontal = 4.dp)
							)
					}
				} else
				{
					Text(
							text = ayaTranslation ,
							style = MaterialTheme.typography.bodySmall ,
							fontSize = translationFontSize.sp ,
							textAlign = if (ayaNumber != "0") TextAlign.Justify else TextAlign.Center ,
							modifier = Modifier
								.fillMaxWidth()
								.padding(horizontal = 4.dp)
						)
				}
			}
		}
	}
}

@Preview
@Composable
fun AyaListItemUIPreview()
{
	NimazTheme {
		//make 10 LocalAya
		val ayaList = ArrayList<Aya>()
		//add the aya to the list
		ayaList.add(
				Aya(
						0 ,
						"بسم الله الرحمن الرحيم" ,
						"In the name of Allah, the Entirely Merciful, the Especially Merciful." ,
						"Surah" ,
						1
				   )
				   )
		ayaList.add(
				Aya(
						1 ,
						"الحمد لله رب العالمين" ,
						"All praise is due to Allah, Lord of the worlds." , "Surah" ,
						1
				   )
				   )
		ayaList.add(
				Aya(
						2 ,
						"الرحمن الرحيم" ,
						"The Entirely Merciful, the Especially Merciful." ,
						"Surah" ,
						1
				   )
				   )
		ayaList.add(
				Aya(
						3 , "مالك يوم الدين" , "Master of the Day of Judgment." , "Surah" ,
						1
				   )
				   )
		ayaList.add(
				Aya(
						4 ,
						"إياك نعبد وإياك نستعين" ,
						"You alone do we worship, and You alone do we implore for help." , "Surah" ,
						1
				   )
				   )
		ayaList.add(
				Aya(
						5 , "اهدنا الصراط المستقيم" , "Guide us to the straight path." , "Surah" ,
						1
				   )
				   )
		ayaList.add(
				Aya(
						6 ,
						"صراط الذين أنعمت عليهم غير المغضوب عليهم ولا الضالين" ,
						"The path of those upon whom You have bestowed favor, not of those who have evoked [Your] anger or of those who are astray." ,
						"Surah" ,
						1
				   )
				   )

		AyaListUI(ayaList , PaddingValues(8.dp) , "english")
	}
}