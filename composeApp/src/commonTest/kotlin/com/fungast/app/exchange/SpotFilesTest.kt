package com.fungast.app.exchange

import com.fungast.app.data.Spot
import com.fungast.app.data.SpotCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.datetime.Instant

class SpotFilesTest {
    private val now = Instant.parse("2026-10-27T18:00:00Z")
    private val spots = listOf(
        Spot(
            id = 7,
            name = "Faggeta <alta> & \"bassa\"",
            category = SpotCategory.MUSHROOMS,
            latitude = 46.1234567,
            longitude = 11.0000001,
            altitude = 1203.4,
            accuracyMeters = 4.5f,
            notes = "Porcini dopo la pioggia\nsotto i faggi",
            createdAt = Instant.parse("2026-09-12T07:30:00Z").toEpochMilliseconds(),
        ),
        Spot(
            id = 8,
            name = "Castagneto",
            category = SpotCategory.CHESTNUTS,
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
    fun gpxRoundTripKeepsNameNotesCategoryPositionAndDate() {
        val text = encodeSpots(spots, ExportFormat.GPX, now)
        val decoded = decodeSpots(text, now)

        assertTrue("1.0E-4" !in text, "coordinates must not use scientific notation")
        assertEquals(spots.map { it.copy(id = 0, accuracyMeters = null) }, decoded)
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
            assertEquals(SpotCategory.OTHER, category)
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

        assertEquals(SpotCategory.OTHER, decodeSpots(text, now).single().category)
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
