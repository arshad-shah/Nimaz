package com.arshadshah.nimaz.data.announcement

import android.os.Bundle
import com.arshadshah.nimaz.domain.model.AnnouncementType
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The second way an announcement arrives, and the blank-field rule that applies to both.
 *
 * When the app is backgrounded or killed the OS posts the tray notification itself and copies
 * the message's custom data onto the tap intent as string extras — so the same payload reaches
 * the same mapper by a different road. The two roads have to agree, or an announcement opens
 * correctly from a running app and blankly from a cold start.
 *
 * The blank-field rule is what the console makes easy to get wrong: an empty text box in the
 * FCM console sends `""`, not nothing. A `cta_label` of `""` would draw a button with no words
 * on it, and a `proof_ref` of `""` a citation card citing nothing — so every optional field is
 * trimmed and emptied to null.
 */
@RunWith(RobolectricTestRunner::class)
class AnnouncementPayloadExtrasTest {

    private val mapper = AnnouncementPayloadMapper()

    @Test
    fun `a cold start with no extras at all opens no announcement`() {
        assertThat(mapper.fromIntentExtras(null)).isNull()
    }

    @Test
    fun `extras that carry nothing the mapper recognises open no announcement`() {
        val extras = Bundle().apply { putString("unrelated", "value") }

        assertThat(mapper.fromIntentExtras(extras)).isNull()
    }

    @Test
    fun `extras carrying the payload map to the same announcement as the payload does`() {
        val data = mapOf(
            "id" to "2026-07-ask-with-proof",
            "type" to "feature",
            "title" to "Ask with Proof is here",
            "body" to "Search the Qur'an, get cited answers.",
            "cta_label" to "Try it",
            "route" to "search/ask",
        )
        val extras = Bundle().apply { data.forEach { (k, v) -> putString(k, v) } }

        assertThat(mapper.fromIntentExtras(extras)).isEqualTo(mapper.fromPayload(data))
    }

    @Test
    fun `a non string extra is ignored rather than crashing the cold start`() {
        val extras = Bundle().apply {
            putString("id", "x")
            putString("type", "feature")
            putString("title", "t")
            putString("body", "b")
            // The OS copies strings, but nothing stops another sender putting an Int here.
            putInt("min_version_code", 142)
        }

        val announcement = mapper.fromIntentExtras(extras)!!

        assertThat(announcement.minVersionCode).isNull()
    }

    @Test
    fun `every optional field sent as an empty box comes back as absent, not blank`() {
        val announcement = mapper.fromPayload(
            mapOf(
                "id" to "x", "type" to "feature", "title" to "t", "body" to "b",
                "cta_label" to "  ", "route" to "", "arabic" to " ",
                "transliteration" to "", "cta2_label" to "  ", "route2" to "",
                "proof_ref" to " ", "proof_text" to "",
            )
        )!!

        // An empty console text box sends "": a button with no words, a card citing nothing.
        assertThat(announcement.ctaLabel).isNull()
        assertThat(announcement.route).isNull()
        assertThat(announcement.arabic).isNull()
        assertThat(announcement.transliteration).isNull()
        assertThat(announcement.cta2Label).isNull()
        assertThat(announcement.route2).isNull()
        assertThat(announcement.proofRef).isNull()
        assertThat(announcement.proofText).isNull()
    }

    @Test
    fun `every optional field sent with content is trimmed and kept`() {
        val announcement = mapper.fromPayload(
            mapOf(
                "id" to " x ", "type" to "celebration", "title" to " t ", "body" to " b ",
                "cta_label" to " Try it ", "route" to " search/ask ",
                "arabic" to " عيد ", "transliteration" to " Eid ",
                "cta2_label" to " Later ", "route2" to " more ",
                "proof_ref" to " 2:255 ", "proof_text" to " Allahu la ilaha illa huwa ",
            )
        )!!

        assertThat(announcement.id).isEqualTo("x")
        assertThat(announcement.ctaLabel).isEqualTo("Try it")
        assertThat(announcement.route).isEqualTo("search/ask")
        assertThat(announcement.arabic).isEqualTo("عيد")
        assertThat(announcement.transliteration).isEqualTo("Eid")
        assertThat(announcement.cta2Label).isEqualTo("Later")
        assertThat(announcement.route2).isEqualTo("more")
        assertThat(announcement.proofRef).isEqualTo("2:255")
        assertThat(announcement.proofText).isEqualTo("Allahu la ilaha illa huwa")
    }

    @Test
    fun `a proof text with no reference is dropped, and so is the reverse`() {
        val textOnly = mapper.fromPayload(
            mapOf(
                "id" to "x", "type" to "celebration", "title" to "t", "body" to "b",
                "proof_text" to "some words", "proof_ref" to "  ",
            )
        )!!

        // A quotation with nothing to cite is not a proof card.
        assertThat(textOnly.proofText).isNull()
        assertThat(textOnly.proofRef).isNull()
    }

    @Test
    fun `a version window sent as an empty box rejects the whole payload`() {
        // "" is not a version, and treating it as "no bound" would show a build-targeted
        // announcement to every install.
        assertThat(
            mapper.fromPayload(
                mapOf("id" to "x", "type" to "feature", "title" to "t", "body" to "b",
                      "min_version_code" to "")
            )
        ).isNull()
        assertThat(
            mapper.fromPayload(
                mapOf("id" to "x", "type" to "feature", "title" to "t", "body" to "b",
                      "max_version_code" to " ")
            )
        ).isNull()
    }

    @Test
    fun `a version window sent with padding still parses`() {
        val announcement = mapper.fromPayload(
            mapOf("id" to "x", "type" to "feature", "title" to "t", "body" to "b",
                  "min_version_code" to " 142 ", "max_version_code" to " 999 ")
        )!!

        assertThat(announcement.minVersionCode).isEqualTo(142)
        assertThat(announcement.maxVersionCode).isEqualTo(999)
        assertThat(announcement.type).isEqualTo(AnnouncementType.fromKey("feature"))
    }
}
