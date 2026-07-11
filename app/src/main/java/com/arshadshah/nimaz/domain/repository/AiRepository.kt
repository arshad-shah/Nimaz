package com.arshadshah.nimaz.domain.repository

import com.arshadshah.nimaz.domain.model.SearchAssist

/**
 * Gateway to the Nimaz AI Worker. The implementation lives in the data layer
 * (Ktor + Play Integrity); presentation/domain depend only on this interface.
 */
interface AiRepository {
    /**
     * The single AI call (`search-assist`): send the question, get back the
     * answer + supporting Quran references + local search terms. Only the
     * question text ever leaves the device. Failures carry an
     * [com.arshadshah.nimaz.domain.model.AiError] via [AiRequestException].
     */
    suspend fun assist(question: String): Result<SearchAssist>
}

/** Wraps an [com.arshadshah.nimaz.domain.model.AiError] so it can travel in a [Result.failure]. */
class AiRequestException(
    val error: com.arshadshah.nimaz.domain.model.AiError,
) : Exception()
