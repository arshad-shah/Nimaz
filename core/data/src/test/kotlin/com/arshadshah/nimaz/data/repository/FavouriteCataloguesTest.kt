package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.data.local.database.dao.AsmaUlHusnaDao
import com.arshadshah.nimaz.data.local.database.dao.AsmaUnNabiDao
import com.arshadshah.nimaz.data.local.database.dao.ProphetDao
import com.arshadshah.nimaz.data.local.database.entity.AsmaUlHusnaEntity
import com.arshadshah.nimaz.data.local.database.entity.AsmaUnNabiEntity
import com.arshadshah.nimaz.data.local.database.entity.ProphetEntity
import com.arshadshah.nimaz.data.local.user.BookmarkDao
import com.arshadshah.nimaz.data.local.user.BookmarkEntity
import com.arshadshah.nimaz.data.local.user.BookmarkKind
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * The three read-only catalogues a user can only *favourite*: the 99 names, the names of the
 * Prophet ﷺ, and the 25 prophets.
 *
 * All three have the same shape and the same trap. Each used to have its own favourites table
 * where the row's only meaning was "favourited", so a toggle was insert-or-delete. The
 * consolidated row can *also* be bookmarked — so a toggle that still deletes the row destroys a
 * bookmark the user made separately, with nothing on screen to say it happened. Three
 * repositories, three copies of the same `when`, and this is the test that keeps them agreeing.
 *
 * The other shared trap is the JSON array columns. `quran_references`, `key_lessons`,
 * `quran_mentions` and `miracles` are all comma-free JSON stored as text, and a corpus that
 * ships a malformed one must give a detail screen with an empty list, not a crash.
 */
class FavouriteCataloguesTest {

    private lateinit var bookmarkDao: BookmarkDao

    private lateinit var asmaUlHusnaDao: AsmaUlHusnaDao
    private lateinit var asmaUnNabiDao: AsmaUnNabiDao
    private lateinit var prophetDao: ProphetDao

    private lateinit var names: AsmaUlHusnaRepositoryImpl
    private lateinit var nabiNames: AsmaUnNabiRepositoryImpl
    private lateinit var prophets: ProphetRepositoryImpl

    @Before
    fun setUp() {
        bookmarkDao = mockk(relaxed = true)
        asmaUlHusnaDao = mockk(relaxed = true)
        asmaUnNabiDao = mockk(relaxed = true)
        prophetDao = mockk(relaxed = true)
        every { bookmarkDao.favourites(any()) } returns flowOf(emptyList())
        names = AsmaUlHusnaRepositoryImpl(asmaUlHusnaDao, bookmarkDao)
        nabiNames = AsmaUnNabiRepositoryImpl(asmaUnNabiDao, bookmarkDao)
        prophets = ProphetRepositoryImpl(prophetDao, bookmarkDao)
    }

    // ── the toggle, for each catalogue ────────────────────────────────────────

    @Test
    fun `favouriting a name with no mark creates a row that is not a bookmark`() = runTest {
        coEvery { bookmarkDao.find(BookmarkKind.ASMA_UL_HUSNA, 1) } returns null
        val saved = mutableListOf<BookmarkEntity>()
        coEvery { bookmarkDao.upsert(capture(saved)) } returns Unit

        names.toggleFavorite(1)

        assertThat(saved.single().favourite).isTrue()
        // Favouriting is not bookmarking: the row must not claim a mark the user never made.
        assertThat(saved.single().bookmarked).isFalse()
    }

    @Test
    fun `un-favouriting a name that is also bookmarked clears the flag and keeps the row`() =
        runTest {
            coEvery { bookmarkDao.find(BookmarkKind.ASMA_UL_HUSNA, 1) } returns
                mark(BookmarkKind.ASMA_UL_HUSNA, favourite = true, bookmarked = true)

            names.toggleFavorite(1)

            coVerify { bookmarkDao.clearFavourite(BookmarkKind.ASMA_UL_HUSNA, 1, any()) }
            coVerify(exactly = 0) { bookmarkDao.delete(BookmarkKind.ASMA_UL_HUSNA, 1) }
        }

