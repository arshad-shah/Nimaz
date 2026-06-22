package com.arshadshah.nimaz.navigation

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arshadshah.nimaz.core.navigation.ScreenTags
import com.arshadshah.nimaz.support.BaseAppTest
import com.arshadshah.nimaz.support.Selectors
import com.arshadshah.nimaz.support.Selectors.More
import com.arshadshah.nimaz.support.Selectors.NavLabel
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exhaustively navigates from the More hub into every feature it exposes and asserts
 * the destination screen rendered (by its [ScreenTags] root tag), then returns. This
 * is the broad "does every feature open" guarantee.
 *
 * Because the tag lives on the NavGraph wrapper (composed immediately, before any data
 * loads), each assertion is deterministic and independent of seeded content, locale,
 * or on-screen copy. Tests are grouped by the menu's sections to keep each scroll
 * short and to isolate failures to a feature area.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class FeatureNavigationTest : BaseAppTest() {

    /** Each entry: (menu-item label resource, expected destination screen tag). */
    private fun visitAll(features: List<Pair<Int, String>>) {
        launchApp()
        tapBottomNav(NavLabel.MORE)
        assertScreen(ScreenTags.More)

        features.forEach { (labelRes, screenTag) ->
            scrollMoreToAndTap(Selectors.str(labelRes))
            assertScreen(screenTag)
            pressBack()
            assertScreen(ScreenTags.More)
        }
    }

    @Test
    fun dailyPracticeSection_opensEveryFeature() = visitAll(
        listOf(
            More.prayerTracker to ScreenTags.PrayerTracker,
            More.fasting to ScreenTags.FastingHome,
            More.khatam to ScreenTags.KhatamList,
        )
    )

    @Test
    fun learningSection_opensEveryFeature() = visitAll(
        listOf(
            More.qaida to ScreenTags.QaidaHome,
            More.asmaUlHusna to ScreenTags.AsmaUlHusnaList,
            More.asmaUnNabi to ScreenTags.AsmaUnNabiList,
            More.prophets to ScreenTags.ProphetsList,
            More.hadith to ScreenTags.HadithHome,
            More.duas to ScreenTags.DuaHome,
            More.tafseer to ScreenTags.TafseerChapters,
        )
    )

    @Test
    fun toolsSection_opensEveryFeature() = visitAll(
        listOf(
            More.calendar to ScreenTags.IslamicCalendar,
            More.prayerTimes to ScreenTags.PrayerTimes,
            More.monthlyPrayerTimes to ScreenTags.MonthlyPrayerTimes,
            More.zakat to ScreenTags.ZakatCalculator,
        )
    )

    @Test
    fun supportSection_opensAboutAndHelp() = visitAll(
        listOf(
            More.aboutNimaz to ScreenTags.SettingsAbout,
            More.helpSupport to ScreenTags.SettingsHelp,
        )
    )
}
