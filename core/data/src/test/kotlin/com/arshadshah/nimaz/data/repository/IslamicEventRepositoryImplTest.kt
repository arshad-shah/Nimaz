package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.data.local.database.dao.IslamicEventDao
import com.arshadshah.nimaz.data.local.database.entity.IslamicEventEntity
import com.arshadshah.nimaz.domain.model.IslamicEventType
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class IslamicEventRepositoryImplTest {

    private lateinit var dao: IslamicEventDao
    private lateinit var repository: IslamicEventRepositoryImpl

    private fun makeEntity(
        id: Int = 1,
        nameEnglish: String = "Eid al-Fitr",
        eventType: String = "holiday",
        hijriMonth: Int = 10,
        hijriDay: Int = 1,
        isHoliday: Int = 1
    ) = IslamicEventEntity(
        id = id, nameEnglish = nameEnglish, nameArabic = "عيد",
        hijriMonth = hijriMonth, hijriDay = hijriDay,
        eventType = eventType, description = "Celebration",
        isHoliday = isHoliday
    )

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        repository = IslamicEventRepositoryImpl(dao)
    }

    @Test
    fun `getAllEvents returns mapped domain events`() = runTest {
        every { dao.getAllEvents() } returns flowOf(
            listOf(makeEntity(1), makeEntity(2, "Eid al-Adha", hijriMonth = 12, hijriDay = 10))
        )

        val result = repository.getAllEvents().first()

        assertThat(result).hasSize(2)
        assertThat(result[0].nameEnglish).isEqualTo("Eid al-Fitr")
        assertThat(result[1].hijriMonth).isEqualTo(12)
    }

    @Test
    fun `getAllEvents returns empty when no events`() = runTest {
        every { dao.getAllEvents() } returns flowOf(emptyList())

        val result = repository.getAllEvents().first()

        assertThat(result).isEmpty()
    }

    @Test
    fun `entity with holiday eventType maps to HOLIDAY domain type`() = runTest {
        every { dao.getAllEvents() } returns flowOf(listOf(makeEntity(eventType = "holiday")))

        val result = repository.getAllEvents().first()

        assertThat(result[0].eventType).isEqualTo(IslamicEventType.HOLIDAY)
    }

    @Test
    fun `entity with fast eventType maps correctly`() = runTest {
        every { dao.getAllEvents() } returns flowOf(
            listOf(makeEntity(eventType = "fast", isHoliday = 0))
        )

        val result = repository.getAllEvents().first()

        assertThat(result[0].isFastingDay).isTrue()
        assertThat(result[0].isHoliday).isFalse()
    }

    @Test
    fun `entity with isHoliday 1 maps to isHoliday true`() = runTest {
        every { dao.getAllEvents() } returns flowOf(listOf(makeEntity(isHoliday = 1)))

        val result = repository.getAllEvents().first()

        assertThat(result[0].isHoliday).isTrue()
    }

    @Test
    fun `entity with isHoliday 0 maps to isHoliday false`() = runTest {
        every { dao.getAllEvents() } returns flowOf(listOf(makeEntity(isHoliday = 0)))

        val result = repository.getAllEvents().first()

        assertThat(result[0].isHoliday).isFalse()
    }

    @Test
    fun `entity id maps to String id in domain`() = runTest {
        every { dao.getAllEvents() } returns flowOf(listOf(makeEntity(id = 42)))

        val result = repository.getAllEvents().first()

        assertThat(result[0].id).isEqualTo("42")
    }

    @Test
    fun `hijri month and day are preserved in domain`() = runTest {
        every { dao.getAllEvents() } returns flowOf(
            listOf(makeEntity(hijriMonth = 9, hijriDay = 27))
        )

        val result = repository.getAllEvents().first()

        assertThat(result[0].hijriMonth).isEqualTo(9)
        assertThat(result[0].hijriDay).isEqualTo(27)
    }
}
