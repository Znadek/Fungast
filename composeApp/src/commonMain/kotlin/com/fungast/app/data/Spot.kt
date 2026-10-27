package com.fungast.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "spots")
data class Spot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: SpotCategory,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double?,
    val accuracyMeters: Float?,
    val notes: String,
    /** Epoch millis (UTC). */
    val createdAt: Long,
)

enum class SpotCategory(val label: String) {
    MUSHROOMS("Funghi"),
    CHESTNUTS("Castagne"),
    BERRIES("Frutti di bosco"),
    ASPARAGUS("Asparagi"),
    OTHER("Altro"),
}
