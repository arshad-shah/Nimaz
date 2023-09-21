package com.arshadshah.nimaz.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.data.remote.models.Category
import com.arshadshah.nimaz.data.remote.models.Chapter
import com.arshadshah.nimaz.data.remote.models.Dua
import com.arshadshah.nimaz.data.remote.repositories.DuaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DuaViewModel : ViewModel()
{

	//categories
	private val _categories = MutableStateFlow(ArrayList<Category>())
	val categories = _categories.asStateFlow()

	//chapters for a category
	private val _chapters = MutableStateFlow(ArrayList<Chapter>())
	val chapters = _chapters.asStateFlow()

	//duas for a chapter
	private val _duas = MutableStateFlow(ArrayList<Dua>())
	val duas = _duas.asStateFlow()

	//get the categories
	fun getCategories()
	{
		viewModelScope.launch {
			val response = DuaRepository.getCategories()
			if (response.data != null)
			{
				_categories.value = response.data
			} else
			{
				Log.e("DuaViewModel" , "getCategories: ${response.message}")
			}
		}
	}

	//get the chapters of a category
	fun getChapters(id : Int)
	{
		viewModelScope.launch {
			val response = DuaRepository.getChaptersByCategory(id)
			_chapters.value = response.data !!
		}
	}

	//get the duas of a chapter
	fun getDuas(chapterId : Int)
	{
		viewModelScope.launch {
			val response = DuaRepository.getDuasOfChapter(chapterId)
			_duas.value = response.data !!
		}
	}

}