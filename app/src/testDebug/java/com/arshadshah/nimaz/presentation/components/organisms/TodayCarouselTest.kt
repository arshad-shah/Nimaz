package com.arshadshah.nimaz.presentation.components.organisms

import com.arshadshah.nimaz.core.ui.R

import androidx.compose.ui.test.onNodeWithText
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.presentation.model.PrayerTimeDisplay
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TodayCarouselTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private fun prayer(
        type: PrayerType,
        status: PrayerStatus = PrayerStatus.NOT_PRAYED
    ) = PrayerTimeDisplay(
        type = type,
        name = type.displayName,
        timeAt = testInstant(12, 0),
        isPassed = false,
        isCurrent = false,
        isNext = false,
        prayerStatus = status
    )

    private val samplePrayers = listOf(
        prayer(PrayerType.FAJR, PrayerStatus.PRAYED),
        prayer(PrayerType.DHUHR),
        prayer(PrayerType.ASR),
        prayer(PrayerType.MAGHRIB),
        prayer(PrayerType.ISHA),
    )

    @Test
    fun `renders the progress page by default`() {
        composeRule.setThemedContent {
            TodayCarousel(
                prayerTimes = samplePrayers,
                fastingToday = false,
                dailyHadith = null
            )
        }

        // PROGRESS is the first page (TodaysProgressCard), visible without swiping.
        // R.string.todays_progress == "Today's Progress"
        composeRule.onNodeWithText("Today's Progress").assertExists()
    }

    @Test
    fun `progress page shows the prayer count`() {
        composeRule.setThemedContent {
            TodayCarousel(
                prayerTimes = samplePrayers,
                fastingToday = false,
                dailyHadith = null
            )
        }

        // R.string.prayers_count_format == "%1$d of %2$d prayers"; one of five prayed.
        composeRule.onNodeWithText("1 of 5 prayers").assertExists()
    }

    @Test
    fun `renders with a hadith without crashing`() {
        composeRule.setThemedContent {
            TodayCarousel(
                prayerTimes = samplePrayers,
                fastingToday = true,
                dailyHadith = "A sample hadith of the day."
            )
        }

        // PROGRESS is still the front page; the HADITH page is added but off-screen.
        composeRule.onNodeWithText("Today's Progress").assertExists()
    }

    @Test
    fun `TodayCarouselPage enum exposes progress, fasting, hadith and dua pages`() {
        assertThat(TodayCarouselPage.values().toList())
            .containsExactly(
                TodayCarouselPage.PROGRESS,
                TodayCarouselPage.FASTING,
                TodayCarouselPage.HADITH,
                TodayCarouselPage.DUA
            )
            .inOrder()
    }
}

/** A fixed wall-clock instant today, so tests read like a real day. */
private fun testInstant(hour: Int, minute: Int): kotlin.time.Instant =
    kotlin.time.Instant.fromEpochMilliseconds(
        java.time.LocalDate.now().atTime(hour, minute)
            .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
