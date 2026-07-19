package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazPalette
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * A slim tooltip with a triangular beak/arrow that anchors near the tapped ayah.
 *
 * The tooltip shows a single row of action icons. It automatically positions
 * itself above or below the tap point depending on available space, and the
 * beak points toward the tap location.
 *
 * @param tapY The Y coordinate of the tap within the parent (used for vertical positioning)
 * @param parentHeight The height of the parent container
 * @param onDismiss Dismiss the tooltip
 * @param isBookmarked Current bookmark state
 * @param isFavorite Current favorite state
 * @param isKhatamActive Whether khatam mode is on
 * @param isKhatamRead Whether this ayah is khatam-read
 * @param showTranslationButton Whether to show the translation button
 * @param onPlayClick Play audio
 * @param onBookmarkClick Toggle bookmark
 * @param onFavoriteClick Toggle favorite
 * @param onCopyClick Copy ayah
 * @param onShareClick Share ayah
 * @param onTafseerClick Open tafseer
 * @param onKhatamToggle Toggle khatam read
 * @param onTranslationClick Open translation sheet
 */
@Composable
fun AyahTooltip(
    tapY: Float,
    parentHeight: Float,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isBookmarked: Boolean = false,
    isFavorite: Boolean = false,
    isKhatamActive: Boolean = false,
    isKhatamRead: Boolean = false,
    showTranslationButton: Boolean = true,
    onPlayClick: () -> Unit = {},
    onBookmarkClick: () -> Unit = {},
    onFavoriteClick: () -> Unit = {},
    onCopyClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onTafseerClick: () -> Unit = {},
    onKhatamToggle: () -> Unit = {},
    onTranslationClick: () -> Unit = {}
) {
    val density = LocalDensity.current

    val tooltipHeightDp = 52.dp
    val beakHeightDp = 10.dp
    val verticalGapDp = 6.dp
    val totalHeightDp = tooltipHeightDp + beakHeightDp + verticalGapDp

    val tooltipHeightPx = with(density) { tooltipHeightDp.toPx() }
    val beakHeightPx = with(density) { beakHeightDp.toPx() }
    val verticalGapPx = with(density) { verticalGapDp.toPx() }
    val totalHeightPx = tooltipHeightPx + beakHeightPx + verticalGapPx

    // Decide if tooltip goes above or below the tap point
    val showAbove = tapY > totalHeightPx + 40f

    // Animate alpha for smooth appear
    val animatedAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "tooltip_alpha"
    )

    val surfaceColor = MaterialTheme.colorScheme.inverseSurface
    val contentColor = MaterialTheme.colorScheme.inverseOnSurface

    // Scrim (transparent, just captures taps to dismiss)
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() }
    ) {
        // Tooltip positioned relative to tap Y
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .alpha(animatedAlpha)
                .then(
                    with(density) {
                        if (showAbove) {
                            Modifier.padding(
                                top = ((tapY - totalHeightPx).coerceAtLeast(8f)).toDp()
                            )
                        } else {
                            Modifier.padding(
                                top = (tapY + verticalGapPx).coerceAtMost(
                                    parentHeight - totalHeightPx - 8f
                                ).coerceAtLeast(8f).toDp()
                            )
                        }
                    }
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            // Beak + card layout
            if (showAbove) {
                // Card on top, beak pointing down
                Box(contentAlignment = Alignment.TopCenter) {
                    TooltipCard(
                        surfaceColor = surfaceColor,
                        contentColor = contentColor,
                        isBookmarked = isBookmarked,
                        isFavorite = isFavorite,
                        isKhatamActive = isKhatamActive,
                        isKhatamRead = isKhatamRead,
                        showTranslationButton = showTranslationButton,
                        onPlayClick = onPlayClick,
                        onBookmarkClick = onBookmarkClick,
                        onFavoriteClick = onFavoriteClick,
                        onCopyClick = onCopyClick,
                        onShareClick = onShareClick,
                        onTafseerClick = onTafseerClick,
                        onKhatamToggle = onKhatamToggle,
                        onTranslationClick = onTranslationClick
                    )

                    // Beak at the bottom of the card
                    Box(
                        modifier = Modifier
                            .padding(top = tooltipHeightDp)
                            .size(width = 20.dp, height = beakHeightDp)
                            .drawBehind {
                                val path = Path().apply {
                                    moveTo(0f, 0f)
                                    lineTo(size.width, 0f)
                                    lineTo(size.width / 2f, size.height)
                                    close()
                                }
                                drawPath(path, surfaceColor)
                            }
                    )
                }
            } else {
                // Beak pointing up, card below
                Box(contentAlignment = Alignment.TopCenter) {
                    // Beak at the top
                    Box(
                        modifier = Modifier
                            .size(width = 20.dp, height = beakHeightDp)
                            .drawBehind {
                                val path = Path().apply {
                                    moveTo(size.width / 2f, 0f)
                                    lineTo(size.width, size.height)
                                    lineTo(0f, size.height)
                                    close()
                                }
                                drawPath(path, surfaceColor)
                            }
                    )

                    Box(modifier = Modifier.padding(top = beakHeightDp)) {
                        TooltipCard(
                            surfaceColor = surfaceColor,
                            contentColor = contentColor,
                            isBookmarked = isBookmarked,
                            isFavorite = isFavorite,
                            isKhatamActive = isKhatamActive,
                            isKhatamRead = isKhatamRead,
                            showTranslationButton = showTranslationButton,
                            onPlayClick = onPlayClick,
                            onBookmarkClick = onBookmarkClick,
                            onFavoriteClick = onFavoriteClick,
                            onCopyClick = onCopyClick,
                            onShareClick = onShareClick,
                            onTafseerClick = onTafseerClick,
                            onKhatamToggle = onKhatamToggle,
                            onTranslationClick = onTranslationClick
                        )
                    }
                }
            }
        }
    }
}

