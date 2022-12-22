package com.arshadshah.nimaz.ui.screens.quran

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.arshadshah.nimaz.data.remote.viewModel.QuranViewModel
import com.arshadshah.nimaz.ui.components.bLogic.quran.AyaJuzList
import com.arshadshah.nimaz.ui.components.bLogic.quran.AyaSurahList

@Composable
fun AyatScreen(
	number : String? ,
	isSurah : String ,
	language : String ,
	paddingValues : PaddingValues ,
			  )
{
	val context = LocalContext.current
	val viewModel = QuranViewModel(context)

	if (isSurah.toBoolean())
	{
		viewModel.getAllAyaForSurah(context , number !!.toInt() , language)
		val ayat = remember { viewModel.ayaSurahState }.collectAsState()
		AyaSurahList(
				number = number.toInt() , language = language ,
				paddingValues = paddingValues ,
				state = ayat ,
					)

	} else
	{
		viewModel.getAllAyaForJuz(context , number !!.toInt() , language)
		val ayat = remember { viewModel.ayaJuzstate }.collectAsState()
		AyaJuzList(
				number = number.toInt() ,
				language = language ,
				paddingValues = paddingValues ,
				state = ayat
				  )
	}
}
