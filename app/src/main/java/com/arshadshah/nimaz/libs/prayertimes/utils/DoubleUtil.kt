package com.arshadshah.nimaz.libs.prayertimes.utils

import kotlin.math.roundToInt

internal object DoubleUtil {
    fun normalizeWithBound(value: Double, max: Double): Double {
        return value - max * kotlin.math.floor(value / max)
    }

    fun unwindAngle(value: Double): Double {
        return normalizeWithBound(value, 360.0)
    }

    fun closestAngle(angle: Double): Double {
        return if (angle >= -180 && angle <= 180) {
            angle
        } else angle - 360 * (angle / 360).roundToInt()
    }
}