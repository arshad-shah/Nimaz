package com.arshadshah.nimaz.core.common

import com.arshadshah.nimaz.domain.model.PrayerAlertStyle
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

/**
 * Which notification channel each alert style lands on, and that no live channel id has moved.
 *
 * The channel is the whole mechanism: Android decides sound and heads-up from it, and will
 * not let an existing channel's importance be lowered from code. So "silent" is not a flag
 * on a notification — it is a different channel, and picking the wrong one is the failure
 * mode where a prayer someone silenced still shouts at them.
 *
 * It moved here from `:app` with [NimazChannels], which is the subject of half of it. The other
 * half — [PrayerAlertStyle] — is `:core:domain`, which `:core:common` already has on its API.
 */
class PrayerAlertChannelTest {

    @Test
    fun `every channel id is exactly the literal already on installed devices`() {
        // Not a tautology, and not a restatement of the source. A channel id is the key Android
        // files a user's per-channel sound, importance and vibration choices under, so editing
        // one silently resets a preference the user set deliberately — no crash, no migration,
        // no way back. Spelling the eleven out here makes an edit show up as a failing test
        // rather than as a support thread six weeks after the release.
        assertThat(NimazChannels.PRAYER).isEqualTo("prayer_notifications")
        assertThat(NimazChannels.PRAYER_SILENT).isEqualTo("prayer_notifications_silent")
        assertThat(NimazChannels.PRAYER_MUTED).isEqualTo("prayer_notifications_muted")
        assertThat(NimazChannels.ADHAN).isEqualTo("adhan_notifications")
        assertThat(NimazChannels.ADHAN_SILENT).isEqualTo("adhan_notifications_silent")
        assertThat(NimazChannels.DAILY_SUMMARY).isEqualTo("daily_summary_notifications")
        assertThat(NimazChannels.KHATAM).isEqualTo("khatam_notifications")
        assertThat(NimazChannels.WORSHIP).isEqualTo("worship_reminders")
        assertThat(NimazChannels.ADHAN_DOWNLOAD).isEqualTo("adhan_download_channel")
        assertThat(NimazChannels.ADHAN_PLAYBACK).isEqualTo("adhan_playback_channel")
        assertThat(NimazChannels.QURAN_AUDIO).isEqualTo("quran_audio_channel")
    }

    @Test
    fun `the announcements channel matches the literal the manifest declares`() {
        // A manifest cannot reference a Kotlin constant, so
        // `com.google.firebase.messaging.default_notification_channel_id` repeats this string.
        // If the two drift, an FCM message delivered while the app is backgrounded is posted by
        // the OS on a channel nothing created — which on API 26+ means it is dropped outright.
        val manifest = File("../../app/src/main/AndroidManifest.xml").readText()
        assertThat(manifest).contains("android:value=\"${NimazChannels.ANNOUNCEMENTS}\"")
    }

    @Test
    fun `a silenced prayer posts on the muted channel whatever the vibration preference says`() {
        assertThat(NimazChannels.forPrayer(vibrate = true, muted = true))
            .isEqualTo(NimazChannels.PRAYER_MUTED)
        assertThat(NimazChannels.forPrayer(vibrate = false, muted = true))
            .isEqualTo(NimazChannels.PRAYER_MUTED)
    }

    @Test
    fun `an audible prayer still honours the vibration preference`() {
        assertThat(NimazChannels.forPrayer(vibrate = true))
            .isEqualTo(NimazChannels.PRAYER)
        assertThat(NimazChannels.forPrayer(vibrate = false))
            .isEqualTo(NimazChannels.PRAYER_SILENT)
    }

    @Test
    fun `the no-vibration channel is not the silent one`() {
        // The regression this guards: PRAYER_SILENT reads like silence but is a
        // no-vibration sibling at IMPORTANCE_HIGH that still carries the channel sound.
        assertThat(NimazChannels.PRAYER_SILENT)
            .isNotEqualTo(NimazChannels.PRAYER_MUTED)
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
