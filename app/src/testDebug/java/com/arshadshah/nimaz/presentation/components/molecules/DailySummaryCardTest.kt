package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.presentation.viewmodel.PrayerTimeDisplay
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DailySummaryCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun display(
        type: PrayerType,
        name: String,
        time: String = "12:00 PM",
        isCurrent: Boolean = false,
        status: PrayerStatus = PrayerStatus.NOT_PRAYED
    ) = PrayerTimeDisplay(
        type = type,
        name = name,
        time = time,
        isPassed = false,
        isCurrent = isCurrent,
        isNext = false,
        prayerStatus = status
    )

    private val prayerTimes = listOf(
        display(PrayerType.FAJR, "Fajr", status = PrayerStatus.PRAYED),
        display(PrayerType.SUNRISE, "Sunrise"),
        display(PrayerType.DHUHR, "Dhuhr", status = PrayerStatus.PRAYED),
        display(PrayerType.ASR, "Asr", isCurrent = true),
        display(PrayerType.MAGHRIB, "Maghrib"),
        display(PrayerType.ISHA, "Isha"),
    )

    @Test
    fun `renders progress header and count (2 of 5 prayed)`() {
        composeRule.setThemedContent {
            DailySummaryCard(
                prayerTimes = prayerTimes,
                fastingToday = true
            )
        }

        composeRule.onNodeWithText("Today's Progress").assertExists()
        // prayers_count_format: "%1$d of %2$d prayers" -> 2 of 5 (Sunrise excluded)
        composeRule.onNodeWithText("2 of 5 prayers").assertExists()
    }

    @Test
    fun `renders prayer dot labels uppercased`() {
        composeRule.setThemedContent {
            DailySummaryCard(
                prayerTimes = prayerTimes,
                fastingToday = false
            )
        }

        // label = prayer.name.take(5).uppercase()
        composeRule.onNodeWithText("FAJR").assertExists()
        composeRule.onNodeWithText("MAGHR").assertExists()
    }

    @Test
    fun `fasting today shows fasting label and today fasting text`() {
        composeRule.setThemedContent {
            DailySummaryCard(
                prayerTimes = prayerTimes,
                fastingToday = true
            )
        }

        composeRule.onNodeWithText("Fasting").assertExists()
        composeRule.onNodeWithText("Today: Fasting").assertExists()
    }

    @Test
    fun `not fasting shows no fast today text`() {
        composeRule.setThemedContent {
            DailySummaryCard(
                prayerTimes = prayerTimes,
                fastingToday = false
            )
        }

        composeRule.onNodeWithText("No fast today").assertExists()
    }

    @Test
    fun `fillHeight true renders without crashing`() {
        composeRule.setThemedContent {
            DailySummaryCard(
                prayerTimes = prayerTimes,
                fastingToday = true,
                fillHeight = true
            )
        }

        composeRule.onNodeWithText("Today's Progress").assertExists()
    }

    @Test
    fun `empty prayer list renders zero of zero`() {
        composeRule.setThemedContent {
            DailySummaryCard(
                prayerTimes = emptyList(),
                fastingToday = false
            )
        }

        composeRule.onNodeWithText("0 of 0 prayers").assertExists()
    }
}
