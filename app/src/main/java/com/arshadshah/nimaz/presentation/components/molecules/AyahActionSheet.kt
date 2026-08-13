package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.AyahReference
import com.arshadshah.nimaz.domain.model.TranslationLanguage
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.components.atoms.NimazToneColors
import com.arshadshah.nimaz.presentation.theme.asTranslationText

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
    arabic: String,
    translation: String?,
    juzNumber: Int,
    pageNumber: Int,
    isBookmarked: Boolean,
    isFavourite: Boolean,
    isKhatamActive: Boolean,
    actions: AyahSheetActions,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    translationLanguage: TranslationLanguage = TranslationLanguage.ENGLISH,
) {
    NimazBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = reference.format(),
        subtitle = stringResource(R.string.juz_page_dot_format, juzNumber, pageNumber),
        icon = Icons.AutoMirrored.Filled.MenuBook,
        onClose = onDismiss,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // The verse the sheet is acting on. Without it a reader who opened the sheet from a
            // list of six near-identical rows has no way to check they tapped the right one.
            NimazSheetPreviewCard {
                ArabicText(text = arabic, size = ArabicTextSize.MEDIUM)
                translation?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium
                            .asTranslationText(translationLanguage),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            NimazSheetActionRow(
                actions = listOf(
                    NimazSheetAction(
                        icon = Icons.Default.PlayArrow,
                        label = stringResource(R.string.ayah_action_play_from_here),
                        onClick = actions.onPlayFromHere,
                    ),
                    NimazSheetAction(
                        icon = Icons.Default.Repeat,
                        label = stringResource(R.string.ayah_action_repeat),
                        onClick = actions.onRepeatAyah,
                    ),
                    NimazSheetAction(
                        icon = if (isBookmarked) Icons.Default.Bookmark
                        else Icons.Default.BookmarkBorder,
                        label = stringResource(
                            if (isBookmarked) R.string.ayah_action_unbookmark
                            else R.string.ayah_action_bookmark
                        ),
                        onClick = actions.onBookmark,
                        selected = isBookmarked,
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
                    ),
                    NimazSheetAction(
                        icon = Icons.Default.EditNote,
                        label = stringResource(R.string.ayah_action_note),
                        onClick = actions.onNote,
                    ),
                )
            )

            NimazSheetActionRow(
                actions = listOfNotNull(
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
                    // Only with a plan to mark it against.
                    NimazSheetAction(
                        icon = Icons.Default.CheckCircle,
                        label = stringResource(R.string.ayah_action_mark_read),
                        onClick = actions.onMarkReadForKhatam,
                        tint = NimazToneColors.foreground(NimazTone.SUCCESS),
                    ).takeIf { isKhatamActive },
                )
            )
        }
    }
}
