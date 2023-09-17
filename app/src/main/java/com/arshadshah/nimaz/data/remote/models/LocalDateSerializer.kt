package com.arshadshah.nimaz.data.remote.models

import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDate

@Serializer(forClass = LocalDate::class)
object LocalDateSerializer
{
	override fun serialize(encoder: Encoder , date: LocalDate)
	{
		encoder.encodeString(date.toString())
	}

	override fun deserialize(decoder : Decoder) : LocalDate
	{
		return LocalDate.parse(decoder.decodeString())
	}
}