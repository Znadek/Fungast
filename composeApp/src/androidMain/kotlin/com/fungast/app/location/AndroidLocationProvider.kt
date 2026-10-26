package com.fungast.app.location

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import kotlin.coroutines.resume
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Uses the platform [LocationManager] (no Google Play Services), so it works offline
 * and on devices without Google services.
 */
class AndroidLocationProvider(private val activity: ComponentActivity) : LocationProvider {

    private var pendingPermission: CompletableDeferred<Boolean>? = null

    private val permissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        pendingPermission?.complete(grants.values.any { it })
        pendingPermission = null
    }

    override suspend fun requestPermission(): Boolean {
        if (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
            hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        ) return true

        val result = CompletableDeferred<Boolean>().also { pendingPermission = it }
        permissionLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
        return result.await()
    }

    override suspend fun currentLocation(): GeoPoint? {
        val locationManager = activity.getSystemService(LocationManager::class.java) ?: return null
        val provider = when {
            hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) &&
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION) &&
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> return null
        }
        // A cold GPS start in the woods can take a while
        return withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
            requestSingleFix(locationManager, provider)
        }?.toGeoPoint()
    }

    @Suppress("MissingPermission") // Checked in currentLocation()
    private suspend fun requestSingleFix(locationManager: LocationManager, provider: String): Location? =
        suspendCancellableCoroutine { continuation ->
            val cancellationSignal = CancellationSignal()
            continuation.invokeOnCancellation { cancellationSignal.cancel() }
            LocationManagerCompat.getCurrentLocation(
                locationManager,
                provider,
                cancellationSignal,
                ContextCompat.getMainExecutor(activity),
            ) { location -> continuation.resume(location) }
        }

    private fun hasPermission(permission: String) =
        ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED

    private fun Location.toGeoPoint() = GeoPoint(
        latitude = latitude,
        longitude = longitude,
        altitude = if (hasAltitude()) altitude else null,
        accuracyMeters = if (hasAccuracy()) accuracy else null,
    )

    private companion object {
        const val LOCATION_TIMEOUT_MS = 60_000L
    }
}
