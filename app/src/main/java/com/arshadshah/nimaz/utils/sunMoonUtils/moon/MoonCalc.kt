package com.arshadshah.nimaz.utils.sunMoonUtils.moon

import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.utils.sunMoonUtils.utils.CalcConstants.rad
import com.arshadshah.nimaz.utils.sunMoonUtils.utils.MathUtils
import com.arshadshah.nimaz.utils.sunMoonUtils.utils.MathUtils.altitude
import com.arshadshah.nimaz.utils.sunMoonUtils.utils.MathUtils.astroRefraction
import com.arshadshah.nimaz.utils.sunMoonUtils.utils.MathUtils.azimuth
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

class MoonCalc @JvmOverloads constructor(
	private val latitude : Double ,
	private val longitude : Double ,
	private val date : LocalDateTime = LocalDateTime.now() ,
										)
{

	private val percentages = arrayOf(0f , .25f , .5f , .75f , 1f)

	/**
	 * Returns the moon position
	 * @return {@link MoonPosition} which represents the moon position
	 */
	private fun getMoonPosition(date : LocalDateTime = this.date) : MoonPosition
	{
		val lw = rad * - longitude
		val phi = rad * latitude
		val d = MathUtils.toDays(date)

		val c = MathUtils.getMoonCords(d)
		val H = MathUtils.siderealTime(d , lw) - c.ra
		var h = altitude(H , phi , c.dec)
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
		val phaseEmoji = getPhaseEmojiByPhasePosition(moonPhasePosition)

		val fraction = ((1 + cos(moonCalculations.inc)) / 2)
		val phaseValue =
			(0.5 + 0.5 * moonCalculations.inc * (if (moonCalculations.angle < 0) - 1 else 1) / Math.PI)

		val percentage = (fraction * 100).toInt()
		val imageToShow = when (phaseName)
		{
			MoonPhase.NEW_MOON ->
			{
				R.drawable.new_moon
			}

			MoonPhase.WAXING_CRESCENT ->
			{
				//get the image to show
				when (percentage)
				{
					in 0 .. 10 -> R.drawable.waxing_cresent_7
					in 10 .. 20 -> R.drawable.waxing_cresent_14
					in 20 .. 30 -> R.drawable.waxing_cresent_21
					in 30 .. 40 -> R.drawable.waxing_cresent_29
					in 40 .. 50 -> R.drawable.waxing_cresent_36
					else -> R.drawable.waxing_cresent_36
				}
			}

			MoonPhase.FIRST_QUARTER ->
			{
				R.drawable.first_quarter_moon
			}

			MoonPhase.WAXING_GIBBOUS ->
			{
				//get the image to show
				when (percentage)
				{
					in 50 .. 60 -> R.drawable.waxing_gib_57
					in 60 .. 70 -> R.drawable.waxing_gib_64
					in 70 .. 80 -> R.drawable.waxing_gib_71
					in 80 .. 90 -> R.drawable.waxing_gib_78
					in 90 .. 100 -> R.drawable.waxing_gib_86
					else -> R.drawable.waxing_gib_71
				}
			}

			MoonPhase.FULL_MOON ->
			{
				R.drawable.full_moon
			}

			MoonPhase.WANING_GIBBOUS ->
			{
				//get the image to show
				when (100 - percentage)
				{
					in 0 .. 10 -> R.drawable.wanning_gib_7
					in 10 .. 20 -> R.drawable.wanning_gib_14
					in 20 .. 30 -> R.drawable.wanning_gib_21
					in 30 .. 40 -> R.drawable.wanning_gib_29
					in 40 .. 50 -> R.drawable.wanning_gib_36
					in 50 .. 60 -> R.drawable.wanning_gib_43
					else -> R.drawable.wanning_gib_36
				}
			}

			MoonPhase.LAST_QUARTER ->
			{
				R.drawable.last_quarter_moon
			}

			MoonPhase.WANING_CRESCENT ->
			{
				//get the image to show
				when (100 - percentage)
				{
					in 50 .. 60 -> R.drawable.wanning_cres_57
					in 60 .. 70 -> R.drawable.wanning_cres_64
					in 70 .. 80 -> R.drawable.wanning_cres_71
					in 80 .. 90 -> R.drawable.wanning_cres_78
					in 90 .. 100 -> R.drawable.wanning_cres_86
					else -> R.drawable.wanning_cres_93
				}
			}
		}

		return MoonPhaseInfo(
				fraction ,
				phaseValue ,
				moonCalculations.angle ,
				phaseName ,
				imageToShow
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
		// 0 - 27
		//where 0 is new moon and 27 is waninng crescent
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

		return index % 27
	}

	private fun getPhaseNameByPhasePosition(value : Int) : MoonPhase
	{
		return when (value)
		{
			0 -> MoonPhase.NEW_MOON
			1 -> MoonPhase.WAXING_CRESCENT
			//in 0 .. 7 -> R.drawable.waxing_cresent_7
			//				in 7 .. 14 -> R.drawable.waxing_cresent_14
			//				in 14 .. 21 -> R.drawable.waxing_cresent_21
			//				in 21 .. 29 -> R.drawable.waxing_cresent_29
			//				in 28 .. 36 -> R.drawable.waxing_cresent_36
			//				in 35 .. 43 -> R.drawable.waxing_cresent_43

			2 -> MoonPhase.FIRST_QUARTER
			3 -> MoonPhase.WAXING_GIBBOUS
			4 -> MoonPhase.FULL_MOON
			5 -> MoonPhase.WANING_GIBBOUS
			6 -> MoonPhase.LAST_QUARTER
			7 -> MoonPhase.WANING_CRESCENT
			else -> throw IllegalStateException("Moon phase position should be between 0-7")
		}
	}

	private fun getPhaseEmojiByPhasePosition(value : Int) : String
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