package com.fungast.app.map

import com.fungast.app.data.SpotCategory

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
