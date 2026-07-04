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
 * The tasbih (dhikr) counter is the core interaction of that screen: tapping it must
 * increment the displayed count. Forces classic (non-bead) mode so the tagged counter
 * circle + count are shown, then taps and asserts the number advances.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class TasbihCounterTest : BaseAppTest() {

    override suspend fun seedState() {
        settings.setOnboardingCompleted(true)
        // Classic mode renders the tagged CounterCircle (TasbihCounter / TasbihCount).
        settings.setTasbihBeadMode(false)
        // The count persists in Room and the suite has no clearPackageData, so reset
        // it here to guarantee the counter starts at 0 regardless of prior tests/runs.
        clearTasbih()
    }

    @Test
    fun tappingCounter_incrementsTheCount() {
        launchApp()
        tapBottomNav(NavLabel.TASBIH)
        assertScreen(ScreenTags.Tasbih)

        onTag(ScreenTags.TasbihCount).assertTextEquals("0")

        onTag(ScreenTags.TasbihCounter).performClick()
        compose.waitForIdle()
        onTag(ScreenTags.TasbihCount).assertTextEquals("1")

        onTag(ScreenTags.TasbihCounter).performClick()
        compose.waitForIdle()
        onTag(ScreenTags.TasbihCount).assertTextEquals("2")
    }
}
