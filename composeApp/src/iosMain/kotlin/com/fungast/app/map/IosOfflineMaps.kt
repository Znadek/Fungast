package com.fungast.app.map

// TODO: implement with MapLibre Native iOS (MLNOfflineStorage) when the iOS app is set up
class IosOfflineMaps : OfflineMaps {
    override suspend fun downloadRegion(name: String, bounds: GeoBounds, onProgress: (Float) -> Unit) = false
}
