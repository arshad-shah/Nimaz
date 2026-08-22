package com.arshadshah.nimaz.presentation.components.molecules

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCheckbox
import com.arshadshah.nimaz.presentation.components.atoms.NimazCheckboxSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazCheckboxType
import com.arshadshah.nimaz.presentation.components.atoms.NimazCheckboxVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconContainerShape
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconType
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcons
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * The app's anchored dropdown/menu system, in one place. Three public entry points,
 * one shared look, zero raw Material `DropdownMenu` at call sites:
 *
 * - [NimazDropdownField] — an inline trigger field that pops a menu to pick **one value**
 *   from a short list (≤ ~7 options). For long / searchable / grouped lists use the modal
 *   [NimazListPicker] instead.
 * - [NimazDropdownMenu] — a bare anchored menu for **action / overflow** commands.
 * - [NimazDropdownRow] — the single selectable/actionable row both of the above are built
 *   from (and the row to use inside any [NimazDropdownMenu]).
 *
 * All of them render on the one [NimazDropdownDefaults] popover surface.
 */

/**
 * Shared surface tokens for every anchored Nimaz menu. Centralising them is what makes the
 * field popup and the action menu read as one component: a rounded `surface` card lifted by
 * **tonal** elevation with only a hair of shadow and a faint outline — the app's popover
 * language, not Material's heavy drop-shadow menu.
 */
object NimazDropdownDefaults {
    /** Corner radius of the popup card — matches `NimazCard`'s soft 16dp. */
    val MenuShape: Shape = RoundedCornerShape(16.dp)

    /** Tonal lift that gives the popup its elevation tint (no harsh shadow). */
    val MenuTonalElevation: Dp = 3.dp

    /** A whisper of shadow purely to separate the popup from content beneath it. */
    val MenuShadowElevation: Dp = 3.dp

    /** Popup card fill. */
    val menuContainerColor: Color
        @Composable get() = MaterialTheme.colorScheme.surface

    /** Faint hairline so the card edge reads on same-colour backgrounds. */
    val menuBorder: BorderStroke
        @Composable get() = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
}

/**
 * A single option in a [NimazDropdownField]. Generic over the underlying value type so
 * callers stay type-safe.
 *
 * - [value]: what gets reported back through `onSelected`.
 * - [label]: the visible option text.
 * - [description]: optional secondary line beneath the label.
 * - [leadingIcon]: optional icon shown before the label.
 * - [textFontFamily]: optional typeface for the label — handy for font pickers where each
 *   option should render in the font it represents.
 */
data class NimazDropdownItem<T>(
    val value: T,
    val label: String,
    val description: String? = null,
    val leadingIcon: ImageVector? = null,
    val textFontFamily: FontFamily? = null,
)

/**
 * The one row used inside every Nimaz dropdown/menu — the design-system replacement for
 * Material's stock `DropdownMenuItem`. It serves both jobs the system needs:
 *
 * - **Selection row:** pass [selected]. The selected row fills with `primaryContainer`,
 *   bolds, and shows a trailing circular check badge — the same visual as [NimazListPicker].
 * - **Action row:** leave [selected] false. Pass [destructive] = true to tint an
 *   irreversible command (delete, reset, …) with the error colour.
 *
 * @param text the row label.
 * @param onClick invoked on tap; the caller closes the menu (the field does this for you).
 * @param selected highlights the row and shows the trailing check badge.
 * @param leadingIcon optional icon shown before the label.
 * @param description optional secondary line beneath the label.
 * @param textFontFamily optional typeface for the label (font pickers).
 * @param destructive tints an unselected action row with the error colour.
 * @param enabled when false the row is dimmed and ignores taps.
 */
@Composable
fun NimazDropdownRow(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    leadingIcon: ImageVector? = null,
    description: String? = null,
    textFontFamily: FontFamily? = null,
    destructive: Boolean = false,
    enabled: Boolean = true,
) {
    val contentColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        selected -> MaterialTheme.colorScheme.onPrimaryContainer
        destructive -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }
    // Plain (enabled, unselected, non-destructive) icons sit muted; selected/destructive/
    // disabled icons share the row's content colour.
    val iconTint = when {
        !enabled || selected || destructive -> contentColor
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            NimazIcon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = iconTint,
                size = NimazIconSize.MEDIUM
            )
            Spacer(modifier = Modifier.width(12.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = textFontFamily,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = contentColor
            )
            if (!description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }

        if (selected) {
            Spacer(modifier = Modifier.width(10.dp))
            NimazCheckbox(
                checked = true,
                onCheckedChange = null,
                type = NimazCheckboxType.CIRCLE,
                variant = NimazCheckboxVariant.PRIMARY,
                size = NimazCheckboxSize.MEDIUM,
            )
        }
    }
}

