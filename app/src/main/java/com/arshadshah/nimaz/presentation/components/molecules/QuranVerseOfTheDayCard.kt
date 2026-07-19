package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * "Verse of the Day" — a deterministic daily ayah, tappable to open in the reader.
 *
 * Deliberately *not* a hero: it sits near the bottom of the Quran home tab and uses the
 * standard elevated card treatment. The teal gradient on that screen is reserved for
 * continue-reading alone, so this card previews the ayah (Arabic clipped to two lines,
 * translation to two) and offers a read affordance rather than rendering the whole verse.
 */
@Composable
internal fun VerseOfTheDayCard(
    arabicText: String,
    translation: String?,
    reference: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NimazCard(
        style = NimazCardStyle.ELEVATED,
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        tone = NimazTone.NEUTRAL
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NimazIcon(
                    imageVector = Icons.Default.AutoStories,
                    contentDescription = null,
                    variant = NimazIconVariant.PRIMARY,
                    size = NimazIconSize.SMALL
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.quran_home_verse_of_the_day),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            ArabicText(
                text = arabicText,
                size = ArabicTextSize.SMALL,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            if (!translation.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = translation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = reference,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.quran_home_read_verse),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    NimazIcon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        variant = NimazIconVariant.PRIMARY,
                        size = NimazIconSize.SMALL
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun VerseOfTheDayCardPreview() {
    NimazTheme {
        VerseOfTheDayCard(
            arabicText = "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ",
            translation = "Allah - there is no deity except Him, the Ever-Living, the Sustainer of existence.",
            reference = "Al-Baqarah · 2:255",
            onClick = {}
        )
    }
}
