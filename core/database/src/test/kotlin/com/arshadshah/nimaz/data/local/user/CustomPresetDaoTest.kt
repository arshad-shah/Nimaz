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
 * The counting presets a person made, in the order they arranged them.
 *
 * `display_order` is not unique, so the `id ASC` tiebreak is what stops two presets a user gave
 * the same position swapping places between reads — a list that reorders itself on every open.
 */
@RunWith(RobolectricTestRunner::class)
class CustomPresetDaoTest {

    private lateinit var db: NimazUserDatabase
    private lateinit var dao: CustomPresetDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NimazUserDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.customPresetDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `presets are listed in the order the user arranged them`() = runTest {
        dao.upsert(preset(name = "Third", order = 3))
        dao.upsert(preset(name = "First", order = 1))
        dao.upsert(preset(name = "Second", order = 2))

        assertThat(dao.all().map { it.name })
            .containsExactly("First", "Second", "Third").inOrder()
        assertThat(dao.observe().first().map { it.name })
            .containsExactly("First", "Second", "Third").inOrder()
    }

    @Test
    fun `presets sharing a position keep a stable order`() = runTest {
        val first = dao.upsert(preset(name = "Alpha", order = 1))
        val second = dao.upsert(preset(name = "Beta", order = 1))

        assertThat(dao.all().map { it.id }).containsExactly(first, second).inOrder()
    }

    @Test
    fun `editing a preset updates it in place`() = runTest {
        val id = dao.upsert(preset(name = "Alpha", order = 1))

        dao.upsert(dao.find(id)!!.copy(name = "Alpha (edited)", targetCount = 100))

        assertThat(dao.all()).hasSize(1)
        assertThat(dao.find(id)?.name).isEqualTo("Alpha (edited)")
        assertThat(dao.find(id)?.targetCount).isEqualTo(100)
    }

    @Test
    fun `a preset that was never saved is not found`() = runTest {
        assertThat(dao.find(42)).isNull()
    }

    @Test
    fun `a preset can be removed by id or by row`() = runTest {
        val first = dao.upsert(preset(name = "Alpha", order = 1))
        dao.upsert(preset(name = "Beta", order = 2))

        dao.delete(first)
        assertThat(dao.all().map { it.name }).containsExactly("Beta")

        dao.delete(dao.all().single())
        assertThat(dao.all()).isEmpty()
    }

    @Test
    fun `restoring a backup writes every preset it carries`() = runTest {
        dao.upsertAll(
            listOf(
                preset(id = 1, name = "Alpha", order = 1),
                preset(id = 2, name = "Beta", order = 2),
            )
        )
        dao.upsertAll(listOf(preset(id = 1, name = "Alpha (edited)", order = 1)))

        assertThat(dao.all().map { it.name })
            .containsExactly("Alpha (edited)", "Beta").inOrder()
    }

    @Test
    fun `clearing removes every preset`() = runTest {
        dao.upsert(preset(name = "Alpha", order = 1))

        dao.clear()

        assertThat(dao.all()).isEmpty()
    }

    private fun preset(
        id: Long = 0,
        name: String,
        order: Int,
    ) = CustomTasbihPresetEntity(
        id = id,
        name = name,
        arabic = "سبحان الله",
        transliteration = "Subhanallah",
        translation = "Glory be to God",
        targetCount = 33,
        displayOrder = order,
        createdAt = 0,
        updatedAt = 0,
    )
}
