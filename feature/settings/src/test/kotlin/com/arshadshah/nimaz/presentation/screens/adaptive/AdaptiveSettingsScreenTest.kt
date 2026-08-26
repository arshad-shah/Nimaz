package com.arshadshah.nimaz.presentation.screens.adaptive

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
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
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.presentation.viewmodel.settings.SettingsViewModel
import com.arshadshah.nimaz.testing.FakeSettingsScreenViewModel
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.arshadshah.nimaz.testing.settingsRow
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The settings entry point, which is two different screens depending on how wide the window is.
 *
 * On a phone every row is a `navigate` to its own destination. On a tablet the same rows open a
 * **detail pane** instead, and the difference is not cosmetic: a row that navigated on a tablet
 * would push a full-screen destination over the list-detail scaffold, losing the list, and a row
 * that opened a pane on a phone would do nothing visible at all.
 *
 * Two rows are deliberately exceptions on the wide layout — Search settings and the Quran
 * pickers still navigate, because they are not panes — and those are exactly the ones a
 * refactor would "tidy" into the pane path.
 *
 * `hiltViewModel()` is reached without Hilt here: `SettingsScreen` and every detail screen take
 * their ViewModel by default argument, and `hiltViewModel()` builds a Hilt factory **only when
 * the `ViewModelStoreOwner` it is handed supplies a default factory**. A plain owner whose store
 * already holds the ViewModel answers from the store first, before any factory is consulted.
 */
