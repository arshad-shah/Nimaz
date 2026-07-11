package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.data.ai.AiApiClient
import com.arshadshah.nimaz.data.ai.AiHttpResult
import com.arshadshah.nimaz.data.ai.DeviceIdProvider
import com.arshadshah.nimaz.data.ai.IntegrityTokenProvider
import com.arshadshah.nimaz.data.ai.dto.AskInput
import com.arshadshah.nimaz.data.ai.dto.InvokeRequest
import com.arshadshah.nimaz.data.ai.dto.PassageDto
import com.arshadshah.nimaz.domain.model.AiError
import com.arshadshah.nimaz.domain.model.AnswerConfidence
import com.arshadshah.nimaz.domain.model.GroundedAnswer
import com.arshadshah.nimaz.domain.model.ProofPassage
import com.arshadshah.nimaz.domain.repository.AiRepository
import com.arshadshah.nimaz.domain.repository.AiRequestException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiRepositoryImpl @Inject constructor(
    private val apiClient: AiApiClient,
    private val integrityTokenProvider: IntegrityTokenProvider,
    private val deviceIdProvider: DeviceIdProvider,
) : AiRepository {

    override suspend fun ask(
        question: String,
        passages: List<ProofPassage>,
    ): Result<GroundedAnswer> {
        val request = InvokeRequest(
            capability = CAPABILITY_ID,
            integrityToken = integrityTokenProvider.getToken(),
            deviceId = deviceIdProvider.getOrCreate(),
            input = AskInput(
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
        )

        return when (val result = apiClient.invoke(request)) {
            is AiHttpResult.Success -> Result.success(result.output.toDomain())
            is AiHttpResult.Transport -> failure(AiError.Network)
            is AiHttpResult.ApiFailure -> failure(mapApiError(result.status, result.body?.error?.code, result.body?.error?.retryAfterSeconds))
        }
    }

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

    private fun failure(error: AiError): Result<GroundedAnswer> =
        Result.failure(AiRequestException(error))

    private fun com.arshadshah.nimaz.data.ai.dto.AskOutput.toDomain(): GroundedAnswer =
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

    companion object {
        private const val CAPABILITY_ID = "ask-with-proof"
    }
}
