package com.arshadshah.nimaz.presentation.components.molecules

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconButtonSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcons
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.foundation.debug.NimazMarginRule
import com.arshadshah.nimaz.presentation.foundation.debug.nimazMarginRules
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * One node of a tree that expands in place.
 *
 * The row has **two** targets and they do different things: the disclosure control opens the
 * node's children beneath it, and the label opens the node itself. Deciding between the two
 * from the data — descend if it has children, open if it doesn't — is what made most rows of
 * the subject browser a dead tap, because most subjects are leaves and a leaf's tap went
 * nowhere. A leaf here has no disclosure control at all, just a mark holding the column, and
 * its label opens like every other label.
 *
 * [depth] draws the indent and its ruling. Cap it at the call site: past three levels a 390dp
 * screen has no text column left, which is what [trailingContent] ("Focus this branch") is for.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NimazTreeRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    depth: Int = 0,
    secondaryLabel: String? = null,
    supportingText: String? = null,
    badgeText: String? = null,
    expandable: Boolean = false,
    expanded: Boolean = false,
    onToggleExpanded: () -> Unit = {},
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val step = NimazMarginRule.IndentStep
    val ruleColor = NimazMarginRule.color
    val indent = step * depth
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        label = "disclosure",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .nimazMarginRules(
                count = depth,
                color = ruleColor,
                start = DisclosureSize / 2,
                step = step,
                rtl = rtl,
            ),
    ) {
        NimazCard(
            style = NimazCardStyle.FILLED,
            tone = NimazTone.TRANSPARENT,
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            elevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = indent),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (expandable) {
                    NimazIconButton(
                        icon = NimazIcons.Forward,
                        onClick = onToggleExpanded,
                        size = NimazIconButtonSize.MEDIUM,
                        contentDescription = stringResource(
                            if (expanded) R.string.cd_tree_collapse else R.string.cd_tree_expand,
                            label,
                        ),
                        modifier = Modifier.rotate(rotation),
                    )
                } else {
                    // A leaf keeps the column, so the text edge does not wander between
                    // siblings — but it is a mark, not a control, and cannot be tapped.
                    Box(
                        modifier = Modifier.size(DisclosureSize),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(LeafDotSize)
                                .background(ruleColor, RoundedCornerShape(50)),
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (secondaryLabel != null) {
                            Text(
                                text = secondaryLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                    if (supportingText != null) {
                        Text(
                            text = supportingText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                if (badgeText != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    NimazBadge(
                        text = badgeText,
                        tone = NimazTone.ACCENT,
                        size = NimazBadgeSize.SMALL,
                    )
                }
                if (trailingContent != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    trailingContent()
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

private val DisclosureSize = 40.dp
private val LeafDotSize = 5.dp

@Preview(showBackground = true, widthDp = 390, name = "NimazTreeRow — Light")
@Composable
private fun NimazTreeRowLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { TreeRowSample() }
}

@Preview(
    showBackground = true, widthDp = 390, name = "NimazTreeRow — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun NimazTreeRowDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) { TreeRowSample() }
}

@Composable
private fun TreeRowSample() {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        NimazTreeRow(
            label = "Doctrine",
            secondaryLabel = "العقيدة",
            badgeText = "412",
            expandable = true,
            expanded = true,
            onClick = {},
        )
        NimazTreeRow(
            label = "God",
            secondaryLabel = "الله",
            badgeText = "153",
            depth = 1,
            expandable = true,
            onClick = {},
        )
        NimazTreeRow(label = "Mercy", badgeText = "71", depth = 1, onClick = {})
        NimazTreeRow(
            label = "Patience in adversity",
            supportingText = "Doctrine · States of the heart",
            badgeText = "14",
            depth = 2,
            onClick = {},
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}
