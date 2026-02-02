package com.arshadshah.nimaz.domain.repository

import com.arshadshah.nimaz.domain.model.AsmaUlHusna
import kotlinx.coroutines.flow.Flow

interface AsmaUlHusnaRepository {
    fun getAllNames(): Flow<List<AsmaUlHusna>>
    suspend fun getNameById(id: Int): AsmaUlHusna?
    fun searchNames(query: String): Flow<List<AsmaUlHusna>>
    fun getFavoriteNames(): Flow<List<AsmaUlHusna>>
    suspend fun toggleFavorite(nameId: Int)
    suspend fun isFavorite(nameId: Int): Boolean
}
