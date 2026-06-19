package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.domain.model.QaidaLetter
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.theme.NimazCornerRadius
import com.arshadshah.nimaz.presentation.theme.NimazSpacing

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
            Surface(
                modifier = if (single) Modifier.width(96.dp) else Modifier.weight(1f),
                shape = RoundedCornerShape(NimazCornerRadius.Medium),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 1.dp,
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
