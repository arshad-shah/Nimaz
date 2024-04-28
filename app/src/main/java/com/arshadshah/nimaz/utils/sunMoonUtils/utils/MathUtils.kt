package com.arshadshah.nimaz.utils.sunMoonUtils.utils

import com.arshadshah.nimaz.utils.sunMoonUtils.utils.CalcConstants.J1970
import com.arshadshah.nimaz.utils.sunMoonUtils.utils.CalcConstants.J2000
import com.arshadshah.nimaz.utils.sunMoonUtils.utils.CalcConstants.dayMs
import com.arshadshah.nimaz.utils.sunMoonUtils.utils.CalcConstants.e
import com.arshadshah.nimaz.utils.sunMoonUtils.utils.CalcConstants.rad
import com.arshadshah.nimaz.utils.sunMoonUtils.utils.CalcConstants.zeroFive
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.tan


internal object MathUtils {

    fun toJulian(date: LocalDateTime): Double =
        date.toInstant(ZoneOffset.UTC).toEpochMilli() / dayMs - zeroFive + J1970

    fun fromJulian(julian: Double): LocalDateTime =
        LocalDateTime.ofInstant(
            Instant.ofEpochMilli(((julian + 0.50 - J1970) * dayMs).toLong()),
            ZoneId.systemDefault()
        )

    fun toDays(date: LocalDateTime): Double =
        toJulian(date) - J2000

    private fun rightAscension(l: Double, b: Double): Double {
        return atan2(sin(l) * cos(e) - tan(b) * sin(e), cos(l))
    }

    private fun declination(l: Double, b: Double): Double {
        return asin(sin(b) * cos(e) + cos(b) * sin(e) * sin(l))
    }

    fun azimuth(H: Double, phi: Double, dec: Double): Double {
        return atan2(sin(H), cos(H) * sin(phi) - tan(dec) * cos(phi))
    }

    fun altitude(H: Double, phi: Double, dec: Double): Double {
        return asin(sin(phi) * sin(dec) + cos(phi) * cos(dec) * cos(H))
    }

    fun siderealTime(d: Double, lw: Double): Double {
        return rad * (280.16 + 360.9856235 * d) - lw
    }

    fun astroRefraction(h: Double): Double {
        val hChecked =
            if (h < 0) h else h // the following formula works for positive altitudes only.

        // formula 16.4 of "Astronomical Algorithms" 2nd edition by Jean Meeus (Willmann-Bell, Richmond) 1998.
        // 1.02 / tan(h + 10.26 / (h + 5.10)) h in degrees, result in arc minutes -> converted to rad:
        return 0.0002967 / tan(hChecked + 0.00312536 / (hChecked + 0.08901179))
    }

    private fun solarMeanAnomaly(d: Double): Double {
        return rad * (357.5291 + 0.98560028 * d)
    }

    private fun eclipticLongitude(M: Double): Double {
        val C =
            rad * (1.9148 * sin(M) + 0.02 * sin(2 * M) + 0.0003 * sin(3 * M)) // equation of center
        val P = rad * 102.9372 // perihelion of the Earth

        return M + C + P + PI
    }

    fun hoursLater(date: LocalDateTime, hoursLater: Int): LocalDateTime =
        date.plusMinutes((hoursLater * 60).toLong())

}

internal fun Double.round(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return round(this * multiplier) / multiplier
}
