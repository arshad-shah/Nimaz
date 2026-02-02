package com.arshadshah.nimaz.domain.repository

import com.arshadshah.nimaz.domain.model.Prophet
import kotlinx.coroutines.flow.Flow

interface ProphetRepository {
    fun getAllProphets(): Flow<List<Prophet>>
    suspend fun getProphetById(id: Int): Prophet?
    fun searchProphets(query: String): Flow<List<Prophet>>
    fun getFavoriteProphets(): Flow<List<Prophet>>
    suspend fun toggleFavorite(prophetId: Int)
    suspend fun isFavorite(prophetId: Int): Boolean
}
