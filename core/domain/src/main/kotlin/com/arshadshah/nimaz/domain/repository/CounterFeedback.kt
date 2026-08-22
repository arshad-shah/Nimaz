package com.arshadshah.nimaz.domain.repository

/**
 * The tick a counter makes when it advances.
 *
 * `TasbihViewModel` used to hold a `Vibrator` and a `ToneGenerator` directly, built
 * from an injected `Context`. `increment()` — the single most-used action in the
 * feature — called both on its first two lines, so it could not run in a JVM test at
 * all. That is why the Tasbih test suite tests a preset filter the screen does not
 * even use, and why the double-tap race that loses a count went unnoticed.
 *
 * Behind this interface the ViewModel says *tick*, and a test says "did it".
 */
interface CounterFeedback {
    /** One counter tick. Each channel is skipped when the user has it switched off. */
    fun tick(vibrate: Boolean, sound: Boolean)

    /** Releases any held audio resources. Call from `onCleared`. */
    fun release()
}
