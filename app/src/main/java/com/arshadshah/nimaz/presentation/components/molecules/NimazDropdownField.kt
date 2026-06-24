package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.DropdownMenu
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * A single option in a [NimazDropdownField]. Generic over the underlying value
 * type so callers stay type-safe.
 *
 * - [value]: what gets reported back through `onSelected`.
 * - [label]: the visible option text.
 * - [description]: optional secondary line beneath the label.
 * - [leadingIcon]: optional icon shown before the label.
 * - [textFontFamily]: optional typeface for the label — handy for font pickers
 *   where each option should render in the font it represents.
 */
data class NimazDropdownItem<T>(
    val value: T,
    val label: String,
    val description: String? = null,
    val leadingIcon: ImageVector? = null,
    val textFontFamily: FontFamily? = null,
)

/**
 * The design-system replacement for Material's [androidx.compose.material3.ExposedDropdownMenuBox].
 *
 * A soft, filled trigger field that pops an anchored menu directly beneath it.
 * The trigger matches the app's filled settings rows (rounded
 * `surfaceContainer` fill, no outline) and lifts to the accent
 * `primaryContainer` while open; the popup reuses [NimazDropdownMenuItem] for
 * its rows, so options inherit the same selection visuals (accent fill +
 * circular check badge) as the rest of the component set.
 *
 * Built on Material's [DropdownMenu], so it inherits correct anchoring,
 * tap-outside / back-press dismissal, IME handling and edge-collision flipping
 * for free — we only restyle the surface and rows. The popup is sized to match
 * the trigger width via [onSizeChanged] (the one convenience `DropdownMenu`
 * does not provide on its own).
 *
 * @param items the selectable options, type-safe over [T].
 * @param selected the currently selected value, or null when nothing is chosen.
 * @param onSelected called with the chosen value; the menu closes automatically.
 * @param label optional caption rendered above the field.
 * @param placeholder shown in the field when [selected] is null.
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
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    var fieldWidthPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current

    val selectedItem = items.firstOrNull { it.value == selected }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "dropdownChevron"
    )

    Column(modifier = modifier) {
        if (!label.isNullOrBlank()) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            // ── Trigger field ──────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { fieldWidthPx = it.width }
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (expanded) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                    .clickable(enabled = enabled) { expanded = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedItem?.leadingIcon != null) {
                    NimazIcon(
                        imageVector = selectedItem.leadingIcon,
                        contentDescription = null,
                        tint = if (expanded) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        iconSize = 20.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(
                    text = selectedItem?.label ?: placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    fontFamily = selectedItem?.textFontFamily,
                    color = when {
                        expanded -> MaterialTheme.colorScheme.onPrimaryContainer
                        selectedItem == null -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                NimazIcon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = if (expanded) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(chevronRotation)
                )
            }

            // ── Anchored popup ─────────────────────────────────────────────
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                shape = NimazDropdownDefaults.MenuShape,
                containerColor = NimazDropdownDefaults.menuContainerColor,
                tonalElevation = NimazDropdownDefaults.MenuTonalElevation,
                shadowElevation = NimazDropdownDefaults.MenuShadowElevation,
                border = NimazDropdownDefaults.menuBorder,
                modifier = Modifier.width(
                    with(density) { fieldWidthPx.toDp() }.coerceAtLeast(0.dp)
                )
            ) {
                items.forEach { item ->
                    NimazDropdownMenuItem(
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
        }
    }
}

// ──── Previews ───────────────────────────────────────────────────────────────

@Preview(showBackground = true, widthDp = 360, name = "NimazDropdownField — collapsed")
@Composable
private fun NimazDropdownFieldPreview() {
    NimazTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            NimazDropdownField(
                label = "Asr Calculation",
                items = listOf(
                    NimazDropdownItem("standard", "Standard", leadingIcon = Icons.Default.WbSunny),
                    NimazDropdownItem("hanafi", "Hanafi", leadingIcon = Icons.Default.Schedule),
                ),
                selected = "standard",
                onSelected = {},
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, name = "NimazDropdownField — placeholder")
@Composable
private fun NimazDropdownFieldPlaceholderPreview() {
    NimazTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            NimazDropdownField(
                label = "Exemption Reason",
                items = listOf(
                    NimazDropdownItem("travel", "Travel"),
                    NimazDropdownItem("illness", "Illness"),
                ),
                selected = null,
                placeholder = "Select reason",
                onSelected = {},
            )
        }
    }
}
