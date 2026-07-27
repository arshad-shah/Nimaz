package com.arshadshah.nimaz.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Pins [KhatamConstants.JUZ_AYAH_RANGES] to the *real* juz boundaries (#325).
 *
 * The constant is a hand-maintained duplicate of knowledge the database already holds in
 * `ayahs.juz`, and it had drifted: juz 7 was off by one and juz 15-30 were wrong by
 * hundreds of ayahs (juz 30 started at 4090 instead of 5673, i.e. it claimed a third of
 * the Quran). The Juz tab's khatam rings read this constant while the Khatam detail screen
 * groups by `ayahs.juz`, so the two surfaces disagreed.
 *
 * Rather than restate the corrected numbers (which would just pin the constant to itself),
 * every expectation here is **derived independently** from two pieces of reference data
 * that are not in the production source: the 114 surah ayah counts, and the classical
 * surah:ayah reference each juz starts at.
 */
class KhatamJuzBoundariesTest {

    /** Ayah count of each of the 114 surahs, in mushaf order. Sums to 6236. */
    private val surahAyahCounts = listOf(
        7, 286, 200, 176, 120, 165, 206, 75, 129, 109,
        123, 111, 43, 52, 99, 128, 111, 110, 98, 135,
        112, 78, 118, 64, 77, 227, 93, 88, 69, 60,
        34, 30, 73, 54, 45, 83, 182, 88, 75, 85,
        54, 53, 89, 59, 37, 35, 38, 29, 18, 45,
        60, 49, 62, 55, 78, 96, 29, 22, 24, 13,
        14, 11, 11, 18, 12, 12, 30, 52, 52, 44,
        28, 28, 20, 56, 40, 31, 50, 40, 46, 42,
        29, 19, 36, 25, 22, 17, 19, 26, 30, 20,
        15, 21, 11, 8, 8, 19, 5, 8, 8, 11,
        11, 8, 3, 9, 5, 4, 7, 3, 6, 3,
        5, 4, 5, 6
    )

    /** The classical (surah, ayah) each juz opens on. */
    private val juzStartReferences = listOf(
        1 to 1, 2 to 142, 2 to 253, 3 to 93, 4 to 24,
        4 to 148, 5 to 82, 6 to 111, 7 to 88, 8 to 41,
        9 to 93, 11 to 6, 12 to 53, 15 to 1, 17 to 1,
        18 to 75, 21 to 1, 23 to 1, 25 to 21, 27 to 56,
        29 to 46, 33 to 31, 36 to 28, 39 to 32, 41 to 47,
        46 to 1, 51 to 31, 58 to 1, 67 to 1, 78 to 1
    )

    /** Global ayah id (1-6236) of a surah:ayah reference. */
    private fun globalAyahId(surah: Int, ayah: Int): Int =
        surahAyahCounts.take(surah - 1).sum() + ayah

    private val expectedStarts: List<Int>
        get() = juzStartReferences.map { (surah, ayah) -> globalAyahId(surah, ayah) }

    @Test
    fun `reference data is self-consistent`() {
        assertThat(surahAyahCounts).hasSize(114)
        assertThat(surahAyahCounts.sum()).isEqualTo(Khatam.TOTAL_QURAN_AYAHS)
        assertThat(juzStartReferences).hasSize(Khatam.TOTAL_JUZ)
    }

    @Test
    fun `every juz starts at the ayah id of its classical reference`() {
        val actualStarts = KhatamConstants.JUZ_AYAH_RANGES.map { it.first }

        assertThat(actualStarts).isEqualTo(expectedStarts)
    }

    @Test
    fun `juz 30 starts at surah An-Naba`() {
        // The regression that motivated #325's validation: 78:1 is global ayah 5673.
        assertThat(globalAyahId(78, 1)).isEqualTo(5673)
        assertThat(KhatamConstants.JUZ_AYAH_RANGES.last().first).isEqualTo(5673)
        assertThat(KhatamConstants.juzForAyahId(5673)).isEqualTo(30)
        assertThat(KhatamConstants.juzForAyahId(5672)).isEqualTo(29)
    }

    @Test
    fun `ranges are contiguous and cover the whole Quran exactly once`() {
        val ranges = KhatamConstants.JUZ_AYAH_RANGES

        assertThat(ranges).hasSize(Khatam.TOTAL_JUZ)
        assertThat(ranges.first().first).isEqualTo(1)
        assertThat(ranges.last().second).isEqualTo(Khatam.TOTAL_QURAN_AYAHS)
        ranges.zipWithNext { (_, end), (nextStart, _) ->
            assertThat(nextStart).isEqualTo(end + 1)
        }
        assertThat(ranges.sumOf { (start, end) -> end - start + 1 })
            .isEqualTo(Khatam.TOTAL_QURAN_AYAHS)
    }

    @Test
    fun `juzForAyahId agrees with the derived boundaries at every juz edge`() {
        expectedStarts.forEachIndexed { index, start ->
            assertThat(KhatamConstants.juzForAyahId(start)).isEqualTo(index + 1)
            if (index > 0) {
                assertThat(KhatamConstants.juzForAyahId(start - 1)).isEqualTo(index)
            }
        }
    }
}
