package com.arshadshah.nimaz.behavior

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arshadshah.nimaz.core.navigation.ScreenTags
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.support.BaseAppTest
import com.arshadshah.nimaz.support.Selectors
import com.arshadshah.nimaz.support.Selectors.NavLabel
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The surah card, raised over the browse list and read from the real database.
 *
 * `SurahInfoSheetHostTest` covers the same seam on the JVM with both ViewModels mocked, which
 * proves the wiring but not the data: the card's opening page comes from the active edition's
 * pagination, and that pagination is built from a table in the shipped content artifact. Whether
 * the two agree — whether a surah's card names a page the surah actually opens on (#325) — can
 * only be answered against the real thing.
 *
 * It is also the one place "Read surah" is proven to navigate. The JVM test asserts the callback
 * fires; only here is there a `NavGraph` on the other end of it.
 *
 * Read-only: opens a sheet and follows one link, writes no user data.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class QuranSurahInfoSheetTest : BaseAppTest() {

    @Test
    fun openingASurahsCard_showsItsFactsAndLeadsToTheReader() {
        launchApp()
        tapBottomNav(NavLabel.QURAN)
        assertScreen(ScreenTags.Quran)

        clickText(Selectors.str(Selectors.Quran.browseTab))
        waitForTag(ScreenTags.QuranSurahList)

        // Every interaction below goes through `OnClick` on the **merged** tree rather than a
        // synthetic tap, which is the same choice `BaseAppTest.scrollListToAndTap` documents: a
        // row scrolled just into view can sit at the viewport edge or under the gesture-nav
        // inset, where a tap is rejected with "Failed to inject touch input". The merged tree
        // matters too — the unmerged one yields the inner `Text`/`Icon`, which carries no click
        // action at all.
        val infoDescription = Selectors.str(R.string.quran_home_surah_info)

        // The info affordance sits on the surah's own row, so scroll it into view first.
        compose.onNodeWithTag(ScreenTags.QuranSurahList)
            .performScrollToNode(hasContentDescription(infoDescription))
        compose.waitForIdle()
        compose.onAllNodesWithContentDescription(infoDescription, useUnmergedTree = false)
            .onFirst()
            .performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()

        // The card renders nothing until the surah is known, so its presence is also the
        // assertion that the ViewModels answered.
        waitForRes(R.string.surah_info_read_surah)

        compose.onNodeWithText(
            Selectors.str(R.string.surah_info_read_surah),
            substring = true,
            useUnmergedTree = false,
        ).performSemanticsAction(SemanticsActions.OnClick)
        assertScreen(ScreenTags.QuranReader)
    }
}
