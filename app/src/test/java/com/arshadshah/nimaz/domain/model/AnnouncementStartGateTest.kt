package com.arshadshah.nimaz.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AnnouncementStartGateTest {

    private fun ann(startsAt: Long?, expiresAt: Long?) = Announcement(
        id = "x", type = AnnouncementType.CELEBRATION, title = "t", body = "b",
        startsAtMillis = startsAt, expiresAtMillis = expiresAt,
    )

    @Test
    fun `not active before startsAt`() {
        assertThat(ann(startsAt = 2_000, expiresAt = null).isActiveFor(1, nowMillis = 1_000)).isFalse()
    }

    @Test
    fun `active at or after startsAt`() {
        assertThat(ann(startsAt = 2_000, expiresAt = null).isActiveFor(1, nowMillis = 2_000)).isTrue()
        assertThat(ann(startsAt = 2_000, expiresAt = null).isActiveFor(1, nowMillis = 3_000)).isTrue()
    }

    @Test
    fun `null startsAt means always started`() {
        assertThat(ann(startsAt = null, expiresAt = null).isActiveFor(1, nowMillis = 0)).isTrue()
    }

    @Test
    fun `start and expiry window both enforced`() {
        val a = ann(startsAt = 2_000, expiresAt = 4_000)
        assertThat(a.isActiveFor(1, 1_999)).isFalse()
        assertThat(a.isActiveFor(1, 2_000)).isTrue()
        assertThat(a.isActiveFor(1, 3_999)).isTrue()
        assertThat(a.isActiveFor(1, 4_000)).isFalse()
    }

    @Test
    fun `CelebrationEvent fromKey degrades unknown to GENERIC`() {
        assertThat(CelebrationEvent.fromKey("eid_al_fitr")).isEqualTo(CelebrationEvent.EID_AL_FITR)
        assertThat(CelebrationEvent.fromKey("nonsense")).isEqualTo(CelebrationEvent.GENERIC)
        assertThat(CelebrationEvent.fromKey(null)).isEqualTo(CelebrationEvent.GENERIC)
    }
}
