package com.arshadshah.nimaz.data.sync

import com.arshadshah.nimaz.data.local.database.dao.KhatamDao
import com.arshadshah.nimaz.data.local.user.TafseerUserDao
import com.arshadshah.nimaz.data.local.database.dao.ZakatDao
import com.arshadshah.nimaz.data.local.database.entity.KhatamAyahEntity
import com.arshadshah.nimaz.data.local.database.entity.KhatamEntity
import com.arshadshah.nimaz.data.local.database.entity.TafseerHighlightEntity
import com.arshadshah.nimaz.data.local.database.entity.ZakatHistoryEntity
import com.arshadshah.nimaz.data.local.user.BookmarkDao
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Sync must not merge two devices' records by their **local row id**.
 *
 * Seven importers keyed their merge on `it.id`, which is Room's `autoGenerate` primary key.
 * Two phones that have both been used allocate 1, 2, 3… independently, so a collision is not
 * an edge case — it is the ordinary case. The consequences were silent and destructive:
 *
 * - Your khatam "Ramadan 2026" (local id 1) is **overwritten** by the other device's unrelated
 *   khatam that also happens to be id 1, if theirs is newer. If theirs is older it is dropped
 *   instead, so nothing arrives at all.
 * - The same for custom dhikr, tasbih sessions, tafseer highlights and notes, makeup fasts, and
 *   zakat history — a financial record replaced by a different one.
 * - Khatam ayahs and daily logs are written with the *sender's* `khatamId` verbatim, so another
 *   device's read-ayah records attach to whichever local khatam holds that id, inflating its
 *   progress.
 *
 * Every one of these tables has a natural key — when the fast was missed, when the session
 * started, which span of which tafseer was highlighted, when the zakat was calculated. Matching
 * on that, and letting Room assign a fresh local id to anything genuinely new (remapping child
 * rows onto it), is what these tests pin.
 */
class SyncDataImporterIdentityTest {

    private lateinit var khatamDao: KhatamDao
    private lateinit var tafseerUserDao: TafseerUserDao
    private lateinit var zakatDao: ZakatDao
    private lateinit var importer: SyncDataImporter

    private val khatams = mutableListOf<KhatamEntity>()
    private val khatamAyahs = mutableListOf<KhatamAyahEntity>()
    private val highlights = mutableListOf<TafseerHighlightEntity>()
    private val zakatRows = mutableListOf<ZakatHistoryEntity>()
    private var nextKhatamId = 100L

    @Before
    fun setUp() {
        khatamDao = mockk(relaxed = true)
        coEvery { khatamDao.getAllKhatamsSync() } answers { khatams.toList() }
        coEvery { khatamDao.insertKhatam(any()) } answers {
            val id = nextKhatamId++
            khatams += firstArg<KhatamEntity>().copy(id = id)
            id
        }
        coEvery { khatamDao.updateKhatam(any()) } answers {
            val row = firstArg<KhatamEntity>()
            khatams.removeAll { it.id == row.id }
            khatams += row
        }
        coEvery { khatamDao.insertAyahs(any()) } answers {
            khatamAyahs += firstArg<List<KhatamAyahEntity>>()
        }

        tafseerUserDao = mockk(relaxed = true)
        coEvery { tafseerUserDao.getAllHighlightsSync() } answers { highlights.toList() }
        coEvery { tafseerUserDao.insertHighlights(any()) } answers {
            firstArg<List<TafseerHighlightEntity>>().forEach { row ->
                if (row.id != 0L) highlights.removeAll { it.id == row.id }
                highlights += row
            }
        }

        zakatDao = mockk(relaxed = true)
        coEvery { zakatDao.getAllHistorySync() } answers { zakatRows.toList() }
        coEvery { zakatDao.insertCalculations(any()) } answers {
            // REPLACE semantics: a row carrying an existing id overwrites that row.
            firstArg<List<ZakatHistoryEntity>>().forEach { row ->
                if (row.id != 0L) zakatRows.removeAll { it.id == row.id }
                zakatRows += row
            }
        }

        importer = importerWith(
            bookmarkDao = mockk<BookmarkDao>(relaxed = true).also {
                coEvery { it.all() } returns emptyList()
            },
            khatamDao = khatamDao,
            tafseerUserDao = tafseerUserDao,
            zakatDao = zakatDao
        )
    }

    @Test
    fun `an unrelated khatam sharing a local id is not overwritten`() = runTest {
        khatams += khatam(id = 1L, name = "Ramadan 2026", createdAt = 1_000L, updatedAt = 1_000L)

        importer.importKhatamData(
            SyncPayload(
                khatams = listOf(
                    syncKhatam(id = 1L, name = "Daily juz", createdAt = 5_000L, updatedAt = 9_000L)
                )
            )
        )

        assertThat(khatams.map { it.name }).containsExactly("Ramadan 2026", "Daily juz")
    }

    @Test
    fun `the same khatam arriving again updates in place rather than duplicating`() = runTest {
        khatams += khatam(id = 1L, name = "Ramadan 2026", createdAt = 1_000L, updatedAt = 1_000L)

        importer.importKhatamData(
            SyncPayload(
                khatams = listOf(
                    // Same khatam (same createdAt), edited on the other device.
                    syncKhatam(id = 7L, name = "Ramadan 2026 ✧", createdAt = 1_000L, updatedAt = 9_000L)
                )
            )
        )

        assertThat(khatams).hasSize(1)
        assertThat(khatams.single().name).isEqualTo("Ramadan 2026 ✧")
        assertThat(khatams.single().id).isEqualTo(1L)
    }

