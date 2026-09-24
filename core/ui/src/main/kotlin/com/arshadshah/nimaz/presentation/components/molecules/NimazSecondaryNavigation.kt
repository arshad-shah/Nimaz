package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Local feature destinations, separate from the app's primary navigation. */
data class NimazSecondaryDestination(val label: String, val icon: ImageVector)

@Composable
fun NimazSecondaryNavigation(
    destinations: List<NimazSecondaryDestination>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp, shadowElevation = 4.dp,
    ) {
        Row(Modifier.selectableGroup().padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            destinations.forEachIndexed { index, destination ->
                val selected = selectedIndex == index
                val fill by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                    label = "destination",
                )
                val foreground = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(22.dp)).background(fill)
                        .selectable(selected, role = Role.Tab, onClick = { onSelect(index) })
                        .heightIn(min = 64.dp).padding(horizontal = 2.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(destination.icon, null, Modifier.size(22.dp), tint = foreground)
                    Text(destination.label, style = MaterialTheme.typography.labelSmall, color = foreground, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
