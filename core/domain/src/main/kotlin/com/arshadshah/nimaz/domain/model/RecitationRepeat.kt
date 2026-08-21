package com.arshadshah.nimaz.domain.model

/**
 * What the reciter should go back and do again.
 *
 * Repeating a verse until it is memorised was the most obvious thing the player could not do,
 * and it is the reason recitation exists in a study app at all.
 *
 * The invariants live **here**, not in `QuranAudioManager`: a repeat of one is not a repeat, and
 * a range that ends before it starts is not a range. Guarding them in the domain type means the
 * player can never be handed one, and the guard is testable without Android.
 */
sealed interface RecitationRepeat {

    /** Play through once, which is what recitation does unless asked otherwise. */
    data object Off : RecitationRepeat

    /**
     * Say this verse [times] times before moving on.
     *
     * Counted, not looped: [times] has to *stop*, so the manager counts completions rather than
     * using `REPEAT_MODE_ONE`, which would never let go.
     */
    data class Ayah(val times: Int) : RecitationRepeat {
        init {
            require(times >= MIN_TIMES) {
                "repeat times must be >= $MIN_TIMES, was $times — one play is not a repeat"
            }
        }
    }

    /** Loop verses [fromAyah]..[toAyah] within the surah, the memorisation drill. */
    data class Range(val fromAyah: Int, val toAyah: Int) : RecitationRepeat {
        init {
            require(fromAyah >= 1) { "fromAyah must be >= 1, was $fromAyah" }
            require(toAyah >= fromAyah) {
                "toAyah must be >= fromAyah, was $toAyah < $fromAyah"
            }
        }
    }

    /** Loop the whole playlist. */
    data object Surah : RecitationRepeat

    companion object {
        /** Below this, "repeat" would mean "play once", which the reader can already do. */
        const val MIN_TIMES = 2

        /** The default when a reader turns ayah repeat on without saying how many. */
        const val DEFAULT_TIMES = 3

        /**
         * [Ayah] with [times] pulled up to the minimum rather than throwing.
         *
         * For the stepper, which can only ever hand this a small integer and should floor at
         * the boundary rather than crash on it. Construction from anywhere else keeps the
         * `require`, because a value out of range there is a bug and not a button press.
         */
        fun ayahClamped(times: Int): Ayah = Ayah(times.coerceAtLeast(MIN_TIMES))
    }
}

/** The speeds the player offers. Anything else is rejected rather than quietly clamped. */
enum class RecitationSpeed(val multiplier: Float) {
    SLOWEST(0.75f),
    NORMAL(1f),
    FASTER(1.25f),
    FASTEST(1.5f);

    companion object {
        val DEFAULT = NORMAL

        /**
         * The offered speed [multiplier] names, or null.
         *
         * Deliberately not a nearest-match: a caller asking for 2× is asking for something the
         * player does not do, and silently giving them 1.5× is a worse answer than none.
         */
        fun of(multiplier: Float): RecitationSpeed? =
            entries.firstOrNull { it.multiplier == multiplier }
    }
}
