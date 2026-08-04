package com.arshadshah.nimaz.presentation.screens.settings

import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.PrayerAlertStyle
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The notifications hub reports live settings rather than describing its screens, so each
 * subtitle is a claim about the app's state. These cases pin the claims: a row that says
 * "both on" when only one is would be worse than the static copy it replaced.
 */
class NotificationHubSubtitlesTest {

    @Test
    fun `the weekly row names exactly what is on`() {
        assertThat(NotificationHubSubtitles.weekly(jumuahEnabled = true, khatamEnabled = true))
            .isEqualTo(R.string.notif_hub_weekly_both)
        assertThat(NotificationHubSubtitles.weekly(jumuahEnabled = true, khatamEnabled = false))
            .isEqualTo(R.string.notif_hub_weekly_jumuah)
        assertThat(NotificationHubSubtitles.weekly(jumuahEnabled = false, khatamEnabled = true))
            .isEqualTo(R.string.notif_hub_weekly_khatam)
        assertThat(NotificationHubSubtitles.weekly(jumuahEnabled = false, khatamEnabled = false))
            .isEqualTo(R.string.notif_hub_weekly_none)
    }

    @Test
    fun `Do Not Disturb outranks vibration on the sound row`() {
        // DND is the rule that stops a sound the user is expecting, so it is the one worth
        // the single line even when vibration is also on.
        assertThat(NotificationHubSubtitles.sound(respectDnd = true, vibrationEnabled = true))
            .isEqualTo(R.string.notif_hub_sound_dnd)
        assertThat(NotificationHubSubtitles.sound(respectDnd = false, vibrationEnabled = true))
            .isEqualTo(R.string.notif_hub_sound_vibration)
        assertThat(NotificationHubSubtitles.sound(respectDnd = false, vibrationEnabled = false))
            .isEqualTo(R.string.notif_hub_sound_plain)
    }

    @Test
    fun `every alert style has its own label`() {
        val labels = PrayerAlertStyle.entries.map { NotificationHubSubtitles.alertStyle(it) }
        assertThat(labels).containsNoDuplicates()
        assertThat(labels).hasSize(3)
    }
}
