package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * Variant determines accent/background colour of a banner pill. Warnings get
 * the error container (red-tinted); updates get the primary container.
 */
enum class HomeBannerVariant { WARNING, UPDATE }

/**
 * One pill in the banner carousel. Keep [title] short — pills are sized to
 * content but truncate aggressively past one line so the carousel keeps a
 * stable height.
 *
 * - [actionLabel] / [onAction]: optional inline button. Omit to make the
 *   whole pill tappable instead.
 * - [isLoading]: when true, replaces the action with a small spinner
 *   (e.g. during an update download).
 */
data class HomeBannerItem(
    val id: String,
    val icon: ImageVector,
    val title: String,
    val variant: HomeBannerVariant,
    val subtitle: String? = null,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
    val isLoading: Boolean = false,
)

/**
 * Banner pills surfaced on the home screen (notifications off, location, update…).
 * One pill per page on the shared [NimazCarousel] — peek of the next pill plus
 * indicator dots — so multiple alerts never stack and push content down.
 *
 * Renders nothing when [banners] is empty, so the caller can drop it in
 * unconditionally.
 */
@Composable
fun HomeBannerCarousel(
    banners: List<HomeBannerItem>,
    modifier: Modifier = Modifier,
) {
    if (banners.isEmpty()) return

    NimazCarousel(
        count = banners.size,
        modifier = modifier,
        pageHeight = 56.dp,
        horizontalPadding = 20.dp,
        pageSpacing = 10.dp,
    ) { page ->
        BannerPill(banner = banners[page])
    }
}

@Composable
private fun BannerPill(banner: HomeBannerItem) {
    val colors = pillColorsFor(banner.variant)
    val pillIsTappable = banner.actionLabel == null && banner.onAction != null
    val hasTrailing = banner.isLoading || banner.actionLabel != null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .clip(RoundedCornerShape(26.dp))
            .background(colors.container)
            .border(BorderStroke(1.dp, colors.border), RoundedCornerShape(26.dp))
            .then(
                if (pillIsTappable) {
                    Modifier.clickable { banner.onAction.invoke() }
                } else Modifier
            )
            // Tighter trailing inset when an action chip sits at the end; roomier
            // when the pill ends in text so it doesn't look cramped.
            .padding(start = 8.dp, end = if (hasTrailing) 8.dp else 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Contained icon chip — same 12dp-radius tinted chip as the Today cards.
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.chip),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = banner.icon,
                contentDescription = null,
                tint = colors.icon,
                modifier = Modifier.size(17.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        // Fills the gap between the icon and the trailing action so the pill
        // spreads across the full page width instead of bunching on the left.
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = banner.title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = colors.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!banner.subtitle.isNullOrBlank()) {
                Text(
                    text = banner.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.subtitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        when {
            banner.isLoading -> {
                Spacer(modifier = Modifier.width(10.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = colors.icon
                )
            }
            banner.actionLabel != null -> {
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.actionContainer)
                        .clickable { banner.onAction?.invoke() }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = banner.actionLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.actionText
                    )
                }
            }
        }
    }
}

private data class PillColors(
    val container: Color,
    val border: Color,
    val chip: Color,
    val title: Color,
    val subtitle: Color,
    val icon: Color,
    val actionContainer: Color,
    val actionText: Color,
)

@Composable
private fun pillColorsFor(variant: HomeBannerVariant): PillColors {
    val scheme = MaterialTheme.colorScheme
    // Both variants ride on the surface (white) with a hairline border to match
    // the card language; only the accent (chip tint, icon, action) changes.
    val accent = when (variant) {
        HomeBannerVariant.WARNING -> scheme.error
        HomeBannerVariant.UPDATE -> scheme.primary
    }
    val onAccent = when (variant) {
        HomeBannerVariant.WARNING -> scheme.onError
        HomeBannerVariant.UPDATE -> scheme.onPrimary
    }
    return PillColors(
        container = scheme.surface,
        border = scheme.outline.copy(alpha = 0.4f),
        chip = accent.copy(alpha = 0.14f),
        title = scheme.onSurface,
        subtitle = scheme.onSurfaceVariant,
        icon = accent,
        actionContainer = accent,
        actionText = onAccent,
    )
}

// ──── Previews ───────────────────────────────────────────────────────────────

@Preview(showBackground = true, widthDp = 412, name = "1. Multiple banners")
@Composable
private fun HomeBannerCarousel_Multiple_Preview() {
    NimazTheme {
        HomeBannerCarousel(
            banners = listOf(
                HomeBannerItem(
                    id = "notifications",
                    icon = Icons.Default.Notifications,
                    title = "Notifications off",
                    variant = HomeBannerVariant.WARNING,
                    actionLabel = "Enable",
                    onAction = {},
                ),
                HomeBannerItem(
                    id = "location",
                    icon = Icons.Default.LocationOn,
                    title = "Location permission needed",
                    variant = HomeBannerVariant.WARNING,
                    actionLabel = "Grant",
                    onAction = {},
                ),
                HomeBannerItem(
                    id = "battery",
                    icon = Icons.Default.BatteryAlert,
                    title = "Battery optimised",
                    variant = HomeBannerVariant.WARNING,
                    actionLabel = "Fix",
                    onAction = {},
                ),
                HomeBannerItem(
                    id = "update",
                    icon = Icons.Default.Download,
                    title = "Update available",
                    variant = HomeBannerVariant.UPDATE,
                    actionLabel = "Update",
                    onAction = {},
                ),
            ),
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 412, name = "2. Single banner")
@Composable
private fun HomeBannerCarousel_Single_Preview() {
    NimazTheme {
        HomeBannerCarousel(
            banners = listOf(
                HomeBannerItem(
                    id = "notifications",
                    icon = Icons.Default.Notifications,
                    title = "Notifications off",
                    variant = HomeBannerVariant.WARNING,
                    actionLabel = "Enable",
                    onAction = {},
                ),
            ),
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 412, name = "3. Downloading update")
@Composable
private fun HomeBannerCarousel_Downloading_Preview() {
    NimazTheme {
        HomeBannerCarousel(
            banners = listOf(
                HomeBannerItem(
                    id = "downloading",
                    icon = Icons.Default.Download,
                    title = "Downloading update",
                    variant = HomeBannerVariant.UPDATE,
                    isLoading = true,
                ),
            ),
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 412, name = "4. Update ready")
@Composable
private fun HomeBannerCarousel_UpdateReady_Preview() {
    NimazTheme {
        HomeBannerCarousel(
            banners = listOf(
                HomeBannerItem(
                    id = "downloaded",
                    icon = Icons.Default.Refresh,
                    title = "Update ready",
                    variant = HomeBannerVariant.UPDATE,
                    actionLabel = "Restart",
                    onAction = {},
                ),
            ),
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}
