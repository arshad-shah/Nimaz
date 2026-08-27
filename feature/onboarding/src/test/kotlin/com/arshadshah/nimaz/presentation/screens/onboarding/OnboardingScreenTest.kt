package com.arshadshah.nimaz.presentation.screens.onboarding

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.platform.LocalContext
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.presentation.viewmodel.onboarding.OnboardingEvent
import com.arshadshah.nimaz.presentation.viewmodel.onboarding.OnboardingUiState
import com.arshadshah.nimaz.presentation.viewmodel.onboarding.OnboardingViewModel
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The first-run walkthrough: three info pages, a permissions page, and exactly one way out.
 *
 * **The stakes are asymmetric here in a way no other screen's are.** `NavGraph` reads
 * `onboardingCompleted` once, when the graph is built, to choose its start destination — so a
 * flow that fails to emit `CompleteOnboarding` does not cost the user a screen, it puts them
 * back in onboarding on every launch for ever. The two events that decide it (`CompleteOnboarding`
 * and the `onComplete` navigation) are dispatched from two different buttons, on two different
 * pages, and both are guarded by a page comparison; there is no ViewModel test that can see
 * whether the right button is on the right page.
 *
 * The other half is the funnel. `OnboardingEvent.SetCurrentPage` is emitted by a `snapshotFlow`
 * watching the pager, not by the buttons — the KDoc in `OnboardingScreen` records that this
 * analytic fired zero times in production while the pager drove itself locally — so what is
 * pinned below is that *paging* reports the step, whichever way the page was reached.
 *
 * The ViewModel is a relaxed mock over a real `MutableStateFlow`, per #604: the screen takes it as
 * a parameter, so nothing here needs Hilt.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class OnboardingScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val state = MutableStateFlow(OnboardingUiState(isLoading = false))
    private val events = mutableListOf<OnboardingEvent>()
    private var completed = 0

    private val viewModel: OnboardingViewModel = mockk(relaxed = true) {
        every { this@mockk.state } returns this@OnboardingScreenTest.state
        every { onEvent(any()) } answers { events += firstArg<OnboardingEvent>() }
    }

    private fun str(@StringRes id: Int): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id)

    /** The host activity, captured from the composition — the screen itself reaches for it. */
    private lateinit var activity: ComponentActivity

    private fun launch() {
        composeRule.setThemedContent {
            activity = LocalContext.current as ComponentActivity
            OnboardingScreen(onComplete = { completed++ }, viewModel = viewModel)
        }
        composeRule.waitForIdle()
    }

    /** Advances the pager the way a user does — the Next button, one page at a time. */
    private fun tapNext(times: Int = 1) = repeat(times) {
        composeRule.onNodeWithText(str(R.string.onboarding_next)).performClick()
        composeRule.waitForIdle()
    }

    private fun pagesReached(): List<Int> =
        events.filterIsInstance<OnboardingEvent.SetCurrentPage>().map { it.page }

    // ------------------------------------------------------------------
    // Paging
    // ------------------------------------------------------------------

    @Test
    fun `the walkthrough opens on the welcome page with nothing to go back to`() {
        launch()

        composeRule.onNodeWithText(str(R.string.onboarding_welcome_title)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.onboarding_feature_quran)).assertIsDisplayed()
        // Back is hidden rather than disabled on the first page: a Back that did nothing would
        // read as a flow that has already broken.
        composeRule.onAllNodesWithText(str(R.string.onboarding_back)).assertCountEquals(0)
    }

    @Test
    fun `each page carries its own copy`() {
        // Three pages built from one `InfoPage` list. An off-by-one in the pager's
        // `if (page < infoPages.size)` shows up as the wrong page's title, or the permissions
        // page appearing one step early.
        launch()

        tapNext()
        composeRule.onNodeWithText(str(R.string.onboarding_prayer_title)).assertIsDisplayed()

        tapNext()
        composeRule.onNodeWithText(str(R.string.onboarding_quran_title)).assertIsDisplayed()

        tapNext()
        composeRule.onNodeWithText(str(R.string.onboarding_permissions_title)).assertIsDisplayed()
    }

    @Test
    fun `every page reached is reported to the funnel, in order`() {
        // The drop-off graph is read by comparing consecutive steps, so a step that is skipped or
        // reported twice makes the whole measurement unreadable.
        launch()

        tapNext(3)

        assertThat(pagesReached()).containsExactly(0, 1, 2, 3).inOrder()
    }

    @Test
    fun `back returns to the previous page and reports it`() {
        launch()
        tapNext(2)

        composeRule.onNodeWithText(str(R.string.onboarding_back)).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(str(R.string.onboarding_prayer_title)).assertIsDisplayed()
        assertThat(pagesReached()).containsExactly(0, 1, 2, 1).inOrder()
    }

    // ------------------------------------------------------------------
    // The one way out
    // ------------------------------------------------------------------

    @Test
    fun `the last page swaps Next for Get Started`() {
        launch()

        tapNext(3)

        composeRule.onNodeWithText(str(R.string.onboarding_get_started)).assertIsDisplayed()
        composeRule.onAllNodesWithText(str(R.string.onboarding_next)).assertCountEquals(0)
    }

    @Test
    fun `finishing persists completion and navigates away exactly once`() {
        launch()
        tapNext(3)

        composeRule.onNodeWithText(str(R.string.onboarding_get_started)).performClick()
        composeRule.waitForIdle()

        assertThat(events.filterIsInstance<OnboardingEvent.CompleteOnboarding>()).hasSize(1)
        assertThat(completed).isEqualTo(1)
    }

    @Test
    fun `no page before the last one completes onboarding`() {
        // `Next` and `Get Started` are the same button under a page comparison. Get that
        // comparison wrong and the walkthrough ends on page one — or, the other way round,
        // never ends at all.
        launch()

        tapNext(3)

        assertThat(events.filterIsInstance<OnboardingEvent.CompleteOnboarding>()).isEmpty()
        assertThat(completed).isEqualTo(0)
    }

    @Test
    fun `skip finishes the flow from an early page`() {
        launch()

        composeRule.onNodeWithText(str(R.string.onboarding_skip)).performClick()
        composeRule.waitForIdle()

        assertThat(events.filterIsInstance<OnboardingEvent.CompleteOnboarding>()).hasSize(1)
        assertThat(completed).isEqualTo(1)
    }

    @Test
    fun `skip is gone from the last page`() {
        // Two buttons that both complete onboarding, side by side, is how a flow ends up
        // dispatching completion twice and navigating twice.
        launch()

        tapNext(3)

        composeRule.onAllNodesWithText(str(R.string.onboarding_skip)).assertCountEquals(0)
    }

    // ------------------------------------------------------------------
    // The permissions page
    // ------------------------------------------------------------------

    @Test
    fun `an ungranted permission offers a way to grant it`() {
        launch()
        tapNext(3)

        composeRule.onNodeWithText(str(R.string.onboarding_location_title)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.onboarding_notification_title)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.onboarding_battery_title)).assertIsDisplayed()
        composeRule.onAllNodesWithText(str(R.string.onboarding_grant)).assertCountEquals(3)
    }

    @Test
    fun `a granted permission says so and stops asking`() {
        state.value = state.value.copy(
            locationPermissionGranted = true,
            notificationPermissionGranted = true,
            batteryOptimizationDisabled = true,
        )
        launch()
        tapNext(3)

        composeRule.onNodeWithText(str(R.string.onboarding_location_granted)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.onboarding_notification_granted))
            .assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.onboarding_battery_granted)).assertIsDisplayed()
        // A Grant button beside "Granted" is the state the card is designed to rule out.
        composeRule.onAllNodesWithText(str(R.string.onboarding_grant)).assertCountEquals(0)
    }

    @Test
    fun `a detected location is named on the card instead of the generic label`() {
        // The card's whole job at this point is to show the location detection actually worked;
        // falling back to "Location Access Granted" when a name is in hand hides the one piece
        // of evidence the user has.
        state.value = state.value.copy(
            locationPermissionGranted = true,
            locationDetected = true,
            locationName = "Dublin, Ireland",
        )
        launch()
        tapNext(3)

        composeRule.onNodeWithText("Dublin, Ireland").assertIsDisplayed()
        composeRule.onAllNodesWithText(str(R.string.onboarding_location_granted))
            .assertCountEquals(0)
    }

    @Test
    fun `a permission granted while the page is open updates the card in place`() {
        launch()
        tapNext(3)
        composeRule.onAllNodesWithText(str(R.string.onboarding_grant)).assertCountEquals(3)

        // What the permission launchers' callbacks do: dispatch UpdatePermissionStatus, which
        // moves the state. The user comes back from the system dialog to this page.
        state.value = state.value.copy(notificationPermissionGranted = true)
        composeRule.waitForIdle()

        composeRule.onNodeWithText(str(R.string.onboarding_notification_granted))
            .assertIsDisplayed()
        composeRule.onAllNodesWithText(str(R.string.onboarding_grant)).assertCountEquals(2)
    }

    @Test
    @Config(sdk = [32])
    fun `below Tiramisu the notification card grants itself with no system prompt`() {
        // There is no POST_NOTIFICATIONS permission to request before API 33, so launching the
        // request contract there would prompt for a permission the platform does not have and
        // leave the card stuck at "not granted" on every older device.
        state.value = state.value.copy(
            locationPermissionGranted = true,
            batteryOptimizationDisabled = true,
        )
        launch()
        tapNext(3)

        composeRule.onNodeWithText(str(R.string.onboarding_grant)).performClick()
        composeRule.waitForIdle()

        assertThat(events).contains(OnboardingEvent.UpdatePermissionStatus(notification = true))
    }

    @Test
    fun `from Tiramisu the notification card asks the system rather than granting itself`() {
        // The mirror of the test above. Marking the permission granted locally on a platform
        // that does have POST_NOTIFICATIONS would leave the card green while every prayer
        // notification is silently dropped.
        state.value = state.value.copy(
            locationPermissionGranted = true,
            batteryOptimizationDisabled = true,
        )
        launch()
        tapNext(3)

        composeRule.onNodeWithText(str(R.string.onboarding_grant)).performClick()
        composeRule.waitForIdle()

        assertThat(events.filterIsInstance<OnboardingEvent.UpdatePermissionStatus>()).isEmpty()
    }

    @Test
    fun `coarse location alone counts as granted`() {
        // The launcher's callback ORs the two results. A user who picks "approximate" in the
        // system dialog — the default on the modern dialog — grants only ACCESS_COARSE_LOCATION,
        // and an AND here would tell them the permission was refused and never detect a location.
        launch()
        tapNext(3)
        composeRule.onAllNodesWithText(str(R.string.onboarding_grant)).onFirst().performClick()
        composeRule.waitForIdle()

        answerPermissionRequest(
            granted = mapOf(
                Manifest.permission.ACCESS_FINE_LOCATION to false,
                Manifest.permission.ACCESS_COARSE_LOCATION to true,
            )
        )

        assertThat(events).contains(OnboardingEvent.UpdatePermissionStatus(location = true))
    }

    @Test
    fun `a refused location dialog is reported as refused`() {
        launch()
        tapNext(3)
        composeRule.onAllNodesWithText(str(R.string.onboarding_grant)).onFirst().performClick()
        composeRule.waitForIdle()

        answerPermissionRequest(
            granted = mapOf(
                Manifest.permission.ACCESS_FINE_LOCATION to false,
                Manifest.permission.ACCESS_COARSE_LOCATION to false,
            )
        )

        assertThat(events).contains(OnboardingEvent.UpdatePermissionStatus(location = false))
    }

    /**
     * Delivers a system permission dialog's answer back to the launcher that opened it.
     *
     * `onRequestPermissionsResult` is deprecated in favour of the result APIs — which is exactly
     * what the screen uses. This is the platform callback underneath them, and it is how a
     * Robolectric test plays the part of the system dialog.
     */
    @Suppress("DEPRECATION")
    private fun answerPermissionRequest(granted: Map<String, Boolean>) {
        val request = shadowOf(activity).lastRequestedPermission
        assertThat(request).isNotNull()
        activity.onRequestPermissionsResult(
            request.requestCode,
            request.requestedPermissions,
            request.requestedPermissions.map {
                if (granted[it] == true) PackageManager.PERMISSION_GRANTED
                else PackageManager.PERMISSION_DENIED
            }.toIntArray(),
        )
        composeRule.waitForIdle()
    }

    // ------------------------------------------------------------------
    // Small screens
    //
    // Both pages switch to a compact layout below 500dp of usable height —
    // tighter spacing, a smaller emblem, smaller type. It is not cosmetic:
    // the pages are a centred, scrollable column, and at the roomy sizes the
    // permission cards alone are taller than a short screen.
    // ------------------------------------------------------------------

    @Test
    @Config(qualifiers = "w411dp-h480dp")
    fun `a short screen keeps the whole walkthrough usable`() {
        launch()

        composeRule.onNodeWithText(str(R.string.onboarding_welcome_title)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.onboarding_feature_duas)).assertExists()
        // The way forward has to survive the squeeze — it is the only control on the page that
        // the user cannot scroll to, because the nav row sits outside the scrolling column.
        composeRule.onNodeWithText(str(R.string.onboarding_next)).assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w411dp-h480dp")
    fun `a short screen still offers all three permissions and the way out`() {
        launch()

        tapNext(3)

        composeRule.onAllNodesWithText(str(R.string.onboarding_grant)).assertCountEquals(3)
        composeRule.onNodeWithText(str(R.string.onboarding_get_started)).assertIsDisplayed()
    }

    // ------------------------------------------------------------------
    // Errors
    // ------------------------------------------------------------------

    @Test
    fun `an error is shown to the user and then cleared`() {
        // Without the DismissError that follows the snackbar, `state.error` stays set, the
        // `LaunchedEffect` key never changes, and the next failure of the same kind is silent.
        launch()

        state.value = state.value.copy(error = "Could not detect location")
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Could not detect location").assertIsDisplayed()

        // The dismissal is dispatched once the snackbar has run its course, not when it appears.
        composeRule.mainClock.advanceTimeBy(10_000)
        composeRule.waitForIdle()

        assertThat(events).contains(OnboardingEvent.DismissError)
    }
}
