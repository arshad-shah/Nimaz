package com.arshadshah.nimaz.domain.repository

import com.arshadshah.nimaz.domain.model.Announcement
import kotlinx.coroutines.flow.Flow

interface AnnouncementRepository {

    /**
     * The most recently received announcement, or null when there is none or
     * it has been dismissed. Expiry/version gating is applied by
     * `ObserveActiveAnnouncementUseCase`, not here.
     */
    fun observeCurrentAnnouncement(): Flow<Announcement?>

    /** Store [announcement] as the current one (from FCM receipt or a notification tap). */
    suspend fun setAnnouncement(announcement: Announcement)

    /** Permanently dismiss the announcement with [id] — it never resurfaces. */
    suspend fun dismiss(id: String)
}
