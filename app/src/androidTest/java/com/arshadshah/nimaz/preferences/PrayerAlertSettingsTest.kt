package com.arshadshah.nimaz.preferences

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arshadshah.nimaz.domain.model.PrayerAlertStyle
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.repository.preReminderMinutesByPrayer
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * The per-prayer alert style and reminder, end to end through DataStore.
 *
 * These are the settings the notifications rework split out of two global preferences, so
 * the property that matters is isolation: setting one prayer must leave the other four
 * exactly as they were. A regression here is silent — the app keeps working, it just stops
 * doing what one person asked for at one time of day.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class PrayerAlertSettingsTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var settings: SettingsRepository

    @Before
    fun setup() = hiltRule.inject()

    @Test
    fun alertStyle_roundTripsPerPrayer() = runTest {
        settings.setPrayerAlertStyle("fajr", PrayerAlertStyle.ADHAN)
        settings.setPrayerAlertStyle("dhuhr", PrayerAlertStyle.SILENT)

        assertThat(settings.prayerAlertStyle("fajr").first()).isEqualTo(PrayerAlertStyle.ADHAN)
        assertThat(settings.prayerAlertStyle("dhuhr").first()).isEqualTo(PrayerAlertStyle.SILENT)
    }

    @Test
    fun settingOnePrayer_leavesTheOthersAlone() = runTest {
        PrayerAlertStyle.PRAYER_KEYS.forEach {
            settings.setPrayerAlertStyle(it, PrayerAlertStyle.NOTIFICATION)
            settings.setPrayerReminderEnabled(it, true)
            settings.setPrayerReminderMinutes(it, 15)
        }

        settings.setPrayerAlertStyle("asr", PrayerAlertStyle.SILENT)
        settings.setPrayerReminderMinutes("asr", 45)

        assertThat(settings.prayerAlertStyle("asr").first()).isEqualTo(PrayerAlertStyle.SILENT)
        assertThat(settings.prayerReminderMinutes("asr").first()).isEqualTo(45)

        (PrayerAlertStyle.PRAYER_KEYS - "asr").forEach { untouched ->
            assertThat(settings.prayerAlertStyle(untouched).first())
                .isEqualTo(PrayerAlertStyle.NOTIFICATION)
            assertThat(settings.prayerReminderMinutes(untouched).first()).isEqualTo(15)
        }
    }

    @Test
    fun reminderOff_dropsThePrayerFromTheScheduleWithoutLosingItsLeadTime() = runTest {
        PrayerAlertStyle.PRAYER_KEYS.forEach {
            settings.setPrayerReminderEnabled(it, true)
            settings.setPrayerReminderMinutes(it, 20)
        }
        settings.setPrayerReminderEnabled("maghrib", false)

        val scheduled = settings.preReminderMinutesByPrayer()

        // Maghrib is absent rather than present with a zero offset — that is how the
        // scheduler is told not to arm a reminder at all.
        assertThat(scheduled.keys).doesNotContain(PrayerType.MAGHRIB)
        assertThat(scheduled).containsEntry(PrayerType.FAJR, 20)
        assertThat(scheduled).hasSize(4)

        // The lead time survives, so switching the reminder back on restores 20 minutes
        // rather than resetting to the default.
        settings.setPrayerReminderEnabled("maghrib", true)
        assertThat(settings.preReminderMinutesByPrayer())
            .containsEntry(PrayerType.MAGHRIB, 20)
    }

    @Test
    fun migration_carriesTheGlobalPreAdhanValueOntoEveryPrayer() = runTest {
        // An install as it was before the split: a deliberately chosen 30-minute reminder
        // and the adhan on everywhere.
        settings.setShowReminderBefore(true)
        settings.setNotificationReminderMinutes(30)
        settings.setAdhanEnabled(true)

        settings.migratePrayerNotificationPreferences()

        PrayerAlertStyle.PRAYER_KEYS.forEach { prayer ->
            assertThat(settings.prayerReminderEnabled(prayer).first()).isTrue()
            assertThat(settings.prayerReminderMinutes(prayer).first()).isEqualTo(30)
            assertThat(settings.prayerAlertStyle(prayer).first())
                .isEqualTo(PrayerAlertStyle.ADHAN)
        }
    }

    @Test
    fun migration_neverRunsTwiceOverAChoiceTheUserHasSinceMade() = runTest {
        settings.setShowReminderBefore(true)
        settings.setNotificationReminderMinutes(30)
        settings.migratePrayerNotificationPreferences()

        // The user then changes their mind about Isha.
        settings.setPrayerReminderMinutes("isha", 5)
        settings.setPrayerAlertStyle("isha", PrayerAlertStyle.SILENT)

        // A later start re-runs the call; the version guard must make it a no-op.
        settings.migratePrayerNotificationPreferences()

        assertThat(settings.prayerReminderMinutes("isha").first()).isEqualTo(5)
        assertThat(settings.prayerAlertStyle("isha").first()).isEqualTo(PrayerAlertStyle.SILENT)
    }
}
