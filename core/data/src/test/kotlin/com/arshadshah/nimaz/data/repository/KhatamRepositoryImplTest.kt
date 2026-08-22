package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.data.local.database.dao.KhatamDao
import com.arshadshah.nimaz.data.local.database.dao.QuranDao
import com.arshadshah.nimaz.data.local.database.entity.KhatamEntity
import com.arshadshah.nimaz.domain.model.Khatam
import com.arshadshah.nimaz.domain.model.KhatamStatus
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class KhatamRepositoryImplTest {

    private lateinit var khatamDao: KhatamDao
    private lateinit var quranDao: QuranDao
    private lateinit var repository: KhatamRepositoryImpl

    private val now = System.currentTimeMillis()

    private fun makeEntity(id: Long = 1L, status: String = "active") = KhatamEntity(
        id = id, name = "Test Khatam", notes = null, status = status,
        isActive = status == "active", dailyTarget = 20, deadline = null,
        reminderEnabled = false, reminderTime = null, totalAyahsRead = 0,
        createdAt = now, startedAt = now, completedAt = null, updatedAt = now
    )

    private fun makeKhatam(id: Long = 1L, status: KhatamStatus = KhatamStatus.ACTIVE) = Khatam(
        id = id, name = "Test Khatam", notes = null, status = status,
        isActive = status == KhatamStatus.ACTIVE, dailyTarget = 20, deadline = null,
        reminderEnabled = false, reminderTime = null, totalAyahsRead = 0,
        createdAt = now, startedAt = now, completedAt = null, updatedAt = now
    )

    @Before
    fun setUp() {
        khatamDao = mockk(relaxed = true)
        quranDao = mockk(relaxed = true)
        repository = KhatamRepositoryImpl(khatamDao, quranDao)
    }

    @Test
    fun `createKhatam delegates to dao and returns id`() = runTest {
        coEvery { khatamDao.insertKhatam(any()) } returns 5L

        val id = repository.createKhatam(makeKhatam())

        assertThat(id).isEqualTo(5L)
        coVerify { khatamDao.insertKhatam(any()) }
    }

    @Test
    fun `deleteKhatam delegates to dao`() = runTest {
        repository.deleteKhatam(1L)
        coVerify { khatamDao.deleteKhatam(1L) }
    }

    @Test
    fun `getKhatamById maps entity to domain`() = runTest {
        coEvery { khatamDao.getKhatamById(1L) } returns makeEntity(id = 1L)

        val result = repository.getKhatamById(1L)

        assertThat(result).isNotNull()
        assertThat(result!!.id).isEqualTo(1L)
        assertThat(result.name).isEqualTo("Test Khatam")
        assertThat(result.status).isEqualTo(KhatamStatus.ACTIVE)
    }

    @Test
    fun `getKhatamById returns null when not found`() = runTest {
        coEvery { khatamDao.getKhatamById(any()) } returns null

        assertThat(repository.getKhatamById(999L)).isNull()
    }

    @Test
    fun `observeActiveKhatam emits domain khatam`() = runTest {
        every { khatamDao.observeActiveKhatam() } returns flowOf(makeEntity(id = 1L))

        val result = repository.observeActiveKhatam().first()

        assertThat(result).isNotNull()
        assertThat(result!!.isActive).isTrue()
    }

    @Test
    fun `observeActiveKhatam emits null when none active`() = runTest {
        every { khatamDao.observeActiveKhatam() } returns flowOf(null)

        val result = repository.observeActiveKhatam().first()

        assertThat(result).isNull()
    }

    @Test
    fun `setActiveKhatam delegates to dao`() = runTest {
        repository.setActiveKhatam(2L)
        coVerify { khatamDao.setActiveKhatam(2L) }
    }

    @Test
    fun `completeKhatam delegates to dao`() = runTest {
        repository.completeKhatam(1L)
        coVerify { khatamDao.completeKhatam(eq(1L), any()) }
    }

    @Test
    fun `observeInProgressKhatams emits list`() = runTest {
        every { khatamDao.observeInProgressKhatams() } returns
            flowOf(listOf(makeEntity(1L), makeEntity(2L)))

        val result = repository.observeInProgressKhatams().first()

        assertThat(result).hasSize(2)
    }

    @Test
    fun `khatam entity with completed status maps to COMPLETED domain status`() = runTest {
        coEvery { khatamDao.getKhatamById(1L) } returns makeEntity(status = "completed")

        val result = repository.getKhatamById(1L)

        assertThat(result!!.status).isEqualTo(KhatamStatus.COMPLETED)
    }
}
