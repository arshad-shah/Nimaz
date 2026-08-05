package com.arshadshah.nimaz.presentation.screens.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.PinnedShortcut
import com.arshadshah.nimaz.presentation.components.atoms.NimazCheckbox
import com.arshadshah.nimaz.presentation.components.molecules.NimazBottomSheet
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuGroup
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuItem

/**
 * Choose which shortcuts sit above the menu.
 *
 * The cap is expressed by **disabling the rows you cannot add**, with the header saying why. The
 * alternatives were worse in specific ways: silently ignoring the tap teaches nothing, and asking
 * "which one should I replace?" turns one decision into two — the person already knows which of
 * their five they care least about, and unpinning it is one tap away in the same list.
 *
 * A pinned row is never disabled, whatever the count, or reaching the cap would make the row you
 * want to remove the one row you cannot touch.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinnedShortcutsSheet(
    pinned: List<PinnedShortcut>,
    onPinnedChange: (List<PinnedShortcut>) -> Unit,
    onDismiss: () -> Unit,
) {
    val atCap = pinned.size >= PinnedShortcut.MAX_PINS
    NimazBottomSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.more_pins_sheet_title),
        subtitle = if (atCap) {
            stringResource(R.string.more_pins_full, PinnedShortcut.MAX_PINS)
        } else {
            stringResource(R.string.more_pins_count, pinned.size, PinnedShortcut.MAX_PINS)
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NimazMenuGroup {
                PinnedShortcut.entries.forEach { shortcut ->
                    val isPinned = shortcut in pinned
                    val toggle = {
                        onPinnedChange(
                            // Appended, not inserted: a new pin joining the end keeps the
                            // arrangement someone already has rather than reshuffling it.
                            if (isPinned) pinned - shortcut else pinned + shortcut
                        )
                    }
                    NimazMenuItem(
                        title = stringResource(shortcut.labelRes()),
                        onClick = toggle,
                        // Unpinning always works. Disabling a pinned row at the cap would make
                        // the one you want gone the one you cannot reach.
                        enabled = isPinned || !atCap,
                        trailingIcon = null,
                        trailing = {
                            NimazCheckbox(
                                checked = isPinned,
                                onCheckedChange = null,
                                enabled = isPinned || !atCap,
                            )
                        },
                    )
                }
            }
            if (pinned.isEmpty()) {
                Text(
                    text = stringResource(R.string.more_pins_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * The row label for a pinnable destination.
 *
 * Its own short strings rather than the menu row's titles: "Track your daily prayers" is a menu
 * entry, "Prayer tracker" is what fits on a pill. Reusing the longer titles would either truncate
 * on the pill or make the sheet and the row disagree about what a thing is called.
 */
internal fun PinnedShortcut.labelRes(): Int = when (this) {
    PinnedShortcut.TASBIH -> R.string.more_pin_tasbih
    PinnedShortcut.PRAYER_TRACKER -> R.string.more_pin_prayer_tracker
    PinnedShortcut.KHATAM -> R.string.more_pin_khatam
    PinnedShortcut.ZAKAT -> R.string.more_pin_zakat
    PinnedShortcut.QIBLA -> R.string.more_pin_qibla
    PinnedShortcut.FASTING -> R.string.more_pin_fasting
    PinnedShortcut.NIGHT_WORSHIP -> R.string.more_pin_night_worship
    PinnedShortcut.QAIDA -> R.string.more_pin_qaida
    PinnedShortcut.ISLAMIC_CALENDAR -> R.string.more_pin_islamic_calendar
}
