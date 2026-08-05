package com.arshadshah.nimaz.presentation.viewmodel

import androidx.annotation.StringRes
import com.arshadshah.nimaz.presentation.components.atoms.NimazErrorKind

/**
 * A failure a screen can render — the value every failing `UiState` carries.
 *
 * [message] is a resource id and never a string, because the alternative is what the
 * app ships today: `"Failed to search locations: ${e.message}"` reaching a user's
 * screen in English, exception-shaped, in a build that otherwise translates
 * everything. The exception's own text belongs in [details], which `NimazErrorState`
 * keeps behind a "Show details" toggle — reachable for a bug report, never the first
 * thing read.
 *
 * The ViewModel picks the [kind], because it is the layer that knows *what* failed: a
 * network fetch, a missing row, a denied permission. The kind then fixes the glyph and
 * the tone, so one kind of failure looks the same wherever it surfaces.
 */
data class UiError(
    @StringRes val message: Int,
    val kind: NimazErrorKind = NimazErrorKind.GENERIC,
    val details: String? = null,
)
