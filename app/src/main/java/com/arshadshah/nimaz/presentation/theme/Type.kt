package com.arshadshah.nimaz.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.TranslationLanguage

// Font Families - Custom fonts
// - Outfit (variable font) for headlines
// - Plus Jakarta Sans (variable font) for body
// - Amiri (regular, bold) for Arabic text

// Using variable fonts - they will use default weight and respond to FontWeight
val OutfitFontFamily = FontFamily(
    Font(R.font.outfit_variable, weight = FontWeight.Normal),
    Font(R.font.outfit_variable, weight = FontWeight.Medium),
    Font(R.font.outfit_variable, weight = FontWeight.SemiBold),
    Font(R.font.outfit_variable, weight = FontWeight.Bold)
)

val PlusJakartaSansFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans_variable, weight = FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_variable, weight = FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_variable, weight = FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_variable, weight = FontWeight.Bold)
)

val AmiriFontFamily = FontFamily(
    Font(R.font.amiri_regular, weight = FontWeight.Normal),
    Font(R.font.amiri_bold, weight = FontWeight.Bold)
)

val ScheherazadeFontFamily = FontFamily(
    Font(R.font.scheherazade_new_regular, weight = FontWeight.Normal),
    Font(R.font.scheherazade_new_bold, weight = FontWeight.Bold)
)

// IndoPak Nastaʿlīq for the 16-line IndoPak Mushaf (issue #267, part of #263).
// "AlQuran IndoPak by QuranWBW" — the exact companion font for the QUL IndoPak
// text/layout bundled in assets/quran/ (ayahs_indopak.json). Besides full IndoPak
// letterform + diacritic coverage, it carries the per-ayah number ornaments in its
// Private Use Area (U+F500…U+F6FF) that the source text embeds at every ayah end —
// no other Arabic font renders those. Ships unmodified (its terms forbid editing),
// single Regular weight only. Attribution/licence: docs/FONT_LICENSES.md.
val IndoPakFontFamily = FontFamily(
    Font(R.font.indopak_nastaleeq, weight = FontWeight.Normal)
)

// Noto Nastaliq Urdu for Urdu *translation* text. Urdu is conventionally set in
// Nastaliq, and the app's Latin faces (Outfit/Plus Jakarta) carry no Arabic-script
// glyphs at all, so an Urdu translation falls back to whatever the system happens to
// have — usually a Naskh face, which reads to an Urdu speaker roughly the way blackletter
// reads in English. The Quran's own Arabic keeps using the selected QuranArabicFont; this
// face is only for translation prose. Variable weight axis (minSdk 29 > the API 26 needed
// for variable fonts), loaded at its default Regular instance.
val NotoNastaliqUrduFontFamily = FontFamily(
    Font(R.font.noto_nastaliq_urdu, weight = FontWeight.Normal)
)

/**
 * The face a translation's prose should be drawn in, or null to keep the default body
 * font. Driven by the translation's [TranslationLanguage] rather than by the translation
 * id, so a second Urdu translation needs no change here.
 *
 * Only Urdu is special-cased today: it is the one shipped language whose script the app's
 * body fonts cannot render. Other RTL languages would slot in the same way.
 */
fun translationFontFamily(language: TranslationLanguage): FontFamily? = when (language) {
    TranslationLanguage.URDU -> NotoNastaliqUrduFontFamily
    else -> null
}

/**
 * Selectable Arabic fonts for Quran text.
 *
 * Single source of truth for the font picker: the [id] is what gets persisted
 * in DataStore, [displayName] is shown in the settings list, and [fontFamily]
 * is what the renderer actually draws with. To add another font, drop the
 * .ttf into res/font/, declare a FontFamily above, and add an entry here —
 * the settings screen, preview, and reader all derive from this enum.
 */
enum class QuranArabicFont(
    val id: String,
    val displayName: String,
    val fontFamily: FontFamily
) {
    AMIRI("amiri", "Amiri", AmiriFontFamily),
    SCHEHERAZADE("scheherazade", "Scheherazade New", ScheherazadeFontFamily),
    INDOPAK("indopak", "IndoPak (Nastaʿlīq)", IndoPakFontFamily);

    companion object {
        val DEFAULT = AMIRI
        fun fromId(id: String?): QuranArabicFont = entries.find { it.id == id } ?: DEFAULT
    }
}


// Typography
val NimazTypography = Typography(
    // Display styles - For large, prominent text
    displayLarge = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),

    // Headline styles - For section headers
    headlineLarge = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),

    // Title styles - For card titles and list items
    titleLarge = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),

    // Body styles - For main content text
    bodyLarge = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),

    // Label styles - For buttons, tabs, and small labels
    labelLarge = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

// Arabic Text Styles (for Quran, Hadith, Dua)
object ArabicTextStyles {
    val quranLarge = TextStyle(
        fontFamily = AmiriFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 56.sp,
        letterSpacing = 0.sp
    )

    val quranMedium = TextStyle(
        fontFamily = AmiriFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    )

    val quranSmall = TextStyle(
        fontFamily = AmiriFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    )

    val hadithArabic = TextStyle(
        fontFamily = AmiriFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    )

    val duaArabic = TextStyle(
        fontFamily = AmiriFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    )

    val bismillah = TextStyle(
        fontFamily = AmiriFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 48.sp,
        letterSpacing = 0.sp
    )
}

// Prayer Time Text Styles
object PrayerTextStyles {
    val prayerName = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    )

    val prayerTime = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    )

    val countdown = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    )

    val currentPrayer = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    )
}

/**
 * Returns the appropriate Typography based on the given locale code.
 * Currently all supported locales use the default Latin typography.
 */
fun typographyForLocale(localeCode: String): Typography {
    return NimazTypography
}

// Counter Text Styles (for Tasbih)
object CounterTextStyles {
    val counterLarge = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 72.sp,
        lineHeight = 80.sp
    )

    val counterMedium = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 56.sp
    )

    val targetCount = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 28.sp
    )
}
