package com.arshadshah.nimaz.domain.repository

import com.arshadshah.nimaz.domain.model.GroundedAnswer
import com.arshadshah.nimaz.domain.model.ProofPassage
import com.arshadshah.nimaz.domain.model.SearchPlan

/**
 * Gateway to the Nimaz AI Worker. The implementation lives in the data layer
 * (Ktor + Play Integrity); presentation/domain depend only on this interface.
 */
interface AiRepository {
    /**
     * Ask the AI to plan retrieval for a question — which local sources to fetch
     * (`search-plan` capability). Returns a [SearchPlan] on success, or a failure
     * carrying an [com.arshadshah.nimaz.domain.model.AiError] via [AiRequestException].
     */
    suspend fun planSearch(question: String): Result<SearchPlan>

    /**
     * Ask a grounded question against the supplied evidence passages. Returns a
     * [GroundedAnswer] on success, or a failure whose exception is an
     * [com.arshadshah.nimaz.domain.model.AiError]-carrying [AiRequestException].
     */
    suspend fun ask(
        question: String,
        passages: List<ProofPassage>,
    ): Result<GroundedAnswer>
}

/** Wraps an [com.arshadshah.nimaz.domain.model.AiError] so it can travel in a [Result.failure]. */
class AiRequestException(
    val error: com.arshadshah.nimaz.domain.model.AiError,
) : Exception()
