package com.example.starfinder

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** 별의 하늘 좌표 → 지금 내 위치에서의 고도·방위각 계산 */
object Astro {

    /** @return Pair(고도°, 방위각°). 방위각은 북쪽 0°, 동쪽 90° */
    fun altAz(
        raDeg: Double, decDeg: Double,
        latDeg: Double, lonDeg: Double,
        utcMillis: Long
    ): Pair<Double, Double> {
        val jd = utcMillis / 86_400_000.0 + 2440587.5
        val d = jd - 2451545.0

        // 항성시: 지금 하늘이 얼마나 돌아갔는지
        val gmst = norm(280.46061837 + 360.98564736629 * d)
        val lst = norm(gmst + lonDeg)
        val ha = Math.toRadians(norm(lst - raDeg))

        val dec = Math.toRadians(decDeg)
        val lat = Math.toRadians(latDeg)

        val sinAlt = sin(dec) * sin(lat) + cos(dec) * cos(lat) * cos(ha)
        val alt = asin(sinAlt.coerceIn(-1.0, 1.0))
        val az = atan2(
            -cos(dec) * sin(ha),
            sin(dec) * cos(lat) - cos(dec) * sin(lat) * cos(ha)

        )
        return Pair(Math.toDegrees(alt), norm(Math.toDegrees(az)))
    }

    private fun norm(x: Double) = ((x % 360.0) + 360.0) % 360.0
}