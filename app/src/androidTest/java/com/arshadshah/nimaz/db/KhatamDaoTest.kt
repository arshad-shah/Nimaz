package com.arshadshah.nimaz.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arshadshah.nimaz.support.NimazDbRule
import com.arshadshah.nimaz.support.TestData
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Khatam (Quran completion goal) tracking: creation, activation, ayah progress. */
@RunWith(AndroidJUnit4::class)
class KhatamDaoTest {

    @get:Rule
    val dbRule = NimazDbRule()
    private val dao get() = dbRule.db.khatamDao()

    @Test
    fun insertKhatam_isReadBackById() = runTest {
        val id = dao.insertKhatam(TestData.khatam(name = "My Khatam"))

        val khatam = dao.getKhatamById(id)
        assertThat(khatam).isNotNull()
        assertThat(khatam!!.name).isEqualTo("My Khatam")
    }

    @Test
    fun setActiveKhatam_makesExactlyOneActive() = runTest {
        val first = dao.insertKhatam(TestData.khatam(name = "First", isActive = false))
        val second = dao.insertKhatam(TestData.khatam(name = "Second", isActive = false))

        dao.setActiveKhatam(second)

        val active = dao.observeActiveKhatam().first()
        assertThat(active).isNotNull()
        assertThat(active!!.id).isEqualTo(second)
        assertThat(dao.getKhatamById(first)!!.isActive).isFalse()
    }

    @Test
    fun markAyahsRead_updatesReadCount() = runTest {
        val id = dao.insertKhatam(TestData.khatam())

        dao.markAyahsRead(id, listOf(1, 2, 3, 4, 5))

        assertThat(dao.getReadAyahCount(id)).isEqualTo(5)
        assertThat(dao.observeReadAyahIds(id).first()).containsExactly(1, 2, 3, 4, 5)
    }

    @Test
    fun unmarkAyah_decrementsReadSet() = runTest {
        val id = dao.insertKhatam(TestData.khatam())
        dao.markAyahsRead(id, listOf(1, 2, 3))

        dao.unmarkAyahRead(id, 2)

        assertThat(dao.observeReadAyahIds(id).first()).containsExactly(1, 3)
    }

    @Test
    fun completeKhatam_movesItToCompletedList() = runTest {
        val id = dao.insertKhatam(TestData.khatam(status = "active"))

        dao.completeKhatam(id)

        assertThat(dao.observeCompletedKhatams().first().map { it.id }).contains(id)
    }
}
