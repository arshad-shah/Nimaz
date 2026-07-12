package com.arshadshah.nimaz.data.announcement

import com.arshadshah.nimaz.domain.model.AnnouncementType
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
}
