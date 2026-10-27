package com.fungast.app.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

class FormatTest {
    @Test
    fun coordinateIsRoundedToFiveDecimals() {
        assertEquals("45.12346", formatCoordinate(45.123456))
        assertEquals("9.00000", formatCoordinate(9.0))
        assertEquals("0.00010", formatCoordinate(0.0001))
    }

    @Test
    fun negativeCoordinateKeepsSign() {
        assertEquals("-3.70379", formatCoordinate(-3.703790))
        assertEquals("-0.50000", formatCoordinate(-0.5))
    }

    @Test
    fun tinyNegativeCoordinateDoesNotShowMinusZero() {
        assertEquals("0.00000", formatCoordinate(-0.000001))
    }

    @Test
    fun latLonIsCommaSeparated() {
        assertEquals("46.50000, 11.25000", formatLatLon(46.5, 11.25))
    }

    @Test
    fun dateTimeIsPadded() {
        val millis = LocalDateTime(2026, 3, 7, 8, 5).toInstant(TimeZone.UTC).toEpochMilliseconds()
        assertEquals("07/03/2026 08:05", formatDateTime(millis, TimeZone.UTC))
    }

    @Test
    fun distanceUsesMetersThenKilometers() {
        assertEquals("0 m", formatDistance(0.2))
        assertEquals("850 m", formatDistance(849.6))
        assertEquals("999 m", formatDistance(999.4))
        assertEquals("1,0 km", formatDistance(999.6))
        assertEquals("2,4 km", formatDistance(2_449.0))
        assertEquals("10 km", formatDistance(9_960.0))
        assertEquals("27 km", formatDistance(26_700.0))
    }

    @Test
    fun cardinalDirectionPicksNearestOfEight() {
        assertEquals("Nord", cardinalDirection(0.0))
        assertEquals("Nord", cardinalDirection(355.0))
        assertEquals("Nord-Est", cardinalDirection(30.0))
        assertEquals("Est", cardinalDirection(90.0))
        assertEquals("Sud-Ovest", cardinalDirection(225.0))
        assertEquals("Nord-Ovest", cardinalDirection(-40.0))
    }
}
