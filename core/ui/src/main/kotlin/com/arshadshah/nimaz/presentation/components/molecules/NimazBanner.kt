package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardColors
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardDefaults
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconButtonSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconWell
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconWellSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcons
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme

enum class NimazBannerVariant { INFO, WARNING, ERROR, UPDATE, EVENT }

enum class NimazBannerDensity { STANDALONE, INLINE }

/** Per-variant colour palette resolved at composition time. */
private data class BannerPalette(
    val fill: Color,
    val border: Color,
    val iconWell: Color,
    val accent: Color,
    val onContent: Color,
    val onMessage: Color,
    val ctaContainerColor: Color,
    val ctaContentColor: Color = Color.White,
    val isGradient: Boolean = false,
    val gradientColors: List<Color> = emptyList(),
)

@Composable
private fun variantPalette(variant: NimazBannerVariant): BannerPalette = when (variant) {
    NimazBannerVariant.INFO -> {
        val accent = MaterialTheme.colorScheme.primary
        BannerPalette(
            fill = accent.copy(alpha = 0.10f),
            border = accent.copy(alpha = 0.30f),
            iconWell = accent.copy(alpha = 0.18f),
            accent = accent,
            onContent = MaterialTheme.colorScheme.onSurface,
            onMessage = MaterialTheme.colorScheme.onSurfaceVariant,
            ctaContainerColor = NimazColors.Primary700,
        )
    }

    NimazBannerVariant.WARNING -> {
        val accent = NimazColors.Warning
        BannerPalette(
            fill = accent.copy(alpha = 0.10f),
            border = accent.copy(alpha = 0.32f),
            iconWell = accent.copy(alpha = 0.20f),
            accent = accent,
            onContent = MaterialTheme.colorScheme.onSurface,
            onMessage = MaterialTheme.colorScheme.onSurfaceVariant,
            ctaContainerColor = NimazColors.GoldDark,
        )
    }

    NimazBannerVariant.ERROR -> {
        val accent = MaterialTheme.colorScheme.error
        BannerPalette(
            fill = accent.copy(alpha = 0.09f),
            border = accent.copy(alpha = 0.32f),
            iconWell = accent.copy(alpha = 0.16f),
            accent = accent,
            onContent = MaterialTheme.colorScheme.onSurface,
            onMessage = MaterialTheme.colorScheme.onSurfaceVariant,
            ctaContainerColor = accent,
        )
    }

    NimazBannerVariant.UPDATE -> {
        val accent = MaterialTheme.colorScheme.tertiary
        BannerPalette(
            fill = accent.copy(alpha = 0.10f),
            border = accent.copy(alpha = 0.30f),
            iconWell = accent.copy(alpha = 0.18f),
            accent = accent,
            onContent = MaterialTheme.colorScheme.onSurface,
            onMessage = MaterialTheme.colorScheme.onSurfaceVariant,
            ctaContainerColor = accent,
        )
    }

    NimazBannerVariant.EVENT -> {
        val teal800 = Color(0xFF115E59)
        val teal950 = Color(0xFF042F2E)
        val teal700 = Color(0xFF0F766E)
        val lightTeal = Color(0xFFEAF7F5)
        val gold = NimazColors.Gold500
        BannerPalette(
            fill = teal800,
            border = teal700,
            iconWell = Color.White.copy(alpha = 0.14f),
            accent = gold,
            onContent = lightTeal,
            onMessage = lightTeal.copy(alpha = 0.72f),
            ctaContainerColor = Color.White.copy(alpha = 0.16f),
            ctaContentColor = Color.White,
            isGradient = true,
            gradientColors = listOf(teal800, teal950),
        )
    }
}

private fun defaultIcon(variant: NimazBannerVariant): ImageVector = when (variant) {
    NimazBannerVariant.INFO -> Icons.Default.Info
    NimazBannerVariant.WARNING -> Icons.Default.Warning
    NimazBannerVariant.ERROR -> Icons.Outlined.Error
    NimazBannerVariant.UPDATE -> Icons.Default.Download
    NimazBannerVariant.EVENT -> Icons.Default.Star
}