    @Test
    fun `un-favouriting a name that is nothing else removes the row`() = runTest {
        coEvery { bookmarkDao.find(BookmarkKind.ASMA_UL_HUSNA, 1) } returns
            mark(BookmarkKind.ASMA_UL_HUSNA, favourite = true, bookmarked = false)

        names.toggleFavorite(1)

        coVerify { bookmarkDao.delete(BookmarkKind.ASMA_UL_HUSNA, 1) }
    }

    @Test
    fun `favouriting a name that is already bookmarked sets the flag on the same row`() =
        runTest {
            coEvery { bookmarkDao.find(BookmarkKind.ASMA_UL_HUSNA, 1) } returns
                mark(BookmarkKind.ASMA_UL_HUSNA, favourite = false, bookmarked = true)
            val saved = mutableListOf<BookmarkEntity>()
            coEvery { bookmarkDao.upsert(capture(saved)) } returns Unit

            names.toggleFavorite(1)

            assertThat(saved.single().favourite).isTrue()
            assertThat(saved.single().bookmarked).isTrue()
        }

    @Test
    fun `the names of the Prophet follow the same four rules`() = runTest {
        val saved = mutableListOf<BookmarkEntity>()
        coEvery { bookmarkDao.upsert(capture(saved)) } returns Unit

        coEvery { bookmarkDao.find(BookmarkKind.ASMA_UN_NABI, 2) } returns null
        nabiNames.toggleFavorite(2)
        assertThat(saved.single().favourite).isTrue()

        coEvery { bookmarkDao.find(BookmarkKind.ASMA_UN_NABI, 2) } returns
            mark(BookmarkKind.ASMA_UN_NABI, favourite = true, bookmarked = true)
        nabiNames.toggleFavorite(2)
        coVerify { bookmarkDao.clearFavourite(BookmarkKind.ASMA_UN_NABI, 2, any()) }

        coEvery { bookmarkDao.find(BookmarkKind.ASMA_UN_NABI, 2) } returns
            mark(BookmarkKind.ASMA_UN_NABI, favourite = true, bookmarked = false)
        nabiNames.toggleFavorite(2)
        coVerify { bookmarkDao.delete(BookmarkKind.ASMA_UN_NABI, 2) }

        coEvery { bookmarkDao.find(BookmarkKind.ASMA_UN_NABI, 2) } returns
            mark(BookmarkKind.ASMA_UN_NABI, favourite = false, bookmarked = true)
        nabiNames.toggleFavorite(2)
        assertThat(saved.last().favourite).isTrue()
        assertThat(saved.last().bookmarked).isTrue()
    }

    @Test
    fun `the prophets follow the same four rules`() = runTest {
        val saved = mutableListOf<BookmarkEntity>()
        coEvery { bookmarkDao.upsert(capture(saved)) } returns Unit

        coEvery { bookmarkDao.find(BookmarkKind.PROPHET, 3) } returns null
        prophets.toggleFavorite(3)
        assertThat(saved.single().favourite).isTrue()

        coEvery { bookmarkDao.find(BookmarkKind.PROPHET, 3) } returns
            mark(BookmarkKind.PROPHET, favourite = true, bookmarked = true)
        prophets.toggleFavorite(3)
        coVerify { bookmarkDao.clearFavourite(BookmarkKind.PROPHET, 3, any()) }

        coEvery { bookmarkDao.find(BookmarkKind.PROPHET, 3) } returns
            mark(BookmarkKind.PROPHET, favourite = true, bookmarked = false)
        prophets.toggleFavorite(3)
        coVerify { bookmarkDao.delete(BookmarkKind.PROPHET, 3) }

        coEvery { bookmarkDao.find(BookmarkKind.PROPHET, 3) } returns
            mark(BookmarkKind.PROPHET, favourite = false, bookmarked = true)
        prophets.toggleFavorite(3)
        assertThat(saved.last().favourite).isTrue()
        assertThat(saved.last().bookmarked).isTrue()
    }

