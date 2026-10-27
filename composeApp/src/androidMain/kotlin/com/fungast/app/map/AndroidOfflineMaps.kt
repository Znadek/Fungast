package com.fungast.app.map

import android.content.Context
import android.util.Log
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.offline.OfflineRegionError
import org.maplibre.android.offline.OfflineRegionStatus
import org.maplibre.android.offline.OfflineTilePyramidRegionDefinition

/**
 * Stores regions with MapLibre's [OfflineManager]. Downloaded tiles are used automatically
 * by every MapView using [MAP_STYLE_URL], with or without network.
 * Must be called from the main thread, like the rest of the MapLibre API.
 */
class AndroidOfflineMaps(private val context: Context) : OfflineMaps {

    override suspend fun downloadRegion(name: String, bounds: GeoBounds, onProgress: (Float) -> Unit): Boolean {
        val offlineManager = OfflineManager.getInstance(context)
        val definition = OfflineTilePyramidRegionDefinition(
            MAP_STYLE_URL,
            LatLngBounds.from(bounds.north, bounds.east, bounds.south, bounds.west),
            MIN_ZOOM,
            // Tiles past the source's own max zoom are not downloaded, the map overzooms them
            MAX_ZOOM,
            context.resources.displayMetrics.density,
        )
        val region = createRegion(offlineManager, definition, name) ?: return false
        val completed = download(region, onProgress)
        if (!completed) region.delete(NoOpDeleteCallback)
        return completed
    }

    private suspend fun createRegion(
        offlineManager: OfflineManager,
        definition: OfflineTilePyramidRegionDefinition,
        name: String,
    ): OfflineRegion? = suspendCancellableCoroutine { continuation ->
        offlineManager.createOfflineRegion(
            definition,
            name.encodeToByteArray(),
            object : OfflineManager.CreateOfflineRegionCallback {
                override fun onCreate(offlineRegion: OfflineRegion) = continuation.resume(offlineRegion)

                override fun onError(error: String) {
                    Log.w(TAG, "Cannot create offline region: $error")
                    continuation.resume(null)
                }
            },
        )
    }

    private suspend fun download(region: OfflineRegion, onProgress: (Float) -> Unit): Boolean =
        suspendCancellableCoroutine { continuation ->
            fun finish(completed: Boolean) {
                region.setObserver(null)
                region.setDownloadState(OfflineRegion.STATE_INACTIVE)
                if (continuation.isActive) continuation.resume(completed)
            }

            region.setObserver(object : OfflineRegion.OfflineRegionObserver {
                override fun onStatusChanged(status: OfflineRegionStatus) {
                    if (status.requiredResourceCount > 0) {
                        onProgress(status.completedResourceCount.toFloat() / status.requiredResourceCount)
                    }
                    if (status.isComplete) finish(completed = true)
                }

                // MapLibre would keep retrying forever while offline: give up and let the user retry
                override fun onError(error: OfflineRegionError) {
                    Log.w(TAG, "Offline download failed: ${error.reason} ${error.message}")
                    finish(completed = false)
                }

                override fun mapboxTileCountLimitExceeded(limit: Long) {
                    Log.w(TAG, "Offline region exceeds the tile limit of $limit")
                    finish(completed = false)
                }
            })
            continuation.invokeOnCancellation {
                region.setObserver(null)
                region.setDownloadState(OfflineRegion.STATE_INACTIVE)
            }
            region.setDownloadState(OfflineRegion.STATE_ACTIVE)
        }

    private object NoOpDeleteCallback : OfflineRegion.OfflineRegionDeleteCallback {
        override fun onDelete() = Unit
        override fun onError(error: String) {
            Log.w(TAG, "Cannot delete offline region: $error")
        }
    }

    private companion object {
        const val TAG = "AndroidOfflineMaps"
        const val MIN_ZOOM = 0.0
        const val MAX_ZOOM = 16.0
    }
}
