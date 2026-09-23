package com.arshadshah.nimaz.core.navigation

import android.content.Intent
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.util.BootReceiver
import com.arshadshah.nimaz.data.announcement.AnnouncementPayloadMapper
import com.arshadshah.nimaz.data.audio.QuranAudioService
import com.arshadshah.nimaz.domain.model.Announcement
import com.arshadshah.nimaz.domain.model.WorshipReminderType
import com.arshadshah.nimaz.widget.hijricalendar.HijriCalendarWidget

/**
 * Everything an intent that opened (or re-opened) `MainActivity` asks for.
 *
 * Read in one place, as a value, so every entry point's handling can be tested without an
 * activity: a tapped announcement, a prayer notification (which also stops the adhan), the Quran
 * audio notification, a worship reminder, and the Hijri calendar widget. `MainActivity` then does
 * the side effects — storing the announcement, stopping playback — and `PendingEntryEffects`
 * does the navigating.
 */
data class LaunchEntries(
    val announcement: Announcement? = null,
    val stopAdhan: Boolean = false,
    val openPlayingSurah: Boolean = false,
    val worshipDestination: Route? = null,
    val openIslamicCalendar: Boolean = false,
) {
    companion object {
        /**
         * Reads [intent]. A worship reminder's extra is removed as it is read, so a configuration
         * change re-delivering the same intent does not navigate a second time over wherever the
         * reader has gone since.
         */
        fun read(intent: Intent?, mapper: AnnouncementPayloadMapper): LaunchEntries {
            if (intent == null) return LaunchEntries()
            // A backgrounded FCM tap: the OS copies the message's data onto the launcher intent
            // as string extras. The mapper returning non-null is what identifies it as ours.
            val announcement = mapper.fromIntentExtras(intent.extras)
            val worship = WorshipReminderType.fromKey(intent.getStringExtra(BootReceiver.EXTRA_OPEN_WORSHIP))
            if (worship != null) intent.removeExtra(BootReceiver.EXTRA_OPEN_WORSHIP)
            return LaunchEntries(
                announcement = announcement,
                stopAdhan = intent.getBooleanExtra(BootReceiver.EXTRA_STOP_ADHAN, false),
                openPlayingSurah = intent.action == QuranAudioService.ACTION_OPEN_PLAYING_SURAH,
                worshipDestination = worship?.let(::worshipCardDestination),
                openIslamicCalendar = intent.action == HijriCalendarWidget.ACTION_OPEN_ISLAMIC_CALENDAR,
            ).also { it.logOpened() }
        }
    }

    private fun logOpened() {
        if (announcement != null) AppAnalytics.logNotificationOpened(source = "announcement")
        if (stopAdhan) AppAnalytics.logNotificationOpened(source = "prayer_notification")
        if (openPlayingSurah) AppAnalytics.logNotificationOpened(source = "quran_audio")
        if (worshipDestination != null) AppAnalytics.logNotificationOpened(source = "worship_reminder")
        if (openIslamicCalendar) AppAnalytics.logNotificationOpened(source = "hijri_calendar_widget")
    }
}
