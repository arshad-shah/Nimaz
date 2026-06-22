package com.arshadshah.nimaz.navigation

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arshadshah.nimaz.support.BaseAppTest
import com.arshadshah.nimaz.support.Selectors
import com.arshadshah.nimaz.support.Selectors.NavLabel
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the More tab as the hub it is: opening a couple of its destinations
 * (Prayer Tracker, Settings) via their menu entries and confirming both the forward
 * navigation and the back-navigation contract (the standard "Back" affordance returns
 * the user to the More menu).
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MoreMenuNavigationTest : BaseAppTest() {

    @Test
    fun more_toPrayerTracker_andBack() {
        launchApp()
        tapBottomNav(NavLabel.MORE)
        waitForRes(Selectors.More.prayerTracker)

        tapText(Selectors.str(Selectors.More.prayerTracker))

        // Prayer Tracker screen shows its title and a back button.
        waitForRes(Selectors.Prayer.trackerTitle)
        onContentDesc(Selectors.str(Selectors.Common.back)).assertExists()

        tapBack()

        // Back on the More menu, the Settings entry is visible again.
        waitForRes(Selectors.More.settings)
        onRes(Selectors.More.settings).assertExists()
    }

    @Test
    fun more_toSettings_andBack() {
        launchApp()
        tapBottomNav(NavLabel.MORE)
        waitForRes(Selectors.More.settings)

        tapText(Selectors.str(Selectors.More.settings))

        // A detail screen with a back affordance is now shown.
        waitForText(Selectors.str(Selectors.Common.back))
        onContentDesc(Selectors.str(Selectors.Common.back)).assertExists()

        tapBack()

        waitForRes(Selectors.More.prayerTracker)
        onRes(Selectors.More.prayerTracker).assertExists()
    }
}
