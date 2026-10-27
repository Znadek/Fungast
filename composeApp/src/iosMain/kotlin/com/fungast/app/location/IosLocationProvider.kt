package com.fungast.app.location

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

// TODO: implement with CLLocationManager when the iOS app is set up (requires macOS to build)
class IosLocationProvider : LocationProvider {
    override suspend fun requestPermission(): Boolean = false
    override suspend fun currentLocation(): GeoPoint? = null
    override fun locationUpdates(): Flow<GeoPoint> = emptyFlow()
}
