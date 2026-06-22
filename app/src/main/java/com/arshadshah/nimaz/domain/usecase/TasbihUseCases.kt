package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.TasbihPreset
import com.arshadshah.nimaz.domain.model.TasbihSession
import com.arshadshah.nimaz.domain.model.TasbihStats
import com.arshadshah.nimaz.domain.repository.TasbihRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

data class TasbihUseCases(
    val getDefaultPresets: GetDefaultPresetsUseCase,
    val getCustomPresets: GetCustomPresetsUseCase,
    val getPresetById: GetPresetByIdUseCase,
    val insertPreset: InsertPresetUseCase,
    val updatePreset: UpdatePresetUseCase,
    val deleteCustomPreset: DeleteCustomPresetUseCase,
    val seedMissingDefaults: SeedMissingDefaultsUseCase,
    val getSessionsForDate: GetSessionsForDateUseCase,
    val getSessionsInRange: GetSessionsInRangeUseCase,
    val getActiveSession: GetActiveSessionUseCase,
    val getSessionById: GetSessionByIdUseCase,
    val insertSession: InsertSessionUseCase,
    val updateSessionCount: UpdateSessionCountUseCase,
    val completeSession: CompleteSessionUseCase,
    val getTasbihStats: GetTasbihStatsUseCase,
    val getTotalCountInRange: GetTotalCountInRangeUseCase,
    val getCompletedSessionsInRange: GetCompletedSessionsInRangeUseCase
)

class GetDefaultPresetsUseCase @Inject constructor(private val repository: TasbihRepository) {
    operator fun invoke(): Flow<List<TasbihPreset>> = repository.getDefaultPresets()
}

class GetCustomPresetsUseCase @Inject constructor(private val repository: TasbihRepository) {
    operator fun invoke(): Flow<List<TasbihPreset>> = repository.getCustomPresets()
}

class GetPresetByIdUseCase @Inject constructor(private val repository: TasbihRepository) {
    suspend operator fun invoke(id: Long): TasbihPreset? = repository.getPresetById(id)
}

class InsertPresetUseCase @Inject constructor(private val repository: TasbihRepository) {
    suspend operator fun invoke(preset: TasbihPreset): Long = repository.insertPreset(preset)
}

class UpdatePresetUseCase @Inject constructor(private val repository: TasbihRepository) {
    suspend operator fun invoke(preset: TasbihPreset) = repository.updatePreset(preset)
}

class DeleteCustomPresetUseCase @Inject constructor(private val repository: TasbihRepository) {
    suspend operator fun invoke(id: Long) = repository.deleteCustomPreset(id)
}

class SeedMissingDefaultsUseCase @Inject constructor(private val repository: TasbihRepository) {
    suspend operator fun invoke() = repository.seedMissingDefaults()
}

class GetSessionsForDateUseCase @Inject constructor(private val repository: TasbihRepository) {
    operator fun invoke(date: Long): Flow<List<TasbihSession>> = repository.getSessionsForDate(date)
}

class GetSessionsInRangeUseCase @Inject constructor(private val repository: TasbihRepository) {
    operator fun invoke(startDate: Long, endDate: Long): Flow<List<TasbihSession>> =
        repository.getSessionsInRange(startDate, endDate)
}

class GetActiveSessionUseCase @Inject constructor(private val repository: TasbihRepository) {
    suspend operator fun invoke(): TasbihSession? = repository.getActiveSession()
}

class GetSessionByIdUseCase @Inject constructor(private val repository: TasbihRepository) {
    suspend operator fun invoke(id: Long): TasbihSession? = repository.getSessionById(id)
}

class InsertSessionUseCase @Inject constructor(private val repository: TasbihRepository) {
    suspend operator fun invoke(session: TasbihSession): Long = repository.insertSession(session)
}

class UpdateSessionCountUseCase @Inject constructor(private val repository: TasbihRepository) {
    suspend operator fun invoke(id: Long, count: Int, laps: Int) =
        repository.updateSessionCount(id, count, laps)
}

class CompleteSessionUseCase @Inject constructor(private val repository: TasbihRepository) {
    suspend operator fun invoke(id: Long, completedAt: Long, duration: Long) =
        repository.completeSession(id, completedAt, duration)
}

class GetTasbihStatsUseCase @Inject constructor(private val repository: TasbihRepository) {
    suspend operator fun invoke(startDate: Long, endDate: Long): TasbihStats =
        repository.getTasbihStats(startDate, endDate)
}

class GetTotalCountInRangeUseCase @Inject constructor(private val repository: TasbihRepository) {
    suspend operator fun invoke(startDate: Long, endDate: Long): Int =
        repository.getTotalCountInRange(startDate, endDate)
}

class GetCompletedSessionsInRangeUseCase @Inject constructor(private val repository: TasbihRepository) {
    suspend operator fun invoke(startDate: Long, endDate: Long): Int =
        repository.getCompletedSessionsInRange(startDate, endDate)
}
