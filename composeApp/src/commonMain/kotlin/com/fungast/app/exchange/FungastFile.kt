package com.fungast.app.exchange

import com.fungast.app.data.Spot
import com.fungast.app.data.SpotCategory
import com.fungast.app.data.lastTwoFinds
import com.fungast.app.data.sortedCategories
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val FORMAT_ID = "fungast"
private const val FORMAT_VERSION = 1

/** File layout, kept separate from the Room entity so the database can change without breaking old files. */
@Serializable
private data class FungastFileDto(
    val format: String,
    val version: Int,
    val exportedAt: Instant? = null,
    val spots: List<SpotDto>,
)

@Serializable
private data class SpotDto(
    val name: String,
    /** First of [categories]; still written because app versions before multiple categories require it. */
    val category: String? = null,
    val categories: List<String> = emptyList(),
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val accuracyMeters: Float? = null,
    val notes: String = "",
    val createdAt: Instant,
    val lastFoundOn: LocalDate? = null,
    val previousFoundOn: LocalDate? = null,
)

private val json = Json {
    prettyPrint = true
    // Newer app versions may add fields: older ones should still read the rest
    ignoreUnknownKeys = true
    explicitNulls = false
}

// Single line: a QR code has no room for indentation
private val compactJson = Json(json) { prettyPrint = false }

internal fun encodeFungast(spots: List<Spot>, exportedAt: Instant?, compact: Boolean = false): String =
    (if (compact) compactJson else json).encodeToString(
        FungastFileDto.serializer(),
        FungastFileDto(
            format = FORMAT_ID,
            version = FORMAT_VERSION,
            exportedAt = exportedAt,
            spots = spots.map {
                SpotDto(
                    name = it.name,
                    category = it.sortedCategories.firstOrNull()?.name,
                    categories = it.sortedCategories.map { category -> category.name },
                    latitude = it.latitude,
                    longitude = it.longitude,
                    altitude = it.altitude,
                    accuracyMeters = it.accuracyMeters,
                    notes = it.notes,
                    createdAt = Instant.fromEpochMilliseconds(it.createdAt),
                    lastFoundOn = it.lastFoundOn,
                    previousFoundOn = it.previousFoundOn,
                )
            },
        ),
    )

internal fun decodeFungast(text: String): List<Spot> {
    val file = try {
        json.decodeFromString(FungastFileDto.serializer(), text)
    } catch (e: SerializationException) {
        throw SpotFileException("File Fungast danneggiato", e)
    } catch (e: IllegalArgumentException) {
        throw SpotFileException("File Fungast danneggiato", e)
    }
    if (file.format != FORMAT_ID) throw SpotFileException("Non è un file Fungast")
    return file.spots.map {
        val (lastFoundOn, previousFoundOn) = lastTwoFinds(listOfNotNull(it.lastFoundOn, it.previousFoundOn))
        Spot(
            name = it.name.trim().ifEmpty { DEFAULT_IMPORTED_NAME },
            categories = it.categories.ifEmpty { listOfNotNull(it.category) }
                .map { name -> SpotCategory.entries.find { category -> category.name == name } ?: SpotCategory.OTHER }
                .toSet()
                .ifEmpty { setOf(SpotCategory.OTHER) },
            latitude = it.latitude,
            longitude = it.longitude,
            altitude = it.altitude,
            accuracyMeters = it.accuracyMeters,
            notes = it.notes.trim(),
            createdAt = it.createdAt.toEpochMilliseconds(),
            lastFoundOn = lastFoundOn,
            previousFoundOn = previousFoundOn,
        )
    }
}

internal const val DEFAULT_IMPORTED_NAME = "Posto importato"
