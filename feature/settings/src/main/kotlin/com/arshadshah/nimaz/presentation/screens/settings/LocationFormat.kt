package com.arshadshah.nimaz.presentation.screens.settings

import java.util.Locale
import kotlin.math.abs

/** Formats a coordinate pair with sign-derived hemispheres, e.g. "21.4225° N, 39.8262° E". */
fun formatCoordinates(latitude: Double, longitude: Double): String {
    val ns = if (latitude >= 0) "N" else "S"
    val ew = if (longitude >= 0) "E" else "W"
    return String.format(Locale.US, "%.4f° %s, %.4f° %s", abs(latitude), ns, abs(longitude), ew)
}
