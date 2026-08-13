package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * The house segmented control: a recessed tray with the selected segment
 * **lifted** out of it as a raised pill.
 *
 * Deliberately not a filled-primary pill (which is what [NimazPillTabs] does).
 * This control appears several times on some screens, and spending the brand
 * colour on every one of them leaves nothing to mark the actual accent. The
 * lift, not the hue, carries the selection.
 *
 * Segments share the width equally, so keep labels short; a label that does not
 * fit is ellipsised rather than wrapped, which keeps the tray one row high.
 */
@Composable
fun NimazSegmentedTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            tabs.forEachIndexed { index, label ->
                val selected = index == selectedIndex

                val container by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        Color.Transparent
                    },
                    animationSpec = tween(180),
                    label = "segment_container",
                )
                val content by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    animationSpec = tween(180),
                    label = "segment_content",
                )
                val lift by animateDpAsState(
                    targetValue = if (selected) 2.dp else 0.dp,
                    animationSpec = tween(180),
                    label = "segment_lift",
                )

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .selectable(
                            selected = selected,
                            enabled = enabled,
                            role = Role.Tab,
                            onClick = { onTabSelect(index) },
                        ),
                    shape = RoundedCornerShape(11.dp),
                    color = container,
                    contentColor = content,
                    shadowElevation = lift,
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Preview(name = "Segmented tabs · light")
@Preview(name = "Segmented tabs · dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NimazSegmentedTabsPreview() {
    NimazTheme {
        NimazSegmentedTabs(
            tabs = listOf("Outline", "By kind", "Index"),
            selectedIndex = 0,
            onTabSelect = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
