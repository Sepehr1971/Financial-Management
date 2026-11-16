package com.example.utils

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/**
 * Accurately calculates Solar Prayer Times (اوقات شرعی) specifically calibrated for Tehran (افق تهران),
 * using the standard Institute of Geophysics, University of Tehran (مؤسسه ژئوفیزیک دانشگاه تهران / باد صبا)
 * method:
 * - Fajr angle: 17.7°
 * - Maghrib angle: 4.5°
 * - Tehran coordinates: Lat 35.6892° N, Lon 51.3890° E
 * - Standard Iran Timezone: UTC + 3.5
 */
object TehranPrayerTimesHelper {

    private const val TEHRAN_LAT = 35.6892
    private const val TEHRAN_LNG = 51.3890
    private const val TIMEZONE_OFFSET = 3.5

    data class PrayerTimesData(
        val fajr: String,        // اذان صبح
        val sunrise: String,     // طلوع آفتاب
        val dhuhr: String,       // اذان ظهر
        val sunset: String,      // غروب آفتاب
        val maghrib: String,     // اذان مغرب
        val midnight: String,    // نیمه‌شب شرعی
        val nextPrayerName: String,
        val nextPrayerTime: String
    )

    fun getTehranPrayerTimes(currentTimeMillis: Long = System.currentTimeMillis()): PrayerTimesData {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tehran")).apply {
            timeInMillis = currentTimeMillis
        }

        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        // Julian Day calculation
        val jd = julianDay(year, month, day)
        val d = jd - 2451545.0

        // Mean solar longitude
        val q = fixAngle(280.459 + 0.98564736 * d)
        // Mean anomaly
        val g = fixAngle(357.529 + 0.98560028 * d)

        // Sun ecliptic longitude
        val l = fixAngle(q + 1.915 * sin(degToRad(g)) + 0.020 * sin(degToRad(2 * g)))
        // Obliquity of ecliptic
        val e = 23.439 - 0.00000036 * d

        // Sun right ascension and declination
        val ra = radToDeg(atan(cos(degToRad(e)) * tan(degToRad(l)))) / 15.0
        val rightAscension = if (l in 90.0..270.0) ra + 12.0 else if (l > 270.0) ra + 24.0 else ra
        val dec = radToDeg(asin(sin(degToRad(e)) * sin(degToRad(l))))

        // Equation of time in hours
        val eqt = q / 15.0 - rightAscension

        // Noon (Dhuhr) in hours
        val noon = fixHour(12.0 + TIMEZONE_OFFSET - TEHRAN_LNG / 15.0 - eqt)

        // Sunrise and Sunset (zenith 90.833°)
        val sunHourAngle = hourAngle(90.833, dec)
        val sunrise = noon - sunHourAngle
        val sunset = noon + sunHourAngle

        // Fajr: Institute of Geophysics (17.7° below horizon -> zenith 107.7°)
        val fajrHourAngle = hourAngle(90.0 + 17.7, dec)
        val fajr = noon - fajrHourAngle

        // Maghrib: Institute of Geophysics (4.5° below horizon -> zenith 94.5°)
        val maghribHourAngle = hourAngle(90.0 + 4.5, dec)
        val maghrib = noon + maghribHourAngle

        // Midnight (نیمه‌شب شرعی): Midpoint between Maghrib and next morning's Fajr
        val midnight = maghrib + ((fajr + 24.0) - maghrib) / 2.0

        val fajrMin = (fajr * 60).toInt()
        val sunriseMin = (sunrise * 60).toInt()
        val dhuhrMin = (noon * 60).toInt()
        val sunsetMin = (sunset * 60).toInt()
        val maghribMin = (maghrib * 60).toInt()
        val midnightMin = (midnight * 60).toInt() % 1440

        val fajrStr = formatMinutes(fajrMin)
        val sunriseStr = formatMinutes(sunriseMin)
        val dhuhrStr = formatMinutes(dhuhrMin)
        val sunsetStr = formatMinutes(sunsetMin)
        val maghribStr = formatMinutes(maghribMin)
        val midnightStr = formatMinutes(midnightMin)

        // Determine next upcoming prayer
        val (nextName, nextTime) = when {
            currentMinutes < fajrMin -> "اذان صبح" to fajrStr
            currentMinutes < sunriseMin -> "طلوع آفتاب" to sunriseStr
            currentMinutes < dhuhrMin -> "اذان ظهر" to dhuhrStr
            currentMinutes < sunsetMin -> "غروب آفتاب" to sunsetStr
            currentMinutes < maghribMin -> "اذان مغرب" to maghribStr
            currentMinutes < midnightMin -> "نیمه‌شب شرعی" to midnightStr
            else -> "اذان صبح فردا" to fajrStr
        }

        return PrayerTimesData(
            fajr = fajrStr,
            sunrise = sunriseStr,
            dhuhr = dhuhrStr,
            sunset = sunsetStr,
            maghrib = maghribStr,
            midnight = midnightStr,
            nextPrayerName = nextName,
            nextPrayerTime = nextTime
        )
    }

    private fun hourAngle(zenith: Double, dec: Double): Double {
        val latRad = degToRad(TEHRAN_LAT)
        val decRad = degToRad(dec)
        val cosH = (cos(degToRad(zenith)) - sin(latRad) * sin(decRad)) / (cos(latRad) * cos(decRad))
        val clamped = cosH.coerceIn(-1.0, 1.0)
        return radToDeg(acos(clamped)) / 15.0
    }

    private fun formatMinutes(minutes: Int): String {
        val h = (minutes / 60) % 24
        val m = minutes % 60
        return String.format(java.util.Locale.US, "%02d:%02d", h, m)
    }

    private fun julianDay(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun fixAngle(a: Double): Double {
        var res = a - 360.0 * floor(a / 360.0)
        if (res < 0) res += 360.0
        return res
    }

    private fun fixHour(h: Double): Double {
        var res = h - 24.0 * floor(h / 24.0)
        if (res < 0) res += 24.0
        return res
    }

    private fun degToRad(d: Double): Double = d * Math.PI / 180.0
    private fun radToDeg(r: Double): Double = r * 180.0 / Math.PI
}
