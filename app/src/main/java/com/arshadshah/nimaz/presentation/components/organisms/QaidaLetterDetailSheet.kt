package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.ui.res.stringResource
import com.arshadshah.nimaz.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.domain.model.MakhrajArea
import com.arshadshah.nimaz.domain.model.QaidaLetter
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.molecules.QAIDA_AUDIO_UI_ENABLED
import com.arshadshah.nimaz.presentation.components.molecules.QaidaLetterForms
import com.arshadshah.nimaz.presentation.components.molecules.QaidaMakhrajHelper
import com.arshadshah.nimaz.presentation.theme.NimazCornerRadius
import com.arshadshah.nimaz.presentation.theme.NimazSpacing
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * The letter detail shown inside a bottom sheet: a hero (big letter, name,
 * phonetic hint, play button), the positional shapes, and the makhraj helper.
 */
@Composable
fun QaidaLetterDetailSheet(
    letter: QaidaLetter,
    onPlay: (QaidaLetter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = NimazSpacing.Large)
            .padding(bottom = NimazSpacing.ExtraLarge),
        verticalArrangement = Arrangement.spacedBy(NimazSpacing.Large),
    ) {
        // Hero
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NimazSpacing.Medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(84.dp),
                shape = RoundedCornerShape(NimazCornerRadius.Large),
                color = MaterialTheme.colorScheme.primary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    ArabicText(
                        text = letter.letterArabic,
                        size = ArabicTextSize.EXTRA_LARGE,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                ArabicText(
                    text = letter.nameArabic,
                    size = ArabicTextSize.MEDIUM,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = letter.nameTransliteration,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                letter.phoneticHint?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            // Hidden while audio is being regenerated (text-only mode).
            if (QAIDA_AUDIO_UI_ENABLED) {
                val playLetterCd = stringResource(R.string.qaida_play_letter)
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(MaterialTheme.colorScheme.secondary)
                        .semantics { contentDescription = playLetterCd }
                        .clickable { onPlay(letter) },
                    contentAlignment = Alignment.Center,
                ) {
                    NimazIcon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondary,
                        iconSize = 28.dp,
                    )
                }
            }
        }

        SectionLabel(stringResource(R.string.qaida_its_shapes))
        QaidaLetterForms(letter)

        SectionLabel(stringResource(R.string.qaida_where_made))
        QaidaMakhrajHelper(area = letter.makhrajArea, detail = letter.makhrajDetail)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}


// ==================== PREVIEWS ====================

private fun sampleDetailLetter() = QaidaLetter(
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
    makhrajDetail = "Pressing the lips together gently.",
    phoneticHint = "like 'b' in 'book'",
    audioKey = "ba",
    audioPath = "",
    displayOrder = 2,
)

@Composable
private fun QaidaLetterDetailSheetShowcase() {
    QaidaLetterDetailSheet(
        letter = sampleDetailLetter(),
        onPlay = {},
        modifier = Modifier.padding(top = 16.dp),
    )
}

@Preview(showBackground = true, name = "Qaida Letter Detail Sheet — Light")
@Composable
private fun QaidaLetterDetailSheetLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        QaidaLetterDetailSheetShowcase()
    }
}

@Preview(showBackground = true, name = "Qaida Letter Detail Sheet — Dark")
@Composable
private fun QaidaLetterDetailSheetDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        QaidaLetterDetailSheetShowcase()
    }
}
