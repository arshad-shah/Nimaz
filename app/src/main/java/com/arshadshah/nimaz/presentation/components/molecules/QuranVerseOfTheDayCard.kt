package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * Signature "Verse of the Day" hero card. Shows a deterministic daily ayah with its
 * Arabic text, translation and reference. Tapping opens the ayah in the reader.
 *
 * Shares the teal/gold brand palette of [ContinueReadingCard] to stay on-language,
 * but is visually distinct through its centred Arabic-first layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VerseOfTheDayCard(
    arabicText: String,
    translation: String?,
    reference: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NimazCard(
        style = NimazCardStyle.FILLED,
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        tone = NimazTone.TRANSPARENT
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(NimazColors.QuranColors.BannerGradient),
                    shape = RoundedCornerShape(20.dp)
                )
                .border(
                    width = 1.dp,
                    color = NimazColors.QuranColors.BannerBorder,
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NimazIcon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = null,
                        tint = NimazColors.QuranColors.BannerAccent,
                        size = NimazIconSize.SMALL
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = stringResource(R.string.quran_home_verse_of_the_day),
                        style = MaterialTheme.typography.labelSmall,
                        color = NimazColors.Primary400,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                ArabicText(
                    text = arabicText,
                    size = ArabicTextSize.LARGE,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                )

                if (!translation.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = translation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = NimazColors.Gray300,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = reference,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = NimazColors.QuranColors.BannerAccent
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun VerseOfTheDayCardPreview() {
    NimazTheme {
        VerseOfTheDayCard(
            arabicText = "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ",
            translation = "Allah - there is no deity except Him, the Ever-Living, the Sustainer of existence.",
            reference = "Al-Baqarah · 2:255",
            onClick = {}
        )
    }
}
