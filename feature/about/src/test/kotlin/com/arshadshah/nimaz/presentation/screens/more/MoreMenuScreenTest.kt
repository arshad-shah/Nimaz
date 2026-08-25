package com.arshadshah.nimaz.presentation.screens.more

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.PinnedShortcut
import com.arshadshah.nimaz.domain.model.WorshipReminderType
import com.arshadshah.nimaz.presentation.viewmodel.more.MoreEvent
import com.arshadshah.nimaz.presentation.viewmodel.more.MoreUiState
import com.arshadshah.nimaz.presentation.viewmodel.more.MoreViewModel
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
 * The menu every feature that is not a bottom-nav tab is reached from.
 *
 * Two properties are worth a test here and nothing else in the repository asserts either on the
 * JVM. The first is that **each row dispatches its own destination**: twenty rows built from
 * twenty lambdas of identical type is exactly the shape where a copy-paste sends "Fasting" to the
 * qibla, and neither the compiler nor `FeatureNavigationTest` — which taps a row and checks a tag
 * appears — can see a swap between two rows whose screens both exist. Each callback here is a
 * separate counter, so a swap fails on the row it was made in.
 *
 * The second is that **a subtitle is a claim about the app right now**, and the screen is where
 * `MoreSubtitles` meets the state that feeds it. `MoreSubtitlesTest` pins what each mapper
 * returns; these pin that the row actually renders it — a wiring the mapper test cannot see,
 * because a row passing `state.khatamJuz` where `state.qaidaLesson` belongs still resolves to a
 * perfectly well-formed string.
 *
 * Rendered at a tall viewport so the whole list composes: a `LazyColumn` at phone height composes
 * one screenful, and the Support section is the last of five.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class MoreMenuScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val state = MutableStateFlow(MoreUiState())
    private val events = mutableListOf<MoreEvent>()

    private val viewModel: MoreViewModel = mockk(relaxed = true) {
        every { this@mockk.state } returns this@MoreMenuScreenTest.state
        every { onEvent(any()) } answers { events += firstArg<MoreEvent>() }
    }

    private val opened = mutableListOf<String>()

    private fun setContent() {
        composeRule.setThemedContent {
            MoreMenuScreen(
                onNavigateToSettings = { opened += "settings" },
                onNavigateToCalendar = { opened += "calendar" },
                onNavigateToAbout = { opened += "about" },
                onNavigateToHelp = { opened += "help" },
                onShareApp = { opened += "share" },
                onRateApp = { opened += "rate" },
                onNavigateToHadith = { opened += "hadith" },
                onNavigateToFasting = { opened += "fasting" },
                onNavigateToZakat = { opened += "zakat" },
                onNavigateToDuas = { opened += "duas" },
                onNavigateToTafseer = { opened += "tafseer" },
                onNavigateToPrayerTracker = { opened += "tracker" },
                onNavigateToNightWorship = { opened += "worship" },
                onNavigateToPrayerTimes = { opened += "times" },
                onNavigateToMonthlyPrayerTimes = { opened += "monthly" },
                onNavigateToKhatam = { opened += "khatam" },
                onNavigateToNames = { opened += "names" },
                onNavigateToQaida = { opened += "qaida" },
                onNavigateToTasbih = { opened += "tasbih" },
                onNavigateToQibla = { opened += "qibla" },
                viewModel = viewModel,
            )
        }
    }

    private fun string(@StringRes res: Int, vararg args: Any): String =
        context.getString(res, *args)

    @Test
    fun `every section and its rows are on the menu`() {
        // Unpinned, because the Zakat pill and the Zakat row carry the same one word and the
        // matcher cannot tell two identical labels apart. The pin row has its own tests below.
        state.value = MoreUiState(pinnedShortcuts = emptyList())
        setContent()

        // Five section headings, in the order a reader meets them.
        listOf(
            R.string.more_pinned_title,
            R.string.daily_practice,
            R.string.learning,
            R.string.tools,
            R.string.support,
        ).forEach { composeRule.onNodeWithText(string(it)).assertExists() }

        // Every row that opens a feature. Nineteen destinations plus Settings in the app bar —
        // the same set `FeatureNavigationTest` walks on a device.
        listOf(
            R.string.prayer_tracker, R.string.fasting, R.string.night_worship_title,
            R.string.khatam_quran, R.string.qaida, R.string.names_title, R.string.hadith,
            R.string.duas, R.string.tafseer, R.string.calendar, R.string.prayer_times,
            R.string.monthly_prayer_times, R.string.zakat, R.string.about_nimaz,
            R.string.help_support, R.string.share_app, R.string.rate_us,
        ).forEach { composeRule.onNodeWithText(string(it)).assertExists() }
    }

    @Test
    fun `each row opens its own destination`() {
        state.value = MoreUiState(pinnedShortcuts = emptyList())
        setContent()

        // Tapped in menu order, and asserted as a sequence: a row wired to its neighbour's
        // lambda still records *something*, and only the order shows it up.
        listOf(
            R.string.prayer_tracker to "tracker",
            R.string.fasting to "fasting",
            R.string.night_worship_title to "worship",
            R.string.khatam_quran to "khatam",
            R.string.qaida to "qaida",
            R.string.names_title to "names",
            R.string.hadith to "hadith",
            R.string.duas to "duas",
            R.string.tafseer to "tafseer",
            R.string.calendar to "calendar",
            R.string.prayer_times to "times",
            R.string.monthly_prayer_times to "monthly",
            R.string.zakat to "zakat",
            R.string.about_nimaz to "about",
            R.string.help_support to "help",
            R.string.share_app to "share",
            R.string.rate_us to "rate",
        ).forEach { (res, _) -> composeRule.onNodeWithText(string(res)).performClick() }

        assertThat(opened).containsExactly(
            "tracker", "fasting", "worship", "khatam", "qaida", "names", "hadith", "duas",
            "tafseer", "calendar", "times", "monthly", "zakat", "about", "help", "share", "rate",
        ).inOrder()
    }

    @Test
    fun `the app bar action opens settings`() {
        setContent()

        composeRule.onNodeWithContentDescription(string(R.string.settings)).performClick()

        assertThat(opened).containsExactly("settings")
    }

    @Test
    fun `coming into view asks for a fresh worship countdown`() {
        // The countdown is a snapshot over a dozen settings rather than a live flow, so coming
        // back to More after an hour must re-ask. Without the resume effect the row keeps
        // reporting an hour-old "in 5h 12m" and nothing looks wrong.
        setContent()

        assertThat(events).contains(MoreEvent.Refresh)
    }

    @Test
    fun `rows report the state behind them`() {
        state.value = MoreUiState(
            prayersLogged = 3,
            prayersTrackable = 5,
            pendingMakeupFasts = 2,
            nextWorship = WorshipReminderType.TAHAJJUD,
            minutesUntilNextWorship = 90,
            khatamJuz = 7,
            khatamDaysAgainstPace = 0,
            qaidaLesson = 4,
            qaidaTotalLessons = 30,
            zakatHistoryLoaded = true,
            zakatDueThisYear = null,
            hijriToday = "12 Rajab 1447",
        )
        setContent()

        composeRule.onNodeWithText(string(R.string.more_tracker_logged, 3, 5)).assertExists()
        composeRule.onNodeWithText(string(R.string.more_qaida_lesson, 4, 30)).assertExists()
        composeRule.onNodeWithText(string(R.string.more_khatam_juz_on_pace, 7)).assertExists()
        // Loaded with no figure is its own sentence, and only says so once the query returned.
        composeRule.onNodeWithText(string(R.string.more_zakat_not_calculated)).assertExists()
        composeRule.onNodeWithText("12 Rajab 1447").assertExists()
    }

    @Test
    fun `a row with nothing true to report renders no subtitle`() {
        // The loading contract: absent, never a dash, a zero or a spinner. `MoreUiState`'s
        // defaults are all null, which is the state the screen opens in.
        setContent()

        composeRule.onNodeWithText(string(R.string.more_tracker_none)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.more_zakat_not_calculated)).assertDoesNotExist()
    }

    @Test
    fun `the pin row opens the destinations it names`() {
        state.value = MoreUiState(pinnedShortcuts = listOf(PinnedShortcut.QIBLA))
        setContent()

        composeRule.onNodeWithText(string(R.string.more_pin_qibla)).performClick()

        // A pill is a *view* of the menu's own lambdas — a pin with navigation of its own would
        // be the second place a destination is reached from, and the two would drift.
        assertThat(opened).containsExactly("qibla")
    }

    @Test
    // Wide, because the pin row is a `LazyRow`: at phone width it composes about four pills and
    // the rest of the enum never renders at all.
    @Config(qualifiers = "w2000dp-h2200dp")
    fun `every pinnable destination has a pill of its own`() {
        // Each pill carries the icon its own menu row uses, so a pin is recognisable as the
        // thing it opens. A shortcut added to the enum without an icon here does not fail to
        // compile — the `when` is exhaustive, but nothing checks that the pill *renders*.
        state.value = MoreUiState(pinnedShortcuts = PinnedShortcut.entries)
        setContent()

        composeRule.onNodeWithText(string(R.string.more_pin_qibla)).assertExists()
        composeRule.onNodeWithText(string(R.string.more_pin_night_worship)).assertExists()
        composeRule.onNodeWithText(string(R.string.more_pin_tasbih)).assertExists()
    }

    @Test
    fun `an empty pin row says so rather than rendering nothing`() {
        state.value = MoreUiState(pinnedShortcuts = emptyList())
        setContent()

        composeRule.onNodeWithText(string(R.string.more_pins_empty)).assertIsDisplayed()
    }

    @Test
    fun `the pinned defaults are what an untouched install shows`() {
        setContent()

        listOf(PinnedShortcut.TASBIH, PinnedShortcut.PRAYER_TRACKER, PinnedShortcut.KHATAM)
            .forEach { composeRule.onNodeWithText(string(it.labelRes())).assertExists() }
        // Zakat is the fourth default, and its pill label is the same single word as its menu
        // row's title — so both nodes carry it, which is itself the assertion.
        composeRule.onAllNodesWithText(string(R.string.more_pin_zakat)).assertCountEquals(2)
        // Not everything is pinned by default; the row would be a second menu if it were.
        composeRule.onNodeWithText(string(R.string.more_pin_qibla)).assertDoesNotExist()
    }
}
