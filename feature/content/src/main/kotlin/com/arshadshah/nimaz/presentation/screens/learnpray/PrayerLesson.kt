package com.arshadshah.nimaz.presentation.screens.learnpray

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.presentation.model.PrayerFigure
import com.arshadshah.nimaz.feature.content.R as ContentR

/** Versioned, app-owned lesson presentation. No prayer-tracker writes or network dependency. */
internal data class PrayerRecitation(
    @param:StringRes val title: Int,
    @param:StringRes val arabic: Int,
    @param:StringRes val transliteration: Int,
    @param:StringRes val meaning: Int,
)

internal data class PrayerLessonStep(
    val id: String,
    val rakah: Int,
    @param:StringRes val title: Int,
    @param:StringRes val instruction: Int,
    @param:DrawableRes val artwork: Int,
    @param:DrawableRes val femaleArtwork: Int,
    val recitations: List<PrayerRecitation>,
    val references: List<String>,
) {
    @DrawableRes
    fun artworkFor(figure: PrayerFigure): Int =
        if (figure == PrayerFigure.WOMAN) femaleArtwork else artwork

    @StringRes
    fun instructionFor(figure: PrayerFigure): Int {
        if (figure != PrayerFigure.WOMAN) return instruction
        return when (id) {
            "bow_1", "bow_2" -> R.string.learn_pray_female_bowing_action
            "prostrate_1a", "prostrate_2a" -> R.string.learn_pray_female_prostration_action
            "prostrate_1b", "prostrate_2b" -> R.string.learn_pray_female_second_action
            "sit_1", "sit_2" -> R.string.learn_pray_female_sitting_action
            "tashahhud" -> R.string.learn_pray_female_tashahhud_action
            else -> instruction
        }
    }

    // A recitation source must not be presented as proof of an uncited posture variant.
    fun referencesFor(figure: PrayerFigure): List<String> {
        if (figure != PrayerFigure.WOMAN) return references
        return when (id) {
            "bow_1", "bow_2" -> listOf("https://sunnah.com/muslim:772")
            "prostrate_1a", "prostrate_2a", "prostrate_1b", "prostrate_2b" -> listOf(
                "https://sunnah.com/muslim:772",
                "https://seekersguidance.org/answers/hanafi-fiqh/what-is-the-proper-method-for-women-to-bow-and-prostrate-in-the-prayer/",
            )
            "sit_1", "sit_2", "tashahhud" -> references.filterNot { it == "https://sunnah.com/bukhari:828" } +
                "https://islamqa.org/hanafi/askimam/126663/womans-sitting-posture-in-salah/"
            else -> references
        }
    }
}

