package com.arshadshah.nimaz.presentation.components.organisms

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.presentation.model.PrayerTimeDisplay
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The prayer card on Home, and the status picker hidden inside it.
 *
 * The picker is the part that had never run: it only appears when a **passed, non-sunrise** row
 * is expanded, so no test that merely rendered the card could reach it. It is also where the
 * app's most deliberate design decision lives — a prayer whose time has passed with nothing
 * recorded is *not* counted as missed until the reader says so, and the note that explains why
 * is inside the picker.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h1400dp")
class HomePrayerCardTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private fun string(id: Int): String =
        ApplicationProvider.getApplicationContext<Application>().getString(id)

    private fun string(id: Int, arg: Any): String =
        ApplicationProvider.getApplicationContext<Application>().getString(id, arg)

    private val now = Clock.System.now()

    private fun prayers(
        passed: Set<PrayerType> = setOf(PrayerType.FAJR, PrayerType.SUNRISE, PrayerType.DHUHR),
        statuses: Map<PrayerType, PrayerStatus> = emptyMap(),
    ) = PrayerType.entries.mapIndexed { index, type ->
        PrayerTimeDisplay(
            type = type,
            name = type.displayName,
            timeAt = if (type in passed) now - (6 - index).hours else now + (index + 1).hours,
            isPassed = type in passed,
            isNext = type == PrayerType.ASR,
            prayerStatus = statuses[type] ?: PrayerStatus.NOT_PRAYED,
        )
    }

    private val setStatuses = mutableListOf<Pair<PrayerType, PrayerStatus>>()
    private var trackerClicks = 0
    private var settingsClicks = 0

    private fun render(
        prayers: List<PrayerTimeDisplay> = prayers(),
    ) {
        composeRule.setThemedContent {
            HomePrayerCard(
                prayers = prayers,
                onSettingsClick = { settingsClicks++ },
                onTrackerClick = { trackerClicks++ },
                onTogglePrayer = {},
                onSetPrayerStatus = { type, status -> setStatuses += type to status },
            )
        }
    }

    // ── The header ──────────────────────────────────────────────────────────────

    @Test
    fun `the header counts only the prayers that are actually done`() {
        // "of 5" is the count a reader checks at a glance; counting sunrise or counting a
        // passed-but-unrecorded prayer would make it quietly wrong every day.
        render(
            prayers(
                statuses = mapOf(
                    PrayerType.FAJR to PrayerStatus.PRAYED,
                    PrayerType.DHUHR to PrayerStatus.LATE,
                )
            )
        )

        composeRule.onNodeWithText(string(R.string.home_prayers_done_of_five, 2)).assertExists()
    }

    @Test
    fun `nothing recorded reads as zero of five, not as five missed`() {
        render(prayers(statuses = emptyMap()))

        composeRule.onNodeWithText(string(R.string.home_prayers_done_of_five, 0)).assertExists()
    }

    @Test
    fun `the settings control is reachable by its accessibility label`() {
        render()

        composeRule.onNodeWithContentDescription(string(R.string.home_prayer_settings_cd))
            .performClick()

        assertThat(settingsClicks).isEqualTo(1)
    }

    @Test
    fun `the card offers a way through to the full tracker`() {
        render()

        composeRule.onNodeWithText(string(R.string.home_open_tracker)).performClick()

        assertThat(trackerClicks).isEqualTo(1)
    }

    // ── The status picker ───────────────────────────────────────────────────────

    @Test
    fun `a passed prayer expands into the four statuses a reader can record`() {
        render()

        composeRule.onNodeWithText(PrayerType.DHUHR.displayName).performClick()

        composeRule.onNodeWithText(string(R.string.on_time)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.late)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.missed)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.made_up)).assertIsDisplayed()
    }

    @Test
    fun `the picker explains that an unrecorded prayer is not yet counted as missed`() {
        // This is the app's position, not a UI detail: confirming a prayer missed is
        // something only the reader does. The note is the only place it is stated.
        render()

        composeRule.onNodeWithText(PrayerType.DHUHR.displayName).performClick()

        composeRule.onNodeWithText(string(R.string.prayer_not_recorded_note)).assertExists()
    }

    @Test
    fun `the note goes away once something has been recorded`() {
        render(prayers(statuses = mapOf(PrayerType.DHUHR to PrayerStatus.PRAYED)))

        composeRule.onNodeWithText(PrayerType.DHUHR.displayName).performClick()

        composeRule.onNodeWithText(string(R.string.prayer_not_recorded_note)).assertDoesNotExist()
    }

    @Test
    fun `picking a status reports it for that prayer`() {
        render()

        composeRule.onNodeWithText(PrayerType.DHUHR.displayName).performClick()
        composeRule.onNodeWithText(string(R.string.late)).performClick()

        assertThat(setStatuses).contains(PrayerType.DHUHR to PrayerStatus.LATE)
    }

    @Test
    fun `each of the four chips reports its own status`() {
        render()
        composeRule.onNodeWithText(PrayerType.DHUHR.displayName).performClick()

        composeRule.onNodeWithText(string(R.string.on_time)).performClick()
        composeRule.onNodeWithText(string(R.string.missed)).performClick()
        composeRule.onNodeWithText(string(R.string.made_up)).performClick()

        assertThat(setStatuses).containsAtLeast(
            PrayerType.DHUHR to PrayerStatus.PRAYED,
            PrayerType.DHUHR to PrayerStatus.MISSED,
            PrayerType.DHUHR to PrayerStatus.QADA,
        )
    }

    @Test
    fun `a prayer whose time has not come cannot be recorded yet`() {
        // Recording Isha at midday is not a thing a reader means to do, and offering it makes
        // the card's count meaningless.
        render()

        composeRule.onNodeWithText(PrayerType.ISHA.displayName).performClick()

        composeRule.onNodeWithText(string(R.string.on_time)).assertDoesNotExist()
    }

    @Test
    fun `sunrise never opens a picker, because it is not a prayer`() {
        render()

        composeRule.onNodeWithText(PrayerType.SUNRISE.displayName).performClick()

        composeRule.onNodeWithText(string(R.string.on_time)).assertDoesNotExist()
    }

    @Test
    fun `only one picker is open at a time`() {
        // Two open pickers would push the whole card past the fold and make it unclear which
        // prayer a chip belongs to.
        render(
            prayers(passed = setOf(PrayerType.FAJR, PrayerType.DHUHR))
        )

        composeRule.onNodeWithText(PrayerType.FAJR.displayName).performClick()
        composeRule.onNodeWithText(PrayerType.DHUHR.displayName).performClick()

        composeRule.onAllNodesWithText(string(R.string.on_time)).fetchSemanticsNodes()
            .let { assertThat(it).hasSize(1) }
    }

    @Test
    fun `tapping an open row again closes it`() {
        render()

        composeRule.onNodeWithText(PrayerType.DHUHR.displayName).performClick()
        composeRule.onNodeWithText(string(R.string.on_time)).assertExists()

        composeRule.onNodeWithText(PrayerType.DHUHR.displayName).performClick()

        composeRule.onNodeWithText(string(R.string.on_time)).assertDoesNotExist()
    }
}
