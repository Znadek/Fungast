package com.fungast.app.exchange

import com.fungast.app.data.Spot
import com.fungast.app.data.SpotCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.datetime.Instant

class SpotQrTest {
    private val now = Instant.parse("2026-10-27T18:00:00Z")
    private val spot = Spot(
        id = 7,
        name = "Faggeta \"alta\"",
        category = SpotCategory.MUSHROOMS,
        latitude = 46.1234567,
        longitude = 11.0000001,
        altitude = 1203.4,
        accuracyMeters = 4.5f,
        notes = "Porcini dopo la pioggia\nsotto i faggi",
        createdAt = Instant.parse("2026-09-12T07:30:00Z").toEpochMilliseconds(),
    )

    @Test
    fun qrRoundTripKeepsEveryFieldButId() {
        val text = encodeSpotQr(spot)

        assertFalse('\n' in text, "a QR payload should be a single line")
        assertTrue(isSpotQr(text))
        assertEquals(listOf(spot.copy(id = 0)), decodeSpots(text, now))
    }

    @Test
    fun longNotesAreShortened() {
        val decoded = decodeSpots(encodeSpotQr(spot.copy(notes = "a".repeat(1000))), now).single()

        assertEquals(QR_MAX_NOTES_LENGTH, decoded.notes.length)
        assertTrue(decoded.notes.endsWith("…"))
    }

    @Test
    fun otherCodesAreNotSpots() {
        assertFalse(isSpotQr("https://maps.app.goo.gl/abc123"))
        assertFalse(isSpotQr("46.1, 11.2"))
    }
}