/**
 * An app-styled anchored menu for **action / overflow** commands (an icon-triggered list of
 * [NimazDropdownRow]s), as opposed to a single-select value field — for that use
 * [NimazDropdownField].
 *
 * A thin wrapper over Material's [DropdownMenu] (so it keeps correct anchoring, tap-outside /
 * back-press dismissal and edge-collision flipping) that swaps the bare Material surface for
 * the shared [NimazDropdownDefaults] popover.
 *
 * @param expanded whether the menu is open.
 * @param onDismissRequest called when the user taps outside or presses back.
 * @param offset shifts the popup from its anchor (passed straight to [DropdownMenu]).
 * @param content the menu rows — typically [NimazDropdownRow]s.
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
 * The design-system replacement for Material's `ExposedDropdownMenuBox`: an inline
 * single-select field. A soft, filled trigger (matching the app's filled settings rows,
 * lifting to the accent `primaryContainer` while open) that pops a [NimazDropdownMenu] of
 * [NimazDropdownRow]s directly beneath it, sized to the trigger width.
 *
 * Use it for **short** option lists (≤ ~7); for long / searchable / grouped lists use the
 * modal [NimazListPicker] instead.
 *
 * Drawn on the shared [NimazFieldShell], which is where its own geometry came from — so the
 * dropdown, [NimazTextField] and [NimazAmountInput] cannot drift apart. The one behaviour that
 * moved into the shell and changed on the way: the border used to go primary whenever a value
 * was *set*, and is now primary only while the menu is open. See [NimazFieldShell]'s docs.
 *
 * @param items the selectable options, type-safe over [T].
 * @param selected the currently selected value, or null when nothing is chosen.
 * @param onSelected called with the chosen value; the menu closes automatically.
 * @param label optional caption rendered above the field.
 * @param placeholder shown in the field when [selected] is null.
 * @param helper optional always-visible line beneath the field.
 * @param error a message (not a boolean) shown in place of [helper], outlining the field in red.
 * @param enabled when false the field is dimmed and ignores taps.
 */
@Composable
fun <T> NimazDropdownField(
    items: List<NimazDropdownItem<T>>,
    selected: T?,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "Select",
    helper: String? = null,
    error: String? = null,
    required: Boolean = false,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    var fieldWidthPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current

    val selectedItem = items.firstOrNull { it.value == selected }
    val isEmpty = selectedItem == null
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "dropdownChevron"
    )

    // The chevron well is the family's one piece of character, and it stays: a 26dp circle at
    // 12% primary that fills solid and flips as the menu opens.
    val chevronCircleColor = if (expanded) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    val chevronTint = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant
        expanded -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.primary
    }

    NimazFieldShell(
        modifier = modifier,
        label = label,
        required = required,
        helper = helper,
        error = error,
        // A dropdown has no caret, so "the menu is open" is what focus means here.
        focused = expanded,
        enabled = enabled,
        onClick = { expanded = true },
        boxModifier = Modifier.onSizeChanged { fieldWidthPx = it.width },
        anchored = {
            NimazDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.width(
                    with(density) { fieldWidthPx.toDp() }.coerceAtLeast(0.dp)
                )
            ) {
                items.forEach { item ->
                    NimazDropdownRow(
                        text = item.label,
                        selected = item.value == selected,
                        leadingIcon = item.leadingIcon,
                        textFontFamily = item.textFontFamily,
                        description = item.description,
                        onClick = {
                            onSelected(item.value)
                            expanded = false
                        }
                    )
                }
            }
        },
    ) {
        if (selectedItem?.leadingIcon != null) {
            NimazIcon(
                imageVector = selectedItem.leadingIcon,
                contentDescription = null,
                type = NimazIconType.CONTAINED,
                containerShape = NimazIconContainerShape.ROUNDED_SQUARE,
                tint = MaterialTheme.colorScheme.primary,
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                containerSize = 32.dp,
                iconSize = 18.dp,
                cornerRadius = 9.dp,
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(
            text = selectedItem?.label ?: placeholder,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            fontFamily = selectedItem?.textFontFamily,
            color = if (isEmpty) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(chevronCircleColor),
            contentAlignment = Alignment.Center
        ) {
            NimazIcon(
                imageVector = NimazIcons.Expand,
                contentDescription = null,
                tint = chevronTint,
                iconSize = 16.dp,
                modifier = Modifier.rotate(chevronRotation)
            )
        }
    }
}

// ──── Previews ───────────────────────────────────────────────────────────────

@Composable
private fun PreviewLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
    )
}

