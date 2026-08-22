package com.arshadshah.nimaz.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * The app's haptic feedback handle — the platform one, gated on the user's setting.
 *
 * [LocalHapticEnabled] carries a real preference (`SettingsRepository.hapticFeedback`, surfaced in
 * settings and threaded through `NimazTheme`), and **nothing read it**. The two components that
 * buzz — the calendar's date change and the time picker's tick — took `LocalHapticFeedback`
 * directly, so switching the setting off changed nothing at all.
 *
 * Reach for this instead of `LocalHapticFeedback` anywhere in the app. A silent no-op object is
 * cheaper than asking every call site to remember an `if`, and it cannot be forgotten.
 */
@Composable
fun rememberNimazHaptics(): HapticFeedback {
    val platform = LocalHapticFeedback.current
    val enabled = LocalHapticEnabled.current
    return remember(platform, enabled) {
        if (enabled) platform else NoHaptics
    }
}

/** What [rememberNimazHaptics] returns when the user has turned haptics off. */
private object NoHaptics : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) = Unit
}
