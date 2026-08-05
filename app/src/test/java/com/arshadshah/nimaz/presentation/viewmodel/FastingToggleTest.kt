package com.arshadshah.nimaz.presentation.viewmodel

import com.arshadshah.nimaz.domain.model.FastRecord
import com.arshadshah.nimaz.domain.model.FastStatus
import com.arshadshah.nimaz.domain.model.FastType
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

/**
 * `FastStatus` has **four** values, and `toggleTodayFast()` handled two of them with a
 * silent `else -> {}`. Marking a day exempted (menstruation, illness, travel) and then
 * using the one-tap control did nothing at all — no change, no error, no feedback.
 *
 * The decision taken was that an exemption is a considered state recorded in the day
 * sheet *with a reason attached*, so a single tap must not silently overwrite it. The
 * control is disabled instead, and says where to change it.
 */
class FastingToggleTest {

    private fun stateWith(status: FastStatus?) = FastingTrackerUiState(
        todayRecord = status?.let {
            FastRecord(
                id = 1L,
                date = LocalDate.of(2026, 3, 12).toEpochDay() * 86_400_000L,
                hijriDate = null,
                hijriMonth = 9,
                hijriYear = 1447,
                fastType = FastType.RAMADAN,
                status = it,
                exemptionReason = null,
                suhoorTime = null,
                iftarTime = null,
                note = null,
                createdAt = 0L,
                updatedAt = 0L,
            )
        }
    )

    @Test
    fun `a day with no record is toggleable`() {
        assertThat(stateWith(null).canToggleToday).isTrue()
        assertThat(stateWith(null).toggleBlockedReason).isNull()
    }

    @Test
    fun `fasted and not-fasted are the two ends of the toggle`() {
        assertThat(stateWith(FastStatus.FASTED).canToggleToday).isTrue()
        assertThat(stateWith(FastStatus.NOT_FASTED).canToggleToday).isTrue()
    }

    @Test
    fun `an exempted day is not toggleable, and says why`() {
        val state = stateWith(FastStatus.EXEMPTED)

        assertThat(state.canToggleToday).isFalse()
        assertThat(state.toggleBlockedReason).isEqualTo(FastStatus.EXEMPTED)
    }

    @Test
    fun `a makeup-due day is not toggleable, and says why`() {
        val state = stateWith(FastStatus.MAKEUP_DUE)

        assertThat(state.canToggleToday).isFalse()
        assertThat(state.toggleBlockedReason).isEqualTo(FastStatus.MAKEUP_DUE)
    }

    @Test
    fun `every FastStatus is accounted for`() {
        // Guards the decision itself: a fifth status must force a choice here rather
        // than silently defaulting to "toggleable" or "blocked".
        val classified = FastStatus.entries.map { stateWith(it).canToggleToday }

        assertThat(classified).hasSize(4)
        assertThat(classified.count { it }).isEqualTo(2)
    }
}
