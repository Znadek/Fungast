package com.fungast.app.map

interface OfflineMaps {
    /**
     * Downloads the map tiles covering [bounds] so they stay available without network.
     * [onProgress] receives values in 0..1. Returns whether the download completed.
     */
    suspend fun downloadRegion(name: String, bounds: GeoBounds, onProgress: (Float) -> Unit): Boolean
}
