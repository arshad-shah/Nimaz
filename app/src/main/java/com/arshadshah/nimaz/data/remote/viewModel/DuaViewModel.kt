package com.arshadshah.nimaz.data.remote.viewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.data.remote.models.Chapter
import com.arshadshah.nimaz.data.remote.models.Dua
import com.arshadshah.nimaz.data.remote.repositories.DuaRepository
import com.arshadshah.nimaz.utils.LocalDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DuaViewModel(context:Context): ViewModel()
{
	sealed class DuaState
	{
		object Loading : DuaState()
		class Success(val duaList : Chapter) : DuaState()
		data class Error(val error: String) : DuaState()
	}

	private val _duaState = MutableStateFlow<DuaState>(DuaState.Loading)
	val duaState = _duaState.asStateFlow()

	//chapter state
	sealed class ChapterState
	{
		object Loading : ChapterState()
		class Success(val chapterList : ArrayList<Chapter>) : ChapterState()
		data class Error(val error: String) : ChapterState()
	}

	private val _chapterState = MutableStateFlow<ChapterState>(ChapterState.Loading)
	val chapterState = _chapterState.asStateFlow()

	init
	{
		_duaState.value = DuaState.Loading
		_chapterState.value = ChapterState.Loading
	}

	//get chapter list
	fun getChapterList()
	{
		viewModelScope.launch(Dispatchers.IO) {
			try
			{
				val dataStore = LocalDataStore.getDataStore()
				val chaptersCount = dataStore.countChapters()
				if (chaptersCount == 0)
				{
					val response = DuaRepository.getChapters()
					if (response != null)
					{
						_chapterState.value = ChapterState.Success(response.data!!)
						dataStore.saveAllChapters(response.data)
					}
				} else
				{
					//get the chapters from the database
					val chapters = dataStore.getAllChapters()
					_chapterState.value = ChapterState.Success(chapters as ArrayList<Chapter>)
				}
			}
			catch (e: Exception)
			{
				_chapterState.value = ChapterState.Error(e.message.toString())
			}
		}
	}

	//get one chapter by id
	fun getChapterById(id: Int)
	{
		viewModelScope.launch(Dispatchers.IO) {
			try
			{
				val dataStore = LocalDataStore.getDataStore()
				val chapter = dataStore.getDuasOfChapter(id)
				_duaState.value = DuaState.Success(chapter)
			}
			catch (e: Exception)
			{
				_duaState.value = DuaState.Error(e.message.toString())
			}
		}
	}
}