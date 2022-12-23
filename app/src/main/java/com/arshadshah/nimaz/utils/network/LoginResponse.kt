package com.arshadshah.nimaz.utils.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
	@SerialName("jwt")
	val token : String ,
						)
