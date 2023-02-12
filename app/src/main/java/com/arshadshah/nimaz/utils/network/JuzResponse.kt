package com.arshadshah.nimaz.utils.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JuzResponse(
	@SerialName("juzNumber")
	val number : Int ,
	@SerialName("name")
	val name : String ,
	@SerialName("tname")
	val tname : String ,
	@SerialName("juzStartAyaInQuran")
	val juzStartAyaInQuran : Int ,
					  )
