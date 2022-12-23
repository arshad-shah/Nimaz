package com.arshadshah.nimaz.utils.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QiblaDirectionResponse(
	@SerialName("bearing")
	val bearing : Double ,
								 )