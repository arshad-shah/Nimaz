package com.arshadshah.nimaz.data.local.user

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Counting a dua up and down, and the qaida progress that shares the table with it.
 *
 * [ProgressDao.increment] and [ProgressDao.decrement] are read-modify-write pairs, which is the
 * shape where a wrong row key looks like a working feature: count to three today, and if `date`
 * is dropped from the lookup tomorrow starts at three rather than at zero. Both also have to keep
 * the fields they did not come to change — the target a dua is counted towards, and the day it
 * was first counted on.
 */
@RunWith(RobolectricTestRunner::class)
class ProgressDaoTest {

    private lateinit var db: NimazUserDatabase
    private lateinit var dao: ProgressDao

    private val today = 1_000L
    private val tomorrow = 2_000L

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NimazUserDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.progressDao()
    }

    @After
    fun tearDown() = db.close()

    // ---- Counting up ----

    @Test
    fun `the first count creates the row`() = runTest {
        dao.increment(ProgressKind.DUA, targetId = 7, date = today, target = 3)

        val row = dao.find(ProgressKind.DUA, targetId = 7, date = today)
        assertThat(row?.completed).isEqualTo(1)
        assertThat(row?.total).isEqualTo(3)
        assertThat(row?.isCompleted).isFalse()
    }

    @Test
    fun `counting to the target marks it done, and beyond it stays done`() = runTest {
        repeat(3) { dao.increment(ProgressKind.DUA, targetId = 7, date = today, target = 3) }

        assertThat(dao.find(ProgressKind.DUA, 7, today)?.completed).isEqualTo(3)
        assertThat(dao.find(ProgressKind.DUA, 7, today)?.isCompleted).isTrue()

        dao.increment(ProgressKind.DUA, targetId = 7, date = today, target = 3)

        assertThat(dao.find(ProgressKind.DUA, 7, today)?.completed).isEqualTo(4)
        assertThat(dao.find(ProgressKind.DUA, 7, today)?.isCompleted).isTrue()
    }

    @Test
    fun `a count with no target is never complete`() = runTest {
        repeat(5) { dao.increment(ProgressKind.DUA, targetId = 7, date = today, target = null) }

        val row = dao.find(ProgressKind.DUA, 7, today)
        assertThat(row?.completed).isEqualTo(5)
        assertThat(row?.total).isNull()
        assertThat(row?.isCompleted).isFalse()
    }

    @Test
    fun `a later count keeps the target the row already had`() = runTest {
        dao.increment(ProgressKind.DUA, targetId = 7, date = today, target = 3)

        // The caller does not always have the target to hand on every tap.
        dao.increment(ProgressKind.DUA, targetId = 7, date = today, target = null)

        val row = dao.find(ProgressKind.DUA, 7, today)
        assertThat(row?.total).isEqualTo(3)
        assertThat(row?.completed).isEqualTo(2)
    }

    @Test
    fun `today's count does not carry into tomorrow`() = runTest {
        repeat(3) { dao.increment(ProgressKind.DUA, targetId = 7, date = today, target = 3) }

        dao.increment(ProgressKind.DUA, targetId = 7, date = tomorrow, target = 3)

        assertThat(dao.find(ProgressKind.DUA, 7, today)?.completed).isEqualTo(3)
        assertThat(dao.find(ProgressKind.DUA, 7, tomorrow)?.completed).isEqualTo(1)
    }

    @Test
    fun `the same target under two kinds is counted separately`() = runTest {
        dao.increment(ProgressKind.DUA, targetId = 7, date = today, target = null)
        dao.increment(ProgressKind.QAIDA_CELL, targetId = 7, date = 0, target = null)
        dao.increment(ProgressKind.QAIDA_CELL, targetId = 7, date = 0, target = null)

        assertThat(dao.find(ProgressKind.DUA, 7, today)?.completed).isEqualTo(1)
        assertThat(dao.find(ProgressKind.QAIDA_CELL, 7, 0)?.completed).isEqualTo(2)
    }

    // ---- Counting down ----

    @Test
    fun `counting down never goes below zero and leaves the row in place`() = runTest {
        dao.increment(ProgressKind.DUA, targetId = 7, date = today, target = 3)

        dao.decrement(ProgressKind.DUA, targetId = 7, date = today)
        assertThat(dao.find(ProgressKind.DUA, 7, today)?.completed).isEqualTo(0)

        dao.decrement(ProgressKind.DUA, targetId = 7, date = today)

        // A row at zero is not the same as no row: the user chose this dua today.
        assertThat(dao.find(ProgressKind.DUA, 7, today)?.completed).isEqualTo(0)
        assertThat(dao.all()).hasSize(1)
    }

    @Test
    fun `counting down a row that does not exist does nothing`() = runTest {
        dao.decrement(ProgressKind.DUA, targetId = 7, date = today)

        assertThat(dao.all()).isEmpty()
    }

    @Test
    fun `counting back below the target un-marks it as done`() = runTest {
        repeat(3) { dao.increment(ProgressKind.DUA, targetId = 7, date = today, target = 3) }

        dao.decrement(ProgressKind.DUA, targetId = 7, date = today)

        assertThat(dao.find(ProgressKind.DUA, 7, today)?.isCompleted).isFalse()
    }

    @Test
    fun `counting down keeps the fields it did not come to change`() = runTest {
        dao.upsert(
            progress(
                kind = ProgressKind.QAIDA_LESSON,
                targetId = 4,
                completed = 2,
                total = 5,
                state = "in_progress",
                score = 3,
                resumeId = 12,
                contextId = 99,
                createdAt = 100,
            )
        )

        dao.decrement(ProgressKind.QAIDA_LESSON, targetId = 4, date = 0)

        val row = dao.find(ProgressKind.QAIDA_LESSON, 4, 0)
        assertThat(row?.completed).isEqualTo(1)
        assertThat(row?.state).isEqualTo("in_progress")
        assertThat(row?.score).isEqualTo(3)
        assertThat(row?.resumeId).isEqualTo(12)
        assertThat(row?.contextId).isEqualTo(99)
        assertThat(row?.createdAt).isEqualTo(100)
    }

    // ---- Reads ----

    @Test
    fun `a kind's rows are listed most recently touched first`() = runTest {
        dao.upsertAll(
            listOf(
                progress(targetId = 1, updatedAt = 100),
                progress(targetId = 2, updatedAt = 300),
                progress(targetId = 3, updatedAt = 200),
            )
        )

        assertThat(dao.ofKind(ProgressKind.DUA).first().map { it.targetId })
            .containsExactly(2, 3, 1).inOrder()
    }

    @Test
    fun `a day's rows are listed by target, and exclude the other days`() = runTest {
        dao.upsertAll(
            listOf(
                progress(targetId = 2, date = today),
                progress(targetId = 1, date = today),
                progress(targetId = 3, date = tomorrow),
            )
        )

        assertThat(dao.onDate(ProgressKind.DUA, date = today).first().map { it.targetId })
            .containsExactly(1, 2).inOrder()
    }

    @Test
    fun `rows in a lesson exclude the ones in another`() = runTest {
        dao.upsertAll(
            listOf(
                progress(kind = ProgressKind.QAIDA_CELL, targetId = 1, contextId = 1),
                progress(kind = ProgressKind.QAIDA_CELL, targetId = 2, contextId = 2),
            )
        )

        assertThat(
            dao.inContext(ProgressKind.QAIDA_CELL, contextId = 1).first().map { it.targetId }
        ).containsExactly(1)
    }

    @Test
    fun `completed counts are per kind`() = runTest {
        dao.upsertAll(
            listOf(
                progress(kind = ProgressKind.DUA, targetId = 1, isCompleted = true),
                progress(kind = ProgressKind.DUA, targetId = 2, isCompleted = false),
                progress(kind = ProgressKind.QAIDA_LESSON, targetId = 1, isCompleted = true),
            )
        )

        assertThat(dao.completedCount(ProgressKind.DUA).first()).isEqualTo(1)
        assertThat(dao.completedCount(ProgressKind.QAIDA_LESSON).first()).isEqualTo(1)
    }

    @Test
    fun `deleting a kind leaves the other kinds standing`() = runTest {
        dao.upsertAll(
            listOf(
                progress(kind = ProgressKind.DUA, targetId = 1),
                progress(kind = ProgressKind.QAIDA_LESSON, targetId = 1),
            )
        )

        dao.deleteKind(ProgressKind.DUA)

        assertThat(dao.all().map { it.kind }).containsExactly(ProgressKind.QAIDA_LESSON)
    }

    @Test
    fun `deleting one day's row leaves the other day's`() = runTest {
        dao.upsertAll(
            listOf(
                progress(targetId = 7, date = today),
                progress(targetId = 7, date = tomorrow),
            )
        )

        dao.delete(ProgressKind.DUA, targetId = 7, date = today)

        assertThat(dao.all().map { it.date }).containsExactly(tomorrow)
    }

    private fun progress(
        kind: String = ProgressKind.DUA,
        targetId: Int,
        date: Long = 0,
        contextId: Int? = null,
        completed: Int = 0,
        total: Int? = null,
        isCompleted: Boolean = false,
        state: String? = null,
        score: Int? = null,
        resumeId: Int? = null,
        createdAt: Long = 0,
        updatedAt: Long = 0,
    ) = ProgressEntity(
        kind = kind,
        targetId = targetId,
        date = date,
        contextId = contextId,
        completed = completed,
        total = total,
        isCompleted = isCompleted,
        state = state,
        score = score,
        resumeId = resumeId,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
