package com.arshadshah.nimaz.presentation.screens.tasbih

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.TasbihSession
import com.arshadshah.nimaz.presentation.viewmodel.tracker.TasbihCounterUiState
import com.arshadshah.nimaz.presentation.viewmodel.tracker.TasbihHistoryUiState
import com.arshadshah.nimaz.presentation.viewmodel.tracker.TasbihStatsUiState
import com.arshadshah.nimaz.presentation.viewmodel.tracker.TasbihViewModel
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

/**
 * What the counter recorded, read back.
 *
 * Two things here are computed in the screen and nowhere else. The **today total** adds the
 * session in progress to what is already saved — read `baseTotalToday` alone and the number
 * someone opened this screen to check is short by everything they have counted since the last
 * save. The **session subtitle** joins count, laps and duration into one line, and its `%d:%02d`
 * is the only place a session's length is formatted at all; a minute rendered as `3:7` instead
 * of `3:07` is wrong in a way no crash reports.
 *
 * The tab strip is the other half. `THIS_WEEK` and `ALL_TIME` deliberately read the same source
 * — there is no all-time query yet — and that is worth pinning so it is a decision rather than
 * something a later change quietly "fixes" into showing a week's data under an all-time label.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class TasbihHistoryScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun session(
        id: Long,
        name: String?,
        count: Int,
        target: Int = 33,
        laps: Int = 0,
        completed: Boolean = false,
        duration: Long? = null,
    ) = TasbihSession(
        id = id,
        presetId = null,
        presetName = name,
        date = 0L,
        currentCount = count,
        targetCount = target,
        totalLaps = laps,
        isCompleted = completed,
        duration = duration,
        startedAt = 0L,
        completedAt = null,
        note = null,
    )

    private val todaySession = session(1, "SubhanAllah", 33, completed = true, duration = 187_000L)
    private val weekSession = session(2, null, 12, laps = 3)

    private val historyState = MutableStateFlow(
        TasbihHistoryUiState(
            todaySessions = listOf(todaySession),
            weekSessions = listOf(weekSession),
            isLoading = false,
        )
    )
    private val statsState = MutableStateFlow(
        TasbihStatsUiState(
            baseTotalToday = 100,
            totalThisWeek = 700,
            completedSessions = 4,
            isLoading = false,
        )
    )
    private val counterState = MutableStateFlow(TasbihCounterUiState())
    private var backs = 0

    private val viewModel: TasbihViewModel = mockk(relaxed = true) {
        every { this@mockk.historyState } returns this@TasbihHistoryScreenTest.historyState
        every { this@mockk.statsState } returns this@TasbihHistoryScreenTest.statsState
        every { this@mockk.counterState } returns this@TasbihHistoryScreenTest.counterState
    }

    private fun setContent() {
        composeRule.setThemedContent {
            TasbihHistoryScreen(onNavigateBack = { backs++ }, viewModel = viewModel)
        }
    }

    private fun string(@StringRes res: Int, vararg args: Any): String =
        context.getString(res, *args)

    @Test
    fun `while loading, no session list is drawn`() {
        historyState.value = historyState.value.copy(isLoading = true)
        setContent()

        // The summary card is part of the list, so its absence is what says the loading arm ran
        // rather than an empty one.
        composeRule.onNodeWithText(string(R.string.sessions).uppercase()).assertDoesNotExist()
        composeRule.onNodeWithText(todaySession.presetName!!).assertDoesNotExist()
    }

    @Test
    fun `the today total counts the session in progress on top of what is saved`() {
        counterState.value = counterState.value.copy(count = 7, laps = 1, targetCount = 33)
        setContent()

        // 100 saved + (7 + 1×33). Reading `baseTotalToday` alone would report 100 while the
        // user watches the counter say otherwise.
        composeRule.onNodeWithText("140").assertExists()
        composeRule.onNodeWithText("4").assertExists()
        composeRule.onNodeWithText("700").assertExists()
    }

    @Test
    fun `the today tab lists today's sessions`() {
        setContent()

        composeRule.onNodeWithText(todaySession.presetName!!).assertExists()
    }

    @Test
    fun `a session's subtitle carries its count, its laps and its length`() {
        setContent()

        // 187 seconds is 3:07 — the zero-padded seconds are the whole point of the `%02d`, and
        // "3:7" is the kind of wrong that ships.
        composeRule.onNodeWithText("33/33 · 3:07").assertExists()
    }

    @Test
    fun `a session with no preset is named as a custom count`() {
        setContent()
        composeRule.onNodeWithText(string(R.string.this_week)).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(string(R.string.custom)).assertExists()
        composeRule.onNodeWithText(
            "12/33 · ${context.resources.getQuantityString(R.plurals.laps_format, 3, 3)}"
        ).assertExists()
    }

    @Test
    fun `only a completed session is badged done`() {
        setContent()
        composeRule.onNodeWithText(string(R.string.done).uppercase()).assertExists()

        composeRule.onNodeWithText(string(R.string.this_week)).performClick()
        composeRule.waitForIdle()

        // The week's one session is unfinished, so the badge must go with it.
        composeRule.onNodeWithText(string(R.string.done).uppercase()).assertDoesNotExist()
    }

    @Test
    fun `all-time falls back to the week, because no wider query exists yet`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.all_time)).performClick()
        composeRule.waitForIdle()

        // Deliberate, and pinned so it stays deliberate: the tab shows the widest set the screen
        // actually has rather than an empty list under an all-time label.
        composeRule.onNodeWithText(string(R.string.custom)).assertExists()
        composeRule.onNodeWithText(todaySession.presetName!!).assertDoesNotExist()
    }

    @Test
    fun `a tab with nothing in it says so instead of showing a blank page`() {
        historyState.value = historyState.value.copy(todaySessions = emptyList())
        setContent()

        composeRule.onNodeWithText(string(R.string.no_sessions_yet)).assertExists()
        composeRule.onNodeWithText(string(R.string.start_counting_hint)).assertExists()
    }

    @Test
    fun `the back arrow goes back`() {
        setContent()

        composeRule.onNodeWithContentDescription(string(R.string.cd_back)).performClick()

        assertThat(backs).isEqualTo(1)
    }
}
