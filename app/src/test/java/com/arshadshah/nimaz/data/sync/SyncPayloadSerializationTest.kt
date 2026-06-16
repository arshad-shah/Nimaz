package com.arshadshah.nimaz.data.sync

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test

/**
 * Serialization round-trip tests for [SyncPayload] — the contract for
 * device-to-device data transfer. If a field stops serializing or the schema
 * drifts, user data (prayers, fasting, Quran progress, tasbih, khatam,
 * tafseer, zakat, preferences) is silently lost or corrupted on transfer.
 *
 * Uses the same `Json { ignoreUnknownKeys = true }` configuration as the
 * production encode/decode path in SyncViewModel.
 */
class SyncPayloadSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun roundTrip(payload: SyncPayload): SyncPayload {
        val encoded = json.encodeToString(SyncPayload.serializer(), payload)
        return json.decodeFromString(SyncPayload.serializer(), encoded)
    }

    /** A payload with every collection populated and all nullable fields set. */
    private fun fullyPopulatedPayload() = SyncPayload(
        exportedAt = 111L,
        appVersion = 11,
        bookmarks = listOf(
            SyncBookmark(
                id = 1, ayahId = 10, surahNumber = 2, ayahNumber = 5,
                note = "a note", color = "#FFAABB", createdAt = 100, updatedAt = 200
            )
        ),
        favorites = listOf(
            SyncFavorite(ayahId = 10, surahNumber = 2, ayahNumber = 5, createdAt = 100, updatedAt = 200)
        ),
        readingProgress = SyncReadingProgress(
            lastReadSurah = 2, lastReadAyah = 5, lastReadPage = 3, lastReadJuz = 1,
            totalAyahsRead = 500, currentKhatmaCount = 2, updatedAt = 200
        ),
        prayerRecords = listOf(
            SyncPrayerRecord(
                id = 1, date = 1000, prayerName = "FAJR", status = "PRAYED",
                prayedAt = 1500, scheduledTime = 900, isJamaah = true,
                isQadaFor = 42, note = "n", createdAt = 100, updatedAt = 200
            )
        ),
        fastRecords = listOf(
            SyncFastRecord(
                id = 1, date = 1000, hijriDate = "1/9/1446", hijriMonth = 9, hijriYear = 1446,
                fastType = "RAMADAN", status = "FASTED", exemptionReason = "TRAVEL",
                suhoorTime = 800, iftarTime = 1900, note = "n", createdAt = 100, updatedAt = 200
            )
        ),
        makeupFasts = listOf(
            SyncMakeupFast(
                id = 1, originalDate = 1000, originalHijriDate = "1/9/1446", reason = "Travel",
                status = "PENDING", completedDate = 5000, fidyaAmount = 12.5,
                note = "n", createdAt = 100, updatedAt = 200
            )
        ),
        tasbihPresets = listOf(
            SyncTasbihPreset(
                id = 1, name = "SubhanAllah", arabic = "ar", transliteration = "tr",
                translation = "tn", targetCount = 33, isCustom = 1, displayOrder = 0, updatedAt = 200
            )
        ),
        tasbihSessions = listOf(
            SyncTasbihSession(
                id = 1, presetId = 2, presetName = "SubhanAllah", date = 1000,
                currentCount = 33, targetCount = 33, totalLaps = 1, isCompleted = true,
                duration = 5000, startedAt = 100, completedAt = 300, note = "n", updatedAt = 400
            )
        ),
        khatams = listOf(
            SyncKhatam(
                id = 1, name = "Ramadan Khatam", notes = "notes", status = "ACTIVE",
                isActive = true, dailyTarget = 20, deadline = 9999, reminderEnabled = true,
                reminderTime = "06:00", totalAyahsRead = 100, createdAt = 100,
                startedAt = 150, completedAt = null, updatedAt = 200
            )
        ),
        khatamAyahs = listOf(SyncKhatamAyah(khatamId = 1, ayahId = 5, readAt = 150, updatedAt = 200)),
        khatamDailyLogs = listOf(SyncKhatamDailyLog(khatamId = 1, date = 1000, ayahsRead = 20, updatedAt = 200)),
        tafseerHighlights = listOf(
            SyncTafseerHighlight(
                id = 1, ayahId = 5, tafseerId = "ibnkathir", startOffset = 0, endOffset = 10,
                color = "#FF0000", note = "n", createdAt = 100, updatedAt = 200
            )
        ),
        tafseerNotes = listOf(
            SyncTafseerNote(
                id = 1, ayahId = 5, tafseerId = "ibnkathir", text = "my note",
                createdAt = 100, updatedAt = 200
            )
        ),
        zakatHistory = listOf(
            SyncZakatHistory(
                id = 1, calculatedAt = 1000, totalAssets = 10_000.0, totalLiabilities = 2_000.0,
                netWorth = 8_000.0, zakatDue = 200.0, nisabType = "GOLD", nisabValue = 5_686.2,
                isPaid = false, paidAt = null, notes = "n", updatedAt = 200
            )
        ),
        preferences = mapOf("calc_method" to "MWL", "theme" to "dark")
    )

    // ── Round-trips ─────────────────────────────────────────────────

    @Test
    fun `a fully populated payload survives an encode-decode round-trip`() {
        val original = fullyPopulatedPayload()
        assertThat(roundTrip(original)).isEqualTo(original)
    }

    @Test
    fun `nullable fields preserve their null values through a round-trip`() {
        val original = fullyPopulatedPayload().copy(
            readingProgress = null,
            bookmarks = listOf(
                SyncBookmark(
                    id = 1, ayahId = 10, surahNumber = 2, ayahNumber = 5,
                    note = null, color = null, createdAt = 100, updatedAt = 200
                )
            ),
            prayerRecords = listOf(
                SyncPrayerRecord(
                    id = 1, date = 1000, prayerName = "FAJR", status = "MISSED",
                    prayedAt = null, scheduledTime = 900, isJamaah = false,
                    isQadaFor = null, note = null, createdAt = 100, updatedAt = 200
                )
            )
        )
        val decoded = roundTrip(original)
        assertThat(decoded).isEqualTo(original)
        assertThat(decoded.readingProgress).isNull()
        assertThat(decoded.bookmarks.first().note).isNull()
        assertThat(decoded.prayerRecords.first().prayedAt).isNull()
    }

    @Test
    fun `an empty default payload round-trips to an equal payload`() {
        val original = SyncPayload(exportedAt = 0L)
        val decoded = roundTrip(original)
        assertThat(decoded).isEqualTo(original)
        assertThat(decoded.bookmarks).isEmpty()
        assertThat(decoded.preferences).isEmpty()
    }

    @Test
    fun `preferences map content is preserved exactly`() {
        val prefs = mapOf("a" to "1", "b" to "two", "empty" to "")
        val decoded = roundTrip(SyncPayload(exportedAt = 0L, preferences = prefs))
        assertThat(decoded.preferences).containsExactlyEntriesIn(prefs)
    }

    // ── Forward / backward compatibility ────────────────────────────

    @Test
    fun `decoding tolerates unknown future fields`() {
        // A payload written by a newer app version may carry fields this
        // version doesn't know about; ignoreUnknownKeys must skip them.
        val futureJson = """{"appVersion":42,"unknownFutureField":"x","bookmarks":[]}"""
        val decoded = json.decodeFromString(SyncPayload.serializer(), futureJson)
        assertThat(decoded.appVersion).isEqualTo(42)
        assertThat(decoded.bookmarks).isEmpty()
    }

    @Test
    fun `decoding a minimal payload fills every collection with defaults`() {
        // An older payload that omits newer sections must still decode, with
        // the missing sections defaulting to empty rather than failing.
        val decoded = json.decodeFromString(SyncPayload.serializer(), "{}")
        assertThat(decoded.appVersion).isEqualTo(0)
        assertThat(decoded.bookmarks).isEmpty()
        assertThat(decoded.favorites).isEmpty()
        assertThat(decoded.readingProgress).isNull()
        assertThat(decoded.prayerRecords).isEmpty()
        assertThat(decoded.fastRecords).isEmpty()
        assertThat(decoded.makeupFasts).isEmpty()
        assertThat(decoded.tasbihPresets).isEmpty()
        assertThat(decoded.tasbihSessions).isEmpty()
        assertThat(decoded.khatams).isEmpty()
        assertThat(decoded.khatamAyahs).isEmpty()
        assertThat(decoded.khatamDailyLogs).isEmpty()
        assertThat(decoded.tafseerHighlights).isEmpty()
        assertThat(decoded.tafseerNotes).isEmpty()
        assertThat(decoded.zakatHistory).isEmpty()
        assertThat(decoded.preferences).isEmpty()
    }
}
