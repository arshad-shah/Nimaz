package com.arshadshah.nimaz.data.local.systems

import com.arshadshah.nimaz.data.local.dao.TafsirDao
import com.arshadshah.nimaz.data.local.dao.TafsirEditionDao
import javax.inject.Inject

class TafsirSystem @Inject constructor(
    private val tafsirDao: TafsirDao,
    private val tafsirEditionDao: TafsirEditionDao
) {
    // Tafsir Content Operations
    suspend fun getTafsirById(id: Long) = tafsirDao.getTafsirById(id)

    suspend fun getTafsirForAya(ayaNumber: Int, editionId: Int) =
        tafsirDao.getTafsirForAya(ayaNumber, editionId)

    suspend fun getTafsirByEdition(editionId: Int) = tafsirDao.getTafsirByEdition(editionId)

    suspend fun getTafsirByLanguage(language: String) = tafsirDao.getTafsirByLanguage(language)

    // Edition Operations
    suspend fun getAllEditions() = tafsirEditionDao.getAllEditions()

    suspend fun getEditionById(id: Int) = tafsirEditionDao.getEditionById(id)

    suspend fun getEditionsByLanguage(language: String) =
        tafsirEditionDao.getEditionsByLanguage(language)

    suspend fun getEditionsByLanguageAndAuthor(language: String, authorName: String) =
        tafsirEditionDao.getEditionsByLanguageAndAuthor(language, authorName)

    suspend fun getEditionsByAuthor(authorName: String) =
        tafsirEditionDao.getEditionsByAuthor(authorName)

    suspend fun getEditionCount() = tafsirEditionDao.getEditionCount()
}