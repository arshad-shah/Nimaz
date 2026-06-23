package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconVariant
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * The single dialog primitive for this app. Every dialog — confirmations,
 * info panels, list pickers, settings selectors — composes onto this.
 *
 * The visual language matches the rest of the design system:
 * - **28dp rounded corners** mirror the home hero's bottom-corner radius,
 *   placing the dialog in the same "page container" tier as the hero.
 * - **Optional [accentColor] stripe** along the top edge echoes the
 *   prayer-card accent bar — opt-in colour cue for destructive actions or
 *   prayer-themed prompts.
 * - **Icon-in-tinted-container header glyph** uses the same pattern as
 *   [NimazSettingsItem] and the home banner pills: small rounded square,
 *   accent colour at 15% opacity behind the icon.
 * - **Tonal elevation, no shadow.** Material 3 surfaces signal depth via
 *   tone rather than drop-shadows — keeps consistent with the rest of the
 *   tonal-elevation cards in the app.
 *
 * For 90% of dialogs you want one of the higher-level wrappers below
 * ([NimazConfirmDialog], [NimazInfoDialog]). Use this primitive directly
 * when you need custom content (forms, list pickers, settings groups).
 *
 * @param actions Slot for the action row. Use the pre-baked helpers
 *   ([NimazDialogCancelButton], [NimazDialogConfirmButton],
 *   [NimazDialogDestructiveButton]) to keep button hierarchy consistent
 *   across the app.
 */
@Composable
fun NimazDialog(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    titleIcon: ImageVector? = null,
    accentColor: Color? = null,
    subtitle: String? = null,
    showCloseButton: Boolean = true,
    showActionsDivider: Boolean = true,
    // When true (default), the [content] slot is wrapped in a surfaceVariant
    // card so text content reads against a tinted surface — same pattern as
    // every other card-on-surface composition in the app. Set false for
    // custom content (forms, list pickers) that does its own structuring.
    wrapContent: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val resolvedAccent = accentColor ?: MaterialTheme.colorScheme.primary

    Dialog(onDismissRequest = onDismiss, properties = properties) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column {
                // Top accent stripe — opt-in colour signal for destructive or
                // prayer-themed dialogs. Omitted for neutral dialogs so the
                // header reads cleanly.
                if (accentColor != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(resolvedAccent)
                    )
                }

                // Header: optional icon-in-container, title (+ subtitle), close X.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 12.dp, top = 20.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (titleIcon != null) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(resolvedAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            NimazIcon(
                                imageVector = titleIcon,
                                contentDescription = null,
                                tint = resolvedAccent,
                                iconSize = 22.dp
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (showCloseButton) {
                        IconButton(onClick = onDismiss) {
                            NimazIcon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.cd_close),
                                variant = NimazIconVariant.MUTED
                            )
                        }
                    }
                }

                // Content slot. By default wraps the content in a
                // surfaceVariant card so text reads against a tinted surface;
                // callers can opt out via wrapContent=false for custom layouts
                // (forms, list pickers) that do their own card structuring.
                if (content != null) {
                    if (wrapContent) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 18.dp, vertical = 16.dp)
                        ) {
                            content()
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(contentPadding)
                        ) {
                            content()
                        }
                    }
                }

                // Actions row: divider above keeps the buttons visually
                // detached from content; suppress it for compact dialogs.
                if (actions != null) {
                    if (showActionsDivider) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        actions()
                    }
                } else {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

// ──── Action-button helpers ──────────────────────────────────────────────────
// Use these in the [NimazDialog.actions] slot to keep button hierarchy
// consistent across every dialog in the app.

/** Neutral dismiss / cancel button. Always rendered as a text-variant button. */
@Composable
fun NimazDialogCancelButton(
    text: String = "Cancel",
    onClick: () -> Unit,
) {
    NimazButton(
        text = text,
        onClick = onClick,
        variant = NimazButtonVariant.TEXT
    )
}

/**
 * Primary affirmative action. Uses the tonal variant so it reads as the
 * preferred action without dominating the dialog like a high-emphasis
 * filled button would.
 */
@Composable
fun NimazDialogConfirmButton(
    text: String,
    onClick: () -> Unit,
) {
    NimazButton(
        text = text,
        onClick = onClick,
        variant = NimazButtonVariant.TONAL
    )
}

/**
 * Destructive primary action (delete, reset, sign out). Filled with the
 * error colour so the colour itself warns the user.
 */
@Composable
fun NimazDialogDestructiveButton(
    text: String,
    onClick: () -> Unit,
) {
    NimazButton(
        text = text,
        onClick = onClick,
        variant = NimazButtonVariant.DESTRUCTIVE
    )
}

// ──── Convenience wrappers ───────────────────────────────────────────────────

/**
 * Confirmation dialog: title, message body, Cancel + Confirm actions.
 *
 * Set [isDestructive] = true for destructive operations (reset, delete, sign
 * out) — paints the top accent stripe error-red and uses the destructive
 * button variant so the colour itself communicates risk.
 */
@Composable
fun NimazConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmText: String = "Confirm",
    cancelText: String = "Cancel",
    titleIcon: ImageVector? = null,
    isDestructive: Boolean = false,
) {
    val accent = if (isDestructive) MaterialTheme.colorScheme.error else null

    NimazDialog(
        title = title,
        onDismiss = onDismiss,
        titleIcon = titleIcon,
        accentColor = accent,
        showCloseButton = false,
        content = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        actions = {
            NimazDialogCancelButton(text = cancelText, onClick = onDismiss)
            if (isDestructive) {
                NimazDialogDestructiveButton(text = confirmText, onClick = {
                    onConfirm()
                    onDismiss()
                })
            } else {
                NimazDialogConfirmButton(text = confirmText, onClick = {
                    onConfirm()
                    onDismiss()
                })
            }
        }
    )
}

