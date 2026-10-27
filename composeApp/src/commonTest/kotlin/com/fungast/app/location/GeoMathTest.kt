package com.fungast.app.location

import kotlin.test.Test
import kotlin.test.assertEquals

class GeoMathTest {
    private val milan = GeoPoint(45.4642, 9.1900)
    private val rome = GeoPoint(41.9028, 12.4964)

    @Test
    fun distanceBetweenCitiesMatchesKnownValue() {
        assertEquals(477_000.0, distanceMeters(milan, rome), absoluteTolerance = 2_000.0)
    }

    @Test
    fun distanceAtSmallScaleIsAccurate() {
        // 0.001° of latitude is about 111 m everywhere
        assertEquals(111.2, distanceMeters(GeoPoint(46.0, 11.0), GeoPoint(46.001, 11.0)), absoluteTolerance = 0.5)
        assertEquals(0.0, distanceMeters(milan, milan))
    }

    @Test
    fun bearingPointsToCardinalDirections() {
        val origin = GeoPoint(0.0, 0.0)
        assertEquals(0.0, bearingDegrees(origin, GeoPoint(1.0, 0.0)), absoluteTolerance = 1e-9)
        assertEquals(90.0, bearingDegrees(origin, GeoPoint(0.0, 1.0)), absoluteTolerance = 1e-9)
        assertEquals(180.0, bearingDegrees(origin, GeoPoint(-1.0, 0.0)), absoluteTolerance = 1e-9)
        assertEquals(270.0, bearingDegrees(origin, GeoPoint(0.0, -1.0)), absoluteTolerance = 1e-9)
    }

    @Test
    fun bearingFromMilanToRomeIsSouthEast() {
        assertEquals(145.0, bearingDegrees(milan, rome), absoluteTolerance = 0.5)
    }

    @Test
    fun normalizeWrapsIntoZeroTo360() {
        assertEquals(350.0, normalizeDegrees(-10.0))
        assertEquals(10.0, normalizeDegrees(370.0))
        assertEquals(0.0, normalizeDegrees(360.0))
    }
}
