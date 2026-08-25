package com.arshadshah.nimaz.presentation.screens.fasting

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.FastRecord
import com.arshadshah.nimaz.domain.model.FastStatus
import com.arshadshah.nimaz.domain.model.FastType
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Which voluntary fasts are coming up, and in what order.
 *
 * The list is built inside a composable — it needs `stringResource` for the phrasing — so it has
 * never been run outside the app. Two of its rules are load-bearing and neither is visible when
 * it breaks:
 *
 *  - **It is sorted by date.** Built unsorted it listed next Monday before today, because the
 *    weekly days are added in a fixed Mon/Thu sequence which is only chronological for part of
 *    the week. "Coming up" is a promise about order, and a list that opens with a day four days
 *    out reads as though the nearer one is gone.
 *  - **The Hijri events are deduplicated across two years.** They are gathered from this Hijri
 *    year *and* the next so the list does not run dry in Dhul-Hijjah, which means Ashura and
 *    Arafah each appear twice before the nearest-upcoming filter runs.
 *
 * The dates are the app's real Hijri arithmetic — that is `HijriDateCalculator`'s own tests'
 * job. What is asserted here is the shape of the list built on top of it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class FastingComingUpTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val today: LocalDate = LocalDate.of(2026, 8, 13)

    private fun record(date: LocalDate, status: FastStatus = FastStatus.FASTED) = FastRecord(
        id = date.toEpochDay(),
        date = date.toEpochDay() * MILLIS_PER_DAY,
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

    private fun string(@StringRes res: Int, vararg args: Any): String =
        context.getString(res, *args)

    /** Runs [rememberComingUpFasts] inside a composition and hands the result back. */
    private fun comingUp(
        records: List<FastRecord> = emptyList(),
        daysUntilAyyamAlBeed: Int = 4,
        on: LocalDate = today,
    ): List<ComingUpFast> {
        lateinit var result: List<ComingUpFast>
        composeRule.setThemedContent {
            Capture(records, daysUntilAyyamAlBeed, on) { result = it }
        }
        composeRule.waitForIdle()
        return result
    }

    @Composable
    private fun Capture(
        records: List<FastRecord>,
        daysUntilAyyamAlBeed: Int,
        on: LocalDate,
        onResult: (List<ComingUpFast>) -> Unit,
    ) {
        val fasts = rememberComingUpFasts(records, daysUntilAyyamAlBeed, on)
        SideEffect { onResult(fasts) }
    }

    @Test
    fun `the list is in date order`() {
        val fasts = comingUp()

        // 13 Aug 2026 is a Thursday, so the Thursday entry is today and the Monday entry is four
        // days out. Built in Mon/Thu order this list opens with next Monday.
        assertThat(fasts.map { it.date }).isInOrder()
        assertThat(fasts.first().date).isEqualTo(today)
    }

    @Test
    fun `both weekly sunnah days are offered, anchored to the next occurrence of each`() {
        val fasts = comingUp()

        val monday = fasts.single { it.name == string(R.string.fasting_monday) }
        val thursday = fasts.single { it.name == string(R.string.fasting_thursday) }

        assertThat(monday.date)
            .isEqualTo(today.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY)))
        assertThat(thursday.date)
            .isEqualTo(today.with(TemporalAdjusters.nextOrSame(DayOfWeek.THURSDAY)))
        assertThat(monday.why).isEqualTo(string(R.string.fasting_sunnah_desc))
    }

    @Test
    fun `a weekly day that falls today is labelled today rather than dated`() {
        val fasts = comingUp()

        // `nextOrSame`, so on a Thursday the Thursday entry *is* today — "Next: 13 Aug" for a
        // day that is already here is a small lie the label has to avoid.
        assertThat(fasts.single { it.name == string(R.string.fasting_thursday) }.whenLabel)
            .isEqualTo(string(R.string.fasting_today))
        assertThat(fasts.single { it.name == string(R.string.fasting_monday) }.whenLabel)
            .startsWith("Next")
    }

    @Test
    fun `Ayyam al-Beed in progress is labelled today`() {
        // The count comes from the ViewModel, where the clock and the user's Hijri day offset
        // both live. Reading the clock here instead would answer yesterday's question on a
        // screen left open across midnight, and would ignore the offset entirely.
        assertThat(comingUp(daysUntilAyyamAlBeed = 0)
            .single { it.name == string(R.string.fasting_ayyam_al_beed) }.whenLabel)
            .isEqualTo(string(R.string.fasting_today))
    }

    @Test
    fun `Ayyam al-Beed a day out is labelled tomorrow`() {
        // A separate test rather than a second case in the one above: a compose rule composes
        // once, so a before/after comparison has to be two tests.
        assertThat(comingUp(daysUntilAyyamAlBeed = 1)
            .single { it.name == string(R.string.fasting_ayyam_al_beed) }.whenLabel)
            .isEqualTo(string(R.string.fasting_tomorrow))
    }

    @Test
    fun `no fast is listed twice, even though the Hijri events span two years`() {
        val fasts = comingUp()

        // Ashura and Arafah are gathered from this Hijri year and the next so the list does not
        // run dry late in the year; without the nearest-upcoming filter each arrives twice.
        assertThat(fasts.map { it.name }).containsNoDuplicates()
        // Three fixed entries plus at most three Hijri ones — a list that had kept both years'
        // copies would be longer than the cap.
        assertThat(fasts.size).isAtMost(6)
    }

    @Test
    fun `nothing in the list is in the past`() {
        comingUp().forEach { assertThat(it.date).isAtLeast(today) }
    }

    @Test
    fun `a day already recorded as fasted is marked logged`() {
        val nextMonday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))

        val fasts = comingUp(records = listOf(record(nextMonday)))

        assertThat(fasts.single { it.date == nextMonday && it.name == string(R.string.fasting_monday) }.isLogged)
            .isTrue()
        assertThat(fasts.filter { it.date != nextMonday }.none { it.isLogged }).isTrue()
    }

    @Test
    fun `only a fasted record counts as logged`() {
        val nextMonday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))

        // A day recorded as *not* fasted is still worth offering — the card would otherwise show
        // a tick against a day the user explicitly said they did not fast.
        val fasts = comingUp(records = listOf(record(nextMonday, FastStatus.NOT_FASTED)))

        assertThat(fasts.none { it.isLogged }).isTrue()
    }

    @Test
    fun `a card reports the fast and offers to log it`() {
        var logged: LocalDate? = null
        composeRule.setThemedContent {
            ComingUpRow(
                fasts = listOf(
                    ComingUpFast("Today", "Thursday fast", "Weekly sunnah", today, isLogged = false),
                ),
                onLogFast = { logged = it },
            )
        }

        composeRule.onNodeWithText(string(R.string.fasting_log_this_fast)).assertExists()
        composeRule.onNodeWithText("Thursday fast").performClick()

        assertThat(logged).isEqualTo(today)
    }

    @Test
    fun `an already-logged card says so instead of inviting a second log`() {
        composeRule.setThemedContent {
            ComingUpRow(
                fasts = listOf(
                    ComingUpFast("Today", "Thursday fast", "Weekly sunnah", today, isLogged = true),
                ),
                onLogFast = {},
            )
        }

        composeRule.onNodeWithText(string(R.string.fasting_logged)).assertExists()
        composeRule.onNodeWithText(string(R.string.fasting_log_this_fast)).assertDoesNotExist()
    }

    private companion object {
        const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
    }
}
