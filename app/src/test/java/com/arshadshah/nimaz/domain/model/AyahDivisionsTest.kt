package com.arshadshah.nimaz.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The mushaf divisions a verse carries, and how the reader is meant to read them.
 *
 * [Ayah.rubNumber] counts hizb quarters across the whole Quran (1..240). The reader used to
 * `when`-match it against 1..4 as though it were the quarter's position *within* a hizb, so
 * the four quarters at the very start of the book produced a marker and the other 236
 * produced an empty label — no marker at all. [Ayah.quarterInHizb] and [Ayah.hizbOfQuarter]
 * are that arithmetic done once, in the model.
 */
class AyahDivisionsTest {

    private fun ayah(rub: Int, hizb: Int = 0) = Ayah(
        id = 1,
        surahNumber = 1,
        ayahNumber = 1,
        textArabic = "",
        textSimple = "",
        juzNumber = 1,
        hizbNumber = hizb,
        rubNumber = rub,
        pageNumber = 1,
        sajdaType = null,
        sajdaNumber = null,
    )

    @Test
    fun `quarters cycle 1 to 4 across every hizb, not just the first`() {
        assertThat(ayah(rub = 1).quarterInHizb).isEqualTo(1)
        assertThat(ayah(rub = 4).quarterInHizb).isEqualTo(4)
        // The regression: quarter 5 opens hizb 2 and must read as its first quarter.
        assertThat(ayah(rub = 5).quarterInHizb).isEqualTo(1)
        assertThat(ayah(rub = 123).quarterInHizb).isEqualTo(3)
        // The last quarter of the Quran.
        assertThat(ayah(rub = 240).quarterInHizb).isEqualTo(4)
    }

    @Test
    fun `the hizb is derived from the same counter as its quarter`() {
        assertThat(ayah(rub = 1).hizbOfQuarter).isEqualTo(1)
        assertThat(ayah(rub = 4).hizbOfQuarter).isEqualTo(1)
        assertThat(ayah(rub = 5).hizbOfQuarter).isEqualTo(2)
        assertThat(ayah(rub = 240).hizbOfQuarter).isEqualTo(60)
    }

    @Test
    fun `an unknown quarter reports nothing and falls back to the stored hizb`() {
        assertThat(ayah(rub = 0, hizb = 7).quarterInHizb).isEqualTo(0)
        assertThat(ayah(rub = 0, hizb = 7).hizbOfQuarter).isEqualTo(7)
    }

    @Test
    fun `division markers default to absent so an unseeded device shows none`() {
        val plain = ayah(rub = 0)
        assertThat(plain.isRubStart).isFalse()
        assertThat(plain.isRukuStart).isFalse()
        assertThat(plain.rukuNumber).isNull()
    }
}
