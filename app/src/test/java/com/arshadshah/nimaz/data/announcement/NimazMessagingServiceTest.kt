package com.arshadshah.nimaz.data.announcement

import com.arshadshah.nimaz.domain.model.Announcement
import com.arshadshah.nimaz.domain.repository.AnnouncementRepository
import com.google.firebase.messaging.RemoteMessage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * An FCM message arriving while the app is in the foreground.
 *
 * It runs headless — no screen, no one to see a failure — so what is pinned is that a valid
 * payload is stored for the Home banner, an invalid one is dropped quietly, and a failing store
 * is reported rather than thrown into the Firebase callback.
 */
@RunWith(RobolectricTestRunner::class)
class NimazMessagingServiceTest {

    private val repository = mockk<AnnouncementRepository>(relaxed = true)

    private fun service() = NimazMessagingService().also {
        it.repository = repository
        it.mapper = AnnouncementPayloadMapper()
    }

    private fun message(data: Map<String, String>) =
        RemoteMessage.Builder("nimaz@fcm.googleapis.com").setData(data).build()

    private val valid = mapOf(
        "id" to "ramadan-2027",
        "type" to "feature",
        "title" to "Ramadan is near",
        "body" to "Set your suhoor reminder",
        "route" to "settings/worship",
    )

    @Test
    fun `a valid announcement is stored for the home banner`() {
        service().onMessageReceived(message(valid))

        coVerify { repository.setAnnouncement(match<Announcement> { it.id == "ramadan-2027" && it.route == "settings/worship" }) }
    }

    @Test
    fun `a payload that is not an announcement is dropped`() {
        service().onMessageReceived(message(mapOf("id" to "x")))

        coVerify(exactly = 0) { repository.setAnnouncement(any()) }
    }

    @Test
    fun `a store that fails is reported, not thrown into the callback`() {
        coEvery { repository.setAnnouncement(any()) } throws IllegalStateException("disk full")

        service().onMessageReceived(message(valid))
    }

    @Test
    fun `a rotated token is noted and nothing more`() {
        service().onNewToken("token")
    }
}
