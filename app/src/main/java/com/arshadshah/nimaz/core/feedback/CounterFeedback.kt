package com.arshadshah.nimaz.core.feedback

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.arshadshah.nimaz.core.monitoring.CrashReporter
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

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

/**
 * The Android [CounterFeedback]: a one-shot vibration and a short beep.
 *
 * `@Singleton` because `ToneGenerator` holds an audio resource — one per process, not
 * one per ViewModel instance, and Tasbih screens each get their own ViewModel.
 */
@Singleton
class AndroidCounterFeedback @Inject constructor(
    @ApplicationContext private val context: Context,
) : CounterFeedback {

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
                .defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private var toneGenerator: ToneGenerator? = runCatching {
        ToneGenerator(AudioManager.STREAM_MUSIC, TONE_VOLUME_PERCENT)
    }.onFailure { CrashReporter.recordException(it) }.getOrNull()

    override fun tick(vibrate: Boolean, sound: Boolean) {
        if (vibrate) {
            runCatching {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(TICK_MS, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            }
        }
        if (sound) {
            runCatching { toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, BEEP_MS) }
        }
    }

    override fun release() {
        toneGenerator?.release()
        toneGenerator = null
    }

    private companion object {
        const val TONE_VOLUME_PERCENT = 50
        const val TICK_MS = 30L
        const val BEEP_MS = 50
    }
}
