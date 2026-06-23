package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.theme.NimazSpacing
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * Shared "Refined Row" card for all three names screens. Accent is a parameter.
 * Prophets pass [titleLabel] + [eraChip] for the story variant.
 */
@Composable
fun NameCard(
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
        style = NimazCardStyle.ELEVATED,
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left accent rail
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accent.rail)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(NimazSpacing.Medium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gradient medallion
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(accent.medallion)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$number",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = accent.onMedallion,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.width(NimazSpacing.Medium))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    ArabicText(
                        text = arabicName,
                        size = ArabicTextSize.MEDIUM,
                        color = accent.contentTint,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = primaryLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = secondaryLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (titleLabel != null) {
                        Text(
                            text = titleLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = accent.contentTint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (eraChip != null) {
                        Surface(
                            shape = RoundedCornerShape(NimazSpacing.Small),
                            color = accent.chipContainer,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = eraChip,
                                style = MaterialTheme.typography.labelSmall,
                                color = accent.onChipContainer,
                                modifier = Modifier.padding(
                                    horizontal = NimazSpacing.Small,
                                    vertical = 2.dp
                                )
                            )
                        }
                    }
                }

                IconButton(onClick = onFavoriteClick) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (isFavorite) {
                            stringResource(R.string.remove_from_favorites)
                        } else {
                            stringResource(R.string.add_to_favorites)
                        },
                        tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}


// ==================== PREVIEWS ====================

@Composable
private fun NameCardShowcase() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Asma ul Husna variant (favorited)
        NameCard(
            number = 1,
            arabicName = "الرَّحْمَٰن",
            primaryLabel = "Ar-Rahman",
            secondaryLabel = "The Most Compassionate",
            isFavorite = true,
            accent = NamesAccents.allah(),
            onClick = {},
            onFavoriteClick = {},
        )
        // Asma un Nabi variant (not favorited)
        NameCard(
            number = 12,
            arabicName = "الْمُصْطَفَىٰ",
            primaryLabel = "Al-Mustafa",
            secondaryLabel = "The Chosen One",
            isFavorite = false,
            accent = NamesAccents.prophetNames(),
            onClick = {},
            onFavoriteClick = {},
        )
        // Prophets story variant (titleLabel + eraChip)
        NameCard(
            number = 3,
            arabicName = "إِبْرَاهِيم",
            primaryLabel = "Ibrahim",
            secondaryLabel = "Abraham",
            isFavorite = false,
            accent = NamesAccents.prophets(),
            onClick = {},
            onFavoriteClick = {},
            titleLabel = "Khalilullah — Friend of Allah",
            eraChip = "Prophet",
        )
    }
}

@Preview(showBackground = true, name = "NameCard — Light")
@Composable
private fun NameCardLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        NameCardShowcase()
    }
}

@Preview(showBackground = true, name = "NameCard — Dark")
@Composable
private fun NameCardDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        NameCardShowcase()
    }
}