/**
 * The slim tooltip card containing the row of action icons.
 */
@Composable
private fun TooltipCard(
    surfaceColor: Color,
    contentColor: Color,
    isBookmarked: Boolean,
    isFavorite: Boolean,
    isKhatamActive: Boolean,
    isKhatamRead: Boolean,
    showTranslationButton: Boolean,
    onPlayClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onCopyClick: () -> Unit,
    onShareClick: () -> Unit,
    onTafseerClick: () -> Unit,
    onKhatamToggle: () -> Unit,
    onTranslationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { /* consume */ },
        shape = RoundedCornerShape(16.dp),
        color = surfaceColor,
        shadowElevation = 8.dp,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TooltipIconButton(
                icon = Icons.Default.PlayArrow,
                contentDescription = stringResource(R.string.cd_play),
                tint = contentColor,
                onClick = onPlayClick
            )

            TooltipIconButton(
                icon = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                contentDescription = stringResource(R.string.cd_bookmark),
                tint = if (isBookmarked) NimazColors.QuranColors.BookmarkPrimary else contentColor,
                onClick = onBookmarkClick
            )

            TooltipIconButton(
                icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = stringResource(R.string.cd_favorite),
                tint = if (isFavorite) NimazPalette.Red500 else contentColor,
                onClick = onFavoriteClick
            )

            TooltipIconButton(
                icon = Icons.Default.ContentCopy,
                contentDescription = stringResource(R.string.cd_copy),
                tint = contentColor,
                onClick = onCopyClick
            )

            TooltipIconButton(
                icon = Icons.Default.Share,
                contentDescription = stringResource(R.string.cd_share),
                tint = contentColor,
                onClick = onShareClick
            )

            TooltipIconButton(
                icon = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = stringResource(R.string.cd_tafseer),
                tint = contentColor,
                onClick = onTafseerClick
            )

            if (isKhatamActive) {
                TooltipIconButton(
                    icon = if (isKhatamRead) Icons.Filled.CheckCircle
                    else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = if (isKhatamRead) stringResource(R.string.cd_mark_as_unread)
                    else stringResource(R.string.cd_mark_as_read),
                    tint = if (isKhatamRead) NimazColors.Success else contentColor,
                    onClick = onKhatamToggle
                )
            }

            if (showTranslationButton) {
                // Subtle separator
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .size(width = 1.dp, height = 24.dp)
                        .background(contentColor.copy(alpha = 0.2f))
                )

                TooltipIconButton(
                    icon = Icons.Default.Translate,
                    contentDescription = stringResource(R.string.cd_translation),
                    tint = contentColor,
                    onClick = onTranslationClick
                )
            }
        }
    }
}

@Composable
private fun TooltipIconButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Transparent,
        modifier = modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            NimazIcon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                size = NimazIconSize.MEDIUM
            )
        }
    }
}

// ==================== PREVIEWS ====================

@Preview(showBackground = true, widthDp = 400, heightDp = 600, name = "Tooltip - Below tap")
@Composable
private fun AyahTooltipBelowPreview() {
    NimazTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            AyahTooltip(
                tapY = 100f,
                parentHeight = 1200f,
                onDismiss = {}
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 600, name = "Tooltip - Above tap")
@Composable
private fun AyahTooltipAbovePreview() {
    NimazTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            AyahTooltip(
                tapY = 500f,
                parentHeight = 600f,
                onDismiss = {}
            )
        }
    }
}

@Preview(
    showBackground = true,
    widthDp = 400,
    heightDp = 600,
    name = "Tooltip - Bookmarked + Favorited"
)
@Composable
private fun AyahTooltipBookmarkedPreview() {
    NimazTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            AyahTooltip(
                tapY = 300f,
                parentHeight = 1200f,
                isBookmarked = true,
                isFavorite = true,
                onDismiss = {}
            )
        }
    }
}

@Preview(
    showBackground = true,
    widthDp = 400,
    heightDp = 600,
    name = "Tooltip - With Khatam"
)
@Composable
private fun AyahTooltipKhatamPreview() {
    NimazTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            AyahTooltip(
                tapY = 300f,
                parentHeight = 1200f,
                isKhatamActive = true,
                isKhatamRead = true,
                onDismiss = {}
            )
        }
    }
}

@Preview(
    showBackground = true,
    widthDp = 400,
    heightDp = 600,
    name = "Tooltip - No Translation"
)
@Composable
private fun AyahTooltipNoTranslationPreview() {
    NimazTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            AyahTooltip(
                tapY = 300f,
                parentHeight = 1200f,
                showTranslationButton = false,
                onDismiss = {}
            )
        }
    }
}