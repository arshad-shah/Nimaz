package com.arshadshah.nimaz.navigation

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arshadshah.nimaz.core.navigation.ScreenTags
import com.arshadshah.nimaz.support.BaseAppTest
import com.arshadshah.nimaz.support.Selectors
import com.arshadshah.nimaz.support.Selectors.NavLabel
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Reaches the Settings hub (via the More screen's top-bar action) and opens every
 * settings sub-screen, asserting each by its [ScreenTags] root tag and returning. This
 * covers the settings surface that the feature-navigation test does not.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SettingsNavigationTest : BaseAppTest() {

    private fun openSettings() {
        launchApp()
        tapBottomNav(NavLabel.MORE)
        assertScreen(ScreenTags.More)
        // More's top app bar exposes a Settings action (contentDescription = "Settings").
        tapContentDesc(Selectors.str(Selectors.More.settings))
        assertScreen(ScreenTags.Settings)
    }

    private fun visit(labelRes: Int, screenTag: String) {
        scrollListToAndTap(ScreenTags.SettingsList, Selectors.str(labelRes))
        assertScreen(screenTag)
        pressBack()
        assertScreen(ScreenTags.Settings)
    }

    @Test
    fun settingsHub_opensEverySubScreen() {
        openSettings()

        visit(Selectors.Settings.calculationMethod, ScreenTags.SettingsPrayerCalculation)
        visit(Selectors.Settings.location, ScreenTags.SettingsLocation)
        visit(Selectors.Settings.notifications, ScreenTags.SettingsNotifications)
        visit(Selectors.Settings.quranSettings, ScreenTags.SettingsQuran)
        visit(Selectors.Settings.appearance, ScreenTags.SettingsAppearance)
        visit(Selectors.Settings.language, ScreenTags.SettingsLanguage)
        visit(Selectors.Settings.widgets, ScreenTags.SettingsWidgets)
        visit(Selectors.Settings.syncData, ScreenTags.SettingsSync)
    }
}
