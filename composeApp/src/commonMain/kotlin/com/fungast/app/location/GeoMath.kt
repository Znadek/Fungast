package com.fungast.app.location

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_METERS = 6_371_000.0

private fun Double.toRadians() = this * PI / 180.0
private fun Double.toDegrees() = this * 180.0 / PI

/** Great-circle (haversine) distance in meters. */
fun distanceMeters(from: GeoPoint, to: GeoPoint): Double {
    val dLat = (to.latitude - from.latitude).toRadians()
    val dLon = (to.longitude - from.longitude).toRadians()
    val a = sin(dLat / 2).let { it * it } +
        cos(from.latitude.toRadians()) * cos(to.latitude.toRadians()) * sin(dLon / 2).let { it * it }
    return 2 * EARTH_RADIUS_METERS * asin(sqrt(a))
}

/** Initial bearing from [from] to [to], in degrees clockwise from true north, in [0, 360). */
fun bearingDegrees(from: GeoPoint, to: GeoPoint): Double {
    val lat1 = from.latitude.toRadians()
    val lat2 = to.latitude.toRadians()
    val dLon = (to.longitude - from.longitude).toRadians()
    val y = sin(dLon) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
    return normalizeDegrees(atan2(y, x).toDegrees())
}

/** Maps any angle to [0, 360). */
fun normalizeDegrees(degrees: Double): Double = ((degrees % 360.0) + 360.0) % 360.0
