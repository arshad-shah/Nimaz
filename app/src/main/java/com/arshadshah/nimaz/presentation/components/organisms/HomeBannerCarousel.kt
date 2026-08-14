package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.molecules.NimazBanner
import com.arshadshah.nimaz.presentation.components.molecules.NimazBannerVariant
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * Variant determines the accent/style of a banner. Warnings get the amber
 * warning treatment; updates get the primary container.
 */
enum class HomeBannerVariant { WARNING, UPDATE }

/**
 * One banner in the carousel. Keep [title] short — banners are sized to a
 * compact page height so the carousel keeps a stable height.
 *
 * - [actionLabel] / [onAction]: optional inline button. Omit to make the
 *   whole banner tappable instead.
 * - [isLoading]: when true, suppresses the action (the [NimazBanner] update
 *   variant shows a spinner in its place).
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
 * Banner alerts surfaced on the home screen (notifications off, location, update…).
 * One [NimazBanner] per page on the shared [NimazCarousel] — peek of the next
 * banner plus indicator dots — so multiple alerts never stack and push content
 * down.
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
        pageHeight = 72.dp,
        horizontalPadding = 20.dp,
        pageSpacing = 10.dp,
    ) { page ->
        BannerCard(banner = banners[page])
    }
}

@Composable
private fun BannerCard(banner: HomeBannerItem) {
    // Whole-card tap when there's no inline action but an onAction is supplied.
    val cardIsTappable = banner.actionLabel == null && banner.onAction != null
    // A spinner-state banner suppresses its inline action (the update variant
    // renders a spinner in its place; matches the old pill behaviour).
    val actionLabel = if (banner.isLoading) null else banner.actionLabel
    val onAction = if (banner.isLoading) null else banner.onAction
    // Promote the subtitle to the banner body so the title stays the heading;
    // with no subtitle the title itself becomes the single-line message.
    val hasSubtitle = !banner.subtitle.isNullOrBlank()

    NimazBanner(
        message = if (hasSubtitle) banner.subtitle else banner.title,
        variant = when (banner.variant) {
            HomeBannerVariant.WARNING -> NimazBannerVariant.WARNING
            HomeBannerVariant.UPDATE -> NimazBannerVariant.UPDATE
        },
        icon = banner.icon,
        title = if (hasSubtitle) banner.title else null,
        actionLabel = actionLabel,
        onAction = onAction,
        isLoading = banner.isLoading,
        modifier = if (cardIsTappable) {
            Modifier.clickable { banner.onAction.invoke() }
        } else {
            Modifier
        },
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
