package com.arshadshah.nimaz.data.repository

import app.cash.turbine.test
import com.arshadshah.nimaz.data.local.database.dao.FastingDao
import com.arshadshah.nimaz.data.local.database.entity.FastRecordEntity
import com.arshadshah.nimaz.data.local.database.entity.MakeupFastEntity
import com.arshadshah.nimaz.domain.model.ExemptionReason
import com.arshadshah.nimaz.domain.model.FastRecord
import com.arshadshah.nimaz.domain.model.FastStatus
import com.arshadshah.nimaz.domain.model.FastType
import com.arshadshah.nimaz.domain.model.MakeupFast
import com.arshadshah.nimaz.domain.model.MakeupFastStatus
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class FastingRepositoryImplTest {

    private lateinit var dao: FastingDao
    private lateinit var repository: FastingRepositoryImpl

    private val now = System.currentTimeMillis()
    private val oneDayMs = 24 * 60 * 60 * 1000L

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        repository = FastingRepositoryImpl(dao)
    }

    // ── Helper factories ────────────────────────────────────────────

    private fun createEntity(
        id: Long = 1,
        date: Long = now,
        fastType: String = "ramadan",
        status: String = "fasted",
        exemptionReason: String? = null
    ) = FastRecordEntity(
        id = id, date = date, hijriDate = "1/9/1446",
        hijriMonth = 9, hijriYear = 1446,
        fastType = fastType, status = status,
        exemptionReason = exemptionReason,
        suhoorTime = null, iftarTime = null, note = null,
        createdAt = now, updatedAt = now
    )

    private fun createDomainRecord(
        id: Long = 1,
        date: Long = now,
        fastType: FastType = FastType.RAMADAN,
        status: FastStatus = FastStatus.FASTED
    ) = FastRecord(
        id = id, date = date, hijriDate = "1/9/1446",
        hijriMonth = 9, hijriYear = 1446,
        fastType = fastType, status = status,
        exemptionReason = null, suhoorTime = null, iftarTime = null,
        note = null, createdAt = now, updatedAt = now
    )

    private fun createMakeupEntity(
        id: Long = 1,
        status: String = "pending"
    ) = MakeupFastEntity(
        id = id, originalDate = now, originalHijriDate = "1/9/1446",
        reason = "Travel", status = status,
        completedDate = null, fidyaAmount = null, note = null,
        createdAt = now, updatedAt = now
    )

    // ── getFastRecordForDate ────────────────────────────────────────

    @Test
    fun `getFastRecordForDate returns mapped domain record`() = runTest {
        val entity = createEntity(fastType = "ramadan", status = "fasted")
        coEvery { dao.getFastRecordForDate(now) } returns entity

        val result = repository.getFastRecordForDate(now)

        assertThat(result).isNotNull()
        assertThat(result!!.fastType).isEqualTo(FastType.RAMADAN)
        assertThat(result.status).isEqualTo(FastStatus.FASTED)
        assertThat(result.id).isEqualTo(1)
    }

    @Test
    fun `getFastRecordForDate returns null when no record exists`() = runTest {
        coEvery { dao.getFastRecordForDate(now) } returns null

        val result = repository.getFastRecordForDate(now)
        assertThat(result).isNull()
    }

    // ── Entity to Domain mapping ────────────────────────────────────

    @Test
    fun `entity with exemption reason maps correctly`() = runTest {
        val entity = createEntity(
            status = "exempted",
            exemptionReason = "travel"
        )
        coEvery { dao.getFastRecordForDate(now) } returns entity

        val result = repository.getFastRecordForDate(now)!!
        assertThat(result.status).isEqualTo(FastStatus.EXEMPTED)
        assertThat(result.exemptionReason).isEqualTo(ExemptionReason.TRAVEL)
    }

    @Test
    fun `entity with alias enum values maps correctly`() = runTest {
        val entity = createEntity(fastType = "nafl", status = "notfasted")
        coEvery { dao.getFastRecordForDate(now) } returns entity

        val result = repository.getFastRecordForDate(now)!!
        assertThat(result.fastType).isEqualTo(FastType.VOLUNTARY)
        assertThat(result.status).isEqualTo(FastStatus.NOT_FASTED)
    }

    @Test
    fun `entity with null exemption reason maps to null`() = runTest {
        val entity = createEntity(exemptionReason = null)
        coEvery { dao.getFastRecordForDate(now) } returns entity

        val result = repository.getFastRecordForDate(now)!!
        assertThat(result.exemptionReason).isNull()
    }

    // ── getFastRecordsInRange (Flow) ────────────────────────────────

    @Test
    fun `getFastRecordsInRange emits mapped domain records`() = runTest {
        val entities = listOf(
            createEntity(id = 1, date = now, status = "fasted"),
            createEntity(id = 2, date = now + oneDayMs, status = "not_fasted")
        )
        every { dao.getFastRecordsInRange(any(), any()) } returns flowOf(entities)

        repository.getFastRecordsInRange(now, now + 2 * oneDayMs).test {
            val records = awaitItem()
            assertThat(records).hasSize(2)
            assertThat(records[0].status).isEqualTo(FastStatus.FASTED)
            assertThat(records[1].status).isEqualTo(FastStatus.NOT_FASTED)
            awaitComplete()
        }
    }

    @Test
    fun `getFastRecordsInRange emits empty list when no records`() = runTest {
        every { dao.getFastRecordsInRange(any(), any()) } returns flowOf(emptyList())

        repository.getFastRecordsInRange(now, now + oneDayMs).test {
            val records = awaitItem()
            assertThat(records).isEmpty()
            awaitComplete()
        }
    }

    // ── getFastRecordsByType (Flow) ─────────────────────────────────

    @Test
    fun `getFastRecordsByType passes lowercase type string to DAO`() = runTest {
        every { dao.getFastRecordsByType("ramadan") } returns flowOf(emptyList())

        repository.getFastRecordsByType(FastType.RAMADAN).test {
            awaitItem()
            awaitComplete()
        }

        // Verify the enum was converted to lowercase string
        coVerify { dao.getFastRecordsByType("ramadan") }
    }

    @Test
    fun `getFastRecordsByStatus passes lowercase status string to DAO`() = runTest {
        every { dao.getFastRecordsByStatus("fasted") } returns flowOf(emptyList())

        repository.getFastRecordsByStatus(FastStatus.FASTED).test {
            awaitItem()
            awaitComplete()
        }

        coVerify { dao.getFastRecordsByStatus("fasted") }
    }

    // ── Insert / Update / Delete ────────────────────────────────────

    @Test
    fun `insertFastRecord converts domain to entity and calls DAO`() = runTest {
        val record = createDomainRecord()
        repository.insertFastRecord(record)

        coVerify {
            dao.insertFastRecord(match { entity ->
                entity.fastType == "ramadan" &&
                entity.status == "fasted" &&
                entity.date == record.date
            })
        }
    }

    @Test
    fun `insertFastRecords converts list and calls DAO`() = runTest {
        val records = listOf(
            createDomainRecord(id = 1),
            createDomainRecord(id = 2, fastType = FastType.VOLUNTARY)
        )
        repository.insertFastRecords(records)

        coVerify {
            dao.insertFastRecords(match { entities ->
                entities.size == 2 &&
                entities[0].fastType == "ramadan" &&
                entities[1].fastType == "voluntary"
            })
        }
    }

    @Test
    fun `updateFastStatus passes lowercase status to DAO`() = runTest {
        repository.updateFastStatus(now, FastStatus.NOT_FASTED)

        coVerify { dao.updateFastStatus(now, "not_fasted", any()) }
    }

    @Test
    fun `deleteFastRecordByDate delegates to DAO`() = runTest {
        repository.deleteFastRecordByDate(now)
        coVerify { dao.deleteFastRecordByDate(now) }
    }

    // ── Domain to Entity mapping (roundtrip) ────────────────────────

    @Test
    fun `domain record with exemption reason converts to entity correctly`() = runTest {
        val record = FastRecord(
            id = 1, date = now, hijriDate = "1/9/1446",
            hijriMonth = 9, hijriYear = 1446,
            fastType = FastType.RAMADAN, status = FastStatus.EXEMPTED,
            exemptionReason = ExemptionReason.ILLNESS,
            suhoorTime = null, iftarTime = null, note = "felt sick",
            createdAt = now, updatedAt = now
        )
        repository.insertFastRecord(record)

        coVerify {
            dao.insertFastRecord(match { entity ->
                entity.exemptionReason == "illness" &&
                entity.status == "exempted" &&
                entity.note == "felt sick"
            })
        }
    }

    // ── Makeup fasts ────────────────────────────────────────────────

    @Test
    fun `getPendingMakeupFasts maps entities to domain`() = runTest {
        val entities = listOf(createMakeupEntity(id = 1, status = "pending"))
        every { dao.getPendingMakeupFasts() } returns flowOf(entities)

        repository.getPendingMakeupFasts().test {
            val fasts = awaitItem()
            assertThat(fasts).hasSize(1)
            assertThat(fasts[0].status).isEqualTo(MakeupFastStatus.PENDING)
            awaitComplete()
        }
    }

    @Test
    fun `getMakeupFastById returns mapped domain or null`() = runTest {
        coEvery { dao.getMakeupFastById(1) } returns createMakeupEntity()
        coEvery { dao.getMakeupFastById(999) } returns null

        val found = repository.getMakeupFastById(1)
        assertThat(found).isNotNull()
        assertThat(found!!.status).isEqualTo(MakeupFastStatus.PENDING)

        val notFound = repository.getMakeupFastById(999)
        assertThat(notFound).isNull()
    }

    @Test
    fun `insertMakeupFast converts domain to entity`() = runTest {
        val makeup = MakeupFast(
            id = 0, originalDate = now, originalHijriDate = "1/9/1446",
            reason = "Travel", status = MakeupFastStatus.PENDING,
            completedDate = null, fidyaAmount = null, note = null,
            createdAt = now, updatedAt = now
        )
        repository.insertMakeupFast(makeup)

        coVerify {
            dao.insertMakeupFast(match { entity ->
                entity.status == "pending" && entity.reason == "Travel"
            })
        }
    }

    @Test
    fun `markMakeupFastCompleted delegates to DAO`() = runTest {
        repository.markMakeupFastCompleted(1, now)
        coVerify { dao.markMakeupFastCompleted(1, now, any()) }
    }

    @Test
    fun `markFidyaPaid delegates to DAO`() = runTest {
        repository.markFidyaPaid(1, 50.0)
        coVerify { dao.markFidyaPaid(1, 50.0, any()) }
    }

    // ── Statistics ──────────────────────────────────────────────────

    @Test
    fun `getFastingStats aggregates DAO results`() = runTest {
        coEvery { dao.getFastedCountInRange(any(), any()) } returns 20
        coEvery { dao.getRamadanFastedCount() } returns 15
        coEvery { dao.getVoluntaryFastCount() } returns 5
        coEvery { dao.getTotalFidyaPaid() } returns 100.0
        coEvery { dao.getRecentFastedRecords(any()) } returns emptyList()

        val stats = repository.getFastingStats(now, now + 30 * oneDayMs)

        assertThat(stats.totalFasted).isEqualTo(20)
        assertThat(stats.ramadanFasted).isEqualTo(15)
        assertThat(stats.voluntaryFasted).isEqualTo(5)
        assertThat(stats.totalFidyaPaid).isEqualTo(100.0)
    }

    @Test
    fun `getTotalFidyaPaid returns 0 when DAO returns null`() = runTest {
        coEvery { dao.getTotalFidyaPaid() } returns null

        val result = repository.getTotalFidyaPaid()
        assertThat(result).isEqualTo(0.0)
    }

    @Test
    fun `getRamadanFastedCount delegates to DAO`() = runTest {
        coEvery { dao.getRamadanFastedCount() } returns 28

        val result = repository.getRamadanFastedCount()
        assertThat(result).isEqualTo(28)
    }

    // ── Streak calculation ──────────────────────────────────────────

    @Test
    fun `streak returns 0 when no fasted records`() = runTest {
        coEvery { dao.getRecentFastedRecords(any()) } returns emptyList()
        coEvery { dao.getFastedCountInRange(any(), any()) } returns 0
        coEvery { dao.getRamadanFastedCount() } returns 0
        coEvery { dao.getVoluntaryFastCount() } returns 0
        coEvery { dao.getTotalFidyaPaid() } returns null

        val stats = repository.getFastingStats(now, now + oneDayMs)
        assertThat(stats.currentStreak).isEqualTo(0)
    }

    @Test
    fun `streak counts consecutive days from today`() = runTest {
        val todayStart = (now / oneDayMs) * oneDayMs
        val entities = listOf(
            createEntity(id = 1, date = todayStart),                  // today
            createEntity(id = 2, date = todayStart - oneDayMs),       // yesterday
            createEntity(id = 3, date = todayStart - 2 * oneDayMs)   // 2 days ago
        )
        coEvery { dao.getRecentFastedRecords(any()) } returns entities
        coEvery { dao.getFastedCountInRange(any(), any()) } returns 3
        coEvery { dao.getRamadanFastedCount() } returns 0
        coEvery { dao.getVoluntaryFastCount() } returns 3
        coEvery { dao.getTotalFidyaPaid() } returns null

        val stats = repository.getFastingStats(now, now + oneDayMs)
        assertThat(stats.currentStreak).isEqualTo(3)
    }

    @Test
    fun `streak breaks on gap day`() = runTest {
        val todayStart = (now / oneDayMs) * oneDayMs
        val entities = listOf(
            createEntity(id = 1, date = todayStart),                  // today
            // yesterday is missing (gap)
            createEntity(id = 2, date = todayStart - 2 * oneDayMs)   // 2 days ago
        )
        coEvery { dao.getRecentFastedRecords(any()) } returns entities
        coEvery { dao.getFastedCountInRange(any(), any()) } returns 2
        coEvery { dao.getRamadanFastedCount() } returns 0
        coEvery { dao.getVoluntaryFastCount() } returns 2
        coEvery { dao.getTotalFidyaPaid() } returns null

        val stats = repository.getFastingStats(now, now + oneDayMs)
        assertThat(stats.currentStreak).isEqualTo(1) // Only today
    }

    // ── getMakeupFastCountForDate ───────────────────────────────────

    @Test
    fun `getMakeupFastCountForDate delegates to DAO`() = runTest {
        coEvery { dao.getMakeupFastCountForDate(now) } returns 2

        val count = repository.getMakeupFastCountForDate(now)
        assertThat(count).isEqualTo(2)
    }
}
