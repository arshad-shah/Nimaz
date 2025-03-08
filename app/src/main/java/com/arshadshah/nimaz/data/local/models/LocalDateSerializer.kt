package com.arshadshah.nimaz.data.local.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDate

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = LocalDate::class)
object LocalDateSerializer {

    override fun serialize(encoder: Encoder, value: LocalDate) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): LocalDate {
        return LocalDate.parse(decoder.decodeString())
    }
}