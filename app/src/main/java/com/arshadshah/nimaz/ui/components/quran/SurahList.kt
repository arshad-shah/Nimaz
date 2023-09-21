package com.arshadshah.nimaz.ui.components.quran


import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import com.arshadshah.nimaz.data.remote.models.Surah

@Composable
fun SurahList(
	onNavigateToAyatScreen : (String , Boolean , String , Int?) -> Unit ,
	state : State<ArrayList<Surah>> ,
	loading : Boolean ,
	error : String ,
			 )
{

	if (loading)
	{
		SurahListUI(
				 surahs = ArrayList(114) ,
				 onNavigateToAyatScreen = onNavigateToAyatScreen ,
				 loading = true
				   )
	} else if (error.isNotEmpty())
	{
		SurahListUI(
				 surahs = ArrayList(114) ,
				 onNavigateToAyatScreen = onNavigateToAyatScreen ,
				 loading = false
				   )
	} else
	{
		SurahListUI(
				 surahs = state.value ,
				 onNavigateToAyatScreen = onNavigateToAyatScreen ,
				 loading = false
				   )
	}
}