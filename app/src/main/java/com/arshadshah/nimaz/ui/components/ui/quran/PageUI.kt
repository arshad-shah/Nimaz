package com.arshadshah.nimaz.ui.components.ui.quran

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.data.remote.models.Aya
import com.arshadshah.nimaz.data.remote.viewModel.QuranViewModel
import com.arshadshah.nimaz.ui.theme.amiri
import com.arshadshah.nimaz.ui.theme.hidayat
import com.arshadshah.nimaz.ui.theme.quranFont
import com.arshadshah.nimaz.ui.theme.utmaniQuranFont
import com.arshadshah.nimaz.utils.LocalDataStore
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer

@Composable
fun Page(
	AyaList : ArrayList<Aya> ,
	paddingValues : PaddingValues ,
	loading : Boolean ,
		)
{

	val context = LocalContext.current
	val viewModel = viewModel(
			key = "QuranViewModel" ,
			initializer = { QuranViewModel(context) } ,
			viewModelStoreOwner = context as ComponentActivity)
	val arabicFontSize = remember {
		viewModel.arabic_Font_size
	}.collectAsState()
	val arabicFont = remember {
		viewModel.arabic_Font
	}.collectAsState()

	val state = rememberLazyListState()

	Log.d("Nimaz: ListState" , remember { derivedStateOf { state.firstVisibleItemIndex } }.toString())

	LazyColumn(
			state = state,
			modifier = Modifier
				.fillMaxWidth()
				.padding(paddingValues)
				.padding(4.dp),
			content ={
		item{
			Verses {
				AyaList.forEach { aya ->
					Verse(

							isNotBismillah = aya.ayaArabic != "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ ﴿١﴾"  && aya.ayaNumber != 0,
							//split the aya into words
							word = aya.ayaArabic ,
							loading = loading ,
							arabicFontSize = arabicFontSize.value ,
							fontStyle = arabicFont.value
						 )
				}
			}
		}
	})
}

@Composable
fun Verse(
	isNotBismillah : Boolean ,
	loading : Boolean ,
	arabicFontSize : Float ,
	word : String ,
	fontStyle : String ,
		 )
{
	val isSelected = remember { mutableStateOf(false) }
	SelectionContainer {
		CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
			Text(
					modifier = if (isNotBismillah)
					{
						Modifier
							.placeholder(
									visible = loading ,
									color = MaterialTheme.colorScheme.outline ,
									shape = RoundedCornerShape(4.dp) ,
									highlight = PlaceholderHighlight.shimmer(
											highlightColor = Color.White ,
																			)
										)
							.background(
									color = if (isSelected.value) MaterialTheme.colorScheme.primary.copy(
											alpha = 0.5f
																										) else Color.Transparent ,
									shape = RoundedCornerShape(8.dp)
									   )
							.clickable {
								isSelected.value = ! isSelected.value
							}
					} else
					{
						Modifier
							.fillMaxWidth()
							.border(
									2.dp ,
									MaterialTheme.colorScheme.outline ,
									RoundedCornerShape(8.dp)
								   )
							.placeholder(
									visible = loading ,
									color = MaterialTheme.colorScheme.outline ,
									shape = RoundedCornerShape(4.dp) ,
									highlight = PlaceholderHighlight.shimmer(
											highlightColor = Color.White ,
																			)
										)
							.background(
									color = if (isSelected.value) MaterialTheme.colorScheme.primary.copy(
											alpha = 0.5f
																										) else Color.Transparent ,
									shape = RoundedCornerShape(8.dp)
									   )
					} ,
					text = AnnotatedString(word) ,
					style = TextStyle(
							fontFamily = when (fontStyle)
							{
								"Default" ->
								{
									utmaniQuranFont
								}

								"Quranme" ->
								{
									quranFont
								}

								"Hidayat" ->
								{
									hidayat
								}

								"Amiri" ->
								{
									amiri
								}

								else ->
								{
									utmaniQuranFont
								}
							} ,
							//if arabic font size is not set then use default font size
							fontSize = if (arabicFontSize == 0f) 24.sp else arabicFontSize.sp ,
							lineHeight = 50.sp ,
							color = MaterialTheme.colorScheme.onSurface ,
							textAlign = if (isNotBismillah) TextAlign.Justify else TextAlign.Center ,
									 ) ,
						 )
		}
	}
}

@Composable
fun Verses(modifier : Modifier = Modifier ,
		   content : @Composable () -> Unit)
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


//preview of page
@Preview(showBackground = true)
@Composable
fun PagePreview()
{
	val context = LocalContext.current
	LocalDataStore.init(context)
	val viewModel = viewModel(
			key = "QuranViewModel" ,
			initializer = { QuranViewModel(context) } ,
			viewModelStoreOwner = context as ComponentActivity)
	viewModel.getAllAyaForJuz(1, "English")

	val listOfAya = remember {
		viewModel.ayaListState
	}.collectAsState()

	val loading = remember {
		viewModel.loadingState
	}.collectAsState()

	Page(
			AyaList = listOfAya.value ,
		 paddingValues =  PaddingValues(16.dp) ,
			loading = loading.value ,
		)
}

//preview of verse
@Preview(showBackground = true)
@Composable
fun VersePreview()
{
	Verse(
			isNotBismillah = true ,
			loading = false ,
			arabicFontSize = 24f ,
			//something long to test the text wrapping
			word = "الرَّحْمَٰنِ الرَّحِيمِ ۚ إِنَّا أَعْطَيْنَاكَ الْكَوْثَرَ ۖ فَصَلِّ لِرَبِّكَ وَانْحَرْ ۚ إِنَّ شَانِئَكَ هُوَ الْأَبْتَرُ ۚ إِنَّهُ لَا يَغْنِي مِنْ اللَّهِ شَيْئًا ۚ إِنَّهُ هُوَ الْعَلِيُّ الْعَظِيمُ ۚ إِنَّهُ لَا يَغْنِي مِنْ اللَّهِ شَيْئًا ۚ إِنَّهُ هُوَ الْعَلِيُّ الْعَظِيمُ ۚ إِنَّهُ لَا يَغْنِي مِنْ اللَّهِ شَيْئًا ۚ إِنَّهُ هُوَ الْعَلِيُّ الْعَظِيمُ ۚ إِنَّهُ لَا يَغْنِي مِنْ اللَّهِ شَيْئًا ۚ إِنَّهُ هُوَ الْعَلِيُّ الْعَظِيمُ ۚ إِنَّهُ لَا يَغْنِي مِنْ اللَّهِ شَيْئًا ۚ إِنَّهُ هُوَ الْعَلِيُّ الْعَظِيمْ" ,
			fontStyle = "Default" ,
		   )
}