package com.arshadshah.nimaz.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for the Khatam (Quran completion) progress models: progress
 * percentages with division-by-zero guards, status parsing, and the juz
 * boundary table that underpins all per-juz progress reporting.
 */
class KhatamModelsTest {

    // ── Khatam.progressPercent ──────────────────────────────────────

    @Test
    fun `khatam progress is zero when nothing has been read`() {
        val khatam = Khatam(name = "Test", totalAyahsRead = 0)
        assertThat(khatam.progressPercent).isWithin(1e-6f).of(0f)
    }

    @Test
    fun `khatam progress is one when the whole quran has been read`() {
        val khatam = Khatam(name = "Test", totalAyahsRead = Khatam.TOTAL_QURAN_AYAHS)
        assertThat(khatam.progressPercent).isWithin(1e-6f).of(1f)
    }

    @Test
    fun `khatam progress is the read fraction of total ayahs`() {
        val khatam = Khatam(name = "Test", totalAyahsRead = 3118) // half of 6236
        assertThat(khatam.progressPercent).isWithin(1e-4f).of(0.5f)
    }

    // ── KhatamStatus parsing ────────────────────────────────────────

    @Test
    fun `khatam status parses known values case-insensitively`() {
        assertThat(KhatamStatus.fromString("completed")).isEqualTo(KhatamStatus.COMPLETED)
        assertThat(KhatamStatus.fromString("ABANDONED")).isEqualTo(KhatamStatus.ABANDONED)
        assertThat(KhatamStatus.fromString("Active")).isEqualTo(KhatamStatus.ACTIVE)
    }

    @Test
    fun `khatam status defaults to active for unknown values`() {
        assertThat(KhatamStatus.fromString("")).isEqualTo(KhatamStatus.ACTIVE)
        assertThat(KhatamStatus.fromString("garbage")).isEqualTo(KhatamStatus.ACTIVE)
    }

    @Test
    fun `khatam status round-trips through its db string`() {
        for (status in KhatamStatus.values()) {
            assertThat(KhatamStatus.fromString(status.toDbString())).isEqualTo(status)
        }
    }

    // ── Per-juz / per-surah progress guards ─────────────────────────

    @Test
    fun `juz progress percent is the read fraction`() {
        val info = JuzProgressInfo(juzNumber = 1, totalAyahs = 148, readAyahs = 74)
        assertThat(info.progressPercent).isWithin(1e-4f).of(0.5f)
    }

    @Test
    fun `juz progress percent is zero when total ayahs is zero`() {
        val info = JuzProgressInfo(juzNumber = 1, totalAyahs = 0, readAyahs = 0)
        assertThat(info.progressPercent).isEqualTo(0f)
    }

    @Test
    fun `surah progress percent is zero when total ayahs is zero`() {
        val info = SurahProgressInfo(
            surahNumber = 1, surahName = "Al-Fatiha", totalAyahs = 0, readAyahs = 0
        )
        assertThat(info.progressPercent).isEqualTo(0f)
    }

    // ── Juz boundary table integrity ────────────────────────────────

    @Test
    fun `there are exactly thirty juz ranges`() {
        assertThat(KhatamConstants.JUZ_AYAH_RANGES).hasSize(30)
    }

    @Test
    fun `juz ranges are contiguous and cover all 6236 ayahs without gaps`() {
        val ranges = KhatamConstants.JUZ_AYAH_RANGES
        assertThat(ranges.first().first).isEqualTo(1)
        assertThat(ranges.last().second).isEqualTo(Khatam.TOTAL_QURAN_AYAHS)

        var coveredAyahs = 0
        for (i in ranges.indices) {
            val (start, end) = ranges[i]
            // Each range must be non-empty and well-ordered.
            assertThat(end).isAtLeast(start)
            coveredAyahs += (end - start + 1)
            // Each range must begin exactly one ayah after the previous one ends.
            if (i > 0) {
                assertThat(start).isEqualTo(ranges[i - 1].second + 1)
            }
        }
        assertThat(coveredAyahs).isEqualTo(Khatam.TOTAL_QURAN_AYAHS)
    }
}
