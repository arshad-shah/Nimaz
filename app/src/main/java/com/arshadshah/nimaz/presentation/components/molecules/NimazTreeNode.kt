package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * One node of an expandable tree: a card carrying a label, an optional count,
 * an optional expand chevron, and its children behind an indent rail.
 *
 * The node takes **two** callbacks on purpose. A subject tree has two distinct
 * actions on one row — open this subject, and show what is under it — and which
 * one the row as a whole should perform is still being decided. Supplying both
 * puts expansion on the chevron and navigation on the label; supplying only
 * [onToggleExpand] makes the whole row a toggle.
 *
 * [content] is only composed while [expanded], so a deep tree costs nothing
 * until it is opened.
 */
@Composable
fun NimazTreeNode(
    label: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
    expanded: Boolean = false,
    onToggleExpand: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    content: (@Composable () -> Unit)? = null,
) {
    val rowToggles = onToggleExpand != null && onClick == null

    Column(modifier = modifier.fillMaxWidth()) {
        NimazCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = if (rowToggles) onToggleExpand else null,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onToggleExpand != null) {
                    val rotation by animateFloatAsState(
                        targetValue = if (expanded) 90f else 0f,
                        animationSpec = tween(180),
                        label = "tree_chevron",
                    )
                    val description = stringResource(
                        if (expanded) R.string.tree_node_collapse else R.string.tree_node_expand,
                        label,
                    )
                    NimazIcon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = description,
                        modifier = Modifier
                            .rotate(rotation)
                            .then(
                                if (rowToggles) Modifier
                                else Modifier.clickable(onClick = onToggleExpand)
                            ),
                    )
                    Spacer(Modifier.width(10.dp))
                } else {
                    Spacer(Modifier.width(26.dp))
                }

                Text(
                    text = label,
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (onClick != null) Modifier.clickable(onClick = onClick)
                            else Modifier
                        ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )

                if (count != null) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (expanded && content != null) {
            Row(modifier = Modifier.padding(start = 16.dp, top = 6.dp)) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    content()
                }
            }
        }
    }
}

@Preview(name = "Tree node · light")
@Preview(name = "Tree node · dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NimazTreeNodePreview() {
    NimazTheme {
        Column(Modifier.padding(16.dp)) {
            NimazTreeNode(
                label = "Doctrine",
                count = 214,
                expanded = true,
                onToggleExpand = {},
                onClick = {},
            ) {
                NimazTreeNode(label = "God", count = 96, onToggleExpand = {}, onClick = {})
                Spacer(Modifier.height(6.dp))
                NimazTreeNode(label = "The hereafter", count = 54, onClick = {})
            }
        }
    }
}
