package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.data.ai.AiApiClient
import com.arshadshah.nimaz.data.ai.AiHttpResult
import com.arshadshah.nimaz.data.ai.DeviceIdProvider
import com.arshadshah.nimaz.data.ai.IntegrityTokenProvider
import com.arshadshah.nimaz.data.ai.dto.AskInput
import com.arshadshah.nimaz.data.ai.dto.AskOutput
import com.arshadshah.nimaz.data.ai.dto.InvokeRequest
import com.arshadshah.nimaz.data.ai.dto.PassageDto
import com.arshadshah.nimaz.data.ai.dto.SearchPlanInput
import com.arshadshah.nimaz.data.ai.dto.SearchPlanOutput
import com.arshadshah.nimaz.domain.model.AiError
import com.arshadshah.nimaz.domain.model.AnswerConfidence
import com.arshadshah.nimaz.domain.model.CitationId
import com.arshadshah.nimaz.domain.model.GroundedAnswer
import com.arshadshah.nimaz.domain.model.ProofPassage
import com.arshadshah.nimaz.domain.model.SearchPlan
import com.arshadshah.nimaz.domain.repository.AiRepository
import com.arshadshah.nimaz.domain.repository.AiRequestException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
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

    override suspend fun planSearch(question: String): Result<SearchPlan> {
        val request = envelope(
            capability = CAPABILITY_SEARCH_PLAN,
            input = json.encodeToJsonElement(
                SearchPlanInput.serializer(),
                SearchPlanInput(question = question),
            ),
        )
        return when (val result = apiClient.invoke(request, SearchPlanOutput.serializer())) {
            is AiHttpResult.Success -> Result.success(result.output.toDomain())
            is AiHttpResult.Transport -> failure(AiError.Network)
            is AiHttpResult.ApiFailure ->
                failure(mapApiError(result.status, result.body?.error?.code, result.body?.error?.retryAfterSeconds))
        }
    }

    override suspend fun ask(
        question: String,
        passages: List<ProofPassage>,
    ): Result<GroundedAnswer> {
        val request = envelope(
            capability = CAPABILITY_ASK,
            input = json.encodeToJsonElement(
                AskInput.serializer(),
                AskInput(
                    question = question,
                    passages = passages.map {
                        PassageDto(
                            id = it.id,
                            source = it.source.wire,
                            text = it.text,
                            meta = it.meta,
                        )
                    },
                ),
            ),
        )

        return when (val result = apiClient.invoke(request, AskOutput.serializer())) {
            is AiHttpResult.Success -> Result.success(result.output.toDomain())
            is AiHttpResult.Transport -> failure(AiError.Network)
            is AiHttpResult.ApiFailure ->
                failure(mapApiError(result.status, result.body?.error?.code, result.body?.error?.retryAfterSeconds))
        }
    }

    private suspend fun envelope(capability: String, input: JsonElement) = InvokeRequest(
        capability = capability,
        integrityToken = integrityTokenProvider.getToken(),
        deviceId = deviceIdProvider.getOrCreate(),
        input = input,
    )

    private fun mapApiError(
        status: Int,
        code: String?,
        retryAfterSeconds: Long?,
    ): AiError = when (code) {
        "RATE_LIMITED" -> AiError.RateLimited(retryAfterSeconds)
        "BUDGET_EXCEEDED" -> AiError.BudgetExceeded
        "ATTESTATION_FAILED" -> AiError.Attestation
        "INVALID_INPUT" -> AiError.Invalid("The question could not be processed.")
        "UPSTREAM_ERROR" -> AiError.Unknown
        else -> when (status) {
            401 -> AiError.Attestation
            429 -> AiError.RateLimited(retryAfterSeconds)
            503 -> AiError.BudgetExceeded
            else -> AiError.Unknown
        }
    }

    private fun <T> failure(error: AiError): Result<T> =
        Result.failure(AiRequestException(error))

    private fun AskOutput.toDomain(): GroundedAnswer =
        GroundedAnswer(
            answer = answer,
            citationIds = citationIds,
            confidence = when (confidence.lowercase()) {
                "high" -> AnswerConfidence.HIGH
                "medium" -> AnswerConfidence.MEDIUM
                else -> AnswerConfidence.LOW
            },
            insufficientEvidence = insufficientEvidence,
        )

    private fun SearchPlanOutput.toDomain(): SearchPlan =
        SearchPlan(
            terms = terms.map { it.trim() }.filter { it.isNotBlank() }.distinct(),
            // Reuse the strict citation grammar: "2:153" -> quran:2:153 -> Quran(2, 153).
            // Malformed refs resolve to null and are dropped.
            quranRefs = quranRefs.mapNotNull { CitationId.parse("quran:${it.trim()}") as? CitationId.Quran },
        )

    companion object {
        private const val CAPABILITY_ASK = "ask-with-proof"
        private const val CAPABILITY_SEARCH_PLAN = "search-plan"
    }
}
