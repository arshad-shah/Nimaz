package com.arshadshah.nimaz.presentation.screens.settings

import androidx.annotation.StringRes
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.PrayerAlertStyle

/**
 * Which string each notifications-hub row reports, given the settings behind it.
 *
 * The choosing is kept apart from the rendering so it can be tested without a device: the
 * hub's whole point is that its subtitles are true, and "the weekly row says both are on
 * when only one is" is exactly the kind of wrong that a screenshot does not catch.
 */
object NotificationHubSubtitles {

    @StringRes
    fun alertStyle(style: PrayerAlertStyle): Int = when (style) {
        PrayerAlertStyle.ADHAN -> R.string.notif_alert_style_adhan
        PrayerAlertStyle.NOTIFICATION -> R.string.notif_alert_style_notification
        PrayerAlertStyle.SILENT -> R.string.notif_alert_style_silent
    }

    @StringRes
    fun weekly(jumuahEnabled: Boolean, khatamEnabled: Boolean): Int = when {
        jumuahEnabled && khatamEnabled -> R.string.notif_hub_weekly_both
        jumuahEnabled -> R.string.notif_hub_weekly_jumuah
        khatamEnabled -> R.string.notif_hub_weekly_khatam
        else -> R.string.notif_hub_weekly_none
    }

    /**
     * The sound row leads with the chosen voice and adds the one delivery rule most likely
     * to surprise someone — Do Not Disturb silencing the adhan outranks vibration, because
     * it is the one that stops a sound the user is expecting.
     */
    @StringRes
    fun sound(respectDnd: Boolean, vibrationEnabled: Boolean): Int = when {
        respectDnd -> R.string.notif_hub_sound_dnd
        vibrationEnabled -> R.string.notif_hub_sound_vibration
        else -> R.string.notif_hub_sound_plain
    }
}
