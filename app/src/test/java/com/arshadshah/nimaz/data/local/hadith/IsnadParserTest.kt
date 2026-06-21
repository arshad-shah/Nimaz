package com.arshadshah.nimaz.data.local.hadith

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class IsnadParserTest {

    // A real Sahih al-Bukhari isnād (from hadith_fills.json, id 5733).
    private val realIsnad =
        "حَدَّثَنَا عَلِيُّ بْنُ عَبْدِ اللَّهِ، حَدَّثَنَا يَحْيَى بْنُ سَعِيدٍ، حَدَّثَنَا سُفْيَانُ، " +
            "قَالَ حَدَّثَنِي مُوسَى بْنُ أَبِي عَائِشَةَ، عَنْ عُبَيْدِ اللَّهِ بْنِ عَبْدِ اللَّهِ، " +
            "عَنِ ابْنِ عَبَّاسٍ، وَعَائِشَةَ، أَنَّ أَبَا بَكْرٍ ـ رضى الله عنه ـ قَبَّلَ النَّبِيَّ صلى الله عليه وسلم وَهْوَ مَيِّتٌ‏."

    @Test
    fun `parses a multi-narrator chain from real isnad`() {
        val chain = IsnadParser.parse(realIsnad)
        assertThat(chain).isNotNull()
        val nodes = chain!!.split("\n")
        // At least the first few transmitters are recovered, in order.
        assertThat(nodes.size).isAtLeast(3)
        assertThat(nodes[0]).contains("علي بن عبد الله")
        assertThat(nodes[1]).contains("يحيى بن سعيد")
        // Honorifics / matn must not leak in as a "narrator".
        assertThat(chain).doesNotContain("صلى الله عليه وسلم")
        assertThat(nodes.all { it.length <= 40 }).isTrue()
    }

    @Test
    fun `returns null for blank input`() {
        assertThat(IsnadParser.parse(null)).isNull()
        assertThat(IsnadParser.parse("   ")).isNull()
    }

    @Test
    fun `returns null when there is no real chain`() {
        // Plain matn with no transmission links.
        assertThat(IsnadParser.parse("الأعمال بالنيات وإنما لكل امرئ ما نوى")).isNull()
    }
}
