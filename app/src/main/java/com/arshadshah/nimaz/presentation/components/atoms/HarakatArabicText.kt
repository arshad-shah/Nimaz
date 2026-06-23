package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import com.arshadshah.nimaz.presentation.theme.AmiriFontFamily
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/** Arabic combining marks (harakat) we tint: tanween/fatha/damma/kasra/shadda/sukoon + superscript alef. */
private fun Char.isHarakah(): Boolean = this.code in 0x064B..0x0652 || this.code == 0x0670

/**
 * Renders an Arabic cell where the harakat (vowel marks) are tinted by their
 * [highlightGroup] so children can see the vowel at a glance:
 *  - fatha / damma family  → teal (theme `primary`)
 *  - kasra family          → gold (theme `secondary`)
 * Base letters keep [baseColor]. When [playing], the whole glyph is drawn in
 * [playingColor] (the tile supplies the teal background behind it).
 */
@Composable
fun HarakatArabicText(
    text: String,
    highlightGroup: String?,
    modifier: Modifier = Modifier,
    size: ArabicTextSize = ArabicTextSize.EXTRA_LARGE,
    baseColor: Color = MaterialTheme.colorScheme.onSurface,
    playing: Boolean = false,
    playingColor: Color = MaterialTheme.colorScheme.onPrimary,
) {
    val fathaColor = MaterialTheme.colorScheme.primary
    val kasraColor = MaterialTheme.colorScheme.secondary

    val harakatColor = when {
        playing -> playingColor
        highlightGroup == null -> baseColor
        highlightGroup.contains("kasra", ignoreCase = true) -> kasraColor
        highlightGroup.contains("fatha", ignoreCase = true) ||
                highlightGroup.contains("damma", ignoreCase = true) -> fathaColor

        else -> baseColor
    }
    val letterColor = if (playing) playingColor else baseColor

    val annotated = buildAnnotatedString {
        text.forEach { ch ->
            val color = if (ch.isHarakah()) harakatColor else letterColor
            withStyle(SpanStyle(color = color)) { append(ch) }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Text(
            text = annotated,
            modifier = modifier,
            style = TextStyle(
                fontFamily = AmiriFontFamily,
                fontSize = size.fontSize,
                lineHeight = size.lineHeight,
                textAlign = TextAlign.Center,
                textDirection = TextDirection.Rtl,
            ),
        )
    }
}


// ==================== PREVIEWS ====================

/**
 * Shows the same Arabic word with each [highlightGroup] so the fatha/damma (teal)
 * vs. kasra (gold) tinting is visible, plus the [playing] state.
 */
@Composable
private fun HarakatArabicTextShowcase() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HarakatArabicText(text = "بَ", highlightGroup = "fatha")
        HarakatArabicText(text = "بُ", highlightGroup = "damma")
        HarakatArabicText(text = "بِ", highlightGroup = "kasra")
        HarakatArabicText(text = "بَا", highlightGroup = null)
        HarakatArabicText(text = "بَ", highlightGroup = "fatha", playing = true)
    }
}

@Preview(showBackground = true, name = "Harakat Arabic Text — Light")
@Composable
private fun HarakatArabicTextLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        HarakatArabicTextShowcase()
    }
}

@Preview(showBackground = true, name = "Harakat Arabic Text — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun HarakatArabicTextDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        HarakatArabicTextShowcase()
    }
}
