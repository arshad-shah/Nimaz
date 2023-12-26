package com.arshadshah.nimaz.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.arshadshah.nimaz.data.remote.models.LocalDateSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Entity(tableName = "Tasbih")
@Serializable
data class LocalTasbih(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate = LocalDate.now(),
    val arabicName: String,
    val englishName: String,
    val translationName: String,
    val goal: Int = 0,
    val count: Int = 0,
)
