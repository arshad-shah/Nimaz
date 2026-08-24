package com.arshadshah.nimaz.core.datastore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.domain.model.Announcement
import com.arshadshah.nimaz.domain.model.AnnouncementType
import com.arshadshah.nimaz.domain.model.CelebrationEvent
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The announcement store, and the device id beside it.
 *
 * An announcement arrives from FCM — a payload this build did not write and cannot type-check —
 * and is then persisted as JSON and read back on a later launch, possibly by a *different build*.
 * Every one of those hops is somewhere a field can quietly go missing, and the whole feature is
 * one banner that either appears or does not: nothing errors, nothing retries, nobody notices.
 *
 * So the properties are about survival rather than logic. A stored announcement comes back whole,
 * including the optional half that only celebrations use; a payload naming a type this build has
 * never heard of resolves to nothing rather than a half-built banner; and a dismissal is
 * permanent, which is the one thing a reader will notice if it breaks.
 *
 * `DeviceIdProvider` is here because it shares the shape and the file boundary. Its contract is
 * short and worth pinning exactly: stable across calls, and **never** a hardware identifier.
 */
@RunWith(RobolectricTestRunner::class)
class AnnouncementLocalDataSourceTest {

    private lateinit var store: AnnouncementLocalDataSource

    @Before
    fun setUp() {
        store = AnnouncementLocalDataSource(ApplicationProvider.getApplicationContext<Context>())
    }

    private fun announcement(
        id: String = "a1",
        type: AnnouncementType = AnnouncementType.FEATURE,
        event: CelebrationEvent? = null,
    ) = Announcement(
        id = id,
        type = type,
        title = "A title",
        body = "A body",
        ctaLabel = "Open",
        route = "Route.Home",
        minVersionCode = 100,
        maxVersionCode = 900,
        expiresAtMillis = 1_800_000_000_000,
        dismissable = true,
        event = event,
        arabic = "نص",
        transliteration = "nass",
        proofRef = "2:255",
        proofText = "Allahu la ilaha illa huwa",
        cta2Label = "Later",
        route2 = "Route.Settings",
        startsAtMillis = 1_700_000_000_000,
    )

    // ---- Storing one ----

    @Test
    fun `nothing has been announced to begin with`() = runTest {
        store.dismiss("clear-the-slate")

        assertThat(store.dismissedIds.first()).contains("clear-the-slate")
    }

    @Test
    fun `an announcement comes back whole`() = runTest {
        val original = announcement(id = "whole")

        store.setCurrentAnnouncement(original)

        // Every optional field, because each one is a separate chance to lose something between
        // the payload, the stored JSON and a later build's reader.
        assertThat(store.currentAnnouncement.first()).isEqualTo(original)
    }

    @Test
    fun `a celebration keeps the occasion behind it`() = runTest {
        val eid = announcement(
            id = "eid",
            type = AnnouncementType.CELEBRATION,
            event = CelebrationEvent.EID_AL_FITR,
        )

        store.setCurrentAnnouncement(eid)

        assertThat(store.currentAnnouncement.first()?.event).isEqualTo(CelebrationEvent.EID_AL_FITR)
    }

    @Test
    fun `an occasion on a non-celebration is dropped rather than carried`() = runTest {
        // The occasion drives a themed banner; on a changelog it would theme the wrong thing.
        val mislabelled = announcement(
            id = "mislabelled",
            type = AnnouncementType.CHANGELOG,
            event = CelebrationEvent.EID_AL_FITR,
        )

        store.setCurrentAnnouncement(mislabelled)

        assertThat(store.currentAnnouncement.first()?.event).isNull()
    }

    @Test
    fun `every announcement type survives a round trip`() = runTest {
        AnnouncementType.entries.forEach { type ->
            store.setCurrentAnnouncement(announcement(id = "t-${type.key}", type = type))

            assertWithMessage(type.key)
                .that(store.currentAnnouncement.first()?.type)
                .isEqualTo(type)
        }
    }

