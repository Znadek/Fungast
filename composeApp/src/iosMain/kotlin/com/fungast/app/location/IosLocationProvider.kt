package com.fungast.app.location

// TODO: implement with CLLocationManager when the iOS app is set up (requires macOS to build)
class IosLocationProvider : LocationProvider {
    override suspend fun requestPermission(): Boolean = false
    override suspend fun currentLocation(): GeoPoint? = null
}
