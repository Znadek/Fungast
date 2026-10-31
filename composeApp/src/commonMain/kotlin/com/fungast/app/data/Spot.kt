package com.fungast.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate

@Entity(tableName = "spots")
data class Spot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** What can be found there; never empty. */
    val categories: Set<SpotCategory>,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double?,
    val accuracyMeters: Float?,
    val notes: String,
    /** Epoch millis (UTC). */
    val createdAt: Long,
    /** Most recent find, if the user remembers it. */
    val lastFoundOn: LocalDate? = null,
    /** Find before [lastFoundOn]; only set when [lastFoundOn] is. */
    val previousFoundOn: LocalDate? = null,
    /** File names in the [com.fungast.app.photos.PhotoStore], oldest first. */
    @ColumnInfo(defaultValue = "") val photos: List<String> = emptyList(),
    /** How much the user usually finds there, if they said. */
    val quantity: SpotQuantity? = null,
)

enum class SpotCategory(val label: String) {
    MUSHROOMS("Funghi"),
    CHESTNUTS("Castagne"),
    BERRIES("Frutti di bosco"),
    ASPARAGUS("Asparagi"),
    OTHER("Altro"),
}

/** Stored by name: add new values at the end and never rename them. */
enum class SpotQuantity(val label: String) {
    LOW("Poco"),
    GOOD("Buono"),
    ABUNDANT("Abbondante"),
}

/** Categories in their fixed order, so lists and files always show them the same way. */
val Spot.sortedCategories: List<SpotCategory>
    get() = categories.sortedBy { it.ordinal }

/** The category that picks the marker color when a spot has several. */
val Spot.mainCategory: SpotCategory
    get() = sortedCategories.firstOrNull() ?: SpotCategory.OTHER

/** Find dates, most recent first. */
val Spot.findDates: List<LocalDate>
    get() = listOfNotNull(lastFoundOn, previousFoundOn)

/** Keeps the two most recent distinct dates, in whatever order they were entered. */
fun lastTwoFinds(dates: List<LocalDate>): Pair<LocalDate?, LocalDate?> {
    val recent = dates.distinct().sortedDescending()
    return recent.getOrNull(0) to recent.getOrNull(1)
}
