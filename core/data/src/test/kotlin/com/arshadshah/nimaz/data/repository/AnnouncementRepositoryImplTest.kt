package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.core.datastore.AnnouncementLocalDataSource
import com.arshadshah.nimaz.domain.model.Announcement
import com.arshadshah.nimaz.domain.model.AnnouncementType
import com.google.common.truth.Truth.assertThat
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AnnouncementRepositoryImplTest {

    private val localDataSource = mockk<AnnouncementLocalDataSource>(relaxed = true)
    private val repository = AnnouncementRepositoryImpl(localDataSource)

    private fun makeAnnouncement(id: String = "a1") = Announcement(
        id = id,
        type = AnnouncementType.FEATURE,
        title = "Title",
        body = "Body",
    )

    @Test
    fun `observeCurrentAnnouncement emits announcement when not dismissed`() = runTest {
        val announcement = makeAnnouncement("a1")
        every { localDataSource.currentAnnouncement } returns flowOf(announcement)
        every { localDataSource.dismissedIds } returns flowOf(emptySet())

        val result = repository.observeCurrentAnnouncement().first()
        assertThat(result).isEqualTo(announcement)
    }

    @Test
    fun `observeCurrentAnnouncement emits null when announcement is dismissed`() = runTest {
        val announcement = makeAnnouncement("a1")
        every { localDataSource.currentAnnouncement } returns flowOf(announcement)
        every { localDataSource.dismissedIds } returns flowOf(setOf("a1"))

        val result = repository.observeCurrentAnnouncement().first()
        assertThat(result).isNull()
    }

    @Test
    fun `observeCurrentAnnouncement emits null when no announcement`() = runTest {
        every { localDataSource.currentAnnouncement } returns flowOf(null)
        every { localDataSource.dismissedIds } returns flowOf(emptySet())

        val result = repository.observeCurrentAnnouncement().first()
        assertThat(result).isNull()
    }

    @Test
    fun `observeCurrentAnnouncement shows announcement whose id is not in the dismissed set`() = runTest {
        val announcement = makeAnnouncement("a2")
        every { localDataSource.currentAnnouncement } returns flowOf(announcement)
        every { localDataSource.dismissedIds } returns flowOf(setOf("a1", "a3"))

        val result = repository.observeCurrentAnnouncement().first()
        assertThat(result).isEqualTo(announcement)
    }

    @Test
    fun `setAnnouncement delegates to data source`() = runTest {
        val announcement = makeAnnouncement("a1")
        repository.setAnnouncement(announcement)
        coVerify { localDataSource.setCurrentAnnouncement(announcement) }
    }

    @Test
    fun `dismiss delegates to data source`() = runTest {
        repository.dismiss("a1")
        coVerify { localDataSource.dismiss("a1") }
    }
}
