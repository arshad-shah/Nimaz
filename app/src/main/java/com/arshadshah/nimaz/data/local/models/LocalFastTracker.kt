package com.arshadshah.nimaz.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.arshadshah.nimaz.data.remote.models.LocalDateSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Entity(tableName = "FastTracker")
@Serializable
data class LocalFastTracker(
    @PrimaryKey
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate = LocalDate.now(),
    val isFasting: Boolean = false,
    val isMenstruating: Boolean = false,
)
