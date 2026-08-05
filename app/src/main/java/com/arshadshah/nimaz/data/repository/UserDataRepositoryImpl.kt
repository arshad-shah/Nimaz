package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.data.local.user.NimazUserDatabase
import com.arshadshah.nimaz.domain.repository.UserDataRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Clears the user database, one DAO at a time.
 *
 * Everything the person made lives in this one database, so the operation is honest by
 * construction: the content database is not reachable from here at all. When a table is
 * added to [NimazUserDatabase], add its clear here — this list is the only place that has
 * to know, and it is the reason the loop no longer lives in a ViewModel.
 */
@Singleton
class UserDataRepositoryImpl @Inject constructor(
    private val userDatabase: NimazUserDatabase
) : UserDataRepository {

    override suspend fun clearAllUserData() {
        userDatabase.bookmarkDao().clear()
        userDatabase.progressDao().clear()
        userDatabase.prayerDao().deleteAllUserData()
        userDatabase.fastingDao().deleteAllUserData()
        userDatabase.zakatDao().deleteAllUserData()
        userDatabase.locationDao().deleteAllUserData()
        userDatabase.tasbihSessionDao().deleteAllSessions()
        userDatabase.tafseerUserDao().deleteAllUserData()
        userDatabase.readingProgressDao().clear()
        userDatabase.khatamDao().deleteAllUserData()
        userDatabase.customPresetDao().clear()
    }
}
