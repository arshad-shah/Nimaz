package com.arshadshah.nimaz.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "QuickJump")
data class QuickJump(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val surahNumber: Int,
    val ayaNumberInSurah: Int,
    val color: String = "#FF6B35",
    val createdAt: Long = System.currentTimeMillis()
)