    @Test
    fun `every celebration occasion survives a round trip`() = runTest {
        CelebrationEvent.entries.forEach { event ->
            store.setCurrentAnnouncement(
                announcement(id = "e-${event.key}", type = AnnouncementType.CELEBRATION, event = event),
            )

            assertWithMessage(event.key)
                .that(store.currentAnnouncement.first()?.event)
                .isEqualTo(event)
        }
    }

    @Test
    fun `a later announcement replaces the one before it`() = runTest {
        store.setCurrentAnnouncement(announcement(id = "first"))

        store.setCurrentAnnouncement(announcement(id = "second"))

        assertThat(store.currentAnnouncement.first()?.id).isEqualTo("second")
    }

    // ---- Dismissing one ----

    @Test
    fun `dismissing the current announcement takes it off the screen`() = runTest {
        store.setCurrentAnnouncement(announcement(id = "showing"))

        store.dismiss("showing")

        assertThat(store.currentAnnouncement.first()).isNull()
        assertThat(store.dismissedIds.first()).contains("showing")
    }

    @Test
    fun `dismissing a different announcement leaves the current one alone`() = runTest {
        // Dismissals arrive by id, and an id from an earlier campaign must not clear today's.
        store.setCurrentAnnouncement(announcement(id = "showing"))

        store.dismiss("some-older-one")

        assertThat(store.currentAnnouncement.first()?.id).isEqualTo("showing")
        assertThat(store.dismissedIds.first()).contains("some-older-one")
    }

    @Test
    fun `dismissals accumulate rather than replacing each other`() = runTest {
        store.dismiss("one")
        store.dismiss("two")
        store.dismiss("three")

        assertThat(store.dismissedIds.first()).containsAtLeast("one", "two", "three")
    }

    @Test
    fun `dismissing the same announcement twice is not an error`() = runTest {
        store.setCurrentAnnouncement(announcement(id = "twice"))

        store.dismiss("twice")
        store.dismiss("twice")

        assertThat(store.dismissedIds.first()).contains("twice")
        assertThat(store.currentAnnouncement.first()).isNull()
    }

    // ---- A payload this build does not understand ----

    @Test
    fun `an announcement of a type this build has never heard of resolves to nothing`() = runTest {
        // Types are added server-side. An older build receiving a newer one must show no banner
        // rather than a half-built one with a blank type.
        val fromTheFuture = announcement(id = "future").copy(type = AnnouncementType.FEATURE)
        store.setCurrentAnnouncement(fromTheFuture)
        assertThat(store.currentAnnouncement.first()).isNotNull()

        // The stored shape is JSON keyed by `type.key`; an unknown key is the same situation.
        val entity = fromTheFuture.toEntity().copy(type = "a_type_from_the_future")

        assertThat(entity.toDomain()).isNull()
    }

    @Test
    fun `an occasion this build has never heard of falls back to the generic treatment`() {
        // Not null, and deliberately so: a celebration announcement is still a celebration, so
        // an occasion added server-side after this build shipped gets the untitled treatment
        // rather than costing the reader the banner.
        val entity = announcement(id = "e", type = AnnouncementType.CELEBRATION)
            .toEntity()
            .copy(event = "an_occasion_from_the_future")

        val domain = entity.toDomain()

        assertThat(domain).isNotNull()
        assertThat(domain?.event).isEqualTo(CelebrationEvent.GENERIC)
    }

    @Test
    fun `an occasion key is matched after trimming and lowercasing`() {
        // Keys come from a hand-authored campaign payload.
        val entity = announcement(id = "e", type = AnnouncementType.CELEBRATION)
            .toEntity()
            .copy(event = "  EID_AL_FITR  ".replace("_", "_"))

        assertThat(entity.toDomain()?.event).isEqualTo(CelebrationEvent.EID_AL_FITR)
    }

    @Test
    fun `an announcement with none of its optional fields still resolves`() {
        val bare = Announcement(
            id = "bare",
            type = AnnouncementType.PRIVACY,
            title = "Title",
            body = "Body",
        )

        assertThat(bare.toEntity().toDomain()).isEqualTo(bare)
    }
}
