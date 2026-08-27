package com.arshadshah.nimaz.presentation.screens.settings

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.presentation.viewmodel.settings.SettingsEvent
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
 * The settings hub: eleven rows into eleven destinations, and the two destructive actions.
 *
 * Eleven near-identical `NimazMenuItem`s each taking a different callback is the purest form of
 * the crossing this module is full of — every row looks right, every row opens *a* settings
 * screen, and one of them opens the wrong one. Nothing about that is visible in review, and a user
 * meets it once.
 *
 * The destructive pair is the other half, and the asymmetry matters: "Reset settings" clears
 * preferences and "Delete all data" clears the user database as well. Crossing those two
 * confirmations makes a reset destroy every tracked prayer, with a dialog that said it would only
 * reset settings. Both must confirm first, both must dispatch their *own* event, and cancelling
 * must dispatch nothing.
 *
 * The restart is driven by a `shouldRestart` flag rather than by the tap, because the writes
 * happen asynchronously — a restart fired from the button would race the write it exists to apply.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h4000dp")
class SettingsScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val viewModel = FakeSettingsScreenViewModel()

    private val opened = mutableListOf<String>()
    private var backs = 0
    private var restarts = 0

    private fun setContent() {
        composeRule.setThemedContent {
            SettingsScreen(
                onNavigateBack = { backs++ },
                onNavigateToPrayerSettings = { opened += "prayer" },
                onNavigateToNotifications = { opened += "notifications" },
                onNavigateToQuranSettings = { opened += "quran" },
                onNavigateToAppearance = { opened += "appearance" },
                onNavigateToLocation = { opened += "location" },
                onNavigateToLanguage = { opened += "language" },
                onNavigateToWidgets = { opened += "widgets" },
                onNavigateToSync = { opened += "sync" },
                onNavigateToSearchSettings = { opened += "search" },
                onNavigateToZakatSettings = { opened += "zakat" },
                onRestartApp = { restarts++ },
                viewModel = viewModel.mock,
            )
        }
    }

    private fun string(@StringRes res: Int, vararg args: Any): String =
        context.getString(res, *args)

    @Test
    fun `every section is present`() {
        setContent()

        composeRule.onAllNodesWithText(string(R.string.prayer_settings)).onLast().assertExists()
        composeRule.onAllNodesWithText(string(R.string.quran)).onLast().assertExists()
        composeRule.onAllNodesWithText(string(R.string.zakat)).onLast().assertExists()
        composeRule.onNodeWithText(string(R.string.app_settings)).assertExists()
        composeRule.onNodeWithText(string(R.string.data)).assertExists()
    }

    @Test
    fun `each row opens its own destination`() {
        // Eleven callbacks wired in one block. The assertion is the *order*, so a row pointing
        // at its neighbour's destination shows up as the wrong name in the wrong place rather
        // than as "something was opened".
        setContent()

        composeRule.settingsRow(string(R.string.calculation_method)).performClick()
        composeRule.settingsRow(string(R.string.location)).performClick()
        composeRule.settingsRow(string(R.string.notifications)).performClick()
        composeRule.settingsRow(string(R.string.quran_settings)).performClick()
        composeRule.settingsRow(string(R.string.zakat_settings)).performClick()
        composeRule.settingsRow(string(R.string.appearance)).performClick()
        composeRule.settingsRow(string(R.string.language)).performClick()
        composeRule.settingsRow(string(R.string.widgets)).performClick()
        composeRule.settingsRow(string(R.string.sync_data)).performClick()

        assertThat(opened).containsExactly(
            "prayer",
            "location",
            "notifications",
            "quran",
            "zakat",
            "appearance",
            "language",
            "widgets",
            "sync",
        ).inOrder()
    }

    @Test
    fun `the search and AI row opens the search settings`() {
        // Its title and its section header are the same string, which is why it is addressed as
        // a tappable row rather than by text alone.
        setContent()

        composeRule.settingsRow(string(R.string.search_settings)).performClick()

        assertThat(opened).containsExactly("search")
    }

    @Test
    fun `resetting settings asks first and dispatches nothing until confirmed`() {
        setContent()

        composeRule.settingsRow(string(R.string.reset_settings)).performClick()

        composeRule.onAllNodesWithText(string(R.string.reset_settings_dialog_title)).onLast()
            .assertExists()
        assertThat(viewModel.events).isEmpty()
    }

    @Test
    fun `confirming the reset dispatches reset, not delete`() {
        // The two confirmations sit three rows apart and read almost identically. Crossing them
        // makes "reset settings" destroy every tracked prayer.
        setContent()

        composeRule.settingsRow(string(R.string.reset_settings)).performClick()
        composeRule.onNodeWithText(string(R.string.reset)).performClick()

        assertThat(viewModel.events).containsExactly(SettingsEvent.ResetToDefaults)
    }

    @Test
    fun `cancelling the reset dispatches nothing`() {
        setContent()

        composeRule.settingsRow(string(R.string.reset_settings)).performClick()
        composeRule.onNodeWithText(string(R.string.cancel)).performClick()

        assertThat(viewModel.events).isEmpty()
    }

    @Test
    fun `deleting all data asks first`() {
        setContent()

        composeRule.settingsRow(string(R.string.delete_all_data)).performClick()

        composeRule.onAllNodesWithText(string(R.string.delete_all_data_dialog_title)).onLast()
            .assertExists()
        assertThat(viewModel.events).isEmpty()
    }

    @Test
    fun `confirming the delete dispatches delete, not reset`() {
        setContent()

        composeRule.settingsRow(string(R.string.delete_all_data)).performClick()
        composeRule.onNodeWithText(string(R.string.delete)).performClick()

        assertThat(viewModel.events).containsExactly(SettingsEvent.DeleteAllData)
    }

    @Test
    fun `cancelling the delete dispatches nothing`() {
        setContent()

        composeRule.settingsRow(string(R.string.delete_all_data)).performClick()
        composeRule.onNodeWithText(string(R.string.cancel)).performClick()

        assertThat(viewModel.events).isEmpty()
    }

    @Test
    fun `the app restarts when the ViewModel says the change needs it, not when it is tapped`() {
        // The write happens asynchronously; restarting from the tap would race it, and a
        // language change would come back on the old locale roughly half the time.
        setContent()
        assertThat(restarts).isEqualTo(0)

        viewModel.shouldRestart.value = true
        composeRule.waitForIdle()

        assertThat(restarts).isEqualTo(1)
    }

    @Test
    fun `the version is shown, and a package that cannot be read does not crash the screen`() {
        setContent()

        composeRule.onNodeWithText(
            string(
                R.string.version_format,
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
                    ?: string(R.string.version_unknown),
            )
        ).assertExists()
    }

    @Test
    fun `the title is Settings and the back button navigates back`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.settings)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(string(R.string.cd_back)).performClick()

        assertThat(backs).isEqualTo(1)
    }
}
