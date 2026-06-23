package com.arshadshah.nimaz.presentation.components.molecules

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconVariant
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * A single selectable row used inside Nimaz dropdown/popup menus.
 *
 * This is the design-system replacement for Material's stock
 * [androidx.compose.material3.DropdownMenuItem]. It mirrors the selection
 * visuals of [NimazListPicker]'s rows (primary-container fill, accent border
 * and a circular check badge when selected) so menus look like the rest of the
 * component set instead of a plain Material list.
 *
 * - [text]: the option label.
 * - [selected]: highlights the row and shows the trailing check badge.
 * - [leadingIcon]: optional icon shown before the label.
 * - [textFontFamily]: optional typeface for the label. Handy for font pickers
 *   where each option should be rendered in the font it represents.
 * - [description]: optional secondary line beneath the label.
 */
@Composable
fun NimazDropdownMenuItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    textFontFamily: FontFamily? = null,
    description: String? = null,
    enabled: Boolean = true,
) {
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
                tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
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
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface
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
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                NimazIcon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    variant = NimazIconVariant.ON_ACCENT,
                    iconSize = 14.dp
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 320, name = "NimazDropdownMenuItem")
@Composable
private fun NimazDropdownMenuItemPreview() {
    NimazTheme {
        Column(modifier = Modifier.padding(8.dp)) {
            NimazDropdownMenuItem(
                text = "Amiri",
                selected = true,
                onClick = {},
            )
            NimazDropdownMenuItem(
                text = "Scheherazade New",
                selected = false,
                onClick = {},
            )
        }
    }
}
