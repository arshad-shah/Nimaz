package com.arshadshah.nimaz.presentation.components.organisms

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.em
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.components.atoms.QuranOrnamentalDivider
import com.arshadshah.nimaz.presentation.components.molecules.NimazDropdownMenu
import com.arshadshah.nimaz.presentation.components.molecules.NimazDropdownRow

/**
 * The saved-item card family — one place for optional swipe-to-delete plumbing, the overflow
 * action menu and the card layout shared by the Bookmarks screen and the Quran Favourites
 * tab. Two features that store ayah/hadith/dua references render identically because they
 * lean on the same pieces here:
 *
 * - [SwipeToDeleteBox] — the end→start swipe-to-delete gesture and its error-tinted backdrop
 *   (used only when swipe deletion is enabled by the caller).
 * - [NimazOverflowMenu] — the `⋮` overflow button + anchored action menu, driven by a list
 *   of [NimazMenuAction]s.
 * - [SwipeableSavedCard] — the full card (swipe wrapper + header with a leading badge,
 *   relative timestamp and overflow menu + title/subtitle/Arabic/note body).
 */

/**
 * A single command in a [NimazOverflowMenu]. [destructive] tints irreversible actions
 * (delete, remove) with the error colour.
 */
data class NimazMenuAction(
    val text: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val destructive: Boolean = false,
)

/**
 * The app's `⋮` overflow control: a muted [Icons.Default.MoreVert] button that pops a
 * [NimazDropdownMenu] of [actions]. The menu closes itself before each action fires, so
 * callers only supply the work to do.
 */
@Composable
fun NimazOverflowMenu(
    actions: List<NimazMenuAction>,
    modifier: Modifier = Modifier,
    contentDescription: String? = stringResource(R.string.cd_more_options),
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(36.dp)) {
            NimazIcon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = contentDescription,
                variant = NimazIconVariant.MUTED,
                size = NimazIconSize.MEDIUM
            )
        }
        NimazDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            actions.forEach { action ->
                NimazDropdownRow(
                    text = action.text,
                    leadingIcon = action.icon,
                    destructive = action.destructive,
                    onClick = {
                        expanded = false
                        action.onClick()
                    },
                )
            }
        }
    }
}

/**
 * Wraps [content] in an end→start swipe-to-delete gesture. The swipe fires [onDelete]
 * immediately (the caller is expected to back it with an Undo snackbar) and then resets so
 * the row settles before the backing list flow removes it. Behind the row sits an
 * error-container backdrop with a trailing delete icon.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteBox(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundShape: Shape = RoundedCornerShape(16.dp),
    content: @Composable () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        initialValue = SwipeToDismissBoxValue.Settled,
        positionalThreshold = SwipeToDismissBoxDefaults.positionalThreshold
    )
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onDelete()
            dismissState.reset()
        }
    }
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer, backgroundShape)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                NimazIcon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        },
        content = { content() }
    )
}

/**
 * The shared saved-item card: an optional [SwipeToDeleteBox] around a [NimazCard] whose header
 * is a [leading] badge slot + relative "Added …" timestamp + [NimazOverflowMenu], followed by a
 * bold [title], an optional [subtitle], an optional gold-divided [arabicText] preview and an
 * optional italic [note] preview.
 *
 * @param title primary locator line (e.g. "Al-Fatihah · 1").
 * @param timestamp epoch millis the item was saved, rendered as a relative "Added …" label.
 * @param menuActions overflow-menu commands (share, edit note, delete/remove…).
 * @param onClick tap on the card body (typically navigates to the item).
 * @param onDelete fired by the swipe gesture when [enableSwipeToDelete] is true; back it with an
 * Undo snackbar.
 * @param enableSwipeToDelete whether to enable the end→start swipe-to-delete gesture.
 * @param leading the header badge slot, drawn at the start of the header row.
 */
@Composable
fun SwipeableSavedCard(
    title: String,
    timestamp: Long,
    menuActions: List<NimazMenuAction>,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    enableSwipeToDelete: Boolean = true,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    arabicText: String? = null,
    note: String? = null,
    /**
     * The kind's colour, drawn as a spine down the left edge and used for [kindLabel]. Null
     * leaves the card unmarked.
     */
    accent: Color? = null,
    /** The kind, set small and letter-spaced in [accent] under the title. */
    kindLabel: String? = null,
    leading: (@Composable () -> Unit)? = null,
) {
    val card: @Composable () -> Unit = {
        NimazCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = onClick,
            tone = NimazTone.NEUTRAL,
            elevation = 0.dp
        ) {
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // A spine in the kind's colour down the left edge, so a list of saved items is
            // scannable by colour before it is read — the same three colours the ayah sheet
            // uses for bookmark, favourite and note.
            accent?.let {
                Box(
                    modifier = Modifier
                        .padding(start = 12.dp, top = 14.dp, bottom = 14.dp)
                        .width(3.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(99.dp))
                        .background(it)
                )
            }
            Column(modifier = Modifier.padding(16.dp)) {
                // Header: leading badge + relative time + overflow.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    leading?.invoke()
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = stringResource(
                            R.string.added_format,
                            DateUtils.getRelativeTimeSpanString(
                                timestamp,
                                System.currentTimeMillis(),
                                DateUtils.DAY_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_RELATIVE
                            )
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    NimazOverflowMenu(actions = menuActions)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Title (locator) — bold.
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Source / subtitle — only when the caller supplies one.
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // What kind of save this is, in its own colour. It replaces the filled corpus
                // badge that used to lead the card: the corpus is the axis you filter by, the
                // kind is the one you are looking at.
                if (!kindLabel.isNullOrBlank() && accent != null) {
                    Text(
                        text = kindLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.07.em,
                        color = accent,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }

                // Arabic preview. No ornamental divider: a gold floret rule on every row turned
                // a list into a page of ornament, and the Arabic is already set apart by being
                // Arabic.
                if (!arabicText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ArabicText(
                        text = arabicText,
                        size = ArabicTextSize.SMALL,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Note preview.
                if (!note.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            }
        }
    }
    if (enableSwipeToDelete) {
        SwipeToDeleteBox(onDelete = onDelete, modifier = modifier) { card() }
    } else {
        Box(modifier = modifier) { card() }
    }
}
