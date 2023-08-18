package com.arshadshah.nimaz.utils.api

import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json


object KtorClient
{

	private val client = HttpClient(Android) {
		//install json serializer
		install(ContentNegotiation) {
			json(
					 json = Json {
						 ignoreUnknownKeys = true
						 isLenient = true
						 allowSpecialFloatingPointValues = true
						 useArrayPolymorphism = true
					 }
				)
		}
	}
	val getInstance = client
}