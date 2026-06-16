package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    categoryIcon: String,
    modifier: Modifier = Modifier,
    fillHeight: Boolean = false,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (fillHeight) Modifier.fillMaxHeight() else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (fillHeight) Modifier.fillMaxHeight() else Modifier.wrapContentHeight())
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DuaAccent.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = categoryIcon.ifBlank { "🤲" },
                        fontSize = 16.sp
                    )
                }
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

            // Body region expands to fill the page in carousel mode.
            Column(
                modifier = if (fillHeight) {
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                } else {
                    Modifier.fillMaxWidth()
                }
            ) {
                ArabicText(
                    text = arabic,
                    size = ArabicTextSize.SMALL,
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
                    maxLines = if (fillHeight) 2 else Int.MAX_VALUE,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private val DuaAccent = Color(0xFF14B8A6)

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
            categoryIcon = "🌅",
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
            categoryIcon = "🌅",
            fillHeight = true,
            modifier = Modifier.padding(16.dp)
        )
    }
}
