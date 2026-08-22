package com.arshadshah.nimaz.data.announcement

import com.arshadshah.nimaz.domain.model.AnnouncementType
import com.arshadshah.nimaz.domain.model.CelebrationEvent
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AnnouncementPayloadMapperTest {

    private val mapper = AnnouncementPayloadMapper()

    private fun validPayload() = mutableMapOf(
        "id" to "2026-07-ask-with-proof",
        "type" to "feature",
        "title" to "Ask with Proof is here",
        "body" to "Search the Qur'an, get cited answers.",
        "cta_label" to "Try it",
        "route" to "search/ask",
        "min_version_code" to "142",
        "max_version_code" to "999",
        "expires_at" to "2026-08-01T00:00:00Z",
        "dismissable" to "true",
    )

    @Test
    fun `valid payload maps all fields`() {
        val announcement = mapper.fromPayload(validPayload())

        assertThat(announcement).isNotNull()
        announcement!!
        assertThat(announcement.id).isEqualTo("2026-07-ask-with-proof")
        assertThat(announcement.type).isEqualTo(AnnouncementType.FEATURE)
        assertThat(announcement.title).isEqualTo("Ask with Proof is here")
        assertThat(announcement.body).isEqualTo("Search the Qur'an, get cited answers.")
        assertThat(announcement.ctaLabel).isEqualTo("Try it")
        assertThat(announcement.route).isEqualTo("search/ask")
        assertThat(announcement.minVersionCode).isEqualTo(142)
        assertThat(announcement.maxVersionCode).isEqualTo(999)
        assertThat(announcement.expiresAtMillis).isEqualTo(1_785_542_400_000L)
        assertThat(announcement.dismissable).isTrue()
    }

    @Test
    fun `minimal payload maps with defaults`() {
        val announcement = mapper.fromPayload(
            mapOf(
                "id" to "x",
                "type" to "changelog",
                "title" to "t",
                "body" to "b",
            )
        )

        assertThat(announcement).isNotNull()
        announcement!!
        assertThat(announcement.ctaLabel).isNull()
        assertThat(announcement.route).isNull()
        assertThat(announcement.minVersionCode).isNull()
        assertThat(announcement.maxVersionCode).isNull()
        assertThat(announcement.expiresAtMillis).isNull()
        assertThat(announcement.dismissable).isTrue()
    }

    @Test
    fun `missing required field returns null`() {
        listOf("id", "type", "title", "body").forEach { required ->
            val payload = validPayload().apply { remove(required) }
            assertThat(mapper.fromPayload(payload)).isNull()
        }
    }

    @Test
    fun `blank required field returns null`() {
        val payload = validPayload().apply { put("title", "   ") }
        assertThat(mapper.fromPayload(payload)).isNull()
    }

    @Test
    fun `unknown type returns null`() {
        val payload = validPayload().apply { put("type", "promo") }
        assertThat(mapper.fromPayload(payload)).isNull()
    }

    @Test
    fun `malformed version code returns null`() {
        val payload = validPayload().apply { put("min_version_code", "abc") }
        assertThat(mapper.fromPayload(payload)).isNull()
    }

    @Test
    fun `malformed expires_at returns null`() {
        val payload = validPayload().apply { put("expires_at", "tomorrow") }
        assertThat(mapper.fromPayload(payload)).isNull()
    }

    @Test
    fun `malformed dismissable returns null`() {
        val payload = validPayload().apply { put("dismissable", "yes") }
        assertThat(mapper.fromPayload(payload)).isNull()
    }

    @Test
    fun `dismissable false is honoured`() {
        val payload = validPayload().apply { put("dismissable", "false") }
        assertThat(mapper.fromPayload(payload)!!.dismissable).isFalse()
    }

    @Test
    fun `type parsing is case-insensitive`() {
        val payload = validPayload().apply { put("type", "Privacy") }
        assertThat(mapper.fromPayload(payload)!!.type).isEqualTo(AnnouncementType.PRIVACY)
    }

    @Test
    fun `reserved and unknown keys are ignored`() {
        val payload = validPayload().apply {
            put("from", "123456")
            put("message_type", "gcm")
            put("google.message_id", "m-1")
            put("gcm.notification.title", "x")
            put("collapse_key", "ck")
        }
        assertThat(mapper.fromPayload(payload)).isNotNull()
    }

    @Test
    fun `blank optional fields become null`() {
        val payload = validPayload().apply {
            put("cta_label", "")
            put("route", " ")
        }
        val announcement = mapper.fromPayload(payload)!!
        assertThat(announcement.ctaLabel).isNull()
        assertThat(announcement.route).isNull()
    }

    @Test
    fun `celebration payload parses event and rich fields`() {
        val a = mapper.fromPayload(
            mapOf(
                "id" to "2027-eid", "type" to "celebration", "event" to "eid_al_fitr",
                "title" to "Eid Mubarak", "body" to "…",
                "arabic" to "تقبل الله", "transliteration" to "taqabbal Allah",
                "proof_ref" to "Al-Baqarah 2:185", "proof_text" to "…complete the count.",
                "cta_label" to "Eid prayer", "route" to "prayer/times",
                "cta2_label" to "Takbir", "route2" to "dua/reader/takbir",
                "starts_at" to "2027-03-07T18:00:00Z",
            )
        )
        assertThat(a).isNotNull()
        assertThat(a!!.type).isEqualTo(AnnouncementType.CELEBRATION)
        assertThat(a.event).isEqualTo(CelebrationEvent.EID_AL_FITR)
        assertThat(a.arabic).isEqualTo("تقبل الله")
        assertThat(a.proofRef).isEqualTo("Al-Baqarah 2:185")
        assertThat(a.cta2Label).isEqualTo("Takbir")
        assertThat(a.route2).isEqualTo("dua/reader/takbir")
        assertThat(a.startsAtMillis).isNotNull()
    }

    @Test
    fun `unknown event degrades to GENERIC not null`() {
        val a = mapper.fromPayload(
            mapOf("id" to "x", "type" to "celebration", "event" to "wat",
                  "title" to "t", "body" to "b")
        )
        assertThat(a).isNotNull()
        assertThat(a!!.event).isEqualTo(CelebrationEvent.GENERIC)
    }

    @Test
    fun `event ignored for non-celebration types`() {
        val a = mapper.fromPayload(
            mapOf("id" to "x", "type" to "feature", "event" to "eid_al_fitr",
                  "title" to "t", "body" to "b")
        )
        assertThat(a!!.event).isNull()
    }

    @Test
    fun `half a proof pair is dropped, rest survives`() {
        val a = mapper.fromPayload(
            mapOf("id" to "x", "type" to "celebration", "title" to "t", "body" to "b",
                  "proof_ref" to "only ref")
        )
        assertThat(a).isNotNull()
        assertThat(a!!.proofRef).isNull()
        assertThat(a.proofText).isNull()
    }

    @Test
    fun `malformed starts_at rejects the whole payload`() {
        val a = mapper.fromPayload(
            mapOf("id" to "x", "type" to "celebration", "title" to "t", "body" to "b",
                  "starts_at" to "not-a-date")
        )
        assertThat(a).isNull()
    }
}
