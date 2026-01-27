package com.arshadshah.nimaz.data.local.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "prayer_records",
    indices = [
        Index(value = ["date"]),
        Index(value = ["prayerName"]),
        Index(value = ["date", "prayerName"], unique = true)
    ]
)
data class PrayerRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: Long, // Date in millis (start of day)
    val prayerName: String, // "fajr", "dhuhr", "asr", "maghrib", "isha"
    val status: String, // "prayed", "missed", "qada", "pending"
    val prayedAt: Long?, // Actual time prayed
    val scheduledTime: Long, // Scheduled prayer time
    val isJamaah: Boolean = false, // Prayed in congregation
    val isQadaFor: Long? = null, // If this is a makeup prayer, reference to original date
    val note: String?,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "fast_records",
    indices = [
        Index(value = ["date"], unique = true),
        Index(value = ["hijriMonth"]),
        Index(value = ["fastType"])
    ]
)
data class FastRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: Long, // Date in millis (start of day)
    val hijriDate: String?, // Hijri date string
    val hijriMonth: Int?, // Hijri month number (1-12)
    val hijriYear: Int?,
    val fastType: String, // "ramadan", "voluntary", "makeup", "expiation", "vow"
    val status: String, // "fasted", "not_fasted", "exempted", "makeup_due"
    val exemptionReason: String?, // "travel", "illness", "menstruation", etc.
    val suhoorTime: Long?, // Time of suhoor
    val iftarTime: Long?, // Time of iftar
    val note: String?,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "makeup_fasts",
    indices = [Index(value = ["originalDate"]), Index(value = ["status"])]
)
data class MakeupFastEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalDate: Long, // Original date that was missed
    val originalHijriDate: String?,
    val reason: String, // Why fast was missed
    val status: String, // "pending", "completed", "fidya_paid"
    val completedDate: Long?, // When makeup fast was completed
    val fidyaAmount: Double?, // Amount paid as fidya if applicable
    val note: String?,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
