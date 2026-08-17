package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcons
import com.arshadshah.nimaz.presentation.components.molecules.NimazBanner
import com.arshadshah.nimaz.presentation.components.molecules.NimazBannerVariant
import com.arshadshah.nimaz.presentation.components.molecules.NimazBottomSheet

/**
 * The home screen's attention slot: the one thing to deal with, and a way to see the rest.
 *
 * Renders nothing if [items] is empty. The first item is the banner; when there are more, a
 * "N more to deal with" button opens **a sheet** listing all of them, ranked.
 *
 * The overflow used to expand *in place*, pushing a stack of banners down the home screen and
 * shoving the prayer card below the fold. That is the wrong instrument: this is a queue of
 * interruptions, and a queue you look through is a thing you open and close, not a thing that
 * grows the page you were reading. A sheet also gives the list somewhere to be tall — there is
 * no bound on how many of these there can be at once — without the home screen having to hold
 * it.
 *
 * Inside the sheet, **acting closes it and dismissing does not**: an action takes you somewhere
 * (a permission dialog, a settings screen, an announcement's destination) and leaving the sheet
 * over the top of that is a modal covering the thing it just sent you to, while a dismissal is
 * housekeeping — the row goes and the queue you are working through stays open.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeBannerSlot(
    items: List<HomeBannerItem>,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return

    var sheetOpen by remember { mutableStateOf(false) }
    val overflowCount = items.size - 1

    // Everything but one can be dealt with while the sheet is open, and a sheet listing a
    // single banner that is already on the screen behind it is a sheet with nothing to say.
    // (The list emptying entirely is covered by the early return, which takes the sheet with it.)
    LaunchedEffect(overflowCount) { if (overflowCount == 0) sheetOpen = false }

    Column(modifier = modifier.fillMaxWidth()) {
        BannerSlotCard(items[0], rank = null)

        if (overflowCount > 0) {
            NimazButton(
                text = stringResource(R.string.home_n_more_banners, overflowCount),
                onClick = { sheetOpen = true },
                variant = NimazButtonVariant.TEXT,
                size = NimazButtonSize.SMALL,
                leadingIcon = NimazIcons.Expand,
                fullWidth = true,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }

    if (sheetOpen) {
        NimazBottomSheet(
            onDismissRequest = { sheetOpen = false },
            title = stringResource(R.string.home_banner_sheet_title),
            subtitle = stringResource(R.string.home_banner_sheet_subtitle, items.size),
            icon = Icons.Default.NotificationsActive,
            onClose = { sheetOpen = false },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEachIndexed { index, item ->
                    BannerSlotCard(
                        banner = item,
                        rank = index + 1,
                        onActed = { sheetOpen = false },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * One banner in the slot.
 *
 * [rank] numbers it inside the sheet and is null for the one on the home screen itself, which
 * has nothing to be numbered against. [onActed] runs after the banner's own action — see the
 * act-closes / dismiss-does-not rule on [HomeBannerSlot] — and is null outside the sheet.
 */
@Composable
private fun BannerSlotCard(
    banner: HomeBannerItem,
    rank: Int?,
    onActed: (() -> Unit)? = null,
) {
    val cardIsTappable = banner.actionLabel == null && banner.onAction != null
    val actionLabel = if (banner.isLoading) null else banner.actionLabel
    val onAction: (() -> Unit)? = if (banner.isLoading) null else banner.onAction?.let { act ->
        { act(); onActed?.invoke() }
    }

    NimazBanner(
        variant = when (banner.variant) {
            HomeBannerVariant.WARNING -> NimazBannerVariant.WARNING
            HomeBannerVariant.UPDATE -> NimazBannerVariant.UPDATE
            HomeBannerVariant.INFO -> NimazBannerVariant.INFO
            HomeBannerVariant.EVENT -> NimazBannerVariant.EVENT
        },
        icon = banner.icon,
        title = banner.title,
        message = banner.subtitle,
        actionLabel = actionLabel,
        onAction = onAction,
        isLoading = banner.isLoading,
        onDismiss = if (banner.dismissable) banner.onDismiss else null,
        onClick = if (cardIsTappable) onAction else null,
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