/**
 * Single-action informational dialog: title, message, "Got it" button.
 * Use for one-way information (e.g. "your data was synced") where the user
 * has no choice but acknowledgement.
 */
@Composable
fun NimazInfoDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    dismissText: String = "Got it",
    titleIcon: ImageVector? = Icons.Default.Info,
    accentColor: Color? = null,
) {
    NimazDialog(
        title = title,
        onDismiss = onDismiss,
        titleIcon = titleIcon,
        accentColor = accentColor,
        showCloseButton = false,
        content = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        actions = {
            NimazDialogConfirmButton(text = dismissText, onClick = onDismiss)
        }
    )
}

// ──── Previews ───────────────────────────────────────────────────────────────

@Preview(showBackground = true, widthDp = 412, heightDp = 320, name = "1. Info dialog")
@Composable
private fun NimazDialog_Info_Preview() {
    NimazTheme {
        NimazInfoDialog(
            title = "Location updated",
            message = "Prayer times have been recalculated for your new location. " +
                    "Notifications will fire at the updated times starting from the next prayer.",
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 320, name = "2. Confirm — neutral")
@Composable
private fun NimazDialog_Confirm_Preview() {
    NimazTheme {
        NimazConfirmDialog(
            title = "Mark all as prayed?",
            message = "This will mark all remaining prayers for today as completed. " +
                    "You can still uncheck individual prayers afterwards.",
            confirmText = "Mark all",
            onConfirm = {},
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 320, name = "3. Confirm — destructive")
@Composable
private fun NimazDialog_Destructive_Preview() {
    NimazTheme {
        NimazConfirmDialog(
            title = "Reset all settings?",
            message = "Calculation method, reciter, themes, and notification preferences " +
                    "will all return to defaults. Your prayer history will not be affected.",
            confirmText = "Reset",
            isDestructive = true,
            titleIcon = Icons.Default.Delete,
            onConfirm = {},
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 360, name = "4. With title icon")
@Composable
private fun NimazDialog_WithIcon_Preview() {
    NimazTheme {
        NimazConfirmDialog(
            title = "Enable location",
            message = "Nimaz needs your location to calculate accurate prayer times. " +
                    "Your location is never sent to any server — calculations happen on-device.",
            confirmText = "Allow",
            titleIcon = Icons.Default.LocationOn,
            onConfirm = {},
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 360, name = "5. Prayer-themed accent")
@Composable
private fun NimazDialog_PrayerAccent_Preview() {
    NimazTheme {
        // Asr-orange accent — illustrates how the accentColor stripe can
        // carry semantic colour from elsewhere in the app (prayer colour,
        // category colour, etc.) into the dialog header.
        val asrOrange = Color(0xFFF97316)
        NimazDialog(
            title = "Asr reminder set",
            subtitle = "Notifying 10 minutes before prayer time",
            onDismiss = {},
            titleIcon = Icons.Default.NightsStay,
            accentColor = asrOrange,
            showCloseButton = false,
            content = {
                Text(
                    text = "You'll get a notification at 4:20 PM today. You can change " +
                            "the timing or disable the reminder in Prayer Settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            actions = {
                NimazDialogConfirmButton(text = "Okay", onClick = {})
            }
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 360, name = "6. With subtitle")
@Composable
private fun NimazDialog_Subtitle_Preview() {
    NimazTheme {
        NimazDialog(
            title = "Sync to Drive",
            subtitle = "Last backed up 3 days ago",
            onDismiss = {},
            titleIcon = Icons.Default.Download,
            content = {
                Text(
                    text = "Backs up prayer tracking, bookmarks, and reading progress " +
                            "to your Google Drive. Restores automatically when you sign in on " +
                            "a new device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            actions = {
                NimazDialogCancelButton(text = "Later", onClick = {})
                NimazDialogConfirmButton(text = "Sync now", onClick = {})
            }
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 320, name = "7. Single primary action")
@Composable
private fun NimazDialog_SinglePrimary_Preview() {
    NimazTheme {
        NimazDialog(
            title = "Synced",
            onDismiss = {},
            titleIcon = Icons.Default.Check,
            accentColor = MaterialTheme.colorScheme.primary,
            showCloseButton = false,
            content = {
                Text(
                    text = "Your data is up to date.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            actions = {
                NimazDialogConfirmButton(text = "Got it", onClick = {})
            }
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 480, name = "8. Custom content (form)")
@Composable
private fun NimazDialog_CustomContent_Preview() {
    NimazTheme {
        NimazDialog(
            title = "Adjust prayer offset",
            subtitle = "Fine-tune the calculated time",
            onDismiss = {},
            titleIcon = Icons.Default.Info,
            // Form structures its own content rows + intro paragraph, so we
            // opt out of the auto-wrap and the caller controls the surface.
            wrapContent = false,
            content = {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Add or subtract minutes from the calculated time for each prayer.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha").forEach { name ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "+0 min",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            },
            actions = {
                NimazDialogCancelButton(text = "Cancel", onClick = {})
                NimazDialogConfirmButton(text = "Save", onClick = {})
            }
        )
    }
}

@Preview(
    showBackground = true,
    widthDp = 412,
    heightDp = 280,
    name = "9. No icon, no accent (minimal)"
)
@Composable
private fun NimazDialog_Minimal_Preview() {
    NimazTheme {
        NimazConfirmDialog(
            title = "Discard changes?",
            message = "You have unsaved edits to this preset.",
            confirmText = "Discard",
            isDestructive = true,
            onConfirm = {},
            onDismiss = {}
        )
    }
}
