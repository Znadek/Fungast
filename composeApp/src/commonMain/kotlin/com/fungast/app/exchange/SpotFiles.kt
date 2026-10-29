package com.fungast.app.exchange

import com.fungast.app.data.Spot
import kotlinx.datetime.Instant

enum class ExportFormat(val extension: String, val mimeType: String) {
    /** Fungast's own format: keeps every field, for backups and moving to another phone. */
    FUNGAST("fungast", "application/json"),

    /** Standard waypoints, readable by other map/GPS apps; categories are kept as text, accuracy and find dates are lost. */
    GPX("gpx", "application/gpx+xml"),
}

data class SharedFile(val name: String, val mimeType: String, val content: String)

/** The file is not a Fungast or GPX file, or it is damaged. */
class SpotFileException(message: String, cause: Throwable? = null) : Exception(message, cause)

fun encodeSpots(spots: List<Spot>, format: ExportFormat, exportedAt: Instant): String = when (format) {
    ExportFormat.FUNGAST -> encodeFungast(spots, exportedAt)
    ExportFormat.GPX -> encodeGpx(spots, exportedAt)
}

/**
 * Reads spots from a .fungast or GPX file, recognized by content rather than by name, since
 * file pickers and chat apps often rename files. Returned spots have id 0, ready to insert.
 * Spots without a date get [now].
 */
fun decodeSpots(text: String, now: Instant): List<Spot> {
    val content = text.trimStart('﻿', ' ', '\t', '\r', '\n')
    return when {
        content.startsWith("{") -> decodeFungast(content)
        content.startsWith("<") -> decodeGpx(content, now)
        else -> throw SpotFileException("Formato del file non riconosciuto")
    }.filter { it.latitude in -90.0..90.0 && it.longitude in -180.0..180.0 }
}
