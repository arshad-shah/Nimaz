package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.molecules.NimazBanner
import com.arshadshah.nimaz.presentation.components.molecules.NimazBannerVariant

/**
 * Unified banner slot for the home screen. Renders nothing if [items] is empty.
 *
 * Shows [items][0] as the primary banner. If there are more items, a "N more to deal with" text
 * link toggles an expanded list showing ALL items with numbered rank badges.
 */
@Composable
fun HomeBannerSlot(
    items: List<HomeBannerItem>,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return

    var expanded by remember { mutableStateOf(false) }
    val overflowCount = items.size - 1

    Column(modifier = modifier.fillMaxWidth()) {
        if (expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEachIndexed { index, item ->
                    BannerSlotCard(item, rank = index + 1)
                }
            }
        } else {
            BannerSlotCard(items[0], rank = null)
        }

        if (overflowCount > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.home_n_more_banners, overflowCount),
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
        }
    }
}

@Composable
private fun BannerSlotCard(banner: HomeBannerItem, rank: Int?) {
    val cardIsTappable = banner.actionLabel == null && banner.onAction != null
    val actionLabel = if (banner.isLoading) null else banner.actionLabel
    val onAction = if (banner.isLoading) null else banner.onAction

    NimazBanner(
        variant = when (banner.variant) {
            HomeBannerVariant.WARNING -> NimazBannerVariant.WARNING
            HomeBannerVariant.UPDATE -> NimazBannerVariant.UPDATE
            HomeBannerVariant.INFO -> NimazBannerVariant.INFO
            HomeBannerVariant.EVENT -> NimazBannerVariant.EVENT
        },
        icon = if (rank == null) banner.icon else banner.icon,
        title = banner.title,
        message = banner.subtitle,
        actionLabel = actionLabel,
        onAction = onAction,
        isLoading = banner.isLoading,
        onDismiss = if (banner.dismissable) banner.onDismiss else null,
        onClick = if (cardIsTappable) { { banner.onAction?.invoke() } } else null,
        leadingContent = if (rank != null) {
            {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = rank.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else null,
    )
}

