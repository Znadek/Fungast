package com.fungast.app.location

/** Follows HTTP redirects; only used to expand short Google Maps links. */
interface LinkResolver {
    /** Returns the final URL after redirects, or null on network errors. */
    suspend fun resolve(url: String): String?
}

sealed interface MapLinkResult {
    data class Found(val point: GeoPoint) : MapLinkResult

    /** The text is not a map link or coordinates, or the place has no coordinates in its link. */
    data object NotFound : MapLinkResult

    /** A short link could not be expanded (no connection). */
    data object NetworkError : MapLinkResult
}

/**
 * Reads a position from what the user pasted: a Google Maps URL (also the "Name https://maps.app.goo.gl/…"
 * text that the Maps share button produces) or plain coordinates. Only short links need the network.
 */
suspend fun locationFromMapLink(text: String, resolver: LinkResolver): MapLinkResult {
    parseMapCoordinates(text)?.let { return MapLinkResult.Found(it) }
    val shortLink = findShortMapsLink(text) ?: return MapLinkResult.NotFound
    val expanded = resolver.resolve(shortLink) ?: return MapLinkResult.NetworkError
    return parseMapCoordinates(expanded)?.let { MapLinkResult.Found(it) } ?: MapLinkResult.NotFound
}

private const val NUMBER = """-?\d{1,3}(?:\.\d+)?"""

// Most precise first: the pin of a place, then explicit query parameters, then the map center
private val PLACE_PIN = Regex("""!3d($NUMBER)!4d($NUMBER)""")
private val QUERY_PARAMETER = Regex(
    """[?&](?:q|query|ll|sll|destination|daddr|center)=(?:loc:)?\s*($NUMBER)\s*,\s*($NUMBER)"""
)
private val MAP_CENTER = Regex("""@($NUMBER),($NUMBER)""")
private val PLAIN_COORDINATES = Regex("""^\s*($NUMBER)\s*[,;\s]\s*($NUMBER)\s*$""")

private val SHORT_LINK = Regex("""https://(?:maps\.app\.goo\.gl|goo\.gl/maps)/[A-Za-z0-9_-]+""")

/** Coordinates written in [text] itself, without any network access; null if there are none. */
fun parseMapCoordinates(text: String): GeoPoint? {
    // Links that went through a redirect or a consent page carry the real URL percent-encoded
    val decoded = percentDecode(text).replace('+', ' ')
    val patterns = if ("://" in decoded) listOf(PLACE_PIN, QUERY_PARAMETER, MAP_CENTER) else listOf(PLAIN_COORDINATES)
    for (pattern in patterns) {
        val match = pattern.find(decoded) ?: continue
        val latitude = match.groupValues[1].toDoubleOrNull() ?: continue
        val longitude = match.groupValues[2].toDoubleOrNull() ?: continue
        if (latitude in -90.0..90.0 && longitude in -180.0..180.0) return GeoPoint(latitude, longitude)
    }
    return null
}

/** The short Google Maps link in [text], if any; it hides the coordinates behind a redirect. */
fun findShortMapsLink(text: String): String? = SHORT_LINK.find(text)?.value

private fun percentDecode(text: String): String {
    if ('%' !in text) return text
    val bytes = mutableListOf<Byte>()
    var i = 0
    while (i < text.length) {
        val hex = if (text[i] == '%' && i + 2 <= text.lastIndex) text.substring(i + 1, i + 3).toIntOrNull(16) else null
        if (hex != null) {
            bytes += hex.toByte()
            i += 3
        } else {
            text[i].toString().encodeToByteArray().forEach { bytes += it }
            i++
        }
    }
    return bytes.toByteArray().decodeToString()
}
