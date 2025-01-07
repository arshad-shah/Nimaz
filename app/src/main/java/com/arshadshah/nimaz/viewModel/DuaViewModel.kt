package com.arshadshah.nimaz.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.data.local.models.LocalCategory
import com.arshadshah.nimaz.data.local.models.LocalChapter
import com.arshadshah.nimaz.data.local.models.LocalDua
import com.arshadshah.nimaz.repositories.DuaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DuaViewModel @Inject constructor(
    private val duaRepository: DuaRepository
) : ViewModel() {

    private val _categories = MutableStateFlow<List<LocalCategory>>(emptyList())
    val categories: StateFlow<List<LocalCategory>> = _categories.asStateFlow()

    private val _chapters = MutableStateFlow<List<LocalChapter>>(emptyList())
    val chapters: StateFlow<List<LocalChapter>> = _chapters.asStateFlow()

    private val _duas = MutableStateFlow<List<LocalDua>>(emptyList())
    val duas: StateFlow<List<LocalDua>> = _duas.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun getCategories() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = duaRepository.getCategories()
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
                val response = duaRepository.getChaptersByCategory(id)
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
                val response = duaRepository.getDuasOfChapter(chapterId)
                _duas.value = response.data ?: emptyList()
            } catch (e: Exception) {
                Log.e("DuaViewModel", "Error getting duas", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
