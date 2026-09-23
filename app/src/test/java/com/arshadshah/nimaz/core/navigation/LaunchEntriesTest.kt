package com.arshadshah.nimaz.core.navigation

import android.content.Intent
import com.arshadshah.nimaz.core.util.BootReceiver
import com.arshadshah.nimaz.data.announcement.AnnouncementPayloadMapper
import com.arshadshah.nimaz.data.audio.QuranAudioService
import com.arshadshah.nimaz.widget.hijricalendar.HijriCalendarWidget
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * What an intent that opens the app is asking for, read without an activity.
 *
 * Each entry point is its own shape of intent — extras copied by the OS from an FCM message, a
 * flag on the prayer notification, an action on the audio notification and the calendar widget,
 * the reminder's type on a worship notification — and each has to be recognised as itself and
 * nothing else, or a notification opens the wrong thing or nothing.
 */
@RunWith(RobolectricTestRunner::class)
class LaunchEntriesTest {

    private val mapper = AnnouncementPayloadMapper()

    private fun read(intent: Intent?) = LaunchEntries.read(intent, mapper)

    @Test
    fun `no intent, or a plain launch, asks for nothing`() {
        assertThat(read(null)).isEqualTo(LaunchEntries())
        assertThat(read(Intent(Intent.ACTION_MAIN))).isEqualTo(LaunchEntries())
    }

    @Test
    fun `a tapped announcement carries the announcement and its route`() {
        val intent = Intent().apply {
            putExtra("id", "eid")
            putExtra("type", "feature")
            putExtra("title", "Eid Mubarak")
            putExtra("body", "From all of us")
            putExtra("route", "calendar")
        }

        val entries = read(intent)

        assertThat(entries.announcement?.id).isEqualTo("eid")
        assertThat(entries.announcement?.route).isEqualTo("calendar")
    }

    @Test
    fun `a prayer notification stops the adhan`() {
        assertThat(read(Intent().putExtra(BootReceiver.EXTRA_STOP_ADHAN, true)).stopAdhan).isTrue()
    }

    @Test
    fun `the quran audio notification asks for the playing surah`() {
        assertThat(read(Intent(QuranAudioService.ACTION_OPEN_PLAYING_SURAH)).openPlayingSurah).isTrue()
    }

    @Test
    fun `the calendar widget asks for the calendar`() {
        assertThat(read(Intent(HijriCalendarWidget.ACTION_OPEN_ISLAMIC_CALENDAR)).openIslamicCalendar)
            .isTrue()
    }

    @Test
    fun `a worship reminder resolves to its destination, once`() {
        val intent = Intent().putExtra(BootReceiver.EXTRA_OPEN_WORSHIP, "tahajjud")

        assertThat(read(intent).worshipDestination).isEqualTo(Route.NightWorship)
        // Consumed: the same intent re-delivered on a configuration change navigates nowhere.
        assertThat(read(intent).worshipDestination).isNull()
    }

    @Test
    fun `an unknown worship key resolves to nothing`() {
        assertThat(read(Intent().putExtra(BootReceiver.EXTRA_OPEN_WORSHIP, "nope")).worshipDestination)
            .isNull()
    }
}
