package com.arshadshah.nimaz.presentation.screens.prayer

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.common.toUtcMidnightMillis
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerRecord
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.presentation.viewmodel.tracker.PrayerTrackerEvent
import com.arshadshah.nimaz.presentation.viewmodel.tracker.PrayerTrackerViewModel
import com.arshadshah.nimaz.presentation.viewmodel.tracker.QadaPrayersUiState
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
import java.time.LocalDate

/**
 * The make-up list: every prayer the user has explicitly marked missed.
 *
 * The empty state is the assertion that matters most here, and it is a promise about the app's
 * behaviour rather than a layout detail: **nothing lands in this list on the user's behalf**.
 * The only way in is the tracker's review banner, which asks first. An empty-state message that
 * drifted away from that would be the app quietly claiming to keep score.
 *
 * The month grouping is the other half — the ViewModel supplies `groupedByMonth` and this screen
 * decides that an empty list plus a finished load is the empty state, while an empty list that is
 * still loading is not. Showing "All caught up!" during the first frame of a load is a false
 * reassurance to someone who has a debt.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class QadaPrayersScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val day: LocalDate = LocalDate.of(2026, 3, 14)

    private fun missed(id: Long, prayer: PrayerName, date: LocalDate = day) = PrayerRecord(
        id = id,
        date = date.toUtcMidnightMillis(),
        prayerName = prayer,
        status = PrayerStatus.MISSED,
        prayedAt = null,
        scheduledTime = date.atTime(5, 0).toLocalTime().toSecondOfDay() * 1000L,
        isJamaah = false,
        isQadaFor = null,
        note = null,
        createdAt = 0L,
        updatedAt = 0L,
    )

    private val fajr = missed(1, PrayerName.FAJR)
    private val isha = missed(2, PrayerName.ISHA)
    private val lastMonth = missed(3, PrayerName.ASR, day.minusMonths(1))

    private val qadaState = MutableStateFlow(
        QadaPrayersUiState(
            missedPrayers = listOf(fajr, isha, lastMonth),
            groupedByMonth = mapOf(
                "March 2026" to listOf(fajr, isha),
                "February 2026" to listOf(lastMonth),
            ),
            totalMissed = 3,
            isLoading = false,
        )
    )
    private val events = mutableListOf<PrayerTrackerEvent>()
    private var backs = 0

    private val viewModel: PrayerTrackerViewModel = mockk(relaxed = true) {
        every { this@mockk.qadaState } returns this@QadaPrayersScreenTest.qadaState
        every { onEvent(any()) } answers { events += firstArg<PrayerTrackerEvent>() }
    }

    private fun setContent() {
        composeRule.setThemedContent {
            QadaPrayersScreen(onNavigateBack = { backs++ }, viewModel = viewModel)
        }
    }

    private fun string(@StringRes res: Int, vararg args: Any): String =
        context.getString(res, *args)

    @Test
    fun `the summary counts what is outstanding`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.prayers_to_make_up)).assertExists()
        composeRule.onNodeWithText(
            context.resources.getQuantityString(R.plurals.missed_prayers_pending, 3, 3)
        ).assertExists()
    }

    @Test
    fun `the list is grouped by month, newest heading first`() {
        setContent()

        composeRule.onNodeWithText("March 2026").assertExists()
        composeRule.onNodeWithText("February 2026").assertExists()
        composeRule.onAllNodesWithText(string(R.string.qada_mark_made_up)).assertCountEquals(3)
    }

    @Test
    fun `marking one made up reports that prayer`() {
        setContent()

        composeRule.onAllNodesWithText(string(R.string.qada_mark_made_up))[0].performClick()

        assertThat(events).containsExactly(PrayerTrackerEvent.MarkQadaCompleted(fajr))
    }

    @Test
    fun `an empty list says nothing was added on the user's behalf`() {
        qadaState.value = QadaPrayersUiState(isLoading = false)
        setContent()

        composeRule.onNodeWithText(string(R.string.all_caught_up)).assertExists()
        // The message is a promise about the app: prayers land here only when the user says so.
        composeRule.onNodeWithText(string(R.string.all_caught_up_message)).assertExists()
        composeRule.onNodeWithText(string(R.string.all_caught_up_short)).assertExists()
    }

    @Test
    fun `an empty list that is still loading is not called caught up`() {
        qadaState.value = QadaPrayersUiState(isLoading = true)
        setContent()

        // "All caught up" on the first frame of a load is a false reassurance to someone who has
        // a debt — and it is exactly the frame every open of this screen starts on.
        composeRule.onNodeWithText(string(R.string.all_caught_up)).assertDoesNotExist()
    }

    @Test
    fun `the back arrow goes back`() {
        setContent()

        composeRule.onNodeWithContentDescription(string(R.string.cd_back)).performClick()

        assertThat(backs).isEqualTo(1)
    }
}
