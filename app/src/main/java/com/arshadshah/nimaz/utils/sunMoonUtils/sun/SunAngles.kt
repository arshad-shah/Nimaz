package com.arshadshah.nimaz.utils.sunMoonUtils.sun

import com.arshadshah.nimaz.utils.sunMoonUtils.utils.CalcConstants.J1970
import com.arshadshah.nimaz.utils.sunMoonUtils.utils.CalcConstants.J2000
import com.arshadshah.nimaz.utils.sunMoonUtils.utils.CalcConstants.dayMs
import com.arshadshah.nimaz.utils.sunMoonUtils.utils.CalcConstants.e
import com.arshadshah.nimaz.utils.sunMoonUtils.utils.CalcConstants.rad
import com.arshadshah.nimaz.utils.sunMoonUtils.utils.CalcConstants.zeroFive
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.collect.Lists
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.collect.Maps
import java.util.*
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.tan


object SunAngles
{

	/**
	 * sun calculations are based on http://aa.quae.nl/en/reken/zonpositie.html formulas
	 * date/time constants and conversions
	 */

	private fun toJulian(date : Date) : Double
	{
		return date.time / dayMs - zeroFive + J1970
	}

	private fun fromJulian(j : Double) : Date
	{
		return Date(((j + zeroFive - J1970) * dayMs).toLong())
	}

	private fun toDays(date : Date) : Double
	{
		return toJulian(date) - J2000
	}

	private fun rightAscension(l : Double , b : Double) : Double
	{
		return atan2(sin(l) * cos(e) - tan(b) * sin(e) , cos(l))
	}

	private fun declination(l : Double , b : Double) : Double
	{
		return asin(sin(b) * cos(e) + cos(b) * sin(e) * sin(l))
	}

	private fun azimuth(H : Double , phi : Double , dec : Double) : Double
	{
		return atan2(sin(H) , cos(H) * sin(phi) - tan(dec) * cos(phi))
	}

	private fun altitude(H : Double , phi : Double , dec : Double) : Double
	{
		return asin(sin(phi) * sin(dec) + cos(phi) * cos(dec) * cos(H))
	}

	private fun siderealTime(d : Double , lw : Double) : Double
	{
		return rad * (280.16 + 360.9856235 * d) - lw
	}

	/**
	 * general sun calculations
	 *
	 * @param d
	 * @return
	 */
	private fun solarMeanAnomaly(d : Double) : Double
	{
		return rad * (357.5291 + 0.98560028 * d)
	}

	private fun eclipticLongitude(M : Double) : Double
	{
		// equation of center
		val C : Double = rad * (1.9148 * sin(M) + 0.02 * sin(2 * M) + 0.0003 * sin(3 * M))
		// perihelion of the Earth
		val P = rad * 102.9372
		return M + C + P + PI
	}

	private fun sunCoords(d : Double) : Map<String , Double>
	{
		val map : MutableMap<String , Double> = Maps.newConcurrentMap()
		val M = solarMeanAnomaly(d)
		val L = eclipticLongitude(M)
		map["dec"] = declination(L , 0.0)
		map["ra"] = rightAscension(L , 0.0)
		return map
	}

	/**
	 * calculates sun position for a given date and latitude/longitude
	 *
	 * @param date
	 * @param lat
	 * @param lng
	 * @return
	 */
	fun getPosition(date : Date , lat : Double , lng : Double) : Map<String , Double>
	{
		val map : MutableMap<String , Double> = Maps.newConcurrentMap()
		val lw = rad * - lng
		val phi = rad * lat
		val d = toDays(date)
		val c = sunCoords(d)
		val H = siderealTime(d , lw) - c["ra"] !!
		map["azimuth"] = azimuth(H , phi , c["dec"] !!)
		map["altitude"] = altitude(H , phi , c["dec"] !!)
		return map
	}

