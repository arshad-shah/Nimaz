package com.arshadshah.nimaz.utils.sunMoonUtils

import android.content.Context
import com.arshadshah.nimaz.helperClasses.utils.sunMoonUtils.MoonCalculations
import com.arshadshah.nimaz.utils.sunMoonUtils.MathUtils.astroRefraction
import com.arshadshah.nimaz.utils.sunMoonUtils.MathUtils.azimuth
import com.arshadshah.nimaz.utils.sunMoonUtils.SunMoonCalcConstants.rad
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.*

class SunMoonCalc constructor(
    private val latitude : Double ,
    private val longitude : Double ,
    private val context : Context ,
                             )
{

	private val date : LocalDateTime = LocalDateTime.now()

	private val percentages = arrayOf(0f , .25f , .5f , .75f , 1f)

	/**
	 * Returns the sun position
	 * @return {@link SunPosition} which represents the Sun position
	 */
	fun getSunPosition() : SunPosition
	{
		val lw = rad * - longitude
		val phi = rad * latitude
		val d = MathUtils.toDays(date)

		val c = MathUtils.getSunCoords(d)
		val H = MathUtils.siderealTime(d , lw) - c.ra

		return SunPosition(
				azimuth(H , phi , c.dec) ,
				MathUtils.altitude(H , phi , c.dec)
						  )
	}

	/**
	 * Returns the sun position for a given date
	 * @return {@link SunPosition} which represents the Sun position
	 */
	fun getSunPositionForDate(dateToShowPositionFor : LocalDateTime) : SunPosition
	{
		val lw = rad * - longitude
		val phi = rad * latitude
		val d = MathUtils.toDays(dateToShowPositionFor)

		val c = MathUtils.getSunCoords(d)
		val H = MathUtils.siderealTime(d , lw) - c.ra

		return SunPosition(
				azimuth(H , phi , c.dec) ,
				MathUtils.altitude(H , phi , c.dec)
						  )
	}

	/**
	 * Returns the sun & moon times
	 * @return {@link SunTimes} which represents the sun & moon times
	 */
	fun getTimes(date : LocalDateTime = this.date , height : Double = 1.0) : SunTimes
	{
		val solarNoonAndNadir = MathUtils.getSolarNoonAndNadir(latitude , longitude , date , height)
		val sunriseAndSunset =
			MathUtils.getTimeAndEndingByValue(latitude , longitude , date , height , - 0.833f)
		val sunriseEndAndSunsetStart =
			MathUtils.getTimeAndEndingByValue(latitude , longitude , date , height , - 0.3f)
		val dawnAndDusk =
			MathUtils.getTimeAndEndingByValue(latitude , longitude , date , height , - 6f)
		val nauticalDawnAndNauticalDusk =
			MathUtils.getTimeAndEndingByValue(latitude , longitude , date , height , - 12f)
		val nightEndAndNight =
			MathUtils.getTimeAndEndingByValue(latitude , longitude , date , height , - 18f)
		val goldenHourEndAndGoldenHour =
			MathUtils.getTimeAndEndingByValue(latitude , longitude , date , height , 6f)

		return SunTimes(
				sunriseAndSunset.first ,
				sunriseEndAndSunsetStart.first ,
				goldenHourEndAndGoldenHour.second ,
				goldenHourEndAndGoldenHour.first ,
				solarNoonAndNadir.first ,
				sunriseEndAndSunsetStart.second ,
				sunriseAndSunset.second ,
				dawnAndDusk.second ,
				nauticalDawnAndNauticalDusk.second ,
				nightEndAndNight.second ,
				nightEndAndNight.first ,
				solarNoonAndNadir.second ,
				nauticalDawnAndNauticalDusk.first ,
				dawnAndDusk.first
					   )
	}

	/**
	 * Returns the moon position
	 * @return {@link MoonPosition} which represents the moon position
	 */
	fun getMoonPosition(date : LocalDateTime = this.date) : MoonPosition
	{
		val lw = rad * - longitude
		val phi = rad * latitude
		val d = MathUtils.toDays(date)

		val c = MathUtils.getMoonCords(d)
		val H = MathUtils.siderealTime(d , lw) - c.ra
		var h = MathUtils.altitude(H , phi , c.dec)
		// formula 14.1 of "Astronomical Algorithms" 2nd edition by Jean Meeus (Willmann-Bell, Richmond) 1998.
		val pa = atan2(sin(H) , tan(phi) * cos(c.dec) - sin(c.dec) * cos(H))

		h += astroRefraction(h) // altitude correction for refraction

		return MoonPosition(
				h ,
				azimuth(H , phi , c.dec) ,
				c.dist ,
				pa
						   )
	}

	/**
	 * Gets the moon's phase information
	 * @return {@link com.costular.sunkalc.MoonIllumination} which represents the moon illumination
	 */
	fun getMoonPhase(date : LocalDateTime = this.date) : MoonPhaseInfo
	{
		val moonCalculations = getMoonCalculations(date)
		val moonCalculationsNextDay = getMoonCalculations(date.plusDays(1))

		val moonPhasePosition = getMoonPhasePosition(moonCalculations , moonCalculationsNextDay)
		val phaseName = getPhaseNameByPhasePosition(moonPhasePosition)
		val phaseEmoji = getPhaseSvgByPhasePosition(moonPhasePosition)

		val fraction = ((1 + cos(moonCalculations.inc)) / 2)
		val phaseValue =
			(0.5 + 0.5 * moonCalculations.inc * (if (moonCalculations.angle < 0) - 1 else 1) / Math.PI)

		return MoonPhaseInfo(
				fraction ,
				phaseValue ,
				moonCalculations.angle ,
				phaseName ,
				phaseEmoji
							)
	}

	private fun getMoonCalculations(date : LocalDateTime) : MoonCalculations
	{
		val d = MathUtils.toDays(date)
		val s = MathUtils.getSunCoords(d)
		val m = MathUtils.getMoonCords(d)

		val sdist = 149598000 // distance from Earth to Sun in km

		val phi = acos(sin(s.dec) * sin(m.dec) + cos(s.dec) * cos(m.dec) * cos(s.ra - m.ra))
		val inc = atan2(sdist * sin(phi) , m.dist - sdist * cos(phi))
		val angle = atan2(
				cos(s.dec) * sin(s.ra - m.ra) , sin(s.dec) * cos(m.dec) -
				cos(s.dec) * sin(m.dec) * cos(s.ra - m.ra)
						 )

		return MoonCalculations(phi , inc , angle)
	}

	private fun getMoonPhasePosition(current : MoonCalculations , next : MoonCalculations) : Int
	{
		var index = 0

		val phase1 = (0.5 + 0.5 * current.inc * (if (current.angle < 0) - 1 else 1) / Math.PI)
		val phase2 = (0.5 + 0.5 * next.inc * (if (next.angle < 0) - 1 else 1) / Math.PI)

		if (phase1 <= phase2)
		{
			for (i in percentages.indices)
			{
				val percentage = percentages[i]
				if (percentage in phase1 .. phase2)
				{
					index = 2 * i
					break
				} else if (percentage > phase1)
				{
					index = (2 * i) - 1
					break
				}
			}
		}

		return index % 8
	}

	private fun getPhaseNameByPhasePosition(value : Int) : MoonPhase
	{
		return when (value)
		{
			0 -> MoonPhase.NEW_MOON
			1 -> MoonPhase.WAXING_CRESCENT
			2 -> MoonPhase.FIRST_QUARTER
			3 -> MoonPhase.WAXING_GIBBOUS
			4 -> MoonPhase.FULL_MOON
			5 -> MoonPhase.WANING_GIBBOUS
			6 -> MoonPhase.LAST_QUARTER
			7 -> MoonPhase.WANING_CRESCENT
			else -> throw IllegalStateException("Moon phase position should be between 0-7")
		}
	}

	private fun getPhaseSvgByPhasePosition(value : Int) : String
	{
		return when (value)
		{
			0 -> "\uD83C\uDF11"
			1 -> "\uD83C\uDF12"
			2 -> "\uD83C\uDF13"
			3 -> "\uD83C\uDF14"
			4 -> "\uD83C\uDF15"
			5 -> "\uD83C\uDF16"
			6 -> "\uD83C\uDF17"
			7 -> "\uD83C\uDF18"
			else -> throw IllegalStateException("Moon phase position should be between 0-7")
		}
	}

	//phase emoji
	//https://emojipedia.org/new-moon/

	/**
	 *
	 */
	fun getZodiacSign(_date : LocalDate = this.date.toLocalDate()) : ZodiacSign
	{
		var longitude = 0.0

		var yy = 0.0
		var mm = 0.0
		var k1 = 0.0
		var k2 = 0.0
		var k3 = 0.0
		var jd = 0.0
		var ip = 0.0
		var dp = 0.0
		var rp = 0.0

		val year = _date.year
		val month = _date.monthValue
		val day = _date.dayOfMonth

		yy = year - floor((12.0 - month) / 10.0)
		mm = month + 9.0
		if (mm >= 12)
		{
			mm -= 12
		}

		k1 = floor(365.25 * (yy + 4712))
		k2 = floor(30.6 * mm + 0.5)
		k3 = floor(floor((yy / 100) + 49) * 0.75) - 38

		jd = k1 + k2 + day + 59
		if (jd > 2299160)
		{
			jd -= k3
		}

		ip = MathUtils.normalize((jd - 2451550.1) / 29.530588853)

		ip *= 2 * PI

		dp = 2 * PI * MathUtils.normalize((jd - 2451562.2) / 27.55454988)

		rp = MathUtils.normalize((jd - 2451555.8) / 27.321582241)
		longitude = 360 * rp + 6.3 * sin(dp) + 1.3 * sin(2 * ip - dp) + 0.7 * sin(2 * ip)

		return when
		{
			longitude < 33.18 ->
			{
				ZodiacSign.ARIES
			}

			longitude < 51.16 ->
			{
				ZodiacSign.CANCER
			}

			longitude < 93.44 ->
			{
				ZodiacSign.GEMINI
			}

			longitude < 119.48 ->
			{
				ZodiacSign.CANCER
			}

			longitude < 135.30 ->
			{
				ZodiacSign.LEO
			}

			longitude < 173.34 ->
			{
				ZodiacSign.VIRGO
			}

			longitude < 224.17 ->
			{
				ZodiacSign.LIBRA
			}

			longitude < 242.57 ->
			{
				ZodiacSign.SCORPIO
			}

			longitude < 271.26 ->
			{
				ZodiacSign.SAGITTARIUS
			}

			longitude < 302.49 ->
			{
				ZodiacSign.CAPRICORN
			}

			longitude < 311.72 ->
			{
				ZodiacSign.AQUARIUS
			}

			longitude < 348.58 ->
			{
				ZodiacSign.PISCES
			}

			else ->
			{
				ZodiacSign.ARIES
			}
		}
	}

	/**
	 * Returns the moon times
	 * @return {@link MoonTime} which represents the times
	 */
	fun getMoonTimes(_date : LocalDateTime = this.date) : MoonTime
	{
		val date = _date.atZone(ZoneId.of("UTC")).toLocalDateTime().apply {
			withHour(0)
			withMinute(0)
			withSecond(0)
			withNano(0)
		}

		val hc = 0.133 * rad
		var h0 = getMoonPosition(date).altitude - hc
		var h1 : Double
		var h2 : Double
		var rise = 0.0
		var set = 0.0
		var a : Double
		var b : Double
		var xe : Double
		var ye = 0.0
		var d : Double
		var roots : Int
		var x1 = 0.0
		var x2 = 0.0
		var dx : Double

		// go in 2-hour chunks, each time seeing if a 3-point quadratic curve crosses zero (which means rise or set)
		for (i in 1 .. 24 step 2)
		{
			h1 = getMoonPosition(MathUtils.hoursLater(date , i)).altitude - hc
			h2 = getMoonPosition(MathUtils.hoursLater(date , i + 1)).altitude - hc

			a = (h0 + h2) / 2 - h1
			b = (h2 - h0) / 2
			xe = - b / (2 * a)
			ye = (a * xe + b) * xe + h1
			d = b * b - 4 * a * h1
			roots = 0

			if (d >= 0)
			{
				dx = sqrt(d) / (abs(a) * 2)
				x1 = xe - dx
				x2 = xe + dx
				if (abs(x1) <= 1) roots ++
				if (abs(x2) <= 1) roots ++
				if (x1 < - 1) x1 = x2
			}

			if (roots == 1)
			{
				if (h0 < 0) rise = i + x1
				else set = i + x1

			} else if (roots == 2)
			{
				rise = i + (if (ye < 0) x2 else x1)
				set = i + (if (ye < 0) x1 else x2)
			}

			if (rise != 0.0 && set != 0.0) break

			h0 = h2
		}

		val alwaysUp = (rise != 0.0 && set != 0.0 && ye > 0.0)
		val alwaysDown = (rise != 0.0 && set != 0.0 && ye <= 0.0)

		return MoonTime(
				if (rise != 0.0) MathUtils.hoursLater(date , rise.toInt()) else date ,
				if (set != 0.0) MathUtils.hoursLater(date , set.toInt()) else date ,
				alwaysUp ,
				alwaysDown
					   )
	}

}