package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.presentation.components.atoms.BottomSheetHandle
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonType
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconVariant
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * # The Nimaz bottom-sheet kit
 *
 * One opinionated modal sheet plus a small set of in-sheet building blocks, so
 * every sheet in the app speaks the same visual language instead of each screen
 * hand-rolling its own header / preview card / action row.
 *
 * Anatomy (all parts optional except the body):
 * ```
 * ┌──────────────────────────┐
 * │          ──              │  drag handle
 * │  [icon]  Title    [badge]│  header slot  (NimazSheetHeader)
 * │          Subtitle    [✕] │
 * │                          │
 * │   …scroll-aware body…    │  content slot  (your composable)
 * │                          │
 * ├──────────────────────────┤
 * │  [Secondary]  [Primary]  │  footer slot   (NimazSheetFooterButtons)
 * └──────────────────────────┘
 * ```
 *
 * Building blocks for the body: [NimazSheetPreviewCard], [NimazSheetSectionLabel],
 * [NimazSheetActionRow].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NimazBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    shape: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    tonalElevation: Dp = BottomSheetDefaults.Elevation,
    showDragHandle: Boolean = true,
    // Header slot — render a standard header when a title is supplied.
    title: String? = null,
    subtitle: String? = null,
    icon: ImageVector? = null,
    badge: String? = null,
    onClose: (() -> Unit)? = null,
    // Body behaviour
    scrollable: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp),
    // Footer slot — pinned below the (scrolling) body.
    footer: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        dragHandle = if (showDragHandle) {
            {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BottomSheetHandle()
                }
            }
        } else null
    ) {
        if (title != null) {
            NimazSheetHeader(
                title = title,
                subtitle = subtitle,
                icon = icon,
                badge = badge,
                onClose = onClose
            )
        }

        // Body: takes the remaining space without forcing the sheet taller than
        // its content (fill = false). Tall content scrolls; the footer stays put.
        val bodyModifier = Modifier
            .fillMaxWidth()
            .weight(1f, fill = false)
            .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
            .padding(contentPadding)

        Column(modifier = bodyModifier, content = content)

        if (footer != null) {
            NimazSheetFooterContainer(content = footer)
        } else {
            Box(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(8.dp)
            )
        }
    }
}

/**
 * Standard "minimal soft" header: a rounded icon chip, a title/subtitle stack,
 * an optional pill badge and an optional close button — all on the plain sheet
 * surface. Every field except [title] is optional.
 */
@Composable
fun NimazSheetHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    badge: String? = null,
    onClose: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                NimazIcon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    iconSize = 22.dp
                )
            }
            Spacer(Modifier.width(12.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (badge != null) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        if (onClose != null) {
            if (badge != null) Spacer(Modifier.width(8.dp))
            Surface(
                onClick = onClose,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                NimazIcon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.cd_close),
                    variant = NimazIconVariant.MUTED,
                    iconSize = 18.dp,
                    modifier = Modifier
                        .padding(7.dp)
                )
            }
        }
    }
}

/**
 * Soft-filled rounded surface used to set off a preview block (Arabic text,
 * translation, a stat group …) inside a sheet body.
 */
@Composable
fun NimazSheetPreviewCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    shape: Shape = RoundedCornerShape(14.dp),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = shape,
        color = color,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

/**
 * Small accent label that introduces a section of the sheet body
 * (e.g. "Translation", "Transliteration", "Reason").
 */
@Composable
fun NimazSheetSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = modifier.padding(bottom = 6.dp)
    )
}

/**
 * A single action in a [NimazSheetActionRow] or a [NimazSheetActionGrid].
 *
 * @param wide grid only: give this action a row of its own instead of half of one. For the odd
 *   action that is longer, rarer or more consequential than its neighbours.
 */
data class NimazSheetAction(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
    val tint: Color? = null,
    val selected: Boolean = false,
    val wide: Boolean = false
)

/**
 * Actions as a **two-column grid** of wide pills — icon beside label, not above it.
 *
 * [NimazSheetActionRow] divides the width by the number of actions, so five across a 360dp
 * screen leaves each about 64dp for a label: "Unbookmark" ellipsises, and the labels crowd. This
 * gives each action half the width and reads the label on one line at any translation.
 *
 * Actions keep their given order. A [NimazSheetAction.wide] action takes a full row; the ones
 * around it pair up as normal.
 */