@RunWith(RobolectricTestRunner::class)
class AdaptiveSettingsScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val viewModel = FakeSettingsScreenViewModel()

    private val navigated = mutableListOf<Route>()
    private var backs = 0
    private var restarts = 0

    private fun setContent() {
        composeRule.setThemedContent {
            CompositionLocalProvider(LocalViewModelStoreOwner provides seededOwner()) {
                AdaptiveSettingsScreen(
                    onNavigate = { navigated += it },
                    onBack = { backs++ },
                    onRestartApp = { restarts++ },
                )
            }
        }
    }

    private fun string(@StringRes res: Int): String = context.getString(res)

    // ── Compact: every row is a destination ──────────────────────────────────────────────────

    @Test
    @Config(qualifiers = "w411dp-h4000dp")
    fun `on a phone the settings list is the whole screen`() {
        setContent()

        composeRule.onAllNodesWithText(string(R.string.settings)).onFirst().assertIsDisplayed()
        composeRule.onAllNodesWithText(string(R.string.prayer_settings)).onFirst().assertExists()
    }

    @Test
    @Config(qualifiers = "w411dp-h4000dp")
    fun `on a phone each row navigates to its own route`() {
        // Nine rows, nine routes, wired in one block. A row pointing at its neighbour's route is
        // invisible in review and obvious to a user exactly once.
        setContent()

        composeRule.settingsRow(string(R.string.calculation_method)).performClick()
        composeRule.settingsRow(string(R.string.notifications)).performClick()
        composeRule.settingsRow(string(R.string.quran_settings)).performClick()
        composeRule.settingsRow(string(R.string.appearance)).performClick()
        composeRule.settingsRow(string(R.string.location)).performClick()
        composeRule.settingsRow(string(R.string.language)).performClick()
        composeRule.settingsRow(string(R.string.widgets)).performClick()
        composeRule.settingsRow(string(R.string.sync_data)).performClick()
        composeRule.settingsRow(string(R.string.zakat_settings)).performClick()

        assertThat(navigated).containsExactly(
            Route.SettingsPrayerCalculation,
            Route.SettingsNotifications,
            Route.SettingsQuran,
            Route.SettingsAppearance,
            Route.SettingsLocation,
            Route.SettingsLanguage,
            Route.SettingsWidgets,
            Route.SettingsSync,
            Route.SettingsZakat,
        ).inOrder()
    }

    @Test
    @Config(qualifiers = "w411dp-h4000dp")
    fun `search settings navigates on a phone too`() {
        setContent()

        composeRule.settingsRow(string(R.string.search_settings)).performClick()

        assertThat(navigated).containsExactly(Route.SearchSettings)
    }

    @Test
    @Config(qualifiers = "w411dp-h4000dp")
    fun `the back arrow leaves settings on a phone`() {
        setContent()

        composeRule.onNodeWithContentDescription(string(R.string.cd_back)).performClick()

        assertThat(backs).isEqualTo(1)
    }

    // ── Expanded: rows open panes instead ────────────────────────────────────────────────────

    @Test
    @Config(qualifiers = "w1000dp-h1200dp")
    fun `on a tablet the list is still shown`() {
        setContent()

        composeRule.onAllNodesWithText(string(R.string.settings)).onFirst().assertExists()
        composeRule.onAllNodesWithText(string(R.string.calculation_method)).onFirst()
            .assertExists()
    }

    @Test
    @Config(qualifiers = "w1000dp-h1200dp")
    fun `on a tablet a row opens its detail pane rather than navigating away`() {
        // Navigating here would push a full-screen destination over the scaffold and lose the
        // list beside it — the one thing a two-pane layout exists to keep.
        setContent()

        composeRule.settingsRow(string(R.string.appearance)).performClick()
        composeRule.waitForIdle()

        assertThat(navigated).isEmpty()
        composeRule.onAllNodesWithText(string(R.string.appearance_theme)).onFirst().assertExists()
    }

    @Test
    @Config(qualifiers = "w1000dp-h1200dp")
    fun `a second row replaces the pane rather than stacking one on the other`() {
        setContent()

        composeRule.settingsRow(string(R.string.appearance)).performClick()
        composeRule.waitForIdle()
        composeRule.settingsRow(string(R.string.language)).performClick()
        composeRule.waitForIdle()

        composeRule.onAllNodesWithText(string(R.string.app_language_section)).onFirst()
            .assertExists()
        composeRule.onNodeWithText(string(R.string.appearance_theme)).assertDoesNotExist()
        assertThat(navigated).isEmpty()
    }

    @Test
    @Config(qualifiers = "w1000dp-h1200dp")
    fun `search settings still navigates on a tablet, because it is not a pane`() {
        // The exception a refactor would "tidy" away. Sending it down the pane path would open
        // a `when` arm that does not exist and show an empty detail pane.
        setContent()

        composeRule.settingsRow(string(R.string.search_settings)).performClick()
        composeRule.waitForIdle()

        assertThat(navigated).containsExactly(Route.SearchSettings)
    }

    @Test
    @Config(qualifiers = "w1000dp-h1200dp")
    fun `the prayer pane can open the notifications pane beside it`() {
        // The one cross-pane hop: from prayer settings to the notifications hub, still inside
        // the scaffold rather than out of it.
        setContent()

        composeRule.settingsRow(string(R.string.calculation_method)).performClick()
        composeRule.waitForIdle()
        composeRule.settingsRow(string(R.string.adhan_notifications)).performClick()
        composeRule.waitForIdle()

        assertThat(navigated).isEmpty()
        composeRule.onAllNodesWithText(string(R.string.notification_settings_enable)).onFirst()
            .assertExists()
    }

    @Test
    // Taller than the rest of the class: the Quran pane's audio section is far down a
    // `LazyColumn`, and a `LazyColumn` composes only a screenful.
    @Config(qualifiers = "w1000dp-h2600dp")
    fun `the Quran pane's pickers navigate, because they are their own destinations`() {
        setContent()

        composeRule.settingsRow(string(R.string.quran_settings)).performClick()
        composeRule.waitForIdle()
        composeRule.settingsRow(string(R.string.reciter)).performClick()
        composeRule.waitForIdle()

        assertThat(navigated).containsExactly(Route.SelectReciter)
    }

    @Test
    @Config(qualifiers = "w1000dp-h1200dp")
    fun `nothing is shown in the detail pane until a row is chosen`() {
        // `currentDestination?.contentKey` is null on open, and the `if` around it is the only
        // thing between that and a crash on entry for every tablet user.
        setContent()

        composeRule.onNodeWithText(string(R.string.appearance_theme)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.app_language_section)).assertDoesNotExist()
    }

    private fun seededOwner(): ViewModelStoreOwner {
        val store = ViewModelStore()
        seed(store, SettingsViewModel::class.java, viewModel.mock)
        return object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore = store
        }
    }

    /**
     * Keyed by asking a real `ViewModelProvider` for it rather than spelling out
     * `androidx.lifecycle.ViewModelProvider.DefaultKey:…` — a hand-written key silently stops
     * matching if the library changes how it derives one, and the symptom is Hilt being reached
     * for and the test failing far from the cause.
     */
    private fun <VM : ViewModel> seed(store: ViewModelStore, type: Class<VM>, instance: VM) {
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
                instance as T
        }
        ViewModelProvider.create(store, factory)[type]
    }
}
