package com.arshadshah.nimaz.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class AyahReferenceTest {

    @Test
    fun `a named reference reads name then colon-separated numbers`() {
        val ref = AyahReference(surahNumber = 18, ayahNumber = 54, surahName = "Al-Kahf")
        assertThat(ref.format()).isEqualTo("Al-Kahf 18:54")
    }

    @Test
    fun `an unnamed reference still carries both numbers`() {
        val ref = AyahReference(surahNumber = 1, ayahNumber = 3, surahName = null)
        assertThat(ref.format()).isEqualTo("1:3")
    }

    @Test
    fun `a blank name is treated as no name`() {
        val ref = AyahReference(surahNumber = 1, ayahNumber = 3, surahName = "   ")
        assertThat(ref.format()).isEqualTo("1:3")
    }

    @Test
    fun `the name is not repeated when it already looks like a reference`() {
        val ref = AyahReference(surahNumber = 2, ayahNumber = 45, surahName = "Al-Baqara")
        assertThat(ref.format()).doesNotContain("Surah")
        assertThat(ref.format()).doesNotContain("Verse")
    }

    @Test
    fun `a surah number outside 1 to 114 is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            AyahReference(surahNumber = 115, ayahNumber = 1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            AyahReference(surahNumber = 0, ayahNumber = 1)
        }
    }

    @Test
    fun `an ayah number below 1 is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            AyahReference(surahNumber = 1, ayahNumber = 0)
        }
    }
}
