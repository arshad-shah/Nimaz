package com.arshadshah.nimaz.behavior

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.navigation.ScreenTags
import com.arshadshah.nimaz.support.BaseAppTest
import com.arshadshah.nimaz.support.Selectors
import com.arshadshah.nimaz.support.Selectors.NavLabel
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Validates that settings toggles actually *work* end to end — the UI switch flips the
 * value in the DataStore-backed [com.arshadshah.nimaz.domain.repository.SettingsRepository].
 *
 * The rows are clickable `NimazSettingsItem`s, so each toggle is driven coordinate-free
 * via its OnClick semantics (see [scrollListToAndTap]); the resulting async DataStore
 * write is awaited via [awaitSettingChange]. This closes the gap between
 * SettingsRepositoryTest (persistence layer alone) and SettingsNavigationTest (screens
 * render) by proving the UI is actually wired to persistence.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SettingsBehaviorTest : BaseAppTest() {

    private fun openSettings() {
        launchApp()
        tapBottomNav(NavLabel.MORE)
        assertScreen(ScreenTags.More)
        tapContentDesc(Selectors.str(Selectors.More.settings))
        assertScreen(ScreenTags.Settings)
    }

    @Test
    fun appearanceToggles_flipTheStoredValue() {
        openSettings()
        scrollListToAndTap(ScreenTags.SettingsList, Selectors.str(Selectors.Settings.appearance))
        assertScreen(ScreenTags.SettingsAppearance)

        // Haptic feedback
        val hapticBefore = runBlocking { settings.hapticFeedback.first() }
        scrollListToAndTap(ScreenTags.AppearanceList, Selectors.str(R.string.appearance_haptic))
        assertThat(awaitSettingChange(settings.hapticFeedback, hapticBefore))
            .isEqualTo(!hapticBefore)

        // 24-hour time format
        val h24Before = runBlocking { settings.use24HourFormat.first() }
        scrollListToAndTap(ScreenTags.AppearanceList, Selectors.str(R.string.appearance_24hour))
        assertThat(awaitSettingChange(settings.use24HourFormat, h24Before))
            .isEqualTo(!h24Before)

        // Animations
        val animBefore = runBlocking { settings.animationsEnabled.first() }
        scrollListToAndTap(ScreenTags.AppearanceList, Selectors.str(R.string.appearance_animations))
        assertThat(awaitSettingChange(settings.animationsEnabled, animBefore))
            .isEqualTo(!animBefore)
    }

    @Test
    fun notificationVibrationToggle_flipsTheStoredValue() {
        openSettings()
        scrollListToAndTap(ScreenTags.SettingsList, Selectors.str(Selectors.Settings.notifications))
        assertScreen(ScreenTags.SettingsNotifications)

        // Vibration now lives in the Sound & delivery subscreen (notifications hub, #301).
        scrollListToAndTap(
            ScreenTags.NotificationsList,
            Selectors.str(R.string.notif_hub_sound_title),
        )
        val before = runBlocking { settings.notificationVibration.first() }
        scrollListToAndTap(
            ScreenTags.NotificationsList,
            Selectors.str(R.string.notification_settings_vibration),
        )
        assertThat(awaitSettingChange(settings.notificationVibration, before))
            .isEqualTo(!before)
    }
}
