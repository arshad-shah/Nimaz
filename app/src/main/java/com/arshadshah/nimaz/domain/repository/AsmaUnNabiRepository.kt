package com.arshadshah.nimaz.domain.repository

import com.arshadshah.nimaz.domain.model.AsmaUnNabi
import kotlinx.coroutines.flow.Flow

interface AsmaUnNabiRepository {
    fun getAllNames(): Flow<List<AsmaUnNabi>>
    suspend fun getNameById(id: Int): AsmaUnNabi?
    fun searchNames(query: String): Flow<List<AsmaUnNabi>>
    fun getFavoriteNames(): Flow<List<AsmaUnNabi>>
    suspend fun toggleFavorite(nameId: Int)
    suspend fun isFavorite(nameId: Int): Boolean
}
