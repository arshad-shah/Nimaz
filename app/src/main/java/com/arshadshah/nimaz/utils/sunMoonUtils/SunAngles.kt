package com.arshadshah.nimaz.utils.sunMoonUtils

import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.collect.Lists
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.collect.Maps
import java.util.*
import kotlin.math.*


object SunAngles
{

	val rad : Double = PI / 180

	/**
	 * sun calculations are based on http://aa.quae.nl/en/reken/zonpositie.html formulas
	 * date/time constants and conversions
	 */
	const val dayMs = (1000 * 60 * 60 * 24).toDouble()
	const val J1970 = 2440588.0
	const val J2000 = 2451545.0
	fun toJulian(date : Date) : Double
	{
		return date.time / dayMs - 0.5 + J1970
	}

	fun fromJulian(j : Double) : Date
	{
		return Date(((j + 0.5 - J1970) * dayMs).toLong())
	}

	fun toDays(date : Date) : Double
	{
		return toJulian(date) - J2000
	}

	/**
	 * general calculations for position
	 * // obliquity of the Earth
	 */
	val e = rad * 23.4397
	fun rightAscension(l : Double , b : Double) : Double
	{
		return atan2(sin(l) * cos(e) - tan(b) * sin(e) , cos(l))
	}

	fun declination(l : Double , b : Double) : Double
	{
		return asin(sin(b) * cos(e) + cos(b) * sin(e) * sin(l))
	}

	fun azimuth(H : Double , phi : Double , dec : Double) : Double
	{
		return atan2(sin(H) , cos(H) * sin(phi) - tan(dec) * cos(phi))
	}

	fun altitude(H : Double , phi : Double , dec : Double) : Double
	{
		return asin(sin(phi) * sin(dec) + cos(phi) * cos(dec) * cos(H))
	}

	fun siderealTime(d : Double , lw : Double) : Double
	{
		return rad * (280.16 + 360.9856235 * d) - lw
	}

	fun astroRefraction(h : Double) : Double
	{
		// the following formula works for positive altitudes only.
		var h = h
		if (h < 0)
		{
			// if h = -0.08901179 a div/0 would occur.
			h = 0.0
		}
		// formula 16.4 of "Astronomical Algorithms" 2nd edition by Jean Meeus (Willmann-Bell, Richmond) 1998.
		// 1.02 / tan(h + 10.26 / (h + 5.10)) h in degrees, result in arc minutes -> converted to rad:
		return 0.0002967 / tan(h + 0.00312536 / (h + 0.08901179))
	}

	/**
	 * general sun calculations
	 *
	 * @param d
	 * @return
	 */
	fun solarMeanAnomaly(d : Double) : Double
	{
		return rad * (357.5291 + 0.98560028 * d)
	}

	fun eclipticLongitude(M : Double) : Double
	{
		// equation of center
		val C : Double = rad * (1.9148 * sin(M) + 0.02 * sin(2 * M) + 0.0003 * sin(3 * M))
		// perihelion of the Earth
		val P = rad * 102.9372
		return M + C + P + PI
	}

	fun sunCoords(d : Double) : Map<String , Double>
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
	fun julianCycle(d : Double , lw : Double) : Double
	{
		return round(d - J0 - lw / (2 * PI))
	}

	fun approxTransit(Ht : Double , lw : Double , n : Double) : Double
	{
		return J0 + (Ht + lw) / (2 * PI) + n
	}

	fun solarTransitJ(ds : Double , M : Double , L : Double) : Double
	{
		return J2000 + ds + 0.0053 * sin(M) - 0.0069 * sin(2 * L)
	}

