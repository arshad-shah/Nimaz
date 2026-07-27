package com.arshadshah.nimaz.presentation.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDirection
import com.arshadshah.nimaz.domain.model.quran.catalogue.TranslationEdition

/**
 * How a translation's text should be drawn — the presentation half of a [TranslationEdition]
 * (ADR-002).
 *
 * Translation text used to be rendered with no direction and no per-edition font, which was
 * invisible while the only shipped translation was English. It stops being invisible the
 * moment an Urdu, Arabic or Persian translation ships: the glyphs would still shape
 * right-to-left (Compose detects that from the content), but the *paragraph* would align to
 * the layout direction, so every line would sit flush left and wrap from the wrong edge.
 * Setting [textDirection] fixes both, because `TextAlign.Start` resolves against it.
 */
val TranslationEdition.textDirection: TextDirection
    get() = if (isRightToLeft) TextDirection.Rtl else TextDirection.Ltr

/**
 * The font family to draw this translation with, or `null` to keep the default body font —
 * correct for Latin-script translations, which have no `fontId`.
 *
 * A non-null `fontId` resolves through [QuranArabicFont], the same registry the Arabic text
 * uses, since a translation needing its own face needs an Arabic-script one (Urdu wants the
 * Nastaʿlīq family, not the body font).
 */
val TranslationEdition.fontFamily: FontFamily?
    get() = fontId?.let { QuranArabicFont.fromId(it).fontFamily }
