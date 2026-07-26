package com.arshadshah.nimaz.behavior

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arshadshah.nimaz.core.navigation.ScreenTags
import com.arshadshah.nimaz.domain.model.WorshipReminderType
import com.arshadshah.nimaz.presentation.components.organisms.WorshipCardTestTag
import com.arshadshah.nimaz.support.BaseAppTest
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The Home "Next Worship" card must lead somewhere.
 *
 * It shipped inert: it counted down at you and then did nothing, because `WorshipEventCard` took an
 * `onAction` that Home never passed. Nothing failed — an unwired callback is not a test failure,
 * which is precisely why this needs a test that drives the real navigation graph.
 *
 * ## Keeping it deterministic
 *
 * Which reminder surfaces depends on the time of day, and the resolver only surfaces one within a
 * 14-hour window. Enabling *both* adhkar reminders guarantees a card at any hour: morning adhkar is
 * anchored to Fajr and evening adhkar to Asr, and at a mid-latitude location the gap between those
 * two anchors never exceeds 14 hours in either direction. So the test asserts the *behaviour*
 * (tapping leaves Home for a real destination) without pinning which card it got.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class WorshipCardNavigationTest : BaseAppTest() {

    override suspend fun seedState() {
        super.seedState()
        settings.updateLocation(LONDON_LAT, LONDON_LON, "London")
        // Both adhkar reminders on, everything else off: guarantees exactly one plausible card and
        // keeps the expected destination set small.
        WorshipReminderType.entries.forEach {
            settings.setWorshipReminderEnabled(it.key, false)
        }
        settings.setWorshipReminderEnabled(WorshipReminderType.ADHKAR_MORNING.key, true)
        settings.setWorshipReminderEnabled(WorshipReminderType.ADHKAR_EVENING.key, true)
    }

    @Test
    fun tappingTheWorshipCardOpensItsDuaCategory() {
        launchApp()
        waitForWorshipCard()

        compose.onNodeWithTag(WorshipCardTestTag, useUnmergedTree = true).performClick()
        compose.waitForIdle()

        // Both adhkar reminders resolve to a dua category, so that is the destination either way.
        assertScreen(ScreenTags.DuaCategory)
    }

    /** Back from the destination must return to Home rather than exiting the app. */
    @Test
    fun backFromTheWorshipDestinationReturnsHome() {
        launchApp()
        waitForWorshipCard()

        compose.onNodeWithTag(WorshipCardTestTag, useUnmergedTree = true).performClick()
        compose.waitForIdle()
        assertScreen(ScreenTags.DuaCategory)

        pressBack()
        assertScreen(ScreenTags.Home)
    }

    @OptIn(ExperimentalTestApi::class)
    private fun waitForWorshipCard() {
        // The card is resolved off ~30 sequential DataStore reads plus an astronomical pass, so it
        // arrives a beat after Home itself.
        compose.waitUntilAtLeastOneExists(hasTestTag(WorshipCardTestTag), 15_000)
        val found = compose.onAllNodes(hasTestTag(WorshipCardTestTag), useUnmergedTree = true)
            .fetchSemanticsNodes().size
        assertTrue("No worship card surfaced with both adhkar reminders enabled", found > 0)
    }

    private companion object {
        const val LONDON_LAT = 51.5074
        const val LONDON_LON = -0.1278
    }
}
