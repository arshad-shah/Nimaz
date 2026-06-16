package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.onNodeWithText
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerStats
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PrayerStatsChartTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private fun stats(
        totalPrayed: Int = 120,
        totalMissed: Int = 30,
        totalJamaah: Int = 45,
        currentStreak: Int = 7,
        longestStreak: Int = 21,
        perfectDays: Int = 15
    ) = PrayerStats(
        totalPrayed = totalPrayed,
        totalMissed = totalMissed,
        totalJamaah = totalJamaah,
        prayedByPrayer = mapOf(
            PrayerName.FAJR to 20,
            PrayerName.DHUHR to 28,
            PrayerName.ASR to 25,
            PrayerName.MAGHRIB to 27,
            PrayerName.ISHA to 20
        ),
        missedByPrayer = mapOf(
            PrayerName.FAJR to 10,
            PrayerName.DHUHR to 2,
            PrayerName.ASR to 5,
            PrayerName.MAGHRIB to 3,
            PrayerName.ISHA to 10
        ),
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        perfectDays = perfectDays,
        startDate = 0L,
        endDate = 0L
    )

    @Test
    fun `renders default title`() {
        composeRule.setThemedContent {
            PrayerStatsChart(stats = stats())
        }

        composeRule.onNodeWithText("Prayer Statistics").assertExists()
    }

    @Test
    fun `renders custom title and subtitle`() {
        composeRule.setThemedContent {
            PrayerStatsChart(
                stats = stats(),
                title = "Prayer Completion",
                subtitle = "January 2026"
            )
        }

        composeRule.onNodeWithText("Prayer Completion").assertExists()
        composeRule.onNodeWithText("January 2026").assertExists()
    }

    @Test
    fun `donut chart shows the prayed and missed legend`() {
        composeRule.setThemedContent {
            PrayerStatsChart(
                stats = stats(),
                chartType = PrayerChartType.DONUT
            )
        }

        // Legend renders "$label: $value" and donut center renders "Completed"
        composeRule.onNodeWithText("Prayed: 120").assertExists()
        composeRule.onNodeWithText("Missed: 30").assertExists()
        composeRule.onNodeWithText("Completed").assertExists()
    }

    @Test
    fun `donut chart hides jamaah legend when totalJamaah is zero`() {
        composeRule.setThemedContent {
            PrayerStatsChart(
                stats = stats(totalJamaah = 0),
                chartType = PrayerChartType.DONUT
            )
        }

        composeRule.onNodeWithText("Jamaah: 0").assertDoesNotExist()
    }

    @Test
    fun `default summary shows streak labels and values`() {
        composeRule.setThemedContent {
            PrayerStatsChart(
                stats = stats(currentStreak = 7, longestStreak = 21),
                summaryItems = null
            )
        }

        // StatItem labels use newline-joined two-line text
        composeRule.onNodeWithText("Current\nStreak").assertExists()
        composeRule.onNodeWithText("Longest\nStreak").assertExists()
    }

    @Test
    fun `empty summaryItems list shows no summary row`() {
        composeRule.setThemedContent {
            PrayerStatsChart(
                stats = stats(),
                summaryItems = emptyList()
            )
        }

        composeRule.onNodeWithText("Current\nStreak").assertDoesNotExist()
    }

    @Test
    fun `custom summaryItems are rendered`() {
        composeRule.setThemedContent {
            PrayerStatsChart(
                stats = stats(),
                summaryItems = listOf(
                    ChartStatItem("99", "My\nLabel", Color.Red)
                )
            )
        }

        composeRule.onNodeWithText("99").assertExists()
        composeRule.onNodeWithText("My\nLabel").assertExists()
    }

    @Test
    fun `bar chart renders prayer names`() {
        composeRule.setThemedContent {
            PrayerStatsChart(
                stats = stats(),
                chartType = PrayerChartType.BAR,
                summaryItems = emptyList()
            )
        }

        // BarChart lists every prayer except Sunrise using displayName()
        composeRule.onNodeWithText("Fajr").assertExists()
        composeRule.onNodeWithText("Isha").assertExists()
    }

    @Test
    fun `radial chart renders without crashing`() {
        composeRule.setThemedContent {
            PrayerStatsChart(
                stats = stats(),
                chartType = PrayerChartType.RADIAL,
                title = "Radial",
                summaryItems = emptyList()
            )
        }

        composeRule.onNodeWithText("Radial").assertExists()
    }

    @Test
    fun `ChartStatItem retains its fields`() {
        val item = ChartStatItem(value = "5", label = "Days", color = Color.Blue)
        assertThat(item.value).isEqualTo("5")
        assertThat(item.label).isEqualTo("Days")
        assertThat(item.color).isEqualTo(Color.Blue)
    }

    @Test
    fun `PrayerChartType has three entries`() {
        assertThat(PrayerChartType.entries).hasSize(3)
        assertThat(PrayerChartType.entries)
            .containsExactly(
                PrayerChartType.DONUT,
                PrayerChartType.BAR,
                PrayerChartType.RADIAL
            )
    }
}
