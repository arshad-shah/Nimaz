package com.arshadshah.nimaz.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "zakat_history")
data class ZakatHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val calculatedAt: Long,
    val totalAssets: Double,
    val totalLiabilities: Double,
    val netWorth: Double,
    val zakatDue: Double,
    val nisabType: String,
    val nisabValue: Double,
    val isPaid: Boolean = false,
    val paidAt: Long? = null,
    val notes: String? = null
)
