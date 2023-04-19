package com.arshadshah.nimaz.ui.components.quran


import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import com.arshadshah.nimaz.data.remote.models.Aya

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