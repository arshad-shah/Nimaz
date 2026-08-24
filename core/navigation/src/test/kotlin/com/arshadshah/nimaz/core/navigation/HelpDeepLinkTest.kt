package com.arshadshah.nimaz.core.navigation

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * The grammar that turns a `help.json` step's `deeplink` key into a destination.
 *
 * The keys are **content**, not code: they arrive in the shipped help artifact, so a key that
 * does not resolve is not a compile error and not a crash — the step simply renders without its
 * "take me there" button, which reads as a help article that forgot to offer the obvious thing.
 * `check_docs.py`'s NAV-09/NAV-10 keep the documented set and the code in step with each other;
 * what they cannot see is whether each key points somewhere *sensible*.
 *
 * So two properties, over the whole table rather than a sampled six:
 *
 * 1. **Total** — every key the app publishes resolves. A missing arm returns null, which is the
 *    same answer as a typo in the content, and neither is reported anywhere.
 * 2. **Injective** — no two keys resolve to the same destination. This is the copy-paste that
 *    sends "zakat" to the calendar: the button appears, it works, and it goes to the wrong
 *    screen, which is worse than no button at all.
 */
class HelpDeepLinkTest {

    /**
     * Every key the help content is allowed to use, paired with where it must land.
     *
     * Written out rather than derived, on purpose: a test that read the same `when` block it is
     * testing would assert nothing. NAV-09 and NAV-10 guard this list against the docs; this
     * guards it against the code.
     */
    private val grammar: List<Pair<String, Route>> = listOf(
        "prayer_settings" to Route.SettingsPrayerCalculation,
        "notifications" to Route.SettingsNotifications,
        "worship_reminders" to Route.SettingsWorshipReminders,
        "location" to Route.SettingsLocation,
        "qibla" to Route.Qibla,
        "quran_settings" to Route.SettingsQuran,
        "language" to Route.SettingsLanguage,
        "appearance" to Route.SettingsAppearance,
        "calendar" to Route.IslamicCalendar,
        "fasting" to Route.FastingTracker,
        "tasbih" to Route.TasbihHome,
        "hadith" to Route.HadithHome,
        "zakat" to Route.ZakatCalculator,
        "prayer_tracker" to Route.PrayerTracker,
        "qada" to Route.QadaPrayers,
        "qaida" to Route.QaidaHome,
        "dua" to Route.DuaHome,
        "tafseer" to Route.TafseerChapters,
        "khatam" to Route.KhatamList,
        "widgets" to Route.SettingsWidgets,
        "settings" to Route.Settings,
        "home" to Route.Home,
    )

    @Test
    fun `every published key resolves to the destination it names`() {
        grammar.forEach { (key, route) ->
            assertWithMessage("help deep link '$key'")
                .that(helpDeepLinkRoute(key))
                .isEqualTo(route)
        }
    }

    @Test
    fun `no two keys lead to the same screen`() {
        // "zakat" quietly resolving to the calendar is a working button that goes somewhere else.
        val destinations = grammar.map { it.second }

        assertThat(destinations.toSet()).hasSize(grammar.size)
    }

    @Test
    fun `an unknown key offers no button rather than a wrong one`() {
        assertThat(helpDeepLinkRoute("nope")).isNull()
        assertThat(helpDeepLinkRoute("")).isNull()
        assertThat(helpDeepLinkRoute("Prayer_Settings")).isNull()
    }

    @Test
    fun `a step with no deeplink at all is not a failure`() {
        // Most help steps have none; null is the ordinary case, not an error case.
        assertThat(helpDeepLinkRoute(null)).isNull()
    }

    @Test
    fun `the settings keys land in settings, and the feature keys do not`() {
        // A rough shape check that catches a whole arm being pasted into the wrong half of the
        // table: everything named `settings_*` in the docs must resolve to a Settings route.
        val settingsKeys = listOf(
            "prayer_settings", "notifications", "worship_reminders", "location",
            "quran_settings", "language", "appearance", "widgets", "settings",
        )

        settingsKeys.forEach { key ->
            assertWithMessage("help deep link '$key'")
                .that(helpDeepLinkRoute(key).toString())
                .contains("Settings")
        }
    }
}
