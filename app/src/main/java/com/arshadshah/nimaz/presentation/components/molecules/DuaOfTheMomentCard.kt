package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.IconBadge
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * "Dua of the moment" card: a time-of-day appropriate supplication (morning /
 * evening / before-sleep adhkar). Shows the category badge, Arabic text, and
 * its translation. Used as a page in the home `TodayCarousel` and in the
 * tablet `TodayInfoCards` stack.
 *
 * In the carousel ([fillHeight] = true) the Arabic and translation are capped
 * so every page is the same height; standalone usage lets the text expand.
 */
@Composable
fun DuaOfTheMomentCard(
    arabic: String,
    translation: String,
    categoryLabel: String,
    modifier: Modifier = Modifier,
    source: String? = null,
    fillHeight: Boolean = false,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (fillHeight) Modifier.fillMaxHeight() else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (fillHeight) Modifier.fillMaxHeight() else Modifier.wrapContentHeight())
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(
                    imageVector = iconForCategory(categoryLabel),
                    backgroundColor = DuaAccent.copy(alpha = 0.2f),
                    iconColor = DuaAccent,
                    containerSize = 32.dp,
                    iconSize = 18.dp,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.dua_of_the_moment),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = categoryLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = DuaAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body region expands to fill the page in carousel mode; the Arabic
            // and translation are centred within that free space so a short dua
            // doesn't leave a gap at the bottom.
            Column(
                modifier = if (fillHeight) {
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                } else {
                    Modifier.fillMaxWidth()
                },
                verticalArrangement = if (fillHeight) Arrangement.Center else Arrangement.Top
            ) {
                ArabicText(
                    text = arabic,
                    size = if (fillHeight) ArabicTextSize.MEDIUM else ArabicTextSize.SMALL,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (fillHeight) 2 else Int.MAX_VALUE,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = translation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                    maxLines = if (fillHeight) 2 else Int.MAX_VALUE,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Source footer (e.g. "Surah Ta-Ha 20:114") anchored to the bottom
            // behind a hairline divider — fills the card and adds attribution.
            if (!source.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = source,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = DuaAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private val DuaAccent = Color(0xFF14B8A6)

/**
 * Picks a time-of-day icon for the dua's adhkar category (replacing the old
 * emoji glyph — issue #161). Matches on the display label so the card stays
 * decoupled from the dua-category ids in the ViewModel.
 */
private fun iconForCategory(categoryLabel: String): ImageVector {
    val label = categoryLabel.lowercase()
    return when {
        "morning" in label -> Icons.Filled.WbSunny
        "evening" in label -> Icons.Filled.WbTwilight
        "sleep" in label || "night" in label -> Icons.Filled.Bedtime
        else -> Icons.Filled.AutoAwesome
    }
}

private const val SAMPLE_ARABIC =
    "اللَّهُمَّ بِكَ أَصْبَحْنَا وَبِكَ أَمْسَيْنَا وَبِكَ نَحْيَا وَبِكَ نَمُوتُ وَإِلَيْكَ النُّشُورُ"
private const val SAMPLE_TRANSLATION =
    "O Allah, by You we enter the morning and by You we enter the evening, by You we live and by You we die, and to You is the resurrection."

@Preview(showBackground = true, widthDp = 400, name = "Standalone")
@Composable
private fun DuaOfTheMomentCard_Preview() {
    NimazTheme {
        DuaOfTheMomentCard(
            arabic = SAMPLE_ARABIC,
            translation = SAMPLE_TRANSLATION,
            categoryLabel = "Morning Adhkar",
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 200, name = "Carousel (capped)")
@Composable
private fun DuaOfTheMomentCard_Carousel_Preview() {
    NimazTheme {
        DuaOfTheMomentCard(
            arabic = SAMPLE_ARABIC,
            translation = SAMPLE_TRANSLATION,
            categoryLabel = "Morning Adhkar",
            source = "Sahih Muslim 2723",
            fillHeight = true,
            modifier = Modifier.padding(16.dp)
        )
    }
}