/** Maps a banner variant to the nearest [NimazButtonVariant] for its action CTA. */
private fun NimazBannerVariant.actionButtonVariant(): NimazButtonVariant = NimazButtonVariant.PRIMARY

/**
 * Unified banner component. A single [BannerPalette] drives all five variants —
 * INFO, WARNING, ERROR, UPDATE and EVENT — through the same layout.
 *
 * - [title] is required; it is the primary text.
 * - [message] is optional, clamped to 2 lines.
 * - [onDismiss] adds an × button.
 * - [onClick] makes the whole banner tappable (adds a chevron, hides the action pill).
 * - [isLoading] replaces the action pill with a spinner.
 * - [density] controls corner radius (STANDALONE=17dp, INLINE=14dp) and shadow.
 */
@Composable
fun NimazBanner(
    title: String,
    variant: NimazBannerVariant,
    modifier: Modifier = Modifier,
    message: String? = null,
    icon: ImageVector? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    isLoading: Boolean = false,
    onClick: (() -> Unit)? = null,
    density: NimazBannerDensity = NimazBannerDensity.STANDALONE,
    leadingContent: (@Composable () -> Unit)? = null,
) {
    val palette = variantPalette(variant)
    val cornerRadius = if (density == NimazBannerDensity.INLINE) 14.dp else 17.dp
    val shape = RoundedCornerShape(cornerRadius)
    val effectiveIcon = icon ?: defaultIcon(variant)

    val backgroundModifier: Modifier = if (palette.isGradient) {
        Modifier.background(
            brush = Brush.linearGradient(colors = palette.gradientColors),
            shape = shape,
        )
    } else {
        Modifier.background(color = palette.fill, shape = shape)
    }

    val clickableModifier: Modifier =
        if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(width = 1.dp, color = palette.border, shape = shape)
            .then(backgroundModifier)
            .then(clickableModifier)
            .padding(horizontal = 15.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {

        if (leadingContent != null) {
            leadingContent()
        } else {
            NimazIconWell(
                icon = effectiveIcon,
                color = palette.accent,
                size = NimazIconWellSize.SMALL,
            )
        }

        // ── Text block ─────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 1.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = palette.onContent,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (message != null) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.onMessage,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // ── Right-side control: chevron | NimazButton (action / loading) ─────────
        Box(contentAlignment = Alignment.Center, modifier = Modifier.align(Alignment.CenterVertically)) {
            when {
                onClick != null -> {
                    NimazIcon(
                        imageVector = NimazIcons.Forward,
                        contentDescription = null,
                        tint = palette.accent,
                        iconSize = 18.dp,
                    )
                }

                actionLabel != null -> {
                    NimazButton(
                        text = actionLabel,
                        onClick = onAction ?: {},
                        variant = NimazButtonVariant.PRIMARY,
                        size = NimazButtonSize.SMALL,
                        loading = isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.ctaContainerColor,
                            contentColor = palette.ctaContentColor,
                            disabledContainerColor = palette.ctaContainerColor.copy(alpha = 0.4f),
                            disabledContentColor = palette.ctaContentColor.copy(alpha = 0.6f),
                        ),
                    )
                }

                isLoading -> {
                    NimazButton(
                        text = "",
                        onClick = {},
                        variant = NimazButtonVariant.PRIMARY,
                        size = NimazButtonSize.SMALL,
                        loading = true,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.ctaContainerColor,
                            contentColor = palette.ctaContentColor,
                            disabledContainerColor = palette.ctaContainerColor.copy(alpha = 0.4f),
                            disabledContentColor = palette.ctaContentColor.copy(alpha = 0.6f),
                        ),
                    )
                }
            }
        }

        // ── Dismiss button ─────────────────────────────────────────────────────
        if (onDismiss != null) {
            NimazIconButton(
                icon = Icons.Default.Close,
                onClick = onDismiss,
                contentDescription = stringResource(R.string.cd_dismiss),
                size = NimazIconButtonSize.SMALL,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = palette.onMessage,
                ),
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
    }
}

