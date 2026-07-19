package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.MakhrajArea
import com.arshadshah.nimaz.domain.model.QaidaLetter
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.theme.NimazCornerRadius
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * One letter on the alphabet board. A square card with the big Arabic letter;
 * a gold star in the corner marks letters the child has already heard.
 */
@Composable
fun QaidaLetterTile(
    letter: QaidaLetter,
    heard: Boolean,
    onClick: (QaidaLetter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val desc = if (heard) {
        stringResource(R.string.qaida_a11y_letter_heard_format, letter.nameTransliteration)
    } else {
        stringResource(R.string.qaida_a11y_letter_format, letter.nameTransliteration)
    }
    NimazCard(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .semantics { contentDescription = desc }
            .clickable { onClick(letter) },
        style = NimazCardStyle.ELEVATED,
        tone = NimazTone.NEUTRAL,
        shape = RoundedCornerShape(NimazCornerRadius.Large),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            ArabicText(
                text = letter.letterArabic,
                size = ArabicTextSize.EXTRA_LARGE,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (heard) {
                NimazIcon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    iconSize = 14.dp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp),
                )
            }
        }
    }
}


// ==================== PREVIEWS ====================

private fun sampleTileLetter(
    id: Int = 2,
    letterArabic: String = "ب",
    name: String = "Ba",
) = QaidaLetter(
    id = id,
    letterArabic = letterArabic,
    nameArabic = "باء",
    nameTransliteration = name,
    isolatedForm = letterArabic,
    initialForm = null,
    medialForm = null,
    finalForm = null,
    isConnecting = true,
    makhrajArea = MakhrajArea.SHAFATAIN,
    makhrajDetail = "From the lips",
    phoneticHint = null,
    audioKey = name.lowercase(),
    audioPath = "",
    displayOrder = id,
)

@Composable
private fun QaidaLetterTileShowcase() {
    Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        QaidaLetterTile(
            letter = sampleTileLetter(id = 1, letterArabic = "ا", name = "Alif"),
            heard = false,
            onClick = {},
            modifier = Modifier.width(88.dp),
        )
        QaidaLetterTile(
            letter = sampleTileLetter(id = 2, letterArabic = "ب", name = "Ba"),
            heard = true,
            onClick = {},
            modifier = Modifier.width(88.dp),
        )
    }
}

@Preview(showBackground = true, name = "Qaida Letter Tile — Light")
@Composable
private fun QaidaLetterTileLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        QaidaLetterTileShowcase()
    }
}

@Preview(showBackground = true, name = "Qaida Letter Tile — Dark")
@Composable
private fun QaidaLetterTileDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        QaidaLetterTileShowcase()
    }
}
