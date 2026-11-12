package com.fungast.app.map

import com.fungast.app.data.Spot
import com.fungast.app.data.SpotCategory
import com.fungast.app.data.sortedCategories
import com.fungast.app.location.GeoPoint

/** OpenStreetMap-based vector style from OpenFreeMap: free, no API key, tiles can be stored offline. */
const val MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"

/** Below this zoom the visible area is too large to download (too many tiles). */
const val MIN_DOWNLOAD_ZOOM = 10.0

data class GeoBounds(
    val south: Double,
    val west: Double,
    val north: Double,
    val east: Double,
)

data class MapViewport(val bounds: GeoBounds, val zoom: Double)

/** Marker color as #RRGGBB, so it can be used by the native map styles. */
val SpotCategory.markerColorHex: String
    get() = when (this) {
        SpotCategory.MUSHROOMS -> "#8D5524"
        SpotCategory.CHESTNUTS -> "#C0392B"
        SpotCategory.BERRIES -> "#7B2D8E"
        SpotCategory.ASPARAGUS -> "#3E8E41"
        SpotCategory.OTHER -> "#1F6FB2"
    }

/** Emoji drawn in the map marker; asparagus has no emoji of its own, a sprout is the closest. */
val SpotCategory.markerEmoji: String
    get() = when (this) {
        SpotCategory.MUSHROOMS -> "\uD83C\uDF44" // 🍄
        SpotCategory.CHESTNUTS -> "\uD83C\uDF30" // 🌰
        SpotCategory.BERRIES -> "\uD83E\uDED0" // 🫐
        SpotCategory.ASPARAGUS -> "\uD83C\uDF31" // 🌱
        SpotCategory.OTHER -> "\uD83D\uDCCD" // 📍
    }

/**
 * Older emoji fonts (blueberry needs Android 11) draw newer emoji as empty boxes: this is used instead
 * when the platform can't draw [markerEmoji].
 */
val SpotCategory.markerEmojiFallback: String
    get() = when (this) {
        SpotCategory.BERRIES -> "\uD83C\uDF53" // 🍓
        else -> markerEmoji
    }

/**
 * Identifies the marker image of a spot: spots with the same categories share one image, so the map
 * only draws as many images as there are category combinations.
 */
val Spot.markerImageId: String
    get() = "spot-" + sortedCategories.ifEmpty { listOf(SpotCategory.OTHER) }.joinToString("+") { it.name }

/** A route as a line on the map; [recording] is drawn differently from the finished ones. */
data class RouteTrack(val routeId: Long, val points: List<GeoPoint>, val recording: Boolean)
