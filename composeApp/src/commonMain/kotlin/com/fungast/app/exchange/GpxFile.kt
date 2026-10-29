package com.fungast.app.exchange

import com.fungast.app.data.Spot
import com.fungast.app.data.SpotCategory
import com.fungast.app.data.sortedCategories
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlinx.datetime.Instant

// GPX 1.1 waypoints (<wpt>) only. Written and read by hand: the subset is tiny and
// a multiplatform XML library would be a large dependency for it.

internal fun encodeGpx(spots: List<Spot>, exportedAt: Instant): String = buildString {
    appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
    appendLine("""<gpx version="1.1" creator="Fungast" xmlns="http://www.topografix.com/GPX/1/1">""")
    appendLine("  <metadata><time>$exportedAt</time></metadata>")
    for (spot in spots) {
        appendLine("""  <wpt lat="${formatDecimal(spot.latitude, 7)}" lon="${formatDecimal(spot.longitude, 7)}">""")
        spot.altitude?.let { appendLine("    <ele>${formatDecimal(it, 1)}</ele>") }
        appendLine("    <time>${Instant.fromEpochMilliseconds(spot.createdAt)}</time>")
        appendLine("    <name>${escapeXml(spot.name)}</name>")
        if (spot.notes.isNotEmpty()) appendLine("    <desc>${escapeXml(spot.notes)}</desc>")
        appendLine("    <type>${escapeXml(spot.sortedCategories.joinToString(", ") { it.label })}</type>")
        appendLine("  </wpt>")
    }
    appendLine("</gpx>")
}

private val WAYPOINT = Regex("""<wpt\b([^>]*?)(?:/>|>([\s\S]*?)</wpt>)""")

internal fun decodeGpx(text: String, now: Instant): List<DecodedSpot> {
    if (!text.contains("<gpx")) throw SpotFileException("Non è un file GPX")
    return WAYPOINT.findAll(text).mapNotNull { match ->
        val attributes = match.groupValues[1]
        val body = match.groupValues[2]
        val latitude = attribute(attributes, "lat")?.toDoubleOrNull() ?: return@mapNotNull null
        val longitude = attribute(attributes, "lon")?.toDoubleOrNull() ?: return@mapNotNull null
        val type = child(body, "type")
        val spot = Spot(
            name = child(body, "name")?.ifEmpty { null } ?: DEFAULT_IMPORTED_NAME,
            categories = type.orEmpty().split(",").map { it.trim() }.filter { it.isNotEmpty() }
                .map { part ->
                    SpotCategory.entries.find { part.equals(it.label, ignoreCase = true) || part == it.name }
                        ?: SpotCategory.OTHER
                }
                .toSet()
                .ifEmpty { setOf(SpotCategory.OTHER) },
            latitude = latitude,
            longitude = longitude,
            altitude = child(body, "ele")?.toDoubleOrNull(),
            accuracyMeters = null,
            notes = child(body, "desc") ?: child(body, "cmt") ?: "",
            createdAt = (child(body, "time")?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: now)
                .toEpochMilliseconds(),
        )
        DecodedSpot(spot)
    }.toList()
}

private fun attribute(attributes: String, name: String): String? =
    Regex("""\b$name\s*=\s*["']([^"']*)["']""").find(attributes)?.groupValues?.get(1)?.trim()

private fun child(body: String, tag: String): String? {
    val raw = Regex("""<$tag\b[^>]*>([\s\S]*?)</$tag>""").find(body)?.groupValues?.get(1) ?: return null
    val cdata = Regex("""<!\[CDATA\[([\s\S]*?)]]>""").find(raw)
    return (cdata?.groupValues?.get(1) ?: unescapeXml(raw)).trim()
}

private fun escapeXml(text: String): String = text
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")

private val ENTITY = Regex("""&(#x[0-9a-fA-F]+|#[0-9]+|amp|lt|gt|quot|apos);""")

private fun unescapeXml(text: String): String = ENTITY.replace(text) { match ->
    when (val entity = match.groupValues[1]) {
        "amp" -> "&"
        "lt" -> "<"
        "gt" -> ">"
        "quot" -> "\""
        "apos" -> "'"
        else -> {
            val code = if (entity.startsWith("#x")) entity.drop(2).toInt(16) else entity.drop(1).toInt()
            if (code in 0..0xFFFF) code.toChar().toString() else match.value
        }
    }
}

/** Fixed-point formatting: Double.toString() may produce "1.0E-4", which GPX readers reject. */
private fun formatDecimal(value: Double, decimals: Int): String {
    var factor = 1L
    repeat(decimals) { factor *= 10 }
    val scaled = (abs(value) * factor).roundToLong()
    val sign = if (value < 0 && scaled != 0L) "-" else ""
    val fraction = (scaled % factor).toString().padStart(decimals, '0')
    return "$sign${scaled / factor}.$fraction"
}
