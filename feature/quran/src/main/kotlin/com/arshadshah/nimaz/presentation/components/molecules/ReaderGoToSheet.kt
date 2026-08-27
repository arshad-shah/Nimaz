package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazSegmentedControl
import com.arshadshah.nimaz.presentation.components.atoms.NimazSegmentedPurpose
import com.arshadshah.nimaz.presentation.components.atoms.NimazSegmentedSize
import com.arshadshah.nimaz.presentation.components.atoms.asSegments

/** What a "go to" is asking for. */
enum class ReaderGoToKind { VERSE, JUZ, PAGE }

/**
 * Jump to a place in the book.
 *
 * The anchor bar's target icon used to open the *passage outline*, which answers "what is this
 * surah about" — a good question, and not the one a control called "Go to" is asking. This
 * takes a number and moves the reader to it.
 *
 * Three kinds because the reader has three coordinates and a reader arrives with whichever one
 * they were given: a verse from a lesson, a juz from a plan, a page from a printed mushaf.
 *
 * @param maxVerse the surah's verse count; the Verse option is hidden when there is no surah on
 *   screen (juz and page modes span several).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderGoToSheet(
    maxVerse: Int,
    maxPage: Int,
    onGoToVerse: (Int) -> Unit,
    onGoToJuz: (Int) -> Unit,
    onGoToPage: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val kinds = buildList {
        if (maxVerse > 0) add(ReaderGoToKind.VERSE)
        add(ReaderGoToKind.JUZ)
        add(ReaderGoToKind.PAGE)
    }
    var kind by remember { mutableStateOf(kinds.first()) }
    var text by remember { mutableStateOf("") }

    val bound = when (kind) {
        ReaderGoToKind.VERSE -> maxVerse
        ReaderGoToKind.JUZ -> JUZ_COUNT
        ReaderGoToKind.PAGE -> maxPage
    }
    val target = text.trim().toIntOrNull()?.takeIf { it in 1..bound }
    val go = {
        target?.let {
            when (kind) {
                ReaderGoToKind.VERSE -> onGoToVerse(it)
                ReaderGoToKind.JUZ -> onGoToJuz(it)
                ReaderGoToKind.PAGE -> onGoToPage(it)
            }
            onDismiss()
        }
        Unit
    }

    NimazBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = stringResource(R.string.reader_go_to),
        icon = Icons.Default.MyLocation,
        onClose = onDismiss,
        footer = {
            NimazSheetFooterButtons(
                primaryText = stringResource(R.string.reader_go_to_action),
                onPrimary = go,
                primaryEnabled = target != null,
                secondaryText = stringResource(R.string.cancel),
                onSecondary = onDismiss,
            )
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            NimazSegmentedControl(
                options = kinds.map { stringResource(it.labelRes()) }.asSegments(),
                selectedIndex = kinds.indexOf(kind),
                onSelect = {
                    kind = kinds[it]
                    // The number means something different per kind, so carrying it across
                    // would offer "verse 300" as "page 300" without the reader asking.
                    text = ""
                },
                size = NimazSegmentedSize.SMALL,
                purpose = NimazSegmentedPurpose.VALUE,
            )

            NimazTextField(
                value = text,
                onValueChange = { new -> if (new.all { it.isDigit() }) text = new },
                label = stringResource(kind.labelRes()),
                variant = NimazFieldVariant.NUMERIC,
                // The bound is stated rather than merely enforced: a disabled button with no
                // explanation is the version of this that wastes a reader's time. Out of range
                // is an error *message* for the same reason — a red box saying nothing is
                // barely better than a dead button.
                helper = stringResource(R.string.reader_go_to_range, 1, bound),
                error = if (text.isNotBlank() && target == null) {
                    stringResource(R.string.reader_go_to_range, 1, bound)
                } else null,
                imeAction = ImeAction.Go,
                onImeAction = go,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private const val JUZ_COUNT = 30

private fun ReaderGoToKind.labelRes(): Int = when (this) {
    ReaderGoToKind.VERSE -> R.string.reader_go_to_verse
    ReaderGoToKind.JUZ -> R.string.quran_home_tab_juz
    ReaderGoToKind.PAGE -> R.string.quran_home_tab_page
}
