package com.arshadshah.nimaz.data.local.user

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.data.local.database.entity.ReadingProgressEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Where the reader was — one row, read on every app open.
 *
 * The whole table is `WHERE id = 1`, which only works while every write carries that id. A save
 * that let the default autogenerate would leave the reads answering an older row forever, and
 * nothing about that failure looks like an error.
 */
@RunWith(RobolectricTestRunner::class)
class ReadingProgressDaoTest {

    private lateinit var db: NimazUserDatabase
    private lateinit var dao: ReadingProgressDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NimazUserDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.readingProgressDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `there is no progress before anything is read`() = runTest {
        assertThat(dao.get()).isNull()
        assertThat(dao.observe().first()).isNull()
    }

    @Test
    fun `saving where the reader got to answers that place`() = runTest {
        dao.upsert(progress(surah = 2, ayah = 255, page = 42, juz = 3, totalAyahsRead = 300))

        val row = dao.get()
        assertThat(row?.lastReadSurah).isEqualTo(2)
        assertThat(row?.lastReadAyah).isEqualTo(255)
        assertThat(row?.lastReadPage).isEqualTo(42)
        assertThat(row?.lastReadJuz).isEqualTo(3)
        assertThat(row?.totalAyahsRead).isEqualTo(300)
    }

    @Test
    fun `reading on replaces the place rather than adding a second`() = runTest {
        dao.upsert(progress(surah = 2, ayah = 255, page = 42, juz = 3, totalAyahsRead = 300))
        dao.upsert(progress(surah = 3, ayah = 1, page = 50, juz = 3, totalAyahsRead = 320))

        assertThat(dao.get()?.lastReadSurah).isEqualTo(3)
        assertThat(dao.get()?.totalAyahsRead).isEqualTo(320)
        assertThat(dao.observe().first()?.lastReadAyah).isEqualTo(1)
    }

    @Test
    fun `clearing forgets where the reader was`() = runTest {
        dao.upsert(progress(surah = 2, ayah = 255, page = 42, juz = 3, totalAyahsRead = 300))

        dao.clear()

        assertThat(dao.get()).isNull()
    }

    private fun progress(
        surah: Int,
        ayah: Int,
        page: Int,
        juz: Int,
        totalAyahsRead: Int,
        khatmaCount: Int = 0,
    ) = ReadingProgressEntity(
        lastReadSurah = surah,
        lastReadAyah = ayah,
        lastReadPage = page,
        lastReadJuz = juz,
        totalAyahsRead = totalAyahsRead,
        currentKhatmaCount = khatmaCount,
        updatedAt = 0,
    )
}
