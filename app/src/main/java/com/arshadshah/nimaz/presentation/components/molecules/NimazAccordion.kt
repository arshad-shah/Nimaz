package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
 * @param title header text, always visible.
 * @param leadingIcon optional accent icon shown in a rounded badge.
 * @param initiallyExpanded whether the body starts open.
 * @param content the collapsible body; laid out in a [ColumnScope].
 */
@Composable
fun NimazAccordion(
    title: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
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
        modifier = modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
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
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
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
