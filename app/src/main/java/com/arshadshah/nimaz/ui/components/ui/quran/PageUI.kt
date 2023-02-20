package com.arshadshah.nimaz.ui.components.ui.quran

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
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
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer
import es.dmoral.toasty.Toasty
import kotlin.reflect.KFunction1

@Composable
fun Page(
	AyaList : ArrayList<Aya> ,
	paddingValues : PaddingValues ,
	loading : Boolean ,
	handleEvents : KFunction1<QuranViewModel.AyaEvent , Unit>
		)
{

	val context = LocalContext.current
	val viewModel = viewModel(key = "QuranViewModel" , initializer = { QuranViewModel(context) } , viewModelStoreOwner = context as ComponentActivity)
	val arabicFontSize = remember {
		viewModel.arabic_Font_size
	}.collectAsState()
	val arabicFont = remember {
		viewModel.arabic_Font
	}.collectAsState()

	Verses(
			modifier = Modifier
				.padding(paddingValues)
				.padding(4.dp)
		  ) {
		AyaList.forEach { aya ->
			val isNotBismillah =
				aya.ayaNumber != 0 || aya.ayaArabic != "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ ﴿١﴾"
			Verse(
					context = context ,
					isNotBismillah = isNotBismillah ,
					//split the aya into words
					word = aya.ayaArabic ,
					loading = loading ,
					arabicFontSize = arabicFontSize.value ,
					fontStyle = arabicFont.value
				 )
		}
	}
}

@Composable
fun Verse(
	context : Context ,
	isNotBismillah : Boolean ,
	loading : Boolean ,
	arabicFontSize : Float ,
	word : String ,
	fontStyle : String ,
		 )
{
	CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
		ClickableText(
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
				} ,
				text = AnnotatedString(word) ,
				softWrap = true ,
				maxLines = 2 ,
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
						lineHeight = 60.sp ,
						color = MaterialTheme.colorScheme.onSurface ,
						textAlign = if (isNotBismillah) TextAlign.Justify else TextAlign.Center ,
								 ) ,
				onClick = {
					Toasty.info(context , word).show()
				}
					 )
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