    // ── "is it favourited" reads the flag, not the row's existence ────────────

    @Test
    fun `a bookmarked but un-favourited row does not count as a favourite`() = runTest {
        coEvery { bookmarkDao.find(BookmarkKind.ASMA_UL_HUSNA, 1) } returns
            mark(BookmarkKind.ASMA_UL_HUSNA, favourite = false, bookmarked = true)
        coEvery { bookmarkDao.find(BookmarkKind.ASMA_UN_NABI, 1) } returns
            mark(BookmarkKind.ASMA_UN_NABI, favourite = false, bookmarked = true)
        coEvery { bookmarkDao.find(BookmarkKind.PROPHET, 1) } returns
            mark(BookmarkKind.PROPHET, favourite = false, bookmarked = true)

        // The row exists; the flag does not. Testing existence is the old table's habit.
        assertThat(names.isFavorite(1)).isFalse()
        assertThat(nabiNames.isFavorite(1)).isFalse()
        assertThat(prophets.isFavorite(1)).isFalse()
    }

    @Test
    fun `a favourited row counts, and a missing row does not`() = runTest {
        coEvery { bookmarkDao.find(BookmarkKind.PROPHET, 1) } returns
            mark(BookmarkKind.PROPHET, favourite = true, bookmarked = false)
        coEvery { bookmarkDao.find(BookmarkKind.PROPHET, 2) } returns null

        assertThat(prophets.isFavorite(1)).isTrue()
        assertThat(prophets.isFavorite(2)).isFalse()
    }

    // ── the favourite flag on the way out ─────────────────────────────────────

    @Test
    fun `a listed name carries whether it is favourited`() = runTest {
        every { asmaUlHusnaDao.getAllNames() } returns flowOf(listOf(name(1), name(2)))
        every { bookmarkDao.favourites(BookmarkKind.ASMA_UL_HUSNA) } returns
            flowOf(listOf(mark(BookmarkKind.ASMA_UL_HUSNA, targetId = 2, favourite = true)))

        val listed = names.getAllNames().first()

        assertThat(listed.single { it.id == 1 }.isFavorite).isFalse()
        assertThat(listed.single { it.id == 2 }.isFavorite).isTrue()
    }

    @Test
    fun `an id the corpus does not hold opens nothing`() = runTest {
        coEvery { asmaUlHusnaDao.getNameById(99) } returns null
        coEvery { asmaUnNabiDao.getNameById(99) } returns null
        coEvery { prophetDao.getProphetById(99) } returns null

        assertThat(names.getNameById(99)).isNull()
        assertThat(nabiNames.getNameById(99)).isNull()
        assertThat(prophets.getProphetById(99)).isNull()
    }

    @Test
    fun `opening one name reads its favourite flag from the user's database`() = runTest {
        coEvery { asmaUlHusnaDao.getNameById(1) } returns name(1)
        coEvery { bookmarkDao.find(BookmarkKind.ASMA_UL_HUSNA, 1) } returns
            mark(BookmarkKind.ASMA_UL_HUSNA, favourite = true)

        assertThat(names.getNameById(1)!!.isFavorite).isTrue()
    }

    @Test
    fun `a device with no favourites does not query the content database for them`() = runTest {
        every { bookmarkDao.favourites(BookmarkKind.PROPHET) } returns flowOf(emptyList())

        assertThat(prophets.getFavoriteProphets().first()).isEmpty()
        // `WHERE id IN ()` is not a query worth making.
        io.mockk.verify(exactly = 0) { prophetDao.getByIds(any()) }
    }

