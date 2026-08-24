package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.Dua
import com.arshadshah.nimaz.domain.model.DuaCategory
import com.arshadshah.nimaz.domain.model.TafseerHighlight
import com.arshadshah.nimaz.domain.model.TafseerSource
import com.arshadshah.nimaz.domain.repository.DuaRepository
import com.arshadshah.nimaz.domain.repository.QuranRepository
import com.arshadshah.nimaz.domain.repository.TafseerRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Two lists the app assembles rather than reads: the day's dua, and every note written on a
 * commentary.
 *
 * **The day's dua** picks a category from the hour and then a dua from the day, so the same
 * device shows the same dua all day and a different one tomorrow. Both halves have a failure that
 * is invisible: an hour outside every band would throw rather than fall back, and an index taken
 * modulo an empty list would too — on a fresh install whose content artifact has not landed yet,
 * which is exactly when it happens.
 *
 * **The notes list** joins two repositories, and the join is where it can go quiet. A highlight
 * whose verse cannot be read is dropped rather than shown with a blank reference, and a highlight
 * with no note is not a note at all — it is a colour on a page. Neither drop is an error, so
 * nothing would report them being wrong.
 */
class DailySelectionAndNotesTest {

    // ---- The day's dua ----

    private val duaRepository: DuaRepository = mockk(relaxed = true)

    private fun category(id: String) = DuaCategory(
        id = id,
        nameArabic = "فئة",
        nameEnglish = "Category $id",
        description = null,
        iconName = "icon_$id",
        displayOrder = 0,
        duaCount = 3,
    )

    private fun dua(id: String, categoryId: String) = Dua(
        id = id,
        categoryId = categoryId,
        titleArabic = "دعاء",
        titleEnglish = "Dua $id",
        textArabic = "نص",
        textTransliteration = null,
        textEnglish = "text",
        reference = null,
        occasion = null,
        benefits = null,
        repeatCount = null,
        audioUrl = null,
        displayOrder = 0,
    )

    private fun givenDuas(categoryId: String, count: Int) {
        coEvery { duaRepository.getCategoryById(categoryId) } returns category(categoryId)
        coEvery { duaRepository.getDuasByCategoryOnce(categoryId) } returns
            (1..count).map { dua("$categoryId-$it", categoryId) }
    }

    @Test
    fun `the morning runs from before dawn to late afternoon`() = runTest {
        givenDuas("1", 3)

        listOf(4, 9, 15).forEach { hour ->
            val selection = GetDailyDuaUseCase(duaRepository)(hourOfDay = hour, dayOfYear = 1)
            assertThat(selection?.dua?.categoryId).isEqualTo("1")
        }
    }

    @Test
    fun `the evening is its own band`() = runTest {
        givenDuas("2", 3)

        listOf(16, 18, 20).forEach { hour ->
            val selection = GetDailyDuaUseCase(duaRepository)(hourOfDay = hour, dayOfYear = 1)
            assertThat(selection?.dua?.categoryId).isEqualTo("2")
        }
    }

    @Test
    fun `every other hour, including the small hours, is before-sleep`() = runTest {
        givenDuas("5", 3)

        listOf(21, 23, 0, 3).forEach { hour ->
            val selection = GetDailyDuaUseCase(duaRepository)(hourOfDay = hour, dayOfYear = 1)
            assertThat(selection?.dua?.categoryId).isEqualTo("5")
        }
    }

    @Test
    fun `an hour outside the clock still lands on a band rather than throwing`() = runTest {
        // A caller passing an out-of-range hour is a bug, but a widget refresh crashing is worse
        // than a wrong dua.
        givenDuas("5", 3)

        assertThat(GetDailyDuaUseCase(duaRepository)(hourOfDay = -1, dayOfYear = 1)).isNotNull()
        assertThat(GetDailyDuaUseCase(duaRepository)(hourOfDay = 99, dayOfYear = 1)).isNotNull()
    }

    @Test
    fun `the day picks which dua, so the same day gives the same one`() = runTest {
        givenDuas("1", 3)

        val first = GetDailyDuaUseCase(duaRepository)(hourOfDay = 9, dayOfYear = 200)
        val again = GetDailyDuaUseCase(duaRepository)(hourOfDay = 11, dayOfYear = 200)

        assertThat(first?.dua?.id).isEqualTo(again?.dua?.id)
    }

    @Test
    fun `the next day gives a different one`() = runTest {
        givenDuas("1", 3)

        val today = GetDailyDuaUseCase(duaRepository)(hourOfDay = 9, dayOfYear = 200)
        val tomorrow = GetDailyDuaUseCase(duaRepository)(hourOfDay = 9, dayOfYear = 201)

        assertThat(today?.dua?.id).isNotEqualTo(tomorrow?.dua?.id)
    }

    @Test
    fun `the selection carries the category's own label and icon`() = runTest {
        givenDuas("1", 3)

        val selection = GetDailyDuaUseCase(duaRepository)(hourOfDay = 9, dayOfYear = 1)

        assertThat(selection?.categoryName).isEqualTo("Category 1")
        assertThat(selection?.categoryIcon).isEqualTo("icon_1")
    }

