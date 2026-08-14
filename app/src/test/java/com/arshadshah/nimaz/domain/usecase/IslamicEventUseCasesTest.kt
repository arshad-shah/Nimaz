package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.IslamicEvent
import com.arshadshah.nimaz.domain.model.IslamicEventType
import com.arshadshah.nimaz.domain.repository.IslamicEventRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class IslamicEventUseCasesTest {

    private lateinit var repo: IslamicEventRepository
    private lateinit var useCases: IslamicEventUseCases

    private fun makeEvent(id: String, nameEnglish: String, month: Int, day: Int) = IslamicEvent(
        id = id, nameArabic = "حدث", nameEnglish = nameEnglish,
        description = null, hijriMonth = month, hijriDay = day,
        eventType = IslamicEventType.HOLIDAY, isHoliday = true,
        isFastingDay = false, isNightOfPower = false,
        gregorianDate = null, year = null, notes = null, priority = 0
    )

    @Before
    fun setUp() {
        repo = mockk(relaxed = true)
        useCases = IslamicEventUseCases(
            getAllEvents = GetAllIslamicEventsUseCase(repo)
        )
    }

    @Test
    fun `getAllEvents returns flow of events`() = runTest {
        val events = listOf(
            makeEvent("eid_al_fitr", "Eid al-Fitr", month = 10, day = 1),
            makeEvent("eid_al_adha", "Eid al-Adha", month = 12, day = 10)
        )
        every { repo.getAllEvents() } returns flowOf(events)

        val result = useCases.getAllEvents().first()

        assertThat(result).hasSize(2)
        assertThat(result[0].nameEnglish).isEqualTo("Eid al-Fitr")
        assertThat(result[1].hijriMonth).isEqualTo(12)
    }

    @Test
    fun `getAllEvents returns empty list when no events`() = runTest {
        every { repo.getAllEvents() } returns flowOf(emptyList())

        val result = useCases.getAllEvents().first()

        assertThat(result).isEmpty()
    }

    @Test
    fun `getAllEvents emits event with correct fields`() = runTest {
        val ramadanEvent = makeEvent("ramadan_start", "Start of Ramadan", month = 9, day = 1)
            .copy(isFastingDay = true, eventType = IslamicEventType.FAST)
        every { repo.getAllEvents() } returns flowOf(listOf(ramadanEvent))

        val result = useCases.getAllEvents().first()

        assertThat(result[0].isFastingDay).isTrue()
        assertThat(result[0].eventType).isEqualTo(IslamicEventType.FAST)
    }
}
