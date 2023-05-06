package com.arshadshah.nimaz.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.data.remote.models.Chapter
import com.arshadshah.nimaz.data.remote.models.Dua
import com.arshadshah.nimaz.data.remote.repositories.DuaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DuaViewModel : ViewModel()
{

	//categories
	private val _categories = MutableStateFlow(ArrayList<Map<String, ArrayList<Chapter>>>())
	val categories = _categories.asStateFlow()

	//chapters for a category
	private val _chapters = MutableStateFlow(ArrayList<Chapter>())
	val chapters = _chapters.asStateFlow()

	//duas for a chapter
	private val _duas = MutableStateFlow(ArrayList<Dua>())
	val duas = _duas.asStateFlow()

	//get the categories
	fun getCategories(){
		viewModelScope.launch {
			val response = DuaRepository.getChaptersByCategories()
			if (response.data != null)
			{
				_categories.value = response.data
			}
			else
			{
				Log.e("DuaViewModel" , "getCategories: ${response.message}")
			}
		}
	}

	//get the chapters of a category
	fun getChapters(category : String){
		viewModelScope.launch {
			if(category == "All Chapters")
			{
				val response = DuaRepository.getAllChapters()
				_chapters.value = response
				return@launch
			}
			val response = DuaRepository.getChaptersByCategory(category)
			_chapters.value = response
		}
	}

	//get the duas of a chapter
	fun getDuas(chapterId : Int){
		viewModelScope.launch {
			val response = DuaRepository.getDuasOfChapter(chapterId)
			_duas.value = response
		}
	}

}