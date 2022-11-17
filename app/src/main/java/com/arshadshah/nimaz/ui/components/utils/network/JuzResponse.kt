package com.arshadshah.nimaz.ui.components.utils.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JuzResponse(
    @SerialName("juznumberdata")
    val number: Int,
    @SerialName("name")
    val name: String,
    @SerialName("tname")
    val tname: String,
    @SerialName("juzStartAyaInQuran")
    val juzStartAyaInQuran: Int,
)
