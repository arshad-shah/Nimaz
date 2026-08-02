package com.arshadshah.nimaz.data.sync

import com.arshadshah.nimaz.data.local.user.BookmarkDao
import com.arshadshah.nimaz.data.local.user.BookmarkEntity
import com.arshadshah.nimaz.data.local.user.BookmarkKind
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Bookmarking an ayah and favouriting it are two independent acts sharing one row.
 *
 * `bookmarks` stores one row per (kind, target) carrying **two** independent flags,
 * `bookmarked` and `favourite`, plus a single `updated_at`. The importer merged with
 * last-writer-wins on that one timestamp — so whichever of the two acts happened *earlier* lost,
 * even though nothing about it conflicted:
 *
 * - Favourite an ayah on Monday, bookmark it on Tuesday, sync to a second device.
 *   `importBookmarks` writes the row stamped Tuesday; `importFavorites` then sees
 *   `local.updatedAt` (Tuesday) newer than the favourite (Monday) and skips. The favourite is
 *   gone.
 * - The mirror image needs no ordering at all: a device holding a favourite stamped Tuesday
 *   receives a bookmark stamped Monday, and `importBookmarks` drops it.
 *
 * There are no tombstones in the payload — it carries what the sending device *has* — so the
 * merge is additive. A flag that is set on either side stays set; the timestamp only decides
 * whose note and colour win.
 */
class SyncDataImporterBookmarkMergeTest {

    private lateinit var bookmarkDao: BookmarkDao
    private lateinit var importer: SyncDataImporter

    /** Stands in for the table: `all()` reads it, `upsert` replaces by (kind, targetId). */
    private val rows = mutableListOf<BookmarkEntity>()

    @Before
    fun setUp() {
        bookmarkDao = mockk(relaxed = true)
        coEvery { bookmarkDao.all() } answers { rows.toList() }
        coEvery { bookmarkDao.upsert(any()) } answers {
            val row = firstArg<BookmarkEntity>()
            rows.removeAll { it.kind == row.kind && it.targetId == row.targetId }
            rows += row
        }
        importer = importerWith(bookmarkDao)
    }

    private fun ayahRow() = rows.single { it.kind == BookmarkKind.AYAH && it.targetId == AYAH }

    @Test
    fun `a favourite older than the bookmark of the same ayah survives the import`() = runTest {
        importer.importQuranData(
            SyncPayload(
                bookmarks = listOf(bookmark(updatedAt = TUESDAY, note = "reflect on this")),
                favorites = listOf(favorite(updatedAt = MONDAY))
            )
        )

        val row = ayahRow()
        assertThat(row.bookmarked).isTrue()
        assertThat(row.favourite).isTrue()
        assertThat(row.note).isEqualTo("reflect on this")
    }

    @Test
    fun `a bookmark older than a local favourite survives the import`() = runTest {
        rows += BookmarkEntity(
            kind = BookmarkKind.AYAH,
            targetId = AYAH,
            bookmarked = false,
            favourite = true,
            note = null,
            colour = null,
            contextId = SURAH,
            ordinal = AYAH_IN_SURAH,
            createdAt = TUESDAY,
            updatedAt = TUESDAY
        )

        importer.importQuranData(
            SyncPayload(bookmarks = listOf(bookmark(updatedAt = MONDAY, note = "from my phone")))
        )

        val row = ayahRow()
        assertThat(row.bookmarked).isTrue()
        assertThat(row.favourite).isTrue()
    }

    @Test
    fun `an incoming favourite never clears an existing bookmark or its note`() = runTest {
        rows += BookmarkEntity(
            kind = BookmarkKind.AYAH,
            targetId = AYAH,
            bookmarked = true,
            favourite = false,
            note = "keep me",
            colour = "amber",
            contextId = SURAH,
            ordinal = AYAH_IN_SURAH,
            createdAt = MONDAY,
            updatedAt = MONDAY
        )

        importer.importQuranData(SyncPayload(favorites = listOf(favorite(updatedAt = TUESDAY))))

        val row = ayahRow()
        assertThat(row.bookmarked).isTrue()
        assertThat(row.favourite).isTrue()
        assertThat(row.note).isEqualTo("keep me")
        assertThat(row.colour).isEqualTo("amber")
    }

