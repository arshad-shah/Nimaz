package com.arshadshah.nimaz.presentation.viewmodel.tracker

import com.arshadshah.nimaz.domain.model.FastRecord
import com.arshadshah.nimaz.domain.model.FastStatus
import com.arshadshah.nimaz.domain.model.FastType
import com.arshadshah.nimaz.domain.model.TasbihCategory
import com.arshadshah.nimaz.domain.model.TasbihPreset
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

/**
 * The two properties the tracker's UI states **derive** rather than store.
 *
 * Both exist because storing them went wrong. `filteredPresets` used to be a stored field that
 * every input had to remember to recompute, and the two Room collectors in `loadPresets` rebuilt
 * it as `defaults + customs` without consulting the selected category — so saving or deleting a
 * custom dhikr re-emitted the presets flow and silently dropped the active filter while the
 * category chip carried on looking selected. `canToggleToday` replaced a switch that looked live
 * over an exempt day and did nothing when tapped.
 *
 * Deriving them means there is no site left to forget, which is precisely why the derivation
 * itself is the thing worth a test: it is hand-written logic on a data class, and it is what the
 * screens read.
 */
class TrackerDerivedStateTest {

    private fun preset(id: Long, category: TasbihCategory?) = TasbihPreset(
        id = id,
        name = "Dhikr $id",
        arabicText = null,
        transliteration = null,
        translation = null,
        targetCount = 33,
        category = category,
        reference = null,
        isDefault = id < 100,
        displayOrder = 0,
        createdAt = 0L,
        updatedAt = 0L,
    )

    private val morning = preset(1, TasbihCategory.MORNING)
    private val evening = preset(2, TasbihCategory.EVENING)
    private val mine = preset(100, TasbihCategory.CUSTOM)
    private val uncategorised = preset(101, null)

    private fun presets(selected: TasbihCategory?) = TasbihPresetsUiState(
        defaultPresets = listOf(morning, evening),
        customPresets = listOf(mine, uncategorised),
        selectedCategory = selected,
        isLoading = false,
    )

    private fun record(status: FastStatus) = FastRecord(
        id = 1L,
        date = 0L,
        hijriDate = null,
        hijriMonth = null,
        hijriYear = null,
        fastType = FastType.VOLUNTARY,
        status = status,
        exemptionReason = null,
        suhoorTime = null,
        iftarTime = null,
        note = null,
        createdAt = 0L,
        updatedAt = 0L,
    )

    private fun fasting(todayRecord: FastRecord?) = FastingTrackerUiState(
        selectedDate = LocalDate.of(2026, 8, 13),
        todayRecord = todayRecord,
        isLoading = false,
    )

    @Test
    fun `with no category chosen every dhikr is listed, defaults before customs`() {
        val filtered = presets(selected = null).filteredPresets

        assertThat(filtered).containsExactly(morning, evening, mine, uncategorised).inOrder()
    }

    @Test
    fun `a chosen category keeps only its own, across defaults and customs alike`() {
        assertThat(presets(selected = TasbihCategory.MORNING).filteredPresets)
            .containsExactly(morning)
        // The user's own dhikr is filtered by the same rule as a shipped one — the two lists are
        // concatenated first, not filtered separately.
        assertThat(presets(selected = TasbihCategory.CUSTOM).filteredPresets)
            .containsExactly(mine)
    }

    @Test
    fun `a dhikr with no category survives only the unfiltered list`() {
        // `category` is nullable all the way down: an imported or hand-edited row need not carry
        // one, and it must not silently match whichever category happens to be selected.
        TasbihCategory.entries.forEach { category ->
            assertThat(presets(selected = category).filteredPresets).doesNotContain(uncategorised)
        }
        assertThat(presets(selected = null).filteredPresets).contains(uncategorised)
    }

    @Test
    fun `an unlogged day and the two ends of the toggle are all actionable`() {
        assertThat(fasting(todayRecord = null).canToggleToday).isTrue()
        assertThat(fasting(record(FastStatus.FASTED)).canToggleToday).isTrue()
        assertThat(fasting(record(FastStatus.NOT_FASTED)).canToggleToday).isTrue()

        assertThat(fasting(todayRecord = null).toggleBlockedReason).isNull()
    }

    @Test
    fun `a day with a reason on file blocks the toggle and says which reason`() {
        // `EXEMPTED` and `MAKEUP_DUE` are recorded in the day sheet with a reason attached, so a
        // single tap must not overwrite them. The blocked reason is what the control shows in
        // place of looking live and doing nothing, which is what it used to do.
        listOf(FastStatus.EXEMPTED, FastStatus.MAKEUP_DUE).forEach { status ->
            val state = fasting(record(status))
            assertThat(state.canToggleToday).isFalse()
            assertThat(state.toggleBlockedReason).isEqualTo(status)
        }
    }
}
