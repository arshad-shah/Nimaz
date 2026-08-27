package com.arshadshah.nimaz.core.datastore

import com.arshadshah.nimaz.domain.model.PrayerAlertStyle
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The notifications rework splits two global preferences into five per-prayer ones: the
 * adhan on/off pair becomes a three-way alert style, and the single pre-adhan reminder
 * becomes a reminder per prayer.
 *
 * Existing installs must come through that split with their settings intact. Somebody who
 * set a 30-minute pre-adhan reminder has to end up with a 30-minute reminder on all five
 * prayers, not a reset to the 15-minute default — which is what these cases pin.
 */
class PrayerNotificationPrefsMigrationTest {

    private val allPrayers = listOf("fajr", "dhuhr", "asr", "maghrib", "isha")

    private fun legacy(
        adhanEnabled: Boolean = false,
        perPrayerAdhan: Map<String, Boolean> = emptyMap(),
        showReminderBefore: Boolean = true,
        reminderMinutes: Int = 15,
    ) = LegacyPrayerNotificationPrefs(
        adhanEnabled = adhanEnabled,
        perPrayerAdhanEnabled = allPrayers.associateWith { perPrayerAdhan[it] ?: true },
        showReminderBefore = showReminderBefore,
        reminderMinutes = reminderMinutes,
    )

    @Test
    fun `a global pre-adhan value carries to every prayer`() {
        val migrated = PrayerNotificationPrefsMigration.plan(
            legacy(showReminderBefore = true, reminderMinutes = 30)
        )

        assertThat(migrated.reminderMinutes.keys).containsExactlyElementsIn(allPrayers)
        assertThat(migrated.reminderMinutes.values.toSet()).containsExactly(30)
        assertThat(migrated.reminderEnabled.values.toSet()).containsExactly(true)
    }

    @Test
    fun `a pre-adhan reminder that was off stays off everywhere`() {
        val migrated = PrayerNotificationPrefsMigration.plan(
            legacy(showReminderBefore = false, reminderMinutes = 25)
        )

        assertThat(migrated.reminderEnabled.values.toSet()).containsExactly(false)
        // The minutes are still carried, so turning a prayer's reminder back on restores
        // the lead time the user last chose rather than the default.
        assertThat(migrated.reminderMinutes.values.toSet()).containsExactly(25)
    }

    @Test
    fun `adhan on globally and per prayer becomes the adhan style`() {
        val migrated = PrayerNotificationPrefsMigration.plan(legacy(adhanEnabled = true))

        assertThat(migrated.alertStyle.values.toSet()).containsExactly(PrayerAlertStyle.ADHAN)
    }

    @Test
    fun `a prayer with the adhan turned off becomes notification only`() {
        val migrated = PrayerNotificationPrefsMigration.plan(
            legacy(adhanEnabled = true, perPrayerAdhan = mapOf("dhuhr" to false))
        )

        assertThat(migrated.alertStyle["dhuhr"]).isEqualTo(PrayerAlertStyle.NOTIFICATION)
        assertThat(migrated.alertStyle["fajr"]).isEqualTo(PrayerAlertStyle.ADHAN)
    }

    @Test
    fun `the adhan off globally makes every prayer notification only`() {
        // ADHAN_ENABLED defaults to false, so this is the path most installs take.
        val migrated = PrayerNotificationPrefsMigration.plan(
            legacy(adhanEnabled = false, perPrayerAdhan = allPrayers.associateWith { true })
        )

        assertThat(migrated.alertStyle.values.toSet())
            .containsExactly(PrayerAlertStyle.NOTIFICATION)
    }

    @Test
    fun `migration never silences a prayer on its own`() {
        // Nothing in the old model expressed "silent", so nothing may migrate to it —
        // a user who has never asked for silence must not find a prayer gone quiet.
        val migrated = PrayerNotificationPrefsMigration.plan(legacy(adhanEnabled = true))

        assertThat(migrated.alertStyle.values).doesNotContain(PrayerAlertStyle.SILENT)
    }

    @Test
    fun `a lead time outside the stepper's range is clamped into it`() {
        // The stepper offers 5-60. A stored value outside that (an old build, a hand-edited
        // preference file) would otherwise render a value the user cannot get back to.
        assertThat(
            PrayerNotificationPrefsMigration.plan(legacy(reminderMinutes = 0))
                .reminderMinutes.values.toSet()
        ).containsExactly(5)
        assertThat(
            PrayerNotificationPrefsMigration.plan(legacy(reminderMinutes = 240))
                .reminderMinutes.values.toSet()
        ).containsExactly(60)
    }

    @Test
    fun `sunrise is not part of the split`() {
        // Sunrise has no adhan and no pre-reminder — it is a plain alert and stays one.
        val migrated = PrayerNotificationPrefsMigration.plan(legacy())

        assertThat(migrated.alertStyle).doesNotContainKey("sunrise")
        assertThat(migrated.reminderMinutes).doesNotContainKey("sunrise")
    }
}
