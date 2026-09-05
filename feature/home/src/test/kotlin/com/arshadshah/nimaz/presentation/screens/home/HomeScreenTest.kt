package com.arshadshah.nimaz.presentation.screens.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasText
import androidx.test.core.app.ApplicationProvider
import android.app.Application
import com.arshadshah.nimaz.core.navigation.ScreenTags
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.Announcement
import com.arshadshah.nimaz.domain.model.AnnouncementType
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.domain.model.WorshipReminderType
import com.arshadshah.nimaz.presentation.components.organisms.WorshipCardUi
import com.arshadshah.nimaz.presentation.model.DailyDua
import com.arshadshah.nimaz.presentation.model.PrayerTimeDisplay
import com.arshadshah.nimaz.presentation.viewmodel.UiError
import com.arshadshah.nimaz.presentation.viewmodel.home.AnnouncementUiState
import com.arshadshah.nimaz.presentation.viewmodel.home.HomeEvent
import com.arshadshah.nimaz.presentation.viewmodel.home.HomeUiState
import com.arshadshah.nimaz.presentation.viewmodel.home.HomeViewModel
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The dashboard, and the only screen `:app` still owns.
 *
 * `HomeScreen.kt` is 783 lines with **two complete layouts** — a phone `LazyColumn` and a
 * two-column tablet arrangement — and it was at 0%: nothing had ever composed it. That matters
 * more here than on a leaf, because the screen is where the app's state becomes *arrangement*:
 * which banner is shown, whether the error takes the whole screen or stays in a card, and which
 * of the two layouts a given window gets. None of that is visible in the ViewModel's tests.
 *
 * The ViewModel is mocked so state can be pushed a field at a time (playbook item 3). Window size
 * is chosen per test with `@Config(qualifiers = …)`, because the compact/tablet fork is the one
 * branch that cannot be reached any other way.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34], qualifiers = "w411dp-h2200dp")
class HomeScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val state = MutableStateFlow(HomeUiState(isLoading = false))
    private val announcement = MutableStateFlow(AnnouncementUiState())
    private val events = mutableListOf<HomeEvent>()

    private val viewModel: HomeViewModel = mockk(relaxed = true) {
        every { this@mockk.state } returns this@HomeScreenTest.state
        every { this@mockk.announcement } returns this@HomeScreenTest.announcement
        every { onEvent(any()) } answers { events += firstArg<HomeEvent>() }
    }

    private val navigated = mutableListOf<String>()

    private fun setContent() {
        composeRule.setThemedContent {
            HomeScreen(
                onNavigateToAlKahf = { navigated += "alkahf" },
                onNavigateToHadith = { navigated += "hadith" },
                onNavigateToDua = { navigated += "dua:$it" },
                onNavigateToTasbih = { navigated += "tasbih" },
                onNavigateToCalendar = { navigated += "calendar" },
                onNavigateToFasting = { navigated += "fasting" },
                onNavigateToZakat = { navigated += "zakat" },
                onNavigateToPrayerTracker = { navigated += "tracker" },
                onNavigateToSettings = { navigated += "settings" },
                onNavigateToPrayerSettings = { navigated += "prayerSettings" },
                onNavigateToPrayerTimes = { navigated += "prayerTimes" },
                onOpenHadith = { navigated += "openHadith:$it" },
                onOpenAnnouncementRoute = { navigated += "route:$it" },
                onOpenWorship = { navigated += "worship:${it.key}" },
                viewModel = viewModel,
            )
        }
    }

    private fun string(id: Int): String =
        ApplicationProvider.getApplicationContext<Application>().getString(id)

    // ── The three top-level arms ────────────────────────────────────────────────

    @Test
    fun `the loading state replaces the whole dashboard, not one card`() {
        // Deliberate and worth pinning: this is the one loader the entire layout is arranged
        // around — without today's prayer times there is no hero, no countdown and no tracker row.
        state.value = HomeUiState(isLoading = true)

        composeRule.mainClock.autoAdvance = false
        setContent()

        composeRule.onNodeWithTag(ScreenTags.HomeList).assertDoesNotExist()
    }

    @Test
    fun `a prayer-times failure takes the screen and offers a retry that reloads`() {
        state.value = HomeUiState(
            isLoading = false,
            error = UiError(message = R.string.home_prayer_times_failed_body),
        )

        setContent()
        composeRule.onNodeWithText(string(R.string.try_again)).performClick()

        assertThat(events).contains(HomeEvent.RefreshPrayerTimes)
    }

    @Test
    fun `a normal state renders the scrolling dashboard`() {
        state.value = loadedState()

        setContent()

        composeRule.onNodeWithTag(ScreenTags.HomeList).assertExists()
    }

    // ── Banners ─────────────────────────────────────────────────────────────────

    @Test
    fun `no banner slot exists when every prerequisite is satisfied`() {
        // The slot collapses rather than rendering an empty pill row — an always-present
        // empty strip under the hero is the thing this arrangement avoids.
        state.value = loadedState()

        setContent()

        composeRule.onNodeWithText(string(R.string.notifications_disabled_title))
            .assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.location_permission_title))
            .assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.battery_optimization_title))
            .assertDoesNotExist()
    }

    @Test
    fun `a missing notification permission is surfaced as its own banner`() {
        // Without this the app is silent and the user has no idea why: the alarms are armed,
        // Android just drops every notification.
        state.value = loadedState(hasNotificationPermission = false)

        setContent()

        composeRule.onNodeWithText(string(R.string.notifications_disabled_title)).assertExists()
    }

    @Test
    fun `a missing location permission is surfaced as its own banner`() {
        state.value = loadedState(hasLocationPermission = false)

        setContent()

        composeRule.onNodeWithText(string(R.string.location_permission_title)).assertExists()
    }

    @Test
    fun `battery optimisation is surfaced as its own banner`() {
        // The quietest failure of the three: alarms are armed and permitted, and Doze delays
        // them past the prayer they were for.
        state.value = loadedState(isBatteryOptimized = true)

        setContent()

        composeRule.onNodeWithText(string(R.string.battery_optimization_title)).assertExists()
    }

    @Test
    fun `three warnings queue behind one banner rather than stacking down the page`() {
        // The slot is a queue, not a stack. Rendering all three in place is what used to push
        // the prayer card below the fold on a fresh install — the state where all three apply.
        state.value = loadedState(
            hasNotificationPermission = false,
            hasLocationPermission = false,
            isBatteryOptimized = true,
        )

        setContent()

        // Warnings come before updates and in a fixed order, so notifications is the one shown.
        composeRule.onNodeWithText(string(R.string.notifications_disabled_title)).assertExists()
        composeRule.onNodeWithText(string(R.string.location_permission_title)).assertDoesNotExist()
        composeRule.onNodeWithText(
            ApplicationProvider.getApplicationContext<Application>()
                .getString(R.string.home_n_more_banners, 2)
        ).assertExists()
    }

    @Test
    fun `the overflow opens a sheet holding the warnings that did not fit`() {
        state.value = loadedState(
            hasNotificationPermission = false,
            hasLocationPermission = false,
            isBatteryOptimized = true,
        )

        setContent()
        composeRule.onNodeWithText(
            ApplicationProvider.getApplicationContext<Application>()
                .getString(R.string.home_n_more_banners, 2)
        ).performClick()
        composeRule.waitForIdle()

        // The sheet slides in, so its content is attached but parked — assertExists, not
        // assertIsDisplayed.
        composeRule.onAllNodesWithText(string(R.string.location_permission_title))
            .fetchSemanticsNodes().let { assertThat(it).isNotEmpty() }
        composeRule.onAllNodesWithText(string(R.string.battery_optimization_title))
            .fetchSemanticsNodes().let { assertThat(it).isNotEmpty() }
    }

    // ── Announcements ───────────────────────────────────────────────────────────

    @Test
    fun `an announcement renders in the banner slot alongside the warnings`() {
        state.value = loadedState()
        announcement.value = AnnouncementUiState(
            announcement = announcement(type = AnnouncementType.CHANGELOG),
            showCta = false,
        )

        setContent()

        composeRule.onNodeWithText("What's new").assertExists()
    }

    @Test
    fun `tapping an announcement CTA both records the tap and opens its route`() {
        // Two things have to happen, and they are easy to get half-right: the analytics event
        // and the navigation. A CTA that navigates without recording looks fine and reports
        // nothing; one that records without navigating looks broken.
        state.value = loadedState()
        announcement.value = AnnouncementUiState(
            announcement = announcement(
                type = AnnouncementType.CHANGELOG,
                ctaLabel = "See it",
                route = "settings",
            ),
            showCta = true,
        )

        setContent()
        composeRule.onNodeWithText("See it").performClick()

        assertThat(events).contains(HomeEvent.AnnouncementCtaClicked)
        assertThat(navigated).contains("route:settings")
    }

    @Test
    fun `an announcement with no CTA shown never offers the button`() {
        state.value = loadedState()
        announcement.value = AnnouncementUiState(
            announcement = announcement(
                type = AnnouncementType.CHANGELOG,
                ctaLabel = "See it",
                route = "settings",
            ),
            showCta = false,
        )

        setContent()

        composeRule.onNodeWithText("See it").assertDoesNotExist()
    }

    // ── The prayer card and the tracker row ─────────────────────────────────────

    @Test
    fun `every prayer of the day is on the card`() {
        state.value = loadedState()

        setContent()

        PrayerType.entries.filter { it != PrayerType.SUNRISE }.forEach {
            composeRule.onAllNodesWithText(it.displayName).fetchSemanticsNodes().let { nodes ->
                assertThat(nodes).isNotEmpty()
            }
        }
    }

    @Test
    fun `the also-today section is below the fold and reachable by scrolling`() {
        // It sits under the hero, the banner slot and the prayer card, which is exactly the
        // arrangement a test that only ever asserts on the first screenful cannot check.
        state.value = loadedState(
            dailyDua = DailyDua(
                duaId = "d1",
                title = "Dua for the morning",
                arabic = "اللهم",
                translation = "O Allah",
                source = "Hisn",
                categoryLabel = "Morning",
                categoryIcon = "sun",
            ),
        )

        setContent()
        composeRule.onNode(hasScrollAction())
            .performScrollToNode(hasText(string(R.string.home_also_today)))

        composeRule.onNodeWithText(string(R.string.dua_of_the_moment)).assertExists()
    }

    @Test
    fun `tapping the worship card opens that reminders destination`() {
        val now = Clock.System.now()
        state.value = loadedState(
            worshipCard = WorshipCardUi(
                type = WorshipReminderType.TAHAJJUD,
                name = "Tahajjud",
                arabic = "تهجد",
                body = "The last third of the night",
                eventAt = now + 3.hours,
            ),
        )

        setContent()
        composeRule.onNode(hasScrollAction())
            .performScrollToNode(hasText(string(R.string.home_also_today)))
        composeRule.onNode(
            hasText("Tahajjud") and androidx.compose.ui.test.hasClickAction()
        ).performClick()

        assertThat(navigated).contains("worship:tahajjud")
    }

    // ── The tablet fork ─────────────────────────────────────────────────────────

    @Test
    @Config(qualifiers = "w1000dp-h1200dp")
    fun `a wide window gets the two-column layout, not the phone list`() {
        // The fork is on window size class, so nothing but the qualifier can reach it — and the
        // two layouts do not share a single composable below the banner list.
        state.value = loadedState()

        setContent()

        composeRule.onNodeWithTag(ScreenTags.HomeList).assertDoesNotExist()
        // The right-hand column's "Today" header exists only in the tablet arrangement.
        composeRule.onAllNodesWithText(string(R.string.today)).fetchSemanticsNodes()
            .let { assertThat(it).isNotEmpty() }
    }

    @Test
    @Config(qualifiers = "w1000dp-h1200dp")
    fun `the tablet layout keeps the same warning banners`() {
        state.value = loadedState(hasNotificationPermission = false)

        setContent()

        composeRule.onNodeWithText(string(R.string.notifications_disabled_title)).assertExists()
    }

    @Test
    @Config(qualifiers = "w1000dp-h1200dp")
    fun `the tablet prayer column opens the tracker when a prayer is tapped`() {
        state.value = loadedState()

        setContent()
        // The prayer name also appears in the header summary, which is not a target — address
        // the row that actually carries a click action.
        composeRule.onNode(
            hasText(PrayerType.DHUHR.displayName) and androidx.compose.ui.test.hasClickAction()
        ).performClick()

        assertThat(navigated).contains("tracker")
    }

    @Test
    @Config(qualifiers = "w1000dp-h1200dp")
    fun `the tablet header carries the location the times were calculated for`() {
        // The header is the only place the tablet layout says *where* these times are for; the
        // compact layout puts it in the morphing top bar instead.
        state.value = loadedState()

        setContent()

        composeRule.onAllNodesWithText("London").fetchSemanticsNodes()
            .let { assertThat(it).isNotEmpty() }
    }

    @Test
    @Config(qualifiers = "w1000dp-h1200dp")
    fun `a friday shows the jumuah card on the tablet carousel`() {
        state.value = loadedState(isFriday = true, jumuahAt = Clock.System.now() + 2.hours)

        setContent()

        composeRule.onAllNodesWithText(string(R.string.jumuah_mubarak)).fetchSemanticsNodes()
            .let { assertThat(it).isNotEmpty() }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    private fun announcement(
        type: AnnouncementType,
        ctaLabel: String? = null,
        route: String? = null,
    ) = Announcement(
        id = "a1",
        type = type,
        title = "What's new",
        body = "A new version landed",
        ctaLabel = ctaLabel,
        route = route,
    )

    private fun loadedState(
        hasNotificationPermission: Boolean = true,
        hasLocationPermission: Boolean = true,
        isBatteryOptimized: Boolean = false,
        isFriday: Boolean = false,
        jumuahAt: Instant? = null,
        dailyDua: DailyDua? = null,
        worshipCard: WorshipCardUi? = null,
    ): HomeUiState {
        val now = Clock.System.now()
        return HomeUiState(
            isLoading = false,
            hijriDate = "7 Rajab 1446",
            locationName = "London",
            prayerTimes = PrayerType.entries.mapIndexed { index, type ->
                PrayerTimeDisplay(
                    type = type,
                    name = type.displayName,
                    timeAt = now + (index - 2).hours,
                )
            },
            tomorrowFajrAt = now + 20.hours,
            hasNotificationPermission = hasNotificationPermission,
            hasLocationPermission = hasLocationPermission,
            isBatteryOptimized = isBatteryOptimized,
            isFriday = isFriday,
            jumuahAt = jumuahAt,
            dailyDua = dailyDua,
            worshipCard = worshipCard,
        )
    }
}
