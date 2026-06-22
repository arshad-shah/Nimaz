package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.IslamicEvent
import com.arshadshah.nimaz.domain.repository.IslamicEventRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

data class IslamicEventUseCases(
    val getAllEvents: GetAllIslamicEventsUseCase
)

class GetAllIslamicEventsUseCase @Inject constructor(private val repository: IslamicEventRepository) {
    operator fun invoke(): Flow<List<IslamicEvent>> = repository.getAllEvents()
}
