package com.arshadshah.nimaz.presentation.components.organisms

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
class TodaysProgressCardTest {

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

    private fun fiveMainPrayers(
        prayedCount: Int = 0
    ): List<PrayerTimeDisplay> {
        val specs = listOf(
            PrayerType.FAJR to "Fajr",
            PrayerType.DHUHR to "Dhuhr",
            PrayerType.ASR to "Asr",
            PrayerType.MAGHRIB to "Maghrib",
            PrayerType.ISHA to "Isha"
        )
        return specs.mapIndexed { index, (type, name) ->
            display(
                type = type,
                name = name,
                status = if (index < prayedCount) PrayerStatus.PRAYED else PrayerStatus.NOT_PRAYED
            )
        }
    }

    @Test
    fun `renders the title`() {
        composeRule.setThemedContent {
            TodaysProgressCard(prayerTimes = fiveMainPrayers())
        }

        // R.string.todays_progress = "Today's Progress"
        composeRule.onNodeWithText("Today's Progress").assertExists()
    }

    @Test
    fun `renders the prayed count with none prayed`() {
        composeRule.setThemedContent {
            TodaysProgressCard(prayerTimes = fiveMainPrayers(prayedCount = 0))
        }

        // R.string.prayers_count_format = "%1$d of %2$d prayers"
        composeRule.onNodeWithText("0 of 5 prayers").assertExists()
    }

    @Test
    fun `renders the prayed count when some prayed`() {
        composeRule.setThemedContent {
            TodaysProgressCard(prayerTimes = fiveMainPrayers(prayedCount = 2))
        }

        composeRule.onNodeWithText("2 of 5 prayers").assertExists()
    }

    @Test
    fun `renders a dot label per main prayer`() {
        composeRule.setThemedContent {
            TodaysProgressCard(prayerTimes = fiveMainPrayers())
        }

        // Dot labels are prayer.name.take(5).uppercase()
        composeRule.onNodeWithText("FAJR").assertExists()
        composeRule.onNodeWithText("DHUHR").assertExists()
        composeRule.onNodeWithText("ASR").assertExists()
        composeRule.onNodeWithText("ISHA").assertExists()
    }

    @Test
    fun `excludes non-main prayers like sunrise from the count`() {
        val withSunrise = fiveMainPrayers(prayedCount = 1) + display(
            type = PrayerType.SUNRISE,
            name = "Sunrise",
            status = PrayerStatus.PRAYED
        )

        composeRule.setThemedContent {
            TodaysProgressCard(prayerTimes = withSunrise)
        }

        // Sunrise is filtered out: total stays 5, only the FAJR prayed counts.
        composeRule.onNodeWithText("1 of 5 prayers").assertExists()
    }
}
