package com.arshadshah.nimaz.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "TafsirEditions")
data class TafsirEdition(
    @PrimaryKey
    val id: Int,
    val author: String,
    val name: String,
    val language: String,
    val source: String
)