@Composable
fun NimazSheetActionGrid(
    actions: List<NimazSheetAction>,
    modifier: Modifier = Modifier
) {
    // Grouped into rows here rather than with a LazyVerticalGrid: the sheet body already
    // scrolls, and nesting a scrollable in it is a measurement error rather than a style choice.
    val rows = buildList {
        var pending: NimazSheetAction? = null
        actions.forEach { action ->
            when {
                action.wide -> {
                    pending?.let { add(listOf(it)) }
                    pending = null
                    add(listOf(action))
                }

                pending == null -> pending = action
                else -> {
                    add(listOf(pending!!, action))
                    pending = null
                }
            }
        }
        pending?.let { add(listOf(it)) }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { action ->
                    ActionTile(action = action, modifier = Modifier.weight(1f))
                }
                // A lone action in a two-column row keeps its half rather than stretching
                // across, so the grid stays a grid on an odd count.
                if (row.size == 1 && !row.first().wide) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ActionTile(
    action: NimazSheetAction,
    modifier: Modifier = Modifier
) {
    val contentColor = when {
        action.tint != null -> action.tint
        action.selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Surface(
        onClick = action.onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (action.selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NimazIcon(
                imageVector = action.icon,
                contentDescription = null,
                tint = contentColor,
                size = NimazIconSize.MEDIUM
            )
            Text(
                text = action.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Row of labelled icon-pill actions (Play / Bookmark / Share …). Up to five
 * actions space evenly across the width; six or more scroll horizontally so the
 * pills keep a comfortable touch size instead of being squeezed.
 */
@Composable
fun NimazSheetActionRow(
    actions: List<NimazSheetAction>,
    modifier: Modifier = Modifier
) {
    val scrolls = actions.size > 5
    val rowModifier = modifier
        .fillMaxWidth()
        .then(if (scrolls) Modifier.horizontalScroll(rememberScrollState()) else Modifier)

    Row(
        modifier = rowModifier,
        horizontalArrangement = if (scrolls) Arrangement.spacedBy(4.dp) else Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        actions.forEach { action ->
            ActionPill(action = action)
        }
    }
}

@Composable
private fun ActionPill(
    action: NimazSheetAction,
    modifier: Modifier = Modifier
) {
    val tint = action.tint ?: MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        onClick = action.onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (action.selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                NimazIcon(
                    imageVector = action.icon,
                    contentDescription = action.label,
                    tint = tint,
                    size = NimazIconSize.LARGE
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = action.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Pinned footer container: a hairline top divider and navigation-bar padding so
 * footer buttons clear the system gesture area. Use directly, or via the
 * [NimazSheetFooterButtons] convenience.
 */
@Composable
internal fun NimazSheetFooterContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            content()
        }
    }
}

/**
 * Standard footer button pair: a filled-tonal primary (or error-filled when
 * [isDestructive]), with an optional outlined secondary to its left. Both are
 * full-pill shaped to match the app's button language. Mirrors the hierarchy of
 * [com.arshadshah.nimaz.presentation.components.molecules.NimazDialogConfirmButton]
 * so sheets and dialogs read the same. Drop into [NimazBottomSheet]'s `footer` slot.
 */
@Composable
fun NimazSheetFooterButtons(
    primaryText: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryText: String? = null,
    onSecondary: (() -> Unit)? = null,
    primaryEnabled: Boolean = true,
    isDestructive: Boolean = false
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (secondaryText != null && onSecondary != null) {
            NimazButton(
                text = secondaryText,
                onClick = onSecondary,
                variant = NimazButtonVariant.OUTLINED,
                type = NimazButtonType.PILL,
                modifier = Modifier.weight(1f)
            )
        }
        NimazButton(
            text = primaryText,
            onClick = onPrimary,
            enabled = primaryEnabled,
            variant = if (isDestructive) NimazButtonVariant.DESTRUCTIVE else NimazButtonVariant.TONAL,
            type = NimazButtonType.PILL,
            modifier = Modifier.weight(1f)
        )
    }
}

// ==================== PREVIEWS ====================

@Preview(showBackground = true, name = "Sheet header — full")
@Composable
private fun NimazSheetHeaderPreview() {
    NimazTheme {
        Surface {
            NimazSheetHeader(
                title = "Al-Fatihah",
                subtitle = "Ayah 2",
                icon = Icons.Default.PlayArrow,
                badge = "Juz 1",
                onClose = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Preview card + section label")
@Composable
private fun NimazSheetPreviewCardPreview() {
    NimazTheme {
        Surface {
            Column(Modifier.padding(20.dp)) {
                NimazSheetSectionLabel(text = "Translation")
                NimazSheetPreviewCard {
                    Text(
                        "All praise is due to Allah, Lord of the worlds.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Action row")
@Composable
private fun NimazSheetActionRowPreview() {
    NimazTheme {
        Surface {
            NimazSheetActionRow(
                actions = listOf(
                    NimazSheetAction(
                        Icons.Default.PlayArrow,
                        "Play",
                        {},
                        tint = MaterialTheme.colorScheme.primary
                    ),
                    NimazSheetAction(Icons.Default.Share, "Share", {}),
                    NimazSheetAction(Icons.Default.Close, "Close", {}, selected = true)
                ),
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }
    }
}

@Preview(showBackground = true, name = "Footer buttons")
@Composable
private fun NimazSheetFooterButtonsPreview() {
    NimazTheme {
        Surface {
            NimazSheetFooterContainer {
                NimazSheetFooterButtons(
                    primaryText = "Confirm",
                    onPrimary = {},
                    secondaryText = "Cancel",
                    onSecondary = {}
                )
            }
        }
    }
}
