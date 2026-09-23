package com.arshadshah.nimaz.presentation.screens.learnpray

import androidx.annotation.StringRes
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.ContentTarget

/** Source URLs are provenance, never reader addresses. Corpus ids are verified against the text. */
internal data class PrayerReference(
    @param:StringRes val label: Int,
    val number: String,
    val target: ContentTarget?,
) {
    companion object {
        // Stable local record ids, NOT printed hadith numbers. Muslim's corpus uses sequential
        // numbering: e.g. the report cited as 772 is local muslim:1814, record 9403.
        // See scripts/test_learn_pray_references.py for the corpus/text cross-check.
        private val hadithIds = mapOf(
            "bukhari:1117" to "1120", "bukhari:3370" to "3382",
            "bukhari:365" to "365", "bukhari:631" to "632",
            "bukhari:735" to "737", "bukhari:740" to "742",
            "bukhari:744" to "746", "bukhari:756" to "758",
            "bukhari:757" to "759", "bukhari:759" to "761",
            "bukhari:780" to "783", "bukhari:789" to "792",
            "bukhari:812" to "815", "bukhari:822" to "825",
            "bukhari:828" to "831",
            "muslim:402a" to "8486", "muslim:580b" to "8899",
            "muslim:588a" to "8913", "muslim:772" to "9403",
            "muslim:582" to "8904",
            "abudawud:640" to "15792", "abudawud:641" to "15793",
            "abudawud:874" to "16026", "abudawud:996" to "16148",
        )

        fun fromUrl(url: String): PrayerReference {
            if (url.startsWith("https://quran.com/")) {
                val parts = url.removePrefix("https://quran.com/").split('/')
                val surah = parts[0].toInt()
                val ayah = parts.getOrNull(1)?.toInt() ?: 1
                return PrayerReference(R.string.learn_pray_reference_quran,
                    "$surah:$ayah", ContentTarget.Ayah(surah, ayah))
            }
            if (url.startsWith("https://sunnah.com/")) {
                val ref = url.removePrefix("https://sunnah.com/")
                val label = when (ref.substringBefore(':')) {
                    "bukhari" -> R.string.learn_pray_reference_bukhari
                    "muslim" -> R.string.learn_pray_reference_muslim
                    "abudawud" -> R.string.learn_pray_reference_abudawud
                    else -> error("Unmapped lesson collection: $ref")
                }
                return PrayerReference(label, ref.substringAfter(':'),
                    ContentTarget.Hadith(hadithIds.getValue(ref)))
            }
            val label = when {
                url.startsWith("https://seekersguidance.org/") -> R.string.learn_pray_reference_seekers
                url.startsWith("https://islamqa.org/hanafi/askimam/") -> R.string.learn_pray_reference_askimam
                else -> error("Unmapped lesson reference: $url")
            }
            return PrayerReference(label, "", null)
        }
    }
}
