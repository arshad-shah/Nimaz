package com.arshadshah.nimaz.ui.components.bLogic.quran


import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import com.arshadshah.nimaz.data.remote.models.Aya
import com.arshadshah.nimaz.ui.components.ui.quran.AyaListUI
import com.arshadshah.nimaz.ui.components.ui.quran.Page

@Composable
fun AyaSurahList(
	paddingValues : PaddingValues ,
	number : Int ,
	language : String ,
	type : String ,
	pageMode : State<String> ,
	state : State<ArrayList<Aya>> ,
	loading : Boolean ,
	error : String ,
	scrollToAya : Int? ,
				)
{

	if (loading)
	{
		if (pageMode.value == "List")
		{
			AyaListUI(
					ayaList = state.value ,
					paddingValues = paddingValues ,
					language = language ,
					loading = true ,
					type = type ,
					number = number ,
					scrollToAya = scrollToAya ,
					 )
		} else
		{
			Page(state.value , paddingValues , true)
		}
	} else if (error.isNotEmpty())
	{
		if (pageMode.value == "List")
		{
			AyaListUI(
					ayaList = state.value ,
					paddingValues = paddingValues ,
					language = language ,
					loading = false ,
					type = type ,
					number = number ,
					scrollToAya = scrollToAya ,
					 )
		} else
		{
			Page(state.value , paddingValues , false)
		}
	} else
	{
		if (pageMode.value == "List")
		{
			AyaListUI(
					ayaList = state.value ,
					paddingValues = paddingValues ,
					language = language ,
					loading = false ,
					type = type ,
					number = number ,
					scrollToAya = scrollToAya ,
					 )
		} else
		{
			Page(state.value , paddingValues , false)
		}
	}
}