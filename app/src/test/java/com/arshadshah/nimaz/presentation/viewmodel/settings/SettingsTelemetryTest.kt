package com.arshadshah.nimaz.presentation.viewmodel.settings

import com.arshadshah.nimaz.domain.model.AsrCalculation
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.domain.model.HighLatitudeRule
import com.arshadshah.nimaz.domain.model.Location
import com.arshadshah.nimaz.domain.model.MushafScript
import com.arshadshah.nimaz.domain.model.PrayerAlertStyle
import com.arshadshah.nimaz.presentation.theme.NimazPatternStyle
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The settings telemetry table: what each event reports, and what it must never report.
 *
 * `SettingsViewModel.onEvent` is a 78-branch `when` in which 15 branches logged and 56 did not.
 * Moving the decision into [asSettingChange] is what makes that ratio visible — and this suite
 * is what keeps it from drifting back, because a table is only better than scattered call sites
 * if something checks it.
 */
class SettingsTelemetryTest {

    /**
     * The names that were already reaching dashboards, pinned exactly.
     *
     * A renamed setting is not a cosmetic change: the old series stops and reads as "nobody
     * changes this any more". `pre_reminder_enabled` is the one that most invites tidying —
     * the event is `SetShowReminderBefore` — so it is listed first.
     */
    @Test
    fun `the fifteen names already in use are unchanged`() {
        val expected = mapOf(
            SettingsEvent.SetShowReminderBefore(true) to "pre_reminder_enabled",
            SettingsEvent.SetTheme(AppTheme.DARK) to "theme",
            SettingsEvent.SetLanguage(AppLanguage.ENGLISH) to "language",
            SettingsEvent.SetCalculationMethod(CalculationMethod.MUSLIM_WORLD_LEAGUE)
                to "calculation_method",
            SettingsEvent.SetAsrMethod(AsrCalculation.STANDARD) to "asr_method",
            SettingsEvent.SetHighLatitudeRule(HighLatitudeRule.MIDDLE_OF_THE_NIGHT)
                to "high_latitude_rule",
            SettingsEvent.SetNotificationsEnabled(true) to "notifications_enabled",
            SettingsEvent.SetPrayerNotification("fajr", true) to "prayer_notification_fajr",
            SettingsEvent.SetAdhanEnabled(true) to "adhan_enabled",
            SettingsEvent.SetPrayerAlertStyle("fajr", PrayerAlertStyle.ADHAN) to "alert_style_fajr",
            SettingsEvent.SetPrayerReminderEnabled("asr", true) to "reminder_enabled_asr",
            SettingsEvent.SetPrayerReminderMinutes("asr", 15) to "reminder_minutes_asr",
            SettingsEvent.SetRespectDnd(true) to "respect_dnd",
            SettingsEvent.SetReminderMinutes(15) to "reminder_minutes",
            SettingsEvent.SetAdhanSound("makkah") to "adhan_sound",
        )

        expected.forEach { (event, name) ->
            assertThat(event.asSettingChange()?.first).isEqualTo(name)
        }
    }

    /**
     * The adjustment is keyed by prayer.
     *
     * #359 calls this "arguably the single most diagnostic setting in the app" and it reported
     * nothing at all. A single `prayer_adjustment` key would have been most of the fix and
     * still not answered the question it exists for: whether one prayer is being nudged the
     * same way by many people, which is what a calculation bug looks like from outside.
     */
    @Test
    fun `a prayer adjustment names the prayer and the offset`() {
        val change = SettingsEvent.SetPrayerAdjustment("fajr", -3).asSettingChange()

        assertThat(change).isEqualTo("prayer_adjustment_fajr" to "-3")
    }

    /**
     * A location's name never reaches analytics.
     *
     * `settingChanged` writes its value to a dashboard, and a location is the most identifying
     * thing this app holds. The events report that the set changed and nothing about where.
     */
    @Test
    fun `location events never carry a place name`() {
        val home = Location(
            id = 7,
            name = "Bad Godesberg, Bonn",
            latitude = 50.68,
            longitude = 7.15,
            timezone = "Europe/Berlin",
            country = "Germany",
            city = "Bonn",
            isCurrentLocation = true,
            isFavorite = false,
            calculationMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE,
            asrCalculation = AsrCalculation.STANDARD,
            highLatitudeRule = null,
            fajrAngle = null,
            ishaAngle = null,
        )

        val changes = listOf(
            SettingsEvent.SetCurrentLocation(home),
            SettingsEvent.AddLocation(home),
            SettingsEvent.RemoveLocation(home),
        ).mapNotNull { it.asSettingChange() }

        assertThat(changes).hasSize(3)
        changes.forEach { (name, value) ->
            assertThat(name).doesNotContain("Bonn")
            assertThat(value).doesNotContain("Bonn")
            assertThat(value).doesNotContain("50.68")
        }
    }

    /** The events that are not setting changes opt out explicitly rather than by omission. */
    @Test
    fun `actions and previews report no setting change`() {
        val notSettings = listOf(
            SettingsEvent.PreviewAdhanSound,
            SettingsEvent.StopAdhanPreview,
            SettingsEvent.TestNotification,
            SettingsEvent.TestAllNotifications,
            SettingsEvent.ResetToDefaults,
            SettingsEvent.ResetNotifications,
            SettingsEvent.DeleteAllData,
        )

        notSettings.forEach { assertThat(it.asSettingChange()).isNull() }
    }

    /**
     * The typography events all report, because they were the largest unreported block: every
     * Qur'an, dua and hadith font and size setting, none of them logged.
     */
    @Test
    fun `every typography setting reports a name and a value`() {
        val typography = listOf(
            SettingsEvent.SetArabicFont("amiri"),
            SettingsEvent.SetArabicFontSize(24f),
            SettingsEvent.SetTranslationFontSize(16f),
            SettingsEvent.SetMushafScript(MushafScript.MADANI),
            SettingsEvent.SetDuaArabicFont("amiri"),
            SettingsEvent.SetDuaArabicFontSize(28f),
            SettingsEvent.SetDuaTranslationFontSize(16f),
            SettingsEvent.SetHadithArabicFont("amiri"),
            SettingsEvent.SetHadithArabicFontSize(24f),
            SettingsEvent.SetHadithTranslationFontSize(16f),
        )

        typography.forEach { event ->
            val change = event.asSettingChange()
            assertThat(change).isNotNull()
            assertThat(change!!.first).isNotEmpty()
            assertThat(change.second).isNotEmpty()
        }
    }

    /** A nulled reciter reports "none" rather than the string "null". */
    @Test
    fun `clearing the reciter reports none`() {
        assertThat(SettingsEvent.SetReciter(null).asSettingChange())
            .isEqualTo("quran_reciter" to "none")
    }

    /** Pattern style is an enum, and reports its name rather than an ordinal. */
    @Test
    fun `enum-valued settings report the enum name`() {
        val change = SettingsEvent.SetPatternStyle(NimazPatternStyle.entries.first())
            .asSettingChange()

        assertThat(change?.second).isEqualTo(NimazPatternStyle.entries.first().name)
    }
}
