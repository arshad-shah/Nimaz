package com.arshadshah.nimaz.data.local.user

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.data.local.database.entity.TasbihSessionEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Counting sessions, and the statistics read off them.
 *
 * Every total here is `currentCount + (totalLaps * targetCount)` — a session that went round the
 * loop three times and stopped at seven is 307 of a 100-count preset, not 7 and not 300. That
 * expression is repeated across four queries, so it is asserted rather than assumed. The two
 * `SUM`s also answer null on an empty range, which the caller has to handle.
 */
@RunWith(RobolectricTestRunner::class)
class TasbihSessionDaoTest {

    private lateinit var db: NimazUserDatabase
    private lateinit var dao: TasbihSessionDao

    private val day1 = 1_000L
    private val day2 = 2_000L
    private val day3 = 3_000L

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NimazUserDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.tasbihSessionDao()
    }

    @After
    fun tearDown() = db.close()

    // ---- Resuming ----

    @Test
    fun `the active session is the most recent unfinished one`() = runTest {
        dao.insertSession(session(date = day1, startedAt = 100, isCompleted = false))
        val newer = dao.insertSession(session(date = day1, startedAt = 300, isCompleted = false))
        dao.insertSession(session(date = day1, startedAt = 400, isCompleted = true))

        assertThat(dao.getActiveSession()?.id).isEqualTo(newer)
    }

    @Test
    fun `there is no active session once everything is finished`() = runTest {
        val id = dao.insertSession(session(date = day1, startedAt = 100, isCompleted = false))

        dao.completeSession(id, completedAt = 500, duration = 60_000)

        assertThat(dao.getActiveSession()).isNull()
        val done = dao.getSessionById(id)
        assertThat(done?.isCompleted).isTrue()
        assertThat(done?.completedAt).isEqualTo(500)
        assertThat(done?.duration).isEqualTo(60_000)
    }

    @Test
    fun `updating the count leaves the rest of the session alone`() = runTest {
        val id = dao.insertSession(
            session(date = day1, startedAt = 100, presetName = "Subhanallah", note = "after fajr")
        )

        dao.updateSessionCount(id, count = 42, laps = 2)

        val row = dao.getSessionById(id)
        assertThat(row?.currentCount).isEqualTo(42)
        assertThat(row?.totalLaps).isEqualTo(2)
        assertThat(row?.presetName).isEqualTo("Subhanallah")
        assertThat(row?.note).isEqualTo("after fajr")
    }

    // ---- Listing ----

    @Test
    fun `a day's sessions come back newest first`() = runTest {
        dao.insertSession(session(date = day1, startedAt = 100))
        dao.insertSession(session(date = day1, startedAt = 300))
        dao.insertSession(session(date = day2, startedAt = 200))

        assertThat(dao.getSessionsForDate(day1).first().map { it.startedAt })
            .containsExactly(300L, 100L).inOrder()
    }

    @Test
    fun `a range includes both its ends`() = runTest {
        dao.insertSession(session(date = day1, startedAt = 100))
        dao.insertSession(session(date = day2, startedAt = 200))
        dao.insertSession(session(date = day3, startedAt = 300))

        assertThat(dao.getSessionsInRange(day1, day2).first().map { it.date })
            .containsExactly(day2, day1).inOrder()
    }

    @Test
    fun `sessions are found by the preset they used`() = runTest {
        dao.insertSession(session(date = day1, startedAt = 100, presetId = 5))
        dao.insertSession(session(date = day1, startedAt = 200, presetId = 6))
        dao.insertSession(session(date = day1, startedAt = 300, presetId = null))

        assertThat(dao.getSessionsForPreset(5).first()).hasSize(1)
        assertThat(dao.getSessionsCountForPreset(5)).isEqualTo(1)
        assertThat(dao.getSessionsCountForPreset(7)).isEqualTo(0)
    }

    // ---- Statistics ----

    @Test
    fun `a session's total counts every completed lap as well as the current count`() = runTest {
        dao.insertSession(
            session(date = day1, startedAt = 100, currentCount = 7, targetCount = 100, laps = 3)
        )

        assertThat(dao.getTotalCountInRange(day1, day1)).isEqualTo(307)
    }

    @Test
    fun `totals are null before anything is counted in the range`() = runTest {
        dao.insertSession(session(date = day3, startedAt = 100, currentCount = 10))

        assertThat(dao.getTotalCountInRange(day1, day2)).isNull()
        assertThat(dao.getTotalDurationInRange(day1, day2)).isNull()
    }

    @Test
    fun `time spent sums across the range`() = runTest {
        dao.insertSession(session(date = day1, startedAt = 100, duration = 30_000))
        dao.insertSession(session(date = day2, startedAt = 200, duration = 45_000))
        dao.insertSession(session(date = day3, startedAt = 300, duration = 99_000))

        assertThat(dao.getTotalDurationInRange(day1, day2)).isEqualTo(75_000)
    }

    @Test
    fun `only finished sessions count towards the completed total`() = runTest {
        dao.insertSession(session(date = day1, startedAt = 100, isCompleted = true))
        dao.insertSession(session(date = day1, startedAt = 200, isCompleted = false))
        dao.insertSession(session(date = day3, startedAt = 300, isCompleted = true))

        assertThat(dao.getCompletedSessionsInRange(day1, day2)).isEqualTo(1)
        assertThat(dao.getCompletedSessionsInRange(day1, day3)).isEqualTo(2)
    }

    @Test
    fun `the most used presets are ranked by total count, not by session count`() = runTest {
        // Five short sessions on one preset, one long session on another.
        repeat(5) {
            dao.insertSession(
                session(date = day1, startedAt = 100L + it, presetId = 5, currentCount = 10)
            )
        }
        dao.insertSession(
            session(
                date = day1,
                startedAt = 200,
                presetId = 6,
                currentCount = 0,
                targetCount = 100,
                laps = 2,
            )
        )
        dao.insertSession(session(date = day1, startedAt = 300, presetId = null, currentCount = 999))

        assertThat(dao.getMostUsedPresets(limit = 5).map { it.presetId to it.totalCount })
            .containsExactly(6L to 200, 5L to 50).inOrder()
        // The free counter has no preset, so it is not a preset anyone used.
        assertThat(dao.getMostUsedPresets(limit = 5).map { it.presetId }).doesNotContain(null)
    }

    @Test
    fun `the ranked list carries how many sessions made each total up`() = runTest {
        repeat(3) {
            dao.insertSession(
                session(date = day1, startedAt = 100L + it, presetId = 5, currentCount = 10)
            )
        }
        dao.insertSession(session(date = day1, startedAt = 200, presetId = 6, currentCount = 5))

        assertThat(
            dao.getMostUsedPresetsWithSessions(limit = 1)
                .map { Triple(it.presetId, it.totalCount, it.sessionsCount) }
        ).containsExactly(Triple(5L, 30, 3))
    }

    @Test
    fun `deleting removes the session named and nothing else`() = runTest {
        val id = dao.insertSession(session(date = day1, startedAt = 100))
        dao.insertSession(session(date = day1, startedAt = 200))

        dao.deleteSession(dao.getSessionById(id)!!)

        assertThat(dao.getAllSessionsSync().map { it.startedAt }).containsExactly(200L)
    }

    @Test
    fun `deleting all sessions empties the table and the statistics with it`() = runTest {
        dao.insertSession(session(date = day1, startedAt = 100, currentCount = 33))

        dao.deleteAllSessions()

        assertThat(dao.getAllSessionsSync()).isEmpty()
        assertThat(dao.getTotalCountInRange(day1, day3)).isNull()
    }

    private fun session(
        date: Long,
        startedAt: Long,
        presetId: Long? = null,
        presetName: String? = null,
        currentCount: Int = 0,
        targetCount: Int = 33,
        laps: Int = 0,
        isCompleted: Boolean = false,
        duration: Long? = null,
        note: String? = null,
    ) = TasbihSessionEntity(
        id = 0,
        presetId = presetId,
        presetName = presetName,
        date = date,
        currentCount = currentCount,
        targetCount = targetCount,
        totalLaps = laps,
        isCompleted = isCompleted,
        duration = duration,
        startedAt = startedAt,
        completedAt = null,
        note = note,
        updatedAt = startedAt,
    )
}
