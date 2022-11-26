package com.arshadshah.nimaz.ui.screens.quran

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.arshadshah.nimaz.data.remote.viewModel.QuranViewModel
import com.arshadshah.nimaz.ui.components.bLogic.quran.AyaJuzList
import com.arshadshah.nimaz.ui.components.bLogic.quran.AyaSurahList

@Composable
fun AyatScreen(
	number : String? ,
	isSurah : String ,
	isEnglish : String ,
	paddingValues : PaddingValues ,
			  )
{
	val viewModel = QuranViewModel()

	if (isSurah.toBoolean())
	{
		viewModel.getAllAyaForSurah(number !!.toInt() , isEnglish.toBoolean())
		AyaSurahList(
				number = number.toInt() , isEnglish = isEnglish.toBoolean() ,
				paddingValues = paddingValues ,
				state = viewModel.ayaSurahState.collectAsState()
					)

	} else
	{
		viewModel.getAllAyaForJuz(number !!.toInt() , isEnglish.toBoolean())
		AyaJuzList(
				number = number.toInt() ,
				isEnglish = isEnglish.toBoolean() ,
				paddingValues = paddingValues ,
				state = viewModel.ayaJuzstate.collectAsState()
				  )
	}
}
