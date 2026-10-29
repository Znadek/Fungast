package com.fungast.app.ui

import kotlin.math.abs
import kotlin.math.roundToLong
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** Formats a coordinate with 5 decimals (~1 m), e.g. 45.12345. */
fun formatCoordinate(value: Double): String {
    val scaled = (abs(value) * 100_000).roundToLong()
    val sign = if (value < 0 && scaled != 0L) "-" else ""
    return "$sign${scaled / 100_000}.${(scaled % 100_000).toString().padStart(5, '0')}"
}

fun formatLatLon(latitude: Double, longitude: Double): String =
    "${formatCoordinate(latitude)}, ${formatCoordinate(longitude)}"

/** Formats a distance: meters up to 1 km, then km with one decimal (Italian comma) up to 10 km. */
fun formatDistance(meters: Double): String {
    val rounded = meters.roundToLong()
    if (rounded < 1_000) return "$rounded m"
    val tenthsOfKm = (meters / 100).roundToLong()
    return if (tenthsOfKm < 100) "${tenthsOfKm / 10},${tenthsOfKm % 10} km" else "${(meters / 1_000).roundToLong()} km"
}

private val CARDINAL_DIRECTIONS =
    listOf("Nord", "Nord-Est", "Est", "Sud-Est", "Sud", "Sud-Ovest", "Ovest", "Nord-Ovest")

/** Name of the nearest of the 8 compass directions for a bearing in degrees. */
fun cardinalDirection(bearingDegrees: Double): String {
    val index = (((bearingDegrees % 360 + 360) % 360 + 22.5) / 45).toInt() % 8
    return CARDINAL_DIRECTIONS[index]
}

/** Formats epoch millis as dd/MM/yyyy HH:mm in the given time zone. */
fun formatDateTime(epochMillis: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
    val dt = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(timeZone)
    fun Int.pad() = toString().padStart(2, '0')
    return "${dt.dayOfMonth.pad()}/${dt.monthNumber.pad()}/${dt.year} ${dt.hour.pad()}:${dt.minute.pad()}"
}

/** Formats a date as dd/MM/yyyy. */
fun formatDate(date: LocalDate): String =
    "${date.dayOfMonth.toString().padStart(2, '0')}/${date.monthNumber.toString().padStart(2, '0')}/${date.year}"

/** Formats a duration as "45 min" or "2 h 05 min", rounded down to the minute. */
fun formatDuration(millis: Long): String {
    val minutes = (millis / 60_000).coerceAtLeast(0)
    return if (minutes < 60) "$minutes min" else "${minutes / 60} h ${(minutes % 60).toString().padStart(2, '0')} min"
}
