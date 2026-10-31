package com.fungast.app.exchange

import com.fungast.app.data.Spot
import com.fungast.app.data.SpotCategory
import com.fungast.app.data.SpotQuantity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

class SpotFilesTest {
    private val now = Instant.parse("2026-10-27T18:00:00Z")
    private val spots = listOf(
        Spot(
            id = 7,
            name = "Faggeta <alta> & \"bassa\"",
            categories = setOf(SpotCategory.MUSHROOMS, SpotCategory.BERRIES),
            latitude = 46.1234567,
            longitude = 11.0000001,
            altitude = 1203.4,
            accuracyMeters = 4.5f,
            notes = "Porcini dopo la pioggia\nsotto i faggi",
            createdAt = Instant.parse("2026-09-12T07:30:00Z").toEpochMilliseconds(),
            lastFoundOn = LocalDate(2026, 10, 20),
            previousFoundOn = LocalDate(2025, 9, 30),
            quantity = SpotQuantity.ABUNDANT,
        ),
        Spot(
            id = 8,
            name = "Castagneto",
            categories = setOf(SpotCategory.CHESTNUTS),
            latitude = -0.0001,
            longitude = -3.5,
            altitude = null,
            accuracyMeters = null,
            notes = "",
            createdAt = Instant.parse("2026-10-01T15:00:00Z").toEpochMilliseconds(),
        ),
    )

    @Test
    fun fungastRoundTripKeepsEveryFieldButId() {
        val decoded = decodeSpots(encodeSpots(spots, ExportFormat.FUNGAST, now), now)

        assertEquals(spots.map { it.copy(id = 0) }, decoded)
    }

    @Test
    fun gpxRoundTripKeepsNameNotesCategoriesPositionAndDate() {
        val text = encodeSpots(spots, ExportFormat.GPX, now)
        val decoded = decodeSpots(text, now)

        assertTrue("1.0E-4" !in text, "coordinates must not use scientific notation")
        assertEquals(spots.map { it.copy(id = 0, accuracyMeters = null, lastFoundOn = null, previousFoundOn = null) }, decoded)
    }

    @Test
    fun gpxQuantityWithoutNotesIsReadBackAsQuantityOnly() {
        val spot = spots[1].copy(quantity = SpotQuantity.LOW)
        val text = encodeSpots(listOf(spot), ExportFormat.GPX, now)

        assertTrue("<desc>Quantità: poco</desc>" in text)
        val decoded = decodeSpots(text, now).single()
        assertEquals("", decoded.notes)
        assertEquals(SpotQuantity.LOW, decoded.quantity)
    }

    @Test
    fun unknownQuantityInFungastFileIsDropped() {
        val text = encodeSpots(spots.take(1), ExportFormat.FUNGAST, now).replace("\"ABUNDANT\"", "\"HUGE\"")

        assertEquals(null, decodeSpots(text, now).single().quantity)
    }

    @Test
    fun readsGpxWrittenByOtherApps() {
        val gpx = """
            <?xml version="1.0"?>
            <gpx version="1.1" creator="OtherApp" xmlns="http://www.topografix.com/GPX/1/1">
              <wpt lon='11.5' lat='46.25'><name><![CDATA[Bosco <nord>]]></name><cmt>Mirtilli &#224; volont&#xE0;</cmt></wpt>
              <wpt lat="46.3" lon="11.6"/>
              <wpt lat="not-a-number" lon="11.6"><name>Rotto</name></wpt>
              <trk><name>Giro</name></trk>
            </gpx>
        """.trimIndent()

        val decoded = decodeSpots(gpx, now)

        assertEquals(2, decoded.size)
        with(decoded[0]) {
            assertEquals("Bosco <nord>", name)
            assertEquals("Mirtilli à volontà", notes)
            assertEquals(46.25, latitude)
            assertEquals(11.5, longitude)
            assertEquals(setOf(SpotCategory.OTHER), categories)
            assertEquals(now.toEpochMilliseconds(), createdAt)
        }
        assertEquals(DEFAULT_IMPORTED_NAME, decoded[1].name)
    }

    @Test
    fun fileWithByteOrderMarkIsRead() {
        val text = "﻿" + encodeSpots(spots.take(1), ExportFormat.FUNGAST, now)

        assertEquals(1, decodeSpots(text, now).size)
    }

    @Test
    fun unknownCategoryInFungastFileBecomesOther() {
        val text = encodeSpots(spots.take(1), ExportFormat.FUNGAST, now).replace("MUSHROOMS", "TRUFFLES")

        assertEquals(setOf(SpotCategory.OTHER, SpotCategory.BERRIES), decodeSpots(text, now).single().categories)
    }

    @Test
    fun fungastFileFromSingleCategoryVersionIsRead() {
        val text = """
            {"format":"fungast","version":1,"spots":[
              {"name":"Vecchio","category":"CHESTNUTS","latitude":46.0,"longitude":11.0,"createdAt":"2026-09-12T07:30:00Z"}
            ]}
        """.trimIndent()

        with(decodeSpots(text, now).single()) {
            assertEquals(setOf(SpotCategory.CHESTNUTS), categories)
            assertEquals(null, lastFoundOn)
        }
    }

    @Test
    fun fungastFileStillWritesSingleCategoryForOlderVersions() {
        val text = encodeSpots(spots.take(1), ExportFormat.FUNGAST, now)

        assertTrue("\"category\": \"MUSHROOMS\"" in text)
    }

    @Test
    fun outOfRangeCoordinatesAreDropped() {
        val text = encodeSpots(spots.take(1).map { it.copy(latitude = 123.0) }, ExportFormat.FUNGAST, now)

        assertEquals(emptyList(), decodeSpots(text, now))
    }

    @Test
    fun invalidFilesAreRejected() {
        assertFailsWith<SpotFileException> { decodeSpots("ciao", now) }
        assertFailsWith<SpotFileException> { decodeSpots("""{"format":"fungast","version":1""", now) }
        assertFailsWith<SpotFileException> { decodeSpots("""{"format":"other","version":1,"spots":[]}""", now) }
        assertFailsWith<SpotFileException> { decodeSpots("<html><body/></html>", now) }
    }
}
