package com.arshadshah.nimaz.data.local.help

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HelpJsonDtoTest {

    private val sample = """
        { "contentVersion": 3, "topics": [
          { "id":"prayer_times","order":1,"icon":"schedule","color":"indigo",
            "title":{"en":"Prayer Times","fr":"Horaires"},
            "subtitle":{"en":"Calculation"},
            "items":[
              {"id":"pt_q1","type":"question","order":1,
               "question":{"en":"Why different?"},"answer":{"en":"Because location."}},
              {"id":"pt_g1","type":"guide","order":2,"icon":"tune","estimatedMinutes":1,
               "title":{"en":"Change method"},
               "steps":[{"id":"pt_g1_s1","order":1,"deeplink":"prayer_settings",
                         "pathLabels":["More","Prayer Settings"],
                         "title":{"en":"Open settings"},"body":{"en":"Go to More."}}]}
            ] }
        ] }
    """.trimIndent()

    @Test
    fun parsesContentVersionTopicsItemsAndSteps() {
        val root = helpJson.decodeFromString(HelpJsonRoot.serializer(), sample)
        assertThat(root.contentVersion).isEqualTo(3)
        assertThat(root.topics).hasSize(1)
        val topic = root.topics.first()
        assertThat(topic.title["fr"]).isEqualTo("Horaires")
        assertThat(topic.items).hasSize(2)
        val guide = topic.items.first { it.type == "guide" }
        assertThat(guide.steps.first().deeplink).isEqualTo("prayer_settings")
        assertThat(guide.steps.first().pathLabels).containsExactly("More", "Prayer Settings")
    }

    @Test
    fun ignoresUnknownKeys() {
        val withExtra = """{ "contentVersion":1, "future":"x", "topics":[] }"""
        val root = helpJson.decodeFromString(HelpJsonRoot.serializer(), withExtra)
        assertThat(root.topics).isEmpty()
    }
}