	/**
	 * * sun times configuration (angle, morning name, evening name)
	 * sunrise	日出（太阳的顶部边缘出现在地平线上）
	 * sunriseEnd	日出结束（太阳的底部边缘接触地平线）
	 * goldenHourEnd	早上黄金时段（柔和的光线，摄影的最佳时间）结束
	 * solarNoon	太阳正午（太阳位于最高位置）
	 * goldenHour	晚上黄金时段开始
	 * sunsetStart	日落开始（太阳的底部边缘接触地平线）
	 * sunset	日落（太阳消失在地平线以下，晚上民间黄昏开始）
	 * dusk	黄昏（傍晚航海黄昏开始）
	 * nauticalDusk	航海黄昏（晚上天文学黄昏开始）
	 * night	夜晚开始（黑暗足以进行天文观测）
	 * nadir	最低点（夜晚最黑暗的时刻，太阳处于最低位置）
	 * nightEnd	夜晚结束（早晨天文学黄昏开始）
	 * nauticalDawn	航海黎明（早上航海暮光之城开始）
	 * dawn	黎明（早晨航海黄昏结束，早晨民间黄昏开始）
	 */
	var times : MutableList<*> = Lists.newArrayList(
			Lists.newArrayList(- 0.833 , "sunrise" , "sunset") ,
			Lists.newArrayList(- 0.3 , "sunriseEnd" , "sunsetStart") ,
			Lists.newArrayList(- 6.0 , "dawn" , "dusk") ,
			Lists.newArrayList(- 12.0 , "nauticalDawn" , "nauticalDusk") ,
			Lists.newArrayList(- 18.0 , "nightEnd" , "night") ,
			Lists.newArrayList(6.0 , "goldenHourEnd" , "goldenHour")
												   )

	/**
	 * calculations for sun times
	 */
	const val J0 = 0.0009
	private fun julianCycle(d : Double , lw : Double) : Double
	{
		return round(d - J0 - lw / (2 * PI))
	}

	private fun approxTransit(Ht : Double , lw : Double , n : Double) : Double
	{
		return J0 + (Ht + lw) / (2 * PI) + n
	}

	private fun solarTransitJ(ds : Double , M : Double , L : Double) : Double
	{
		return J2000 + ds + 0.0053 * sin(M) - 0.0069 * sin(2 * L)
	}

	private fun hourAngle(h : Double , phi : Double , d : Double) : Double
	{
		return acos((sin(h) - sin(phi) * sin(d)) / (cos(phi) * cos(d)))
	}

	/**
	 * returns set time for the given sun altitude
	 *
	 * @param h
	 * @param lw
	 * @param phi
	 * @param dec
	 * @param n
	 * @param M
	 * @param L
	 * @return
	 */
	private fun getSetJ(
		h : Double ,
		lw : Double ,
		phi : Double ,
		dec : Double ,
		n : Double ,
		M : Double ,
		L : Double ,
					   ) : Double
	{
		val w = hourAngle(h , phi , dec)
		val a = approxTransit(w , lw , n)
		return solarTransitJ(a , M , L)
	}

	/**
	 * calculates sun times for a given date and latitude/longitude
	 * 计算给定日期和纬度/经度的太阳时间
	 *
	 * @param date
	 * @param lat
	 * @param lng
	 * @return
	 */
	fun getTimes(date : Date , lat : Double , lng : Double) : Map<String , Date>
	{
		val lw = rad * - lng
		val phi = rad * lat
		val d = toDays(date)
		val n = julianCycle(d , lw)
		val ds = approxTransit(0.0 , lw , n)
		val M = solarMeanAnomaly(ds)
		val L = eclipticLongitude(M)
		val dec = declination(L , 0.0)
		val jNoon = solarTransitJ(ds , M , L)
		val result : MutableMap<String , Date> = Maps.newConcurrentMap()
		result["solarNoon"] = fromJulian(jNoon)
		result["nadir"] = fromJulian(jNoon - zeroFive)
		var i = 0
		val len = times.size
		while (i < len)
		{
			val time = times[i] as List<*>
			val jSet = getSetJ(
					java.lang.Double.valueOf(time[0].toString()) * rad ,
					lw ,
					phi ,
					dec ,
					n ,
					M ,
					L
							  )
			val jRise = jNoon - (jSet - jNoon)
			result[time[1] as String] = fromJulian(jRise)
			result[time[2] as String] = fromJulian(jSet)
			i += 1
		}
		return result
	}
}