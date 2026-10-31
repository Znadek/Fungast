package com.fungast.app.exchange

import com.fungast.app.data.Spot
import kotlinx.datetime.Instant

enum class ExportFormat(val extension: String, val mimeType: String) {
    /** Fungast's own format: keeps every field, for backups and moving to another phone. */
    FUNGAST("fungast", "application/json"),

    /** Standard waypoints, readable by other map/GPS apps; categories are kept as text, accuracy and find dates are lost. */
    GPX("gpx", "application/gpx+xml"),
}

/** A spot read from a file, with the JPEG photos it carried; [spot] has no photo names until they are stored. */
class DecodedSpot(val spot: Spot, val photos: List<ByteArray> = emptyList())

data class SharedFile(val name: String, val mimeType: String, val content: String)

/** The file is not a Fungast or GPX file, or it is damaged. */
class SpotFileException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** [photos] (name → JPEG) are embedded in .fungast files only; GPX has no place for them. */
fun encodeSpots(
    spots: List<Spot>,
    format: ExportFormat,
    exportedAt: Instant,
    photos: Map<String, ByteArray> = emptyMap(),
): String = when (format) {
    ExportFormat.FUNGAST -> encodeFungast(spots, exportedAt, photos = photos)
    ExportFormat.GPX -> encodeGpx(spots, exportedAt)
}

/**
 * Reads spots from a .fungast or GPX file, recognized by content rather than by name, since
 * file pickers and chat apps often rename files. Returned spots have id 0, ready to insert.
 * Spots without a date get [now].
 */
fun decodeSpots(text: String, now: Instant): List<Spot> = decodeSpotsWithPhotos(text, now).map { it.spot }

/** Like [decodeSpots], also returning the photos embedded in a .fungast backup. */
fun decodeSpotsWithPhotos(text: String, now: Instant): List<DecodedSpot> {
    val content = text.trimStart('﻿', ' ', '\t', '\r', '\n')
    return when {
        content.startsWith("{") -> decodeFungast(content)
        content.startsWith("<") -> decodeGpx(content, now)
        else -> throw SpotFileException("Formato del file non riconosciuto")
    }.filter { it.spot.latitude in -90.0..90.0 && it.spot.longitude in -180.0..180.0 }
}
