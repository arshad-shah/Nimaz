package com.arshadshah.nimaz.presentation.viewmodel.more

import com.arshadshah.nimaz.domain.model.PinnedShortcut

sealed interface MoreEvent {

    /**
     * Replace the pinned row with [pins], in this order.
     *
     * The whole list rather than a pin/unpin pair: order is part of what is being set, and a
     * two-event pin/unpin API would need a third to express a reorder. The cap is enforced by
     * `PinnedShortcut.encode` on the way to disk, so an over-long list is truncated rather than
     * rejected — the sheet is what stops it happening, this is what makes it harmless.
     */
    data class SetPins(val pins: List<PinnedShortcut>) : MoreEvent

    /**
     * Re-resolve the snapshot figures.
     *
     * The flow-backed rows re-emit on their own. The next worship reminder does not — it is a
     * suspend computation over a dozen settings — so the screen asks for it when it comes into
     * view, rather than a timer keeping it warm behind a menu.
     */
    data object Refresh : MoreEvent
}
