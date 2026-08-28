package com.arshadshah.nimaz.data.platform

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.data.audio.AdhanDownloadService
import com.arshadshah.nimaz.data.audio.AdhanSound
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The four lines behind the domain's `AdhanDownloader` port.
 *
 * Small enough to look like it needs no test, and it is the only thing between "download the
 * adhan" anywhere in the app and the service that does it. What is worth pinning is the
 * **name resolution**: the port speaks in `String`s because the domain must not see
 * [AdhanSound], and an unrecognised name has to land on a real sound rather than throwing
 * inside a settings screen or, worse, a prayer broadcast.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ServiceAdhanDownloaderTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    private fun started() =
        generateSequence { shadowOf(context as Application).nextStartedService }.toList()

    @Test
    fun `a download request reaches the download service, carrying the sound`() {
        ServiceAdhanDownloader(context).download(AdhanSound.MISHARY.name)

        val intent = started().single()
        assertThat(intent.component?.className).isEqualTo(AdhanDownloadService::class.java.name)
        assertThat(intent.getStringExtra(AdhanDownloadService.EXTRA_ADHAN_SOUND))
            .isEqualTo(AdhanSound.MISHARY.name)
    }

    @Test
    fun `an unrecognised name falls back rather than throwing into a prayer broadcast`() {
        // The port takes a `String` because the domain cannot see `AdhanSound`, so a preference
        // written by a newer build — or one whose enum entry was renamed — arrives here. Throwing
        // would take the notification with it.
        ServiceAdhanDownloader(context).download("NOT_A_SOUND")

        assertThat(started()).hasSize(1)
    }
}
