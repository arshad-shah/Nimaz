package com.arshadshah.nimaz.presentation.components.molecules

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconWell
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconWellShape
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconWellSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazSwitch
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * Centralised expand/collapse ("accordion") card used across the app — most
 * notably to render every expandable row in the Help &amp; Support screen
 * (FAQs, troubleshooting steps, feature guides) from a single component.
 *
 * A tap on the header toggles [content]'s visibility. The trailing chevron
 * rotates 180° to signal state. Pass a [leadingIcon] for a small tinted accent
 * badge on the left; omit it for a plain header.
 *
 * The header can also carry a [subtitle] under the title and a [trailing] slot
 * before the chevron, so a row can summarise its own state without being opened
 * ("Adhan · 10 min before", plus the time and an enable switch). The header
 * itself is the expand/collapse target, so anything interactive placed in
 * [trailing] handles its own taps — a `NimazSwitch` there toggles without
 * expanding the body.
 *
 * @param title header text, always visible.
 * @param subtitle optional summary line under the title, always visible.
 * @param leadingIcon optional accent icon shown in a rounded badge.
 * @param trailing optional header content rendered between the title and the
 *   chevron, laid out in the header's [RowScope].
 * @param initiallyExpanded whether the body starts open.
 * @param content the collapsible body; laid out in a [ColumnScope].
 */
@Composable
fun NimazAccordion(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "accordion_chevron"
    )

    NimazCard(
        style = NimazCardStyle.FILLED,
        onClick = { expanded = !expanded },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leadingIcon != null) {
                    NimazIconWell(
                        icon = leadingIcon,
                        tone = NimazTone.ACCENT,
                        size = NimazIconWellSize.SMALL,
                        shape = NimazIconWellShape.ROUNDED
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (trailing != null) {
                    Spacer(modifier = Modifier.width(12.dp))
                    trailing()
                    Spacer(modifier = Modifier.width(4.dp))
                }
                NimazIcon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    variant = NimazIconVariant.MUTED,
                    modifier = Modifier.rotate(chevronRotation)
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    content = content
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Accordion — collapsed")
@Composable
private fun NimazAccordion_Collapsed_Preview() {
    NimazTheme {
        NimazAccordion(
            title = "How are prayer times calculated?",
            leadingIcon = Icons.AutoMirrored.Filled.HelpOutline,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Nimaz calculates prayer times from your saved location and the calculation method you choose.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Accordion — expanded")
@Composable
private fun NimazAccordion_Expanded_Preview() {
    NimazTheme {
        NimazAccordion(
            title = "How are prayer times calculated?",
            initiallyExpanded = true,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Nimaz calculates prayer times from your saved location and the calculation method you choose.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// The subtitle + trailing configuration the prayer rows are built from: the header
// has to carry the name, its summary, the prayer time and the enable switch without
// crowding, which is the first thing that breaks at a large font scale.

@Preview(showBackground = true, widthDp = 400, name = "Accordion — subtitle + trailing")
@Composable
private fun NimazAccordion_Trailing_Preview() {
    NimazTheme {
        NimazAccordion(
            title = "Fajr",
            subtitle = "Adhan · 10 min before",
            trailing = {
                Text(
                    text = "05:12",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                NimazSwitch(checked = true, onCheckedChange = {})
            },
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Alert style and reminder settings for Fajr.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(
    showBackground = true,
    widthDp = 400,
    name = "Accordion — subtitle + trailing (dark)",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun NimazAccordion_Trailing_Dark_Preview() {
    NimazTheme {
        NimazAccordion(
            title = "Maghrib",
            subtitle = "Notification only · no reminder",
            initiallyExpanded = true,
            trailing = {
                Text(
                    text = "20:47",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                NimazSwitch(checked = false, onCheckedChange = {})
            },
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Alert style and reminder settings for Maghrib.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
