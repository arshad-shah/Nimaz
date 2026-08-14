package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.data.local.database.dao.QuranDao
import com.arshadshah.nimaz.data.local.database.dao.TafseerDao
import com.arshadshah.nimaz.data.local.database.entity.TafseerBlockEntity
import com.arshadshah.nimaz.data.local.user.TafseerUserDao
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class TafseerRepositoryImplTest {

    private lateinit var tafseerDao: TafseerDao
    private lateinit var tafseerUserDao: TafseerUserDao
    private lateinit var quranDao: QuranDao
    private lateinit var repository: TafseerRepositoryImpl

    private fun makeBlockEntity(id: Long = 1L, surah: Int = 1, ayahStart: Int = 1, ayahEnd: Int = 1) =
        TafseerBlockEntity(
            id = id, tafseerId = "ibn_kathir_en",
            surahNumber = surah, ayahStart = ayahStart, ayahEnd = ayahEnd,
            text = "Commentary on ayah $ayahStart"
        )

    @Before
    fun setUp() {
        tafseerDao = mockk(relaxed = true)
        tafseerUserDao = mockk(relaxed = true)
        quranDao = mockk(relaxed = true)
        repository = TafseerRepositoryImpl(tafseerDao, tafseerUserDao, quranDao)
    }

    @Test
    fun `getTafseerForAyah returns mapped domain object`() = runTest {
        val entity = makeBlockEntity()
        coEvery { tafseerDao.getTafseerForAyah(1, 1, "ibn_kathir_en") } returns entity

        val result = repository.getTafseerForAyah(1, 1, "ibn_kathir_en")

        assertThat(result).isNotNull()
        assertThat(result!!.surahNumber).isEqualTo(1)
        assertThat(result.tafseerId).isEqualTo("ibn_kathir_en")
        assertThat(result.text).contains("Commentary")
    }

    @Test
    fun `getTafseerForAyah returns null when not found`() = runTest {
        coEvery { tafseerDao.getTafseerForAyah(any(), any(), any()) } returns null

        assertThat(repository.getTafseerForAyah(1, 1, "unknown_tafseer")).isNull()
    }

    @Test
    fun `getTafseerForSurah returns flow of mapped objects`() = runTest {
        val entities = listOf(
            makeBlockEntity(id = 1L, surah = 1, ayahStart = 1, ayahEnd = 1),
            makeBlockEntity(id = 2L, surah = 1, ayahStart = 2, ayahEnd = 2)
        )
        every { tafseerDao.getTafseerForSurah(1, "ibn_kathir_en") } returns flowOf(entities)

        val result = repository.getTafseerForSurah(1, "ibn_kathir_en").first()

        assertThat(result).hasSize(2)
        assertThat(result[0].ayahStart).isEqualTo(1)
        assertThat(result[1].ayahStart).isEqualTo(2)
    }

    @Test
    fun `getTafseerForSurah returns empty when no tafseer`() = runTest {
        every { tafseerDao.getTafseerForSurah(any(), any()) } returns flowOf(emptyList())

        val result = repository.getTafseerForSurah(1, "ibn_kathir_en").first()

        assertThat(result).isEmpty()
    }

    @Test
    fun `block entity maps correctly to domain TafseerText`() = runTest {
        val entity = makeBlockEntity(id = 5L, surah = 2, ayahStart = 255, ayahEnd = 257)
        coEvery { tafseerDao.getTafseerForAyah(2, 255, "ibn_kathir_en") } returns entity

        val result = repository.getTafseerForAyah(2, 255, "ibn_kathir_en")

        assertThat(result!!.id).isEqualTo(5L)
        assertThat(result.surahNumber).isEqualTo(2)
        assertThat(result.ayahStart).isEqualTo(255)
        assertThat(result.ayahEnd).isEqualTo(257)
    }
}