/** Every [NimazDropdownRow] flavour: selection, description, leading icon, action, destructive, disabled. */
@Composable
private fun NimazDropdownRowShowcase() {
    Column {
        NimazDropdownRow(text = "Amiri", selected = false, onClick = {})
        NimazDropdownRow(text = "Amiri", selected = true, onClick = {})
        NimazDropdownRow(
            text = "Scheherazade New",
            selected = true,
            description = "Classic naskh typeface",
            onClick = {}
        )
        NimazDropdownRow(
            text = "Standard",
            selected = false,
            leadingIcon = Icons.Default.WbSunny,
            onClick = {}
        )
        NimazDropdownRow(text = "Share", leadingIcon = Icons.Filled.Share, onClick = {})
        NimazDropdownRow(
            text = "Reset Journey",
            leadingIcon = Icons.Filled.RestartAlt,
            destructive = true,
            onClick = {}
        )
        NimazDropdownRow(text = "Unavailable", enabled = false, onClick = {})
    }
}

/** A faux open menu — the real popup can't render in an isolated preview, so the rows are
 *  shown on a [NimazDropdownDefaults] surface to represent the dropped menu. */
@Composable
private fun NimazDropdownMenuShowcase() {
    Surface(
        shape = NimazDropdownDefaults.MenuShape,
        color = NimazDropdownDefaults.menuContainerColor,
        tonalElevation = NimazDropdownDefaults.MenuTonalElevation,
        border = NimazDropdownDefaults.menuBorder,
        modifier = Modifier.width(260.dp)
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            NimazDropdownRow(text = "Standard", selected = true, onClick = {})
            NimazDropdownRow(text = "Hanafi", selected = false, onClick = {})
        }
    }
}

/** [NimazDropdownField] in each state: resting (selected), leading icon, placeholder, disabled. */
@Composable
private fun NimazDropdownFieldShowcase() {
    val fonts = listOf(
        NimazDropdownItem("amiri", "Amiri"),
        NimazDropdownItem("scheherazade", "Scheherazade New"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        NimazDropdownField(
            label = "Arabic Font",
            items = fonts,
            selected = "amiri",
            onSelected = {})
        NimazDropdownField(
            label = "Asr Calculation",
            items = listOf(
                NimazDropdownItem("standard", "Standard", leadingIcon = Icons.Default.WbSunny),
                NimazDropdownItem("hanafi", "Hanafi"),
            ),
            selected = "standard",
            onSelected = {},
        )
        NimazDropdownField(
            label = "Exemption Reason",
            items = fonts,
            selected = null,
            placeholder = "Select reason",
            onSelected = {},
        )
        NimazDropdownField(
            label = "High Latitude Rule",
            items = fonts,
            selected = "amiri",
            enabled = false,
            onSelected = {},
        )
    }
}

@Composable
private fun NimazDropdownShowcase() {
    Column(modifier = Modifier.padding(16.dp)) {
        PreviewLabel("Field — resting · leading icon · placeholder · disabled")
        NimazDropdownFieldShowcase()
        PreviewLabel("Menu (open) — selected + unselected rows")
        NimazDropdownMenuShowcase()
        PreviewLabel("Rows — every flavour")
        NimazDropdownRowShowcase()
    }
}

@Preview(showBackground = true, widthDp = 360, name = "NimazDropdown — Light")
@Composable
private fun NimazDropdownLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { NimazDropdownShowcase() }
}

@Preview(
    showBackground = true, widthDp = 360, name = "NimazDropdown — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun NimazDropdownDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) { NimazDropdownShowcase() }
}
