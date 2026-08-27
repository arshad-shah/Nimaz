package com.arshadshah.nimaz.domain.repository

/**
 * Everything the person using the app has made — bookmarks, progress, prayer and fasting
 * records, zakat calculations, saved locations, tasbih sessions, tafseer notes, khatams and
 * custom presets.
 *
 * Deliberately **not** the shipped content corpus. The Qur'an, hadith, duas and the presets
 * we ship are ours to replace and never theirs to lose, so "delete all my data" must not be
 * able to reach them. Keeping the distinction in one place is the point of this interface:
 * the caller cannot forget a table, and cannot accidentally aim at the content database.
 */
interface UserDataRepository {
    /** Clears every table holding user-created data. Does not touch shipped content. */
    suspend fun clearAllUserData()
}
