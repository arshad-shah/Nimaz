package com.arshadshah.nimaz.presentation.components.atoms

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * One cell of a [NimazSegmentedControl].
 *
 * @param selectedTone the colour this cell takes **while selected** — cells may differ, which is
 *   the case that made the control necessary: "fasted" wants green while "exempt" wants amber.
 */
data class NimazSegmentedOption(
    val label: String,
    val icon: ImageVector? = null,
    val selectedTone: NimazTone = NimazTone.ACCENT,
    val contentDescription: String? = null,
)

/** Cell padding and glyph scale. */
enum class NimazSegmentedSize(
    internal val verticalPadding: Dp,
    internal val iconSize: Dp,
) {
    SMALL(8.dp, 16.dp),
    MEDIUM(11.dp, 19.dp)
}

/** How long a cell takes to take on (or give up) its selected colours. */
private const val SelectionAnimationMillis = 180

/**
 * A mutually-exclusive choice laid out as one inset row of cells.
 *
 * Distinct from [com.arshadshah.nimaz.presentation.components.organisms.NimazPillTabs], which
 * switches *views*, is text-only, and paints every selected tab `primary`. This chooses a
 * **value**, carries an icon per cell, and lets each cell own the colour it takes when selected.
 *
 * [selectedIndex] is nullable, and that is the point: "nothing chosen yet" is a real state — a
 * day with no fast record — and a boolean toggle cannot express it. The switch this replaced
 * showed such a day as explicitly "not fasting", which is a different claim.
 *
 * @param options the cells, left to right. Two or three; beyond four, use a picker.
 * @param selectedIndex the chosen cell, or `null` when nothing is chosen.
 * @param onSelect invoked with the tapped index — **including when it is already selected**, so
 *   callers can implement tap-to-clear. Deciding what a repeat tap means belongs to the caller,
 *   not to a control that cannot know whether clearing is legal.
 * @param enabled when false, dims the control and blocks selection.
 */
@Composable
fun NimazSegmentedControl(
    options: List<NimazSegmentedOption>,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    size: NimazSegmentedSize = NimazSegmentedSize.MEDIUM,
    enabled: Boolean = true,
) {
    val trackShape = RoundedCornerShape(15.dp)
    val cellShape = RoundedCornerShape(12.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(trackShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { index, option ->
            val selected = selectedIndex == index

            val background by animateColorAsState(
                targetValue = if (selected) {
                    MaterialTheme.colorScheme.surface
                } else {
                    Color.Transparent
                },
                animationSpec = tween(SelectionAnimationMillis),
                label = "segment_background"
            )
            val contentColor by animateColorAsState(
                targetValue = if (selected) {
                    NimazToneColors.foreground(option.selectedTone)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                animationSpec = tween(SelectionAnimationMillis),
                label = "segment_content"
            )
            val resolvedContent =
                if (enabled) contentColor else contentColor.copy(alpha = 0.38f)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(cellShape)
                    .background(background)
                    .selectable(
                        selected = selected,
                        enabled = enabled,
                        // RadioButton rather than Tab: this picks a value, and TalkBack should
                        // say "selected" rather than announce a view switch that never happens.
                        role = Role.RadioButton,
                        onClick = { onSelect(index) }
                    )
                    .padding(vertical = size.verticalPadding, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                option.icon?.let { icon ->
                    NimazIcon(
                        imageVector = icon,
                        contentDescription = option.contentDescription,
                        iconSize = size.iconSize,
                        tint = resolvedContent
                    )
                }
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = resolvedContent,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

// ==================== PREVIEWS ====================

private val previewOptions = listOf(
    NimazSegmentedOption("Fasted", Icons.Default.Check, NimazTone.SUCCESS),
    NimazSegmentedOption("Not fasting", Icons.Default.Clear, NimazTone.NEUTRAL),
    NimazSegmentedOption("Exempt", Icons.Default.Info, NimazTone.WARNING),
)

@Composable
private fun ShowcaseLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun NimazSegmentedControlShowcase() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ShowcaseLabel("Nothing selected — a day with no record")
        NimazSegmentedControl(previewOptions, selectedIndex = null, onSelect = {})

        ShowcaseLabel("Each cell selected, showing its own tone")
        previewOptions.indices.forEach { index ->
            NimazSegmentedControl(previewOptions, selectedIndex = index, onSelect = {})
        }

        ShowcaseLabel("Small")
        NimazSegmentedControl(
            previewOptions,
            selectedIndex = 0,
            onSelect = {},
            size = NimazSegmentedSize.SMALL
        )

        ShowcaseLabel("Disabled")
        NimazSegmentedControl(
            previewOptions,
            selectedIndex = 1,
            onSelect = {},
            enabled = false
        )

        ShowcaseLabel("Two cells, no icons")
        NimazSegmentedControl(
            listOf(
                NimazSegmentedOption("Owed", selectedTone = NimazTone.WARNING),
                NimazSegmentedOption("Settled", selectedTone = NimazTone.SUCCESS),
            ),
            selectedIndex = 0,
            onSelect = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 360, name = "NimazSegmentedControl — Light")
@Composable
private fun NimazSegmentedControlLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { NimazSegmentedControlShowcase() }
}

@Preview(
    showBackground = true, widthDp = 360, name = "NimazSegmentedControl — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun NimazSegmentedControlDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) { NimazSegmentedControlShowcase() }
}
