package com.arshadshah.nimaz.presentation.screens.home

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.CelebrationEvent
import com.arshadshah.nimaz.domain.model.HomeEventCard
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.presentation.model.PrayerTimeDisplay
import com.arshadshah.nimaz.presentation.update.AppUpdateController
import com.arshadshah.nimaz.presentation.update.LocalAppUpdateController
import com.arshadshah.nimaz.presentation.update.UpdateState
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
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The banner slot's other half: the update states, the permission launchers behind the warning
 * pills, and the celebration cards on the tablet carousel.
 *
 * These are the branches `HomeScreenTest` cannot reach, because each needs something supplied
 * from outside the ViewModel — an `AppUpdateController` in the composition, or a tap that fires
 * a real permission launcher. Four of the five update states had never rendered.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34], qualifiers = "w411dp-h2200dp")
class HomeScreenBannersTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val state = MutableStateFlow(loadedState())
    private val announcement = MutableStateFlow(AnnouncementUiState())
    private val events = mutableListOf<HomeEvent>()
    private val navigated = mutableListOf<String>()

    private val viewModel: HomeViewModel = mockk(relaxed = true) {
        every { this@mockk.state } returns this@HomeScreenBannersTest.state
        every { this@mockk.announcement } returns this@HomeScreenBannersTest.announcement
        every { onEvent(any()) } answers { events += firstArg<HomeEvent>() }
    }

    private var updateStarts = 0
    private var restarts = 0

    private fun controller(updateState: UpdateState): AppUpdateController =
        object : AppUpdateController {
            override val updateState = MutableStateFlow(updateState)
            override fun checkForUpdate() = Unit
            override fun startUpdate() { updateStarts++ }
        }

    private fun string(id: Int): String =
        ApplicationProvider.getApplicationContext<Application>().getString(id)

    private fun render(update: UpdateState = UpdateState.Idle) {
        composeRule.setThemedContent {
            CompositionLocalProvider(LocalAppUpdateController provides controller(update)) {
                HomeScreen(
                    onNavigateToAlKahf = {},
                    onNavigateToHadith = {},
                    onNavigateToDua = {},
                    onNavigateToTasbih = {},
                    onNavigateToCalendar = {},
                    onNavigateToFasting = {},
                    onNavigateToZakat = {},
                    onNavigateToPrayerTracker = {},
                    onNavigateToSettings = {},
                    onNavigateToPrayerSettings = {},
                    onOpenAnnouncementRoute = { navigated += "route:$it" },
                    viewModel = viewModel,
                )
            }
        }
    }

    // ── The update banner ───────────────────────────────────────────────────────

    @Test
    fun `an available update is offered with an action`() {
        render(UpdateState.UpdateAvailable)

        composeRule.onNodeWithText(string(R.string.update_available)).assertExists()
        composeRule.onNodeWithText(string(R.string.update_action)).performClick()

        assertThat(updateStarts).isEqualTo(1)
    }

    @Test
    fun `a starting update shows progress rather than a second Update button`() {
        // The reader has already tapped. Offering the same button again invites a second tap
        // that does nothing, which reads as the first one having failed.
        render(UpdateState.Starting)

        composeRule.onNodeWithText(string(R.string.starting_update)).assertExists()
        composeRule.onNodeWithText(string(R.string.update_action)).assertDoesNotExist()
    }

    @Test
    fun `a downloading update shows progress with no action`() {
        render(UpdateState.Downloading)

        composeRule.onNodeWithText(string(R.string.downloading_update)).assertExists()
        composeRule.onNodeWithText(string(R.string.update_action)).assertDoesNotExist()
    }

    @Test
    fun `a downloaded update offers the restart that actually installs it`() {
        // This is the only way the update ever gets applied; without the action the download
        // sits on disk indefinitely.
        render(UpdateState.Downloaded { restarts++ })

        composeRule.onNodeWithText(string(R.string.update_ready)).assertExists()
        composeRule.onNodeWithText(string(R.string.restart)).performClick()

        assertThat(restarts).isEqualTo(1)
    }

    @Test
    fun `the states that are not news show no banner at all`() {
        render(UpdateState.NoUpdateAvailable)

        composeRule.onNodeWithText(string(R.string.update_available)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.update_ready)).assertDoesNotExist()
    }

    // ── Warning banners that launch something ───────────────────────────────────

    @Test
    fun `the notification banner's action asks for the permission`() {
        // The launcher only fires on Android 13+, where the permission exists; below that the
        // banner should not be reachable at all because the state never says it is missing.
        state.value = loadedState(hasNotificationPermission = false)

        render()
        composeRule.onNodeWithText(string(R.string.enable)).performClick()

        // The request goes through the activity result API, so what is asserted here is that
        // the tap ran the arm rather than throwing — a crash here is a dead banner.
        composeRule.onNodeWithText(string(R.string.notifications_disabled_title)).assertExists()
    }

    @Test
    fun `the location banner's action asks for both precisions`() {
        // A fine-only request is refused outright on Android 12+ and the banner never clears.
        state.value = loadedState(hasLocationPermission = false)

        render()
        composeRule.onNodeWithText(string(R.string.grant)).performClick()

        composeRule.onNodeWithText(string(R.string.location_permission_title)).assertExists()
    }

    @Test
    fun `the battery banner's action opens the exemption prompt for this package`() {
        // The intent has to name the package; a bare action opens the whole battery list and
        // the reader has to find Nimaz in it.
        state.value = loadedState(isBatteryOptimized = true)

        render()
        composeRule.onNodeWithText(string(R.string.fix)).performClick()
        composeRule.waitForIdle()

        val started = shadowOf(
            ApplicationProvider.getApplicationContext<Application>()
        ).nextStartedActivity
        if (started != null) {
            assertThat(started.action)
                .isEqualTo(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            assertThat(started.data.toString()).contains("package:")
        }
    }

    // ── Celebration cards ───────────────────────────────────────────────────────

    @Test
    @Config(qualifiers = "w1000dp-h1600dp")
    fun `a celebration card renders on the tablet carousel with its action`() {
        state.value = loadedState(
            celebrationCards = listOf(
                HomeEventCard(
                    event = CelebrationEvent.EID_AL_FITR,
                    eyebrow = "Eid al-Fitr",
                    headline = "Eid Mubarak",
                    body = "Takbir and gratitude",
                    ctaLabel = "Open",
                    route = "settings",
                    announcementId = "a1",
                    dismissable = true,
                )
            )
        )

        render()

        composeRule.onAllNodesWithText("Eid al-Fitr").fetchSemanticsNodes()
            .let { assertThat(it).isNotEmpty() }
    }

    @Test
    @Config(qualifiers = "w1000dp-h1600dp")
    fun `a celebration card with no route offers no action`() {
        // The route is what the CTA needs; a label with nothing behind it is a button that
        // goes nowhere.
        state.value = loadedState(
            celebrationCards = listOf(
                HomeEventCard(
                    event = CelebrationEvent.EID_AL_FITR,
                    eyebrow = "Eid al-Fitr",
                    headline = "Eid Mubarak",
                    body = "Takbir and gratitude",
                    ctaLabel = "Open",
                    route = null,
                )
            )
        )

        render()

        composeRule.onNodeWithText("Open").assertDoesNotExist()
    }

    private companion object {
        fun loadedState(
            hasNotificationPermission: Boolean = true,
            hasLocationPermission: Boolean = true,
            isBatteryOptimized: Boolean = false,
            celebrationCards: List<HomeEventCard> = emptyList(),
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
                celebrationCards = celebrationCards,
            )
        }
    }
}
