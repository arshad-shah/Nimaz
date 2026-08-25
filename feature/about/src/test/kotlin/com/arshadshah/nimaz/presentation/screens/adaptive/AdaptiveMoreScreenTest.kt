package com.arshadshah.nimaz.presentation.screens.adaptive

import android.app.Application
import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.navigation.Route
import com.arshadshah.nimaz.domain.model.HelpTopic
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.presentation.app.AppIdentity
import com.arshadshah.nimaz.presentation.app.LocalAppIdentity
import com.arshadshah.nimaz.presentation.viewmodel.help.HelpHomeUiState
import com.arshadshah.nimaz.presentation.viewmodel.help.HelpViewModel
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
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * About, Help and More are **one destination**, and this is the thing that makes them one.
 *
 * On a phone it is the menu and nothing else; on a tablet the same menu is the list pane of a
 * list-detail scaffold, and About and Help open *beside* it rather than on top of it. That single
 * difference changes what a tap does: on a phone "About Nimaz" is a `navigate(Route.SettingsAbout)`
 * that pushes a screen, and on a tablet it must **not** navigate at all — it moves the scaffold's
 * own detail pane. A regression either way is silent. Navigating on a tablet pushes a full-screen
 * About over a layout designed to show it beside the menu; failing to navigate on a phone makes
 * the row do nothing.
 *
 * Everything else on the screen is shared between the two branches — the same `shareApp` and
 * `rateApp` lambdas are handed to both — so the pane behaviour is the whole of what is worth
 * testing twice.
 *
 * The screens inside resolve their ViewModels through `hiltViewModel()`, which is why this was
 * left uncovered in `:feature:quran` (#598). It does not actually need Hilt: `hiltViewModel()`
 * asks `LocalViewModelStoreOwner` for the ViewModel and only reaches for a Hilt factory when the
 * owner supplies a default one, so an owner whose store is **pre-seeded** hands back the mock
 * before any factory is consulted. That is what [seededOwner] does, and it is the difference
 * between this file existing and 142 lines staying at zero.
 */
@RunWith(RobolectricTestRunner::class)
class AdaptiveMoreScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val moreState = MutableStateFlow(MoreUiState())
    private val helpState = MutableStateFlow(HelpHomeUiState(isLoading = false))

    private val moreViewModel: MoreViewModel = mockk(relaxed = true) {
        every { state } returns moreState
    }
    private val helpViewModel: HelpViewModel = mockk(relaxed = true) {
        every { homeState } returns helpState
    }

    private val navigated = mutableListOf<Route>()

    private fun setContent() {
        // Unpinned: the Zakat pill and the Zakat row carry the same one word, and the pin row
        // is `MoreMenuScreenTest`'s subject rather than this file's.
        moreState.value = MoreUiState(pinnedShortcuts = emptyList())
        composeRule.setThemedContent {
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides seededOwner(),
                LocalAppIdentity provides AppIdentity("3.1.4", 415, R.drawable.ic_dua),
            ) {
                AdaptiveMoreScreen(onNavigate = { navigated += it })
            }
        }
    }

    private fun string(@StringRes res: Int): String = context.getString(res)

    @Test
    @Config(qualifiers = "w411dp-h2200dp")
    fun `on a phone the menu is the whole screen`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.about_nimaz)).assertExists()
        composeRule.onNodeWithText(string(R.string.help_support)).assertExists()
        composeRule.onNodeWithText(string(R.string.prayer_tracker)).assertExists()
    }

    @Test
    @Config(qualifiers = "w411dp-h2200dp")
    fun `on a phone About and Help push their own destinations`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.about_nimaz)).performClick()
        composeRule.onNodeWithText(string(R.string.help_support)).performClick()

        assertThat(navigated).containsExactly(Route.SettingsAbout, Route.SettingsHelp).inOrder()
    }

    @Test
    @Config(qualifiers = "w411dp-h2200dp")
    fun `the menu rows that are not panes navigate on any size`() {
        setContent()

        composeRule.onNodeWithContentDescription(string(R.string.settings)).performClick()

        assertThat(navigated).containsExactly(Route.Settings)
    }

    @Test
    @Config(qualifiers = "w411dp-h2200dp")
    fun `every feature row maps to a route`() {
        setContent()

        listOf(
            R.string.prayer_tracker, R.string.fasting, R.string.night_worship_title,
            R.string.khatam_quran, R.string.qaida, R.string.names_title, R.string.hadith,
            R.string.duas, R.string.tafseer, R.string.calendar, R.string.prayer_times,
            R.string.monthly_prayer_times, R.string.zakat,
        ).forEach { composeRule.onNodeWithText(string(it)).performClick() }

        assertThat(navigated).containsExactly(
            Route.PrayerTracker, Route.FastingHome, Route.NightWorship, Route.KhatamList,
            Route.QaidaHome, Route.Names(), Route.HadithHome, Route.DuaHome,
            Route.TafseerChapters, Route.IslamicCalendar, Route.PrayerTimes,
            Route.MonthlyPrayerTimes, Route.ZakatCalculator,
        ).inOrder()
    }

    @Test
    @Config(qualifiers = "w411dp-h2200dp")
    fun `sharing the app opens the share sheet rather than a destination`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.share_app)).performClick()
        composeRule.waitForIdle()

        // The invite card is built and shared in place; there is no route for it, so a
        // `navigate` here would be a bug that shows up as an empty screen.
        assertThat(navigated).isEmpty()
    }

    @Test
    @Config(qualifiers = "w411dp-h2200dp")
    fun `rating the app stays in the app rather than navigating`() {
        setContent()

        // The Play in-app review flow needs an `Activity`, which is why the handler reaches for
        // one from the composition rather than taking a lambda. With no Play services behind it
        // the flow cannot complete — what must hold is that the tap is absorbed rather than
        // throwing out of the composition or pushing a destination.
        composeRule.onNodeWithText(string(R.string.rate_us)).performClick()
        composeRule.waitForIdle()

        assertThat(navigated).isEmpty()
    }

    @Test
    @Config(qualifiers = "w1000dp-h2400dp")
    fun `on a tablet About opens beside the menu rather than over it`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.about_nimaz)).performClick()
        composeRule.waitForIdle()

        // The pane, not a push: the About surface appears while the menu is still on screen.
        composeRule.onAllNodesWithText(string(R.string.about_tagline)).onFirst().assertExists()
        composeRule.onNodeWithText(string(R.string.prayer_tracker)).assertExists()
        assertThat(navigated).isEmpty()
    }

    @Test
    @Config(qualifiers = "w1000dp-h2400dp")
    fun `on a tablet Help opens beside the menu rather than over it`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.help_support)).performClick()
        composeRule.waitForIdle()

        composeRule.onAllNodesWithText(string(R.string.help_still_need)).onFirst().assertExists()
        assertThat(navigated).isEmpty()
    }

    @Test
    @Config(qualifiers = "w1000dp-h2400dp")
    fun `on a tablet the licence list is still a push, because it is not a pane`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.about_nimaz)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(string(R.string.open_source_licenses)).performClick()

        assertThat(navigated).containsExactly(Route.Licenses)
    }

    @Test
    @Config(qualifiers = "w1000dp-h2400dp")
    fun `the About pane's own links work from inside the scaffold`() {
        // The pane rebuilds About's lambdas rather than reusing the graph's, so each one is a
        // second implementation that can drift from the phone's. Privacy and Terms are external
        // pages, and contacting support is a mail intent — none of them a route.
        setContent()
        composeRule.onNodeWithText(string(R.string.about_nimaz)).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(string(R.string.privacy_policy)).performClick()
        assertThat(startedUri()).isEqualTo("https://nimaz.arshadshah.com/privacy")

        composeRule.onNodeWithText(string(R.string.terms_of_service)).performClick()
        assertThat(startedUri()).isEqualTo("https://nimaz.arshadshah.com/terms")

        composeRule.onNodeWithText(string(R.string.contact_support)).performClick()
        assertThat(shadowOf(ApplicationProvider.getApplicationContext<Application>())
            .nextStartedActivity).isNotNull()

        assertThat(navigated).isEmpty()
    }

    @Test
    @Config(qualifiers = "w1000dp-h2400dp")
    fun `closing the About pane leaves the menu standing`() {
        // Back inside a list-detail scaffold pops the *pane*, not the destination — a
        // `popBackStack` here would take the whole More tab off the stack on a tablet.
        setContent()
        composeRule.onNodeWithText(string(R.string.about_nimaz)).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription(string(R.string.cd_back)).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(string(R.string.prayer_tracker)).assertExists()
        assertThat(navigated).isEmpty()
    }

    @Test
    @Config(qualifiers = "w1000dp-h2400dp")
    fun `a help topic opened from the pane is still a push`() {
        // Help's *topics* have no pane of their own, so the row has to leave the scaffold.
        helpState.value = HelpHomeUiState(topics = listOf(helpTopic), isLoading = false)
        setContent()
        composeRule.onNodeWithText(string(R.string.help_support)).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Prayer times").performClick()

        assertThat(navigated).containsExactly(Route.HelpTopicDetail("prayer"))
    }

    private fun startedUri(): String? =
        shadowOf(ApplicationProvider.getApplicationContext<Application>())
            .nextStartedActivity?.data?.toString()

    private val helpTopic = HelpTopic(
        id = "prayer",
        iconKey = "schedule",
        colorKey = "indigo",
        title = "Prayer times",
        subtitle = "",
        order = 0,
        itemCount = 1,
    )

    /**
     * A [ViewModelStoreOwner] whose store already holds the mocks, keyed the way
     * `ViewModelProvider` itself keys them.
     *
     * The keys are computed by asking a real provider for each ViewModel rather than by spelling
     * out `androidx.lifecycle.ViewModelProvider.DefaultKey:…` — a hand-written key is a string
     * that silently stops matching when the library changes how it derives one, and the symptom
     * would be Hilt being reached for and the test failing far from the cause.
     *
     * The owner deliberately does **not** implement `HasDefaultViewModelProviderFactory`: that is
     * the exact condition `createHiltViewModelFactory` tests, and implementing it would send
     * `hiltViewModel()` looking for a Hilt entry point on the activity.
     */
    private fun seededOwner(): ViewModelStoreOwner {
        val store = ViewModelStore()
        seed(store, MoreViewModel::class.java, moreViewModel)
        seed(store, HelpViewModel::class.java, helpViewModel)
        return object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore = store
        }
    }

    private fun <VM : ViewModel> seed(store: ViewModelStore, type: Class<VM>, instance: VM) {
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
                instance as T
        }
        ViewModelProvider.create(store, factory)[type]
    }
}
