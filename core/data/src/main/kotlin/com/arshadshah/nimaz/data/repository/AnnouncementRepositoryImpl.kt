package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.core.datastore.AnnouncementLocalDataSource
import com.arshadshah.nimaz.domain.model.Announcement
import com.arshadshah.nimaz.domain.repository.AnnouncementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnnouncementRepositoryImpl @Inject constructor(
    private val localDataSource: AnnouncementLocalDataSource,
) : AnnouncementRepository {

    override fun observeCurrentAnnouncement(): Flow<Announcement?> =
        combine(
            localDataSource.currentAnnouncement,
            localDataSource.dismissedIds,
        ) { announcement, dismissedIds ->
            announcement?.takeIf { it.id !in dismissedIds }
        }

    override suspend fun setAnnouncement(announcement: Announcement) {
        localDataSource.setCurrentAnnouncement(announcement)
    }

    override suspend fun dismiss(id: String) {
        localDataSource.dismiss(id)
    }
}
