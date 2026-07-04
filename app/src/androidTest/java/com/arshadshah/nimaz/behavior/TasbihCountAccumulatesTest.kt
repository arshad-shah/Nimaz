package com.arshadshah.nimaz.behavior

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arshadshah.nimaz.core.navigation.ScreenTags
import com.arshadshah.nimaz.support.BaseAppTest
import com.arshadshah.nimaz.support.Selectors.NavLabel
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Beyond the single tap covered by [TasbihCounterTest], repeated dhikr taps must keep
 * accumulating within a session. Forces classic (non-bead) mode so the tagged counter
 * is shown. Reuses the existing counter tags — no new production selectors.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class TasbihCountAccumulatesTest : BaseAppTest() {

    override suspend fun seedState() {
        settings.setOnboardingCompleted(true)
        settings.setTasbihBeadMode(false)
    }

    @Test
    fun repeatedTaps_accumulateTheCount() {
        launchApp()
        tapBottomNav(NavLabel.TASBIH)
        assertScreen(ScreenTags.Tasbih)

        onTag(ScreenTags.TasbihCount).assertTextEquals("0")
        repeat(7) {
            onTag(ScreenTags.TasbihCounter).performClick()
            compose.waitForIdle()
        }
        onTag(ScreenTags.TasbihCount).assertTextEquals("7")
    }
}
