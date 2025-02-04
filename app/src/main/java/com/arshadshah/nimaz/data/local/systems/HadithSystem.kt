package com.arshadshah.nimaz.data.local.systems

import com.arshadshah.nimaz.data.local.dao.CategoryDao
import com.arshadshah.nimaz.data.local.dao.HadithDao
import javax.inject.Inject

class HadithSystem @Inject constructor(
    private val hadithDao: HadithDao,
    private val categoryDao: CategoryDao
) {
    suspend fun getAllMetadata() = hadithDao.getAllMetadata()
    suspend fun getAllHadithChaptersForABook(bookId: Int) =
        hadithDao.getAllHadithChaptersForABook(bookId)

    suspend fun getAllHadithsForABook(bookId: Int, chapterId: Int) =
        hadithDao.getAllHadithsForABook(bookId, chapterId)

    suspend fun updateFavouriteStatus(id: Int, favourite: Boolean) =
        hadithDao.updateFavouriteStatus(id, favourite)

    suspend fun getAllFavourites() = hadithDao.getAllFavourites()
    suspend fun getAllCategories() = categoryDao.getAllCategories()
}