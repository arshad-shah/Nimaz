package com.arshadshah.nimaz.data.ai

import com.arshadshah.nimaz.data.ai.dto.ApiError
import com.arshadshah.nimaz.data.ai.dto.InvokeRequest
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json

/**
 * Result of an HTTP invocation, already decoded into success/error/transport.
 * Generic over the capability's output type [O].
 */
sealed interface AiHttpResult<out O> {
    data class Success<O>(val output: O) : AiHttpResult<O>
    data class ApiFailure(val status: Int, val body: ApiError?) : AiHttpResult<Nothing>
    data class Transport(val cause: Throwable) : AiHttpResult<Nothing>
}

/**
 * Thin Ktor wrapper over the Nimaz AI Worker. Owns the configured [HttpClient]
 * and the base URL (from BuildConfig). Error mapping to domain [AiError] lives
 * in the repository. Generic over capability output so a single client serves
 * every capability.
 */
class AiApiClient(
    private val client: HttpClient,
    private val baseUrl: String,
    private val json: Json,
) {
    suspend fun <O> invoke(
        request: InvokeRequest,
        outputDeserializer: DeserializationStrategy<O>,
    ): AiHttpResult<O> {
        return try {
            val response: HttpResponse =
                client.post("${baseUrl.trimEnd('/')}/v1/invoke") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
            val text = response.bodyAsText()
            if (response.status.isSuccess()) {
                AiHttpResult.Success(json.decodeFromString(outputDeserializer, text))
            } else {
                val error = runCatching {
                    json.decodeFromString(ApiError.serializer(), text)
                }.getOrNull()
                AiHttpResult.ApiFailure(response.status.value, error)
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e // never swallow structured-concurrency cancellation
        } catch (e: Exception) {
            AiHttpResult.Transport(e)
        }
    }
}
