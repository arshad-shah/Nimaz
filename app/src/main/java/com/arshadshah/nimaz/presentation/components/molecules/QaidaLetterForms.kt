package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.domain.model.MakhrajArea
import com.arshadshah.nimaz.domain.model.QaidaLetter
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazSurfaceCard
import com.arshadshah.nimaz.presentation.theme.NimazCornerRadius
import com.arshadshah.nimaz.presentation.theme.NimazSpacing
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * The letter's positional shapes. Connecting letters show all four
 * (start / middle / end / alone); non-connecting letters show just the
 * isolated form. Falls back gracefully when a form is null.
 */
@Composable
fun QaidaLetterForms(
    letter: QaidaLetter,
    modifier: Modifier = Modifier,
) {
    val forms: List<Pair<String, String>> = if (letter.isConnecting) {
        listOf(
            (letter.initialForm ?: letter.isolatedForm) to "start",
            (letter.medialForm ?: letter.isolatedForm) to "middle",
            (letter.finalForm ?: letter.isolatedForm) to "end",
            letter.isolatedForm to "alone",
        )
    } else {
        listOf(letter.isolatedForm to "alone")
    }

    val single = forms.size == 1
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(NimazSpacing.Small),
    ) {
        forms.forEach { (glyph, label) ->
            NimazSurfaceCard(
                modifier = if (single) Modifier.width(96.dp) else Modifier.weight(1f),
                shape = RoundedCornerShape(NimazCornerRadius.Medium),
            ) {
                Column(
                    modifier = Modifier.padding(vertical = NimazSpacing.Small),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ArabicText(
                        text = glyph,
                        size = ArabicTextSize.LARGE,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}


// ==================== PREVIEWS ====================

private fun sampleConnectingLetter() = QaidaLetter(
    id = 2,
    letterArabic = "ب",
    nameArabic = "باء",
    nameTransliteration = "Ba",
    isolatedForm = "ب",
    initialForm = "بـ",
    medialForm = "ـبـ",
    finalForm = "ـب",
    isConnecting = true,
    makhrajArea = MakhrajArea.SHAFATAIN,
    makhrajDetail = "From the lips",
    phoneticHint = "like 'b' in 'book'",
    audioKey = "ba",
    audioPath = "",
    displayOrder = 2,
)

private fun sampleNonConnectingLetter() = QaidaLetter(
    id = 1,
    letterArabic = "ا",
    nameArabic = "ألف",
    nameTransliteration = "Alif",
    isolatedForm = "ا",
    initialForm = null,
    medialForm = null,
    finalForm = "ـا",
    isConnecting = false,
    makhrajArea = MakhrajArea.JAWF,
    makhrajDetail = "From the empty space of the mouth",
    phoneticHint = "like 'a' in 'father'",
    audioKey = "alif",
    audioPath = "",
    displayOrder = 1,
)

@Composable
private fun QaidaLetterFormsShowcase() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        QaidaLetterForms(letter = sampleConnectingLetter())
        QaidaLetterForms(letter = sampleNonConnectingLetter())
    }
}

@Preview(showBackground = true, name = "Qaida Letter Forms — Light")
@Composable
private fun QaidaLetterFormsLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        QaidaLetterFormsShowcase()
    }
}

@Preview(showBackground = true, name = "Qaida Letter Forms — Dark")
@Composable
private fun QaidaLetterFormsDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        QaidaLetterFormsShowcase()
    }
}
