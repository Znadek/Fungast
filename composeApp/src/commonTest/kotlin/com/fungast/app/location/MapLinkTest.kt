package com.fungast.app.location

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

private class FakeLinkResolver(private val redirects: Map<String, String>) : LinkResolver {
    val requested = mutableListOf<String>()
    override suspend fun resolve(url: String): String? {
        requested += url
        return redirects[url]
    }
}

class MapLinkTest {
    private fun assertParsed(latitude: Double, longitude: Double, text: String) =
        assertEquals(GeoPoint(latitude, longitude), parseMapCoordinates(text), text)

    @Test
    fun placePinWinsOverMapCenter() = assertParsed(
        46.0123456, 11.6543210,
        "https://www.google.com/maps/place/Rifugio/@46.0100000,11.6500000,15z/data=!3m1!4b1!4m6!3m5!1s0x0:0x0!8m2!3d46.0123456!4d11.654321",
    )

    @Test
    fun readsQueryParameters() {
        assertParsed(45.4642, 9.19, "https://maps.google.com/?q=45.4642,9.19")
        assertParsed(45.4642, 9.19, "https://www.google.com/maps?q=loc:45.4642+9.19".replace("+", ",+"))
        assertParsed(-33.8568, 151.2153, "https://www.google.com/maps/search/?api=1&query=-33.8568%2C151.2153")
        assertParsed(46.5, 11.25, "https://www.google.com/maps/dir/?api=1&destination=46.5,11.25&travelmode=walking")
        assertParsed(46.5, 11.25, "https://maps.google.it/maps?ll=46.5,11.25&z=14")
    }

    @Test
    fun fallsBackToMapCenter() = assertParsed(46.1, 11.2, "https://www.google.com/maps/@46.1,11.2,14z")

    @Test
    fun readsLinkInsideConsentPageRedirect() = assertParsed(
        46.0123, 11.6543,
        "https://consent.google.com/ml?continue=https://www.google.com/maps/place/X/data%3D!4m2!3m1!3d46.0123!4d11.6543&gl=IT",
    )

    @Test
    fun readsPlainCoordinates() {
        assertParsed(45.4642, 9.19, "45.4642, 9.19")
        assertParsed(45.4642, -9.19, "  45.4642 -9.19 ")
        assertParsed(45.4642, 9.19, "45.4642;9.19")
    }

    @Test
    fun rejectsTextWithoutCoordinates() {
        assertNull(parseMapCoordinates("https://www.google.com/maps/place/Rifugio+Brentei"))
        assertNull(parseMapCoordinates("Faggeta sopra il rifugio"))
        assertNull(parseMapCoordinates("https://maps.app.goo.gl/AbC123xyz"))
        assertNull(parseMapCoordinates("95.0, 9.0"), "latitude out of range")
        assertNull(parseMapCoordinates("Via Roma 12, 20121 Milano"))
    }

    @Test
    fun fullLinkIsReadOffline() = runTest {
        val resolver = FakeLinkResolver(emptyMap())

        val result = locationFromMapLink("https://maps.google.com/?q=45.4642,9.19", resolver)

        assertEquals(MapLinkResult.Found(GeoPoint(45.4642, 9.19)), result)
        assertEquals(emptyList(), resolver.requested)
    }

    @Test
    fun shortLinkFromShareTextIsExpanded() = runTest {
        val resolver = FakeLinkResolver(
            mapOf("https://maps.app.goo.gl/AbC123xyz" to "https://www.google.com/maps/place/P/@46.2,11.3,17z/data=!3d46.21!4d11.31")
        )

        val result = locationFromMapLink("Rifugio Brentei\nhttps://maps.app.goo.gl/AbC123xyz?g_st=ac", resolver)

        assertEquals(MapLinkResult.Found(GeoPoint(46.21, 11.31)), result)
        assertEquals(listOf("https://maps.app.goo.gl/AbC123xyz"), resolver.requested)
    }

    @Test
    fun shortLinkOfflineOrWithoutCoordinates() = runTest {
        val resolver = FakeLinkResolver(mapOf("https://goo.gl/maps/Named1" to "https://www.google.com/maps/place/Solo+nome"))

        assertEquals(MapLinkResult.NetworkError, locationFromMapLink("https://maps.app.goo.gl/Unknown", resolver))
        assertEquals(MapLinkResult.NotFound, locationFromMapLink("https://goo.gl/maps/Named1", resolver))
        assertEquals(MapLinkResult.NotFound, locationFromMapLink("https://example.com/redirect", resolver))
        assertEquals(2, resolver.requested.size, "only Google short links may reach the network")
    }
}
