package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.onNodeWithText
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazStatsGridTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    // ── NimazStatData (public data class) ──────────────────────────────────

    @Test
    fun `NimazStatData carries value label and optional color`() {
        val withColor = NimazStatData(value = "15", label = "Fasted", color = Color.Red)
        assertThat(withColor.value).isEqualTo("15")
        assertThat(withColor.label).isEqualTo("Fasted")
        assertThat(withColor.color).isEqualTo(Color.Red)

        val withoutColor = NimazStatData(value = "3", label = "Missed")
        assertThat(withoutColor.color).isNull()
    }

    // ── NimazStatsGrid ─────────────────────────────────────────────────────

    @Test
    fun `renders each stat value and label`() {
        composeRule.setThemedContent {
            NimazStatsGrid(
                stats = listOf(
                    NimazStatData("15", "Fasted"),
                    NimazStatData("3", "Missed"),
                    NimazStatData("12", "Remaining")
                )
            )
        }

        composeRule.onNodeWithText("15").assertExists()
        composeRule.onNodeWithText("Fasted").assertExists()
        composeRule.onNodeWithText("3").assertExists()
        composeRule.onNodeWithText("Missed").assertExists()
        composeRule.onNodeWithText("12").assertExists()
        composeRule.onNodeWithText("Remaining").assertExists()
    }

    @Test
    fun `renders a stat with a custom color`() {
        composeRule.setThemedContent {
            NimazStatsGrid(
                stats = listOf(
                    NimazStatData("7", "Streak", color = Color.Green)
                )
            )
        }

        composeRule.onNodeWithText("7").assertExists()
        composeRule.onNodeWithText("Streak").assertExists()
    }

    @Test
    fun `renders in compact mode`() {
        composeRule.setThemedContent {
            NimazStatsGrid(
                stats = listOf(
                    NimazStatData("1", "One"),
                    NimazStatData("2", "Two")
                ),
                compact = true
            )
        }

        composeRule.onNodeWithText("One").assertExists()
        composeRule.onNodeWithText("Two").assertExists()
    }
}
