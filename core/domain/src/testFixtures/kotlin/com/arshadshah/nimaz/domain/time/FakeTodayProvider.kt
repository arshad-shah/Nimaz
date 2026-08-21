package com.arshadshah.nimaz.domain.time

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDate

/**
 * A [TodayProvider] whose "today" a test decides, and can move.
 *
 * Set [now] to roll the date over: everything collecting [todayChanges] is told, exactly as it
 * would be at a real midnight, without waiting for one.
 */
class FakeTodayProvider(initial: LocalDate) : TodayProvider {

    private val state = MutableStateFlow(initial)

    var now: LocalDate
        get() = state.value
        set(value) {
            state.value = value
        }

    override fun today(): LocalDate = state.value

    override val todayChanges: Flow<LocalDate> = state
}
