package com.arshadshah.nimaz.data.sync

import kotlinx.serialization.Serializable

@Serializable
data class SyncPayload(
    val exportedAt: Long = System.currentTimeMillis(),
    val appVersion: Int = 0,
    // Quran
    val bookmarks: List<SyncBookmark> = emptyList(),
    val favorites: List<SyncFavorite> = emptyList(),
    val readingProgress: SyncReadingProgress? = null,
    // Prayer & Fasting
    val prayerRecords: List<SyncPrayerRecord> = emptyList(),
    val fastRecords: List<SyncFastRecord> = emptyList(),
    val makeupFasts: List<SyncMakeupFast> = emptyList(),
    // Tasbih
    val tasbihPresets: List<SyncTasbihPreset> = emptyList(),
    val tasbihSessions: List<SyncTasbihSession> = emptyList(),
    // Khatam
    val khatams: List<SyncKhatam> = emptyList(),
    val khatamAyahs: List<SyncKhatamAyah> = emptyList(),
    val khatamDailyLogs: List<SyncKhatamDailyLog> = emptyList(),
    // Tafseer
    val tafseerHighlights: List<SyncTafseerHighlight> = emptyList(),
    val tafseerNotes: List<SyncTafseerNote> = emptyList(),
    // Zakat
    val zakatHistory: List<SyncZakatHistory> = emptyList(),
    // Preferences
    val preferences: Map<String, String> = emptyMap()
)

// --- Quran ---

@Serializable
data class SyncBookmark(
    val id: Long,
    val ayahId: Int,
    val surahNumber: Int,
    val ayahNumber: Int,
    val note: String?,
    val color: String?,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class SyncFavorite(
    val ayahId: Int,
    val surahNumber: Int,
    val ayahNumber: Int,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class SyncReadingProgress(
    val lastReadSurah: Int,
    val lastReadAyah: Int,
    val lastReadPage: Int,
    val lastReadJuz: Int,
    val totalAyahsRead: Int,
    val currentKhatmaCount: Int,
    val updatedAt: Long
)

// --- Prayer & Fasting ---

@Serializable
data class SyncPrayerRecord(
    val id: Long,
    val date: Long,
    val prayerName: String,
    val status: String,
    val prayedAt: Long?,
    val scheduledTime: Long,
    val isJamaah: Boolean,
    val isQadaFor: Long?,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class SyncFastRecord(
    val id: Long,
    val date: Long,
    val hijriDate: String?,
    val hijriMonth: Int?,
    val hijriYear: Int?,
    val fastType: String,
    val status: String,
    val exemptionReason: String?,
    val suhoorTime: Long?,
    val iftarTime: Long?,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class SyncMakeupFast(
    val id: Long,
    val originalDate: Long,
    val originalHijriDate: String?,
    val reason: String,
    val status: String,
    val completedDate: Long?,
    val fidyaAmount: Double?,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long
)

// --- Tasbih ---

@Serializable
data class SyncTasbihPreset(
    val id: Long,
    val name: String,
    val arabic: String,
    val transliteration: String,
    val translation: String,
    val targetCount: Int,
    val isCustom: Int,
    val displayOrder: Int,
    val updatedAt: Long
)

@Serializable
data class SyncTasbihSession(
    val id: Long,
    val presetId: Long?,
    val presetName: String?,
    val date: Long,
    val currentCount: Int,
    val targetCount: Int,
    val totalLaps: Int,
    val isCompleted: Boolean,
    val duration: Long?,
    val startedAt: Long,
    val completedAt: Long?,
    val note: String?,
    val updatedAt: Long
)

// --- Khatam ---

@Serializable
data class SyncKhatam(
    val id: Long,
    val name: String,
    val notes: String?,
    val status: String,
    val isActive: Boolean,
    val dailyTarget: Int,
    val deadline: Long?,
    val reminderEnabled: Boolean,
    val reminderTime: String?,
    val totalAyahsRead: Int,
    val createdAt: Long,
    val startedAt: Long?,
    val completedAt: Long?,
    val updatedAt: Long
)

@Serializable
data class SyncKhatamAyah(
    val khatamId: Long,
    val ayahId: Int,
    val readAt: Long,
    val updatedAt: Long
)

@Serializable
data class SyncKhatamDailyLog(
    val khatamId: Long,
    val date: Long,
    val ayahsRead: Int,
    val updatedAt: Long
)

// --- Tafseer ---

@Serializable
data class SyncTafseerHighlight(
    val id: Long,
    val ayahId: Int,
    val tafseerId: String,
    val startOffset: Int,
    val endOffset: Int,
    val color: String,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class SyncTafseerNote(
    val id: Long,
    val ayahId: Int,
    val tafseerId: String,
    val text: String,
    val createdAt: Long,
    val updatedAt: Long
)

// --- Zakat ---

@Serializable
data class SyncZakatHistory(
    val id: Long,
    val calculatedAt: Long,
    val totalAssets: Double,
    val totalLiabilities: Double,
    val netWorth: Double,
    val zakatDue: Double,
    val nisabType: String,
    val nisabValue: Double,
    val isPaid: Boolean,
    val paidAt: Long?,
    val notes: String?,
    val updatedAt: Long
)
