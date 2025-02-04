package com.arshadshah.nimaz.data.local.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "Tafsir",
    foreignKeys = [
        ForeignKey(
            entity = LocalAya::class,
            parentColumns = ["ayaNumberInQuran"],
            childColumns = ["ayaNumberInQuran"]
        )
    ],
)
data class Tafsir(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val ayaNumberInQuran: Int,
    val editionId: Int,
    val content: String,
    val language: String
)