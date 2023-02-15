package com.arshadshah.nimaz.ui.screens.quran

import android.util.Log
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.arshadshah.nimaz.constants.AppConstants
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


	Log.d(AppConstants.QURAN_SCREEN_TAG , "AyatScreen: $number $isSurah $language")

	if (isSurah.toBoolean())
	{
		Log.d(AppConstants.QURAN_SCREEN_TAG , "AyatScreen: isSurah")
		viewModel.getAllAyaForSurah(number !!.toInt() , language)
		val ayat = remember { viewModel.ayaSurahState }.collectAsState()
		val ayatState = remember { viewModel.ayaState }.collectAsState()
		AyaSurahList(
				number = number.toInt() , language = language ,
				paddingValues = paddingValues ,
				state = ayat ,
				handleEvents = viewModel::handleAyaEvent ,
				ayaState = ayatState
					)

	} else
	{
		Log.d(AppConstants.QURAN_SCREEN_TAG , "AyatScreen: isJuz")
		viewModel.getAllAyaForJuz(number !!.toInt() , language)
		val ayat = remember { viewModel.ayaJuzstate }.collectAsState()
		val ayatState = remember { viewModel.ayaState }.collectAsState()
		AyaJuzList(
				number = number.toInt() ,
				language = language ,
				paddingValues = paddingValues ,
				state = ayat ,
				handleEvents = viewModel::handleAyaEvent ,
				ayaState = ayatState
				  )
	}
}
