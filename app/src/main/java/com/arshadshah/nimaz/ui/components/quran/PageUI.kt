package com.arshadshah.nimaz.ui.components.quran

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.models.Aya
import com.arshadshah.nimaz.ui.components.common.placeholder.material.PlaceholderHighlight
import com.arshadshah.nimaz.ui.components.common.placeholder.material.placeholder
import com.arshadshah.nimaz.ui.components.common.placeholder.material.shimmer
import com.arshadshah.nimaz.ui.theme.almajeed
import com.arshadshah.nimaz.ui.theme.amiri
import com.arshadshah.nimaz.ui.theme.hidayat
import com.arshadshah.nimaz.ui.theme.quranFont
import com.arshadshah.nimaz.ui.theme.utmaniQuranFont
import com.arshadshah.nimaz.viewModel.QuranViewModel

@Composable
fun Page(
    AyaList: ArrayList<Aya>,
    paddingValues: PaddingValues,
    loading: Boolean,
) {

    val context = LocalContext.current
    val viewModel = viewModel(
        key = AppConstants.QURAN_VIEWMODEL_KEY,
        initializer = { QuranViewModel(context) },
        viewModelStoreOwner = context as ComponentActivity
    )
    val arabicFontSize = remember {
        viewModel.arabic_Font_size
    }.collectAsState()
    val arabicFont = remember {
        viewModel.arabic_Font
    }.collectAsState()

    val state = rememberLazyListState()

    Log.d(
        "Nimaz: ListState",
        remember { derivedStateOf { state.firstVisibleItemIndex } }.toString()
    )

    val surah = remember {
        viewModel.surahState
    }.collectAsState()

    LazyColumn(
        state = state,
        modifier = Modifier
            .fillMaxWidth()
            .padding(paddingValues)
            .padding(4.dp),
        content = {
            item {
                Verses {
                    AyaList.forEach { aya ->
                        if (
                            aya.ayaNumberInQuran == 0 ||
                            aya.ayaArabic == "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ ﴿١﴾" ||
                            aya.ayaArabic == "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ" ||
                            aya.suraNumber == 9 && aya.ayaNumberInSurah == 1
                        ) {
                            viewModel.getSurahById(aya.suraNumber)
                            SurahHeader(
                                surah = surah.value,
                                loading = loading,
                            )
                        }
                        Verse(

                            isNotBismillah = aya.ayaArabic != "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ ﴿١﴾" && aya.ayaNumber != 0,
                            //split the aya into words
                            word = aya.ayaArabic,
                            loading = loading,
                            arabicFontSize = arabicFontSize.value,
                            fontStyle = arabicFont.value
                        )
                    }
                }
            }
        })
}

@Composable
fun Verse(
    isNotBismillah: Boolean,
    loading: Boolean,
    arabicFontSize: Float,
    word: String,
    fontStyle: String,
) {
    val cardBackgroundColor = if (!isNotBismillah) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val isSelected = remember { mutableStateOf(false) }
    SelectionContainer {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Text(
                modifier = if (isNotBismillah) {
                    Modifier
                        .placeholder(
                            visible = loading,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(4.dp),
                            highlight = PlaceholderHighlight.shimmer(
                                highlightColor = Color.White,
                            )
                        )
                        .background(
                            color = cardBackgroundColor,
                            shape = MaterialTheme.shapes.extraLarge
                        )
                        .clickable {
                            isSelected.value = !isSelected.value
                        }
                } else {
                    Modifier
                        .fillMaxWidth()
                        .placeholder(
                            visible = loading,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(4.dp),
                            highlight = PlaceholderHighlight.shimmer(
                                highlightColor = Color.White,
                            )
                        )
                        .background(
                            color = cardBackgroundColor,
                            shape = MaterialTheme.shapes.extraLarge
                        )
                },
                text = AnnotatedString(word),
                style = TextStyle(
                    fontFamily = when (fontStyle) {
                        "Default" -> {
                            utmaniQuranFont
                        }

                        "Quranme" -> {
                            quranFont
                        }

                        "Hidayat" -> {
                            hidayat
                        }

                        "Amiri" -> {
                            amiri
                        }

                        "IndoPak" -> {
                            almajeed
                        }

                        else -> {
                            utmaniQuranFont
                        }
                    },
                    //if arabic font size is not set then use default font size
                    fontSize = if (arabicFontSize == 0f) 24.sp else arabicFontSize.sp,
                    lineHeight = 50.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = if (isNotBismillah) TextAlign.Justify else TextAlign.Center,
                ),
            )
        }
    }
}

@Composable
fun Verses(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(
        modifier = modifier,
        content = content,
        measurePolicy = { measurables, constraints ->
            val placeables = measurables.map { measurable ->
                //width of the screen
                val width = constraints.maxWidth
                measurable.measure(constraints.copy(minWidth = 0, maxWidth = width))
            }
            // Place the children in the parent layout in a layout
            // where the children are placed one after the other
            // like sentences in a paragraph
            layout(constraints.maxWidth, constraints.maxHeight) {
                var currentX = constraints.maxWidth
                var currentY = 0
                var currentHeight = 0
                var smallestHeight = 0
                placeables.forEach { placeable ->
                    if (currentX - placeable.width < 0) {
                        currentY += currentHeight.coerceAtLeast(smallestHeight)
                        currentX = constraints.maxWidth
                        currentHeight = placeable.height
                        smallestHeight = smallestHeight.coerceAtMost(placeable.height)
                    } else {
                        currentHeight = currentHeight.coerceAtMost(placeable.height)
                        smallestHeight = placeable.height
                    }
                    // Place the child in the parent layout
                    placeable.placeRelative(x = currentX - placeable.width, y = currentY)
                    // Update the current X position
                    currentX -= placeable.width
                }
            }
        }
    )
}

//preview of verse
@Preview(showBackground = true)
@Composable
fun VersePreview() {
    Verse(
        isNotBismillah = true,
        loading = false,
        arabicFontSize = 24f,
        //something long to test the text wrapping
        word = "الرَّحْمَٰنِ الرَّحِيمِ ۚ إِنَّا أَعْطَيْنَاكَ الْكَوْثَرَ ۖ فَصَلِّ لِرَبِّكَ وَانْحَرْ ۚ إِنَّ شَانِئَكَ هُوَ الْأَبْتَرُ ۚ إِنَّهُ لَا يَغْنِي مِنْ اللَّهِ شَيْئًا ۚ إِنَّهُ هُوَ الْعَلِيُّ الْعَظِيمُ ۚ إِنَّهُ لَا يَغْنِي مِنْ اللَّهِ شَيْئًا ۚ إِنَّهُ هُوَ الْعَلِيُّ الْعَظِيمُ ۚ إِنَّهُ لَا يَغْنِي مِنْ اللَّهِ شَيْئًا ۚ إِنَّهُ هُوَ الْعَلِيُّ الْعَظِيمُ ۚ إِنَّهُ لَا يَغْنِي مِنْ اللَّهِ شَيْئًا ۚ إِنَّهُ هُوَ الْعَلِيُّ الْعَظِيمُ ۚ إِنَّهُ لَا يَغْنِي مِنْ اللَّهِ شَيْئًا ۚ إِنَّهُ هُوَ الْعَلِيُّ الْعَظِيمْ",
        fontStyle = "Default",
    )
}