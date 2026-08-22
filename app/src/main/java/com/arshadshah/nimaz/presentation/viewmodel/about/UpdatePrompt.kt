package com.arshadshah.nimaz.presentation.viewmodel.about

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.graphics.vector.ImageVector
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.core.util.UpdateState

/**
 * How the About screen's "check for updates" row should read, for one [UpdateState].
 *
 * @param isBusy work is in flight, so the row must not accept a tap. Note a **failed**
 *   check is not busy: retrying is the only thing a reader can do about it.
 * @param isActionable there is something to complete — download it, install it.
 * @param isHighlighted the row should use the accent colour. Wider than [isActionable]:
 *   "you're up to date" is good news worth colouring, and is not an action.
 * @param isError the check failed; the row reads in the error colour.
 */
data class UpdatePrompt(
    @StringRes val label: Int,
    val icon: ImageVector,
    val isBusy: Boolean,
    val isActionable: Boolean,
    val isHighlighted: Boolean,
    val isError: Boolean,
)

/**
 * Maps an [UpdateState] onto the row that describes it.
 *
 * This lived inside `AboutScreen` as four parallel `when` expressions over the same
 * subject, next to the lambda that performed the click — so nothing about it could be
 * asserted, including whether a failed check could be retried at all.
 *
 * The `InAppUpdateManager` handle itself stays a CompositionLocal: the Play flow needs an
 * `Activity` to start, and a ViewModel is the wrong place to hold one. See
 * `docs/ARCHITECTURE.md` §9.
 */
fun updatePrompt(state: UpdateState): UpdatePrompt {
    val isActionable = state is UpdateState.UpdateAvailable || state is UpdateState.Downloaded
    return UpdatePrompt(
        label = when (state) {
            is UpdateState.Checking -> R.string.update_checking
            is UpdateState.UpdateAvailable -> R.string.update_new_version
            is UpdateState.Starting -> R.string.update_starting
            is UpdateState.Downloading -> R.string.update_downloading
            is UpdateState.Downloaded -> R.string.update_downloaded
            is UpdateState.NoUpdateAvailable -> R.string.update_up_to_date
            is UpdateState.Error -> R.string.update_check_failed
            else -> R.string.update_tap_to_check
        },
        icon = when (state) {
            is UpdateState.UpdateAvailable -> Icons.Default.Download
            is UpdateState.Downloaded -> Icons.Default.InstallMobile
            is UpdateState.NoUpdateAvailable -> Icons.Default.CheckCircle
            is UpdateState.Error -> Icons.Default.ErrorOutline
            else -> Icons.Default.Refresh
        },
        isBusy = state is UpdateState.Checking ||
                state is UpdateState.Starting ||
                state is UpdateState.Downloading,
        isActionable = isActionable,
        isHighlighted = isActionable || state is UpdateState.NoUpdateAvailable,
        isError = state is UpdateState.Error,
    )
}
