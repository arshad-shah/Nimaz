package com.arshadshah.nimaz.data.local.user

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.data.local.database.entity.TafseerHighlightEntity
import com.arshadshah.nimaz.data.local.database.entity.TafseerNoteEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * A reader's highlights and notes on the commentary.
 *
 * The range reads are the point. They used to `INNER JOIN ayahs` to turn a surah and a span into
 * ids, and cannot now that the verses live in a different database file — so the ids arrive as a
 * parameter and the `IN (:ayahIds)` clause is the only thing scoping the read. That clause is
 * also what keeps one tafseer's marks out of another's.
 */
@RunWith(RobolectricTestRunner::class)
class TafseerUserDaoTest {

    private lateinit var db: NimazUserDatabase
    private lateinit var dao: TafseerUserDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NimazUserDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.tafseerUserDao()
    }

    @After
    fun tearDown() = db.close()

    // ---- Highlights ----

    @Test
    fun `an ayah's highlights come back in reading order`() = runTest {
        dao.insertHighlights(
            listOf(
                highlight(ayahId = 1, startOffset = 40, endOffset = 50),
                highlight(ayahId = 1, startOffset = 10, endOffset = 20),
                highlight(ayahId = 1, startOffset = 25, endOffset = 30),
            )
        )

        assertThat(dao.getHighlightsForAyah(ayahId = 1, tafseerId = TAFSEER).first()
            .map { it.startOffset })
            .containsExactly(10, 25, 40).inOrder()
    }

    @Test
    fun `one tafseer's highlights are not another's`() = runTest {
        dao.insertHighlight(highlight(ayahId = 1, tafseerId = TAFSEER))
        dao.insertHighlight(highlight(ayahId = 1, tafseerId = "maarif"))

        assertThat(dao.getHighlightsForAyah(ayahId = 1, tafseerId = TAFSEER).first()).hasSize(1)
        assertThat(dao.getAllHighlights().first()).hasSize(2)
    }

    @Test
    fun `a block's highlights span every ayah in it, and stop there`() = runTest {
        dao.insertHighlights(
            listOf(
                highlight(ayahId = 1, startOffset = 30),
                highlight(ayahId = 2, startOffset = 10),
                highlight(ayahId = 3, startOffset = 0),
            )
        )

        assertThat(
            dao.getHighlightsForRange(TAFSEER, ayahIds = listOf(1, 2)).first()
                .map { it.ayahId to it.startOffset }
        ).containsExactly(2 to 10, 1 to 30).inOrder()
    }

    @Test
    fun `a block with no verses asks for nothing`() = runTest {
        dao.insertHighlight(highlight(ayahId = 1))

        assertThat(dao.getHighlightsForRange(TAFSEER, ayahIds = emptyList()).first()).isEmpty()
    }

    @Test
    fun `editing a highlight's colour keeps it a single mark`() = runTest {
        val id = dao.insertHighlight(highlight(ayahId = 1, colour = "yellow"))

        dao.updateHighlight(
            dao.getAllHighlightsSync().single().copy(color = "green", updatedAt = 500)
        )

        val rows = dao.getAllHighlightsSync()
        assertThat(rows).hasSize(1)
        assertThat(rows.single().id).isEqualTo(id)
        assertThat(rows.single().color).isEqualTo("green")
    }

    @Test
    fun `a highlight can be removed by id or by row`() = runTest {
        val first = dao.insertHighlight(highlight(ayahId = 1, startOffset = 0))
        dao.insertHighlight(highlight(ayahId = 2, startOffset = 0))

        dao.deleteHighlightById(first)
        assertThat(dao.getAllHighlightsSync().map { it.ayahId }).containsExactly(2)

        dao.deleteHighlight(dao.getAllHighlightsSync().single())
        assertThat(dao.getAllHighlightsSync()).isEmpty()
    }

    // ---- Notes ----

    @Test
    fun `an ayah's notes come back newest first`() = runTest {
        dao.insertNotes(
            listOf(
                note(ayahId = 1, text = "older", createdAt = 100),
                note(ayahId = 1, text = "newer", createdAt = 300),
            )
        )

        assertThat(dao.getNotesForAyah(ayahId = 1, tafseerId = TAFSEER).first().map { it.text })
            .containsExactly("newer", "older").inOrder()
    }

    @Test
    fun `a block's notes are ordered by the verse they sit on`() = runTest {
        dao.insertNotes(
            listOf(
                note(ayahId = 3, text = "third", createdAt = 100),
                note(ayahId = 1, text = "first", createdAt = 300),
                note(ayahId = 2, text = "second", createdAt = 200),
            )
        )

        assertThat(dao.getNotesForRange(TAFSEER, ayahIds = listOf(1, 2, 3)).first().map { it.text })
            .containsExactly("first", "second", "third").inOrder()
    }

    @Test
    fun `editing a note replaces its text without adding a second note`() = runTest {
        dao.insertNote(note(ayahId = 1, text = "draft"))

        dao.updateNote(dao.getAllNotesSync().single().copy(text = "final", updatedAt = 500))

        assertThat(dao.getAllNotesSync().map { it.text }).containsExactly("final")
    }

    @Test
    fun `a note can be removed by id or by row`() = runTest {
        val first = dao.insertNote(note(ayahId = 1, text = "one"))
        dao.insertNote(note(ayahId = 2, text = "two"))

        dao.deleteNoteById(first)
        assertThat(dao.getAllNotesSync().map { it.text }).containsExactly("two")

        dao.deleteNote(dao.getAllNotesSync().single())
        assertThat(dao.getAllNotesSync()).isEmpty()
    }

    // ---- Clearing ----

    @Test
    fun `deleting all user data clears highlights and notes together`() = runTest {
        dao.insertHighlight(highlight(ayahId = 1))
        dao.insertNote(note(ayahId = 1, text = "one"))

        dao.deleteAllUserData()

        assertThat(dao.getAllHighlightsSync()).isEmpty()
        assertThat(dao.getAllNotesSync()).isEmpty()
    }

    @Test
    fun `clearing highlights leaves the notes standing`() = runTest {
        dao.insertHighlight(highlight(ayahId = 1))
        dao.insertNote(note(ayahId = 1, text = "one"))

        dao.deleteAllHighlights()

        assertThat(dao.getAllHighlightsSync()).isEmpty()
        assertThat(dao.getAllNotesSync()).hasSize(1)
    }

    private fun highlight(
        ayahId: Int,
        tafseerId: String = TAFSEER,
        startOffset: Int = 0,
        endOffset: Int = startOffset + 5,
        colour: String = "yellow",
        createdAt: Long = 0,
    ) = TafseerHighlightEntity(
        id = 0,
        ayahId = ayahId,
        tafseerId = tafseerId,
        startOffset = startOffset,
        endOffset = endOffset,
        color = colour,
        note = null,
        createdAt = createdAt,
        updatedAt = createdAt,
    )

    private fun note(
        ayahId: Int,
        text: String,
        tafseerId: String = TAFSEER,
        createdAt: Long = 0,
    ) = TafseerNoteEntity(
        id = 0,
        ayahId = ayahId,
        tafseerId = tafseerId,
        text = text,
        createdAt = createdAt,
        updatedAt = createdAt,
    )

    private companion object {
        const val TAFSEER = "ibn-kathir"
    }
}
