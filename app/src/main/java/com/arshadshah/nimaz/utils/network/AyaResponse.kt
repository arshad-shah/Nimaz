package com.arshadshah.nimaz.utils.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AyaResponse(
    @SerialName("ayaNumber")
    val number: Int,
    @SerialName("ayaArabic")
    val arabic: String,
    @SerialName("ayaTranslation")
    val translation: String,
)
