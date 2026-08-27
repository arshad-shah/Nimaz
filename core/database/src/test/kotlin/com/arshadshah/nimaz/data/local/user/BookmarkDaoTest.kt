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
 * The one table that replaced seven, and the two flags that replaced two tables.
 *
 * `bookmarked` and `favourite` sharing a row is the whole risk of the consolidation: the pair of
 * tables it replaced could not lose one mark while clearing the other, and this one can. The
 * `kind` column carries the same risk at the other end — one table means a query that forgets to
 * filter by kind answers a hadith question with a verse.
 */
@RunWith(RobolectricTestRunner::class)
class BookmarkDaoTest {

    private lateinit var db: NimazUserDatabase
    private lateinit var dao: BookmarkDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NimazUserDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.bookmarkDao()
    }

    @After
    fun tearDown() = db.close()

    // ---- The two flags ----

    @Test
    fun `un-favouriting a verse that is also bookmarked keeps the bookmark`() = runTest {
        dao.upsert(bookmark(targetId = 1, bookmarked = true, favourite = true))

        dao.clearFavourite(BookmarkKind.AYAH, targetId = 1, now = 500)

        val row = dao.find(BookmarkKind.AYAH, targetId = 1)
        assertThat(row?.favourite).isFalse()
        assertThat(row?.bookmarked).isTrue()
        assertThat(row?.updatedAt).isEqualTo(500)
        assertThat(dao.observeIsBookmarked(BookmarkKind.AYAH, 1).first()).isTrue()
        assertThat(dao.observeIsFavourite(BookmarkKind.AYAH, 1).first()).isFalse()
    }

    @Test
    fun `un-bookmarking a verse that is also a favourite keeps the favourite`() = runTest {
        dao.upsert(bookmark(targetId = 1, bookmarked = true, favourite = true))

        dao.clearBookmark(BookmarkKind.AYAH, targetId = 1, now = 500)

        assertThat(dao.observeIsBookmarked(BookmarkKind.AYAH, 1).first()).isFalse()
        assertThat(dao.observeIsFavourite(BookmarkKind.AYAH, 1).first()).isTrue()
    }

    @Test
    fun `pruning removes only the rows with neither flag left`() = runTest {
        dao.upsertAll(
            listOf(
                bookmark(targetId = 1, bookmarked = true, favourite = true),
                bookmark(targetId = 2, bookmarked = true, favourite = false),
                bookmark(targetId = 3, bookmarked = false, favourite = false),
            )
        )
        dao.clearFavourite(BookmarkKind.AYAH, targetId = 1, now = 500)

        dao.pruneEmpty()

        assertThat(dao.all().map { it.targetId }).containsExactly(1, 2)
    }

    @Test
    fun `clearing both flags then pruning leaves nothing behind`() = runTest {
        dao.upsert(bookmark(targetId = 1, bookmarked = true, favourite = true))

        dao.clearFavourite(BookmarkKind.AYAH, targetId = 1, now = 500)
        dao.clearBookmark(BookmarkKind.AYAH, targetId = 1, now = 500)
        dao.pruneEmpty()

        assertThat(dao.find(BookmarkKind.AYAH, targetId = 1)).isNull()
    }

    // ---- One table, many kinds ----

    @Test
    fun `the same target id under two kinds is two rows`() = runTest {
        dao.upsertAll(
            listOf(
                bookmark(kind = BookmarkKind.AYAH, targetId = 1),
                bookmark(kind = BookmarkKind.HADITH, targetId = 1),
            )
        )

        assertThat(dao.all()).hasSize(2)
        assertThat(dao.bookmarkedIds(BookmarkKind.AYAH)).containsExactly(1)
        assertThat(dao.bookmarkedIds(BookmarkKind.HADITH)).containsExactly(1)
        assertThat(dao.bookmarkedIds(BookmarkKind.DUA)).isEmpty()
    }

    @Test
    fun `deleting one kind's mark leaves the other kind's alone`() = runTest {
        dao.upsertAll(
            listOf(
                bookmark(kind = BookmarkKind.AYAH, targetId = 1),
                bookmark(kind = BookmarkKind.HADITH, targetId = 1),
            )
        )

        dao.delete(BookmarkKind.AYAH, targetId = 1)

        assertThat(dao.all().map { it.kind }).containsExactly(BookmarkKind.HADITH)
    }

    @Test
    fun `re-marking a target updates the row rather than adding one`() = runTest {
        dao.upsert(bookmark(targetId = 1, note = "first", createdAt = 100, updatedAt = 100))
        dao.upsert(bookmark(targetId = 1, note = "second", createdAt = 100, updatedAt = 200))

        // (kind, target_id) is the primary key, so the upsert is the whole dedup story.
        assertThat(dao.all()).hasSize(1)
        assertThat(dao.find(BookmarkKind.AYAH, targetId = 1)?.note).isEqualTo("second")
    }

    @Test
    fun `bookmarks and favourites are listed newest first`() = runTest {
        dao.upsertAll(
            listOf(
                bookmark(targetId = 1, favourite = true, createdAt = 100),
                bookmark(targetId = 2, favourite = true, createdAt = 300),
                bookmark(targetId = 3, favourite = false, createdAt = 200),
            )
        )

        assertThat(dao.bookmarks(BookmarkKind.AYAH).first().map { it.targetId })
            .containsExactly(2, 3, 1).inOrder()
        assertThat(dao.favourites(BookmarkKind.AYAH).first().map { it.targetId })
            .containsExactly(2, 1).inOrder()
        assertThat(dao.favouriteIds(BookmarkKind.AYAH)).containsExactly(1, 2)
    }

    @Test
    fun `marks within a surah exclude the ones outside it`() = runTest {
        dao.upsertAll(
            listOf(
                bookmark(targetId = 1, contextId = 2, createdAt = 100),
                bookmark(targetId = 2, contextId = 2, createdAt = 300),
                bookmark(targetId = 3, contextId = 7, createdAt = 200),
            )
        )

        assertThat(dao.inContext(BookmarkKind.AYAH, contextId = 2).first().map { it.targetId })
            .containsExactly(2, 1).inOrder()
    }

    @Test
    fun `an un-bookmarked row is not counted and not listed`() = runTest {
        dao.upsertAll(
            listOf(
                bookmark(targetId = 1, bookmarked = true),
                bookmark(targetId = 2, bookmarked = false, favourite = true),
            )
        )

        assertThat(dao.bookmarkCount(BookmarkKind.AYAH).first()).isEqualTo(1)
        assertThat(dao.bookmarks(BookmarkKind.AYAH).first().map { it.targetId })
            .containsExactly(1)
        // It is still a favourite, though.
        assertThat(dao.favourites(BookmarkKind.AYAH).first().map { it.targetId })
            .containsExactly(2)
    }

    @Test
    fun `deleting by entity removes the row it names`() = runTest {
        val row = bookmark(targetId = 1)
        dao.upsertAll(listOf(row, bookmark(targetId = 2)))

        dao.delete(row)

        assertThat(dao.all().map { it.targetId }).containsExactly(2)
    }

    @Test
    fun `clearing removes every kind at once`() = runTest {
        dao.upsertAll(
            listOf(
                bookmark(kind = BookmarkKind.AYAH, targetId = 1),
                bookmark(kind = BookmarkKind.PROPHET, targetId = 1),
            )
        )

        dao.clear()

        assertThat(dao.all()).isEmpty()
    }

    private fun bookmark(
        kind: String = BookmarkKind.AYAH,
        targetId: Int,
        bookmarked: Boolean = true,
        favourite: Boolean = false,
        note: String? = null,
        contextId: Int? = null,
        createdAt: Long = 0,
        updatedAt: Long = 0,
    ) = BookmarkEntity(
        kind = kind,
        targetId = targetId,
        bookmarked = bookmarked,
        favourite = favourite,
        note = note,
        contextId = contextId,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
