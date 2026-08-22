package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.AyahReference
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazPalette
import com.arshadshah.nimaz.presentation.theme.NimazToneColors

/** What the ayah sheet can do, in one bundle so the host wires them once. */
data class AyahSheetActions(
    val onPlayFromHere: () -> Unit,
    val onRepeatAyah: () -> Unit,
    val onBookmark: () -> Unit,
    val onFavourite: () -> Unit,
    val onNote: () -> Unit,
    val onTafseer: () -> Unit,
    val onSubjects: () -> Unit,
    val onCopy: () -> Unit,
    val onShare: () -> Unit,
    val onMarkReadForKhatam: () -> Unit,
)

/**
 * Everything you can do to one verse, on request.
 *
 * Every ayah used to carry a permanent five-icon pill, so a screenful of six verses drew thirty
 * icons and the reader read around them. The sheet holds **ten** actions — more than the pill
 * ever offered, including the note editor and the subject index that had no home in the list at
 * all — and costs nothing until it is asked for.
 *
 * **Actions only**, as a two-column [NimazSheetActionGrid]. It used to reprint the verse and its
 * translation at the top; the reader tapped that verse to get here and it is still on the screen
 * behind the sheet, so the copy pushed the actions down — off the first screenful entirely on a
 * long verse — to confirm something the header's reference already states. Five icon-pills to a
 * row also left about 64dp per label, which is where "Unbookmark" started ellipsising.
 *
 * "Mark read for khatam" is drawn only while a khatam is active, matching the gate
 * `QuranMushafPageBar` already applies to the page-level mark: most reading is not part of a
 * plan and should carry no tracking chrome.
 *
 * Bookmark and favourite are **toggles** showing their current state, so a reader can undo from
 * the same place they did it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AyahActionSheet(
    reference: AyahReference,
    juzNumber: Int,
    pageNumber: Int,
    isBookmarked: Boolean,
    isFavourite: Boolean,
    isKhatamActive: Boolean,
    actions: AyahSheetActions,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NimazBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = reference.format(),
        subtitle = stringResource(R.string.juz_page_dot_format, juzNumber, pageNumber),
        icon = Icons.AutoMirrored.Filled.MenuBook,
        onClose = onDismiss,
    ) {
        // Actions only. The verse itself is on the screen behind the sheet — the reader tapped
        // it to get here — so reprinting it pushed the actions down and, on a long verse with a
        // translation, off the first screenful entirely. The header's reference
        // ("Al-Anfal 8:11 · Juz 9 · Page 178") is enough to confirm which verse this is.
        NimazSheetActionGrid(
            actions = listOfNotNull(
                NimazSheetAction(
                    icon = Icons.Default.PlayArrow,
                    label = stringResource(R.string.ayah_action_play_from_here),
                    onClick = actions.onPlayFromHere,
                    tint = MaterialTheme.colorScheme.primary,
                ),
                NimazSheetAction(
                    icon = Icons.Default.Repeat,
                    label = stringResource(R.string.ayah_action_repeat),
                    onClick = actions.onRepeatAyah,
                ),
                // Bookmark and favourite keep the colours they carried on the pill this sheet
                // replaced — gold for a mark, red for a heart. They are the two actions a reader
                // looks for by colour rather than by reading the label, and a grid of identical
                // grey tiles takes that away.
                NimazSheetAction(
                    icon = if (isBookmarked) Icons.Default.Bookmark
                    else Icons.Default.BookmarkBorder,
                    label = stringResource(
                        if (isBookmarked) R.string.ayah_action_unbookmark
                        else R.string.ayah_action_bookmark
                    ),
                    onClick = actions.onBookmark,
                    selected = isBookmarked,
                    tint = NimazColors.QuranColors.BookmarkPrimary,
                ),
                NimazSheetAction(
                    icon = if (isFavourite) Icons.Default.Favorite
                    else Icons.Default.FavoriteBorder,
                    label = stringResource(
                        if (isFavourite) R.string.ayah_action_unfavourite
                        else R.string.ayah_action_favourite
                    ),
                    onClick = actions.onFavourite,
                    selected = isFavourite,
                    tint = NimazPalette.Red500,
                ),
                NimazSheetAction(
                    icon = Icons.Default.EditNote,
                    label = stringResource(R.string.ayah_action_note),
                    onClick = actions.onNote,
                ),
                NimazSheetAction(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    label = stringResource(R.string.ayah_action_tafseer),
                    onClick = actions.onTafseer,
                ),
                NimazSheetAction(
                    icon = Icons.Default.AccountTree,
                    label = stringResource(R.string.ayah_action_subjects),
                    onClick = actions.onSubjects,
                ),
                NimazSheetAction(
                    icon = Icons.Default.ContentCopy,
                    label = stringResource(R.string.ayah_action_copy),
                    onClick = actions.onCopy,
                ),
                NimazSheetAction(
                    icon = Icons.Default.Share,
                    label = stringResource(R.string.ayah_action_share),
                    onClick = actions.onShare,
                ),
                // Only with a plan to mark it against, and across the full width: it is the one
                // action here that changes a record rather than the view.
                NimazSheetAction(
                    icon = Icons.Default.CheckCircle,
                    label = stringResource(R.string.ayah_action_mark_read),
                    onClick = actions.onMarkReadForKhatam,
                    tint = NimazToneColors.foreground(NimazTone.SUCCESS),
                    wide = true,
                ).takeIf { isKhatamActive },
            )
        )
    }
}
