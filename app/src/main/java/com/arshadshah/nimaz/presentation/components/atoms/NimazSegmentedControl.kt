package com.arshadshah.nimaz.presentation.components.atoms

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.NimazToneColors
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

/**
 * How the tray divides its width.
 *
 * [FILL] gives every cell an equal share of the full width — the right shape for a control that
 * owns its row. [WRAP] sizes each cell to its own label, for a control sharing a row with
 * something else (a title, a spacer, an action) or one that scrolls horizontally because it
 * carries more labels than fit.
 */
enum class NimazSegmentedWidth(internal val horizontalPadding: Dp) {
    FILL(4.dp),
    WRAP(16.dp)
}

/**
 * What a tap means, which is what TalkBack announces.
 *
 * [VALUE] picks a value and reads as a radio button. [VIEW] switches which content is shown and
 * reads as a tab. The distinction is not cosmetic: announcing "tab" for a control that records a
 * fast, or "selected" for one that swaps a list, both mislead.
 */
enum class NimazSegmentedPurpose(internal val role: Role) {
    VALUE(Role.RadioButton),
    VIEW(Role.Tab)
}

/** How long a cell takes to take on (or give up) its selected colours. */
private const val SelectionAnimationMillis = 180

/** The disabled-content alpha Material 3's own `ButtonDefaults` apply. */
private const val DisabledContentAlpha = 0.38f

/** How far the selected cell lifts out of the tray. */
private val SelectedLift = 2.dp

/** Convenience for the common case: plain labels, all taking the same selected tone. */
fun List<String>.asSegments(tone: NimazTone = NimazTone.ACCENT): List<NimazSegmentedOption> =
    map { NimazSegmentedOption(label = it, selectedTone = tone) }

/**
 * The house segmented control: a recessed tray with the selected cell **lifted** out of it as a
 * raised pill.
 *
 * The lift, not the hue, carries the selection. That matters because several of these can appear
 * on one screen, and spending the brand colour on every one of them leaves nothing to mark the
 * actual accent — which is why the filled-primary pill this replaced was retired.
 *
 * [selectedIndex] is nullable, and that is the point: "nothing chosen yet" is a real state — a
 * day with no fast record — and a boolean toggle cannot express it. An index outside the list
 * selects nothing too, so a caller need not bounds-check before passing one.
 *
 * @param options the cells, in reading order. Beyond four, use a picker — or [NimazSegmentedWidth.WRAP]
 *   plus a horizontal scroll if the labels genuinely belong in one row.
 * @param selectedIndex the chosen cell, or `null` when nothing is chosen.
 * @param onSelect invoked with the tapped index — **including when it is already selected**, so
 *   callers can implement tap-to-clear. Deciding what a repeat tap means belongs to the caller,
 *   not to a control that cannot know whether clearing is legal.
 * @param enabled when false, dims the content and drops the lift, since a floating shadow reads
 *   as interactive. The selected cell keeps its fill so the choice stays legible while inert.
 */
@Composable
fun NimazSegmentedControl(
    options: List<NimazSegmentedOption>,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    size: NimazSegmentedSize = NimazSegmentedSize.MEDIUM,
    width: NimazSegmentedWidth = NimazSegmentedWidth.FILL,
    purpose: NimazSegmentedPurpose = NimazSegmentedPurpose.VALUE,
    enabled: Boolean = true,
) {
    val trackShape = RoundedCornerShape(15.dp)
    val cellShape = RoundedCornerShape(12.dp)

    Row(
        modifier = modifier
            .then(if (width == NimazSegmentedWidth.FILL) Modifier.fillMaxWidth() else Modifier)
            .clip(trackShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { index, option ->
            // Equality, not a bounds check: an index outside the list matches no cell and so
            // selects none, which is what a "nothing chosen yet" caller wants. Keep it
            // equality-based — an index lookup here would turn that case into a crash.
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
            val lift by animateDpAsState(
                targetValue = if (selected && enabled) SelectedLift else 0.dp,
                animationSpec = tween(SelectionAnimationMillis),
                label = "segment_lift"
            )
            val resolvedContent = resolveSegmentContentColor(contentColor, enabled)

            Surface(
                modifier = Modifier
                    .then(
                        if (width == NimazSegmentedWidth.FILL) Modifier.weight(1f) else Modifier
                    )
                    // Clipped *before* selectable, or the ripple is a square over a rounded
                    // pill: `Surface(shape)` clips its own drawing, not the indication of a
                    // modifier applied outside it.
                    .clip(cellShape)
                    .selectable(
                        selected = selected,
                        enabled = enabled,
                        role = purpose.role,
                        onClick = { onSelect(index) }
                    ),
                shape = cellShape,
                color = background,
                contentColor = resolvedContent,
                shadowElevation = lift,
            ) {
                Column(
                    modifier = Modifier.padding(
                        vertical = size.verticalPadding,
                        horizontal = width.horizontalPadding,
                    ),
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
                        maxLines = 1,
                        // A label that does not fit is ellipsised rather than wrapped, which
                        // keeps the tray one row high whatever the translation does to it.
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * A cell's content colour, folding in the disabled fade.
 *
 * Matches the disabled-content alpha Material 3's own [androidx.compose.material3.ButtonDefaults]
 * apply (and that [com.arshadshah.nimaz.presentation.components.molecules.NimazNumberStepper]
 * mirrors by hand), so a disabled segmented control reads exactly like a disabled [NimazButton].
 */
internal fun resolveSegmentContentColor(base: Color, enabled: Boolean): Color =
    if (enabled) base else base.copy(alpha = base.alpha * DisabledContentAlpha)

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

        ShowcaseLabel("Switching a view, sized to its labels")
        NimazSegmentedControl(
            options = listOf("Outline", "By kind", "Index").asSegments(),
            selectedIndex = 0,
            onSelect = {},
            width = NimazSegmentedWidth.WRAP,
            purpose = NimazSegmentedPurpose.VIEW,
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