    @Test
    fun `favourited ids come from the user's database and the rows from the content one`() =
        runTest {
            every { bookmarkDao.favourites(BookmarkKind.ASMA_UN_NABI) } returns
                flowOf(listOf(mark(BookmarkKind.ASMA_UN_NABI, targetId = 4, favourite = true)))
            every { asmaUnNabiDao.getByIds(listOf(4)) } returns flowOf(listOf(nabiName(4)))

            val favourites = nabiNames.getFavoriteNames().first()

            assertThat(favourites.single().id).isEqualTo(4)
            assertThat(favourites.single().isFavorite).isTrue()
        }

    // ── the JSON array columns ────────────────────────────────────────────────

    @Test
    fun `a name's references are parsed out of its JSON column`() = runTest {
        coEvery { asmaUlHusnaDao.getNameById(1) } returns
            name(1, references = """["1:1","2:255"]""")

        assertThat(names.getNameById(1)!!.quranReferences)
            .containsExactly("1:1", "2:255").inOrder()
    }

    @Test
    fun `a malformed references column gives an empty list, not a crash`() = runTest {
        coEvery { asmaUlHusnaDao.getNameById(1) } returns name(1, references = "not json")

        // A detail screen with no references beats a detail screen that will not open.
        assertThat(names.getNameById(1)!!.quranReferences).isEmpty()
    }

    @Test
    fun `a prophet's three JSON columns are each parsed`() = runTest {
        coEvery { prophetDao.getProphetById(1) } returns prophet(
            1,
            keyLessons = """["patience"]""",
            quranMentions = """["2:124"]""",
            miracles = """["the staff","the sea"]""",
        )

        val read = prophets.getProphetById(1)!!

        assertThat(read.keyLessons).containsExactly("patience")
        assertThat(read.quranMentions).containsExactly("2:124")
        assertThat(read.miracles).containsExactly("the staff", "the sea").inOrder()
    }

    @Test
    fun `one malformed column does not take the other two down with it`() = runTest {
        coEvery { prophetDao.getProphetById(1) } returns prophet(
            1,
            keyLessons = "{",
            quranMentions = """["2:124"]""",
            miracles = """["the staff"]""",
        )

        val read = prophets.getProphetById(1)!!

        assertThat(read.keyLessons).isEmpty()
        assertThat(read.quranMentions).containsExactly("2:124")
        assertThat(read.miracles).containsExactly("the staff")
    }

    // ── fixtures ──────────────────────────────────────────────────────────────

    private fun mark(
        kind: String,
        targetId: Int = 1,
        favourite: Boolean = false,
        bookmarked: Boolean = false,
    ) = BookmarkEntity(
        kind = kind,
        targetId = targetId,
        bookmarked = bookmarked,
        favourite = favourite,
        createdAt = 1L,
        updatedAt = 1L,
    )

    private fun name(id: Int, references: String = "[]") = AsmaUlHusnaEntity(
        id = id, number = id, nameArabic = "الرحمن", nameTransliteration = "Ar-Rahman",
        nameEnglish = "The Most Merciful", meaning = "mercy", explanation = "e",
        benefits = "b", quranReferences = references, usageInDua = "u", displayOrder = id,
    )

    private fun nabiName(id: Int) = AsmaUnNabiEntity(
        id = id, number = id, nameArabic = "محمد", nameTransliteration = "Muhammad",
        nameEnglish = "The Praised One", meaning = "praise", explanation = "e",
        source = "s", displayOrder = id,
    )

    private fun prophet(
        id: Int,
        keyLessons: String = "[]",
        quranMentions: String = "[]",
        miracles: String = "[]",
    ) = ProphetEntity(
        id = id, number = id, nameArabic = "موسى", nameEnglish = "Moses",
        nameTransliteration = "Musa", titleArabic = "كليم الله",
        titleEnglish = "The one who spoke with God", storySummary = "s",
        keyLessons = keyLessons, quranMentions = quranMentions, era = "e", lineage = "l",
        yearsLived = "120", placeOfPreaching = "Egypt", miracles = miracles, displayOrder = id,
    )
}
