package com.arshadshah.nimaz.behavior

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
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
 * The Home "Next Worship" entry must lead somewhere.
 *
 * It shipped inert: it counted down at you and then did nothing, because `WorshipEventCard` took an
 * `onAction` that Home never passed. Nothing failed — an unwired callback is not a test failure,
 * which is precisely why this needs a test that drives the real navigation graph. Compact Home now
 * surfaces the reminder as an "Also today" row rather than a carousel card; both shapes carry
 * [WorshipCardTestTag], so this test asserts the destination, not the presentation.
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
        scrollToWorshipCard()

        tapWorshipCard()

        // Both adhkar reminders resolve to a dua category, so that is the destination either way.
        assertScreen(ScreenTags.DuaCategory)
    }

    /** Back from the destination must return to Home rather than exiting the app. */
    @Test
    fun backFromTheWorshipDestinationReturnsHome() {
        launchApp()
        scrollToWorshipCard()

        tapWorshipCard()
        assertScreen(ScreenTags.DuaCategory)

        pressBack()
        assertScreen(ScreenTags.Home)
    }

    /**
     * Bring the worship entry on screen.
     *
     * Two things keep a plain `waitUntilAtLeastOneExists` from ever seeing it: the card is resolved
     * off ~30 sequential DataStore reads plus an astronomical pass, so it arrives a beat after Home
     * itself; and "Also today" sits below the fold in a `LazyColumn`, so the row is not composed at
     * all until it is scrolled to. Hence scroll on each attempt until the node exists.
     */
    private fun scrollToWorshipCard() {
        waitForTag(ScreenTags.HomeList)
        val deadline = System.currentTimeMillis() + WORSHIP_CARD_TIMEOUT_MS
        var found = false
        while (!found && System.currentTimeMillis() < deadline) {
            found = runCatching {
                compose.onNodeWithTag(ScreenTags.HomeList)
                    .performScrollToNode(hasTestTag(WorshipCardTestTag))
            }.isSuccess
            compose.waitForIdle()
        }
        assertTrue("No worship card surfaced with both adhkar reminders enabled", found)
    }

    /**
     * Tap coordinate-free via the row's `OnClick` semantics: a list row scrolled just into view can
     * sit at the viewport edge or under the gesture-nav inset, where a synthetic tap is rejected.
     */
    private fun tapWorshipCard() {
        compose.onNodeWithTag(WorshipCardTestTag)
            .performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
    }

    private companion object {
        const val LONDON_LAT = 51.5074
        const val LONDON_LON = -0.1278

        /**
         * 30 s, not 15. On launch the card is resolved **twice** before it can show: once from
         * `HomeViewModel.init`, then again when the prayer-settings pass marks it stale and
         * cancel-and-replaces that first job — each ~30 sequential DataStore reads plus an
         * astronomical pass per candidate day. On a cold API 30 emulator that chain overran 15 s:
         * PR #647's run failed at 15.8 s on one attempt and passed on the retry at the same
         * hour, so the card was late, not absent. A slow first resolve is the designed behaviour
         * (the card "arrives a beat after Home"); what this test asserts is where it leads.
         */
        const val WORSHIP_CARD_TIMEOUT_MS = 30_000L
    }
}
