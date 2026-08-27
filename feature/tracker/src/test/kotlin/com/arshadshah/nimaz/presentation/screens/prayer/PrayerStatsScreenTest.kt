package com.arshadshah.nimaz.presentation.screens.prayer

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.common.formatMonthYear
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerStats
import com.arshadshah.nimaz.presentation.viewmodel.tracker.PrayerStatsUiState
import com.arshadshah.nimaz.presentation.viewmodel.tracker.PrayerTrackerEvent
import com.arshadshah.nimaz.presentation.viewmodel.tracker.PrayerTrackerViewModel
import com.arshadshah.nimaz.presentation.viewmodel.tracker.StatsPeriod
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.ZoneId

/**
 * The statistics screen, and the three insights it derives.
 *
 * `PrayerDaoTest` (#597) pins the arithmetic underneath — perfect days, per-prayer counts,
 * streaks. What lives only here is the **presentation** of it, and the insights in particular
 * are computed in this file from `prayedByPrayer`/`missedByPrayer` and asserted nowhere:
 *
 *  - the weakest prayer is offered only below 90%, so a user who is doing well is not nagged;
 *  - a prayer with no data at all counts as 100% rather than 0%, or every prayer the user has
 *    never recorded would be reported as their weakest;
 *  - with no prayers recorded at all, none of the three insights may appear — otherwise a fresh
 *    install opens on "Overall completion: 0%".
 *
 * The screen also renders nothing but the period chips while `stats` is null, which is the state
 * every launch passes through.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class PrayerStatsScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun stats(
        prayed: Int = 100,
        missed: Int = 20,
        prayedByPrayer: Map<PrayerName, Int> = mapOf(
            PrayerName.FAJR to 10,
            PrayerName.DHUHR to 25,
            PrayerName.ASR to 25,
            PrayerName.MAGHRIB to 20,
            PrayerName.ISHA to 20,
        ),
        missedByPrayer: Map<PrayerName, Int> = mapOf(
            PrayerName.FAJR to 15,
            PrayerName.DHUHR to 0,
            PrayerName.ASR to 0,
            PrayerName.MAGHRIB to 5,
            PrayerName.ISHA to 0,
        ),
        perfectDays: Int = 12,
        startDate: Long = 1_710_000_000_000L,
    ) = PrayerStats(
        totalPrayed = prayed,
        totalMissed = missed,
        totalJamaah = 3,
        prayedByPrayer = prayedByPrayer,
        missedByPrayer = missedByPrayer,
        currentStreak = 4,
        longestStreak = 9,
        perfectDays = perfectDays,
        startDate = startDate,
        endDate = startDate + 1,
    )

    private val statsState = MutableStateFlow(PrayerStatsUiState(isLoading = false))
    private val events = mutableListOf<PrayerTrackerEvent>()
    private var backs = 0

    private val viewModel: PrayerTrackerViewModel = mockk(relaxed = true) {
        every { this@mockk.statsState } returns this@PrayerStatsScreenTest.statsState
        every { onEvent(any()) } answers { events += firstArg<PrayerTrackerEvent>() }
    }

    private fun setContent() {
        composeRule.setThemedContent {
            PrayerStatsScreen(onNavigateBack = { backs++ }, viewModel = viewModel)
        }
    }

    private fun string(@StringRes res: Int, vararg args: Any): String =
        context.getString(res, *args)

    @Test
    fun `before any statistics arrive only the period chips are drawn`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.stats_period_week)).assertExists()
        // Every launch passes through this state; a chart drawn over a null `stats` is a crash,
        // and an insights section over one is a claim about nothing.
        composeRule.onNodeWithText(string(R.string.prayer_completion)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.insights)).assertDoesNotExist()
    }

    @Test
    fun `all four periods are offered and choosing one reports it`() {
        setContent()

        StatsPeriod.entries.forEach { period ->
            val label = when (period) {
                StatsPeriod.WEEK -> string(R.string.stats_period_week)
                StatsPeriod.MONTH -> string(R.string.stats_period_month)
                StatsPeriod.YEAR -> string(R.string.stats_period_year)
                StatsPeriod.ALL_TIME -> string(R.string.all_time)
            }
            composeRule.onNodeWithText(label).performClick()
        }

        assertThat(events).containsExactlyElementsIn(
            StatsPeriod.entries.map { PrayerTrackerEvent.SetStatsPeriod(it) }
        ).inOrder()
    }

    @Test
    fun `the donut carries the counts, the perfect days and both streaks`() {
        statsState.value = statsState.value.copy(
            stats = stats(),
            currentStreak = 4,
            longestStreak = 9,
        )
        setContent()

        composeRule.onNodeWithText(string(R.string.prayer_completion)).assertExists()
        composeRule.onNodeWithText(string(R.string.prayer_breakdown)).assertExists()
        composeRule.onNodeWithText("100").assertExists()
        composeRule.onNodeWithText("20").assertExists()
        composeRule.onNodeWithText("12").assertExists()
        composeRule.onNodeWithText("9").assertExists()
    }

    @Test
    fun `the donut is subtitled with the month the statistics start in`() {
        val startDate = 1_710_000_000_000L
        statsState.value = statsState.value.copy(stats = stats(startDate = startDate))
        setContent()

        val expected = Instant.ofEpochMilli(startDate).atZone(ZoneId.systemDefault())
            .toLocalDate().formatMonthYear()
        composeRule.onNodeWithText(expected).assertExists()
    }

    @Test
    fun `a prayer below ninety percent is flagged for attention`() {
        statsState.value = statsState.value.copy(stats = stats())
        setContent()

        // Fajr at 10 of 25 is 40%: the weakest, and the one the user can act on.
        composeRule.onNodeWithText(
            string(R.string.prayer_insight_needs_attention, string(R.string.prayer_fajr))
        ).assertExists()
        composeRule.onNodeWithText(
            string(R.string.prayer_insight_needs_attention_desc, string(R.string.prayer_fajr), 40)
        ).assertExists()
    }

    @Test
    fun `a strong record is not nagged about its weakest prayer`() {
        statsState.value = statsState.value.copy(
            stats = stats(
                prayed = 100,
                missed = 2,
                prayedByPrayer = PrayerName.entries.associateWith { 20 },
                missedByPrayer = PrayerName.entries.associateWith { 1 },
            )
        )
        setContent()

        // Every prayer is at 95%. There is always a weakest one; below 90% is what makes it
        // worth saying, and a screen that always shows a warning teaches the user to ignore it.
        composeRule.onNodeWithText(
            string(R.string.prayer_insight_needs_attention, string(R.string.prayer_fajr))
        ).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.prayer_insight_overall, 98)).assertExists()
    }

    @Test
    fun `the overall insight reports the completion percentage and the counts behind it`() {
        statsState.value = statsState.value.copy(stats = stats(prayed = 90, missed = 10))
        setContent()

        composeRule.onNodeWithText(string(R.string.prayer_insight_overall, 90)).assertExists()
        composeRule.onNodeWithText(
            string(R.string.prayer_insight_overall_desc, 90, 100)
        ).assertExists()
    }

    @Test
    fun `the best prayer is the one with the fewest misses`() {
        statsState.value = statsState.value.copy(stats = stats())
        setContent()

        // Dhuhr, Asr and Isha are all at 100%; `maxByOrNull` keeps the first, which is Dhuhr.
        composeRule.onNodeWithText(
            string(R.string.prayer_insight_best, string(R.string.prayer_dhuhr))
        ).assertExists()
    }

    @Test
    fun `a prayer with no record at all is not called the weakest`() {
        statsState.value = statsState.value.copy(
            stats = stats(
                prayed = 40,
                missed = 10,
                prayedByPrayer = mapOf(PrayerName.FAJR to 10, PrayerName.DHUHR to 30),
                missedByPrayer = mapOf(PrayerName.DHUHR to 10),
            )
        )
        setContent()

        // Asr, Maghrib and Isha have nothing recorded either way; counting them as 0% would make
        // one of them the weakest every time and bury the prayer the user is actually behind on.
        composeRule.onNodeWithText(
            string(R.string.prayer_insight_needs_attention, string(R.string.prayer_dhuhr))
        ).assertExists()
        composeRule.onNodeWithText(
            string(R.string.prayer_insight_needs_attention, string(R.string.prayer_asr))
        ).assertDoesNotExist()
    }

    @Test
    fun `with nothing recorded at all, no insight is offered`() {
        statsState.value = statsState.value.copy(
            stats = stats(
                prayed = 0,
                missed = 0,
                prayedByPrayer = emptyMap(),
                missedByPrayer = emptyMap(),
                perfectDays = 0,
            )
        )
        setContent()

        // A fresh install has statistics — all zero. "Overall completion: 0%" as the first thing
        // it says is a judgement about a user who has not used the feature yet.
        composeRule.onNodeWithText(string(R.string.prayer_insight_overall, 0)).assertDoesNotExist()
        composeRule.onNodeWithText(
            string(R.string.prayer_insight_best, string(R.string.prayer_fajr))
        ).assertDoesNotExist()
    }

    @Test
    fun `the back arrow goes back`() {
        setContent()

        composeRule.onNodeWithContentDescription(string(R.string.cd_back)).performClick()

        assertThat(backs).isEqualTo(1)
    }
}