    @Test
    fun `a newer incoming note replaces the local one`() = runTest {
        rows += BookmarkEntity(
            kind = BookmarkKind.AYAH,
            targetId = AYAH,
            bookmarked = true,
            favourite = false,
            note = "old thought",
            colour = "amber",
            contextId = SURAH,
            ordinal = AYAH_IN_SURAH,
            createdAt = MONDAY,
            updatedAt = MONDAY
        )

        importer.importQuranData(
            SyncPayload(bookmarks = listOf(bookmark(updatedAt = TUESDAY, note = "new thought")))
        )

        assertThat(ayahRow().note).isEqualTo("new thought")
    }

    @Test
    fun `an older incoming note does not overwrite the local one`() = runTest {
        rows += BookmarkEntity(
            kind = BookmarkKind.AYAH,
            targetId = AYAH,
            bookmarked = true,
            favourite = false,
            note = "the newer thought",
            colour = "amber",
            contextId = SURAH,
            ordinal = AYAH_IN_SURAH,
            createdAt = TUESDAY,
            updatedAt = TUESDAY
        )

        importer.importQuranData(
            SyncPayload(bookmarks = listOf(bookmark(updatedAt = MONDAY, note = "stale thought")))
        )

        assertThat(ayahRow().note).isEqualTo("the newer thought")
    }

    @Test
    fun `an incoming favourite on an already-bookmarked name is not dropped`() = runTest {
        // The three name importers skipped the whole row whenever the target already existed
        // locally, so a name bookmarked here and favourited on the other device stayed
        // un-favourited. SyncNameBookmark carries no updatedAt, so there is no timestamp to
        // arbitrate with — the union is the only merge available.
        rows += BookmarkEntity(
            kind = BookmarkKind.ASMA_UL_HUSNA,
            targetId = 7,
            bookmarked = true,
            favourite = false,
            createdAt = MONDAY,
            updatedAt = MONDAY
        )

        importer.importNamesData(
            SyncPayload(
                asmaUlHusnaBookmarks = listOf(
                    SyncNameBookmark(id = 1L, refId = 7, isFavorite = true, createdAt = TUESDAY)
                )
            )
        )

        val row = rows.single { it.kind == BookmarkKind.ASMA_UL_HUSNA && it.targetId == 7 }
        assertThat(row.bookmarked).isTrue()
        assertThat(row.favourite).isTrue()
    }

    @Test
    fun `an incoming name bookmark never clears a local favourite`() = runTest {
        rows += BookmarkEntity(
            kind = BookmarkKind.PROPHET,
            targetId = 3,
            bookmarked = true,
            favourite = true,
            createdAt = TUESDAY,
            updatedAt = TUESDAY
        )

        importer.importNamesData(
            SyncPayload(
                prophetBookmarks = listOf(
                    SyncNameBookmark(id = 1L, refId = 3, isFavorite = false, createdAt = MONDAY)
                )
            )
        )

        assertThat(rows.single { it.kind == BookmarkKind.PROPHET && it.targetId == 3 }.favourite)
            .isTrue()
    }

    private fun bookmark(updatedAt: Long, note: String?) = SyncBookmark(
        id = 1L,
        ayahId = AYAH,
        surahNumber = SURAH,
        ayahNumber = AYAH_IN_SURAH,
        note = note,
        color = "amber",
        createdAt = updatedAt,
        updatedAt = updatedAt
    )

    private fun favorite(updatedAt: Long) = SyncFavorite(
        ayahId = AYAH,
        surahNumber = SURAH,
        ayahNumber = AYAH_IN_SURAH,
        createdAt = updatedAt,
        updatedAt = updatedAt
    )

    private companion object {
        const val AYAH = 262
        const val SURAH = 2
        const val AYAH_IN_SURAH = 255
        const val MONDAY = 1_000L
        const val TUESDAY = 2_000L
    }
}
