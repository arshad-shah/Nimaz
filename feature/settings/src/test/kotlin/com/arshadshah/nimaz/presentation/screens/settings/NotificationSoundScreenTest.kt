package com.arshadshah.nimaz.presentation.screens.settings

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.data.audio.AdhanSound
import com.arshadshah.nimaz.data.audio.DownloadState
import com.arshadshah.nimaz.presentation.viewmodel.settings.NotificationSettingsUiState
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
import org.robolectric.shadows.ShadowToast

/**
 * Adhan and delivery: the voice picker, vibration, and the Do Not Disturb rule.
 *
 * The voice picker is the part with real behaviour behind it. Auditioning a muezzin means playing
 * several in a row, so the sheet **stays open on selection** (`autoDismiss = false`) — and that
 * makes stopping the preview a matter of closing it, from any of four routes. Every dismissal is
 * funnelled through one lambda for exactly that reason: a sheet that closed without stopping
 * leaves the adhan playing over whatever the user does next, with no visible control to stop it.
 *
 * The preview button carries the same double duty. It plays, and it *stops* when the sound it
 * names is the one playing — a button that only ever started playback would give the user no way
 * to interrupt a two-minute recitation. It also selects the sound before previewing it, so what
 * you last auditioned is what you have chosen.
 *
 * The failure path is a Toast rather than a banner, and it is cleared after showing so it cannot
 * repeat on the next recomposition.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class NotificationSoundScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val viewModel = FakeSettingsScreenViewModel()
    private var backs = 0

    private val mishary = AdhanSound.entries.first()
    private val other = AdhanSound.entries[1]

    private fun setContent(state: NotificationSettingsUiState = NotificationSettingsUiState()) {
        viewModel.notificationState.value = state
        composeRule.setThemedContent {
            NotificationSoundScreen(onNavigateBack = { backs++ }, viewModel = viewModel.mock)
        }
    }

    private fun string(@StringRes res: Int, vararg args: Any): String =
        context.getString(res, *args)

    private fun openVoicePicker() {
        composeRule.settingsRow(string(R.string.notif_sound_voice)).performClick()
    }

    @Test
    fun `the adhan section and the delivery toggles render`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.notification_settings_adhan_section))
            .assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.notification_settings_enable_adhan))
            .assertExists()
        composeRule.onNodeWithText(string(R.string.notification_settings_vibration)).assertExists()
        composeRule.onNodeWithText(string(R.string.notification_settings_dnd)).assertExists()
    }

    @Test
    fun `the voice row appears only once the adhan is switched on`() {
        // Choosing a muezzin for an adhan that will not play is a setting with no effect.
        setContent(NotificationSettingsUiState(adhanEnabled = false))

        composeRule.onNodeWithText(string(R.string.notif_sound_voice)).assertDoesNotExist()
    }

    @Test
    fun `the voice row names the selected muezzin and where they are from`() {
        setContent(
            NotificationSettingsUiState(adhanEnabled = true, selectedAdhanSound = other.name)
        )

        composeRule.onNodeWithText(other.displayName).assertExists()
        composeRule.onNodeWithText(other.origin).assertExists()
    }

    @Test
    fun `the vibration and DnD toggles dispatch their own events`() {
        // Two adjacent rows on a screen about sound. Crossing them silences the adhan for
        // someone who asked only to stop the buzz.
        setContent(
            NotificationSettingsUiState(vibrationEnabled = true, respectDnd = true)
        )

        composeRule.settingsRow(string(R.string.notification_settings_vibration)).performClick()
        composeRule.settingsRow(string(R.string.notification_settings_dnd)).performClick()

        assertThat(viewModel.only<SettingsEvent.SetVibrationEnabled>().enabled).isFalse()
        assertThat(viewModel.only<SettingsEvent.SetRespectDnd>().enabled).isFalse()
    }

    @Test
    fun `the adhan master switch dispatches the adhan event`() {
        setContent(NotificationSettingsUiState(adhanEnabled = false))

        composeRule.onAllNodes(isToggleable()).onFirst().performClick()

        assertThat(viewModel.only<SettingsEvent.SetAdhanEnabled>().enabled).isTrue()
    }

    @Test
    fun `the picker offers every shipped muezzin`() {
        setContent(NotificationSettingsUiState(adhanEnabled = true))

        openVoicePicker()

        AdhanSound.entries.forEach {
            composeRule.onAllNodesWithText(it.displayName).onFirst().assertExists()
        }
    }

    @Test
    fun `picking a voice stores it and leaves the sheet open to audition another`() {
        // `autoDismiss = false`. A sheet that closed on selection would make comparing two
        // muezzins a matter of reopening it between each.
        setContent(NotificationSettingsUiState(adhanEnabled = true))

        openVoicePicker()
        composeRule.onAllNodesWithText(other.displayName).onLast().performClick()

        assertThat(viewModel.only<SettingsEvent.SetAdhanSound>().sound).isEqualTo(other.name)
        composeRule.onAllNodesWithText(AdhanSound.entries.last().displayName).onFirst()
            .assertExists()
    }

    @Test
    fun `the preview button selects the voice before playing it`() {
        // Otherwise the sound you last auditioned is not the one you end up with, and there is
        // nothing on screen to tell you that.
        setContent(
            NotificationSettingsUiState(adhanEnabled = true, selectedAdhanSound = mishary.name)
        )

        openVoicePicker()
        composeRule.onAllNodesWithContentDescription(string(R.string.notification_settings_preview))
            .onLast().performClick()

        assertThat(viewModel.events.filterIsInstance<SettingsEvent.SetAdhanSound>()).isNotEmpty()
        assertThat(viewModel.events).contains(SettingsEvent.PreviewAdhanSound)
    }

    @Test
    fun `the preview button stops the sound it is already playing`() {
        // A two-minute recitation with no way to interrupt it is the failure this prevents.
        viewModel.isAdhanPlaying.value = true
        viewModel.currentlyPlayingAdhan.value = mishary
        setContent(
            NotificationSettingsUiState(adhanEnabled = true, selectedAdhanSound = mishary.name)
        )

        openVoicePicker()
        composeRule.onAllNodesWithContentDescription(string(R.string.notification_settings_preview))
            .onFirst().performClick()

        assertThat(viewModel.events).contains(SettingsEvent.StopAdhanPreview)
        assertThat(viewModel.events).doesNotContain(SettingsEvent.PreviewAdhanSound)
    }

    @Test
    fun `a voice being downloaded cannot be previewed`() {
        // Tapping play on a file that is still arriving plays nothing, and the second tap would
        // start a second download.
        viewModel.adhanDownloadState.value =
            mapOf(mishary to DownloadState.Downloading(progress = 40))
        setContent(
            NotificationSettingsUiState(adhanEnabled = true, selectedAdhanSound = mishary.name)
        )

        openVoicePicker()
        composeRule.onAllNodesWithContentDescription(string(R.string.notification_settings_preview))
            .onFirst().performClick()

        assertThat(viewModel.events).doesNotContain(SettingsEvent.PreviewAdhanSound)
    }

    @Test
    fun `closing the picker stops whatever it was previewing`() {
        // Dismissal is funnelled through one lambda so this holds for Done, Cancel, a swipe and
        // the back gesture alike — a sheet that closed without stopping leaves the adhan playing
        // over the next screen with no control in sight.
        setContent(NotificationSettingsUiState(adhanEnabled = true))

        openVoicePicker()
        composeRule.onNodeWithText(string(R.string.cancel)).performClick()

        assertThat(viewModel.events).contains(SettingsEvent.StopAdhanPreview)
    }

    @Test
    fun `a preview failure is shown to the user and then cleared`() {
        // Cleared, or the Toast repeats on every recomposition for the rest of the screen's life.
        viewModel.adhanPreviewError.value = "Failed to download adhan audio."
        setContent(NotificationSettingsUiState(adhanEnabled = true))

        assertThat(ShadowToast.getTextOfLatestToast())
            .isEqualTo("Failed to download adhan audio.")
        io.mockk.verify { viewModel.mock.clearAdhanPreviewError() }
    }

    @Test
    fun `no toast is shown when nothing has failed`() {
        setContent(NotificationSettingsUiState(adhanEnabled = true))

        assertThat(ShadowToast.getTextOfLatestToast()).isNull()
    }

    @Test
    fun `the back button navigates back`() {
        setContent()

        composeRule.onNodeWithContentDescription(string(R.string.cd_back)).performClick()

        assertThat(backs).isEqualTo(1)
    }
}
