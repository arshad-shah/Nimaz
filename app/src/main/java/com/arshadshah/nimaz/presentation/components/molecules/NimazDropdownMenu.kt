package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * Shared surface tokens for every anchored Nimaz menu (the [NimazDropdownMenu] action
 * menu and the [NimazDropdownField] selector popup). Centralising them here is what makes
 * the two read as one component: a rounded `surface` card lifted by **tonal** elevation
 * with only a hair of shadow and a faint outline — the app's popover language, not
 * Material's heavy drop-shadow menu.
 */
object NimazDropdownDefaults {
    /** Corner radius of the popup card — matches `NimazCard`'s soft 16dp. */
    val MenuShape: Shape = RoundedCornerShape(16.dp)

    /** Tonal lift that gives the popup its elevation tint (no harsh shadow). */
    val MenuTonalElevation: Dp = 3.dp

    /** A whisper of shadow purely to separate the popup from content beneath it. */
    val MenuShadowElevation: Dp = 3.dp

    /** Popup card fill. */
    val menuContainerColor: androidx.compose.ui.graphics.Color
        @Composable get() = MaterialTheme.colorScheme.surface

    /** Faint hairline so the card edge reads on same-colour backgrounds. */
    val menuBorder: BorderStroke
        @Composable get() = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
}

/**
 * The design-system replacement for Material's [DropdownMenu] when you need an
 * **action / overflow menu** (an icon-triggered list of commands), as opposed to a
 * single-select value field — for that, reach for [NimazDropdownField].
 *
 * It is a thin wrapper over Material's [DropdownMenu] (so it keeps correct anchoring,
 * tap-outside / back-press dismissal and edge-collision flipping) that swaps the bare
 * Material surface for the shared [NimazDropdownDefaults] popover. Fill it with
 * [NimazDropdownAction] rows so the commands inherit the app's row styling.
 *
 * @param expanded whether the menu is open.
 * @param onDismissRequest called when the user taps outside or presses back.
 * @param offset shifts the popup from its anchor (passed straight to [DropdownMenu]).
 * @param content the menu rows — typically [NimazDropdownAction]s.
 */
@Composable
fun NimazDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        offset = offset,
        shape = NimazDropdownDefaults.MenuShape,
        containerColor = NimazDropdownDefaults.menuContainerColor,
        tonalElevation = NimazDropdownDefaults.MenuTonalElevation,
        shadowElevation = NimazDropdownDefaults.MenuShadowElevation,
        border = NimazDropdownDefaults.menuBorder,
        content = content,
    )
}

/**
 * A single command row inside a [NimazDropdownMenu].
 *
 * Mirrors the metrics of [NimazDropdownMenuItem] (the *selection* row) — same rounded
 * hit target, padding and `MEDIUM` leading icon — but carries no selection state or
 * check badge, because actions are commands, not choices. Set [destructive] to tint the
 * label and icon with the error colour for irreversible actions (delete, reset, …).
 *
 * @param text the command label.
 * @param onClick invoked on tap; the caller is responsible for closing the menu.
 * @param leadingIcon optional icon shown before the label.
 * @param destructive tints the row with the error colour for irreversible actions.
 * @param enabled when false the row is dimmed and ignores taps.
 */
@Composable
fun NimazDropdownAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    destructive: Boolean = false,
    enabled: Boolean = true,
) {
    val contentColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        destructive -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            NimazIcon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = contentColor,
                size = NimazIconSize.MEDIUM
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor,
            modifier = Modifier.weight(1f)
        )
    }
}

// ──── Previews ───────────────────────────────────────────────────────────────

@Preview(showBackground = true, widthDp = 280, name = "NimazDropdownMenu — actions")
@Composable
private fun NimazDropdownActionsPreview() {
    NimazTheme {
        // Render the rows directly (a real popup can't be previewed in isolation) so the
        // surface tokens and row styling are visible.
        Column {
            NimazDropdownAction(
                text = "Share",
                leadingIcon = Icons.Filled.Share,
                onClick = {},
            )
            NimazDropdownAction(
                text = "Reset Journey",
                leadingIcon = Icons.Filled.RestartAlt,
                destructive = true,
                onClick = {},
            )
        }
    }
}
