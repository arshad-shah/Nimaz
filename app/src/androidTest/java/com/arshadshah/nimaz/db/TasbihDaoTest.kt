package com.arshadshah.nimaz.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arshadshah.nimaz.support.NimazDbRule
import com.arshadshah.nimaz.support.TestData
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tasbih (dhikr counter) presets and session history. */
@RunWith(AndroidJUnit4::class)
class TasbihDaoTest {

    @get:Rule
    val dbRule = NimazDbRule()
    private val dao get() = dbRule.db.tasbihDao()

    @Test
    fun insertPreset_returnsRowIdAndIsReadable() = runTest {
        val id = dao.insertPreset(TestData.tasbihPreset(name = "Alhamdulillah", targetCount = 33))

        assertThat(id).isGreaterThan(0L)
        val preset = dao.getPresetById(id)
        assertThat(preset).isNotNull()
        assertThat(preset!!.name).isEqualTo("Alhamdulillah")
        assertThat(preset.targetCount).isEqualTo(33)
    }

    @Test
    fun defaultAndCustomPresets_areSeparated() = runTest {
        dao.insertPreset(TestData.tasbihPreset(name = "Default one", isCustom = 0))
        dao.insertPreset(TestData.tasbihPreset(name = "My custom", isCustom = 1))

        assertThat(dao.getAllPresets().first()).hasSize(2)
        assertThat(dao.getDefaultPresets().first().map { it.name }).contains("Default one")
        assertThat(dao.getCustomPresets().first().map { it.name }).contains("My custom")
    }

    @Test
    fun session_completes_andCountsInRange() = runTest {
        val presetId = dao.insertPreset(TestData.tasbihPreset())
        val sessionId = dao.insertSession(
            TestData.tasbihSession(presetId = presetId, currentCount = 33, targetCount = 33)
        )

        dao.completeSession(sessionId, completedAt = TestData.DAY, duration = 60_000L)

        val completedInRange =
            dao.getCompletedSessionsInRange(TestData.DAY - 1, TestData.DAY + 1)
        assertThat(completedInRange).isEqualTo(1)
        val total = dao.getTotalCountInRange(TestData.DAY - 1, TestData.DAY + 1)
        assertThat(total).isEqualTo(33)
    }

    @Test
    fun deleteCustomPreset_removesOnlyThatRow() = runTest {
        val keep = dao.insertPreset(TestData.tasbihPreset(name = "Keep", isCustom = 1))
        val drop = dao.insertPreset(TestData.tasbihPreset(name = "Drop", isCustom = 1))

        dao.deleteCustomPreset(drop)

        assertThat(dao.getPresetById(drop)).isNull()
        assertThat(dao.getPresetById(keep)).isNotNull()
    }

    @Test
    fun deleteAllUserData_clearsSessions() = runTest {
        val presetId = dao.insertPreset(TestData.tasbihPreset())
        dao.insertSession(TestData.tasbihSession(presetId = presetId))

        dao.deleteAllUserData()

        assertThat(dao.getAllSessionsSync()).isEmpty()
    }
}
