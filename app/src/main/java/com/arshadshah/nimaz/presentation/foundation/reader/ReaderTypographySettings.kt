package com.arshadshah.nimaz.presentation.foundation.reader

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazDivider
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.theme.QuranArabicFont

/**
 * The "how the text looks" half of a reader's settings screen: Arabic size, Arabic font, and
 * translation size.
 *
 * `DuaSettingsScreen` and `HadithSettingsScreen` had this identically, twice (#488) — same
 * three controls, same ranges, same strings, differing only in which preference each wrote.
 * The two screens keep everything that makes them different: the live preview at the top and
 * their own display toggles at the bottom, including the grade badge and chain of narration
 * that only hadith has.
 *
 * `QuranSettingsScreen` has the same three controls but interleaved with its own script and
 * translator pickers, so it is deliberately **not** a caller: making it one would mean
 * reordering that screen to suit this function, which is the tail wagging the dog.
 *
 * A `LazyListScope` extension rather than a composable because the sections are separate list
 * items — folding them into one item would make the whole block a single scroll unit and
 * change how the list recycles.
 */
fun LazyListScope.readerTypographySettings(
    arabicFontSize: Float,
    onArabicFontSize: (Float) -> Unit,
    selectedFont: QuranArabicFont,
    onArabicFont: (String) -> Unit,
    translationFontSize: Float,
    onTranslationFontSize: (Float) -> Unit,
) {
    item { NimazSectionHeader(title = stringResource(R.string.arabic_text)) }
    item {
        _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.NimazMenuGroup {
            _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.NimazSettingsSlider(
                title = stringResource(R.string.arabic_font_size),
                valueLabel = stringResource(
                    R.string.arabic_font_size_value,
                    arabicFontSize.toInt()
                ),
                value = arabicFontSize,
                onValueChange = onArabicFontSize,
                valueRange = ARABIC_RANGE,
                contentDescription = stringResource(R.string.arabic_font_size)
            )

            NimazDivider(modifier = Modifier.padding(horizontal = 16.dp))

            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp)
            ) {
                _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.NimazDropdownField(
                    label = stringResource(R.string.arabic_font),
                    items = QuranArabicFont.entries.map { font ->
                        _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.NimazDropdownItem(
                            value = font.id,
                            label = font.displayName,
                            textFontFamily = font.fontFamily,
                        )
                    },
                    selected = selectedFont.id,
                    onSelected = onArabicFont
                )
            }
        }
    }

    item { NimazSectionHeader(title = stringResource(R.string.translation)) }
    item {
        _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.NimazMenuGroup {
            _root_ide_package_.com.arshadshah.nimaz.presentation.components.molecules.NimazSettingsSlider(
                title = stringResource(R.string.translation_font_size),
                valueLabel = stringResource(
                    R.string.arabic_font_size_value,
                    translationFontSize.toInt()
                ),
                value = translationFontSize,
                onValueChange = onTranslationFontSize,
                valueRange = TRANSLATION_RANGE,
                contentDescription = stringResource(R.string.translation_font_size)
            )
        }
    }
}

/** Both readers used these, and a reader whose Arabic can shrink further than another's is a bug. */
private val ARABIC_RANGE = 18f..42f
private val TRANSLATION_RANGE = 12f..28f
