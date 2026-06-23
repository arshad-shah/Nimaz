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
 * Focused checks on the More hub's two navigation contracts: opening a feature from a
 * list item, and the top-bar Settings action — each verified to land on the right
 * screen (by tag) and to return to More on back. Breadth across every feature lives in
 * [FeatureNavigationTest]; this guards the round-trip mechanics.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MoreMenuNavigationTest : BaseAppTest() {

    @Test
    fun more_toPrayerTracker_backReturnsToMore() {
        launchApp()
        tapBottomNav(NavLabel.MORE)
        assertScreen(ScreenTags.More)

        scrollMoreToAndTap(Selectors.str(Selectors.More.prayerTracker))
        assertScreen(ScreenTags.PrayerTracker)
        // The detail screen also carries the standard Back affordance.
        onContentDesc(Selectors.str(Selectors.Common.back)).assertExists()

        pressBack()
        assertScreen(ScreenTags.More)
    }

    @Test
    fun more_topBarSettings_backReturnsToMore() {
        launchApp()
        tapBottomNav(NavLabel.MORE)
        assertScreen(ScreenTags.More)

        tapContentDesc(Selectors.str(Selectors.More.settings))
        assertScreen(ScreenTags.Settings)

        pressBack()
        assertScreen(ScreenTags.More)
    }
}