    @Test
    fun `read ayahs follow their khatam onto its local id`() = runTest {
        khatams += khatam(id = 1L, name = "Ramadan 2026", createdAt = 1_000L, updatedAt = 1_000L)

        importer.importKhatamData(
            SyncPayload(
                khatams = listOf(
                    syncKhatam(id = 1L, name = "Daily juz", createdAt = 5_000L, updatedAt = 9_000L)
                ),
                khatamAyahs = listOf(SyncKhatamAyah(khatamId = 1L, ayahId = 42, readAt = 0L, updatedAt = 0L))
            )
        )

        // The incoming khatam was inserted under a fresh local id; its ayah must point there,
        // not at the local "Ramadan 2026" that happened to hold id 1.
        val inserted = khatams.single { it.name == "Daily juz" }
        assertThat(khatamAyahs.single().khatamId).isEqualTo(inserted.id)
        assertThat(khatamAyahs.single().khatamId).isNotEqualTo(1L)
    }

    @Test
    fun `an unrelated tafseer highlight sharing a local id is not overwritten`() = runTest {
        highlights += highlight(id = 1L, ayahId = 262, start = 0, end = 10, note = "mine")

        importer.importTafseerData(
            SyncPayload(
                tafseerHighlights = listOf(
                    syncHighlight(id = 1L, ayahId = 999, start = 50, end = 60, note = "theirs")
                )
            )
        )

        assertThat(highlights.map { it.note }).containsExactly("mine", "theirs")
    }

    @Test
    fun `the same tafseer span arriving again updates in place`() = runTest {
        highlights += highlight(id = 1L, ayahId = 262, start = 0, end = 10, note = "old")

        importer.importTafseerData(
            SyncPayload(
                tafseerHighlights = listOf(
                    syncHighlight(id = 9L, ayahId = 262, start = 0, end = 10, note = "new", updatedAt = 9_000L)
                )
            )
        )

        assertThat(highlights).hasSize(1)
        assertThat(highlights.single().note).isEqualTo("new")
    }

    @Test
    fun `an unrelated zakat calculation sharing a local id is not overwritten`() = runTest {
        zakatRows += zakat(id = 1L, calculatedAt = 1_000L, zakatDue = 250.0)

        importer.importZakatData(
            SyncPayload(
                zakatHistory = listOf(syncZakat(id = 1L, calculatedAt = 5_000L, zakatDue = 800.0))
            )
        )

        assertThat(zakatRows.map { it.zakatDue }).containsExactly(250.0, 800.0)
    }

    // --- builders ---

    private fun khatam(id: Long, name: String, createdAt: Long, updatedAt: Long) = KhatamEntity(
        id = id, name = name, notes = null, status = "active", isActive = false,
        dailyTarget = 20, deadline = null, reminderEnabled = false, reminderTime = null,
        totalAyahsRead = 0, createdAt = createdAt, startedAt = null, completedAt = null,
        updatedAt = updatedAt
    )

    private fun syncKhatam(id: Long, name: String, createdAt: Long, updatedAt: Long) = SyncKhatam(
        id = id, name = name, notes = null, status = "active", isActive = false,
        dailyTarget = 20, deadline = null, reminderEnabled = false, reminderTime = null,
        totalAyahsRead = 0, createdAt = createdAt, startedAt = null, completedAt = null,
        updatedAt = updatedAt
    )

    private fun highlight(id: Long, ayahId: Int, start: Int, end: Int, note: String?) =
        TafseerHighlightEntity(
            id = id, ayahId = ayahId, tafseerId = "ibn_kathir_en", startOffset = start,
            endOffset = end, color = "yellow", note = note, createdAt = 1_000L, updatedAt = 1_000L
        )

    private fun syncHighlight(
        id: Long, ayahId: Int, start: Int, end: Int, note: String?, updatedAt: Long = 1_000L
    ) = SyncTafseerHighlight(
        id = id, ayahId = ayahId, tafseerId = "ibn_kathir_en", startOffset = start,
        endOffset = end, color = "yellow", note = note, createdAt = 1_000L, updatedAt = updatedAt
    )

    private fun zakat(id: Long, calculatedAt: Long, zakatDue: Double) = ZakatHistoryEntity(
        id = id, calculatedAt = calculatedAt, totalAssets = 0.0, totalLiabilities = 0.0,
        netWorth = 0.0, zakatDue = zakatDue, nisabType = "SILVER", nisabValue = 0.0,
        isPaid = false, paidAt = null, notes = null, updatedAt = calculatedAt
    )

    private fun syncZakat(id: Long, calculatedAt: Long, zakatDue: Double) = SyncZakatHistory(
        id = id, calculatedAt = calculatedAt, totalAssets = 0.0, totalLiabilities = 0.0,
        netWorth = 0.0, zakatDue = zakatDue, nisabType = "SILVER", nisabValue = 0.0,
        isPaid = false, paidAt = null, notes = null, updatedAt = calculatedAt
    )
}