	fun hourAngle(h : Double , phi : Double , d : Double) : Double
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
	fun getSetJ(
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
		result["nadir"] = fromJulian(jNoon - 0.5)
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

	/**
	 * moon calculations, based on http://aa.quae.nl/en/reken/hemelpositie.html formulas
	 *
	 * @param d
	 * @return
	 */
	fun moonCoords(d : Double) : Map<String , Double>
	{
		// geocentric ecliptic coordinates of the moon
		val result : MutableMap<String , Double> = Maps.newConcurrentMap()
		// ecliptic longitude
		val L = rad * (218.316 + 13.176396 * d)
		// mean anomaly
		val M = rad * (134.963 + 13.064993 * d)
		//// mean distance
		val F = rad * (93.272 + 13.229350 * d)
		// longitude
		val l : Double = L + rad * 6.289 * sin(M)
		// latitude
		val b : Double = rad * 5.128 * sin(F)
		// distance to the moon in km
		val dt : Double = 385001 - 20905 * cos(M)
		result["ra"] = rightAscension(l , b)
		result["dec"] = declination(l , b)
		result["dist"] = dt
		return result
	}

	/**
	 * calculates moon position for a given date and latitude/longitude
	 * 计算给定日期和纬度/经度的月亮位置
	 *
	 * @param date
	 * @param lat
	 * @param lng
	 * @return
	 */
	fun getMoonPosition(date : Date , lat : Double , lng : Double) : Map<String , Double>
	{
		val result : MutableMap<String , Double> = Maps.newConcurrentMap()
		val lw = rad * - lng
		val phi = rad * lat
		val d = toDays(date)
		val c = moonCoords(d)
		val H = siderealTime(d , lw) - c["ra"] !!
		var h = altitude(H , phi , c["dec"] !!)
		// formula 14.1 of "Astronomical Algorithms" 2nd edition by Jean Meeus (Willmann-Bell, Richmond) 1998.
		val pa : Double =
			atan2(sin(H) , tan(phi) * cos(c["dec"] as Double) - sin(c["dec"] as Double) * cos(H))
		// altitude correction for refraction
		h = h + astroRefraction(h)
		result["azimuth"] = azimuth(H , phi , c["dec"] as Double)
		result["altitude"] = h
		result["distance"] = c["dist"] !!
		result["parallacticAngle"] = pa
		return result
	}

	/**
	 * calculations for illumination parameters of the moon
	 * 月球照明参数的计算
	 * // based on http://idlastro.gsfc.nasa.gov/ftp/pro/astro/mphase.pro formulas and
	 * // Chapter 48 of "Astronomical Algorithms" 2nd edition by Jean Meeus (Willmann-Bell, Richmond) 1998.
	 *
	 * @param date
	 * @return
	 */
	fun getMoonIllumination(date : Date?) : Map<String , Double>
	{
		val result : MutableMap<String , Double> = Maps.newConcurrentMap()
		val d = toDays(date ?: Date())
		val s = sunCoords(d)
		val m = moonCoords(d)
		// distance from Earth to Sun in km
		val sdist = 149598000.0
		val phi : Double = acos(
				sin(s["dec"] !!) * sin(m["dec"] !!) + cos(s["dec"] !!) * cos(m["dec"] !!) * cos(
						s["ra"] !! - m["ra"] !!
																							   )
							   )
		val inc : Double = atan2(sdist * sin(phi) , m["dist"]?.minus(sdist * cos(phi)) ?: 0.0)
		val angle : Double = atan2(
				cos(s["dec"] !!) * sin(s["ra"] !! - m["ra"] !!) , sin(s["dec"] !!) * cos(
				m["dec"] !!
																						) - cos(
				s["dec"] !!
																							   ) * sin(
				m["dec"] !!
																									  ) * cos(
				s["ra"] !! - m["ra"] !!
																											 )
								  )
		result["fraction"] = (1 + cos(inc)) / 2
		result["phase"] = 0.5 + 0.5 * inc * (if (angle < 0) - 1 else 1) / PI
		result["angle"] = angle
		return result
	}

	fun hoursLater(date : Date , h : Double) : Date
	{
		return Date((date.time + h * dayMs / 24).toLong())
	}

	/**
	 * calculations for moon rise/set times
	 * 月亮升起/落下时间的计算
	 *
	 * @param date
	 * @param lat
	 * @param lng
	 * @return
	 */
	fun getMoonTimes(date : Date? , lat : Double , lng : Double) : Map<String , Date>
	{
		return getMoonTimes(date , lat , lng , false)
	}

	/**
	 * calculations for moon rise/set times are based on http://www.stargazing.net/kepler/moonrise.html article
	 * 月亮升起/落下时间的计算
	 *
	 * @param date
	 * @param lat
	 * @param lng
	 * @return
	 */
	fun getMoonTimes(
		date : Date? ,
		lat : Double ,
		lng : Double ,
		isUTC : Boolean ,
					) : Map<String , Date>
	{
		val result : MutableMap<String , Date> = Maps.newConcurrentMap()
		//is GMT
		var calendar : Calendar = Calendar.getInstance()
		if (isUTC)
		{
			calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
		}
		calendar.time = date
		calendar.set(Calendar.HOUR_OF_DAY , 0)
		calendar.set(Calendar.MINUTE , 0)
		calendar.set(Calendar.SECOND , 0)
		calendar.set(Calendar.MILLISECOND , 0)
		val t : Date = calendar.time
		val hc = 0.133 * rad
		var h0 = getMoonPosition(t , lat , lng)["altitude"] !! - hc
		var h1 : Double
		var h2 : Double
		var rise = 0.0
		var set = 0.0
		var a : Double
		var b : Double
		var xe : Double
		var ye = 0.0
		var d : Double
		var roots : Double
		var x1 = 0.0
		var x2 = 0.0
		var dx : Double

		// go in 2-hour chunks, each time seeing if a 3-point quadratic curve crosses zero (which means rise or set)
		var i = 1
		while (i <= 24)
		{
			h1 = getMoonPosition(hoursLater(t , i.toDouble()) , lat , lng)["altitude"] !! - hc
			h2 = getMoonPosition(hoursLater(t , (i + 1).toDouble()) , lat , lng)["altitude"] !! - hc
			a = (h0 + h2) / 2 - h1
			b = (h2 - h0) / 2
			xe = - b / (2 * a)
			ye = (a * xe + b) * xe + h1
			d = b * b - 4 * a * h1
			roots = 0.0
			if (d >= 0)
			{
				dx = Math.sqrt(d) / (Math.abs(a) * 2)
				x1 = xe - dx
				x2 = xe + dx
				if (Math.abs(x1) <= 1)
				{
					roots ++
				}
				if (Math.abs(x2) <= 1)
				{
					roots ++
				}
				if (x1 < - 1)
				{
					x1 = x2
				}
			}
			if (roots == 1.0)
			{
				if (h0 < 0)
				{
					rise = i + x1
				} else
				{
					set = i + x1
				}
			} else if (roots == 2.0)
			{
				rise = i + if (ye < 0) x2 else x1
				set = i + if (ye < 0) x1 else x2
			}
			if (rise != 0.0 && set != 0.0)
			{
				break
			}
			h0 = h2
			i += 2
		}
		if (rise != 0.0)
		{
			result["rise"] = hoursLater(t , rise)
		}
		if (set != 0.0)
		{
			result["set"] = hoursLater(t , set)
		}
		return result
	}
}