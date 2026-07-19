package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.core.monitoring.CrashReporter
import com.arshadshah.nimaz.data.ai.AiApiClient
import com.arshadshah.nimaz.data.ai.AiHttpResult
import com.arshadshah.nimaz.data.ai.DeviceIdProvider
import com.arshadshah.nimaz.data.ai.IntegrityTokenProvider
import com.arshadshah.nimaz.data.ai.dto.AssistInput
import com.arshadshah.nimaz.data.ai.dto.AssistOutput
import com.arshadshah.nimaz.data.ai.dto.InvokeRequest
import com.arshadshah.nimaz.domain.model.AiError
import com.arshadshah.nimaz.domain.model.AnswerConfidence
import com.arshadshah.nimaz.domain.model.CitationId
import com.arshadshah.nimaz.domain.model.HadithRef
import com.arshadshah.nimaz.domain.model.SearchAssist
import com.arshadshah.nimaz.domain.repository.AiRepository
import com.arshadshah.nimaz.domain.repository.AiRequestException
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class AiRepositoryImpl @Inject constructor(
    private val apiClient: AiApiClient,
    private val integrityTokenProvider: IntegrityTokenProvider,
    private val deviceIdProvider: DeviceIdProvider,
    @param:Named("aiJson") private val json: Json,
) : AiRepository {

    override suspend fun assist(question: String): Result<SearchAssist> {
        val request = InvokeRequest(
            capability = CAPABILITY_SEARCH_ASSIST,
            // Best-effort: an unavailable token is sent empty and the Worker
            // fails open (verification "unavailable") — only an explicit
            // failed Play Integrity verdict is rejected.
            integrityToken = integrityTokenProvider.getToken(),
            deviceId = deviceIdProvider.getOrCreate(),
            input = json.encodeToJsonElement(
                AssistInput.serializer(),
                AssistInput(question = question),
            ),
        )
        return when (val result = apiClient.invoke(request, AssistOutput.serializer())) {
            is AiHttpResult.Success -> Result.success(result.output.toDomain())
            is AiHttpResult.Transport -> {
                // The throwable is otherwise collapsed into AiError.Network and lost.
                CrashReporter.recordException(result.cause)
                failure(AiError.Network)
            }

            is AiHttpResult.ApiFailure ->
                failure(
                    mapApiError(
                        result.status,
                        result.body?.error?.code,
                        result.body?.error?.retryAfterSeconds,
                    ),
                )
        }
    }

    private fun mapApiError(
        status: Int,
        code: String?,
        retryAfterSeconds: Long?,
    ): AiError = when (code) {
        "RATE_LIMITED" -> AiError.RateLimited(retryAfterSeconds)
        "ATTESTATION_FAILED" -> AiError.Unverified
        "BUDGET_EXCEEDED" -> AiError.BudgetExceeded
        "INVALID_INPUT" -> AiError.Invalid("The question could not be processed.")
        "UPSTREAM_ERROR" -> AiError.Unknown
        else -> when (status) {
            403 -> AiError.Unverified
            429 -> AiError.RateLimited(retryAfterSeconds)
            503 -> AiError.BudgetExceeded
            else -> AiError.Unknown
        }
    }

    private fun <T> failure(error: AiError): Result<T> =
        Result.failure(AiRequestException(error))

    private fun AssistOutput.toDomain(): SearchAssist =
        SearchAssist(
            answer = answer.trim(),
            // Reuse the strict citation grammar: "2:153" -> quran:2:153 ->
            // Quran(2, 153). Malformed refs resolve to null and are dropped.
            quranRefs = quranRefs.mapNotNull {
                CitationId.parse("quran:${it.trim()}") as? CitationId.Quran
            }.distinct(),
            // Same strictness for "bukhari:6018"-style hadith refs.
            hadithRefs = hadithRefs.mapNotNull(HadithRef::parse).distinct(),
            terms = terms.map { it.trim() }.filter { it.isNotBlank() }.distinct(),
            confidence = when (confidence.lowercase()) {
                "high" -> AnswerConfidence.HIGH
                "medium" -> AnswerConfidence.MEDIUM
                else -> AnswerConfidence.LOW
            },
        )

    companion object {
        private const val CAPABILITY_SEARCH_ASSIST = "search-assist"
    }
}
