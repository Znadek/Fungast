package com.fungast.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A walk recorded with the GPS. While [endedAt] is null it is still being recorded; there is at most one such route. */
@Entity(tableName = "routes")
data class Route(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Epoch millis (UTC). */
    val startedAt: Long,
    /** Epoch millis (UTC); null while recording. */
    val endedAt: Long? = null,
    /** Kept up to date while recording, so lists don't need to load every point. */
    val distanceMeters: Double = 0.0,
)

@Entity(
    tableName = "route_points",
    foreignKeys = [
        ForeignKey(entity = Route::class, parentColumns = ["id"], childColumns = ["routeId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("routeId")],
)
data class RoutePoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routeId: Long,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double?,
    val accuracyMeters: Float?,
    /** Epoch millis (UTC). */
    val time: Long,
)
