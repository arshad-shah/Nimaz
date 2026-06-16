package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
    val isLoading: Boolean = false,
)

/**
 * Compact horizontal carousel of banner pills. Use this in place of stacked
 * full-width banners when the count of attention items varies and you don't
 * want each one pushing the rest of the page down.
 *
 * The carousel renders nothing when [banners] is empty — the caller doesn't
 * need to wrap it in a conditional, just let it self-elide.
 */
@Composable
fun HomeBannerCarousel(
    banners: List<HomeBannerItem>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp),
) {
    if (banners.isEmpty()) return

    LazyRow(
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(banners, key = { it.id }) { banner ->
            BannerPill(banner = banner)
        }
    }
}

@Composable
private fun BannerPill(banner: HomeBannerItem) {
    val colors = pillColorsFor(banner.variant)
    val pillIsTappable = banner.actionLabel == null && banner.onAction != null

    Row(
        modifier = Modifier
            .height(48.dp)
            .widthIn(min = 0.dp, max = 320.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(colors.container)
            .then(
                if (pillIsTappable) {
                    Modifier.clickable { banner.onAction.invoke() }
                } else Modifier
            )
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = banner.icon,
            contentDescription = null,
            tint = colors.icon,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = banner.title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = colors.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )

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
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.actionContainer)
                        .clickable { banner.onAction?.invoke() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
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
    val title: Color,
    val icon: Color,
    val actionContainer: Color,
    val actionText: Color,
)

@Composable
private fun pillColorsFor(variant: HomeBannerVariant): PillColors {
    val scheme = MaterialTheme.colorScheme
    return when (variant) {
        HomeBannerVariant.WARNING -> PillColors(
            container = scheme.errorContainer,
            title = scheme.onErrorContainer,
            icon = scheme.error,
            actionContainer = scheme.error,
            actionText = scheme.onError,
        )
        HomeBannerVariant.UPDATE -> PillColors(
            container = scheme.primaryContainer,
            title = scheme.onPrimaryContainer,
            icon = scheme.primary,
            actionContainer = scheme.primary,
            actionText = scheme.onPrimary,
        )
    }
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