    @Test
    fun `a category with no duas yields nothing rather than dividing by zero`() = runTest {
        // The fresh-install window: the tables exist, the content artifact has not landed.
        coEvery { duaRepository.getCategoryById(any()) } returns category("1")
        coEvery { duaRepository.getDuasByCategoryOnce(any()) } returns emptyList()

        assertThat(GetDailyDuaUseCase(duaRepository)(hourOfDay = 9, dayOfYear = 1)).isNull()
    }

    @Test
    fun `a category that is not there yields nothing`() = runTest {
        coEvery { duaRepository.getCategoryById(any()) } returns null

        assertThat(GetDailyDuaUseCase(duaRepository)(hourOfDay = 9, dayOfYear = 1)).isNull()
    }

    @Test
    fun `a day of year past the end of a short list still lands inside it`() = runTest {
        givenDuas("1", 2)

        val selection = GetDailyDuaUseCase(duaRepository)(hourOfDay = 9, dayOfYear = 366)

        assertThat(selection).isNotNull()
    }

    // ---- Notes written on a commentary ----

    private val tafseerRepository: TafseerRepository = mockk(relaxed = true)
    private val quranRepository: QuranRepository = mockk(relaxed = true)

    private fun highlight(
        id: Long,
        ayahId: Int,
        note: String?,
        tafseerId: String = TafseerSource.entries.first().id,
    ) = TafseerHighlight(
        id = id,
        ayahId = ayahId,
        tafseerId = tafseerId,
        startOffset = 0,
        endOffset = 10,
        color = "#FDE68A",
        note = note,
        createdAt = 0,
        updatedAt = 0,
    )

    private fun ayah(id: Int, surah: Int, number: Int) = Ayah(
        id = id,
        surahNumber = surah,
        ayahNumber = number,
        textArabic = "نص",
        textSimple = "nass",
        juzNumber = 1,
        hizbNumber = 1,
        rubNumber = 0,
        pageNumber = 1,
        sajdaType = null,
        sajdaNumber = null,
    )

    private fun notes() = GetTafseerNotesUseCase(tafseerRepository, quranRepository)

    @Test
    fun `a note is reported against the verse it was written on`() = runTest {
        every { tafseerRepository.getAllHighlights() } returns
            flowOf(listOf(highlight(1, ayahId = 2140, note = "worth returning to")))
        coEvery { quranRepository.getAyahById(2140) } returns ayah(2140, surah = 18, number = 1)

        val result = notes()().first()

        assertThat(result).hasSize(1)
        with(result.single()) {
            assertThat(highlightId).isEqualTo(1)
            assertThat(surahNumber).isEqualTo(18)
            assertThat(ayahNumber).isEqualTo(1)
            assertThat(note).isEqualTo("worth returning to")
            assertThat(color).isEqualTo("#FDE68A")
        }
    }

    @Test
    fun `a highlight with no note is a colour on a page, not a note`() = runTest {
        every { tafseerRepository.getAllHighlights() } returns flowOf(
            listOf(
                highlight(1, ayahId = 2140, note = null),
                highlight(2, ayahId = 2141, note = "   "),
                highlight(3, ayahId = 2142, note = "a real one"),
            )
        )
        coEvery { quranRepository.getAyahById(any()) } answers {
            ayah(firstArg(), surah = 18, number = 1)
        }

        val result = notes()().first()

        assertThat(result.map { it.highlightId }).containsExactly(3L)
    }

    @Test
    fun `a note on a verse that cannot be read is dropped, not shown blank`() = runTest {
        // A row pointing at a verse the content database no longer has would otherwise render
        // as a note filed under "0:0".
        every { tafseerRepository.getAllHighlights() } returns
            flowOf(listOf(highlight(1, ayahId = 999_999, note = "orphaned")))
        coEvery { quranRepository.getAyahById(999_999) } returns null

        assertThat(notes()().first()).isEmpty()
    }

    @Test
    fun `the source is named, not left as its stored id`() = runTest {
        val source = TafseerSource.entries.first()
        every { tafseerRepository.getAllHighlights() } returns
            flowOf(listOf(highlight(1, ayahId = 2140, note = "n", tafseerId = source.id)))
        coEvery { quranRepository.getAyahById(any()) } returns ayah(2140, 18, 1)

        assertThat(notes()().first().single().sourceLabel).isEqualTo(source.displayName)
    }

    @Test
    fun `a source the app no longer ships falls back to its stored id`() = runTest {
        // Better a raw id than an empty label: the reader can still tell two notes apart.
        every { tafseerRepository.getAllHighlights() } returns
            flowOf(listOf(highlight(1, ayahId = 2140, note = "n", tafseerId = "retired-source")))
        coEvery { quranRepository.getAyahById(any()) } returns ayah(2140, 18, 1)

        assertThat(notes()().first().single().sourceLabel).isEqualTo("retired-source")
    }

    @Test
    fun `no highlights means no notes`() = runTest {
        every { tafseerRepository.getAllHighlights() } returns flowOf(emptyList())

        assertThat(notes()().first()).isEmpty()
    }
}
