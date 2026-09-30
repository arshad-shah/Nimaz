package com.arshadshah.nimaz.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.arshadshah.nimaz.data.local.database.dao.QuranDao
import com.arshadshah.nimaz.data.local.user.NimazUserDatabase
import com.arshadshah.nimaz.domain.model.TafseerHighlight
import com.arshadshah.nimaz.domain.model.TafseerNote
import com.arshadshah.nimaz.domain.model.toNoteItem
import com.arshadshah.nimaz.domain.usecase.TafseerNoteActions
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Exercise Delete/Undo against Room, including the flow already observed by the reader. */
@RunWith(RobolectricTestRunner::class)
class TafseerHighlightDeletionTest {
    private lateinit var db: NimazUserDatabase
    private lateinit var repository: TafseerRepositoryImpl
    private lateinit var actions: TafseerNoteActions
    private val highlight = TafseerHighlight(7, 1, "ibn_kathir_en", 2, 8, "#FDE68A", "My note", 100, 200)
    private val reflection = TafseerNote(7, 1, "ibn_kathir_en", "Separate reflection", 300, 400)

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), NimazUserDatabase::class.java,
        ).allowMainThreadQueries().build()
        val quranDao = mockk<QuranDao>()
        coEvery { quranDao.getAyahIdsInRange(1, 1, 1) } returns listOf(1)
        repository = TafseerRepositoryImpl(mockk(), db.tafseerUserDao(), quranDao)
        actions = TafseerNoteActions(repository)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `delete removes highlight from live reader and storage and undo restores exact record`() = runTest {
        repository.restoreHighlight(highlight)
        val other = highlight.copy(id = 8, startOffset = 12, endOffset = 18, note = null)
        repository.restoreHighlight(other)
        repository.restoreNote(reflection)
        val item = highlight.toNoteItem(1, 1, "01234567890123456789")

        repository.getHighlightsForRange(1, 1, 1, highlight.tafseerId).test {
            assertThat(awaitItem()).containsExactly(highlight, other).inOrder()
            actions.delete(item)
            assertThat(awaitItem()).containsExactly(other)
            assertThat(repository.getAllHighlights().first()).containsExactly(other)
            assertThat(repository.getAllNotes().first()).containsExactly(reflection)

            actions.restore(item)
            assertThat(awaitItem()).containsExactly(highlight, other).inOrder()
            assertThat(repository.getAllHighlights().first()).containsExactly(highlight, other)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleting standalone reflection leaves same-id highlight alone and undo restores reflection`() = runTest {
        repository.restoreHighlight(highlight)
        repository.restoreNote(reflection)
        val item = reflection.toNoteItem(1, 1)

        actions.delete(item)
        assertThat(repository.getAllNotes().first()).isEmpty()
        assertThat(repository.getAllHighlights().first()).containsExactly(highlight)
        actions.restore(item)
        assertThat(repository.getAllNotes().first()).containsExactly(reflection)
        assertThat(repository.getAllHighlights().first()).containsExactly(highlight)
    }
}
