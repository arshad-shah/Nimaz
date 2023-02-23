package com.arshadshah.nimaz.ui.components.bLogic.quran


import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import com.arshadshah.nimaz.data.remote.models.Surah
import com.arshadshah.nimaz.ui.components.ui.quran.SurahListUI

@Composable
fun SurahList(
	onNavigateToAyatScreen : (String , Boolean , String) -> Unit ,
	state : State<ArrayList<Surah>> ,
	loading : Boolean ,
	error : String ,
			 )
{

	if(loading)
	{
		SurahListUI(
				surahs = ArrayList(114) ,
				onNavigateToAyatScreen = onNavigateToAyatScreen ,
				loading = true
				   )
	}else if(error.isNotEmpty())
	{
		SurahListUI(
				surahs = ArrayList(114) ,
				onNavigateToAyatScreen = onNavigateToAyatScreen ,
				loading = false
				   )
	}else
	{
		SurahListUI(
				surahs = state.value ,
				onNavigateToAyatScreen = onNavigateToAyatScreen ,
				loading = false
				   )
	}
}