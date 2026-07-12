package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.data.ai.AiApiClient
import com.arshadshah.nimaz.data.ai.DeviceIdProvider
import com.arshadshah.nimaz.data.ai.IntegrityTokenProvider
import com.arshadshah.nimaz.domain.model.AiError
import com.arshadshah.nimaz.domain.model.AnswerConfidence
import com.arshadshah.nimaz.domain.repository.AiRequestException
import com.google.common.truth.Truth.assertThat
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Test
import java.io.IOException

class AiRepositoryImplTest {

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    private fun repo(
        handler: suspend io.ktor.client.engine.mock.MockRequestHandleScope.(
            io.ktor.client.request.HttpRequestData,
        ) -> io.ktor.client.request.HttpResponseData,
    ): AiRepositoryImpl {
        val client = HttpClient(MockEngine(handler)) {
            expectSuccess = false
            install(ContentNegotiation) { json(json) }
        }
        val apiClient = AiApiClient(client, "https://example.workers.dev", json)
        val integrity = mockk<IntegrityTokenProvider>()
        coEvery { integrity.getToken() } returns "debug-skip"
        val device = mockk<DeviceIdProvider>()
        coEvery { device.getOrCreate() } returns "dev-1"
        return AiRepositoryImpl(apiClient, integrity, device, json)
    }

    private fun jsonHeaders() = headersOf(HttpHeaders.ContentType, "application/json")

    @Test
    fun `success maps answer, terms, refs and confidence`() = runTest {
        val body = """
            {"answer":" Patience is virtuous. ",
             "quranRefs":["2:153","garbage","39:10","2:153"],
             "hadithRefs":["bukhari:6018","Muslim:2999","not-a-ref","bukhari:6018","bukhari:0"],
             "terms":["patience"," sabr ","","patience"],
             "confidence":"medium"}
        """.trimIndent()
        val result = repo { respond(body, HttpStatusCode.OK, jsonHeaders()) }
            .assist("How do I show patience?")

        assertThat(result.isSuccess).isTrue()
        val assist = result.getOrThrow()
        assertThat(assist.answer).isEqualTo("Patience is virtuous.")
        assertThat(assist.confidence).isEqualTo(AnswerConfidence.MEDIUM)
        // Malformed refs and blank/duplicate terms are dropped.
        assertThat(assist.quranRefs.map { it.raw })
            .containsExactly("quran:2:153", "quran:39:10")
        assertThat(assist.hadithRefs.map { it.reference })
            .containsExactly("bukhari:6018", "muslim:2999")
        assertThat(assist.terms).containsExactly("patience", "sabr")
    }

    @Test
    fun `a response without hadithRefs (older Worker) still parses`() = runTest {
        val body = """
            {"answer":"Patience is virtuous.",
             "quranRefs":["2:153"],
             "terms":["patience"],
             "confidence":"high"}
        """.trimIndent()
        val result = repo { respond(body, HttpStatusCode.OK, jsonHeaders()) }
            .assist("q?")

        assertThat(result.getOrThrow().hadithRefs).isEmpty()
    }

    @Test
    fun `429 maps to RateLimited with retryAfterSeconds`() = runTest {
        val body = """
            {"error":{"code":"RATE_LIMITED","message":"slow down","retryAfterSeconds":3600}}
        """.trimIndent()
        val result = repo { respond(body, HttpStatusCode.TooManyRequests, jsonHeaders()) }
            .assist("q?")

        val error = (result.exceptionOrNull() as AiRequestException).error
        assertThat(error).isEqualTo(AiError.RateLimited(3600))
    }

    @Test
    fun `403 maps to Unverified`() = runTest {
        val body = """{"error":{"code":"ATTESTATION_FAILED","message":"not verified"}}"""
        val result = repo { respond(body, HttpStatusCode.Forbidden, jsonHeaders()) }
            .assist("q?")
        assertThat((result.exceptionOrNull() as AiRequestException).error)
            .isEqualTo(AiError.Unverified)
    }

    @Test
    fun `503 maps to BudgetExceeded`() = runTest {
        val body = """{"error":{"code":"BUDGET_EXCEEDED","message":"resting"}}"""
        val result = repo { respond(body, HttpStatusCode.ServiceUnavailable, jsonHeaders()) }
            .assist("q?")
        assertThat((result.exceptionOrNull() as AiRequestException).error)
            .isEqualTo(AiError.BudgetExceeded)
    }

    @Test
    fun `400 maps to Invalid`() = runTest {
        val body = """{"error":{"code":"INVALID_INPUT","message":"bad"}}"""
        val result = repo { respond(body, HttpStatusCode.BadRequest, jsonHeaders()) }
            .assist("q?")
        assertThat((result.exceptionOrNull() as AiRequestException).error)
            .isInstanceOf(AiError.Invalid::class.java)
    }

    @Test
    fun `502 upstream maps to Unknown`() = runTest {
        val body = """{"error":{"code":"UPSTREAM_ERROR","message":"boom"}}"""
        val result = repo { respond(body, HttpStatusCode.BadGateway, jsonHeaders()) }
            .assist("q?")
        assertThat((result.exceptionOrNull() as AiRequestException).error)
            .isEqualTo(AiError.Unknown)
    }

    @Test
    fun `unrecognized error code falls back to the http status`() = runTest {
        // An old client talking to a newer Worker (or vice versa) must degrade
        // gracefully: unknown code + 429 still reads as RateLimited.
        val body = """{"error":{"code":"SOMETHING_NEW","message":"?","retryAfterSeconds":10}}"""
        val result = repo { respond(body, HttpStatusCode.TooManyRequests, jsonHeaders()) }
            .assist("q?")
        assertThat((result.exceptionOrNull() as AiRequestException).error)
            .isEqualTo(AiError.RateLimited(10))
    }

    @Test
    fun `transport failure maps to Network`() = runTest {
        val result = repo { throw IOException("offline") }.assist("q?")
        assertThat((result.exceptionOrNull() as AiRequestException).error)
            .isEqualTo(AiError.Network)
    }
}
