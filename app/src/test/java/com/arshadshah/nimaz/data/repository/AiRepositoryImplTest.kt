package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.data.ai.AiApiClient
import com.arshadshah.nimaz.data.ai.DeviceIdProvider
import com.arshadshah.nimaz.data.ai.IntegrityTokenProvider
import com.arshadshah.nimaz.domain.model.AiError
import com.arshadshah.nimaz.domain.model.AnswerConfidence
import com.arshadshah.nimaz.domain.model.ProofPassage
import com.arshadshah.nimaz.domain.model.ProofSource
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

    private val passages = listOf(
        ProofPassage("quran:2:153", ProofSource.QURAN, "Be patient.", "Al-Baqarah 153"),
    )

    private fun jsonHeaders() = headersOf(HttpHeaders.ContentType, "application/json")

    @Test
    fun `success maps to a grounded answer with confidence`() = runTest {
        val body = """
            {"answer":"Patience is virtuous.","citationIds":["quran:2:153"],
             "confidence":"medium","insufficientEvidence":false}
        """.trimIndent()
        val result = repo { respond(body, HttpStatusCode.OK, jsonHeaders()) }
            .ask("q?", passages)

        assertThat(result.isSuccess).isTrue()
        val answer = result.getOrThrow()
        assertThat(answer.confidence).isEqualTo(AnswerConfidence.MEDIUM)
        assertThat(answer.citationIds).containsExactly("quran:2:153")
    }

    @Test
    fun `planSearch maps terms and parses quran refs, dropping malformed ones`() = runTest {
        val body = """
            {"terms":["patience"," sabr ",""],"quranRefs":["2:153","garbage","39:10"]}
        """.trimIndent()
        val result = repo { respond(body, HttpStatusCode.OK, jsonHeaders()) }
            .planSearch("How do I show patience?")

        assertThat(result.isSuccess).isTrue()
        val plan = result.getOrThrow()
        assertThat(plan.terms).containsExactly("patience", "sabr")
        assertThat(plan.quranRefs.map { it.raw })
            .containsExactly("quran:2:153", "quran:39:10")
    }

    @Test
    fun `planSearch surfaces api errors`() = runTest {
        val body = """{"error":{"code":"RATE_LIMITED","message":"slow","retryAfterSeconds":60}}"""
        val result = repo { respond(body, HttpStatusCode.TooManyRequests, jsonHeaders()) }
            .planSearch("q?")
        assertThat((result.exceptionOrNull() as AiRequestException).error)
            .isEqualTo(AiError.RateLimited(60))
    }

    @Test
    fun `429 maps to RateLimited with retryAfterSeconds`() = runTest {
        val body = """
            {"error":{"code":"RATE_LIMITED","message":"slow down","retryAfterSeconds":3600}}
        """.trimIndent()
        val result = repo { respond(body, HttpStatusCode.TooManyRequests, jsonHeaders()) }
            .ask("q?", passages)

        val error = (result.exceptionOrNull() as AiRequestException).error
        assertThat(error).isEqualTo(AiError.RateLimited(3600))
    }

    @Test
    fun `503 maps to BudgetExceeded`() = runTest {
        val body = """{"error":{"code":"BUDGET_EXCEEDED","message":"resting"}}"""
        val result = repo { respond(body, HttpStatusCode.ServiceUnavailable, jsonHeaders()) }
            .ask("q?", passages)
        assertThat((result.exceptionOrNull() as AiRequestException).error)
            .isEqualTo(AiError.BudgetExceeded)
    }

    @Test
    fun `401 maps to Attestation`() = runTest {
        val body = """{"error":{"code":"ATTESTATION_FAILED","message":"no"}}"""
        val result = repo { respond(body, HttpStatusCode.Unauthorized, jsonHeaders()) }
            .ask("q?", passages)
        assertThat((result.exceptionOrNull() as AiRequestException).error)
            .isEqualTo(AiError.Attestation)
    }

    @Test
    fun `400 maps to Invalid`() = runTest {
        val body = """{"error":{"code":"INVALID_INPUT","message":"bad"}}"""
        val result = repo { respond(body, HttpStatusCode.BadRequest, jsonHeaders()) }
            .ask("q?", passages)
        assertThat((result.exceptionOrNull() as AiRequestException).error)
            .isInstanceOf(AiError.Invalid::class.java)
    }

    @Test
    fun `502 upstream maps to Unknown`() = runTest {
        val body = """{"error":{"code":"UPSTREAM_ERROR","message":"boom"}}"""
        val result = repo { respond(body, HttpStatusCode.BadGateway, jsonHeaders()) }
            .ask("q?", passages)
        assertThat((result.exceptionOrNull() as AiRequestException).error)
            .isEqualTo(AiError.Unknown)
    }

    @Test
    fun `transport failure maps to Network`() = runTest {
        val result = repo { throw IOException("offline") }.ask("q?", passages)
        assertThat((result.exceptionOrNull() as AiRequestException).error)
            .isEqualTo(AiError.Network)
    }
}
