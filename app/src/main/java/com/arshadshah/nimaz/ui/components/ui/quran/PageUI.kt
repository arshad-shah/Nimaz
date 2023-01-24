package com.arshadshah.nimaz.ui.components.ui.quran

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.data.remote.models.Aya
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.arshadshah.nimaz.ui.theme.quranFont
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import es.dmoral.toasty.Toasty

@Composable
fun Page(AyaList : ArrayList<Aya> , paddingValues : PaddingValues)
{
	val context = LocalContext.current

	//get font size from shared preferences#
	val sharedPreferences = PrivateSharedPreferences(context)
	val arabicFontSize = sharedPreferences.getDataFloat("ArabicFontSize")
	Verses(
			modifier = Modifier
				.padding(paddingValues)
				.padding(4.dp)
		  ) {
		AyaList.forEach { aya ->
			val isNotBismillah =
				aya.ayaNumber != 0 || aya.ayaArabic != "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ ﴿١﴾"
			CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
				ClickableText(
						modifier = if (isNotBismillah)
						{
							Modifier.wrapContentWidth(align = Alignment.End)
						} else
						{
							Modifier
								.fillMaxWidth()
								.border(
										2.dp ,
										MaterialTheme.colorScheme.outline ,
										RoundedCornerShape(8.dp)
									   )
						} ,
						text = AnnotatedString(aya.ayaArabic) ,
						softWrap = true ,
						maxLines = 2 ,
						style = TextStyle(
								fontFamily = quranFont ,
								fontSize = arabicFontSize.sp ,
								lineHeight = 60.sp ,
								color = MaterialTheme.colorScheme.onSurface ,
								textAlign = if (isNotBismillah) TextAlign.Justify else TextAlign.Center ,
										 ) ,
						onClick = {
							Toasty.info(context , aya.ayaNumber.toString()).show()
						}
							 )
			}
		}
	}
}

@Composable
fun Verses(modifier : Modifier , content : @Composable () -> Unit)
{
	Layout(
			//scroll until content height is reached
			modifier = modifier ,
			content = content ,
			measurePolicy = { measurables , constraints ->
				val placeables = measurables.map { measurable ->
					//width of the screen
					val width = constraints.maxWidth
					measurable.measure(constraints.copy(minWidth = 0 , maxWidth = width))
				}
				// Place the children in the parent layout in a layout where the children are placed one after the other
				// like sentences in a paragraph
				layout(constraints.maxWidth , constraints.maxHeight) {
					var currentX = constraints.maxWidth
					var currentY = 0
					var currentHeight = 0
					var smallestHeight = 0
					placeables.forEach { placeable ->
						if (currentX - placeable.width < 0)
						{
							currentY += currentHeight.coerceAtLeast(smallestHeight)
							currentX = constraints.maxWidth
							currentHeight = placeable.height
							smallestHeight = smallestHeight.coerceAtMost(placeable.height)
						} else
						{
							currentHeight = currentHeight.coerceAtMost(placeable.height)
							smallestHeight = placeable.height
						}
						// Place the child in the parent layout
						placeable.placeRelative(x = currentX - placeable.width , y = currentY)
						// Update the current X position
						currentX -= placeable.width
					}
				}
			}
		  )
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
					0 ,
					"بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ ﴿١﴾" ,
					"In the name of Allah, the Entirely Merciful, the Especially Merciful." ,
					"Surah" ,
					1 ,
					"ENGLISH"
			   )
			   )
	ayaList.add(
			Aya(
					1 ,
					"ٱلْحَمْدُ لِلَّهِ رَبِّ ٱلْعَـٰلَمِينَ ﴿٢﴾" ,
					"All praise is due to Allah, Lord of the worlds." , "Surah" ,
					1 ,
					"ENGLISH"
			   )
			   )
	ayaList.add(
			Aya(
					2 ,
					"ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ ﴿٣﴾" ,
					"The Entirely Merciful, the Especially Merciful." ,
					"Surah" ,
					1 ,
					"ENGLISH"
			   )
			   )
	ayaList.add(
			Aya(
					3 ,
					"مَـٰلِكِ يَوْمِ ٱلدِّينِ ﴿٤﴾" ,
					"Master of the Day of Judgment." ,
					"Surah" ,
					1 ,
					"ENGLISH"
			   )
			   )
	ayaList.add(
			Aya(
					4 ,
					"إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ ﴿٥﴾" ,
					"You alone do we worship, and You alone do we implore for help." , "Surah" ,
					1 ,
					"ENGLISH"
			   )
			   )
	ayaList.add(
			Aya(
					5 ,
					"ٱهْدِنَا ٱلصِّرَٰطَ ٱلْمُسْتَقِيمَ ﴿٦﴾" ,
					"Guide us to the straight path." ,
					"Surah" ,
					1 ,
					"ENGLISH"
			   )
			   )
	ayaList.add(
			Aya(
					6 ,
					"صِرَٰطَ ٱلَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ ٱلْمَغْضُوبِ عَلَيْهِمْ وَلَا ٱلضَّآلِّينَ ﴿٧﴾" ,
					"The path of those upon whom You have bestowed favor, not of those who have evoked [Your] anger or of those who are astray." ,
					"Surah" ,
					1 ,
					"ENGLISH"
			   )
			   )
	NimazTheme(darkTheme = true) {
		Page(ayaList , PaddingValues())
	}

}