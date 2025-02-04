package com.arshadshah.nimaz.utils

import com.arshadshah.nimaz.data.local.models.LocalAya
import com.arshadshah.nimaz.ui.components.settings.state.FloatPreferenceSettingValueState
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

    fun setFontBasedOnFontStyle(
        fontStyle: String,
        arabicFontSizeState: FloatPreferenceSettingValueState,
        translationFontSizeState: FloatPreferenceSettingValueState,
    ) {
        when (fontStyle) {
            "Default" -> {
                arabicFontSizeState.value = 26f
                translationFontSizeState.value = 16f
            }

            "Quranme" -> {
                arabicFontSizeState.value = 24f
                translationFontSizeState.value = 16f
            }

            "Hidayat" -> {
                arabicFontSizeState.value = 24f
                translationFontSizeState.value = 16f
            }

            "Amiri" -> {
                arabicFontSizeState.value = 24f
                translationFontSizeState.value = 16f
            }

            "IndoPak" -> {
                arabicFontSizeState.value = 32f
                translationFontSizeState.value = 16f
            }
        }
    }
}