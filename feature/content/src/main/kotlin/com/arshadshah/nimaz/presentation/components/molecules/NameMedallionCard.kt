package com.arshadshah.nimaz.presentation.components.molecules

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconButtonSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode
import androidx.compose.ui.res.stringResource

/**
 * A name as a medallion, for the two-column catalogue grid.
 *
 * **The Arabic leads.** On the row card this replaced, the Arabic was the smallest element on a
 * card that exists to show it — wedged between the English meaning and a favourite heart, at
 * roughly the transliteration's size. Here it is the largest thing in the cell and it is centred,
 * with the transliteration and meaning beneath as support.
 *
 * The ordinal is an outlined ring rather than a filled badge: ninety-nine of them down a screen
 * is a lot of accent, and the number is a locator, not a fact about the name.
 *
 * @param titleLabel the Prophets tab's extra line ("Messenger of Allah"), shown under the meaning.
 * @param eraChip the Prophets tab's era. Rendered as a badge when present.
 */
@Composable
fun NameMedallionCard(
    number: Int,
    arabicName: String,
    primaryLabel: String,
    secondaryLabel: String,
    isFavorite: Boolean,
    accent: NamesAccent,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier,
    titleLabel: String? = null,
    eraChip: String? = null,
) {
    NimazCard(
        modifier = modifier.fillMaxWidth(),
        style = NimazCardStyle.FILLED,
        // MUTED, not NEUTRAL: at BASE, NEUTRAL resolves to `colorScheme.surface` — the screen's
        // own background — and the card disappears.
        tone = NimazTone.MUTED,
        shape = RoundedCornerShape(20.dp),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    NimazBadge(
                        text = number.toString(),
                        size = NimazBadgeSize.SMALL,
                    )
                }
                NimazIconButton(
                    icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    onClick = onFavoriteClick,
                    contentDescription = stringResource(
                        if (isFavorite) R.string.remove_from_favorites else R.string.add_to_favorites
                    ),
                    size = NimazIconButtonSize.SMALL,
                )
            }

            ArabicText(
                text = arabicName,
                size = ArabicTextSize.MEDIUM,
                color = accent.contentTint,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = primaryLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = secondaryLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            if (titleLabel != null) {
                Text(
                    text = titleLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = accent.contentTint,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (eraChip != null) {
                NimazBadge(
                    text = eraChip,
                    size = NimazBadgeSize.SMALL,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

// ==================== PREVIEWS ====================

@Composable
private fun NameMedallionCardShowcase() {
    val accent = NamesAccents.allah()
    Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        NameMedallionCard(
            number = 1,
            arabicName = "الرَّحْمَٰن",
            primaryLabel = "Ar-Rahman",
            secondaryLabel = "The Most Compassionate",
            isFavorite = true,
            accent = accent,
            onClick = {},
            onFavoriteClick = {},
            modifier = Modifier.weight(1f),
        )
        NameMedallionCard(
            number = 2,
            arabicName = "الرَّحِيم",
            primaryLabel = "Ar-Raheem",
            secondaryLabel = "The Most Merciful",
            isFavorite = false,
            accent = accent,
            onClick = {},
            onFavoriteClick = {},
            modifier = Modifier.weight(1f),
        )
    }
}

@Preview(showBackground = true, widthDp = 380, name = "NameMedallionCard — Light")
@Composable
private fun NameMedallionCardLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { NameMedallionCardShowcase() }
}

@Preview(
    showBackground = true, widthDp = 380, name = "NameMedallionCard — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
)
@Composable
private fun NameMedallionCardDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) { NameMedallionCardShowcase() }
}
