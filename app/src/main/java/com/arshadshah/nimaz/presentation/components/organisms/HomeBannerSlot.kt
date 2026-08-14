package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.domain.model.Announcement
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.molecules.AnnouncementBanner
import com.arshadshah.nimaz.presentation.components.molecules.NimazBanner
import com.arshadshah.nimaz.presentation.components.molecules.NimazBannerVariant
import androidx.compose.ui.res.stringResource
import com.arshadshah.nimaz.R

/**
 * Unified banner slot for the home screen. Renders nothing if there are no banners and no active
 * announcement.
 *
 * Priority: if [announcement] is non-null it is shown first; system [banners] are the "N more".
 * If no announcement, the first system banner is the primary; remaining system banners are the
 * "N more". The overflow section is expanded/collapsed with a text link.
 */
@Composable
fun HomeBannerSlot(
    banners: List<HomeBannerItem>,
    announcement: Announcement?,
    showAnnouncementCta: Boolean,
    onAnnouncementCta: () -> Unit,
    onAnnouncementDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasAnnouncement = announcement != null
    val hasBanners = banners.isNotEmpty()
    if (!hasAnnouncement && !hasBanners) return

    var expanded by remember { mutableStateOf(false) }

    // Determine primary item and overflow list.
    // Primary is rendered as a full NimazBanner; overflow is shown/hidden via "N more" link.
    val overflowBanners: List<HomeBannerItem>
    val primaryContent: @Composable () -> Unit

    if (hasAnnouncement) {
        primaryContent = {
            AnnouncementBanner(
                announcement = announcement,
                showCta = showAnnouncementCta,
                onCtaClick = onAnnouncementCta,
                onDismiss = onAnnouncementDismiss,
            )
        }
        overflowBanners = banners
    } else {
        val first = banners.first()
        primaryContent = { BannerSlotCard(first) }
        overflowBanners = banners.drop(1)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        primaryContent()

        if (overflowBanners.isNotEmpty()) {
            val count = overflowBanners.size
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.home_n_more_banners, count),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(4.dp))
                NimazIcon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    iconSize = 14.dp,
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    overflowBanners.forEach { banner ->
                        BannerSlotCard(banner)
                    }
                }
            }
        }
    }
}

@Composable
private fun BannerSlotCard(banner: HomeBannerItem) {
    val cardIsTappable = banner.actionLabel == null && banner.onAction != null
    val actionLabel = if (banner.isLoading) null else banner.actionLabel
    val onAction = if (banner.isLoading) null else banner.onAction

    NimazBanner(
        variant = when (banner.variant) {
            HomeBannerVariant.WARNING -> NimazBannerVariant.WARNING
            HomeBannerVariant.UPDATE -> NimazBannerVariant.UPDATE
        },
        icon = banner.icon,
        title = banner.title,
        message = banner.subtitle,
        actionLabel = actionLabel,
        onAction = onAction,
        isLoading = banner.isLoading,
        onClick = if (cardIsTappable) { { banner.onAction?.invoke() } } else null,
    )
}
