package com.arshadshah.nimaz.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.data.remote.models.Category
import com.arshadshah.nimaz.data.remote.models.Chapter
import com.arshadshah.nimaz.data.remote.models.Dua
import com.arshadshah.nimaz.data.remote.repositories.DuaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DuaViewModel : ViewModel() {

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _chapters = MutableStateFlow<List<Chapter>>(emptyList())
    val chapters: StateFlow<List<Chapter>> = _chapters.asStateFlow()

    private val _duas = MutableStateFlow<List<Dua>>(emptyList())
    val duas: StateFlow<List<Dua>> = _duas.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun getCategories() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = DuaRepository.getCategories()
                _categories.value = response.data ?: emptyList()
            } catch (e: Exception) {
                Log.e("DuaViewModel", "Error getting categories", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getChapters(id: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = DuaRepository.getChaptersByCategory(id)
                _chapters.value = response.data ?: emptyList()
            } catch (e: Exception) {
                Log.e("DuaViewModel", "Error getting chapters", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getDuas(chapterId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = DuaRepository.getDuasOfChapter(chapterId)
                _duas.value = response.data ?: emptyList()
            } catch (e: Exception) {
                Log.e("DuaViewModel", "Error getting duas", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