// ── Backward-compatible shim for HomeBannerCarousel ─────────────────────────────────────────────
// NimazBannerDefaults is retained so AnnouncementBanner can continue importing accent/colors.
object NimazBannerDefaults {
    @Composable
    fun accent(tone: NimazTone): Color =
        when (tone) {
            NimazTone.ACCENT,
            NimazTone.PROMINENT -> MaterialTheme.colorScheme.primary
            NimazTone.SUCCESS -> MaterialTheme.colorScheme.tertiary
            NimazTone.WARNING -> MaterialTheme.colorScheme.secondary
            NimazTone.ERROR -> MaterialTheme.colorScheme.error
            NimazTone.NEUTRAL,
            NimazTone.MUTED,
            NimazTone.TRANSPARENT ->
                MaterialTheme.colorScheme.outlineVariant
        }

    @Composable
    fun colors(
        tone: NimazTone,
        border: Color? = null,
    ): NimazCardColors =
        NimazCardDefaults.colors(
            container = MaterialTheme.colorScheme.surface,
            border = border ?: accent(tone),
        )
}

// ── Previews ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true, widthDp = 400, name = "Info Banner")
@Composable
private fun InfoBannerPreview() {
    NimazTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NimazBanner(
                title = "High latitude adjustment",
                message = "Summer nights in your area are too short for a natural Isha time.",
                variant = NimazBannerVariant.INFO,
            )
            NimazBanner(
                title = "Makeup fast info",
                variant = NimazBannerVariant.INFO,
                density = NimazBannerDensity.INLINE,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Warning Banner")
@Composable
private fun WarningBannerPreview() {
    NimazTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NimazBanner(
                title = "Notifications Disabled",
                message = "Prayer notifications need permission to alert you at prayer times.",
                variant = NimazBannerVariant.WARNING,
                icon = Icons.Default.Notifications,
                actionLabel = "Enable",
                onAction = {},
            )
            NimazBanner(
                title = "Battery Optimization Active",
                message = "Battery optimization may prevent timely prayer notifications.",
                variant = NimazBannerVariant.WARNING,
                icon = Icons.Default.BatteryAlert,
                actionLabel = "Fix",
                onAction = {},
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Error Banner")
@Composable
private fun ErrorBannerPreview() {
    NimazTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NimazBanner(
                title = "Calibration Needed",
                message = "Tap here for calibration instructions",
                variant = NimazBannerVariant.ERROR,
                onClick = {},
            )
            NimazBanner(
                title = "Calibration Needed",
                message = "Move your phone in a figure-8 pattern to calibrate the compass",
                variant = NimazBannerVariant.ERROR,
                actionLabel = "Calibrate",
                onAction = {},
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Update Banner")
@Composable
private fun UpdateBannerPreview() {
    NimazTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NimazBanner(
                title = "Update Available",
                message = "A new version of Nimaz is available.",
                variant = NimazBannerVariant.UPDATE,
                actionLabel = "Update",
                onAction = {},
            )
            NimazBanner(
                title = "Downloading update…",
                variant = NimazBannerVariant.UPDATE,
                isLoading = true,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Event Banner")
@Composable
private fun EventBannerPreview() {
    NimazTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NimazBanner(
                title = "Ramadan Mubarak!",
                message = "Wishing you a blessed and spiritually uplifting month.",
                variant = NimazBannerVariant.EVENT,
                actionLabel = "Explore",
                onAction = {},
            )
            NimazBanner(
                title = "Dismiss example",
                variant = NimazBannerVariant.EVENT,
                onDismiss = {},
            )
        }
    }
}
