package com.arshadshah.nimaz.data.ai

import com.arshadshah.nimaz.data.ai.dto.ApiError
import com.arshadshah.nimaz.data.ai.dto.AskOutput
import com.arshadshah.nimaz.data.ai.dto.InvokeRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

/** Result of an HTTP invocation, already decoded into success/error/transport. */
sealed interface AiHttpResult {
    data class Success(val output: AskOutput) : AiHttpResult
    data class ApiFailure(val status: Int, val body: ApiError?) : AiHttpResult
    data class Transport(val cause: Throwable) : AiHttpResult
}

/**
 * Thin Ktor wrapper over the Nimaz AI Worker. Owns the configured [HttpClient]
 * and the base URL (from BuildConfig). Error mapping to domain [AiError] lives
 * in the repository.
 */
class AiApiClient(
    private val client: HttpClient,
    private val baseUrl: String,
    private val json: Json,
) {
    suspend fun invoke(request: InvokeRequest): AiHttpResult {
        return try {
            val response: HttpResponse =
                client.post("${baseUrl.trimEnd('/')}/v1/invoke") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
            if (response.status.isSuccess()) {
                AiHttpResult.Success(response.body())
            } else {
                val error = runCatching {
                    json.decodeFromString(ApiError.serializer(), response.bodyAsText())
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