internal object PrayerLesson {
    private val opening = PrayerRecitation(
        R.string.learn_pray_optional_opening,
        R.string.learn_pray_opening_ar,
        R.string.learn_pray_opening_tr,
        R.string.learn_pray_opening_en,
    )
    private val refuge = PrayerRecitation(
        R.string.learn_pray_refuge_title,
        R.string.learn_pray_refuge_ar,
        R.string.learn_pray_refuge_tr,
        R.string.learn_pray_refuge_en,
    )
    private val fatihah = PrayerRecitation(
        R.string.learn_pray_fatihah_title,
        R.string.learn_pray_fatihah_ar,
        R.string.learn_pray_fatihah_tr,
        R.string.learn_pray_fatihah_en,
    )
    private val amin = PrayerRecitation(
        R.string.learn_pray_amin_title,
        R.string.learn_pray_amin_ar,
        R.string.learn_pray_amin_tr,
        R.string.learn_pray_amin_en,
    )
    private val ikhlas = PrayerRecitation(
        R.string.learn_pray_ikhlas_title,
        R.string.learn_pray_ikhlas_ar,
        R.string.learn_pray_ikhlas_tr,
        R.string.learn_pray_ikhlas_en,
    )
    private val takbir = PrayerRecitation(
        R.string.learn_pray_recite,
        R.string.learn_pray_takbir_ar,
        R.string.learn_pray_takbir_tr,
        R.string.learn_pray_takbir_en,
    )
    private val ruku = PrayerRecitation(
        R.string.learn_pray_recite,
        R.string.learn_pray_ruku_ar,
        R.string.learn_pray_ruku_tr,
        R.string.learn_pray_ruku_en,
    )
    private val rise = PrayerRecitation(
        R.string.learn_pray_recite,
        R.string.learn_pray_rise_ar,
        R.string.learn_pray_rise_tr,
        R.string.learn_pray_rise_en,
    )
    private val sujud = PrayerRecitation(
        R.string.learn_pray_recite,
        R.string.learn_pray_sujud_ar,
        R.string.learn_pray_sujud_tr,
        R.string.learn_pray_sujud_en,
    )
    private val sit = PrayerRecitation(
        R.string.learn_pray_recite,
        R.string.learn_pray_sit_ar,
        R.string.learn_pray_sit_tr,
        R.string.learn_pray_sit_en,
    )
    private val tashahhud = PrayerRecitation(
        R.string.learn_pray_recite,
        R.string.learn_pray_tashahhud_ar,
        R.string.learn_pray_tashahhud_tr,
        R.string.learn_pray_tashahhud_en,
    )
    private val salawat = PrayerRecitation(
        R.string.learn_pray_recite,
        R.string.learn_pray_salawat_ar,
        R.string.learn_pray_salawat_tr,
        R.string.learn_pray_salawat_en,
    )
    private val dua = PrayerRecitation(
        R.string.learn_pray_recite,
        R.string.learn_pray_dua_ar,
        R.string.learn_pray_dua_tr,
        R.string.learn_pray_dua_en,
    )
    private val salam = PrayerRecitation(
        R.string.learn_pray_recite,
        R.string.learn_pray_salam_ar,
        R.string.learn_pray_salam_tr,
        R.string.learn_pray_salam_en,
    )
    val steps = listOf(
        PrayerLessonStep(
            "opening_takbir", 1, R.string.learn_pray_takbir_title,
            R.string.learn_pray_takbir_action, ContentR.drawable.learn_pray_takbir, ContentR.drawable.learn_pray_female_takbir,
            listOf(takbir),
            listOf("https://sunnah.com/bukhari:735", "https://sunnah.com/bukhari:740"),
        ),
        PrayerLessonStep(
            "recitation_1", 1, R.string.learn_pray_standing_title,
            R.string.learn_pray_standing_action, ContentR.drawable.learn_pray_standing, ContentR.drawable.learn_pray_female_standing,
            listOf(opening, refuge, fatihah, amin, ikhlas),
            listOf("https://sunnah.com/bukhari:744", "https://sunnah.com/bukhari:756", "https://sunnah.com/bukhari:759", "https://sunnah.com/bukhari:780", "https://quran.com/16/98", "https://quran.com/1", "https://quran.com/112"),
        ),
        PrayerLessonStep(
            "bow_1", 1, R.string.learn_pray_bowing_title,
            R.string.learn_pray_bowing_action, ContentR.drawable.learn_pray_bowing, ContentR.drawable.learn_pray_female_bowing,
            listOf(ruku),
            listOf("https://sunnah.com/bukhari:735", "https://sunnah.com/bukhari:828", "https://sunnah.com/muslim:772"),
        ),
        PrayerLessonStep(
            "rise_1", 1, R.string.learn_pray_rise_title,
            R.string.learn_pray_rise_action, ContentR.drawable.learn_pray_rise, ContentR.drawable.learn_pray_female_rise,
            listOf(rise),
            listOf("https://sunnah.com/bukhari:735", "https://sunnah.com/bukhari:757"),
        ),
        PrayerLessonStep(
            "prostrate_1a", 1, R.string.learn_pray_prostration_title,
            R.string.learn_pray_prostration_action, ContentR.drawable.learn_pray_prostration, ContentR.drawable.learn_pray_female_prostration,
            listOf(sujud),
            listOf("https://sunnah.com/bukhari:812", "https://sunnah.com/bukhari:822", "https://sunnah.com/muslim:772"),
        ),
        PrayerLessonStep(
            "sit_1", 1, R.string.learn_pray_sitting_title,
            R.string.learn_pray_sitting_action, ContentR.drawable.learn_pray_sitting, ContentR.drawable.learn_pray_female_sitting,
            listOf(sit),
            listOf("https://sunnah.com/bukhari:828", "https://sunnah.com/abudawud:874"),
        ),
        PrayerLessonStep(
            "prostrate_1b", 1, R.string.learn_pray_second_title,
            R.string.learn_pray_second_action, ContentR.drawable.learn_pray_prostration, ContentR.drawable.learn_pray_female_prostration,
            listOf(sujud),
            listOf("https://sunnah.com/bukhari:789", "https://sunnah.com/muslim:772"),
        ),
        PrayerLessonStep(
            "recitation_2", 2, R.string.learn_pray_second_rakah_title,
            R.string.learn_pray_second_rakah_action, ContentR.drawable.learn_pray_standing, ContentR.drawable.learn_pray_female_standing,
            listOf(fatihah, amin, ikhlas),
            listOf("https://sunnah.com/bukhari:789", "https://sunnah.com/bukhari:759", "https://quran.com/1", "https://quran.com/112"),
        ),
        PrayerLessonStep(
            "bow_2", 2, R.string.learn_pray_bowing_title,
            R.string.learn_pray_bowing_action, ContentR.drawable.learn_pray_bowing, ContentR.drawable.learn_pray_female_bowing,
            listOf(ruku),
            listOf("https://sunnah.com/bukhari:735", "https://sunnah.com/bukhari:828", "https://sunnah.com/muslim:772"),
        ),
        PrayerLessonStep(
            "rise_2", 2, R.string.learn_pray_rise_title,
            R.string.learn_pray_rise_action, ContentR.drawable.learn_pray_rise, ContentR.drawable.learn_pray_female_rise,
            listOf(rise),
            listOf("https://sunnah.com/bukhari:735", "https://sunnah.com/bukhari:757"),
        ),
        PrayerLessonStep(
            "prostrate_2a", 2, R.string.learn_pray_prostration_title,
            R.string.learn_pray_prostration_action, ContentR.drawable.learn_pray_prostration, ContentR.drawable.learn_pray_female_prostration,
            listOf(sujud),
            listOf("https://sunnah.com/bukhari:812", "https://sunnah.com/bukhari:822", "https://sunnah.com/muslim:772"),
        ),
        PrayerLessonStep(
            "sit_2", 2, R.string.learn_pray_sitting_title,
            R.string.learn_pray_sitting_action, ContentR.drawable.learn_pray_sitting, ContentR.drawable.learn_pray_female_sitting,
            listOf(sit),
            listOf("https://sunnah.com/bukhari:828", "https://sunnah.com/abudawud:874"),
        ),
        PrayerLessonStep(
            "prostrate_2b", 2, R.string.learn_pray_second_title,
            R.string.learn_pray_second_action, ContentR.drawable.learn_pray_prostration, ContentR.drawable.learn_pray_female_prostration,
            listOf(sujud),
            listOf("https://sunnah.com/bukhari:789", "https://sunnah.com/muslim:772"),
        ),
        PrayerLessonStep(
            "tashahhud", 2, R.string.learn_pray_tashahhud_title,
            R.string.learn_pray_tashahhud_action, ContentR.drawable.learn_pray_tashahhud, ContentR.drawable.learn_pray_female_tashahhud,
            listOf(tashahhud),
            listOf("https://sunnah.com/muslim:402a", "https://sunnah.com/muslim:580b"),
        ),
        PrayerLessonStep(
            "salawat", 2, R.string.learn_pray_salawat_title,
            R.string.learn_pray_salawat_action, ContentR.drawable.learn_pray_tashahhud, ContentR.drawable.learn_pray_female_tashahhud,
            listOf(salawat),
            listOf("https://sunnah.com/bukhari:3370"),
        ),
        PrayerLessonStep(
            "dua", 2, R.string.learn_pray_dua_title,
            R.string.learn_pray_dua_action, ContentR.drawable.learn_pray_tashahhud, ContentR.drawable.learn_pray_female_tashahhud,
            listOf(dua),
            listOf("https://sunnah.com/muslim:588a", "https://sunnah.com/muslim:402a"),
        ),
        PrayerLessonStep(
            "salam_right", 2, R.string.learn_pray_right_title,
            R.string.learn_pray_right_action, ContentR.drawable.learn_pray_right, ContentR.drawable.learn_pray_female_right,
            listOf(salam),
            listOf("https://sunnah.com/muslim:582", "https://sunnah.com/abudawud:996"),
        ),
        PrayerLessonStep(
            "salam_left", 2, R.string.learn_pray_left_title,
            R.string.learn_pray_left_action, ContentR.drawable.learn_pray_left, ContentR.drawable.learn_pray_female_left,
            listOf(salam),
            listOf("https://sunnah.com/muslim:582", "https://sunnah.com/abudawud:996"),
        ),
    )
}
