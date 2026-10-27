package com.fungast.app.location

import kotlinx.coroutines.flow.Flow

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val accuracyMeters: Float? = null,
)

interface LocationProvider {
    /** Asks the user for location permission if not granted yet; returns whether it is granted. */
    suspend fun requestPermission(): Boolean

    /** Returns a fresh position fix, or null if none is available (GPS off, no permission, timeout). */
    suspend fun currentLocation(): GeoPoint?

    /** Emits position fixes while collected (permission must already be granted); stops GPS when collection stops. */
    fun locationUpdates(): Flow<GeoPoint>
}
