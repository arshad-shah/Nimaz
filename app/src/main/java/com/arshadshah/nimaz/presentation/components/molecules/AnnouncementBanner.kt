package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.Announcement
import com.arshadshah.nimaz.domain.model.AnnouncementType
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * Dismissable engagement banner rendered at the top of Home when an FCM
 * announcement is active. Icon/accent follow the announcement [AnnouncementType];
 * the CTA button renders only when [showCta] (a label AND a resolvable route).
 * Animates in/out; renders nothing once [announcement] goes null so callers can
 * drop it in unconditionally.
 */
@Composable
fun AnnouncementBanner(
    announcement: Announcement?,
    showCta: Boolean,
    onCtaClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // AnimatedVisibility keeps rendering during the exit animation, after the
    // announcement has gone null — hold the last non-null one to animate out.
    var lastAnnouncement by remember { mutableStateOf(announcement) }
    if (announcement != null) lastAnnouncement = announcement

    AnimatedVisibility(
        visible = announcement != null,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        // The modifier (caller padding) lives on the card, inside the animated
        // content, so a hidden banner contributes zero space.
        lastAnnouncement?.let {
            AnnouncementBannerCard(it, showCta, onCtaClick, onDismiss, modifier)
        }
    }
}

@Composable
private fun AnnouncementBannerCard(
    announcement: Announcement,
    showCta: Boolean,
    onCtaClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The banner's tone carries the announcement type. Styling matches every other
    // banner in the app (see NimazBannerDefaults): a quiet container with the tone
    // in the border and the icon, so an announcement annotates Home rather than
    // shouting over it.
    val tone = when (announcement.type) {
        AnnouncementType.FEATURE -> NimazTone.ACCENT
        AnnouncementType.PRIVACY, AnnouncementType.TOS -> NimazTone.SUCCESS
        AnnouncementType.CHANGELOG -> NimazTone.WARNING
        // Celebration banners get their own dedicated card (Task 6); this generic
        // banner path is a fallback and shouldn't normally render CELEBRATION.
        AnnouncementType.CELEBRATION -> NimazTone.ACCENT
    }
    val accent = NimazBannerDefaults.accent(tone)
    val icon = when (announcement.type) {
        AnnouncementType.FEATURE -> Icons.Outlined.AutoAwesome
        AnnouncementType.PRIVACY, AnnouncementType.TOS -> Icons.Outlined.Shield
        AnnouncementType.CHANGELOG -> Icons.Outlined.Info
        AnnouncementType.CELEBRATION -> Icons.Outlined.AutoAwesome
    }

    NimazCard(
        modifier = modifier.fillMaxWidth(),
        style = NimazCardStyle.OUTLINED,
        tone = tone,
        shape = RoundedCornerShape(16.dp),
        colors = NimazBannerDefaults.colors(tone),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.Top,
        ) {
            NimazIcon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                size = NimazIconSize.LARGE,
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = announcement.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = announcement.body,
                    style = MaterialTheme.typography.bodySmall,
                )
                if (showCta && announcement.ctaLabel != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    NimazButton(
                        text = announcement.ctaLabel,
                        onClick = onCtaClick,
                        variant = NimazButtonVariant.TONAL,
                        size = NimazButtonSize.SMALL,
                    )
                }
            }
            if (announcement.dismissable) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.cd_dismiss_announcement),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

// ──── Previews ───────────────────────────────────────────────────────────────

@Preview(showBackground = true, widthDp = 400, name = "Feature announcement")
@Composable
private fun AnnouncementBanner_Feature_Preview() {
    NimazTheme {
        AnnouncementBanner(
            announcement = Announcement(
                id = "2026-07-ask-with-proof",
                type = AnnouncementType.FEATURE,
                title = "Ask with Proof is here",
                body = "Search the Qur'an, get cited answers.",
                ctaLabel = "Try it",
                route = "search/ask",
            ),
            showCta = true,
            onCtaClick = {},
            onDismiss = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Privacy notice, no CTA")
@Composable
private fun AnnouncementBanner_Privacy_Preview() {
    NimazTheme {
        AnnouncementBanner(
            announcement = Announcement(
                id = "2026-07-privacy",
                type = AnnouncementType.PRIVACY,
                title = "Privacy policy updated",
                body = "We've clarified how location data is used for prayer times.",
            ),
            showCta = false,
            onCtaClick = {},
            onDismiss = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
