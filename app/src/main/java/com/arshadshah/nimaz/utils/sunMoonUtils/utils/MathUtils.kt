package com.arshadshah.nimaz.utils.sunMoonUtils.utils

import kotlin.math.round


internal fun Double.round(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return round(this * multiplier) / multiplier
}
