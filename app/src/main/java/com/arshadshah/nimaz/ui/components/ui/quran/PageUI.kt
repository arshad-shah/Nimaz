package com.arshadshah.nimaz.ui.components.ui.quran

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.data.remote.models.Aya
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.arshadshah.nimaz.ui.theme.quranFont
import java.text.NumberFormat
import java.util.*

@Composable
fun Verses(AyaList : ArrayList<Aya> , paddingValues : PaddingValues)
{
	//if the aya is bismillah, then it is the first aya of the surah
	var text = ""
	for (aya in AyaList)
	{
		val unicodeAyaEndEnd = "\uFD3E"
		val unicodeAyaEndStart = "\uFD3F"

		val number = aya.ayaNumber
		val arabicLocal = Locale.forLanguageTag("AR")
		val nf : NumberFormat = NumberFormat.getInstance(arabicLocal)
		val endOfAyaWithNumber = nf.format(number)
		//add the unicode characters to the end of the aya
		val unicodeWithNumber = "$unicodeAyaEndStart$endOfAyaWithNumber$unicodeAyaEndEnd"

		val ayaArabicWithEnd = "${aya.ayaArabic} $unicodeWithNumber"

		//if the aya is bisillah, then add a new line at the start and end of the aya
		//check both english and urdu translations of bismillah and the arabic text
		if (aya.ayaArabic == "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ")
		{
			text += "\t$text $ayaArabicWithEnd\n"
		} else
		{
			text = "$text $ayaArabicWithEnd"
		}
	}

	LazyColumn {
		item {
			Verse(text , paddingValues)
		}
	}
}

@Composable
fun Verse(arabic : String , paddingValues : PaddingValues)
{

	CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
		Text(
				text = arabic ,
				style = MaterialTheme.typography.headlineMedium ,
				fontFamily = quranFont ,
				textAlign = TextAlign.Justify ,
				modifier = Modifier
					.padding(paddingValues)
					.padding(16.dp) ,
				lineHeight = 60.sp ,
			)
	}
}

@Preview
@Composable
fun PageUIPreview()
{

	//make 10 LocalAya
	val ayaList = ArrayList<Aya>()
	//add the aya to the list
	ayaList.add(
			Aya(
					1 ,
					"بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ" ,
					"In the name of Allah, the Entirely Merciful, the Especially Merciful." ,
					"Surah" ,
					1
			   )
			   )
	ayaList.add(
			Aya(
					2 ,
					"الحمد لله رب العالمين" ,
					"All praise is due to Allah, Lord of the worlds." , "Surah" ,
					1
			   )
			   )
	ayaList.add(
			Aya(
					3 ,
					"الرحمن الرحيم" ,
					"The Entirely Merciful, the Especially Merciful." ,
					"Surah" ,
					1
			   )
			   )
	ayaList.add(
			Aya(
					4 , "مالك يوم الدين" , "Master of the Day of Judgment." , "Surah" ,
					1
			   )
			   )
	ayaList.add(
			Aya(
					5 ,
					"إياك نعبد وإياك نستعين" ,
					"You alone do we worship, and You alone do we implore for help." , "Surah" ,
					1
			   )
			   )
	ayaList.add(
			Aya(
					6 , "اهدنا الصراط المستقيم" , "Guide us to the straight path." , "Surah" ,
					1
			   )
			   )
	ayaList.add(
			Aya(
					7 ,
					"صراط الذين أنعمت عليهم غير المغضوب عليهم ولا الضالين" ,
					"The path of those upon whom You have bestowed favor, not of those who have evoked [Your] anger or of those who are astray." ,
					"Surah" ,
					1
			   )
			   )

	NimazTheme {
		Verses(ayaList , PaddingValues())
	}

}