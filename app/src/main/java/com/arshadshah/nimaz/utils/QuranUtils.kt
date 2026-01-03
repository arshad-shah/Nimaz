package com.arshadshah.nimaz.utils

import com.arshadshah.nimaz.data.local.models.LocalAya
import com.arshadshah.nimaz.ui.theme.almajeed
import com.arshadshah.nimaz.ui.theme.amiri
import com.arshadshah.nimaz.ui.theme.hidayat
import com.arshadshah.nimaz.ui.theme.quranFont
import com.arshadshah.nimaz.ui.theme.utmaniQuranFont
import java.text.NumberFormat
import java.util.Locale

object QuranUtils {

    fun processAyaEnd(ayas: ArrayList<LocalAya>): ArrayList<LocalAya> {
        for (i in ayas.indices) {
            val ayaArabic = ayas[i].ayaArabic
            val ayaNumber = ayas[i].ayaNumberInSurah
            val processedAyaArabic =
                if (ayaNumber != null && ayaArabic != null) ayaEndProcesser(
                    ayaArabic,
                    ayaNumber
                ) else {
                    throw Exception("Invalid aya number")
                }
            ayas[i].ayaArabic = processedAyaArabic
        }
        return ayas
    }

    private fun ayaEndProcesser(arabic: String, ayaNumber: Int): String {
        val unicodeAyaEndEnd = "\uFD3E"
        val unicodeAyaEndStart = "\uFD3F"
        val arabiclocal = Locale.forLanguageTag("ar")
        val nf: NumberFormat = NumberFormat.getInstance(arabiclocal)
        val endOfAyaWithNumber = nf.format(ayaNumber)
        //remove the comma separator from the number
        var unicodeWithNumber = ""
        //if the endOfAyaWithNumber has ٬ then remove it
        unicodeWithNumber = if (endOfAyaWithNumber.contains("٬")) {
            val endOfAyaWithNumberNoComma = endOfAyaWithNumber.replace("٬", "")
            "$unicodeAyaEndStart$endOfAyaWithNumberNoComma$unicodeAyaEndEnd"
        } else {
            "$unicodeAyaEndStart$endOfAyaWithNumber$unicodeAyaEndEnd"
        }

        return "$arabic $unicodeWithNumber"
    }

    fun getArabicFont(fontName: String) = when (fontName) {
        "Default" -> utmaniQuranFont
        "Quranme" -> quranFont
        "Hidayat" -> hidayat
        "Amiri" -> amiri
        "IndoPak" -> almajeed
        else -> utmaniQuranFont
    }

    // Surah aya counts for accurate progress calculation (Surah 1-114)
    val surahAyaCounts = listOf(
        7, 286, 200, 176, 120, 165, 206, 75, 129, 109,   // 1-10
        123, 111, 43, 52, 99, 128, 111, 110, 98, 135,    // 11-20
        112, 78, 118, 64, 77, 227, 93, 88, 69, 60,       // 21-30
        34, 30, 73, 54, 45, 83, 182, 88, 75, 85,         // 31-40
        54, 53, 89, 59, 37, 35, 38, 29, 18, 45,          // 41-50
        60, 49, 62, 55, 78, 96, 29, 22, 24, 13,          // 51-60
        14, 11, 11, 18, 12, 12, 30, 52, 52, 44,          // 61-70
        28, 28, 20, 56, 40, 31, 50, 40, 46, 42,          // 71-80
        29, 19, 36, 25, 22, 17, 19, 26, 30, 20,          // 81-90
        15, 21, 11, 8, 8, 19, 5, 8, 8, 11,               // 91-100
        11, 8, 3, 9, 5, 4, 7, 3, 6, 3,                   // 101-110
        5, 4, 5, 6                                       // 111-114
    )

    fun calculateTotalAyasRead(surahNumber: Int, ayaNumber: Int): Int {
        if (surahNumber < 1 || surahNumber > 114) return 0

        // Sum all ayas from surahs before current surah
        var total = 0
        for (i in 0 until (surahNumber - 1)) {
            total += surahAyaCounts[i]
        }
        // Add the ayas read in current surah
        total += ayaNumber
        return total
    }
}
