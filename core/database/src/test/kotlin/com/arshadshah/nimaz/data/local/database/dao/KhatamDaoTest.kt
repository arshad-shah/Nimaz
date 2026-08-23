package com.arshadshah.nimaz.data.local.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.data.local.database.entity.KhatamAyahEntity
import com.arshadshah.nimaz.data.local.database.entity.KhatamDailyLogEntity
import com.arshadshah.nimaz.data.local.database.entity.KhatamEntity
import com.arshadshah.nimaz.data.local.user.NimazUserDatabase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * What a khatam's tables hold after the taps that make one.
 *
 * The three transactions here — [KhatamDao.setActiveKhatam], [KhatamDao.markAyahsRead] and
 * [KhatamDao.deleteAllUserData] — are each two or three statements that only mean anything
 * together, and every one of their failure modes is a *wrong row count* rather than an error:
 * two khatams flagged active, a total that drifts from the ayahs actually marked, a delete
 * that leaves orphans behind. None of that is visible from a ViewModel, so it is asserted
 * against a real database.
 */
@RunWith(RobolectricTestRunner::class)
class KhatamDaoTest {

    private lateinit var db: NimazUserDatabase
    private lateinit var dao: KhatamDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NimazUserDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.khatamDao()
    }

    @After
    fun tearDown() = db.close()

    // ---- Exactly one active khatam ----

    @Test
    fun `activating a khatam deactivates the one before it`() = runTest {
        val first = dao.insertKhatam(khatam("First"))
        val second = dao.insertKhatam(khatam("Second"))

        dao.setActiveKhatam(first)
        dao.setActiveKhatam(second)

        assertThat(dao.getAllKhatamsSync().filter { it.isActive }.map { it.name })
            .containsExactly("Second")
    }

    @Test
    fun `the active khatam is the one just activated`() = runTest {
        dao.insertKhatam(khatam("First"))
        val second = dao.insertKhatam(khatam("Second"))

        dao.setActiveKhatam(second)

        assertThat(dao.getActiveKhatam()?.name).isEqualTo("Second")
        assertThat(dao.observeActiveKhatam().first()?.name).isEqualTo("Second")
    }

    @Test
    fun `activating stamps a start time once and keeps it across re-activations`() = runTest {
        val id = dao.insertKhatam(khatam("Ramadan"))

        dao.activateKhatam(id, timestamp = 1_000)
        val firstStart = dao.getKhatamById(id)?.startedAt

        dao.deactivateAllKhatams(timestamp = 2_000)
        dao.activateKhatam(id, timestamp = 3_000)

        // COALESCE(started_at, :timestamp) — putting a khatam down and picking it back up must
        // not reset when it began, or every "days so far" reads zero.
        assertThat(firstStart).isEqualTo(1_000)
        assertThat(dao.getKhatamById(id)?.startedAt).isEqualTo(1_000)
        assertThat(dao.getKhatamById(id)?.updatedAt).isEqualTo(3_000)
    }

    @Test
    fun `no khatam is active before one is chosen`() = runTest {
        dao.insertKhatam(khatam("First"))

        assertThat(dao.getActiveKhatam()).isNull()
    }

    // ---- Marking ayahs read ----

    @Test
    fun `marking ayahs read stores them and updates the khatam's total`() = runTest {
        val id = dao.insertKhatam(khatam("First"))

        dao.markAyahsRead(id, listOf(1, 2, 3))

        assertThat(dao.getReadAyahIds(id)).containsExactly(1, 2, 3)
        assertThat(dao.getKhatamById(id)?.totalAyahsRead).isEqualTo(3)
    }

    @Test
    fun `marking the same ayah twice does not count it twice`() = runTest {
        val id = dao.insertKhatam(khatam("First"))

        dao.markAyahsRead(id, listOf(1, 2, 3))
        dao.markAyahsRead(id, listOf(2, 3, 4))

        // `insertAyahs` is `onConflict = IGNORE` on the (khatam, ayah) key, and the total is
        // re-counted rather than added to — a scroll back over verses already read must not
        // inflate progress.
        assertThat(dao.getReadAyahIds(id)).containsExactly(1, 2, 3, 4)
        assertThat(dao.getKhatamById(id)?.totalAyahsRead).isEqualTo(4)
    }

    @Test
    fun `one khatam's progress is not another's`() = runTest {
        val first = dao.insertKhatam(khatam("First"))
        val second = dao.insertKhatam(khatam("Second"))

        dao.markAyahsRead(first, listOf(1, 2, 3))
        dao.markAyahsRead(second, listOf(1))

        assertThat(dao.getReadAyahCount(first)).isEqualTo(3)
        assertThat(dao.getReadAyahCount(second)).isEqualTo(1)
        assertThat(dao.getKhatamById(second)?.totalAyahsRead).isEqualTo(1)
    }

    @Test
    fun `marking an empty surah is a no-op rather than a zeroed total`() = runTest {
        val id = dao.insertKhatam(khatam("First"))
        dao.markAyahsRead(id, listOf(1, 2, 3))

        dao.markSurahAsRead(id, emptyList())

        assertThat(dao.getKhatamById(id)?.totalAyahsRead).isEqualTo(3)
    }

    @Test
    fun `unmarking an ayah leaves the total stale until it is recalculated`() = runTest {
        val id = dao.insertKhatam(khatam("First"))
        dao.markAyahsRead(id, listOf(1, 2, 3))

        dao.unmarkAyahRead(id, ayahId = 2)

        // The delete alone does not touch `khatams` — this is why every caller that unmarks
        // has to follow it with the recalculation below.
        assertThat(dao.getReadAyahIds(id)).containsExactly(1, 3)
        assertThat(dao.getKhatamById(id)?.totalAyahsRead).isEqualTo(3)

        dao.recalculateTotalAyahsRead(id, timestamp = 9_000)

        assertThat(dao.getKhatamById(id)?.totalAyahsRead).isEqualTo(2)
        assertThat(dao.getKhatamById(id)?.updatedAt).isEqualTo(9_000)
    }

    @Test
    fun `read ayahs are reported within the juz span asked for`() = runTest {
        val id = dao.insertKhatam(khatam("First"))
        dao.markAyahsRead(id, listOf(1, 50, 148, 149, 300))

        assertThat(dao.getReadAyahIdsInRange(id, startAyahId = 1, endAyahId = 148))
            .containsExactly(1, 50, 148)
    }

    @Test
    fun `observing read ayahs emits the marks made after subscribing`() = runTest {
        val id = dao.insertKhatam(khatam("First"))

        assertThat(dao.observeReadAyahCount(id).first()).isEqualTo(0)

        dao.markAyahsRead(id, listOf(1, 2))

        assertThat(dao.observeReadAyahCount(id).first()).isEqualTo(2)
        assertThat(dao.observeReadAyahIds(id).first()).containsExactly(1, 2)
    }

    // ---- Status transitions ----

    @Test
    fun `completing a khatam stamps it, files it as completed and stands it down`() = runTest {
        val id = dao.insertKhatam(khatam("First"))
        dao.setActiveKhatam(id)

        dao.completeKhatam(id, timestamp = 5_000)

        val completed = dao.getKhatamById(id)
        assertThat(completed?.status).isEqualTo("completed")
        assertThat(completed?.completedAt).isEqualTo(5_000)
        // A finished khatam that stays flagged active keeps the reader pinned to it.
        assertThat(completed?.isActive).isFalse()
        assertThat(dao.observeCompletedKhatams().first().map { it.name }).containsExactly("First")
        assertThat(dao.observeInProgressKhatams().first()).isEmpty()
    }

    @Test
    fun `abandoning a khatam stands it down too, and reactivating files it back`() = runTest {
        val id = dao.insertKhatam(khatam("First"))
        dao.setActiveKhatam(id)

        dao.abandonKhatam(id, timestamp = 5_000)
        assertThat(dao.getKhatamById(id)?.isActive).isFalse()
        assertThat(dao.observeAbandonedKhatams().first().map { it.name }).containsExactly("First")

        dao.reactivateKhatam(id, timestamp = 6_000)
        assertThat(dao.observeInProgressKhatams().first().map { it.name }).containsExactly("First")
        assertThat(dao.observeAbandonedKhatams().first()).isEmpty()
    }

    @Test
    fun `in-progress and completed khatams are listed most recent first`() = runTest {
        dao.insertKhatam(khatam("Older", updatedAt = 1_000))
        dao.insertKhatam(khatam("Newer", updatedAt = 2_000))

        assertThat(dao.observeInProgressKhatams().first().map { it.name })
            .containsExactly("Newer", "Older").inOrder()
        assertThat(dao.observeAllKhatams().first().map { it.name })
            .containsExactly("Newer", "Older").inOrder()
    }

    @Test
    fun `deleting a khatam does not disturb the others`() = runTest {
        val first = dao.insertKhatam(khatam("First"))
        dao.insertKhatam(khatam("Second"))
        dao.markAyahsRead(first, listOf(1, 2))

        dao.deleteKhatam(first)

        assertThat(dao.getAllKhatamsSync().map { it.name }).containsExactly("Second")
        // khatam_ayahs cascades from khatams.
        assertThat(dao.getKhatamAyahsSync(first)).isEmpty()
    }

    // ---- Lifetime counters ----

    @Test
    fun `lifetime ayah count sums every khatam, and answers zero on an empty table`() = runTest {
        assertThat(dao.observeTotalAyahsReadAllTime().first()).isEqualTo(0)

        val first = dao.insertKhatam(khatam("First"))
        val second = dao.insertKhatam(khatam("Second"))
        dao.markAyahsRead(first, listOf(1, 2, 3))
        dao.markAyahsRead(second, listOf(1, 2))
        dao.completeKhatam(first, timestamp = 5_000)

        // COALESCE guards the empty table; the sum spans a completed khatam as well as a live one.
        assertThat(dao.observeTotalAyahsReadAllTime().first()).isEqualTo(5)
        assertThat(dao.observeCompletedKhatamCount().first()).isEqualTo(1)
        assertThat(dao.observeActiveKhatamCount().first()).isEqualTo(1)
    }

    @Test
    fun `read stamps come back oldest first, across all khatams`() = runTest {
        val first = dao.insertKhatam(khatam("First"))
        val second = dao.insertKhatam(khatam("Second"))
        dao.insertAyahs(listOf(ayah(first, 1, readAt = 3_000), ayah(first, 2, readAt = 1_000)))
        dao.insertAyahs(listOf(ayah(second, 1, readAt = 2_000)))

        assertThat(dao.observeReadTimestamps(first).first())
            .containsExactly(1_000L, 3_000L).inOrder()
        assertThat(dao.observeAllReadTimestamps().first())
            .containsExactly(1_000L, 2_000L, 3_000L).inOrder()
    }

    // ---- Daily log ----

    @Test
    fun `a second log for the same day replaces the first`() = runTest {
        val id = dao.insertKhatam(khatam("First"))

        dao.upsertDailyLog(dailyLog(id, date = 100, ayahsRead = 5))
        dao.upsertDailyLog(dailyLog(id, date = 100, ayahsRead = 12))

        assertThat(dao.getDailyLog(id, date = 100)?.ayahsRead).isEqualTo(12)
        assertThat(dao.getDailyLogsSync(id)).hasSize(1)
    }

    @Test
    fun `daily logs come back newest first, and empty days are left out of the lifetime list`() =
        runTest {
            val id = dao.insertKhatam(khatam("First"))
            dao.upsertDailyLog(dailyLog(id, date = 100, ayahsRead = 5))
            dao.upsertDailyLog(dailyLog(id, date = 200, ayahsRead = 0))
            dao.upsertDailyLog(dailyLog(id, date = 300, ayahsRead = 7))

            assertThat(dao.observeDailyLogs(id).first().map { it.date })
                .containsExactly(300L, 200L, 100L).inOrder()
            // A day logged with nothing read is not a day of the streak.
            assertThat(dao.observeAllDailyLogs().first().map { it.date })
                .containsExactly(300L, 100L).inOrder()
        }

    // ---- Delete all ----

    @Test
    fun `deleting all user data empties every khatam table`() = runTest {
        val id = dao.insertKhatam(khatam("First"))
        dao.markAyahsRead(id, listOf(1, 2, 3))
        dao.upsertDailyLog(dailyLog(id, date = 100, ayahsRead = 3))

        dao.deleteAllUserData()

        assertThat(dao.getAllKhatamsSync()).isEmpty()
        assertThat(dao.getKhatamAyahsSync(id)).isEmpty()
        assertThat(dao.getDailyLogsSync(id)).isEmpty()
    }

    private fun khatam(
        name: String,
        updatedAt: Long = 0,
    ) = KhatamEntity(
        id = 0,
        name = name,
        createdAt = 0,
        updatedAt = updatedAt,
    )

    private fun ayah(khatamId: Long, ayahId: Int, readAt: Long) = KhatamAyahEntity(
        khatamId = khatamId,
        ayahId = ayahId,
        readAt = readAt,
        updatedAt = readAt,
    )

    private fun dailyLog(khatamId: Long, date: Long, ayahsRead: Int) = KhatamDailyLogEntity(
        khatamId = khatamId,
        date = date,
        ayahsRead = ayahsRead,
    )
}
