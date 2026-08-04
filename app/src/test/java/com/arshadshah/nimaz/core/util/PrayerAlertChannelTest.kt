package com.arshadshah.nimaz.core.util

import com.arshadshah.nimaz.domain.model.PrayerAlertStyle
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Which notification channel each alert style lands on.
 *
 * The channel is the whole mechanism: Android decides sound and heads-up from it, and will
 * not let an existing channel's importance be lowered from code. So "silent" is not a flag
 * on a notification — it is a different channel, and picking the wrong one is the failure
 * mode where a prayer someone silenced still shouts at them.
 */
class PrayerAlertChannelTest {

    @Test
    fun `a silenced prayer posts on the muted channel whatever the vibration preference says`() {
        assertThat(PrayerNotificationScheduler.channelForPrayer(vibrate = true, muted = true))
            .isEqualTo(PrayerNotificationScheduler.CHANNEL_ID_PRAYER_MUTED)
        assertThat(PrayerNotificationScheduler.channelForPrayer(vibrate = false, muted = true))
            .isEqualTo(PrayerNotificationScheduler.CHANNEL_ID_PRAYER_MUTED)
    }

    @Test
    fun `an audible prayer still honours the vibration preference`() {
        assertThat(PrayerNotificationScheduler.channelForPrayer(vibrate = true))
            .isEqualTo(PrayerNotificationScheduler.CHANNEL_ID_PRAYER)
        assertThat(PrayerNotificationScheduler.channelForPrayer(vibrate = false))
            .isEqualTo(PrayerNotificationScheduler.CHANNEL_ID_PRAYER_SILENT)
    }

    @Test
    fun `the no-vibration channel is not the silent one`() {
        // The regression this guards: CHANNEL_ID_PRAYER_SILENT reads like silence but is a
        // no-vibration sibling at IMPORTANCE_HIGH that still carries the channel sound.
        assertThat(PrayerNotificationScheduler.CHANNEL_ID_PRAYER_SILENT)
            .isNotEqualTo(PrayerNotificationScheduler.CHANNEL_ID_PRAYER_MUTED)
    }

    @Test
    fun `the adhan style falls back to a plain notification when the global switch is off`() {
        // The global adhan switch stays a master gate over the per-prayer style, so turning
        // it off silences the call everywhere without rewriting five styles.
        assertThat(PrayerAlertStyle.ADHAN.playsAdhan(globalAdhanEnabled = true)).isTrue()
        assertThat(PrayerAlertStyle.ADHAN.playsAdhan(globalAdhanEnabled = false)).isFalse()
        assertThat(PrayerAlertStyle.NOTIFICATION.playsAdhan(globalAdhanEnabled = true)).isFalse()
        assertThat(PrayerAlertStyle.SILENT.playsAdhan(globalAdhanEnabled = true)).isFalse()
    }

    @Test
    fun `sunrise never takes a style of its own`() {
        // Sunrise gets a beep, never the adhan, and cannot be silenced independently —
        // it is the end of Fajr's window rather than a prayer with settings.
        assertThat(PrayerAlertStyle.ADHAN.playsAdhan(globalAdhanEnabled = true, isSunrise = true))
            .isFalse()
        assertThat(PrayerAlertStyle.SILENT.isMuted(isSunrise = true)).isFalse()
        assertThat(PrayerAlertStyle.SILENT.isMuted()).isTrue()
    }

    @Test
    fun `an unrecognised stored style reads as a plain notification`() {
        // A preference written by a newer build, or a value that never existed, must not
        // silence a prayer or start playing the adhan unasked.
        assertThat(PrayerAlertStyle.fromStorage(null)).isEqualTo(PrayerAlertStyle.NOTIFICATION)
        assertThat(PrayerAlertStyle.fromStorage("")).isEqualTo(PrayerAlertStyle.NOTIFICATION)
        assertThat(PrayerAlertStyle.fromStorage("VIBRATE_ONLY"))
            .isEqualTo(PrayerAlertStyle.NOTIFICATION)
        assertThat(PrayerAlertStyle.fromStorage("ADHAN")).isEqualTo(PrayerAlertStyle.ADHAN)
    }
}
