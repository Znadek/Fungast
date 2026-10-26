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
}
